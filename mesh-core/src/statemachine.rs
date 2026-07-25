//! `statemachine` — relay decisions + dedup set for the message pipeline.
//!
//! The live ingest pipeline (parse -> verify -> decide, invariant #2) is
//! `MeshService.ingestFrame` in the platform shim, driven over FFI. This module holds the
//! two pieces the core owns: the epoch-aware bounded dedup set and the pure relay decision.
//! The old Rust-side `Engine` (a second, divergent ingest pipeline) was deleted — one
//! pipeline only, or the two will drift.

use crate::codec::{self, MsgType, FRAME_LEN};
use crate::crypto;
use crate::message::DEFAULT_TTL_REGIONAL;
use crate::vdl;
use std::collections::{HashMap, VecDeque};

/// Per-epoch insertion sub-cap (C8). A flood of distinct valid frames sharing one epoch
/// bucket can fill at most this many slots, so a single-epoch storm cannot evict the
/// legitimate hashes of the other live epochs (replay/eviction-window mitigation).
pub const EPOCH_BUCKET_CAP: usize = 1024;

/// Bounded FIFO-evicting dedup set with time-decaying epoch awareness (E4).
///
/// Stores up to `cap` frame (hash, epoch) pairs.  When the set is full and a new hash arrives,
/// the oldest entry is evicted before insertion.  [`check_and_insert_epoch`] additionally evicts
/// entries whose epoch is more than 2 behind the provided epoch, providing time-decaying behavior
/// without relying solely on capacity-based eviction (window of ~3 epochs).
///
/// The plain [`check_and_insert`] delegates with epoch 0 (no epoch-based eviction — only
/// capacity-based FIFO).  Use [`check_and_insert_epoch`] when the caller has the frame epoch.
pub struct Dedup {
    /// hash → epoch (fast lookup + epoch metadata for time-decaying eviction).
    seen: HashMap<[u8; 16], u32>,
    order: VecDeque<[u8; 16]>,
    cap: usize,
    /// epoch → live entry count (C8 sub-cap accounting; epoch-0 sentinel entries excluded).
    epoch_counts: HashMap<u32, usize>,
}

impl Dedup {
    /// Create a new `Dedup` with the given capacity.  A `cap` of 0 is clamped to 1.
    pub fn new(cap: usize) -> Self {
        let cap = cap.max(1);
        Dedup {
            seen: HashMap::with_capacity(cap),
            order: VecDeque::with_capacity(cap),
            cap,
            epoch_counts: HashMap::new(),
        }
    }

    /// Returns `true` iff `hash` was NOT seen before (fresh).  Delegates to
    /// [`check_and_insert_epoch`] with `epoch = 0` (no time-decaying eviction).
    pub fn check_and_insert(&mut self, hash: [u8; 16]) -> bool {
        self.check_and_insert_epoch(hash, 0)
    }

    /// Returns `true` iff `hash` was NOT seen before (fresh).
    ///
    /// Before inserting, evicts entries whose stored epoch is more than 2 behind `epoch`
    /// (time-decaying window of ~3 epochs).  Entries stored with epoch 0 (the backwards-compatible
    /// sentinel) are never purged by the time-decay path, only by capacity eviction.
    ///
    /// C8: insertions into an epoch bucket that already holds [`EPOCH_BUCKET_CAP`] entries are
    /// REFUSED (returns false, nothing evicted) — a single-epoch flood cannot push legitimate
    /// hashes of other epochs out of the global FIFO.
    ///
    /// Then, if the set is already at `cap`, the single oldest entry is evicted (FIFO).
    pub fn check_and_insert_epoch(&mut self, hash: [u8; 16], epoch: u32) -> bool {
        // Purge entries older than epoch-2 (time-decaying eviction).
        // Entries with epoch 0 (legacy sentinel) are skipped so old callers that don't
        // supply epoch don't get unexpected eviction.
        while let Some(oldest_hash) = self.order.front().copied() {
            // R7: saturating_sub — epochs arrive from the wire; `oldest_epoch + 2` would
            // overflow on an adversarial u32::MAX epoch (panic in overflow-checked builds).
            match self.seen.get(&oldest_hash) {
                Some(&oldest_epoch) if oldest_epoch != 0 && epoch.saturating_sub(oldest_epoch) > 2 => {
                    self.order.pop_front();
                    self.seen.remove(&oldest_hash);
                    self.decrement_bucket(oldest_epoch);
                }
                _ => break,
            }
        }

        // Check for duplicate.
        if self.seen.contains_key(&hash) {
            return false;
        }

        // C8: per-epoch sub-cap — refuse without evicting other epochs' entries.
        if epoch != 0 && self.epoch_counts.get(&epoch).copied().unwrap_or(0) >= EPOCH_BUCKET_CAP {
            return false;
        }

        // Cap-based eviction (FIFO).
        if self.order.len() >= self.cap
            && let Some(oldest) = self.order.pop_front()
        {
            if let Some(oldest_epoch) = self.seen.remove(&oldest) {
                self.decrement_bucket(oldest_epoch);
            }
        }

        self.seen.insert(hash, epoch);
        self.order.push_back(hash);
        if epoch != 0 {
            *self.epoch_counts.entry(epoch).or_insert(0) += 1;
        }
        true
    }

    fn decrement_bucket(&mut self, epoch: u32) {
        if epoch == 0 {
            return;
        }
        if let Some(c) = self.epoch_counts.get_mut(&epoch) {
            *c = c.saturating_sub(1);
            if *c == 0 {
                self.epoch_counts.remove(&epoch);
            }
        }
    }
}

// Wire offset at which the witness field begins; VDL prefix is buf[0..WITNESS_PREFIX_END].
// Mirrors codec layout: body occupies bytes 38..102, pocp_wit occupies 102..118.
const WITNESS_PREFIX_END: usize = 102;

/// Relay decision for a single received frame, parameterised by VDL difficulty.
///
/// Processing order (invariant #2): decode → sig verify → TTL cap/witness → decide.
/// The ephemeral Ed25519 pubkey is embedded at buf[118..150] so every relay can verify.
///
/// Production callers use `relay_decision` (which supplies `vdl::VDL_DIFFICULTY_BITS`).
/// Tests use this helper directly with a low difficulty so they don't burn 2^22 hashes.
///
/// Logic:
/// - All types: CLOBBER incoming TTL at byte 214 to max(DEFAULT_TTL_REGIONAL) (TTL cap, C1).
/// - `RegionalPropagated`: relay if ttl > 0 (decrement byte 214).
/// - `Private`: relay only if `vdl::verify` passes AND ttl > 0 (decrement byte 214).
/// - `LocalImmediate`: relay exactly once — any incoming ttl > 0 is CLOBBERED to 0, never
///   decremented, so an adversary advertising ttl=255 still gets exactly one hop. The echo
///   is the originator's receipt (send-and-listen); display stays PoCP-gated upstream.
pub fn relay_decision_with_difficulty(
    buf: &[u8; FRAME_LEN],
    difficulty_bits: u8,
) -> Option<[u8; FRAME_LEN]> {
    let frame = codec::decode(buf).ok()?;

    // A1: verify Ed25519 signature using the embedded ephemeral pubkey.
    if !crypto::verify(&frame.pk, codec::signing_region(buf), &frame.sig) {
        return None;
    }

    match frame.msg_type {
        MsgType::RegionalPropagated => {
            // C1: cap incoming TTL to DEFAULT_TTL_REGIONAL (anti-flood).
            let ttl = buf[214].min(DEFAULT_TTL_REGIONAL);
            if ttl == 0 {
                return None;
            }
            let mut out = *buf;
            out[214] = ttl - 1;
            Some(out)
        }
        MsgType::Private => {
            // C1: same TTL cap before VDL check.
            let raw_ttl = buf[214].min(DEFAULT_TTL_REGIONAL);
            if !vdl::verify(&buf[..WITNESS_PREFIX_END], &frame.pocp_wit, difficulty_bits) {
                return None;
            }
            if raw_ttl == 0 {
                return None;
            }
            let mut out = *buf;
            out[214] = raw_ttl - 1;
            Some(out)
        }
        MsgType::LocalImmediate => {
            // C1+: any incoming ttl > 0 relays exactly once, with TTL clobbered to 0.
            // Not decremented: an adversary setting ttl=255 gets the same single hop as
            // an honest ttl=1. TTL=0 on the wire → no further relay, hard bound.
            if buf[214] == 0 {
                return None;
            }
            let mut out = *buf;
            out[214] = 0;
            Some(out)
        }
    }
}

// DEFAULT_TTL_REGIONAL is defined in crate::message — used as TTL cap here.

/// Pure relay decision for a single received frame.
///
/// Production entry point; tests use `relay_decision_with_difficulty` with a low difficulty
/// so they don't burn 2^22 hashes.
///
/// Decodes `buf` via the codec (returns `None` on any decode error).  Then:
    /// - `RegionalPropagated`: relay if ttl > 0 (decrement byte 214).
/// - `Private`: relay only if VDL witness passes at `vdl::VDL_DIFFICULTY_BITS` and ttl > 0.
/// - `LocalImmediate`: relay once with TTL clobbered to 0 (reflection receipt; anti-flood).
///
/// # Caller contract
/// Invoke this only for frames that have already passed dedup (`Dedup::check_and_insert`
/// returned `true`).  Rebroadcast the returned buffer verbatim.
pub fn relay_decision(buf: &[u8; FRAME_LEN]) -> Option<[u8; FRAME_LEN]> {
    relay_decision_with_difficulty(buf, vdl::VDL_DIFFICULTY_BITS)
}

#[cfg(test)]
mod tests {
    use super::{relay_decision, relay_decision_with_difficulty, Dedup};
    use crate::codec::{self, MsgType, FRAME_LEN};
    use crate::crypto;
    use crate::message;

    fn hash(tag: u8) -> [u8; 16] {
        [tag; 16]
    }

    #[test]
    fn dedup_fresh_then_repeat() {
        let mut d = Dedup::new(4);
        assert!(d.check_and_insert(hash(1)), "first insert is fresh");
        assert!(!d.check_and_insert(hash(1)), "second insert is a duplicate");
    }

    #[test]
    fn dedup_eviction_at_cap() {
        // cap=2: insert hashes A, B, C. A should be evicted so a fourth call with A returns true.
        let mut d = Dedup::new(2);
        assert!(d.check_and_insert(hash(0xa))); // A — fresh
        assert!(d.check_and_insert(hash(0xb))); // B — fresh, set now at cap
        assert!(d.check_and_insert(hash(0xc))); // C — fresh, A evicted
        // A was evicted, so it should be fresh again.
        assert!(d.check_and_insert(hash(0xa)), "A must be fresh after eviction");
        // B was also evicted when C was inserted, so it should also be fresh.
        assert!(d.check_and_insert(hash(0xb)), "B must be fresh after eviction");
    }

    #[test]
    fn dedup_zero_cap_clamped_to_one() {
        let mut d = Dedup::new(0);
        assert!(d.check_and_insert(hash(1)));
        // With cap=1, inserting a second distinct hash evicts the first.
        assert!(d.check_and_insert(hash(2)));
        // hash(1) was evicted, so it is fresh again.
        assert!(d.check_and_insert(hash(1)));
    }

    // ----- relay_decision tests -----

    fn test_seed() -> [u8; 32] {
        let mut s = [0u8; 32];
        for (i, b) in s.iter_mut().enumerate() {
            *b = i as u8;
        }
        s
    }

    fn test_beacon_seed() -> [u8; 32] {
        let mut bs = [0u8; 32];
        for (i, b) in bs.iter_mut().enumerate() {
            *b = (i + 100) as u8;
        }
        bs
    }

    #[test]
    fn relay_decision_regional_decrements_ttl() {
        let seed = test_seed();
        let bs = test_beacon_seed();
        let buf =
            message::make_message_frame_ttl(&seed, 1, &bs, MsgType::RegionalPropagated, "relay", 8)
                .expect("short text");
        let relayed = relay_decision(&buf).expect("should relay");
        assert_eq!(relayed[214], 7, "TTL must be decremented by 1");
        // All other bytes must be identical.
        for i in 0..FRAME_LEN {
            if i != 214 {
                assert_eq!(
                    relayed[i], buf[i],
                    "byte {i} must be unchanged after relay"
                );
            }
        }
    }

    #[test]
    fn relay_decision_ttl_zero_returns_none() {
        let seed = test_seed();
        let bs = test_beacon_seed();
        let buf =
            message::make_message_frame_ttl(&seed, 1, &bs, MsgType::RegionalPropagated, "relay", 0)
                .expect("short text");
        assert!(
            relay_decision(&buf).is_none(),
            "TTL=0 must produce None (drop)"
        );
    }

    #[test]
    fn relay_decision_local_ttl_zero_returns_none() {
        let seed = test_seed();
        let bs = test_beacon_seed();
        // LocalImmediate with TTL 0 is end-of-line: not relayed.
        let buf =
            message::make_message_frame_ttl(&seed, 1, &bs, MsgType::LocalImmediate, "local", 0)
                .expect("short text");
        assert!(
            relay_decision(&buf).is_none(),
            "LocalImmediate with TTL 0 must not be relayed"
        );
    }

    #[test]
    fn relay_decision_local_relays_once_with_ttl_clobbered_to_zero() {
        let seed = test_seed();
        let bs = test_beacon_seed();
        // Honest local frame (ttl=1): relayed exactly once, as ttl=0.
        let buf = message::make_message_frame(&seed, 1, &bs, MsgType::LocalImmediate, "local")
            .expect("short text");
        assert_eq!(buf[214], 1, "fresh local frame must originate at ttl=1");
        let relayed = relay_decision(&buf).expect("local frame must be relayed once");
        assert_eq!(relayed[214], 0, "relayed local TTL must be clobbered to 0");
        // All other bytes must be identical.
        for i in 0..FRAME_LEN {
            if i != 214 {
                assert_eq!(relayed[i], buf[i], "byte {i} must be unchanged after relay");
            }
        }
        // The ttl=0 echo is never relayed again.
        assert!(
            relay_decision(&relayed).is_none(),
            "relayed local echo (ttl=0) must not be relayed"
        );
    }

    #[test]
    fn relay_decision_local_adversary_high_ttl_clobbered_to_zero() {
        let seed = test_seed();
        let bs = test_beacon_seed();
        // Adversary originates a local frame with an inflated TTL: the relay clobbers it
        // to 0 — the flood budget is one hop regardless.
        let buf =
            message::make_message_frame_ttl(&seed, 1, &bs, MsgType::LocalImmediate, "local", 255)
                .expect("short text");
        let relayed = relay_decision(&buf).expect("ttl>0 relays once");
        assert_eq!(
            relayed[214], 0,
            "adversarial ttl=255 must be clobbered to 0, not decremented"
        );
        assert!(relay_decision(&relayed).is_none());
    }

    #[test]
    fn relay_decision_garbage_returns_none() {
        // A buffer of all zeros has a wrong version byte — codec must reject it.
        let buf = [0u8; FRAME_LEN];
        assert!(
            relay_decision(&buf).is_none(),
            "garbage/BadVersion buffer must produce None"
        );
    }

    // ----- Private-frame relay tests (difficulty 8 to avoid 2^22 hashes in CI) -----

    #[test]
    fn private_frame_with_valid_witness_relays() {
        let seed = [7u8; 32];
        let bs = test_beacon_seed();
        let pair_key = [9u8; 32];
        let frame = message::make_private_frame(&seed, 1, &bs, &pair_key, "x", 8, 0)
            .expect("short text");
        let initial_ttl = frame[214];
        let result = relay_decision_with_difficulty(&frame, 8);
        let relayed = result.expect("valid witness must relay");
        assert_eq!(
            relayed[214],
            initial_ttl - 1,
            "byte 214 must be decremented by 1"
        );
        for i in 0..FRAME_LEN {
            if i != 214 {
                assert_eq!(relayed[i], frame[i], "byte {i} must be unchanged");
            }
        }
    }

    #[test]
    fn private_frame_with_bad_witness_drops() {
        let seed = [7u8; 32];
        let bs = test_beacon_seed();
        let pair_key = [9u8; 32];
        let mut frame = message::make_private_frame(&seed, 1, &bs, &pair_key, "x", 8, 0)
            .expect("short text");
        // Flip a bit inside the witness field (bytes 102..118) to corrupt it.
        frame[102] ^= 0x01;
        assert!(
            relay_decision_with_difficulty(&frame, 8).is_none(),
            "corrupted witness must drop silently"
        );
    }

    #[test]
    fn private_frame_ttl_zero_drops() {
        let seed = [7u8; 32];
        let bs = test_beacon_seed();
        let pair_key = [9u8; 32];
        let mut frame = message::make_private_frame(&seed, 1, &bs, &pair_key, "x", 8, 0)
            .expect("short text");
        frame[214] = 0;
        assert!(
            relay_decision_with_difficulty(&frame, 8).is_none(),
            "TTL=0 private frame must drop"
        );
    }

    #[test]
    fn relayed_frame_decodes_and_sig_verifies() {
        let seed = test_seed();
        let bs = [200u8; 32];
        let epoch = 5u32;
        let buf =
            message::make_message_frame_ttl(&seed, epoch, &bs, MsgType::RegionalPropagated, "verify", 8)
                .expect("short text");
        let relayed = relay_decision(&buf).expect("should relay");

        // Decode must succeed.
        let frame = codec::decode(&relayed).expect("relayed frame must decode");

        // Signature must still verify — reserved is outside SIG_REGION.
        let e = crypto::from_seed(&seed, &bs);
        let pk = crypto::public_key(&e);
        assert!(
            crypto::verify(&pk, codec::signing_region(&relayed), &frame.sig),
            "signature must be valid after relay"
        );
    }
}

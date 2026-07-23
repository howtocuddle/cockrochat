//! `statemachine` — the message engine. Owns seen-set (time-decaying Bloom, window 2*T_epoch),
//! Trickle timers (K_supp, W, RSSI-biased slot), TTL/H_max, tier routing, dispatch to `trust`.
//!
//! PROCESSING ORDER IS ENFORCED HERE AND NON-NEGOTIABLE (invariant #2, v1 §5.5):
//!   len -> mark-unseen -> sig-verify -> pocp-witness-check -> relay/render.
//! Nothing is relayed or rendered before validation completes. See README.md §2.

use crate::codec::{self, MsgType, FRAME_LEN};
use crate::crypto;
use crate::message::{self, DEFAULT_TTL_REGIONAL};
use crate::pocp::{self, CellSketch};
use crate::vdl;
use std::collections::{HashMap, VecDeque};

/// Routing tier for an originated message.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Tier {
    /// Tier-1: local-immediate (single hop, no propagation).
    LocalImmediate,
    /// Tier-2: regional-propagated (flood + Trickle + dedup).
    RegionalPropagated,
    /// Tier-3: private plane (QR pairing + Noise ratchet). DEFERRED past v0 — interface stub only.
    Private,
}

/// A validated, renderable alert handed up to the UI.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Alert {
    pub id: [u8; 16],
    pub body: [u8; 64],
}

/// Why a frame was dropped (never surfaced to the wire; local diagnostics only).
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Reason {
    Decode,
    StaleEpoch,
    Seen,
    BadSig,
    BadWitness,
}

/// A security-relevant event to log/alarm (e.g. CellMismatch => relocation/replay).
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SecurityEvent {
    CellMismatch,
    ChainStall,
    MalformedStorm,
}

/// The single decision produced by ingesting a received frame.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Ingest {
    Relay([u8; FRAME_LEN]),
    Deliver(Alert),
    Drop(Reason),
    Alarm(SecurityEvent),
}

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
}

impl Dedup {
    /// Create a new `Dedup` with the given capacity.  A `cap` of 0 is clamped to 1.
    pub fn new(cap: usize) -> Self {
        let cap = cap.max(1);
        Dedup {
            seen: HashMap::with_capacity(cap),
            order: VecDeque::with_capacity(cap),
            cap,
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
                }
                _ => break,
            }
        }

        // Check for duplicate.
        if self.seen.contains_key(&hash) {
            return false;
        }

        // Cap-based eviction (FIFO).
        if self.order.len() >= self.cap
            && let Some(oldest) = self.order.pop_front()
        {
            self.seen.remove(&oldest);
        }

        self.seen.insert(hash, epoch);
        self.order.push_back(hash);
        true
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
/// - `LocalImmediate`: never relayed.
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
        MsgType::LocalImmediate => None,
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
/// - `LocalImmediate`: never relayed.
///
/// # Caller contract
/// Invoke this only for frames that have already passed dedup (`Dedup::check_and_insert`
/// returned `true`).  Rebroadcast the returned buffer verbatim.
pub fn relay_decision(buf: &[u8; FRAME_LEN]) -> Option<[u8; FRAME_LEN]> {
    relay_decision_with_difficulty(buf, vdl::VDL_DIFFICULTY_BITS)
}

/// **DEAD CODE — DO NOT USE.** The live ingest pipeline is `MeshService.ingestFrame` in Kotlin.
/// This `Engine` was the intended Rust-side ingestion path but is not wired into the running
/// node. The Rust test suite still exercises `on_recv`; production paths MUST go through
/// Kotlin.  If the two pipelines diverge further, delete this and move its tests.
///
/// Constructed by the platform shim (cap = dedup capacity, e.g. 4096). Driven by
/// radio callbacks + a timer tick.
///
/// # v0 note: epoch validation
/// The engine does NOT yet validate that the frame's epoch is within [N, N-1] of the
/// local clock epoch — that check is delegated to the shim (`now_ms` is accepted but
/// unused). Future versions will own the epoch clock.
#[allow(dead_code)]
pub struct Engine {
    dedup: Dedup,
}

#[allow(dead_code)]
impl Engine {
    /// Create a new `Engine` with a dedup set of the given `cap`acity (minimum 1).
    pub fn new(cap: usize) -> Self {
        Engine {
            dedup: Dedup::new(cap),
        }
    }

    /// Ingest one raw received frame: parse -> verify -> decide (order fixed above).
    ///
    /// Processing order (invariant #2):
    ///   1. Decode — structural check (length, version, message type).
    ///   2. Dedup — time-decaying epoch-aware duplicate suppression.
    ///   3. Sig verify — Ed25519 against the embedded ephemeral pubkey.
    ///   4. PoCP witness check (v0: Tier 1/2 only, skipped if `local_sketch` is `None`) —
    ///      verifies the sender knew the claimed cell sketch AND the sketch overlaps
    ///      the local observation (`jaccard >= tau`). CellMismatch → `Alarm`, Stale → `Drop`.
    ///   5. Relay decision — TTL cap/decrement, VDL witness check for Private.
    ///
    /// Returns:
    /// - [`Ingest::Relay`] if the frame should be forwarded (TTL > 0, sig + witness OK).
    /// - [`Ingest::Deliver`] for `LocalImmediate` frames (display only, never relayed).
    /// - [`Ingest::Drop`] with a [`Reason`] explaining the rejection.
    /// - [`Ingest::Alarm`] if a security event (e.g. CellMismatch) is detected.
    ///
    /// The shim is responsible for independently extracting body text for UI display
    /// after this call.
    pub fn on_recv(
        &mut self,
        raw: &[u8; FRAME_LEN],
        _rssi: i8,
        _now_ms: u64,
        local_sketch: Option<&CellSketch>,
        tau: f32,
    ) -> Ingest {
        // 1. Decode — structural check.
        let frame = match codec::decode(raw) {
            Ok(f) => f,
            Err(_) => return Ingest::Drop(Reason::Decode),
        };

        // 2. Dedup — time-decaying epoch-aware suppression.
        let hash = message::frame_hash(raw);
        if !self.dedup.check_and_insert_epoch(hash, frame.epoch) {
            return Ingest::Drop(Reason::Seen);
        }

        // 3. Sig verify — Ed25519 against the embedded ephemeral pubkey.
        if !crypto::verify(&frame.pk, codec::signing_region(raw), &frame.sig) {
            return Ingest::Drop(Reason::BadSig);
        }

        // 4. PoCP witness check — for Tier 1/2, verify sender proximity.
        //    Private frames skip this (they use VDL cost gate + AEAD instead).
        if (frame.msg_type == MsgType::LocalImmediate
            || frame.msg_type == MsgType::RegionalPropagated)
            && let Some(local) = local_sketch
        {
                match pocp::verify_witness_local(
                    local,
                    &frame.div_sketch,
                    frame.epoch,
                    &raw[..WITNESS_PREFIX_END],
                    &frame.pocp_wit,
                    tau,
                ) {
                    pocp::WitVerdict::Valid => { /* proceed to step 5 */ }
                    pocp::WitVerdict::CellMismatch => {
                        return Ingest::Alarm(SecurityEvent::CellMismatch);
                    }
                    pocp::WitVerdict::Stale => {
                        return Ingest::Drop(Reason::BadWitness);
                }
            }
        }

        // 5. Relay decision — TTL cap/decrement + VDL witness for Private.
        match frame.msg_type {
            MsgType::LocalImmediate => {
                // Display only, never relayed.
                Ingest::Deliver(Alert {
                    id: frame.mark,
                    body: frame.body,
                })
            }
            MsgType::RegionalPropagated | MsgType::Private => {
                match relay_decision(raw) {
                    Some(relayed) => Ingest::Relay(relayed),
                    None => Ingest::Drop(Reason::BadWitness),
                }
            }
        }
    }

    /// Originate a local message on the given tier; returns the frame to advertise.
    pub fn on_originate(&mut self, _tier: Tier, _body: [u8; 64]) -> [u8; FRAME_LEN] {
        todo!("M4")
    }

    /// Fire any due (unsuppressed) rebroadcasts.
    pub fn tick(&mut self, _now_ms: u64) -> Vec<[u8; FRAME_LEN]> {
        todo!("M4: Trickle")
    }
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
    fn relay_decision_local_immediate_returns_none() {
        let seed = test_seed();
        let bs = test_beacon_seed();
        // LocalImmediate with any TTL must never be relayed.
        let buf =
            message::make_message_frame_ttl(&seed, 1, &bs, MsgType::LocalImmediate, "local", 8)
                .expect("short text");
        assert!(
            relay_decision(&buf).is_none(),
            "LocalImmediate must never be relayed"
        );
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

//! Narrow, byte-oriented FFI surface exposed to the platform shims via UniFFI.
//!
//! INVARIANT #1: the shims pass RAW bytes only. Every parse/verify/decide step lives in the
//! core; nothing here hands a shim a half-parsed structure it could act on. The surface is
//! intentionally tiny — it grows only as the state machine (M4+) needs to be driven.

use std::sync::atomic::{AtomicBool, Ordering};

use crate::beacon;
use crate::codec::{self, FRAME_LEN, MsgType};
use crate::crypto;
use crate::message;
use crate::pocp::{self, CellSketch, KMV_K};
use crate::statemachine;
use crate::vdl;

/// Global panic-wipe flag. Set by `panic_wipe()`; the platform shim polls or checks after the
/// call and must clear persisted state (PairStore, ConfigStore) and stop the service.
static PANIC_WIPED: AtomicBool = AtomicBool::new(false);

/// Fixed wire frame size in bytes (226). Lets a shim size its radio buffers correctly.
#[uniffi::export]
pub fn frame_len() -> u32 {
    FRAME_LEN as u32
}

/// True iff `bytes` is a structurally-valid frame (len + version + type). Parse-before-forward:
/// a shim can cheaply reject junk before touching the state machine.
#[uniffi::export]
pub fn frame_decodes(bytes: Vec<u8>) -> bool {
    codec::decode(&bytes).is_ok()
}

/// Build a signed test frame from a 32-byte `seed`. Proves encode + crypto across the FFI
/// boundary from Kotlin/Swift. Returns the 226 B wire frame, or `None` if `seed` is not 32 B.
/// Smoke-test helper only — real origination goes through the state machine.
///
/// Delegates to `message::make_message_frame` so there is exactly one origination path.
#[uniffi::export]
pub fn make_test_frame(seed: Vec<u8>, epoch: u32, beacon_seed: Vec<u8>) -> Option<Vec<u8>> {
    let seed: &[u8; 32] = seed.as_slice().try_into().ok()?;
    let bs: &[u8; 32] = beacon_seed.as_slice().try_into().ok()?;
    Some(message::make_message_frame(seed, epoch, bs, MsgType::RegionalPropagated, "")?.to_vec())
}

// ---------------------------------------------------------------------------
// Message origination + body-text extraction
// ---------------------------------------------------------------------------

/// Build a signed message frame. `local_immediate` selects LocalImmediate; otherwise
/// RegionalPropagated. Returns `None` if `seed` or `beacon_seed` is not 32 bytes or
/// `text` is longer than 63 bytes.
#[uniffi::export]
pub fn make_message_frame(
    seed: Vec<u8>,
    epoch: u32,
    beacon_seed: Vec<u8>,
    local_immediate: bool,
    text: String,
) -> Option<Vec<u8>> {
    let seed: &[u8; 32] = seed.as_slice().try_into().ok()?;
    let bs: &[u8; 32] = beacon_seed.as_slice().try_into().ok()?;
    let msg_type = if local_immediate {
        MsgType::LocalImmediate
    } else {
        MsgType::RegionalPropagated
    };
    Some(message::make_message_frame(seed, epoch, bs, msg_type, &text)?.to_vec())
}

/// Build a signed message frame with an explicit TTL. `local_immediate` selects LocalImmediate;
/// otherwise RegionalPropagated. Returns `None` if `seed` is not 32 bytes or `text` is longer
/// than 63 bytes.
///
/// Wire byte 214 (`reserved[0]`) is set to `ttl`; the signature is unaffected because the
/// reserved region is outside `SIG_REGION` (`0..150`).
#[uniffi::export]
pub fn make_message_frame_ttl(
    seed: Vec<u8>,
    epoch: u32,
    beacon_seed: Vec<u8>,
    local_immediate: bool,
    text: String,
    ttl: u8,
) -> Option<Vec<u8>> {
    let seed: &[u8; 32] = seed.as_slice().try_into().ok()?;
    let bs: &[u8; 32] = beacon_seed.as_slice().try_into().ok()?;
    let msg_type = if local_immediate {
        MsgType::LocalImmediate
    } else {
        MsgType::RegionalPropagated
    };
    Some(message::make_message_frame_ttl(seed, epoch, bs, msg_type, &text, ttl)?.to_vec())
}

/// Build a signed public message frame WITH a PoCP spacetime witness.
///
/// Same as `make_message_frame_ttl` but embeds a `div_sketch` (16 bytes from
/// `pocp_sketch_to_div_sketch`) and computes a PoCP witness so the frame proves the
/// sender was physically present in the cell.
///
/// `epoch` is the frame epoch (freshness); `wit_epoch` is the epoch the SKETCH was built
/// from and seeds the witness MAC — pass `epoch - 1` when signing the previous epoch's
/// completed sketch at epoch rollover (A1/C2 bootstrap). Receivers accept both.
///
/// Returns `None` if `seed` is not 32 bytes, `div_sketch` is not 16 bytes, or `text`
/// exceeds 63 UTF-8 bytes. Private frames must use `make_private_frame` instead.
#[uniffi::export]
#[allow(clippy::too_many_arguments)]
pub fn make_message_frame_with_witness(
    seed: Vec<u8>,
    epoch: u32,
    beacon_seed: Vec<u8>,
    local_immediate: bool,
    text: String,
    ttl: u8,
    div_sketch: Vec<u8>,
    wit_epoch: u32,
) -> Option<Vec<u8>> {
    let seed: &[u8; 32] = seed.as_slice().try_into().ok()?;
    let bs: &[u8; 32] = beacon_seed.as_slice().try_into().ok()?;
    let div: [u8; 16] = div_sketch.as_slice().try_into().ok()?;
    let msg_type = if local_immediate {
        MsgType::LocalImmediate
    } else {
        MsgType::RegionalPropagated
    };
    Some(message::make_message_frame_with_witness(seed, epoch, bs, msg_type, &text, ttl, div, wit_epoch)?.to_vec())
}

/// Relay a received frame: decrement the TTL at byte 214 and return the modified buffer, or
/// `None` if the frame should be dropped (bad length, decode error, or TTL already 0).
/// The returned buffer is safe to rebroadcast verbatim; the signature is intact.
///
/// LocalImmediate IS relayed — exactly once, with its TTL clobbered to 0 so the copy cannot
/// be relayed again. That single reflected hop is the delivery-receipt mechanism
/// (send-and-listen): the originator hears its own frame come back. This doc previously
/// claimed LocalImmediate was dropped, which would make both receipts and local propagation
/// impossible; the code has always relayed it (see `statemachine::relay_decision`).
#[uniffi::export]
pub fn relay_frame(bytes: Vec<u8>) -> Option<Vec<u8>> {
    let buf: [u8; FRAME_LEN] = bytes.as_slice().try_into().ok()?;
    Some(statemachine::relay_decision(&buf)?.to_vec())
}

/// Extract the TTL from wire byte 214 of a frame. Returns `None` unless the frame decodes
/// successfully (correct length, version, and message type).
#[uniffi::export]
pub fn frame_ttl(bytes: Vec<u8>) -> Option<u8> {
    let buf: [u8; FRAME_LEN] = bytes.as_slice().try_into().ok()?;
    codec::decode(&buf).ok()?;
    Some(buf[214])
}

/// The TTL a RegionalPropagated or Private frame carries AT ORIGINATION.
///
/// Presence / direct-RF detection: relays always decrement (regional/private) or clobber
/// to 0 (local), so a received frame whose TTL still equals its type's origination TTL
/// came straight from the originator (direct RF), while any lower TTL arrived via the
/// relay path. Kept in Rust so the shim never hardcodes protocol constants (invariant #1).
#[uniffi::export]
pub fn default_ttl_regional() -> u32 {
    message::DEFAULT_TTL_REGIONAL as u32
}

/// Origination TTL for LocalImmediate frames — see [`default_ttl_regional`] for why the
/// shim must read this from the core. Local frames originate at this TTL and relays
/// clobber to 0, so `ttl == default_ttl_local` ⇔ direct RF from the originator.
#[uniffi::export]
pub fn default_ttl_local() -> u32 {
    message::DEFAULT_TTL_LOCAL as u32
}

/// Decode `bytes` then extract the body text. Returns `None` on any failure.
#[uniffi::export]
pub fn frame_body_text(bytes: Vec<u8>) -> Option<String> {
    let frame = codec::decode(&bytes).ok()?;
    Some(message::body_text(&frame)?.to_owned())
}

/// Compute the 16-byte dedup hash of a frame buffer. Returns `None` unless `bytes` is exactly
/// 226 bytes long.
#[uniffi::export]
pub fn frame_hash(bytes: Vec<u8>) -> Option<Vec<u8>> {
    let buf: [u8; FRAME_LEN] = bytes.as_slice().try_into().ok()?;
    Some(message::frame_hash(&buf).to_vec())
}

// ---------------------------------------------------------------------------
// Dedup object (UniFFI)
// ---------------------------------------------------------------------------

/// A bounded FIFO-evicting dedup set, exposed to the platform shims via UniFFI.
#[derive(uniffi::Object)]
pub struct FfiDedup {
    inner: std::sync::Mutex<crate::statemachine::Dedup>,
}

#[uniffi::export]
impl FfiDedup {
    /// Create a new `FfiDedup` with the given capacity, clamped to 1..=2^20 so a
    /// shim bug cannot trigger a multi-GB allocation abort across the FFI boundary (R6).
    #[uniffi::constructor]
    pub fn new(cap: u32) -> std::sync::Arc<Self> {
        let cap = (cap as usize).clamp(1, 1 << 20);
        std::sync::Arc::new(FfiDedup {
            inner: std::sync::Mutex::new(crate::statemachine::Dedup::new(cap)),
        })
    }

    /// Returns `true` iff the hash is fresh (not previously seen). A hash of the wrong length
    /// returns `false` and inserts nothing.
    pub fn check_and_insert(&self, hash: Vec<u8>) -> bool {
        let hash: [u8; 16] = match hash.as_slice().try_into() {
            Ok(h) => h,
            Err(_) => return false,
        };
        self.inner
            .lock()
            .unwrap_or_else(|e| e.into_inner())
            .check_and_insert(hash)
    }

    /// Like [`check_and_insert`] but also evicts entries whose epoch is more than 2 behind
    /// the given `epoch` (time-decaying window of ~3 epochs). Use this instead of
    /// [`check_and_insert`] when the caller has the frame's epoch.
    pub fn check_and_insert_epoch(&self, hash: Vec<u8>, epoch: u32) -> bool {
        let hash: [u8; 16] = match hash.as_slice().try_into() {
            Ok(h) => h,
            Err(_) => return false,
        };
        self.inner
            .lock()
            .unwrap_or_else(|e| e.into_inner())
            .check_and_insert_epoch(hash, epoch)
    }

    /// Admission check WITHOUT inserting. See [`crate::statemachine::Dedup::check_epoch`].
    /// A hash of the wrong length reports `Duplicate` (drop it — it can never be valid).
    pub fn check_epoch(&self, hash: Vec<u8>, epoch: u32) -> FfiDedupVerdict {
        let hash: [u8; 16] = match hash.as_slice().try_into() {
            Ok(h) => h,
            Err(_) => return FfiDedupVerdict::Duplicate,
        };
        self.inner
            .lock()
            .unwrap_or_else(|e| e.into_inner())
            .check_epoch(hash, epoch)
            .into()
    }

    /// Insert a hash the caller has finished acting on. Returns false on a wrong-length
    /// hash, an already-present hash, or a full epoch bucket.
    pub fn insert_epoch(&self, hash: Vec<u8>, epoch: u32) -> bool {
        let hash: [u8; 16] = match hash.as_slice().try_into() {
            Ok(h) => h,
            Err(_) => return false,
        };
        self.inner
            .lock()
            .unwrap_or_else(|e| e.into_inner())
            .insert_epoch(hash, epoch)
    }
}

/// Wire form of [`crate::statemachine::DedupVerdict`] for the shim.
#[derive(uniffi::Enum, Debug, Clone, Copy, PartialEq, Eq)]
pub enum FfiDedupVerdict {
    Fresh,
    Duplicate,
    BucketFull,
}

impl From<crate::statemachine::DedupVerdict> for FfiDedupVerdict {
    fn from(v: crate::statemachine::DedupVerdict) -> Self {
        match v {
            crate::statemachine::DedupVerdict::Fresh => FfiDedupVerdict::Fresh,
            crate::statemachine::DedupVerdict::Duplicate => FfiDedupVerdict::Duplicate,
            crate::statemachine::DedupVerdict::BucketFull => FfiDedupVerdict::BucketFull,
        }
    }
}

// ---------------------------------------------------------------------------
// Trust accumulator (UniFFI) — multi-locale diversity for BroadcastCHAT (H2)
// ---------------------------------------------------------------------------

/// A diversity-tracking trust accumulator.
/// Counts how many distinct cell sketches have verified a given frame hash.
/// Gates BroadcastCHAT display on ≥ k distinct verified cells.
#[derive(uniffi::Object)]
pub struct FfiTrust {
    inner: std::sync::Mutex<crate::trust::TrustState>,
}

#[uniffi::export]
impl FfiTrust {
    /// Create a new `FfiTrust` with empty verification state.
    #[uniffi::constructor]
    pub fn new() -> std::sync::Arc<Self> {
        std::sync::Arc::new(FfiTrust {
            inner: std::sync::Mutex::new(crate::trust::TrustState::new()),
        })
    }

    /// Record that `frame_hash` was verified from the cell identified by `div_sketch`.
    /// Returns the new distinct-cell count for this frame hash.
    ///
    /// Anti-inflation (R2): claims that are fuzzy-equal (Jaccard ≥ `tau`) to an already
    /// recorded claim count as the SAME cell. Witness-less / empty claims never count.
    pub fn record_verification(&self, frame_hash: Vec<u8>, div_sketch: Vec<u8>, tau: f32) -> u32 {
        let fh: [u8; 16] = match frame_hash.as_slice().try_into() {
            Ok(h) => h,
            Err(_) => return 0,
        };
        let ds: [u8; 16] = match div_sketch.as_slice().try_into() {
            Ok(d) => d,
            Err(_) => return 0,
        };
        self.inner
            .lock()
            .unwrap_or_else(|e| e.into_inner())
            .record(fh, ds, tau)
    }

    /// Return the number of distinct cells that have verified `frame_hash`.
    /// 0 means the hash has never been verified (or inputs were wrong length).
    pub fn distinct_count(&self, frame_hash: Vec<u8>) -> u32 {
        let fh: [u8; 16] = match frame_hash.as_slice().try_into() {
            Ok(h) => h,
            Err(_) => return 0,
        };
        self.inner
            .lock()
            .unwrap_or_else(|e| e.into_inner())
            .distinct_count(&fh)
    }
}

/// The parse -> verify path a shim runs for every scanned frame: decode `bytes`, then verify its
/// signature against `pubkey` (32 B) over the canonical region. True iff structurally valid AND
/// the signature checks out. Never asserts anything about the sender beyond this one message.
#[uniffi::export]
pub fn verify_frame(bytes: Vec<u8>, pubkey: Vec<u8>) -> bool {
    let frame = match codec::decode(&bytes) {
        Ok(f) => f,
        Err(_) => return false,
    };
    let pk: [u8; 32] = match pubkey.as_slice().try_into() {
        Ok(p) => p,
        Err(_) => return false,
    };
    let buf: [u8; FRAME_LEN] = match bytes.as_slice().try_into() {
        Ok(b) => b,
        Err(_) => return false,
    };
    crypto::verify(&pk, codec::signing_region(&buf), &frame.sig)
}

/// Verify a frame's signature using its own embedded ephemeral pubkey.
/// True iff the frame decodes AND the embedded pubkey verifies the signature.
/// No separate pubkey needed — the pubkey is extracted from the frame itself.
#[uniffi::export]
pub fn frame_verify_self(bytes: Vec<u8>) -> bool {
    let buf: [u8; FRAME_LEN] = match bytes.as_slice().try_into() {
        Ok(b) => b,
        Err(_) => return false,
    };
    let frame = match codec::decode(&buf) {
        Ok(f) => f,
        Err(_) => return false,
    };
    crypto::verify(&frame.pk, codec::signing_region(&buf), &frame.sig)
}

// ---------------------------------------------------------------------------
// Measurement / debug surface — drives the RF-overlap τ rig (README.md §4).
// Still byte-only: the shim logs raw overheard (mark, rssi, epoch) and asks the core to
// compute sketches + Jaccard. τ and the RSSI floor stay caller-supplied so they can be TUNED
// from real field data, never hardcoded/guessed.
// ---------------------------------------------------------------------------

/// Extract the 16-byte `mark` of a valid frame (for per-epoch overheard-set logging), else `None`.
#[uniffi::export]
pub fn frame_mark(bytes: Vec<u8>) -> Option<Vec<u8>> {
    codec::decode(&bytes).ok().map(|f| f.mark.to_vec())
}

/// Extract the `epoch` field of a valid frame, else `None`.
#[uniffi::export]
pub fn frame_epoch(bytes: Vec<u8>) -> Option<u32> {
    codec::decode(&bytes).ok().map(|f| f.epoch)
}

/// Build this device's KMV cell sketch from the marks it overheard this epoch. `marks_flat` is the
/// concatenation of 16-byte marks (trailing partial mark ignored); `rssi[i]` is the dBm of mark `i`;
/// marks below `rssi_floor_dbm` are windowed out. Returns the 16-slot sketch as a `u64` list.
#[uniffi::export]
pub fn observe_marks(marks_flat: Vec<u8>, rssi: Vec<i8>, seed: u32, rssi_floor_dbm: i8) -> Vec<u64> {
    let marks: Vec<[u8; 16]> = marks_flat
        .chunks_exact(16)
        .map(|c| c.try_into().unwrap())
        .collect();
    pocp::observe(&marks, &rssi, seed, rssi_floor_dbm).0.to_vec()
}

/// Jaccard similarity in [0,1] of two sketches (each a 16-slot `u64` list from `observe_marks`).
/// Two co-located devices score high; a remote van scores low. Lengths != 16 return 0.0.
#[uniffi::export]
pub fn jaccard_sketch(a: Vec<u64>, b: Vec<u64>) -> f32 {
    let to_sketch = |v: &[u64]| -> Option<CellSketch> {
        if v.len() != KMV_K {
            return None;
        }
        Some(CellSketch(v.try_into().ok()?))
    };
    match (to_sketch(&a), to_sketch(&b)) {
        (Some(sa), Some(sb)) => pocp::jaccard(&sa, &sb),
        _ => 0.0,
    }
}

// ---------------------------------------------------------------------------
// PoCP witness surface (M5a) — spacetime witness MAC + verification
// ---------------------------------------------------------------------------

/// Truncate a 16-slot u64 cell sketch to a 16-byte `div_sketch` for the wire.
/// Takes the low byte of each u64 slot. Returns `None` if `sketch` is not 16 u64 values.
#[uniffi::export]
pub fn pocp_sketch_to_div_sketch(sketch: Vec<u64>) -> Option<Vec<u8>> {
    if sketch.len() != KMV_K {
        return None;
    }
    let arr: [u64; 16] = sketch.try_into().ok()?;
    let cell = CellSketch(arr);
    Some(pocp::sketch_to_div_sketch(&cell).to_vec())
}

/// Compute a PoCP witness MAC for a frame.
///
/// `div_sketch` is 16 bytes (from `pocp_sketch_to_div_sketch`). `seed` is the epoch index
/// (must match the frame's epoch field). `frame_prefix` is the first 102 bytes of the
/// unsigned frame (everything before the `pocp_wit` field).
///
/// Returns the 16-byte witness, or `None` if inputs are wrong length.
#[uniffi::export]
pub fn pocp_witness(div_sketch: Vec<u8>, seed: u32, frame_prefix: Vec<u8>) -> Option<Vec<u8>> {
    let div: [u8; 16] = div_sketch.as_slice().try_into().ok()?;
    Some(pocp::witness(&div, seed, &frame_prefix).to_vec())
}

/// Verify a PoCP witness AND check co-presence against the local cell sketch.
///
/// Returns a verdict code:
///   - 0: Valid — MAC valid + sketches overlap (Jaccard ≥ tau)
///   - 1: CellMismatch — MAC valid but sketches don't overlap
///   - 2: Stale — MAC invalid (bad witness or wrong sketch/seed)
///   - 255: Error — wrong input lengths
///
/// `local_sketch` is 16 u64 values from `observe_marks`. `claimed_div` is 16 bytes
/// from the frame's `div_sketch` field. `frame_prefix` is the first 102 bytes of the
/// frame. `wit` is 16 bytes from the frame's `pocp_wit` field.
#[uniffi::export]
pub fn pocp_verify_witness_local(
    local_sketch: Vec<u64>,
    claimed_div: Vec<u8>,
    seed: u32,
    frame_prefix: Vec<u8>,
    wit: Vec<u8>,
    tau: f32,
) -> u8 {
    if local_sketch.len() != KMV_K || claimed_div.len() != 16 || wit.len() != 16 {
        return 255;
    }
    let local_arr: [u64; 16] = match local_sketch.try_into() {
        Ok(a) => a,
        Err(_) => return 255,
    };
    let local = CellSketch(local_arr);
    let div: [u8; 16] = match claimed_div.as_slice().try_into() {
        Ok(d) => d,
        Err(_) => return 255,
    };
    let wit_arr: [u8; 16] = match wit.as_slice().try_into() {
        Ok(w) => w,
        Err(_) => return 255,
    };
    match pocp::verify_witness_local(&local, &div, seed, &frame_prefix, &wit_arr, tau) {
        pocp::WitVerdict::Valid => 0,
        pocp::WitVerdict::CellMismatch => 1,
        pocp::WitVerdict::Stale => 2,
    }
}

/// Witness parts extracted from a received frame for PoCP/VDL verification.
#[derive(uniffi::Record)]
pub struct WitnessParts {
    /// 16-byte div_sketch from the frame (claimed cell digest or counter).
    pub div_sketch: Vec<u8>,
    /// 16-byte PoCP witness MAC / VDL witness from the frame.
    pub pocp_wit: Vec<u8>,
    /// First 102 bytes of the frame (everything before the witness field).
    pub frame_prefix: Vec<u8>,
    /// Epoch field from the frame.
    pub epoch: u32,
    /// Message type byte (1=LocalImmediate, 2=RegionalPropagated, 3=Private).
    pub msg_type: u8,
    /// 16-byte blake3 hash of the frame body (bytes 38..102).
    /// Used as the trust diversity key — same alert text → same body_hash.
    pub body_hash: Vec<u8>,
}

/// Extract witness-related fields from a frame for PoCP/VDL verification.
/// Returns `None` if the frame does not decode.
#[uniffi::export]
pub fn frame_witness_parts(bytes: Vec<u8>) -> Option<WitnessParts> {
    let buf: [u8; FRAME_LEN] = bytes.as_slice().try_into().ok()?;
    let f = codec::decode(&buf).ok()?;
    let mut body_hash = [0u8; 16];
    body_hash.copy_from_slice(&blake3::hash(&f.body).as_bytes()[..16]);
    Some(WitnessParts {
        div_sketch: f.div_sketch.to_vec(),
        pocp_wit: f.pocp_wit.to_vec(),
        frame_prefix: buf[..102].to_vec(),
        epoch: f.epoch,
        msg_type: f.msg_type.to_u8(),
        body_hash: body_hash.to_vec(),
    })
}

// ---------------------------------------------------------------------------
// Pairing + private messaging surface
// ---------------------------------------------------------------------------

/// X25519 public key for the device's long-term pairing secret (32 OS-random bytes generated by
/// the app). Returns the 32-byte public key, or `None` if `sk` is not exactly 32 bytes.
#[uniffi::export]
pub fn pair_public(sk: Vec<u8>) -> Option<Vec<u8>> {
    let sk: &[u8; 32] = sk.as_slice().try_into().ok()?;
    Some(crypto::pair_public(sk).to_vec())
}

/// Derive the 32-byte pairwise message key from our secret key and their public key. Returns
/// `None` if either input is not exactly 32 bytes or if the contributory check fails (all-zero
/// output, i.e. the peer supplied a low-order point).
///
/// NOTE (A3): keys derived here are STATIC (no forward secrecy). New pairings must go through
/// `pair_seed_v2` + `pair_ratchet`.
#[uniffi::export]
pub fn pair_derive(our_sk: Vec<u8>, their_pk: Vec<u8>) -> Option<Vec<u8>> {
    let our_sk: &[u8; 32] = our_sk.as_slice().try_into().ok()?;
    let their_pk: &[u8; 32] = their_pk.as_slice().try_into().ok()?;
    Some(crypto::pair_derive(our_sk, their_pk)?.to_vec())
}

/// v2 pairing chain seed (A3 forward secrecy). `shared` is the `pair_derive` output;
/// `salt_a`/`salt_b` are the two sides' per-pairing salts (order-independent — the core
/// sorts them). Both sides delete their salts after pairing; the chain seed is then
/// unrecoverable from the seized long-term secret alone. Returns `None` on wrong lengths.
#[uniffi::export]
pub fn pair_seed_v2(shared: Vec<u8>, salt_a: Vec<u8>, salt_b: Vec<u8>) -> Option<Vec<u8>> {
    let shared: &[u8; 32] = shared.as_slice().try_into().ok()?;
    let salt_a: &[u8; 32] = salt_a.as_slice().try_into().ok()?;
    let salt_b: &[u8; 32] = salt_b.as_slice().try_into().ok()?;
    Some(crypto::pair_seed_v2(shared, salt_a, salt_b).to_vec())
}

/// Advance a pair-chain key from `from_epoch` to `to_epoch`, one one-way BLAKE3 step per
/// epoch (A3 forward secrecy — past keys unrecoverable). Returns `None` on wrong key length,
/// `to_epoch < from_epoch`, or a span over 8192 steps (DoS bound on wire-controlled epochs).
#[uniffi::export]
pub fn pair_ratchet(key: Vec<u8>, from_epoch: u32, to_epoch: u32) -> Option<Vec<u8>> {
    let key: &[u8; 32] = key.as_slice().try_into().ok()?;
    Some(crypto::pair_ratchet(key, from_epoch, to_epoch)?.to_vec())
}

/// Build an encrypted private frame. `seed` is 32 bytes; `pair_key` is the 32-byte pairwise key
/// from `pair_derive`. Returns the 226-byte wire frame, or `None` if `seed` or `pair_key` are not
/// 32 bytes, or `text` exceeds 47 UTF-8 bytes.
///
/// `counter` is a monotonic per-device u64 that prevents AEAD nonce reuse under the same
/// (seed, epoch) tuple. The shim MUST persist and increment this value across private sends
/// and service restarts.
///
/// WARNING: this call performs a blocking VDL proof-of-work solve that may take several seconds of
/// CPU time. Always call off the UI thread.
/// Build an encrypted private frame. `seed` is 32 bytes; `beacon_seed` is 32 bytes from the
/// beacon chain; `pair_key` is the 32-byte pairwise key from `pair_derive`. Returns the 226-byte
/// wire frame, or `None` if inputs are wrong length or `text` exceeds 47 UTF-8 bytes.
///
/// WARNING: this call performs a blocking VDL proof-of-work solve that may take several seconds of
/// CPU time. Always call off the UI thread.
#[uniffi::export]
pub fn make_private_frame(
    seed: Vec<u8>,
    epoch: u32,
    beacon_seed: Vec<u8>,
    pair_key: Vec<u8>,
    text: String,
    counter: u64,
) -> Option<Vec<u8>> {
    let seed: &[u8; 32] = seed.as_slice().try_into().ok()?;
    let bs: &[u8; 32] = beacon_seed.as_slice().try_into().ok()?;
    let pair_key: &[u8; 32] = pair_key.as_slice().try_into().ok()?;
    Some(message::make_private_frame(seed, epoch, bs, pair_key, &text, vdl::VDL_DIFFICULTY_BITS, counter)?.to_vec())
}

/// Decrypt and verify a private frame using the 32-byte pairwise key. Returns the plaintext, or
/// `None` if `frame` is not 226 bytes, `pair_key` is not 32 bytes, the frame is not a private
/// message type, the VDL witness fails, or the key is wrong.
#[uniffi::export]
pub fn open_private_frame(frame: Vec<u8>, pair_key: Vec<u8>) -> Option<String> {
    let pair_key: &[u8; 32] = pair_key.as_slice().try_into().ok()?;
    message::open_private_frame(&frame, pair_key, vdl::VDL_DIFFICULTY_BITS)
}

/// Verify ONLY the VDL proof-of-work witness of a private frame (B5). True iff the frame
/// decodes, is `MsgType::Private`, and the witness meets the production difficulty.
/// The shim calls this ONCE per received private frame before the per-contact trial-decrypt
/// loop, instead of paying for it inside every `open_private_frame` attempt.
#[uniffi::export]
pub fn vdl_check_frame(bytes: Vec<u8>) -> bool {
    let buf: [u8; FRAME_LEN] = match bytes.as_slice().try_into() {
        Ok(b) => b,
        Err(_) => return false,
    };
    let f = match codec::decode(&buf) {
        Ok(f) => f,
        Err(_) => return false,
    };
    if f.msg_type != MsgType::Private {
        return false;
    }
    vdl::verify(&buf[..102], &f.pocp_wit, vdl::VDL_DIFFICULTY_BITS)
}

/// Trial-decrypt a private frame body against one pair key WITHOUT re-verifying the
/// signature or VDL witness (B5). CALLER CONTRACT: the frame already passed
/// `frame_verify_self` AND `vdl_check_frame` exactly once upstream. Returns the plaintext,
/// or `None` on wrong key, wrong type, or malformed body — indistinguishable by design.
#[uniffi::export]
pub fn open_private_body_only(frame: Vec<u8>, pair_key: Vec<u8>) -> Option<String> {
    let pair_key: &[u8; 32] = pair_key.as_slice().try_into().ok()?;
    message::open_private_body_only(&frame, pair_key)
}

/// The VDL difficulty in bits used for private frames. Exposed for display in the debug UI.
#[uniffi::export]
pub fn vdl_difficulty_bits() -> u8 {
    vdl::VDL_DIFFICULTY_BITS
}

// ---------------------------------------------------------------------------
// M5b: Beacon chain (forward-secrecy hash chain) — UniFFI
// ---------------------------------------------------------------------------

/// A forward-secrecy beacon chain.
///
/// Each epoch, LocalImmediate marks observed this epoch are hashed into an entropy block.
/// The chain advances: `seed_N = BLAKE3(seed_{N-1} || entropy)`. The one-way hash chain
/// ensures past seeds are unrecoverable from the current seed — providing forward secrecy
/// for marks and ephemeral signing keys even if the device is later seized.
#[derive(uniffi::Object)]
pub struct BeaconFfi {
    inner: std::sync::Mutex<beacon::Beacon>,
}

#[uniffi::export]
impl BeaconFfi {
    /// Create a new beacon chain from OS-random bytes (seed0). Epoch 0.
    ///
    /// Any input length is accepted: the bytes are BLAKE3-hashed to the 32-byte seed,
    /// so a shim passing a wrong-length buffer degrades to a different chain instead of
    /// aborting the whole process across the FFI boundary (R6).
    #[uniffi::constructor]
    pub fn new(seed0: Vec<u8>) -> std::sync::Arc<Self> {
        let seed: [u8; 32] = *blake3::hash(&seed0).as_bytes();
        std::sync::Arc::new(Self {
            inner: std::sync::Mutex::new(beacon::new(&seed)),
        })
    }

    /// Attempt to advance the beacon chain using entropy from LocalImmediate marks.
    /// `entropy_bytes` is 32 bytes from `beacon_entropy()`.
    /// Returns true if the chain advanced, false if within the acceleration floor.
    pub fn advance(&self, entropy_bytes: Vec<u8>, now_ms: u64, floor_ms: u64) -> bool {
        let e_bytes: [u8; 32] = match entropy_bytes.as_slice().try_into() {
            Ok(b) => b,
            Err(_) => return false,
        };
        let ent = beacon::Entropy(e_bytes);
        let mut inner = self.inner.lock().unwrap_or_else(|e| e.into_inner());
        match beacon::advance(&inner, &ent, now_ms, floor_ms) {
            Some(next) => {
                *inner = next;
                true
            }
            None => false,
        }
    }

    /// Fallback advance: chain with zero external entropy.
    /// Returns true if the chain advanced, false if within the floor.
    pub fn advance_fallback(&self, now_ms: u64, floor_ms: u64) -> bool {
        let mut inner = self.inner.lock().unwrap_or_else(|e| e.into_inner());
        match beacon::fallback_local(&inner, now_ms, floor_ms) {
            Some(next) => {
                *inner = next;
                true
            }
            None => false,
        }
    }

    /// Current 32-byte beacon seed. Feed this into `make_message_frame` et al.
    pub fn seed(&self) -> Vec<u8> {
        self.inner.lock().unwrap_or_else(|e| e.into_inner()).seed.to_vec()
    }

    /// Whether the beacon is in low-entropy mode (no neighbors heard).
    pub fn is_low_entropy(&self) -> bool {
        self.inner.lock().unwrap_or_else(|e| e.into_inner()).low_entropy
    }

    /// Current beacon epoch number. Not used for frame epoch (wall clock handles that).
    pub fn epoch(&self) -> u32 {
        self.inner.lock().unwrap_or_else(|e| e.into_inner()).epoch
    }

    /// Zero the live beacon seed and reset the chain (C7 panic-wipe gap). Previously only a
    /// flag was set — the current seed stayed in Rust memory, recoverable until process exit.
    /// After this call the object is sterile: `seed()` returns zeros and further `advance`
    /// calls build a chain unrelated to anything broadcast before the wipe.
    pub fn wipe(&self) {
        let mut inner = self.inner.lock().unwrap_or_else(|e| e.into_inner());
        inner.seed = [0u8; 32];
        inner.epoch = 0;
        inner.last_advance_ms = 0;
        inner.low_entropy = true;
    }
}

/// Compute beacon entropy from a list of 16-byte LocalImmediate marks.
/// `marks_flat` is the concatenation of 16-byte mark bytes.
/// Returns 32-byte entropy block, or `None` if fewer than `min_hearers` unique marks.
#[uniffi::export]
pub fn beacon_entropy(marks_flat: Vec<u8>, min_hearers: u32) -> Option<Vec<u8>> {
    let marks: Vec<[u8; 16]> = marks_flat
        .chunks_exact(16)
        .map(|c| c.try_into().unwrap())
        .collect();
    beacon::local_entropy(&marks, min_hearers).map(|e| e.0.to_vec())
}

// ---------------------------------------------------------------------------
// Panic wipe (B1) — emergency data destruction
// ---------------------------------------------------------------------------

/// Immediately flag a panic-wipe. The platform shim MUST, after calling this:
///   1. Clear all persisted key material (PairStore.wipe)
///   2. Clear configuration (ConfigStore)
///   3. Stop the BLE service (MeshService.stopForeground + stopSelf)
///   4. Optionally terminate the process
///
/// This sets an internal flag that `was_panic_wiped()` returns once.
#[uniffi::export]
pub fn panic_wipe() {
    PANIC_WIPED.store(true, Ordering::SeqCst);
}

/// Returns `true` once after a `panic_wipe()` call, then resets to `false`.
/// The platform shim calls this from the service loop to detect a wipe request.
#[uniffi::export]
pub fn was_panic_wiped() -> bool {
    PANIC_WIPED.swap(false, Ordering::SeqCst)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn pair_public_wrong_length_returns_none() {
        assert!(pair_public(vec![0u8; 31]).is_none());
        assert!(pair_public(vec![0u8; 33]).is_none());
        assert!(pair_public(vec![]).is_none());
    }

    #[test]
    fn pair_derive_wrong_length_returns_none() {
        let good = vec![0u8; 32];
        assert!(pair_derive(vec![0u8; 31], good.clone()).is_none());
        assert!(pair_derive(good.clone(), vec![0u8; 33]).is_none());
    }

    #[test]
    fn pair_public_derive_roundtrip() {
        // Two random-ish (but deterministic) seeds.
        let sk_a: Vec<u8> = (1u8..=32).collect();
        let sk_b: Vec<u8> = (33u8..=64).collect();

        let pk_a = pair_public(sk_a.clone()).expect("pk_a");
        let pk_b = pair_public(sk_b.clone()).expect("pk_b");

        assert_eq!(pk_a.len(), 32);
        assert_eq!(pk_b.len(), 32);

        let shared_ab = pair_derive(sk_a.clone(), pk_b.clone()).expect("shared_ab");
        let shared_ba = pair_derive(sk_b.clone(), pk_a.clone()).expect("shared_ba");

        assert_eq!(shared_ab, shared_ba, "ECDH shared secret must be equal both ways");
        assert_eq!(shared_ab.len(), 32);
    }
}

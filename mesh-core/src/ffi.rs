//! Narrow, byte-oriented FFI surface exposed to the platform shims via UniFFI.
//!
//! INVARIANT #1: the shims pass RAW bytes only. Every parse/verify/decide step lives in the
//! core; nothing here hands a shim a half-parsed structure it could act on. The surface is
//! intentionally tiny — it grows only as the state machine (M4+) needs to be driven.

use crate::codec::{self, FRAME_LEN, Frame, MsgType, PROTO_VERSION};
use crate::crypto;
use crate::pocp::{self, CellSketch, KMV_K};

/// Fixed wire frame size in bytes (194). Lets a shim size its radio buffers correctly.
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
/// boundary from Kotlin/Swift. Returns the 194 B wire frame, or `None` if `seed` is not 32 B.
/// Smoke-test helper only — real origination goes through the state machine.
#[uniffi::export]
pub fn make_test_frame(seed: Vec<u8>, epoch: u32) -> Option<Vec<u8>> {
    let seed: [u8; 32] = seed.as_slice().try_into().ok()?;
    let e = crypto::from_seed(&seed, epoch);
    // Device-unique, epoch-rotating mark so co-located devices overhear DISTINCT neighbour marks
    // (a shared mark would make every cell look identical and defeat the RF-overlap τ rig).
    let mut mark = [0u8; 16];
    let mut h = blake3::Hasher::new();
    h.update(b"mesh-core:v1:mark");
    h.update(&seed);
    h.update(&epoch.to_le_bytes());
    mark.copy_from_slice(&h.finalize().as_bytes()[..16]);
    let mut f = Frame {
        mark,
        version: PROTO_VERSION,
        msg_type: MsgType::RegionalPropagated,
        div_sketch: [0u8; 16],
        epoch,
        body: [0u8; 64],
        pocp_wit: [0u8; 16],
        sig: [0u8; 64],
        reserved: [0u8; 12],
    };
    // The signature covers only [0..118); sig/reserved bytes are outside it, so signing the
    // not-yet-signed encoding yields the same region we verify against.
    let unsigned = codec::encode(&f);
    f.sig = crypto::sign(&e, codec::signing_region(&unsigned));
    Some(codec::encode(&f).to_vec())
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

// ---------------------------------------------------------------------------
// Measurement / debug surface — drives the RF-overlap τ rig (mesh-build-plan.md §5).
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


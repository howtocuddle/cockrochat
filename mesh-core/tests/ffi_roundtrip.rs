//! Locks the byte-only shim contract (mesh-core/src/ffi.rs): a frame minted by `make_test_frame`
//! must decode and verify under the matching public key, and tamper must be rejected. This is the
//! exact parse -> verify path Kotlin/Swift drive over UniFFI.

use mesh_core::crypto;
use mesh_core::ffi::{frame_decodes, frame_len, make_test_frame, verify_frame};

fn seed() -> Vec<u8> {
    (0u8..32).collect()
}

fn beacon_seed() -> Vec<u8> {
    (100u8..132).collect()
}

#[test]
fn frame_len_is_226() {
    assert_eq!(frame_len(), 226);
}

#[test]
fn mint_then_verify_roundtrips() {
    let bs_arr: [u8; 32] = beacon_seed().try_into().unwrap();
    let e = crypto::from_seed(&seed().try_into().unwrap(), &bs_arr);
    let pk = crypto::public_key(&e).to_vec();

    let frame = make_test_frame(seed(), 100, beacon_seed()).expect("32-byte seed");
    assert_eq!(frame.len(), 226);
    assert!(frame_decodes(frame.clone()));
    assert!(verify_frame(frame, pk));
}

#[test]
fn wrong_key_rejected() {
    let bs_arr: [u8; 32] = beacon_seed().try_into().unwrap();
    let frame = make_test_frame(seed(), 100, beacon_seed()).unwrap();
    let wrong_pk = crypto::public_key(&crypto::from_seed(&[9u8; 32], &bs_arr)).to_vec();
    assert!(!verify_frame(frame, wrong_pk));
}

#[test]
fn tampered_body_rejected() {
    let bs_arr: [u8; 32] = beacon_seed().try_into().unwrap();
    let e = crypto::from_seed(&seed().try_into().unwrap(), &bs_arr);
    let pk = crypto::public_key(&e).to_vec();
    let mut frame = make_test_frame(seed(), 100, beacon_seed()).unwrap();
    frame[40] ^= 0xff; // flip a body byte (inside the signed region)
    assert!(!verify_frame(frame, pk));
}

#[test]
fn bad_seed_len_returns_none() {
    let bs = beacon_seed();
    assert!(make_test_frame(vec![0u8; 31], 1, bs.clone()).is_none());
    assert!(make_test_frame(seed(), 1, vec![0u8; 31]).is_none());
}

#[test]
fn junk_does_not_decode() {
    assert!(!frame_decodes(vec![0u8; 225])); // one shy of 226
    assert!(!frame_decodes(vec![])); // empty
}

// ---------------------------------------------------------------------------
// Pairing salt length contract (v2 QR pairing)
// ---------------------------------------------------------------------------
//
// `pair_seed_v2` takes its salts as `[u8; 32]` and returns None on any other length. The
// Android shim generated 16-byte salts, so pair_seed_v2 returned None for EVERY v2 QR scan
// and the user saw "Derivation failed" — the only pairings that worked were raw-hex/v1
// payloads, which silently fall back to the static non-forward-secret key.
//
// The existing crypto unit test passed `[u8; 32]` arrays directly, so the type system hid
// the mismatch. These tests exercise the FFI's Vec<u8> boundary, which is what the shim
// actually calls, and pin both the accepted and the rejected lengths.

#[test]
fn pair_seed_v2_requires_32_byte_salts() {
    let shared = vec![7u8; 32];
    let salt_a = vec![1u8; 32];
    let salt_b = vec![2u8; 32];
    assert!(
        mesh_core::ffi::pair_seed_v2(shared, salt_a, salt_b).is_some(),
        "32-byte salts must derive a chain seed"
    );
}

#[test]
fn pair_seed_v2_rejects_16_byte_salts() {
    let shared = vec![7u8; 32];
    // Exactly the shape the shim used to send.
    assert!(
        mesh_core::ffi::pair_seed_v2(shared.clone(), vec![1u8; 16], vec![2u8; 16]).is_none(),
        "16-byte salts must be rejected — this was the broken v2 pairing path"
    );
    // A single wrong-length side is enough to fail.
    assert!(
        mesh_core::ffi::pair_seed_v2(shared, vec![1u8; 32], vec![2u8; 16]).is_none()
    );
}

#[test]
fn pair_seed_v2_is_order_independent() {
    // Both sides must derive the SAME chain seed regardless of who scanned first.
    let shared = vec![9u8; 32];
    let a = vec![0xAAu8; 32];
    let b = vec![0xBBu8; 32];
    let ab = mesh_core::ffi::pair_seed_v2(shared.clone(), a.clone(), b.clone()).unwrap();
    let ba = mesh_core::ffi::pair_seed_v2(shared, b, a).unwrap();
    assert_eq!(ab, ba, "pairing must not depend on scan order");
}

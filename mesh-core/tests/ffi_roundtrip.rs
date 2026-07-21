//! Locks the byte-only shim contract (mesh-core/src/ffi.rs): a frame minted by `make_test_frame`
//! must decode and verify under the matching public key, and tamper must be rejected. This is the
//! exact parse -> verify path Kotlin/Swift drive over UniFFI.

use mesh_core::crypto;
use mesh_core::ffi::{frame_decodes, frame_len, make_test_frame, verify_frame};

fn seed() -> Vec<u8> {
    (0u8..32).collect()
}

#[test]
fn frame_len_is_194() {
    assert_eq!(frame_len(), 194);
}

#[test]
fn mint_then_verify_roundtrips() {
    let e = crypto::from_seed(&seed().try_into().unwrap(), 100);
    let pk = crypto::public_key(&e).to_vec();

    let frame = make_test_frame(seed(), 100).expect("32-byte seed");
    assert_eq!(frame.len(), 194);
    assert!(frame_decodes(frame.clone()));
    assert!(verify_frame(frame, pk));
}

#[test]
fn wrong_key_rejected() {
    let frame = make_test_frame(seed(), 100).unwrap();
    let wrong_pk = crypto::public_key(&crypto::from_seed(&[9u8; 32], 100)).to_vec();
    assert!(!verify_frame(frame, wrong_pk));
}

#[test]
fn tampered_body_rejected() {
    let e = crypto::from_seed(&seed().try_into().unwrap(), 100);
    let pk = crypto::public_key(&e).to_vec();
    let mut frame = make_test_frame(seed(), 100).unwrap();
    frame[40] ^= 0xff; // flip a body byte (inside the signed region)
    assert!(!verify_frame(frame, pk));
}

#[test]
fn bad_seed_len_returns_none() {
    assert!(make_test_frame(vec![0u8; 31], 1).is_none());
}

#[test]
fn junk_does_not_decode() {
    assert!(!frame_decodes(vec![0u8; 193]));
    assert!(!frame_decodes(vec![])); // empty
}

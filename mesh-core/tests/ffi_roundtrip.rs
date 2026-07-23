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

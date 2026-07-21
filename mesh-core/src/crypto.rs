//! `crypto` — ephemeral Ed25519 identity (hourly rotation) + AEAD.
//!
//! Constant-time verify. Domain-separated signatures. Keys persisted only via the platform
//! `KeyStore` trait (Keystore/Keychain) — never written to core-owned storage (invariant #5).
//! See README.md §2.

use chacha20poly1305::{ChaCha20Poly1305, KeyInit, Nonce, aead::Aead};
use ed25519_dalek::{Signature, Signer, SigningKey, VerifyingKey};

pub const DOMAIN_SIG: &[u8] = b"mesh-core:v1:frame-sig";

/// A rotating ephemeral signing identity. Rotates hourly (`rotate(now_epoch)`), never persisted
/// beyond the platform secure store, wiped on panic-wipe.
pub struct Ephemeral {
    key: SigningKey,
    epoch: u32,
}

/// Secure key storage owned by the platform shim (Android Keystore / iOS Keychain).
pub trait KeyStore {
    fn load_seed(&self) -> Option<[u8; 32]>;
    fn store_seed(&self, seed: &[u8; 32]);
    fn wipe(&self);
}

pub fn from_seed(seed: &[u8; 32], epoch: u32) -> Ephemeral {
    Ephemeral {
        key: SigningKey::from_bytes(seed),
        epoch,
    }
}

pub fn rotate(now_epoch: u32) -> Ephemeral {
    let mut seed = [0u8; 32];
    getrandom::fill(&mut seed).expect("OS CSPRNG unavailable");
    from_seed(&seed, now_epoch)
}

pub fn public_key(e: &Ephemeral) -> [u8; 32] {
    e.key.verifying_key().to_bytes()
}

pub fn epoch(e: &Ephemeral) -> u32 {
    e.epoch
}

pub fn sign(e: &Ephemeral, canonical: &[u8]) -> [u8; 64] {
    let msg = [DOMAIN_SIG, canonical].concat();
    e.key.sign(&msg).to_bytes()
}

pub fn verify(pk: &[u8; 32], canonical: &[u8], sig: &[u8; 64]) -> bool {
    let vk = match VerifyingKey::from_bytes(pk) {
        Ok(v) => v,
        Err(_) => return false,
    };
    let s = Signature::from_bytes(sig);
    let msg = [DOMAIN_SIG, canonical].concat();
    vk.verify_strict(&msg, &s).is_ok()
}

pub fn aead_seal(key: &[u8; 32], nonce: &[u8; 12], pt: &[u8]) -> Vec<u8> {
    ChaCha20Poly1305::new_from_slice(key)
        .expect("32-byte key")
        .encrypt(&Nonce::from(*nonce), pt)
        .expect("aead encrypt")
}

pub fn aead_open(key: &[u8; 32], nonce: &[u8; 12], ct: &[u8]) -> Option<Vec<u8>> {
    ChaCha20Poly1305::new_from_slice(key)
        .ok()?
        .decrypt(&Nonce::from(*nonce), ct)
        .ok()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn ed25519_kat() {
        let seed: [u8; 32] =
            hex::decode("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f")
                .unwrap()
                .try_into()
                .unwrap();

        let canon = hex::decode(
            "01080f161d242b323940474e555c636a71787f868d949ba2a9b0b7bec5ccd3dae1e8eff6fd040b121920272e353c434a51585f666d747b828990979ea5acb3bac1c8cfd6dde4ebf2f900070e151c232a31383f464d545b626970777e858c939aa1a8afb6bdc4cbd2d9e0e7eef5fc030a11181f262d34",
        )
        .unwrap();

        let expected_pk: [u8; 32] =
            hex::decode("03a107bff3ce10be1d70dd18e74bc09967e4d6309ba50d5f1ddc8664125531b8")
                .unwrap()
                .try_into()
                .unwrap();

        let expected_sig: [u8; 64] = hex::decode(
            "ce5a9940648f8aa6b4c4a07df6e7c4e70289810070356b80327dceec8bdc5f354e06dacc600ddebc736f456c339d75cfe6f8c99a527c2149fe9b9dff628c1309",
        )
        .unwrap()
        .try_into()
        .unwrap();

        let e = from_seed(&seed, 7);
        assert_eq!(public_key(&e), expected_pk);

        let sig = sign(&e, &canon);
        assert_eq!(sig, expected_sig);

        assert!(verify(&expected_pk, &canon, &expected_sig));
    }

    #[test]
    fn ed25519_rejects_tamper() {
        let seed: [u8; 32] =
            hex::decode("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f")
                .unwrap()
                .try_into()
                .unwrap();

        let canon = hex::decode(
            "01080f161d242b323940474e555c636a71787f868d949ba2a9b0b7bec5ccd3dae1e8eff6fd040b121920272e353c434a51585f666d747b828990979ea5acb3bac1c8cfd6dde4ebf2f900070e151c232a31383f464d545b626970777e858c939aa1a8afb6bdc4cbd2d9e0e7eef5fc030a11181f262d34",
        )
        .unwrap();

        let pk: [u8; 32] =
            hex::decode("03a107bff3ce10be1d70dd18e74bc09967e4d6309ba50d5f1ddc8664125531b8")
                .unwrap()
                .try_into()
                .unwrap();

        let sig: [u8; 64] = hex::decode(
            "ce5a9940648f8aa6b4c4a07df6e7c4e70289810070356b80327dceec8bdc5f354e06dacc600ddebc736f456c339d75cfe6f8c99a527c2149fe9b9dff628c1309",
        )
        .unwrap()
        .try_into()
        .unwrap();

        let e = from_seed(&seed, 7);
        let _ = e;

        // flip sig[0]
        let mut bad_sig = sig;
        bad_sig[0] ^= 0xff;
        assert!(!verify(&pk, &canon, &bad_sig));

        // flip canon byte
        let mut bad_canon = canon.clone();
        bad_canon[0] ^= 0xff;
        assert!(!verify(&pk, &bad_canon, &sig));

        // flip pubkey[0]
        let mut bad_pk = pk;
        bad_pk[0] ^= 0xff;
        assert!(!verify(&bad_pk, &canon, &sig));
    }

    #[test]
    fn rotate_is_random() {
        let e1 = rotate(0);
        let e2 = rotate(0);
        assert_ne!(public_key(&e1), public_key(&e2));
        assert_eq!(epoch(&rotate(42)), 42);
    }

    #[test]
    fn aead_kat() {
        let key: [u8; 32] =
            hex::decode("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f")
                .unwrap()
                .try_into()
                .unwrap();

        let nonce: [u8; 12] = hex::decode("000102030405060708090a0b")
            .unwrap()
            .try_into()
            .unwrap();

        let pt =
            hex::decode("726567696f6e616c2d70726f706167617465642064616e67657220616c657274207630")
                .unwrap();

        let expected_ct = hex::decode(
            "fb9e6f694679c42c9af34d9ce87c6902bd15d6c73515c3de83e50fa419a5d448ccf61cef3e01959294d46410cd721f19611f5e",
        )
        .unwrap();

        let ct = aead_seal(&key, &nonce, &pt);
        assert_eq!(ct, expected_ct);

        let decrypted = aead_open(&key, &nonce, &ct);
        assert_eq!(decrypted, Some(pt));
    }

    #[test]
    fn aead_rejects_tamper() {
        let key: [u8; 32] =
            hex::decode("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f")
                .unwrap()
                .try_into()
                .unwrap();

        let nonce: [u8; 12] = hex::decode("000102030405060708090a0b")
            .unwrap()
            .try_into()
            .unwrap();

        let mut ct = hex::decode(
            "fb9e6f694679c42c9af34d9ce87c6902bd15d6c73515c3de83e50fa419a5d448ccf61cef3e01959294d46410cd721f19611f5e",
        )
        .unwrap();

        ct[0] ^= 0xff;
        assert_eq!(aead_open(&key, &nonce, &ct), None);
    }
}

//! Tier-3 private message body sealing.
//!
//! The 64-byte frame body of a `MsgType::Private` frame is a ChaCha20-Poly1305
//! ciphertext: 48 bytes of plaintext block + 16 bytes of tag. The plaintext block
//! is `[len][utf-8 text, len <= 47][zero padding]`.
//!
//! Nonce = epoch (4 bytes big-endian, matching the wire encoding)
//!       || BLAKE3("mesh-core:v1:nonce" || sender_pk || counter_be_u64)[..8].
//!
//! The 8-byte nonce suffix is derived from the sender's FULL 32-byte ephemeral public
//! key (embedded in the frame) and a monotonic counter, so the two paired devices cannot
//! collide in nonce space even when their `pk[..4]` prefixes happen to match (R4: the old
//! 32-bit partition made keystream reuse a ~2^-32-per-epoch-pair catastrophic event).
//! The wire-visible counter is the low 32 bits at `div_sketch[4..8]`; wrapping it takes
//! 2^32 private sends within a single epoch — outside the threat model.
//!
//! The counter is device-global (not per-contact) and persists across service restarts
//! so it never wraps back to a previous value.
//!
//! There is no recipient address on the wire: the receiver trial-decrypts against
//! each stored pair key. A successful tag check both selects the conversation and
//! authenticates the sender (only the two paired devices hold the key).

use crate::crypto;

/// Max UTF-8 bytes of text in a private body (48-byte block minus 1 length byte).
pub const PRIVATE_TEXT_MAX: usize = 47;

/// Plaintext block length; block + 16-byte Poly1305 tag fills the 64-byte body exactly.
const PT_BLOCK: usize = 48;

/// 8-byte AEAD nonce suffix: domain-separated hash of the FULL sender ephemeral pubkey
/// and the monotonic counter. Sender separation is 256-bit (R4).
fn nonce_suffix(sender_pk: &[u8; 32], counter: u64) -> [u8; 8] {
    let mut h = blake3::Hasher::new();
    h.update(b"mesh-core:v1:nonce");
    h.update(sender_pk);
    h.update(&counter.to_be_bytes());
    let mut out = [0u8; 8];
    out.copy_from_slice(&h.finalize().as_bytes()[..8]);
    out
}

/// Build the 12-byte AEAD nonce from epoch, sender pubkey, and counter.
fn nonce_for(epoch: u32, sender_pk: &[u8; 32], counter: u64) -> [u8; 12] {
    let mut n = [0u8; 12];
    n[..4].copy_from_slice(&epoch.to_be_bytes());
    n[4..].copy_from_slice(&nonce_suffix(sender_pk, counter));
    n
}

/// Extract the wire-visible counter (low 32 bits) from a frame's div_sketch field.
fn counter_from_div_sketch(div_sketch: &[u8; 16]) -> u64 {
    u32::from_be_bytes([div_sketch[4], div_sketch[5], div_sketch[6], div_sketch[7]]) as u64
}

/// Seal `text` into a 64-byte private body. None if the text exceeds
/// [`PRIVATE_TEXT_MAX`] UTF-8 bytes.
///
/// `sender_pk` is the sender's ephemeral frame pubkey; `counter` is the monotonic
/// device-global counter (persisted by the shim). Together they form the AEAD nonce
/// suffix, preventing nonce reuse under a given pair key within the same epoch.
pub fn seal_private_body(
    pair_key: &[u8; 32],
    epoch: u32,
    sender_pk: &[u8; 32],
    counter: u64,
    text: &str,
) -> Option<[u8; 64]> {
    let bytes = text.as_bytes();
    if bytes.len() > PRIVATE_TEXT_MAX {
        return None;
    }
    let mut pt = [0u8; PT_BLOCK];
    pt[0] = bytes.len() as u8;
    pt[1..1 + bytes.len()].copy_from_slice(bytes);
    let ct = crypto::aead_seal(pair_key, &nonce_for(epoch, sender_pk, counter), &pt);
    let mut body = [0u8; 64];
    body.copy_from_slice(&ct);
    Some(body)
}

/// Open a 64-byte private body. None on tag failure (wrong key or tampering),
/// bad length byte, non-zero padding, or invalid UTF-8.
///
/// `sender_pk` is the frame's embedded ephemeral pubkey; the counter is read from
/// `div_sketch[4..8]`. Both recombine into the nonce used at seal time.
pub fn open_private_body(
    pair_key: &[u8; 32],
    epoch: u32,
    sender_pk: &[u8; 32],
    div_sketch: &[u8; 16],
    body: &[u8; 64],
) -> Option<String> {
    let counter = counter_from_div_sketch(div_sketch);
    let pt = crypto::aead_open(pair_key, &nonce_for(epoch, sender_pk, counter), body)?;
    if pt.len() != PT_BLOCK {
        return None;
    }
    let len = pt[0] as usize;
    if len > PRIVATE_TEXT_MAX {
        return None;
    }
    if pt[1 + len..].iter().any(|&b| b != 0) {
        return None;
    }
    core::str::from_utf8(&pt[1..1 + len]).ok().map(String::from)
}

#[cfg(test)]
mod tests {
    use super::*;

    fn decode_hex64(s: &str) -> [u8; 64] {
        assert_eq!(s.len(), 128, "hex string must be 128 chars for 64 bytes");
        let mut out = [0u8; 64];
        for i in 0..64 {
            out[i] = u8::from_str_radix(&s[i * 2..i * 2 + 2], 16)
                .expect("valid hex digit pair");
        }
        out
    }

    fn test_pair_key() -> [u8; 32] {
        core::array::from_fn(|i| (i + 64) as u8)
    }

    fn test_epoch() -> u32 {
        0x00010203
    }

    fn test_pk() -> [u8; 32] {
        core::array::from_fn(|i| (i + 200) as u8)
    }

    fn test_counter() -> u64 {
        7
    }

    /// Wire layout matching make_private_frame: pk[..4] || counter_low32_be || zeros.
    fn ds_for(pk: &[u8; 32], counter: u64) -> [u8; 16] {
        let mut ds = [0u8; 16];
        ds[..4].copy_from_slice(&pk[..4]);
        ds[4..8].copy_from_slice(&(counter as u32).to_be_bytes());
        ds
    }

    #[test]
    fn seal_kat_matches_independent_vector() {
        // KAT for nonce = epoch_be || blake3("mesh-core:v1:nonce" || pk || 7_be_u64)[..8].
        let expected = decode_hex64(
            "efc2f656161c7727c653c7f435cd8db7902f26491fae6e5105ab6293985746b20b837a04249ad9a09d565c265303de9fa4b418c7bbad1d332cfab4c8ebd54648",
        );

        let key = test_pair_key();
        let pk = test_pk();
        let ds = ds_for(&pk, test_counter());
        let body = seal_private_body(&key, test_epoch(), &pk, test_counter(), "hello")
            .expect("short text");

        assert_eq!(body, expected, "seal output must match KAT vector");
        assert_eq!(
            open_private_body(&key, test_epoch(), &pk, &ds, &body),
            Some("hello".to_string()),
            "KAT body must open to 'hello'"
        );
    }

    #[test]
    fn roundtrip_max_len() {
        let key = test_pair_key();
        let pk = test_pk();
        let ds = ds_for(&pk, 1);

        let text_47 = "a".repeat(47);
        let body = seal_private_body(&key, 1, &pk, 1, &text_47).expect("47 bytes must seal");
        assert_eq!(
            open_private_body(&key, 1, &pk, &ds, &body),
            Some(text_47),
            "47-byte text must roundtrip"
        );

        let text_48 = "a".repeat(48);
        assert!(
            seal_private_body(&key, 1, &pk, 1, &text_48).is_none(),
            "48-byte text must return None"
        );
    }

    #[test]
    fn open_rejects_wrong_key_and_tamper() {
        let key = test_pair_key();
        let pk = test_pk();
        let ds = ds_for(&pk, 2);
        let body = seal_private_body(&key, 2, &pk, 2, "secret").expect("short text");

        let mut wrong_key = key;
        wrong_key[0] ^= 0xff;
        assert!(
            open_private_body(&wrong_key, 2, &pk, &ds, &body).is_none(),
            "wrong key must not open"
        );

        let mut tampered = body;
        tampered[0] ^= 0x01;
        assert!(
            open_private_body(&key, 2, &pk, &ds, &tampered).is_none(),
            "tampered body must not open"
        );
    }

    #[test]
    fn open_rejects_wrong_epoch_or_div_sketch() {
        let key = test_pair_key();
        let pk = test_pk();
        let ds = ds_for(&pk, 3);
        let body = seal_private_body(&key, 3, &pk, 3, "nonce test").expect("short text");

        assert!(
            open_private_body(&key, 4, &pk, &ds, &body).is_none(),
            "epoch+1 must not open (nonce mismatch)"
        );

        let mut bad_ds = ds;
        bad_ds[4] ^= 0xff; // counter byte → different nonce suffix
        assert!(
            open_private_body(&key, 3, &pk, &bad_ds, &body).is_none(),
            "altered counter must not open (nonce mismatch)"
        );
    }

    #[test]
    fn colliding_pk_prefixes_still_have_distinct_nonces() {
        // R4 regression: two senders whose pk[..4] collide (the old 32-bit partition)
        // must still get different nonce suffixes because the FULL pk is hashed.
        let key = test_pair_key();
        let pk_a = test_pk();
        let mut pk_b = pk_a;
        pk_b[31] ^= 0x01; // differs only in the last byte — pk[..4] identical
        assert_eq!(pk_a[..4], pk_b[..4], "test requires colliding pk prefixes");

        let body_a = seal_private_body(&key, 5, &pk_a, 0, "same epoch+counter").expect("seal a");
        let body_b = seal_private_body(&key, 5, &pk_b, 0, "same epoch+counter").expect("seal b");
        assert_ne!(
            body_a, body_b,
            "same (epoch, counter) but different full pk → different nonce → different ciphertext"
        );
    }

    #[test]
    fn empty_text_roundtrips() {
        let key = test_pair_key();
        let pk = test_pk();
        let ds = ds_for(&pk, 0);
        let body = seal_private_body(&key, 0, &pk, 0, "").expect("empty text must seal");
        assert_eq!(
            open_private_body(&key, 0, &pk, &ds, &body),
            Some(String::new()),
            "empty text must roundtrip"
        );
    }
}

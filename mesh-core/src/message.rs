//! `message` — frame origination and body-text accessors.
//!
//! This is the single origination path for all signed frames. `ffi::make_test_frame` delegates
//! here; no mark/sign logic exists anywhere else (invariant #1).

use crate::codec::{self, Frame, MsgType, FRAME_LEN, PROTO_VERSION};
use crate::crypto;
use crate::private;
use crate::vdl;

// Wire offset where the witness field begins; VDL prefix is buf[0..WITNESS_PREFIX_END].
// Matches codec layout: body ends at 102, pocp_wit occupies 102..118.
const WITNESS_PREFIX_END: usize = 102;

/// Default TTL for `RegionalPropagated` messages (hop budget before the frame is silently dropped).
pub const DEFAULT_TTL_REGIONAL: u8 = 8;

/// Device-unique, beacon-rotating mark.
///
/// `mark = blake3("mesh-core:v1:mark" || seed || beacon_seed)[..16]`
///
/// The beacon seed provides forward secrecy: if the static `seed` is extracted post-seizure,
/// past marks cannot be recomputed without the past beacon seed — which the one-way hash
/// chain makes unrecoverable.
fn derive_mark(seed: &[u8; 32], beacon_seed: &[u8; 32]) -> [u8; 16] {
    let mut mk = [0u8; 16];
    let mut h = blake3::Hasher::new();
    h.update(b"mesh-core:v1:mark");
    h.update(seed);
    h.update(beacon_seed);
    mk.copy_from_slice(&h.finalize().as_bytes()[..16]);
    mk
}

/// Build a signed message frame from a 32-byte `seed`, with an explicit TTL.
///
/// `reserved[0]` (wire byte 214) is set to `ttl` before encoding.  The `reserved` region is
/// outside `SIG_REGION` (`0..150`), so signing is unaffected regardless — but we set it before
/// calling `encode` to keep the build path clear and unambiguous.
///
/// Body layout: `body[0] = len`, `body[1..1+len]` = UTF-8 text, rest zero.
/// Returns `None` if `text` is longer than 63 bytes (would overflow the body length prefix).
///
/// Mark derivation (device-unique, beacon-rotating):
///   blake3("mesh-core:v1:mark" || seed || beacon_seed)[..16]
///
/// `beacon_seed` provides forward secrecy: post-seizure, past marks cannot be recomputed.
///
/// Signing: Ed25519 over `codec::signing_region` of the not-yet-signed encoding, domain-separated
/// by `crypto::DOMAIN_SIG`.  The ephemeral Ed25519 public key is embedded at bytes 118..150 so
/// relays and endpoints can verify the signature without pre-shared key material.
///
/// The ephemeral signing key is also derived from `seed || beacon_seed`, so post-seizure,
/// past public keys are unrecoverable — closing the back door that would otherwise link
/// a user's entire session history.
pub fn make_message_frame_ttl(
    seed: &[u8; 32],
    epoch: u32,
    beacon_seed: &[u8; 32],
    msg_type: MsgType,
    text: &str,
    ttl: u8,
) -> Option<[u8; FRAME_LEN]> {
    let text_bytes = text.as_bytes();
    if text_bytes.len() > 63 {
        return None;
    }

    let mark = derive_mark(seed, beacon_seed);

    // Build body: body[0] = len, body[1..1+len] = text bytes, rest already zero.
    let mut body = [0u8; 64];
    body[0] = text_bytes.len() as u8;
    body[1..1 + text_bytes.len()].copy_from_slice(text_bytes);

    let e = crypto::from_seed(seed, beacon_seed);
    let pk = crypto::public_key(&e);

    // Set reserved[0] = ttl BEFORE encoding (reserved is outside the signed region).
    let mut reserved = [0u8; 12];
    reserved[0] = ttl;

    let mut f = Frame {
        mark,
        version: PROTO_VERSION,
        msg_type,
        div_sketch: [0u8; 16],
        epoch,
        body,
        pocp_wit: [0u8; 16],
        pk,
        sig: [0u8; 64],
        reserved,
    };

    // Sign SIG_REGION (mark through pk) of the not-yet-signed encoding.
    let unsigned = codec::encode(&f);
    f.sig = crypto::sign(&e, codec::signing_region(&unsigned));

    Some(codec::encode(&f))
}

/// Build a signed message frame from a 32-byte `seed`.
///
/// Delegates to [`make_message_frame_ttl`] with TTL chosen by type:
/// - `LocalImmediate` → ttl 0 (single-hop, never relayed)
/// - `RegionalPropagated` → [`DEFAULT_TTL_REGIONAL`]
///
/// Body layout: `body[0] = len`, `body[1..1+len]` = UTF-8 text, rest zero.
/// Returns `None` if `text` is longer than 63 bytes (would overflow the body length prefix).
pub fn make_message_frame(
    seed: &[u8; 32],
    epoch: u32,
    beacon_seed: &[u8; 32],
    msg_type: MsgType,
    text: &str,
) -> Option<[u8; FRAME_LEN]> {
    // E2: guard — make_private_frame is the sole path for Private frames.
    if msg_type == MsgType::Private {
        return None;
    }
    let ttl = match msg_type {
        MsgType::LocalImmediate => 0,
        MsgType::RegionalPropagated => DEFAULT_TTL_REGIONAL,
        _ => return None,
    };
    make_message_frame_ttl(seed, epoch, beacon_seed, msg_type, text, ttl)
}

/// Build a signed public message frame WITH a PoCP spacetime witness.
///
/// Same as [`make_message_frame_ttl`] but embeds a `div_sketch` and PoCP witness so the
/// frame proves the sender was physically present in the cell. Steps:
///   1. Build frame with supplied `div_sketch` and zero `pocp_wit`.
///   2. Encode, compute PoCP witness over bytes 0..102.
///   3. Set `pocp_wit`, re-encode, sign bytes 0..150 (witness now signature-bound).
///   4. Encode final frame.
///
/// `div_sketch` is 16 bytes from `pocp::sketch_to_div_sketch` (low-byte truncation of the
/// local KMV sketch). `epoch` is both the frame epoch and the witness seed index.
///
/// Private frames are rejected — use `make_private_frame` instead.
pub fn make_message_frame_with_witness(
    seed: &[u8; 32],
    epoch: u32,
    beacon_seed: &[u8; 32],
    msg_type: MsgType,
    text: &str,
    ttl: u8,
    div_sketch: [u8; 16],
) -> Option<[u8; FRAME_LEN]> {
    use crate::pocp;

    // Private frames go through make_private_frame (VDL witness, encrypted body).
    if msg_type == MsgType::Private {
        return None;
    }

    let text_bytes = text.as_bytes();
    if text_bytes.len() > 63 {
        return None;
    }

    let mark = derive_mark(seed, beacon_seed);

    // Build body: body[0] = len, body[1..1+len] = text bytes, rest zero.
    let mut body = [0u8; 64];
    body[0] = text_bytes.len() as u8;
    body[1..1 + text_bytes.len()].copy_from_slice(text_bytes);

    let e = crypto::from_seed(seed, beacon_seed);
    let pk = crypto::public_key(&e);

    let mut reserved = [0u8; 12];
    reserved[0] = ttl;

    // Step 1: build frame with zero witness.
    let mut f = Frame {
        mark,
        version: PROTO_VERSION,
        msg_type,
        div_sketch,
        epoch,
        body,
        pocp_wit: [0u8; 16],
        pk,
        sig: [0u8; 64],
        reserved,
    };

    // Step 2: encode, compute witness over bytes 0..102 (everything before pocp_wit).
    let unsigned = codec::encode(&f);
    let wit = pocp::witness(&div_sketch, epoch, &unsigned[..WITNESS_PREFIX_END]);
    f.pocp_wit = wit;

    // Step 3: re-encode (witness now present), then sign SIG_REGION.
    let with_witness = codec::encode(&f);
    f.sig = crypto::sign(&e, codec::signing_region(&with_witness));

    // Step 4: final encode.
    Some(codec::encode(&f))
}

/// Build a signed Tier-3 private frame: encrypted body + VDL witness.
///
/// `counter` is a monotonic per-device u64 that prevents AEAD nonce reuse under the same
/// pair key within one epoch. The shim persists it across service restarts.
///
/// Steps, in order:
///   1. mark = derive_mark(seed, beacon_seed)  (same as public path)
///   2. epk = crypto::from_seed(seed, beacon_seed)
///   3. div_sketch[0..4] = pk[..4] (wire-visible sender tag), div_sketch[4..8] = counter as u32
///   4. body = private::seal_private_body(pair_key, epoch, &pk, counter, text)
///      (None if text > 47 bytes; nonce suffix = BLAKE3(pk || counter) — R4)
///   5. ttl  = DEFAULT_TTL_REGIONAL (private frames propagate; the VDL witness, not TTL, gates origination)
///   6. witness = vdl::solve over the unsigned encoding's bytes 0..102 (everything before the
///      witness field), at `difficulty_bits`. Blocking — callers run it off the UI thread.
///   7. sign SIG_REGION (mark through pk, 0..150) of the encoding that already contains the
///      witness, so the witness is signature-bound.
pub fn make_private_frame(
    seed: &[u8; 32],
    epoch: u32,
    beacon_seed: &[u8; 32],
    pair_key: &[u8; 32],
    text: &str,
    difficulty_bits: u8,
    counter: u64,
) -> Option<[u8; FRAME_LEN]> {
    let mark = derive_mark(seed, beacon_seed);

    // Step 2: derive ephemeral pubkey for sender-direction tag and verification.
    let e = crypto::from_seed(seed, beacon_seed);
    let pk = crypto::public_key(&e);

    // Step 3: div_sketch carries a wire-visible sender tag + counter for the AEAD nonce.
    // The nonce suffix itself is BLAKE3("mesh-core:v1:nonce" || full_pk || counter)[..8]
    // (R4): sender separation is 256-bit, so paired devices cannot collide even when
    // their pk[..4] prefixes match.
    let mut div_sketch = [0u8; 16];
    div_sketch[..4].copy_from_slice(&pk[..4]);
    div_sketch[4..8].copy_from_slice(&(counter as u32).to_be_bytes());

    // Step 4: encrypted body (None if text > 47 bytes). Nonce from pk + counter.
    let body = private::seal_private_body(pair_key, epoch, &pk, counter, text)?;

    // Step 5: TTL for private frames (propagated; witness gates origination, not TTL).
    let mut reserved = [0u8; 12];
    reserved[0] = DEFAULT_TTL_REGIONAL;

    // Build frame with zero witness and zero sig first.
    let mut f = Frame {
        mark,
        version: PROTO_VERSION,
        msg_type: MsgType::Private,
        div_sketch,
        epoch,
        body,
        pocp_wit: [0u8; 16],
        pk,
        sig: [0u8; 64],
        reserved,
    };

    // Step 6: encode with zero witness, solve VDL over bytes 0..WITNESS_PREFIX_END.
    let unsigned = codec::encode(&f);
    let wit = vdl::solve(&unsigned[..WITNESS_PREFIX_END], difficulty_bits);
    f.pocp_wit = wit;

    // Step 7: re-encode (witness now present), then sign SIG_REGION (mark through pk)
    // so witness is signature-bound.
    let with_witness = codec::encode(&f);
    f.sig = crypto::sign(&e, codec::signing_region(&with_witness));

    Some(codec::encode(&f))
}

/// Try to open `buf` as a private frame under one pair key.
/// Parse -> verify -> decide: decode (226-byte structural check), require MsgType::Private,
/// verify the embedded Ed25519 signature (R8: self-contained — no caller precondition),
/// verify the VDL witness at `difficulty_bits`, then AEAD-open the body.
/// None at any failure — wrong key is indistinguishable from a non-private or invalid frame.
pub fn open_private_frame(
    buf: &[u8],
    pair_key: &[u8; 32],
    difficulty_bits: u8,
) -> Option<String> {
    let arr: &[u8; FRAME_LEN] = buf.try_into().ok()?;
    let f = codec::decode(arr).ok()?;
    if f.msg_type != MsgType::Private {
        return None;
    }
    // R8: verify the embedded ephemeral signature before spending VDL/AEAD work.
    if !crypto::verify(&f.pk, codec::signing_region(arr), &f.sig) {
        return None;
    }
    if !vdl::verify(&arr[..WITNESS_PREFIX_END], &f.pocp_wit, difficulty_bits) {
        return None;
    }
    private::open_private_body(pair_key, f.epoch, &f.pk, &f.div_sketch, &f.body)
}

/// Extract the body text from a decoded `Frame`.
///
/// Returns `None` if:
/// - `body[0]` (the length byte) is > 63
/// - Any byte in `body[1+len..64]` is non-zero (tail not zeroed)
/// - The text bytes are not valid UTF-8
///
/// No allocation, no panic.
pub fn body_text(f: &Frame) -> Option<&str> {
    let len = f.body[0] as usize;
    if len > 63 {
        return None;
    }
    // Tail must be all-zero.
    if f.body[1 + len..].iter().any(|&b| b != 0) {
        return None;
    }
    core::str::from_utf8(&f.body[1..1 + len]).ok()
}

/// Dedup key: blake3 of `buf[0..214]` (excludes the hop-mutable `reserved` region at [214..226]), first 16 bytes.
pub fn frame_hash(buf: &[u8; FRAME_LEN]) -> [u8; 16] {
    let mut out = [0u8; 16];
    let digest = blake3::hash(&buf[..214]);
    out.copy_from_slice(&digest.as_bytes()[..16]);
    out
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::codec::{MsgType, FRAME_LEN, decode};
    use crate::crypto;

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
    fn message_roundtrip_sig_verifies() {
        let seed = test_seed();
        let bs = test_beacon_seed();
        let epoch = 42u32;
        let text = "hello protest mesh";

        let buf = make_message_frame(&seed, epoch, &bs, MsgType::RegionalPropagated, text)
            .expect("short text");

        // Decode must succeed.
        let frame = decode(&buf).expect("valid frame");

        // body_text must recover original text.
        assert_eq!(body_text(&frame), Some(text));

        // Signature must verify against the ephemeral pubkey derived from the same seed/beacon_seed.
        let e = crypto::from_seed(&seed, &bs);
        let pk = crypto::public_key(&e);
        assert!(crypto::verify(&pk, codec::signing_region(&buf), &frame.sig));
    }

    #[test]
    fn text_63_bytes_ok() {
        let seed = test_seed();
        let bs = test_beacon_seed();
        let text = "a".repeat(63);
        assert!(make_message_frame(&seed, 1, &bs, MsgType::LocalImmediate, &text).is_some());
    }

    #[test]
    fn text_64_bytes_returns_none() {
        let seed = test_seed();
        let bs = test_beacon_seed();
        let text = "a".repeat(64);
        assert!(make_message_frame(&seed, 1, &bs, MsgType::LocalImmediate, &text).is_none());
    }

    #[test]
    fn body_text_rejects_len_too_large() {
        let mut body = [0u8; 64];
        body[0] = 64; // invalid: max is 63
        let f = Frame {
            mark: [0u8; 16],
            version: crate::codec::PROTO_VERSION,
            msg_type: MsgType::RegionalPropagated,
            div_sketch: [0u8; 16],
            epoch: 0,
            body,
            pocp_wit: [0u8; 16],
            pk: [0u8; 32],
            sig: [0u8; 64],
            reserved: [0u8; 12],
        };
        assert_eq!(body_text(&f), None);
    }

    #[test]
    fn body_text_rejects_nonzero_tail() {
        let mut body = [0u8; 64];
        body[0] = 2; // len = 2
        body[1] = b'h';
        body[2] = b'i';
        body[5] = 0xff; // nonzero in tail — invalid
        let f = Frame {
            mark: [0u8; 16],
            version: crate::codec::PROTO_VERSION,
            msg_type: MsgType::RegionalPropagated,
            div_sketch: [0u8; 16],
            epoch: 0,
            body,
            pocp_wit: [0u8; 16],
            pk: [0u8; 32],
            sig: [0u8; 64],
            reserved: [0u8; 12],
        };
        assert_eq!(body_text(&f), None);
    }

    #[test]
    fn body_text_rejects_invalid_utf8() {
        let mut body = [0u8; 64];
        body[0] = 2; // len = 2
        body[1] = 0xff; // invalid UTF-8 start byte
        body[2] = 0xfe;
        let f = Frame {
            mark: [0u8; 16],
            version: crate::codec::PROTO_VERSION,
            msg_type: MsgType::RegionalPropagated,
            div_sketch: [0u8; 16],
            epoch: 0,
            body,
            pocp_wit: [0u8; 16],
            pk: [0u8; 32],
            sig: [0u8; 64],
            reserved: [0u8; 12],
        };
        assert_eq!(body_text(&f), None);
    }

    #[test]
    fn frame_hash_ignores_reserved() {
        let seed = test_seed();
        let bs = test_beacon_seed();
        let buf = make_message_frame(&seed, 1, &bs, MsgType::RegionalPropagated, "hash test")
            .expect("short text");
        let hash1 = frame_hash(&buf);

        // Flip a reserved byte (bytes 214..226).
        let mut buf2 = buf;
        buf2[217] ^= 0xff;
        let hash2 = frame_hash(&buf2);

        assert_eq!(hash1, hash2, "reserved bytes must not affect the hash");
    }

    #[test]
    fn frame_hash_sensitive_to_sig() {
        let seed = test_seed();
        let bs = test_beacon_seed();
        let buf = make_message_frame(&seed, 1, &bs, MsgType::RegionalPropagated, "sig test")
            .expect("short text");
        let hash1 = frame_hash(&buf);

        // Flip a sig byte (bytes 150..214 — inside the hashed region).
        let mut buf2 = buf;
        buf2[152] ^= 0xff;
        let hash2 = frame_hash(&buf2);

        assert_ne!(hash1, hash2, "flipping a sig byte must change the hash");
    }

    // ----- M4-lite TTL tests -----

    #[test]
    fn make_message_frame_ttl_sets_byte_214() {
        let seed = test_seed();
        let bs = test_beacon_seed();
        let buf = make_message_frame_ttl(&seed, 1, &bs, MsgType::RegionalPropagated, "ttl test", 5)
            .expect("short text");
        assert_eq!(buf[214], 5, "TTL must be written to wire byte 214");
    }

    #[test]
    fn make_message_frame_ttl_sig_verifies_any_ttl() {
        let seed = test_seed();
        let bs = test_beacon_seed();
        // Verify that the signature is valid regardless of the TTL value, confirming reserved
        // is outside the signed region.
        for ttl in [0u8, 1, 8, 255] {
            let buf =
                make_message_frame_ttl(&seed, 2, &bs, MsgType::RegionalPropagated, "sig check", ttl)
                    .expect("short text");
            let frame = decode(&buf).expect("valid frame");
            let e = crypto::from_seed(&seed, &bs);
            let pk = crypto::public_key(&e);
            assert!(
                crypto::verify(&pk, codec::signing_region(&buf), &frame.sig),
                "sig must verify for ttl={ttl}"
            );
        }
    }

    #[test]
    fn frame_hash_identical_across_ttl_values() {
        let seed = test_seed();
        let bs = test_beacon_seed();
        let buf1 =
            make_message_frame_ttl(&seed, 3, &bs, MsgType::RegionalPropagated, "hash ttl", 3)
                .expect("short text");
        let buf2 =
            make_message_frame_ttl(&seed, 3, &bs, MsgType::RegionalPropagated, "hash ttl", 255)
                .expect("short text");
        // frame_hash covers buf[0..214], so differing TTL must not change the hash.
        assert_eq!(
            frame_hash(&buf1),
            frame_hash(&buf2),
            "frame_hash must be identical for different TTL values"
        );
    }

    #[test]
    fn make_message_frame_defaults_local_immediate_ttl_0() {
        let seed = test_seed();
        let bs = test_beacon_seed();
        let buf = make_message_frame(&seed, 4, &bs, MsgType::LocalImmediate, "local")
            .expect("short text");
        assert_eq!(buf[214], 0, "LocalImmediate must have TTL 0 at byte 214");
    }

    #[test]
    fn make_message_frame_defaults_regional_propagated_ttl_8() {
        let seed = test_seed();
        let bs = test_beacon_seed();
        let buf = make_message_frame(&seed, 4, &bs, MsgType::RegionalPropagated, "regional")
            .expect("short text");
        assert_eq!(
            buf[214], DEFAULT_TTL_REGIONAL,
            "RegionalPropagated must have DEFAULT_TTL_REGIONAL at byte 214"
        );
    }

    // ----- private frame tests -----

    #[test]
    fn private_frame_roundtrips() {
        let seed = [7u8; 32];
        let bs = test_beacon_seed();
        let epoch = 42u32;
        let pair_key = [9u8; 32];
        let text = "secret hi";

        let buf = make_private_frame(&seed, epoch, &bs, &pair_key, text, 8, 0)
            .expect("short text, low difficulty");

        assert_eq!(buf.len(), FRAME_LEN);
        assert_eq!(buf[17], 3, "msg_type Private must be 3 on wire");
        assert_eq!(buf[214], DEFAULT_TTL_REGIONAL, "TTL must be DEFAULT_TTL_REGIONAL at byte 214");

        let result = open_private_frame(&buf, &pair_key, 8);
        assert_eq!(result, Some("secret hi".to_string()));
    }

    #[test]
    fn private_frame_wrong_key_fails() {
        let seed = [7u8; 32];
        let bs = test_beacon_seed();
        let epoch = 42u32;
        let pair_key = [9u8; 32];
        let wrong_key = [10u8; 32];

        let buf = make_private_frame(&seed, epoch, &bs, &pair_key, "secret hi", 8, 0)
            .expect("short text");

        assert!(open_private_frame(&buf, &wrong_key, 8).is_none());
    }

    #[test]
    fn private_frame_witness_tampered_fails() {
        let seed = [7u8; 32];
        let bs = test_beacon_seed();
        let epoch = 42u32;
        let pair_key = [9u8; 32];

        let buf = make_private_frame(&seed, epoch, &bs, &pair_key, "secret hi", 8, 0)
            .expect("short text");

        let mut tampered = buf;
        tampered[102] ^= 0x01;

        assert!(open_private_frame(&tampered, &pair_key, 8).is_none());
    }

    #[test]
    fn private_frame_text_too_long() {
        let seed = [7u8; 32];
        let bs = test_beacon_seed();
        let pair_key = [9u8; 32];
        let text = "a".repeat(48);

        assert!(make_private_frame(&seed, 1, &bs, &pair_key, &text, 8, 0).is_none());
    }

    #[test]
    fn private_frame_body_not_plaintext() {
        let seed = [7u8; 32];
        let bs = test_beacon_seed();
        let epoch = 42u32;
        let pair_key = [9u8; 32];

        let buf = make_private_frame(&seed, epoch, &bs, &pair_key, "secret hi", 8, 0)
            .expect("short text");

        let decoded = decode(&buf).expect("valid frame");
        assert_ne!(
            body_text(&decoded),
            Some("secret hi"),
            "body_text must not return the secret (body is ciphertext)"
        );
    }

    #[test]
    fn private_frame_counter_changes_nonce() {
        let seed = [7u8; 32];
        let bs = test_beacon_seed();
        let epoch = 1u32;
        let pair_key = [9u8; 32];
        let text = "same epoch text";

        let buf0 = make_private_frame(&seed, epoch, &bs, &pair_key, text, 8, 0)
            .expect("counter=0");
        let buf1 = make_private_frame(&seed, epoch, &bs, &pair_key, text, 8, 1)
            .expect("counter=1");

        // Different counter → different div_sketch → different nonce → different body
        assert_ne!(buf0, buf1, "different counter must produce different frame");

        // Both must decrypt with the same key
        assert_eq!(
            open_private_frame(&buf0, &pair_key, 8),
            Some(text.to_string()),
            "counter=0 must open"
        );
        assert_eq!(
            open_private_frame(&buf1, &pair_key, 8),
            Some(text.to_string()),
            "counter=1 must open"
        );
    }
}

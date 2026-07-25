use mesh_core::codec::{DecodeErr, FRAME_LEN, Frame, MsgType, PROTO_VERSION, decode, encode};
use proptest::prelude::*;

// ---------------------------------------------------------------------------
// proptest suite
// ---------------------------------------------------------------------------

proptest! {
    /// Any vec whose length != FRAME_LEN must return BadLen.
    #[test]
    fn decode_rejects_wrong_len(bytes in proptest::collection::vec(any::<u8>(), 0usize..400usize)) {
        prop_assume!(bytes.len() != FRAME_LEN);
        prop_assert_eq!(decode(&bytes), Err(DecodeErr::BadLen));
    }

    /// decode never panics on any input of any length.
    #[test]
    fn decode_never_panics(bytes in proptest::collection::vec(any::<u8>(), 0usize..400usize)) {
        let _ = decode(&bytes);
    }

    /// encode then decode is identity for valid frames.
    #[test]
    fn roundtrip_encode_decode(
        mark    in any::<[u8;16]>(),
        div_sketch in any::<[u8;16]>(),
        epoch   in any::<u32>(),
        body    in any::<[u8;64]>(),
        pocp_wit in any::<[u8;16]>(),
        pk      in any::<[u8;32]>(),
        sig     in any::<[u8;64]>(),
        ttl     in any::<u8>(),
        msg_type_raw in 1u8..=2u8,
    ) {
        // D3: reserved[0] (TTL) is hop-mutable; reserved[1..12] must be zero to decode.
        let mut reserved = [0u8; 12];
        reserved[0] = ttl;
        let msg_type = MsgType::from_u8(msg_type_raw).unwrap();
        let f = Frame { mark, version: PROTO_VERSION, msg_type, div_sketch, epoch, body, pocp_wit, pk, sig, reserved };
        let encoded = encode(&f);
        let decoded = decode(&encoded).unwrap();
        prop_assert_eq!(decoded, f);
    }

    /// For a valid FRAME_LEN-byte buffer, decode then encode is identity.
    #[test]
    fn roundtrip_decode_encode(
        mut buf in proptest::collection::vec(any::<u8>(), FRAME_LEN..=FRAME_LEN),
        msg_type_raw in 1u8..=2u8,
    ) {
        buf[16] = PROTO_VERSION;
        buf[17] = msg_type_raw;
        // D3: bytes 215..226 must be zero for the buffer to be a valid frame.
        for b in &mut buf[215..226] { *b = 0; }
        let frame = decode(&buf).unwrap();
        let reencoded = encode(&frame);
        prop_assert_eq!(reencoded.as_ref(), buf.as_slice());
    }

    /// D3: any non-zero byte in reserved[1..12] (wire bytes 215..226) must reject.
    #[test]
    fn decode_rejects_nonzero_reserved_tail(
        mut buf in proptest::collection::vec(any::<u8>(), FRAME_LEN..=FRAME_LEN),
        idx in 215usize..226usize,
        val in 1u8..=255u8,
        msg_type_raw in 1u8..=2u8,
    ) {
        buf[16] = PROTO_VERSION;
        buf[17] = msg_type_raw;
        for b in &mut buf[215..226] { *b = 0; }
        buf[idx] = val;
        prop_assert_eq!(decode(&buf), Err(DecodeErr::BadReserved));
    }
}

// ---------------------------------------------------------------------------
// Unit tests
// ---------------------------------------------------------------------------

#[test]
fn bad_version_returns_bad_version() {
    let mut buf = [0u8; FRAME_LEN];
    buf[16] = 2; // version != PROTO_VERSION (1)
    buf[17] = 1; // valid msg_type
    assert_eq!(decode(&buf), Err(DecodeErr::BadVersion));
}

#[test]
fn bad_type_returns_bad_type() {
    let mut buf = [0u8; FRAME_LEN];
    buf[16] = PROTO_VERSION;
    buf[17] = 99; // unknown msg_type
    assert_eq!(decode(&buf), Err(DecodeErr::BadType));
}

#[test]
fn short_buf_returns_bad_len() {
    let buf = [0u8; 225];
    assert_eq!(decode(&buf), Err(DecodeErr::BadLen));
}

#[test]
fn long_buf_returns_bad_len() {
    let buf = [0u8; 227];
    assert_eq!(decode(&buf), Err(DecodeErr::BadLen));
}

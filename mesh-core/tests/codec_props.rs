use mesh_core::codec::{DecodeErr, FRAME_LEN, Frame, MsgType, PROTO_VERSION, decode, encode};
use proptest::prelude::*;

// ---------------------------------------------------------------------------
// proptest suite
// ---------------------------------------------------------------------------

proptest! {
    /// Any vec whose length != 194 must return BadLen.
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
        sig     in any::<[u8;64]>(),
        reserved in any::<[u8;12]>(),
        msg_type_raw in 1u8..=2u8,
    ) {
        let msg_type = MsgType::from_u8(msg_type_raw).unwrap();
        let f = Frame { mark, version: PROTO_VERSION, msg_type, div_sketch, epoch, body, pocp_wit, sig, reserved };
        let encoded = encode(&f);
        let decoded = decode(&encoded).unwrap();
        prop_assert_eq!(decoded, f);
    }

    /// For a valid 194-byte buffer, decode then encode is identity.
    #[test]
    fn roundtrip_decode_encode(
        mut buf in proptest::collection::vec(any::<u8>(), FRAME_LEN..=FRAME_LEN),
        msg_type_raw in 1u8..=2u8,
    ) {
        buf[16] = PROTO_VERSION;
        buf[17] = msg_type_raw;
        let frame = decode(&buf).unwrap();
        let reencoded = encode(&frame);
        prop_assert_eq!(reencoded.as_ref(), buf.as_slice());
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
    let buf = [0u8; 193];
    assert_eq!(decode(&buf), Err(DecodeErr::BadLen));
}

#[test]
fn long_buf_returns_bad_len() {
    let buf = [0u8; 195];
    assert_eq!(decode(&buf), Err(DecodeErr::BadLen));
}

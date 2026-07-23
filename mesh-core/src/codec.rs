//! `codec` — the fixed 226 B wire frame. THE anti-zip-bomb boundary (invariant #3).
//!
//! Hand-rolled fixed-offset encode/decode. No serde, no varint, no compression.
//! `decode` is len-checked and total-fail on any deviation, zero side effects.
//!
//! Wire format (226 B):
//!   [0..16)   mark          — 16 B pseudo-random message identifier
//!   [16..18)  hdr           — 2 B header (version, msg_type)
//!   [18..34)  div_sketch    — 16 B KMV diversity sketch / counter
//!   [34..38)  epoch         — 4 B BE epoch index
//!   [38..102) body          — 64 B payload (plaintext or AEAD ciphertext)
//!   [102..118) pocp_wit    — 16 B Proof-of-Co-Presence witness / VDL witness
//!   [118..150) pk           — 32 B ephemeral Ed25519 public key
//!   [150..214) sig          — 64 B Ed25519 signature over [0..150)
//!   [214..226) reserved     — 12 B hop-mutable region (TTL, RSSI metrics)
//!   Total: 226 B

use arrayref::array_ref;

/// Total on-wire frame size. BLE 5 Extended Advertising supports up to 255 B. Deviation => total drop.
pub const FRAME_LEN: usize = 226;

/// Protocol version byte this codec speaks.
pub const PROTO_VERSION: u8 = 1;

/// Byte range that the signature authenticates (mark through pk; excludes sig + reserved).
pub const SIG_REGION: core::ops::Range<usize> = 0..150;

/// Known message types.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum MsgType {
    LocalImmediate = 1,
    RegionalPropagated = 2,
    /// End-to-end encrypted pairwise message; body is ChaCha20-Poly1305 ciphertext, relayed only with a valid VDL witness.
    Private = 3,
}

impl MsgType {
    pub fn from_u8(v: u8) -> Option<MsgType> {
        match v {
            1 => Some(MsgType::LocalImmediate),
            2 => Some(MsgType::RegionalPropagated),
            3 => Some(MsgType::Private),
            _ => None,
        }
    }

    pub fn to_u8(self) -> u8 {
        self as u8
    }
}

/// Decode failures. Every variant means: drop the frame, mutate nothing, relay nothing.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum DecodeErr {
    BadLen,
    BadVersion,
    BadType,
}

/// Parsed wire frame (226 B, fixed-offset, big-endian).
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Frame {
    pub mark: [u8; 16],
    pub version: u8,
    pub msg_type: MsgType,
    pub div_sketch: [u8; 16],
    pub epoch: u32,
    pub body: [u8; 64],
    pub pocp_wit: [u8; 16],
    /// Ephemeral Ed25519 public key (32 B) — enables any relay or endpoint to verify the
    /// frame signature without pre-shared key material.
    pub pk: [u8; 32],
    pub sig: [u8; 64],
    pub reserved: [u8; 12],
}

/// Decode a wire buffer into a `Frame`. Total-fail on any deviation. No allocation, no panic.
pub fn decode(buf: &[u8]) -> Result<Frame, DecodeErr> {
    if buf.len() != FRAME_LEN {
        return Err(DecodeErr::BadLen);
    }
    let arr = array_ref!(buf, 0, 226);

    let version = arr[16];
    if version != PROTO_VERSION {
        return Err(DecodeErr::BadVersion);
    }

    let msg_type = MsgType::from_u8(arr[17]).ok_or(DecodeErr::BadType)?;

    let mark = *array_ref!(arr, 0, 16);
    let div_sketch = *array_ref!(arr, 18, 16);
    let epoch = u32::from_be_bytes(*array_ref!(arr, 34, 4));
    let body = *array_ref!(arr, 38, 64);
    let pocp_wit = *array_ref!(arr, 102, 16);
    let pk = *array_ref!(arr, 118, 32);
    let sig = *array_ref!(arr, 150, 64);
    let reserved = *array_ref!(arr, 214, 12);

    Ok(Frame {
        mark,
        version,
        msg_type,
        div_sketch,
        epoch,
        body,
        pocp_wit,
        pk,
        sig,
        reserved,
    })
}

/// Encode a `Frame` into a fixed 226-byte buffer. Inverse of `decode`.
pub fn encode(f: &Frame) -> [u8; FRAME_LEN] {
    let mut out = [0u8; FRAME_LEN];
    out[0..16].copy_from_slice(&f.mark);
    out[16] = f.version;
    out[17] = f.msg_type.to_u8();
    out[18..34].copy_from_slice(&f.div_sketch);
    out[34..38].copy_from_slice(&f.epoch.to_be_bytes());
    out[38..102].copy_from_slice(&f.body);
    out[102..118].copy_from_slice(&f.pocp_wit);
    out[118..150].copy_from_slice(&f.pk);
    out[150..214].copy_from_slice(&f.sig);
    out[214..226].copy_from_slice(&f.reserved);
    out
}

/// Return the bytes the signature authenticates (mark through pk).
pub fn signing_region(buf: &[u8; FRAME_LEN]) -> &[u8] {
    &buf[SIG_REGION]
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn msg_type_private_roundtrips() {
        assert_eq!(MsgType::from_u8(3), Some(MsgType::Private));
        assert_eq!(MsgType::Private as u8, 3);
        assert_eq!(MsgType::from_u8(4), None);
    }
}

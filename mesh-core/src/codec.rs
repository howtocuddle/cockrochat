//! `codec` — the fixed 194 B wire frame. THE anti-zip-bomb boundary (invariant #3).
//!
//! Hand-rolled fixed-offset encode/decode. No serde, no varint, no compression.
//! `decode` is len-checked and total-fail on any deviation, zero side effects.

use arrayref::array_ref;

/// Total on-wire frame size. Fixed forever (BLE 5 AUX PDU budget). Deviation => total drop.
pub const FRAME_LEN: usize = 194;

/// Protocol version byte this codec speaks.
pub const PROTO_VERSION: u8 = 1;

/// Byte range that the signature authenticates (mark through pocp_wit; excludes sig + reserved).
pub const SIG_REGION: core::ops::Range<usize> = 0..118;

/// Known message types.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum MsgType {
    LocalImmediate = 1,
    RegionalPropagated = 2,
}

impl MsgType {
    pub fn from_u8(v: u8) -> Option<MsgType> {
        match v {
            1 => Some(MsgType::LocalImmediate),
            2 => Some(MsgType::RegionalPropagated),
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

/// Parsed wire frame (194 B, fixed-offset, big-endian).
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Frame {
    pub mark: [u8; 16],
    pub version: u8,
    pub msg_type: MsgType,
    pub div_sketch: [u8; 16],
    pub epoch: u32,
    pub body: [u8; 64],
    pub pocp_wit: [u8; 16],
    pub sig: [u8; 64],
    pub reserved: [u8; 12],
}

/// Decode a wire buffer into a `Frame`. Total-fail on any deviation. No allocation, no panic.
pub fn decode(buf: &[u8]) -> Result<Frame, DecodeErr> {
    if buf.len() != FRAME_LEN {
        return Err(DecodeErr::BadLen);
    }
    let arr = array_ref!(buf, 0, 194);

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
    let sig = *array_ref!(arr, 118, 64);
    let reserved = *array_ref!(arr, 182, 12);

    Ok(Frame {
        mark,
        version,
        msg_type,
        div_sketch,
        epoch,
        body,
        pocp_wit,
        sig,
        reserved,
    })
}

/// Encode a `Frame` into a fixed 194-byte buffer. Inverse of `decode`.
pub fn encode(f: &Frame) -> [u8; FRAME_LEN] {
    let mut out = [0u8; FRAME_LEN];
    out[0..16].copy_from_slice(&f.mark);
    out[16] = f.version;
    out[17] = f.msg_type.to_u8();
    out[18..34].copy_from_slice(&f.div_sketch);
    out[34..38].copy_from_slice(&f.epoch.to_be_bytes());
    out[38..102].copy_from_slice(&f.body);
    out[102..118].copy_from_slice(&f.pocp_wit);
    out[118..182].copy_from_slice(&f.sig);
    out[182..194].copy_from_slice(&f.reserved);
    out
}

/// Return the bytes the signature authenticates (mark through pocp_wit).
pub fn signing_region(buf: &[u8; FRAME_LEN]) -> &[u8] {
    &buf[SIG_REGION]
}

//! `beacon` — chained hash beacon (NO VDF in v0, audit F1). Entropy = marks from
//! NON-propagating (TTL 0/1) local traffic: agreed among locals, unobservable to a remote van.
//! Acceleration cap via `floor_ms`. Chain-stall falls back to a local beacon (F6).
//! See README.md §4.

use crate::pocp::CellSketch;

/// A beacon step in the local hash chain.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Beacon {
    pub seed: [u8; 32],
    pub epoch: u32,
    pub last_advance_ms: u64,
}

/// Local entropy gathered from non-propagating marks (must clear `min_hearers`).
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Entropy(pub [u8; 32]);

/// Derive entropy from non-propagating local marks; `None` if too few hearers.
pub fn local_entropy(_nonprop_marks: &[[u8; 16]], _min_hearers: u32) -> Option<Entropy> {
    todo!("M5")
}

/// Advance the chain: `Some(new)` iff entropy observed AND (now - last_advance) >= floor_ms.
pub fn advance(_prev: &Beacon, _e: &Entropy, _now_ms: u64, _floor_ms: u64) -> Option<Beacon> {
    todo!("M5: acceleration cap")
}

/// Chain-stall path (F6): synthesize a beacon from the local cell so the wall still works.
pub fn fallback_local(_cell: &CellSketch) -> Beacon {
    todo!("M5")
}

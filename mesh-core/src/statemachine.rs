//! `statemachine` — the message engine. Owns seen-set (time-decaying Bloom, window 2*T_epoch),
//! Trickle timers (K_supp, W, RSSI-biased slot), TTL/H_max, tier routing, dispatch to `trust`.
//!
//! PROCESSING ORDER IS ENFORCED HERE AND NON-NEGOTIABLE (invariant #2, v1 §5.5):
//!   len -> epoch in {N, N-1} -> mark-unseen -> sig-verify -> witness-structural -> THEN relay/render.
//! Nothing is relayed or rendered before validation completes. See mesh-build-plan.md §2.6.

use crate::codec::FRAME_LEN;

/// Routing tier for an originated message.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Tier {
    /// Tier-1: local-immediate (single hop, no propagation).
    LocalImmediate,
    /// Tier-2: regional-propagated (flood + Trickle + dedup).
    RegionalPropagated,
    /// Tier-3: private plane (QR pairing + Noise ratchet). DEFERRED past v0 — interface stub only.
    Private,
}

/// A validated, renderable alert handed up to the UI.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Alert {
    pub id: [u8; 16],
    pub body: [u8; 64],
}

/// Why a frame was dropped (never surfaced to the wire; local diagnostics only).
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Reason {
    Decode,
    StaleEpoch,
    Seen,
    BadSig,
    BadWitness,
}

/// A security-relevant event to log/alarm (e.g. CellMismatch => relocation/replay).
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SecurityEvent {
    CellMismatch,
    ChainStall,
    MalformedStorm,
}

/// The single decision produced by ingesting a received frame.
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Ingest {
    Relay([u8; FRAME_LEN]),
    Deliver(Alert),
    Drop(Reason),
    Alarm(SecurityEvent),
}

/// The message engine. Constructed by the shim, driven by radio callbacks + a timer tick.
pub struct Engine {
    _private: (),
}

impl Engine {
    /// Ingest one raw received frame: parse -> verify -> decide (order fixed above).
    pub fn on_recv(&mut self, _raw: &[u8; FRAME_LEN], _rssi: i8, _now_ms: u64) -> Ingest {
        todo!("M4/M5")
    }

    /// Originate a local message on the given tier; returns the frame to advertise.
    pub fn on_originate(&mut self, _tier: Tier, _body: [u8; 64]) -> [u8; FRAME_LEN] {
        todo!("M4")
    }

    /// Fire any due (unsuppressed) rebroadcasts.
    pub fn tick(&mut self, _now_ms: u64) -> Vec<[u8; FRAME_LEN]> {
        todo!("M4: Trickle")
    }
}

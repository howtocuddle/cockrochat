//! `trust` — diversity + corroboration. Danger escalates only at >= k DISTINCT locally-verified
//! cells (invariant #7: trust is per-message physical corroboration, never bound to an identity).
//! v0: only locally-verified cells increment (F5); CellMismatch events are logged, not yet
//! challenged (fraud-proof protocol deferred). See mesh-build-plan.md §2.5.

/// KMV over DISTINCT locally-verified cell digests backing one alert.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct DiversitySketch(pub [u64; 16]);

/// Corroboration state shown on the confidence wall — NEVER a boolean, danger-only (invariant #4).
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Confidence {
    pub cells_for: u32,
    pub cells_dispute: u32,
    pub liveness: u32,
}

/// Merge two diversity sketches (KMV union).
pub fn merge(_a: &DiversitySketch, _b: &DiversitySketch) -> DiversitySketch {
    todo!("M6")
}

/// Threshold-only distinct-cell estimate (">= D?"), liveness-weighted.
pub fn distinct_estimate(_s: &DiversitySketch) -> u32 {
    todo!("M6")
}

/// Current corroboration for an alert.
pub fn corroboration(_alert_id: &[u8; 16]) -> Confidence {
    todo!("M6")
}

//! `trust` — diversity + corroboration. Danger escalates only at >= k DISTINCT locally-verified
//! cells (invariant #7: trust is per-message physical corroboration, never bound to an identity).
//! v0: only locally-verified cells increment (F5); CellMismatch events are logged, not yet
//! challenged (fraud-proof protocol deferred). See README.md §5.

use std::collections::{HashMap, HashSet};

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

// ---- v0 simplified trust state (H2 fix) ----

use crate::pocp;
use std::collections::VecDeque;

/// Default bound on tracked frame hashes (R5: remote memory-exhaustion DoS otherwise).
const DEFAULT_CAP: usize = 4096;

/// Maximum distinct cell claims retained per frame hash.
///
/// `distinct_count` is a HINT, never proof: a single co-located adversary can mint claims
/// at will (the witness MAC key is public — see the `pocp` module header). The cap bounds
/// the memory that costs, and stops the displayed corroboration number running away.
const MAX_CLAIMS_PER_FRAME: usize = 32;

/// Simplified trust state for v0: counts distinct cell sketches that verified each frame.
/// Full DiversitySketch KMV union is deferred (M6).
///
/// Bounded (R5): at most `cap` frame hashes are tracked; oldest are FIFO-evicted.
pub struct TrustState {
    verifications: HashMap<[u8; 16], HashSet<[u8; 16]>>,
    /// FIFO insertion order of frame_hash keys for bounded eviction.
    order: VecDeque<[u8; 16]>,
    cap: usize,
}

impl Default for TrustState {
    fn default() -> Self {
        Self::new()
    }
}

impl TrustState {
    pub fn new() -> Self {
        TrustState {
            verifications: HashMap::new(),
            order: VecDeque::new(),
            cap: DEFAULT_CAP,
        }
    }

    /// Record that a frame was verified from a given cell sketch.
    /// Returns the new distinct cell count for this frame.
    ///
    /// Anti-inflation (R2): a claim counts as a NEW cell only if it is dissimilar
    /// (Jaccard < `tau`) from every claim already recorded for this frame. Two claims
    /// that both fuzzy-match the verifier's local cell are necessarily similar to each
    /// other, so a single physical cell cannot be counted twice by re-claiming its own
    /// sketch with small perturbations.
    ///
    /// Witness-less (all-zero) and empty-cell (all-0xFF) claims never corroborate.
    pub fn record(&mut self, frame_hash: [u8; 16], div_sketch: [u8; 16], tau: f32) -> u32 {
        if div_sketch.iter().all(|&b| b == 0) || div_sketch.iter().all(|&b| b == 0xFF) {
            return self.distinct_count(&frame_hash);
        }

        if !self.verifications.contains_key(&frame_hash) {
            // Bounded eviction (R5): make room before inserting a new key.
            while self.order.len() >= self.cap {
                match self.order.pop_front() {
                    Some(oldest) => {
                        self.verifications.remove(&oldest);
                    }
                    None => break,
                }
            }
            self.order.push_back(frame_hash);
        }

        let new_cell = pocp::div_sketch_to_cell(&div_sketch);
        let set = self.verifications.entry(frame_hash).or_default();
        // Per-hash claim cap. Only the OUTER map was bounded, so one co-located attacker
        // sending the same text with a fresh random div_sketch each time inserted an entry
        // every round (random sketches are pairwise dissimilar, so the R2 domination check
        // never collapses them) — unbounded growth from wire input, and a corroboration
        // count limited only by how long the attacker keeps transmitting.
        if set.len() >= MAX_CLAIMS_PER_FRAME {
            return set.len() as u32;
        }
        let dominated = set
            .iter()
            .any(|c| pocp::jaccard(&pocp::div_sketch_to_cell(c), &new_cell) >= tau);
        if !dominated {
            set.insert(div_sketch);
        }
        set.len() as u32
    }

    /// Number of distinct cells that have verified this frame.
    pub fn distinct_count(&self, frame_hash: &[u8; 16]) -> u32 {
        self.verifications
            .get(frame_hash)
            .map(|s| s.len() as u32)
            .unwrap_or(0)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    const TAU: f32 = 0.5;

    /// A div_sketch with the first `n` slots filled (rest padding 0xFF).
    fn div_with(slots: &[u8]) -> [u8; 16] {
        let mut d = [0xFFu8; 16];
        d[..slots.len()].copy_from_slice(slots);
        d
    }

    #[test]
    fn identical_claims_count_once() {
        let mut t = TrustState::new();
        let fh = [1u8; 16];
        assert_eq!(t.record(fh, div_with(&[1, 2, 3]), TAU), 1);
        assert_eq!(t.record(fh, div_with(&[1, 2, 3]), TAU), 1, "same sketch → same cell");
    }

    #[test]
    fn fuzzy_similar_claims_count_once() {
        // Two sketches from the same physical cell overlap heavily → count once (R2).
        let mut t = TrustState::new();
        let fh = [2u8; 16];
        let a = div_with(&[10, 20, 30, 40, 50, 60, 70, 80]);
        let b = div_with(&[10, 20, 30, 40, 50, 60, 70, 81]); // one slot differs
        assert_eq!(t.record(fh, a, TAU), 1);
        assert_eq!(
            t.record(fh, b, TAU),
            1,
            "fuzzy-equal sketch must not inflate the distinct count"
        );
    }

    #[test]
    fn dissimilar_claims_count_separately() {
        let mut t = TrustState::new();
        let fh = [3u8; 16];
        let a = div_with(&[1, 2, 3, 4]);
        let b = div_with(&[101, 102, 103, 104]); // disjoint
        assert_eq!(t.record(fh, a, TAU), 1);
        assert_eq!(t.record(fh, b, TAU), 2, "genuinely different cells both count");
    }

    #[test]
    fn witnessless_and_empty_claims_never_count() {
        let mut t = TrustState::new();
        let fh = [4u8; 16];
        assert_eq!(t.record(fh, [0u8; 16], TAU), 0, "all-zero claim must not count");
        assert_eq!(t.record(fh, [0xFFu8; 16], TAU), 0, "all-0xFF claim must not count");
    }

    #[test]
    fn capacity_is_bounded() {
        let mut t = TrustState::new();
        for i in 0..(DEFAULT_CAP + 100) {
            let mut fh = [0u8; 16];
            fh[..4].copy_from_slice(&(i as u32).to_le_bytes());
            t.record(fh, div_with(&[1, 2, 3]), TAU);
        }
        assert!(
            t.verifications.len() <= DEFAULT_CAP,
            "trust state must stay within its cap"
        );
    }
}

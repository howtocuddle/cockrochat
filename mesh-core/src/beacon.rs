//! `beacon` — chained hash beacon (NO VDF in v0, audit F1).
//!
//! Forward-secrecy chain: seed_N = BLAKE3(seed_{N-1} || E_N) where E_N is a digest of
//! LocalImmediate marks overheard this epoch. A remote attacker cannot compute past seeds
//! even after seizing the device and extracting the current seed — the one-way hash chain
//! makes all prior seeds unrecoverable.
//!
//! Honesty note (R9): the on-device `floor_ms` throttle only paces THIS device's local
//! advance calls. It cannot slow an attacker recomputing a chain off-device at full BLAKE3
//! speed, and it is not an anti-grinding mechanism. Future-seed unpredictability rests on
//! entropy freshness: future marks are not yet observable, so future chain states are not
//! yet computable by anyone.
//!
//! Zero-entropy fallback when alone (low_entropy flag set).
//! See README.md §4.

use std::collections::BTreeSet;

/// A beacon step in the local hash chain.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Beacon {
    /// 32-byte chain seed (this epoch's entropy root).
    pub seed: [u8; 32],
    /// Monotonic epoch counter (not used for frame epoch — wall clock handles coordination).
    pub epoch: u32,
    /// Wall-clock ms of last advance (acceleration cap anchor).
    pub last_advance_ms: u64,
    /// True when the chain was advanced without external entropy (zero-entropy fallback).
    pub low_entropy: bool,
}

/// Local entropy gathered from LocalImmediate marks (must clear `min_hearers`).
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Entropy(pub [u8; 32]);

/// Create a fresh beacon chain from a 32-byte OS-random seed (seed0).
/// Epoch starts at 0, not in low-entropy mode.
pub fn new(seed0: &[u8; 32]) -> Beacon {
    Beacon {
        seed: *seed0,
        epoch: 0,
        last_advance_ms: 0,
        low_entropy: false,
    }
}

/// Derive entropy from LocalImmediate marks.
///
/// Only LocalImmediate frames (originated TTL=0, never relayed) are accepted — their marks
/// stay within a single radio cell and are unobservable to a remote van. Regional frames
/// arriving with TTL=1 have already traveled the relay path and are visible to every node
/// along it.
///
/// Returns `None` if fewer than `min_hearers` distinct marks are provided.
pub fn local_entropy(nonprop_marks: &[[u8; 16]], min_hearers: u32) -> Option<Entropy> {
    // Deduplicate via BTreeSet (deterministic ordering).
    let unique: BTreeSet<&[u8; 16]> = nonprop_marks.iter().collect();
    if (unique.len() as u32) < min_hearers {
        return None;
    }
    let mut hasher = blake3::Hasher::new();
    hasher.update(b"mesh-core:v1:beacon-entropy");
    for mark in &unique {
        hasher.update(&**mark);
    }
    let mut e = Entropy([0u8; 32]);
    e.0.copy_from_slice(&hasher.finalize().as_bytes()[..32]);
    Some(e)
}

/// Advance the chain with external entropy: seed_N = BLAKE3(seed_{N-1} || entropy).
///
/// Returns `None` if (now - last_advance) < floor_ms. The floor paces this device's
/// chain only; it is NOT an anti-grinding mechanism (off-device recomputation cannot
/// be throttled — R9). Unpredictability comes from entropy freshness, not the floor.
pub fn advance(prev: &Beacon, e: &Entropy, now_ms: u64, floor_ms: u64) -> Option<Beacon> {
    if now_ms.saturating_sub(prev.last_advance_ms) < floor_ms {
        return None;
    }
    Some(Beacon {
        seed: advance_seed(&prev.seed, &e.0),
        epoch: prev.epoch + 1,
        last_advance_ms: now_ms,
        low_entropy: false,
    })
}

/// Zero-entropy fallback: chain with all-zeros entropy block.
///
/// Used when alone (fewer than min_hearers LocalImmediate marks). Sets `low_entropy = true`
/// to signal "I cannot prove I am in a crowd." The chain still advances so marks rotate
/// and the device keeps working.
pub fn fallback_local(prev: &Beacon, now_ms: u64, floor_ms: u64) -> Option<Beacon> {
    // Respect floor even in fallback — prevents trivial fast-forward.
    if now_ms.saturating_sub(prev.last_advance_ms) < floor_ms {
        return None;
    }
    Some(Beacon {
        seed: advance_seed(&prev.seed, &[0u8; 32]),
        epoch: prev.epoch + 1,
        last_advance_ms: now_ms,
        low_entropy: true,
    })
}

/// Core chain step: seed' = BLAKE3("mesh-core:v1:beacon-advance" || prev || entropy).
fn advance_seed(prev_seed: &[u8; 32], entropy: &[u8; 32]) -> [u8; 32] {
    let mut hasher = blake3::Hasher::new();
    hasher.update(b"mesh-core:v1:beacon-advance");
    hasher.update(prev_seed);
    hasher.update(entropy);
    let mut seed = [0u8; 32];
    seed.copy_from_slice(&hasher.finalize().as_bytes()[..32]);
    seed
}

#[cfg(test)]
mod tests {
    use super::*;

    fn test_seed0() -> [u8; 32] {
        core::array::from_fn(|i| i as u8)
    }

    fn mark_at(idx: u8) -> [u8; 16] {
        core::array::from_fn(|i| idx.wrapping_add(i as u8))
    }

    #[test]
    fn chain_determinism() {
        let seed0 = test_seed0();
        let b = new(&seed0);
        assert_eq!(b.epoch, 0);
        assert!(!b.low_entropy);
    }

    #[test]
    fn advance_produces_different_seed() {
        let seed0 = test_seed0();
        let b0 = new(&seed0);
        let marks: [[u8; 16]; 3] = [mark_at(1), mark_at(2), mark_at(3)];
        let ent = local_entropy(&marks, 1).expect("3 >= 1");
        let b1 = advance(&b0, &ent, 100, 10).expect("100 >= 10 floor");
        assert_ne!(b1.seed, b0.seed);
        assert_eq!(b1.epoch, 1);
        assert!(!b1.low_entropy);
    }

    #[test]
    fn advance_respects_floor() {
        let seed0 = test_seed0();
        let b0 = Beacon { seed: seed0, epoch: 0, last_advance_ms: 1000, low_entropy: false };
        let marks: [[u8; 16]; 3] = [mark_at(1), mark_at(2), mark_at(3)];
        let ent = local_entropy(&marks, 1).expect("3 >= 1");
        // Only 9ms elapsed, floor is 10ms.
        assert!(advance(&b0, &ent, 1009, 10).is_none());
        // 10ms elapsed exactly.
        assert!(advance(&b0, &ent, 1010, 10).is_some());
        // 11ms elapsed.
        assert!(advance(&b0, &ent, 1011, 10).is_some());
    }

    #[test]
    fn floor_uses_saturating_sub() {
        let seed0 = test_seed0();
        // last_advance_ms > now_ms (clock skew) — saturating_sub returns 0, which is < floor.
        let b0 = Beacon { seed: seed0, epoch: 0, last_advance_ms: 2000, low_entropy: false };
        let marks: [[u8; 16]; 3] = [mark_at(1), mark_at(2), mark_at(3)];
        let ent = local_entropy(&marks, 1).expect("3 >= 1");
        assert!(advance(&b0, &ent, 1000, 10).is_none());
    }

    #[test]
    fn one_way_chain() {
        let seed0 = test_seed0();
        let b0 = new(&seed0);
        let mut b = b0;
        for i in 1..=10 {
            let marks: [[u8; 16]; 3] = [mark_at(i), mark_at(i + 1), mark_at(i + 2)];
            let ent = local_entropy(&marks, 1).expect("3 >= 1");
            b = advance(&b, &ent, (i as u64) * 100, 10).expect("floor ok");
        }
        // After 10 advances, seed is different from seed0 and from any intermediate.
        assert_ne!(b.seed, seed0);
        assert_eq!(b.epoch, 10);
        // Cannot reverse: BLAKE3 is one-way.
    }

    #[test]
    fn entropy_requires_min_hearers() {
        let marks: [[u8; 16]; 2] = [mark_at(1), mark_at(2)];
        assert!(local_entropy(&marks, 3).is_none()); // 2 < 3
        assert!(local_entropy(&marks, 2).is_some()); // 2 == 2
        assert!(local_entropy(&marks, 1).is_some()); // 2 > 1
    }

    #[test]
    fn entropy_deduplicates() {
        // Same mark twice — should count as 1 distinct.
        let marks = [mark_at(1), mark_at(1), mark_at(2)];
        let ent = local_entropy(&marks, 2).expect("2 distinct >= 2");
        assert!(ent.0.iter().any(|&b| b != 0));
    }

    #[test]
    fn entropy_different_marks_different_entropy() {
        let marks_a = [mark_at(1), mark_at(2), mark_at(3)];
        let marks_b = [mark_at(4), mark_at(5), mark_at(6)];
        let ea = local_entropy(&marks_a, 1).expect("3 >= 1");
        let eb = local_entropy(&marks_b, 1).expect("3 >= 1");
        assert_ne!(ea.0, eb.0);
    }

    #[test]
    fn entropy_order_independent() {
        // Same set of marks in different order → same entropy (BTreeSet sorts).
        let marks_a = [mark_at(3), mark_at(1), mark_at(2)];
        let marks_b = [mark_at(1), mark_at(2), mark_at(3)];
        let ea = local_entropy(&marks_a, 1).expect("3 >= 1");
        let eb = local_entropy(&marks_b, 1).expect("3 >= 1");
        assert_eq!(ea.0, eb.0);
    }

    #[test]
    fn fallback_sets_low_entropy() {
        let seed0 = test_seed0();
        let b0 = new(&seed0);
        let b1 = fallback_local(&b0, 100, 10).expect("100 >= 10 floor");
        assert!(b1.low_entropy);
        assert_eq!(b1.epoch, 1);
        assert_ne!(b1.seed, b0.seed);
    }

    #[test]
    fn fallback_respects_floor() {
        let seed0 = test_seed0();
        let b0 = Beacon { seed: seed0, epoch: 0, last_advance_ms: 1000, low_entropy: false };
        assert!(fallback_local(&b0, 1005, 10).is_none());
    }

    #[test]
    fn chain_kat() {
        // Known-answer test: verify deterministic output for a fixed seed + entropy.
        let seed0 = [0u8; 32];
        let b0 = new(&seed0);
        let marks = [mark_at(1), mark_at(2), mark_at(3)];
        let ent = local_entropy(&marks, 1).expect("3 >= 1");
        let b1 = advance(&b0, &ent, 100, 10).expect("floor ok");

        // Independently computed: blake3("mesh-core:v1:beacon-advance" || 0^32 || entropy)
        let expected_hex = {
            let mut h = blake3::Hasher::new();
            h.update(b"mesh-core:v1:beacon-advance");
            h.update(&seed0);
            h.update(&ent.0);
            h.finalize()
        };
        assert_eq!(b1.seed, expected_hex.as_bytes()[..32]);
    }

    #[test]
    fn advance_updates_last_advance_ms() {
        let seed0 = test_seed0();
        let b0 = new(&seed0);
        let marks: [[u8; 16]; 3] = [mark_at(1), mark_at(2), mark_at(3)];
        let ent = local_entropy(&marks, 1).expect("3 >= 1");
        let b1 = advance(&b0, &ent, 5000, 10).expect("floor ok");
        assert_eq!(b1.last_advance_ms, 5000);
    }
}

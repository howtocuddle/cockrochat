use mesh_core::pocp::{CellSketch, KMV_K, jaccard, matches, observe};
use proptest::prelude::*;

// ---------------------------------------------------------------------------
// helpers
// ---------------------------------------------------------------------------

fn mark16(v: u8) -> [u8; 16] {
    [v; 16]
}

fn above_floor() -> i8 {
    -30i8
}

fn floor() -> i8 {
    -50i8
}

// ---------------------------------------------------------------------------
// proptest suite
// ---------------------------------------------------------------------------

prop_compose! {
    fn arb_marks_rssi(max: usize)
        (n in 0usize..=max)
        (marks in proptest::collection::vec(
             proptest::array::uniform16(any::<u8>()),
             n..=n),
         rssi in proptest::collection::vec(any::<i8>(), n..=n))
    -> (Vec<[u8;16]>, Vec<i8>) {
        (marks, rssi)
    }
}

proptest! {
    #[test]
    fn observe_is_deterministic(
        (marks, rssi) in arb_marks_rssi(40),
        seed: u32,
        floor: i8,
    ) {
        let a = observe(&marks, &rssi, seed, floor);
        let b = observe(&marks, &rssi, seed, floor);
        prop_assert_eq!(a, b);
    }

    #[test]
    fn observe_sketch_is_sorted_ascending(
        (marks, rssi) in arb_marks_rssi(40),
        seed: u32,
        floor: i8,
    ) {
        let s = observe(&marks, &rssi, seed, floor);
        for w in s.0.windows(2) {
            prop_assert!(w[0] <= w[1], "not sorted: {} > {}", w[0], w[1]);
        }
    }

    #[test]
    fn jaccard_self_is_one_when_nonempty(
        (marks, _rssi) in arb_marks_rssi(40),
        seed: u32,
    ) {
        // force rssi above floor so we get non-empty sketches (when marks non-empty)
        let rssi: Vec<i8> = vec![-10i8; marks.len()];
        let s = observe(&marks, &rssi, seed, i8::MIN);
        // only check if non-empty
        let nonempty = s.0.iter().any(|v| *v != u64::MAX);
        if nonempty {
            prop_assert_eq!(jaccard(&s, &s), 1.0f32);
        }
    }

    #[test]
    fn jaccard_symmetric(
        (marks_a, rssi_a) in arb_marks_rssi(40),
        (marks_b, rssi_b) in arb_marks_rssi(40),
        seed: u32,
        floor: i8,
    ) {
        let a = observe(&marks_a, &rssi_a, seed, floor);
        let b = observe(&marks_b, &rssi_b, seed, floor);
        prop_assert_eq!(jaccard(&a, &b), jaccard(&b, &a));
    }

    #[test]
    fn jaccard_in_unit_range(
        (marks_a, rssi_a) in arb_marks_rssi(40),
        (marks_b, rssi_b) in arb_marks_rssi(40),
        seed: u32,
        floor: i8,
    ) {
        let a = observe(&marks_a, &rssi_a, seed, floor);
        let b = observe(&marks_b, &rssi_b, seed, floor);
        let j = jaccard(&a, &b);
        prop_assert!((0.0..=1.0).contains(&j), "out of range: {}", j);
    }

    #[test]
    fn rssi_floor_filters(
        marks in proptest::collection::vec(
            proptest::array::uniform16(any::<u8>()),
            1usize..=40),
        seed: u32,
    ) {
        let rssi: Vec<i8> = vec![-100i8; marks.len()];
        let s = observe(&marks, &rssi, seed, -50i8);
        let empty = CellSketch([u64::MAX; 16]);
        prop_assert_eq!(s, empty);
    }
}

// ---------------------------------------------------------------------------
// unit tests
// ---------------------------------------------------------------------------

#[test]
fn identical_mark_sets_match() {
    let marks: Vec<[u8; 16]> = (0u8..20).map(mark16).collect();
    let rssi: Vec<i8> = vec![above_floor(); marks.len()];
    let seed = 42u32;
    let fl = floor();
    let a = observe(&marks, &rssi, seed, fl);
    let b = observe(&marks, &rssi, seed, fl);
    assert_eq!(jaccard(&a, &b), 1.0f32);
    assert!(matches(&a, &b, 0.9));
}

#[test]
fn disjoint_mark_sets_dont_match() {
    // A: marks with low byte pattern
    let marks_a: Vec<[u8; 16]> = (0u8..10).map(mark16).collect();
    // B: marks with high byte pattern — all bytes 0x80..0x89, distinct from A
    let marks_b: Vec<[u8; 16]> = (0x80u8..0x8a).map(mark16).collect();
    let rssi_a: Vec<i8> = vec![above_floor(); marks_a.len()];
    let rssi_b: Vec<i8> = vec![above_floor(); marks_b.len()];
    let seed = 99u32;
    let fl = floor();
    let a = observe(&marks_a, &rssi_a, seed, fl);
    let b = observe(&marks_b, &rssi_b, seed, fl);
    assert_eq!(
        jaccard(&a, &b),
        0.0f32,
        "expected 0 jaccard for disjoint sets"
    );
    assert!(!matches(&a, &b, 0.5));
}

#[test]
fn seed_changes_sketch() {
    let marks: Vec<[u8; 16]> = (0u8..20).map(mark16).collect();
    let rssi: Vec<i8> = vec![above_floor(); marks.len()];
    let fl = floor();
    let s1 = observe(&marks, &rssi, 1u32, fl);
    let s2 = observe(&marks, &rssi, 2u32, fl);
    assert_ne!(s1, s2, "different seeds should produce different sketches");
}

#[test]
fn empty_marks_returns_empty_sketch() {
    let s = observe(&[], &[], 0u32, floor());
    assert_eq!(s, CellSketch([u64::MAX; 16]));
}

#[test]
fn kmv_k_constant_is_16() {
    assert_eq!(KMV_K, 16);
}

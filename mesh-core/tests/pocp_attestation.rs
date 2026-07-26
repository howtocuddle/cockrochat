//! Regression guards for the PoCP count floor.
//!
//! The forgery table in the `pocp` module header was a one-off measurement with nothing
//! holding it in place. These tests pin both halves of the defect it describes: the
//! single-byte grind that reached full trust against small cells, and the honest
//! just-started phone whose claim was discarded by large ones.

use mesh_core::pocp::{
    CellSketch, MIN_ATTESTING_OVERLAP, WitVerdict, jaccard_with_overlap, observe,
    sketch_to_div_sketch, verify_witness_local, witness,
};

const TAU: f32 = 0.3;
const SEED: u32 = 0xC0FFEE;
const PREFIX: &[u8] = b"frame prefix bytes, 102 of them in the real thing";

fn mark16(v: u8) -> [u8; 16] {
    [v; 16]
}

/// A cell of `n` devices, all heard above the RSSI floor.
fn cell_of(n: usize) -> CellSketch {
    let marks: Vec<[u8; 16]> = (1..=n).map(|i| mark16(i as u8)).collect();
    let rssi = vec![-30i8; n];
    observe(&marks, &rssi, SEED, -50)
}

/// The cheapest possible forgery: claim exactly one element, pad the rest.
fn single_element_claim(byte: u8) -> [u8; 16] {
    let mut div = [0xFFu8; 16];
    div[0] = byte;
    div
}

/// Distinct wire bytes a cell actually occupies — the targets a sweep can hit.
fn occupied_bytes(cell: &CellSketch) -> Vec<u8> {
    let mut v: Vec<u8> = sketch_to_div_sketch(cell)
        .into_iter()
        .filter(|b| *b != 0xFF)
        .collect();
    v.sort_unstable();
    v.dedup();
    v
}

/// S3: sweep all 256 single-element claims against cells of every interesting size and
/// confirm none reaches `Valid`. This used to land 2–3 accepted forgeries at cell sizes 2–3,
/// which are exactly the small clandestine cells worth attacking.
#[test]
fn single_byte_sweep_never_reaches_full_trust() {
    for n in 2..=8usize {
        let local = cell_of(n);
        let mut valid = 0;
        let mut unattested = 0;
        for byte in 0..=255u8 {
            let claim = single_element_claim(byte);
            let wit = witness(&claim, SEED, PREFIX);
            match verify_witness_local(&local, &claim, SEED, PREFIX, &wit, TAU) {
                WitVerdict::Valid => valid += 1,
                WitVerdict::Unattested => unattested += 1,
                _ => {}
            }
        }
        assert_eq!(valid, 0, "cell size {n}: {valid} single-byte forgeries reached Valid");
        // The hits are not gone, they are DEMOTED — one per byte the cell occupies. Asserting
        // this keeps the test honest: it proves the sweep still lands, just not at full trust.
        assert_eq!(
            unattested,
            occupied_bytes(&local).len(),
            "cell size {n}: demoted-hit count should equal the cell's occupied byte count"
        );
    }
}

/// C8: a phone whose scanner has only just started claims a sketch holding nothing but its
/// own mark. Every established peer scored that `1/N` and, below tau, dropped its LOCAL
/// alerts outright — worst in a growing crowd. It must now be displayable-but-unverified.
#[test]
fn honest_cold_start_is_unattested_not_dropped() {
    // 4 is the first size where 1/N falls under tau=0.3 and the old code dropped the frame.
    for n in 4..=12usize {
        let established = cell_of(n);
        // The newcomer is device 1, and has heard only itself.
        let alone = observe(&[mark16(1)], &[-30i8], SEED, -50);
        let claim = sketch_to_div_sketch(&alone);
        let wit = witness(&claim, SEED, PREFIX);
        assert_eq!(
            verify_witness_local(&established, &claim, SEED, PREFIX, &wit, TAU),
            WitVerdict::Unattested,
            "cell size {n}: an honest cold-start claim must be shown, not discarded"
        );
    }
}

/// The floor's lower bound: two phones that have heard each other overlap on exactly
/// MIN_ATTESTING_OVERLAP elements. Raising the floor would make this topology — the smallest
/// real one — permanently unattestable.
#[test]
fn two_device_mesh_still_attests() {
    let a = observe(&[mark16(1), mark16(2)], &[-30i8, -30i8], SEED, -50);
    let b = observe(&[mark16(2), mark16(1)], &[-30i8, -30i8], SEED, -50);
    let (_, overlap) = jaccard_with_overlap(&a, &b);
    assert_eq!(overlap, MIN_ATTESTING_OVERLAP);

    let claim = sketch_to_div_sketch(&b);
    let wit = witness(&claim, SEED, PREFIX);
    assert_eq!(
        verify_witness_local(&a, &claim, SEED, PREFIX, &wit, TAU),
        WitVerdict::Valid
    );
}

/// A cell with nothing in common is still a hard mismatch — the remote-van case PoCP exists
/// to stop. The floor must not have softened this into "unattested".
#[test]
fn disjoint_cells_remain_a_mismatch() {
    let local = cell_of(4);
    let far_away = observe(
        &[mark16(200), mark16(201), mark16(202)],
        &[-30i8, -30i8, -30i8],
        SEED,
        -50,
    );
    let claim = sketch_to_div_sketch(&far_away);
    let wit = witness(&claim, SEED, PREFIX);
    assert_eq!(
        verify_witness_local(&local, &claim, SEED, PREFIX, &wit, TAU),
        WitVerdict::CellMismatch
    );
}

/// A bad MAC still outranks everything: no overlap count rescues an unverifiable witness.
#[test]
fn bad_witness_is_stale_regardless_of_overlap() {
    let local = cell_of(6);
    let claim = sketch_to_div_sketch(&local); // perfect overlap
    let wit = [0u8; 16]; // but a garbage MAC
    assert_eq!(
        verify_witness_local(&local, &claim, SEED, PREFIX, &wit, TAU),
        WitVerdict::Stale
    );
}

/// The ratio and the count must come from the same window — a caller comparing them against
/// each other should never see them disagree.
#[test]
fn ratio_and_overlap_are_consistent() {
    for n in 1..=16usize {
        let cell = cell_of(n);
        let (sim, overlap) = jaccard_with_overlap(&cell, &cell);
        assert!((sim - 1.0).abs() < f32::EPSILON, "self-similarity must be 1.0");
        assert_eq!(overlap, occupied_bytes(&cell).len().min(16));
    }
}

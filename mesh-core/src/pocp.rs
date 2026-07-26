//! `pocp` — Proof-of-Co-Presence. Cell digest (fuzzy KMV over overheard marks) + spacetime
//! witness. Blocks the remote-van flood: you cannot forge co-presence you did not physically
//! observe. See README.md §4. `tau` is MEASURED (RF-overlap rig), never guessed.
//!
//! # KNOWN LIMIT: no protection for cells smaller than 4 marks
//!
//! [`witness_key`] is derived from `div_sketch || seed`, both public, so the MAC is
//! anti-malleability only — anyone can compute a valid witness for any sketch they care to
//! claim (documented at [`witness`]). The entire co-presence guarantee therefore rests on
//! [`jaccard`] clearing `tau`, and that is a RATIO with no absolute-count floor: a claim of
//! a single element scores `1/N` against an N-mark local cell, which clears `tau = 0.3`
//! whenever `N <= 3`.
//!
//! The claimed element is one wire byte. So an attacker who has never been anywhere near
//! the cell can enumerate all 256 single-element sketches, compute a valid witness for each
//! for free, and land roughly 2–3 accepted forgeries against any 2–3 device cell —
//! measured, over 200 random cells per size:
//!
//! ```text
//! cell size | accepted forgeries per 256-frame sweep
//!         2 | 2.00
//!         3 | 2.98
//!         4 | 0.17
//!         5 | 0.01
//!        8+ | 0.00
//! ```
//!
//! Large crowds are protected as designed; small clandestine cells are not, and those are
//! the higher-value target.
//!
//! # RESOLVED: the count floor, and the honest failure that blocked it
//!
//! The note above used to end by saying a count floor (`intersection >= 2`) would close the
//! grind but would also reject the 1-element sketch a cold-started phone legitimately has,
//! so the two had to be redesigned together. They have been.
//!
//! The reason they are one problem: the ratio has no absolute floor, so a 1-element claim
//! scores `1/N` — which CLEARS `tau` for `N <= 3` (the forgery above) and MISSES it for
//! `N >= 4` (an honest phone whose scanner just started, having its LOCAL alerts dropped by
//! every established peer, worst exactly in a growing crowd). Same cause, opposite symptoms,
//! disjoint cell sizes.
//!
//! The fix is to stop conflating "attested" with "displayable". A one-element overlap now
//! returns [`WitVerdict::Unattested`] instead of being scored: callers DISPLAY it and mark it
//! unverified, rather than either trusting it or discarding it. `Valid` requires
//! [`MIN_ATTESTING_OVERLAP`]. No single-byte sweep reaches full trust at any cell size, and
//! the honest cold start is no longer silently dropped.
//!
//! Callers must still degrade the badge when their own cell holds fewer than 4 marks — a
//! verified witness judged against a tiny local cell is weak evidence regardless of overlap.
//! See [`verify_witness_local`] for what remains open (a two-byte grind against very small
//! cells) and why the floor cannot simply be raised.

use std::collections::BTreeSet;

pub const KMV_K: usize = 16;

/// KMV sketch over truncated overheard marks within an RSSI window — one physical "cell".
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct CellSketch(pub [u64; 16]);

/// Result of checking a received witness against the locally-observed cell.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum WitVerdict {
    Valid,
    /// Cell does not match local observation => relocation/replay attack. Raise F4 ALARM.
    CellMismatch,
    Stale,
    /// The witness verifies and the cells touch, but on exactly ONE element — the weakest
    /// possible overlap, and the one an attacker can reach by grinding a single wire byte.
    /// Not evidence of co-presence, but not evidence against it either: it is also what an
    /// honest phone that has only just started scanning legitimately claims. Callers must
    /// DISPLAY these and mark them unverified; dropping them is what broke cold-start LOCAL
    /// delivery in a crowd. See [`MIN_ATTESTING_OVERLAP`].
    Unattested,
}

/// Overlapping elements required before a verified witness counts as co-presence evidence.
///
/// Two, because that is the largest floor a two-device mesh can satisfy: A holds
/// `{mark_A, mark_B}` and B holds `{mark_B, mark_A}`, so honest overlap is exactly 2 there.
/// Three would make the smallest real topology permanently unattestable.
///
/// This is the count floor the module header asks for, and it resolves both sides of the
/// same defect at once — see that header for the measured forgery table.
pub const MIN_ATTESTING_OVERLAP: usize = 2;

fn cell_key(seed: u32) -> [u8; 32] {
    blake3::derive_key("mesh-core:v1:pocp-cell", &seed.to_le_bytes())
}

fn mark_hash(key: &[u8; 32], mark: &[u8; 16]) -> u64 {
    let h = blake3::keyed_hash(key, mark);
    let b = h.as_bytes();
    u64::from_le_bytes(b[0..8].try_into().unwrap())
}

/// Build a cell sketch from marks overheard this epoch, RSSI-windowed and seed-bound.
pub fn observe(marks: &[[u8; 16]], rssi: &[i8], seed: u32, rssi_floor_dbm: i8) -> CellSketch {
    let key = cell_key(seed);
    let mut set: BTreeSet<u64> = BTreeSet::new();
    for (mark, r) in marks.iter().zip(rssi.iter()) {
        if *r < rssi_floor_dbm {
            continue;
        }
        set.insert(mark_hash(&key, mark));
    }
    let mut arr = [u64::MAX; 16];
    for (i, v) in set.iter().take(KMV_K).enumerate() {
        arr[i] = *v;
    }
    CellSketch(arr)
}

/// Jaccard similarity of two cell sketches in [0,1], with the overlap count it came from.
///
/// The count is the numerator of the ratio, taken over the same KMV window — never a
/// separately computed intersection, so the two can never disagree. For cells larger than
/// [`KMV_K`] it is an ESTIMATE (the window caps it at 16), which is fine for its only
/// purpose: distinguishing "one element in common" from "genuinely overlapping cells".
/// An honest large cell lands around `KMV_K * tau` ≈ 4.8 at `tau = 0.3`, comfortably clear.
pub fn jaccard_with_overlap(a: &CellSketch, b: &CellSketch) -> (f32, usize) {
    let set_a: BTreeSet<u64> = a.0.iter().copied().filter(|v| *v != u64::MAX).collect();
    let set_b: BTreeSet<u64> = b.0.iter().copied().filter(|v| *v != u64::MAX).collect();
    let union: Vec<u64> = set_a.union(&set_b).copied().collect();
    if union.is_empty() {
        return (0.0, 0);
    }
    let x_len = KMV_K.min(union.len());
    let x = &union[..x_len];
    let inter_in_x = x
        .iter()
        .filter(|v| set_a.contains(v) && set_b.contains(v))
        .count();
    (inter_in_x as f32 / x_len as f32, inter_in_x)
}

/// Jaccard similarity of two cell sketches in [0,1].
pub fn jaccard(a: &CellSketch, b: &CellSketch) -> f32 {
    jaccard_with_overlap(a, b).0
}

/// Fuzzy cell match at measured threshold `tau`.
pub fn matches(a: &CellSketch, b: &CellSketch, tau: f32) -> bool {
    jaccard(a, b) >= tau
}

// ---- div_sketch helpers ----

/// Truncate a `CellSketch` to 16 bytes for the wire `div_sketch` field.
/// Takes the low byte of each u64 slot. High bytes are discarded.
///
/// A real slot whose low byte is 0xFF is remapped to 0xFE, because 0xFF is also the KMV
/// padding sentinel that [`div_sketch_to_cell`] maps back to `u64::MAX` and `jaccard`
/// filters out. Without the remap that element simply vanished from the comparison — and
/// for a lone sender, whose sketch holds only its own mark, the whole wire sketch became
/// `[0xFF; 16]`, the "empty cell" encoding. Every receiver then scored Jaccard 0.0 and
/// dropped the frame, so 1 in 256 epochs a solitary originator was undisplayable
/// mesh-wide. Marks rotate per epoch, so it self-healed — but silently, and only next epoch.
///
/// The cost is a 0xFE/0xFF aliasing collision at the same ~1/256 per-slot rate, which
/// merely adds a false MATCH between two honest sketches. Trading a hard availability
/// failure for a marginal similarity false-positive is the safe direction.
pub fn sketch_to_div_sketch(sketch: &CellSketch) -> [u8; 16] {
    let mut out = [0u8; 16];
    for (i, slot) in sketch.0.iter().enumerate() {
        out[i] = if *slot == u64::MAX {
            0xFF // genuinely an empty KMV slot
        } else {
            let b = *slot as u8;
            if b == 0xFF { 0xFE } else { b }
        };
    }
    out
}

/// Convert a wire `div_sketch` (16 u8 low-byte values) back to a `CellSketch`.
///
/// 0xFF bytes are treated as KMV-padding empty slots and mapped to `u64::MAX`
/// so the existing `jaccard` filter (`v != u64::MAX`) correctly discards them.
/// Without this, a remote van's all-0xFF empty sketch would match any empty local
/// sketch at Jaccard 1.0 — the exact bypass PoCP was built to prevent.
pub fn div_sketch_to_cell(div: &[u8; 16]) -> CellSketch {
    let mut arr = [0u64; 16];
    for (i, &b) in div.iter().enumerate() {
        arr[i] = if b == 0xFF { u64::MAX } else { b as u64 };
    }
    CellSketch(arr)
}

// ---- witness MAC ----

/// Domain-separated key derivation for the PoCP witness MAC.
/// key = blake3::derive_key("mesh-core:v1:pocp-wit", div_sketch || seed_le)
fn witness_key(div_sketch: &[u8; 16], seed: u32) -> [u8; 32] {
    let mut material = [0u8; 20];
    material[..16].copy_from_slice(div_sketch);
    material[16..].copy_from_slice(&seed.to_le_bytes());
    blake3::derive_key("mesh-core:v1:pocp-wit", &material)
}

/// Spacetime witness: `MAC_{KDF(div_sketch || epoch)}(frame_prefix)`.
///
/// `frame_prefix` is the first 102 bytes of the unsigned frame (everything before
/// the `pocp_wit` field at bytes 102..118). Returns the 16-byte witness to place
/// at `pocp_wit` before signing.
///
/// SECURITY PROPERTIES — read carefully (R1):
///   * The MAC key is derived from PUBLIC values (the claimed `div_sketch` and the
///     epoch index). Anyone can recompute it. The MAC therefore provides
///     ANTI-MALLEABILITY ONLY: it binds the div_sketch to this exact frame prefix,
///     so a relay cannot swap or perturb the sketch on an existing frame without
///     invalidating the witness.
///   * Co-presence evidence comes from the Jaccard gate in `verify_witness_local`:
///     the claimed sketch must overlap the verifier's own KMV sketch of marks it
///     actually heard over the air. A remote party that never observed the cell's
///     current marks cannot fabricate an overlapping sketch.
///   * RESIDUAL GAP: within one epoch, an attacker can copy the div_sketch truncation
///     broadcast by another frame from the same cell and claim it as its own (the
///     truncation is public by design). Mitigations live outside this function:
///     shim-side same-epoch sketch-reuse detection across distinct sender marks, and
///     `trust` pairwise-dissimilarity counting. Fully unforgeable co-presence would
///     require fuzzy-extractor / secure-sketch keying of the MAC (deferred, M6+).
pub fn witness(div_sketch: &[u8; 16], seed: u32, frame_prefix: &[u8]) -> [u8; 16] {
    let key = witness_key(div_sketch, seed);
    let mac = blake3::keyed_hash(&key, frame_prefix);
    let mut out = [0u8; 16];
    out.copy_from_slice(&mac.as_bytes()[..16]);
    out
}

/// Verify a received witness against a claimed `div_sketch`.
/// Returns `true` if the MAC is valid (sender knew this sketch at this epoch),
/// `false` otherwise.
pub fn verify_witness(
    div_sketch: &[u8; 16],
    seed: u32,
    frame_prefix: &[u8],
    wit: &[u8; 16],
) -> bool {
    let expected = witness(div_sketch, seed, frame_prefix);
    // constant-time comparison to avoid timing side-channels
    let mut acc = 0u8;
    for (a, b) in expected.iter().zip(wit.iter()) {
        acc |= a ^ b;
    }
    acc == 0
}

/// Verify a received witness AND check co-presence against the local cell sketch.
///
/// Processing order:
///   1. Verify the witness MAC — did the sender know this `claimed_div` sketch?
///   2. Truncate the local sketch to u8, convert both to `CellSketch`.
///   3. Compute Jaccard AND the overlap count between the two u8-truncated sketches.
///   4. Decide on both, not on the ratio alone (see below).
///
/// The ratio alone was the whole co-presence test, and being a ratio it had no absolute
/// floor: a claim of one element scores `1/N` against an N-mark local cell. That single
/// property produced two opposite failures, which is why they are fixed together —
///
///   * FORGERY (small cells): `1/N >= tau` for `N <= 3`, and the claimed element is one wire
///     byte, so 256 frames sweep the whole space and land 2–3 accepted forgeries against the
///     small clandestine cells that are the highest-value target.
///   * HONEST LOSS (large cells): `1/N < tau` for `N >= 4`, so a phone whose scanner has only
///     just started — its sketch holding nothing but its own mark — was judged CellMismatch
///     by every established peer and had its LOCAL alerts dropped outright. Worst exactly in
///     a growing crowd, and worst in the first epoch after starting.
///
/// Requiring [`MIN_ATTESTING_OVERLAP`] for `Valid` closes the forgery, and routing a
/// one-element overlap to [`WitVerdict::Unattested`] rather than `CellMismatch` stops
/// discarding the honest claim. A one-element sweep can no longer reach full trust at ANY
/// cell size.
///
/// RESIDUAL, stated precisely: an attacker who grinds TWO colliding bytes still reaches
/// `Valid` against cells of about six or fewer (both elements must land in the victim's
/// cell, and `2/union >= tau` bounds the union). That costs on the order of `(256/N)^2`
/// signed frames instead of 256, and the floor cannot be raised past 2 without making
/// two-device meshes unattestable. Closing it properly needs the witness MAC keyed by a
/// fuzzy extractor over the full u64 sketch (deferred, M6+), which also closes the
/// unrelated and larger hole of copying a relayed sketch wholesale.
///
/// `claimed_div` comes from the frame's `div_sketch` field (bytes 18..34).
/// `frame_prefix` is the first 102 bytes of the frame (bytes 0..102).
pub fn verify_witness_local(
    local: &CellSketch,
    claimed_div: &[u8; 16],
    seed: u32,
    frame_prefix: &[u8],
    wit: &[u8; 16],
    tau: f32,
) -> WitVerdict {
    if !verify_witness(claimed_div, seed, frame_prefix, wit) {
        return WitVerdict::Stale;
    }
    // Jaccard on u8-truncated sketches: both sides truncated before comparison.
    let local_div = sketch_to_div_sketch(local);
    let local_cell = div_sketch_to_cell(&local_div);
    let sender_cell = div_sketch_to_cell(claimed_div);
    let (sim, overlap) = jaccard_with_overlap(&local_cell, &sender_cell);
    match overlap {
        // Nothing in common: the sender was not where it claims. Unchanged behaviour.
        0 => WitVerdict::CellMismatch,
        // The weakest possible overlap. Ambiguous by construction — forgeable AND honest.
        1 => WitVerdict::Unattested,
        _ if sim >= tau => WitVerdict::Valid,
        // Real but partial overlap that misses tau: two different cells that happen to share
        // a few marks. Still a mismatch.
        _ => WitVerdict::CellMismatch,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    /// Build a cell sketch with deterministic u64 values for testing.
    fn test_sketch(values: &[u64]) -> CellSketch {
        let mut arr = [u64::MAX; 16];
        for (i, &v) in values.iter().enumerate() {
            if i < 16 {
                arr[i] = v;
            }
        }
        CellSketch(arr)
    }

    // ---- div_sketch round-trip ----

    #[test]
    fn sketch_to_div_sketch_preserves_low_byte() {
        let sketch = test_sketch(&[0xDEADBEEF00000042, 0xCAFE0000000000FF]);
        let div = sketch_to_div_sketch(&sketch);
        assert_eq!(div[0], 0x42);
        // A REAL element with low byte 0xFF is remapped to 0xFE so it cannot be mistaken
        // for the KMV padding sentinel and silently dropped from the comparison.
        assert_eq!(div[1], 0xFE);
        // remaining slots are genuinely empty and stay 0xFF
        for (slot, val) in div.iter().enumerate().skip(2) {
            assert_eq!(*val, 0xFF, "slot {slot}: empty KMV slot encodes as 0xFF");
        }
    }

    #[test]
    fn div_sketch_to_cell_zero_extends() {
        let div: [u8; 16] = [
            0x42, 0xFF, 0x00, 0x7F, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        ];
        let cell = div_sketch_to_cell(&div);
        assert_eq!(cell.0[0], 0x42u64);
        assert_eq!(cell.0[1], u64::MAX, "0xFF → u64::MAX (padding sentinel)");
        assert_eq!(cell.0[2], 0x00u64);
        assert_eq!(cell.0[3], 0x7Fu64);
        for i in 4..16 {
            assert_eq!(cell.0[i], 0u64, "slot {i} must be zero");
        }
    }

    #[test]
    fn empty_div_sketch_does_not_match_another_empty() {
        // The remote-van bypass: an all-0xFF div_sketch (empty cell) must NOT
        // produce Jaccard 1.0 against any other sketch — empty + empty ≠ match.
        let empty_div = [0xFFu8; 16];
        let empty_cell = div_sketch_to_cell(&empty_div);
        // All slots → u64::MAX, which jaccard filters → empty set → Jaccard 0.
        let one_mark = test_sketch(&[0x0000000000000042]);
        let div_one = sketch_to_div_sketch(&one_mark);
        let cell_one = div_sketch_to_cell(&div_one);
        assert_eq!(jaccard(&empty_cell, &cell_one), 0.0,
            "empty div_sketch must not match a single-mark cell");
        assert_eq!(jaccard(&empty_cell, &empty_cell), 0.0,
            "two empty div_sketches must not match each other");
    }

    #[test]
    fn div_sketch_roundtrip_via_cell_truncation() {
        // Two sketches with known overlap on low bytes
        let a = test_sketch(&[0x0100, 0x0200, 0x0300, 0x0400, 0x0500, 0x0600, 0x0700, 0x0800]);
        let b = test_sketch(&[0x0101, 0x0201, 0x0301, 0x0401, 0x0501, 0x0601, 0x0701, 0x0801]);
        // Different high bytes, same low bytes → truncated sketches identical → Jaccard = 1.0
        let _div_a = sketch_to_div_sketch(&a);
        let _div_b = sketch_to_div_sketch(&b);
        // a and b have different low bytes (0x00 vs 0x01) → different truncated sketches.
        // Use values with same low byte below for the real roundtrip test.
        let c = test_sketch(&[0x0000000000000042, 0x00000000000000FF]);
        let d = test_sketch(&[0xDEADBEEF00000042, 0xCAFEBABE000000FF]);
        let div_c = sketch_to_div_sketch(&c);
        let div_d = sketch_to_div_sketch(&d);
        assert_eq!(div_c[0], 0x42);
        assert_eq!(div_d[0], 0x42);
        // Real elements ending in 0xFF remap to 0xFE — consistently on both sides, so two
        // honest sketches still agree, which is what this round-trip test is checking.
        assert_eq!(div_c[1], 0xFE);
        assert_eq!(div_d[1], 0xFE);
    }

    // ---- witness MAC ----

    #[test]
    fn witness_deterministic() {
        let div: [u8; 16] = [0xAA; 16];
        let seed = 42u32;
        let prefix = b"hello world test prefix data";
        let w1 = witness(&div, seed, prefix);
        let w2 = witness(&div, seed, prefix);
        assert_eq!(w1, w2, "witness must be deterministic");
    }

    #[test]
    fn witness_changes_with_div_sketch() {
        let div_a = [0xAA; 16];
        let div_b = [0xBB; 16];
        let seed = 1u32;
        let prefix = b"test";
        let wa = witness(&div_a, seed, prefix);
        let wb = witness(&div_b, seed, prefix);
        assert_ne!(wa, wb, "different div_sketch → different witness");
    }

    #[test]
    fn witness_changes_with_seed() {
        let div = [0x42; 16];
        let prefix = b"test";
        let w1 = witness(&div, 1, prefix);
        let w2 = witness(&div, 2, prefix);
        assert_ne!(w1, w2, "different seed → different witness");
    }

    #[test]
    fn witness_changes_with_prefix() {
        let div = [0x77; 16];
        let seed = 5u32;
        let w1 = witness(&div, seed, b"prefix A");
        let w2 = witness(&div, seed, b"prefix B");
        assert_ne!(w1, w2, "different prefix → different witness");
    }

    #[test]
    fn verify_witness_accepts_valid() {
        let div = [0x11; 16];
        let seed = 100u32;
        let prefix = b"valid test prefix";
        let wit = witness(&div, seed, prefix);
        assert!(verify_witness(&div, seed, prefix, &wit));
    }

    #[test]
    fn verify_witness_rejects_wrong_div() {
        let div = [0x11; 16];
        let wrong_div = [0x22; 16];
        let seed = 100u32;
        let prefix = b"test";
        let wit = witness(&div, seed, prefix);
        assert!(!verify_witness(&wrong_div, seed, prefix, &wit));
    }

    #[test]
    fn verify_witness_rejects_wrong_seed() {
        let div = [0x33; 16];
        let prefix = b"test";
        let wit = witness(&div, 10, prefix);
        assert!(!verify_witness(&div, 20, prefix, &wit));
    }

    #[test]
    fn verify_witness_rejects_tampered_prefix() {
        let div = [0x44; 16];
        let seed = 7u32;
        let prefix = b"original";
        let wit = witness(&div, seed, prefix);
        assert!(!verify_witness(&div, seed, b"tampered", &wit));
    }

    #[test]
    fn verify_witness_rejects_tampered_witness() {
        let div = [0x55; 16];
        let seed = 3u32;
        let prefix = b"test";
        let mut wit = witness(&div, seed, prefix);
        wit[0] ^= 0x01;
        assert!(!verify_witness(&div, seed, prefix, &wit));
    }

    // ---- verify_witness_local integration ----

    #[test]
    fn verify_local_valid_same_cell() {
        // Two devices in same cell, same marks → same truncated sketches
        let marks = [
            [0x01u8; 16], [0x02u8; 16], [0x03u8; 16], [0x04u8; 16],
            [0x05u8; 16], [0x06u8; 16], [0x07u8; 16], [0x08u8; 16],
            [0x09u8; 16], [0x0Au8; 16], [0x0Bu8; 16], [0x0Cu8; 16],
            [0x0Du8; 16], [0x0Eu8; 16], [0x0Fu8; 16], [0x10u8; 16],
        ];
        let rssi = [0i8; 16];
        let seed = 42u32;
        // Both devices see identical marks → identical sketches
        let local = observe(&marks, &rssi, seed, -100);
        let sender = observe(&marks, &rssi, seed, -100);
        let claimed_div = sketch_to_div_sketch(&sender);
        let prefix = b"frame prefix bytes for witness test";
        let wit = witness(&claimed_div, seed, prefix);
        let verdict = verify_witness_local(&local, &claimed_div, seed, prefix, &wit, 0.5);
        assert_eq!(verdict, WitVerdict::Valid, "same marks → same sketch → Valid");
    }

    #[test]
    fn verify_local_cell_mismatch_different_marks() {
        // Device A hears marks 1..16, device B hears marks 17..32 → no overlap
        let marks_a: Vec<[u8; 16]> = (1u8..=16).map(|i| [i; 16]).collect();
        let marks_b: Vec<[u8; 16]> = (17u8..=32).map(|i| [i; 16]).collect();
        let rssi = [0i8; 16];
        let seed = 99u32;
        let local = observe(&marks_a, &rssi, seed, -100);
        let sender = observe(&marks_b, &rssi, seed, -100);
        let claimed_div = sketch_to_div_sketch(&sender);
        let prefix = b"mismatch test";
        let wit = witness(&claimed_div, seed, prefix);
        let verdict = verify_witness_local(&local, &claimed_div, seed, prefix, &wit, 0.5);
        assert_eq!(
            verdict,
            WitVerdict::CellMismatch,
            "non-overlapping marks → CellMismatch"
        );
    }

    #[test]
    fn verify_local_stale_bad_mac() {
        let marks = [[0xAAu8; 16]; 16];
        let rssi = [0i8; 16];
        let seed = 1u32;
        let local = observe(&marks, &rssi, seed, -100);
        let claimed_div = [0xBB; 16]; // wrong div_sketch
        let prefix = b"stale test";
        let bad_wit = [0xFF; 16]; // garbage witness
        let verdict = verify_witness_local(&local, &claimed_div, seed, prefix, &bad_wit, 0.5);
        assert_eq!(verdict, WitVerdict::Stale, "bad MAC → Stale");
    }

    // ---- Known-Answer Test (KAT) ----

    #[test]
    fn witness_kat() {
        // Independent vector: div_sketch = 0x00..0x0F, seed = 0xDEADBEEF,
        // prefix = b"mesh-core PoCP witness KAT v1"
        let div: [u8; 16] = core::array::from_fn(|i| i as u8);
        let seed = 0xDEADBEEFu32;
        let prefix = b"mesh-core PoCP witness KAT v1";
        let wit = witness(&div, seed, prefix);

        // Expected witness computed independently.
        // Key = blake3::derive_key("mesh-core:v1:pocp-wit", 0x0001..0F || 0xEFBEADDE)
        // MAC = blake3::keyed_hash(key, prefix)[..16]
        let expected: [u8; 16] = [
            0x3D, 0xC7, 0xF8, 0x90, 0xE8, 0x2D, 0xE0, 0xAA,
            0x5A, 0xF6, 0xA6, 0xC0, 0xD1, 0xD1, 0x1A, 0xB6,
        ];
        assert_eq!(wit, expected, "KAT: witness must match independent vector");

        // Verify round-trip
        assert!(verify_witness(&div, seed, prefix, &wit));
    }

    // ---- Self-inclusion in the cell sketch (P2/P3/P5) ----
    //
    // A device never records its OWN mark from the air, so "marks I heard" excludes self.
    // With exactly two phones that made the two sketches disjoint, which is Jaccard 0.0 and
    // an automatic CellMismatch: LOCAL could never display on a two-device setup, and a
    // phone that had heard nobody built an EMPTY sketch and originated a witnessless frame
    // that every receiver relayed but never showed. The shim now includes its own mark, so
    // a cell is "the devices in RF range of each other, including me".

    #[test]
    fn two_mutual_neighbours_without_self_inclusion_are_disjoint() {
        // The old behaviour, asserted so the regression is visible if it ever comes back.
        let mark_a = [0xA1u8; 16];
        let mark_b = [0xB2u8; 16];
        let epoch = 77u32;
        // A heard only B; B heard only A.
        let a = observe(&[mark_b], &[-50], epoch, -80);
        let b = observe(&[mark_a], &[-50], epoch, -80);
        let sim = jaccard(
            &div_sketch_to_cell(&sketch_to_div_sketch(&a)),
            &div_sketch_to_cell(&sketch_to_div_sketch(&b)),
        );
        assert_eq!(sim, 0.0, "disjoint sketches — this is why LOCAL never displayed");
        assert!(!matches(&a, &b, 0.3), "below default tau");
    }

    /// A real mark whose low byte is 0xFF must not encode as the empty-cell sentinel.
    #[test]
    fn real_ff_low_byte_does_not_encode_as_empty_slot() {
        // Slot value with low byte 0xFF, plus 15 genuinely empty slots.
        let mut arr = [u64::MAX; 16];
        arr[0] = 0x1234_5678_9ABC_DEFF;
        let div = sketch_to_div_sketch(&CellSketch(arr));
        assert_ne!(div[0], 0xFF, "real element must not collide with the padding sentinel");
        assert_eq!(div[0], 0xFE);
        assert_eq!(&div[1..], &[0xFFu8; 15], "empty slots stay 0xFF");

        // The round-tripped cell must be non-empty, so a lone sender with such a mark is
        // still comparable instead of scoring 0.0 against everyone for the whole epoch.
        let cell = div_sketch_to_cell(&div);
        let live = cell.0.iter().filter(|v| **v != u64::MAX).count();
        assert_eq!(live, 1);
    }

    #[test]
    fn lone_sender_with_ff_mark_still_matches_a_receiver_holding_it() {
        // Receiver's cell contains the sender's 0xFF-low-byte mark plus its own.
        let mut sender = [u64::MAX; 16];
        sender[0] = 0x00FF;
        let mut receiver = [u64::MAX; 16];
        receiver[0] = 0x00FF;
        receiver[1] = 0x0042;

        let claim = div_sketch_to_cell(&sketch_to_div_sketch(&CellSketch(sender)));
        let local = div_sketch_to_cell(&sketch_to_div_sketch(&CellSketch(receiver)));
        // Before the remap this was 0.0 (claim decoded as the empty cell) and the frame was
        // dropped by every phone in range.
        assert!(jaccard(&claim, &local) >= 0.3, "j={}", jaccard(&claim, &local));
    }

    #[test]
    fn two_mutual_neighbours_with_self_inclusion_match() {
        let mark_a = [0xA1u8; 16];
        let mark_b = [0xB2u8; 16];
        let epoch = 77u32;
        // A heard B and includes itself; B heard A and includes itself.
        let a = observe(&[mark_b, mark_a], &[-50, 0], epoch, -80);
        let b = observe(&[mark_a, mark_b], &[-50, 0], epoch, -80);
        let sim = jaccard(
            &div_sketch_to_cell(&sketch_to_div_sketch(&a)),
            &div_sketch_to_cell(&sketch_to_div_sketch(&b)),
        );
        assert_eq!(sim, 1.0, "identical cell membership");
        assert!(matches(&a, &b, 0.3));
    }

    #[test]
    fn cold_start_sender_still_overlaps_the_receiver() {
        // A has just started and has heard NOBODY, so its sketch is {self} only. The
        // receiver B has already heard A (ingest records the mark before verifying), so
        // B's sketch is {A, B}. Overlap = {A} of union {A, B} = 0.5, above the 0.3 default.
        // Before self-inclusion A's sketch was EMPTY and it shipped a witnessless frame.
        let mark_a = [0xA1u8; 16];
        let mark_b = [0xB2u8; 16];
        let epoch = 5u32;
        let a = observe(&[mark_a], &[0], epoch, -80);
        let b = observe(&[mark_a, mark_b], &[-50, 0], epoch, -80);
        assert_ne!(sketch_to_div_sketch(&a), [0xFFu8; 16], "sketch must not be empty");
        let sim = jaccard(
            &div_sketch_to_cell(&sketch_to_div_sketch(&b)),
            &div_sketch_to_cell(&sketch_to_div_sketch(&a)),
        );
        assert!(sim >= 0.3, "cold-start claim must clear default tau, got {sim}");
    }

    #[test]
    fn remote_attacker_still_fails_with_self_inclusion() {
        // Self-inclusion must not weaken PoCP: a device that never shared RF with the
        // verifier holds a disjoint mark set and is still rejected.
        let mark_a = [0xA1u8; 16];
        let mark_b = [0xB2u8; 16];
        let mark_van = [0xE9u8; 16];
        let mark_van2 = [0xEEu8; 16];
        let epoch = 12u32;
        let victim = observe(&[mark_b, mark_a], &[-50, 0], epoch, -80);
        // The van hears only its own accomplice, never A or B.
        let van = observe(&[mark_van2, mark_van], &[-50, 0], epoch, -80);
        assert!(
            !matches(&victim, &van, 0.3),
            "a remote cell must never clear tau"
        );
    }
}

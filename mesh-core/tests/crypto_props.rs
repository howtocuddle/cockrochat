use mesh_core::crypto::pair_ratchet;
use proptest::prelude::*;

// ---------------------------------------------------------------------------
// Ratchet composition
//
// `pair_ratchet` refuses spans over 8192 (a DoS bound on wire-supplied epochs), which left
// private messaging permanently dead for any pair that went 8192 epochs (22.8 h at a 10 s
// epoch) without exchanging a message: every catch-up attempt exceeded the cap in both
// directions, and neither the send nor the receive path can advance a chain without first
// succeeding. The Android shim now walks long spans in chunks instead.
//
// Chunking is only sound because the ratchet is a left fold over the epoch INDEX sequence —
// `k_e = derive_key(k_{e-1} || e)` iterating `(from+1)..=to`. Splitting at any b walks the
// identical sequence. These properties pin that, because the shim helper is built on it.
// ---------------------------------------------------------------------------

/// Reference implementation of the shim's chunked walk (PairStore.ratchetTo).
fn ratchet_to(key: &[u8; 32], from: u32, to: u32, chunk: u32) -> Option<[u8; 32]> {
    if to < from {
        return None;
    }
    let mut k = *key;
    let mut e = from;
    while e < to {
        let next = to.min(e + chunk);
        k = pair_ratchet(&k, e, next)?;
        e = next;
    }
    Some(k)
}

prop_compose! {
    /// `a <= b <= c` with `c - a` inside the 8192 cap, so the direct call is defined.
    fn arb_split()(
        a in 0u32..1_000_000,
        span1 in 0u32..=4096,
        span2 in 0u32..=4096,
    ) -> (u32, u32, u32) {
        (a, a + span1, a + span1 + span2)
    }
}

proptest! {
    /// The property the chunked helper rests on: splitting a span anywhere is a no-op.
    #[test]
    fn ratchet_composes_at_any_split(key: [u8; 32], (a, b, c) in arb_split()) {
        let direct = pair_ratchet(&key, a, c);
        let stepped = pair_ratchet(&key, a, b).and_then(|k| pair_ratchet(&k, b, c));
        prop_assert_eq!(direct, stepped);
    }

    /// The helper itself agrees with a direct call wherever a direct call is defined,
    /// for any chunk size — including sizes that do not divide the span evenly.
    #[test]
    fn chunked_walk_matches_direct_call(
        key: [u8; 32],
        from in 0u32..1_000_000,
        span in 0u32..=8192,
        chunk in 1u32..=8192,
    ) {
        prop_assert_eq!(
            ratchet_to(&key, from, from + span, chunk),
            pair_ratchet(&key, from, from + span)
        );
    }

    /// Spans past the cap: the direct call refuses, the chunked walk succeeds. This is the
    /// C1 brick, and its fix.
    #[test]
    fn chunked_walk_clears_spans_the_cap_refuses(
        key: [u8; 32],
        from in 0u32..1_000_000,
        span in 8193u32..=60_000,
    ) {
        let to = from + span;
        prop_assert_eq!(pair_ratchet(&key, from, to), None);
        prop_assert!(ratchet_to(&key, from, to, 8000).is_some());
    }

    /// Chunk size must not be observable in the result.
    #[test]
    fn chunk_size_does_not_change_the_key(
        key: [u8; 32],
        from in 0u32..1_000_000,
        span in 0u32..=30_000,
        chunk_a in 1u32..=8192,
        chunk_b in 1u32..=8192,
    ) {
        prop_assert_eq!(
            ratchet_to(&key, from, from + span, chunk_a),
            ratchet_to(&key, from, from + span, chunk_b)
        );
    }

    /// Still one-way and still ordered: no chunking makes a backwards span legal.
    #[test]
    fn backwards_spans_stay_refused(
        key: [u8; 32],
        to in 0u32..1_000_000,
        back in 1u32..=10_000,
    ) {
        prop_assert_eq!(ratchet_to(&key, to + back, to, 8000), None);
    }
}

//! VDL — Verifiable Delay Lottery (v0).
//!
//! Origination gate for Tier-3 private frames. A private payload is opaque to
//! relays, so without a cost function any node could flood the mesh with
//! unverifiable ciphertext. The VDL witness makes each private frame cost the
//! sender a deliberately slow computation while a relay verifies it with a
//! single hash.
//!
//! v0 is a blake3 proof-of-work: find a 16-byte witness w such that
//! blake3("mesh-core:v1:vdl" || prefix || w) has at least `difficulty_bits`
//! leading zero bits, where `prefix` is the frame bytes 0..102 (everything
//! before the witness field). The witness sits inside the signed region, so it
//! is bound to the frame by the signature.
//!
//! Honest limitation: this is parallelizable proof-of-work, not a sequential
//! verifiable delay function. It bounds spam per unit of compute, not per unit
//! of wall-clock time. A sequential VDF can replace it behind the same
//! interface later.

const DOMAIN: &[u8] = b"mesh-core:v1:vdl";

/// Default difficulty for private-frame origination: ~2^22 hashes,
/// a few seconds of one phone core per frame per epoch.
pub const VDL_DIFFICULTY_BITS: u8 = 22;

fn hash_with(prefix: &[u8], witness: &[u8; 16]) -> [u8; 32] {
    let mut h = blake3::Hasher::new();
    h.update(DOMAIN);
    h.update(prefix);
    h.update(witness);
    *h.finalize().as_bytes()
}

fn leading_zero_bits(digest: &[u8; 32]) -> u32 {
    let mut bits = 0u32;
    for &b in digest {
        if b == 0 {
            bits += 8;
        } else {
            bits += b.leading_zeros();
            break;
        }
    }
    bits
}

/// Search for a witness meeting `difficulty_bits`. Deterministic counter search;
/// runtime grows ~2^difficulty_bits. Blocking — callers run it off the UI thread.
///
/// `difficulty_bits` is clamped to 64: the parameter is a `u8`, so a caller could ask for
/// 255 bits and hang the thread effectively forever (the u128 counter would wrap long
/// before a hit). Not reachable from the FFI, which hardcodes `VDL_DIFFICULTY_BITS`, but a
/// future caller should get a slow answer rather than a permanent one.
pub fn solve(prefix: &[u8], difficulty_bits: u8) -> [u8; 16] {
    let difficulty_bits = difficulty_bits.min(64);
    let mut counter: u128 = 0;
    loop {
        let witness = counter.to_le_bytes();
        if leading_zero_bits(&hash_with(prefix, &witness)) >= u32::from(difficulty_bits) {
            return witness;
        }
        counter = counter.wrapping_add(1);
    }
}

/// One-hash check that `witness` meets `difficulty_bits` for `prefix`.
pub fn verify(prefix: &[u8], witness: &[u8; 16], difficulty_bits: u8) -> bool {
    leading_zero_bits(&hash_with(prefix, witness)) >= u32::from(difficulty_bits)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn solve_then_verify_low_difficulty() {
        let prefix = b"test prefix";
        let witness = solve(prefix, 8);
        assert!(verify(prefix, &witness, 8));
    }

    #[test]
    fn verify_rejects_wrong_witness() {
        let prefix = b"x";
        let witness = [0xFFu8; 16];
        assert!(!verify(prefix, &witness, 16));
    }

    #[test]
    fn verify_difficulty_zero_always_true() {
        let prefix = b"anything";
        let witness = [0xABu8; 16];
        assert!(verify(prefix, &witness, 0));
    }

    #[test]
    fn witness_bound_to_prefix() {
        let prefix_a = b"a";
        let witness = solve(prefix_a, 12);
        assert!(!verify(b"b", &witness, 12));
    }

    #[test]
    fn leading_zero_bits_exact() {
        let mut d1 = [0u8; 32];
        d1[0] = 0x00;
        d1[1] = 0x0f;
        assert_eq!(leading_zero_bits(&d1), 12);

        let d_all_zero = [0u8; 32];
        assert_eq!(leading_zero_bits(&d_all_zero), 256);

        let mut d_high = [0u8; 32];
        d_high[0] = 0x80;
        assert_eq!(leading_zero_bits(&d_high), 0);
    }
}

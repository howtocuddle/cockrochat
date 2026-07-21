# Contributing — non-negotiable invariants

This project defends people from aggressive state actors. The rules below are **not style
preferences** — each one is a failure class that has killed a real system (see the "Breaking
Bridgefy" papers). Violating one is a security bug, not a nit. A reviewer WILL block on these.

## The seven invariants (mesh-build-plan.md §7)

1. **One codec, in Rust, shared.** No parsing in the platform shims (Kotlin/Swift). The shims
   move raw bytes in and out; `mesh-core::codec` is the *only* thing that interprets them. A
   second, lenient parser is how these systems die.
2. **Parse → verify → decide, in that order, always.** Nothing is relayed or rendered before
   validation completes. The order is enforced in `statemachine::on_recv` and is not negotiable:
   `len → epoch∈{N,N-1} → mark-unseen → sig-verify → witness-structural → then relay/render`.
3. **Fixed 194 B frame. No compression, no variable-length fields.** Any deviation ⇒ silent total
   drop (`DecodeErr`, mutate nothing, relay nothing). `codec::decode` must stay allocation-free and
   panic-free on every input — it is the anti-zip-bomb boundary. This is fuzzed in CI.
4. **Danger-only on the wire. Never assert "safe."** Silence ≠ safe. The confidence wall shows
   corroboration/dispute counts, never a boolean, and only for danger.
5. **Ephemeral keys, minimal persisted state, panic-wipe.** Keys rotate hourly and live only in the
   platform secure store via the `KeyStore` trait. The `store` module is size-capped and
   auto-decaying. `panic_wipe` must actually erase.
6. **The public plane is openly unencrypted — never label it E2E.** Only the deferred Tier-3
   private plane (Noise double-ratchet) is end-to-end encrypted.
7. **Trust is per-message physical corroboration, never accumulated to an identity.** Diversity
   counts *distinct locally-verified cells*, not reputation.

## Codec / crypto specifics

- The wire layout is frozen (`codec.rs`): `mark[16] hdr[2] div_sketch[16] epoch[4](BE) body[64]
  pocp_wit[16] sig[64] reserved[12] = 194`. The signature (Ed25519, 64 B) authenticates `[0..118)`
  only; `reserved[182..194)` is the **unsigned, hop-mutable** region (TTL/H_max live there so flood
  can decrement without breaking the signature). Do not sign the reserved bytes.
- All signatures are domain-separated (`crypto::DOMAIN_SIG`). Verification is constant-time
  (`verify_strict`).
- Crypto changes require Known-Answer Tests validated against an **independent** implementation, not
  a value captured from our own code.

## Before you push

```
cd mesh-core
cargo fmt --check
cargo clippy --all-targets -- -D warnings
cargo test
cargo +nightly fuzz run decode -- -max_total_time=30   # if you touched codec
```

All must be green. CI runs the same.

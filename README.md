# cockroachat — offline protest mesh

A decentralized, infrastructure-free message/alert client for protests under aggressive state
actors. Phones relay short **danger alerts** to each other over Bluetooth LE with no servers, no
accounts, and no cell/Wi-Fi dependency. Trust comes from *physical co-presence*, not identity, so a
remote adversary (a van running the app off-site) cannot inject or amplify alerts.

> Security posture: the public plane is **openly unencrypted and authenticated-per-message only** —
> it is deliberately *not* end-to-end encrypted (invariant #6). It carries danger signals a crowd is
> already broadcasting by being there, not private conversation. Read `CONTRIBUTING.md` before
> touching anything: the invariants there are load-bearing.

## Architecture

One Rust core holds **all** security-critical logic (byte parsing, crypto, proof-of-co-presence,
beacon chain, trust/diversity, state machine, storage). Thin platform shims own only the radio, OS
lifecycle, UI, secure key storage, and the clock. Bindings are generated with UniFFI.

```
mesh-core (Rust) ── UniFFI ──┬── Android shim (Kotlin + BLE)
  codec crypto pocp beacon   └── iOS shim (Swift + CoreBluetooth)
  trust statemachine store
```

Full design + milestones: `mesh-build-plan.md`.

## Status

| Milestone | State |
|---|---|
| **M0** repo + frozen interfaces + UniFFI Kotlin/Swift | ✅ done |
| **M1** codec + crypto + tests (fuzz, KATs) | ✅ done |
| M2 single-hop loopback (same platform) | ⏳ needs devices/SDKs |
| M3 cross-platform single-hop | ⏳ |
| M4 flood + Trickle + dedup | stubbed (`statemachine`) |
| M5 PoCP + beacon chain | stubbed (`pocp`, `beacon`) |
| M6 diversity + confidence wall | stubbed (`trust`) |
| M7 private plane (deferred) | interface stub only |
| M8 field + adversarial + battery | — |

Done now (host-verifiable):
- Fixed **194 B** wire codec, fixed-offset, total-failing. Fuzzed (cargo-fuzz + ASAN): **51.5M runs,
  0 panics, 0 heap allocations** in `decode` (enforced by a counting allocator).
- Ephemeral **Ed25519** (hourly rotation) + **ChaCha20-Poly1305** AEAD, domain-separated,
  constant-time verify. KATs validated against an independent implementation.
- UniFFI surface generating **Kotlin + Swift** (`frame_len`, `frame_decodes`, `make_test_frame`,
  `verify_frame`) — the byte-only shim contract.

Frozen wire layout:
`mark[16] hdr[2] div_sketch[16] epoch[4](BE) body[64] pocp_wit[16] sig[64] reserved[12] = 194`.
Signature covers `[0..118)`; `reserved` is the unsigned, hop-mutable region (TTL lives there).

## Build

```
cd mesh-core
cargo test                                   # 19 tests (codec, crypto KATs, FFI round-trip)
cargo clippy --all-targets -- -D warnings
cargo build --release                        # produces target/release/libmesh_core.so

# regenerate bindings
cargo run --bin uniffi-bindgen -- generate --library target/release/libmesh_core.so \
    --language kotlin --out-dir bindings/kotlin
cargo run --bin uniffi-bindgen -- generate --library target/release/libmesh_core.so \
    --language swift  --out-dir bindings/swift

# fuzz the parse boundary (nightly)
cargo +nightly fuzz run decode -- -max_total_time=60
```

# Build Plan — Offline Protest Mesh (Android + iOS)

Handoff spec for a coding agent. Assumes architecture v1 (`mesh-protest-architecture.md`) + audit v2 (this session). Design decisions below are **frozen for v0**; do not re-litigate mid-build.

---

## 0. v0 scope (build) vs later (defer)

**Build in v0:**
- Fixed 194 B wire codec + parse-before-forward validation.
- Ephemeral Ed25519 identity (hourly rotation) + AEAD.
- Tier-1 Local-Immediate + Tier-2 Regional-Propagated (flood + Trickle + dedup).
- PoCP cell digest (fuzzy KMV) + hash-chain beacon (NO VDF) + spacetime witness.
- k-distinct-cell corroboration + confidence wall (danger-only).
- Cross-platform (Android↔iOS) single-hop, then multi-hop.

**Defer past v0:**
- Tier-3 private plane (QR pairing + Noise double-ratchet) — stub the interface, don't implement.
- Optional VDF regional-injection rate-limiter — leave a trait seam, no impl.
- Fraud-proof challenge protocol for diversity — v0 uses liveness-weighting only; log-and-flag mismatches.
- Hardware pocket-beacon relays (nRF52/ESP32) — separate track, shares the codec.

**Hard rule (Bridgefy lesson):** the wire codec + validation is exactly ONE implementation (Rust), shared by both platforms. No ad-hoc parsing in Kotlin/Swift. Divergent/lenient parsers are how these systems die.

---

## 1. Architecture: shared core + thin shims

```
┌────────────────────────── Rust core (mesh-core) ──────────────────────────┐
│ codec | crypto | pocp | beacon | trust/diversity | statemachine | store   │
│                         (all security-critical logic)                     │
└──────────────▲───────────────────────────────────────▲────────────────────┘
        UniFFI  │ (generated bindings)                    │ UniFFI
        ┌───────┴────────┐                       ┌────────┴─────────┐
        │ Android shim    │                       │ iOS shim          │
        │ Kotlin + BLE    │                       │ Swift + CoreBT    │
        │ (radio + UI)    │                       │ (radio + UI)      │
        └─────────────────┘                       └───────────────────┘
```

- **Language:** Rust core (memory safety on the byte-parsing layer). Bind via **UniFFI** (Mozilla) → idiomatic Kotlin + Swift. Alt: `cxx`/JNI + C headers if UniFFI friction on async.
- **Platform owns ONLY:** BLE radio I/O, OS lifecycle/background, UI, secure key storage (Keystore/Keychain), local clock. Everything else = core.
- **Core is `no_std`-friendly** where possible so the same crate later powers hardware relays.

---

## 2. Core modules + interfaces

### 2.1 `codec` — the 194 B frame
Fixed layout (from v1 §5.5). No serde, no varint, no compression. Hand-rolled fixed-offset encode/decode.
```rust
pub struct Frame {
  mark: [u8;16], hdr: Hdr /*2*/, div_sketch: [u8;16], epoch: u32,
  body: [u8;64], pocp_wit: [u8;16], sig: [u8;60], // pad to 194
}
pub enum DecodeErr { BadLen, BadVersion, BadType, /* ... */ }
pub fn decode(buf: &[u8;194]) -> Result<Frame, DecodeErr>; // len-checked, total-fail on any deviation
pub fn encode(f: &Frame) -> [u8;194];
```
**Property tests (mandatory):** any input ≠ 194 B → `BadLen`, zero side effects. Fuzz decode (cargo-fuzz) → must never panic, never allocate unboundedly, never partial-parse. This module is the anti-zip-bomb boundary.

### 2.2 `crypto`
```rust
pub struct Ephemeral { /* rotates hourly */ }
pub fn rotate(now_epoch: u32) -> Ephemeral;
pub fn sign(e:&Ephemeral, canonical:&[u8]) -> [u8;60]; // Ed25519, domain-separated
pub fn verify(pk:&[u8;32], canonical:&[u8], sig:&[u8;60]) -> bool;
pub fn aead_seal(key:&[u8;32], nonce:&[u8;12], pt:&[u8]) -> Vec<u8>; // Tier-3 stub-callable
```
Crates to evaluate: `ed25519-dalek`, `chacha20poly1305`, `blake3` (H/KDF/sketch hashing). Constant-time verify. Keys persisted via platform secure storage through a `KeyStore` trait the shim implements.

### 2.3 `pocp` — Proof-of-Co-Presence
```rust
pub struct CellSketch([u64;16]); // KMV over truncated overheard marks
pub fn observe(marks:&[[u8;16]], rssi:&[i8], seed:u32) -> CellSketch; // RSSI-windowed, seed-bound
pub fn jaccard(a:&CellSketch, b:&CellSketch) -> f32;
pub fn matches(a:&CellSketch, b:&CellSketch, tau:f32) -> bool;
pub fn witness(cell:&CellSketch, seed:u32, msg_canonical:&[u8]) -> [u8;16]; // MAC_{KDF(cell‖seed)}
pub fn verify_witness_local(local:&CellSketch, seed:u32, msg:&[u8], wit:&[u8;16], tau:f32) -> WitVerdict;
// WitVerdict: Valid | CellMismatch(relocation/replay → ALARM, F4) | Stale
```
`tau` is a **measured** parameter (see §5 RF-overlap rig), not guessed.

### 2.4 `beacon` — chained hash beacon (NO VDF in v0)
```rust
pub struct Beacon { seed: u32/*or [u8;32]*/, epoch: u32, last_advance_ms: u64 }
pub fn local_entropy(nonprop_marks:&[[u8;16]], min_hearers:u32) -> Entropy;
pub fn advance(prev:&Beacon, e:&Entropy, now_ms:u64, floor_ms:u64) -> Option<Beacon>;
//   Some(new) iff observed AND (now - last_advance) >= floor_ms      // acceleration cap
pub fn fallback_local(cell:&CellSketch) -> Beacon;                    // chain-stall path (F6)
```
Entropy source = marks from **non-propagating** (TTL=0/1) local traffic, so it's agreed-among-locals yet unobservable to a remote van (F1/F3). Collapse-map for the wall = `H(seed ‖ alert_content)`.

### 2.5 `trust` — diversity + corroboration
```rust
pub struct DiversitySketch([u64;16]);       // KMV over DISTINCT locally-verified cell digests
pub fn merge(a:&DiversitySketch, b:&DiversitySketch) -> DiversitySketch;
pub fn distinct_estimate(s:&DiversitySketch) -> u32; // threshold-only ("≥ D?"), liveness-weighted
pub fn corroboration(alert_id:&[u8;16]) -> Confidence; // {cells_for, cells_dispute, liveness}
```
v0: only **locally-verified** cells increment (F5). Log CellMismatch events; no full fraud-proof protocol yet.

### 2.6 `statemachine` — the message engine
Owns: seen-set (time-decaying Bloom, window 2·T_epoch), Trickle timers (K_supp, W, RSSI-biased slot), TTL/H_max, tier routing, dispatch to `trust`.
```rust
pub enum Ingest { Relay(Frame), Deliver(Alert), Drop(Reason), Alarm(SecurityEvent) }
pub fn on_recv(&mut self, raw:&[u8;194], rssi:i8, now_ms:u64) -> Ingest; // parse→verify→decide (order fixed, v1 §5.5)
pub fn on_originate(&mut self, tier:Tier, body:[u8;64]) -> [u8;194];
pub fn tick(&mut self, now_ms:u64) -> Vec<[u8;194]>;                    // fires due (unsuppressed) rebroadcasts
```
**Processing order is enforced here and non-negotiable:** len → epoch∈{N,N-1} → mark-unseen → sig-verify → witness-structural → *then* relay/render.

### 2.7 `store` — bounded persistence
Seen-set, cell history (last 2 epochs), pending alerts. All size-capped, auto-decay. Minimal on-disk footprint (coercion resistance). Expose panic-wipe.

---

## 3. Platform BLE layer (the actual hard part)

Transport = **BLE 5 extended advertising, connectionless**, one 194 B AUX PDU per frame. Use **Coded PHY** on frontier for range. Both platforms implement the same `RadioPort` trait:
```rust
pub trait RadioPort {
  fn advertise(&self, frame:&[u8;194]);      // extended adv set, non-connectable pref (see iOS caveat)
  fn on_scan(&self, cb: impl Fn(&[u8;194], i8/*rssi*/)); // deliver raw + RSSI to statemachine
  fn set_duty(&self, scan_ms:u32, sleep_ms:u32);
}
```

### 3.1 Android
- `BluetoothLeAdvertiser.startAdvertisingSet(...)` with `AdvertisingSetParameters` extended, `setPrimaryPhy/SecondaryPhy(PHY_LE_CODED)`, non-connectable; payload in manufacturer/service-data.
- `BluetoothLeScanner` + `ScanFilter` on service UUID, `SCAN_MODE_LOW_LATENCY` foreground / balanced background.
- **Background survival:** foreground service w/ persistent notification (required for sustained BLE). Handle Doze/battery-optimization exemption prompt. Request runtime BLE + location perms.
- Android↔Android extended adv is the *reliable* path — validate here first.

### 3.2 iOS (constrained — budget extra time)
- `CBPeripheralManager.startAdvertising` + `CBCentralManager.scanForPeripherals(withServices:)`.
- **Background reality (design around it, don't fight it):**
  - Backgrounded peripheral: local name dropped; service UUIDs go to the undocumented **overflow area**, readable only by another *iOS device explicitly scanning* — **Android cannot decode it**.
  - Backgrounded central: reports **only connectable, service-UUID-filtered** ads; non-connectable suppressed. → iOS background may require *connectable* adv + explicit UUID filter, accepting the enumeration surface.
  - Newest iOS/hardware regressions: background scanning may stop entirely — treat background iOS as best-effort, not guaranteed.
- **Consequences baked into UX/architecture:**
  1. iOS↔iOS background works via overflow-area; iOS↔Android background largely does **not** → heterogeneous mesh, plan for it.
  2. Offer explicit "active/marching" foreground mode (reliable) with the honest battery + screen-visibility trade-off surfaced to the user.
  3. **Pocket-beacon hardware relays are the real reliability fix** — an always-awake backbone that sidesteps iOS policy. Prioritize the hardware track if field tests show iOS background gaps.

### 3.3 Interop test matrix (must pass before multi-hop work)
| | Android fg | Android bg | iOS fg | iOS bg |
|---|---|---|---|---|
| Android fg | ✅ target | ✅ | ✅ | ✅ (connectable+filter) |
| Android bg | ✅ | ✅ | ✅ | ⚠️ overflow-area gap |
| iOS fg | ✅ | ✅ | ✅ | ✅ |
| iOS bg | ✅(conn+filter) | ⚠️ | ✅ | ✅ (iOS↔iOS) |
Record actual pass/fail per OS version + device; this table drives whether hardware relays become mandatory.

---

## 4. Milestones (with acceptance criteria + concrete first tasks)

| M | Goal | Acceptance |
|---|---|---|
| **M0** | Repo + interfaces frozen | `mesh-core` crate scaffolded; all traits above compile as stubs; UniFFI produces Kotlin+Swift; CI green |
| **M1** | Codec + crypto + tests | 194 B encode/decode round-trips; **fuzz decode 0 panics/allocs**; Ed25519 sign/verify + AEAD KATs pass; property test "≠194 B ⇒ no side effect" |
| **M2** | Single-hop loopback, same-platform | Android→Android and iOS→iOS: originate → advertise → scan → decode → verify → deliver, one hop, foreground |
| **M3** | Cross-platform single-hop | Android↔iOS foreground interop per §3.3; document every failure cell |
| **M4** | Flood + Trickle + dedup | 5+ device lab: message reaches all; retransmits bounded by K_supp; RSSI-biased outward propagation observed; seen-set caps memory |
| **M5** | PoCP + beacon chain | Cell digests fuzzy-match among co-located devices at measured τ; hash-chain advances w/ floor + stall-fallback; spacetime witness verifies locally; **CellMismatch alarm fires on relocation/replay test** |
| **M6** | Diversity + confidence wall | Danger escalates only at ≥k distinct locally-verified cells; wall shows corroboration/dispute counts (never boolean, danger-only); van-flood-from-one-cell test → gated out |
| **M7** | (defer-gate) private plane | QR pairing → Noise ratchet E2E over flood transport; only paired peer decrypts |
| **M8** | Field + adversarial + battery | Real crowd test; battery bench; red-team: remote-van flood, co-located mole dispute, replay/relocation, chain-stall, malformed-frame storm |

**M0 concrete first tasks (hand to coder):**
1. `cargo new --lib mesh-core`; add `ed25519-dalek`, `chacha20poly1305`, `blake3`, `arrayref`; set up UniFFI + `cargo-fuzz` + `proptest`.
2. Implement `codec::{Frame, decode, encode}` fixed-offset + the `DecodeErr` total-failure semantics.
3. Write the two guardrail tests first (TDD): fuzz `decode`, and "non-194 ⇒ `BadLen`, no mutation."
4. Implement `crypto::{rotate, sign, verify}`; add KATs.
5. Stub `pocp`, `beacon`, `trust`, `statemachine`, `store`, `RadioPort` as compiling traits.
6. Generate bindings; stand up empty Android (Kotlin) + iOS (Swift) apps that link the core and call `encode`/`decode` on a hardcoded frame (proves the binding path end-to-end before any BLE).

---

## 5. Test rigs (build alongside code)

- **Frame fuzzer / property suite** — the parse-before-forward guarantee. Highest priority; gates M1.
- **Multi-device BLE lab** — ≥5 mixed Android/iOS + a sniffer (nRF52840 + Wireshark) to verify on-air bytes, PHY, timing, suppression.
- **RF-observation-overlap rig [sets τ]** — co-locate N phones, log each device's overheard-mark set per epoch, compute pairwise Jaccard → pick τ and KMV size from *real* data. Do NOT ship a guessed τ.
- **Battery bench** — measure mAh/hour vs duty-cycle + suppression + Coded PHY; per device class.
- **Adversary simulator** — scripted: remote-van flood (must be PoCP-blocked), one-cell mole (must be diversity-gated), captured-packet relocation (must raise CellMismatch alarm), chain-stall (must fall back local), malformed-frame storm (must total-drop, no hang).

---

## 6. Dependency risk register

| Risk | Severity | Mitigation |
|---|---|---|
| **iOS background BLE** (overflow area, connectable-only bg scan, HW regressions) | **High** | Design for heterogeneous mesh; foreground "marching" mode; hardware pocket-beacon backbone as real fix (§3.2) |
| **Cross-platform extended-adv interop** | High | M3 gate before building on top; per-OS-version matrix |
| UniFFI async/threading friction | Med | Fallback to cxx/JNI + C ABI; keep core sync where possible |
| KMV/τ mis-set → cell false match/miss | Med | Measure τ empirically (§5); tune KMV size |
| Beacon fork under entropy disagreement | Low-Med | Coarse widely-agreed entropy (k-min corroborated); longest-verified-chain tie-break; local fallback |
| ~~VDF impl / hardware-gap~~ | ~~was Highest~~ | **Removed from beacon in v0 (audit F1)**; only reappears if a strong-bias-resistance requirement is later proven |

---

## 7. Non-negotiable invariants (put in CONTRIBUTING)

1. One codec, in Rust, shared. No parsing in shims.
2. Parse → verify → decide, in that order, always. Nothing relayed/rendered pre-validation.
3. Fixed 194 B, no compression, no variable fields. Deviation ⇒ silent total drop.
4. Danger-only on the wire. Never assert "safe." Silence ≠ safe.
5. Ephemeral keys, minimal persisted state, panic-wipe.
6. Public plane is openly unencrypted — never label it E2E.
7. Trust is per-message physical corroboration, never accumulated to an identity.

---

*Grounding for platform claims: Bluetooth Core Spec 5.x extended advertising (≤255 B single AUX PDU); Apple CoreBluetooth background advertising docs + developer reports (overflow-area, connectable-only background scanning, recent-hardware regressions); Albrecht et al. "Breaking Bridgefy" (CT-RSA 2021) and "Breaking Bridgefy, again" (USENIX Security 2022) for the failure classes this plan structurally avoids.*

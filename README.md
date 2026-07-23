# cockroachat — Offline Decentralized Protest Mesh

`cockroachat` is a decentralized, infrastructure-free mesh messaging and alert protocol built for protests, civil demonstrations, and emergency situations under hostile electronic surveillance or network blackouts.

Phones relay short **danger alerts** over Bluetooth Low Energy (BLE 5.0 Extended Advertising) without cell towers, Wi-Fi routers, central servers, internet access, or user accounts. Trust in `cockroachat` is grounded in **Physical Co-Presence** and **Spatial Diversity**, preventing remote adversaries (such as a surveillance van running virtual nodes off-site) from injecting, forging, or amplifying false alerts.

> ⚠️ **v0 status — design target vs. shipped code.** The threat-model defenses below describe the protocol's **design target**. Three critical mechanisms are documented but **not yet enforced** in the running v0 node:
>
> * **PoCP witness (proximity gating):** The `pocp::witness` / `verify_witness_local` functions exist and are wired into the ingest pipeline (M5a). Public frames with a valid PoCP witness are verified against the local cell sketch; mismatching or stale witnesses are dropped or flagged. The witness key uses the public epoch number (not the beacon seed) to preserve cross-cell Tier-2 verification.
> * **Beacon chain (forward secrecy):** The `beacon.rs` self-clocking chain is implemented (M5b). Marks and ephemeral signing keys are derived from the one-way hash chain (`seed_N = BLAKE3(seed_{N-1} || E_N)` where $E_N$ is ambient RF entropy from LocalImmediate marks). Post-seizure, past seeds, marks, and public keys are unrecoverable. Wall-clock epochs remain for coordination; the beacon provides forward secrecy, not epoch hiding.
> * **Diversity trust (multi-cell confidence):** `trust.rs` is fully stubbed. A single-cell origination is treated identically to multi-cell crowd corroboration.
>
> **What v0 DOES enforce:** Ed25519 signature authentication (`verify_strict`), parse-before-forward codec safety (fixed 226-byte frames, no varint, no compression), frame-hash deduplication, danger-only alert filtering, VDL proof-of-work cost-gating on private frames (Tier-3), and ChaCha20-Poly1305 AEAD for pairwise messages. See §4 for per-module implementation status and the audit findings for a complete gap analysis.

---

## 1. Threat Model & Bridgefy Vulnerabilities Addressed

`cockroachat` is explicitly engineered to overcome the systemic flaws discovered in early mesh apps like Bridgefy (documented in the 2021 CT-RSA and 2022 USENIX Security studies):

1. **Parse-Before-Forward Security (Anti-Zip-Bomb Boundary)**
   - *Bridgefy Flaw*: Relayed payloads before parsing/validating them, enabling single malformed packets to crash every node on the mesh.
   - *`cockroachat` Fix*: Enforces a strict `Parse -> Verify -> Decide -> Forward` execution pipeline inside a memory-safe Rust core. Nothing is relayed or presented to the UI until length checks, signature verification, and witness checks pass completely.

2. **Physical Spatial Diversity over Virtual Web-of-Trust** *(v0: documented, not enforced)*
   - *Bridgefy Flaw*: Relied on digital identity tokens or accumulated reputation, enabling an adversary to spawn thousands of virtual identities (Sybil attack) to hijack trust algorithms.
   - *`cockroachat` Fix*: Ignores virtual identity counts. A message gains confidence only as it is verified by distinct *geographic cells* (derived from physical ambient RF observations). An off-site adversary creating 10,000 virtual identities across two physical devices produces zero spatial entropy and is ignored by the crowd.

3. **Chained Epoch Beacon (Forward Secrecy / Coercion Resistance)**
   - *Bridgefy Flaw*: A seized device could have its static identity seed extracted, allowing retrospective reconstruction of the user's entire session history — every mark, every public key, every movement trace — by matching against logs.
   - *`cockroachat` Fix*: Derives marks and ephemeral signing keys from a one-way hash-chain beacon (`seed_N = BLAKE3(seed_{N-1} || E_N)` where $E_N$ is ambient entropy from LocalImmediate marks). Past beacon seeds are unrecoverable from the current seed, so post-seizure analysis cannot reconstruct prior marks or public keys. A seized device yields the current epoch only — not the user's full protest history.

4. **Danger-Only Asymmetric Alerting**
   - *Bridgefy Flaw*: Symmetric messaging allowed attackers to inject false "ALL CLEAR" or "SAFE HERE" broadcasts to lure protesters into traps.
   - *`cockroachat` Fix*: The public mesh only carries danger alerts. Silence is never interpreted as safety, and nodes cannot broadcast "safe" status on the public plane.

5. **Open Public Plane Transparency**
   - *Bridgefy Flaw*: Attempted to wrap public broadcast alerts in broken E2E crypto wrappers, giving users a false sense of privacy while leaking metadata.
   - *`cockroachat` Fix*: The public plane is openly unencrypted and authenticated per message. It carries public danger signals that are already physically visible to anyone present.

---

## 2. Architecture & Module Design

All security-critical logic (codec parsing, cryptography, Proof-of-Co-Presence, beacon chaining, trust aggregation, and protocol state machine) resides in a single, memory-safe Rust core (`mesh-core`). Platform shims in Kotlin (Android) and Swift (iOS) are thin layers responsible **only** for radio hardware I/O, OS background lifecycles, UI rendering, and secure key storage. A Linux laptop client (`laptop/`) links the same `mesh-core` crate directly for desktop testing via BlueZ.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            Rust Core (mesh-core)                            │
│ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌───────┐ ┌───────────┐ │
│ │  codec   │ │  crypto  │ │   pocp   │ │  beacon  │ │ trust │ │statemach. │ │
│ ├──────────┤ ├──────────┤ ├──────────┤ ├──────────┤ ├───────┤ ├───────────┤ │
│ │ message  │ │ private  │ │   vdl    │ │  store   │ │  ffi  │ │  radio    │ │
│ └──────────┘ └──────────┘ └──────────┘ └──────────┘ └───────┘ └───────────┘ │
└──────────────────────▲───────────────────────────────▲──────────────────────┘
                       │ UniFFI (Generated Bindings)   │ Direct Rust link
           ┌───────────┴──────────┐        ┌───────────┴───────────┐
           │ Android Shim         │        │ Laptop Client          │
           │ (Kotlin + BLE 5)     │        │ (Rust + BlueZ/bluer)   │
           └──────────────────────┘        └───────────────────────┘
```

### Rust Core (`mesh-core`) Modules

* **`codec`**: Zero-allocation, non-panicking, fixed-offset 226-byte encoder/decoder. Strict total-failure semantics: any byte-length deviation or malformed header results in immediate drop with zero side effects.
* **`crypto`**: Ephemeral Ed25519 signature scheme with beacon-rotating keys (forward secrecy via chained beacon seed), domain-separated hashing (BLAKE3), X25519 Diffie-Hellman key exchange, and ChaCha20-Poly1305 AEAD routines.
* **`message`**: Single origination path for all signed frames. Derives per-epoch marks from the beacon seed, builds the wire frame, and provides `body_text` / `frame_hash` accessors.
* **`pocp`**: Proof-of-Co-Presence engine. Constructs K-Minimum Values (KMV) fuzzy sketches from overheard ambient BLE marks, evaluates Jaccard similarity ($\tau$), and verifies spacetime witnesses ($MAC_{KDF(cell \parallel seed)}(msg)$).
* **`beacon`**: Self-clocking, chained hash beacon. Computes dynamic epoch seeds using locally observed non-propagating mark entropy and enforces acceleration floor constraints.
* **`trust`**: Spatial diversity aggregator. Tracks distinct, locally verified physical cell digests and flags spatial anomalies (`CellMismatch` relocation alarms).
* **`statemachine`**: Core packet processing engine. Controls seen-set dedup (time-decaying hash map), Trickle suppression timers ($K_{supp}$, $W$, RSSI slot biasing), TTL/hop management, and alert dispatch.
* **`private`**: Tier-3 private messaging. Seals/opens 64-byte message bodies with ChaCha20-Poly1305 AEAD using pairwise keys derived via `crypto::pair_derive`. Nonces are constructed from epoch + mark prefix to prevent reuse.
* **`vdl`**: Verifiable Delay Lottery. Proof-of-work cost gate for Tier-3 private frames — `solve` finds a witness with ≥ `VDL_DIFFICULTY_BITS` leading zero bits; `verify` checks it with a single BLAKE3 hash.
* **`store`**: Size-capped, memory-bounded persistent storage with automated auto-decay and hardware panic-wipe capabilities.
* **`ffi`**: UniFFI-exported interface consumed by the Android Kotlin shim (and future iOS Swift shim).
* **`radio`**: Trait definition (`RadioPort`) for the BLE transport seam implemented by each platform shim.

### Laptop Client (`laptop/`)

A Linux desktop mesh node built on `bluer` (BlueZ async bindings) and `tokio`. Links `mesh-core` directly (no UniFFI). Advertises via BLE Extended Advertising (1M PHY), scans for peers, computes per-epoch KMV sketches, and accepts interactive text input from stdin. Useful for protocol debugging without a phone.

---

## 3. Fixed 226-Byte Wire Protocol Spec

To maximize transmission reliability over BLE Extended Advertising without fragmentation, every `cockroachat` packet is serialized into a strict **226-byte fixed-size binary layout**. Variable-length fields, TLVs, and compression are strictly prohibited.

```
+-------------------+------------------+-----------------------+--------------------+
|  mark (16 B)      |  hdr (2 B)       |  div_sketch (16 B)    |  epoch (4 B)       |
|  [0..16)          |  [16..18)        |  [18..34)             |  [34..38)          |
+-------------------+------------------+-----------------------+--------------------+
|  body (64 B)                         |  pocp_wit (16 B)     |  pk (32 B)          |
|  [38..102)                           |  [102..118)          |  [118..150)         |
+--------------------------------------+--------------------------------------------+
|  sig (64 B)                                                                       |
|  [150..214)                                                                       |
+-----------------------------------------------------------------------------------+
|  reserved (12 B) [Unsigned, Hop-Mutable: TTL / Hop Count]                         |
|  [214..226)                                                                       |
+-----------------------------------------------------------------------------------+
```

### Frame Field Breakdown

| Byte Range | Field Name | Type / Size | Description |
|---|---|---|---|
| `0..16` | `mark` | `[u8; 16]` | Pseudo-random message identifier used for deduplication, Trickle suppression, and local entropy generation. |
| `16..18` | `hdr` | `[u8; 2]` | Packet header: Version (4 bits), Packet Type (4 bits), Flags/Tier (8 bits). |
| `18..34` | `div_sketch` | `[u8; 16]` | KMV sketch digest / AEAD nonce counter (first 8 bytes). |
| `34..38` | `epoch` | `u32` (BE) | Big-endian epoch index derived from the self-clocking beacon chain. |
| `38..102` | `body` | `[u8; 64]` | Payload area containing danger alert text or structured emergency codes. |
| `102..118` | `pocp_wit` | `[u8; 16]` | VDL proof-of-work witness (Tier-3) / PoCP witness (future). |
| `118..150` | `pk` | `[u8; 32]` | Ephemeral Ed25519 public key. Enables any relay or endpoint to verify the frame signature without pre-shared key material. |
| `150..214` | `sig` | `[u8; 64]` | Ephemeral Ed25519 signature authenticating canonical bytes `[0..150)`. |
| `214..226` | `reserved` | `[u8; 12]` | Unsigned, hop-mutable region containing TTL ($H_{max}$), hop count, and RSSI metrics. Modified in-flight without invalidating `sig`. |

---

## 4. Cryptographic & Protocol Mechanics

### Proof-of-Co-Presence (PoCP)
1. **Ambient Mark Sampling**: Each device passively records ambient marks from raw BLE advertisements emitted by nearby nodes (within ~30 meters).
2. **KMV Sketch Construction**: The set of overheard marks is hashed and truncated into a K-Minimum Values (KMV) bottom-$k$ sketch representing the device's immediate physical cell.
3. **Proximity Verification**: Devices compute Jaccard distance between sketches:
   $$J(A, B) = \frac{|A \cap B|}{|A \cup B|} \ge \tau$$
   If $J(A, B) \ge \tau$, co-presence is verified. A cell mismatch triggers a `CellMismatch` security alert, identifying relocation or replay attacks.

### Chained Epoch Beacon
- **Seed Chaining Formula**:
  $$seed_N = \text{BLAKE3}(seed_{N-1} \parallel E_N)$$
- **Entropy Source ($E_N$)**: Digest of the $k$ smallest marks collected from *non-propagating* (TTL=0 or 1) local traffic during epoch $N-1$. Because $E_N$ is observed locally, an off-site surveillance node cannot observe or predict it.
- **Self-Clocking Cap**: Nodes only advance to $seed_N$ if $\ge 4\text{ minutes}$ have elapsed since $seed_{N-1}$, preventing clock-acceleration attacks.
- **v0 status**: STUB — `beacon.rs` (local_entropy, advance, fallback_local all return `todo!()`).\
  The beacon self-clocking and entropy-gathering logic is not yet implemented. In v0, the epoch
  counter is derived from the unix wall clock (`unix-time-ms / epochMs`). An adversary with
  control over the device clock can manipulate epoch boundaries. The chained hash *formula* is
  documented but not wired into the running node.

### Proof-of-Co-Presence (PoCP)

- **v0 status**: STUB — `pocp.rs::witness` and `pocp.rs::verify_witness_local` return `todo!()`.\
  The KMV sketch-building (`observe`) and Jaccard comparison (`jaccard`, `matches`) are fully
  implemented and tested. The spacetime witness MAC that proves physical presence (the critical
  gate against remote-van Sybil attacks) is *not yet built*. Frames have the 16-byte `pocp_wit`
  field on the wire but it carries the VDL proof-of-work witness for Tier-3 frames instead.
  Co-presence verification is not enforced at relay or display time.

### Spatial Diversity / Trust

- **v0 status**: STUB — `trust.rs::merge`, `distinct_estimate`, and `corroboration` all return
  `todo!()`. The diversity aggregator that distinguishes "10,000 virtual identities on 2 physical
  devices" from "10,000 real devices in distinct cells" does not exist yet. The `div_sketch` field
  on the wire is repurposed for the AEAD nonce counter (Tier-3 private frames).

---

## 5. Multi-Tier Broadcast Strategy

| Tier | Name | Latency / Range | Origination Gate | Relaying / Trust Gate |
|---|---|---|---|---|
| **Tier 1** | Local-Immediate | Instant (0-1 hop, ~30m) | Valid PoCP witness + ephemeral Ed25519 sig | Direct display to nearby devices (human ground truth verification). |
| **Tier 2** | Regional-Propagated | Multi-hop flood (seconds) | Valid PoCP witness + ephemeral Ed25519 sig | Rebroadcast via Trickle algorithm ($K_{supp}$). Confidence scales only when verified by $\ge k$ distinct local cells. |
| **Tier 3** | Private-Directed (v0 shipped) | End-to-end multi-hop | Out-of-band X25519 key exchange + VDL cost proof | Body is ChaCha20-Poly1305 ciphertext over the flood transport; relays carry it only with a valid VDL witness. |

**Tier-2 flood (v0)**: A TTL hop counter lives at `reserved[0]` (starts at 8, each relay decrements by 1, dies at 0). Deduplication by frame hash prevents rebroadcast storms: a relay that has already seen a frame hash drops it silently. The relay decision itself lives in `statemachine::relay_decision` in the Rust core — platform shims never make relay choices.

**Tier-3 private (v0 implemented)**: Two devices pair out-of-band — each generates a long-term X25519 secret, exchanges public keys (paste/QR/paper), and both derive an identical pairwise key via `crypto::pair_derive` (X25519 DH → blake3 domain-separated). A private message seals its 64-byte body with ChaCha20-Poly1305 (`private::seal_private_body`): nonce = `epoch_be || mark[0..8]`, plaintext block `[len][utf-8 ≤47 bytes][zero pad]`. There is **no recipient address on the wire** — the receiver trial-decrypts every incoming private frame against each stored pair key; a tag match both selects the conversation and authenticates the sender. The frame carries `MsgType::Private` (wire byte 17 = 3).

**Tier-3 DoS problem + VDL**: A private body is opaque to relays — they cannot inspect content, so without a cost function an attacker could flood the mesh with junk ciphertext that every node dutifully relays (denial of service). **VDL (Verifiable Delay Lottery)** = each private origination must carry a proof-of-work witness in the `pocp_wit` field: `blake3("mesh-core:v1:vdl" || frame[0..102] || witness)` must have ≥ `VDL_DIFFICULTY_BITS` (v0 = 22) leading zero bits. Finding the witness costs the sender seconds of CPU (`vdl::solve`, run off the UI thread); any relay verifies it with a **single hash** (`vdl::verify`) before carrying the frame. The witness lives inside the signed region, so it is bound to the frame. **Honest limitation:** v0 is parallelizable proof-of-work, not a sequential verifiable delay function — it bounds spam per unit of compute, not per unit of wall-clock time. A true sequential VDF can drop in behind the same `solve`/`verify` interface later. The name "Verifiable Delay Lottery" is this project's own term, kept for continuity.

---

## 6. Mobile Platform Implementations & BLE Realities

### BLE Transport Mode
- Uses **BLE 5.0 Extended Advertising** (AUX_ADV_IND PDUs) on **LE Coded PHY** (for maximum range under crowded conditions).
- Packets are broadcast as non-connectable, undirected extended advertisements carrying the 226-byte payload.

### Android Shim
- Utilizes `BluetoothLeAdvertiser` with extended advertising parameters and `BluetoothLeScanner` with low-latency filters.
- Runs inside a Foreground Service with persistent notifications to survive Android Doze mode and OS process sweeps.

### iOS Shim & Background Constraints
- Apple CoreBluetooth imposes heavy background constraints:
  - Background peripheral mode strips local device names and moves service UUIDs into an undocumented **overflow area** readable only by other explicitly scanning iOS devices (Android devices cannot read overflow UUIDs).
  - Background central mode suppresses non-connectable advertisements entirely.
- *Architectural Accommodation*: iOS devices operate with connectable background advertisements when necessary, and the app prompts users to use active foreground mode during marches.
- *Hardware Pocket-Beacon Relays*: To maintain an un-throttled mesh backbone unaffected by mobile OS background restrictions, small pocketable microcontrollers (nRF52840 or ESP32-C3) running `mesh-core` can act as dedicated mesh repeaters.

---

## 7. Non-Negotiable Security Invariants

All contributors and maintainers must strictly enforce the following seven invariants:

1. **One Codec in Rust**: Platform shims (Kotlin/Swift) must never parse or construct frame fields. They pass raw 226-byte arrays directly to `mesh-core`.
2. **Parse -> Verify -> Decide -> Forward**: Processing order is fixed: `Length check -> Epoch window -> Mark unseen -> Signature verify -> Witness check -> State machine decision`.
3. **Fixed 226-Byte Frame**: No variable-length fields, no compression, no optional headers. Deviation results in silent drop.
4. **Danger-Only Alerts**: The public plane carries danger alerts only. Never transmit "safe" or "all clear" signals.
5. **Ephemeral Keys & Minimal Persistence**: Identity keys rotate with the beacon chain (floor ~4 min real, epoch-duration in test). Storage automatically decays, and `panic_wipe()` immediately purges all state.
6. **Unencrypted Public Plane**: The public plane is authenticated, not private. Never label it as E2E encrypted.
7. **Physical Spatial Trust**: Trust is derived solely from physical cell corroboration, never accumulated identity reputation.

---

## 8. Build & Verification

```bash
# Navigate to core library
cd mesh-core

# Run test suite (crypto KATs, wire codec, and UniFFI round-trips)
cargo test

# Run linter checks
cargo clippy --all-targets -- -D warnings

# Build release library
cargo build --release

# Generate Kotlin and Swift bindings
cargo run --bin uniffi-bindgen -- generate --library target/release/libmesh_core.so \
    --language kotlin --out-dir bindings/kotlin
cargo run --bin uniffi-bindgen -- generate --library target/release/libmesh_core.so \
    --language swift  --out-dir bindings/swift

# Fuzz the 226-byte parser boundary (Nightly toolchain required)
cargo +nightly fuzz run decode -- -max_total_time=60
```

### Debug App Parameters (the τ rig)

The debug APK and laptop Rust client expose every tunable parameter and show live readouts. All values persist in Android SharedPreferences (`mesh_cfg`).

| Parameter | Default | What it controls |
|---|---|---|
| `epochMs` | `10000` (10 s) | Length of one measurement window. **All devices in a test run MUST use the same value** — epoch number = `unix-time-ms / epochMs`, so a mismatch causes epoch numbers to diverge and Jaccard comparisons to match the wrong windows. |
| `tauThreshold` (τ) | `0.5` | Jaccard similarity cutoff. At or above = "same cell"; below = "different cell". This is the single most important calibration knob — tune it from real field measurements, not guesses. |
| `rssiFloorDbm` | `-80` | Signal-strength floor in dBm. Frames heard weaker than this value are ignored when building the local KMV cell sketch, filtering out distant or van-grade observers. Closer to 0 = stricter: e.g. `-60` means only very near neighbors count toward your sketch. |
| `advIntervalMs` | `1000` | How often the radio repeats the current advertisement (milliseconds). Lower = more visible to scanners, higher battery cost. |
| `codedPhy` | `true` | BLE long-range mode (LE Coded PHY, S=8). Provides ~4× range over 1M PHY but requires Bluetooth 5 Coded PHY support on **both** ends. The laptop's Intel controller does NOT support Coded PHY — this is not a problem because 1M PHY is always scanned simultaneously, so leaving `codedPhy` on is safe in mixed deployments. |

**Live readouts** shown by the debug UI:

- **Epoch**: The current window number (`unix-time-ms / epochMs`). Verify all devices in a run show the same epoch number.
- **Neighbors**: Count of distinct `markHex` values heard **this epoch** (after RSSI floor filter). This is your crowd density indicator for the current window.
- **Total Rx**: All frames received since the app started, after frame-hash dedup. A monotonically rising counter.
- **Sketch[0..3]**: The first 4 slots of the 16-slot KMV sketch for the current epoch. Slots showing `0xffff…` are empty padding (fewer than 16 distinct neighbors seen yet).
- **Jaccard verdict line**: Displays the computed $J(A, B)$ against τ and whether the two sketches qualify as co-present.

**Export JSON fields** (tap "Export" to write `mesh_export.json`):

- `config` — the full `MeshConfig` settings at export time (`epochMs`, `tauThreshold`, `rssiFloorDbm`, `codedPhy`, `advIntervalMs`).
- `heard[]` — one row per received frame:
  - `epoch` — which window this frame arrived in.
  - `markHex` — the sender's rotating 16-byte mark as a hex string. **This is NOT a stable device ID** — it rotates every epoch by design (derived from `blake3("mesh-core:v1:mark" || seed || epoch_le)[..16]`), so the same physical device appears under a different `markHex` each window.
  - `rssi` — received signal strength in dBm. More negative = weaker / farther. Rough field guide: `~-40` = same desk, `~-60` = same room, `~-80` = across the street.
  - `tsMs` — Unix timestamp in milliseconds when the frame was recorded.

**Body layout**: `body[0]` = length byte (0–63), `body[1..1+len]` = UTF-8 text, remainder zeroed. The Rust codec rejects any frame where the tail is not all-zero or the length byte exceeds 63.

**Frame hash and epoch re-appearance**: The frame-hash dedup key is `blake3(buf[0..214])[..16]` — it covers everything except the hop-mutable `reserved` region. Each new epoch produces a **new** signed frame (different `epoch` field, different `mark`), so the same message text will legitimately re-appear in the `heard[]` log each epoch — it is a distinct frame, not a rebroadcast storm.

---

## 9. Glossary of Terms

* **Advertising Interval** (`advIntervalMs`): How often the BLE radio repeats the current advertisement frame. Default 1000 ms. Shorter intervals make the node more visible to scanners at the cost of battery. Does not affect frame content or epoch timing.
* **AUX PDU (Auxiliary Packet Data Unit)**: In BLE 5 Extended Advertising, additional payload bytes (up to 255 B per packet) offloaded from the primary 31-byte legacy channels (channels 37, 38, 39) onto secondary data channels (channels 0–36).
* **BLAKE3**: An extremely fast, cryptographic hash function used in `cockroachat` for key derivation (`KDF`), mark hashing, KMV sketching, and beacon seed chaining.
* **Bridgefy Class Vulnerabilities**: A category of protocol design flaws documented in 2021/2022 security audits where mesh nodes relayed unparsed payloads (causing parsing crashes / zip bombs), relied on virtual identities (vulnerable to Sybil attacks), or allowed passive location tracking.
* **CellMismatch Alarm**: An internal security event generated when a received message claims a Proof-of-Co-Presence witness that fails to match the receiving device's locally observed physical cell (indicating a packet replay or relocation attack).
* **Chained Epoch Beacon**: A self-clocking, forward-unpredictable random seed generator where the seed for epoch $N$ is derived via $seed_N = \text{BLAKE3}(seed_{N-1} \parallel E_N)$. Prevents attackers from pre-computing future seeds or building offline surveillance tracking dictionaries.
* **Diversity Sketch**: A K-Minimum Values (KMV) sketch constructed solely from *distinct, locally-verified* physical cell digests. Used to determine how many independent geographic locations have corroborated a danger alert.
* **Ed25519 Ephemeral Signature**: An elliptic-curve signature scheme using public-key cryptography where keys rotate automatically every hour (`Ephemeral`), preventing long-term tracking of user devices.
* **Epoch ($T_{epoch}$)**: A fixed time window (typically 5 minutes) during which devices sample local ambient marks, compute cell sketches, and sync beacon state.
* **Epoch Skew**: A condition where two devices compute different epoch numbers for the same wall-clock moment — caused by clock drift between devices or by different `epochMs` settings. Epoch-skewed devices build sketches over non-overlapping windows, making Jaccard comparison meaningless. The debug app flags it when received frames carry an epoch number that differs from the local current epoch.
* **Frame Hash (dedup key)**: `blake3(buf[0..214])[..16]` — a 16-byte digest of everything in the frame except the hop-mutable `reserved` region. Relays store seen frame hashes and drop any frame whose hash has already been processed, preventing rebroadcast storms. Because the hash covers the `epoch` and `mark` fields, a legitimately re-originated frame (new epoch, new mark) gets a new hash and is not suppressed.
* **Jaccard Distance ($\tau$)**: A mathematical measure of similarity between two sets $A$ and $B$, defined as $J(A, B) = \frac{|A \cap B|}{|A \cup B|}$. In `cockroachat`, $J(A, B) \ge \tau$ determines whether two devices are physically co-present in the same cell.
* **KMV Sketch (K-Minimum Values)**: A probabilistic data structure that retains the $K$ smallest hash values of an observed dataset. Allows devices to compare physical cell composition in constant memory without transmitting raw observation lists.
* **LE Coded PHY**: A physical layer option introduced in Bluetooth 5 that uses Forward Error Correction (FEC) (S=2 or S=8) to quadruple radio range at the expense of lower data throughput.
* **Local Ambient Mark**: A 16-byte pseudo-random identifier emitted by nearby devices in non-propagating local broadcasts, sampled by nearby nodes to construct physical cell sketches.
* **Overflow Area (iOS CoreBluetooth)**: A special Apple-proprietary hashing mechanism used when an iOS app advertises service UUIDs in the background. Accessible only by other iOS devices explicitly scanning for those exact service UUIDs, rendering the advertisement invisible to Android background scanners.
* **Parse-Before-Forward**: A strict architectural invariant requiring every incoming packet to be completely decoded, length-checked, epoch-verified, signature-authenticated, and witness-checked before any decision is made to relay or display it.
* **Proof-of-Co-Presence (PoCP)**: A cryptographic protocol proving that a node was physically present in a specific crowd cell at a specific time by demonstrating knowledge of the ambient RF observations of that cell.
* **RSSI (Received Signal Strength Indicator)**: A measure of the power of a received radio signal, reported in dBm (decibels relative to one milliwatt). The scale is negative: 0 dBm = 1 mW (very strong), −40 dBm = same desk, −60 dBm = same room, −80 dBm = across the street. Weaker (more negative) values indicate greater distance or obstruction. `cockroachat` uses `rssiFloorDbm` to discard frames below a configurable threshold so that distant nodes do not pollute the local cell sketch.
* **Spacetime Witness**: A short message authentication code ($MAC_{KDF(cell \parallel seed)}(msg)$) embedded in a frame that authenticates a message against a specific physical cell sketch and epoch seed.
* **Trickle Algorithm**: A self-regulating, epidemic broadcast algorithm (RFC 6206) that adjusts retransmission intervals based on local network density ($K_{supp}$), preventing broadcast storms while ensuring rapid propagation over multi-hop networks.
* **TTL (Time-To-Live / hop budget)**: Stored at `reserved[0]`. Starts at 8 for Tier-2 frames. Each relay decrements it by 1 before rebroadcasting; a frame with TTL = 0 is not relayed further (it dies at the current node). Stored in the unsigned `reserved` region so relays can decrement without invalidating the Ed25519 signature.
* **UniFFI**: Mozilla’s multi-language binding generator tool used to expose the Rust `mesh-core` interface cleanly to Kotlin (Android) and Swift (iOS) shims.
* **VDL (Verifiable Delay Lottery)**: This project’s own term (not an external standard) for the Tier-3 origination cost gate. Each private-message origination must carry a witness in `pocp_wit` such that `blake3("mesh-core:v1:vdl" || frame[0..102] || witness)` has at least `VDL_DIFFICULTY_BITS` (v0 = 22) leading zero bits. Producing it costs the sender seconds of CPU (`vdl::solve`); any node verifies it with one hash (`vdl::verify`). This rate-limits how fast a single sender can inject opaque private frames into the mesh without requiring identity, accounts, or any trusted third party. **v0 is parallelizable proof-of-work, not a true sequential VDF** — it bounds spam per unit of compute, not per unit of wall-clock time; a sequential VDF can replace it behind the same interface later. Implemented in `mesh-core/src/vdl.rs`.
* **Pairwise Key (Tier-3 pairing)**: The symmetric key two devices share for private messaging. Each device holds a long-term X25519 secret (32 OS-random bytes, never leaves the device); they exchange public keys out-of-band (paste/QR/paper) and both compute the same key via `crypto::pair_derive` = blake3-domain-separated X25519 Diffie-Hellman. There is no recipient address on the wire — receivers trial-decrypt against every stored pairwise key, and a successful ChaCha20-Poly1305 tag check both selects the conversation and authenticates the sender.
* **Unsigned Hop-Mutable Region**: The final 12 bytes of the 226-byte frame (`reserved`), containing mutable metrics like TTL and hop count. Excluded from the Ed25519 signature so relays can decrement TTL without invalidating signatures.


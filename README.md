<![CDATA[<div align="center">

# 🪳 cockroachat

**Offline Decentralized Protest Mesh**

[![Rust](https://img.shields.io/badge/Core-Rust-b7410e?style=flat-square&logo=rust)](mesh-core/)
[![Android](https://img.shields.io/badge/Shim-Android%20(Kotlin)-3DDC84?style=flat-square&logo=android)](android/)
[![BLE 5.0](https://img.shields.io/badge/Transport-BLE%205.0%20Extended-0082FC?style=flat-square&logo=bluetooth)]()
[![License](https://img.shields.io/badge/License-TBD-lightgrey?style=flat-square)]()

*Phones relay short danger alerts over Bluetooth Low Energy without cell towers, Wi-Fi, servers, internet, or accounts.*

*Trust is grounded in **Physical Co-Presence** and **Spatial Diversity** — preventing remote adversaries from injecting, forging, or amplifying false alerts.*

</div>

---

> [!IMPORTANT]
> **v0 status — design target vs. shipped code.**
>
> The three protocol pillars and their current implementation state:
>
> | Defense | Status | Detail |
> |---|:---:|---|
> | **PoCP witness** (proximity gating) | ✅ Implemented | `pocp::witness` and `verify_witness_local` are wired into the ingest pipeline. Public frames with a valid PoCP witness are verified against the local cell sketch; mismatching or stale witnesses are dropped or flagged. |
> | **Beacon chain** (forward secrecy) | ✅ Implemented | `beacon::advance` computes chained seeds `seed_N = BLAKE3(seed_{N-1} ∥ E_N)` with local RF entropy, acceleration-floor checks, and low-entropy fallback. Marks and ephemeral signing keys are derived from the one-way chain. |
> | **Diversity trust** (multi-cell confidence) | 🔲 Stubbed | `trust::merge`, `distinct_estimate`, and `corroboration` return `todo!("M6")`. Single-cell origination is treated identically to multi-cell crowd corroboration. v0 tracks per-frame cell claims via `TrustState` but does not aggregate or threshold them. |
>
> **What v0 enforces today:** Ed25519 signature authentication (`verify_strict`), parse-before-forward codec safety (fixed 226-byte frames, no varint, no compression), frame-hash deduplication, PoCP witness verification against local cell sketches, chained beacon forward secrecy, danger-only alert filtering, VDL proof-of-work cost-gating on Tier-3 private frames, and ChaCha20-Poly1305 AEAD for pairwise messages.

---

## 📐 Architecture & Module Design

All security-critical logic lives in a single, memory-safe Rust core (`mesh-core`). Platform shims are thin layers responsible **only** for radio I/O, OS lifecycles, UI rendering, and key storage. A Linux laptop client links `mesh-core` directly for desktop testing via BlueZ.

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

### Module Status Matrix

| Module | Purpose | Status | Tests |
|:---|:---|:---:|:---:|
| `codec` | Zero-allocation fixed 226-byte encoder/decoder. Strict total-failure semantics. | ✅ | 9 |
| `crypto` | Ed25519 ephemeral signing, BLAKE3 KDF, X25519 DH, ChaCha20-Poly1305 AEAD | ✅ | 8 |
| `message` | Frame origination (`originate`, `originate_private`), `body_text`, `frame_hash` | ✅ | 19 |
| `pocp` | KMV sketches, Jaccard similarity, spacetime witness MAC, `verify_witness_local` | ✅ | 18 |
| `beacon` | Self-clocking chained hash beacon, entropy gathering, acceleration floor | ✅ | 13 |
| `private` | Tier-3 ChaCha20-Poly1305 seal/open with epoch+mark nonces | ✅ | 6 |
| `vdl` | Proof-of-work cost gate (`solve`/`verify`, 22-bit difficulty) | ✅ | 5 |
| `statemachine` | Parse→Verify→Decide→Forward pipeline, `relay_decision`, `Dedup` | ✅ | 12 |
| `trust` | Cell-claim tracking & anti-inflation. **Aggregation stubbed** (`todo!("M6")`) | 🔲 | 5 |
| `store` | Panic-wipe trigger. Persistence lives on platform shims. | ✅ | — |
| `ffi` | UniFFI exports (30+ functions) consumed by Android Kotlin shim | ✅ | 9 |
| `radio` | `RadioPort` trait definition for platform transport seam | ✅ | — |

> **Test suite:** 115 unit + property + integration tests across the core. Includes 10 `proptest` property-based tests for codec zero-panic/roundtrip and PoCP determinism/symmetry.

### Laptop Client (`laptop/`)

A Linux desktop mesh node built on `bluer` (BlueZ async bindings) and `tokio`. Links `mesh-core` directly (no UniFFI). Advertises via BLE Extended Advertising (1M PHY), scans for peers, computes per-epoch KMV sketches, and accepts interactive text input from stdin. Useful for protocol debugging without a phone.

---

## 🛡️ Threat Model & Bridgefy Vulnerabilities Addressed

`cockroachat` is explicitly engineered to overcome the systemic flaws discovered in early mesh apps like Bridgefy (documented in the 2021 CT-RSA and 2022 USENIX Security studies):

### 1. Parse-Before-Forward Security (Anti-Zip-Bomb Boundary)

> **Bridgefy Flaw:** Relayed payloads before parsing/validating them, enabling single malformed packets to crash every node on the mesh.

**cockroachat Fix:** Enforces a strict `Parse → Verify → Decide → Forward` execution pipeline inside a memory-safe Rust core. Nothing is relayed or presented to the UI until length checks, signature verification, and witness checks pass completely.

### 2. Physical Spatial Diversity over Virtual Web-of-Trust

> **Bridgefy Flaw:** Relied on digital identity tokens or accumulated reputation, enabling Sybil attacks with thousands of virtual identities.

**cockroachat Fix:** Ignores virtual identity counts. A message gains confidence only as it is verified by distinct *geographic cells* (derived from physical ambient RF observations). An off-site adversary creating 10,000 virtual identities across two physical devices produces zero spatial entropy and is ignored by the crowd.

### 3. Chained Epoch Beacon (Forward Secrecy / Coercion Resistance)

> **Bridgefy Flaw:** A seized device could have its static identity seed extracted, allowing retrospective reconstruction of the user's entire session history.

**cockroachat Fix:** Derives marks and ephemeral signing keys from a one-way hash-chain beacon (`seed_N = BLAKE3(seed_{N-1} ∥ E_N)` where `E_N` is ambient entropy from LocalImmediate marks). Past beacon seeds are unrecoverable from the current seed, so post-seizure analysis cannot reconstruct prior marks or public keys. A seized device yields the current epoch only — not the user's full protest history.

### 4. Danger-Only Asymmetric Alerting

> **Bridgefy Flaw:** Symmetric messaging allowed attackers to inject false "ALL CLEAR" broadcasts to lure protesters into traps.

**cockroachat Fix:** The public mesh only carries danger alerts. Silence is never interpreted as safety, and nodes cannot broadcast "safe" status on the public plane.

### 5. Open Public Plane Transparency

> **Bridgefy Flaw:** Attempted to wrap public broadcast alerts in broken E2E crypto wrappers, giving users a false sense of privacy while leaking metadata.

**cockroachat Fix:** The public plane is openly unencrypted and authenticated per message. It carries public danger signals that are already physically visible to anyone present.

---

## 📦 Fixed 226-Byte Wire Protocol

Every `cockroachat` packet is serialized into a strict **226-byte fixed-size binary layout** to maximize BLE Extended Advertising reliability without fragmentation. Variable-length fields, TLVs, and compression are strictly prohibited.

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

| Byte Range | Field | Size | Description |
|:---|:---|:---|:---|
| `0..16` | `mark` | 16 B | Pseudo-random ID for deduplication, Trickle suppression, and local entropy |
| `16..18` | `hdr` | 2 B | Version (4b), Packet Type (4b), Flags/Tier (8b) |
| `18..34` | `div_sketch` | 16 B | KMV sketch digest / AEAD nonce counter (first 8 bytes) |
| `34..38` | `epoch` | 4 B | Big-endian epoch index from self-clocking beacon chain |
| `38..102` | `body` | 64 B | Danger alert text or structured emergency codes |
| `102..118` | `pocp_wit` | 16 B | PoCP witness MAC (public) / VDL proof-of-work witness (Tier-3) |
| `118..150` | `pk` | 32 B | Ephemeral Ed25519 public key |
| `150..214` | `sig` | 64 B | Ed25519 signature over canonical bytes `[0..150)` |
| `214..226` | `reserved` | 12 B | Unsigned hop-mutable: TTL, hop count, RSSI metrics |

---

## 🔐 Cryptographic & Protocol Mechanics

### Proof-of-Co-Presence (PoCP)

1. **Ambient Mark Sampling** — Each device passively records ambient marks from BLE advertisements emitted by nearby nodes (~30m radius).
2. **KMV Sketch Construction** — Overheard marks are hashed and truncated into a K-Minimum Values (KMV) bottom-*k* sketch representing the device's immediate physical cell.
3. **Spacetime Witness** — The originator computes `BLAKE3_MAC(witness_key(div_sketch, seed), frame[0..102])` and embeds it in the `pocp_wit` field.
4. **Proximity Verification** — Receivers verify the witness MAC, then compute Jaccard distance between the frame's `div_sketch` and their own local sketch:

$$J(A, B) = \frac{|A \cap B|}{|A \cup B|} \ge \tau$$

A cell mismatch triggers a `CellMismatch` security alert, identifying relocation or replay attacks.

### Chained Epoch Beacon

- **Seed Chaining:** $seed_N = \text{BLAKE3}(\texttt{"mesh-core:v1:beacon-advance"} \parallel seed_{N-1} \parallel E_N)$
- **Entropy Source ($E_N$):** Digest of deduplicated marks from *non-propagating* local traffic during epoch $N-1$. Because $E_N$ is observed locally, an off-site surveillance node cannot observe or predict it.
- **Acceleration Floor:** Nodes only advance to $seed_N$ if the configured `floor_ms` has elapsed since $seed_{N-1}$, preventing clock-acceleration attacks.
- **Low-Entropy Fallback:** If fewer than `min_hearers` unique marks were observed, `fallback_local` advances with all-zero entropy and flags `low_entropy = true` for downstream handling.

### Spatial Diversity / Trust

> **v0 status:** `trust::merge`, `distinct_estimate`, and `corroboration` return `todo!("M6")`. The per-frame cell-claim tracker (`TrustState`) is implemented — it records distinct div-sketch claims per frame hash, enforces anti-inflation via Jaccard dissimilarity checks, and caps storage at 4096 entries with FIFO eviction. The aggregation layer that distinguishes "10,000 virtual identities on 2 physical devices" from "10,000 real devices in distinct cells" is not yet built.

---

## 📡 Multi-Tier Broadcast Strategy

| Tier | Name | Range | Origination Gate | Relay / Trust Gate |
|:---:|:---|:---|:---|:---|
| **1** | Local-Immediate | 0–1 hop (~30m) | PoCP witness + Ed25519 sig | Direct display (human ground truth) |
| **2** | Regional-Propagated | Multi-hop flood | PoCP witness + Ed25519 sig | Trickle rebroadcast. Confidence scales with distinct local cells. |
| **3** | Private-Directed | E2E multi-hop | X25519 key exchange + VDL cost proof | ChaCha20-Poly1305 ciphertext over flood; relays require valid VDL witness. |

### Tier-2 Flood

TTL hop counter at `reserved[0]` (starts at 8, each relay decrements, dies at 0). Deduplication by frame hash prevents storms. The relay decision lives in `statemachine::relay_decision` in the Rust core — platform shims never make relay choices.

### Tier-3 Private Messaging

Two devices pair out-of-band — each generates a long-term X25519 secret, exchanges public keys (paste/QR/paper), and both derive an identical pairwise key via `crypto::pair_derive` (X25519 DH → BLAKE3 domain-separated KDF).

A private message seals its 64-byte body with ChaCha20-Poly1305 (`private::seal_private_body`): nonce = `epoch_be ∥ BLAKE3("mesh-core:v1:nonce" ∥ sender_pk ∥ counter_be)[..8]`, plaintext block `[len][utf-8 ≤47 bytes][zero pad]`.

**No recipient address on the wire** — the receiver trial-decrypts every incoming private frame against each stored pair key; a tag match both selects the conversation and authenticates the sender. The frame carries `MsgType::Private` (wire byte 17 = 3).

### Tier-3 DoS Protection (VDL)

Private bodies are opaque to relays, so without a cost function an attacker could flood junk ciphertext. **VDL (Verifiable Delay Lottery)** requires each private origination to carry a proof-of-work witness: `BLAKE3("mesh-core:v1:vdl" ∥ frame[0..102] ∥ witness)` must have ≥ `VDL_DIFFICULTY_BITS` (22) leading zero bits. Finding the witness costs seconds of CPU (`vdl::solve`); any relay verifies it with a **single hash** (`vdl::verify`).

> **Honest limitation:** v0 is parallelizable proof-of-work, not a sequential verifiable delay function — it bounds spam per unit of compute, not per unit of wall-clock time. A true sequential VDF can drop in behind the same `solve`/`verify` interface later.

---

## 📱 Mobile Platform Implementations & BLE Realities

### BLE Transport Mode
- Uses **BLE 5.0 Extended Advertising** (AUX_ADV_IND PDUs) on **LE Coded PHY** (maximum range under crowded conditions).
- Packets are broadcast as non-connectable, undirected extended advertisements carrying the 226-byte payload.

### Android Shim
- `BluetoothLeAdvertiser` with extended advertising + `BluetoothLeScanner` with low-latency filters.
- Runs inside a Foreground Service with persistent notifications to survive Doze mode and OS process sweeps.

### iOS Shim & Background Constraints
- CoreBluetooth background mode strips local names and moves service UUIDs into an undocumented **overflow area** (readable only by other scanning iOS devices).
- Background central mode suppresses non-connectable advertisements entirely.
- **Accommodation:** Connectable background advertisements when necessary; prompts for active foreground mode during marches.
- **Hardware Pocket-Beacon Relays:** Pocketable microcontrollers (nRF52840 or ESP32-C3) running `mesh-core` act as un-throttled mesh backbone repeaters.

---

## 🔒 Non-Negotiable Security Invariants

| # | Invariant | Rationale |
|:---:|:---|:---|
| 1 | **One Codec in Rust** | Platform shims never parse or construct frames. Raw 226-byte arrays only. |
| 2 | **Parse → Verify → Decide → Forward** | Fixed processing order: length → epoch → mark unseen → sig verify → witness → state machine |
| 3 | **Fixed 226-Byte Frame** | No variable-length fields, no compression, no optional headers |
| 4 | **Danger-Only Alerts** | Public plane carries danger only. Never transmit "safe" or "all clear". |
| 5 | **Ephemeral Keys & Minimal Persistence** | Keys rotate with beacon chain. `panic_wipe()` purges all state immediately. |
| 6 | **Unencrypted Public Plane** | Authenticated, not private. Never label it E2E encrypted. |
| 7 | **Physical Spatial Trust** | Trust from physical cell corroboration only, never accumulated reputation. |

---

## 🔧 Build & Verification

```bash
# Navigate to core library
cd mesh-core

# Run full test suite (crypto KATs, wire codec, PoCP, beacon, private, VDL, statemachine)
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

The debug APK and laptop Rust client expose every tunable parameter with live readouts. All values persist in Android SharedPreferences (`mesh_cfg`).

| Parameter | Default | What it controls |
|:---|:---|:---|
| `epochMs` | `10000` (10s) | Measurement window length. **All test devices must match.** Epoch = `unix-time-ms / epochMs`. |
| `tauThreshold` (τ) | `0.5` | Jaccard similarity cutoff. ≥ τ = same cell. The single most important calibration knob. |
| `rssiFloorDbm` | `-80` | Signal-strength floor (dBm). Filters distant observers from cell sketch. |
| `advIntervalMs` | `1000` | BLE advertisement repeat interval (ms). Lower = more visible, higher battery cost. |
| `codedPhy` | `true` | BLE long-range mode (LE Coded PHY, S=8). ~4× range, requires BT5 on both ends. |

### Live Debug Readouts

| Readout | Description |
|:---|:---|
| **Epoch** | Current window number. Verify all test devices show the same value. |
| **Neighbors** | Distinct marks heard this epoch (after RSSI filter). Crowd density indicator. |
| **Total Rx** | All frames received since app start, after frame-hash dedup. |
| **Sketch[0..3]** | First 4 slots of the 16-slot KMV sketch. `0xffff…` = empty padding. |
| **Jaccard verdict** | Computed $J(A,B)$ against τ and co-presence verdict. |

### Export JSON Fields

Tap "Export" to write `mesh_export.json`:

| Field | Description |
|:---|:---|
| `config` | Full `MeshConfig` at export time |
| `heard[]` | One row per received frame |
| `heard[].epoch` | Which window this frame arrived in |
| `heard[].markHex` | Sender's rotating 16-byte mark (hex). **Not a stable device ID** — rotates every epoch. |
| `heard[].rssi` | Signal strength (dBm). `-40` = same desk, `-60` = same room, `-80` = across the street. |
| `heard[].tsMs` | Unix timestamp (ms) |

**Body layout:** `body[0]` = length byte (0–63), `body[1..1+len]` = UTF-8 text, remainder zeroed. The codec rejects frames where the tail is not all-zero or length exceeds 63.

**Frame hash dedup key:** `blake3(buf[0..214])[..16]` — covers everything except the hop-mutable `reserved` region. Same text re-originated in a new epoch gets a new hash (different `epoch` + `mark` fields), so it legitimately re-appears in `heard[]`.

---

## 📖 Glossary

<details>
<summary><strong>Click to expand full glossary (25 terms)</strong></summary>

| Term | Definition |
|:---|:---|
| **Advertising Interval** | How often the BLE radio repeats the current advertisement frame. Default 1000 ms. |
| **AUX PDU** | BLE 5 Extended Advertising auxiliary payload (up to 255 B) offloaded onto secondary data channels. |
| **BLAKE3** | Fast cryptographic hash used for KDF, mark hashing, KMV sketching, and beacon seed chaining. |
| **Bridgefy Class Vulnerabilities** | Protocol design flaws from 2021/2022 audits: unparsed relay, Sybil-vulnerable identity, location tracking. |
| **CellMismatch Alarm** | Security event when a PoCP witness fails to match the receiver's local cell sketch. |
| **Chained Epoch Beacon** | Forward-unpredictable seed generator: $seed_N = \text{BLAKE3}(seed_{N-1} \parallel E_N)$. |
| **Diversity Sketch** | KMV sketch from distinct, locally-verified physical cell digests. |
| **Ed25519 Ephemeral Signature** | Elliptic-curve signature with keys rotating via the beacon chain for forward secrecy. |
| **Epoch ($T_{epoch}$)** | Fixed time window for mark sampling, sketch computation, and beacon sync. |
| **Epoch Skew** | Different epoch numbers at the same wall-clock time from clock drift or `epochMs` mismatch. |
| **Frame Hash** | `blake3(buf[0..214])[..16]` — dedup key covering all signed fields. |
| **Jaccard Distance (τ)** | $J(A,B) = \|A \cap B\| / \|A \cup B\|$. ≥ τ = co-present in the same cell. |
| **KMV Sketch** | Probabilistic data structure retaining K smallest hash values for constant-memory set comparison. |
| **LE Coded PHY** | BLE 5 physical layer with FEC (S=8) for ~4× range at lower throughput. |
| **Local Ambient Mark** | 16-byte pseudo-random ID from nearby devices in non-propagating broadcasts. |
| **Overflow Area** | Apple-proprietary CoreBluetooth background service UUID hashing, invisible to Android scanners. |
| **Pairwise Key** | Symmetric key from X25519 DH + BLAKE3 domain-separated KDF for Tier-3 private messaging. |
| **Parse-Before-Forward** | Invariant: decode, verify, authenticate before any relay or display decision. |
| **PoCP** | Proof-of-Co-Presence: cryptographic proof of physical presence via ambient RF observation. |
| **RSSI** | Received signal strength (dBm). -40 = same desk, -60 = same room, -80 = across the street. |
| **Spacetime Witness** | MAC embedding in frame that authenticates against a specific cell sketch and epoch seed. |
| **Trickle Algorithm** | Self-regulating epidemic broadcast (RFC 6206) with density-adaptive retransmission. |
| **TTL** | Hop budget at `reserved[0]`. Starts at 8, decremented per relay, dies at 0. |
| **UniFFI** | Mozilla's binding generator exposing Rust `mesh-core` to Kotlin and Swift shims. |
| **VDL** | Verifiable Delay Lottery — project-specific Tier-3 cost gate. 22-bit PoW, single-hash verify. v0 is parallelizable PoW, not sequential VDF. |

</details>

---

<div align="center">
<sub>Built for protesters. No accounts. No servers. No internet. Just mesh.</sub>
</div>
]]>

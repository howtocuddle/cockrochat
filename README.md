# cockroachat — Offline Decentralized Protest Mesh

`cockroachat` is a decentralized, infrastructure-free mesh messaging and alert protocol built for protests, civil demonstrations, and emergency situations under hostile electronic surveillance or network blackouts.

Phones relay short **danger alerts** over Bluetooth Low Energy (BLE 5.0 Extended Advertising) without cell towers, Wi-Fi routers, central servers, internet access, or user accounts. Trust in `cockroachat` is grounded in **Physical Co-Presence** and **Spatial Diversity**, preventing remote adversaries (such as a surveillance van running virtual nodes off-site) from injecting, forging, or amplifying false alerts.

---

## 1. Threat Model & Bridgefy Vulnerabilities Addressed

`cockroachat` is explicitly engineered to overcome the systemic flaws discovered in early mesh apps like Bridgefy (documented in the 2021 CT-RSA and 2022 USENIX Security studies):

1. **Parse-Before-Forward Security (Anti-Zip-Bomb Boundary)**
   - *Bridgefy Flaw*: Relayed payloads before parsing/validating them, enabling single malformed packets to crash every node on the mesh.
   - *`cockroachat` Fix*: Enforces a strict `Parse -> Verify -> Decide -> Forward` execution pipeline inside a memory-safe Rust core. Nothing is relayed or presented to the UI until length checks, signature verification, and witness checks pass completely.

2. **Physical Spatial Diversity over Virtual Web-of-Trust**
   - *Bridgefy Flaw*: Relied on digital identity tokens or accumulated reputation, enabling an adversary to spawn thousands of virtual identities (Sybil attack) to hijack trust algorithms.
   - *`cockroachat` Fix*: Ignores virtual identity counts. A message gains confidence only as it is verified by distinct *geographic cells* (derived from physical ambient RF observations). An off-site adversary creating 10,000 virtual identities across two physical devices produces zero spatial entropy and is ignored by the crowd.

3. **Chained Epoch Beacon (Anti-Precomputation without VDF overhead)**
   - *Bridgefy Flaw*: Static or predictable identifiers allowed state actors to build pre-computed rainbow tables and surveillance tracking dictionaries.
   - *`cockroachat` Fix*: Implements a forward-unpredictable hash-chain beacon (`seed_N = H(seed_{N-1} || E_N)`). Because future ambient RF entropy ($E_N$) does not exist until epoch $N-1$ occurs, no adversary—regardless of computational power—can pre-compute future beacon seeds or build offline tracking dictionaries.

4. **Danger-Only Asymmetric Alerting**
   - *Bridgefy Flaw*: Symmetric messaging allowed attackers to inject false "ALL CLEAR" or "SAFE HERE" broadcasts to lure protesters into traps.
   - *`cockroachat` Fix*: The public mesh only carries danger alerts. Silence is never interpreted as safety, and nodes cannot broadcast "safe" status on the public plane.

5. **Open Public Plane Transparency**
   - *Bridgefy Flaw*: Attempted to wrap public broadcast alerts in broken E2E crypto wrappers, giving users a false sense of privacy while leaking metadata.
   - *`cockroachat` Fix*: The public plane is openly unencrypted and authenticated per message. It carries public danger signals that are already physically visible to anyone present.

---

## 2. Architecture & Module Design

All security-critical logic (codec parsing, cryptography, Proof-of-Co-Presence, beacon chaining, trust aggregation, and protocol state machine) resides in a single, memory-safe Rust core (`mesh-core`). Platform shims in Kotlin (Android) and Swift (iOS) are thin layers responsible **only** for radio hardware I/O, OS background lifecycles, UI rendering, and secure key storage.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            Rust Core (mesh-core)                            │
│ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌───────┐ ┌───────────┐ │
│ │  codec   │ │  crypto  │ │   pocp   │ │  beacon  │ │ trust │ │statemach. │ │
│ └──────────┘ └──────────┘ └──────────┘ └──────────┘ └───────┘ └───────────┘ │
└──────────────────────▲───────────────────────────────▲──────────────────────┘
                       │ UniFFI (Generated Bindings)   │
           ┌───────────┴──────────┐        ┌────────────┴──────────┐
           │ Android Shim         │        │ iOS Shim              │
           │ (Kotlin + BLE 5)     │        │ (Swift + CoreBluetooth)│
           └──────────────────────┘        └───────────────────────┘
```

### Rust Core (`mesh-core`) Modules

* **`codec`**: Zero-allocation, non-panicking, fixed-offset 194-byte encoder/decoder. Strict total-failure semantics: any byte-length deviation or malformed header results in immediate drop with zero side effects.
* **`crypto`**: Ephemeral Ed25519 signature scheme with hourly key rotation, domain-separated hashing (BLAKE3), and ChaCha20-Poly1305 AEAD routines.
* **`pocp`**: Proof-of-Co-Presence engine. Constructs K-Minimum Values (KMV) fuzzy sketches from overheard ambient BLE marks, evaluates Jaccard similarity ($\tau$), and verifies spacetime witnesses ($MAC_{KDF(cell \parallel seed)}(msg)$).
* **`beacon`**: Self-clocking, chained hash beacon. Computes dynamic epoch seeds using locally observed non-propagating mark entropy and enforces acceleration floor constraints.
* **`trust`**: Spatial diversity aggregator. Tracks distinct, locally verified physical cell digests and flags spatial anomalies (`CellMismatch` relocation alarms).
* **`statemachine`**: Core packet processing engine. Controls seen-set Bloom filters, Trickle suppression timers ($K_{supp}$, $W$, RSSI slot biasing), TTL/hop management, and alert dispatch.
* **`store`**: Size-capped, memory-bounded persistent storage with automated auto-decay and hardware panic-wipe capabilities.

---

## 3. Fixed 194-Byte Wire Protocol Spec

To maximize transmission reliability over BLE Extended Advertising without fragmentation, every `cockroachat` packet is serialized into a strict **194-byte fixed-size binary layout**. Variable-length fields, TLVs, and compression are strictly prohibited.

```
+-------------------+------------------+-----------------------+--------------------+
|  mark (16 B)      |  hdr (2 B)       |  div_sketch (16 B)    |  epoch (4 B)       |
|  [0..16)          |  [16..18)        |  [18..34)             |  [34..38)          |
+-------------------+------------------+-----------------------+--------------------+
|  body (64 B)                         |  pocp_wit (16 B)                           |
|  [38..102)                           |  [102..118)                                |
+--------------------------------------+--------------------------------------------+
|  sig (64 B)                                                                       |
|  [118..182)                                                                       |
+-----------------------------------------------------------------------------------+
|  reserved (12 B) [Unsigned, Hop-Mutable: TTL / Hop Count]                         |
|  [182..194)                                                                       |
+-----------------------------------------------------------------------------------+
```

### Frame Field Breakdown

| Byte Range | Field Name | Type / Size | Description |
|---|---|---|---|
| `0..16` | `mark` | `[u8; 16]` | Pseudo-random message identifier used for deduplication, Trickle suppression, and local entropy generation. |
| `16..18` | `hdr` | `[u8; 2]` | Packet header: Version (4 bits), Packet Type (4 bits), Flags/Tier (8 bits). |
| `18..34` | `div_sketch` | `[u8; 16]` | KMV sketch digest representing spatial cell diversity. |
| `34..38` | `epoch` | `u32` (BE) | Big-endian epoch index derived from the self-clocking beacon chain. |
| `38..102` | `body` | `[u8; 64]` | Payload area containing danger alert text or structured emergency codes. |
| `102..118` | `pocp_wit` | `[u8; 16]` | Proof-of-Co-Presence witness: $MAC_{KDF(cell \parallel seed)}(msg)$. Validates local physical presence. |
| `118..182` | `sig` | `[u8; 64]` | Ephemeral Ed25519 signature authenticating canonical bytes `[0..118)`. |
| `182..194` | `reserved` | `[u8; 12]` | Unsigned, hop-mutable region containing TTL ($H_{max}$), hop count, and RSSI metrics. Modified in-flight without invalidating `sig`. |

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

---

## 5. Multi-Tier Broadcast Strategy

| Tier | Name | Latency / Range | Origination Gate | Relaying / Trust Gate |
|---|---|---|---|---|
| **Tier 1** | Local-Immediate | Instant (0-1 hop, ~30m) | Valid PoCP witness + ephemeral Ed25519 sig | Direct display to nearby devices (human ground truth verification). |
| **Tier 2** | Regional-Propagated | Multi-hop flood (seconds) | Valid PoCP witness + ephemeral Ed25519 sig | Rebroadcast via Trickle algorithm ($K_{supp}$). Confidence scales only when verified by $\ge k$ distinct local cells. |
| **Tier 3** | Private-Directed (Deferred) | End-to-end multi-hop | QR/NFC out-of-band key exchange | E2E encrypted via Noise protocol over flood transport. |

---

## 6. Mobile Platform Implementations & BLE Realities

### BLE Transport Mode
- Uses **BLE 5.0 Extended Advertising** (AUX_ADV_IND PDUs) on **LE Coded PHY** (for maximum range under crowded conditions).
- Packets are broadcast as non-connectable, undirected extended advertisements carrying the 194-byte payload.

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

1. **One Codec in Rust**: Platform shims (Kotlin/Swift) must never parse or construct frame fields. They pass raw 194-byte arrays directly to `mesh-core`.
2. **Parse -> Verify -> Decide -> Forward**: Processing order is fixed: `Length check -> Epoch window -> Mark unseen -> Signature verify -> Witness check -> State machine decision`.
3. **Fixed 194-Byte Frame**: No variable-length fields, no compression, no optional headers. Deviation results in silent drop.
4. **Danger-Only Alerts**: The public plane carries danger alerts only. Never transmit "safe" or "all clear" signals.
5. **Ephemeral Keys & Minimal Persistence**: Identity keys rotate hourly. Storage automatically decays, and `panic_wipe()` immediately purges all state.
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

# Fuzz the 194-byte parser boundary (Nightly toolchain required)
cargo +nightly fuzz run decode -- -max_total_time=60
```

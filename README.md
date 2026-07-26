# bileichat

**Offline Decentralized Mesh Messaging for Protests & Emergencies**

[![Rust Core](https://img.shields.io/badge/Core-Rust%202024-b7410e?style=flat-square&logo=rust)](mesh-core/)
[![Android](https://img.shields.io/badge/Platform-Android%20(Kotlin%20%2B%20Compose)-3DDC84?style=flat-square&logo=android)](android/)
[![BLE 5.0](https://img.shields.io/badge/Transport-BLE%205.0%20Extended-0082FC?style=flat-square&logo=bluetooth)]()
[![License](https://img.shields.io/badge/License-AGPL--3.0-orange?style=flat-square)](mesh-core/Cargo.toml)
[![Version](https://img.shields.io/badge/Latest-v0.18--relaygate-teal?style=flat-square)]()

*Phones relay emergency alerts directly to each other using Bluetooth Low Energy — no cell towers, Wi-Fi routers, central servers, internet access, or user accounts required.*

---

## What is bileichat?

During protests, civil demonstrations, or natural disasters, cellular networks and Wi-Fi are frequently jammed, monitored, or shut down.

**bileichat** turns nearby smartphones into a resilient, self-healing peer-to-peer mesh network. Devices pass short emergency alerts phone-to-phone through the crowd using BLE 5.0 Extended Advertising — completely automatically, with zero configuration.

### Key Highlights
- **100% Offline & Serverless**: Works entirely over Bluetooth Low Energy (BLE 5.0). No accounts, no phone numbers, no internet.
- **Anti-Fake Alert Protection**: Tier-1 local alerts require a Proof-of-Co-Presence (PoCP) witness; witnessless public frames are relay-only and never displayed. Broadcast corroboration counts only claims heard directly over the air — and is shown as a hint, never as a guarantee (a determined *nearby* attacker can forge claims; see §Tier 2 below).
- **Self-Destructing Identity**: Marks and signing keys rotate every epoch over a one-way beacon chain. v2 private pairings ratchet message keys every epoch from a seed mixed with deleted pairing salts — a seized phone exposes at most the current and previous epoch of private history.
- **Crash-Proof Rust Core**: Every packet is parsed and verified in memory-safe Rust before forwarding, preventing crash-attacks (zip bombs).
- **Danger-Only Alerts**: The public mesh strictly carries danger signals (e.g. teargas, police kettling, medical emergency). Silence is never assumed to mean safety.
- **Instant Panic Wipe**: A single long-press instantly zero-fills and purges all stored state and cryptographic keys.

---

## Platform Support

| Component | Platform | Language | Status |
|:---|:---|:---|:---|
| **[`mesh-core`](mesh-core/)** | Core Library | Rust 2024 | Active — all protocol parsing, security, cryptography, and relay state machine |
| **[`android`](android/)** | Android App | Kotlin + Jetpack Compose | Active — foreground service handling BLE 5.0 Extended Advertising and UI (min SDK 26, target 35) |
| **[`laptop`](laptop/)** | Linux Desktop | Rust (BlueZ/D-Bus) | Experimental — CLI BLE mesh node for field testing and KMV debug |

---

## Architecture & 3-Tier Messaging Model

The protocol uses a 3-tier messaging model to balance latency, crowd coverage, and security:

```
+───────────────────────────────────────────────────────────────────────────────+
│                       3-Tier Messaging Architecture                         │
+───────────────────────────────────────────────────────────────────────────────+

  Tier 1: Immediate Local Broadcast (~30m Radius)
  [ Sender ] ───> (BLE Extended Adv, TTL=1) ───> [ Nearby Nodes ]
   • 1-Hop direct proximity broadcast for immediate danger ground truth.
   • Authenticated by Proof-of-Co-Presence (PoCP) physical witness.

  Tier 2: Multi-Hop Regional Mesh Flood
  [ Sender ] ───> [ Relay Node 1 ] ───> [ Relay Node 2 ] ───> [ Crowd Mesh ]
   • Multi-hop flood re-broadcasted through the crowd.
   • Prioritized relay queue, epoch-bucketed frame-hash dedup & TTL limits.

  Tier 3: Encrypted Direct Private Message
  [ Sender ] ════════════════════════════════════════════> [ Recipient ]
   • End-to-end encrypted pairwise message (ChaCha20-Poly1305).
   • Gated by Proof-of-Work (VDL witness) to prevent network spam.

+───────────────────────────────────────────────────────────────────────────────+
```

### Tier Summary

1. **Tier 1 — Immediate Local Alerts (~30m)**: Instant alerts broadcasted to people right next to you. Display requires a valid Proof-of-Co-Presence witness; the sender repeats until it hears its own echo, then re-airs sparsely for up to 30 minutes.
2. **Tier 2 — Crowd-Relayed Regional Alerts**: Multi-hop alerts propagated through the mesh. Displayed frames carry a valid witness; the badge shows how many *distinct, directly overheard* devices vouched for the same alert body — a hint, not a guarantee.
3. **Tier 3 — Encrypted Direct Messages**: Pairwise private messages between trusted contacts with built-in spam protection (Proof-of-Work) and epoch-ratcheted forward-secret keys (v2 pairing).

### Real-World Crowd Propagation Examples

#### **Tier 1 Example: Immediate Local Alert (1-Hop / Direct RF Range)**
* **Scenario**: A user at the **North Gate** sees teargas deployed nearby and sends an immediate local alert *"TEAR GAS AT NORTH GATE"*.
* **Flow**:
  1. The frame is generated with `TTL = 1` and `MsgType::LocalImmediate`, carrying a PoCP witness bound to the sender's cell sketch (at epoch rollover, the previous epoch's completed sketch is signed and accepted).
  2. Broadcasted directly via BLE Extended Advertising to all devices within **10–30 meters** (1-hop direct radio range).
  3. **Display**: Displays on nearby devices **only if the PoCP witness verifies** against the receiver's own cell sketch. Frames with no witness are **relay-only — never displayed** (a remote injector cannot pass the co-presence gate without hearing the cell's marks).
  4. **Propagation**: Relayed **exactly once** — any receiving node relays it with TTL clobbered to 0, so the hop bound holds even against an adversary advertising `ttl=255`.
  5. **Receipt**: When the originator hears its own relayed echo, it knows at least one peer carried it (an echo is *not* proof of delivery — a single relay can forge it). The sender then re-airs the alert sparsely (every 4th epoch) until a 30-minute cap, instead of screaming every epoch forever.
  6. **Beacon Entropy**: Nearby devices collect the frame's sender mark as a physical co-presence witness (`localImmediateMarks`) to generate beacon entropy for key rotation.

#### **Tier 2 Example: Crowd-Relayed Regional Mesh Broadcast (Multi-Hop / Spatial Diversity)**
* **Scenario**: A user at the **North Gate** broadcasts a regional warning *"POLICE KETTLING NORTH EXIT"*.
* **Flow**:
  1. **Origination**: The packet is sent with `TTL = 8` (`MsgType::RegionalPropagated`) carrying the sender's local cell sketch and a signed PoCP witness.
  2. **Display Gate — valid witness required**: Before any device shows the alert, it checks that the witness MAC (sender's cell sketch vs receiver's observed marks) passes — **any Jaccard outcome** qualifies; a failed MAC check means the frame is relay-only, never displayed. This prevents remote injectors who aren't in the crowd from making their frame pop up on any phone.
  3. **Corroboration as hint, not lock**: Each receiving node that already holds the same body text from another overheard direct transmission increments a local counter. The UI displays this count on the badge: *"3 nearby devices just sent the same alert"*. This is **not** a verified count — a single nearby attacker can fabricate multiple claims — but in practice, a high count across different physical locations increases confidence. The corroboration score is scoped to the device's own radio observations; it does not aggregate across the mesh.
  4. **Mesh Hopping**: The packet hops phone-to-phone across the crowd, each relay decrementing TTL and pushing it onto the GATT plane too (`relayOnce` on both advertisement and GATT). Relay nodes use a priority queue (LOCAL echo > regional > private), draining only when `radio.capacityAvailable()`.
  5. **TTL clobbering**: Unlike the old design where TTL was trusted from the wire, every hop now clobbers TTL to `min(ttl, originator_ttl - hop_count)` so an adversary cannot inflate range by setting `ttl=255`.
  6. **Loop Suppression**: Originators stop re-broadcasting once they hear their own reflection (`ownFrameHash`), and relay nodes suppress duplicates using a per-epoch bounded dedup bucket (capped at 1024 entries, 0-indexed by frame-hash byte mod 1024).

#### **Tier 3 Example: Encrypted Direct Private Chat (End-to-End AEAD / Oblivious Mesh)**
* **Scenario**: Alice wants to send a private message to Bob *"Meet at South Entrance in 10 mins"* in a dense crowd.
* **Flow**:
  1. **v2 Out-of-Band Pairing**: Before any private messages can be exchanged, Alice and Bob pair by scanning each other's QR codes. Each QR carries a public key **plus a 32-byte random salt**. The shared chain seed is `BLAKE3(X25519_DH(alice_sk, bob_pk) ‖ sort(alice_salt, bob_salt))`. Both salts exist only in memory and are deleted immediately after pairing — a seized phone cannot recompute the seed without the salts (`pairSeedV2`).
  2. **Epoch Ratchet**: Starting from the chain seed, message keys advance as `key_e = BLAKE3(key_{e-1} ‖ epoch)`. At any point, the phone stores the current key and the previous key (to handle reordered delivery). A seized phone exposes at most the current and previous epoch's messages (`pairRatchet`).
  3. **Proof-of-Work & Encryption**: Alice's phone computes a VDL proof-of-work witness once per frame (`vdlCheckFrame`) — not once per contact — to rate-limit spam. She encrypts the body using ChaCha20-Poly1305 under the current epoch's message key for Bob.
  4. **Private Counter**: Each outgoing private frame carries an epoch-scoped per-contact counter. The counter's base is randomly chosen at the start of each epoch and stored in EncryptedSharedPreferences — the old plaintext `PRIVATE_COUNTER_KEY` has been removed.
  5. **No Recipient Address on Wire**: The frame contains no recipient address, phone number, or user ID.
  6. **Oblivious Multi-Hop Relay**: Intermediary nodes in the crowd verify the Ed25519 signature and VDL PoW witness once (`vdlCheckFrame`). They **cannot read the message or know who it is for**, but they decrement TTL by 1 and relay it across the mesh (`advertiseRelayOnce` + GATT `relayOnce`).
  7. **Constant-Time Trial Decryption**: Bob's phone (and all receiving phones) runs `vdlCheckFrame` once, then fetches its `candidateKeys` from the encrypted contact store (avoiding Keystore burn per contact). It trial-decrypts the body against those keys in sequence without breaking early (preventing timing side-channel attacks via `openPrivateBodyOnly`).
  8. **Delivery**: Bob's phone successfully authenticates the Poly1305 tag and displays `🔒 Alice: Meet at South Entrance in 10 mins`.

---

## Wire Frame Format

All messages use a **fixed 226-byte** frame — no variable-length fields, no compression, no allocations during decode. This is the anti-zip-bomb boundary (invariant #3). Any frame that deviates from exactly 226 bytes is silently dropped with zero side effects.

```
 Offset       Size   Field          Purpose
 ──────       ────   ─────          ───────
 [0..16)       16 B  mark           Pseudo-random message identifier
 [16..18)       2 B  hdr            Version + message type
 [18..34)      16 B  div_sketch     KMV diversity sketch / counter
 [34..38)       4 B  epoch          Big-endian epoch index
 [38..102)     64 B  body           Payload (plaintext or AEAD ciphertext)
 [102..118)    16 B  pocp_wit       Proof-of-Co-Presence witness / VDL witness
 [118..150)    32 B  pk             Ephemeral Ed25519 public key
 [150..214)    64 B  sig            Ed25519 signature over [0..150)
 [214..226)    12 B  reserved       Hop-mutable region (TTL, RSSI metrics)
 ──────────────────────────────────────────────────────────────────────
 Total: 226 B (fits within BLE 5 Extended Advertising's 255 B limit)
```

The Ed25519 signature authenticates bytes `[0..150)` — everything from `mark` through `pk`. The `reserved` tail (`[214..226)`) is deliberately unsigned and hop-mutable, so relay nodes can decrement TTL and attach RSSI metrics without invalidating the originator's signature.

---

## Repository Structure

```
bileichat/
├── mesh-core/                  # Rust core library (all security-critical logic)
│   ├── Cargo.toml              #   AGPL-3.0, Rust 2024 edition
│   ├── src/
│   │   ├── lib.rs              #   Crate root — 7 non-negotiable invariants documented
│   │   ├── codec.rs            #   Fixed 226 B frame encoder/decoder (allocation-free, panic-free)
│   │   ├── crypto.rs           #   Ed25519 signing, BLAKE3 KDF, X25519 DH, ChaCha20-Poly1305
│   │   ├── pocp.rs             #   Proof-of-Co-Presence (KMV sketches, Jaccard similarity, witness MAC)
│   │   ├── beacon.rs           #   Chained-hash beacon for epoch key rotation and forward secrecy
│   │   ├── message.rs          #   Tier-1/2 public danger alert & frame construction (wit_epoch param)
│   │   ├── private.rs          #   Tier-3 E2EE body sealing/opening with per-epoch random counter base
│   │   ├── vdl.rs              #   Verifiable Delay Lottery (proof-of-work spam gate, vdlCheckFrame)
│   │   ├── statemachine.rs     #   Parse→verify→decide pipeline, per-epoch dedup bucket (1024-cap)
│   │   ├── trust.rs            #   Multi-cell crowd corroboration aggregator (direct-heard only)
│   │   ├── radio.rs            #   Radio capacity abstraction for relay queue draining
│   │   └── ffi.rs              #   UniFFI bridge to Kotlin/Swift (7 exported functions)
│   ├── tests/                  #   Property-based (proptest) & Known-Answer-Test integration tests
│   │   ├── codec_props.rs      #     Codec round-trip and edge-case properties
│   │   ├── crypto_props.rs     #     Cryptographic KAT vectors and property tests
│   │   ├── ffi_roundtrip.rs    #     FFI binding round-trip validation
│   │   ├── pocp_attestation.rs #     PoCP witness generation and verification
│   │   └── pocp_props.rs       #     PoCP Jaccard/KMV property tests
│   ├── fuzz/                   #   cargo-fuzz targets for codec safety
│   │   └── fuzz_targets/       #     Fuzz harness for `decode` (runs in CI)
│   └── bindings/               #   Generated UniFFI binding artifacts
│
├── android/                    # Android app (Kotlin + Jetpack Compose)
│   ├── build-android.sh        #   Cross-compile mesh-core for 4 ABIs + generate Kotlin bindings
│   ├── build.gradle.kts        #   AGP 8.7.3, Kotlin 2.0.21, Compose plugin
│   ├── settings.gradle.kts     #   rootProject.name = "bileichat"
│   ├── gradle.properties       #   AndroidX and JVM configuration
│   └── app/
│       ├── build.gradle.kts    #   compileSdk 35, minSdk 26, targetSdk 35, v0.18-relaygate
│       ├── proguard-rules.pro  #   R8/ProGuard rules for release minification
│       └── src/main/
│           ├── AndroidManifest.xml
│           ├── java/org/bileichat/mesh/
│           │   ├── MeshService.kt      # Foreground service: BLE scanning, advertising, Rust FFI
│           │   ├── BleRadio.kt         # BLE 5.0 Extended Advertising and scanner hardware manager
│           │   ├── GattPlane.kt        # GATT server/client fallback for multi-packet delivery
│           │   ├── PairStore.kt        # Encrypted contact pair storage (EncryptedSharedPreferences)
│           │   │                       #   X25519 DH pairing with ephemeral 32-byte salts, QR codes
│           │   ├── MainActivity.kt     # Entry activity: permissions and Compose UI host
│           │   ├── Config.kt           # Runtime-tunable parameters (epoch length, tau, RSSI floor)
│           │   ├── MeshState.kt        # Observable mesh state (nearby count, epoch, connected peers)
│           │   ├── MeshApp.kt          # Application class
│           │   ├── SelfTest.kt         # Diagnostic self-tests (crypto, codec, PoCP round-trips)
│           │   ├── Measurement.kt      # Performance metrics logger
│           │   └── ui/
│           │       ├── MeshUi.kt       # Compose UI — Local / Broadcast / Private tab views
│           │       ├── Drawer.kt       # Settings drawer, tier guide, detector, panic wipe button
│           │       └── Theme.kt        # Industrial AMOLED dark theme
│           ├── java/uniffi/mesh_core/
│           │   └── mesh_core.kt        # Auto-generated UniFFI Kotlin bindings (do not edit)
│           ├── jniLibs/                # Cross-compiled libmesh_core.so (arm64-v8a, armeabi-v7a,
│           │                           #   x86_64, x86) — produced by build-android.sh
│           └── res/                    # Android resources (layouts, drawables, strings)
│
├── laptop/                     # Linux BLE mesh node (experimental)
│   ├── Cargo.toml              #   bluer (BlueZ D-Bus) + tokio + clap + chrono
│   ├── README.md               #   Usage and requirements
│   └── src/
│       └── main.rs             #   CLI node: advertise, scan, relay, KMV epoch debug output
│
├── docs/                       # Documentation assets
│   ├── screenshot-chat.jpg     #   Chat view screenshot (Local/Broadcast/Private tabs)
│   └── screenshot-drawer.jpg   #   Control panel screenshot (tier guide, settings, panic wipe)
│
├── CONTRIBUTING.md             # 7 non-negotiable security invariants & pre-push checklist
└── README.md                   # This file
```

---

## Key Security Guarantees

These **7 non-negotiable invariants** are enforced in code and CI. They are not style preferences — each one is a failure class that has killed a real system (see the "Breaking Bridgefy" papers). Violating one is a security bug, not a nit. See [CONTRIBUTING.md](CONTRIBUTING.md) for the full enforcement details.

1. **One codec, in Rust, shared.** No parsing in the platform shims (Kotlin/Swift). The shims move raw bytes in and out; `mesh-core::codec` is the *only* thing that interprets them. A second, lenient parser is how these systems die.
2. **Parse → verify → decide, in that order, always.** Nothing is relayed or rendered before validation completes. The order is enforced in `statemachine::on_recv` and is not negotiable: `len → epoch∈{N,N-1} → mark-unseen → sig-verify → witness-structural → then relay/render`.
3. **Fixed 226 B frame. No compression, no variable-length fields.** Any deviation ⇒ silent total drop (`DecodeErr`, mutate nothing, relay nothing). `codec::decode` must stay allocation-free and panic-free on every input — it is the anti-zip-bomb boundary. This is fuzzed in CI.
4. **Danger-only on the wire. Never assert "safe."** Silence ≠ safe. The confidence wall shows corroboration/dispute counts, never a boolean, and only for danger.
5. **Ephemeral keys, minimal persisted state, panic-wipe.** Keys rotate with the beacon chain and live only in the platform secure store via the `KeyStore` trait. The `store` module is size-capped and auto-decaying. `panic_wipe` must actually erase.
6. **The public plane is openly unencrypted — never label it E2E.** Only the Tier-3 private plane (ChaCha20-Poly1305 with epoch-ratcheted keys) is end-to-end encrypted.
7. **Trust is per-message physical corroboration, never accumulated to an identity.** Diversity counts *distinct locally-verified cells*, not reputation.

---

## Implementation Status (v0)

| Module | Description | Tests |
|:---|:---|:---:|
| **`codec`** | Zero-allocation fixed 226-byte packet encoder/decoder (reserved-tail check) | 11 |
| **`crypto`** | Ed25519 signing, BLAKE3 KDF, X25519 DH, ChaCha20 AEAD, forward-secure v2 ratchet | 10 |
| **`message`** | Public danger alert & private message frame generator (wit_epoch param) | 20 |
| **`pocp`** | Physical proximity verification (Proof-of-Co-Presence) | 23 |
| **`beacon`** | Self-clocking key rotation & forward secrecy beacon | 13 |
| **`private`** | Tier-3 encrypted direct messaging with per-epoch random counter base | 6 |
| **`vdl`** | Proof-of-work cost gate for spam protection (vdlCheckFrame) | 5 |
| **`statemachine`** | Packet processing, relay decisions, per-epoch dedup bucket (1024-cap) | 13 |
| **`trust`** | Multi-cell crowd corroboration aggregator (direct-heard only) | 5 |
| **`store`** | Memory-bounded message buffer & instant panic wipe | — |
| **`ffi`** | UniFFI bindings for Android (Kotlin) — pairSeedV2, pairRatchet, vdlCheckFrame, openPrivateBodyOnly, makeMessageFrameWithWitness, BeaconFfi.wipe | 14 |

---

## Technology Stack

| Layer | Technology | Details |
|:---|:---|:---|
| **Core protocol** | Rust 2024 edition | `blake3` (KDF/hashing), `chacha20poly1305` (AEAD), `ed25519-dalek` (signatures), `x25519-dalek` (DH key exchange), `zeroize` (secret wiping), `arrayref` (zero-copy field access) |
| **FFI bridge** | UniFFI v0.32.0 | Proc-macro mode (no UDL file). Generates Kotlin bindings from the compiled `cdylib`. Supports `lib`, `cdylib` (Android .so), and `staticlib` (iOS) crate types. |
| **Native build** | `cargo-ndk` | Cross-compiles `libmesh_core.so` for 4 Android ABIs: `arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86` (min API 26). Orchestrated by `build-android.sh`. |
| **Android app** | Kotlin 2.0.21 | Jetpack Compose with Material3 (BOM 2024.12.01). Single-activity architecture with a foreground service for BLE operations. |
| **Android SDK** | AGP 8.7.3 | compileSdk 35, minSdk 26, targetSdk 35. Debug builds have JDWP disabled in release. ProGuard/R8 minification enabled for release APKs. |
| **Android security** | EncryptedSharedPreferences | At-rest protection of pairing keys and per-epoch private counter bases. JNA 5.14.0 for UniFFI runtime binding. |
| **QR pairing** | ZXing Android Embedded 4.3.0 | On-device QR encoding/decoding for out-of-band pairing. No key material is sent to any server. |
| **Laptop client** | Rust + BlueZ | `bluer` v0.17 (BlueZ D-Bus) for BLE 5 extended advertising, `tokio` async runtime, `clap` CLI parsing, `chrono` log timestamps. |
| **Testing** | proptest + cargo-fuzz | Property-based tests (`proptest` v1.11) for codec/crypto/PoCP invariants. `cargo-fuzz` for continuous codec decode fuzzing. Known-Answer Tests validated against independent implementations. |

---

## Developer Quick Start

### Prerequisites

- **Rust** (stable toolchain, 2024 edition) with Android cross-compilation targets:
  ```bash
  rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android i686-linux-android
  cargo install cargo-ndk
  ```
- **Android SDK** (API 35) and **NDK** (e.g. 27.2.12479018)
- **Android Studio** (optional — only needed for the IDE workflow; CLI-only builds work fine)

### 1. Clone the repository

```bash
git clone https://github.com/howtocuddle/cockrochat.git
cd cockrochat
```

### 2. Run the Rust core test suite

```bash
cd mesh-core

# Run all unit tests, property tests, and KAT vectors
cargo test

# Run codec fuzzer (optional, requires nightly)
cargo +nightly fuzz run decode -- -max_total_time=30
```

### 3. Build the Android app

```bash
cd android

# Step 1: Cross-compile libmesh_core.so for all 4 ABIs and generate Kotlin bindings
./build-android.sh

# Step 2: Assemble the debug APK
./gradlew assembleDebug

# Or for a minified, debug-signed release APK (sideload-ready, no Play Store)
./gradlew assembleRelease
```

The `build-android.sh` script does two things:
1. Runs `cargo ndk` to cross-compile `libmesh_core.so` for `arm64-v8a`, `armeabi-v7a`, `x86_64`, and `x86` into `app/src/main/jniLibs/`.
2. Runs `uniffi-bindgen` to generate the Kotlin binding file (`mesh_core.kt`) from the host `cdylib`.

You must re-run `build-android.sh` whenever `mesh-core` source code changes, before running `./gradlew assembleDebug`.

### 4. Run the Linux laptop node (experimental)

```bash
cd laptop
cargo build --release
sudo ./target/release/bileichat-laptop [--epoch-ms 10000] [--rssi-floor -80] [--text "hello"]
```

**Requirements:** BlueZ ≥ 5.65, `systemctl start bluetooth`, Bluetooth 5 adapter supporting extended advertising (`AUX_ADV_IND`), root or `CAP_NET_ADMIN` + `CAP_NET_RAW`.

**Output lines:**
- `[HH:MM:SS.mmm] rssi=X dBm mark=AABBCCDD epoch=N text="..."` — received peer frame (deduplicated)
- `[epoch N] ended — K distinct neighbours | KMV: v0 v1 … v15` — paste the 16 space-separated u64 values into the Android app's Compare box to compute Jaccard co-presence similarity
- `[adv] epoch=N text="..." registered OK` — own advertisement live; type a new line on stdin to change the outgoing message (max 63 bytes)

---

## Before You Push

```bash
cd mesh-core
cargo fmt --check
cargo clippy --all-targets -- -D warnings
cargo test
cargo +nightly fuzz run decode -- -max_total_time=30   # if you touched codec
```

All four must be green. CI runs the same checks. See [CONTRIBUTING.md](CONTRIBUTING.md) for the complete invariant list and codec/crypto specifics.

---

## Technical Glossary

This glossary explains technical terms and protocol concepts used throughout `bileichat`.

### Cryptography & Security Terms

- **BLAKE3**: An ultra-fast cryptographic hash function used for deriving keys, hashing marks, and chaining epoch seeds. Used as the KDF for both beacon rotation and private pairing ratchets.
- **ChaCha20-Poly1305**: A high-speed authenticated encryption scheme (AEAD) used to keep Tier-3 private messages secure and tamper-proof. The Poly1305 tag provides message authentication; ChaCha20 provides confidentiality.
- **Ed25519**: A public-key signature scheme used to verify message authenticity without revealing private identity. All signatures are domain-separated (`crypto::DOMAIN_SIG`) and verified in constant time (`verify_strict`).
- **Ephemeral Keys**: Temporary encryption/signing keys that rotate automatically with each epoch, ensuring past communications remain secure even if a device is later inspected.
- **Forward Secrecy**: A security property guaranteeing that compromised current keys cannot be used to decrypt past session data. Enforced via the one-way beacon chain and per-epoch key ratcheting.
- **Panic Wipe**: An emergency function that immediately zero-fills and purges all in-memory cryptographic keys and stored messages. Triggered by a long-press on the "HOLD TO WIPE — PANIC" button.
- **Proof-of-Work (PoW) / VDL**: Verifiable Delay Lottery — a brief computational task required before sending private messages to prevent spammers from flooding the network. Computed once per frame (`vdlCheckFrame`), not once per contact.
- **X25519**: A Diffie-Hellman key exchange algorithm enabling two devices to establish a shared secret key out-of-band (e.g. via QR code scan). The shared secret is mixed with ephemeral salts to produce the chain seed.

### Mesh Protocol Terms

- **BLE 5.0 Extended Advertising**: A Bluetooth Low Energy standard allowing devices to broadcast larger packets (up to 255 bytes) without requiring Bluetooth pairing. This is the primary transport for all three message tiers.
- **Epoch**: A fixed time window (e.g., 10 seconds in testing, configurable in production) during which devices sample background BLE signals and rotate internal keys. Epoch boundaries trigger key rotation, dedup bucket clearing, and KMV sketch finalization.
- **Frame Hash (Dedup Key)**: A unique 16-byte identifier computed from a message's contents, allowing relay nodes to ignore duplicate broadcasts. Stored in a per-epoch bounded bucket capped at 1024 entries.
- **GATT Plane**: A secondary BLE transport using GATT server/client connections for multi-packet delivery. Used as a fallback when Extended Advertising alone is insufficient (e.g., for larger payloads or direct peer connections).
- **Jaccard Similarity (τ)**: A mathematical formula measuring set similarity. In `bileichat`, it determines whether two devices share the same physical radio environment by comparing their KMV sketches of ambient BLE marks.
- **KMV Sketch (K-Minimum Values)**: A compact summary (16 minimum hash values) of ambient Bluetooth signals, allowing devices to compare physical surroundings efficiently in memory. Used to compute PoCP witnesses.
- **LE Coded PHY**: A Bluetooth 5 mode using error correction (S=8) to quadruple radio range, ideal for dense or obstructed crowd environments.
- **Parse-Before-Forward**: The security rule requiring every packet to be fully validated in Rust before being displayed or relayed. Enforced by `statemachine::on_recv` in strict order.
- **Proof-of-Co-Presence (PoCP)**: A cryptographic mechanism verifying that a message originated from someone physically present in the crowd cell. Computed as a MAC over the sender's KMV sketch using the receiver's observed marks.
- **RSSI (Received Signal Strength Indicator)**: A measurement of signal power (in dBm). Closer devices show higher RSSI values (e.g. −40 dBm), while distant devices show lower values (e.g. −80 dBm). Used for the configurable RSSI floor filter.
- **Spatial Diversity**: A security mechanism where alert confidence scales based on corroboration from distinct physical geographic cells (ambient RF observations), ignoring remote virtual identity counts (Sybil attacks).
- **Trickle Algorithm**: An epidemic broadcast algorithm (RFC 6206) that adjusts retransmission intervals based on crowd density to conserve battery and bandwidth.
- **TTL (Time-To-Live)**: A hop counter on packets. Each relay decrements TTL by 1; when it reaches 0, the packet stops propagating. TTL is monotonically clobbered to prevent adversarial inflation.
- **UniFFI**: Mozilla's multi-language binding generator used to connect the Rust core cleanly to Kotlin (Android) and Swift (iOS). Runs in proc-macro mode (no UDL file required).

---

---

## License

[AGPL-3.0-or-later](mesh-core/Cargo.toml)

---

<div align="center">
<sub>Built for human safety and free expression. No accounts. No servers. No internet. </sub>
</div>

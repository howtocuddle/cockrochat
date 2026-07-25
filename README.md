# bileichat

**Offline Decentralized Mesh Messaging for Protests & Emergencies**

[![Rust Core](https://img.shields.io/badge/Core-Rust-b7410e?style=flat-square&logo=rust)](mesh-core/)
[![Android](https://img.shields.io/badge/Platform-Android%20(Kotlin)-3DDC84?style=flat-square&logo=android)](android/)
[![BLE 5.0](https://img.shields.io/badge/Transport-BLE%205.0%20Extended-0082FC?style=flat-square&logo=bluetooth)]()
[![License](https://img.shields.io/badge/License-AGPL--3.0-orange?style=flat-square)](mesh-core/Cargo.toml)

*Phones relay emergency alerts directly to each other using Bluetooth Low Energy — no cell towers, Wi-Fi routers, central servers, internet access, or user accounts required.*

---

## What is bileichat?

During protests, civil demonstrations, or natural disasters, cellular networks and Wi-Fi are frequently jammed, monitored, or shut down. 

**bileichat** turns nearby smartphones into a resilient, self-healing peer-to-peer mesh network. Devices pass short emergency alerts phone-to-phone through the crowd automatically.

### Key Highlights
- **100% Offline & Serverless**: Works entirely over Bluetooth Low Energy (BLE 5.0).
- **Anti-Fake Alert Protection**: Tier-1 local alerts require a Proof-of-Co-Presence witness; witnessless public frames are relay-only and never displayed. Broadcast corroboration counts only claims heard directly over the air — and is shown as a hint, never as a guarantee (a determined *nearby* attacker can forge claims; see §Tier 2 below).
- **Self-Destructing Identity**: Marks and signing keys rotate every epoch over a one-way beacon chain. v2 private pairings ratchet message keys every epoch from a seed mixed with deleted pairing salts — a seized phone exposes at most the current and previous epoch of private history.
- **Crash-Proof Rust Core**: Every packet is parsed and verified in memory-safe Rust before forwarding, preventing crash-attacks (zip bombs).
- **Danger-Only Alerts**: The public mesh strictly carries danger signals (e.g. teargas, police kettling, medical emergency). Silence is never assumed to mean safety.

---

## Platform Support

| Component | Platform | Details |
|:---|:---|:---|
| **`mesh-core`** | Rust (Core Library) | Contains all protocol parsing, security, cryptography, and relay state machine. |
| **`android`** | Kotlin (Android App) | Foreground Service handling BLE 5.0 Extended Advertising and UI rendering. |
| **`laptop`** | Rust (Linux Desktop, deprecated) | Phone-to-phone only; laptop client is no longer maintained. |

---

## Architecture & 3-Tier Messaging Model

The protocol uses a 3-tier messaging model to balance latency, crowd coverage, and security:

```
+-------------------------------------------------------------------------------+
|                       3-Tier Messaging Architecture                           |
+-------------------------------------------------------------------------------+

  Tier 1: Immediate Local Broadcast (~30m Radius)
  [ Sender ] ---> (BLE Extended Adv) ---> [ Nearby Nodes ]
   * 1-Hop direct proximity broadcast for immediate danger ground truth.
   * Authenticated by Proof-of-Co-Presence (PoCP) physical witness.

  Tier 2: Multi-Hop Regional Mesh Flood
  [ Sender ] ---> [ Relay Node 1 ] ---> [ Relay Node 2 ] ---> [ Crowd Mesh ]
   * Multi-hop flood re-broadcasted through the crowd.
   * Prioritized relay queue, epoch-bucketed frame-hash dedup & TTL limits.

  Tier 3: Encrypted Direct Private Message
  [ Sender ] ================================================> [ Recipient ]
   * End-to-end encrypted pairwise message (ChaCha20-Poly1305).
   * Gated by Proof-of-Work (VDL witness) to prevent network spam.

+-------------------------------------------------------------------------------+
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

## Implementation Status (v0)

| Module | Description | Status | Tests |
|:---|:---|:---:|:---:|
| **`codec`** | Zero-allocation fixed 226-byte packet encoder/decoder (reserved-tail check) | Implemented | 11 |
| **`crypto`** | Ed25519 signing, BLAKE3 KDF, X25519 DH, ChaCha20 AEAD, forward-secure v2 ratchet | Implemented | 10 |
| **`message`** | Public danger alert & private message frame generator (wit_epoch param) | Implemented | 20 |
| **`pocp`** | Physical proximity verification (Proof-of-Co-Presence) | Implemented | 23 |
| **`beacon`** | Self-clocking key rotation & forward secrecy beacon | Implemented | 13 |
| **`private`** | Tier-3 encrypted direct messaging with per-epoch random counter base | Implemented | 6 |
| **`vdl`** | Proof-of-work cost gate for spam protection (vdlCheckFrame) | Implemented | 5 |
| **`statemachine`** | Packet processing, relay decisions, per-epoch dedup bucket (1024-cap) | Implemented | 13 |
| **`trust`** | Multi-cell crowd corroboration aggregator (direct-heard only) | Implemented | 5 |
| **`store`** | Memory-bounded message buffer & instant panic wipe | Implemented | — |
| **`ffi`** | Language bindings for Android (Kotlin) — 7 new exports (pairSeedV2, pairRatchet, vdlCheckFrame, openPrivateBodyOnly, makeMessageFrameWithWitness wit_epoch, BeaconFfi.wipe) | Implemented | 14 |

---

## Key Security Guarantees

1. **Parse-Before-Forward**: Incoming data is validated in memory-safe Rust before any decision is made to display or relay it.
2. **Fixed 226-Byte Packet**: No variable lengths, no compression, zero room for buffer overflow or zip-bomb attacks.
3. **Danger-Only Public Mesh**: Public broadcasts carry danger alerts only. Nodes cannot broadcast "all clear" signals.
4. **Instant Panic Wipe**: A single command instantly zero-fills and purges all stored state and cryptographic keys.

---

## Developer Quick Start

```bash
# Clone the repository
git clone https://github.com/howtocuddle/cockrochat.git
cd bileichat/mesh-core

# Run test suite (cryptographic vectors, codec safety, property tests)
cargo test

# Build release binary
cargo build --release

# Run Linux laptop node (requires BlueZ, Bluetooth 5 hardware, and root/CAP_NET_ADMIN privileges)
cd ../laptop
sudo cargo run
```

---

## Technical Glossary

This glossary explains technical terms and protocol concepts used throughout `bileichat`.

### Cryptography & Security Terms

- **BLAKE3**: An ultra-fast cryptographic hash function used for deriving keys, hashing marks, and chaining epoch seeds.
- **ChaCha20-Poly1305**: A high-speed authenticated encryption scheme used to keep Tier-3 private messages secure and tamper-proof.
- **Ed25519**: A public-key signature scheme used to verify message authenticity without revealing private identity.
- **Ephemeral Keys**: Temporary encryption/signing keys that rotate automatically, ensuring past communications remain secure even if a device is later inspected.
- **Forward Secrecy**: A security property guaranteeing that compromised current keys cannot be used to decrypt past session data.
- **Panic Wipe**: An emergency function that immediately zero-fills and purges all in-memory cryptographic keys and stored messages.
- **Proof-of-Work (PoW) / VDL**: Verifiable Delay Lottery — a brief computational task required before sending private messages to prevent spammers from flooding the network.
- **X25519**: A Diffie-Hellman key exchange algorithm enabling two devices to establish a shared secret key out-of-band (e.g. via QR code).

### Mesh Protocol Terms

- **BLE 5.0 Extended Advertising**: A Bluetooth Low Energy standard allowing devices to broadcast larger packets (up to 255 bytes) without requiring Bluetooth pairing.
- **Epoch**: A fixed time window (e.g., 10 seconds in testing, minutes in production) during which devices sample background signals and rotate internal keys.
- **Frame Hash (Dedup Key)**: A unique 16-byte identifier computed from a message's contents, allowing relay nodes to ignore duplicate broadcasts.
- **Jaccard Similarity ($\tau$)**: A mathematical formula measuring set similarity. In `bileichat`, it determines whether two devices share the same physical radio environment.
- **KMV Sketch (K-Minimum Values)**: A compact summary of ambient Bluetooth signals, allowing devices to compare physical surroundings efficiently in memory.
- **LE Coded PHY**: A Bluetooth 5 mode using error correction (S=8) to quadruple radio range, ideal for dense or obstructed crowd environments.
- **Parse-Before-Forward**: The security rule requiring every packet to be fully validated in Rust before being displayed or relayed.
- **Proof-of-Co-Presence (PoCP)**: A cryptographic mechanism verifying that a message originated from someone physically present in the crowd cell.
- **RSSI (Received Signal Strength Indicator)**: A measurement of signal power (in dBm). Closer devices show higher RSSI values (e.g. -40 dBm), while distant devices show lower values (e.g. -80 dBm).
- **Spatial Diversity**: A security mechanism where alert confidence scales based on corroboration from distinct physical geographic cells (ambient RF observations), ignoring remote virtual identity counts (Sybil attacks).
- **Trickle Algorithm**: An epidemic broadcast algorithm (RFC 6206) that adjusts retransmission intervals based on crowd density to conserve battery and bandwidth.
- **TTL (Time-To-Live)**: A hop counter on packets. Each relay decrements TTL by 1; when it reaches 0, the packet stops propagating.
- **UniFFI**: Mozilla's multi-language binding generator used to connect the Rust core cleanly to Kotlin (Android) and Swift (iOS).

---

<div align="center">
<sub>Built for human safety and free expression. No accounts. No servers. No internet. Just mesh.</sub>
</div>

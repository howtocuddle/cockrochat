# cockroachat

**Offline Decentralized Mesh Messaging for Protests & Emergencies**

[![Rust Core](https://img.shields.io/badge/Core-Rust-b7410e?style=flat-square&logo=rust)](mesh-core/)
[![Android](https://img.shields.io/badge/Platform-Android%20(Kotlin)-3DDC84?style=flat-square&logo=android)](android/)
[![BLE 5.0](https://img.shields.io/badge/Transport-BLE%205.0%20Extended-0082FC?style=flat-square&logo=bluetooth)]()
[![License](https://img.shields.io/badge/License-MIT%2FApache--2.0-blue?style=flat-square)]()

*Phones relay emergency alerts directly to each other using Bluetooth Low Energy — no cell towers, Wi-Fi routers, central servers, internet access, or user accounts required.*

---

## What is cockroachat?

During protests, civil demonstrations, or natural disasters, cellular networks and Wi-Fi are frequently jammed, monitored, or shut down. 

**cockroachat** turns nearby smartphones into a resilient, self-healing peer-to-peer mesh network. Devices pass short emergency alerts phone-to-phone through the crowd automatically.

### Key Highlights
- **100% Offline & Serverless**: Works entirely over Bluetooth Low Energy (BLE 5.0).
- **Anti-Fake Alert Protection**: Uses physical presence checks ("Proof-of-Co-Presence") so remote actors outside the crowd cannot inject false alerts or panic.
- **Self-Destructing Identity**: Keys auto-rotate continuously. If a phone is seized, past messages and location history remain unrecoverable.
- **Crash-Proof Rust Core**: Every packet is parsed and verified in memory-safe Rust before forwarding, preventing crash-attacks (zip bombs).
- **Danger-Only Alerts**: The public mesh strictly carries danger signals (e.g. teargas, police kettling, medical emergency). Silence is never assumed to mean safety.

---

## Platform Support

| Component | Platform | Details |
|:---|:---|:---|
| **`mesh-core`** | Rust (Core Library) | Contains all protocol parsing, security, cryptography, and relay state machine. |
| **`android`** | Kotlin (Android App) | Foreground Service handling BLE 5.0 Extended Advertising and UI rendering. |
| **`laptop`** | Rust (Linux Desktop) | Native Linux testing client built on BlueZ for desktop debugging & fixed relay nodes. |

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
   * Density-adaptive Trickle algorithm, Frame Hash dedup & TTL limits.

  Tier 3: Encrypted Direct Private Message
  [ Sender ] ================================================> [ Recipient ]
   * End-to-end encrypted pairwise message (ChaCha20-Poly1305).
   * Gated by Proof-of-Work (VDL witness) to prevent network spam.

+-------------------------------------------------------------------------------+
```

### Tier Summary

1. **Tier 1 — Immediate Local Alerts (~30m)**: Instant alerts broadcasted to people right next to you.
2. **Tier 2 — Crowd-Relayed Regional Alerts**: Multi-hop alerts propagated through the mesh. Re-broadcast frequency automatically adjusts to crowd density.
3. **Tier 3 — Encrypted Direct Messages**: Pairwise private messages between trusted contacts with built-in spam protection (Proof-of-Work).

---

## Implementation Status (v0)

| Module | Description | Status | Tests |
|:---|:---|:---:|:---:|
| **`codec`** | Zero-allocation fixed 226-byte packet encoder/decoder | Implemented | 9 |
| **`crypto`** | Ephemeral Ed25519 signing, BLAKE3 KDF, X25519 DH, ChaCha20 AEAD | Implemented | 8 |
| **`message`** | Public danger alert & private message frame generator | Implemented | 19 |
| **`pocp`** | Physical proximity verification (Proof-of-Co-Presence) | Implemented | 18 |
| **`beacon`** | Self-clocking key rotation & forward secrecy beacon | Implemented | 13 |
| **`private`** | Tier-3 encrypted direct messaging with epoch nonces | Implemented | 6 |
| **`vdl`** | Proof-of-work cost gate for spam protection | Implemented | 5 |
| **`statemachine`** | Packet processing, relay decisions, and deduplication | Implemented | 12 |
| **`trust`** | Multi-cell crowd corroboration aggregator | In Progress (M6) | 5 |
| **`store`** | Memory-bounded message buffer & instant panic wipe | Implemented | — |
| **`ffi`** | Language bindings for Android (Kotlin) & iOS (Swift) | Implemented | 9 |

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
cd cockroachat/mesh-core

# Run test suite (cryptographic vectors, codec safety, property tests)
cargo test

# Build release binary
cargo build --release

# Run Linux laptop node (requires BlueZ & Bluetooth 5 hardware)
cd ../laptop
cargo run
```

---

## Technical Glossary

This glossary explains technical terms and protocol concepts used throughout `cockroachat`.

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
- **Jaccard Distance ($\tau$)**: A mathematical formula measuring set similarity. In `cockroachat`, it determines whether two devices share the same physical radio environment.
- **KMV Sketch (K-Minimum Values)**: A compact summary of ambient Bluetooth signals, allowing devices to compare physical surroundings efficiently in memory.
- **LE Coded PHY**: A Bluetooth 5 mode using error correction (S=8) to quadruple radio range, ideal for dense or obstructed crowd environments.
- **Parse-Before-Forward**: The security rule requiring every packet to be fully validated in Rust before being displayed or relayed.
- **Proof-of-Co-Presence (PoCP)**: A cryptographic mechanism verifying that a message originated from someone physically present in the crowd cell.
- **RSSI (Received Signal Strength Indicator)**: A measurement of signal power (in dBm). Closer devices show higher RSSI values (e.g. -40 dBm), while distant devices show lower values (e.g. -80 dBm).
- **Trickle Algorithm**: An epidemic broadcast algorithm (RFC 6206) that adjusts retransmission intervals based on crowd density to conserve battery and bandwidth.
- **TTL (Time-To-Live)**: A hop counter on packets. Each relay decrements TTL by 1; when it reaches 0, the packet stops propagating.
- **UniFFI**: Mozilla's multi-language binding generator used to connect the Rust core cleanly to Kotlin (Android) and Swift (iOS).

---

<div align="center">
<sub>Built for human safety and free expression. No accounts. No servers. No internet. Just mesh.</sub>
</div>

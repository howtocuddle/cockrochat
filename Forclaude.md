# Bluetooth mesh chat security vulnerabilities

**Session ID:** ses_062e2b329ffe55kOr9nl9HFI1L
**Created:** 7/26/2026, 11:59:30 AM
**Updated:** 7/26/2026, 2:29:34 PM

---

## User

find security vulnerablities in this bluetooth mesh based chat. 

---

## Assistant (Plan · Kimi K3 (2x usage) · 141.5s)

_Thinking:_

The user wants me to find security vulnerabilities in a Bluetooth mesh based chat project. I'm in plan mode (read-only), so I need to explore the codebase and analyze it without making changes.

Let me start by exploring the project structure to understand what we're working with.

**Tool: bash**

**Input:**
```json
{
  "command": "ls -la"
}
```

**Output:**
```
total 254228
drwxr-xr-x 8 Likun Likun     4096 Jul 26 11:39 .
drwxr-xr-x 9 Likun Likun      162 Jul 25 00:38 ..
drwxr-xr-x 5 Likun Likun      171 Jul 25 11:10 android
-rw-r--r-- 1 Likun Likun      658 Jul 24 02:45 api.qrserver.png
-rw-r--r-- 1 Likun Likun 16780861 Jul 26 10:40 bileichat-0.10-vdlepoch.apk
-rw-r--r-- 1 Likun Likun 16780874 Jul 26 10:45 bileichat-0.11-tierleak.apk
-rw-r--r-- 1 Likun Likun 16780874 Jul 26 10:56 bileichat-0.12-messenger-ui.apk
-rw-r--r-- 1 Likun Likun 16781492 Jul 26 11:16 bileichat-0.13-txjitter.apk
-rw-r--r-- 1 Likun Likun 16782847 Jul 26 11:25 bileichat-0.14-relay-redundancy.apk
-rw-r--r-- 1 Likun Likun 16824381 Jul 26 11:39 bileichat-0.15-ticks-search.apk
-rw-r--r-- 1 Likun Likun 16676634 Jul 26 00:16 bileichat-0.8-diagnostics.apk
-rw-r--r-- 1 Likun Likun 16779710 Jul 26 00:34 bileichat-0.9-privatefix.apk
-rw-r--r-- 1 Likun Likun    18372 Jul 24 02:44 Claude_Chat_1784792696492.md
drwxr-xr-x 6 Likun Likun     4096 Jul 24 14:31 cockroachat
-rw-r--r-- 1 Likun Likun 17626036 Jul 25 11:13 cockroachat-0.5-unified.apk
-rw-r--r-- 1 Likun Likun 16767495 Jul 25 12:21 cockroachat-0.6-unified.apk
-rw-r--r-- 1 Likun Likun 17675628 Jul 25 17:49 cockroachat-0.7-fixes.apk
-rw-r--r-- 1 Likun Likun 16767495 Jul 25 12:21 cockroachat-debug.apk
-rw-r--r-- 1 Likun Likun  9334272 Jul 24 02:20 cockroachat-live-0.3-fullrestart.apk
-rw-r--r-- 1 Likun Likun  9335480 Jul 24 13:55 cockroachat-live-0.4-localreceipt.apk
-rw-r--r-- 1 Likun Likun  9335480 Jul 24 13:55 cockroachat-live-debug.apk
-rw-r--r-- 1 Likun Likun  9334272 Jul 24 02:20 cockroachat-rig-0.3-fullrestart.apk
-rw-r--r-- 1 Likun Likun  9335476 Jul 24 13:55 cockroachat-rig-0.4-localreceipt.apk
-rw-r--r-- 1 Likun Likun  9335476 Jul 24 13:55 cockroachat-rig-debug.apk
-rw-r--r-- 1 Likun Likun     2824 Jul 25 11:10 CONTRIBUTING.md
drwxr-xr-x 2 Likun Likun       62 Jul 25 11:31 docs
drwxr-xr-x 7 Likun Likun     4096 Jul 26 11:57 .git
-rw-r--r-- 1 Likun Likun      526 Jul 25 11:10 .gitignore
drwxr-xr-x 4 Likun Likun       84 Jul 25 11:10 laptop
drwxr-xr-x 7 Likun Likun      108 Jul 26 10:38 mesh-core
-rw-r--r-- 1 Likun Likun    16276 Jul 25 23:49 README.md
-rw-r--r-- 1 Likun Likun   552580 Jul 25 23:49 RepoAnalysis.md
-rw-r--r-- 1 Likun Likun   508855 Jul 24 02:45 session-ses_0718.md
-rw-r--r-- 1 Likun Likun   121414 Jul 24 02:45 session-ses_0769.md

```

**Tool: glob**

**Input:**
```json
{
  "pattern": "**/AGENTS.md"
}
```

**Output:**
```
No files found
```

---

## Assistant (Plan · Kimi K3 (2x usage) · 10.1s)

_Thinking:_

This is a project with multiple components:
- android/ - Android app
- laptop/ - Laptop client?
- mesh-core/ - Mesh networking core
- cockroachat/ - another directory

The user wants a security audit of this Bluetooth mesh based chat. Let me explore the structure. Given the size of this task, I should use explore agents to survey the codebase, and also do some reading myself of the key security-relevant files (crypto, mesh routing, identity, key exchange).

Let me first get a high-level structure, then delegate deep dives.

**Tool: bash**

**Input:**
```json
{
  "command": "find android laptop mesh-core cockroachat -type f \\( -name \"*.kt\" -o -name \"*.java\" -o -name \"*.rs\" -o -name \"*.toml\" -o -name \"*.py\" -o -name \"*.js\" -o -name \"*.ts\" \\) 2>/dev/null | grep -v build/ | grep -v \"/.git/\" | head -100"
}
```

**Output:**
```
android/app/src/main/java/org/bileichat/mesh/ui/Theme.kt
android/app/src/main/java/org/bileichat/mesh/ui/Drawer.kt
android/app/src/main/java/org/bileichat/mesh/ui/MeshUi.kt
android/app/src/main/java/org/bileichat/mesh/MeshApp.kt
android/app/src/main/java/org/bileichat/mesh/BleRadio.kt
android/app/src/main/java/org/bileichat/mesh/Config.kt
android/app/src/main/java/org/bileichat/mesh/GattPlane.kt
android/app/src/main/java/org/bileichat/mesh/Measurement.kt
android/app/src/main/java/org/bileichat/mesh/PairStore.kt
android/app/src/main/java/org/bileichat/mesh/MainActivity.kt
android/app/src/main/java/org/bileichat/mesh/MeshState.kt
android/app/src/main/java/org/bileichat/mesh/MeshService.kt
android/app/src/main/java/uniffi/mesh_core/mesh_core.kt
laptop/src/main.rs
laptop/Cargo.toml
mesh-core/fuzz/fuzz_targets/decode.rs
mesh-core/fuzz/Cargo.toml
mesh-core/tests/ffi_roundtrip.rs
mesh-core/tests/pocp_props.rs
mesh-core/tests/codec_props.rs
mesh-core/bindings/kotlin/uniffi/mesh_core/mesh_core.kt
mesh-core/src/bin/uniffi-bindgen.rs
mesh-core/src/beacon.rs
mesh-core/src/lib.rs
mesh-core/src/pocp.rs
mesh-core/src/private.rs
mesh-core/src/radio.rs
mesh-core/src/trust.rs
mesh-core/src/vdl.rs
mesh-core/src/codec.rs
mesh-core/src/crypto.rs
mesh-core/src/ffi.rs
mesh-core/src/message.rs
mesh-core/src/statemachine.rs
mesh-core/Cargo.toml
cockroachat/mesh-core/fuzz/Cargo.toml
cockroachat/mesh-core/fuzz/fuzz_targets/decode.rs
cockroachat/mesh-core/tests/codec_props.rs
cockroachat/mesh-core/tests/ffi_roundtrip.rs
cockroachat/mesh-core/tests/pocp_props.rs
cockroachat/mesh-core/bindings/kotlin/uniffi/mesh_core/mesh_core.kt
cockroachat/mesh-core/Cargo.toml
cockroachat/mesh-core/src/bin/uniffi-bindgen.rs
cockroachat/mesh-core/src/radio.rs
cockroachat/mesh-core/src/beacon.rs
cockroachat/mesh-core/src/codec.rs
cockroachat/mesh-core/src/crypto.rs
cockroachat/mesh-core/src/ffi.rs
cockroachat/mesh-core/src/lib.rs
cockroachat/mesh-core/src/message.rs
cockroachat/mesh-core/src/pocp.rs
cockroachat/mesh-core/src/private.rs
cockroachat/mesh-core/src/statemachine.rs
cockroachat/mesh-core/src/store.rs
cockroachat/mesh-core/src/trust.rs
cockroachat/mesh-core/src/vdl.rs
cockroachat/laptop/Cargo.toml
cockroachat/laptop/src/main.rs
cockroachat/android/app/src/main/java/org/cockroachat/mesh/ui/MeshUi.kt
cockroachat/android/app/src/main/java/org/cockroachat/mesh/ui/Drawer.kt
cockroachat/android/app/src/main/java/org/cockroachat/mesh/ui/Theme.kt
cockroachat/android/app/src/main/java/org/cockroachat/mesh/MeshService.kt
cockroachat/android/app/src/main/java/org/cockroachat/mesh/BleRadio.kt
cockroachat/android/app/src/main/java/org/cockroachat/mesh/Config.kt
cockroachat/android/app/src/main/java/org/cockroachat/mesh/GattPlane.kt
cockroachat/android/app/src/main/java/org/cockroachat/mesh/MainActivity.kt
cockroachat/android/app/src/main/java/org/cockroachat/mesh/Measurement.kt
cockroachat/android/app/src/main/java/org/cockroachat/mesh/MeshApp.kt
cockroachat/android/app/src/main/java/org/cockroachat/mesh/MeshState.kt
cockroachat/android/app/src/main/java/org/cockroachat/mesh/PairStore.kt
cockroachat/android/app/src/main/java/uniffi/mesh_core/mesh_core.kt

```

**Tool: read**

**Input:**
```json
{
  "filePath": "/home/Likun/projects/cockroachat/README.md"
}
```

**Output:**
```
<path>/home/Likun/projects/cockroachat/README.md</path>
<type>file</type>
<content>
1: # bileichat
2: 
3: **Offline Decentralized Mesh Messaging for Protests & Emergencies**
4: 
5: [![Rust Core](https://img.shields.io/badge/Core-Rust-b7410e?style=flat-square&logo=rust)](mesh-core/)
6: [![Android](https://img.shields.io/badge/Platform-Android%20(Kotlin)-3DDC84?style=flat-square&logo=android)](android/)
7: [![BLE 5.0](https://img.shields.io/badge/Transport-BLE%205.0%20Extended-0082FC?style=flat-square&logo=bluetooth)]()
8: [![License](https://img.shields.io/badge/License-AGPL--3.0-orange?style=flat-square)](mesh-core/Cargo.toml)
9: 
10: *Phones relay emergency alerts directly to each other using Bluetooth Low Energy — no cell towers, Wi-Fi routers, central servers, internet access, or user accounts required.*
11: 
12: ---
13: 
14: ## What is bileichat?
15: 
16: During protests, civil demonstrations, or natural disasters, cellular networks and Wi-Fi are frequently jammed, monitored, or shut down. 
17: 
18: **bileichat** turns nearby smartphones into a resilient, self-healing peer-to-peer mesh network. Devices pass short emergency alerts phone-to-phone through the crowd automatically.
19: 
20: ### Key Highlights
21: - **100% Offline & Serverless**: Works entirely over Bluetooth Low Energy (BLE 5.0).
22: - **Anti-Fake Alert Protection**: Tier-1 local alerts require a Proof-of-Co-Presence witness; witnessless public frames are relay-only and never displayed. Broadcast corroboration counts only claims heard directly over the air — and is shown as a hint, never as a guarantee (a determined *nearby* attacker can forge claims; see §Tier 2 below).
23: - **Self-Destructing Identity**: Marks and signing keys rotate every epoch over a one-way beacon chain. v2 private pairings ratchet message keys every epoch from a seed mixed with deleted pairing salts — a seized phone exposes at most the current and previous epoch of private history.
24: - **Crash-Proof Rust Core**: Every packet is parsed and verified in memory-safe Rust before forwarding, preventing crash-attacks (zip bombs).
25: - **Danger-Only Alerts**: The public mesh strictly carries danger signals (e.g. teargas, police kettling, medical emergency). Silence is never assumed to mean safety.
26: 
27: ---
28: 
29: ## Platform Support
30: 
31: | Component | Platform | Details |
32: |:---|:---|:---|
33: | **`mesh-core`** | Rust (Core Library) | Contains all protocol parsing, security, cryptography, and relay state machine. |
34: | **`android`** | Kotlin (Android App) | Foreground Service handling BLE 5.0 Extended Advertising and UI rendering. |
35: | **`laptop`** | Rust (Linux Desktop, deprecated) | Phone-to-phone only; laptop client is no longer maintained. |
36: 
37: ---
38: 
39: ## Architecture & 3-Tier Messaging Model
40: 
41: The protocol uses a 3-tier messaging model to balance latency, crowd coverage, and security:
42: 
43: ```
44: +-------------------------------------------------------------------------------+
45: |                       3-Tier Messaging Architecture                           |
46: +-------------------------------------------------------------------------------+
47: 
48:   Tier 1: Immediate Local Broadcast (~30m Radius)
49:   [ Sender ] ---> (BLE Extended Adv) ---> [ Nearby Nodes ]
50:    * 1-Hop direct proximity broadcast for immediate danger ground truth.
51:    * Authenticated by Proof-of-Co-Presence (PoCP) physical witness.
52: 
53:   Tier 2: Multi-Hop Regional Mesh Flood
54:   [ Sender ] ---> [ Relay Node 1 ] ---> [ Relay Node 2 ] ---> [ Crowd Mesh ]
55:    * Multi-hop flood re-broadcasted through the crowd.
56:    * Prioritized relay queue, epoch-bucketed frame-hash dedup & TTL limits.
57: 
58:   Tier 3: Encrypted Direct Private Message
59:   [ Sender ] ================================================> [ Recipient ]
60:    * End-to-end encrypted pairwise message (ChaCha20-Poly1305).
61:    * Gated by Proof-of-Work (VDL witness) to prevent network spam.
62: 
63: +-------------------------------------------------------------------------------+
64: ```
65: 
66: ### Tier Summary
67: 
68: 1. **Tier 1 — Immediate Local Alerts (~30m)**: Instant alerts broadcasted to people right next to you. Display requires a valid Proof-of-Co-Presence witness; the sender repeats until it hears its own echo, then re-airs sparsely for up to 30 minutes.
69: 2. **Tier 2 — Crowd-Relayed Regional Alerts**: Multi-hop alerts propagated through the mesh. Displayed frames carry a valid witness; the badge shows how many *distinct, directly overheard* devices vouched for the same alert body — a hint, not a guarantee.
70: 3. **Tier 3 — Encrypted Direct Messages**: Pairwise private messages between trusted contacts with built-in spam protection (Proof-of-Work) and epoch-ratcheted forward-secret keys (v2 pairing).
71: 
72: ### Real-World Crowd Propagation Examples
73: 
74: #### **Tier 1 Example: Immediate Local Alert (1-Hop / Direct RF Range)**
75: * **Scenario**: A user at the **North Gate** sees teargas deployed nearby and sends an immediate local alert *"TEAR GAS AT NORTH GATE"*.
76: * **Flow**:
77:   1. The frame is generated with `TTL = 1` and `MsgType::LocalImmediate`, carrying a PoCP witness bound to the sender's cell sketch (at epoch rollover, the previous epoch's completed sketch is signed and accepted).
78:   2. Broadcasted directly via BLE Extended Advertising to all devices within **10–30 meters** (1-hop direct radio range).
79:   3. **Display**: Displays on nearby devices **only if the PoCP witness verifies** against the receiver's own cell sketch. Frames with no witness are **relay-only — never displayed** (a remote injector cannot pass the co-presence gate without hearing the cell's marks).
80:   4. **Propagation**: Relayed **exactly once** — any receiving node relays it with TTL clobbered to 0, so the hop bound holds even against an adversary advertising `ttl=255`.
81:   5. **Receipt**: When the originator hears its own relayed echo, it knows at least one peer carried it (an echo is *not* proof of delivery — a single relay can forge it). The sender then re-airs the alert sparsely (every 4th epoch) until a 30-minute cap, instead of screaming every epoch forever.
82:   6. **Beacon Entropy**: Nearby devices collect the frame's sender mark as a physical co-presence witness (`localImmediateMarks`) to generate beacon entropy for key rotation.
83: 
84: #### **Tier 2 Example: Crowd-Relayed Regional Mesh Broadcast (Multi-Hop / Spatial Diversity)**
85: * **Scenario**: A user at the **North Gate** broadcasts a regional warning *"POLICE KETTLING NORTH EXIT"*.
86: * **Flow**:
87:   1. **Origination**: The packet is sent with `TTL = 8` (`MsgType::RegionalPropagated`) carrying the sender's local cell sketch and a signed PoCP witness.
88:   2. **Display Gate — valid witness required**: Before any device shows the alert, it checks that the witness MAC (sender's cell sketch vs receiver's observed marks) passes — **any Jaccard outcome** qualifies; a failed MAC check means the frame is relay-only, never displayed. This prevents remote injectors who aren't in the crowd from making their frame pop up on any phone.
89:   3. **Corroboration as hint, not lock**: Each receiving node that already holds the same body text from another overheard direct transmission increments a local counter. The UI displays this count on the badge: *"3 nearby devices just sent the same alert"*. This is **not** a verified count — a single nearby attacker can fabricate multiple claims — but in practice, a high count across different physical locations increases confidence. The corroboration score is scoped to the device's own radio observations; it does not aggregate across the mesh.
90:   4. **Mesh Hopping**: The packet hops phone-to-phone across the crowd, each relay decrementing TTL and pushing it onto the GATT plane too (`relayOnce` on both advertisement and GATT). Relay nodes use a priority queue (LOCAL echo > regional > private), draining only when `radio.capacityAvailable()`.
91:   5. **TTL clobbering**: Unlike the old design where TTL was trusted from the wire, every hop now clobbers TTL to `min(ttl, originator_ttl - hop_count)` so an adversary cannot inflate range by setting `ttl=255`.
92:   6. **Loop Suppression**: Originators stop re-broadcasting once they hear their own reflection (`ownFrameHash`), and relay nodes suppress duplicates using a per-epoch bounded dedup bucket (capped at 1024 entries, 0-indexed by frame-hash byte mod 1024).
93: 
94: #### **Tier 3 Example: Encrypted Direct Private Chat (End-to-End AEAD / Oblivious Mesh)**
95: * **Scenario**: Alice wants to send a private message to Bob *"Meet at South Entrance in 10 mins"* in a dense crowd.
96: * **Flow**:
97:   1. **v2 Out-of-Band Pairing**: Before any private messages can be exchanged, Alice and Bob pair by scanning each other's QR codes. Each QR carries a public key **plus a 32-byte random salt**. The shared chain seed is `BLAKE3(X25519_DH(alice_sk, bob_pk) ‖ sort(alice_salt, bob_salt))`. Both salts exist only in memory and are deleted immediately after pairing — a seized phone cannot recompute the seed without the salts (`pairSeedV2`).
98:   2. **Epoch Ratchet**: Starting from the chain seed, message keys advance as `key_e = BLAKE3(key_{e-1} ‖ epoch)`. At any point, the phone stores the current key and the previous key (to handle reordered delivery). A seized phone exposes at most the current and previous epoch's messages (`pairRatchet`).
99:   3. **Proof-of-Work & Encryption**: Alice's phone computes a VDL proof-of-work witness once per frame (`vdlCheckFrame`) — not once per contact — to rate-limit spam. She encrypts the body using ChaCha20-Poly1305 under the current epoch's message key for Bob.
100:   4. **Private Counter**: Each outgoing private frame carries an epoch-scoped per-contact counter. The counter's base is randomly chosen at the start of each epoch and stored in EncryptedSharedPreferences — the old plaintext `PRIVATE_COUNTER_KEY` has been removed.
101:   5. **No Recipient Address on Wire**: The frame contains no recipient address, phone number, or user ID.
102:   6. **Oblivious Multi-Hop Relay**: Intermediary nodes in the crowd verify the Ed25519 signature and VDL PoW witness once (`vdlCheckFrame`). They **cannot read the message or know who it is for**, but they decrement TTL by 1 and relay it across the mesh (`advertiseRelayOnce` + GATT `relayOnce`).
103:   7. **Constant-Time Trial Decryption**: Bob's phone (and all receiving phones) runs `vdlCheckFrame` once, then fetches its `candidateKeys` from the encrypted contact store (avoiding Keystore burn per contact). It trial-decrypts the body against those keys in sequence without breaking early (preventing timing side-channel attacks via `openPrivateBodyOnly`).
104:   8. **Delivery**: Bob's phone successfully authenticates the Poly1305 tag and displays `🔒 Alice: Meet at South Entrance in 10 mins`.
105: 
106: ---
107: 
108: ## Implementation Status (v0)
109: 
110: | Module | Description | Status | Tests |
111: |:---|:---|:---:|:---:|
112: | **`codec`** | Zero-allocation fixed 226-byte packet encoder/decoder (reserved-tail check) | Implemented | 11 |
113: | **`crypto`** | Ed25519 signing, BLAKE3 KDF, X25519 DH, ChaCha20 AEAD, forward-secure v2 ratchet | Implemented | 10 |
114: | **`message`** | Public danger alert & private message frame generator (wit_epoch param) | Implemented | 20 |
115: | **`pocp`** | Physical proximity verification (Proof-of-Co-Presence) | Implemented | 23 |
116: | **`beacon`** | Self-clocking key rotation & forward secrecy beacon | Implemented | 13 |
117: | **`private`** | Tier-3 encrypted direct messaging with per-epoch random counter base | Implemented | 6 |
118: | **`vdl`** | Proof-of-work cost gate for spam protection (vdlCheckFrame) | Implemented | 5 |
119: | **`statemachine`** | Packet processing, relay decisions, per-epoch dedup bucket (1024-cap) | Implemented | 13 |
120: | **`trust`** | Multi-cell crowd corroboration aggregator (direct-heard only) | Implemented | 5 |
121: | **`store`** | Memory-bounded message buffer & instant panic wipe | Implemented | — |
122: | **`ffi`** | Language bindings for Android (Kotlin) — 7 new exports (pairSeedV2, pairRatchet, vdlCheckFrame, openPrivateBodyOnly, makeMessageFrameWithWitness wit_epoch, BeaconFfi.wipe) | Implemented | 14 |
123: 
124: ---
125: 
126: ## Key Security Guarantees
127: 
128: 1. **Parse-Before-Forward**: Incoming data is validated in memory-safe Rust before any decision is made to display or relay it.
129: 2. **Fixed 226-Byte Packet**: No variable lengths, no compression, zero room for buffer overflow or zip-bomb attacks.
130: 3. **Danger-Only Public Mesh**: Public broadcasts carry danger alerts only. Nodes cannot broadcast "all clear" signals.
131: 4. **Instant Panic Wipe**: A single command instantly zero-fills and purges all stored state and cryptographic keys.
132: 
133: ---
134: 
135: ## Developer Quick Start
136: 
137: ```bash
138: # Clone the repository
139: git clone https://github.com/howtocuddle/cockrochat.git
140: cd bileichat/mesh-core
141: 
142: # Run test suite (cryptographic vectors, codec safety, property tests)
143: cargo test
144: 
145: # Build release binary
146: cargo build --release
147: 
148: # Run Linux laptop node (requires BlueZ, Bluetooth 5 hardware, and root/CAP_NET_ADMIN privileges)
149: cd ../laptop
150: sudo cargo run
151: ```
152: 
153: ---
154: 
155: ## Technical Glossary
156: 
157: This glossary explains technical terms and protocol concepts used throughout `bileichat`.
158: 
159: ### Cryptography & Security Terms
160: 
161: - **BLAKE3**: An ultra-fast cryptographic hash function used for deriving keys, hashing marks, and chaining epoch seeds.
162: - **ChaCha20-Poly1305**: A high-speed authenticated encryption scheme used to keep Tier-3 private messages secure and tamper-proof.
163: - **Ed25519**: A public-key signature scheme used to verify message authenticity without revealing private identity.
164: - **Ephemeral Keys**: Temporary encryption/signing keys that rotate automatically, ensuring past communications remain secure even if a device is later inspected.
165: - **Forward Secrecy**: A security property guaranteeing that compromised current keys cannot be used to decrypt past session data.
166: - **Panic Wipe**: An emergency function that immediately zero-fills and purges all in-memory cryptographic keys and stored messages.
167: - **Proof-of-Work (PoW) / VDL**: Verifiable Delay Lottery — a brief computational task required before sending private messages to prevent spammers from flooding the network.
168: - **X25519**: A Diffie-Hellman key exchange algorithm enabling two devices to establish a shared secret key out-of-band (e.g. via QR code).
169: 
170: ### Mesh Protocol Terms
171: 
172: - **BLE 5.0 Extended Advertising**: A Bluetooth Low Energy standard allowing devices to broadcast larger packets (up to 255 bytes) without requiring Bluetooth pairing.
173: - **Epoch**: A fixed time window (e.g., 10 seconds in testing, minutes in production) during which devices sample background signals and rotate internal keys.
174: - **Frame Hash (Dedup Key)**: A unique 16-byte identifier computed from a message's contents, allowing relay nodes to ignore duplicate broadcasts.
175: - **Jaccard Similarity ($\tau$)**: A mathematical formula measuring set similarity. In `bileichat`, it determines whether two devices share the same physical radio environment.
176: - **KMV Sketch (K-Minimum Values)**: A compact summary of ambient Bluetooth signals, allowing devices to compare physical surroundings efficiently in memory.
177: - **LE Coded PHY**: A Bluetooth 5 mode using error correction (S=8) to quadruple radio range, ideal for dense or obstructed crowd environments.
178: - **Parse-Before-Forward**: The security rule requiring every packet to be fully validated in Rust before being displayed or relayed.
179: - **Proof-of-Co-Presence (PoCP)**: A cryptographic mechanism verifying that a message originated from someone physically present in the crowd cell.
180: - **RSSI (Received Signal Strength Indicator)**: A measurement of signal power (in dBm). Closer devices show higher RSSI values (e.g. -40 dBm), while distant devices show lower values (e.g. -80 dBm).
181: - **Spatial Diversity**: A security mechanism where alert confidence scales based on corroboration from distinct physical geographic cells (ambient RF observations), ignoring remote virtual identity counts (Sybil attacks).
182: - **Trickle Algorithm**: An epidemic broadcast algorithm (RFC 6206) that adjusts retransmission intervals based on crowd density to conserve battery and bandwidth.
183: - **TTL (Time-To-Live)**: A hop counter on packets. Each relay decrements TTL by 1; when it reaches 0, the packet stops propagating.
184: - **UniFFI**: Mozilla's multi-language binding generator used to connect the Rust core cleanly to Kotlin (Android) and Swift (iOS).
185: 
186: ---
187: 
188: <div align="center">
189: <sub>Built for human safety and free expression. No accounts. No servers. No internet. Just mesh.</sub>
190: </div>

(End of file - total 190 lines)
</content>
```

---

## Assistant (Plan · Kimi K3 (2x usage) · 20.9s)

_Thinking:_

This is a security audit request for a Bluetooth mesh chat application. The main source is `mesh-core/` (Rust) plus `android/` (Kotlin) plus `laptop/`. There's also `cockroachat/` which appears to be an older copy.

I'm in plan mode, so I should:
1. Explore the codebase thoroughly (read-only is fine — reading files and delegating explore agents is allowed)
2. Identify security vulnerabilities
3. Present findings as a plan/report

The user asked to "find security vulnerabilities" — this is an analysis task, not a code-change task. In plan mode, I should do the analysis and present findings. Actually, plan mode says my responsibility is to construct a plan. But the user's request is essentially an audit — the deliverable is the audit findings themselves. I think the best approach: do the security analysis now (read-only work is allowed), and present findings + a remediation plan. Let me check what plan mode permits: "You may ONLY observe, analyze, and plan." So analysis is fine. The final output should be a well-formed plan — in this case, the "plan" would be the audit findings and remediation plan.

Let me explore the security-critical code. Key files:
- mesh-core/src/crypto.rs
- mesh-core/src/codec.rs
- mesh-core/src/message.rs
- mesh-core/src/pocp.rs
- mesh-core/src/beacon.rs
- mesh-core/src/private.rs
- mesh-core/src/vdl.rs
- mesh-core/src/statemachine.rs
- mesh-core/src/trust.rs
- mesh-core/src/ffi.rs
- android: PairStore.kt, GattPlane.kt, BleRadio.kt, MeshService.kt, Config.kt

Note there are two copies: mesh-core/ (bileichat, newer — APKs 0.8-0.15) and cockroachat/mesh-core (older, 0.5-0.7). The root has README for bileichat. The current one appears to be the top-level mesh-core + android (org.bileichat). Let me check git status to see which is current.

Given the scale, I should delegate parallel explore agents to audit different modules, then synthesize. But I can also read the files myself since they're probably small-ish. Let me check sizes first.

This is a security audit task. Let me explore the codebase structure and analyze the security-critical code. First, let me check the repo layout and file sizes.

**Tool: bash**

**Input:**
```json
{
  "command": "wc -l mesh-core/src/*.rs android/app/src/main/java/org/bileichat/mesh/*.kt android/app/src/main/java/org/bileichat/mesh/ui/*.kt laptop/src/main.rs 2>/dev/null; echo ---; git log --oneline -8; git status --short | head"
}
```

**Output:**
```
   270 mesh-core/src/beacon.rs
   181 mesh-core/src/codec.rs
   352 mesh-core/src/crypto.rs
   814 mesh-core/src/ffi.rs
    28 mesh-core/src/lib.rs
   723 mesh-core/src/message.rs
   601 mesh-core/src/pocp.rs
   270 mesh-core/src/private.rs
    20 mesh-core/src/radio.rs
   580 mesh-core/src/statemachine.rs
   199 mesh-core/src/trust.rs
   117 mesh-core/src/vdl.rs
   706 android/app/src/main/java/org/bileichat/mesh/BleRadio.kt
    91 android/app/src/main/java/org/bileichat/mesh/Config.kt
  1022 android/app/src/main/java/org/bileichat/mesh/GattPlane.kt
   517 android/app/src/main/java/org/bileichat/mesh/MainActivity.kt
   184 android/app/src/main/java/org/bileichat/mesh/Measurement.kt
    32 android/app/src/main/java/org/bileichat/mesh/MeshApp.kt
  1515 android/app/src/main/java/org/bileichat/mesh/MeshService.kt
   247 android/app/src/main/java/org/bileichat/mesh/MeshState.kt
   514 android/app/src/main/java/org/bileichat/mesh/PairStore.kt
   436 android/app/src/main/java/org/bileichat/mesh/ui/Drawer.kt
  1196 android/app/src/main/java/org/bileichat/mesh/ui/MeshUi.kt
   115 android/app/src/main/java/org/bileichat/mesh/ui/Theme.kt
   459 laptop/src/main.rs
 11189 total
---
fa25f61 chore: rename branding from cockroachat to BileiChat
5f6f883 feat: resolve security audit issues (A1-A3, B1-B10, C1-C9, D1-D6)
0f2bd30 feat: restore v0.5-unified Jetpack Compose UI (industrial AMOLED aesthetic) and update README screenshots
dd8eb1b docs: add UI screenshots for live (chat) and rig (field tool) flavors
faeb04f docs: update README with UI overhaul — dual build flavors (rig/live), XML Views, tier-colored chat bubbles
b000e7b docs: add real-world crowd propagation examples for all 3 messaging tiers to README
ee7f070 Add Spatial Diversity explicitly to Key Highlights, Tier 2 description, and Technical Glossary in README
59a5c9c Update diagram in README to show Tier 1, Tier 2, Tier 3 messaging model instead of platform shims
 M android/app/src/main/java/org/bileichat/mesh/BleRadio.kt
 M android/app/src/main/java/org/bileichat/mesh/MainActivity.kt
 M android/app/src/main/java/org/bileichat/mesh/MeshService.kt
 M android/app/src/main/java/org/bileichat/mesh/MeshState.kt
 M android/app/src/main/java/org/bileichat/mesh/PairStore.kt
 M android/app/src/main/java/org/bileichat/mesh/ui/MeshUi.kt
 M android/app/src/main/java/org/bileichat/mesh/ui/Theme.kt
?? cockroachat/

```

---

## Assistant (Plan · Kimi K3 (2x usage) · 1180.2s)

_Thinking:_

The current working tree is `mesh-core/` + `android/` (bileichat). `cockroachat/` is untracked (old copy). The audit should focus on the current code: top-level mesh-core + android.

The codebase is ~11k lines. I'll delegate parallel deep-dives to explore agents for different modules, plus read key files myself. Actually, given plan mode and the size, spawning a few parallel explore agents with focused security-audit prompts is the efficient route. Let me spawn 3-4 agents:

1. mesh-core crypto/codec/message (Rust core: crypto.rs, codec.rs, message.rs, vdl.rs, lib.rs)
2. mesh-core protocol logic (pocp.rs, beacon.rs, private.rs, statemachine.rs, trust.rs, ffi.rs)
3. Android layer (PairStore, GattPlane, BleRadio, MeshService, Config, MeshState, MainActivity)

Each returns vulnerabilities. Then I synthesize. I should also read a few key files myself to verify claims, since agent outputs should be verified for an audit. Let me spawn the agents with very detailed prompts.

**Tool: task**

**Input:**
```json
{
  "description": "Audit mesh-core crypto/codec",
  "subagent_type": "explore",
  "prompt": "You are performing a security audit (very thorough) of a BLE mesh chat app's Rust core. READ-ONLY: do not modify anything.\n\nFiles to audit in /home/Likun/projects/cockroachat/mesh-core/src/:\n- crypto.rs (Ed25519, X25519, BLAKE3 KDF, ChaCha20-Poly1305, v2 pairing seed, epoch ratchet)\n- codec.rs (fixed 226-byte packet encoder/decoder)\n- message.rs (public alert + private message frame construction/parsing)\n- vdl.rs (proof-of-work spam gate)\n- lib.rs\nAlso check /home/Likun/projects/cockroachat/mesh-core/Cargo.toml for dependency versions/risks.\n\nHunt specifically for:\n1. Crypto misuse: nonce reuse (especially ChaCha20-Poly1305 nonce construction from counter/epoch — can two different messages ever share key+nonce?), missing AAD binding (header fields not authenticated?), weak KDF info/context separation, key confusion between v1/v2 pairing, downgrade paths, small/grindable PoW difficulty, predictable randomness (non-CSPRNG), missing zeroization of secrets, insecure comparisons (non-constant-time where it matters).\n2. Codec/parser bugs: length confusion, index-out-of-bounds panics reachable from remote input (panics = DoS via malicious BLE frame), integer overflow/underflow, reserved-field checks bypassable, ambiguous parsing.\n3. Signature issues: what exactly is signed vs not signed (can unsigned fields be mutated by relays — e.g., TTL, msg type, witness?), signature replay across epochs/contexts (missing domain separation), malleability.\n4. PoW (vdl.rs): difficulty too low, verification cost asymmetry, bypass paths, can PoW be precomputed/reused across messages/epochs?\n\nFor EACH finding report: file:line, severity (critical/high/medium/low), concrete attack scenario, and the exact code snippet involved. Verify each claim by actually reading the code — no speculation. Also list what is done CORRECTLY (brief). Return a structured findings list."
}
```

**Output:**
```
<task id="ses_062dfa422ffendtpD0632jFSH4" state="completed">
<task_result>
Audit complete. All claims below were verified directly against the source (including vendored dependency sources in `~/.cargo/registry` and `Cargo.lock`). Nothing was modified.

---

# Security Audit: mesh-core (BLE mesh chat Rust core)

**Scope read:** `crypto.rs`, `codec.rs`, `message.rs`, `vdl.rs`, `lib.rs`, `Cargo.toml`, plus the directly-implicated modules needed to verify claims: `private.rs` (nonce/AEAD), `pocp.rs`, `statemachine.rs`, `beacon.rs`, `ffi.rs`, `trust.rs`, `radio.rs`, `Cargo.lock`, and vendored `ed25519-dalek-3.0.0` / `aead-0.6.1` sources.

**Verdict summary:** No critical flaws found. The codec/parser is clean (no remotely-reachable panics), signature verification is the strictest available variant, and KDF domain separation is thorough. The serious issues are: a nonce-reuse pathway resting entirely on an unenforced shim contract, a documented PoCP grind against small cells, missing freshness/replay enforcement in the core, and a precomputable, low-difficulty PoW.

---

## HIGH

### H1. ChaCha20-Poly1305 nonce reuse if the shim counter contract fails — and the API silently invites it
**Files:** `/home/Likun/projects/cockroachat/mesh-core/src/private.rs:53-58`, `/home/Likun/projects/cockroachat/mesh-core/src/message.rs:257`, `/home/Likun/projects/cockroachat/mesh-core/src/message.rs:236-243`

```rust
// private.rs:53
fn nonce_for(epoch: u32, sender_pk: &[u8; 32], counter: u64) -> [u8; 12] {
    let mut n = [0u8; 12];
    n[..4].copy_from_slice(&epoch.to_be_bytes());
    n[4..].copy_from_slice(&nonce_suffix(sender_pk, counter as u32 as u64)); // silent u64→u32 truncation
    n
}
// message.rs:257
div_sketch[4..8].copy_from_slice(&(counter as u32).to_be_bytes()); // truncation again
```

`nonce = epoch_be(4) || BLAKE3("…nonce" || pk || (counter mod 2^32))[..8]`, key = static `pair_key`. Nonce uniqueness under a given pair key depends entirely on the shim's counter being monotonic **mod 2^32 within one epoch**. Two things make this fragile:

1. **The contract is unenforceable by the core.** The doc (`message.rs:222-223`) says "The shim persists it across service restarts." A crash between counter increment and flash write, or a reinstall restoring an old counter while the beacon chain state (and thus `pk`) is restored, silently produces key+nonce reuse with *different* plaintexts → catastrophic: XOR of the two 48-byte plaintext blocks is exposed and the Poly1305 one-time key becomes recoverable (standard ChaCha20-Poly1305 nonce-reuse forgery).
2. **Silent truncation:** the public API takes `counter: u64`, but only the low 32 bits are used, with no `counter < 2^32` guard, error, or debug-assert. Any two u64 values congruent mod 2^32 produce identical nonces. The comment acknowledges this but the type still lies.

**Attack scenario:** adversary records ciphertext frames; victim's phone crashes mid-epoch and restarts with counter reset (or a shim bug passes a non-monotonic value); two different texts seal under identical `(pair_key, epoch, pk, counter_low32)` → plaintext recovery + ciphertext forgery against that pair.

**Mitigations already present (verified):** `pk` rotates per epoch via the beacon chain (`crypto.rs:32-40`), and `epoch` is in the nonce, so reuse requires a *same-epoch* counter collision. **Recommended fix:** hash 8 random bytes (placed in the currently-unused, *signed* `div_sketch[8..16]`) into the nonce suffix, making reuse impossible even on counter reset.

---

### H2. PoCP co-presence is grindable for cells of ≤3 marks (documented KNOWN LIMIT — claims verified)
**File:** `/home/Likun/projects/cockroachat/mesh-core/src/pocp.rs:1-36` (doc), mechanics at `pocp.rs:151-156`, `pocp.rs:180-186`, `pocp.rs:83-97`

```rust
// pocp.rs:151
fn witness_key(div_sketch: &[u8; 16], seed: u32) -> [u8; 32] {
    let mut material = [0u8; 20];
    material[..16].copy_from_slice(div_sketch);      // public, attacker-chosen
    material[16..].copy_from_slice(&seed.to_le_bytes()); // public epoch
    blake3::derive_key("mesh-core:v1:pocp-wit", &material)
}
```

I verified the doc's claims against the code: the witness MAC key is derived **only from public values**, so anyone can mint a valid witness for any claimed sketch (anti-malleability only — correctly implemented constant-time compare at `pocp.rs:199-203`). The co-presence guarantee is entirely the `jaccard` ratio (`pocp.rs:83-97`), which has **no absolute-count floor**: a single claimed element scores `1/N` and clears `tau = 0.3` for any `N ≤ 3`. A remote attacker enumerates all 256 single-element `div_sketch` claims, witnesses each for free, and lands ~2 accepted forgeries against any 2–3-device cell — precisely the small clandestine cells the app is built for. Severity is HIGH but it is honestly documented, and the shim reportedly degrades the confidence badge below 4 marks. No code fix exists short of the deferred fuzzy-extractor redesign (M6+).

---

## MEDIUM

### M1. No replay/freshness enforcement anywhere in the core — private frames re-decrypt, regional frames re-relay
**Files:** `/home/Likun/projects/cockroachat/mesh-core/src/message.rs:298-316`, `/home/Likun/projects/cockroachat/mesh-core/src/statemachine.rs:193-240`, `/home/Likun/projects/cockroachat/mesh-core/src/statemachine.rs:112`

```rust
// message.rs:298 — open_private_frame: decode → type → sig → VDL → AEAD. No epoch/freshness check.
pub fn open_private_frame(buf: &[u8], pair_key: &[u8; 32], difficulty_bits: u8) -> Option<String> {
    ...
    if !crypto::verify(&f.pk, codec::signing_region(arr), &f.sig) { return None; }
    if !vdl::verify(&arr[..WITNESS_PREFIX_END], &f.pocp_wit, difficulty_bits) { return None; }
    private::open_private_body(pair_key, f.epoch, &f.pk, &f.div_sketch, &f.body)
}
```

- `DOMAIN_SIG` (`crypto.rs:11`) is a fixed string; the signature binds only the frame's *content*, not the current epoch or beacon state. A validly-signed frame verifies **forever** (`crypto::verify` is fully self-contained with the embedded pk).
- `open_private_frame` has no epoch-against-wall-clock check; the received counter is never recorded for monotonicity. The only replay defense is the dedup set, which **time-decays entries after ~3 epochs** (`statemachine.rs:112`, `epoch.saturating_sub(oldest_epoch) > 2`) and FIFO-evicts under pressure. An exact-byte replay of a partner's private frame after that window passes dedup, VDL (witness still valid), signature, and AEAD — and displays again as a new message. (Re-wrapping under a new pk is not possible — pk is baked into the nonce — so replay is limited to verbatim copies. Still real.)
- `relay_decision` (`statemachine.rs:204-239`) checks signature + TTL only; it never inspects `epoch`. A witnessless `RegionalPropagated` frame (`make_message_frame` produces these, `message.rs:120-137`) has *no* freshness anchor at all and is re-relayed (TTL capped at 8) every time it is re-injected, at zero cost — no PoW exists on the public plane. Stale "danger" alerts can be re-flooded indefinitely.

### M2. Unchecked wire `epoch` enables dedup future-bucket pre-fill and unbounded relay amplification
**Files:** `/home/Likun/projects/cockroachat/mesh-core/src/statemachine.rs:18`, `statemachine.rs:104-131`, `statemachine.rs:254-256`

```rust
// statemachine.rs:126
if epoch != 0 && self.epoch_counts.get(&epoch).copied().unwrap_or(0) >= EPOCH_BUCKET_CAP {
    return DedupVerdict::BucketFull;
}
```

The frame's `epoch` is attacker-chosen (it need only be signed by a self-generated ephemeral key). An attacker can insert 1024 junk hashes stamped with a *future* epoch E (signing 1024 frames costs milliseconds — the code's own comment at `statemachine.rs:22-27` says so). Those entries are never time-purged until real frames arrive stamped ≥ E+3, so during real epochs E..E+2 every legitimate frame stamped E gets `BucketFull` — a cheap, rolling, per-epoch blackout (bounded in time; severity depends on the shim treating `BucketFull` as drop). Symmetrically, because buckets are keyed by the unvalidated epoch value, an attacker cycling epoch values defeats the 1024-per-bucket relay throttle that the dedup caller contract ("relay only after `Fresh`", `statemachine.rs:254-256`) relies on — each frame then gets up to 8 relay hops of amplification. A core-side sanity window on `epoch` (reject frames beyond ±N epochs of wall clock before dedup insertion) would close both.

### M3. VDL proof-of-work is precomputable across epochs — no freshness anchor
**Files:** `/home/Likun/projects/cockroachat/mesh-core/src/vdl.rs:54-69`, `/home/Likun/projects/cockroachat/mesh-core/src/message.rs:280-282`

```rust
// message.rs:281
let unsigned = codec::encode(&f);
let wit = vdl::solve(&unsigned[..WITNESS_PREFIX_END], difficulty_bits);
```

The PoW prefix is frame bytes `0..102`: mark, header, div_sketch, **epoch**, body — every byte attacker-chosen. The witness is correctly bound to the exact frame (inside `SIG_REGION`, `codec.rs:27`, so no transplant), and reuse across messages/epochs of *different* frames is impossible (epoch is in the prefix — verified `vdl.rs:97-101` test). **But** nothing unpredictable (e.g., a recent beacon-chain output) enters the prefix, so an attacker can grind witnesses for frames stamped with a *future* epoch at leisure, stockpile them for weeks, and dump a burst the moment that epoch arrives — converting the intended steady per-frame cost into a pre-paid, arbitrarily large burst. The module doc admits parallelizability (`vdl.rs:15-18`) but not precomputation. Anchoring the prefix to the current beacon seed (unpredictable one epoch ahead, `beacon.rs:8-12` shows the freshness argument already exists in-tree) would close it.

### M4. VDL difficulty (22 bits) is GPU-trivial; spam gate stops phones, not determined adversaries
**File:** `/home/Likun/projects/cockroachat/mesh-core/src/vdl.rs:24`

```rust
pub const VDL_DIFFICULTY_BITS: u8 = 22;  // ~2^22 ≈ 4.2M BLAKE3 hashes
```

Verification asymmetry is excellent (1 hash, `vdl.rs:67-69`), difficulty is hardcoded at every production call site (verified `ffi.rs:605`, `ffi.rs:614`, `ffi.rs:634`, `statemachine.rs:258` — no wire-reachable difficulty parameter), and `solve` is clamped to 64 bits (`vdl.rs:55`). But 2^22 BLAKE3 hashes is ~1 second on one desktop core and single-digit *milliseconds* on a GPU. A BLE cell can carry maybe tens of 226 B frames/sec; one GPU mints witnesses far faster than the mesh can drain them. Combined with M3 (precompute), the gate excludes casual phone spammers only. Medium because the limitation is documented and the BLE bottleneck bounds absolute damage.

### M5. No zeroization of core-owned secrets despite a seizure/panic-wipe threat model
**Files:** `/home/Likun/projects/cockroachat/mesh-core/src/crypto.rs:33-39`, `crypto.rs:99-105`, `crypto.rs:142-149`, `/home/Likun/projects/cockroachat/mesh-core/src/private.rs:82-88`, `private.rs:104-115`, `/home/Likun/projects/cockroachat/mesh-core/Cargo.toml` (no `zeroize` dep)

Verified: `SigningKey` zeroizes on drop **only** because ed25519-dalek 3.0.0's default features include `zeroize` (vendored `signing.rs:718-726`, `Cargo.toml` `default = ["fast", "zeroize"]`). But mesh-core itself never wipes: the `material` buffer and caller's `seed`/`beacon_seed` in `from_seed`, the X25519 `shared` secret and derived `pair_key` in `pair_derive`, the v2 chain seed and every intermediate `k` in `pair_ratchet`, the 48-byte plaintext block in `seal_private_body`, and the decrypted plaintext in `open_private_body`. `BeaconFfi::wipe` (`ffi.rs:734-740`) shows the project knows the pattern (it manually zeroes the live beacon seed) — the same treatment is missing for pair keys, chain keys, and plaintexts. After `panic_wipe()`, these remain in Rust/FFI memory until process death.

---

## LOW

### L1. v1 static pairing key remains a silent downgrade path
**Files:** `/home/Likun/projects/cockroachat/mesh-core/src/crypto.rs:96-105`, `/home/Likun/projects/cockroachat/mesh-core/src/ffi.rs:546-554`

`pair_derive` (static, no forward secrecy) is still FFI-exported alongside `pair_seed_v2`/`pair_ratchet`, guarded only by a doc warning. The wire frame carries **no indicator** of whether the body was sealed under a v1 static key or a v2 ratcheted epoch key — a recipient cannot detect that a conversation silently used the non-FS scheme. Verified there is no *algebraic* confusion: `mesh-core:v1:pairkey`, `mesh-core:v1:pairseed-v2`, and `mesh-core:v1:pairratchet` are distinct `derive_key` contexts (`crypto.rs:104`, `:124`, `:147`), so v1/v2 keys cannot collide or be interderived.

### L2. AEAD uses empty AAD — header binding rests entirely on the outer signature
**Files:** `/home/Likun/projects/cockroachat/mesh-core/src/crypto.rs:71-83`, vendored `aead-0.6.1/src/lib.rs:432-436`

```rust
// crypto.rs:71
pub fn aead_seal(key: &[u8; 32], nonce: &[u8; 12], pt: &[u8]) -> Vec<u8> {
    ChaCha20Poly1305::new_from_slice(key).expect("32-byte key")
        .encrypt(&Nonce::from(*nonce), pt)   // &[u8] → Payload { msg, aad: b"" } (verified in aead crate)
        .expect("aead encrypt")
}
```

`aad` is `b""`. I attempted to construct a working attack and could not: the 64-byte body is inside `SIG_REGION`, and the nonce already binds epoch + full sender pk + counter, so any header mutation either breaks the Ed25519 signature or (on re-sign with a new key) changes the nonce and fails AEAD. This is a defense-in-depth gap only — passing `SIG_REGION` (or at least `epoch || div_sketch`) as AAD would make the binding explicit and independent of the self-signed outer signature.

### L3. `todo!("M6")` stubs under `panic = "abort"` — process-killing landmines
**Files:** `/home/Likun/projects/cockroachat/mesh-core/src/trust.rs:22,27,32`, `/home/Likun/projects/cockroachat/mesh-core/Cargo.toml:36`

Three `pub fn`s (`merge`, `distinct_estimate`, `corroboration`) are `todo!()`. Not currently reachable via FFI (verified `FfiTrust` only calls `record`/`distinct_count`), but with `panic = "abort"` any future call aborts the entire host app process rather than unwinding. Same profile turns the (remotely-unreachable) `expect` calls at `crypto.rs:46,48,73,75` into aborts. Fail-stop is defensible for the CSPRNG failure; less so for accidental stub calls.

### L4. FFI `open_private_frame` is a per-contact-loop DoS footgun
**File:** `/home/Likun/projects/cockroachat/mesh-core/src/ffi.rs:611-615`

The exported `open_private_frame` re-verifies signature + VDL on every call. The optimized split exists (`vdl_check_frame` + `open_private_body_only`, `ffi.rs:621-645`, with the caller contract documented), but the naive function remains exported and a shim author *will* loop it over contacts — N × (Ed25519 verify + VDL) per frame during a private-frame storm. Consider removing it from the FFI or renaming to make the cost obvious.

---

## INFORMATIONAL / HYGIENE

- **I1.** `hex = "0.4"` is a runtime dependency (`Cargo.toml:20`) used only in `crypto.rs` tests; also duplicated in `[dev-dependencies]` (`Cargo.toml:31`). Dead weight in a security-minimal cdylib.
- **I2.** Exact-pinned versions (`=0.3.9`, `=1.8.5`, `=0.11.0`, `=3.0.0`, `=2.0.1`, `=0.32.0`) aid reproducibility but block automatic security-patch uptake; `getrandom = "0.3"` is unpinned, inconsistently. Resolved versions (`Cargo.lock`: ed25519-dalek 3.0.0 / curve25519-dalek 4.1.3 + 5.0.0, chacha20poly1305 0.11.0, blake3 1.8.5, getrandom 0.3.4) appear current with no advisories I can confirm; run `cargo audit` in CI — note the 32-bit timing concerns historically fixed in curve25519-dalek 4.1.3 are relevant to 32-bit Android targets.
- **I3.** Private frames leave `div_sketch[8..16]` as 8 signed-but-semantically-null bytes (`message.rs:255-257`) — a covert channel for the sender and a future-compat hazard (cf. H1 recommendation to use them for nonce randomness).
- **I4.** Duplicated doc paragraph on `ffi::make_private_frame` (`ffi.rs:577-592`), and it misdescribes the counter as preventing reuse "under the same (seed, epoch) tuple" — it is under the pair key.
- **I5.** Endianness is inconsistent across KDF inputs (`pocp.rs:154` LE seed vs `crypto.rs:146` BE epoch vs wire BE) — consistent within each module, so no bug, but a trap for cross-implementation ports.
- **I6.** `beacon.rs:82` `prev.epoch + 1` can wrap at u32::MAX (unreachable in practice); release profile has no `overflow-checks` — the one wire-reachable arithmetic (`statemachine.rs:112`) correctly uses `saturating_sub`.
- **I7.** `ffi::frame_body_text` (`ffi.rs:176-179`) applies `body_text` without a msg_type check; on a Private frame it interprets ciphertext as `[len][utf8][zeros]` — the zero-tail + UTF-8 gates make a false positive astronomically unlikely, but a type check costs nothing.

---

## Answers to the specific hunt list

1. **Nonce reuse:** possible only via H1 (counter reset/truncation within one epoch under the same pair key + pk). Cross-device collision is 2^-64 via full-pk hashing (regression-tested, `private.rs:241-256`). Cross-epoch is blocked by epoch-in-nonce + per-epoch pk rotation.
2. **AAD:** missing (L2); no working exploit found due to sig + nonce bindings.
3. **KDF separation:** all contexts distinct — verified `ephemeral`, `mark`, `nonce`, `pairkey`, `pairseed-v2`, `pairratchet`, `pocp-wit`, `pocp-cell`, `beacon-advance`, `beacon-entropy`, `vdl`, and `DOMAIN_SIG`. No cross-KDF confusion.
4. **v1/v2 confusion/downgrade:** no algebraic confusion; downgrade path exists (L1).
5. **PoW:** difficulty GPU-low (M4), verify asymmetry good, no wire-reachable bypass, precomputable across epochs (M3), replay-reusable verbatim (M1).
6. **Randomness:** `getrandom` CSPRNG only (`crypto.rs:46,48`); no weak RNG anywhere. Beacon low-entropy mode is honestly flagged (`beacon.rs:29`).
7. **Zeroization:** dalek `SigningKey` wipes on drop; everything else doesn't (M5).
8. **Comparisons:** witness MAC uses a constant-time loop (`pocp.rs:199-203`); AEAD tag compare is inside chacha20poly1305 (constant-time); no secret-dependent branches found on the verify path.
9. **Parser panics from remote input:** none found — every index/slice was traced (`decode` len-checks first; `body_text` bounds len ≤ 63 before slicing; `open_private_body` bounds len ≤ 47; the two `unwrap`s in `ffi.rs:402,750` are on `chunks_exact(16)` outputs and cannot fail; `pocp.rs:62` unwrap is on a fixed 8-byte slice of a 32-byte hash).

## Done correctly (verified, brief)

- **Signature verification is `verify_strict`** (`crypto.rs:68`) — rejects non-canonical S/R and small-order R *and* small-order public keys (vendored `verifying.rs:367-388`). Non-malleable.
- **`SIG_REGION = 0..150` covers everything that matters**: mark, version, msg_type, div_sketch, epoch, body, witness, pk (`codec.rs:26-27`). The witness is signature-bound; TTL is the *only* unsigned semantic field and is hard-capped/clobbered by relay logic (`statemachine.rs:204-239`) with adversarial tests.
- **Codec is total-fail, fixed-offset, panic-free**, with a reserved-tail zero check that pre-empts covert-channel accretion (`codec.rs:96-100`).
- **Body validation is strict on both planes**: length, zero tail, UTF-8 (`message.rs:341-351`, `private.rs:108-115`); private bodies are constant-size 48-byte blocks (no length leakage).
- **Private-plane ordering is correct**: decode → sig → VDL → AEAD (`message.rs:303-315`), and the B5 verify-once/trial-decrypt-N split is sound.
- **`pair_ratchet`** is one-way, rejects backward jumps, and is span-bounded at 8192 (`crypto.rs:135-150`); **`pair_derive`** does the all-zero contributory check (`crypto.rs:101-103`); **`pair_seed_v2`** salt-order canonicalization is correct.
- **Dedup** excludes the hop-mutable region from `frame_hash`, is epoch-aware with saturation, and reports `BucketFull` distinctly from `Duplicate` (the P-DoS fix).
- **Beacon chain** provides genuine forward secrecy with an honest low-entropy flag; `BeaconFfi::wipe` actually zeroes the live seed.
- **`solve` difficulty clamp** (`vdl.rs:55`) and hardcoded production difficulty at all call sites.
</task_result>
</task>
```

**Tool: task**

**Input:**
```json
{
  "description": "Audit mesh-core protocol logic",
  "subagent_type": "explore",
  "prompt": "You are performing a security audit (very thorough) of a BLE mesh chat app's Rust protocol layer. READ-ONLY: do not modify anything.\n\nFiles to audit in /home/Likun/projects/cockroachat/mesh-core/src/:\n- pocp.rs (proof-of-co-presence witness, KMV sketch, Jaccard)\n- beacon.rs (epoch rotation, beacon chain entropy)\n- private.rs (tier-3 encrypted DMs, per-epoch counter base, trial decryption)\n- statemachine.rs (packet processing, relay decisions, dedup, TTL clobbering)\n- trust.rs (corroboration counting, Sybil resistance)\n- ffi.rs (UniFFI exports to Android — look for unsafe argument handling, missing validation at FFI boundary)\nAlso mesh-core/tests/*.rs to see what's covered.\n\nHunt specifically for:\n1. Relay/DoS: dedup bypass (attacker mutating unsigned byte to evade dedup and flood), TTL manipulation, priority queue abuse, amplification, memory exhaustion (unbounded buffers, dedup bucket cap bypass), epoch confusion.\n2. PoCP forgery: can a remote (non-co-present) attacker forge a witness? Can witness be replayed from a different epoch or copied from another frame? Is the witness bound to the message body? Jaccard \"any outcome qualifies\" — does that mean the MAC check is the only gate, and is the MAC key guessable/observable?\n3. Private messaging: trial-decryption oracle/timing, counter base predictability, replay of private frames (same epoch counter reuse → nonce reuse?), missing sender authentication binding, contact enumeration, what happens on epoch rollover edge cases.\n4. Beacon: entropy starvation (empty cell → predictable keys?), can attacker control beacon chain by injecting marks, rollback attacks.\n5. Trust/corroboration: Sybil — can one attacker inflate corroboration count, is anything stopping forged distinct marks?\n6. FFI: panics across FFI, unvalidated inputs from Kotlin, secret leakage through FFI return values.\n\nFor EACH finding: file:line, severity (critical/high/medium/low), concrete attack scenario, exact code snippet. Verify by reading code — no speculation. Also briefly note what's done correctly. Return a structured findings list."
}
```

**Output:**
```
<task id="ses_062df6078ffe0wWEDn9KG4XtTR" state="completed">
<task_result>
I've now read the full protocol layer, the Android shim's ingest pipeline (to verify how core APIs are actually driven), and the test suites. All findings below were verified against the code; line numbers are exact.

---

# Security Audit: mesh-core protocol layer

## Scope note
The core deliberately has no wall clock; epoch freshness is enforced by the shim (K4 gate, `MeshService.kt:939`). Where a core finding is currently contained by the Android shim, I say so — the core API is the contract every shim (iOS, GATT path, future) must re-implement correctly, and the Rust docs rarely state these preconditions.

---

## FINDINGS

### F1 — PoCP "co-presence" is remotely forgeable by div_sketch copying; the witness MAC key is public
**File:** `pocp.rs:151-156`, `pocp.rs:180-186`; claim extraction is plaintext at `codec.rs:103` (wire bytes 18..34)
**Severity:** HIGH

The witness MAC key is derived exclusively from public values:

```rust
// pocp.rs:151
fn witness_key(div_sketch: &[u8; 16], seed: u32) -> [u8; 32] {
    let mut material = [0u8; 20];
    material[..16].copy_from_slice(div_sketch);
    material[16..].copy_from_slice(&seed.to_le_bytes());
    blake3::derive_key("mesh-core:v1:pocp-wit", &material)
}
```

**Attack scenario:** Attacker anywhere within the 8-hop relay range receives a `RegionalPropagated` frame originated in target cell C. The frame carries C's `div_sketch` in cleartext. The attacker originates its *own* frame with its *own* ephemeral key, copies C's `div_sketch` verbatim into bytes 18..34, computes a valid witness for free (`witness_key` is public), and signs. Every verifier in/near C computes Jaccard(copied claim, their local sketch of C) ≈ 1.0 ≥ τ=0.3 → `WitVerdict::Valid` → "co-present origin" badge. The attacker was never near C. The module doc (`pocp.rs:174-179`) discloses this as "RESIDUAL GAP" but frames it as copying "from the same cell" — in fact the copy source travels up to 8 hops, so the attacker needs no proximity at all. The only shim mitigation is a debug log (`MeshService.kt` R1 block, `sketchSeen` reuse → `logDebug`), not a verdict change. For `msgType=2` (BROADCAST) the shim displays on MAC-validity alone (`MeshService.kt:1175`: verdict 1 → `displayOk = true`), and MAC validity is free — so for broadcast the PoCP check only gates the *badge*, and the badge is what F1/F2 forge.

### F2 — Small-cell grind: 256 single-element sketches yield ~2–3 accepted forgeries against any ≤3-mark cell
**File:** `pocp.rs:83-97` (jaccard), `pocp.rs:216-236` (verify_witness_local); documented at `pocp.rs:5-36`
**Severity:** HIGH (documented in-code with a measured table; Android degrades the badge below 4 marks)

```rust
// pocp.rs:90-96 — ratio with no absolute-count floor
let x_len = KMV_K.min(union.len());
let x = &union[..x_len];
let inter_in_x = x.iter().filter(|v| set_a.contains(v) && set_b.contains(v)).count();
inter_in_x as f32 / x_len as f32
```

**Attack scenario:** A remote attacker who never observed the cell claims each of the 256 possible 1-element div_sketches, computing a valid MAC for each for free. Against a local cell of N marks, a hit scores Jaccard 1/N, clearing τ=0.3 for N≤3. Per the in-code measurement: 2.00 accepted forgeries per sweep vs 2-device cells, 2.98 vs 3-device cells. Small clandestine cells — the highest-value targets — are exactly the unprotected ones. The `0xFE/0xFF` remap (`pocp.rs:120-131`) and empty-cell non-match (`pocp.rs:87-89`) are correctly handled; the missing piece is a count floor (`intersection >= 2`), which the doc notes was deferred because it breaks cold-start.

### F3 — Dedup time-decay purge is keyed to an unvalidated wire epoch: one far-future frame wipes the entire dedup set
**File:** `statemachine.rs:104-119`
**Severity:** MEDIUM (core-side; currently gated by the Android shim — see below)

```rust
// statemachine.rs:108
while let Some(oldest_hash) = self.order.front().copied() {
    match self.seen.get(&oldest_hash) {
        Some(&oldest_epoch) if oldest_epoch != 0 && epoch.saturating_sub(oldest_epoch) > 2 => {
            self.order.pop_front();
            self.seen.remove(&oldest_hash);
            self.decrement_bucket(oldest_epoch);
        }
        _ => break,
    }
}
```

**Attack scenario:** The `epoch` argument comes from the frame's epoch field — attacker-signed, attacker-chosen. One validly-signed frame stamped `epoch = u32::MAX` (cost: a single Ed25519 signature over a self-generated key) makes `saturating_sub` huge for every entry → the loop evicts the *entire* seen-set. The ~3-epoch replay/amplification window resets: the attacker immediately re-broadcasts every frame recorded over the last 3 epochs and each is treated `Fresh` (re-displayed, re-relayed mesh-wide), then refills the current epoch's 1024-slot bucket to black out legitimate traffic. The core docs on `check_and_insert_epoch`/`check_epoch` do not state "epoch must be freshness-gated before calling." Android currently saves this: `MeshService.kt:939` drops frames with `|frameEp − ownEpoch| > 2` *before* `checkEpoch`. Any shim omitting K4 is exposed; with K4, an attacker at +2 can still purge entries 3+ epochs old, which decay soon anyway — hence MEDIUM not HIGH.

### F4 — Head-of-line blocking: a future-epoch entry at the FIFO head freezes decay for everything behind it
**File:** `statemachine.rs:108-119` (the `_ => break` at line 117)
**Severity:** LOW

The purge inspects only `order.front()` and breaks on the first non-evictable entry. Insert `(h1, epoch=u32::MAX)` then `(h2, epoch=10)`; a later check at epoch 60 finds h1 at the head (`60.saturating_sub(MAX)=0` → break), so h2 — 50 epochs stale — is never time-decayed. Memory stays bounded by `cap`, so this is not exhaustion, but it defeats the "~3-epoch window" invariant for all entries queued behind an adversarial head and extends the effective replay-suppression window unpredictably. (Epoch-0 sentinel entries from the legacy `check_and_insert` path are *never* time-purged — line 112's `oldest_epoch != 0` guard — so mixing the two FFI APIs also weakens decay.)

### F5 — C8 per-epoch bucket = bounded per-epoch blackout, and bucket identity is wire-controlled
**File:** `statemachine.rs:18`, `statemachine.rs:126-128`, `statemachine.rs:141-143`
**Severity:** MEDIUM (documented tradeoff)

```rust
// statemachine.rs:126
if epoch != 0 && self.epoch_counts.get(&epoch).copied().unwrap_or(0) >= EPOCH_BUCKET_CAP {
    return DedupVerdict::BucketFull;
}
```

Signing 1024 distinct frames stamped with the *current* epoch is milliseconds of work; thereafter every legitimate frame of that epoch is refused (no display, no relay, no measurement) until decay frees the bucket ~3 epochs later. The code does the right things — distinct `BucketFull` verdict (`statemachine.rs:29-36`, fixing the prior silent-blackout bug), no cross-epoch eviction, time-bounded — and the shim logs it as a jamming signal. It remains a cheap, repeatable ~3-epoch silencing of a radio cell, so it belongs in the findings.

### F6 — Private frames: no replay protection in-core; nonce uniqueness rests entirely on shim counter persistence
**File:** `private.rs:53-63`, `private.rs:96-116`, `message.rs:324-331`, `ffi.rs:594-606`
**Severity:** MEDIUM

```rust
// private.rs:53
fn nonce_for(epoch: u32, sender_pk: &[u8; 32], counter: u64) -> [u8; 12] {
    let mut n = [0u8; 12];
    n[..4].copy_from_slice(&epoch.to_be_bytes());
    n[4..].copy_from_slice(&nonce_suffix(sender_pk, counter as u32 as u64));
    n
}
```

Two distinct issues verified:
1. **Replay:** `open_private_body` (`private.rs:96-116`) reconstructs the nonce from the frame's *own* epoch and `div_sketch[4..8]` counter. A bit-for-bit replayed frame therefore always opens. The core keeps no (pk, epoch, counter) seen-cache and has no clock. On Android this is currently contained by three shim layers: K4 ±2-epoch freshness, the ~3-epoch dedup window, and ratchet key deletion (`PairStore.kt:383-398` only offers `prevKey` one epoch back; `noteOpened` fast-forwards only after a successful AEAD open, so an attacker can't force key deletion). But the core's `open_private_frame`/`open_private_body_only` contracts (`message.rs:318-323`) say nothing about replay — a shim that trial-decrypts without the K4/dedup gates accepts stale private frames indefinitely.
2. **Nonce reuse on counter reset:** within an epoch, `pk` is fixed (derived from seed‖beacon_seed), so the counter is the *sole* nonce separator under a pair key. If the shim's persisted counter ever rolls back (restart race, data restore), the same (epoch, pk, counter) seals different plaintexts → ChaCha20 keystream reuse → plaintext XOR recovery and Poly1305 key recovery (forgery). The FFI doc (`ffi.rs:581-583`) states the MUST but the core cannot detect a violation. A random 8-byte nonce in the (currently zeroed) `div_sketch[8..16]` would make this self-healing; the wire space is available.

Also verified but by-design: `counter as u32` truncation is consistent on both sides (`private.rs:47-58`); the ≥2³²-per-epoch case is documented out of scope. Within-pair sender authentication is only pair-granular (symmetric key — either contact can forge the other's "side"; no binding to a long-term identity), and `div_sketch[0..4]=pk[..4]` (`message.rs:255-256`) links a device's private frames within an epoch. Both are inherent to the design; noting for completeness.

### F7 — Trial-decryption path: no oracle, but the shim-side contract is load-bearing
**File:** `message.rs:298-331`, `ffi.rs:622-645`
**Severity:** LOW (verified clean in-core; fragile contract)

Verified good: AEAD tag check precedes all plaintext-structure checks (`private.rs:104-115`), so wrong key / tamper / bad padding / bad UTF-8 are indistinguishable — no padding oracle; `chacha20poly1305` tag comparison is constant-time; VDL verify is one hash (`vdl.rs:67-69`), and `open_private_frame` verifies the signature *before* spending VDL/AEAD work (`message.rs:309-312`, R8). The B5 split (`vdl_check_frame` once + `open_private_body_only` per contact) is sound, and Android iterates all contacts with no early break (`MeshService.kt:1081-1097`, E3) so decrypt-call count doesn't leak the matching contact index. No recipient field on the wire → no contact enumeration. The only soft spot: `open_private_body_only` skips sig+VDL by contract (`ffi.rs:637-641`); a shim calling it without the upstream checks would accept unsigned garbage — a documentation-level hazard, not a code bug.

### F8 — `local_entropy(min_hearers=0)` returns constant entropy and the chain reports it as healthy
**File:** `beacon.rs:55-69`, `beacon.rs:80-86`, `ffi.rs:747-753`
**Severity:** LOW

```rust
// beacon.rs:58
if (unique.len() as u32) < min_hearers { return None; }  // min_hearers=0 → never None
```

With `min_hearers=0`, even an empty mark set yields `Some(H("mesh-core:v1:beacon-entropy"))` — a *constant* — and `advance` then hardcodes `low_entropy: false` (`beacon.rs:83`), defeating the honesty signal. Future seeds become computable to anyone holding the current seed. Android clamps `minHearers` to 1..16 (`Config.kt:MIN_HEARERS_RANGE`), but the core accepts 0 from the FFI (`ffi.rs:747`) without a floor.

### F9 — `BeaconFfi::new` silently accepts a wrong-length/empty seed0
**File:** `ffi.rs:670-681`
**Severity:** LOW

```rust
// ffi.rs:676
pub fn new(seed0: Vec<u8>) -> std::sync::Arc<Self> {
    let seed: [u8; 32] = *blake3::hash(&seed0).as_bytes();
```

A shim bug passing an empty or constant buffer yields a publicly computable chain seed0 (entropy inputs are public wire marks). Confidentiality survives (ephemeral keys and marks are additionally keyed by the secret static seed), but forward secrecy is silently void: seizure of the static seed then recovers all past ephemeral keys. Deliberate anti-abort tradeoff (R6), but a hard error on `len() != 32` would fail loudly instead of degrading silently. Rollback within the chain itself is not possible (`advance` only increments; floor paced; `wipe` is the documented panic path).

### F10 — `observe()` silently truncates marks when the rssi vector is short
**File:** `pocp.rs:66-74`, `ffi.rs:399-405`
**Severity:** LOW

```rust
// pocp.rs:69
for (mark, r) in marks.iter().zip(rssi.iter()) {
```

`zip` truncates to the shorter input: a shim passing fewer RSSI values than marks silently drops trailing marks from the local sketch → smaller N → the F2 grind gets *easier* (1/N clears τ). Fail-open direction for PoCP with no error signaled. Mismatched lengths should be rejected at the FFI boundary.

### F11 — No `tau` validation at the FFI boundary
**File:** `ffi.rs:464-493` (`pocp_verify_witness_local`), `ffi.rs:311-324` (`record_verification`), `pocp.rs:100-102`
**Severity:** LOW

`matches(a, b, tau)` is `jaccard >= tau` with no range check: `tau ≤ 0.0` makes *every* claim match (PoCP disabled, including empty cells at exactly 0.0), `tau = NaN` fails closed (all CellMismatch), `tau > 1` disables everything. Android clamps to 0.10..0.90; the core should assert `tau.is_finite() && (0.0..=1.0).contains(&tau)`.

### F12 — Trust corroboration: one co-located attacker mints up to 32 "distinct cells"
**File:** `trust.rs:44-48`, `trust.rs:86-121`
**Severity:** MEDIUM (documented as HINT-only; display-side impact)

```rust
// trust.rs:114-118 — domination collapses only *similar* claims
let dominated = set.iter().any(|c| pocp::jaccard(&pocp::div_sketch_to_cell(c), &new_cell) >= tau);
if !dominated { set.insert(div_sketch); }
```

Per F1 the witness MAC is free, so a nearby attacker originates ≤32 frames carrying pairwise-dissimilar random div_sketches over the same alert text (same `body_hash` key, `ffi.rs:519-520`) — each counts as a new "cell", capped only by `MAX_CLAIMS_PER_FRAME=32`. The shim's A2 gate (direct-heard TTL only, `MeshService.kt:1226`) does not help: the attacker *is* nearby, so their claims are direct. Mitigations present: all-zero/all-0xFF claims rejected (`trust.rs:87-89`), memory bounded (4096 frame hashes × 32 claims, FIFO eviction, `trust.rs:91-102`), and the UI is supposed to show a hint, never a boolean — the old `distinct≥2` display lock is explicitly called "security theater" in the shim comment. Still: a displayed corroboration count of up to 32 from one Sybil is a meaningful social-engineering lever. Additionally, `record()` cannot verify the witness itself — it trusts the shim to have verified the MAC before recording; that precondition is not stated in the Rust doc.

### F13 — Deferred-insert dedup has a check/act/insert TOCTOU across the FFI
**File:** `ffi.rs:240-263`, `statemachine.rs:95-137`
**Severity:** LOW

`check_epoch` and `insert_epoch` are separate FFI calls; the mutex is dropped between them. Two ingest threads (BLE binder + GATT callback, both feed `ingestFrame`) can both observe `Fresh` for the same hash and both relay/display before either inserts. Impact is bounded (idempotent display; one duplicate relay per race; no panic), and the atomic `check_and_insert_epoch` exists, but the deferred pattern is the documented path for transient-verification retry.

### F14 — Minor FFI hygiene
**File:** `ffi.rs:39-44`, `ffi.rs:204-209`
**Severity:** LOW

- `make_test_frame` (a smoke-test helper minting signed frames from a caller seed) is compiled into the production UniFFI surface.
- `FfiDedup::new` clamps cap to `1 << 20` (`ffi.rs:205`), which is still a ~50–100 MB `HashMap`+`VecDeque` allocation from a single shim argument — generous for an anti-abort clamp.

**Verified clean on the FFI panic surface:** every length check uses `try_into().ok()?`; the `chunks_exact(16).map(try_into().unwrap())` at `ffi.rs:402`/`750` is infallible by construction; all mutex locks use `unwrap_or_else(|e| e.into_inner())` (poison-tolerant, no panic across FFI); `was_panic_wiped` is a one-shot swap; `BeaconFfi::wipe` zeroes the live seed. `panic = "abort"` in release means any missed panic is a crash, not UB — and I found no reachable panic on any FFI path. No secret leaks via return values: `BeaconFfi::seed()` and `pair_derive` return secrets by design; error paths return `None`/`false` uniformly.

---

## What is done correctly (verified)

- **Dedup bypass via unsigned bytes: closed.** `frame_hash` covers `buf[0..214]` (`message.rs:354-359`) — the TTL byte (the only hop-mutable byte) is excluded, so TTL tampering cannot evade dedup; `reserved[1..12]` must be zero or decode fails with `BadReserved` (`codec.rs:96-100`), killing the covert-channel/dedup-mutation vector (D3). A malicious relay *can* re-broadcast a frame verbatim with TTL restored (byte 214 is unsigned by design), but the dedup hash is unchanged, so each honest node still relays it at most once — reach extension is linear, not exponential.
- **TTL handling:** incoming TTL clamped to `DEFAULT_TTL_REGIONAL` *before* decrement (`statemachine.rs:207`, `217`); LocalImmediate clobbered to 0 rather than decremented (`statemachine.rs:228-238`) — an adversary advertising ttl=255 gets exactly one hop.
- **Invariant #2 ordering:** `relay_decision_with_difficulty` decodes → verifies the Ed25519 signature → then decides (`statemachine.rs:197-204`); nothing is relayed pre-verification. Relay amplification is ≤1 frame out per unique frame in.
- **Witness↔body binding:** the PoCP MAC covers bytes 0..102 including the body, and the signature covers 0..150 including the witness (`message.rs:205-214`) — a relay cannot swap body, sketch, or witness. Constant-time MAC compare (`pocp.rs:198-203`).
- **Crypto hygiene:** `verify_strict` Ed25519 (`crypto.rs:68`); X25519 all-zero contributory check (`crypto.rs:101-103`); `pair_ratchet` span-capped at 8192 against wire-controlled epochs (`crypto.rs:139-141`); VDL solve clamps absurd difficulty (`vdl.rs:54-55`); private nonce suffix hashes the *full* pk (`private.rs:34-42`, R4 regression-tested); R8 signature-before-VDL ordering in `open_private_frame`.
- **Robustness fixes present and tested:** distinct `BucketFull` verdict (P-DoS), deferred dedup insert for transient-failure retry, epoch-0 sentinel handling, saturating epoch subtraction (R7), 0xFF→0xFE remap keeping lone senders comparable, empty-cell Jaccard = 0.0, witnessless → relay-only at the shim, GATT/BLE shared ingest with a crash barrier.
- **Shim gates that currently contain core gaps:** K4 ±2-epoch freshness before dedup; E3 constant-work trial-decrypt; config clamping of τ/minHearers/epochMs.

## Test coverage
Present: codec proptests (wrong-len, never-panics, roundtrips, reserved-tail rejection), PoCP proptests (determinism, sortedness, symmetry, range, RSSI floor), FFI roundtrip incl. the 16-byte-salt regression, plus strong unit coverage of dedup caps/decay, TTL clobbering, VDL, witness/beacon/private KATs, trust caps, and a `decode` fuzz target.
Gaps worth adding tests for: **no test drives `check_epoch` with far-future/adversarial epochs (F3/F4)**, no test for `min_hearers=0` (F8), `tau=0/NaN` (F11), mismatched marks/RSSI lengths (F10), a div_sketch-copy forgery pin (F1, currently only documented), or the small-cell grind table (F2, documented numerically but not regression-locked); no fuzz target beyond `decode` (relay_decision/open_private_body are unparsed-input-adjacent and unfuzzed).

## Severity summary
| # | Finding | Severity |
|---|---------|----------|
| F1 | PoCP remote forgery via div_sketch copy (public MAC key) | HIGH |
| F2 | Small-cell (<4 marks) grind: 2–3 accepted forgeries per 256-frame sweep | HIGH |
| F3 | Dedup wipe via far-future epoch (shim-gated on Android) | MEDIUM |
| F4 | Decay head-of-line blocking by future-epoch entry | LOW |
| F5 | 1024-frame per-epoch bucket blackout (documented) | MEDIUM |
| F6 | No in-core private-frame replay protection; counter-reset nonce reuse hazard | MEDIUM |
| F7 | `open_private_body_only` caller contract load-bearing | LOW |
| F8 | `min_hearers=0` → constant entropy, `low_entropy=false` | LOW |
| F9 | Any-length beacon seed0 silently degrades forward secrecy | LOW |
| F10 | `observe()` zip-truncation shrinks local sketch silently | LOW |
| F11 | No τ validation (0.0 disables PoCP) | LOW |
| F12 | Sybil corroboration up to 32 cells from one nearby attacker | MEDIUM |
| F13 | check/act/insert TOCTOU across FFI | LOW |
| F14 | `make_test_frame` in prod FFI; generous 2²⁰ dedup cap clamp | LOW |

F1 and F2 are both explicitly documented in the `pocp.rs` module header (with the Android badge degradation as the interim mitigation) — the audit confirms the documentation is accurate, and in F1's case slightly *understates* the reach: the copy source is any frame within relay range, not merely same-cell. The durable fixes noted in-code (fuzzy-extractor keying of the witness MAC, intersection-count floor redesigned with cold-start) remain the correct direction.
</task_result>
</task>
```

**Tool: task**

**Input:**
```json
{
  "description": "Audit Android app layer",
  "subagent_type": "explore",
  "prompt": "You are performing a security audit (very thorough) of the Android layer of a BLE mesh chat app. READ-ONLY: do not modify anything.\n\nFiles in /home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/:\n- PairStore.kt (contact/pairing storage, EncryptedSharedPreferences, QR pairing flow)\n- MeshService.kt (foreground service, message handling, panic wipe)\n- GattPlane.kt (GATT server/client relay plane)\n- BleRadio.kt (BLE advertising/scanning)\n- Config.kt, MeshState.kt, MainActivity.kt, Measurement.kt, MeshApp.kt\n- ui/MeshUi.kt, ui/Drawer.kt (only for security-relevant things: exported components, intents, screenshot/backup flags)\nAlso check android/app/src/main/AndroidManifest.xml and build.gradle if present.\n\nHunt specifically for:\n1. Pairing: QR code flow — MITM during pairing, no SAS/authentication of public keys, salt handling (are salts really deleted?), storage of private keys (Keystore-backed? extractable?), backup of EncryptedSharedPreferences via adb backup / Auto Backup (android:allowBackup?), screenshots (FLAG_SECURE on chat UI?).\n2. IPC/attack surface: exported activities/services/receivers, unprotected intents, deep links, GATT server write permissions (can any nearby BLE device write characteristics? MITM on GATT plane — is GATT traffic authenticated/encrypted or plaintext relay?), BLE pairing/bonding requirements.\n3. Data handling: logs leaking message content/keys (Log.d of frames/keys), messages persisted unencrypted anywhere, panic wipe completeness (does it wipe PairStore, files, prefs, GATT cache?), clipboard leaks.\n4. Replay/injection at radio layer: does the app re-verify everything in Rust, or is there trust in Kotlin-side parsing? Can GATT plane inject frames bypassing PoW/witness checks?\n5. Privacy: device name/MAC leakage, static identity in advertisements, metadata (who talks to whom), foreground service notification content leaking messages.\n\nFor EACH finding: file:line, severity (critical/high/medium/low), concrete attack scenario, exact code snippet. Verify by reading code — no speculation. Also briefly note what's done correctly. Return a structured findings list."
}
```

**Output:**
```
<task id="ses_062df1d9fffe3IZyYXwcMsbCdC" state="completed">
<task_result>
I have now read every Android-layer file completely, plus the manifest, build.gradle.kts, and the Rust FFI contracts the Kotlin side relies on. All findings below are verified against the actual code.

---

# Security Audit: BileiChat Android Layer

**Scope:** `/home/Likun/projects/cockroachat/android/app/src/main/` (all 11 Kotlin files read in full, manifest, build.gradle.kts), with Rust FFI contracts (`mesh-core/src/ffi.rs`, `crypto.rs`, `message.rs`) checked where Kotlin trust boundaries depend on them.

---

## FINDINGS

### F1 — HIGH — Pairing has no authentication: no SAS, no fingerprint verification, silent key replacement

**Files:**
- `PairStore.kt:150-151` (QR payload = pubkey + salt + epoch, nothing authenticating it)
- `PairStore.kt:264-332` (`addContact` — no existing-key change warning)
- `ui/MeshUi.kt:1072-1185` (`PairingDialog` — shows only *my* key/salt; the peer's key is never displayed again after pairing, so there is nothing to compare out-of-band)

```kotlin
// PairStore.kt:150-151
fun qrPayload(ctx: Context): String =
    "bileichat:key:v3:${myPublicHex(ctx)}:${myPairSalt(ctx).toHex()}:${currentEpoch()}"
```

```kotlin
// PairStore.kt:322 — re-pairing under an existing label silently REPLACES the key
val updated = contacts(ctx).filter { it.label != trimmedLabel } + contact
persist(ctx, updated)
```

**Attack scenario:** Classic two-QR MITM. Eve shows Alice Eve's QR while Alice scans "Bob", and shows Bob Eve's QR while Bob scans "Alice". Both pairings succeed; both show "🔒 forward secret". There is no SAS, no post-pairing fingerprint screen, no key-continuity check — the contact list shows only the user-chosen label and "forward secret"/"legacy". Additionally, anyone who socially engineers a victim into re-scanning ("pair me again, it stopped working") under an existing contact's name silently overwrites that contact's key with no "key changed" warning. All later "E2E" traffic to that label is readable by the attacker (they hold the other end of the ECDH). The v2 ratchet does not help: the attacker is a legitimate-looking endpoint from day one.

**Mitigating factor:** the flow is nominally face-to-face; the label is user-chosen. Still, for an app whose threat model is state actors at protests, this is the core E2E authenticity gap.

---

### F2 — HIGH — Forward-secrecy salt is NOT deleted after pairing: lives in process memory for the whole run, reused across contacts, displayed as copyable text

**Files:** `PairStore.kt:69-72` (stale comment), `PairStore.kt:130-136`, `PairStore.kt:326-330`, `ui/MeshUi.kt:1122-1124`

The header comment claims rotation-on-pairing; the code explicitly does the opposite:

```kotlin
// PairStore.kt:69-72 — the (false) claim
/** Per-process pairing salt shown in our QR (v2). NEVER persisted: ...
 *  Rotated after every successful pairing so each contact gets fresh salt. */

// PairStore.kt:326-330 — the reality
// NOTE: mySalt deliberately does NOT rotate here. The salt in the displayed QR
// must equal the salt used for every pairing made while that QR is shown ...
```

```kotlin
// ui/MeshUi.kt:1122-1124 — salt rendered in a copyable SelectionContainer
SelectionContainer {
    Text("SALT $mySalt", style = monoMicro(TierPrivate), lineHeight = 14.sp)
}
```

**Attack scenario:** The v2 FS guarantee is "seized long-term secret cannot recompute the chain — salts are deleted after pairing" (`PairStore.kt:14-15`, Rust `ffi.rs:558`). That property only materializes at **process death or panic wipe**. `mySalt` is a `@Volatile` field kept alive for the entire process run and shared by *all* pairings made in that run. A memory dump of the running process (see F3 — the distributed build is debuggable; or root) recovers `mySalt` + the long-term `sk`; the peer's salt travels in cleartext in their QR. With all three, `seed0 = pairSeedV2(ECDH(sk, peerPub), mySalt, theirSalt)` is recomputed and the one-way ratchet re-derived forward — **every private message of every contact paired during that process run becomes decryptable** from recorded ciphertext. The copyable "SALT …" UI text adds a trivial clipboard-leak path (clipboard managers persist history) with no functional need — the QR already carries the salt; the user never needs to paste their own.

---

### F3 — HIGH (conditional on distribution) — Distributed builds are debuggable; no hardened release build type exists

**File:** `android/app/build.gradle.kts:20-25`

```kotlin
buildTypes {
    debug {
        isMinifyEnabled = false
        isDebuggable = true
    }
}   // no release block at all
```

The repo ships field-tested APKs named `cockroachat-debug.apk`, `bileichat-0.15-ticks-search.apk`, etc., and the defaultConfig comment ("Debug/measurement build") confirms debug builds are what people actually run. `android:debuggable="true"` lets any computer with adb attach JDWP and read process memory: long-term `sk` (after decrypt), `mySalt`, all `Contact.pairKey`/`prevKey`, and in-memory message plaintext — defeating EncryptedSharedPreferences, the ratchet, and the panic wipe (a debugger attached *before* the wipe sees everything). Also enables `run-as` access to app-private files. If these APKs are the distribution channel, this turns F2 into a practical break. Fix: a real `release` build type (debuggable=false, minify) for anything installed on at-risk devices.

---

### F4 — MEDIUM — Contact labels leak into the exportable debug log, contradicting the codebase's own stated policy

**Files:** `MeshService.kt:1118`, `PairStore.kt:432-435`

`PairStore.kt:393-396` explicitly documents the policy: *"Contact label deliberately omitted: the debug log is exportable … social-graph metadata."* Two call sites violate it:

```kotlin
// MeshService.kt:1118
MeshState.logDebug("noteOpened failed for '$label': ${e.message}")

// PairStore.kt:432-435
MeshState.logDebug(
    "chain fast-forward for '" + c.label + "' failed: span " +
        (target - c.chainEpoch) + " epochs exceeds the ratchet cap"
)
```

**Attack scenario:** The log is exportable in-app (`/export`, drawer EXPORT LOG, `MainActivity.kt:127-130`) and routinely shared for debugging (the drawer even warns it is sensitive). These two lines fire precisely in the failure modes users report (ratchet desync), so a shared export names the user's contacts. A seized/shared export hands over the social graph the design tried to keep out of it. Severity capped at medium because it requires the failure path + an export.

---

### F5 — MEDIUM — GATT server: no link-layer encryption or bonding; unauthenticated writes accepted from any nearby device; bad-signature frames are never deduplicated (CPU/battery DoS)

**Files:** `GattPlane.kt:268-286` (permissions), `MeshService.kt:919-930` (bad-sig path)

```kotlin
// GattPlane.kt:268-286 — PERMISSION_READ / PERMISSION_WRITE, not *_ENCRYPTED
val frameTx = BluetoothGattCharacteristic(CHAR_FRAME_TX, PROPERTY_READ or PROPERTY_NOTIFY,
    BluetoothGattCharacteristic.PERMISSION_READ)
...
val frameRx = BluetoothGattCharacteristic(CHAR_FRAME_RX, PROPERTY_WRITE or PROPERTY_WRITE_NO_RESPONSE,
    BluetoothGattCharacteristic.PERMISSION_WRITE)
```

```kotlin
// MeshService.kt:923-930 — sig failure returns BEFORE any dedup insert
if (!frameVerifySelf(bytes)) {
    rxBadSig.incrementAndGet()
    return        // <- never inserted into dedup
}
```

**Attack scenarios:**
1. **DoS:** any BLE device in range (no pairing/bonding prompt) connects and writes 226-byte frames to `FRAME_RX` at connection speed (hundreds/sec, far faster than advertising). Each write runs an Ed25519 verify on a BLE binder thread. Because bad-sig frames return before the dedup gate and are never inserted, re-sending the *same* invalid frame costs a full verify every time — the C8 sub-cap and dedup table offer zero protection here. Sustained: CPU pegged, battery drained, legitimate scan/GATT callbacks starved. (PoW-failing private frames *are* deduped at `MeshService.kt:1077` — good; the bad-sig gate is the hole.)
2. **MITM/eavesdrop on the GATT plane:** link traffic is unencrypted, so an active attacker can read/drop/inject. Content impact is nil (frames are self-authenticating and private bodies are AEAD — verified in Rust `ffi.rs:364-374`, `622-645`, and Kotlin routes GATT frames through the identical `ingestFrame`), but the attacker can selectively blackhole one direction of a link invisibly.

Note: requiring bonding would conflict with the protest threat model (pairing records are themselves forensic evidence), so this is partially a design trade-off — but the missing dedup-on-bad-sig is a straight bug.

---

### F6 — MEDIUM — GATT slot exhaustion mitigations (B9) are incomplete: subscribed centrals and connected peers are never evicted

**File:** `GattPlane.kt:162`, `GattPlane.kt:54`, `GattPlane.kt:66`

```kotlin
// GattPlane.kt:162 — watchdog skips any subscribed central forever
if (notifySubscribers.containsKey(addr)) continue
```

`MAX_SUBSCRIBERS = 4`, `MAX_PEERS = 3`. Four dongles that connect + subscribe hold every subscriber slot permanently (the idle watchdog explicitly exempts them). Three dongles advertising past `MIN_PEER_AGE_MS` and then simply staying connected pin all three client slots — the client side has **no idle timeout at all** once connected (the 5 s RSSI poll keeps the link busy by design). Result: legitimate legacy-advertising phones — the entire reason the GATT plane exists — are starved out at radio range for as long as the attacker stays. The B9 comment ("three cheap dongles flashing the UUID must not exhaust slots") covers only transient flashers, not persistent ones.

---

### F7 — MEDIUM — Legacy v1 downgrade path silently produces non-forward-secret pairings

**File:** `PairStore.kt:180-191`, `PairStore.kt:313-316`

`parsePairingOffer` still accepts `bileichat:key:v1:<pk>` **and a bare 64-hex public key**; both produce `Contact(v2 = false)` with the static `pair_derive` key:

```kotlin
// PairStore.kt:186-190 — raw hex accepted as a pairing code
val clean = trimmed.replace(" ", "").replace("\n", "")
if (clean.length == 64 && clean.hexToBytesOrNull()?.size == 32) {
    PairingOffer(clean, null)      // saltHex = null → static key
}
```

Combined with F1 (no authentication), an attacker presenting a v1-style or raw-hex code forces the victim into a pairing with no salt and no ratchet — one seizure of either phone (or one memory read, F3) exposes the *entire* history. The UI does badge these "legacy" in amber (`ui/MeshUi.kt:1158`), which keeps this at medium rather than high.

---

### F8 — MEDIUM-LOW — Panic wipe is broad but not complete: queued plaintext private sends and several in-memory artifacts survive

**File:** `MeshService.kt:1432-1489`

Wiped correctly: radio/GATT stopped first, Rust `panicWipe()`, `beacon.wipe()`, `PairStore.wipe()` (encrypted prefs cleared + `memSk`/`mySalt` zeroed), `ConfigStore.clear()`, legacy `mesh_pairing` + `crash_log` prefs cleared, measurement cleared, messages cleared, debug log replaced, `seed`/`currentPublicFrame` zeroed, relay queue + marks zeroed.

**Not wiped:**
```kotlin
// MeshState.kt:209 — plaintext PrivateSend(label, text) queue; performPanicWipe never drains it
val privateSends = kotlinx.coroutines.channels.Channel<PrivateSend>(capacity = 8)
```
- `MeshState.privateSends` — a private message queued behind a seconds-long VDL solve (exactly when panic is likely) survives in plaintext, with its recipient label, until process death.
- `pendingPrivate` (`MeshService.kt:144`) — sealed-frame ciphertext buffer, never zeroed.
- `ownHashes`/`ownHashesAcked` (`MeshService.kt:162-165`) — hashes linking our sent messages to epochs.
- `dedup` (4096 frame hashes) and `trust` (claim records) Rust objects held by Kotlin — frames seen, not cleared (Rust `panicWipe()` clears Rust *globals*, not these FFI instances).
- `Contact.pairKey`/`prevKey` `ByteArray`s — references dropped but contents not zeroed (acknowledged in the C7 comment as a JVM limit; note the actual key bytes stay live on the heap until GC).
- The AndroidKeyStore master key and the system Bluetooth GATT service-discovery cache are untouched (the latter is outside app control; noted for completeness).

**Attack scenario:** seizure within seconds of pressing panic (e.g., device taken while still unlocked and process alive): the queued plaintext and the heap-resident contact keys are recoverable via F3's debug bridge. The headline wipe contract ("device is sterile") is mostly true for on-disk state, not for the live heap.

---

### F9 — LOW — Private-send advertising window leaks sender identity by absence

**File:** `MeshService.kt:444-463`

```kotlin
// C3: ... While the private frame uses the primary
// advertising set our public presence frame is OFF the air
val windowMs = minOf(cfg.messageRepeatEpochs.toLong() * cfg.epochMs, 6_000L)
```

An RF observer correlating advertisements sees a device's per-epoch public frame disappear for ~6 s exactly when a `msgType=3` frame appears (and re-appear between re-airings, `reairPendingPrivate`). On single-advertising-set hardware this is a hard constraint (documented in the code), but it means **private sends are linkable to the sending device by a passive sniffer** — metadata the "relays see that a private frame passed, not what it says" story doesn't cover.

---

### F10 — LOW — Foreground-service notification leaks live mesh-participation metadata on the lock screen

**File:** `MeshService.kt:711-713`, `MeshService.kt:1506-1514`

```kotlin
val notifText = "Epoch $epoch | neighbors=$neighbors | total=$total"
notifManager.notify(NOTIFICATION_ID, buildNotification(notifText))
```

No message content (good), but a lock-screen-glanceable, continuously updating neighbor/crowd counter. On a seized locked phone it proves active mesh participation and approximate crowd size over time. `IMPORTANCE_LOW` and no badge are set correctly.

---

### F11 — LOW — Clipboard: cell sketch copied without sensitivity flag or expiry; salt copyable (see F2)

**File:** `MainActivity.kt:191-197`

```kotlin
cb.setPrimaryClip(ClipData.newPlainText("mesh_sketch", sketch.joinToString(" ") { it.toString() }))
```

The sketch is the KMV summary of who is physically near you (the app itself calls this "RF-proximity data … handle like location history" in `Measurement.kt:150-153`). It lands in the global clipboard with no `EXTRA_IS_SENSITIVE` equivalent, no auto-clear; clipboard-manager apps persist it. Low because it's user-initiated and not message content.

---

### F12 — LOW — Malicious QR can brick a contact's private messaging via a tiny anchor epoch (ratchet-span DoS)

**File:** `PairStore.kt:299-312`

```kotlin
val start = offer.anchorEpoch?.let { minOf(it, currentEpoch()) } ?: ...
```

The peer-supplied `anchorEpoch` is clamped only from above. A QR encoding `anchorEpoch = 0` yields `chainEpoch ≈ 0`; every send then calls `pairRatchet(key, 0, currentEpoch)` with a span of ~1.7×10⁸ — far over the Rust 8192-step cap (`ffi.rs:570-574`) — so `keyForSend` returns null forever ("key ratchet failed") and receives fail identically. No secrecy impact; the contact is silently dead until re-paired. Clamping `start` to `currentEpoch() - smallConstant` would fix it.

---

### F13 — LOW — Log line exposes rotating mark prefix; crash log stored in plaintext prefs

- `MeshService.kt:1169-1172`: relay-only log includes 4 bytes of the sender's mark — linkable metadata inside the exportable log (marks rotate per epoch, so the window is small).
- `MeshApp.kt:10-26`: uncaught stack traces written to plaintext `SharedPreferences("crash_log")` (cleared on panic and on next start). Content is class/method names only — no user data observed — but it is an unencrypted file with a 4 KB trace.

---

## WHAT IS DONE CORRECTLY (verified, not assumed)

- **Backup:** `android:allowBackup="false"` (manifest:42) — adb backup and Auto Backup both off; EncryptedSharedPreferences cannot leave the device that way.
- **Screenshots:** `FLAG_SECURE` set on the single activity (`MainActivity.kt:318`) covering chat, QR, and salt views.
- **Attack surface:** no `INTERNET` permission at all; `MeshService` `exported="false"`; `MainActivity` exported only for `MAIN`/`LAUNCHER` and reads **no** intent data (verified — no `intent.` reads in `onCreate`); no receivers, providers, or deep links; the library QR-scanner activity has no intent-filter (non-exported at targetSdk 35). `ACTION_PANIC` is unreachable from other apps.
- **Key storage:** long-term sk + contact chain keys live only in `EncryptedSharedPreferences` (AES-256-SIV/GCM under a TEE-backed `MasterKey`, `PairStore.kt:81-99`); fail-closed to memory-only with a UI banner (`secureStorageOk`) rather than silent plaintext; nonce counter moved into the encrypted store with per-epoch random base (`PairStore.kt:479-502`).
- **Trust boundaries:** Kotlin parses **zero** frame bytes — everything (decode, Ed25519 verify, K4 freshness, dedup, VDL PoW, PoCP witness, AEAD open) is done in Rust, and the GATT plane feeds the *identical* `ingestFrame` path (`MeshService.kt:905-1140`), so GATT-plane injection cannot bypass PoW/witness checks. The `open_private_body_only` caller contract (verify + VDL exactly once upstream) is honored. Private trial-decrypt iterates all contacts/keys without early break (timing side-channel, E3).
- **Forward secrecy mechanics:** peer salts are used transiently and never stored; the ratchet is one-way with past keys dropped (`noteOpened`, `fastForwardChains`); v1 static keys are clearly badged "legacy".
- **Radio privacy:** `setIncludeDeviceName(false)` on all advertising paths; ephemeral frame identity (mark + Ed25519 key) derived from `(seed, beacon_seed)` and rotated per epoch via the beacon chain (verified in `crypto.rs:13-27`); device seed is per service start (`MeshService.kt:302`).
- **DoS hardening already present:** dedup per-epoch sub-cap with flood warning (C8), relay queue bound with priority eviction (B8), subscriber cap, connect timeout, peer-age dwell (B9 — partial, see F6), ratchet-span cap, crash barrier around ingest so one malformed frame can't kill the process.
- **Receipts honesty:** echo receipts explicitly documented and labeled as non-proof (B1); forged-echo can't silence LOCAL alerts.
- **No content in logs:** verified — no `Log.*` call and no `logDebug` call anywhere logs message text, keys, or salts (only labels leak, F4; mark prefix, F13).
- **Messages never persisted:** the feed is an in-memory 200-row `StateFlow`; `/clear` and panic wipe drop it.

---

## TOP PRIORITIES

1. **F3** — ship a non-debuggable release build (enables F2 exploitation and heap recovery after F8's gaps).
2. **F1** — add key verification to the pairing flow (SAS/fingerprint comparison of the peer's pubkey post-pairing + "key changed" warning on re-pair under an existing label).
3. **F2** — rotate `mySalt` when the pairing dialog closes (not per-add, preserving the sequential-pairing fix), and remove the copyable "SALT …" text.
4. **F5** — insert bad-signature frame hashes into dedup before returning (kills the verify-flood amplifier).
5. **F8** — drain `privateSends` and zero `pendingPrivate`/`ownHashes` in `performPanicWipe`.
</task_result>
</task>
```

---

## User

also find communication problems, simulate scenarios in your mind. Why the receipt is late but the message isn't

Every phone computes epoch = now / epochMs from the wall clock. Your clocks are 1s apart, so both cross the boundary essentially together — and then both do the same three things in the same instant: tear down the advertising set, re-originate, and relay whatever they just heard. Two devices in lockstep talk over each other there every time.

Origination survives it. advIntervalMs = 1000 means your frame airs ~10 times spread across the epoch, so one collision costs nothing.

The echo doesn't. Dedup lets a peer relay a given frame once per epoch — every later copy that epoch is a DUPLICATE. So the receipt depends on a single ~2s relay burst, and that burst fires right in the most congested slice of the epoch. Miss it and you wait a full epoch for the next chance. Three epochs of messageRepeatEpochs gives you three coin flips, which is exactly "sometimes late, sometimes never".

That asymmetry — delivery reliable, echo flaky — is the signature. It also means the earlier "no echo heard" was never evidence the message wasn't delivered.

Changed

- TX_JITTER_MAX_MS = 1200 — random delay after the epoch boundary before re-originating. Well under one epoch, so the frame is still stamped with the epoch it was built for.
- RELAY_JITTER_MAX_MS = 350 — random delay before airing a queued relay, so two relayers don't answer in unison.

Both re-drawn each time rather than a fixed per-device offset — a fixed offset would let two devices that happen to land close together collide with each other forever. 

---

## Assistant (Plan · Kimi K3 (2x usage) · 48.0s)

_Thinking:_

The user has expanded the request. They want:
1. Security vulnerabilities (done - three audit agents reported)
2. Communication problems — simulate scenarios mentally
3. Specific question: "Why the receipt is late but the message isn't"

And then the user pasted what looks like an analysis of the epoch-boundary synchronization problem — the lockstep collision at epoch rollover, why origination survives but echo doesn't, and a fix they made (TX_JITTER_MAX_MS = 1200, RELAY_JITTER_MAX_MS = 350). This looks like the user is sharing a changelog/analysis from a previous session (perhaps from the APK 0.13-txjitter). They seem to want me to continue in that vein: find communication problems, simulate scenarios.

I'm in plan mode (read-only). I need to:
- Analyze communication/reliability problems in the protocol/implementation
- Simulate scenarios mentally
- Answer "why the receipt is late but the message isn't" — the user already pasted an answer to this (their jitter analysis). Actually, re-reading: the user pasted the question AND what appears to be the resolution of that question (the jitter fix). So maybe they want me to verify that analysis and find MORE communication problems.

Let me think about what's being asked:
- "also find communication problems, simulate scenarios in your mind. Why the receipt is late but the message isn't" — find comms/reliability bugs, and specifically explain the receipt-lateness anomaly.
- Then the pasted text explains the epoch-boundary lockstep theory and the jitter fix. This is likely from their dev session — they're showing me the fix that was already applied (APK 0.13-txjitter exists in the directory listing!). And 0.14-relay-redundancy, 0.15-ticks-search also exist.

So the user wants a comprehensive review: security vulnerabilities + communication/reliability problems, with scenario simulation. The pasted block gives context on a problem already diagnosed and fixed.

I should now:
1. Verify the jitter fix exists in the current code (check MeshService.kt for TX_JITTER / RELAY_JITTER).
2. Analyze communication paths for other problems: advertising windows, GATT plane, dedup, relay queue, echo receipts, epoch rollover, private message delivery, re-airing logic, scan duty cycling, etc.
3. Simulate scenarios.

Let me read the key Android files myself to analyze the communication logic — MeshService.kt is 1515 lines, BleRadio.kt 706, GattPlane.kt 1022. I already have agent reports covering security aspects of these files, but for communication problems I need to trace the actual flow: advertiseRelayOnce, relayOnce, echo detection, ownHashes, reairPendingPrivate, etc.

Given plan mode, I can read files. Let me spawn an explore agent to trace the communication/reliability logic, or read the key files myself. Since precision matters here (the user wants scenario simulation), I should read MeshService.kt and BleRadio.kt myself. That's ~2200 lines. Let me read them in parallel with maybe delegating GattPlane to an agent. Actually the previous agent already read GattPlane fully and reported on it — but from a security angle, not reliability.

Let me read MeshService.kt and BleRadio.kt myself, and delegate a reliability-focused pass on GattPlane.kt + relay queue logic.

Plan:
1. Read MeshService.kt (1515 lines) — the core ingest/advertise/echo/re-air logic.
2. Read BleRadio.kt (706 lines) — advertising/scanning mechanics.
3. Read Config.kt (91 lines).
4. Maybe MeshState.kt (247).
5. Delegate GattPlane reliability analysis or read it too (1022 lines).

That's a lot but doable. Let me start reading in parallel.

Good context — the jitter fix explains the echo-lateness asymmetry. Now let me verify that analysis against the actual code and hunt for more communication/reliability problems. Let me read the core radio/service logic myself.

**Tool: read**

**Input:**
```json
{
  "filePath": "/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/MeshService.kt"
}
```

**Output:**
```
<path>/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/MeshService.kt</path>
<type>file</type>
<content>
1: package org.bileichat.mesh
2: 
3: import android.app.Notification
4: import android.app.NotificationChannel
5: import android.app.NotificationManager
6: import android.content.Context
7: import android.content.Intent
8: import android.os.Build
9: import android.os.PowerManager
10: import androidx.core.app.NotificationCompat
11: import androidx.lifecycle.LifecycleService
12: import androidx.lifecycle.lifecycleScope
13: import kotlinx.coroutines.Dispatchers
14: import kotlinx.coroutines.delay
15: import kotlinx.coroutines.isActive
16: import kotlinx.coroutines.launch
17: import kotlinx.coroutines.withContext
18: import uniffi.mesh_core.BeaconFfi
19: import uniffi.mesh_core.FfiDedup
20: import uniffi.mesh_core.FfiDedupVerdict
21: import uniffi.mesh_core.FfiTrust
22: import uniffi.mesh_core.beaconEntropy
23: import uniffi.mesh_core.defaultTtlLocal
24: import uniffi.mesh_core.defaultTtlRegional
25: import uniffi.mesh_core.frameBodyText
26: import uniffi.mesh_core.frameDecodes
27: import uniffi.mesh_core.frameEpoch
28: import uniffi.mesh_core.frameHash
29: import uniffi.mesh_core.frameMark
30: import uniffi.mesh_core.frameTtl
31: import uniffi.mesh_core.frameVerifySelf
32: import uniffi.mesh_core.frameWitnessParts
33: import uniffi.mesh_core.makeMessageFrame
34: import uniffi.mesh_core.makeMessageFrameWithWitness
35: import uniffi.mesh_core.makePrivateFrame
36: import uniffi.mesh_core.openPrivateBodyOnly
37: import uniffi.mesh_core.panicWipe
38: import uniffi.mesh_core.pocpSketchToDivSketch
39: import uniffi.mesh_core.pocpVerifyWitnessLocal
40: import uniffi.mesh_core.relayFrame
41: import uniffi.mesh_core.vdlCheckFrame
42: import uniffi.mesh_core.wasPanicWiped
43: import java.security.SecureRandom
44: 
45: class MeshService : LifecycleService() {
46: 
47:     companion object {
48:         const val NOTIFICATION_ID = 1
49:         const val CHANNEL_ID = "mesh"
50:         const val CHANNEL_NAME = "Mesh BLE"
51: 
52:         /** Start the service with this action to trigger an immediate panic wipe. */
53:         const val ACTION_PANIC = "org.bileichat.mesh.ACTION_PANIC"
54: 
55:         // Legacy plaintext counter prefs file (B4/C9: counter moved into PairStore's
56:         // encrypted store with a per-epoch random base). performPanicWipe still deletes
57:         // this file explicitly to erase traces left by older installs.
58:         const val PAIR_PREFS_NAME = "mesh_pairing"
59: 
60:         /** B2: hard cap on LOCAL re-broadcast lifetime. An unheard local alert must not
61:          *  scream every epoch forever (battery + stale-danger re-airing hours later). */
62:         const val LOCAL_REBROADCAST_WINDOW_MS = 30 * 60_000L
63: 
64:         /** B1/B2: after the first reflected echo, LOCAL messages re-air sparsely (every
65:          *  Nth epoch) instead of every epoch — a single forged echo can no longer silence
66:          *  the alert, but battery use stays bounded until the hard cap. */
67:         const val LOCAL_SPARSE_EVERY_N_EPOCHS = 4L
68: 
69:         /** B8: relay queue bound; lowest-priority tasks are evicted when full. */
70:         const val RELAY_QUEUE_CAP = 64
71: 
72:         /**
73:          * Extra airings of an already-sealed private frame, one per epoch rollover.
74:          *
75:          * Bounded by the receiver's ±2-epoch freshness gate: a frame sealed at epoch E stops
76:          * being accepted after E+2, so two re-airings is the most that can still be opened.
77:          */
78:         const val PRIVATE_REAIRINGS = 2
79: 
80:         /** Re-stamp + re-solve attempts when a VDL solve overruns the epoch it was stamped in. */
81:         const val PRIVATE_SEAL_ATTEMPTS = 3
82: 
83:         /**
84:          * Max delay after an epoch boundary before re-originating, drawn fresh each epoch.
85:          *
86:          * Kept well under one epoch so the frame is still stamped with the epoch it was built
87:          * for. Re-drawn rather than a fixed per-device offset: a fixed offset would let two
88:          * devices that happen to land close together collide with each other forever.
89:          */
90:         const val TX_JITTER_MAX_MS = 1_200L
91: 
92:         /** Max delay before airing a queued relay, so two relayers don't answer in unison. */
93:         const val RELAY_JITTER_MAX_MS = 350L
94: 
95:         /** Airings of one relayed frame when we are the only possible relayer. */
96:         const val RELAY_REPEATS_MAX = 3
97: 
98:         /**
99:          * Gap between repeat airings of the same relayed frame.
100:          *
101:          * Wide enough that a repeat lands in genuinely different radio conditions rather than
102:          * inside the same interference burst, and 3 airings still fit inside one 10 s epoch.
103:          */
104:         const val RELAY_REPEAT_SPACING_MS = 2_600L
105: 
106:         /** verifyPocpAcrossRollover: no local sketch exists for any candidate bucket, so the
107:          *  frame could not be judged at all. Distinct from Stale (a real MAC failure) because
108:          *  it is transient — the same frame may verify moments later. */
109:         const val POCP_NO_LOCAL_SKETCH = 3
110: 
111:         /**
112:          * Trigger a panic wipe from anywhere. Sets the Rust flag (the running service's
113:          * epoch-loop poller picks it up within 1 s) AND starts the service with
114:          * ACTION_PANIC so the wipe also runs when the service is not currently running.
115:          */
116:         fun requestPanicWipe(ctx: Context) {
117:             panicWipe()
118:             val intent = Intent(ctx, MeshService::class.java).setAction(ACTION_PANIC)
119:             try {
120:                 ctx.startService(intent)
121:             } catch (_: Exception) {
122:                 // Background-start restrictions: the Rust flag is set; any future
123:                 // service start checks it below before doing anything else.
124:             }
125:         }
126:     }
127: 
128:     private lateinit var seed: ByteArray
129:     private lateinit var beacon: BeaconFfi
130:     private lateinit var radio: BleRadio
131:     private lateinit var gattPlane: GattPlane
132: 
133:     /** The normal public frame restored after a temporary private transmission. */
134:     private var currentPublicFrame: ByteArray? = null
135:     private var privateTransportActive = false
136: 
137:     /** A sealed private frame awaiting its remaining re-airings (see [PRIVATE_REAIRINGS]). */
138:     private class PendingPrivate(
139:         val frame: ByteArray,
140:         val sealedEpoch: UInt,
141:         var airingsLeft: Int
142:     )
143:     private val pendingPrivateLock = Any()
144:     private var pendingPrivate: PendingPrivate? = null
145: 
146:     /**
147:      * P4: hashes of our RECENTLY-originated public frames — used to hear our own reflection
148:      * coming back through the mesh (send-and-listen), keyed by hex hash.
149:      *
150:      * This was a single slot, overwritten by every rebuildAndAdvertise (epoch rollover, tier
151:      * switch, outgoing-text change, advertising self-heal). Relays are queued behind a 250 ms
152:      * poll and aired for 2 s, so an echo that crossed an epoch boundary was compared against
153:      * a DIFFERENT hash and the receipt never fired — the sender kept re-transmitting until
154:      * "no echo heard", even though the receiver had displayed the message. Keeping a few
155:      * epochs of hashes closes that window.
156:      *
157:      * Entries older than [ownHashRetentionEpochs] are evicted.
158:      * Guarded by [ownHashesLock]: written on the service coroutine, read on BLE binder threads.
159:      */
160:     private data class OwnFrame(val epoch: UInt, val carriedText: Boolean)
161:     private val ownHashesLock = Any()
162:     private val ownHashes = LinkedHashMap<String, OwnFrame>()
163: 
164:     /** Hashes we have already reacted to, so the receipt still fires once per origination. */
165:     private val ownHashesAcked = HashSet<String>()
166: 
167:     /** Set when a relayed echo of our frame is heard (receipt). B1: an echo proves only
168:      *  that ONE (possibly adversarial) peer relayed us once — LOCAL no longer hard-stops
169:      *  on it; it switches to sparse re-airing until [LOCAL_REBROADCAST_WINDOW_MS] passes.
170:      *  Reset when new outgoing text is composed. */
171:     @Volatile
172:     private var reflectionHeard = false
173: 
174:     /** Epoch at which the first echo was heard (LOCAL sparse re-air anchor). */
175:     @Volatile
176:     private var echoEpoch: UInt? = null
177: 
178:     /** LocalImmediate marks heard this epoch (for beacon entropy collection).
179:      *  Guarded by [marksLock]: ingest runs on BLE binder threads, the epoch loop on main. */
180:     private val marksLock = Any()
181:     private val localImmediateMarks = mutableListOf<ByteArray>()
182: 
183:     // Per-service-start dedup table: catches the same frame arriving many times per epoch
184:     // via extended advertising or GATT (normal behaviour — not a protocol error).
185:     private val dedup = FfiDedup(4096u)
186: 
187:     // H2: per-service-start trust accumulator for multi-locale diversity
188:     private val trust = FfiTrust()
189: 
190:     // Rate-limit epoch-skew log: only log when the (frameEpoch, ownEpoch) pair changes.
191:     private var lastSkewPair: Pair<UInt, UInt>? = null
192: 
193:     // Track whether the first frame of the current epoch has been logged.
194:     private var firstFrameEpoch: UInt? = null
195: 
196:     // Rate-limit the dedup bucket-full warning to once per epoch.
197:     @Volatile
198:     private var lastBucketFullEpoch: UInt? = null
199: 
200:     /**
201:      * Per-epoch receive counters, reported at every rollover.
202:      *
203:      * Every early gate in ingestFrameInner is a bare `return`: a frame that fails to decode,
204:      * fails signature verification, or is a duplicate leaves NO trace. That made "our radio
205:      * is deaf" and "we are hearing plenty and rejecting all of it" produce byte-identical
206:      * logs — a phone sitting at neighbors=0 could not be told apart from a phone whose
207:      * scanner never started. One counted line per epoch distinguishes them.
208:      */
209:     private val rxTotal = java.util.concurrent.atomic.AtomicInteger(0)
210:     private val rxUndecodable = java.util.concurrent.atomic.AtomicInteger(0)
211:     private val rxBadSig = java.util.concurrent.atomic.AtomicInteger(0)
212:     private val rxSkewed = java.util.concurrent.atomic.AtomicInteger(0)
213:     private val rxDuplicate = java.util.concurrent.atomic.AtomicInteger(0)
214:     private val rxShown = java.util.concurrent.atomic.AtomicInteger(0)
215: 
216:     // B8: prioritized relay queue. Priority: LOCAL echo (0) > regional (1) > private (2);
217:     // FIFO within a class. Drained by a service coroutine whenever the radio has a free
218:     // hardware slot — frames WAIT instead of being silently dropped (B8 starvation fix).
219:     private data class RelayTask(
220:         val frame: ByteArray,
221:         val priority: Int,
222:         val seq: Long,
223:         /** Airings still owed for this frame (see [relayRepeatsForDensity]). */
224:         val repeats: Int = 1
225:     )
226:     private val relayQueueLock = Any()
227:     private val relayQueue = ArrayDeque<RelayTask>()
228:     private var relaySeq = 0L
229: 
230:     // Repeated-text suppression (display-only). B6: keyed by (text, sender-mark prefix) —
231:     // an attacker pre-broadcasting the same words can no longer suppress the REAL alert
232:     // from a different sender. Guarded by itself: ingestFrame runs concurrently.
233:     private val recentTexts = HashMap<String, UInt>()
234: 
235:     // K9: guard against duplicate onStartCommand initialization (MainActivity + ChatActivity
236:     // can both startForegroundService on the live instance).
237:     private var started = false
238: 
239:     // K3: panic wipe is one-shot; subsequent triggers are no-ops.
240:     private var wiped = false
241: 
242:     // R1 mitigation: div_sketch reuse across distinct sender marks within one epoch is a
243:     // copy-attack signal (see pocp.rs "RESIDUAL GAP"). Soft response: log only — tiny cells
244:     // can legitimately produce identical sketches. Keyed by div_sketch hex → first mark hex.
245:     private val sketchSeenLock = Any()
246:     private val sketchSeen = HashMap<String, String>()
247:     private var sketchSeenEpoch: UInt = 0u
248: 
249:     private lateinit var wakeLock: PowerManager.WakeLock
250: 
251:     override fun onCreate() {
252:         super.onCreate()
253:         createNotificationChannel()
254: 
255:         // Prepare the wake lock but do NOT acquire it yet — if the start aborts before
256:         // startForeground succeeds, an acquired lock would outlive the failed service.
257:         // Acquired in onStartCommand once the service is genuinely in the foreground.
258:         val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
259:         wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "bileichat:mesh")
260:     }
261: 
262:     override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
263:         super.onStartCommand(intent, flags, startId)
264: 
265:         // K3: explicit panic request — wipe immediately, whether or not we were running.
266:         // Also honors a wipe requested while the service was stopped (Rust flag survived).
267:         if (intent?.action == ACTION_PANIC || wasPanicWiped()) {
268:             startForeground(NOTIFICATION_ID, buildNotification("Wiping…"))
269:             performPanicWipe()
270:             return START_NOT_STICKY
271:         }
272: 
273:         // K9: duplicate starts (MainActivity btnStart + ChatActivity swRun both call
274:         // startForegroundService on the live instance) must not re-init identity,
275:         // collectors, or the epoch loop.
276:         if (started) return START_STICKY
277: 
278:         // P1: startForeground throws SecurityException on API 34+ when the connectedDevice
279:         // FGS type is claimed without a Bluetooth permission. Uncaught, that killed the
280:         // process the moment the service started — indistinguishable from "the app quit".
281:         try {
282:             startForeground(NOTIFICATION_ID, buildNotification("Starting…"))
283:         } catch (e: Exception) {
284:             MeshState.logDebug("startForeground failed: ${e.message}")
285:             MeshState.running.value = false
286:             stopSelf()
287:             return START_NOT_STICKY
288:         }
289:         started = true
290: 
291:         // Now in the foreground: keep the CPU awake so Doze cannot suspend BLE scans and
292:         // advertisements. Failure here must not abort the mesh.
293:         try {
294:             if (!wakeLock.isHeld) wakeLock.acquire()
295:         } catch (e: Exception) {
296:             MeshState.logDebug("wake lock acquire failed: ${e.message}")
297:         }
298: 
299:         MeshState.running.value = true
300:         MeshState.logDebug("service started")
301: 
302:         seed = ByteArray(32).also { SecureRandom().nextBytes(it) }
303:         val beaconSeed0 = ByteArray(32).also { SecureRandom().nextBytes(it) }
304:         beacon = BeaconFfi(beaconSeed0)
305:         radio = BleRadio(this)
306: 
307:         // Wire debug callback before any radio operations
308:         radio.onDebug = { MeshState.logDebug(it) }
309: 
310:         // Log radio capability once at start
311:         MeshState.logDebug(radio.capabilityReport())
312:         if (!radio.extendedAdvCapable()) {
313:             // P6: this phone cannot put a 226-byte frame in an advertisement. It stays a full
314:             // mesh member, but every frame it sends OR relays travels over the GATT plane,
315:             // which needs a connection (bounded by GattPlane.MAX_PEERS) rather than a
316:             // broadcast. Say so plainly — this was previously invisible.
317:             MeshState.logDebug(
318:                 "NOTE: no extended advertising on this adapter — frames and relays travel " +
319:                     "over the GATT plane only (connection-based, fewer simultaneous peers)"
320:             )
321:         }
322: 
323:         // Set up GATT fallback plane
324:         gattPlane = GattPlane(
325:             ctx = this,
326:             onFrame = { bytes, rssi -> ingestFrame(bytes, rssi) },
327:             onDebug = { MeshState.logDebug(it) }
328:         )
329:         gattPlane.start()
330: 
331:         // Start scanning — frames go to ingest; mesh peers without frame data (no extended
332:         // advertising support) are handed to the GATT fallback plane for connection.
333:         val cfg = MeshState.config
334:         radio.startScanning(
335:             cfg.scanLowLatency,
336:             onLegacyPeer = { device, rssi -> gattPlane.onPeerSeen(device, rssi) }
337:         ) { bytes, rssi ->
338:             ingestFrame(bytes, rssi)
339:         }
340: 
341:         // Collect outgoing text changes: rebuild frame and re-advertise immediately on change
342:         lifecycleScope.launch {
343:             MeshState.outgoingText.collect { text ->
344:                 val cfg2 = MeshState.config
345:                 val nowMs = System.currentTimeMillis()
346:                 val epoch = (nowMs / cfg2.epochMs).toUInt()
347:                 if (text.isNotEmpty()) {
348:                     MeshState.outgoingSetAtEpoch = epoch
349:                     reflectionHeard = false // new message → wait for a fresh receipt
350:                     echoEpoch = null
351:                 } else {
352:                     MeshState.outgoingSetAtEpoch = null
353:                 }
354:                 // Drives the "still sending" indicator. LOCAL keeps re-originating for up to
355:                 // 30 minutes until it is heard back (B2), which is right for a danger alert
356:                 // and alarming for "hi" — so the user has to be able to see it and stop it.
357:                 MeshState.outgoingAiring.value = text.isNotEmpty()
358:                 rebuildAndAdvertise(epoch, cfg2, text)
359:             }
360:         }
361: 
362:         // Tier switch (Local/Broadcast) takes effect immediately, not at the next epoch:
363:         // rebuild the current frame with the new TTL as soon as the tier changes.
364:         lifecycleScope.launch {
365:             MeshState.outgoingTier.collect {
366:                 val cfg2 = MeshState.config
367:                 val epoch = (System.currentTimeMillis() / cfg2.epochMs).toUInt()
368:                 // Only the empty (presence) frame follows a tier change. outgoingText holds
369:                 // the text of a message ALREADY SENT — it stays set for messageRepeatEpochs
370:                 // so the re-airings can run. Rebuilding it here re-originated that message at
371:                 // the new tier: send on BROADCAST, tap LOCAL to read within ~30 s, and your
372:                 // broadcast went back out as a LOCAL message. The composer keeps its own draft
373:                 // state and clears on send, so nothing being typed is lost by skipping this.
374:                 if (MeshState.outgoingText.value.isNotEmpty()) return@collect
375:                 rebuildAndAdvertise(epoch, cfg2, "")
376:             }
377:         }
378: 
379:         // Private (Tier-3) send queue (C4). Solving the VDL witness blocks for seconds, so it
380:         // runs on the default dispatcher; queued sends are processed sequentially. The sealed
381:         // frame is advertised for a short window; relays with a valid witness carry it
382:         // regionally (no per-epoch re-solve).
383:         //
384:         // A3: the pair key is resolved AND epoch-ratcheted here (v2 contacts) — key material
385:         // never rides the queue. B4/C9: the nonce counter uses a per-epoch random base from
386:         // the encrypted store (cross-epoch unlinkability; no plaintext send-volume leak).
387:         lifecycleScope.launch {
388:             for (ps in MeshState.privateSends) {
389:                 val cfg2 = MeshState.config
390:                 // The frame's epoch is stamped BEFORE the VDL solve, and the solve is a
391:                 // randomized search (~2^22 hashes, exponentially distributed — the mean is a
392:                 // few seconds on a phone but the tail is long). On a slow phone or an unlucky
393:                 // search the finished frame is already several epochs old when it reaches the
394:                 // air, and the receiver's ±2 K4 gate drops it before it ever reaches the
395:                 // private branch. Silent on both ends, and it hits ONLY private, because
396:                 // private is the only tier that pays for a VDL witness.
397:                 //
398:                 // So: measure the solve, re-check the clock, and re-stamp rather than putting
399:                 // a frame on air that is already guaranteed to be rejected.
400:                 var frame: ByteArray? = null
401:                 var sealFailure: String? = null
402:                 for (attempt in 1..PRIVATE_SEAL_ATTEMPTS) {
403:                     val sealEpoch = (System.currentTimeMillis() / cfg2.epochMs).toUInt()
404:                     val pairKey = PairStore.keyForSend(this@MeshService, ps.label, sealEpoch)
405:                     if (pairKey == null) {
406:                         // No label in the log: it is exportable and would name your contacts.
407:                         sealFailure = "private send dropped: contact unknown or key ratchet failed"
408:                         break
409:                     }
410:                     val counter = PairStore.nextPrivateCounter(this@MeshService, sealEpoch)
411:                     MeshState.logDebug("sealing private message (VDL solve, ~seconds of CPU)…")
412:                     val startedAt = System.currentTimeMillis()
413:                     val candidate = withContext(Dispatchers.Default) {
414:                         val beaconSeed = beacon.seed()
415:                         makePrivateFrame(seed, sealEpoch, beaconSeed, pairKey, ps.text, counter.toULong())
416:                     }
417:                     val solveMs = System.currentTimeMillis() - startedAt
418:                     val drift = (System.currentTimeMillis() / cfg2.epochMs).toLong() - sealEpoch.toLong()
419:                     if (candidate == null) {
420:                         sealFailure = "private seal failed (text > 47 bytes or bad key)"
421:                         break
422:                     }
423:                     // Budget one epoch normally: the frame still has to reach the air, cross a
424:                     // relay hop, and leave the re-airings room inside the gate. On the last
425:                     // attempt take anything the gate would still accept rather than nothing.
426:                     val budget = if (attempt == PRIVATE_SEAL_ATTEMPTS) 2L else 1L
427:                     if (drift <= budget) {
428:                         MeshState.logDebug("VDL solve ${solveMs}ms, epoch drift $drift")
429:                         frame = candidate
430:                         break
431:                     }
432:                     MeshState.logDebug(
433:                         "VDL solve ${solveMs}ms — frame stamped $drift epochs stale, outside the " +
434:                             "receiver's ±2 gate; re-sealing (attempt $attempt/$PRIVATE_SEAL_ATTEMPTS)"
435:                     )
436:                     sealFailure = "private send dropped: this phone cannot solve the VDL witness " +
437:                         "within 2 epochs (last solve ${solveMs}ms vs ${cfg2.epochMs}ms epoch)"
438:                 }
439:                 if (frame != null) {
440:                     // C3: cap the window at 6 s. While the private frame uses the primary
441:                     // advertising set our public presence frame is OFF the air — a 30 s
442:                     // window made us vanish from neighbors' sketches and cascaded
443:                     // CellMismatch drops across the cell.
444:                     val windowMs = minOf(cfg2.messageRepeatEpochs.toLong() * cfg2.epochMs, 6_000L)
445:                     // A phone usually supports one advertising set.  Reuse the primary set for
446:                     // this window (rather than opening a second one), and push the same frame to
447:                     // GATT peers so legacy-advertising devices receive private messages too.
448:                     val restoreFrame = currentPublicFrame
449:                     if (restoreFrame == null) {
450:                         MeshState.logDebug("private send delayed: public advertising is not ready")
451:                     } else {
452:                         privateTransportActive = true
453:                         val started = radio.advertisePrivateOnce(
454:                             frame = frame,
455:                             durationMs = windowMs,
456:                             restoreFrame = restoreFrame,
457:                             codedPhy = cfg2.codedPhy,
458:                             advIntervalMs = cfg2.advIntervalMs
459:                         ) {
460:                             privateTransportActive = false
461:                             gattPlane.currentFrame = currentPublicFrame ?: restoreFrame
462:                             MeshState.logDebug("private advertising window ended; public frame restored")
463:                         }
464:                         if (started) {
465:                             gattPlane.currentFrame = frame
466:                             // Private gets one tick only: private frames are not registered
467:                             // with rememberOwnFrame, so no relayed echo is ever recognised for
468:                             // them and a second tick could never honestly appear.
469:                             MeshState.privateRowTs?.let { MeshState.markOutgoing(it, SendState.ON_AIR) }
470:                         } else {
471:                             privateTransportActive = false
472:                             MeshState.logDebug("private send skipped: another private window is active")
473:                         }
474:                     }
475:                     // Insert our own frame hash so the relayed echo doesn't come back as incoming.
476:                     val ownEpoch = (System.currentTimeMillis() / cfg2.epochMs).toUInt()
477:                     frameHash(frame)?.let { dedup.checkAndInsertEpoch(it, ownEpoch) }
478:                     // Queue re-airings. A private message got ONE 6 s window and was never
479:                     // repeated, while LOCAL/BROADCAST re-originate every epoch for
480:                     // messageRepeatEpochs — so a receiver that missed that single window (a
481:                     // scan gap, a relay burst, a busy epoch) lost the message permanently and
482:                     // silently, and the sender had no receipt to notice. The sealed bytes are
483:                     // reused verbatim: no second VDL solve, and the frame's own epoch stays
484:                     // valid inside the receiver's ±2-epoch freshness gate.
485:                     synchronized(pendingPrivateLock) {
486:                         pendingPrivate = PendingPrivate(frame, ownEpoch, airingsLeft = PRIVATE_REAIRINGS)
487:                     }
488:                     MeshState.logDebug(
489:                         "private message sealed + advertised (${windowMs}ms window, " +
490:                             "$PRIVATE_REAIRINGS re-airings queued)"
491:                     )
492:                 } else {
493:                     MeshState.logDebug(sealFailure ?: "private seal failed")
494:                 }
495:             }
496:         }
497: 
498:         // B7/B8: relay queue drain. Relay tasks wait for a free hardware advertising slot
499:         // (previously frames were silently dropped when 2 relay sets were active — in a
500:         // busy crowd that starved propagation of real alerts). Every relayed frame also
501:         // goes to the GATT plane so legacy phones receive multi-hop traffic too (B7).
502:         lifecycleScope.launch {
503:             while (isActive) {
504:                 val task = synchronized(relayQueueLock) {
505:                     if (radio.relayCapacityAvailable()) relayQueue.removeFirstOrNull() else null
506:                 }
507:                 if (task != null) {
508:                     // Same lockstep problem as origination: both phones hear the other's new
509:                     // frame at the same instant and would answer with a relay burst at the
510:                     // same instant. Decorrelate them.
511:                     delay(kotlin.random.Random.nextLong(RELAY_JITTER_MAX_MS))
512:                     // P6: on adapters without extended advertising the over-the-air relay is
513:                     // impossible; the GATT plane is then the ONLY multi-hop path, so it runs
514:                     // unconditionally. Pace the loop so a legacy phone doesn't spin the queue.
515:                     val onAir = radio.advertiseRelayOnce(task.frame, 2000L, MeshState.config.codedPhy)
516:                     gattPlane.relayOnce(task.frame)
517:                     // Owe another airing? Re-queue it later in the epoch rather than looping
518:                     // here, so higher-priority relays (a LOCAL danger echo) still overtake it.
519:                     // The frame is unchanged, so it keeps its original epoch and stays inside
520:                     // the receiver's ±2 freshness gate; peers that already saw it drop the
521:                     // copy at the cheap dedup gate, so this costs airtime and nothing else.
522:                     if (task.repeats > 1) {
523:                         launch {
524:                             delay(RELAY_REPEAT_SPACING_MS + kotlin.random.Random.nextLong(RELAY_JITTER_MAX_MS))
525:                             synchronized(relayQueueLock) {
526:                                 if (relayQueue.size < RELAY_QUEUE_CAP) {
527:                                     relayQueue.addLast(
528:                                         task.copy(seq = relaySeq++, repeats = task.repeats - 1)
529:                                     )
530:                                 }
531:                             }
532:                         }
533:                     }
534:                     if (!onAir) delay(250L)
535:                 } else {
536:                     delay(250L)
537:                 }
538:             }
539:         }
540: 
541:         // Epoch loop
542:         lifecycleScope.launch {
543:             var lastEpoch = UInt.MAX_VALUE
544:             // Grace period before the advertising watchdog may fire: the very first
545:             // startAdvertisingSet is async, and firing inside that window double-started
546:             // the hardware set (frozen-frame bug).
547:             var lastAdvRestartMs = System.currentTimeMillis()
548:             while (isActive) {
549:                 val cfg = MeshState.config
550:                 val nowMs = System.currentTimeMillis()
551:                 val epoch = (nowMs / cfg.epochMs).toUInt()
552: 
553:                 if (epoch != lastEpoch) {
554:                     lastEpoch = epoch
555: 
556:                     // M5b: advance beacon chain with LocalImmediate entropy.
557:                     // If too few hearers, fallback to zero-entropy chaining.
558:                     val nowMs = System.currentTimeMillis()
559:                     val marksFlat = synchronized(marksLock) {
560:                         val flat = localImmediateMarks.flatMap { it.toList() }.toByteArray()
561:                         localImmediateMarks.clear()
562:                         flat
563:                     }
564:                     val ent = beaconEntropy(marksFlat, cfg.minHearers.toUInt())
565:                     val advanced = if (ent != null) {
566:                         beacon.advance(ent, nowMs.toULong(), cfg.beaconFloorMs.toULong())
567:                     } else {
568:                         beacon.advanceFallback(nowMs.toULong(), cfg.beaconFloorMs.toULong())
569:                     }
570:                     if (advanced) {
571:                         MeshState.logDebug(
572:                             "beacon advanced: epoch=${beacon.epoch()} low_entropy=${beacon.isLowEntropy()}"
573:                         )
574:                     }
575: 
576:                     // Outgoing auto-expire
577:                     val text = MeshState.outgoingText.value
578:                     if (text.isNotEmpty()) {
579:                         if (MeshState.outgoingTier.value == SendTier.LOCAL) {
580:                             // B2: LOCAL re-originates every epoch until heard back, but a
581:                             // hard 30-minute cap ends it regardless — alone, a phone would
582:                             // otherwise scream a stale danger alert every epoch forever.
583:                             val setAt = MeshState.outgoingSetAtEpoch
584:                             val maxAge = (LOCAL_REBROADCAST_WINDOW_MS / cfg.epochMs).toUInt().coerceAtLeast(1u)
585:                             if (setAt != null && epoch >= setAt && epoch - setAt >= maxAge) {
586:                                 MeshState.outgoingText.value = ""
587:                                 MeshState.receipt.value =
588:                                     "local broadcast stopped after 30 min — re-send if still relevant"
589:                                 MeshState.logDebug("local message expired (30 min re-broadcast cap)")
590:                             } else if (reflectionHeard && echoEpoch == null) {
591:                                 // B1: the echo switches us to sparse re-airing; it does NOT
592:                                 // stop the message (a single forged echo must not silence it).
593:                                 echoEpoch = epoch
594:                                 MeshState.logDebug("local echo heard — switching to sparse re-airing")
595:                             }
596:                         } else {
597:                             val setAt = MeshState.outgoingSetAtEpoch
598:                             if (setAt != null && epoch >= setAt && epoch - setAt >= cfg.messageRepeatEpochs.toUInt()) {
599:                                 MeshState.outgoingText.value = ""
600:                                 if (MeshState.receipt.value == null) {
601:                                     // State the measurement, not an outcome. All we know is
602:                                     // that no peer's relay of our frame came back — which is
603:                                     // ordinary when there is only one peer to do the relaying,
604:                                     // and was field-observed reading as "it never arrived"
605:                                     // while the other phone had the message on screen.
606:                                     val alone = MeshState.stats.value.neighborsThisEpoch <= 1
607:                                     MeshState.receipt.value = if (alone) {
608:                                         "sent — no relay echo came back, which is normal with " +
609:                                             "only one phone nearby. It does not mean the message was missed."
610:                                     } else {
611:                                         "sent — no relay echo came back after " +
612:                                             "${cfg.messageRepeatEpochs} epochs. Delivery is unconfirmed, not ruled out."
613:                                     }
614:                                 }
615:                                 MeshState.logDebug("outgoing message expired after ${cfg.messageRepeatEpochs} epochs")
616:                             }
617:                         }
618:                     }
619: 
620:                     // Build and advertise a new frame for this epoch. Always a full
621:                     // stop+start of the advertising set: setAdvertisingData() is silently
622:                     // broken on real stacks (field-observed: off-air on Samsung, stale-on-
623:                     // air on MIUI, success returned both times), while stop+start frames
624:                     // were demonstrably heard by the peer.
625:                     // B1/B2: after the first echo, LOCAL airs the text only every
626:                     // LOCAL_SPARSE_EVERY_N_EPOCHS-th epoch (presence frame still rotates).
627:                     val rawText = MeshState.outgoingText.value
628:                     val sparseEcho = echoEpoch
629:                     val currentText = if (
630:                         rawText.isNotEmpty() &&
631:                         MeshState.outgoingTier.value == SendTier.LOCAL &&
632:                         sparseEcho != null &&
633:                         epoch >= sparseEcho &&
634:                         (epoch - sparseEcho).toLong() % LOCAL_SPARSE_EVERY_N_EPOCHS != 0L
635:                     ) "" else rawText
636:                     // Transmit jitter. Every phone derives its epoch from the wall clock, so
637:                     // two phones in sync cross the boundary within milliseconds of each other
638:                     // — and then both tear down their advertising set, re-originate, and relay
639:                     // whatever they just heard, all inside the same slice of the epoch. In
640:                     // lockstep they talk over each other there every single time.
641:                     //
642:                     // A missed relay is expensive: dedup lets a peer relay a given frame only
643:                     // ONCE per epoch (every later copy that epoch is a DUPLICATE), so one
644:                     // collision costs a full epoch of waiting for the echo — which is exactly
645:                     // the "receipt eventually arrived, but late" symptom.
646:                     launch {
647:                         delay(kotlin.random.Random.nextLong(TX_JITTER_MAX_MS))
648:                         rebuildAndAdvertise(epoch, cfg, currentText)
649:                     }
650: 
651:                     // Step v2 pair chains with the clock so a long idle period can never
652:                     // exceed the ratchet span cap and brick private messaging (see
653:                     // PairStore.fastForwardChains). One BLAKE3 per contact per epoch.
654:                     // On IO: this touches EncryptedSharedPreferences.
655:                     launch(Dispatchers.IO) {
656:                         try {
657:                             PairStore.fastForwardChains(this@MeshService, epoch)
658:                         } catch (e: Exception) {
659:                             MeshState.logDebug("chain fast-forward failed: ${e.message}")
660:                         }
661:                     }
662: 
663:                     // Log epoch rollover with neighbor/total counts
664:                     val neighbors = MeshState.measurement.neighborsDirect(epoch)
665:                     val total = MeshState.measurement.totalHeard()
666:                     MeshState.logDebug(
667:                         "epoch rollover: epoch=$epoch neighbors=$neighbors total=$total"
668:                     )
669: 
670:                     reairPendingPrivate(epoch, cfg)
671: 
672:                     // Receive accounting for the epoch just ended. "rx=0" means the radio
673:                     // delivered nothing at all — a scan/permission/hardware problem, NOT a
674:                     // protocol rejection. Any non-zero rx with nothing shown means frames are
675:                     // arriving and being rejected, and the breakdown says at which gate.
676:                     val rx = rxTotal.getAndSet(0)
677:                     val bad = rxBadSig.getAndSet(0)
678:                     val undec = rxUndecodable.getAndSet(0)
679:                     val skew = rxSkewed.getAndSet(0)
680:                     val dup = rxDuplicate.getAndSet(0)
681:                     val shown = rxShown.getAndSet(0)
682:                     MeshState.logDebug(
683:                         "rx: $rx frames (undecodable=$undec bad-sig=$bad skewed=$skew " +
684:                             "dup=$dup shown=$shown)" +
685:                             if (rx == 0) " — RADIO DELIVERED NOTHING: check scanning/permissions" else ""
686:                     )
687:                 }
688: 
689:                 // Recompute stats and push to state
690:                 val sketch = MeshState.measurement.localSketch(epoch, seed, cfg.rssiFloorDbm)
691:                 // Presence: direct-RF devices counted per epoch bucket. Marks rotate every
692:                 // epoch, so a 15–20 s wall-clock window counted one phone 2–3 times; max
693:                 // over adjacent per-epoch buckets can't double-count (one device = one
694:                 // mark per epoch) and tolerates one fully-missed epoch.
695:                 val neighbors = MeshState.measurement.neighborsDirect(epoch)
696:                 val total = MeshState.measurement.totalHeard()
697: 
698:                 val stats = Stats(
699:                     epoch = epoch,
700:                     neighborsThisEpoch = neighbors,
701:                     totalHeard = total,
702:                     localSketch = sketch,
703:                     advertising = true,
704:                     scanning = true,
705:                     codedPhyActive = cfg.codedPhy && radio.codedPhySupported(),
706:                     note = radio.advMode
707:                 )
708:                 MeshState.stats.value = stats
709: 
710:                 // Update notification
711:                 val notifText = "Epoch $epoch | neighbors=$neighbors | total=$total"
712:                 val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
713:                 notifManager.notify(NOTIFICATION_ID, buildNotification(notifText))
714: 
715:                 // B1: check panic-wipe flag (set by Rust core or by panic-wipe button).
716:                 if (wasPanicWiped()) {
717:                     performPanicWipe()
718:                     return@launch  // exit the epoch loop
719:                 }
720: 
721:                 // Advertising self-heal: the controller can reclaim the hardware set (relay
722:                 // bursts, GATT connections, stack hiccups). The per-epoch rebuild would take
723:                 // up to a full epoch to notice — re-advertise within ~1 s instead.
724:                 // Rate-limited: a dead/off BT stack must not spam restart attempts.
725:                 if (!privateTransportActive && !radio.advertisingActive() &&
726:                     nowMs - lastAdvRestartMs >= 5_000L
727:                 ) {
728:                     lastAdvRestartMs = nowMs
729:                     MeshState.logDebug("advertising set inactive — re-advertising current frame")
730:                     rebuildAndAdvertise(epoch, cfg, MeshState.outgoingText.value)
731:                 }
732: 
733:                 delay(1_000L)
734:             }
735:         }
736: 
737:         return START_STICKY
738:     }
739: 
740:     override fun onDestroy() {
741:         // P1: these are lateinit and are only assigned partway through onStartCommand. If the
742:         // start aborted before that (permission failure, radio init throw), touching them here
743:         // threw UninitializedPropertyAccessException and MASKED the original crash.
744:         if (::radio.isInitialized) {
745:             try { radio.stop() } catch (e: Exception) { MeshState.logDebug("radio.stop failed: ${e.message}") }
746:         }
747:         if (::gattPlane.isInitialized) {
748:             try { gattPlane.stop() } catch (e: Exception) { MeshState.logDebug("gattPlane.stop failed: ${e.message}") }
749:         }
750:         if (::wakeLock.isInitialized && wakeLock.isHeld) wakeLock.release()
751:         MeshState.running.value = false
752:         MeshState.stats.value = MeshState.stats.value.copy(
753:             advertising = false,
754:             scanning = false
755:         )
756:         MeshState.logDebug("service stopped")
757:         super.onDestroy()
758:     }
759: 
760:     /** P4: how many epochs of our own frame hashes stay eligible for a reflection receipt.
761:      *  Must outlive the relay path (250 ms queue poll + 2 s air time) across a rollover. */
762:     private fun ownHashRetentionEpochs(cfg: MeshConfig): UInt =
763:         (cfg.messageRepeatEpochs.toUInt() + 1u).coerceAtLeast(2u)
764: 
765:     /** P4: remember a frame we just originated so its relayed echo is recognisable.
766:      *  [carriedText] separates a real message from the empty presence frames LOCAL airs
767:      *  between sparse re-broadcasts — echoing a presence frame is not a delivery receipt. */
768:     private fun rememberOwnFrame(hash: ByteArray, epoch: UInt, cfg: MeshConfig, carriedText: Boolean) {
769:         val hex = hash.joinToString("") { "%02x".format(it) }
770:         val retain = ownHashRetentionEpochs(cfg)
771:         synchronized(ownHashesLock) {
772:             ownHashes[hex] = OwnFrame(epoch, carriedText)
773:             val iter = ownHashes.entries.iterator()
774:             while (iter.hasNext()) {
775:                 val e = iter.next()
776:                 if (epoch >= e.value.epoch && epoch - e.value.epoch > retain) {
777:                     iter.remove()
778:                     ownHashesAcked.remove(e.key)
779:                 }
780:             }
781:             // Belt and braces: bound the map even if epochs run backwards (clock change).
782:             while (ownHashes.size > 16) {
783:                 val oldest = ownHashes.keys.first()
784:                 ownHashes.remove(oldest)
785:                 ownHashesAcked.remove(oldest)
786:             }
787:         }
788:     }
789: 
790:     /**
791:      * P4: claim [hash] as the echo of one of our own recent originations, at most once per
792:      * origination. Returns null when it is not ours; otherwise the remembered frame, whose
793:      * [OwnFrame.carriedText] says whether a delivery receipt is warranted.
794:      */
795:     private fun claimOwnEcho(hash: ByteArray): OwnFrame? {
796:         val hex = hash.joinToString("") { "%02x".format(it) }
797:         synchronized(ownHashesLock) {
798:             val own = ownHashes[hex] ?: return null
799:             return if (ownHashesAcked.add(hex)) own else null
800:         }
801:     }
802: 
803:     /**
804:      * How many times to air one relayed frame, spread across the epoch.
805:      *
806:      * A peer relays a given frame only ONCE per epoch — every later copy it hears that epoch
807:      * is a dedup DUPLICATE — so the originator's delivery receipt hangs on catching a single
808:      * ~2 s burst. Jitter stops the two devices firing in unison but adds no second chance:
809:      * one lost burst still costs a full epoch.
810:      *
811:      * Redundancy is spent where it is actually needed. With one neighbour there is exactly
812:      * one possible relayer and no path diversity at all, so repeat; in a crowd many peers
813:      * relay the same frame independently, which is better diversity than any single phone
814:      * repeating itself, so fall back to one airing and keep the airtime. This scales DOWN
815:      * with density, so a dense protest cannot be turned into a relay storm.
816:      */
817:     private fun relayRepeatsForDensity(): Int {
818:         val neighbors = MeshState.stats.value.neighborsThisEpoch
819:         return when {
820:             neighbors <= 1 -> RELAY_REPEATS_MAX
821:             neighbors <= 3 -> 2
822:             else -> 1
823:         }
824:     }
825: 
826:     /** B8: enqueue a relay task with tier priority, evicting the lowest-priority queued
827:      *  task when the queue is full (never the new LOCAL echo). */
828:     private fun enqueueRelay(frame: ByteArray, msgType: Int, repeats: Int = relayRepeatsForDensity()) {
829:         val prio = when (msgType) { 1 -> 0; 2 -> 1; else -> 2 }
830:         synchronized(relayQueueLock) {
831:             if (relayQueue.size >= RELAY_QUEUE_CAP) {
832:                 val worstIdx = relayQueue.indices.maxByOrNull { relayQueue[it].priority }
833:                 if (worstIdx != null && relayQueue[worstIdx].priority > prio) {
834:                     relayQueue.removeAt(worstIdx)
835:                 } else {
836:                     MeshState.logDebug("relay queue full — dropping relay task (prio $prio)")
837:                     return
838:                 }
839:             }
840:             relayQueue.addLast(RelayTask(frame, prio, relaySeq++, repeats.coerceAtLeast(1)))
841:         }
842:     }
843: 
844:     /**
845:      * A1/C2: PoCP verification accepting the frame's own epoch sketch OR the previous
846:      * epoch's completed sketch. Marks rotate every epoch, so a sketch built from epoch N-1
847:      * marks only ever matches the verifier's N-1 bucket — at rollover an honest sender
848:      * signs that completed sketch with witness seed N-1, and we must try both.
849:      *
850:      * Returns 0 = Valid, 1 = CellMismatch (witness MAC valid but sketches disjoint —
851:      * an honestly remote cell), 2 = Stale (bad MAC / unverifiable),
852:      * [POCP_NO_LOCAL_SKETCH] = we hold no sketch for any candidate bucket, so no verdict is
853:      * possible. That last case is NOT a judgement about the frame: it means we have not yet
854:      * heard anything in those epochs. Callers must not cache it as a decision.
855:      */
856:     private fun verifyPocpAcrossRollover(
857:         frameEp: UInt,
858:         divSketch: ByteArray,
859:         prefix: ByteArray,
860:         wit: ByteArray,
861:         cfg: MeshConfig
862:     ): Int {
863:         var macValid = false
864:         var judged = false
865:         // Candidate buckets: the frame's own epoch, the previous one (marks rotate, so a
866:         // sketch signed at a rollover only matches the N-1 bucket), and the NEXT one — which
867:         // covers a receiver running one epoch BEHIND the sender. Sketches are bucketed by the
868:         // frame's own epoch field, so PoCP is more skew-sensitive than the ±2-epoch K4 gate:
869:         // without the +1 bucket a receiver with a slow clock silently CellMismatched
870:         // everything from a faster peer.
871:         val candidates = buildList {
872:             add(frameEp)
873:             if (frameEp > 0u) add(frameEp - 1u)
874:             add(frameEp + 1u)
875:         }
876:         for (ep in candidates) {
877:             val sketch = MeshState.measurement.localSketch(ep, seed, cfg.rssiFloorDbm)
878:             if (sketch.isEmpty()) continue
879:             judged = true
880:             val v = pocpVerifyWitnessLocal(sketch, divSketch, ep, prefix, wit, cfg.tauThreshold).toInt()
881:             if (v == 0) return 0
882:             if (v == 1) macValid = true
883:         }
884:         return when {
885:             macValid -> 1
886:             judged -> 2
887:             else -> POCP_NO_LOCAL_SKETCH
888:         }
889:     }
890: 
891:     /**
892:      * Single ingest path for received frames, shared by the BLE scan callback and GattPlane.
893:      *
894:      * Invariants enforced:
895:      *   - Byte arrays that are not exactly 226 bytes are silently dropped (checked by frameDecodes
896:      *     in Rust core, which returns false for any length != 226).
897:      *   - No frame bytes are parsed in Kotlin; all interpretation is done by Rust core functions.
898:      */
899:     /**
900:      * Crash barrier. ingestFrame runs on BLE binder threads and on the GATT callback thread;
901:      * an exception anywhere inside it (FFI edge case, encrypted-prefs I/O, OOM on a malformed
902:      * frame) propagated straight to the default uncaught handler and killed the process. One
903:      * bad frame from any nearby device could take the app down.
904:      */
905:     private fun ingestFrame(bytes: ByteArray, rssi: Int) {
906:         try {
907:             ingestFrameInner(bytes, rssi)
908:         } catch (e: Throwable) {
909:             MeshState.logDebug("ingestFrame failed (frame dropped): ${e::class.java.simpleName}: ${e.message}")
910:         }
911:     }
912: 
913:     private fun ingestFrameInner(bytes: ByteArray, rssi: Int) {
914:         val cfg = MeshState.config
915:         val nowMs = System.currentTimeMillis()
916:         val ownEpoch = (nowMs / cfg.epochMs).toUInt()
917: 
918:         rxTotal.incrementAndGet()
919:         if (!frameDecodes(bytes)) {
920:             rxUndecodable.incrementAndGet()
921:             return
922:         }
923:         if (!frameVerifySelf(bytes)) {
924:             // Signature failure on a well-formed frame is NOT normal background noise: it
925:             // means a peer is transmitting frames this build cannot authenticate (version
926:             // skew, corrupted reassembly over GATT, or forgery). Counted, not logged per
927:             // frame, so a flood cannot spam the log.
928:             rxBadSig.incrementAndGet()
929:             return
930:         }
931:         val hash = frameHash(bytes)
932:         val frameEp = frameEpoch(bytes)
933:         if (hash == null || frameEp == null) {
934:             rxUndecodable.incrementAndGet()
935:             return
936:         }
937: 
938:         // K4: epoch freshness — reject frames whose epoch is more than 2 buckets from ours
939:         // (before dedup, so a stale/future frame never occupies a dedup slot).
940:         val diff = frameEp.toLong() - ownEpoch.toLong()
941:         if (diff > 2 || diff < -2) {
942:             val pair = Pair(frameEp, ownEpoch)
943:             if (pair != lastSkewPair) {
944:                 lastSkewPair = pair
945:                 MeshState.logDebug(
946:                     "epoch skew: frame=$frameEp own=$ownEpoch (check epochMs match + clocks)"
947:                 )
948:             }
949:             // Surface it: a skewed peer is invisible, and silence is indistinguishable from
950:             // "nobody is around". diff is in epochs; report it in seconds, which is what the
951:             // user can actually act on.
952:             rxSkewed.incrementAndGet()
953:             val offBySec = diff * cfg.epochMs / 1000L
954:             // Word this as what we actually measured. All we know is that the frame's OWN
955:             // epoch field is N seconds out of step with ours — which is a skewed clock OR a
956:             // frame that took too long to build (the VDL solve) or to relay. Naming the clock
957:             // as the cause sends the user to fix something that may be perfectly fine.
958:             MeshState.clockSkewWarning.value =
959:                 "⚠ FRAMES ARRIVING ${kotlin.math.abs(offBySec)}s " +
960:                     (if (offBySec > 0) "AHEAD OF" else "BEHIND") +
961:                     " THIS PHONE'S CLOCK — THEY ARE BEING DROPPED. CHECK BOTH CLOCKS ARE ON " +
962:                     "NETWORK TIME; IF THEY ARE, THE SENDER IS TOO SLOW TO BUILD FRAMES."
963:             return
964:         }
965: 

(Output capped at 50 KB. Showing lines 1-965. Use offset=966 to continue.)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/Config.kt"
}
```

**Output:**
```
<path>/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/Config.kt</path>
<type>file</type>
<content>
1: package org.bileichat.mesh
2: 
3: import android.content.Context
4: 
5: data class MeshConfig(
6:     val epochMs: Long = 10_000L,
7:     /** Minimum ms between beacon chain advances (acceleration cap). Default 240s real, 10s rig. */
8:     val beaconFloorMs: Long = 10_000L,
9:     /** Minimum distinct LocalImmediate marks required to produce beacon entropy. */
10:     val minHearers: Int = 3,
11:     val tauThreshold: Float = 0.3f,
12:     val rssiFloorDbm: Int = -80,
13:     val codedPhy: Boolean = true,
14:     val advIntervalMs: Long = 1000L,
15:     val scanLowLatency: Boolean = true,
16:     val messageRepeatEpochs: Int = 3
17: ) {
18:     companion object {
19:         // C1: safe ranges. Values outside these are clamped on load and on apply —
20:         // τ=0 would make every sketch match, minHearers=0 makes the beacon constant-entropy,
21:         // and mismatched epochMs silently partitions the mesh via the K4 skew-drop.
22:         val EPOCH_RANGE = 5_000L..120_000L
23:         val BEACON_FLOOR_RANGE = 1_000L..120_000L
24:         val MIN_HEARERS_RANGE = 1..16
25:         val TAU_RANGE = 0.10f..0.90f
26:         val RSSI_FLOOR_RANGE = -100..-40
27:         val ADV_INTERVAL_RANGE = 100L..5_000L
28:         val REPEAT_EPOCHS_RANGE = 1..12
29:     }
30: 
31:     /** C1: clamp every field into its safe range. */
32:     fun sanitized(): MeshConfig = copy(
33:         epochMs = epochMs.coerceIn(EPOCH_RANGE),
34:         beaconFloorMs = beaconFloorMs.coerceIn(BEACON_FLOOR_RANGE),
35:         minHearers = minHearers.coerceIn(MIN_HEARERS_RANGE),
36:         tauThreshold = tauThreshold.coerceIn(TAU_RANGE),
37:         rssiFloorDbm = rssiFloorDbm.coerceIn(RSSI_FLOOR_RANGE),
38:         advIntervalMs = advIntervalMs.coerceIn(ADV_INTERVAL_RANGE),
39:         messageRepeatEpochs = messageRepeatEpochs.coerceIn(REPEAT_EPOCHS_RANGE)
40:     )
41: }
42: 
43: object ConfigStore {
44:     private const val PREFS_NAME = "mesh_cfg"
45:     private const val KEY_EPOCH_MS = "epochMs"
46:     private const val KEY_BEACON_FLOOR_MS = "beaconFloorMs"
47:     private const val KEY_MIN_HEARERS = "minHearers"
48:     private const val KEY_TAU = "tauThreshold"
49:     private const val KEY_RSSI_FLOOR = "rssiFloorDbm"
50:     private const val KEY_CODED_PHY = "codedPhy"
51:     private const val KEY_ADV_INTERVAL = "advIntervalMs"
52:     private const val KEY_SCAN_LOW_LATENCY = "scanLowLatency"
53:     private const val KEY_MESSAGE_REPEAT_EPOCHS = "messageRepeatEpochs"
54: 
55:     fun load(ctx: Context): MeshConfig {
56:         val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
57:         return MeshConfig(
58:             epochMs = prefs.getLong(KEY_EPOCH_MS, 10_000L),
59:             beaconFloorMs = prefs.getLong(KEY_BEACON_FLOOR_MS, 10_000L),
60:             minHearers = prefs.getInt(KEY_MIN_HEARERS, 3),
61:             tauThreshold = prefs.getFloat(KEY_TAU, 0.3f),
62:             rssiFloorDbm = prefs.getInt(KEY_RSSI_FLOOR, -80),
63:             codedPhy = prefs.getBoolean(KEY_CODED_PHY, true),
64:             advIntervalMs = prefs.getLong(KEY_ADV_INTERVAL, 1000L),
65:             scanLowLatency = prefs.getBoolean(KEY_SCAN_LOW_LATENCY, true),
66:             messageRepeatEpochs = prefs.getInt(KEY_MESSAGE_REPEAT_EPOCHS, 3)
67:         ).sanitized() // C1: stored footguns (τ=0, minHearers=0, …) never reach the engine
68:     }
69: 
70:     fun save(ctx: Context, cfg: MeshConfig) {
71:         ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
72:             .edit()
73:             .putLong(KEY_EPOCH_MS, cfg.epochMs)
74:             .putLong(KEY_BEACON_FLOOR_MS, cfg.beaconFloorMs)
75:             .putInt(KEY_MIN_HEARERS, cfg.minHearers)
76:             .putFloat(KEY_TAU, cfg.tauThreshold)
77:             .putInt(KEY_RSSI_FLOOR, cfg.rssiFloorDbm)
78:             .putBoolean(KEY_CODED_PHY, cfg.codedPhy)
79:             .putLong(KEY_ADV_INTERVAL, cfg.advIntervalMs)
80:             .putBoolean(KEY_SCAN_LOW_LATENCY, cfg.scanLowLatency)
81:             .putInt(KEY_MESSAGE_REPEAT_EPOCHS, cfg.messageRepeatEpochs)
82:             .apply()
83:     }
84: 
85:     fun clear(ctx: Context) {
86:         ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
87:             .edit()
88:             .clear()
89:             .commit()
90:     }
91: }

(End of file - total 91 lines)
</content>
```

---

## Assistant (Plan · Kimi K3 (2x usage) · 10.7s)

_Thinking:_

I have the first 965 lines of MeshService.kt. I need the rest (966-1515), plus BleRadio.kt and GattPlane.kt key parts. Let me continue reading MeshService.kt and read BleRadio.kt in parallel.

**Tool: read**

**Input:**
```json
{
  "filePath": "/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/MeshService.kt",
  "offset": 966
}
```

**Output:**
```
<path>/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/MeshService.kt</path>
<type>file</type>
<content>
966:         val mark = frameMark(bytes) ?: return
967:         val wp = frameWitnessParts(bytes)
968: 
969:         // Send-and-listen (spec): a copy of OUR OWN frame coming back over the relay path
970:         // means at least one peer relayed it. B1: that echo is NOT a delivery guarantee —
971:         // a single adversarial device can forge it by relaying once and blackholing the
972:         // rest. LOCAL therefore only drops to sparse re-airing; BROADCAST still stops at
973:         // the next epoch boundary (its TTL does the propagating, not our re-origination).
974:         // Our hash went into dedup at origination, so this check must run BEFORE the dedup
975:         // gate. TTL sits outside the hashed region, so the relayed echo hashes identically.
976:         val ownEcho = claimOwnEcho(hash)
977:         if (ownEcho != null) {
978:             // Only a frame that actually carried the user's text is a delivery signal. LOCAL
979:             // airs empty presence frames between sparse re-broadcasts; echoing one of those
980:             // is liveness, not receipt.
981:             val textEcho = ownEcho.carriedText && MeshState.outgoingText.value.isNotEmpty()
982:             if (textEcho) reflectionHeard = true
983:             MeshState.logDebug(
984:                 "own echo heard (epoch=${ownEcho.epoch} text=${ownEcho.carriedText}) — a peer relayed us"
985:             )
986:             if (textEcho) {
987:                 // Second tick: a peer put our frame back on air. Still not "delivered" — one
988:                 // adversarial device can relay once and blackhole everything after.
989:                 MeshState.outgoingRowTs?.let { MeshState.markOutgoing(it, SendState.ECHOED) }
990:                 MeshState.receipt.value =
991:                     "✓ heard back once — a peer relayed it (not proof of delivery)"
992:                 if (MeshState.outgoingTier.value != SendTier.LOCAL) {
993:                     val repeat = cfg.messageRepeatEpochs.toLong()
994:                     if (repeat > 0) {
995:                         MeshState.outgoingSetAtEpoch =
996:                             (ownEpoch.toLong() + 1L - repeat).coerceAtLeast(0L).toUInt()
997:                         MeshState.logDebug("reflection heard: mesh is carrying our broadcast; stopping re-origination")
998:                     }
999:                 }
1000:             }
1001:         }
1002: 
1003:         // Presence: direct-RF liveness only, BEFORE the dedup gate. A frame counts only
1004:         // at its ORIGINATION TTL: relays decrement (regional/private) or clobber to 0
1005:         // (local), so ttl == origin TTL ⇔ straight from the originator. Relayed copies
1006:         // must not register the originator as "nearby" — including the relayed echo of
1007:         // our OWN frame, which would otherwise count us as our own neighbor.
1008:         // Deliberately no RSSI floor: any frame that decoded + verified is a real
1009:         // transmission. The −80 dBm config floor is a sketch/trust window, NOT a liveness
1010:         // window — applying it here made the count flicker at the boundary while messages
1011:         // kept flowing.
1012:         val localTtl = defaultTtlLocal().toInt()
1013:         val originTtl = defaultTtlRegional().toInt()
1014:         val direct = when (wp?.msgType?.toInt()) {
1015:             1 -> frameTtl(bytes)?.toInt() == localTtl
1016:             2, 3 -> frameTtl(bytes)?.toInt() == originTtl
1017:             else -> false
1018:         }
1019:         if (direct) MeshState.measurement.recordPresence(mark, frameEp)
1020: 
1021:         // Admission check only — the INSERT happens at the end, once we have actually acted
1022:         // on this frame. Inserting here meant a frame that transiently failed verification
1023:         // (empty local sketch at the start of an epoch, contacts not yet loaded) was stuck in
1024:         // the seen-set for the whole ~3-epoch window: every retransmission of those exact
1025:         // bytes was dropped here, so the loss could never self-heal.
1026:         when (dedup.checkEpoch(hash, frameEp)) {
1027:             FfiDedupVerdict.FRESH -> Unit
1028:             FfiDedupVerdict.DUPLICATE -> {
1029:                 rxDuplicate.incrementAndGet()
1030:                 return
1031:             }
1032:             FfiDedupVerdict.BUCKET_FULL -> {
1033:                 // C8 anti-eviction sub-cap hit. This is NOT a duplicate: a fresh, validly
1034:                 // signed frame is being refused, and while it lasts nothing stamped with this
1035:                 // epoch is displayed, relayed, or measured. 1024 signed frames is cheap to
1036:                 // produce, so treat a sustained occurrence as a jamming signal.
1037:                 if (lastBucketFullEpoch != frameEp) {
1038:                     lastBucketFullEpoch = frameEp
1039:                     MeshState.logDebug(
1040:                         "dedup bucket for epoch $frameEp is FULL — further frames stamped with " +
1041:                             "this epoch are being refused (not displayed, not relayed); possible flood"
1042:                     )
1043:                 }
1044:                 return
1045:             }
1046:         }
1047: 
1048:         // A frame inside the freshness window means at least one peer's clock agrees with
1049:         // ours; the skew banner is no longer accurate.
1050:         if (MeshState.clockSkewWarning.value != null) {
1051:             MeshState.clockSkewWarning.value = null
1052:             lastSkewPair = null
1053:         }
1054: 
1055:         // Log first frame heard each epoch
1056:         if (firstFrameEpoch != ownEpoch) {
1057:             firstFrameEpoch = ownEpoch
1058:             MeshState.logDebug("first frame heard in epoch $ownEpoch")
1059:         }
1060: 
1061:         MeshState.measurement.record(mark, rssi, frameEp)
1062: 
1063:         // K7 + K5: collect LocalImmediate marks for beacon entropy. Only LocalImmediate
1064:         // frames above the RSSI floor count as direct-RF co-presence witnesses.
1065:         if (wp != null && wp.msgType.toInt() == 1 && rssi >= cfg.rssiFloorDbm) {
1066:             synchronized(marksLock) { localImmediateMarks.add(mark) }
1067:         }
1068: 
1069:         // K2: Tier-3 private (msgType 3). Trial-decrypt against every paired contact key.
1070:         // E3: iterate ALL contacts unconditionally (no early break) — the NUMBER of decrypt
1071:         // calls must not leak which contact index matched (timing side-channel).
1072:         if (wp != null && wp.msgType.toInt() == 3) {
1073:             if (!vdlCheckFrame(bytes)) {
1074:                 MeshState.logDebug("drop: private frame failed VDL proof-of-work check")
1075:                 // A bad PoW is a permanent property of these bytes — mark seen so a flood of
1076:                 // copies is rejected at the cheap gate instead of re-running the check.
1077:                 dedup.insertEpoch(hash, frameEp)
1078:                 return // invalid PoW: drop, do not relay
1079:             }
1080:             var privatePlaintext: String? = null
1081:             var privateLabel: String? = null
1082:             var contactsTried = 0
1083:             var keysTried = 0
1084:             for (contact in PairStore.contacts(this)) {
1085:                 contactsTried++
1086:                 // A3: v2 contacts try the epoch-ratcheted key for the frame's epoch
1087:                 // (fast-forwarding when the sender is ahead); v1 uses the static key.
1088:                 for (key in PairStore.candidateKeys(this, contact, frameEp)) {
1089:                     keysTried++
1090:                     val pt = openPrivateBodyOnly(bytes, key)
1091:                     if (pt != null && privatePlaintext == null) {
1092:                         privatePlaintext = pt
1093:                         privateLabel = contact.label
1094:                     }
1095:                 }
1096:             }
1097:             if (privatePlaintext == null) {
1098:                 // A private frame that reaches us, passes VDL, and opens under no key is the
1099:                 // single most confusing failure in the app: it is relayed normally, so the
1100:                 // mesh looks healthy, while the recipient's screen stays empty and the sender
1101:                 // gets no signal. Most often it is simply not addressed to us — but with a
1102:                 // divergent ratchet anchor it is EVERY frame, so the counts matter.
1103:                 MeshState.logDebug(
1104:                     "private frame not for us (or key mismatch): tried $keysTried key(s) " +
1105:                         "across $contactsTried contact(s) at epoch $frameEp"
1106:                 )
1107:             }
1108:             if (privatePlaintext != null) {
1109:                 // A3: persist any fast-forwarded chain state (past keys deleted). This does a
1110:                 // synchronous commit() to EncryptedSharedPreferences, so it must not run on
1111:                 // the BLE binder thread that delivered this frame — disk I/O there stalls
1112:                 // scan callback delivery for every other frame in flight.
1113:                 val label = privateLabel!!
1114:                 lifecycleScope.launch(Dispatchers.IO) {
1115:                     try {
1116:                         PairStore.noteOpened(this@MeshService, label, frameEp)
1117:                     } catch (e: Exception) {
1118:                         MeshState.logDebug("noteOpened failed for '$label': ${e.message}")
1119:                     }
1120:                 }
1121:                 MeshState.appendMessage(
1122:                     MsgRow(
1123:                         tsMs = System.currentTimeMillis(),
1124:                         epoch = frameEp,
1125:                         markHexPrefix = "🔒 $privateLabel",
1126:                         rssi = rssi,
1127:                         text = privatePlaintext,
1128:                         mine = false,
1129:                         tier = SendTier.PRIVATE,
1130:                         direct = direct,
1131:                         contactLabel = privateLabel
1132:                     )
1133:                 )
1134:             }
1135:             // Relay regardless of whether we could decrypt (multi-hop delivery).
1136:             relayFrame(bytes)?.let { enqueueRelay(it, 3) }
1137:             // Acted on: relayed, and decrypted if it was for us. Safe to mark seen.
1138:             dedup.insertEpoch(hash, frameEp)
1139:             return
1140:         }
1141: 
1142:         // Public path (msgType 1/2).
1143:         //
1144:         // A1: a frame WITHOUT a witness is relay-only — NEVER displayed. Before this fix the
1145:         // witness check was skipped entirely when both fields were zero, so a remote van could
1146:         // inject a fake "TEAR GAS" that displayed as DIRECT · VERIFIED on every phone.
1147:         //
1148:         // Display rules:
1149:         //   LOCAL     — witness must be PoCP-Valid against our cell (current or previous
1150:         //               epoch sketch bucket). CellMismatch/Stale: dropped entirely.
1151:         //   BROADCAST — witness MAC must be valid. Jaccard outcome only feeds the badge:
1152:         //               co-present origin vs remote-cell claim. A2: corroboration counts
1153:         //               ONLY claims heard DIRECTLY (origination TTL) and is shown as a HINT,
1154:         //               never as a boolean unlock (a single nearby attacker can forge two
1155:         //               dissimilar claims — the old distinct≥2 display lock was security theater).
1156:         var displayOk = false
1157:         var relayOnly = false
1158:         var unjudged = false
1159:         var corroborations = 0u
1160:         if (wp != null) {
1161:             val msgType = wp.msgType.toInt()
1162:             val hasWitness = wp.pocpWit.any { it != 0.toByte() } ||
1163:                 wp.divSketch.any { it != 0.toByte() }
1164:             if (!hasWitness) {
1165:                 relayOnly = true // A1: relay-only, never display
1166:                 // Every "the other phone relayed it but never showed it" report lands here.
1167:                 // An up-to-date peer never originates witnessless, so this now means the
1168:                 // sender is running an older build or genuinely had no cell to attest to.
1169:                 MeshState.logDebug(
1170:                     "relay-only: frame from ${mark.joinToString("") { "%02x".format(it) }.take(8)} " +
1171:                         "carries NO PoCP witness — relayed but not displayed (sender heard nobody?)"
1172:                 )
1173:             } else {
1174:                 when (val verdict = verifyPocpAcrossRollover(wp.epoch, wp.divSketch, wp.framePrefix, wp.pocpWit, cfg)) {
1175:                     0 -> displayOk = true // Valid: co-present with our cell
1176:                     1 -> {
1177:                         if (msgType == 2) {
1178:                             displayOk = true // honest remote-cell broadcast
1179:                         } else {
1180:                             // LOCAL from a cell that doesn't overlap ours: dropped entirely.
1181:                             MeshState.logDebug(
1182:                                 "drop LOCAL: PoCP CellMismatch (jaccard < tau=${cfg.tauThreshold}) — " +
1183:                                     "our epoch-$frameEp sketch has " +
1184:                                     "${MeshState.measurement.localSketch(frameEp, seed, cfg.rssiFloorDbm).size} slots"
1185:                             )
1186:                         }
1187:                     }
1188:                     POCP_NO_LOCAL_SKETCH -> {
1189:                         // We hold no sketch for any candidate bucket, so this is not a
1190:                         // judgement about the frame — we simply have not heard anything yet.
1191:                         // Leave it out of the seen-set so a re-air can still be displayed.
1192:                         unjudged = true
1193:                         MeshState.logDebug(
1194:                             "defer: no local sketch for epochs ${frameEp - 1u}..${frameEp + 1u} — " +
1195:                                 "cannot judge this frame yet, leaving it eligible for retry"
1196:                         )
1197:                     }
1198:                     else -> {
1199:                         // Stale: the witness MAC did not verify against any sketch bucket.
1200:                         MeshState.logDebug(
1201:                             "drop: PoCP verdict=$verdict (stale/unverifiable witness) type=$msgType " +
1202:                                 "frameEpoch=$frameEp ownEpoch=$ownEpoch — no display, no relay"
1203:                         )
1204:                     }
1205:                 }
1206: 
1207:                 // R1: soft detection of div_sketch reuse across distinct marks (copy signal).
1208:                 synchronized(sketchSeenLock) {
1209:                     if (sketchSeenEpoch != frameEp) {
1210:                         sketchSeen.clear()
1211:                         sketchSeenEpoch = frameEp
1212:                     }
1213:                     val divHex = wp.divSketch.joinToString("") { "%02x".format(it) }
1214:                     val markHex = mark.joinToString("") { "%02x".format(it) }
1215:                     val prev = sketchSeen[divHex]
1216:                     if (prev == null) {
1217:                         sketchSeen[divHex] = markHex
1218:                     } else if (prev != markHex) {
1219:                         MeshState.logDebug("R1: div_sketch reuse across distinct marks (copy signal)")
1220:                     }
1221:                 }
1222: 
1223:                 // A2: corroboration hint for broadcast — direct-heard claims only.
1224:                 if (displayOk && msgType == 2) {
1225:                     corroborations = if (direct) {
1226:                         trust.recordVerification(wp.bodyHash, wp.divSketch, cfg.tauThreshold)
1227:                     } else {
1228:                         trust.distinctCount(wp.bodyHash)
1229:                     }
1230:                 }
1231:             }
1232:         }
1233: 
1234:         // Relay if the frame is either displayable or relay-only.
1235:         if (displayOk || relayOnly) {
1236:             relayFrame(bytes)?.let { enqueueRelay(it, wp?.msgType?.toInt() ?: 2) }
1237:         }
1238: 
1239:         // Mark seen only now, and only if we reached an actual decision. A frame we could
1240:         // not judge (no local sketch for any candidate epoch) stays out of the seen-set so a
1241:         // re-air moments later — once we have heard someone — can still be displayed.
1242:         // Inserting before the decision meant one transient failure suppressed those exact
1243:         // bytes for the whole ~3-epoch dedup window and the loss never self-healed.
1244:         if (!unjudged) dedup.insertEpoch(hash, frameEp)
1245: 
1246:         // Display only when verified and not relay-only.
1247:         if (displayOk) {
1248:             val text = frameBodyText(bytes)
1249:             if (!text.isNullOrEmpty()) {
1250:                 val markHex = mark.joinToString("") { "%02x".format(it) }
1251:                 rxShown.incrementAndGet()
1252:                 val tier = if (wp?.msgType?.toInt() == 1) SendTier.LOCAL else SendTier.BROADCAST
1253:                 // How much evidence the co-presence check actually had. A verified
1254:                 // witness against a 2-3 mark cell is cheaply forgeable by someone who
1255:                 // was never there (see the pocp module header), so the UI must not
1256:                 // present it as proof.
1257:                 val cellSize = MeshState.measurement
1258:                     .localSketch(frameEp, seed, MeshState.config.rssiFloorDbm).size
1259:                 // Re-airings of the same alert collapse into one row with a count. The old
1260:                 // (text, mark-prefix) suppression key rotated every epoch — exactly as fast
1261:                 // as the sender re-aired — so it never suppressed anything and one message
1262:                 // rendered as messageRepeatEpochs identical rows.
1263:                 MeshState.appendOrMergeIncoming(
1264:                     MsgRow(
1265:                         tsMs = System.currentTimeMillis(),
1266:                         epoch = frameEp,
1267:                         markHexPrefix = markHex.take(8),
1268:                         rssi = rssi,
1269:                         text = text,
1270:                         mine = false,
1271:                         tier = tier,
1272:                         direct = direct,
1273:                         corroborations = corroborations.toInt(),
1274:                         lowConfidenceCell = cellSize < MIN_TRUSTWORTHY_CELL
1275:                     )
1276:                 )
1277:             }
1278:         }
1279:     }
1280: 
1281:     /**
1282:      * Re-air a still-fresh sealed private frame, once per epoch rollover.
1283:      *
1284:      * Private used to be strictly one-shot: a single ~6 s advertising window, no repeats, no
1285:      * receipt. Everything else on the mesh re-originates for messageRepeatEpochs, so private
1286:      * was by far the easiest message to lose — and losing it was completely silent on both
1287:      * ends. Re-airing the already-sealed bytes costs no second VDL solve.
1288:      */
1289:     private fun reairPendingPrivate(epoch: UInt, cfg: MeshConfig) {
1290:         val pending = synchronized(pendingPrivateLock) {
1291:             val p = pendingPrivate ?: return
1292:             // Past the receiver's freshness gate, or out of attempts: drop it.
1293:             if (p.airingsLeft <= 0 || epoch < p.sealedEpoch || epoch - p.sealedEpoch > 2u) {
1294:                 pendingPrivate = null
1295:                 return
1296:             }
1297:             p.airingsLeft -= 1
1298:             if (p.airingsLeft <= 0) pendingPrivate = null
1299:             p
1300:         }
1301:         val restoreFrame = currentPublicFrame ?: return
1302:         if (privateTransportActive) return
1303:         val windowMs = minOf(cfg.messageRepeatEpochs.toLong() * cfg.epochMs, 6_000L)
1304:         privateTransportActive = true
1305:         val started = radio.advertisePrivateOnce(
1306:             frame = pending.frame,
1307:             durationMs = windowMs,
1308:             restoreFrame = restoreFrame,
1309:             codedPhy = cfg.codedPhy,
1310:             advIntervalMs = cfg.advIntervalMs
1311:         ) {
1312:             privateTransportActive = false
1313:             gattPlane.currentFrame = currentPublicFrame ?: restoreFrame
1314:         }
1315:         if (started) {
1316:             gattPlane.currentFrame = pending.frame
1317:             MeshState.logDebug("private frame re-aired (${pending.airingsLeft} left)")
1318:         } else {
1319:             privateTransportActive = false
1320:         }
1321:     }
1322: 
1323:     /**
1324:      * Build a message frame for [epoch] carrying [text], start advertising it, and push it to the
1325:      * GATT plane (triggers notify + writes to connected peers).
1326:      * If [text] is too long (> 63 UTF-8 bytes) the fact is logged and the frame falls back
1327:      * to empty text via makeMessageFrame with an empty string.
1328:      */
1329:     private fun rebuildAndAdvertise(epoch: UInt, cfg: MeshConfig, text: String) {
1330:         val effectiveText = if (text.toByteArray(Charsets.UTF_8).size > 63) {
1331:             MeshState.logDebug(
1332:                 "outgoing text too long (${text.toByteArray(Charsets.UTF_8).size} UTF-8 bytes, max 63); " +
1333:                     "falling back to empty"
1334:             )
1335:             ""
1336:         } else {
1337:             text
1338:         }
1339: 
1340:         val beaconSeed = beacon.seed()
1341:         val localImmediate = MeshState.outgoingTier.value == SendTier.LOCAL
1342:         // TTL from the Rust core (invariant #1): local = 1 (relayable once so the
1343:         // reflection receipt can come back), regional/private = 8.
1344:         val ttl: UByte = if (localImmediate) defaultTtlLocal().toUByte() else defaultTtlRegional().toUByte()
1345:         // P2/P3/P5: register OUR OWN mark for this epoch before building the sketch. The mark
1346:         // is a pure function of (seed, beaconSeed), so the unwitnessed frame built here has
1347:         // the same mark as the witnessed frame built below — we build it once to read the
1348:         // mark out, and reuse it as the fallback if witnessed construction fails.
1349:         //
1350:         // Why this matters: a cell is "the devices in RF range of each other, INCLUDING me".
1351:         // Without self-inclusion a phone that had heard nobody yet produced an EMPTY sketch
1352:         // and originated a WITNESSLESS frame, which every receiver relays but never displays
1353:         // (the relayOnly branch in ingestFrame) — while the originator still heard its own
1354:         // relayed echo and printed a delivery receipt. Blank screen on one phone, "heard
1355:         // back once" on the other. Self-inclusion also fixes the two-device case: A held
1356:         // {mark_B} and B held {mark_A}, which are disjoint, so LOCAL could never display.
1357:         val baseFrame = makeMessageFrame(seed, epoch, beaconSeed, localImmediate, effectiveText)
1358:         if (baseFrame != null) {
1359:             frameMark(baseFrame)?.let { MeshState.measurement.recordSelf(it, epoch) }
1360:         }
1361: 
1362:         // A1/C2: sign whichever cell view is RICHER — the current epoch's bucket or the
1363:         // previous epoch's completed one (receivers try both, plus epoch+1, so either
1364:         // verifies).
1365:         //
1366:         // This used to key off "is the current sketch empty", which worked only because a
1367:         // freshly rolled-over bucket WAS empty. Now that our own mark is always in it, the
1368:         // current bucket is never empty, so that test would always pick it — and immediately
1369:         // after a rollover it holds nothing but our own mark. A receiver in a crowd has a
1370:         // fuller bucket, so a one-element claim scores 1/N and falls under tau: honest frames
1371:         // would CellMismatch for the first moments of every epoch, worse the denser the crowd.
1372:         // Comparing sizes keeps the original rollover intent and the self-inclusion guarantee.
1373:         val sketchCur = MeshState.measurement.localSketch(epoch, seed, cfg.rssiFloorDbm)
1374:         val sketchPrev = MeshState.measurement.localSketch(epoch - 1u, seed, cfg.rssiFloorDbm)
1375:         val (sketch, witEpoch) = if (sketchPrev.size > sketchCur.size) {
1376:             sketchPrev to (epoch - 1u)
1377:         } else {
1378:             sketchCur to epoch
1379:         }
1380:         val divSketch = if (sketch.isNotEmpty()) pocpSketchToDivSketch(sketch) else null
1381:         if (divSketch == null) {
1382:             // Unreachable once recordSelf has run for this epoch — a self-inclusive sketch
1383:             // is never empty. Loud, because the resulting frame is relay-only at receivers.
1384:             MeshState.logDebug(
1385:                 "WITNESSLESS origination epoch=$epoch (own mark missing) — receivers will " +
1386:                     "relay this frame but NOT display it"
1387:             )
1388:         }
1389:         val frame = (
1390:             if (divSketch != null) {
1391:                 makeMessageFrameWithWitness(
1392:                     seed, epoch, beaconSeed, localImmediate, effectiveText, ttl, divSketch, witEpoch
1393:                 )
1394:             } else {
1395:                 null
1396:             }
1397:             ) ?: baseFrame
1398:         if (frame != null) {
1399:             currentPublicFrame = frame
1400:             // Insert our own frame's hash into dedup: a relayed copy of our frame comes back
1401:             // with TTL decremented, but TTL sits in the hop-mutable region excluded from the
1402:             // frame hash — so the echo has OUR hash and dedup drops it instead of showing our
1403:             // own message as incoming.
1404:             frameHash(frame)?.let {
1405:                 rememberOwnFrame(it, epoch, cfg, carriedText = effectiveText.isNotEmpty())
1406:                 dedup.checkAndInsertEpoch(it, epoch)
1407:             }
1408:             if (!privateTransportActive) {
1409:                 // Full stop+start every epoch — see BleRadio note on setAdvertisingData.
1410:                 radio.startAdvertising(frame, cfg.codedPhy, cfg.advIntervalMs)
1411:                 // Push to GATT plane: notifies subscribed centrals and writes to connected peripherals.
1412:                 gattPlane.currentFrame = frame
1413:                 // One tick: our frame is on air. This is the strongest claim the radio can
1414:                 // actually support at this point — nothing has been heard back yet.
1415:                 if (effectiveText.isNotEmpty()) {
1416:                     MeshState.outgoingRowTs?.let { MeshState.markOutgoing(it, SendState.ON_AIR) }
1417:                 }
1418:             }
1419:         } else {
1420:             MeshState.logDebug("frame origination returned null for epoch=$epoch")
1421:         }
1422:     }
1423: 
1424:     /**
1425:      * B1: emergency panic-wipe. Clears all persisted key material, configuration, measurement
1426:      * data, and measurement export files. Then stops the BLE service and removes the persistent
1427:      * notification. After this call the device is sterile (no trace of mesh activity remains on
1428:      * the filesystem).
1429:      *
1430:      * Call from the Rust panic flag poller (epoch loop) or the UI panic button.
1431:      */
1432:     private fun performPanicWipe() {
1433:         // K3: one-shot — subsequent triggers are no-ops.
1434:         if (wiped) return
1435:         wiped = true
1436:         MeshState.logDebug("!!! PANIC WIPE initiated")
1437:         try {
1438:             // Radio FIRST, before anything is zeroed. radio.stop() also cancels a pending
1439:             // private-window restore; if that runnable were still armed while we zeroed the
1440:             // frames below, it would put 226 zero bytes of mesh service data back on air
1441:             // after the wipe. Silencing the transmitter is the part an RF observer can see,
1442:             // so it must not wait behind key zeroization.
1443:             if (::radio.isInitialized) radio.stop()
1444:             if (::gattPlane.isInitialized) gattPlane.stop()
1445: 
1446:             // Clear Rust in-memory state (the flag was already set; we call the function).
1447:             panicWipe()
1448:             // C7: zero the live beacon seed too — previously it stayed in Rust memory,
1449:             // recoverable until process exit.
1450:             if (::beacon.isInitialized) beacon.wipe()
1451: 
1452:             // Clear Android persisted state.
1453:             PairStore.wipe(this)
1454:             ConfigStore.clear(this)
1455:             // Legacy plaintext counter file (older installs) and crash log.
1456:             getSharedPreferences(PAIR_PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
1457:             getSharedPreferences("crash_log", Context.MODE_PRIVATE).edit().clear().commit()
1458: 
1459:             // Clear in-memory measurement data.
1460:             MeshState.measurement.clear()
1461:             MeshState.messages.value = emptyList()
1462:             MeshState.debugLog.value = listOf("!!! PANIC WIPE at ${System.currentTimeMillis()}")
1463:             MeshState.outgoingText.value = ""
1464:             // C7: force the UI to drop remembered Contact objects (they hold pair keys until
1465:             // GC — a documented JVM limit; recomposition to an empty list is the best we can do).
1466:             MeshState.contactsVersion.value += 1
1467: 
1468:             // Zeroize in-memory secrets. seed is lateinit — a cold-start ACTION_PANIC
1469:             // (service never fully started) reaches here before seed is assigned.
1470:             if (::seed.isInitialized) seed.fill(0)
1471:             currentPublicFrame?.fill(0)
1472:             currentPublicFrame = null
1473:             synchronized(relayQueueLock) { relayQueue.clear() }
1474:             synchronized(marksLock) {
1475:                 localImmediateMarks.forEach { it.fill(0) }
1476:                 localImmediateMarks.clear()
1477:             }
1478: 
1479:             // Radio/GATT were already stopped at the top of the wipe.
1480: 
1481:             // Remove foreground notification and stop the service.
1482:             stopForeground(STOP_FOREGROUND_REMOVE)
1483:             stopSelf()
1484:         } catch (e: Exception) {
1485:             android.util.Log.e("MeshService", "panic-wipe error: ${e.message}")
1486:             // Kill process if cleanup fails — any residual data is an unacceptable risk.
1487:             android.os.Process.killProcess(android.os.Process.myPid())
1488:         }
1489:     }
1490: 
1491:     private fun createNotificationChannel() {
1492:         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
1493:             val channel = NotificationChannel(
1494:                 CHANNEL_ID,
1495:                 CHANNEL_NAME,
1496:                 NotificationManager.IMPORTANCE_LOW
1497:             ).apply {
1498:                 description = "BLE mesh background service"
1499:                 setShowBadge(false)
1500:             }
1501:             val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
1502:             nm.createNotificationChannel(channel)
1503:         }
1504:     }
1505: 
1506:     private fun buildNotification(text: String): Notification {
1507:         return NotificationCompat.Builder(this, CHANNEL_ID)
1508:             .setContentTitle("Mesh Radio")
1509:             .setContentText(text)
1510:             .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
1511:             .setOngoing(true)
1512:             .setOnlyAlertOnce(true)
1513:             .build()
1514:     }
1515: }

(End of file - total 1515 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/BleRadio.kt"
}
```

**Output:**
```
<path>/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/BleRadio.kt</path>
<type>file</type>
<content>
1: package org.bileichat.mesh
2: 
3: import android.annotation.SuppressLint
4: import android.bluetooth.BluetoothDevice
5: import android.bluetooth.BluetoothManager
6: import android.bluetooth.le.AdvertiseData
7: import android.bluetooth.le.AdvertisingSet
8: import android.bluetooth.le.AdvertisingSetCallback
9: import android.bluetooth.le.AdvertisingSetParameters
10: import android.bluetooth.le.ScanCallback
11: import android.bluetooth.le.ScanFilter
12: import android.bluetooth.le.ScanResult
13: import android.bluetooth.le.ScanSettings
14: import android.content.Context
15: import android.os.Build
16: import android.os.ParcelUuid
17: import java.util.UUID
18: 
19: class BleRadio(private val ctx: Context) {
20: 
21:     companion object {
22:         val SERVICE_UUID: UUID = UUID.fromString("6c6f6361-6c6d-4573-6800-000000000001")
23:         val PARCEL_UUID = ParcelUuid(SERVICE_UUID)
24: 
25:         // BLE extended advertising interval units: 0.625 ms per unit
26:         // Valid range: 0x000020 (20ms) to 0xFFFFFF (~10485s)
27:         private const val INTERVAL_UNIT_MIN = 0x000020
28:         private const val INTERVAL_UNIT_MAX = 0xFFFFFF
29:     }
30: 
31:     private val bluetoothManager =
32:         ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
33:     private val adapter get() = bluetoothManager.adapter
34: 
35:     @Volatile private var currentAdvSet: AdvertisingSet? = null
36:     private var advCallback: AdvertisingSetCallback? = null
37:     private val scanCallbacks = mutableListOf<ScanCallback>()
38:     /** Relay advertising slots in use. Incremented on the relay-drain coroutine, decremented
39:      *  from both the BLE callback thread and the main handler — must be atomic. */
40:     private val activeRelaySets = java.util.concurrent.atomic.AtomicInteger(0)
41:     @Volatile private var privateAdvActive = false
42: 
43:     /**
44:      * Pending private-window restore runnable.
45:      *
46:      * This used to be posted on a freshly constructed anonymous Handler, so nothing could
47:      * ever cancel it. A panic wipe (or a plain radio-off toggle) during the ≤6 s private
48:      * window tore everything down, and then the orphaned runnable fired and called
49:      * startAdvertising() again — the phone resumed beaconing the mesh service UUID after a
50:      * wipe whose contract is that the device is sterile, with no service, no watchdog and
51:      * nothing left to ever stop it. The frame bytes are zeroed by the wipe, so no content
52:      * leaked, but the RF presence did.
53:      *
54:      * Held here (and posted on the shared [mainHandler]) so [stopAdvertising] can cancel it.
55:      */
56:     private var privateRestore: Runnable? = null
57: 
58:     // Advertising single-flight. startAdvertisingSet is asynchronous: the handle only
59:     // arrives in the callback, so currentAdvSet is null for a window after every start.
60:     // Callers (epoch rollover, send-path, tier switch, watchdog) can all fire within that
61:     // window — two overlapping hardware starts used to leave one set as an untracked
62:     // zombie broadcasting a stale frame forever (observed in field testing: both phones
63:     // kept re-airing their startup frame; K4 then dropped it as epoch-skewed and the mesh
64:     // looked dead). The lock serializes control; advStartInFlight collapses overlapping
65:     // starts into pendingFrame, applied when the set reports started; and any set that
66:     // reports started AFTER being superseded/stopped immediately stops itself.
67:     private val advLock = Any()
68:     @Volatile private var advStartInFlight = false
69:     private var pendingFrame: ByteArray? = null
70: 
71:     // Scan self-healing: a failed scan is dead until restarted (controller resource
72:     // exhaustion, stack hiccup). Without a restart, frame reception silently stops and
73:     // presence collapses to 0 while advertising keeps working.
74:     @Volatile private var wantScanning = false
75:     private var lastLowLatency = false
76:     private var lastOnLegacyPeer: ((BluetoothDevice, Int) -> Unit)? = null
77:     private var lastOnFrame: ((ByteArray, Int) -> Unit)? = null
78:     private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
79: 
80:     /** Called with a human-readable debug line on notable events. */
81:     var onDebug: ((String) -> Unit)? = null
82: 
83:     /** Human-readable current advertising mode, shown in the stats pane. */
84:     @Volatile
85:     var advMode: String = "off"
86: 
87:     /** One-line adapter capability summary for the debug log. */
88:     @SuppressLint("MissingPermission")
89:     fun capabilityReport(): String {
90:         val a = adapter ?: return "radio: no bluetooth adapter"
91:         return try {
92:             "radio: enabled=${a.isEnabled} extAdv=${a.isLeExtendedAdvertisingSupported} " +
93:                 "codedPhy=${a.isLeCodedPhySupported} maxAdvData=${a.leMaximumAdvertisingDataLength}"
94:         } catch (e: Exception) {
95:             "radio: capability query failed: ${e.message}"
96:         }
97:     }
98: 
99:     /**
100:      * The two host conditions that silently kill BLE scanning.
101:      *
102:      * On API <= 30 the system location toggle must be ON or `startScan` succeeds, reports no
103:      * error, and simply never delivers a result — producing a phone that advertises normally
104:      * (so peers see it) while hearing nothing at all, with no log anywhere. API 31+ is exempt
105:      * because the manifest declares BLUETOOTH_SCAN with `neverForLocation`.
106:      */
107:     private fun scanEnvironment(): String {
108:         val sdk = Build.VERSION.SDK_INT
109:         val locationOn = try {
110:             val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
111:             when {
112:                 lm == null -> "unknown"
113:                 Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> lm.isLocationEnabled.toString()
114:                 else -> android.provider.Settings.Secure.getInt(
115:                     ctx.contentResolver,
116:                     android.provider.Settings.Secure.LOCATION_MODE,
117:                     android.provider.Settings.Secure.LOCATION_MODE_OFF
118:                 ).let { (it != android.provider.Settings.Secure.LOCATION_MODE_OFF).toString() }
119:             }
120:         } catch (e: Exception) {
121:             "unknown(${e.message})"
122:         }
123:         val needsLocation = sdk <= 30
124:         return "sdk=$sdk locationServices=$locationOn" +
125:             if (needsLocation && locationOn == "false") {
126:                 " *** SCANNING WILL RETURN NOTHING: Android $sdk requires location services ON ***"
127:             } else ""
128:     }
129: 
130:     fun isSupported(): Boolean {
131:         return adapter != null && adapter.isEnabled && adapter.isLeExtendedAdvertisingSupported
132:     }
133: 
134:     fun codedPhySupported(): Boolean {
135:         return adapter != null && adapter.isLeCodedPhySupported
136:     }
137: 
138:     /**
139:      * True when this adapter can put a whole 226-byte frame into an advertisement.
140:      * Frame (226 B) + UUID + AD framing overhead needs ~260 B of advertising capacity.
141:      *
142:      * P6: adapters that fail this can ONLY move frames over the GATT plane. Origination
143:      * already honoured that (startLegacyFallbackLocked), but advertiseRelayOnce did not —
144:      * it built setLegacyMode(false) unconditionally, so on these phones every relay failed
145:      * silently while still holding an activeRelaySets slot for its full duration. Multi-hop
146:      * propagation was dead on older hardware.
147:      */
148:     @SuppressLint("MissingPermission")
149:     fun extendedAdvCapable(): Boolean = try {
150:         val a = adapter
151:         a != null && a.isLeExtendedAdvertisingSupported && a.leMaximumAdvertisingDataLength >= 260
152:     } catch (e: Exception) {
153:         false
154:     }
155: 
156:     @SuppressLint("MissingPermission")
157:     fun startAdvertising(frame: ByteArray, codedPhy: Boolean, advIntervalMs: Long) {
158:         synchronized(advLock) {
159:             if (advStartInFlight) {
160:                 // A start is already in flight; coalesce into pendingFrame so the newest
161:                 // frame goes on air when the pending set reports started.
162:                 pendingFrame = frame
163:                 onDebug?.invoke("adv start coalesced: start already in flight")
164:                 return
165:             }
166:             // Legacy fallback carries no frame data (frames flow over GATT): restarting
167:             // the beacon every epoch is pure churn. Keep it running.
168:             if (advMode == "legacy-uuid(gatt)" && currentAdvSet != null) return
169:             try {
170:                 // Stop any prior advertising set
171:                 stopAdvertisingLocked()
172: 
173:                 val advertiser = adapter?.bluetoothLeAdvertiser ?: return
174: 
175:                 // Convert ms to interval units (0.625 ms each), clamp to valid range
176:                 val intervalUnits = ((advIntervalMs * 1000L) / 625L)
177:                     .toInt()
178:                     .coerceIn(INTERVAL_UNIT_MIN, INTERVAL_UNIT_MAX)
179: 
180:                 // Adapters that can't fit a whole frame (or can't do extended adv at all) get
181:                 // the legacy fallback: a connectable UUID-only beacon; frames flow over GATT.
182:                 if (!extendedAdvCapable()) {
183:                     startLegacyFallbackLocked(advertiser, intervalUnits)
184:                     return
185:                 }
186: 
187:                 val useCoded = codedPhy && codedPhySupported()
188:                 val phy = if (useCoded) BluetoothDevice.PHY_LE_CODED else BluetoothDevice.PHY_LE_1M
189:                 advMode = if (useCoded) "ext+coded" else "ext-1M"
190: 
191:                 val params = AdvertisingSetParameters.Builder()
192:                     .setLegacyMode(false)
193:                     .setConnectable(false)
194:                     .setScannable(false)
195:                     .setInterval(intervalUnits)
196:                     .setPrimaryPhy(phy)
197:                     .setSecondaryPhy(phy)
198:                     .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
199:                     .build()
200: 
201:                 val data = AdvertiseData.Builder()
202:                     // Advertise the service UUID too: the scanner's ScanFilter matches on the Service
203:                     // UUID AD field, which is distinct from the Service Data field carrying the frame.
204:                     // Without this the filtered scan sees nothing.
205:                     .addServiceUuid(PARCEL_UUID)
206:                     .addServiceData(PARCEL_UUID, frame)
207:                     .setIncludeDeviceName(false)
208:                     .build()
209: 
210:                 val cb = object : AdvertisingSetCallback() {
211:                     override fun onAdvertisingSetStarted(
212:                         advertisingSet: AdvertisingSet?,
213:                         txPower: Int,
214:                         status: Int
215:                     ) {
216:                         val ok = status == ADVERTISE_SUCCESS
217:                         var superseded = false
218:                         synchronized(advLock) {
219:                             advStartInFlight = false
220:                             if (advCallback === this) {
221:                                 currentAdvSet = if (ok) advertisingSet else null
222:                             } else {
223:                                 superseded = true
224:                             }
225:                         }
226:                         if (superseded) {
227:                             // Started after being replaced/stopped while in flight. Kill it
228:                             // immediately — an untracked set would otherwise stay on air
229:                             // forever re-broadcasting its stale frame.
230:                             try {
231:                                 adapter?.bluetoothLeAdvertiser?.stopAdvertisingSet(this)
232:                             } catch (e: Exception) {
233:                                 onDebug?.invoke("superseded adv set stop failed: ${e.message}")
234:                             }
235:                             onDebug?.invoke("adv set started after supersede — stopped it (zombie prevented)")
236:                             return
237:                         }
238:                         onDebug?.invoke("adv set started: status=$status (${if (ok) "ok" else "failed"})")
239:                         if (ok && advertisingSet != null) {
240:                             val pending = synchronized(advLock) {
241:                                 val p = pendingFrame
242:                                 pendingFrame = null
243:                                 p
244:                             }
245:                             if (pending != null) {
246:                                 // Full stop+start, NOT setAdvertisingData(). That API is the
247:                                 // one this file already documents as silently broken on real
248:                                 // stacks (see the note above the scan section): it killed the
249:                                 // set outright on a Samsung and no-opped on MIUI, both
250:                                 // returning success. Using it here meant a frame composed
251:                                 // during the async start window — hitting SEND while the
252:                                 // epoch-rollover start was in flight, which is the common
253:                                 // case — either never reached the air or left the stale frame
254:                                 // airing for the rest of the epoch, with the watchdog unable
255:                                 // to tell either had happened.
256:                                 //
257:                                 // Posted rather than called inline: we are on the BLE callback
258:                                 // thread and are about to stop the very set whose callback is
259:                                 // currently running.
260:                                 onDebug?.invoke("applying coalesced frame via full restart")
261:                                 mainHandler.post {
262:                                     startAdvertising(pending, codedPhy, advIntervalMs)
263:                                 }
264:                             }
265:                         }
266:                         if (status == ADVERTISE_FAILED_DATA_TOO_LARGE) {
267:                             synchronized(advLock) {
268:                                 stopAdvertisingLocked()
269:                                 adapter?.bluetoothLeAdvertiser?.let { startLegacyFallbackLocked(it, intervalUnits) }
270:                             }
271:                         }
272:                     }
273: 
274:                     override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet?) {
275:                         synchronized(advLock) {
276:                             if (advCallback === this && currentAdvSet == advertisingSet) {
277:                                 currentAdvSet = null
278:                             }
279:                         }
280:                         onDebug?.invoke("adv set stopped")
281:                     }
282:                 }
283:                 advCallback = cb
284:                 advStartInFlight = true
285: 
286:                 advertiser.startAdvertisingSet(params, data, null, null, null, cb)
287:             } catch (e: SecurityException) {
288:                 advStartInFlight = false
289:                 onDebug?.invoke("startAdvertising SecurityException: ${e.message}")
290:             } catch (e: Exception) {
291:                 advStartInFlight = false
292:                 onDebug?.invoke("startAdvertising exception: ${e.message}")
293:             }
294:         }
295:     }
296: 
297:     /**
298:      * Legacy-advertising fallback for adapters that can't carry the 226-byte frame.
299:      * Advertises a connectable UUID-only beacon; peers see the mesh UUID with no
300:      * service data and pull/push frames over the GATT plane instead.
301:      * Caller must hold advLock.
302:      */
303:     @SuppressLint("MissingPermission")
304:     private fun startLegacyFallbackLocked(
305:         advertiser: android.bluetooth.le.BluetoothLeAdvertiser,
306:         intervalUnits: Int
307:     ) {
308:         try {
309:             advMode = "legacy-uuid(gatt)"
310:             val params = AdvertisingSetParameters.Builder()
311:                 .setLegacyMode(true)
312:                 .setConnectable(true)
313:                 .setScannable(true)
314:                 .setInterval(intervalUnits)
315:                 .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
316:                 .build()
317:             val data = AdvertiseData.Builder()
318:                 .addServiceUuid(PARCEL_UUID)
319:                 .setIncludeDeviceName(false)
320:                 .build()
321:             val cb = object : AdvertisingSetCallback() {
322:                 override fun onAdvertisingSetStarted(
323:                     advertisingSet: AdvertisingSet?,
324:                     txPower: Int,
325:                     status: Int
326:                 ) {
327:                     val ok = status == ADVERTISE_SUCCESS
328:                     var superseded = false
329:                     synchronized(advLock) {
330:                         advStartInFlight = false
331:                         if (advCallback === this) {
332:                             currentAdvSet = if (ok) advertisingSet else null
333:                         } else {
334:                             superseded = true
335:                         }
336:                     }
337:                     if (superseded) {
338:                         try {
339:                             adapter?.bluetoothLeAdvertiser?.stopAdvertisingSet(this)
340:                         } catch (e: Exception) {
341:                             onDebug?.invoke("superseded legacy set stop failed: ${e.message}")
342:                         }
343:                         return
344:                     }
345:                     onDebug?.invoke("legacy adv started: status=$status (${if (ok) "ok" else "failed"})")
346:                 }
347: 
348:                 override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet?) {
349:                     synchronized(advLock) {
350:                         if (advCallback === this && currentAdvSet == advertisingSet) {
351:                             currentAdvSet = null
352:                         }
353:                     }
354:                     onDebug?.invoke("legacy adv stopped")
355:                 }
356:             }
357:             advCallback = cb
358:             advStartInFlight = true
359:             advertiser.startAdvertisingSet(params, data, null, null, null, cb)
360:         } catch (e: SecurityException) {
361:             advStartInFlight = false
362:             onDebug?.invoke("legacy adv SecurityException: ${e.message}")
363:         } catch (e: Exception) {
364:             advStartInFlight = false
365:             onDebug?.invoke("legacy adv exception: ${e.message}")
366:         }
367:     }
368: 
369:     /**
370:      * REMOVED (field-observed 2026-07-24): AdvertisingSet.setAdvertisingData() is broken on
371:      * real stacks. On a Samsung it silently killed the set (off air, no callback, handle
372:      * still valid); on MIUI it silently no-opped (set kept broadcasting stale data). Both
373:      * returned success, so neither the caller nor the watchdog could detect the failure.
374:      * Every epoch now uses a full stop+start via startAdvertising(), which both stacks
375:      * demonstrably honor — the peer hears those frames.
376:      */
377: 
378:     /**
379:      * [onFrame] fires for every mesh advertisement carrying a frame in service data.
380:      * [onLegacyPeer] fires for mesh-UUID scan results with NO service data — a peer whose radio
381:      * cannot do extended advertising and therefore needs the GATT fallback plane. Peers with
382:      * working extended advertising are NOT surfaced (connecting to them adds only radio churn).
383:      */
384:     @SuppressLint("MissingPermission")
385:     fun startScanning(
386:         lowLatency: Boolean,
387:         onLegacyPeer: ((BluetoothDevice, Int) -> Unit)? = null,
388:         onFrame: (ByteArray, Int) -> Unit
389:     ) {
390:         try {
391:             stopScanning()
392:             // Mark intent AFTER stopScanning (which clears it) so failure-restarts work.
393:             wantScanning = true
394:             lastLowLatency = lowLatency
395:             lastOnLegacyPeer = onLegacyPeer
396:             lastOnFrame = onFrame
397: 
398:             // No scanner: Bluetooth is off (or mid-toggle). wantScanning is already set, but
399:             // no scan was ever started, so no onScanFailed will ever arrive to trigger the
400:             // self-heal below — the mesh stayed permanently deaf until the service restarted.
401:             // MeshService calls startScanning exactly once, so "BT off at service start" or a
402:             // BT off/on toggle killed reception for the rest of the session. Retry instead.
403:             val scanner = adapter?.bluetoothLeScanner ?: run {
404:                 onDebug?.invoke("no BLE scanner (bluetooth off?) — retrying scan in 2s")
405:                 mainHandler.postDelayed({
406:                     val frameCb = lastOnFrame
407:                     if (wantScanning && frameCb != null) {
408:                         startScanning(lastLowLatency, lastOnLegacyPeer, frameCb)
409:                     }
410:                 }, 2_000L)
411:                 return
412:             }
413: 
414:             val filter = ScanFilter.Builder()
415:                 .setServiceUuid(PARCEL_UUID)
416:                 .build()
417: 
418:             val settings = ScanSettings.Builder()
419:                 .setLegacy(false)
420:                 .setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
421:                 .setScanMode(if (lowLatency) ScanSettings.SCAN_MODE_LOW_LATENCY else ScanSettings.SCAN_MODE_BALANCED)
422:                 .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
423:                 .build()
424: 
425:             // [legacyPeerEvents]: only the legacy scan may classify a UUID-without-service-data
426:             // result as a GATT-fallback peer. The extended scan also produces such results
427:             // (truncated/unsynced ext records, relay sets mid-teardown) from peers that have
428:             // WORKING extended advertising — GATT-connecting to them is pure radio churn and
429:             // on single-set-class controllers costs us our own advertising slot.
430:             fun callback(legacyPeerEvents: Boolean): ScanCallback = object : ScanCallback() {
431:                 private fun handle(result: ScanResult) {
432:                     val bytes = result.scanRecord?.getServiceData(PARCEL_UUID)
433:                     if (bytes != null) {
434:                         onFrame(bytes, result.rssi)
435:                     } else if (legacyPeerEvents) {
436:                         // Mesh UUID but no frame payload: extended-adv-incapable peer.
437:                         onLegacyPeer?.invoke(result.device, result.rssi)
438:                     }
439:                 }
440: 
441:                 override fun onScanResult(callbackType: Int, result: ScanResult) = handle(result)
442: 
443:                 override fun onBatchScanResults(results: MutableList<ScanResult>) {
444:                     for (result in results) handle(result)
445:                 }
446: 
447:                 override fun onScanFailed(errorCode: Int) {
448:                     onDebug?.invoke("scan failed: errorCode=$errorCode — restarting in 2s")
449:                     // A failed scan delivers nothing until restarted. Without this the
450:                     // mesh silently went deaf and presence decayed to 0.
451:                     mainHandler.postDelayed({
452:                         val frameCb = lastOnFrame
453:                         if (wantScanning && frameCb != null) {
454:                             startScanning(lastLowLatency, lastOnLegacyPeer, frameCb)
455:                         }
456:                     }, 2_000L)
457:                 }
458:             }
459:             val extendedCallback = callback(legacyPeerEvents = false)
460:             scanCallbacks += extendedCallback
461:             scanner.startScan(listOf(filter), settings, extendedCallback)
462: 
463:             // The extended scan above does not return legacy advertisements.  Start a second
464:             // legacy-only scan so UUID-only GATT fallback beacons are actually discovered.
465:             // (Android's setLegacy(true) explicitly restricts results to Bluetooth 4.2-style
466:             // advertisements.)  The callbacks are distinct so each scan can be stopped cleanly.
467:             val legacySettings = ScanSettings.Builder()
468:                 .setLegacy(true)
469:                 .setScanMode(if (lowLatency) ScanSettings.SCAN_MODE_LOW_LATENCY else ScanSettings.SCAN_MODE_BALANCED)
470:                 .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
471:                 .build()
472:             val legacyCallback = callback(legacyPeerEvents = true)
473:             scanCallbacks += legacyCallback
474:             scanner.startScan(listOf(filter), legacySettings, legacyCallback)
475:             // Only scan FAILURES were logged, so a successfully-started scan that then
476:             // delivers nothing (the classic "location services off" case, which returns no
477:             // results and no error) was indistinguishable from a scan that never started.
478:             onDebug?.invoke(
479:                 "scan started: 2 scans (extended + legacy) mode=" +
480:                     (if (lowLatency) "LOW_LATENCY" else "BALANCED") + " " + scanEnvironment()
481:             )
482:         } catch (e: SecurityException) {
483:             onDebug?.invoke("startScanning SecurityException: ${e.message}")
484:         }
485:     }
486: 
487:     /** B8: true while a hardware relay slot is free. The service's relay queue drains only
488:      *  when this holds — frames WAIT for a slot instead of being silently dropped. */
489:     fun relayCapacityAvailable(): Boolean = activeRelaySets.get() < 2
490: 
491:     /**
492:      * One-shot relay advertisement. [codedPhy] (C6): honor the configured PHY so relayed
493:      * frames reach the same long-range frontier as originations — previously relays were
494:      * hardcoded to 1M and died at the edge of coded-PHY range.
495:      *
496:      * Returns true when a relay advertisement was actually started. False means this
497:      * adapter cannot air the frame; the caller's GATT-plane relay is then the only path.
498:      */
499:     @SuppressLint("MissingPermission")
500:     fun advertiseRelayOnce(frame: ByteArray, durationMs: Long, codedPhy: Boolean = false): Boolean {
501:         // P6: a legacy-only adapter cannot carry 226 bytes in an advertisement. Attempting it
502:         // burns a relay slot for durationMs and fails silently. Say so and let GATT carry it.
503:         if (!extendedAdvCapable()) {
504:             onDebug?.invoke("relay over-the-air skipped: adapter has no extended advertising (GATT plane only)")
505:             return false
506:         }
507:         if (activeRelaySets.get() >= 2) {
508:             onDebug?.invoke("relay skipped: 2 relay sets already active")
509:             return false
510:         }
511:         var counted = false
512:         try {
513:             val advertiser = adapter?.bluetoothLeAdvertiser ?: return false
514:             val useCoded = codedPhy && codedPhySupported()
515:             val phy = if (useCoded) BluetoothDevice.PHY_LE_CODED else BluetoothDevice.PHY_LE_1M
516:             val params = AdvertisingSetParameters.Builder()
517:                 .setLegacyMode(false)
518:                 .setConnectable(false)
519:                 .setScannable(false)
520:                 .setInterval(AdvertisingSetParameters.INTERVAL_LOW)
521:                 .setPrimaryPhy(phy)
522:                 .setSecondaryPhy(phy)
523:                 .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
524:                 .build()
525:             val data = AdvertiseData.Builder()
526:                 .addServiceUuid(PARCEL_UUID)
527:                 .addServiceData(PARCEL_UUID, frame)
528:                 .setIncludeDeviceName(false)
529:                 .build()
530:             // One-shot slot release, shared by the failure path and the scheduled stop.
531:             val released = java.util.concurrent.atomic.AtomicBoolean(false)
532: 
533:             // The old callback was `object : AdvertisingSetCallback() {}` — an empty override
534:             // set that swallowed every failure status. A relay that never reached the air was
535:             // indistinguishable from one that did.
536:             val cb = object : AdvertisingSetCallback() {
537:                 override fun onAdvertisingSetStarted(
538:                     advertisingSet: AdvertisingSet?,
539:                     txPower: Int,
540:                     status: Int
541:                 ) {
542:                     if (status != ADVERTISE_SUCCESS) {
543:                         onDebug?.invoke("relay adv rejected by stack: status=$status")
544:                         // Nothing went on air, so there is no set to stop — just free the
545:                         // slot now instead of holding it for the whole window.
546:                         if (released.compareAndSet(false, true)) activeRelaySets.decrementAndGet()
547:                         return
548:                     }
549:                     // Started successfully — but if the stop timer has ALREADY fired, the
550:                     // stopAdvertisingSet() it issued raced ahead of this callback and stopped
551:                     // nothing. The set is coming up right now with no one tracking it: it
552:                     // would re-air its 2-second relay frame indefinitely and hold a hardware
553:                     // advertising slot until process death. Kill it here.
554:                     if (released.get()) {
555:                         try {
556:                             adapter?.bluetoothLeAdvertiser?.stopAdvertisingSet(this)
557:                         } catch (e: Exception) {
558:                             onDebug?.invoke("late relay set stop failed: ${e.message}")
559:                         }
560:                         onDebug?.invoke("relay set started after its stop — stopped it (zombie prevented)")
561:                     }
562:                 }
563:             }
564:             activeRelaySets.incrementAndGet()
565:             counted = true
566:             advertiser.startAdvertisingSet(params, data, null, null, null, cb)
567:             mainHandler.postDelayed({
568:                 if (released.compareAndSet(false, true)) {
569:                     activeRelaySets.decrementAndGet()
570:                     try {
571:                         advertiser.stopAdvertisingSet(cb)
572:                     } catch (e: Exception) {
573:                         onDebug?.invoke("relay stop failed: ${e.message}")
574:                     }
575:                 }
576:             }, durationMs)
577:             return true
578:         } catch (e: SecurityException) {
579:             if (counted) activeRelaySets.decrementAndGet()
580:             onDebug?.invoke("relay adv denied: ${e.message}")
581:             return false
582:         } catch (e: Exception) {
583:             if (counted) activeRelaySets.decrementAndGet()
584:             onDebug?.invoke("relay adv failed: ${e.message}")
585:             return false
586:         }
587:     }
588: 
589:     @SuppressLint("MissingPermission")
590:     fun advertisePrivateOnce(
591:         frame: ByteArray,
592:         durationMs: Long,
593:         restoreFrame: ByteArray,
594:         codedPhy: Boolean,
595:         advIntervalMs: Long,
596:         onRestored: () -> Unit
597:     ): Boolean {
598:         if (privateAdvActive) {
599:             onDebug?.invoke("private adv skipped: one already active")
600:             return false
601:         }
602:         /*
603:          * Do not open a second AdvertisingSet here.  Most phone controllers expose one set, so
604:          * the previous implementation successfully sealed the private frame but could not put it
605:          * on air while the regular public advertisement was active.  Reuse that set instead —
606:          * with a FULL stop+start, because setAdvertisingData() is silently broken on real
607:          * stacks (see note above).  In legacy/GATT mode startAdvertising is a no-op (beacon
608:          * already running) and MeshService transports the frame over GATT.
609:          */
610:         privateAdvActive = true
611:         startAdvertising(frame, codedPhy, advIntervalMs)
612:         onDebug?.invoke("private frame using primary advertising set")
613:         val restore = Runnable {
614:             synchronized(advLock) { privateRestore = null }
615:             privateAdvActive = false
616:             startAdvertising(restoreFrame, codedPhy, advIntervalMs)
617:             onRestored()
618:         }
619:         synchronized(advLock) { privateRestore = restore }
620:         mainHandler.postDelayed(restore, durationMs)
621:         return true
622:     }
623: 
624:     @SuppressLint("MissingPermission")
625:     fun stop() {
626:         stopAdvertising()
627:         stopScanning()
628:     }
629: 
630:     @SuppressLint("MissingPermission")
631:     fun stopAdvertising() {
632:         synchronized(advLock) {
633:             // Teardown, not a restart: drop any pending private-window restore so it cannot
634:             // put the radio back on air afterwards. Deliberately NOT done inside
635:             // stopAdvertisingLocked(), which startAdvertising() calls on every normal
636:             // stop+start — cancelling there would strand the private frame on air forever.
637:             cancelPrivateRestoreLocked()
638:             stopAdvertisingLocked()
639:         }
640:     }
641: 
642:     /** Caller must hold advLock. */
643:     private fun cancelPrivateRestoreLocked() {
644:         privateRestore?.let {
645:             mainHandler.removeCallbacks(it)
646:             onDebug?.invoke("private restore cancelled by teardown")
647:         }
648:         privateRestore = null
649:         // Reset unconditionally: this was never cleared on stop, so a stop/start cycle
650:         // inside one private window left the flag set and refused every later private send.
651:         privateAdvActive = false
652:     }
653: 
654:     /** Caller must hold advLock. */
655:     @SuppressLint("MissingPermission")
656:     private fun stopAdvertisingLocked() {
657:         try {
658:             pendingFrame = null
659:             val cb = advCallback
660:             if (cb != null) {
661:                 adapter?.bluetoothLeAdvertiser?.stopAdvertisingSet(cb)
662:             }
663:             advCallback = null
664:             currentAdvSet = null
665:             advStartInFlight = false
666:             advMode = "off"
667:         } catch (e: SecurityException) {
668:             onDebug?.invoke("stopAdvertising SecurityException: ${e.message}")
669:         } catch (e: Exception) {
670:             onDebug?.invoke("stopAdvertising exception: ${e.message}")
671:         }
672:     }
673: 
674:     @SuppressLint("MissingPermission")
675:     private fun stopScanning() {
676:         wantScanning = false
677:         try {
678:             // Clear the callback list even when the scanner is gone (BT off): the old early
679:             // return left stale callbacks accumulating across every off/on cycle.
680:             val scanner = adapter?.bluetoothLeScanner
681:             if (scanner != null) {
682:                 for (cb in scanCallbacks) scanner.stopScan(cb)
683:             }
684:             scanCallbacks.clear()
685:         } catch (e: SecurityException) {
686:             onDebug?.invoke("stopScanning SecurityException: ${e.message}")
687:             scanCallbacks.clear()
688:         }
689:     }
690: 
691:     /**
692:      * True while our own frame is believed to be on air (or a start is in flight). The
693:      * controller can reclaim a hardware advertising set (relay bursts, GATT connections,
694:      * stack hiccups) — the service watchdog uses this to re-advertise within ~1 s instead
695:      * of waiting for the next epoch rollover. Counting in-flight starts is essential:
696:      * without it the watchdog fired inside the async start window and double-started.
697:      *
698:      * KNOWN BLIND SPOT (deliberate): [currentAdvSet] is cleared only by
699:      * onAdvertisingSetStopped or an explicit stop. A controller that reclaims the set with
700:      * no callback leaves this reporting true forever, so the watchdog cannot see it. Not
701:      * papered over with a staleness timer: rebuildAndAdvertise already does an unconditional
702:      * full stop+start every epoch, which bounds the dead air at one epoch, and a timer that
703:      * restarts more often than that would cost more airtime and battery than it recovers.
704:      */
705:     fun advertisingActive(): Boolean = advStartInFlight || currentAdvSet != null
706: }

(End of file - total 706 lines)
</content>
```

---

## Assistant (Plan · Kimi K3 (2x usage) · 180.2s)

_Thinking:_

Now I have MeshService.kt and BleRadio.kt fully read. Let me also check GattPlane.kt quickly for the communication path, and statemachine.rs relay logic. The security agents already covered GattPlane security; I need the communication/reliability angle: how GATT relays work, notify vs write, connection management.

Let me read GattPlane.kt myself, focusing on relay mechanics. Also let me check statemachine.rs relayFrame and the Rust relay logic to understand TTL handling for echoes.

Actually, let me think about what I need for the "communication problems" analysis:

1. The echo receipt path: origination → peer hears → peer relays once per epoch → we hear echo → receipt. Already analyzed by user (jitter fix). I should verify the code matches their description and identify remaining issues.

2. Key areas to trace:
   - relayFrame in Rust (statemachine.rs): TTL handling — LOCAL TTL=1 clobbered to 0 relay; regional decrement.
   - The dedup "once per epoch" relay limitation: a peer relays a given frame once per epoch. The echo path timing.
   - Relay queue: drain poll 250ms, jitter 350ms, relay air 2s window (advertiseRelayOnce 2000ms), INTERVAL_LOW interval (~100ms? INTERVAL_LOW = 0xA0 = 160 * 0.625 = 100ms).
   - Scanning: LOW_LATENCY, filter on service UUID. Two scans (extended + legacy).
   - GATT plane: connection-based fallback.
   - Private message path: 6s window, re-airings, no receipt for private (comment says "no relayed echo is ever recognised for them" — private frames are not registered with rememberOwnFrame).
   - messageRepeatEpochs = 3: origination repeats for 3 epochs.

Let me simulate scenarios mentally:

**Scenario A: Two phones, A sends LOCAL to B.**
- A composes text at epoch E. rebuildAndAdvertise(E, text) immediately (outgoingText collector). Frame TTL=1 (local), with witness.
- B's scanner hears it (advertised every 1000ms for up to ~10s, i.e., ~10 airings). Likely heard within 1-2s.
- B ingest: verify sig, K4 epoch check (±2), dedup FRESH, PoCP verify → display. B enqueues relay (LOCAL clobbered TTL=0 relay — wait, relayFrame for local: TTL 1 → relay once with TTL clobbered to 0).
- B's relay queue drain: within 250ms + jitter 350ms → advertiseRelayOnce for 2s.
- A hears its own echo: hash matches ownHashes → receipt "heard back once".
- A stops... no, LOCAL keeps re-originating every epoch until echo heard; after echo, sparse every 4th epoch.

Where can receipt be late?
- B's relay burst is 2s; A's scanner must catch it. A's own advertising teardown at epoch boundary doesn't stop scanning... scanning is continuous. So A should catch it. But the epoch-boundary collision: at rollover, A tears down adv set and B tears down its adv set; B's relay queue might fire exactly when A... actually scanning isn't torn down. The collision is on the advertising side: two devices' relay bursts + originations in the same RF window collide. With only 2 devices and ~10 airings, origination survives; the single relay burst might not if it collides with A's epoch-rollover restart.

Actually wait — there's another important subtlety: **dedup "once per epoch"** means B relays A's frame only once *per epoch of the frame*. B hears A's frame re-originated at epoch E+1 (new frame, new hash) — that's a fresh hash, so B relays again. So actually each epoch's re-origination gets one relay from B. So A has 3 epochs × 1 relay each = 3 chances. The user's analysis matches: "Three epochs of messageRepeatEpochs gives you three coin flips."

**Remaining communication problems to identify:**

Let me enumerate systematically by walking the data paths:

1. **Echo receipt for BROADCAST stops re-origination mid-epoch?** Line 992-999: on echo, if not LOCAL, set outgoingSetAtEpoch = ownEpoch+1-repeat, so at next rollover the message expires (epoch - setAt >= messageRepeatEpochs). OK by design.

2. **Private messages: no receipt at all.** Comment at 466-469: "Private gets one tick only: private frames are not registered with rememberOwnFrame, so no relayed echo is ever recognised for them and a second tick could never honestly appear." So a private sender never knows if anyone relayed it. Also private frame gets ON_AIR tick but never ECHOED. Communication gap: private delivery is fully unconfirmed. Is that a "problem"? It's a design limitation worth noting. Could register own private frame hash for echo — they insert into dedup (line 477) but not rememberOwnFrame. Why not? Maybe because the private frame hash... could be echoed. Not registering means no receipt. The comment claims a second tick "could never honestly appear" — but that's only true because they chose not to register. Actually there might be a subtle issue: the private frame re-airs verbatim 2 more times; an echo of it would be recognizable if registered. This is a real UX/communication gap: private messages have zero delivery signal, worse than public.

3. **reairPendingPrivate window collision with epoch-rollover origination**: at epoch rollover, the epoch loop calls reairPendingPrivate AND launches rebuildAndAdvertise with TX jitter (0-1200ms). reairPendingPrivate calls advertisePrivateOnce → startAdvertising(private frame) then restore after 6s. The jittered rebuildAndAdvertise fires 0-1200ms later and calls radio.startAdvertising(currentPublicFrame)... but wait, rebuildAndAdvertise checks `if (!privateTransportActive)` before startAdvertising (line 1408). reairPendingPrivate sets privateTransportActive=true before the jittered rebuild fires? Order in epoch loop: `launch { delay(jitter); rebuildAndAdvertise }` is launched at line 646, and `reairPendingPrivate(epoch, cfg)` at line 670 runs synchronously right after. Since the rebuild has delay 0-1200ms and reairPendingPrivate runs immediately, privateTransportActive=true will usually be set before rebuild runs → rebuild skips startAdvertising (just updates currentPublicFrame). Good — but `gattPlane.currentFrame = frame` is also skipped? No — line 1408-1412: the whole block including gattPlane.currentFrame is inside `if (!privateTransportActive)`. So currentPublicFrame variable IS updated (line 1399) but GATT plane keeps the private frame. OK that's intended.

   But what about the reverse race: advertisePrivateOnce's restore runnable (after 6s window) calls startAdvertising(restoreFrame) — restoreFrame is currentPublicFrame captured at send time, which may be one or two epochs stale by restore time. The epoch-rollover rebuild will fix within an epoch. Minor.

   Also: if a private send is being sealed (VDL solve, seconds) across an epoch boundary, privateTransportActive is false during solve; no conflict.

4. **The relay "once per epoch" + relay repeats**: relayRepeatsForDensity gives 3 airings when ≤1 neighbor. The repeats are re-queued with RELAY_REPEAT_SPACING_MS=2600ms spacing. But here's a subtle issue: the *echo* back to the originator — the originator hears the first airing; fine.

5. **B8 relay queue eviction**: maxByOrNull priority — evicts the worst-priority task. Priority 0 = LOCAL echo... wait, enqueueRelay maps msgType 1 → prio 0. Fine.

6. **GATT plane relayOnce**: relays over GATT. Need to read GattPlane to understand.

7. **The echo depends on the peer's relay happening while we still listen** — scanning is continuous, fine.

8. **Dedup insert of own frame at origination** (line 1406): checkAndInsertEpoch(ownHash, epoch). This means when a *different* peer's relay of our frame comes back, dedup = DUPLICATE → return... but claimOwnEcho runs BEFORE dedup gate (line 976). Good. But: what if our frame is relayed back to us a *second* time (from another peer or next epoch)? claimOwnEcho: ownHashesAcked.add(hex) — second time returns null (already acked) → falls through to dedup gate → DUPLICATE → return. Fine.

   But here's an issue: **the first echo we hear might be an empty presence frame relay** vs text frame — they handle carriedText. OK.

9. **Race: ownHashes eviction vs late echo.** ownHashRetentionEpochs = messageRepeatEpochs+1 = 4 epochs. Relay path: 250ms poll + 2s air. Fine.

10. **B's relay of A's LOCAL frame: TTL clobbered to 0.** relayFrame (Rust) — for LocalImmediate TTL=1: relay with TTL 0. When A hears the echo, frameTtl == 0, so `direct` = (ttl == localTtl=1) → false. So the echo doesn't count as presence. Good — comment says relayed echo of own frame must not register us as our own neighbor.

11. **What about A hearing B's relay of A's frame and B simultaneously hearing A's relay of B's frame** — lockstep, addressed by jitter.

12. **Measurement/sketch timing at epoch boundary**: At rollover, A's new frame uses sketch from previous epoch if richer. The receiver's verifyPocpAcrossRollover tries frameEp-1, frameEp, frameEp+1. There's a subtle issue: marks rotate every epoch, so a sketch built from epoch N marks (hashed with seed... let me check Measurement.localSketch) — the localSketch buckets by epoch. If A sends at epoch E using sketch from E-1 (richer), witEpoch=E-1. B receives at own epoch E or E+1. B tries candidates of *frameEp* (E, E-1, E+1) with B's local sketches for those buckets. The witness seed must match: verify uses `ep` as seed for pocpVerifyWitnessLocal(sketch, divSketch, ep, prefix, wit). Frame's witness was computed with seed = witEpoch = E-1. B tries ep=E-1 → matches. Good.

    But wait — there's a deeper issue: **B's sketch for epoch E-1 contains B's heard marks from epoch E-1, while A's sketch contains A's heard marks from epoch E-1**. For Jaccard to pass τ=0.3, they need overlap of heard marks. In a 2-phone scenario: A's sketch = {mark_A(self), mark_B}, B's sketch = {mark_B(self), mark_A}. Overlap = {mark_A, mark_B}, union = same → Jaccard 1.0. Good (self-inclusion fixed this).

    In a 3-phone scenario with A,B close and C far... fine.

13. **The relay-once-per-epoch dedup interacts with relay repeats**: relayRepeatsForDensity gives 3 airings of the same frame spaced 2.6s. Peers dedup on hash — extra airings are DUPLICATE for peers that heard the first, so they don't relay again. Only helps if first airing was missed. OK.

14. **GATT relay path**: need to read. Let me read GattPlane.kt.

15. **advertiseRelayOnce duration 2000ms with INTERVAL_LOW** (~100ms interval → ~20 airings). Fine.

16. **Scanning filters**: setServiceUuid(PARCEL_UUID) — matches ads containing the service UUID AD structure. Extended advertising on coded PHY: scan PHY_LE_ALL_SUPPORTED. Legacy scan separate. Issue: on some phones, two simultaneous scans with filters... fine presumably.

17. **Timing: relay drain loop polls every 250ms when idle; when a task is drained it does jitter delay 0-350ms, then advertiseRelayOnce (2s), gattPlane.relayOnce, then possibly re-queue repeats; if !onAir delay 250ms. Then loop continues immediately (no delay at top when task drained!).** Look: `if (task != null) { ... if (!onAir) delay(250L) } else { delay(250L) }`. So when onAir=true, the loop iterates immediately. relayCapacityAvailable() = activeRelaySets < 2. So it can drain a second task immediately (2 sets), then blocks until the 2s timers release. OK — bounded by hardware slots.

18. **Echo when A is also relaying**: A's relay bursts for B's frames might collide with B's relay bursts for A's frames — jitter decorrelates by 0-350ms. With 2.6s spacing for repeats. OK.

19. **Private frame relay over GATT + advertisePrivateOnce reuses the primary set — during the 6s private window, our public frame is off air (documented). Neighbors' sketches lose us → their LOCAL alerts to us may CellMismatch? No — their sketch is built from marks heard in current/previous epoch; 6s out of 10s epoch means we might be absent from their current-epoch bucket but present in previous. verifyPocpAcrossRollover tries both. Mitigated.

20. **VDL solve time vs ±2 gate** — handled with re-stamping, 3 attempts.

21. **Relay of private frames**: enqueueRelay(it, 3) → priority 2. relayFrame(bytes) for private: TTL decrement. Private frame TTL=8 regional-style. OK.

22. **What happens when two phones' clocks differ by, say, 4s (epochMs=10s)?** K4 gate: |diff| ≤ 2 epochs. Frame from fast clock arrives when own epoch is E, frameEp=E+1 (if sent at start of their E+1 while we're at end of E). diff=+1 OK. But PoCP: witness seed = witEpoch (E or E-1). We try frameEp±1. OK. Dedup: keyed by frameEp. Fine.

    But here's a real one: **clock skew + TTL-8 regional relay chain**: each relay preserves frameEp. K4 checks against *our* epoch. A frame stamped at E can travel for at most... at E+2 it's dropped. So multi-hop propagation is limited to ~3 epochs = 30s of mesh travel time! TTL=8 hops must complete within 3 epochs. In a sparse crowd where a relay opportunity comes once per epoch (or less — a phone in pocket scanning balanced?), frames die after ~3 epochs regardless of TTL. This is a significant communication limitation: **the K4 freshness gate caps mesh propagation at ~3 epochs (30s) end-to-end, while TTL allows 8 hops**. In a crowd that's fine (dense relays), but in sparse/bridge scenarios (one relay node carrying between two groups, walking), a frame can only survive 30s. Combined with relay-once-per-epoch dedup, a store-and-forward scenario (phone crosses a gap over minutes) fails entirely: when the carrier reaches the far group 2 minutes later, every receiver drops the frame as stale. No store-and-forward. Worth noting as a communication limitation — maybe by design (danger alerts are time-sensitive), but it breaks the "regional" 8-hop claim unless hops are fast.

23. **Dedup "insert at end, not at admission"** — they check at admission, insert after acting. Two copies arriving concurrently on BLE binder + GATT thread both pass check → double display? appendOrMergeIncoming merges same text, so UI collapse. Double relay: both enqueue relay tasks → two relay bursts of same frame. Minor.

24. **The `relayFrame` in Rust for LOCAL: TTL 1 → clobber 0, meaning relay once.** But does the receiver display the *relayed* LOCAL copy? Relayed LOCAL has TTL=0. At ingest: `direct` check: msgType 1 → frameTtl == localTtl(1)? TTL=0 → not direct. Then display gate: displayOk requires witness valid. A relayed LOCAL from our own cell: witness MAC valid, Jaccard... the originator's divSketch vs our sketch — still valid. So displayOk=true, and the message displays — good, that's the intended 1-hop extension? Wait, README says Tier-1 is 1-hop direct only, relayed exactly once with TTL clobbered to 0 "so the hop bound holds". Hmm, TTL=0 clobbered means it can't be relayed further. But the *first* relay is by the receiver. So a LOCAL alert travels: originator → direct neighbors (display) → each neighbor relays once with TTL=0 → second ring (display?) → TTL 0 stops further relay. So LOCAL actually reaches 2 hops of RF range. By design ("Relayed exactly once").

25. **Reflection receipt timing analysis** (user's question): already answered. Let me verify specifics:
    - advIntervalMs=1000: their analysis says frame airs ~10 times per epoch. Advertising interval 1000ms → ~10 adverts per 10s epoch. Yes.
    - Relay: one 2s burst per epoch per peer.
    - Jitter fixes lockstep.

26. **What about the case where A hears its own echo but B never displayed?** B relays before display? Ingest order: for public path, displayOk computed, then relay if displayOk||relayOnly, then dedup insert, then display. B relays regardless of display success (relayOnly frames relayed). So echo ≠ display. Documented ("not proof of delivery").

27. **GATT plane: relayOnce** — writes to connected peripherals? Need to read GattPlane.

28. **messageRepeatEpochs=3 for BROADCAST with TTL=8**: originator re-originates 3 epochs then stops (or stops on echo). Relay chain: each relay relays once per epoch per hash. Since each epoch's re-origination is a NEW hash (new mark, new signature...), each gets relayed independently. OK.

29. **The empty presence frame every epoch**: every phone originates a frame every epoch even with no text. With N phones in a dense crowd, every phone relays every other phone's presence frame once per epoch?? That's O(N²) relay traffic per epoch! Wait — relay gate: `if (displayOk || relayOnly)` → relay. Presence frame: empty text. displayOk requires witness valid → yes (co-present). So yes, every presence frame gets relayed by every phone once per epoch. In a crowd of 50, each phone originates 1 frame/epoch and relays up to 49 others = ~50 relay bursts/epoch/phone × 2s each = 100s of airtime needed per 10s epoch! Relay queue cap 64, eviction... This is a **broadcast storm / scalability problem**: presence frames are relayed, causing O(N²) relay traffic. Hmm, but is that true? relayFrame in Rust — does it relay frames with TTL... Regional TTL=8, presence frame has TTL=8 (tier BROADCAST default). Empty-body frames: does relayFrame relay empty frames? Let me check statemachine.rs relay_decision. I need to read it. If empty presence frames relay mesh-wide, the whole mesh floods with everyone's presence beacons every epoch — that's the storm. Maybe there's a guard (e.g., only relay frames with non-empty body, or TTL-based). Must check.

    Actually wait — presence frames: `MeshState.outgoingTier.value == SendTier.LOCAL` default? What is the default tier? Let me check MeshState.outgoingTier default. If default tier is LOCAL, presence frames are TTL=1, relayed once by direct neighbors with TTL=0 — bounded. If BROADCAST default, presence floods. Need to check MeshState.

30. **Dedup per-epoch bucket cap 1024** — in a crowd of >1024 phones each originating one frame/epoch, legitimate frames get BucketFull → dropped. Scalability ceiling: the mesh collapses (no display, no relay) beyond ~1024 distinct frames per epoch per node. Combined with #29 (presence relay storm), a dense protest of >1000 phones breaks the dedup table. Hmm, but with N phones each phone hears N originations + N*(neighbors) relayed copies... all with distinct hashes? Relayed copies have the same hash as origin (TTL excluded). So distinct hashes per epoch ≈ N (one presence frame per phone) + alerts. So >1024 phones in RF+relay range → bucket full. But relayed copies arriving are DUPLICATE → dropped before insert? No wait — dedup counts each *distinct hash* once. Duplicates don't add. So bucket fills at ~1024 distinct frames/epoch. A protest of 5000 phones partitioned across cells... each cell hears maybe hundreds. Probably OK. But presence relay (#29) means frames propagate... let me check relayFrame for empty body first.

Let me read statemachine.rs and MeshState.kt, GattPlane.kt.

Also **scenario: phone in pocket, screen off, Doze** — wakelock held, OK.

**Scenario: BT toggle off/on** — scanning self-heals via 2s retry; advertising watchdog 5s. OK.

**Scenario: two phones paired, private chat, one restarts app** — seed changes per service start (line 302: new random seed each start!). `seed = ByteArray(32).also { SecureRandom().nextBytes(it) }` — device seed is per service start. So marks change entirely on restart — presence continuity lost (mark rotation is per epoch anyway). Pair keys persist (PairStore). OK.

**Ratchet desync scenario**: A sends at epoch E, B offline for an hour → B's chain at E_old, frame at E_new; candidateKeys fast-forwards span ≤8192. If span >8192, private messages undecryptable forever?? fastForwardChains steps chains each epoch so the phone's stored chainEpoch follows wall clock — so span stays ≤2. But if app killed for a day: on restart, fastForwardChains(currentEpoch) — span = epochs in a day = 8640 epochs at 10s > 8192 cap → keyForSend fails ("ratchet failed") → private messaging bricked until re-pair? PairStore.kt F12 analysis mentioned anchor clamp. Let me check PairStore.fastForwardChains to see how it handles span > 8192. The security agent F12 mentioned malicious anchorEpoch=0 bricking. For the natural case: app off for 24h → chainEpoch behind by 8640 → ratchet cap 8192 → cannot catch up → private messaging dead until re-pairing. Is there handling? Need to read PairStore.

Hmm wait, actually — the ratchet cap is on a single ratchet call span. fastForwardChains might loop in chunks of ≤8192? Let me read PairStore.kt.

Let me now read: statemachine.rs (relay logic), MeshState.kt (defaults, appendOrMergeIncoming), PairStore.kt (ratchet, keyForSend, candidateKeys), GattPlane.kt (relay mechanics). That's a lot of reading but necessary for scenario simulation. Let me read statemachine.rs and MeshState.kt in parallel first.

Now let me examine the relay state machine, state defaults, and pairing/ratchet logic to trace communication flows end-to-end.

**Tool: read**

**Input:**
```json
{
  "filePath": "/home/Likun/projects/cockroachat/mesh-core/src/statemachine.rs"
}
```

**Output:**
```
<path>/home/Likun/projects/cockroachat/mesh-core/src/statemachine.rs</path>
<type>file</type>
<content>
1: //! `statemachine` — relay decisions + dedup set for the message pipeline.
2: //!
3: //! The live ingest pipeline (parse -> verify -> decide, invariant #2) is
4: //! `MeshService.ingestFrame` in the platform shim, driven over FFI. This module holds the
5: //! two pieces the core owns: the epoch-aware bounded dedup set and the pure relay decision.
6: //! The old Rust-side `Engine` (a second, divergent ingest pipeline) was deleted — one
7: //! pipeline only, or the two will drift.
8: 
9: use crate::codec::{self, MsgType, FRAME_LEN};
10: use crate::crypto;
11: use crate::message::DEFAULT_TTL_REGIONAL;
12: use crate::vdl;
13: use std::collections::{HashMap, VecDeque};
14: 
15: /// Per-epoch insertion sub-cap (C8). A flood of distinct valid frames sharing one epoch
16: /// bucket can fill at most this many slots, so a single-epoch storm cannot evict the
17: /// legitimate hashes of the other live epochs (replay/eviction-window mitigation).
18: pub const EPOCH_BUCKET_CAP: usize = 1024;
19: 
20: /// Outcome of a dedup admission check.
21: ///
22: /// P-DoS: the old API collapsed all three cases into `bool`, so the shim could not tell
23: /// "already seen" from "this epoch's bucket is full". Once 1024 distinct frames carrying one
24: /// epoch had been ingested, every further frame stamped with that epoch was refused and the
25: /// caller treated it as a duplicate — no display, no relay, no measurement, for the rest of
26: /// the epoch. Signing 1024 frames is milliseconds of work, so an anti-eviction mitigation
27: /// doubled as a complete per-epoch silencing DoS. Callers must now distinguish the cases.
28: #[derive(Debug, Clone, Copy, PartialEq, Eq)]
29: pub enum DedupVerdict {
30:     /// Not seen before, and admissible.
31:     Fresh,
32:     /// Already present in the set.
33:     Duplicate,
34:     /// Not seen before, but this epoch's bucket is at [`EPOCH_BUCKET_CAP`].
35:     BucketFull,
36: }
37: 
38: /// Bounded FIFO-evicting dedup set with time-decaying epoch awareness (E4).
39: ///
40: /// Stores up to `cap` frame (hash, epoch) pairs.  When the set is full and a new hash arrives,
41: /// the oldest entry is evicted before insertion.  [`check_and_insert_epoch`] additionally evicts
42: /// entries whose epoch is more than 2 behind the provided epoch, providing time-decaying behavior
43: /// without relying solely on capacity-based eviction (window of ~3 epochs).
44: ///
45: /// The plain [`check_and_insert`] delegates with epoch 0 (no epoch-based eviction — only
46: /// capacity-based FIFO).  Use [`check_and_insert_epoch`] when the caller has the frame epoch.
47: pub struct Dedup {
48:     /// hash → epoch (fast lookup + epoch metadata for time-decaying eviction).
49:     seen: HashMap<[u8; 16], u32>,
50:     order: VecDeque<[u8; 16]>,
51:     cap: usize,
52:     /// epoch → live entry count (C8 sub-cap accounting; epoch-0 sentinel entries excluded).
53:     epoch_counts: HashMap<u32, usize>,
54: }
55: 
56: impl Dedup {
57:     /// Create a new `Dedup` with the given capacity.  A `cap` of 0 is clamped to 1.
58:     pub fn new(cap: usize) -> Self {
59:         let cap = cap.max(1);
60:         Dedup {
61:             seen: HashMap::with_capacity(cap),
62:             order: VecDeque::with_capacity(cap),
63:             cap,
64:             epoch_counts: HashMap::new(),
65:         }
66:     }
67: 
68:     /// Returns `true` iff `hash` was NOT seen before (fresh).  Delegates to
69:     /// [`check_and_insert_epoch`] with `epoch = 0` (no time-decaying eviction).
70:     pub fn check_and_insert(&mut self, hash: [u8; 16]) -> bool {
71:         self.check_and_insert_epoch(hash, 0)
72:     }
73: 
74:     /// Returns `true` iff `hash` was NOT seen before (fresh).
75:     ///
76:     /// Before inserting, evicts entries whose stored epoch is more than 2 behind `epoch`
77:     /// (time-decaying window of ~3 epochs).  Entries stored with epoch 0 (the backwards-compatible
78:     /// sentinel) are never purged by the time-decay path, only by capacity eviction.
79:     ///
80:     /// C8: insertions into an epoch bucket that already holds [`EPOCH_BUCKET_CAP`] entries are
81:     /// REFUSED (returns false, nothing evicted) — a single-epoch flood cannot push legitimate
82:     /// hashes of other epochs out of the global FIFO.
83:     ///
84:     /// Then, if the set is already at `cap`, the single oldest entry is evicted (FIFO).
85:     pub fn check_and_insert_epoch(&mut self, hash: [u8; 16], epoch: u32) -> bool {
86:         match self.check_epoch(hash, epoch) {
87:             DedupVerdict::Fresh => {
88:                 self.insert_epoch(hash, epoch);
89:                 true
90:             }
91:             _ => false,
92:         }
93:     }
94: 
95:     /// Admission check WITHOUT inserting. Runs the time-decay purge, then reports whether
96:     /// `hash` is fresh, a duplicate, or blocked by the per-epoch sub-cap.
97:     ///
98:     /// Split out from [`check_and_insert_epoch`] so the shim can defer the insert until it
99:     /// has actually acted on the frame. Marking a frame seen before deciding whether to
100:     /// display it meant a frame that transiently failed verification (e.g. an empty local
101:     /// sketch at the start of an epoch) was suppressed for the whole ~3-epoch dedup window:
102:     /// every retransmission of those exact bytes hit the seen-set and was dropped, so the
103:     /// loss could never self-heal.
104:     pub fn check_epoch(&mut self, hash: [u8; 16], epoch: u32) -> DedupVerdict {
105:         // Purge entries older than epoch-2 (time-decaying eviction).
106:         // Entries with epoch 0 (legacy sentinel) are skipped so old callers that don't
107:         // supply epoch don't get unexpected eviction.
108:         while let Some(oldest_hash) = self.order.front().copied() {
109:             // R7: saturating_sub — epochs arrive from the wire; `oldest_epoch + 2` would
110:             // overflow on an adversarial u32::MAX epoch (panic in overflow-checked builds).
111:             match self.seen.get(&oldest_hash) {
112:                 Some(&oldest_epoch) if oldest_epoch != 0 && epoch.saturating_sub(oldest_epoch) > 2 => {
113:                     self.order.pop_front();
114:                     self.seen.remove(&oldest_hash);
115:                     self.decrement_bucket(oldest_epoch);
116:                 }
117:                 _ => break,
118:             }
119:         }
120: 
121:         if self.seen.contains_key(&hash) {
122:             return DedupVerdict::Duplicate;
123:         }
124: 
125:         // C8: per-epoch sub-cap — refuse without evicting other epochs' entries.
126:         if epoch != 0 && self.epoch_counts.get(&epoch).copied().unwrap_or(0) >= EPOCH_BUCKET_CAP {
127:             return DedupVerdict::BucketFull;
128:         }
129: 
130:         DedupVerdict::Fresh
131:     }
132: 
133:     /// Insert `hash` into the seen set, evicting the oldest entry if at capacity.
134:     /// Returns `false` if the hash was already present or the epoch bucket is full.
135:     /// Call after [`check_epoch`] has returned [`DedupVerdict::Fresh`] and the frame has
136:     /// been acted on.
137:     pub fn insert_epoch(&mut self, hash: [u8; 16], epoch: u32) -> bool {
138:         if self.seen.contains_key(&hash) {
139:             return false;
140:         }
141:         if epoch != 0 && self.epoch_counts.get(&epoch).copied().unwrap_or(0) >= EPOCH_BUCKET_CAP {
142:             return false;
143:         }
144: 
145:         // Cap-based eviction (FIFO).
146:         if self.order.len() >= self.cap
147:             && let Some(oldest) = self.order.pop_front()
148:             && let Some(oldest_epoch) = self.seen.remove(&oldest)
149:         {
150:             self.decrement_bucket(oldest_epoch);
151:         }
152: 
153:         self.seen.insert(hash, epoch);
154:         self.order.push_back(hash);
155:         if epoch != 0 {
156:             *self.epoch_counts.entry(epoch).or_insert(0) += 1;
157:         }
158:         true
159:     }
160: 
161:     fn decrement_bucket(&mut self, epoch: u32) {
162:         if epoch == 0 {
163:             return;
164:         }
165:         if let Some(c) = self.epoch_counts.get_mut(&epoch) {
166:             *c = c.saturating_sub(1);
167:             if *c == 0 {
168:                 self.epoch_counts.remove(&epoch);
169:             }
170:         }
171:     }
172: }
173: 
174: // Wire offset at which the witness field begins; VDL prefix is buf[0..WITNESS_PREFIX_END].
175: // Mirrors codec layout: body occupies bytes 38..102, pocp_wit occupies 102..118.
176: const WITNESS_PREFIX_END: usize = 102;
177: 
178: /// Relay decision for a single received frame, parameterised by VDL difficulty.
179: ///
180: /// Processing order (invariant #2): decode → sig verify → TTL cap/witness → decide.
181: /// The ephemeral Ed25519 pubkey is embedded at buf[118..150] so every relay can verify.
182: ///
183: /// Production callers use `relay_decision` (which supplies `vdl::VDL_DIFFICULTY_BITS`).
184: /// Tests use this helper directly with a low difficulty so they don't burn 2^22 hashes.
185: ///
186: /// Logic:
187: /// - All types: CLOBBER incoming TTL at byte 214 to max(DEFAULT_TTL_REGIONAL) (TTL cap, C1).
188: /// - `RegionalPropagated`: relay if ttl > 0 (decrement byte 214).
189: /// - `Private`: relay only if `vdl::verify` passes AND ttl > 0 (decrement byte 214).
190: /// - `LocalImmediate`: relay exactly once — any incoming ttl > 0 is CLOBBERED to 0, never
191: ///   decremented, so an adversary advertising ttl=255 still gets exactly one hop. The echo
192: ///   is the originator's receipt (send-and-listen); display stays PoCP-gated upstream.
193: pub fn relay_decision_with_difficulty(
194:     buf: &[u8; FRAME_LEN],
195:     difficulty_bits: u8,
196: ) -> Option<[u8; FRAME_LEN]> {
197:     let frame = codec::decode(buf).ok()?;
198: 
199:     // A1: verify Ed25519 signature using the embedded ephemeral pubkey.
200:     if !crypto::verify(&frame.pk, codec::signing_region(buf), &frame.sig) {
201:         return None;
202:     }
203: 
204:     match frame.msg_type {
205:         MsgType::RegionalPropagated => {
206:             // C1: cap incoming TTL to DEFAULT_TTL_REGIONAL (anti-flood).
207:             let ttl = buf[214].min(DEFAULT_TTL_REGIONAL);
208:             if ttl == 0 {
209:                 return None;
210:             }
211:             let mut out = *buf;
212:             out[214] = ttl - 1;
213:             Some(out)
214:         }
215:         MsgType::Private => {
216:             // C1: same TTL cap before VDL check.
217:             let raw_ttl = buf[214].min(DEFAULT_TTL_REGIONAL);
218:             if !vdl::verify(&buf[..WITNESS_PREFIX_END], &frame.pocp_wit, difficulty_bits) {
219:                 return None;
220:             }
221:             if raw_ttl == 0 {
222:                 return None;
223:             }
224:             let mut out = *buf;
225:             out[214] = raw_ttl - 1;
226:             Some(out)
227:         }
228:         MsgType::LocalImmediate => {
229:             // C1+: any incoming ttl > 0 relays exactly once, with TTL clobbered to 0.
230:             // Not decremented: an adversary setting ttl=255 gets the same single hop as
231:             // an honest ttl=1. TTL=0 on the wire → no further relay, hard bound.
232:             if buf[214] == 0 {
233:                 return None;
234:             }
235:             let mut out = *buf;
236:             out[214] = 0;
237:             Some(out)
238:         }
239:     }
240: }
241: 
242: // DEFAULT_TTL_REGIONAL is defined in crate::message — used as TTL cap here.
243: 
244: /// Pure relay decision for a single received frame.
245: ///
246: /// Production entry point; tests use `relay_decision_with_difficulty` with a low difficulty
247: /// so they don't burn 2^22 hashes.
248: ///
249: /// Decodes `buf` via the codec (returns `None` on any decode error).  Then:
250:     /// - `RegionalPropagated`: relay if ttl > 0 (decrement byte 214).
251: /// - `Private`: relay only if VDL witness passes at `vdl::VDL_DIFFICULTY_BITS` and ttl > 0.
252: /// - `LocalImmediate`: relay once with TTL clobbered to 0 (reflection receipt; anti-flood).
253: ///
254: /// # Caller contract
255: /// Invoke this only for frames that have already passed dedup (`Dedup::check_and_insert`
256: /// returned `true`).  Rebroadcast the returned buffer verbatim.
257: pub fn relay_decision(buf: &[u8; FRAME_LEN]) -> Option<[u8; FRAME_LEN]> {
258:     relay_decision_with_difficulty(buf, vdl::VDL_DIFFICULTY_BITS)
259: }
260: 
261: #[cfg(test)]
262: mod tests {
263:     use super::{
264:         relay_decision, relay_decision_with_difficulty, Dedup, DedupVerdict, EPOCH_BUCKET_CAP,
265:     };
266:     use crate::codec::{self, MsgType, FRAME_LEN};
267:     use crate::crypto;
268:     use crate::message;
269: 
270:     fn hash(tag: u8) -> [u8; 16] {
271:         [tag; 16]
272:     }
273: 
274:     /// Distinct 16-byte hash from a counter (for bucket-cap tests).
275:     fn hash_n(n: u32) -> [u8; 16] {
276:         let mut h = [0u8; 16];
277:         h[..4].copy_from_slice(&n.to_be_bytes());
278:         h
279:     }
280: 
281:     // ---- check_epoch / insert_epoch split (deferred-insert support) ----
282: 
283:     #[test]
284:     fn check_epoch_does_not_insert() {
285:         let mut d = Dedup::new(8);
286:         assert_eq!(d.check_epoch(hash(1), 5), DedupVerdict::Fresh);
287:         // Checking must not consume the hash — this is the point of the split: the shim
288:         // decides whether to display/relay BEFORE marking the frame seen.
289:         assert_eq!(d.check_epoch(hash(1), 5), DedupVerdict::Fresh);
290:         assert!(d.insert_epoch(hash(1), 5));
291:         assert_eq!(d.check_epoch(hash(1), 5), DedupVerdict::Duplicate);
292:     }
293: 
294:     #[test]
295:     fn deferred_insert_lets_a_transient_failure_retry() {
296:         // Models the real bug: the first copy of a frame arrives, cannot be judged (no local
297:         // sketch yet), and is NOT inserted. A retransmission must still be admissible.
298:         let mut d = Dedup::new(8);
299:         assert_eq!(d.check_epoch(hash(7), 3), DedupVerdict::Fresh);
300:         // ... verification was inconclusive, so no insert_epoch call ...
301:         assert_eq!(
302:             d.check_epoch(hash(7), 3),
303:             DedupVerdict::Fresh,
304:             "retry must still be eligible"
305:         );
306:         // Second time it verifies and is acted on.
307:         assert!(d.insert_epoch(hash(7), 3));
308:         assert_eq!(d.check_epoch(hash(7), 3), DedupVerdict::Duplicate);
309:     }
310: 
311:     #[test]
312:     fn check_and_insert_epoch_still_atomic() {
313:         let mut d = Dedup::new(8);
314:         assert!(d.check_and_insert_epoch(hash(2), 9));
315:         assert!(!d.check_and_insert_epoch(hash(2), 9));
316:     }
317: 
318:     // ---- C8 per-epoch sub-cap must be distinguishable from a duplicate ----
319: 
320:     #[test]
321:     fn bucket_full_is_reported_distinctly_from_duplicate() {
322:         let mut d = Dedup::new(EPOCH_BUCKET_CAP * 4);
323:         let epoch = 42u32;
324:         for i in 0..EPOCH_BUCKET_CAP {
325:             assert!(d.insert_epoch(hash_n(i as u32), epoch), "fill slot {i}");
326:         }
327:         // A FRESH, never-seen hash in a full bucket must report BucketFull, not Duplicate.
328:         // Collapsing the two into `false` is what turned an anti-eviction mitigation into a
329:         // silent per-epoch blackout: the shim read the refusal as "already handled".
330:         let verdict = d.check_epoch(hash_n(0xFFFF), epoch);
331:         assert_eq!(verdict, DedupVerdict::BucketFull);
332:         assert_ne!(verdict, DedupVerdict::Duplicate);
333: 
334:         // A different epoch is unaffected — the sub-cap is per bucket.
335:         assert_eq!(d.check_epoch(hash_n(0xFFFF), epoch + 1), DedupVerdict::Fresh);
336:     }
337: 
338:     #[test]
339:     fn epoch_decay_frees_a_full_bucket() {
340:         let mut d = Dedup::new(EPOCH_BUCKET_CAP * 4);
341:         let epoch = 100u32;
342:         for i in 0..EPOCH_BUCKET_CAP {
343:             assert!(d.insert_epoch(hash_n(i as u32), epoch));
344:         }
345:         assert_eq!(d.check_epoch(hash_n(0xAAAA), epoch), DedupVerdict::BucketFull);
346:         // Three epochs on, the old bucket decays out of the window, so the blackout is
347:         // bounded in time rather than permanent.
348:         assert_eq!(d.check_epoch(hash_n(0xAAAA), epoch + 3), DedupVerdict::Fresh);
349:     }
350: 
351:     #[test]
352:     fn epoch_decay_evicts_entries_older_than_two_epochs() {
353:         let mut d = Dedup::new(64);
354:         assert!(d.check_and_insert_epoch(hash(1), 10));
355:         // Still inside the ~3-epoch window.
356:         assert!(!d.check_and_insert_epoch(hash(1), 12));
357:         // Beyond it: purged, so the hash is fresh again.
358:         assert!(d.check_and_insert_epoch(hash(1), 13));
359:     }
360: 
361:     #[test]
362:     fn dedup_fresh_then_repeat() {
363:         let mut d = Dedup::new(4);
364:         assert!(d.check_and_insert(hash(1)), "first insert is fresh");
365:         assert!(!d.check_and_insert(hash(1)), "second insert is a duplicate");
366:     }
367: 
368:     #[test]
369:     fn dedup_eviction_at_cap() {
370:         // cap=2: insert hashes A, B, C. A should be evicted so a fourth call with A returns true.
371:         let mut d = Dedup::new(2);
372:         assert!(d.check_and_insert(hash(0xa))); // A — fresh
373:         assert!(d.check_and_insert(hash(0xb))); // B — fresh, set now at cap
374:         assert!(d.check_and_insert(hash(0xc))); // C — fresh, A evicted
375:         // A was evicted, so it should be fresh again.
376:         assert!(d.check_and_insert(hash(0xa)), "A must be fresh after eviction");
377:         // B was also evicted when C was inserted, so it should also be fresh.
378:         assert!(d.check_and_insert(hash(0xb)), "B must be fresh after eviction");
379:     }
380: 
381:     #[test]
382:     fn dedup_zero_cap_clamped_to_one() {
383:         let mut d = Dedup::new(0);
384:         assert!(d.check_and_insert(hash(1)));
385:         // With cap=1, inserting a second distinct hash evicts the first.
386:         assert!(d.check_and_insert(hash(2)));
387:         // hash(1) was evicted, so it is fresh again.
388:         assert!(d.check_and_insert(hash(1)));
389:     }
390: 
391:     // ----- relay_decision tests -----
392: 
393:     fn test_seed() -> [u8; 32] {
394:         let mut s = [0u8; 32];
395:         for (i, b) in s.iter_mut().enumerate() {
396:             *b = i as u8;
397:         }
398:         s
399:     }
400: 
401:     fn test_beacon_seed() -> [u8; 32] {
402:         let mut bs = [0u8; 32];
403:         for (i, b) in bs.iter_mut().enumerate() {
404:             *b = (i + 100) as u8;
405:         }
406:         bs
407:     }
408: 
409:     #[test]
410:     fn relay_decision_regional_decrements_ttl() {
411:         let seed = test_seed();
412:         let bs = test_beacon_seed();
413:         let buf =
414:             message::make_message_frame_ttl(&seed, 1, &bs, MsgType::RegionalPropagated, "relay", 8)
415:                 .expect("short text");
416:         let relayed = relay_decision(&buf).expect("should relay");
417:         assert_eq!(relayed[214], 7, "TTL must be decremented by 1");
418:         // All other bytes must be identical.
419:         for i in 0..FRAME_LEN {
420:             if i != 214 {
421:                 assert_eq!(
422:                     relayed[i], buf[i],
423:                     "byte {i} must be unchanged after relay"
424:                 );
425:             }
426:         }
427:     }
428: 
429:     #[test]
430:     fn relay_decision_ttl_zero_returns_none() {
431:         let seed = test_seed();
432:         let bs = test_beacon_seed();
433:         let buf =
434:             message::make_message_frame_ttl(&seed, 1, &bs, MsgType::RegionalPropagated, "relay", 0)
435:                 .expect("short text");
436:         assert!(
437:             relay_decision(&buf).is_none(),
438:             "TTL=0 must produce None (drop)"
439:         );
440:     }
441: 
442:     #[test]
443:     fn relay_decision_local_ttl_zero_returns_none() {
444:         let seed = test_seed();
445:         let bs = test_beacon_seed();
446:         // LocalImmediate with TTL 0 is end-of-line: not relayed.
447:         let buf =
448:             message::make_message_frame_ttl(&seed, 1, &bs, MsgType::LocalImmediate, "local", 0)
449:                 .expect("short text");
450:         assert!(
451:             relay_decision(&buf).is_none(),
452:             "LocalImmediate with TTL 0 must not be relayed"
453:         );
454:     }
455: 
456:     #[test]
457:     fn relay_decision_local_relays_once_with_ttl_clobbered_to_zero() {
458:         let seed = test_seed();
459:         let bs = test_beacon_seed();
460:         // Honest local frame (ttl=1): relayed exactly once, as ttl=0.
461:         let buf = message::make_message_frame(&seed, 1, &bs, MsgType::LocalImmediate, "local")
462:             .expect("short text");
463:         assert_eq!(buf[214], 1, "fresh local frame must originate at ttl=1");
464:         let relayed = relay_decision(&buf).expect("local frame must be relayed once");
465:         assert_eq!(relayed[214], 0, "relayed local TTL must be clobbered to 0");
466:         // All other bytes must be identical.
467:         for i in 0..FRAME_LEN {
468:             if i != 214 {
469:                 assert_eq!(relayed[i], buf[i], "byte {i} must be unchanged after relay");
470:             }
471:         }
472:         // The ttl=0 echo is never relayed again.
473:         assert!(
474:             relay_decision(&relayed).is_none(),
475:             "relayed local echo (ttl=0) must not be relayed"
476:         );
477:     }
478: 
479:     #[test]
480:     fn relay_decision_local_adversary_high_ttl_clobbered_to_zero() {
481:         let seed = test_seed();
482:         let bs = test_beacon_seed();
483:         // Adversary originates a local frame with an inflated TTL: the relay clobbers it
484:         // to 0 — the flood budget is one hop regardless.
485:         let buf =
486:             message::make_message_frame_ttl(&seed, 1, &bs, MsgType::LocalImmediate, "local", 255)
487:                 .expect("short text");
488:         let relayed = relay_decision(&buf).expect("ttl>0 relays once");
489:         assert_eq!(
490:             relayed[214], 0,
491:             "adversarial ttl=255 must be clobbered to 0, not decremented"
492:         );
493:         assert!(relay_decision(&relayed).is_none());
494:     }
495: 
496:     #[test]
497:     fn relay_decision_garbage_returns_none() {
498:         // A buffer of all zeros has a wrong version byte — codec must reject it.
499:         let buf = [0u8; FRAME_LEN];
500:         assert!(
501:             relay_decision(&buf).is_none(),
502:             "garbage/BadVersion buffer must produce None"
503:         );
504:     }
505: 
506:     // ----- Private-frame relay tests (difficulty 8 to avoid 2^22 hashes in CI) -----
507: 
508:     #[test]
509:     fn private_frame_with_valid_witness_relays() {
510:         let seed = [7u8; 32];
511:         let bs = test_beacon_seed();
512:         let pair_key = [9u8; 32];
513:         let frame = message::make_private_frame(&seed, 1, &bs, &pair_key, "x", 8, 0)
514:             .expect("short text");
515:         let initial_ttl = frame[214];
516:         let result = relay_decision_with_difficulty(&frame, 8);
517:         let relayed = result.expect("valid witness must relay");
518:         assert_eq!(
519:             relayed[214],
520:             initial_ttl - 1,
521:             "byte 214 must be decremented by 1"
522:         );
523:         for i in 0..FRAME_LEN {
524:             if i != 214 {
525:                 assert_eq!(relayed[i], frame[i], "byte {i} must be unchanged");
526:             }
527:         }
528:     }
529: 
530:     #[test]
531:     fn private_frame_with_bad_witness_drops() {
532:         let seed = [7u8; 32];
533:         let bs = test_beacon_seed();
534:         let pair_key = [9u8; 32];
535:         let mut frame = message::make_private_frame(&seed, 1, &bs, &pair_key, "x", 8, 0)
536:             .expect("short text");
537:         // Flip a bit inside the witness field (bytes 102..118) to corrupt it.
538:         frame[102] ^= 0x01;
539:         assert!(
540:             relay_decision_with_difficulty(&frame, 8).is_none(),
541:             "corrupted witness must drop silently"
542:         );
543:     }
544: 
545:     #[test]
546:     fn private_frame_ttl_zero_drops() {
547:         let seed = [7u8; 32];
548:         let bs = test_beacon_seed();
549:         let pair_key = [9u8; 32];
550:         let mut frame = message::make_private_frame(&seed, 1, &bs, &pair_key, "x", 8, 0)
551:             .expect("short text");
552:         frame[214] = 0;
553:         assert!(
554:             relay_decision_with_difficulty(&frame, 8).is_none(),
555:             "TTL=0 private frame must drop"
556:         );
557:     }
558: 
559:     #[test]
560:     fn relayed_frame_decodes_and_sig_verifies() {
561:         let seed = test_seed();
562:         let bs = [200u8; 32];
563:         let epoch = 5u32;
564:         let buf =
565:             message::make_message_frame_ttl(&seed, epoch, &bs, MsgType::RegionalPropagated, "verify", 8)
566:                 .expect("short text");
567:         let relayed = relay_decision(&buf).expect("should relay");
568: 
569:         // Decode must succeed.
570:         let frame = codec::decode(&relayed).expect("relayed frame must decode");
571: 
572:         // Signature must still verify — reserved is outside SIG_REGION.
573:         let e = crypto::from_seed(&seed, &bs);
574:         let pk = crypto::public_key(&e);
575:         assert!(
576:             crypto::verify(&pk, codec::signing_region(&relayed), &frame.sig),
577:             "signature must be valid after relay"
578:         );
579:     }
580: }

(End of file - total 580 lines)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/MeshState.kt"
}
```

**Output:**
```
<path>/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/MeshState.kt</path>
<type>file</type>
<content>
1: package org.bileichat.mesh
2: 
3: import kotlinx.coroutines.flow.MutableStateFlow
4: import java.text.SimpleDateFormat
5: import java.util.Date
6: import java.util.Locale
7: 
8: data class Stats(
9:     val epoch: UInt,
10:     val neighborsThisEpoch: Int,
11:     val totalHeard: Int,
12:     val localSketch: List<ULong>,
13:     val advertising: Boolean,
14:     val scanning: Boolean,
15:     val codedPhyActive: Boolean,
16:     val note: String = ""
17: )
18: 
19: data class MsgRow(
20:     val tsMs: Long,
21:     val epoch: UInt,
22:     val markHexPrefix: String, // first 8 hex chars
23:     val rssi: Int?,
24:     val text: String,
25:     val mine: Boolean,
26:     val tier: SendTier = SendTier.BROADCAST,
27:     /** True when the frame arrived at its origination TTL — straight off the sender's
28:      *  radio, no relay hop. Drives the per-message trust meter. */
29:     val direct: Boolean = false,
30:     /** A2: distinct DIRECT-heard claims for this alert body. A HINT for the user —
31:      *  never a proof (a determined nearby attacker can forge claims). */
32:     val corroborations: Int = 0,
33:     /** For PRIVATE messages: the paired contact label (e.g. "ALICE"). Null for non-private. */
34:     val contactLabel: String? = null,
35:     /**
36:      * True when the PoCP witness verified against a local cell holding fewer than
37:      * [MIN_TRUSTWORTHY_CELL] marks. The witness MAC key is derived from public material, so
38:      * co-presence rests entirely on the Jaccard ratio — and a one-element claim scores 1/N,
39:      * clearing tau for any cell of 3 or fewer. An attacker who has never been near the cell
40:      * can sweep all 256 single-byte sketches and land 2-3 accepted forgeries. Verification
41:      * at that size is not evidence, so the badge must not claim it is.
42:      */
43:     val lowConfidenceCell: Boolean = false,
44:     /** How many times this exact alert has been heard (re-airings collapse into one row). */
45:     val repeats: Int = 1,
46:     /** Transmission state of one of OUR messages. Null on received rows. */
47:     val sendState: SendState? = null
48: )
49: 
50: /**
51:  * WhatsApp-style delivery state for a message we sent.
52:  *
53:  * Deliberately stops short of "delivered". Nothing in this protocol can prove a specific
54:  * device received a specific message — there is no acknowledgement, by design, because an
55:  * ack would tie a receiver to a sender and undo the unlinkability the rotating marks exist
56:  * to provide. So the two ticks mean what the radio can actually witness.
57:  */
58: enum class SendState {
59:     /** Queued: the frame is being built (a private frame is solving its VDL witness). */
60:     SENDING,
61: 
62:     /** One tick: the frame is on air from this phone. */
63:     ON_AIR,
64: 
65:     /** Two ticks: we heard our own frame come back, so a peer relayed it. Not proof of
66:      *  delivery — one adversarial device can relay once and blackhole the rest. */
67:     ECHOED
68: }
69: 
70: /** Below this many marks in our own cell, a verified witness is not meaningful evidence. */
71: const val MIN_TRUSTWORTHY_CELL = 4
72: 
73: enum class SendTier { LOCAL, BROADCAST, PRIVATE }
74: 
75: /** A queued private (Tier-3) message: recipient label + plaintext. The pair key is
76:  *  resolved (and ratcheted, A3) by the service at seal time — no key material rides
77:  *  the queue. */
78: data class PrivateSend(
79:     val label: String,
80:     val text: String
81: )
82: 
83: object MeshState {
84:     val running = MutableStateFlow(false)
85:     val stats = MutableStateFlow(
86:         Stats(
87:             epoch = 0u,
88:             neighborsThisEpoch = 0,
89:             totalHeard = 0,
90:             localSketch = emptyList(),
91:             advertising = false,
92:             scanning = false,
93:             codedPhyActive = false
94:         )
95:     )
96:     val measurement = Measurement()
97: 
98:     @Volatile
99:     var config: MeshConfig = MeshConfig()
100: 
101:     // Message feed — newest last, capped at 200 rows
102:     val messages = MutableStateFlow<List<MsgRow>>(emptyList())
103: 
104:     // Called from the BLE scan (binder) thread and the main thread; synchronized so the
105:     // read-modify-write on the StateFlow can't drop rows.
106:     @Synchronized
107:     fun appendMessage(row: MsgRow) {
108:         val current = messages.value
109:         val updated = if (current.size >= 200) current.drop(1) + row else current + row
110:         messages.value = updated
111:     }
112: 
113:     /**
114:      * Append an incoming alert, or collapse it into a recent identical one.
115:      *
116:      * A sender re-airs the same text every epoch for `messageRepeatEpochs`, and the frame
117:      * carries a fresh epoch (so a fresh hash — dedup can't catch it) AND a fresh mark, because
118:      * marks rotate per epoch for unlinkability. The display-side suppression key was
119:      * `text|markPrefix`, so it rotated at exactly the same rate as the repeats and never
120:      * matched: one sent message showed up as three identical rows.
121:      *
122:      * Merging instead of suppressing also keeps B6 honest — an attacker pre-broadcasting the
123:      * same words can no longer hide the real alert, because nothing is ever hidden; the row
124:      * is shown once with a repeat count, and the most trustworthy observation wins each field.
125:      */
126:     @Synchronized
127:     fun appendOrMergeIncoming(row: MsgRow, withinEpochs: UInt = 4u) {
128:         val current = messages.value
129:         val idx = current.indexOfLast {
130:             !it.mine && it.text == row.text && it.tier == row.tier &&
131:                 row.epoch >= it.epoch && row.epoch - it.epoch <= withinEpochs
132:         }
133:         if (idx < 0) {
134:             appendMessage(row)
135:             return
136:         }
137:         val old = current[idx]
138:         // Keep the FIRST-heard timestamp and list position; upgrade the trust fields, since a
139:         // later copy arriving direct, or judged against a bigger cell, is better evidence.
140:         messages.value = current.toMutableList().also {
141:             it[idx] = old.copy(
142:                 repeats = old.repeats + 1,
143:                 direct = old.direct || row.direct,
144:                 rssi = row.rssi ?: old.rssi,
145:                 corroborations = maxOf(old.corroborations, row.corroborations),
146:                 lowConfidenceCell = old.lowConfidenceCell && row.lowConfidenceCell
147:             )
148:         }
149:     }
150: 
151:     /**
152:      * [MsgRow.tsMs] of the message we are currently transmitting, so its ticks can be
153:      * upgraded in place. Single slot because [outgoingText] is a single slot.
154:      */
155:     @Volatile
156:     var outgoingRowTs: Long? = null
157: 
158:     /** Same, for the private send queue — private frames are sealed on their own path. */
159:     @Volatile
160:     var privateRowTs: Long? = null
161: 
162:     /**
163:      * True while a message of ours is still being re-aired.
164:      *
165:      * Surfaced because LOCAL re-originates every epoch for up to
166:      * LOCAL_REBROADCAST_WINDOW_MS (30 min) until it is heard back — correct for a danger
167:      * alert, but it was completely invisible, so an ordinary message looked like it was
168:      * "sending forever" with no way to stop it.
169:      */
170:     val outgoingAiring = MutableStateFlow(false)
171: 
172:     /** Upgrade the ticks on one of our rows. Monotonic: state never moves backwards. */
173:     @Synchronized
174:     fun markOutgoing(ts: Long, state: SendState) {
175:         val current = messages.value
176:         val idx = current.indexOfLast { it.mine && it.tsMs == ts }
177:         if (idx < 0) return
178:         val old = current[idx]
179:         if (old.sendState != null && old.sendState.ordinal >= state.ordinal) return
180:         messages.value = current.toMutableList().also { it[idx] = old.copy(sendState = state) }
181:     }
182: 
183:     // Outgoing message text
184:     val outgoingText = MutableStateFlow("")
185: 
186:     /**
187:      * Bumped on every explicit send. MutableStateFlow conflates equal values, so re-sending
188:      * the SAME text was a silent no-op: the collector in MeshService never fired, no frame
189:      * was ever built, and outgoingSetAtEpoch/reflectionHeard were never reset — yet the
190:      * user's own bubble was still appended, so the message looked sent. Sending "HELP"
191:      * twice in a row transmitted once.
192:      */
193:     val outgoingRevision = MutableStateFlow(0)
194: 
195:     /** Tier the NEXT outgoing message is sent at. Changing this re-originates the current
196:      *  frame with a new TTL and msgType, so it must only change on an explicit send-tier
197:      *  choice — never as a side effect of reading a different feed. */
198:     val outgoingTier = MutableStateFlow(SendTier.BROADCAST)
199: 
200:     /** Tier whose feed is currently VISIBLE. Split from [outgoingTier]: the tab bar drove
201:      *  both, so switching tabs to read LOCAL traffic mutated how an in-flight broadcast was
202:      *  being transmitted. Selecting a tab still sets the send tier to match (that is the
203:      *  intuitive behaviour) — but a bare view change no longer does. */
204:     val viewTier = MutableStateFlow(SendTier.BROADCAST)
205: 
206:     // C4: private-send QUEUE (was a single-slot StateFlow — two quick sends overwrote each
207:     // other and the reset could erase a send queued during the VDL solve). The service
208:     // consumes sequentially; trySend failure means the queue is full.
209:     val privateSends = kotlinx.coroutines.channels.Channel<PrivateSend>(capacity = 8)
210: 
211:     /** Delivery-receipt notice shown above the composer ("heard back once", "stopped").
212:      *  Null = nothing to show. Set by the service on reflection/expiry; cleared when a
213:      *  new message is composed. B1: wording must never imply guaranteed delivery. */
214:     val receipt = MutableStateFlow<String?>(null)
215: 
216:     /** Bumped whenever the pairing contact list changes so the UI recomposes. */
217:     val contactsVersion = MutableStateFlow(0)
218: 
219:     /** D4: false when the TEE-backed encrypted store is unavailable and pairings live in
220:      *  memory only (die on process death). Surfaced as a banner, not just a log line. */
221:     val secureStorageOk = MutableStateFlow(true)
222: 
223:     /**
224:      * Set when frames are being dropped because the sender's epoch is more than ±2 buckets
225:      * from ours (K4). Mismatched clocks or a mismatched epochMs partition the mesh totally
226:      * and silently — a rate-limited debug line was the only trace, so a user just saw "no
227:      * messages" with no reason. Cleared once traffic verifies again.
228:      */
229:     val clockSkewWarning = MutableStateFlow<String?>(null)
230: 
231:     @Volatile
232:     var outgoingSetAtEpoch: UInt? = null
233: 
234:     // Debug log — newest first, capped at 200 lines
235:     val debugLog = MutableStateFlow<List<String>>(emptyList())
236: 
237:     private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US)
238: 
239:     @Synchronized
240:     fun logDebug(line: String) {
241:         val ts = timeFmt.format(Date())
242:         val entry = "$ts $line"
243:         val current = debugLog.value
244:         val updated = if (current.size >= 200) listOf(entry) + current.dropLast(1) else listOf(entry) + current
245:         debugLog.value = updated
246:     }
247: }

(End of file - total 247 lines)
</content>
```

---

## Assistant (Plan · Kimi K3 (2x usage) · 8.1s)

_Thinking:_

Now let me look at PairStore.kt (ratchet, candidateKeys, keyForSend) and GattPlane.kt (relay mechanics). Also Measurement.kt for sketch bucketing. Let me read PairStore.kt and GattPlane.kt key sections.

**Tool: read**

**Input:**
```json
{
  "filePath": "/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/PairStore.kt"
}
```

**Output:**
```
<path>/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/PairStore.kt</path>
<type>file</type>
<content>
1: package org.bileichat.mesh
2: 
3: import android.content.Context
4: import androidx.security.crypto.EncryptedSharedPreferences
5: import androidx.security.crypto.MasterKey
6: import java.security.SecureRandom
7: 
8: /**
9:  * A paired contact.
10:  *
11:  * v1 (legacy): [pairKey] is the static `pair_derive` output — NO forward secrecy (A3).
12:  * v2: [pairKey] is the CURRENT epoch's ratcheted chain key; [chainEpoch] is its epoch;
13:  * [prevKey]/[prevEpoch] retain exactly one previous epoch key for clock-skew tolerance.
14:  * The chain seed was mixed with per-pairing salts that both sides deleted after pairing,
15:  * so a seized long-term secret cannot recompute the chain — past epochs are unrecoverable.
16:  */
17: data class Contact(
18:     val label: String,
19:     val pairKey: ByteArray,
20:     val v2: Boolean = false,
21:     val chainEpoch: UInt = 0u,
22:     val prevKey: ByteArray? = null,
23:     val prevEpoch: UInt = 0u
24: ) {
25:     override fun equals(other: Any?): Boolean {
26:         if (other !is Contact) return false
27:         val prevEq = if (prevKey == null) other.prevKey == null
28:         else other.prevKey != null && prevKey.contentEquals(other.prevKey)
29:         return label == other.label && pairKey.contentEquals(other.pairKey) &&
30:             v2 == other.v2 && chainEpoch == other.chainEpoch && prevEq && prevEpoch == other.prevEpoch
31:     }
32: 
33:     override fun hashCode(): Int = 31 * label.hashCode() + pairKey.contentHashCode()
34: }
35: 
36: /** A parsed out-of-band pairing offer. [saltHex] non-null ⇒ v2 (forward-secret ratchet). */
37: /**
38:  * A scanned pairing payload.
39:  *
40:  * [anchorEpoch] is the epoch at which the OTHER phone generated its QR. It exists because the
41:  * ratchet chain start must be AGREED, not observed locally — see PairStore.addContact.
42:  * Null for legacy v1/v2 payloads, which carry no epoch.
43:  */
44: data class PairingOffer(val pkHex: String, val saltHex: String?, val anchorEpoch: UInt? = null)
45: 
46: object PairStore {
47:     private const val PREFS_NAME = "mesh_pairing_v2"
48:     private const val KEY_SK = "sk"
49:     private const val KEY_CONTACTS = "contacts"
50: 
51:     // B4/C9: private-send nonce counter — per-epoch random base + in-epoch sequence,
52:     // stored in the ENCRYPTED prefs (was: plaintext SharedPreferences, forever-monotonic
53:     // → cross-epoch linkability + send-volume leak).
54:     private const val KEY_CTR_EPOCH = "privCtrEpoch"
55:     private const val KEY_CTR_BASE = "privCtrBase"
56:     private const val KEY_CTR_SEQ = "privCtrSeq"
57: 
58:     @Volatile private var memSk: ByteArray? = null
59:     private val memContacts = java.util.concurrent.CopyOnWriteArrayList<Contact>()
60: 
61:     /** Cached EncryptedSharedPreferences instance — building MasterKey + the encrypted
62:      *  store on EVERY access burned the Keystore under a private-frame storm (B5). */
63:     @Volatile private var prefsCache: android.content.SharedPreferences? = null
64:     @Volatile private var prefsFailed = false
65: 
66:     /** B5: in-memory contact cache; invalidated on every mutation. */
67:     @Volatile private var contactCache: List<Contact>? = null
68: 
69:     /** Per-process pairing salt shown in our QR (v2). NEVER persisted: it is the entropy
70:      *  that makes the v2 chain seed unrecomputable after seizure. Rotated after every
71:      *  successful pairing so each contact gets fresh salt. */
72:     @Volatile private var mySalt: ByteArray? = null
73: 
74:     /**
75:      * EncryptedSharedPreferences derived from a MasterKey stored in AndroidKeyStore (TEE-backed).
76:      *
77:      * Fail-closed: on failure return null. We never persist plaintext key material.
78:      * D4: callers can surface [secureStorageAvailable] to the user instead of pairing
79:      * silently dying on process death.
80:      */
81:     private fun prefs(ctx: Context): android.content.SharedPreferences? {
82:         prefsCache?.let { return it }
83:         if (prefsFailed) return null
84:         return runCatching {
85:             val mk = MasterKey.Builder(ctx, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
86:                 .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
87:                 .build()
88:             EncryptedSharedPreferences.create(
89:                 ctx, PREFS_NAME, mk,
90:                 EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
91:                 EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
92:             )
93:         }.onSuccess { prefsCache = it }
94:             .getOrElse { e ->
95:                 android.util.Log.e("PairStore", "EncryptedSharedPreferences failed, using in-memory only: ${e.message}")
96:                 prefsFailed = true
97:                 null
98:             }
99:     }
100: 
101:     /** D4: false when the TEE-backed store is unavailable (pairings live in memory only
102:      *  and die with the process). The UI must SAY this, not just log it. */
103:     fun secureStorageAvailable(ctx: Context): Boolean = prefs(ctx) != null
104: 
105:     private fun ByteArray.toHex(): String =
106:         joinToString("") { "%02x".format(it) }
107: 
108:     private fun String.hexToBytesOrNull(): ByteArray? {
109:         if (length % 2 != 0) return null
110:         val out = ByteArray(length / 2)
111:         for (i in out.indices) {
112:             val hi = Character.digit(this[i * 2], 16)
113:             val lo = Character.digit(this[i * 2 + 1], 16)
114:             if (hi == -1 || lo == -1) return null
115:             out[i] = ((hi shl 4) or lo).toByte()
116:         }
117:         return out
118:     }
119: 
120:     /** Per-pairing salt length. MUST match the `[u8; 32]` that `pair_seed_v2` requires
121:      *  (ffi.rs) — this was 16, so pairSeedV2 rejected every salt, returned null, and EVERY
122:      *  v2 QR scan failed with "Derivation failed". Only raw-hex / v1 payloads could pair,
123:      *  silently downgrading users to the static non-forward-secret key. */
124:     private const val PAIR_SALT_LEN = 32
125: 
126:     /** Epochs to backdate a new v2 contact's ratchet start, so two phones that scan each
127:      *  other seconds apart still have overlapping key windows. See addContact. */
128:     private const val PAIR_EPOCH_BACKDATE = 3u
129: 
130:     private fun myPairSalt(ctx: Context): ByteArray {
131:         mySalt?.let { return it }
132:         val s = ByteArray(PAIR_SALT_LEN)
133:         SecureRandom().nextBytes(s)
134:         mySalt = s
135:         return s
136:     }
137: 
138:     private fun currentEpoch(): UInt =
139:         (System.currentTimeMillis() / MeshState.config.epochMs).toUInt()
140: 
141:     /**
142:      * v2 QR payload: public key + per-pairing salt. Both public; the secret and the chain
143:      * keys never leave the device. The salt is fresh per pairing and rotated after each add.
144:      */
145:     /**
146:      * v3 adds the generating epoch. Both phones scan each other, so both end up holding both
147:      * epochs and can pick the SAME chain anchor with min(); without it each side anchored on
148:      * its own wall clock and the two ratchets never produced a common key (see addContact).
149:      */
150:     fun qrPayload(ctx: Context): String =
151:         "bileichat:key:v3:${myPublicHex(ctx)}:${myPairSalt(ctx).toHex()}:${currentEpoch()}"
152: 
153:     fun mySaltHex(ctx: Context): String = myPairSalt(ctx).toHex()
154: 
155:     /** Parse a scanned/typed pairing offer: v2 payload, legacy v1 payload, or raw 64-hex key. */
156:     fun parsePairingOffer(value: String): PairingOffer? {
157:         val trimmed = value.trim()
158:         val parts = trimmed.split(":")
159:         return when {
160:             parts.size == 6 && parts[0].equals("bileichat", true) &&
161:                 parts[1].equals("key", true) && parts[2] == "v3" -> {
162:                 val pk = parts[3]
163:                 val salt = parts[4]
164:                 val epoch = parts[5].toUIntOrNull()
165:                 if (pk.length == 64 && pk.hexToBytesOrNull()?.size == 32 &&
166:                     salt.length == PAIR_SALT_LEN * 2 &&
167:                     salt.hexToBytesOrNull()?.size == PAIR_SALT_LEN &&
168:                     epoch != null
169:                 ) PairingOffer(pk, salt, epoch) else null
170:             }
171:             parts.size == 5 && parts[0].equals("bileichat", true) &&
172:                 parts[1].equals("key", true) && parts[2] == "v2" -> {
173:                 val pk = parts[3]
174:                 val salt = parts[4]
175:                 if (pk.length == 64 && pk.hexToBytesOrNull()?.size == 32 &&
176:                     salt.length == PAIR_SALT_LEN * 2 &&
177:                     salt.hexToBytesOrNull()?.size == PAIR_SALT_LEN
178:                 ) PairingOffer(pk, salt) else null
179:             }
180:             parts.size == 4 && parts[0].equals("bileichat", true) &&
181:                 parts[1].equals("key", true) && parts[2] == "v1" -> {
182:                 val pk = parts[3]
183:                 if (pk.length == 64 && pk.hexToBytesOrNull()?.size == 32) PairingOffer(pk, null) else null
184:             }
185:             else -> {
186:                 val clean = trimmed.replace(" ", "").replace("\n", "")
187:                 if (clean.length == 64 && clean.hexToBytesOrNull()?.size == 32) {
188:                     PairingOffer(clean, null)
189:                 } else null
190:             }
191:         }
192:     }
193: 
194:     /** Legacy shim for the QR scanner validation path. */
195:     fun publicKeyFromQrOrHex(value: String): String? = parsePairingOffer(value)?.pkHex
196: 
197:     /**
198:      * Long-term X25519 secret key. Generated once, stored in EncryptedSharedPreferences.
199:      *
200:      * FUTURE (API 33+): use AndroidKeyStore KeyPairGenerator with
201:      * `KeyProperties.PURPOSE_AGREE_KEY` so the secret never leaves TEE. For now the seed is
202:      * stored encrypted at rest via EncryptedSharedPreferences.
203:      */
204:     @Synchronized
205:     fun secret(ctx: Context): ByteArray {
206:         val p = prefs(ctx)
207:         if (p == null) {
208:             memSk?.let { return it }
209:             val sk = ByteArray(32)
210:             SecureRandom().nextBytes(sk)
211:             memSk = sk
212:             return sk
213:         }
214:         val stored = p.getString(KEY_SK, null)
215:         if (stored != null) {
216:             val bytes = stored.hexToBytesOrNull()
217:             if (bytes != null && bytes.size == 32) return bytes
218:         }
219:         val sk = ByteArray(32)
220:         SecureRandom().nextBytes(sk)
221:         p.edit().putString(KEY_SK, sk.toHex()).commit()
222:         return sk
223:     }
224: 
225:     fun myPublicHex(ctx: Context): String {
226:         val pub = uniffi.mesh_core.pairPublic(secret(ctx)) ?: return ""
227:         return pub.toHex()
228:     }
229: 
230:     /** B5: cached contact list — no Keystore/EncryptedSharedPreferences rebuild per call. */
231:     fun contacts(ctx: Context): List<Contact> {
232:         contactCache?.let { return it }
233:         val loaded = loadContacts(ctx)
234:         contactCache = loaded
235:         return loaded
236:     }
237: 
238:     private fun loadContacts(ctx: Context): List<Contact> {
239:         val p = prefs(ctx) ?: return memContacts.toList()
240:         val raw = p.getString(KEY_CONTACTS, null) ?: return emptyList()
241:         return raw.lines()
242:             .filter { it.isNotBlank() }
243:             .mapNotNull { line ->
244:                 val fields = line.split('\t')
245:                 if (fields.size < 2) return@mapNotNull null
246:                 val label = fields[0]
247:                 val keyBytes = fields[1].hexToBytesOrNull() ?: return@mapNotNull null
248:                 if (keyBytes.size != 32) return@mapNotNull null
249:                 if (fields.size >= 6 && fields[2] == "2") {
250:                     val chainEpoch = fields[3].toUIntOrNull() ?: return@mapNotNull null
251:                     val prevBytes = if (fields[4].isEmpty()) null else fields[4].hexToBytesOrNull()
252:                     if (prevBytes != null && prevBytes.size != 32) return@mapNotNull null
253:                     val prevEpoch = fields[5].toUIntOrNull() ?: 0u
254:                     Contact(label, keyBytes, v2 = true, chainEpoch = chainEpoch,
255:                         prevKey = prevBytes, prevEpoch = prevEpoch)
256:                 } else {
257:                     // Legacy v1 line: static key, no forward secrecy.
258:                     Contact(label, keyBytes, v2 = false)
259:                 }
260:             }
261:     }
262: 
263:     @Synchronized
264:     fun addContact(ctx: Context, label: String, offerRaw: String): String? {
265:         val trimmedLabel = label.trim()
266:         if (trimmedLabel.isEmpty()) return "Contact name cannot be empty"
267:         if (trimmedLabel.any { it == '\t' || it == '\n' || it == '\r' }) return "Contact name contains invalid characters"
268:         if (trimmedLabel.length > 32) return "Contact name too long (max 32 chars)"
269:         val offer = parsePairingOffer(offerRaw) ?: return "Invalid pairing key format"
270:         val peerPub = offer.pkHex.hexToBytesOrNull() ?: return "Invalid public key"
271:         if (peerPub.size != 32) return "Invalid public key size"
272:         // D5: pairing with ourselves is never valid.
273:         if (offer.pkHex.equals(myPublicHex(ctx), ignoreCase = true)) return "Pairing with your own key is not allowed"
274: 
275:         val shared = uniffi.mesh_core.pairDerive(secret(ctx), peerPub) ?: return "Key agreement failed"
276:         val contact = if (offer.saltHex != null) {
277:             // v2: chain seed = f(ECDH, both salts). Salts are NOT stored — after this call
278:             // only the ratchet chain state survives, which is what gives seizure resistance.
279:             val theirSalt = offer.saltHex.hexToBytesOrNull() ?: return "Invalid salt"
280:             val seed0 = uniffi.mesh_core.pairSeedV2(shared, myPairSalt(ctx), theirSalt) ?: return "Derivation failed"
281:             // The chain anchor must be AGREED, not observed locally.
282:             //
283:             // pair_ratchet mixes the epoch INDEX into every step
284:             // (k_e = derive_key(k_{e-1} || e)), so the key at epoch E is a function of the
285:             // whole index sequence from the anchor onward — i.e. of where the chain started.
286:             // Both sides begin from the same seed0 (pair_seed_v2 is order-independent), but
287:             // each used its OWN wall clock at scan time as the anchor. QR pairing is
288:             // inherently sequential, so those clocks land in different 10 s epochs almost
289:             // every time: Alice anchored at 100, Bob at 102, and from then on their chains
290:             // produced different keys at EVERY epoch. Not a skew window — a permanent
291:             // mismatch. Every v2 private message failed to open, forever, silently.
292:             //
293:             // Backdating both by a constant did not help: it shifted both anchors equally and
294:             // left the difference intact. That was a misdiagnosis of this same bug.
295:             //
296:             // v3 puts the generating epoch in the QR, so each side holds both epochs and takes
297:             // min() — same value on both phones, no clock agreement needed. The backdate is
298:             // kept only to widen the retained prevKey window.
299:             val start = offer.anchorEpoch?.let { minOf(it, currentEpoch()) } ?: run {
300:                 MeshState.logDebug(
301:                     "pairing with a pre-v3 code: no shared chain anchor, so private messages " +
302:                         "will only open if both phones scanned within the same epoch — " +
303:                         "re-pair once both are updated"
304:                 )
305:                 currentEpoch()
306:             }
307:             Contact(
308:                 trimmedLabel,
309:                 seed0,
310:                 v2 = true,
311:                 chainEpoch = if (start >= PAIR_EPOCH_BACKDATE) start - PAIR_EPOCH_BACKDATE else 0u
312:             )
313:         } else {
314:             // Legacy v1: static key, no forward secrecy (shown as LEGACY in the UI).
315:             Contact(trimmedLabel, shared, v2 = false)
316:         }
317: 
318:         if (prefs(ctx) == null) {
319:             memContacts.removeAll { it.label == trimmedLabel }
320:             memContacts.add(contact)
321:         } else {
322:             val updated = contacts(ctx).filter { it.label != trimmedLabel } + contact
323:             persist(ctx, updated)
324:         }
325:         contactCache = null
326:         // NOTE: mySalt deliberately does NOT rotate here. The salt in the displayed QR
327:         // must equal the salt used for every pairing made while that QR is shown — rotating
328:         // on add would break sequential face-to-face pairing (the second scanner would get
329:         // a different salt than the one they scanned). The salt is per-process only and
330:         // never persisted, which is what preserves forward secrecy after process death.
331:         return null
332:     }
333: 
334:     @Synchronized
335:     fun removeContact(ctx: Context, label: String) {
336:         if (prefs(ctx) == null) {
337:             memContacts.removeAll { it.label == label }
338:         } else {
339:             val updated = contacts(ctx).filter { it.label != label }
340:             persist(ctx, updated)
341:         }
342:         contactCache = null
343:     }
344: 
345:     private fun persist(ctx: Context, list: List<Contact>) {
346:         val p = prefs(ctx) ?: return
347:         val raw = list.joinToString("\n") { c ->
348:             if (c.v2) {
349:                 "${c.label}\t${c.pairKey.toHex()}\t2\t${c.chainEpoch}\t${c.prevKey?.toHex() ?: ""}\t${c.prevEpoch}"
350:             } else {
351:                 "${c.label}\t${c.pairKey.toHex()}"
352:             }
353:         }
354:         p.edit().putString(KEY_CONTACTS, raw).commit()
355:     }
356: 
357:     /** Advance a v2 contact's chain to [epoch] (one-way ratchet), persist, and return the
358:      *  key for that epoch. v1 contacts return the static key. Null if label unknown or the
359:      *  ratchet span is absurd. */
360:     @Synchronized
361:     fun keyForSend(ctx: Context, label: String, epoch: UInt): ByteArray? {
362:         val contact = contacts(ctx).firstOrNull { it.label == label } ?: return null
363:         if (!contact.v2) return contact.pairKey
364:         if (epoch <= contact.chainEpoch) return contact.pairKey
365:         val advanced = uniffi.mesh_core.pairRatchet(contact.pairKey, contact.chainEpoch, epoch)
366:             ?: return null
367:         val updated = contact.copy(
368:             pairKey = advanced,
369:             chainEpoch = epoch,
370:             prevKey = contact.pairKey,
371:             prevEpoch = contact.chainEpoch
372:         )
373:         storeUpdated(ctx, updated)
374:         return advanced
375:     }
376: 
377:     /** Candidate AEAD keys for opening a frame with [frameEpoch] (A3). Order matters;
378:      * callers try all of them (no early-break timing leak beyond key count). */
379:     @Synchronized
380:     fun candidateKeys(ctx: Context, contact: Contact, frameEpoch: UInt): List<ByteArray> {
381:         if (!contact.v2) return listOf(contact.pairKey)
382:         return when {
383:             frameEpoch == contact.chainEpoch -> listOf(contact.pairKey)
384:             contact.prevKey != null && frameEpoch == contact.prevEpoch -> listOf(contact.prevKey)
385:             frameEpoch > contact.chainEpoch -> {
386:                 // Sender is ahead of our stored chain — fast-forward (one-way, cheap).
387:                 uniffi.mesh_core.pairRatchet(contact.pairKey, contact.chainEpoch, frameEpoch)
388:                     ?.let { listOf(it) } ?: emptyList()
389:             }
390:             else -> {
391:                 // Older than the retained previous epoch: undecryptable by design (forward
392:                 // secrecy — those keys are gone). Logged because it is otherwise completely
393:                 // silent, and it is what a pairing-epoch mismatch looks like from here.
394:                 // Contact label deliberately omitted: the debug log is exportable, and a
395:                 // line naming who you are paired with is social-graph metadata that a
396:                 // seized or shared export would hand over for free.
397:                 MeshState.logDebug(
398:                     "private frame at epoch $frameEpoch is behind that contact's chain " +
399:                         "(${contact.chainEpoch}) — key already ratcheted away, cannot open"
400:                 )
401:                 emptyList()
402:             }
403:         }
404:     }
405: 
406:     /**
407:      * Step every v2 contact's chain forward with the clock. Called once per epoch by the
408:      * service loop.
409:      *
410:      * `pair_ratchet` refuses spans longer than 8192 steps (a DoS bound on wire-supplied
411:      * epochs), but chains only ever advanced ON USE — the sender in [keyForSend], the
412:      * receiver in [noteOpened]. Two people who paired and then exchanged no private message
413:      * for 8192 epochs (22.8 h at a 10 s epoch — i.e. pairing the night before and first
414:      * using it the next day) blew that cap in both directions simultaneously: every send
415:      * failed with "key ratchet failed", every receive with "already ratcheted away". Since
416:      * neither path can advance the chain without first succeeding, it never recovered —
417:      * private messaging was permanently dead until the pair met again in person.
418:      *
419:      * Advancing to `epoch - 1` rather than `epoch` deliberately keeps the retained window
420:      * aligned with the ±2-epoch freshness gate: chainEpoch covers epoch-1, prevEpoch covers
421:      * epoch-2, and a current-epoch frame is one cheap on-the-fly step ahead in
422:      * [candidateKeys]. Forward secrecy is unchanged — superseded keys are still dropped.
423:      */
424:     @Synchronized
425:     fun fastForwardChains(ctx: Context, epoch: UInt) {
426:         if (epoch == 0u) return
427:         val target = epoch - 1u
428:         for (c in contacts(ctx)) {
429:             if (!c.v2 || target <= c.chainEpoch) continue
430:             val advanced = uniffi.mesh_core.pairRatchet(c.pairKey, c.chainEpoch, target)
431:             if (advanced == null) {
432:                 MeshState.logDebug(
433:                     "chain fast-forward for '" + c.label + "' failed: span " +
434:                         (target - c.chainEpoch) + " epochs exceeds the ratchet cap"
435:                 )
436:                 continue
437:             }
438:             storeUpdated(ctx, c.copy(
439:                 pairKey = advanced,
440:                 chainEpoch = target,
441:                 prevKey = c.pairKey,
442:                 prevEpoch = c.chainEpoch
443:             ))
444:         }
445:     }
446: 
447:     /** After a successful trial-open at [frameEpoch], persist the fast-forwarded chain
448:      *  state (past keys deleted). No-op when [frameEpoch] is not ahead. */
449:     @Synchronized
450:     fun noteOpened(ctx: Context, label: String, frameEpoch: UInt) {
451:         val contact = contacts(ctx).firstOrNull { it.label == label } ?: return
452:         if (!contact.v2 || frameEpoch <= contact.chainEpoch) return
453:         val advanced = uniffi.mesh_core.pairRatchet(contact.pairKey, contact.chainEpoch, frameEpoch)
454:             ?: return
455:         storeUpdated(ctx, contact.copy(
456:             pairKey = advanced,
457:             chainEpoch = frameEpoch,
458:             prevKey = contact.pairKey,
459:             prevEpoch = contact.chainEpoch
460:         ))
461:     }
462: 
463:     private fun storeUpdated(ctx: Context, updated: Contact) {
464:         if (prefs(ctx) == null) {
465:             memContacts.removeAll { it.label == updated.label }
466:             memContacts.add(updated)
467:         } else {
468:             persist(ctx, contacts(ctx).map { if (it.label == updated.label) updated else it })
469:         }
470:         contactCache = null
471:     }
472: 
473:     /**
474:      * B4/C9: next private-send nonce counter for [epoch]. A fresh random 32-bit base per
475:      * epoch breaks the cross-epoch continuity an observer could use to link a sender and
476:      * count their private traffic; within the epoch the value stays monotonic (nonce
477:      * uniqueness under that epoch's ephemeral pk). Stored encrypted.
478:      */
479:     @Synchronized
480:     fun nextPrivateCounter(ctx: Context, epoch: UInt): ULong {
481:         val p = prefs(ctx)
482:         if (p == null) {
483:             // Memory-only fallback: random base per call is still unlinkable; in-epoch
484:             // monotonicity across restarts is best-effort (nonce suffix also includes the
485:             // per-start ephemeral pk, so reuse risk stays negligible).
486:             return (SecureRandom().nextInt().toUInt() and 0x7FFF_FFFFu).toULong()
487:         }
488:         val storedEpoch = p.getString(KEY_CTR_EPOCH, null)?.toUIntOrNull()
489:         var base = p.getString(KEY_CTR_BASE, null)?.toULongOrNull()
490:         var seq = p.getString(KEY_CTR_SEQ, null)?.toULongOrNull() ?: 0uL
491:         if (storedEpoch != epoch || base == null) {
492:             base = (SecureRandom().nextInt().toUInt() and 0x7FFF_FFFFu).toULong()
493:             seq = 0uL
494:         }
495:         val counter = (base + seq) and 0xFFFF_FFFFuL
496:         p.edit()
497:             .putString(KEY_CTR_EPOCH, epoch.toString())
498:             .putString(KEY_CTR_BASE, base.toString())
499:             .putString(KEY_CTR_SEQ, (seq + 1uL).toString())
500:             .commit()
501:         return counter
502:     }
503: 
504:     /** Wipe all pairing state. Called from the panic-wipe path. */
505:     fun wipe(ctx: Context) {
506:         prefs(ctx)?.edit()?.clear()?.commit()
507:         memSk?.fill(0)
508:         memSk = null
509:         memContacts.clear()
510:         contactCache = null
511:         mySalt?.fill(0)
512:         mySalt = null
513:     }
514: }

(End of file - total 514 lines)
</content>
```

---

## Assistant (Plan · Kimi K3 (2x usage) · 11.4s)

_Thinking:_

Now I have PairStore. Let me quickly scan GattPlane.kt for relay mechanics and Measurement.kt for sketch bucketing. Then I can do the scenario simulation. Let me read GattPlane.kt — I'll focus on the relay/notify/write paths and connection management. 1022 lines. Let me read it in two parts, or grep for key functions. Actually, let me read the whole thing but I can skim. Let me read the important parts: start, onPeerSeen, relayOnce, currentFrame setter, connection callbacks.

**Tool: read**

**Input:**
```json
{
  "filePath": "/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/GattPlane.kt"
}
```

**Output:**
```
<path>/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/GattPlane.kt</path>
<type>file</type>
<content>
1: package org.bileichat.mesh
2: 
3: import android.annotation.SuppressLint
4: import android.bluetooth.BluetoothDevice
5: import android.bluetooth.BluetoothGatt
6: import android.bluetooth.BluetoothGattCallback
7: import android.bluetooth.BluetoothGattCharacteristic
8: import android.bluetooth.BluetoothGattDescriptor
9: import android.bluetooth.BluetoothGattServer
10: import android.bluetooth.BluetoothGattServerCallback
11: import android.bluetooth.BluetoothGattService
12: import android.bluetooth.BluetoothManager
13: import android.bluetooth.BluetoothProfile
14: import android.content.Context
15: import android.os.Build
16: import java.util.UUID
17: import java.util.concurrent.ConcurrentHashMap
18: 
19: /**
20:  * GATT fallback transport for the bileichat BLE mesh.
21:  *
22:  * Provides the same wire contract as the extended-advertising plane but over GATT connections,
23:  * so phones that do not support BLE 5 extended advertising can still exchange 226-byte frames.
24:  *
25:  * Wire contract (shared with the laptop client — do NOT change UUIDs):
26:  *   Service  : 6c6f6361-6c6d-4573-6800-000000000001  (same as advertising service UUID)
27:  *   FRAME_TX : 6c6f6361-6c6d-4573-6800-000000000002  READ | NOTIFY  — our current outgoing frame
28:  *   FRAME_RX : 6c6f6361-6c6d-4573-6800-000000000003  WRITE | WRITE_NO_RESPONSE — peers send here
29:  *   CCCD     : 00002902-0000-1000-8000-00805f9b34fb
30:  *
31:  * Invariants enforced:
32:  *   - Received byte arrays that are not exactly 226 bytes are silently dropped.
33:  *   - No frame bytes are parsed in Kotlin; parsing is done by Rust core via onFrame callback.
34:  *   - Nothing is labeled encrypted or private.
35:  */
36: @SuppressLint("MissingPermission")
37: class GattPlane(
38:     private val ctx: Context,
39:     /** Called with (frameBytes, rssi) when a valid 226-byte frame arrives from a peer. */
40:     private val onFrame: (ByteArray, Int) -> Unit,
41:     private val onDebug: (String) -> Unit
42: ) {
43:     companion object {
44:         val SERVICE_UUID: UUID        = UUID.fromString("6c6f6361-6c6d-4573-6800-000000000001")
45:         val CHAR_FRAME_TX: UUID       = UUID.fromString("6c6f6361-6c6d-4573-6800-000000000002")
46:         val CHAR_FRAME_RX: UUID       = UUID.fromString("6c6f6361-6c6d-4573-6800-000000000003")
47:         val CCCD_UUID: UUID           = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
48: 
49:         private const val FRAME_SIZE = 226
50:         private const val MTU_REQUEST = 247
51:         /** Minimum MTU that fits a 226-byte ATT notification (ATT overhead = 3 bytes). */
52:         private const val MTU_MIN_FOR_NOTIFY = 229
53:         /** Maximum simultaneous outbound GATT client connections. */
54:         private const val MAX_PEERS = 3
55:         /** Maximum tracked peer entries in the peers map (bounds memory). */
56:         private const val MAX_TRACKED_PEERS = 32
57:         /** Maximum entries in the scan-RSSI cache. */
58:         private const val MAX_SCAN_RSSI = 64
59:         /** Reconnect back-off after disconnection (ms). */
60:         private const val RECONNECT_BACKOFF_MS = 5_000L
61:         /** RSSI poll interval per connection (ms). */
62:         private const val RSSI_POLL_MS = 5_000L
63: 
64:         // ---- B9: exhaustion caps ----
65:         /** Maximum simultaneous subscribed centrals on our server. */
66:         private const val MAX_SUBSCRIBERS = 4
67:         /** Centrals with no GATT activity for this long are disconnected (ms). */
68:         private const val CENTRAL_IDLE_TIMEOUT_MS = 60_000L
69:         /** A legacy peer must beacon persistently for this long before we spend a client
70:          *  slot on it (three cheap dongles flashing the UUID must not exhaust slots). */
71:         private const val MIN_PEER_AGE_MS = 8_000L
72:         /** Give up on a connectGatt that never reports a state change (ms). autoConnect=false
73:          *  has no stack-level timeout, so without this a stuck attempt holds a slot forever. */
74:         private const val CONNECT_TIMEOUT_MS = 10_000L
75: 
76:         // ---- C5: app-level chunking for small MTUs ----
77:         /** First byte of a chunk write (distinguishes chunks from whole-frame writes). */
78:         private const val CHUNK_MAGIC: Byte = 0xA5.toByte()
79:         /** Stale partial chunk assemblies are dropped after this long (ms). */
80:         private const val CHUNK_BUFFER_TTL_MS = 10_000L
81:     }
82: 
83:     private val bluetoothManager =
84:         ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
85: 
86:     private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
87: 
88:     // ---- Server side -------------------------------------------------------
89: 
90:     private var gattServer: BluetoothGattServer? = null
91:     /** Devices that have enabled notifications on FRAME_TX (address -> device). */
92:     private val notifySubscribers = ConcurrentHashMap<String, BluetoothDevice>()
93:     /**
94:      * Per-device accumulation buffer for prepared (long) writes on FRAME_RX.
95:      * Key = device address.
96:      */
97:     private val preparedWriteBuffers = ConcurrentHashMap<String, ByteArray>()
98: 
99:     // B9: every connected central (subscribed or not) with its last-activity timestamp;
100:     // a watchdog disconnects idlers so cheap dongles can't hold the server open.
101:     private val centralActivity = ConcurrentHashMap<String, Pair<BluetoothDevice, Long>>()
102:     private var centralWatchdog: java.util.Timer? = null
103: 
104:     // C5: negotiated ATT MTU per central (server side) — the chunk stride for reassembly.
105:     private val centralMtu = ConcurrentHashMap<String, Int>()
106:     // C5: partial chunk assemblies per central (buffer, last-update-ms).
107:     private val chunkBuffers = ConcurrentHashMap<String, Pair<ByteArray, Long>>()
108: 
109:     // ---- Client side -------------------------------------------------------
110: 
111:     /** address -> PeerState */
112:     private val peers = ConcurrentHashMap<String, PeerState>()
113: 
114:     // B9: first-sighting timestamp per candidate peer — a peer must beacon persistently
115:     // for MIN_PEER_AGE_MS before we spend a connection slot on it.
116:     private val pendingPeers = ConcurrentHashMap<String, Long>()
117: 
118:     /** addr -> (rssi, tsMs) scan-RSSI cache for server-side frame RSSI resolution. Capped at MAX_SCAN_RSSI. */
119:     private val scanRssi = ConcurrentHashMap<String, Pair<Int, Long>>()
120: 
121:     /** The current outgoing 226-byte frame. Setting it notifies subscribers and writes to peers. */
122:     @Volatile
123:     var currentFrame: ByteArray = ByteArray(FRAME_SIZE)
124:         set(value) {
125:             field = value
126:             notifySubscribers(value)
127:             writeToPeers(value)
128:         }
129: 
130:     /**
131:      * B7: push a RELAYED frame to GATT peers without touching [currentFrame] (reads must
132:      * keep returning OUR outgoing frame). Legacy phones — the fallback's entire reason to
133:      * exist — previously received only originations, never multi-hop traffic.
134:      */
135:     fun relayOnce(frame: ByteArray) {
136:         notifySubscribers(frame)
137:         writeToPeers(frame)
138:     }
139: 
140:     // ---- Lifecycle ---------------------------------------------------------
141: 
142:     /** Open the GATT server. Call once from MeshService.onStartCommand. */
143:     fun start() {
144:         openServer()
145:         // B9: idle-central watchdog — a central that never subscribes/reads/writes still
146:         // holds a connection slot; disconnect it after CENTRAL_IDLE_TIMEOUT_MS.
147:         val timer = java.util.Timer("gatt-central-watchdog", true)
148:         centralWatchdog = timer
149:         timer.scheduleAtFixedRate(object : java.util.TimerTask() {
150:             override fun run() {
151:                 val now = System.currentTimeMillis()
152:                 for ((addr, pair) in centralActivity) {
153:                     // A SUBSCRIBED central is a healthy listener, not an idle squatter.
154:                     // Notifications are server-initiated, so a legacy phone that connects,
155:                     // subscribes and then quietly receives produces no server-side ATT
156:                     // traffic at all — touchCentral() fires on reads/writes/descriptor
157:                     // writes only, never on notifySubscribers(). Every such peer was
158:                     // therefore evicted 60 s after subscribing, and reconnecting cost
159:                     // RECONNECT_BACKOFF plus a fresh MIN_PEER_AGE_MS dwell: roughly 13 s
160:                     // deaf every minute, on exactly the phones this fallback plane exists
161:                     // to carry.
162:                     if (notifySubscribers.containsKey(addr)) continue
163:                     if (now - pair.second > CENTRAL_IDLE_TIMEOUT_MS) {
164:                         try {
165:                             gattServer?.cancelConnection(pair.first)
166:                             onDebug("gatt server: evicted idle central $addr")
167:                         } catch (e: Exception) {
168:                             onDebug("gatt server: idle evict $addr failed: ${e.message}")
169:                         }
170:                         centralActivity.remove(addr)
171:                     }
172:                 }
173:             }
174:         }, 30_000L, 30_000L)
175:     }
176: 
177:     /** Close GATT server and all client connections. Call from MeshService.onDestroy. */
178:     fun stop() {
179:         try {
180:             centralWatchdog?.cancel()
181:             centralWatchdog = null
182: 
183:             // Close all client GATTs.
184:             for ((addr, state) in peers) {
185:                 try {
186:                     // Cancel the per-peer RSSI timer and clear the liveness flags FIRST.
187:                     // The timer's self-cancel guard is `!state.connected || state.gatt == null`,
188:                     // and stop() used to leave both untrue while readRemoteRssi() on a closed
189:                     // BluetoothGatt merely returns false without throwing — so every peer
190:                     // leaked a daemon Timer waking every 5 s until process death, including
191:                     // after a panic wipe.
192:                     state.rssiPollHandle?.cancel()
193:                     state.rssiPollHandle = null
194:                     state.connected = false
195:                     state.connecting = false
196:                     state.gatt?.close()
197:                     state.gatt = null
198:                 } catch (e: Exception) {
199:                     onDebug("gatt close[$addr] exception: ${e.message}")
200:                 }
201:             }
202:             peers.clear()
203:             pendingPeers.clear()
204: 
205:             gattServer?.close()
206:             gattServer = null
207:             notifySubscribers.clear()
208:             centralActivity.clear()
209:             centralMtu.clear()
210:             chunkBuffers.clear()
211:             onDebug("gatt plane stopped")
212:         } catch (e: SecurityException) {
213:             onDebug("stop SecurityException: ${e.message}")
214:         } catch (e: Exception) {
215:             onDebug("stop exception: ${e.message}")
216:         }
217:     }
218: 
219:     /**
220:      * Called by MeshService from the scan callback for every scan result that advertises our
221:      * service UUID (with or without service data). If we are not already connected/connecting
222:      * to this device and we are under the peer cap, initiate a GATT connection.
223:      */
224:     fun onPeerSeen(device: BluetoothDevice, rssi: Int) {
225:         val addr = device.address
226:         // Update scan-RSSI cache for server-side frame RSSI resolution
227:         cacheScanRssi(addr, rssi)
228:         val existing = peers[addr]
229:         // Update cached RSSI even for connected peers
230:         if (existing != null) {
231:             existing.lastRssi = rssi
232:         }
233: 
234:         if (existing != null && existing.connected) return
235:         if (existing != null && existing.connecting) return
236:         // Enforce reconnect back-off
237:         if (existing != null) {
238:             val elapsed = System.currentTimeMillis() - existing.lastDisconnectMs
239:             if (elapsed < RECONNECT_BACKOFF_MS) return
240:         }
241:         // B9: require persistent beaconing before spending a slot. First sighting just
242:         // starts the clock; the connect happens on a later sighting (scan cadence ~1 s).
243:         val now = System.currentTimeMillis()
244:         val firstSeen = pendingPeers.putIfAbsent(addr, now)
245:         if (firstSeen == null) {
246:             if (pendingPeers.size > 32) pendingPeers.entries.minByOrNull { it.value }?.key?.let { pendingPeers.remove(it) }
247:             return
248:         }
249:         if (now - firstSeen < MIN_PEER_AGE_MS) return
250:         // Peer cap
251:         val activeCount = peers.values.count { it.connected || it.connecting }
252:         if (activeCount >= MAX_PEERS) return
253: 
254:         pendingPeers.remove(addr)
255:         connectPeer(device, rssi)
256:     }
257: 
258:     // ---- Server implementation ---------------------------------------------
259: 
260:     private fun openServer() {
261:         try {
262:             val service = BluetoothGattService(
263:                 SERVICE_UUID,
264:                 BluetoothGattService.SERVICE_TYPE_PRIMARY
265:             )
266: 
267:             // FRAME_TX: READ | NOTIFY
268:             val frameTx = BluetoothGattCharacteristic(
269:                 CHAR_FRAME_TX,
270:                 BluetoothGattCharacteristic.PROPERTY_READ or
271:                     BluetoothGattCharacteristic.PROPERTY_NOTIFY,
272:                 BluetoothGattCharacteristic.PERMISSION_READ
273:             )
274:             val cccd = BluetoothGattDescriptor(
275:                 CCCD_UUID,
276:                 BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
277:             )
278:             frameTx.addDescriptor(cccd)
279: 
280:             // FRAME_RX: WRITE | WRITE_NO_RESPONSE
281:             val frameRx = BluetoothGattCharacteristic(
282:                 CHAR_FRAME_RX,
283:                 BluetoothGattCharacteristic.PROPERTY_WRITE or
284:                     BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
285:                 BluetoothGattCharacteristic.PERMISSION_WRITE
286:             )
287: 
288:             service.addCharacteristic(frameTx)
289:             service.addCharacteristic(frameRx)
290: 
291:             val server = bluetoothManager.openGattServer(ctx, serverCallback)
292:             if (server == null) {
293:                 onDebug("gatt server: openGattServer returned null")
294:                 return
295:             }
296:             server.addService(service)
297:             gattServer = server
298:             onDebug("gatt server: opened")
299:         } catch (e: SecurityException) {
300:             onDebug("openServer SecurityException: ${e.message}")
301:         } catch (e: Exception) {
302:             onDebug("openServer exception: ${e.message}")
303:         }
304:     }
305: 
306:     private val serverCallback = object : BluetoothGattServerCallback() {
307: 
308:         override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
309:             val addr = device.address
310:             if (newState == BluetoothProfile.STATE_DISCONNECTED) {
311:                 notifySubscribers.remove(addr)
312:                 preparedWriteBuffers.remove(addr)
313:                 centralActivity.remove(addr)
314:                 centralMtu.remove(addr)
315:                 chunkBuffers.remove(addr)
316:                 onDebug("gatt server: central $addr disconnected (status=$status)")
317:             } else if (newState == BluetoothProfile.STATE_CONNECTED) {
318:                 centralActivity[addr] = Pair(device, System.currentTimeMillis())
319:                 onDebug("gatt server: central $addr connected")
320:             }
321:         }
322: 
323:         override fun onMtuChanged(device: BluetoothDevice, mtu: Int) {
324:             centralMtu[device.address] = mtu
325:         }
326: 
327:         override fun onCharacteristicReadRequest(
328:             device: BluetoothDevice,
329:             requestId: Int,
330:             offset: Int,
331:             characteristic: BluetoothGattCharacteristic
332:         ) {
333:             touchCentral(device)
334:             if (characteristic.uuid != CHAR_FRAME_TX) {
335:                 gattServer?.sendResponse(device, requestId,
336:                     BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED, 0, null)
337:                 return
338:             }
339:             val frame = currentFrame
340:             val data = if (offset < frame.size) frame.copyOfRange(offset, frame.size) else ByteArray(0)
341:             gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, data)
342:         }
343: 
344:         override fun onCharacteristicWriteRequest(
345:             device: BluetoothDevice,
346:             requestId: Int,
347:             characteristic: BluetoothGattCharacteristic,
348:             preparedWrite: Boolean,
349:             responseNeeded: Boolean,
350:             offset: Int,
351:             value: ByteArray?
352:         ) {
353:             if (characteristic.uuid != CHAR_FRAME_RX) {
354:                 if (responseNeeded) {
355:                     gattServer?.sendResponse(device, requestId,
356:                         BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED, 0, null)
357:                 }
358:                 return
359:             }
360:             if (value == null) {
361:                 if (responseNeeded) {
362:                     gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null)
363:                 }
364:                 return
365:             }
366:             touchCentral(device)
367: 
368:             if (preparedWrite) {
369:                 // Accumulate for long write; enforce frame bound
370:                 val addr = device.address
371:                 if (offset < 0 || offset + value.size > FRAME_SIZE) {
372:                     preparedWriteBuffers.remove(addr)
373:                     if (responseNeeded) {
374:                         gattServer?.sendResponse(device, requestId,
375:                             BluetoothGatt.GATT_INVALID_OFFSET, offset, null)
376:                     }
377:                     return
378:                 }
379:                 val current = preparedWriteBuffers.getOrDefault(addr, ByteArray(0))
380:                 val needed = offset + value.size
381:                 val buf = if (current.size < needed) current.copyOf(needed) else current
382:                 value.copyInto(buf, offset)
383:                 preparedWriteBuffers[addr] = buf
384:                 if (responseNeeded) {
385:                     gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null)
386:                 }
387:             } else {
388:                 if (responseNeeded) {
389:                     gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null)
390:                 }
391:                 // Immediate write: enforce 226-byte invariant
392:                 if (value.size == FRAME_SIZE) {
393:                     val rssi = resolveServerRssi(device.address)
394:                     onFrame(value, rssi)
395:                 } else if (value.size >= 2 && value[0] == CHUNK_MAGIC) {
396:                     // C5: app-level chunk from a small-MTU peer — reassemble.
397:                     handleChunk(device, value)
398:                 }
399:                 // Silently drop anything else (invariant)
400:             }
401:         }
402: 
403:         /** C5: accumulate one chunk; emit the frame when 226 bytes have assembled. */
404:         private fun handleChunk(device: BluetoothDevice, value: ByteArray) {
405:             val addr = device.address
406:             val now = System.currentTimeMillis()
407:             val stride = ((centralMtu[addr] ?: 23) - 5).coerceAtLeast(8)
408:             val seq = value[1].toInt() and 0xFF
409:             val offset = seq * stride
410:             if (offset >= FRAME_SIZE) {
411:                 chunkBuffers.remove(addr)
412:                 return
413:             }
414:             val stale = chunkBuffers[addr]
415:             val buf = if (stale == null || now - stale.second > CHUNK_BUFFER_TTL_MS) {
416:                 ByteArray(FRAME_SIZE)
417:             } else stale.first
418:             val n = minOf(value.size - 2, FRAME_SIZE - offset)
419:             value.copyInto(buf, offset, 2, 2 + n)
420:             val assembledEnd = offset + n
421:             if (assembledEnd >= FRAME_SIZE) {
422:                 chunkBuffers.remove(addr)
423:                 val rssi = resolveServerRssi(addr)
424:                 onFrame(buf, rssi)
425:             } else {
426:                 chunkBuffers[addr] = Pair(buf, now)
427:             }
428:         }
429: 
430:         override fun onExecuteWrite(device: BluetoothDevice, requestId: Int, execute: Boolean) {
431:             gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
432:             if (!execute) {
433:                 preparedWriteBuffers.remove(device.address)
434:                 return
435:             }
436:             val buf = preparedWriteBuffers.remove(device.address) ?: return
437:             if (buf.size == FRAME_SIZE) {
438:                 val rssi = resolveServerRssi(device.address)
439:                 onFrame(buf, rssi)
440:             }
441:             // Silently drop non-226-byte buffers (invariant)
442:         }
443: 
444:         override fun onDescriptorWriteRequest(
445:             device: BluetoothDevice,
446:             requestId: Int,
447:             descriptor: BluetoothGattDescriptor,
448:             preparedWrite: Boolean,
449:             responseNeeded: Boolean,
450:             offset: Int,
451:             value: ByteArray?
452:         ) {
453:             if (responseNeeded) {
454:                 gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
455:             }
456:             if (descriptor.uuid != CCCD_UUID) return
457:             touchCentral(device)
458:             val addr = device.address
459:             val enabled = value != null &&
460:                 value.size >= 2 &&
461:                 value[0] == BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE[0] &&
462:                 value[1] == BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE[1]
463:             if (enabled) {
464:                 // B9: bound the subscriber set — beyond the cap, refuse and drop the central.
465:                 if (!notifySubscribers.containsKey(addr) && notifySubscribers.size >= MAX_SUBSCRIBERS) {
466:                     onDebug("gatt server: subscriber cap reached — refusing $addr")
467:                     try {
468:                         gattServer?.cancelConnection(device)
469:                     } catch (e: Exception) {
470:                         onDebug("gatt server: refuse $addr failed: ${e.message}")
471:                     }
472:                     return
473:                 }
474:                 notifySubscribers[addr] = device
475:                 onDebug("gatt server: $addr subscribed to FRAME_TX")
476:             } else {
477:                 notifySubscribers.remove(addr)
478:                 onDebug("gatt server: $addr unsubscribed from FRAME_TX")
479:             }
480:         }
481:     }
482: 
483:     /** B9: refresh a central's last-activity timestamp (read/write/descriptor traffic). */
484:     private fun touchCentral(device: BluetoothDevice) {
485:         centralActivity[device.address] = Pair(device, System.currentTimeMillis())
486:     }
487: 
488:     /** Resolve RSSI for a server-side frame: connection RSSI if tracked, else scan cache, else -127. */
489:     private fun resolveServerRssi(addr: String): Int {
490:         val peer = peers[addr]
491:         if (peer != null && peer.lastRssi != -127) return peer.lastRssi
492:         return scanRssi[addr]?.first ?: -127
493:     }
494: 
495:     /** Push [frame] to all subscribed centrals via GATT notification. */
496:     private fun notifySubscribers(frame: ByteArray) {
497:         val server = gattServer ?: return
498:         val service = server.getService(SERVICE_UUID) ?: return
499:         val char = service.getCharacteristic(CHAR_FRAME_TX) ?: return
500:         for ((addr, device) in notifySubscribers) {
501:             try {
502:                 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
503:                     // API 33+: pass value directly, avoids deprecated value setter
504:                     val result = server.notifyCharacteristicChanged(device, char, false, frame)
505:                     if (result != BluetoothGatt.GATT_SUCCESS) {
506:                         onDebug("gatt server: notify $addr result=$result")
507:                     }
508:                 } else {
509:                     @Suppress("DEPRECATION")
510:                     char.value = frame
511:                     @Suppress("DEPRECATION")
512:                     val ok = server.notifyCharacteristicChanged(device, char, false)
513:                     if (!ok) onDebug("gatt server: notify $addr returned false")
514:                 }
515:             } catch (e: SecurityException) {
516:                 onDebug("notifySubscribers[$addr] SecurityException: ${e.message}")
517:             } catch (e: Exception) {
518:                 onDebug("notifySubscribers[$addr] exception: ${e.message}")
519:             }
520:         }
521:     }
522: 
523:     // ---- Client implementation ---------------------------------------------
524: 
525:     /** Per-peer connection state. */
526:     private inner class PeerState(val address: String) {
527:         @Volatile var gatt: BluetoothGatt? = null
528:         @Volatile var connected = false
529:         @Volatile var connecting = false
530:         @Volatile var lastRssi: Int = -127
531:         @Volatile var lastDisconnectMs: Long = 0L
532:         /** MTU negotiated for this connection. */
533:         @Volatile var mtu: Int = 23
534:         /** True once FRAME_TX notifications have been enabled. */
535:         @Volatile var notifyEnabled = false
536:         /** True once the initial FRAME_TX read is done. */
537:         @Volatile var initialReadDone = false
538:         /** True once we have written our own frame to FRAME_RX. */
539:         @Volatile var initialWriteDone = false
540: 
541:         // C5: serialized chunk writes for small MTUs (one ATT write in flight per peer).
542:         val writeLock = Any()
543:         val pendingWrites = java.util.ArrayDeque<ByteArray>()
544:         @Volatile var writeInFlight = false
545: 
546:         // Running RSSI poll: cancelled by clearing gatt reference
547:         @Volatile var rssiPollHandle: java.util.Timer? = null
548:     }
549: 
550:     /** Update the scan-RSSI cache, evicting the oldest entry when over cap. */
551:     private fun cacheScanRssi(addr: String, rssi: Int) {
552:         scanRssi[addr] = Pair(rssi, System.currentTimeMillis())
553:         if (scanRssi.size > MAX_SCAN_RSSI) {
554:             val oldest = scanRssi.entries.minByOrNull { it.value.second }?.key
555:             if (oldest != null && oldest != addr) scanRssi.remove(oldest)
556:         }
557:     }
558: 
559:     private fun connectPeer(device: BluetoothDevice, rssi: Int) {
560:         val addr = device.address
561:         // Bound tracked peers: evict least-recently-active before inserting a new one
562:         if (!peers.containsKey(addr) && peers.size >= MAX_TRACKED_PEERS) {
563:             val oldest = peers.entries
564:                 .filter { !it.value.connected && !it.value.connecting }
565:                 .minByOrNull { it.value.lastDisconnectMs }
566:                 ?: peers.entries.minByOrNull { it.value.lastDisconnectMs }
567:             oldest?.let { peers.remove(it.key) }
568:         }
569:         val state = PeerState(addr).also {
570:             it.lastRssi = rssi
571:             it.connecting = true
572:         }
573:         peers[addr] = state
574:         onDebug("gatt client: connecting to $addr")
575:         try {
576:             val gatt = device.connectGatt(ctx, false, makeClientCallback(state),
577:                 BluetoothDevice.TRANSPORT_LE)
578:             state.gatt = gatt
579:             if (gatt == null) {
580:                 // connectGatt can return null. Nothing resets `connecting` in that case, so
581:                 // the slot was held forever by a connection that never existed.
582:                 onDebug("connectGatt[$addr] returned null — freeing slot")
583:                 state.connecting = false
584:                 peers.remove(addr)
585:                 return
586:             }
587:             // With autoConnect=false there is no stack-level connect timeout: on a marginal
588:             // link onConnectionStateChange may take ~30 s or never fire at all. The peer cap
589:             // counts `connected || connecting`, so a few stuck attempts burned every client
590:             // slot until process death — a cheap dongle that beacons past MIN_PEER_AGE_MS and
591:             // then goes quiet could pin them deliberately.
592:             mainHandler.postDelayed({
593:                 if (state.connecting && !state.connected) {
594:                     onDebug("gatt client: connect to $addr timed out — freeing slot")
595:                     state.connecting = false
596:                     try {
597:                         state.gatt?.disconnect()
598:                         state.gatt?.close()
599:                     } catch (e: Exception) {
600:                         onDebug("connect-timeout close[$addr] failed: ${e.message}")
601:                     }
602:                     state.gatt = null
603:                     peers.remove(addr)
604:                 }
605:             }, CONNECT_TIMEOUT_MS)
606:         } catch (e: SecurityException) {
607:             onDebug("connectGatt[$addr] SecurityException: ${e.message}")
608:             state.connecting = false
609:             peers.remove(addr)
610:         } catch (e: Exception) {
611:             onDebug("connectGatt[$addr] exception: ${e.message}")
612:             state.connecting = false
613:             peers.remove(addr)
614:         }
615:     }
616: 
617:     private fun makeClientCallback(state: PeerState) = object : BluetoothGattCallback() {
618: 
619:         override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
620:             val addr = state.address
621:             if (newState == BluetoothProfile.STATE_CONNECTED) {
622:                 state.connected = true
623:                 state.connecting = false
624:                 onDebug("gatt client: connected to $addr (status=$status)")
625:                 try {
626:                     gatt.requestMtu(MTU_REQUEST)
627:                 } catch (e: SecurityException) {
628:                     onDebug("requestMtu[$addr] SecurityException: ${e.message}")
629:                     disconnectPeer(state)
630:                 }
631:             } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
632:                 onDebug("gatt client: disconnected from $addr (status=$status)")
633:                 disconnectPeer(state)
634:             }
635:         }
636: 
637:         override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
638:             val addr = state.address
639:             state.mtu = mtu
640:             if (mtu < MTU_MIN_FOR_NOTIFY) {
641:                 onDebug("gatt client: $addr MTU=$mtu < $MTU_MIN_FOR_NOTIFY; chunking writes + polling reads")
642:             } else {
643:                 onDebug("gatt client: $addr MTU=$mtu ok")
644:             }
645:             try {
646:                 gatt.discoverServices()
647:             } catch (e: SecurityException) {
648:                 onDebug("discoverServices[$addr] SecurityException: ${e.message}")
649:                 disconnectPeer(state)
650:             }
651:         }
652: 
653:         override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
654:             val addr = state.address
655:             if (status != BluetoothGatt.GATT_SUCCESS) {
656:                 onDebug("gatt client: $addr discoverServices failed status=$status")
657:                 disconnectPeer(state)
658:                 return
659:             }
660:             val service = gatt.getService(SERVICE_UUID)
661:             if (service == null) {
662:                 onDebug("gatt client: $addr service not found — not a mesh peer")
663:                 disconnectPeer(state)
664:                 return
665:             }
666:             val frameTx = service.getCharacteristic(CHAR_FRAME_TX)
667:             if (frameTx == null) {
668:                 onDebug("gatt client: $addr FRAME_TX characteristic not found")
669:                 disconnectPeer(state)
670:                 return
671:             }
672:             // Step 1: enable notifications
673:             try {
674:                 val ok = gatt.setCharacteristicNotification(frameTx, true)
675:                 if (!ok) {
676:                     onDebug("gatt client: $addr setCharacteristicNotification failed")
677:                 }
678:                 val cccd = frameTx.getDescriptor(CCCD_UUID)
679:                 if (cccd != null) {
680:                     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
681:                         gatt.writeDescriptor(cccd,
682:                             BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
683:                     } else {
684:                         @Suppress("DEPRECATION")
685:                         cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
686:                         @Suppress("DEPRECATION")
687:                         gatt.writeDescriptor(cccd)
688:                     }
689:                 } else {
690:                     onDebug("gatt client: $addr CCCD not found; skipping notify enable")
691:                     doInitialRead(gatt, state)
692:                 }
693:             } catch (e: SecurityException) {
694:                 onDebug("enableNotify[$addr] SecurityException: ${e.message}")
695:                 disconnectPeer(state)
696:             }
697:         }
698: 
699:         override fun onDescriptorWrite(
700:             gatt: BluetoothGatt,
701:             descriptor: BluetoothGattDescriptor,
702:             status: Int
703:         ) {
704:             val addr = state.address
705:             if (descriptor.uuid == CCCD_UUID) {
706:                 if (status == BluetoothGatt.GATT_SUCCESS) {
707:                     state.notifyEnabled = true
708:                     onDebug("gatt client: $addr FRAME_TX notify enabled")
709:                 } else {
710:                     onDebug("gatt client: $addr CCCD write failed status=$status")
711:                 }
712:                 // Step 2: initial read regardless of notify outcome
713:                 doInitialRead(gatt, state)
714:             }
715:         }
716: 
717:         override fun onCharacteristicRead(
718:             gatt: BluetoothGatt,
719:             characteristic: BluetoothGattCharacteristic,
720:             status: Int
721:         ) {
722:             val addr = state.address
723:             if (characteristic.uuid != CHAR_FRAME_TX) return
724:             if (status == BluetoothGatt.GATT_SUCCESS) {
725:                 @Suppress("DEPRECATION")
726:                 val value = characteristic.value
727:                 if (value != null && value.size == FRAME_SIZE) {
728:                     onFrame(value, state.lastRssi)
729:                 }
730:                 // Silently drop non-226-byte (invariant)
731:             } else {
732:                 onDebug("gatt client: $addr FRAME_TX read failed status=$status")
733:             }
734:             if (!state.initialReadDone) {
735:                 state.initialReadDone = true
736:                 // Step 3: write our frame to FRAME_RX
737:                 doInitialWrite(gatt, state)
738:             }
739:         }
740: 
741:         // API 33+ override with value parameter
742:         override fun onCharacteristicRead(
743:             gatt: BluetoothGatt,
744:             characteristic: BluetoothGattCharacteristic,
745:             value: ByteArray,
746:             status: Int
747:         ) {
748:             val addr = state.address
749:             if (characteristic.uuid != CHAR_FRAME_TX) return
750:             if (status == BluetoothGatt.GATT_SUCCESS) {
751:                 if (value.size == FRAME_SIZE) {
752:                     onFrame(value, state.lastRssi)
753:                 }
754:             } else {
755:                 onDebug("gatt client: $addr FRAME_TX read failed status=$status")
756:             }
757:             if (!state.initialReadDone) {
758:                 state.initialReadDone = true
759:                 doInitialWrite(gatt, state)
760:             }
761:         }
762: 
763:         override fun onCharacteristicChanged(
764:             gatt: BluetoothGatt,
765:             characteristic: BluetoothGattCharacteristic
766:         ) {
767:             if (characteristic.uuid != CHAR_FRAME_TX) return
768:             @Suppress("DEPRECATION")
769:             val value = characteristic.value ?: return
770:             if (value.size == FRAME_SIZE) {
771:                 onFrame(value, state.lastRssi)
772:             }
773:             // Silently drop non-226-byte (invariant)
774:         }
775: 
776:         // API 33+ override with value parameter
777:         override fun onCharacteristicChanged(
778:             gatt: BluetoothGatt,
779:             characteristic: BluetoothGattCharacteristic,
780:             value: ByteArray
781:         ) {
782:             if (characteristic.uuid != CHAR_FRAME_TX) return
783:             if (value.size == FRAME_SIZE) {
784:                 onFrame(value, state.lastRssi)
785:             }
786:         }
787: 
788:         override fun onCharacteristicWrite(
789:             gatt: BluetoothGatt,
790:             characteristic: BluetoothGattCharacteristic,
791:             status: Int
792:         ) {
793:             val addr = state.address
794:             if (characteristic.uuid == CHAR_FRAME_RX) {
795:                 if (status != BluetoothGatt.GATT_SUCCESS) {
796:                     onDebug("gatt client: $addr FRAME_RX write failed status=$status")
797:                     synchronized(state.writeLock) {
798:                         state.pendingWrites.clear()
799:                         state.writeInFlight = false
800:                     }
801:                     // Start the poll anyway. Sending and receiving are independent: for a
802:                     // small-MTU peer (<229) notifications cannot carry a whole frame, so the
803:                     // RSSI poll's read-back IS the only receive path. Gating it on the first
804:                     // write succeeding meant one transient GATT_BUSY left the link "connected"
805:                     // but permanently unable to hear that peer, with no retry.
806:                     if (!state.initialWriteDone) {
807:                         state.initialWriteDone = true
808:                         onDebug("gatt client: $addr initial write failed; starting RSSI poll anyway")
809:                         startRssiPoll(gatt, state)
810:                     }
811:                     return
812:                 }
813:                 // C5: continue a chunk chain, or finish.
814:                 val more = synchronized(state.writeLock) {
815:                     if (state.pendingWrites.isNotEmpty()) {
816:                         val service = gatt.getService(SERVICE_UUID)
817:                         val char = service?.getCharacteristic(CHAR_FRAME_RX)
818:                         if (char != null) writeNextChunkLocked(gatt, state, char)
819:                         true
820:                     } else {
821:                         state.writeInFlight = false
822:                         false
823:                     }
824:                 }
825:                 if (!more && !state.initialWriteDone) {
826:                     state.initialWriteDone = true
827:                     onDebug("gatt client: $addr initial FRAME_RX write ok; starting RSSI poll")
828:                     startRssiPoll(gatt, state)
829:                 }
830:             }
831:         }
832: 
833:         override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
834:             if (status == BluetoothGatt.GATT_SUCCESS) {
835:                 state.lastRssi = rssi
836:             }
837:         }
838:     }
839: 
840:     private fun doInitialRead(gatt: BluetoothGatt, state: PeerState) {
841:         val addr = state.address
842:         val service = gatt.getService(SERVICE_UUID) ?: return
843:         val char = service.getCharacteristic(CHAR_FRAME_TX) ?: return
844:         try {
845:             val ok = gatt.readCharacteristic(char)
846:             if (!ok) {
847:                 onDebug("gatt client: $addr readCharacteristic returned false")
848:                 // Proceed to write anyway
849:                 state.initialReadDone = true
850:                 doInitialWrite(gatt, state)
851:             }
852:         } catch (e: SecurityException) {
853:             onDebug("readCharacteristic[$addr] SecurityException: ${e.message}")
854:             disconnectPeer(state)
855:         }
856:     }
857: 
858:     private fun doInitialWrite(gatt: BluetoothGatt, state: PeerState) {
859:         writeFrameToGatt(gatt, state, currentFrame)
860:     }
861: 
862:     /** Write [frame] to the FRAME_RX characteristic of a connected peer. */
863:     private fun writeFrameToGatt(gatt: BluetoothGatt, state: PeerState, frame: ByteArray) {
864:         val addr = state.address
865:         val service = gatt.getService(SERVICE_UUID) ?: return
866:         val char = service.getCharacteristic(CHAR_FRAME_RX) ?: return
867:         // C5: below MTU_MIN_FOR_NOTIFY a 226-byte write just fails — chunk the frame into
868:         // (mtu-5)-byte payloads with a 2-byte header; the peer's server reassembles.
869:         if (state.mtu < MTU_MIN_FOR_NOTIFY) {
870:             enqueueChunkedWrite(gatt, state, char, frame)
871:             return
872:         }
873:         writeOne(gatt, state, char, frame)
874:     }
875: 
876:     private fun enqueueChunkedWrite(
877:         gatt: BluetoothGatt,
878:         state: PeerState,
879:         char: BluetoothGattCharacteristic,
880:         frame: ByteArray
881:     ) {
882:         val payload = (state.mtu - 5).coerceAtLeast(8)
883:         synchronized(state.writeLock) {
884:             var offset = 0
885:             var seq = 0
886:             while (offset < frame.size) {
887:                 val n = minOf(payload, frame.size - offset)
888:                 val chunk = ByteArray(2 + n)
889:                 chunk[0] = CHUNK_MAGIC
890:                 chunk[1] = seq.toByte()
891:                 frame.copyInto(chunk, 2, offset, offset + n)
892:                 state.pendingWrites.add(chunk)
893:                 offset += n
894:                 seq++
895:             }
896:             if (!state.writeInFlight) {
897:                 state.writeInFlight = true
898:                 writeNextChunkLocked(gatt, state, char)
899:             }
900:         }
901:     }
902: 
903:     /** Caller must hold state.writeLock. */
904:     private fun writeNextChunkLocked(
905:         gatt: BluetoothGatt,
906:         state: PeerState,
907:         char: BluetoothGattCharacteristic
908:     ) {
909:         val next = state.pendingWrites.poll()
910:         if (next == null) {
911:             state.writeInFlight = false
912:             return
913:         }
914:         val ok = writeOne(gatt, state, char, next)
915:         if (!ok) {
916:             state.pendingWrites.clear()
917:             state.writeInFlight = false
918:         }
919:     }
920: 
921:     /** Fire one ATT write. Returns false when the stack refused it outright. */
922:     private fun writeOne(
923:         gatt: BluetoothGatt,
924:         state: PeerState,
925:         char: BluetoothGattCharacteristic,
926:         data: ByteArray
927:     ): Boolean {
928:         val addr = state.address
929:         return try {
930:             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
931:                 val result = gatt.writeCharacteristic(
932:                     char, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
933:                 if (result != BluetoothGatt.GATT_SUCCESS) {
934:                     onDebug("gatt client: $addr writeCharacteristic result=$result")
935:                 }
936:                 result == BluetoothGatt.GATT_SUCCESS
937:             } else {
938:                 @Suppress("DEPRECATION")
939:                 char.value = data
940:                 @Suppress("DEPRECATION")
941:                 char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
942:                 @Suppress("DEPRECATION")
943:                 val ok = gatt.writeCharacteristic(char)
944:                 if (!ok) onDebug("gatt client: $addr writeCharacteristic returned false")
945:                 ok
946:             }
947:         } catch (e: SecurityException) {
948:             onDebug("writeCharacteristic[$addr] SecurityException: ${e.message}")
949:             false
950:         } catch (e: Exception) {
951:             onDebug("writeCharacteristic[$addr] exception: ${e.message}")
952:             false
953:         }
954:     }
955: 
956:     /** Start a periodic RSSI read on a 5 s cadence for [state]'s connection. */
957:     private fun startRssiPoll(gatt: BluetoothGatt, state: PeerState) {
958:         val timer = java.util.Timer("rssi-${state.address}", true)
959:         state.rssiPollHandle = timer
960:         timer.scheduleAtFixedRate(object : java.util.TimerTask() {
961:             override fun run() {
962:                 if (!state.connected || state.gatt == null) {
963:                     cancel()
964:                     return
965:                 }
966:                 try {
967:                     gatt.readRemoteRssi()
968:                     // C5: below MTU_MIN_FOR_NOTIFY the peer's 226-byte notifications never
969:                     // arrive — poll-read FRAME_TX instead (server supports offset reads,
970:                     // so the stack's blob-read assembles the full frame).
971:                     // Skip the read while a chunked write is in flight: Android serializes
972:                     // one GATT operation per connection, so an overlapping read makes the
973:                     // next writeCharacteristic return false and writeNextChunkLocked drops
974:                     // the whole queue — losing that frame to this peer entirely. The next
975:                     // tick 5 s later picks the read back up.
976:                     val busy = synchronized(state.writeLock) { state.writeInFlight }
977:                     if (state.mtu < MTU_MIN_FOR_NOTIFY && !busy) {
978:                         val char = gatt.getService(SERVICE_UUID)?.getCharacteristic(CHAR_FRAME_TX)
979:                         if (char != null) gatt.readCharacteristic(char)
980:                     }
981:                 } catch (e: SecurityException) {
982:                     onDebug("readRemoteRssi[${state.address}] SecurityException: ${e.message}")
983:                     cancel()
984:                 } catch (e: Exception) {
985:                     cancel()
986:                 }
987:             }
988:         }, RSSI_POLL_MS, RSSI_POLL_MS)
989:     }
990: 
991:     /** Write the current frame to FRAME_RX on every connected peer. */
992:     private fun writeToPeers(frame: ByteArray) {
993:         for ((_, state) in peers) {
994:             if (!state.connected) continue
995:             val gatt = state.gatt ?: continue
996:             writeFrameToGatt(gatt, state, frame)
997:         }
998:     }
999: 
1000:     private fun disconnectPeer(state: PeerState) {
1001:         state.rssiPollHandle?.cancel()
1002:         state.rssiPollHandle = null
1003:         state.connected = false
1004:         state.connecting = false
1005:         state.notifyEnabled = false
1006:         state.initialReadDone = false
1007:         state.initialWriteDone = false
1008:         synchronized(state.writeLock) {
1009:             state.pendingWrites.clear()
1010:             state.writeInFlight = false
1011:         }
1012:         state.lastDisconnectMs = System.currentTimeMillis()
1013:         try {
1014:             state.gatt?.close()
1015:         } catch (e: Exception) {
1016:             onDebug("gatt close[${state.address}] exception: ${e.message}")
1017:         }
1018:         state.gatt = null
1019:         // Keep state in map so back-off timer works; onPeerSeen will reconnect after back-off
1020:         onDebug("gatt client: ${state.address} disconnected; back-off ${RECONNECT_BACKOFF_MS}ms")
1021:     }
1022: }

(End of file - total 1022 lines)
</content>
```

---

## Assistant (Plan · Kimi K3 (2x usage) · 9.5s)

_Thinking:_

I now have a comprehensive picture. Let me check one more thing — Measurement.kt for sketch bucketing and neighborsDirect, since presence counting and PoCP sketch timing matter for communication scenarios.

**Tool: read**

**Input:**
```json
{
  "filePath": "/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/Measurement.kt"
}
```

**Output:**
```
<path>/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/Measurement.kt</path>
<type>file</type>
<content>
1: package org.bileichat.mesh
2: 
3: import org.json.JSONArray
4: import org.json.JSONObject
5: import uniffi.mesh_core.observeMarks
6: import java.util.concurrent.ConcurrentHashMap
7: 
8: data class Heard(
9:     val epoch: UInt,
10:     val markHex: String,
11:     val rssi: Int,
12:     val tsMs: Long
13: )
14: 
15: class Measurement {
16:     private val rowsLock = Any()
17: 
18:     // All raw rows, in insertion order (guarded by rowsLock)
19:     private val rows = ArrayDeque<Heard>()
20: 
21:     // Per-epoch set of distinct mark hex strings for fast neighbor counting
22:     private val epochMarks = ConcurrentHashMap<UInt, MutableSet<String>>()
23: 
24:     // Direct-RF presence: marks heard at their origination TTL (relays excluded upstream),
25:     // bucketed by the frame's epoch. Marks rotate every epoch, so the previous wall-clock
26:     // window counted one physical phone 2–3 times (2–3 of its rotating marks in window).
27:     // Per-epoch buckets + max (NOT sum) cannot double-count: one device = one mark per
28:     // epoch. Relayed copies never land here (TTL-direct gate at the call site), so a
29:     // device two hops away is not "nearby".
30:     private val directMarks = ConcurrentHashMap<UInt, MutableSet<String>>()
31: 
32:     // P2/P5: this device's OWN mark for each epoch it advertised in. A cell is "the devices
33:     // in RF range of each other, INCLUDING me" — excluding self made two mutual neighbours
34:     // hold disjoint sketches (A held {mark_B}, B held {mark_A}), which is Jaccard 0.0 and a
35:     // guaranteed CellMismatch. It also let a phone that had heard nobody yet build an EMPTY
36:     // sketch and originate a witnessless frame, which every receiver relays but never shows.
37:     // Kept in its own map (not routed through record()) so totalHeard() and exportJson stay
38:     // honest about what was actually heard over the air.
39:     private val selfMarks = ConcurrentHashMap<UInt, String>()
40: 
41:     private companion object {
42:         const val MAX_ROWS = 4000
43:         const val MAX_EPOCHS = 32
44: 
45:         /** RSSI attributed to our own mark. RSSI_FLOOR_RANGE is entirely negative, so 0
46:          *  always clears the configured floor — our own presence is never RSSI-filtered. */
47:         const val SELF_RSSI = 0
48:     }
49: 
50:     fun record(mark: ByteArray, rssi: Int, epoch: UInt) {
51:         val hex = mark.joinToString("") { "%02x".format(it) }
52:         val row = Heard(epoch, hex, rssi, System.currentTimeMillis())
53:         synchronized(rowsLock) {
54:             rows.addLast(row)
55:             while (rows.size > MAX_ROWS) rows.removeFirst()
56:         }
57:         epochMarks.computeIfAbsent(epoch) {
58:             java.util.Collections.synchronizedSet(HashSet<String>())
59:         }.add(hex)
60:         if (epochMarks.size > MAX_EPOCHS) {
61:             epochMarks.keys.minOrNull()?.let { epochMarks.remove(it) }
62:         }
63:     }
64: 
65:     /** Record a DIRECT-RF mark for presence. Called before dedup, only for frames at
66:      *  their origination TTL (relays excluded). Bucketed by the frame's own epoch. */
67:     fun recordPresence(mark: ByteArray, epoch: UInt) {
68:         val hex = mark.joinToString("") { "%02x".format(it) }
69:         directMarks.computeIfAbsent(epoch) {
70:             java.util.Collections.synchronizedSet(HashSet<String>())
71:         }.add(hex)
72:         if (directMarks.size > MAX_EPOCHS) {
73:             directMarks.keys.minOrNull()?.let { directMarks.remove(it) }
74:         }
75:     }
76: 
77:     /**
78:      * Record THIS device's own mark for [epoch]. Called from the frame-origination path once
79:      * per epoch, before the local sketch is built. See [selfMarks] for why this exists.
80:      */
81:     fun recordSelf(mark: ByteArray, epoch: UInt) {
82:         selfMarks[epoch] = mark.joinToString("") { "%02x".format(it) }
83:         if (selfMarks.size > MAX_EPOCHS) {
84:             selfMarks.keys.minOrNull()?.let { selfMarks.remove(it) }
85:         }
86:     }
87: 
88:     /** True once [recordSelf] has run for [epoch] — i.e. the sketch for that epoch can
89:      *  never be empty, so origination cannot fall back to a witnessless frame. */
90:     fun hasSelfMark(epoch: UInt): Boolean = selfMarks.containsKey(epoch)
91: 
92:     fun neighborsThisEpoch(epoch: UInt): Int {
93:         return epochMarks[epoch]?.size ?: 0
94:     }
95: 
96:     /**
97:      * Estimated nearby devices: max (not sum) of the direct-RF mark counts of the current
98:      * and adjacent epoch buckets. Adjacent buckets cover sender/receiver epoch skew and
99:      * one fully-missed epoch; zero requires two consecutive silent epochs (~20 s), the
100:      * same smoothing horizon as before but rotation-proof. Deliberately not RSSI-filtered:
101:      * any frame that decoded and verified is a real transmission — the −80 dBm config
102:      * floor is a sketch/trust window, not a liveness window.
103:      */
104:     fun neighborsDirect(epoch: UInt): Int {
105:         // epoch-1 wraps to UInt.MAX_VALUE at epoch 0; that bucket never exists → 0.
106:         return maxOf(
107:             directMarks[epoch]?.size ?: 0,
108:             directMarks[epoch - 1u]?.size ?: 0,
109:             directMarks[epoch + 1u]?.size ?: 0
110:         )
111:     }
112: 
113:     fun totalHeard(): Int = synchronized(rowsLock) { rows.size }
114: 
115:     fun localSketch(epoch: UInt, seed: ByteArray, floorDbm: Int): List<ULong> {
116:         // Collect rows for this epoch
117:         val epochRows = synchronized(rowsLock) { rows.filter { it.epoch == epoch } }
118: 
119:         // Our own mark joins the cell (see [selfMarks]). Deduped against heard rows: a
120:         // relayed copy of our own frame could otherwise put our mark in twice, and KMV
121:         // slots are scarce (16).
122:         val selfHex = selfMarks[epoch]
123:         val heardHex = epochRows.map { it.markHex }
124:         val includeSelf = selfHex != null && selfHex !in heardHex
125:         if (epochRows.isEmpty() && !includeSelf) return emptyList()
126: 
127:         val markHexes = if (includeSelf) heardHex + selfHex!! else heardHex
128:         val marksFlat = markHexes.flatMap { hex ->
129:             // decode hex back to 16 bytes
130:             (hex.chunked(2).map { it.toInt(16).toByte() })
131:         }.toByteArray()
132: 
133:         val rssiList: List<Byte> =
134:             epochRows.map { it.rssi.toByte() } +
135:                 (if (includeSelf) listOf(SELF_RSSI.toByte()) else emptyList())
136: 
137:         // The KMV seed MUST be a value all co-located devices agree on, so the SAME overheard mark
138:         // hashes to the SAME u64 on every phone — otherwise Jaccard is meaningless. The epoch is that
139:         // shared value. The device's private `seed` (its advertising identity) must NOT be used here.
140:         val sketchSeed: UInt = epoch
141: 
142:         return observeMarks(marksFlat, rssiList, sketchSeed, floorDbm.toByte())
143:     }
144: 
145:     fun exportJson(cfg: MeshConfig): String {
146:         val root = JSONObject()
147: 
148:         // D6: mark sightings are RF-proximity evidence — say so inside the file itself.
149:         root.put(
150:             "warning",
151:             "CONTAINS RF-PROXIMITY DATA: mark sightings reveal which devices were " +
152:                 "physically near this phone and when. Handle like location history."
153:         )
154: 
155:         val cfgObj = JSONObject()
156:         cfgObj.put("epochMs", cfg.epochMs)
157:         cfgObj.put("tauThreshold", cfg.tauThreshold)
158:         cfgObj.put("rssiFloorDbm", cfg.rssiFloorDbm)
159:         cfgObj.put("codedPhy", cfg.codedPhy)
160:         cfgObj.put("advIntervalMs", cfg.advIntervalMs)
161:         root.put("config", cfgObj)
162: 
163:         val arr = JSONArray()
164:         val snapshot = synchronized(rowsLock) { rows.toList() }
165:         for (row in snapshot) {
166:             val obj = JSONObject()
167:             obj.put("epoch", row.epoch.toLong())
168:             obj.put("markHex", row.markHex)
169:             obj.put("rssi", row.rssi)
170:             obj.put("tsMs", row.tsMs)
171:             arr.put(obj)
172:         }
173:         root.put("heard", arr)
174: 
175:         return root.toString(2)
176:     }
177: 
178:     fun clear() {
179:         synchronized(rowsLock) { rows.clear() }
180:         epochMarks.clear()
181:         directMarks.clear()
182:         selfMarks.clear()
183:     }
184: }

(End of file - total 184 lines)
</content>
```

---

## Assistant (Plan · Kimi K3 (2x usage) · 511.5s)

_Thinking:_

I now have a complete picture of the system. Let me now synthesize:

1. Security vulnerabilities (from 3 audit agents — done)
2. Communication problems with mental scenario simulation
3. Answer the "receipt late but message isn't" question — verify the user's analysis against the code, and confirm/refine

The user pasted their own analysis of the receipt problem (epoch lockstep + jitter fix, which IS in the code: TX_JITTER_MAX_MS=1200, RELAY_JITTER_MAX_MS=350, plus 0.14 added RELAY_REPEATS_MAX=3 relay redundancy). So the current code already contains their fix. My job: verify it, and find *remaining* communication problems.

Let me simulate scenarios carefully:

**Verify the receipt analysis against code:**
- A sends BROADCAST text at epoch E. outgoingText collector → rebuildAndAdvertise(E, text). Frame airs ~every advIntervalMs=1000ms.
- B hears it → ingest → displayOk → enqueueRelay(prio 1, repeats = relayRepeatsForDensity()). With 1 neighbor, repeats=3.
- Relay drain: 250ms poll + 0-350ms jitter → advertiseRelayOnce(frame, 2000ms). Then repeats re-queued at +2600ms + jitter.
- A hears echo before dedup gate (claimOwnEcho) → receipt.
- A's own hash was inserted into dedup at origination, so echo passes claimOwnEcho then hits dedup → DUPLICATE → return. Good.

The user's analysis: correct. The dedup "relay once per epoch" (peer relays a given hash once — because subsequent copies are DUPLICATE at checkEpoch... wait, actually the relay decision happens on FRESH frames only — the peer inserts the hash after acting. So subsequent copies of the same hash are DUPLICATE → no re-relay. Right. One relay per hash per peer. And since the originator re-originates a NEW frame (new hash) each epoch for messageRepeatEpochs=3 epochs, there are 3 relay chances.

But wait — there's a subtlety: relayRepeatsForDensity gives 3 airings of the SAME relayed frame when ≤1 neighbor. Does the originator hear its echo more reliably now? The relay airs 3×2s bursts at 0s, ~2.6s, ~5.2s within the epoch. That covers more of the epoch. But do these repeats help the *echo* reach A? Yes — 3 bursts instead of 1.

However, there's an interaction worth noting: **B's relay repeats are of the frame stamped epoch E. When does A stop listening for the echo?** ownHashRetentionEpochs = messageRepeatEpochs+1 = 4 epochs. Fine.

Another subtlety: when A hears the echo at, say, E+0.5, and tier is BROADCAST: outgoingSetAtEpoch = ownEpoch+1-3 = E-2ish → at next rollover E+1, epoch - setAt = E+1-(E-2) = 3 >= 3 → message expires, text cleared. So A stops re-originating at E+1. Fine.

**Now — remaining communication problems. Let me brainstorm by scenario:**

**C1. The relay-once-per-epoch + "insert only after acting" + relay repeats creates a subtle gap: repeats happen with the same bytes, peers that missed the first burst but heard the second still relay. Fine.

**C2. Store-and-forward impossible / K4 freshness wall.** A frame lives ±2 epochs from its stamp. TTL=8 regional hops must complete within ~3 epochs (30s). In dense crowd OK; sparse/bridge scenarios fail silently. Also — a phone that was asleep (Doze prevented by wakelock, OK) or BT off for a minute never gets old alerts. By design for danger alerts, but "regional mesh flood" with TTL=8 advertising implies longer reach. Worth noting as a limitation: effective mesh diameter is min(TTL hops, 3-epoch wall-clock).

Also interplay: dedup time-decay is keyed to frame epoch. If a stale frame (2 epochs old) is relayed by a node that just heard it... it's dropped by K4 at next receiver. So multi-hop chains longer than 3 epochs die mid-path: each relay hop takes time (relay queue 250ms poll + jitter + 2s air + receiver scan latency), so 8 hops ≈ 8×~2-4s = 16-32s ≈ 2-3 epochs. Marginal! In practice, the 8th hop of a TTL=8 frame often arrives exactly at the K4 boundary → dropped as skewed. So TTL=8 is mostly unreachable except in fast-relay conditions. The clockSkewWarning would then fire on receivers ("FRAMES ARRIVING BEHIND... BEING DROPPED") — telling users to check clocks when actually the frame is just old. Misleading UX in exactly the disaster scenario.

**C3. Private message: no receipt at all (by code comment "no relayed echo is ever recognised").** The sender never learns whether anyone relayed the private frame. Given 2 re-airings, there's delivery redundancy but zero feedback. The fix used for public (rememberOwnFrame + claimOwnEcho) isn't applied to private — the private frame's hash IS inserted into dedup (line 477) but never registered in ownHashes, so an echo is swallowed as DUPLICATE silently. This is an honest-design choice per the comment ("a second tick could never honestly appear" — wait, why not? An echo of a private frame proves a peer relayed it, same as public. The comment's reasoning is circular: it's not recognized because it's not registered. Actually there IS a real reason: private frames are re-aired verbatim 3 times with the SAME hash. If ownHashes registered it, the first echo acks... that's fine. I think they just didn't implement it. It's a communication gap worth flagging: private has the weakest delivery signal (ON_AIR only) while being the tier users most want confirmation for.

Hmm wait, actually there's a subtlety: the private frame re-airs are the same bytes. claimOwnEcho acks once per origination. It would work the same. So the "could never honestly appear" comment is wrong-ish; the real cost is that an echo of a private frame only proves relay, not decryption — same caveat as public. This is a finding: private messages lack the echo receipt for no fundamental reason.

**C4. GATT plane echo blindness for legacy phones.** A legacy-only phone (no extended adv): it originates over GATT only (startLegacyFallbackLocked, UUID beacon, frames over GATT). Its own frame hash goes into dedup + ownHashes (rebuildAndAdvertise does this regardless). Echo path: peer relays via advertiseRelayOnce (ext adv) AND gattPlane.relayOnce. The legacy phone receives GATT relays → ingestFrame → claimOwnEcho works. OK, echo works over GATT.

But: **legacy phone's presence** — it advertises UUID-only connectable beacon, no frame. Peers see it via legacy scan → GATT connect. It does NOT broadcast its mark over the air. So other phones' sketches don't include its mark via RF; it includes others' marks in ITS sketch (it hears their ext adv frames). Asymmetry: legacy phone can prove co-presence (its sketch has their marks), but their sketches lack the legacy phone's mark... For PoCP verify of legacy phone's originated frame: receiver's local sketch = marks it heard (not including legacy phone). Legacy phone's divSketch = marks IT heard (the receivers' marks + others). Jaccard overlap = the receivers' own marks. Receiver's sketch includes self mark + others. Overlap non-empty → passes τ if enough overlap. Self-inclusion on both sides helps. Probably OK in small cells.

**C5. GATT plane: notifySubscribers cap=4, MAX_PEERS=3 — legacy mesh diameter is tiny.** In a crowd with 5+ legacy phones near one ext-adv phone, some never connect. Documented B9 tradeoff (security cap). Communication limitation: GATT plane doesn't scale.

Also: relayOnce + currentFrame setter both notify+write. On a legacy phone, currentFrame changes every epoch → writeToPeers every epoch. Plus relayOnce for every relayed frame. With MTU < 229, chunking = 226/(mtu-5) ≈ 13 ATT writes per frame per peer, serialized one-in-flight. A relay burst (say 5 frames in an epoch) → 65 writes × ~30-50ms = 2-3s per peer. The RSSI poll skips reads while writeInFlight — so during relay bursts, the phone stops hearing that peer (poll read is the only receive path for small MTU). **Relay storms blind small-MTU GATT links** — the busier the mesh, the deafer legacy phones. That's a real communication finding.

**C6. The 6s private window takes the public frame off air (documented) — including relay capability?** During privateTransportActive, rebuildAndAdvertise skips startAdvertising (public frame not updated) — but relay drain continues: advertiseRelayOnce uses SEPARATE advertising sets (up to 2). Wait — "A phone usually supports one advertising set" (comment at 445). If the controller has ONE set total, then while the primary set carries the private frame, advertiseRelayOnce's startAdvertisingSet fails (no hardware set) → relay adv rejected → onAir=false → GATT only. So during each 6s private window, over-the-air relaying stops. Three windows per private message (initial + 2 re-airs) = 18s of degraded relaying per private send. In a busy mesh, private sends degrade everyone's relays. Also the reverse: if 2 relay sets are somehow active, the private send can't get the set → "private send skipped: another private window is active"? No — advertisePrivateOnce checks privateAdvActive flag only, then calls startAdvertising which... would fail at the hardware level if no set free, callback status != SUCCESS, currentAdvSet=null, and the restore runnable still fires 6s later restoring public frame. So a failed private send still blacks out public advertising for 6s and reports ON_AIR tick (line 464-469: started=true → markOutgoing ON_AIR) even if the hardware never aired it! Actually advertisePrivateOnce returns true immediately after startAdvertising() — which is async. The tick shows ON_AIR regardless of hardware success. Minor honesty gap.

**C7. Adv interval 1000ms + scan LOW_LATENCY: discovery latency.** New phone arrives: it starts advertising; peers hear within ~1-2s. Fine.

**C8. Epoch-boundary sketch starvation (already handled via prev-sketch) — but what about the FIRST epoch after service start?** localSketch(epoch) has self mark only (recordSelf). divSketch = 1 element. LOCAL frame with 1-element claim: receiver with N marks → Jaccard 1/N... self-inclusion means receiver's sketch includes receiver's self mark + sender's mark (if heard). Sender's claim = {sender's own mark}. Receiver's sketch includes sender's mark only if receiver heard sender's frame this epoch. Circular: to display sender's LOCAL, receiver must have heard... the same frame? No — any frame from the sender this epoch (the presence frame). And the sender's claim includes only sender's mark. Receiver's sketch: {self, sender, ...}. Intersection = {sender}. Jaccard = 1/(union size). For 2 phones: union = {A, B} → 1/2 = 0.5 ≥ 0.3 ✓. For 3 phones: 1/3 = 0.33 ≥ 0.3 ✓. 4 phones: 0.25 < 0.3 ✗! 

Wait, this is the same grind math as the attack but for HONEST frames: an honest sender whose sketch is sparse (just arrived, or its scan missed frames) claims few elements; against a larger cell, Jaccard falls below τ=0.3 → CellMismatch → LOCAL dropped. The richer-sketch selection (prev epoch) mitigates after the first epoch. But: **a sender that hears nobody (scan broken, or everyone else just arrived) produces a 1-element claim; receivers in a 4+ device cell CellMismatch it.** Honest LOCAL alerts dropped in exactly a growing crowd. The "sign the richer sketch" logic picks max(prev, cur) — after one epoch of hearing, cur has everyone's marks. OK so transient (first epoch or after scan gaps). Medium communication edge case.

**C9. Clock skew between two phones > 2 epochs (20s+) → total partition.** Documented with banner. But: clockSkewWarning clears as soon as ANY frame passes K4 (line 1050). In a mixed crowd (one skewed phone among normal ones), the banner flaps off while the skewed phone stays partitioned. Minor.

**C10. Ratchet catch-up after long offline: fastForwardChains advances to epoch-1 each epoch — if span > 8192 (22.8h), pairRatchet returns null → "chain fast-forward failed" logged with label (security F4) — and continues every epoch forever. Never recovers!** If app is off for >22.8h: on restart, chainEpoch is 8640+ behind. pairRatchet refuses span >8192. fastForwardChains logs failure and `continue`s — chainEpoch stays stuck. Next epoch, target = epoch-1, span still >8192 → fails again. **Permanently bricked private messaging for that contact until re-pair.** The comment says fastForwardChains exists precisely to prevent this ("so a long idle period can never exceed the ratchet span cap") — but it only works if the app runs at least once every 22.8h. App killed for a weekend = dead contacts, silently. This is a real communication bug: the fix should catch up in ≤8192-step chunks (ratchet in a loop) or jump anchor. High severity for usability: two protestors pair Friday, meet Sunday, private chat dead, no error surfaced to user (only exportable debug log line). Actually wait — keyForSend would also fail (span >8192 → null → "contact unknown or key ratchet failed" logged). User sees... the send just drops. UI shows SENDING forever? The private row was appended optimistically? Let me check: the UI appends the outgoing private row when? `MeshState.privateRowTs` — set in UI on send presumably. The send fails → logDebug only, no UI feedback, row stays at SENDING/one tick forever. Silent message loss — the worst communication failure mode for this app's purpose.

Chunked ratchet would fix: while span > 8000: ratchet +8000, repeat. That's ~8640 BLAKE3 ops per extra day — trivial.

**C11. nextPrivateCounter nonce reuse across process restart within same epoch**: storedEpoch == epoch, base persisted, seq persisted — commit() before returning. Crash after commit → restart reads seq → monotonic. OK. But memory-only fallback (prefs fail): random base per CALL — two calls in same epoch → two random 31-bit bases; collision probability negligible (~2^-31 per pair). OK as documented.

BUT: the counter is global across contacts, while nonce uniqueness is per pair key — fine. And nonce includes pk (ephemeral, per epoch, derived from seed+beaconSeed). seed is per service start. So even same counter+epoch under a restart → different pk → different nonce. Safe. OK.

**C12. Relay queue drain: when a task drains, jitter 0-350ms then advertiseRelayOnce 2000ms. Loop iterates immediately; if another task queued and capacity<2... drains second. Two 2s bursts concurrent. Then capacity full → next poll 250ms. Fine.

But relayRepeatsForDensity uses neighborsThisEpoch from stats — computed from measurement. At service start (no neighbors heard yet), neighbors=0 → repeats=3 for every relay. In a crowd where we haven't heard anyone yet but they hear us (asymmetric scan), we triple-air everything → airtime cost. Minor.

**C13. Echo receipt when BOTH phones send simultaneously**: A sends "X", B sends "Y" same epoch. A relays B's frame; B relays A's frame. A hears echo of X. But ALSO: A's relay burst of B's frame and B's relay burst of A's frame — jittered. Both phones' scanners catch the other's burst. Fine.

**C14. The BROADCAST stop-on-echo race**: A hears echo at E+0.3 → sets outgoingSetAtEpoch back so message expires at E+1 rollover. But the epoch-loop's sparse/expire logic for BROADCAST at E+1: outgoingText cleared → next frame is empty presence. The echo also triggered markOutgoing(ECHOED). Fine.

**C15. appendOrMergeIncoming withinEpochs=4 keyed on (text, tier) — NOT sender.** Two DIFFERENT people send "POLICE" within 4 epochs: merged into one row with repeats++. Is that desired? For corroboration UX maybe. But the first row's mark stays. B6 comment says suppression keyed by (text, mark prefix) allowed attacker pre-broadcast to suppress real alert; merge fixes by showing. Merging different senders' identical texts into one row means the second sender's distinct alert never gets its own row — arguably fine (same text = same alert, corroboration). OK.

**C16. Trust recordVerification only when displayOk && direct. A relayed corroboration never counted. Fine (A2).

**C17. GATT server FRAME_TX read returns currentFrame — including during private window (currentFrame=private frame). A legacy peer reading during the window gets our private frame — intended.

**C18. Chunk reassembly: handleChunk uses stride derived from centralMtu — the SENDER computes payload = (state.mtu - 5); the RECEIVER computes stride = (centralMtu[addr] - 5). Both are the negotiated MTU of the SAME connection? The client's connection MTU (state.mtu) is negotiated client-side; the server's centralMtu for that central is from onMtuChanged server-side. Same underlying ATT MTU — symmetric. OK. But if a second frame's chunks arrive interleaved (sender sends frame1 chunks then frame2 chunks — sequential per writeLock, OK; but two different chunk sequences from same addr can't interleave due to one-in-flight). Seq collision: seq numbering resets per frame; receiver's chunkBuffers keyed by addr only, single buffer. If a chunk from frame2 arrives while frame1 partially assembled (>10s TTL? no, sequential writes)... chunk loss → frame1 buffer stale → frame2 chunks mix with frame1 residue? If one chunk of frame1 is lost (ATT write failed → pendingWrites CLEARED on failure, so rest of frame1 never sent), receiver keeps partial frame1 buffer; frame2's chunks then fill into frame1's buffer at offsets — first chunk seq=0 offset=0 overwrites start... but the assembledEnd check: completes at 226 only when offsets cover. Mixed bytes → garbage frame → sig fail → dropped. Bounded. OK-ish.

**C19. MIN_PEER_AGE_MS=8000 dwell: a legacy phone walking by is visible for <8s → never connected. Drive-by legacy peers never exchange. By design (B9) but means legacy plane needs ~8s+ stationary proximity. Plus CONNECT (up to 10s) + MTU + discovery + notify enable... first frame exchange ~15-20s after first sighting. For moving crowds, legacy phones may never sync. Communication limitation worth noting.

**C20. Scan filter setServiceUuid(PARCEL_UUID) with extended advertising: the service UUID AD field is in the primary/secondary PDU. OK.

**C21. One more on the receipt: what if the RELAYING peer is a third phone C, and A/B are out of range of each other? A sends LOCAL (TTL=1). C relays (TTL=0). B hears C's relay, displays (if PoCP passes — B's sketch vs A's claim... A and B never heard each other! A's claim = marks near A. B's sketch = marks near B. If A and B share neighbor C: C's mark in both → overlap. Jaccard might pass in a chain. Fine — that's the design.)

But the echo receipt for A: A hears C's relay of A's frame (A-C in range). Receipt fires. But B never displayed (PoCP mismatch). Receipt says "a peer relayed it (not proof of delivery)" — honest. OK.

**C22. LOCAL sparse re-air after echo: every 4th epoch for 30 min. The echo heard might be from an adversary relaying once then blackholing — documented.

**C23. What about messageRepeatEpochs for BROADCAST when NO echo heard: re-originate 3 epochs then expire with honest "no relay echo" message. In a 2-phone scenario where B heard and displayed every copy but B's relays never made it back (B's relay burst collided, or B is a legacy phone whose GATT relay writes to A but A's GATT server... wait — B legacy, A ext: B relays via gattPlane.relayOnce → notify/write to A if connected. A gets echo over GATT → receipt. If not connected (B seen <8s), no echo. Message delivered (A heard B? no wait, A is sender)... 

Let me redo: A (ext adv) sends BROADCAST. B (legacy) receives via... B hears A's ext adv? B's phone lacks ext ADVERTISING, but can it SCAN extended? isLeExtendedAdvertisingSupported is about advertising; scanning ext PDUs needs BLE 5 controller — usually coupled. Assume B can't hear ext adv either → B relies on GATT: A must connect to B (A sees B's legacy UUID beacon → after 8s dwell → connect). Then A's currentFrame writeToPeers → B ingests → displays. B relays: gattPlane.relayOnce writes back to A. A hears echo. Works but ~15-20s latency. OK within 3-epoch? 20s = 2 epochs — borderline. First delivery might miss K4? B receives frame stamped E at E+1 or E+2 — within ±2. OK.

**C24. Private frame relay across GATT: relayOnce writes private frame to peers. Peer relays again (multi-hop over GATT mesh). Each GATT link re-writes. TTL decrements per hop. But GATT mesh is not a flood — each phone has ≤3 peers + ≤4 subscribers. Sparse. OK.

**C25. Duplicate display via concurrent ingest (BLE + GATT same frame): both pass checkEpoch (TOCTOU, security F13) → both display → appendOrMergeIncoming collapses (same text+tier within 4 epochs) → one row, repeats=2. Fine.

**C26. ownHashesAcked: echo acked once per origination. If A re-originates same text at E+1 (new hash), new entry. Fine.

**C27. rebuildAndAdvertise on tier collect: only when outgoingText empty. OK.

**C28. Adv watchdog: rebuildAndAdvertise(epoch, cfg, MeshState.outgoingText.value) — note: passes rawText without sparse logic — a LOCAL in sparse mode re-aired by watchdog → minor extra airing. Harmless.

**C29. `direct` flag for msgType 2/3 requires ttl == originTtl(8). A relayed regional frame at TTL 7 → not direct. Corroboration only counts direct. Fine. BUT: an originator whose frame was capped... relay_decision caps incoming TTL at 8 before decrement — a malicious originator setting ttl=200: receivers see ttl=200 ≠ 8 → not direct (good).

Hmm wait — what sets the origin TTL? defaultTtlRegional() from Rust = 8 presumably. rebuildAndAdvertise uses defaultTtlRegional().toUByte(). OK.

**C30. Beacon advance gating: beacon.advance(ent, nowMs, beaconFloorMs) — floor 10s = epochMs. If floor not met (epoch shorter?), advance returns false → beacon seed unchanged → marks DON'T rotate this epoch? beaconFloorMs default = 10000 = epochMs. The epoch loop calls advance once per epoch (~10s apart) — borderline: if the epoch loop fires 1ms early (epoch boundary computed from wall clock vs last advance timestamp), advance might reject → marks stay the same for an epoch → presence counts double? Measurement dedups by mark per epoch bucket; same mark two epochs running → neighborsDirect counts it once per bucket — fine. But unlinkability slightly degraded. And PoCP: peer sketches include the stale mark — consistent. Minor.

Actually wait, more subtle: `beacon.advance(ent, nowMs, cfg.beaconFloorMs)` — if it returns false (floor not elapsed), the beacon seed does NOT advance but the wall-clock epoch did. Then the frame built this epoch uses the OLD beaconSeed with the NEW epoch → mark = H(seed, beaconSeed, epoch)? Mark derivation includes epoch? If mark = f(seed, beacon_seed) only (crypto.rs: ephemeral from seed‖beacon_seed, epoch not in mark?), then same mark two epochs → dedup of presence per-epoch buckets: same mark appears in two epoch buckets — neighborsDirect takes max of buckets, not sum — OK still fine. Frame hash differs (epoch in body region? epoch is at bytes 34-38 in SIG_REGION → different hash). OK.

**C31. Now the BIG one I should verify — does the relayed echo actually get back in the 2-phone case considering B's own epoch-rollover restart blackout?** B tears down its advertising set at each rollover (stop+start). During teardown (~50-200ms), B is off air — but B's relay bursts use separate sets. A hears B's relay bursts regardless. The lockstep issue was: both relay at same instant → RF collision → both lost. Jitter spreads 0-350ms. Relay burst = 2s at INTERVAL_LOW (~100ms interval → ~20 packets). Two bursts overlapping partially still likely deliver some packets. So receipt reliability is now decent. The user's fix looks right.

Remaining latency in receipt: B hears A's frame at some point in epoch E (A airs ~10× spread over the epoch; B catches copy #k). B enqueues relay. Drain ≤250ms + jitter ≤350ms + burst start. A hears within ~1s of B's relay. So echo typically 1-4s after A's frame first heard by B. But if B first hears A's frame LATE in epoch E (say at E+9.5s), B relays at E+9.8s, burst extends into E+1. A hears at E+1. Fine — receipt ~instant. The "late receipt" scenario: B misses ALL of A's airings in epoch E (collision with B's own rollover restart — every one of A's ~10 airings? unlikely) — more likely: B's relay burst in E collides with A's rollover restart blackout window (A's scanner is fine though! A's advertising teardown doesn't affect A's scanning). Hmm — wait, actually what collides is ON THE AIR: B's relay burst packets vs A's re-origination packets transmitted at overlapping instants → A's receiver can't decode B's packets while A's own transmitter... BLE is TDD-ish; A's radio can't receive while transmitting. A transmits ~1% duty (100ms interval, ~1ms packets?) — negligible. The real collision is at B: B can't hear A while B transmits. Also negligible duty.

So why was the receipt late pre-jitter? Because both phones' relay/relay-origination bursts fired simultaneously and... on Coded PHY S=8, a 226-byte frame takes ~15-20ms on air! That's significant: 20ms packet at 100ms interval = 20% duty cycle. Two phones firing bursts simultaneously → 20ms packets colliding repeatedly if intervals align (both INTERVAL_LOW=100ms, same phase) → EVERY packet collides for the whole 2s burst. That's the lockstep mechanism — plausible. Jitter (0-350ms) shifts phase. TX jitter 1200ms shifts the 1000ms-interval origination phase too. The user's analysis holds. Good.

But note: **TX_JITTER up to 1200ms at epoch rollover delays origination; advIntervalMs=1000 → ~9 airings left in the epoch.** Fine.

And one more receipt subtlety: **A's echo recognition requires A's scanner to catch B's relay — but A tears down its own primary set at rollover; that doesn't stop scanning. Scanning continuous. OK.

**C32. GATT writeToPeers on EVERY relayOnce + every currentFrame change: a busy epoch with 10 relays → 10 writes × peers. With WRITE_TYPE_DEFAULT (with response), each ~2 connection events. Fine.

**C33. What about relay of relayed frame creating a loop back to originator... dedup handles.

**C34. The K4 gate uses the frame's epoch vs own epoch. A phone whose clock is 1s fast crosses the boundary first. It originates E+1 while peers are at E. Peers accept (diff +1). PoCP: witness seed = E+1 or E (richer sketch = E usually at rollover start) — verify with candidates E+1, E, E+2 → OK. Its frameEp+1 candidate... fine.

**C35. dedup.checkAndInsertEpoch for own frame uses ownEpoch — the same epoch as stamped. Fine.

**C36. VDL solve on RECEIVE path: vdlCheckFrame on every msgType-3 FRESH frame — 1 hash, cheap. relay_decision ALSO verifies VDL for private (again) — 2 hashes. Fine.

**C37. Private trial-decrypt loop calls PairStore.contacts(this) on the BLE binder thread → cached (B5). candidateKeys does pairRatchet (fast-forward) on binder thread — CPU only, span ≤ ~3. OK. noteOpened on IO. OK.

**C38. EncryptedSharedPreferences commit() on binder thread? nextPrivateCounter is called from the private-send coroutine (lifecycleScope), not binder. OK.

**C39. First-frame-after-rollover sketch: verifyPocpAcrossRollover uses sketch buckets frameEp-1/E/E+1. The +1 bucket: measurement.localSketch(epoch+1) — future bucket, usually empty at receive time unless a faster peer already originated E+1 frames we heard. Handled (empty → skipped; judged flag). POCP_NO_LOCAL_SKETCH → defer (not inserted into dedup) → re-air retries. But: **deferred frames rely on the SENDER re-airing.** Sender re-airs for messageRepeatEpochs=3 epochs. If receiver heard nothing for 3 epochs (just started, empty measurement), then accumulates marks... first heard frame → sketch available → next re-air of the pending alert displays. OK self-heals.

**C40. Startup deadlock of mutual witnesslessness: A and B both just started, empty sketches. A sends LOCAL text immediately at E0. A's sketch at E0 = {A's self mark} (recordSelf). Claim = 1 element. B at E0: sketch = {B self, A?} — B heard A's frame at E0 → B's E0 bucket has A's mark. So B's sketch = {B, A} → Jaccard({A},{A,B}) = 1/2 = 0.5 ✓ display. Good — self-inclusion fixed the cold start.

**Now the direct answer to "Why the receipt is late but the message isn't":**
The user's analysis is correct and the code reflects the fix. Let me restate it crisply with code references:
1. Message delivery is redundant: ~10 airings/epoch × 3 epochs (advIntervalMs=1000, messageRepeatEpochs=3) → many chances.
2. The receipt depends on the peer's relay of a specific hash, which happens exactly ONCE per hash per peer (dedup: later copies that epoch are DUPLICATE at checkEpoch → no re-relay). Pre-0.14, that was a single ~2s burst; post-0.14, relayRepeatsForDensity gives 3 bursts when ≤1 neighbor.
3. Wall-clock-derived epochs (epoch = now/epochMs) synchronize all phones to the same boundary → simultaneous advertising teardown + re-origination + relay bursts → collisions in the same congested slice. On Coded PHY, a 226B frame is ~15-20ms on air; synchronized bursts at the same 100ms phase wipe each other out for the full burst.
4. Pre-jitter: one lost burst = wait a full epoch for the next hash's relay. Three epochs → three coin flips → "sometimes late, sometimes never". Delivery (many chances) vs echo (few chances) asymmetry = the observed symptom.
5. Also note the echo was never proof of delivery — and its absence was never proof of non-delivery (code comment line 600-613 handles the wording).

Also I can add a residual observation: even with jitter, the receipt is inherently single-path: exactly one relay per peer per hash; with ONE peer, if that peer's relay bursts all land in A's scan gaps (e.g., A is mid-advertising-restart ~200ms × 3... no, A's scanning isn't interrupted)... honestly receipt should now be reliable. The remaining late-receipt case: relay queue contention — B's queue has other tasks (prio eviction: LOCAL echo prio 0 > regional 1 > private 2; FIFO within class). A BROADCAST relay is prio 1; if B's queue is busy with private relays (prio 2, evicted first) and other regionals... up to 64 queued × (250ms poll...) — drain rate: 2 concurrent 2s bursts → ~1 task/s sustained. Queue of 10 regionals → ~10s delay before our echo's burst. In a busy mesh, echo latency ~ queue depth seconds. With cap 64 and eviction, worst ~30s+. So in a crowd, receipts are late because the relay QUEUE is deep — different mechanism than lockstep. Worth mentioning: the receipt's timeliness degrades with mesh density, exactly when... hmm, but in a crowd many peers relay → first echo comes fast from whoever drains first. OK.

One more important asymmetry: **relayRepeatsForDensity uses the RELAYER's neighbor count. B has 1 neighbor (A) → 3 repeats. Good for 2-phone. But consider A and B plus C far away... fine.

Now let me also reconsider **echo for LOCAL**: LOCAL TTL=1; B relays with TTL clobbered to 0. A hears echo. A switches to sparse (every 4th epoch). B keeps displaying? B displays once (merge). Fine.

And **echo forgery** — documented.

Let me also check: **the "relayOnce" GATT echo for private: private frames re-aired 3× same hash; ownHashes never registers → no echo. Already noted (C3).

**C41. One more real comm bug candidate: `advertisePrivateOnce` restore uses `restoreFrame` captured at SEND time. The send happens possibly mid-epoch E; window 6s spans rollover into E+1; at E+1 rollover the epoch loop's jittered rebuildAndAdvertise sees privateTransportActive=true → skips startAdvertising (updates currentPublicFrame only). Window ends at E+1+~4s → restore runnable starts advertising the STALE E-frame (restoreFrame = currentPublicFrame captured at send = E frame). That stale frame airs until the NEXT epoch rollover (up to ~10s later), because the E+2 rollover rebuild will call startAdvertising fresh. Wait: rebuild at E+2: privateTransportActive=false by then → startAdvertising(currentPublicFrame = E+1 frame built at E+1 rollover? No — rebuildAndAdvertise at E+1 BUILT the E+1 frame and assigned currentPublicFrame (line 1399) but skipped airing. So at restore time (E+1+4s), the restore runnable airs `restoreFrame` (the E frame, stale by 1 epoch) — NOT currentPublicFrame (E+1 frame). K4 still accepts E frame at E+1/E+2 (±2). Presence mark: stale mark from epoch E airing at E+2 — peers record presence for frameEp=E bucket... fine-ish. But the E+1 frame never airs until E+2 rollover. Net effect: ~1 epoch of stale advertising per private send. Minor but real: the restore should air `currentPublicFrame` (fresh), not the captured `restoreFrame`. Actually the onRestored callback sets gattPlane.currentFrame = currentPublicFrame ?: restoreFrame — so GATT gets the FRESH frame but the air gets the STALE one. Inconsistent. Low-severity comm bug.

Wait, re-check: advertisePrivateOnce(frame, durationMs, restoreFrame = currentPublicFrame at send time...). In the send path: `val restoreFrame = currentPublicFrame` (line 448) — captured BEFORE the window. During the window, epoch rollover builds E+1 frame → currentPublicFrame = E+1. Restore at window end airs restoreFrame (E frame). Yes — stale air for up to one epoch. In reairPendingPrivate (line 1301): restoreFrame = currentPublicFrame (fresh at that rollover). Window 6s spans into next epoch E+1; rollover E+1 skips airing; restore airs the E frame again (now 1 epoch stale). Same issue. Low.

**C42. `rememberOwnFrame` is only called in rebuildAndAdvertise for public frames; the echo of a LOCAL sparse presence frame (empty text, carriedText=false) → logged but no receipt. Intended.

**C43. Skew banner: "FRAMES ARRIVING N s AHEAD/BEHIND" computed as diff*epochMs/1000 — diff in whole epochs; a 1s clock offset never triggers (diff 0). But a phone 25s off shows 20s or 30s depending on boundary crossing — imprecise but fine.

**C44. Two phones with different epochMs configs (user-configurable 5-120s!) → total partition with K4 drops; config comment notes it. Config is user-editable — a footgun: user sets 20s on one phone, 10s on another → epochs align every other tick; diff alternates 0/1 → sometimes passes! Half the frames dropped, flapping. Their peers at 5s → mostly dropped. Documented in C1 comment. Worth listing as comm hazard: epochMs must match fleet-wide but there's no handshake/version exchange to detect mismatch — only the skew banner. The banner only fires when diff>2. With 10s vs 20s, diff ∈ {0,1} → no banner, just half the frames missing silently. Nasty.

**C45. Beacon floor: beaconFloorMs default 10s must be < epochMs for advance to fire each epoch. If user sets epochMs=5s and beaconFloorMs stays 10s → beacon advances every OTHER epoch → marks persist 2 epochs → presence double-count? No (per-epoch buckets + max). Unlinkability halved. And `minHearers` config... fine.

**C46. Relay priority: LOCAL echo prio 0 — a LOCAL alert's relay outranks regional. In queue full: evict worst (highest prio number) — private first. OK.

**C47. The B8 relay queue drains only when `radio.relayCapacityAvailable()` (activeRelaySets<2) — on a legacy-only phone, advertiseRelayOnce returns false immediately (no ext adv) and the loop `if (!onAir) delay(250L)` — but gattPlane.relayOnce still ran. Queue drains at 4/s max on legacy. Fine.

**C48. GATT server: `currentFrame` starts as 226 zero bytes (line 123). A peer connecting before our first origination reads 226 zeros → onFrame → frameDecodes(zeros) → BadVersion → undecodable. Harmless.

But wait — notifying 226 ZERO bytes when? currentFrame setter only called with real frames. OK.

**C49. Prepared write path (PROPERTY_WRITE): client code uses WRITE_TYPE_DEFAULT always, which for 226B with MTU≥229 is a single write request. Prepared writes only if the peer's stack chooses them. Our client never does prepared writes. OK.

**C50. onCharacteristicReadRequest offset handling: `if (offset < frame.size) frame.copyOfRange(offset, frame.size) else ByteArray(0)` — fine for blob reads.

OK. I also want to double-check the claim in the security audit about `frameVerifySelf` not deduping bad-sig frames (Android F5) — confirmed at MeshService.kt:923-930: returns before dedup insert. Yes — a GATT flooder re-sending the same bad-sig frame costs a full Ed25519 verify each time (~1-2ms on phone). At hundreds/sec sustained → one core pegged. Real but bounded by GATT write throughput (~100s/s per connection, ×3 peers + ×4 subscribers). Medium DoS.

Also note: the K4 gate happens AFTER sig verify (line 923 vs 938-941)? Order: frameDecodes → frameVerifySelf → hash/epoch → K4 → dedup. So a replayed stale frame costs a sig verify each time too — but if sig VALID and epoch stale, it's dropped at K4 BEFORE dedup insert → re-sending a stale valid frame also costs full verify per copy, no dedup protection. Same class as F5 but for stale frames. An attacker replaying recorded frames from 1h ago: each copy = 1 Ed25519 verify on every receiver in range, forever, never deduped. Both BLE adv path (attacker advertises) and GATT. Bounded by airtime but persistent. Finding: frames dropped at K4 (and bad-sig) never enter dedup → verify-flood amplifier. Fix: insert hash before K4 return (or a small separate "recently rejected" LRU).

Hmm, but careful: the reason they don't insert at K4 is the comment "before dedup, so a stale/future frame never occupies a dedup slot" — deliberate to avoid bucket pollution. A small bounded reject-cache (e.g., 256 entries) would do.

Now let me also verify one security agent claim about div_sketch copy: bytes 18..34 in the frame carry div_sketch in cleartext, and witness key = BLAKE3(divSketch‖epoch). A copier needs a valid div_sketch of the target cell — copies from any overheard frame of that cell (up to 8 hops away via relay). Then their frame's witness verifies against the cell's sketches with Jaccard ≈ 1 (copied claim == cell's actual marks... well, the copied sketch is one epoch stale at rollover — candidates cover ±1). Yes, F1 verified. The R1 soft-detection logs reuse across distinct marks same epoch. Attacker copies and re-uses in the SAME epoch → detected-log-only. Next epoch (marks rotate, sketch changes) → attacker needs the new sketch — copies the cell's NEW frame again. Works while the cell keeps originating. So a remote attacker within relay range can continuously forge co-presence. Confirmed.

Also small note: witness seed is the epoch — but the witness MAC covers buf[0..102] which includes the epoch (34..38)? The prefix is bytes 0..102 (WITNESS_PREFIX_END=102) — includes mark, version, type, div_sketch, epoch, body. And the key includes seed=epoch. OK.

Now, prioritization for the final report. The user asked:
1. Security vulnerabilities ✓ (3 agent reports — I should synthesize the top ones, dedupe F1/H2 overlap between agents: protocol agent F1/F2 ≈ crypto agent H2 — merge).
2. Communication problems with mental simulation ✓ (my analysis above).
3. "Why the receipt is late but the message isn't" — confirm their analysis + add the queue-depth point.

I should present a plan (plan mode). The deliverable: a findings report + remediation plan. Should I ask clarifying questions? Maybe ask which findings to fix first / whether they want fixes implemented next. Since plan mode, I'll present the consolidated findings and a prioritized remediation plan, then ask what to implement.

Keep it structured but not bloated. Use tables where helpful. Include file:line references. The user is clearly the developer (deep context), so technical density is appropriate.

Structure:
1. Answer the receipt question (verify their analysis + one addition).
2. Communication problems found (scenario-simulated), ranked.
3. Security vulnerabilities (synthesized from audits), ranked.
4. Remediation plan (proposed fixes, minimal diffs).
5. Ask what to implement.

Length: this is a lot of material. I should be comprehensive but concise. Let me write it.

For the receipt question, also mention: with the jitter fix + relay redundancy (0.14), the remaining late-receipt causes are (a) relay-queue depth in crowds, (b) single-relayer dependence when neighbors≤1... actually relayRepeatsForDensity handles that, (c) legacy GATT path latency (8s dwell + connect ≈ 13-20s before first echo).

Also worth explicitly validating: "no echo ≠ not delivered" — the code already messages this (line 600-613).

Let me also double check relayRepeatsForDensity interplay with echo: B relays A's frame with 3 repeats (2.6s apart) when B sees ≤1 neighbor. A hears first burst → ack. Good.

And one more subtle one for the receipt: **A's own dedup insert at origination means if B's relay echo arrives while A's own frame is STILL being... no, claimOwnEcho before dedup. Fine.

And: **what if B relays A's frame back to A, and A's claimOwnEcho acks it — but the echo A heard was actually A's OWN advertising... no, A doesn't hear its own advertisements (BLE can't receive own transmissions). OK.

Also the echo can come from GATT: relayOnce notifies/writes — A's GATT server RX write → ingestFrame → claimOwnEcho. Fine.

One more potential comm issue: **B inserts A's frame hash into dedup AFTER acting. B's relay of A's frame echoes back to A. A hears it: claimOwnEcho (acked) → falls through to... wait, after claimOwnEcho returns non-null (first time), code CONTINUES (no return!). Let me re-read lines 976-1001: `val ownEcho = claimOwnEcho(hash); if (ownEcho != null) { ...receipt... }` — then falls through to presence, dedup check → DUPLICATE (we inserted at origination) → return. Good.

Second echo of same hash (from another peer or B's repeat): claimOwnEcho → ownHashesAcked.add returns false → null → dedup → DUPLICATE → return. Good.

OK now — one more scan of relay drain: `relayFrame(bytes)?.let { enqueueRelay(it, ...) }` — relayFrame is the Rust relay_decision. For regional: decrements TTL. For LOCAL: clobbers 0. The relayed bytes go on air. A hears the echo with TTL=0 (LOCAL) or 7 (regional). Hash same. Good.

Alright — also confirm H1 nonce reuse claim about `counter as u32` truncation: PairStore.nextPrivateCounter returns (base+seq) & 0xFFFFFFFF — always < 2^32, so truncation never bites in practice from the Kotlin shim. The u64 API truncation is theoretical for this shim. But the counter-reset hazard: base is persisted per epoch with commit BEFORE return... order: p.edit().putString(...).commit() then return counter. If the app crashes after commit but before sealing, seq was incremented but frame never sent — gap, no reuse. If crash before commit (in-memory edit?) — commit is synchronous; counter returned after commit completes. So reuse needs: process death AFTER commit, restart, same epoch, read persisted (epoch, base, seq) → next counter = base+seq (incremented) → no reuse. Actually the persisted state survives correctly → monotonic within epoch. Reuse only if the encrypted prefs file is restored from backup — allowBackup=false, so no. Or if two app instances... no. So H1's catastrophic path is largely closed by the shim; the remaining hazard is the memory-only fallback: random base per call (collision ~2^-31 birthday per pair within same epoch... with k messages, k²/2^32; k=100 → ~2^-19). Negligible. So I'll downgrade H1 in synthesis but keep the "use div_sketch[8..16] random nonce" recommendation as hardening.

Also note the private counter is GLOBAL across contacts (base+seq shared) — the nonce includes pair key? No — nonce = epoch ‖ BLAKE3("nonce"‖pk‖counter)[..8]; key = pair_key. Nonce uniqueness required per KEY. Same counter under different pair keys = different keys → fine. Same counter+same contact same epoch: monotonic seq prevents. OK.

Synthesis of security findings (deduped, ranked):

CRITICAL: none found (audits agree).

HIGH:
1. **No pairing authentication (SAS/fingerprint)** — QR MITM + silent key replacement on re-pair under same label. (Android F1)
2. **PoCP co-presence remotely forgeable**: witness MAC key from public values; div_sketch travels cleartext up to 8 hops → copy attack needs no proximity (protocol F1; understated in pocp.rs doc). Plus small-cell grind (≤3 marks) — 2-3 forgeries per 256-frame sweep (protocol F2 / crypto H2). Both documented; badge degradation is the only mitigation.
3. **Debuggable distributed builds** — no release buildType; adb can dump process memory → sk, salts, pair keys, plaintext (Android F3). Combined with:
4. **v2 salt NOT deleted after pairing** — lives per-process, shared across contacts, copyable in UI; a memory dump + recorded traffic = full private history decryption. Breaks the headline forward-secrecy claim until process death (Android F2).

MEDIUM:
5. **No replay/freshness in core** — verbatim private-frame replay re-decrypts after ~3-epoch dedup window (core M1/F6); shim contains on Android (K4+dedup+ratchet).
6. **Wire epoch unchecked in core** → dedup wipe via far-future epoch, future-bucket pre-fill blackout (F3/F5/core M2); shim K4 gates on Android.
7. **VDL PoW 22 bits GPU-trivial + precomputable across epochs** (M3/M4).
8. **GATT bad-sig/stale frames never deduped** → verify-flood CPU/battery DoS (Android F5 + my K4 observation).
9. **GATT slot exhaustion**: subscribed centrals never evicted; connected peers pinned (Android F6).
10. **v1 downgrade**: bare hex / v1 codes accepted → silent non-FS pairing (Android F7, core L1).
11. **Ratchet brick after >8192 epochs idle (22.8h)** — comm AND security-ish; permanent until re-pair (my C10). Fast-forward only works if app runs daily. [This is communication really]
12. **Sybil corroboration** up to 32 claims from one nearby attacker (F12).
13. **Panic wipe gaps**: queued privateSends plaintext, pendingPrivate, ownHashes, dedup/trust FFI instances survive (Android F8).
14. **Label leaks into exportable log** on failure paths (Android F4).

LOW: zeroization gaps (M5), empty AAD (L2), min_hearers=0 (F8-core), tau unvalidated (F11), observe() zip truncation (F10), FFI footguns (L4, F14, F7), anchorEpoch clamp (F12-android), private-send RF signature (F9-android), notification metadata (F10-android), clipboard (F11-android), make_test_frame in prod FFI, head-of-line dedup decay (F4-core).

Communication findings (ranked):
1. **Ratchet brick >22.8h idle** (C10) — silent, permanent until re-pair, no UI error. Fix: chunked ratchet.
2. **No receipt for private messages** (C3) — ownHashes never registers private frames; weakest feedback on the tier that needs it most.
3. **K4 freshness wall caps mesh at ~3 epochs (30s) regardless of TTL=8** (C2) — no store-and-forward; slow relay chains die mid-path and trigger misleading "check your clocks" banner. Sparse/bridge scenarios silently fail.
4. **Small-MTU GATT links go deaf during relay bursts** (C5) — writeInFlight skips poll reads; busier mesh = deafer legacy phones.
5. **epochMs mismatch → silent partial partition** (C44) — no handshake, no banner for diff≤2.
6. **Legacy peer acquisition latency ~13-20s** (C19) — drive-by legacy phones never exchange.
7. **Private windows black out OTA relaying on single-set controllers** (C6) — 18s per private send; and ON_AIR tick shown even if hardware never aired.
8. **Stale frame aired after private window** (C41) — restore uses captured frame, not currentPublicFrame; ~1 epoch stale per send; GATT gets fresh, air gets stale (inconsistent).
9. **Honest 1-element claims CellMismatch in 4+ device cells** (C8) — first epoch after start/scan-gap; LOCAL dropped in exactly the growing-crowd case. (Jaccard 1/N < 0.3 for N≥4.)
10. **Echo latency in crowds ~ relay-queue depth** (my addition to the receipt answer).
11. **clockSkewWarning clears on ANY fresh frame** (C9) — flaps off in mixed crowds while skewed phone stays partitioned.
12. beaconFloorMs ≥ epochMs config footgun (C45) — marks don't rotate every epoch.

Answer to "why receipt late but message isn't": confirm their analysis, point to code lines, add: (a) the residual single-relay-per-hash-per-peer structure means receipt latency = E[time for first peer's relay burst to reach you], message latency = E[time for first direct airing to reach peer] — inherently different distributions with different redundancy; (b) in crowds add relay-queue depth; (c) legacy path adds 13-20s. Also confirm: absence of echo never meant non-delivery; code now says so (line 600-613).

Remediation plan: propose minimal fixes in priority order, ask which to implement. Since plan mode, end with questions about scope/priority.

Format: headers, tables for findings, concise. This will be long but the task is a full audit; user expects depth. I'll keep each finding tight.

I now have a complete picture — three audit agents reported on security, and I've traced the communication paths end-to-end (MeshService, BleRadio, GattPlane, PairStore, Measurement, statemachine). Here's the consolidated assessment.

---

# Part 1 — Your receipt analysis: verified correct, with one addition

Your lockstep diagnosis matches the code exactly:

- **Delivery is massively redundant**: `advIntervalMs=1000` → ~10 airings/epoch, re-originated for `messageRepeatEpochs=3` epochs (fresh hash each time, so each epoch gets its own relay chance) — ~30 independent reception opportunities.
- **The echo is structurally fragile**: a peer relays a given hash **exactly once** — every later copy that epoch is `DUPLICATE` at `checkEpoch` (`MeshService.kt:1026-1031`), never re-relayed. Pre-0.14 that was one ~2s burst per epoch; the whole receipt hung on it.
- **The lockstep mechanism is real**: all phones compute `epoch = now/epochMs` (`MeshService.kt:551`), so all tear down + re-originate + relay in the same slice. On Coded PHY S=8 a 226-byte frame is ~15–20ms on air — two bursts at the same ~100ms phase annihilate each other for the full 2s. `TX_JITTER_MAX_MS`/`RELAY_JITTER_MAX_MS` (0.13) and density-scaled relay repeats (0.14) are the right fix and are correctly implemented.

**One addition — residual late-receipt causes that survive the jitter fix:**

1. **Relay-queue depth in crowds.** The drain loop sustains ~1 task/s (2 concurrent 2s bursts, 250ms poll). In a busy mesh, your BROADCAST echo sits behind other regional relays (prio 1, FIFO within class) — echo latency ≈ queue depth in seconds, worst case tens of seconds before eviction. The lockstep fix solved the 2-phone case; in a dense crowd the dominant delay is the queue, not collisions.
2. **Legacy GATT path**: 8s `MIN_PEER_AGE_MS` dwell + up to 10s connect + MTU/discovery → first echo 13–20s, vs ~2s on the adv plane.
3. And as the code now states (`MeshService.kt:600-613`): absence of echo was never evidence of non-delivery — B displayed the message every time while its relay burst died on the air.

---

# Part 2 — Communication problems (scenario-simulated)

### C1. HIGH — Private messaging silently bricks after >22.8h idle
`PairStore.fastForwardChains` (`PairStore.kt:425-445`) advances chains to `epoch-1` each epoch, but `pairRatchet` refuses spans >8192. App killed/off for >8192 epochs (22.8h at 10s) → every fast-forward fails, `chainEpoch` is stuck forever, `keyForSend` returns null → "private send dropped: key ratchet failed". Sends drop silently (row sits at one tick forever), receives fail identically, neither side can recover — **dead until in-person re-pair, with no UI signal**. Two users pair Friday, need it Sunday: dead. *Fix: ratchet in ≤8000-step chunks (`while (span > 8000) ratchet(+8000)`) — a day of catch-up is ~8.6k BLAKE3 ops, trivial.*

### C2. HIGH — The K4 freshness wall caps the mesh at ~30s regardless of TTL=8
Frames die ±2 epochs from their stamp (`MeshService.kt:938-941`). An 8-hop regional chain at ~2–4s/hop (250ms queue poll + jitter + 2s air + scan latency) needs 16–32s — the outer hops arrive exactly at the K4 boundary and are dropped as "skewed", firing the banner that tells users to **check their clocks** when the frame is just old. Worse: **no store-and-forward is possible** — a carrier walking a frame across a 2-minute gap between two groups delivers nothing; every receiver drops it. The "regional 8-hop mesh" only exists in dense, fast-relay crowds. *Fix options: separate "age" from "skew" in the banner wording; consider hop-count-aware freshness (accept older frames at low remaining TTL); document the 30s effective diameter.*

### C3. HIGH — Private messages have zero delivery feedback
`MeshService.kt:466-469`: private frames are never registered with `rememberOwnFrame`, so a relayed echo is swallowed by dedup silently. Public gets ON_AIR→ECHOED; private — the tier where users most want confirmation — gets one tick, forever. The comment ("a second tick could never honestly appear") is circular: it can't appear *because* it's not registered. An echo would prove relay, not decryption — the same honest caveat as public. *Fix: register the sealed private hash (same 4-epoch retention), ack on first echo.*

### C4. MEDIUM — Small-MTU GATT links go deaf exactly when the mesh gets busy
The RSSI poll's read-back is the **only receive path** for MTU<229 links, and it's skipped while `writeInFlight` (`GattPlane.kt:976-980`). A relay burst = 13 serialized chunk-writes per frame per peer = seconds of continuous write-busy → the phone hears nothing from that peer for the whole burst. Busier mesh ⇒ deafer legacy phones, anti-correlated with when alerts matter.

### C5. MEDIUM — `epochMs` mismatch partitions silently
epochMs is user-configurable (5–120s) with no handshake or version exchange. 10s vs 20s: diff alternates 0/1 → K4 never fires the banner, half the frames just vanish. Only diff>2 shows a warning.

### C6. MEDIUM — Private windows black out OTA relaying on single-set controllers
Each 6s private window hijacks the primary advertising set; on 1-set controllers `advertiseRelayOnce` fails during it (~18s per private send incl. re-airs). Also `advertisePrivateOnce` returns true before the hardware confirms — ON_AIR tick shown even if the frame never aired (`MeshService.kt:464-469`).

### C7. MEDIUM — Stale frame restored to air after private window
`restoreFrame` is captured at send time (`MeshService.kt:448`); if the window spans a rollover, the epoch-loop builds the fresh frame but skips airing it (`privateTransportActive`), and the restore runnable puts the **epoch-old** frame back on air for up to a full epoch — while GATT correctly gets the fresh one (`onRestored` uses `currentPublicFrame`). Inconsistent; *fix: restore `currentPublicFrame ?: restoreFrame` on air too.*

### C8. MEDIUM — Honest 1-element claims CellMismatch in cells of ≥4 devices
First epoch after service start / scan gap: sender's sketch is just its self-mark. Jaccard(1-claim vs N-mark cell) = 1/N < τ=0.3 for N≥4 → LOCAL **dropped entirely** (`MeshService.kt:1179-1186`). Honest LOCAL alerts fail in exactly the growing-crowd case, worst in the first epoch. Same math as the grind attack, hit by honest users. *Mitigation exists (richer-of-prev/cur sketch) but doesn't cover cold start.*

### C9. LOW — Legacy peer acquisition ~13–20s
Drive-by legacy peers (<8s visible) never connect; moving crowds may never sync over GATT.

### C10. LOW — `clockSkewWarning` clears on *any* fresh frame (`MeshService.kt:1050`)
Mixed crowd (one 25s-skewed phone among normal ones): banner flaps off while the skewed phone stays fully partitioned.

### C11. LOW — `beaconFloorMs ≥ epochMs` config combo stops mark rotation every other epoch (unlinkability halved, no warning).

---

# Part 3 — Security vulnerabilities (consolidated from three deep audits)

**No critical cryptographic breaks found.** Signature verification is `verify_strict`, the codec is panic-free on remote input, KDF domain separation is thorough, TTL/dedup mutation vectors are closed. The serious findings:

### HIGH
| # | Finding | Where |
|---|---------|-------|
| S1 | **Pairing has no authentication**: no SAS/fingerprint, no key-continuity check; re-pairing under an existing label silently replaces the key (`PairStore.kt:322`). Two-QR MITM or one social-engineered re-scan → all "E2E" traffic readable | PairStore/MeshUi |
| S2 | **PoCP co-presence remotely forgeable**: witness MAC key derives only from public values (`pocp.rs:151-156`); div_sketch travels **cleartext inside relayable frames up to 8 hops** — the copy source needs no proximity at all (in-code doc understates this as same-cell). R1 mitigation is log-only | pocp.rs |
| S3 | **Small-cell grind**: no intersection floor — 1-element claims score 1/N ≥ τ for N≤3; 256-frame sweep lands ~2–3 accepted forgeries against the small clandestine cells that are the highest-value targets | pocp.rs:83-97 |
| S4 | **Distributed builds are debuggable** — no release buildType at all; adb JDWP dumps `sk`, salts, pair keys, plaintext. Field APKs are debug builds | build.gradle.kts:20-25 |
| S5 | **v2 salt not deleted after pairing**: `mySalt` lives per-process, shared across all contacts in the run, shown as copyable text. Memory dump (via S4) + recorded ciphertext → full private history of every contact paired that run. Headline FS claim only holds after process death | PairStore.kt:326-330, MeshUi.kt:1122 |

### MEDIUM
| # | Finding |
|---|---------|
| S6 | **Verify-flood amplifier**: bad-sig frames (`MeshService.kt:923-930`) and K4-stale frames return *before* dedup insert — replaying the same frame costs a full Ed25519 verify per copy forever, over both BLE and unauthenticated GATT writes. Fix: small bounded reject-cache |
| S7 | **GATT slot pinning**: subscribed centrals exempt from idle eviction forever (`GattPlane.kt:162`); client peers have no idle timeout once connected → 4+3 dongles starve legitimate legacy phones indefinitely |
| S8 | **Core has no replay/freshness enforcement**; dedup time-decay is keyed to *unvalidated wire epoch* — one far-future frame wipes the whole seen-set (contained on Android by K4, but every future shim must re-discover this) |
| S9 | **VDL PoW 22 bits** ≈ milliseconds on GPU, and precomputable across epochs (no unpredictable anchor in the prefix) — stops phone spammers only |
| S10 | **v1 silent downgrade**: bare 64-hex accepted as pairing code → static non-FS key (`PairStore.kt:186-190`); no wire indicator of v1/v2 |
| S11 | **Sybil corroboration**: one nearby attacker mints up to 32 "distinct cells" per alert body (hint-only by design, but it's a social-engineering lever) |
| S12 | **Panic-wipe gaps**: queued `privateSends` plaintext, `pendingPrivate`, `ownHashes`, and the Kotlin-held `dedup`/`trust` FFI instances survive |
| S13 | **Contact labels leak into the exportable debug log** on exactly the failure paths users report (`MeshService.kt:1118`, `PairStore.kt:432`) — contradicts the codebase's own stated policy |

### LOW (selected)
No zeroization of pair keys/plaintexts in Rust (M5); empty AAD (defense-in-depth); `min_hearers=0` → constant beacon entropy reported healthy; no τ/`observe()` length validation at FFI; `anchorEpoch=0` ratchet-brick via malicious QR; private-send RF signature (public frame vanishes ~6s — linkable by passive sniffer); lock-screen notification shows live neighbor counts; `make_test_frame` in production FFI; nonce reuse rests entirely on shim counter persistence — recommend random 8 bytes in the signed, unused `div_sketch[8..16]` to make it self-healing.

---

# Part 4 — Proposed remediation order

1. **C1** (ratchet chunking) — one-function fix, unbricks private chat
2. **S4+S5** (release buildType; rotate salt on pairing-dialog close + remove copyable SALT text)
3. **S6** (reject-cache before K4/bad-sig returns)
4. **C3** (private echo receipt) + **C7** (restore fresh frame)
5. **S1** (post-pairing fingerprint screen + key-changed warning)
6. **C2** (banner wording split age-vs-skew; document 30s diameter)
7. S12/S13 wipe + log hygiene; S7 GATT eviction

**Questions before I implement anything:**
- Should I start with the communication fixes (C1, C3, C7), the security quick wins (S4, S5, S6), or a specific subset you pick?
- For S2/S3 (PoCP forgery/grind): the durable fix is the fuzzy-extractor redesign deferred to M6+ — do you want an interim mitigation now (e.g., intersection-count floor with cold-start exemption, harder R1 response), or leave as documented?
- Is the legacy GATT plane still a supported target, or should comm fixes prioritize the extended-adv path?

---

## User

Audit remediation — full scope

 Context

 A three-part audit landed against BileiChat at 0.15: a verification of the delivery-receipt
 lockstep diagnosis, 11 communication defects (C1–C11), and ~24 security findings (S1–S13 plus
 lows). I verified the load-bearing claims against the source before planning. Most are real.
 Two need correcting, one is already documented in-code, and several interact in ways that
 change what a safe fix looks like.

 The user chose full scope, including pairing authentication, and a debug-signed release
 build with minification for the APK hardening.

 Nothing here is a wire-format change. Two items change receiver-side policy (PoCP verdicts,
 dedup decay) and one adds a pairing-time step (SAS). Frames stay byte-identical, so a phone
 on this build and a phone on 0.15 still exchange traffic.

 ---
 Verification verdict

 ┌────────────────┬─────────────────────────────────────────┬──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐     │       ID       │                  Claim                  │                                                                                 Verdict                                                                                  │      ├────────────────┼─────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
 │ C1             │ Private bricks permanently after >22.8  │ Confirmed — worst finding. fastForwardChains (PairStore.kt:425) makes one pairRatchet call; crypto.rs:139 refuses spans >8192, returns None, loop hits continue.         │
 │                │ h offline                               │ chainEpoch freezes and every later epoch widens the span. Its own doc comment (PairStore.kt:406-423) computes the 22.8 h figure and fixes only the app-running half.     │     ├────────────────┼─────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤     │ S4             │ Field APKs are debuggable               │ Confirmed. build.gradle.kts:20-25 declares only debug { isDebuggable = true }. No release block exists.                                                                  │
 ├────────────────┼─────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
 │ C3             │ Private has no delivery echo            │ Confirmed. rememberOwnFrame is called once, at MeshService.kt:1405 (public path only). The "could never honestly appear" comment at :467 is circular.                    │
 ├────────────────┼─────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
 │ S13            │ Contact labels leak to the exportable   │ Confirmed. PairStore.kt:432 interpolates c.label; PairStore.kt:394-396 forbids exactly this, 35 lines earlier.                                                           │     │                │ log                                     │                                                                                                                                                                          ├────────────────┼─────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
 │ C7             │ Stale frame restored to air after a     │ Confirmed. BleRadio.kt:616 re-airs the captured restoreFrame; MeshService's onRestored gives GATT currentPublicFrame. Air stale, GATT fresh.                             │
 │                │ private window                          │                                                                                                                                                                          │      ├────────────────┼─────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
 │ C8 / S3        │ Honest 1-element claims rejected at     │ Confirmed, and they are one defect. Jaccard is a ratio with no count floor, so a 1-element claim scores 1/N. Disjoint N ranges, same knob. pocp.rs:34-36 already states  │
 │                │ N≥4; forged ones accepted at N≤3        │ they must be redesigned together.                                                                                                                                        │
 ├────────────────┼─────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
 │ S8             │ Dedup decays on unvalidated wire epoch  │ Confirmed. statemachine.rs:76-78 purges relative to the frame's epoch. K4 contains it today at ±2.                                                                       │
───────────────┼─────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
 │ S12, S7, C4,   │ —                                       │ Confirmed as described.                                                                                                                                                  │
 │ C6, C10, S10   │                                         │                                                                                                                                                                          │
 ├────────────────┼─────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
 │ S2, S3         │ PoCP forgeability                       │ Already documented. pocp.rs:5-36 is a module header covering both, with the measured forgery table. Not new; the witness() doc does understate reach (see below).        │
 ├────────────────┼─────────────────────────────────────────┼──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
 │ C2             │ ±2-epoch wall caps mesh diameter        │ Confirmed; proposed fix is unsafe. See below.                                                                                                                            │
 └────────────────┴─────────────────────────────────────────┴──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘

 Two corrections

 C2's suggested fix is a replay amplifier. "Hop-count-aware freshness (accept older frames at
 low remaining TTL)" cannot be done: TTL sits in the hop-mutable region deliberately excluded from
 the frame hash and signature (MeshService.kt:1396-1399). It is attacker-settable. Gating freshness
 on it lets anyone re-air an arbitrarily old frame with TTL=1 and have it accepted.

 There is a safe version, and S8 is its precondition — see T3-C2.

 S2 understates its own reach. witness()'s residual-gap note (pocp.rs:174-179) says an attacker
 can copy a div_sketch "from the same cell", implying proximity. div_sketch rides in the relayable
 frame body up to 8 hops, so the copier needs no proximity at all. Doc fix, and it raises the
 priority of the R1 same-epoch reuse detector already at MeshService.kt:1206.

 Cross-finding interaction — must be handled together

 Chunking the ratchet (C1) makes any span computable. A malicious QR carrying anchorEpoch=0
 (the "low" in the audit) then sets chainEpoch = 0 at PairStore.kt:299-311, and the next
 fast-forward tries ~170 M BLAKE3 steps. The C1 fix without an anchor sanity-clamp converts a
 brick into a CPU bomb. Both land in T1 together.

 ---
 T1 — Silent killers (no wire change, no protocol risk)

 F1. Chunked ratchet — PairStore.kt. Add one private helper and route all four pairRatchet
 call sites (fastForwardChains, keyForSend, candidateKeys, noteOpened) through it:

 private const val RATCHET_CHUNK = 8000u
 /** Chunked because pair_ratchet refuses spans >8192 (crypto.rs:139). Composition is exact:
  *  the ratchet iterates e in (from+1)..=to, so (a→b)+(b→c) walks the same index sequence
  *  as (a→c). */
 private fun ratchetTo(key: ByteArray, from: UInt, to: UInt): ByteArray?

 Clamp offer.anchorEpoch at PairStore.kt:299 to currentEpoch() - MAX_ANCHOR_BACKDATE
 (one day of epochs) before the minOf, rejecting the pairing outright below that. Add a
 matching absurd-span refusal in ratchetTo that surfaces "re-pair required" rather than
 grinding. Note in the code that a backward clock jump still stalls the chain until the clock
 catches up — self-healing, unlike C1, so not fixed here.

 Catch-up runs once per epoch in fastForwardChains; keeping it succeeding is what holds
 candidateKeys' on-the-fly span at ≤2 and stops per-frame recomputation in the receive path.
 Confirm it stays off the main thread (it does a commit() per contact).

 F2. Release build — build.gradle.kts + new proguard-rules.pro. Debug-signed, minified, as
 chosen. Minification is the risk here: UniFFI and JNA resolve by reflection and fail at
 runtime, not compile time. Keep rules for uniffi.**, com.sun.jna.**, and JNA Structure
 subclasses are mandatory. Bump versionName. This must be validated by installing the release
 APK and exercising a private send — the FFI-heaviest path — not by a successful build.

 F3. Private second tick — MeshService.kt. Register the sealed frame at :477 alongside the
 existing dedup.checkAndInsertEpoch. Add a private: Boolean to OwnFrame so the echo branch
 (:976-995) routes to MeshState.privateRowTs instead of outgoingRowTs. Re-airings reuse the
 same bytes, so the same hash, and ownHashesAcked already makes claimOwnEcho one-shot.
 Semantics stay honest: a relayed echo of a sealed frame proves relay, not decryption — the same
 claim the public tick makes, and SendState's doc already says it.

 F4. Label leak — PairStore.kt:432. Drop c.label. Grep every logDebug for label
 interpolation; MeshService.kt:1118 was also cited.

 F5. Live restore frame — BleRadio.kt:602-621. Change restoreFrame: ByteArray to
 restoreFrame: () -> ByteArray? so the restore Runnable reads the current frame at fire time.
 Two call sites (private send, reairPendingPrivate).

 F6. Honest ON_AIR tick — BleRadio.kt / MeshService.kt. Both the public path (:1409-1414) and
 advertisePrivateOnce set the tick immediately after an async start call. Move the transition to
 fire from onAdvertisingSetStarted on ADVERTISE_SUCCESS.

 F7. Panic-wipe gaps — MeshService.kt:1432. Add: zero and clear pendingPrivate; clear
 ownHashes + ownHashesAcked; drain MeshState.privateSends (it holds plaintext); zero
 each relayQueue frame before clear(); null outgoingRowTs / privateRowTs.

 ---
 T2 — PoCP attest/display split (C8 + S3, one change)

 Receiver-side policy only. No wire change.

 today:     jaccard >= tau        -> Valid (full trust)
            else                  -> CellMismatch, LOCAL dropped

 proposed:  inter >= 2 && j>=tau  -> Valid
            inter == 1            -> Unattested: DISPLAY, low-confidence badge
            inter == 0            -> CellMismatch, drop

 verify_witness_local (pocp.rs) gains the intersection count and a fourth verdict. Kotlin maps
 it to displayOk = true with lowConfidenceCell forced true, reusing the badge path that
 already exists at MeshService.kt:1258 and MIN_TRUSTWORTHY_CELL.

 This closes both directions with one knob:
 - S3: a ground-out 1-element sketch can no longer reach Valid. The 256-frame sweep yields
 zero full-trust forgeries at every cell size.
 - C8: a cold-started phone in a crowd of ≥4 is displayed-and-badged instead of dropped.
 - Two-device cold start improves too: today a 1-element claim scores 1/2 and is granted
 full trust — the forgeable case. It becomes badged.

 Sharpen the witness() residual-gap doc to say the copier can be up to 8 hops away.

 ---
 T3 — Reliability and hardening

 C2 + S8 (ordered — S8 first). S8 is the precondition. Split dedup's two uses of epoch:
 bucket by the frame's epoch (sub-cap accounting), decay by the local epoch. That removes the
 "one far-future frame wipes the seen-set" primitive and makes the window authoritative rather
 than wire-controlled. Only then is widening K4 from ±2 to ±4 safe — replay protection shifts onto
 dedup, so dedup's ~3-epoch decay (statemachine.rs:76-78) must be widened to cover it in the same
 change. Target ~40 s, enough for 8 hops at ~4 s/hop. Do not touch TTL-based freshness.
 Reword the skew banner to name transit age as a cause alongside clock skew, and document the
 effective diameter.

 C10. Clear clockSkewWarning only after 3 consecutive rollovers with rxSkewed == 0, reusing
 the counters added in 0.15, instead of on the first fresh frame (MeshService.kt:1050).

 C5. No handshake exists and none is worth adding. Detect instead: a sustained non-zero
 rxSkewed fraction that never trips the ±N banner is the signature of a mismatched epochMs.
 Surface it as its own note, and warn on the settings row that this value must match every phone.

 C4. GattPlane.kt:976 skips the RSSI read-back — the only receive path on MTU<229 links —
 whenever a write is in flight, so a relay burst deafens the link for its duration. Read the
 write/read interleave at GattPlane.kt:890-1010 and give reads a guaranteed slot between chunk
 writes.

 S6. Bad-signature and past-stale frames return before any dedup insert, so replaying one
 costs a full Ed25519 verify per copy forever. Add a small bounded reject-cache keyed on a cheap
 hash of the raw 226 bytes, checked before verification. Cache bad-sig and past-stale only —
 future-stale frames legitimately become valid as our clock advances.

 S7. GattPlane.kt:153 exempts subscribed centrals from idle eviction permanently. Keep the
 exemption (the comment at :158 explains the churn it avoids) but add a long hard tenure cap with
 rotation, and an idle timeout for client-side connections (:590).

 S10. A bare 64-hex string silently pairs as v1 with no forward secrecy
 (PairStore.kt:186-190). Require explicit confirmation and mark the contact
 LEGACY-NO-FORWARD-SECRECY in the list.

 ---
 T4 — Pairing authentication (S1) and salt lifetime (S5)

 S1a — key-continuity guard. PairStore.kt:322 replaces an existing label's key silently. Make
 addContact refuse when the label exists with a different derived key, returning a distinct
 result the UI turns into a blocking confirmation. This closes the one-scan social-engineering
 path on its own and is cheap.

 S1b — SAS. New pair_sas(shared, pk_a, pk_b) -> [u8; 4] in crypto.rs, domain-separated,
 public keys sorted so both sides agree; rendered as 6 digits. Both phones display it after ECDH;
 each user confirms it matches before the contact is persisted. A MITM holds two different shared
 secrets, so the two SAS values differ. ~20 bits — adequate for a one-shot face-to-face check,
 which is the only threat model that applies to QR pairing. Adds one confirm step to a flow that
 currently authenticates nothing while claiming E2E.

 S5 — salt scoped to a pairing session, not the process. PairStore.kt:326-330 deliberately
 never rotates mySalt, so it lives all day and mySaltHex displays it. Sequential
 face-to-face pairing needs it stable only across one session: add
 beginPairingSession()/endPairingSession() driven by the pairing dialog's lifecycle plus a
 timeout, zeroing on close. Correct the forward-secrecy wording, which currently holds only
 "after process death".

 Lows, same pass. zeroize on Rust key types (M5); min_hearers=0 reporting constant beacon
 entropy as healthy; τ and observe() length validation at the FFI boundary; make_test_frame
 (ffi.rs:40) gated out of production builds; beaconFloorMs >= epochMs warning (C11); the
 lock-screen notification's live neighbour count. Nonce reuse: move the 8 unused bytes of
 div_sketch[8..16] to random, inside the signed region, so uniqueness stops depending entirely
 on shim counter persistence.

 ---
 Explicitly not doing

 - Hop-aware freshness — TTL is unauthenticated. Replay amplifier.
 - Fuzzy-extractor witness keying (the complete S2 fix) — deferred M6+ in-code; real crypto
sign, not remediation.
 - Sequential VDF (S9) — 22-bit PoW stops phone spammers, not GPUs. The interface already
lows a swap.
 - C9 (13–20 s legacy peer acquisition) — inherent to the MIN_PEER_AGE_MS dwell plus connect
st; would need a redesign of the GATT plane.  do you agree? tell me what else do you need tochange

---

## Assistant (Plan · Kimi K3 (2x usage) · 846.2s)

_Thinking:_

The user has presented a full remediation plan (T1-T4) based on my audit, with their own verification verdicts, corrections, and ordering. They ask: "do you agree? tell me what else do you need to change"

I'm in plan mode — read-only. My job: review their remediation plan critically, verify their claims against code where load-bearing, point out disagreements, gaps, and refinements. They explicitly asked for my opinion and what else needs changing.

Let me think carefully about each tier and check for problems in their plan. Key things to verify/scrutinize:

1. **T1-F1 ratchet chunking** — composition exactness claim: pair_ratchet iterates e in (from+1)..=to? Let me verify crypto.rs pair_ratchet semantics to confirm (a→b)+(b→c) == (a→c). Also the anchor clamp interacting with minOf: they clamp offer.anchorEpoch to currentEpoch() - MAX_ANCHOR_BACKDATE before minOf. Wait — clamp *below*? "rejecting the pairing outright below that" — anchor too old → reject. Also they note the CPU bomb interaction: with chunking, anchorEpoch=0 → chainEpoch=0 → fastForwardChains would try ~170M BLAKE3 steps once per epoch per contact — actually ratchetTo with an absurd-span refusal handles it. But also: what does `start` clamp mean for the backdate? chainEpoch = start - 3. If anchor=0 rejected, fine.

   Also there's a subtle issue: fastForwardChains target = epoch - 1. If chainEpoch = 0 legitimately (old contact from before?), chunking computes 17k steps × ... fine, one-time ~17k BLAKE3 ≈ fast (BLAKE3 ~1M/s single thread for small inputs? Actually BLAKE3 does ~1 GB/s, each step is tiny input → maybe 2-5M ops/s. 170M steps = ~1 minute of CPU. Hmm, that's the CPU bomb. With rejection above MAX span (e.g. > 8640*7 = a week?) → refuse and surface re-pair. OK.)

   Another subtlety: chunked ratchet in candidateKeys on the BLE binder thread — if a frame arrives with epoch far ahead of chainEpoch (attacker-stamped far-future epoch within K4 gate... K4 limits to ±2, so span ≤ ~few. Safe.)

2. **T1-F2 release build** — they chose debug-signed + minified. Keep rules for uniffi/jna. Also EncryptedSharedPreferences/Tink may need keep rules; Compose runtime usually fine with consumer rules. Validation: install release APK, exercise private send. Agree.

3. **T1-F3 private second tick** — add `private: Boolean` to OwnFrame, register sealed private frame at MeshService.kt:477. One issue: the private frame is re-aired verbatim 3× — same hash; ownHashesAcked one-shot; fine. Another: the echo could come back *after* the ±2 gate... irrelevant, echo recognition is hash-based, ownHashes retention 4 epochs. Fine. Also routing to privateRowTs: privateRowTs is single-slot — if two private sends are queued in flight (channel cap 8, sequential sealing), only the latest row ticks. Acceptable; note it.

   Edge: `claimOwnEcho` is called before dedup — private frame echo arrives; but wait: when the sender hears its own re-aired private frame... it can't hear its own transmission. OK. When a relay sends it back — claimOwnEcho fires. But careful: the private frame was inserted into dedup at :477, and also rememberOwnFrame would now insert into ownHashes. When echo arrives, textEcho requires `ownEcho.carriedText && MeshState.outgoingText.value.isNotEmpty()` — for private, outgoingText is empty! So routing must bypass the carriedText/outgoingText condition for private. Their plan says "routes to MeshState.privateRowTs instead of outgoingRowTs" — need to make sure the receipt text isn't set for private (or set a private-appropriate one) and that the LOCAL/BROADCAST expiry logic at :992-999 doesn't run for private. I'll flag implementation details.

4. **T1-F4 label leak** — agreed; also grep all logDebug.

5. **T1-F5 live restore frame** — restoreFrame: () -> ByteArray?. Careful: the lambda captures currentPublicFrame at fire time — but MeshService owns currentPublicFrame, BleRadio would need the lambda supplied by caller: `restoreFrame = { currentPublicFrame ?: restoreFrame }`... The signature change: advertisePrivateOnce(frame, durationMs, restoreFrame: () -> ByteArray?, ...). Call sites pass `{ currentPublicFrame }`. But zeroing: panic wipe zeroes frames; a lambda reading currentPublicFrame after wipe → null → skip startAdvertising. Good — handles that too. Also `onRestored` already uses currentPublicFrame. Agree, minor implementation notes.

6. **T1-F6 honest ON_AIR tick** — move to onAdvertisingSetStarted success. Careful: the tick path runs on BLE callback thread; markOutgoing is @Synchronized — fine. Also the watchdog and supersede logic: a start that succeeds then gets superseded by a coalesced frame — the tick refers to the *row*, still fine. Edge: legacy/GATT-only phones — startLegacyFallbackLocked's onAdvertisingSetStarted success means the UUID beacon is on air, NOT the frame (frames flow over GATT). On legacy, ON_AIR should fire on GATT write success or not at all? Currently on legacy, rebuildAndAdvertise → startAdvertising → early return at "legacy-uuid(gatt)" already running → no callback → tick never fires → row stuck at SENDING forever. Hmm — today: on legacy, startAdvertising returns early (line 168) without any callback; MeshService sets ON_AIR immediately (line 1416) — so today the tick fires. With F6 moving tick into the callback, legacy phones lose the tick entirely unless handled. Need a legacy-path tick (e.g., first GATT write/notify completes, or just fire ON_AIR when gattPlane.currentFrame set with subscribers>0...). Must flag this interaction.

7. **T1-F7 panic wipe** — agreed. Also relayQueue frames zeroed. Also `recentTexts`, `sketchSeen`, `lastSkewPair`... minor. And `MeshState.receipt`, `outgoingAiring`. Not critical. Also they mention draining privateSends — iterate channel and zero texts? Strings immutable on JVM — can't zero String plaintext; note the JVM limit (same as Contact keys). Best effort: drain + clear refs.

8. **T2 PoCP split** — inter>=2 && j>=tau → Valid; inter==1 → Unattested (display, low-confidence badge); inter==0 → drop.

   Wait, need care: inter counts intersection of the KMV-windowed set? In pocp.rs, jaccard computes over union-min sketch (KMV: x = union[..K], inter_in_x). "inter >= 2" — intersection within the KMV window. The grind table: 1-element claim vs N-mark cell — inter ∈ {0,1}. So inter==1 → Unattested. An attacker sweeping 256 1-element claims: each hits inter≤1 → never Valid → zero full-trust forgeries. ✓. But attacker can claim a 2-element sketch: 256² = 65536 combinations per epoch; hits inter==2 with probability ~C(N,2)/C(256,2)... for N=4: 6/32640 ≈ 0.018% per frame; ~6 accepted Valid forgeries per 65k sweep against a 4-device cell. Much worse than now? No — now, 1-element sweeps get *Valid* at N≤3 (tau 0.3, 1/3≥0.3). With floor: at N≤3, attacker needs 2-element hits: C(3,2)=3 pairs → 3/32640 per frame → ~0.009%... The sweep cost went from 256 frames (trivial, 1 epoch) to 65k frames (256 epochs of airtime at 1 frame/epoch... or burst: per-epoch bucket cap 1024 → 64 epochs minimum). Significantly raised. But not zero. And C8 side: honest 2-device cell cold start — honest claim includes self + heard marks, so honest inter ≥ 2 typically (self-inclusion on both sides: A claims {A,B}, B holds {B,A} → inter=2 ✓ Valid). A cold-started phone in a 4+ crowd claiming 1 element → inter=1 → Unattested, displayed with badge ✓.

   Edge: what about the honest 1-element claim in a 2-device cell — inter could be 1 ({A}∩{A,B}) → Unattested (badged). Their plan says this improves 2-device cold start. ✓.

   False-positive risk: does inter==1 happen for *honest co-located* senders often enough to matter? Honest sender in steady state has a rich sketch (heard everyone) → inter large → Valid. Only cold start / scan gaps → badged temporarily. Acceptable UX. Also BROADCAST path: displayOk already true on MAC-valid; badge feeds from Jaccard — the new verdict maps cleanly.

   Also: the KMV window — jaccard() uses union[..min(K, union.len())] and counts intersection within that window. inter count from KMV is an *estimate* of true intersection, biased down for large unions. With K=16 slots and cell sizes <32ish, fine. For big cells (union > 16), the KMV estimate of inter is a sample — a claim with true inter=5 might show inter_in_x=1 → downgraded to Unattested. Hmm — that's a real false-downgrade path for large cells. But wait: for the claim to be Valid you also need jaccard≥tau. Large honest cells → claims rich → inter_in_x scales ~K·jaccard... e.g. j=0.3, K=16 → expected inter_in_x ≈ 4.8 ≥ 2 ✓. Marginal cases (j≈tau) could flip between Valid/Unattended — acceptable, badge is a hint anyway. OK, T2 is sound. One more: verify_witness_local returns verdict; the FFI returns int; adding a 4th verdict value (e.g., 4=Unattested) — wire-compatible, FFI-compatible. Kotlin mapping updated. Rust tests updated. Fine.

9. **T3 C2+S8** — split dedup: bucket by frame epoch, decay by local epoch. Wait — decay by local epoch: the Dedup is in Rust core; local epoch must be supplied by shim. New API: check_and_insert_epoch(hash, frame_epoch, local_epoch)? Or a separate `advance(local_epoch)`? Their plan: "bucket by the frame's epoch (sub-cap accounting), decay by the local epoch". The decay purge currently happens inside check_epoch using the frame's epoch. New design: purge entries older than local_epoch - 2. Local epoch supplied by shim per call or via a setter. Then widen K4 to ±4 *and* widen dedup decay window to cover ±4 (entries must live ≥5 epochs to catch replays within the widened gate... decay >4 behind local → keep 5-6 epochs). Dedup cap 4096 / bucket cap 1024 — widening the window to ~6 epochs raises steady-state entries ~2×; in crowds, 1024/epoch bucket × 6 epochs = up to 6144 > cap 4096 → FIFO eviction under pressure → replay window shrinks under load. Note: raising per-node memory (4096×~48B ≈ 200KB — fine). Also `was panic...` fine.

   Also careful: decaying by local epoch means an attacker stamping future epochs can no longer purge — ✓. But entries stamped with future epochs stay until local clock passes them + decay... a future-stamped entry (epoch = local+2, max K4) decays at local ≥ epoch+decay → fine.

   K4 ±2→±4: also PRIVATE frames: the re-airing budget (PRIVATE_REAIRINGS=2) and the ratchet window (prevKey only 1 back + chainEpoch at epoch-1 → covers ±2... candidateKeys: frameEpoch > chainEpoch fast-forwards (≤ span), == chainEpoch, == prevEpoch. With ±4, a private frame 3-4 epochs behind chainEpoch is undecryptable (keys ratcheted away). Widening K4 widens public acceptability but private open-window stays ±2ish — harmless (frames arrive, VDL-check, fail to open, relay anyway). But also fastForwardChains anchor "epoch-1 keeps window aligned with ±2 gate" — needs re-alignment to epoch-3 if K4=±4, else private frames at ownEpoch-3/-4 pass K4 but can't be opened (fine, they'd be relayed) — but *legit* re-airings: PRIVATE_REAIRINGS=2 covers +2; sender re-airing at E+3/E+4 would now pass K4 but receiver has no key (ratcheted away) → silently unopenable. So widening K4 for private needs either more re-airings or retaining more prevKeys. Simplest: widen K4 to ±4 for public path only, keep private at ±2? The K4 gate is type-agnostic today. Options: keep K4 ±2 for msgType 3, ±4 for 1/2. That's clean — private re-air budget already tuned to ±2. Flag this.

   Also skew banner wording + docs. OK.

   And S8 precondition ordering is right.

10. **T3 C10** — clear banner after 3 consecutive clean rollovers. Fine.

11. **T3 C5** — detect mismatched epochMs via sustained sub-threshold skew fraction. Hmm, 10s vs 20s: diff alternates 0/1 → rxSkewed counts... wait, rxSkewed increments only when |diff|>2 (K4 drop). 10s vs 20s → diff ∈ {0,1} → no drops, rxSkewed=0. Their C5 says "sustained non-zero rxSkewed fraction that never trips the ±N banner" — but rxSkewed IS the banner trip counter. Mismatched epochMs 10v20 produces *zero* rxSkewed. The actual signature: frames arrive with frameEp alternating = own/own+1 — measurable via lastSkewPair-ish stats, not rxSkewed. Also frames from a 20s-epoch phone observed against 10s local: every other frame diff=1 — no drop. Detection: track distribution of (frameEp - ownEpoch) over time; a persistent +1 bias with both phones NTP-synced suggests 2× epoch ratio. Honestly, simplest robust detection: *epochMs in the QR/pairing?* No — public mesh has no pairing. Alternative: warn on the settings row (their second half) + optionally surface "frames consistently from the future/past by exactly 1 epoch" hint. Their detection idea as stated doesn't work for the 10v20 case (rxSkewed stays 0). For 10s vs 5s: 5s phone's frames vs 10s local: local epoch advances half as fast; frames stamped with 2× epoch numbers... diff grows unboundedly → K4 trips constantly → banner fires (that's the loud case). The silent case is harmonic ratios (10v20, 10v30?): 10v30 → diff ∈ {0,1,2} → no drop; frames every third local epoch... some frames diff=2 pass, all pass. Silent. So detection via skew *drops* misses exactly the harmonic mismatches. Better: count frames by (frameEp - ownEpoch); if a sustained fraction has diff ∈ {+1,+2} (or the diff distribution is bimodal), warn "possible epochMs mismatch". I'll propose that instead.

    Actually wait — simpler: with 10v20, the 20s phone sends 1 frame per 20s; local 10s phone sees presence flicker (marks every other epoch) + every frame diff ∈{0,1}. The 20s phone sees the 10s phone's frames with diff ∈ {0,-1} — fine. Who loses? PoCP: sketches bucketed per epoch — the 20s phone's divSketch built from its 20s bucket vs local 10s buckets — candidate buckets frameEp±1 → frame from the 20s phone at frameEp=E20 arrives at local E10≈2·E20; local tries localSketch(2E20-? )... K4 diff = E20_frame - E10_own — wait the frame epoch numbers are now/20000 vs now/10000 — the *numbers* differ by 2×! K4: |E20 - E10| = |now/20000 - now/10000| = now/20000 → huge → K4 drops EVERYTHING. Hold on — epoch = now/epochMs. Two phones with different epochMs produce completely divergent epoch *numbers* (diverging linearly). So 10v20: diff = now/10000 - now/20000 = now/20000 → after 20s of uptime diff≥1, after 40s diff≥2 → K4 drops everything → banner fires loudly. I had it backwards: the epoch *index* diverges, so ANY epochMs mismatch → total partition + banner (loud), except in the first seconds. So C5's "silent harmonic partition" concern of mine was wrong; mismatch IS detected by the banner. The remaining C5 value is just the settings-row warning + banner wording ("check epochMs match" — already in the log line at :946 "check epochMs match + clocks"). Their detection idea is unnecessary; settings warning is cheap. I should correct this.

    Hmm wait, but their plan says "a sustained non-zero rxSkewed fraction that never trips the ±N banner is the signature of a mismatched epochMs" — as shown, rxSkewed increments only when K4 trips... and K4 trip = banner set at :958. rxSkewed>0 ⟺ banner set. So the premise is confused; but the underlying truth (mismatch → K4 → banner) means detection already exists. The only gap: banner wording says clocks, doesn't mention epochMs mismatch (log line does). So C5 reduces to: banner wording + settings-row warning. Cheap. Good.

12. **T3 C4** — GATT read/write interleave. Fine.

13. **T3 S6** — reject-cache before verification, keyed on cheap hash of 226 bytes. Cache bad-sig + past-stale only, not future-stale. ✓ correct reasoning (future-stale becomes valid as clock advances... wait, future-stale = frame from the future; as our clock advances it may enter the window — yes don't cache. But ALSO: don't cache frames rejected for other transient reasons (unjudged PoCP — already not inserted). Where exactly: insert into reject-cache at the bad-sig return (:923-930) and at the K4 past-stale return (diff < -2 branch only). The K4 branch currently returns for both; split. Cache size ~512, LRU. Cheap hash: first 16 bytes of a non-crypto hash? The frame hash FFI is BLAKE3 anyway (cheap, 226B); but that runs *after* verify today. Actually frameHash is computed at :931 AFTER frameVerifySelf — order: decodes → verifySelf → hash. For the reject-cache to short-circuit verify, hash must be computed before verify. frameHash is Rust BLAKE3 over buf[0..214] — microseconds. Reorder: hash → reject-cache check → verify → ... fine, hash of undecoded bytes? frameHash needs decode? Let me think — frameHash FFI likely decodes. A cheap Kotlin-side hash (e.g., contentHashCode is weak; use the 16-byte... simplest: call frameDecodes first (cheap), then frameHash (BLAKE3), then reject-cache, then verify. Verify order change is safe (hash doesn't trust content).

    Note: an attacker can still force verifies by *mutating* signed bytes (each mutation = new hash) — but each mutation costs them a BLE packet; the current hole is *free* replay of identical bytes. Reject-cache fixes the free replay; mutation flood remains bounded by airtime + bucket cap after insert... mutated bad-sig frames never insert → still verify-per-packet. But that's inherent (can't dedup what you can't authenticate); airtime-bound. Fine — note residual.

14. **T3 S7** — GATT tenure cap + client idle timeout. Fine.

15. **T3 S10** — v1 confirmation + LEGACY badge. Fine.

16. **T4 S1a** — refuse same-label different-key re-pair with blocking confirmation. Good. Detail: compare derived key (v1: shared; v2: seed0) or peer pubkey? Peer pubkey is the right comparison ("same person, new QR" — same pk → same key → allow silently; different pk under same label → block). Actually compare pkHex: if label exists and new pkHex != existing contact's... but Contact doesn't store peer pubkey! Contact stores pairKey (derived). Comparing derived key works: same peer pk + same salts → same seed0; but v2 salts differ per pairing session (mySalt rotates per session after S5!) → same peer re-pairing later yields a DIFFERENT seed0 even though same peer. So derived-key comparison would false-positive on legitimate re-pair (e.g., after wipe/reinstall, or salt rotation). Hmm: S1a's threat is "attacker convinces victim to scan attacker's QR under Bob's label". Legit re-pair with real Bob also produces different key (new salts) — so the guard can't distinguish; it must be a *blocking confirmation* in both cases ("Bob's key changed — confirm in person"). That's exactly their design ("returning a distinct result the UI turns into a blocking confirmation") — TOFU key-change warning, à la Signal. Fine. But note: with S5 salt-per-session, EVERY re-pair changes the key → every legit re-pair hits the blocking dialog. That's acceptable (re-pairing is rare and face-to-face). Also need: store nothing extra (pairKey comparison suffices).

    Also the same-label refusal should consider: after panic wipe, no labels exist → clean. OK.

17. **T4 S1b — SAS**: pair_sas(shared, pk_a, pk_b) → 4 bytes → 6 digits. 6 digits = 10^6 ≈ 19.9 bits. MITM forging must try ~500k ECDH... no wait — MITM can't grind: they perform two ECDHs with their own keys; the SAS values are determined by their key choices. To make SAS_alice == SAS_bob, MITM must find key pairs making the two SAS collide — a search over ~10^6/2... each attempt = 2 ECDH + hash ≈ feasible offline (~10^6 ops, seconds!). Hmm: 6-digit SAS gives ~20 bits; a MITM pre-computing keypairs to collide the two SAS values: they need f(shared_A, pkA, pkM1) ≈ f(shared_B, pkM2, pkB) — but shared_A depends on ECDH(sk_A, pkM1) — unknown to MITM until Alice's side runs (ECDH needs Alice's secret). MITM can't precompute shared_A. During the live exchange, MITM chooses pkM1/pkM2 adaptively? They see Alice's pk first (QR), choose pkM1; then present to Bob... To collide, MITM needs BLAKE3(shared_A‖...) ≡ BLAKE3(shared_B‖...) mod 10^6 — they'd grind pkM2 offline: for each candidate pkM2, compute shared_B = ECDH(skM2, pkB) — but shared_A is unknown (needs sk_A)! shared_A = ECDH(sk_A, pkM1) — MITM can't compute it... wait, ECDH(sk_A, pkM1) = ECDH(skM1, pkA) — MITM knows skM1! So shared_A = X25519(skM1, pkA) — computable. shared_B = X25519(skM2, pkB) — computable per candidate. So MITM CAN grind: fix pkM1, compute sA; then iterate skM2 candidates until SAS(sA, pkA, pkM1) == SAS(sB, pkB, pkM2)... but wait — the SAS displayed on Alice's phone: pair_sas(shared_A, sort(pkA, pkM1)); on Bob's: pair_sas(shared_B, sort(pkM2, pkB)). For the attack, need the two 6-digit values equal. Grinding skM2: each trial = 1 ECDH + 1 BLAKE3 ≈ ~1-2µs → 10^6 trials ≈ seconds. But the grind must happen DURING the pairing ceremony (pkM1 shown to Alice first, then pkM2 ground and shown to Bob) — sequential face-to-face pairing takes ~10-60s; an online grind of ~1M ECDH on a laptop = ~10-60s. Feasible but tight; a determined state actor with a GPU does it easily (ECDH ~50k/s/core → 1M in <1s parallelized). So 6 digits (20 bits) is grindable by the exact adversary in the threat model. Standard SAS uses 6 digits for *voice* comparison with liveness; for unattended screen comparison, still ~20 bits. Recommendation: 8-10 digits or 6 words from a wordlist (PGP word list: 8 words = 64 bits; even 4 words = 32 bits) — or keep 6 digits but rate-limit... no rate-limit possible offline. Cheap upgrade: 4-byte digest → render as 8 digits (10^8/2^32... use 26 bits = 8 digits via mod 10^8 → grind 10^8 ≈ minute-hours on GPU) or 3 BIP39 words (33 bits). I'd push back on 6 digits → recommend ≥31 bits (8 digits or 3-4 words). Their "~20 bits — adequate for a one-shot face-to-face check" is the weak point I should contest. Actually the classic analysis (ZRTP): MITM must commit to keys before seeing the other side's SAS... in QR pairing both public keys are fixed before SAS display, and the MITM's two shareds are determined by their chosen keypairs. The grind is: choose skM2 until SAS_B == SAS_A. SAS_A known to MITM only AFTER computing sA (yes, they compute it immediately from skM1, pkA — before approaching Bob). So full offline grind window = the time between scanning Alice and presenting to Bob — could be minutes (pre-stage: grab Alice's QR... wait, they can't get Alice's QR without Alice present; but the MITM device can grind in real-time while the victims fumble with phones). 20 bits = 1M ECDH ≈ 20s on one CPU core, <1s on a laptop with multiple cores. Too low. 30+ bits (e.g., 9 digits or 4 words) puts single-core at ~5 hours. I'll recommend 4-byte SAS rendered as 4 PGP/BIP39 words (32+ bits) or 9 digits with a note. This is a substantive disagreement to raise.

    Also SAS construction: hash must bind shared + both pubkeys sorted — ✓ their spec. Also include a label/context? Keep simple.

    Also S1b flow: confirm before persisting contact — addContact currently persists immediately; need pending-contact state. Fine, UI work.

18. **T4 S5** — beginPairingSession/endPairingSession + timeout, zero on close. Good. Detail: sequential pairing across dialog close → new salt each session ✓. Also mySaltHex display — remove copyable SALT text (their earlier T... S5 mentions wording fix; the copyable text removal was in my audit — they fold into S5? "mySaltHex displays it" — they reference; ensure removal of the SelectionContainer SALT line. Also the QR itself displays salt — necessary.)

    Also: pairings made in one session share mySalt — memory dump compromises that session's contacts. Accepted tradeoff, narrower than per-process. Note: endPairingSession must NOT break pending sequential pairs (timeout based on last addContact).

19. **Lows** — zeroize, min_hearers floor, tau/observe validation, make_test_frame gating, beaconFloor warning, notification neighbor count, random nonce in div_sketch[8..16].

    ⚠️ The random nonce in div_sketch[8..16]: "Nothing here is a wire-format change" — using the 8 reserved-but-signed bytes changes what's ON the wire (bytes 26..34 of div_sketch). Wait — div_sketch[8..16] is currently zeroed for private frames (message.rs:255-257 fills [0..4]=pk prefix, [4..8]=counter, [8..16]=zeros?). Filling them with random changes frame bytes → hash changes (fine), signature covers them (fine), PoCP witness prefix covers bytes 0..102 which INCLUDES div_sketch (18..34) → private frames don't use PoCP witness (they use VDL)... VDL prefix is also buf[0..102] → the VDL solve covers those bytes — solve after filling. Old receivers: private frame open path — openPrivateBodyOnly uses div_sketch[4..8] as counter for nonce; [8..16] ignored? pocp sketch interpretation of div_sketch: for private frames, does the receiver interpret div_sketch as a sketch? Trust/pocp paths apply to msgType 1/2. msgType 3 path: vdlCheckFrame + openPrivateBodyOnly. So filling [8..16] random is backward-compatible for private receivers IF nothing validates those bytes as zero. Codec reserved-tail check applies to bytes 215..226 (reserved region), not div_sketch. So compatible-ish: it IS a wire-format change in the strict sense (old code expected zeros there — does any old code reject nonzero? message.rs body validation? For private, body is the 48-byte AEAD block; div_sketch is not validated as zero by old receivers — the audit said "8 signed-but-semantically-null bytes". If no validation, old builds accept. Need to verify no zero-check on div_sketch[8..16] in message.rs/codec.rs — the earlier audit said codec reserved check is on the tail; private frame construction zeroes them. Old receiver: frameDecodes → OK. So safe.) But: does the *nonce* change break old SENDER/receiver interop? The nonce derivation is sender-side: nonce = epoch ‖ suffix(pk, counter); if the new scheme uses random in div_sketch[8..16] as nonce suffix instead of counter-hash, the RECEIVER must derive the same nonce — open_private_body reconstructs nonce from (epoch, pk, counter from div_sketch[4..8]) — if sender now uses div_sketch[8..16] random as the suffix, old receivers compute the wrong nonce → cannot decrypt. **THIS IS a private-tier wire-protocol change** unless done as: nonce_suffix = BLAKE3(pk ‖ counter ‖ div_sketch[8..16]) with receivers updated — old receiver + new sender = broken private chat. New receiver + old sender = zeros in [8..16] → must fall back to counter-only derivation. So it needs version-conditional derivation — a compat shim — or accept private-tier break between 0.15 and 0.16. The user's plan says "Nothing here is a wire-format change" — this item violates that invariant for the private path unless implemented with dual-derivation trial (try new nonce, then old nonce — cheap, one extra AEAD open per key... or gate on frame version byte? Version is fixed... there may be no version bump mechanism). Flag this clearly: either (a) accept private compat break (old↔new can't private-chat), (b) dual-trial on receive (new receivers open both schemes; old receivers can't open new senders — still a one-way break), or (c) defer. Given "frames stay byte-identical" claim — this item breaks it. Important catch.

    Actually wait — re-check: is div_sketch[8..16] really unused on the receive path for private? openPrivateBodyOnly(bytes, key) → reconstructs nonce from frame fields: epoch, pk, counter = div_sketch[4..8]. If sender puts random in [8..16] AND uses it in nonce derivation, receiver must too. There's no way to keep old receivers working. So yes — private wire change. Alternatively: keep wire unchanged and derive the nonce as BLAKE3(pk‖counter‖body_random?) no. Or: make the *counter itself* random per message (nextPrivateCounter returns random 31-bit each call — memory-only fallback already does exactly this!) — collision domain 2^31 per (epoch, pk) pair; with ~100 msgs/epoch, birthday bound ~2^-17... The clean no-wire-change fix: counter = 32-bit random per frame (persist nothing) → nonce reuse probability per epoch ≈ k²/2^32; k=1000 → 2^-12. Hmm, not negligible at high volume. The div_sketch[8..16] approach adds 64 bits — much better. So the real tradeoff: private compat break vs. nonce-reuse robustness. Since the shim (encrypted-prefs counter) is already solid on Android, this is hardening for hypothetical future shims — I'd recommend deferring it or doing the dual-trial. Flag it.

20. **"Explicitly not doing"** — agree with all four: hop-aware freshness (correctly identified as replay amplifier), fuzzy extractor, sequential VDF, C9.

21. What did they MISS? Let me scan my findings for anything not covered:
    - **C6** (private windows black out OTA relaying on single-set controllers) — their verification table says "S12, S7, C4, C6, C10, S10 — Confirmed as described" but I don't see C6 in any T! T1-F6 touches honest ON_AIR tick (related to my C6 second half). The first half — 18s of relay blackout per private send on 1-set controllers — no fix planned. Is there one? Options: cap windows (already 6s), coalesce re-airings, accept. Reasonable to accept as hardware constraint, but should be stated in "explicitly not doing" with rationale. Ask.
    - **C3** ✓ T1-F3. **C8** ✓ T2. **C11** ✓ lows (beaconFloor warning).
    - **S4** ✓ T1-F2. **S5** ✓ T4. **S13** ✓ T1-F4. **S12** ✓ T1-F7. **S1** ✓ T4. **S6** ✓ T3. **S7** ✓ T3. **S8** ✓ T3. **S10** ✓ T3.
    - **S9** — not doing (agreed).
    - **S11 Sybil corroboration** — not mentioned! trust.rs 32-claim Sybil. Options: cap display count, weight by cell overlap... It's "hint-only" — acceptable to defer, but should be explicitly acknowledged in "not doing".
    - **Lows from audit not in their list**: FFI open_private_frame per-contact-loop footgun (L4 — remove/rename export); F4-core dedup head-of-line blocking (fixed incidentally by local-epoch decay? Decay by local epoch: purge from front while entry too old — HOL blocking by future-epoch entry persists? If decay keyed on local epoch and entries bucketed by frame epoch: a far-future entry at head... purge condition "entry.frame_epoch < local_epoch - window" — far-future entry never purgeable by time → still blocks head → entries behind never decay → but they're evicted by cap FIFO eventually. Their split: does purge still scan from front? If yes, HOL remains for future-stamped entries. Suggest: purge predicate should treat future-stamped entries as expiring at (their_epoch + window) i.e. evict when local_epoch > entry_epoch + window — future entries eventually expire when local clock passes them. With K4 ±4 gate before dedup, entry epochs are within ±4 of local → HOL bounded ~4+window epochs. OK minor; mention.)
    - **F13 TOCTOU** — minor, unaddressed; fine.
    - **F9-android RF signature of private sends** — not addressed; hardware constraint, fine to explicitly not-do.
    - **F10 notification** ✓ lows.
    - **F11 clipboard** (cell sketch copy sensitivity) — not in their lows list. Minor; could add EXTRA_IS_SENSITIVE or auto-clear. Mention as optional.
    - **F12-android anchorEpoch=0** ✓ folded into T1-F1 clamp.
    - **Android F5 dedup-on-bad-sig** = S6 ✓.
    - **Crypto agent's L1 v1 downgrade** = S10 ✓. **L2 AAD** — not mentioned in their lows! Empty AAD hardening (bind SIG_REGION as AAD) — changes AEAD → private wire change (old receivers fail). Same compat issue as nonce randomization; defer with it. Should be grouped: if ever doing a private v2 wire bump, bundle AAD + random nonce. Otherwise leave. Mention.
    - **M5 zeroize** ✓ lows.
    - **I1 hex dep** — trivial, skip.
    - **make_test_frame gating** ✓.
    - **min_hearers=0** ✓ lows.
    - **tau/observe validation** ✓ lows.
    - **R1 soft detector priority** — they say "raises the priority of the R1 detector" but no concrete strengthening planned (still log-only?). T2's inter-floor reduces the payoff... The copy attack still yields *Valid* with a full copied sketch (inter = full overlap ≥ 2!). Wait — T2 doesn't stop S2! Copying a complete div_sketch gives inter = min(|copied|, |local|) which is ≥2 for any copied sketch with ≥2 elements → passes the floor → Valid. T2 only stops the *grind* (S3), not the *copy* (S2). Their verification table acknowledges S2 is doc-only + R1. So after remediation, a remote copier within 8 hops still forges co-presence at full trust. The R1 detector (same-epoch sketch reuse across marks) catches the lazy case; copying with a per-epoch fresh source frame still works. State clearly: S2 remains open by design (M6+ fuzzy extractor), doc sharpened. Make sure the user is signing off on that consciously — it's in "not doing" implicitly (fuzzy extractor). OK.

    - **Echo/queue-depth** (my addition to receipt latency — relay queue depth in crowds) — not in plan; it's informational, no fix needed. Fine.
    - **C5 detection** — corrected above (mismatch is already loud via K4 banner; epoch numbers diverge linearly).
    - **GATT MITM blackholing** — inherent, not planned; fine.
    - **Android F3 debuggable → release build** ✓ T1-F2.
    - **The stale-witnessless frame: rebuildAndAdvertise fallback to baseFrame when divSketch==null** — with recordSelf, unreachable. Fine.
    - **What about `PairStore.contacts()` cache invalidation races** — contactCache volatile, nulled on mutation; binder thread reads — benign.
    - **`prefsCache` after wipe**: PairStore.wipe clears prefs but prefsCache remains — fine (recreated). After panic wipe, service stops anyway.

22. Also — **T1-F1 ordering detail**: ratchetTo chunk helper + absurd-span refusal. Also keyForSend currently: `if (epoch <= contact.chainEpoch) return contact.pairKey` — if a sender's clock jumped backward, keyForSend returns current key with frame stamped at *their* (backward) epoch — receivers drop at K4 (frame too old). Self-healing per their note. OK.

    Also the anchor clamp: `minOf(it, currentEpoch())` then backdate 3 → if offer.anchorEpoch is far future (malicious QR with anchor = now+1 year): minOf → currentEpoch → fine ✓. Far past: clamp/reject ✓.

    One more C1-related: **candidateKeys fast-forward on receive path** — `frameEpoch > contact.chainEpoch` → ratchet span ≤ K4+1 — tiny ✓. And noteOpened persists. OK.

23. **T1-F2 ProGuard specifics**: keep rules — uniffi.mesh_core.** (the generated Kotlin bindings use JNA direct mapping? UniFFI uses JNA with... keep `com.sun.jna.**`, the UniFFI generated classes, and `kotlin.**`? Also `org.jetbrains.**`? Practical: keep uniffi.**, com.sun.jna.**, and the JNA Structure/Library interfaces; also keep org.bileichat.** if reflection... not needed. Also EncryptedSharedPreferences → Tink keep rules usually come with consumer ProGuard files; minify + Tink historically needs keep for com.google.crypto.tink.** — consumer rules ship with the artifact. Also must keep `androidx.security.**`? Ships consumer rules. The mandatory hand-written ones: uniffi + jna. Agree with their assessment; validation by installing.

    Also: debug-signed release — since signing cert is debug, android:debuggable=false, minifyEnabled=true, shrinkResources. versionName bump. Also they should decide testOnly / extractNativeLibs etc. Fine.

    One caution: `isMinifyEnabled` on release with UniFFI — the native .so loading via JNA: JNA needs `com.sun.jna.**` keep + the generated `uniffi.**` keep; ALSO R8 full mode (default in AGP 8) — may need `-keepclassmembers` for JNA Structure fields. Validation path noted. OK.

24. **F3 detail — privateRowTs single slot**: queued sends sequential; if user sends 2 messages quickly, only latest row gets tick. Acceptable; note.

25. **SAS rendering** — my main disagreement (20 bits too low). Recommend: pair_sas returns 4 bytes; render as 4 words from a 2048-word list (44 bits) or 8 digits (~26.6 bits) minimum; 6 digits is GPU-grindable in seconds within the sequential-pairing window. Since both phones are present and the MITM must grind between the two scans (typically 15–120s), cost model: 6 digits=1M ECDH ≈ seconds on laptop → broken; 8 digits=100M ≈ 10–60 min single-core, ~1 min on GPU — marginal; 4 words (44 bits) ≈ years. Words also read aloud more easily across two screens. I'll push 4 words or 9 digits.

    Hmm wait, actually let me reconsider the attack window: sequential pairing — Alice displays QR, Bob scans it; then Bob displays QR, Alice scans. MITM device(s) near both. The grind starts when MITM has victim1's pk (scan 1) and must complete before presenting to victim2 — realistically tens of seconds, but the MITM can also pre-seed: present to Alice FIRST with pkM1 (grind later for Bob's side once Bob's pk is seen? The SAS must match on BOTH screens: SAS_A = f(sA, pkA, pkM1) shown on Alice's screen; SAS_B = f(sB, pkM2, pkB) on Bob's. MITM needs them equal. MITM sees pkA when Alice shows QR to "Bob"(M1). Then MITM must present to Bob a pkM2 with matching SAS — grind 2^20 ECDH — seconds-to-minutes live. Yes, 20 bits is within the live window for a prepared adversary. 44 bits is not. Confirmed: push back on 6 digits.

26. Also — **T2 doc**: "Sharpen the witness() residual-gap doc to say the copier can be up to 8 hops away" ✓.

27. **Plan-mode answer**: agree overall; list corrections/additions:

    A. SAS entropy: 6 digits (20 bits) is live-grindable — bump to ≥40 bits (4 words) or ~9 digits.
    B. div_sketch[8..16] random nonce is a private-tier wire change (breaks 0.15↔0.16 private chat) — contradicts the "no wire-format change" invariant; either defer, dual-trial on receive (one-way break remains), or bundle into a future private-v2 bump together with AAD binding (L2). Same for AAD.
    C. C5 correction: epochMs mismatch is NOT silent — epoch indices diverge linearly (epoch = now/epochMs), so K4 trips within seconds and the banner fires; the detection machinery described (rxSkewed without banner) cannot exist since rxSkewed only counts K4 drops. Reduce C5 to: banner wording mentioning epochMs + settings-row warning.
    D. T3 K4 widening: apply ±4 to public tiers only; keep private at ±2 (ratchet window only retains prevKey; re-air budget is tuned to ±2). Else private frames pass K4 but are unopenable. Also need dedup cap review (4096 vs 6 epochs × 1024 bucket = 6144 worst case → raise global cap or accept FIFO pressure) and ownHashRetentionEpochs/messageRepeatEpochs interplay — BROADCAST re-originates only 3 epochs; with ±4 acceptance, senders could optionally repeat 4; not required.
    E. S8 dedup split API: decay needs local epoch — pass into FFI per call (checkAndInsertEpoch(hash, frameEpoch, localEpoch)) or advance(localEpoch) setter; ensure HOL purge predicate also expires future-stamped entries when local clock passes them.
    F. T1-F6 honest tick: handle legacy/GATT-only path — on legacy adapters startAdvertising early-returns with no callback, so the tick would never fire; fire ON_AIR on first successful GATT write/notify or keep immediate-tick for legacy mode only.
    G. T1-F3: echo branch must not require outgoingText non-empty for private rows, and skip the BROADCAST expiry rewrite (:992-999) for private; privateRowTs single-slot caveat for rapid successive sends.
    H. T1-F1: also route candidateKeys' fast-forward through ratchetTo (listed ✓) and confirm fastForwardChains stays on Dispatchers.IO with the commit() per contact — already IO ✓. Add: reject-anchor-too-old error string must be distinguishable for UI.
    I. S1a nuance: after S5 (salt per session), every legit re-pair changes the derived key → blocking dialog fires on all legit re-pairs too; that's correct (TOFU) but word the dialog as key-changed-confirm-in-person, not "attack detected".
    J. Explicitly-not-doing additions to state consciously: S11 Sybil corroboration cap; C6 relay blackout during private windows (hardware constraint; consider coalescing re-air windows); S2 full fix deferred (T2's floor does NOT stop full-sketch copies — inter≥2 satisfied trivially by a copied sketch; R1 stays log-only); AAD binding bundled with any future private wire bump; clipboard sensitivity flag (one-liner, may as well take it).
    K. T1-F2: ProGuard keep rules — uniffi.**, com.sun.jna.** (+Structure members), and verify EncryptedSharedPreferences/Tink consumer rules survive R8 full mode; validate by installing release APK and doing a private send + pairing + export log.
    L. Dedup decay-by-local-epoch: remember ownHashes retention (4 epochs) vs K4 ±4 — echo of a re-originated frame still fine; retention maybe bump to 5? ownHashRetentionEpochs = messageRepeatEpochs+1 = 4 — echoes of the last origination arrive within ~1 epoch; fine.
    M. Also under T2: the inter count in pocp must come from the same KMV window used for jaccard (inter_in_x), and tests need updating; check FFI verdict int mapping doesn't collide with POCP_NO_LOCAL_SKETCH=3 on the Kotlin side (new verdict e.g. 4 = Unattested).
    N. fastForwardChains failure path: with chunking, spans become computable but huge spans (e.g. 30 days = 259k steps ≈ 260k BLAKE3 ≈ <1s) — the "absurd-span refusal" threshold: what value? Suggest: refuse > ~7 days (60480 epochs ≈ 0.5-1s CPU) → actually that's computable in a second. Refuse > some days? The CPU bomb was 170M steps (~minutes). A 30-day span ≈ 259k ≈ ~0.5s — fine to compute once. So set MAX_RATCHET_SPAN ≈ 7-30 days of epochs; beyond → "re-pair required". Anchor clamp MAX_ANCHOR_BACKDATE = 1 day per their plan — but then legit 3-day-old QR is rejected... QRs are shown live, anchor is the generation epoch — a QR displayed for days on screen? Pairing is live face-to-face; 1 day is generous. Fine. But note asymmetry: anchor clamp (1d) vs ratchet refusal (7-30d) — the ratchet cap handles app-offline catch-up; anchor handles malicious QR. Consistent.

    O. One more: **the S6 reject-cache ordering** — compute frameHash before frameVerifySelf; frameHash decodes first (cheap) — keep frameDecodes first. Cache keyed on hash; 512-entry LRU; insert on bad-sig and past-stale only.

    P. Also T1-F7: add MeshState.receipt.value = null, outgoingAiring=false, privateRowTs/outgoingRowTs=null ✓ (they have the nulls). Add recentTexts/sketchSeen clears? Content-wise minor; recentTexts holds (text → epoch) of displayed messages — plaintext message texts in memory! recentTexts maps text strings — wait, recentTexts is keyed by text? Line 233: `recentTexts = HashMap<String, UInt>` — B6 key (text, mark prefix)? It says keyed by (text, sender-mark prefix)... whatever — it holds message text in memory; panic wipe should clear it. Actually looking at current code — recentTexts declared at :233 but is it even used after appendOrMergeIncoming replaced suppression? grep needed — it may be dead. If live, wipe it. Flag to check during implementation.

    Q. **Version bump + upgrade path**: 0.16 name (e.g., bileichat-0.16-audit-remediation.apk).

    R. Test plan additions: cargo test for pocp inter-floor + dedup local-decay; proptests updated; Android: manual 2-phone matrix (public LOCAL/BROADCAST receipt, private send + echo tick, 24h+ ratchet catch-up simulation by advancing wall clock? — can simulate by setting epochMs small? Ratchet catch-up test: temporarily set chainEpoch back via... testing hook. Or unit-test ratchetTo chunking in Kotlin? PairStore is Android — test via instrumentation or a quick Robolectric... simplest: test ratchetTo composition against Rust pairRatchet directly in mesh-core tests (Rust-side: verify (a→b)+(b→c) == (a→c) property — add a Rust test proving composition, then the Kotlin chunk helper is trivially correct.)

Also the user asked "do you agree?" — so answer: yes with N corrections. Keep it organized by tier. Be concise but complete. Also answer "what else do you need to change" — the list above.

Should I ask clarifying questions? Yes — the SAS entropy one needs their decision (digits vs words), the private-wire-change items need a decision (defer vs break vs dual-trial), K4 private-scope decision. Use the question tool? The instructions say ask when weighing tradeoffs. I think presenting the review with explicit decision points is right; then ask the 2-3 real questions via the question tool. Let me use the question tool for the key decisions:

1. SAS format/entropy (6 digits 20 bits vs 8-9 digits ~27-30 bits vs 4 words 44 bits)
2. div_sketch random nonce / AAD: defer both to a future private-v2 wire bump (recommended), ship with one-way compat break, or dual-trial receive
3. K4 widening scope: public-only ±4, private stays ±2 (recommended) vs uniform
4. S2: consciously accept copy-attack residual (R1 log-only) — confirm?

That's 4 questions; maybe fold 4 into the text. Keep questions to the top 3.

Before finalizing, let me double-check a few code claims I made:

- rxSkewed increments only inside K4 branch — MeshService.kt:952 `rxSkewed.incrementAndGet()` inside the `if (diff > 2 || diff < -2)` block ✓. And banner set at :958 same block ✓. So rxSkewed>0 ⟺ banner set — their C5 premise (non-zero rxSkewed without banner) is impossible. ✓ my correction stands.

- epoch divergence: epoch = now/epochMs. epochMs 10000 vs 20000 → E10 = 2·E20 → diff = E10-E20 = E20 = now/20000 grows ~1 per 20s. K4 trips after ~40-60s uptime mismatch. Loud ✓. So mismatch is NOT silent — the earlier audit's C5-as-silent concern was wrong too (I said "diff alternates 0/1" — that was wrong; that's only if epochs were derived from aligned phase... no, indices diverge. Good catch for the review.)

- Legacy tick: BleRadio.kt:166-168 — `if (advMode == "legacy-uuid(gatt)" && currentAdvSet != null) return` — no callback fires on this path ✓. First legacy start does get a callback (legacy cb). So ON_AIR could fire from legacy set start once; subsequent epochs no. With F6, on legacy hardware the tick fires once (first epoch) — subsequent frames same... actually the frame changes but legacy beacon carries no frame. Tick semantics on legacy = "GATT plane has the frame". Reasonable: fire ON_AIR in legacy mode immediately (as today) or on first GATT write. My F flag stands.

- Private echo: outgoingText empty during private sends ✓ (:981 textEcho requires outgoingText.isNotEmpty()).

- ownHashes eviction keyed on insertion epoch ✓.

- pair_ratchet composition: need to verify crypto.rs:135-150. The audit said "one-way, rejects backward jumps, span-bounded at 8192". Composition exactness: k_e = derive(k_{e-1} ‖ e) — sequence from from+1..=to. Chunked (a→b) then (b→c) iterates a+1..b then b+1..c = same sequence ✓. Their claim holds (assuming implementation iterates inclusively — verify during implementation; add a Rust proptest).

- dedup decay API: FfiDedup.checkAndInsertEpoch(hash, epoch) — new signature needs frame_epoch + local_epoch; UniFFI signature change — Kotlin call sites updated (:477, :1026 checkEpoch, :1138, :1244, :1406). Fine, no wire change.

- T2 inter source: pocp.rs jaccard computes inter_in_x within the KMV window — the verdict must use that same count; note the estimate caveat for large cells (inter_in_x ≈ K·j ≈ 4.8 at j=0.3,K=16 — well above 2, so honest large cells stay Valid ✓).

- POCP_NO_LOCAL_SKETCH = 3 (Kotlin) — new Rust verdict must not collide; Rust WitVerdict currently 0/1/2 presumably; add 3=Unattested in Rust, Kotlin POCP_NO_LOCAL_SKETCH stays a Kotlin-side sentinel 3... collision! Kotlin uses 3 for its own sentinel; if Rust returns 3 for Unattested, Kotlin's `POCP_NO_LOCAL_SKETCH` branch (:1188) would misinterpret. Rust side verify_witness_local returns enum → FFI int. Kotlin when(): 0→Valid, 1→(msgType2 display / LOCAL drop), POCP_NO_LOCAL_SKETCH(3)→defer, else→Stale. If Rust adds verdict 3=Unattested, Kotlin maps... the `else` branch catches 3 as Stale → WRONG. So the new Rust verdict must be a value Kotlin distinguishes — e.g. Rust returns 4=Unattested, or Kotlin sentinel moved to -1. Flag this implementation detail — actually important correctness note. (Currently Rust FFI pocpVerifyWitnessLocal returns 0/1/2; Kotlin sentinel 3 is Kotlin-only since verifyPocpAcrossRollover synthesizes it. So Rust can use 3? No — verifyPocpAcrossRollover's return space mixes both. Cleanest: Rust Unattested = 4.)

Wait, I should double check whether pocpVerifyWitnessLocal (FFI) can even return the intersection — verifyPocpAcrossRollover calls it per bucket and only keeps first Valid. With Unattested: collect "best" verdict across buckets: Valid(0) > Unattested(4) > CellMismatch(1) > Stale(2) > no-sketch(3). Implementation detail for the plan.

- MeshService.kt:923 bad-sig return before hash compute ✓ (hash at :931). Reorder noted.

- recentTexts usage — I flagged to check; from my read of MeshService, recentTexts is declared at :233 with comment "Repeated-text suppression (display-only)" but ingest uses appendOrMergeIncoming... I didn't see recentTexts used in the shown code (966-1515 didn't reference it; 1-965 either? I don't recall seeing it). Possibly dead. Implementation should grep and remove or wipe. ✓ flag.

- S1a: Contact doesn't store peer pkHex — comparison via derived pairKey/seed0 ✓ as discussed; legit re-pair triggers dialog (acceptable TOFU). ✓.

- T1-F2: also bump versionName ✓ they said. And add proguard-rules.pro ✓ they said.

- Anything about the APK naming: bileichat-0.16-remediation.apk.

- The user's plan mentions "Add a matching absurd-span refusal in ratchetTo that surfaces 're-pair required' rather than grinding" ✓ consistent with my N.

- Their T1-F1 says "keeping it succeeding is what holds candidateKeys' on-the-fly span at ≤2" ✓.

Also should double-check: does the ratchet anchor backdate (PAIR_EPOCH_BACKDATE=3) interact with the 1-day anchor clamp? chainEpoch = start-3; fine.

One more thing: **ownHashRetentionEpochs and K4 ±4**: retention = messageRepeatEpochs+1 = 4 < K4 window 4... an echo of our frame relayed 4 epochs later? We stop re-originating after 3; echo of the last hash arrives within ~1-2 epochs. Retention 4 fine.

**messageRepeatEpochs vs ±4**: receivers accept frames up to 4 epochs old; sender re-originates only 3 → 4th-epoch relay still carries original hash (dedup'd) — fine, no change needed.

Also **PRIVATE_REAIRINGS=2 with private ±2** — consistent with my D recommendation.

Also: with K4 widened to ±4 for public, the dedup decay window must be ≥ local-4 → entries live ~5-6 epochs; per-epoch bucket cap 1024 stays; global cap 4096 might evict early under crowd load — suggest raising global cap to 8192 (memory ~400KB, fine). Include in E.

Also skew banner wording: "CHECK BOTH CLOCKS... IF THEY ARE, THE SENDER IS TOO SLOW TO BUILD FRAMES" — add transit age: "or the alert has been relaying for a long time". ✓ their plan includes rewording.

OK. Also the user asked: "do you agree? tell me what else do you need to change". Deliver: structured review — agree overall; corrections C2 (theirs is right), C5 correction (theirs is impossible as specified — detection already loud), SAS entropy pushback, private-wire items flagged, K4 scope, plus the implementation-detail landmines (verdict value collision, legacy tick, private echo conditions, dedup cap, HOL purge predicate, recentTexts, reject-cache ordering). Then questions.

Format: organized by tier, tables sparingly. Keep tight. End with the question tool for the 3 decisions. Actually — should I use the question tool or just inline questions? The guidance says ask user opinion on tradeoffs — question tool is appropriate for the 3 real decisions. Let me write the review text first, then call question tool.

Length: substantial but focused. Let me draft.

Your plan is solid and I agree with ~90% of it — including both corrections (C2-as-replay-amplifier is right, and TTL is unauthenticated so hop-aware freshness is dead on arrival; S8-first ordering is right). I verified the load-bearing claims against the source. Below: what I agree with silently, then **7 things to change** — one of your "verified" items is actually wrong, one T4 parameter is too weak, one T-plan item violates your own "no wire change" invariant, and four are implementation landmines that will bite during execution.

---

## Correction to your verification table: C5 as specified cannot work

> "a sustained non-zero `rxSkewed` fraction that never trips the ±N banner is the signature of a mismatched epochMs"

That state is unreachable. `rxSkewed` increments **only inside** the K4 drop branch (`MeshService.kt:952`), and the banner is set in the same branch (`:958`) — `rxSkewed > 0` ⟺ banner shown. There is no sub-banner skew signal to detect.

More importantly, the premise that epochMs mismatch is *silent* is wrong (my original C5 phrasing misled you here — correcting my own finding). Epoch is `now / epochMs`, so the epoch **index** diverges linearly: 10s vs 20s phones differ by `now/20000` buckets — K4 trips within ~40–60s of uptime mismatch and the banner fires loudly. Harmonic ratios don't rescue it; the indices still diverge.

**C5 reduces to:** settings-row warning ("must match every phone") + add "or mismatched epoch setting" to the banner wording (the log line at `:946` already says it, the user-facing banner at `:958-962` doesn't). No detection machinery needed.

## Disagreement: 6-digit SAS (20 bits) is inside the live grind window

The MITM's cost model: they learn Alice's pk when she shows her QR, compute `shared_A = X25519(sk_M1, pk_A)` themselves, then grind `sk_M2` until `SAS(shared_B, pk_M2, pk_B)` collides with `SAS_A` — **before presenting to Bob**. That grind happens live, during the 15–120s sequential-pairing fumble, and both shareds are computable by the attacker per candidate. 2²⁰ ECDH+hash is seconds on one CPU core, <1s parallelized. Your threat model is state actors at protests — the one adversary class that has the laptop.

- **Recommend ≥40 bits: 4 words from a 2048-word list (44 bits)**, or 8 digits (~26.6 bits, still GPU-marginal). Words are easier to compare across two screens than digits. Same `pair_sas(shared, sort(pk_a, pk_b)) -> [u8;4]` construction, just a different renderer.
- 6 digits is the ZRTP-voice number; it assumes a human voice authenticates liveness. Two screens of hex don't.

## Invariant violation: `div_sketch[8..16]` random nonce is a private-tier wire change

You said "frames stay byte-identical, so a phone on this build and a phone on 0.15 still exchange traffic." True for everything **except** this one. The receiver reconstructs the AEAD nonce from `(epoch, pk, counter = div_sketch[4..8])` in `openPrivateBodyOnly`. If the sender mixes `div_sketch[8..16]` randomness into the nonce, **0.15 receivers cannot decrypt 0.16 private frames** — and there is no version byte or feature flag to branch on. Same problem for AAD binding (L2), which isn't in your lows but belongs to the same decision.

Options: **(a) defer both to a future private-v2 wire bump** (recommended — the Android shim's encrypted-prefs counter already closes the realistic reuse path; this is hardening for hypothetical future shims); (b) dual-trial on receive (new receivers try both derivations — still a one-way break: old receivers can't open new senders); (c) accept the break and say so in the changelog.

## K4 ±4 must be scoped to public tiers only

Widening K4 uniformly creates a silent private-tier hole: frames stamped 3–4 epochs back would pass K4, VDL-verify, relay fine — and be **unopenable**, because the ratchet retains only `prevKey` (one epoch) and `fastForwardChains` anchors the window to the ±2 gate (`PairStore.kt:420-423`). Receivers would show nothing, sender sees a relay echo (if T1-F3 lands), and the failure is invisible. Also `PRIVATE_REAIRINGS=2` is tuned to ±2.

- Keep `msgType==3` at ±2; widen types 1/2 to ±4.
- With ±4, dedup's decay window must extend to ~6 epochs (you noted this) — but also **raise the global cap**: 1024-bucket × 6 epochs = 6144 > current `FfiDedup(4096)`. 8192 entries ≈ ~400KB, fine on a phone.
- `ownHashRetentionEpochs` (4) still covers echoes; no change needed.

## Implementation landmines

1. **T2 verdict-value collision.** Kotlin's `POCP_NO_LOCAL_SKETCH = 3` sentinel is synthesized inside `verifyPocpAcrossRollover`, and its `when` treats any unmatched Rust verdict as Stale (`MeshService.kt:1198-1204`). If Rust adds `Unattested = 3`, it will be misread as **Stale → dropped**, reintroducing C8 exactly where you fixed it. Rust must return `Unattested = 4` (or move the Kotlin sentinel), and `verifyPocpAcrossRollover` needs a best-of-buckets ordering: Valid > Unattested > CellMismatch > Stale > no-sketch. Also: take the intersection count from the **same KMV window** jaccard uses (`inter_in_x`, `pocp.rs:93-95`), and note in-code that for large cells it's an estimate (~K·τ ≈ 4.8 at τ=0.3 — honest large cells stay Valid, good).
2. **T1-F6 breaks the tick on legacy adapters.** `startAdvertising` early-returns on the `legacy-uuid(gatt)` path (`BleRadio.kt:168`) — **no callback ever fires**, so moving ON_AIR into `onAdvertisingSetStarted` leaves legacy phones' rows at SENDING forever. Fire the tick immediately in legacy mode (frames move over GATT, "on air" means "handed to GATT plane"), callback-driven only on the ext-adv path.
3. **T1-F3 conditions.** The echo branch requires `MeshState.outgoingText.value.isNotEmpty()` (`:981`) — always false for private sends — and the BROADCAST expiry rewrite (`:992-999`) must not run for private. The `OwnFrame.private` flag needs to route around both, not just pick a different row-ts. Caveat to accept: `privateRowTs` is single-slot, so two rapid private sends → only the latest row ticks.
4. **S6 reject-cache ordering.** Today: `frameDecodes` → `frameVerifySelf` → `frameHash` (`:919-931`). The cache must be checked **before** verify, so hash moves up (BLAKE3 over 226B is µs — fine). Insert on bad-sig and **past-stale only**; split the K4 branch so future-stale isn't cached (correct in your plan — future frames legitimately become valid). Residual: byte-mutating floods still cost one verify per distinct packet; that's airtime-bound and inherent.
5. **`recentTexts`** (`MeshService.kt:233`) — appears dead since `appendOrMergeIncoming` replaced the suppression path. Grep during T1-F7: if live it holds plaintext message texts and belongs in the wipe list; if dead, delete it.

## Conscious sign-offs needed (not in your "not doing" list)

- **T2 does not stop S2.** A copied *full* sketch trivially satisfies `inter ≥ 2` → Valid. The floor kills the grind (S3); the copy attack (S2) remains fully open at full trust, R1 log-only. That's the documented M6+ deferral — fine, but state it in "explicitly not doing," not implied.
- **C6** (private windows black out OTA relay on 1-set controllers, ~18s/send): no fix in any tier. Hardware constraint; consider coalescing re-air windows or accept and document.
- **S11** (one Sybil → 32 corroboration claims): unaddressed. Acceptable as hint-only, but say so explicitly.
- **S1a nuance:** after S5 (salt-per-session), every *legitimate* re-pair also changes the derived key, so the blocking dialog fires for honest users too. That's correct TOFU — but the copy must read "key changed — confirm in person," never "attack detected."

## Agreed as-specified

T1-F1 chunked ratchet (composition claim is exact *if* the Rust loop iterates `(from+1)..=to` — add a Rust proptest `(a→b)+(b→c) == (a→c)` to pin it before building the Kotlin helper on it; anchor clamp + absurd-span refusal together defuse the CPU-bomb interaction correctly), T1-F2 (keep `uniffi.**`, `com.sun.jna.**` incl. Structure members; verify Tink consumer rules survive R8 full mode; validate by installing and doing a private send, not by building), T1-F4/F5/F7, T2 overall, T3 S6/S7/S8-split (pass `local_epoch` into the FFI per call; make the purge predicate also expire future-stamped entries once the local clock passes them, else head-of-line blocking survives), C10, T4 S1a/S5, all four "not doing" items, and the lows except the nonce/AAD item above.

Three decisions I need from you:

**Tool: question**

**Input:**
```json
{
  "questions": [
    {
      "question": "SAS entropy/format for pairing authentication? The MITM grind happens live during the sequential QR exchange, and 20 bits is within that window for a prepared adversary.",
      "header": "SAS format",
      "options": [
        {
          "label": "4 words, 44 bits (Recommended)",
          "description": "pair_sas -> [u8;4] rendered as 4 words from a 2048-word list. Grind cost ~years; words compare easily across two screens."
        },
        {
          "label": "8 digits, ~27 bits",
          "description": "Single-core grind ~1 hour, GPU ~1 minute. Marginal against the state-actor threat model but keeps the numeric-SAS UX."
        },
        {
          "label": "6 digits, 20 bits (your spec)",
          "description": "Live-grindable in seconds-to-minutes during the pairing window. Only stops opportunistic MITM."
        }
      ]
    },
    {
      "question": "div_sketch[8..16] random nonce + AAD binding break 0.15↔0.16 private decryption (receivers reconstruct the nonce from frame fields; no version byte exists). How to handle?",
      "header": "Private wire change",
      "options": [
        {
          "label": "Defer both to private-v2 (Recommended)",
          "description": "Android's encrypted-prefs counter already closes the realistic nonce-reuse path; bundle nonce randomization + AAD into a future deliberate wire bump. Keeps 'no wire change' true for this remediation."
        },
        {
          "label": "Dual-trial receive",
          "description": "New receivers try both nonce derivations; old receivers still can't open new senders. One-way break, extra AEAD open per key."
        },
        {
          "label": "Accept the break",
          "description": "Ship it, document that 0.15 and 0.16 can't exchange private messages. Public tiers unaffected."
        }
      ]
    },
    {
      "question": "K4 freshness widening scope?",
      "header": "K4 scope",
      "options": [
        {
          "label": "Public ±4, private ±2 (Recommended)",
          "description": "Private ratchet retains only prevKey and re-air budget is tuned to ±2; a uniform ±4 makes stale private frames pass K4 but fail to open, silently."
        },
        {
          "label": "Uniform ±4 + ratchet work",
          "description": "Also retain more prevKeys / extend fastForward anchor window and bump PRIVATE_REAIRINGS. More moving parts, same effective gain."
        },
        {
          "label": "Keep ±2 everywhere",
          "description": "Drop the C2 widening; just do the S8 dedup split + banner rewording and document the ~30s effective diameter."
        }
      ]
    }
  ]
}
```

**Output:**
```
User has answered your questions: "SAS entropy/format for pairing authentication? The MITM grind happens live during the sequential QR exchange, and 20 bits is within that window for a prepared adversary."="4 words, 44 bits (Recommended)", "div_sketch[8..16] random nonce + AAD binding break 0.15↔0.16 private decryption (receivers reconstruct the nonce from frame fields; no version byte exists). How to handle?"="Defer both to private-v2 (Recommended)", "K4 freshness widening scope?"="Public ±4, private ±2 (Recommended)". You can now continue with the user's answers in mind.
```

---

## Assistant (Plan · Kimi K3 (2x usage) · 82.0s)

_Thinking:_

The user has answered all three questions with the recommended options:
1. SAS: 4 words, 44 bits
2. Private wire change: defer both to private-v2
3. K4 scope: public ±4, private ±2

Now I should present the final consolidated plan incorporating all corrections and decisions. This is plan mode — the deliverable is a well-formed plan. Let me write the final, tight, execution-ready plan that merges their T1-T4 structure with my 7 corrections and the three decisions.

Structure:
- Decision record (the 3 answers + corrections agreed)
- Final tier plan with concrete file/line touch points
- Test/validation plan
- Explicit not-doing list (updated)

Keep it comprehensive but concise. This is the plan to execute once they exit plan mode. I shouldn't make edits. I could end by asking if they want me to proceed with implementation (exit plan mode).

All three decisions locked in. Here is the final consolidated plan, merging your T1–T4 with the corrections and the three rulings.

---

# Final Remediation Plan — bileichat 0.16

**Invariant:** no wire-format change. Public and private frames stay byte-identical; 0.16 ↔ 0.15 interoperates fully. Two receiver-side policy changes (PoCP verdicts, dedup decay), one pairing-flow addition (SAS).

## T1 — Silent killers

**F1. Chunked ratchet + anchor clamp (C1).** `PairStore.kt`:
- `private fun ratchetTo(key, from, to): ByteArray?` — loops `pairRatchet` in ≤8000-step chunks. Composition exactness must be pinned **first** by a Rust proptest in `mesh-core`: `(a→b)+(b→c) == (a→c)` (verifies the `(from+1)..=to` iteration assumption the helper is built on).
- Route all four call sites (`fastForwardChains`, `keyForSend`, `candidateKeys`, `noteOpened`) through it.
- Clamp `offer.anchorEpoch` at `PairStore.kt:299`: reject the pairing if older than `currentEpoch() - MAX_ANCHOR_BACKDATE` (1 day of epochs), with a **distinct error string** the UI can surface. Add `MAX_RATCHET_SPAN` (~7 days of epochs) refusal in `ratchetTo` returning "re-pair required" rather than grinding.
- Code comment: backward clock jump stalls the chain until clock catches up — self-healing, not fixed.
- `fastForwardChains` stays on `Dispatchers.IO` (per-contact `commit()`).

**F2. Release build (S4).** `build.gradle.kts` + new `proguard-rules.pro`: release type, `isDebuggable=false`, minify + shrinkResources, debug-signed (as chosen), versionName bump. Keep rules: `uniffi.**`, `com.sun.jna.**` including Structure member fields (R8 full mode), verify Tink/`androidx.security` consumer rules survive. **Validation = install the release APK and complete a private send + pairing + log export** — FFI reflection failures are runtime-only.

**F3. Private echo tick (C3).** `MeshService.kt`: register the sealed private frame with `rememberOwnFrame` at `:477`; add `private: Boolean` to `OwnFrame`. In the echo branch (`:976-999`): for private rows, **skip** the `outgoingText.isNotEmpty()` condition and the BROADCAST expiry rewrite; route the tick to `MeshState.privateRowTs`. No receipt text change for private (relay ≠ decryption; `SendState` doc already covers it). Accepted caveat: `privateRowTs` is single-slot — rapid successive sends tick only the latest row.

**F4. Label leak (S13).** Drop `c.label` at `PairStore.kt:432` and `MeshService.kt:1118`; grep all `logDebug`/`Log.` for label interpolation.

**F5. Live restore frame (C7).** `BleRadio.kt:590-621`: `restoreFrame: ByteArray` → `restoreFrame: () -> ByteArray?`; the restore Runnable invokes it at fire time; both call sites pass `{ currentPublicFrame }`. Null (post-wipe) → skip re-airing.

**F6. Honest ON_AIR tick.** Move tick into `onAdvertisingSetStarted` on `ADVERTISE_SUCCESS` for the ext-adv path. **Legacy exception:** on the `legacy-uuid(gatt)` path the callback never fires after the first start — fire the tick immediately there (meaning: handed to GATT plane), or rows stick at SENDING forever.

**F7. Panic-wipe gaps (S12).** `performPanicWipe`: zero+clear `pendingPrivate`; clear `ownHashes`/`ownHashesAcked`; drain `MeshState.privateSends`; zero each `relayQueue` frame before clear; null `outgoingRowTs`/`privateRowTs`; clear `receipt`/`outgoingAiring`. Grep `recentTexts` (`:233`) — dead since `appendOrMergeIncoming`: delete it; if somehow live, it holds plaintext and joins the wipe list.

## T2 — PoCP attest/display split (C8+S3)

- `pocp.rs`: `verify_witness_local` gains intersection count from the **same KMV window** jaccard uses (`inter_in_x`), and a fourth verdict. Verdicts: `inter≥2 && j≥τ` → Valid; `inter==1` → Unattested; `inter==0` → CellMismatch; MAC failure → Stale. Comment that the count is a KMV estimate for large cells (~K·τ ≈ 4.8 at τ=0.3, honest cells stay Valid).
- **Verdict value = 4 for Unattested** — 3 collides with Kotlin's `POCP_NO_LOCAL_SKETCH` sentinel and would be misread as Stale (drop), reintroducing C8. `verifyPocpAcrossRollover` gets best-of-buckets ordering: Valid > Unattested > CellMismatch > Stale > no-sketch.
- Kotlin: Unattested → `displayOk = true` with `lowConfidenceCell` forced true (reuses existing badge + `MIN_TRUSTWORTHY_CELL` path).
- Update Rust unit tests + pocp proptests; pin the grind table (256-frame 1-element sweep → zero Valid at every cell size) and the honest-cold-start case (1-element claim in 4-device cell → Unattested, displayed).
- Sharpen `witness()` residual-gap doc: copier can be up to 8 hops away, no proximity needed.

## T3 — Reliability & hardening

**C2+S8 (S8 first).** Dedup split: bucket by frame epoch (sub-cap), **decay by local epoch** — `local_epoch` passed into the FFI per call (`checkEpoch`/`insertEpoch`/`checkAndInsertEpoch` signatures + Kotlin call sites). Purge predicate must also expire future-stamped entries once local clock passes them (kills the head-of-line freeze). Then widen K4 to **±4 for msgType 1/2 only; msgType 3 stays ±2** (ratchet window + `PRIVATE_REAIRINGS=2` are tuned to ±2). Extend dedup decay to ~6 epochs and **raise `FfiDedup` cap 4096 → 8192** (1024-bucket × 6 epochs = 6144 worst case). Reword skew banner: clock skew **or long relay transit**; document ~40s effective diameter. Do not touch TTL-based freshness (unauthenticated — replay amplifier).

**C10.** Clear `clockSkewWarning` only after 3 consecutive rollovers with `rxSkewed == 0`.

**C5 (reduced).** Detection machinery dropped — impossible as specified (`rxSkewed>0` ⟺ banner; epoch indices diverge linearly so mismatch trips K4 loudly within ~40–60s). Do only: settings-row warning "must match every phone" + banner wording gains "or mismatched epoch setting".

**C4.** `GattPlane.kt`: give poll-reads a guaranteed slot between chunk writes on MTU<229 links (interleave at the `writeNextChunkLocked` level) so relay bursts no longer deafen the only receive path.

**S6.** Reject-cache: reorder to `frameDecodes` → `frameHash` → reject-cache check → `frameVerifySelf`. ~512-entry LRU on the 16-byte hash; insert on bad-sig and **past-stale only** (split the K4 branch; future-stale legitimately becomes valid).

**S7.** `GattPlane.kt`: keep subscribed-central idle exemption, add hard tenure cap with rotation; add idle timeout for client-side connections.

**S10.** Bare-64-hex/v1 pairing requires explicit confirmation; contact badged `LEGACY — NO FORWARD SECRECY`.

## T4 — Pairing authentication (S1) & salt lifetime (S5)

**S1a.** `addContact` refuses when label exists with a different derived key; returns a distinct result → blocking UI confirmation. Copy: "key changed — confirm in person," never "attack detected" (fires on legit re-pairs too, since S5 rotates salts per session — correct TOFU).

**S1b. SAS — decided: 4 words, 44 bits.** `pair_sas(shared, pk_a, pk_b) -> [u8; 4]` in crypto.rs: domain-separated `derive_key`, pubkeys sorted, rendered as 4 words from a 2048-word list (wordlist embedded both sides). Displayed post-ECDH on both phones; contact persists **only after** match confirmation (pending-contact state in the dialog flow). Rust KAT tests for sort-independence and domain separation.

**S5.** `beginPairingSession()`/`endPairingSession()` driven by pairing-dialog lifecycle + inactivity timeout; `mySalt` zeroed on close. Remove the copyable `SALT …` `SelectionContainer` text. Fix FS wording (holds after session end, not process death).

**Lows (same pass):** `zeroize` on Rust key/plaintext types (M5); `min_hearers` floor ≥1 in core (F8-core); `tau` + `observe()` length validation at FFI; `make_test_frame` gated out of production FFI; `beaconFloorMs ≥ epochMs` config warning (C11); drop live neighbor count from lock-screen notification; clipboard sensitivity flag on sketch copy (one-liner).
**Removed from lows:** div_sketch[8..16] random nonce and AAD binding — **deferred to private-v2 wire bump** (decided; they break 0.15↔0.16 private decryption).

## Explicitly not doing (final)

- Hop-aware freshness — TTL unauthenticated, replay amplifier.
- Fuzzy-extractor witness keying — **S2 (full-sketch copy, 8-hop remote) remains open at full trust**; T2's floor kills only the grind (S3). M6+, documented.
- Sequential VDF (S9) — 22-bit PoW stops phones, interface allows swap.
- C9 legacy acquisition latency — GATT plane redesign.
- C6 relay blackout during private windows — single-set hardware constraint; documented.
- S11 Sybil corroboration (32 claims) — hint-only by design; documented.

## Validation plan

1. `cargo test` + proptests (ratchet composition, pocp inter-floor table, dedup local-decay incl. future-entry expiry); `cargo fuzz` decode target still clean.
2. Two-phone matrix on release APKs: LOCAL receipt, BROADCAST receipt, private send + echo tick, panic wipe, pairing + SAS match/mismatch, key-change re-pair dialog, legacy-mode phone (tick fires, GATT exchange).
3. Ratchet catch-up: advance wall clock 24h+ (or set chainEpoch back via a debug hook) → private messaging self-heals; anchorEpoch=0 QR → clean rejection, no CPU grind.
4. K4: relay chain of 3+ phones, confirm outer hops land inside ±4; stale-banner wording check.

Estimated touch: ~10 Kotlin files, 4 Rust files, 2 gradle files, plus tests. Suggested order: T1-F1 → T1-F2 → T2 → T3-S8/C2 → rest of T3 → T4 → lows, with a named APK per checkpoint (`0.16-ratchet`, `0.16-release`, …) matching your existing cadence.

Want me to proceed with implementation in that order, or adjust the sequencing first?

---

## User

i implemented a lot of fixes. Full audit remediation implemented. bileichat-0.16-audit-release.apk (5.3 MB, minified, not debuggable) and -debug.apk. 150 Rust tests, zero failures.

The one that mattered most

C1 is a permanent silent brick, and its fix has two halves that cannot be separated. fastForwardChains made one pairRatchet call; the core refuses spans over 8192, so a pair that went 22.8 h without a private message froze chainEpoch forever — every later epoch widened the span. Its own doc comment computed the 22.8 h figure and fixed only the app-running case.

Chunking alone would have converted that brick into a CPU bomb: a hostile QR naming anchorEpoch=0 becomes ~170 M BLAKE3 steps. So the anchor clamp ships with it. Already-bricked contacts self-heal on the next epoch.

Composition exactness is pinned by proptest before the Kotlin was written — (a→b)+(b→c) == (a→c) across arbitrary splits, plus chunk-size invariance.

What I changed from the plan, and why

C8+S3 verdict is 4, not 3. Your catch was right and load-bearing. POCP_NO_LOCAL_SKETCH = 3 (MeshService.kt:109) with an else -> Stale arm would have dropped every demoted frame — reintroducing C8 precisely where T2 fixes it.

The sweep test didn't exist to extend; the forgery table was an ad-hoc measurement with no regression guard. Wrote one. It asserts zero Valid at cell sizes 2–8 and that the hits still land as Unattested — otherwise it would pass vacuously if the sweep stopped matching at all.

SAS wordlist is generated, not English. 16 onsets × 8 vowels × 16 codas = exactly 2^11, so no index is wasted or biased. The only dictionary available is Webster's 2nd — a sample gave "besnow", "abeigh", "wodgy" — with no frequency data to filter by, and a 2048-entry blob in a security-critical confirmation screen is unreviewable. 44 bits is unchanged.

S6's safety rests on one fact I checked first: the signature at bytes 150..214 is inside the hashed region (blake3(buf[..214])). Corrupting it changes the hash, so the reject cache can't be used to blacklist a legitimate frame. That's recorded in the code — if the hashed region ever stops covering the signature, the cache becomes a censorship primitive.

C5 reduced to copy changes. Your correction was right; I'd proposed a detector on an unreachable signal. Saved as a memory, since it's the third time in this project I've reasoned past a verification step.

A bug I introduced and caught before shipping

My first S5 implementation started the pairing session in DisposableEffect. Compose runs remember blocks during composition but defers effect bodies until it completes — so the session would have zeroed the salt after the QR was encoded from it. Both phones derive different chain seeds, no private message ever opens, silently and permanently. Exactly the C1-class failure. Session start now sits in a remember above the QR, with the ordering documented as load-bearing.

Untested on hardware

Everything below compiles, passes tests, and survives R8 (verified RustBuffer, FieldOrder, capacity, and the pair_sas_words native symbol in the release dex). None of it has run on a phone. The release APK specifically needs installing and exercising — UniFFI/JNA break at runtime only, so the green build proves nothing there.

Two checks need a rig I don't have: setting chainEpoch back >8192 to confirm C1 recovery, and scanning an anchorEpoch=0 QR to confirm clean rejection rather than grinding.

Left open, deliberately

T2 does not close S2. A div_sketch copied wholesale off a relayed frame satisfies the overlap floor and lands at full trust, from up to 8 hops away. The floor killed the single-byte grind only. Also open: the two-byte grind against cells ≲6 — the floor can't go past 2 without making two-device meshes unattestable. Plus C6, C9, S9, S11, and the private-v2 items per your ruling.  but the problem now is: private messages are being relayed. but not being displayed. i see two check marks. i think i knnow the problem lil bit now. phones are set to not send the same message when they hear the same message right? so when i send something from one phone to another using broadcast, it sends the message. other phone recieves it but hasnt realyed yet. so my phone thinks no recipts so i send again, the other phone hears the same message again and thinks its a storm lol.. also it still says resending even when heard back once, that stop prompt  doesnt go away i am testing this in local chat. how does local chat work? cant the police just flood the local chat cause no witnesses is allowed?

---

## Assistant (Build · Kimi K3 (2x usage) · 310.3s)

_Thinking:_

Let me parse the user's bug report carefully. They implemented the 0.16 audit remediation. Now testing on hardware, they report:

1. **Private messages are being relayed but not displayed.** They see two check marks (ECHOED — a peer relayed it, which per their new F3 works for private now).
2. Their theory: "phones are set to not send the same message when they hear the same message right?" — they're conflating some things. Let me think through the actual mechanism.
3. "It still says resending even when heard back once, that stop prompt doesn't go away" — testing in LOCAL chat.
4. Question: "how does local chat work? can't the police just flood the local chat cause no witnesses is allowed?"

Wait — re-reading. There are actually multiple intertwined symptoms:

**Symptom A: Private message relayed (two ticks = echo heard) but not displayed on the other phone.**

Private display path: frame arrives → frameDecodes → frameVerifySelf → K4 (private ±2 now) → dedup FRESH → msgType 3 → vdlCheckFrame → trial-decrypt against all contacts' candidateKeys → if opens, display.

The echo (two ticks) means: some peer (or the recipient) relayed the frame back and the sender heard it. Note: the echo comes from the RELAY path, which for private requires only sig + VDL — NOT decryption. So two ticks ≠ the recipient decrypted it. The recipient relays REGARDLESS of whether it could decrypt (`MeshService.kt`: "Relay regardless of whether we could decrypt").

So why wouldn't it display? Possible causes:

1. **Ratchet/key mismatch**: candidateKeys returns keys that don't match the sender's key. The sender used keyForSend(sealEpoch) — which advances the chain to sealEpoch. Receiver's chain: fastForwardChains advances to epoch-1. candidateKeys for frameEpoch: if frameEpoch == chainEpoch → pairKey; frameEpoch > chainEpoch → fast-forward on the fly; frameEpoch == prevEpoch → prevKey. Should work if both paired with same anchor...

   BUT WAIT — the v3 anchor: `start = minOf(offer.anchorEpoch, currentEpoch())`. Both sides scan each other. Hmm, but there's an asymmetry: Alice scans Bob's QR (anchor = Bob's QR-gen epoch), Bob scans Alice's QR (anchor = Alice's QR-gen epoch). start_A = min(BobAnchor, now_A), start_B = min(AliceAnchor, now_B). If both QRs were generated in the same session and scans happen close, min picks the older of the two anchors on both sides... Let me think: Alice's side holds BobAnchor and now_A. Bob's side holds AliceAnchor and now_B. min(BobAnchor, now_A) vs min(AliceAnchor, now_B). Suppose AliceAnchor=100 (Alice generated QR at 100), BobAnchor=102, now_A=105 (when Alice scans Bob), now_B=106. Alice's start = min(102, 105) = 102. Bob's start = min(100, 106) = 100. **MISMATCH!** 102 ≠ 100!

   Hold on — is that right? The comment in PairStore says "each side holds both epochs and takes min() — same value on both phones". But that's WRONG per my reading! Each side holds the OTHER's anchor epoch and its own current time — NOT both anchors. Alice (scanning Bob's QR) knows BobAnchor=102 and now_A=105 → start=102. Bob (scanning Alice's QR) knows AliceAnchor=100 and now_B=106 → start=100. Different!

   Wait wait. Let me re-read: "v3 puts the generating epoch in the QR, so each side holds both epochs and takes min()". Hmm, "both epochs" = the scanned anchor and the local clock. For min to agree, both sides need THE SAME two values. Alice computes min(BobAnchor, now_A_at_scan); Bob computes min(AliceAnchor, now_B_at_scan). For these to agree... if AliceAnchor < now_B and BobAnchor > ... no.

   Actually suppose both phones show their QRs simultaneously at epochs a (Alice's QR anchor) and b (Bob's QR anchor). Sequential scanning: say Bob scans Alice first at time t1, then Alice scans Bob at t2.
   - Bob's start = min(a, t1_epoch_B)
   - Alice's start = min(b, t2_epoch_A)
   With a ≈ b (both generated around the same time) and t1, t2 later than a, b: Bob's start = a, Alice's start = b. If a ≠ b (QRs generated in different epochs), the anchors differ → chains never align → **private messages never open, permanently**. That matches "private messages relayed but not displayed"!

   Hmm but wait — this was supposedly the v3 FIX. Let me re-derive what the correct fix should be. The chain anchor must be the SAME on both sides. Available common knowledge: both anchor epochs (a from Alice's QR, b from Bob's QR) — but each phone only sees the OTHER's QR. So Alice's phone knows b (scanned) and her own a (she generated it!). Oh wait — Alice's phone DOES know her own QR anchor! qrPayload uses currentEpoch() at generation. So Alice's phone could use min(a, b) if it remembered a.

   Does addContact use the local phone's own QR anchor? Looking at the code: `start = offer.anchorEpoch?.let { minOf(it, currentEpoch()) }`. It uses the SCANNED anchor and CURRENT TIME — not the phone's own QR generation epoch. currentEpoch() at scan time ≠ the phone's own QR anchor unless they scan in the same epoch their QR was generated.

   Hmm, so is the v3 fix correct? min(scanned_anchor, my_now). For agreement we need both sides to compute the same value. Suppose Alice's QR anchor a=100, Bob's b=102. Bob scans Alice at 103: Bob's start = min(100, 103) = 100. Alice scans Bob at 104: Alice's start = min(102, 104) = 102. **100 ≠ 102 → permanent mismatch.** The fix is WRONG unless each side uses min(own_anchor, scanned_anchor): Bob: min(a_own=102, a_scanned=100) = 100. Alice: min(a_own=100, a_scanned=102) = 100. ✓ THAT would agree.

   So the correct v3 logic should be: min(my own QR's anchor epoch, scanned anchor epoch). The code uses min(scanned anchor, NOW) which only works if each phone scans the other in the same epoch its own QR was generated — i.e., QR generated and scanned in the same epoch (10s window). In practice: user opens pairing dialog (QR generated at epoch X), peer scans within a few seconds (same or +1 epoch). If both manage it within the same epoch: a=b → agree. If the dialog stays open >10s before the second scan, anchors differ by the drift → brick.

   Hmm, but the user reported private messaging WORKED before? Did they? In 0.15 testing they tested echo/receipt with public. The earlier audit noted the anchor bug as F12 (ratchet-span DoS) but the v3 mechanism... Actually wait, let me not assume — maybe both phones in the current test paired >22.8h ago?? No wait, C1 fix self-heals. Hmm.

   Actually — hold on. The user says "private messages are being relayed. but not being displayed. i see two check marks." And they implemented T1-F3 (private echo tick). So the two check marks come from the new private echo — meaning a peer relayed the private frame. Not displayed on the recipient = trial decryption failed on the recipient.

   Candidate causes for trial-decrypt failure:
   a) Ratchet anchor mismatch (the v3 min() bug above — if the fix has the flaw I derived).
   b) The ±2 private K4 gate with VDL solve drift — but they have re-stamping.
   c) The sender used a key ahead: keyForSend advances chain to sealEpoch; receiver's fastForwardChains at epoch-1; candidateKeys frameEpoch == chainEpoch or fast-forward. If sender's chain started at a DIFFERENT anchor, keys never match regardless of epoch.
   d) The new SAS flow: contact persisted only after SAS confirm — maybe persisted wrong.
   e) 0.16 sender → 0.16 receiver but the nonce/AAD... no, they deferred those.
   f) The private frame is being received but dropped at an earlier gate: K4 private ±2 (did they scope it?), dedup, VDL. The echo proves the RECIPIENT (or another peer) relayed it — relay requires sig+VDL only, and relay happens regardless of decryption. But the recipient would only even GET to the private branch if K4 passed (private ±2 per our plan). If they widened K4 uniformly to ±4, private frames 3-4 epochs old pass K4 but can't be opened — but here messages are fresh, so ±2/±4 doesn't matter for immediate delivery.
   
   Most likely: key mismatch from anchor mismatch. The debug log would say "private frame not for us (or key mismatch): tried N key(s) across M contact(s)". That's verifiable.

   Actually, let me look at their description again: "i think i knnow the problem lil bit now. phones are set to not send the same message when they hear the same message right? so when i send something from one phone to another using broadcast, it sends the message. other phone recieves it but hasnt realyed yet. so my phone thinks no recipts so i send again, the other phone hears the same message again and thinks its a storm lol"

   Hmm, they're now describing BROADCAST/public behavior, not private. Their theory: sender re-sends (user manually re-sends?) because no receipt; receiver hears duplicate and "thinks it's a storm". What's "storm"? B6 was repeated-text suppression... replaced by appendOrMergeIncoming which merges. There's no storm detection on receive... unless they mean the relay queue eviction log "relay queue full — dropping relay task"? Or C8 bucket-full "possible flood"? Hmm.

   Actually their model is wrong in an instructive way: when the user manually re-sends the same TEXT, the app originates a NEW frame (new epoch, new mark, new signature) — not a duplicate. The receiver merges same-text rows within 4 epochs into one row with repeats++. So no harm. The dedup only suppresses byte-identical frames.

   **Symptom B: "it still says resending even when heard back once, that stop prompt doesn't go away i am testing this in local chat."**

   LOCAL chat: after echo heard, LOCAL switches to sparse re-airing (every 4th epoch) for up to 30 min — BY DESIGN (B1/B2: a forged echo must not silence a danger alert). `outgoingAiring` stays true → UI shows "still sending/resending" with a stop affordance. So "heard back once" receipt + continued airing for 30 min is the designed behavior for LOCAL. The receipt text is set ("✓ heard back once..."), but outgoingAiring stays true and the message keeps re-airing sparsely until the 30-min cap or manual stop. That's why "the stop prompt doesn't go away". 

   Is that a bug or intended? Intended per B1/B2 — but UX-wise confusing for a chat message ("hi" re-airs for 30 min). The comment at MeshService.kt:354-356 acknowledges: "which is right for a danger alert and alarming for 'hi' — so the user has to be able to see it and stop it." So the stop prompt staying is the affordance. But the user expected it to stop after receipt. Explanation needed; maybe suggest UX copy improvement ("heard back — still re-airing sparsely for 30 min, tap to stop").

   How does LOCAL chat work (their question)? msgType=1, TTL=1, relayed exactly once (TTL clobbered to 0) → 2 RF hops max. Display requires PoCP witness Valid against the receiver's cell sketch (or Unattested badge after T2). Witnessless LOCAL = dropped entirely (not even relayed? no wait — witnessless is relayOnly for broadcast; for LOCAL, !hasWitness → relayOnly=true → relayed but not displayed... wait let me re-check: `if (!hasWitness) { relayOnly = true }` regardless of type — so witnessless LOCAL is relayed but not displayed. Hmm, and relayed LOCAL gets TTL clobbered 0.)

   "can't the police just flood the local chat cause no witnesses is allowed?" — their question conflates: witnesses ARE required for display. A LOCAL frame without a witness is relay-only (never displayed). A LOCAL frame WITH a forged witness: the PoCP MAC key is public, so the attacker needs a plausible div_sketch: post-T2, Valid requires inter≥2 with the receiver's cell. The attacker within RF range hears the cell's marks and can build a real sketch → Valid. So a *nearby* attacker (police in the crowd) can absolutely flood local chat with validly-witnessed fake alerts — PoCP doesn't stop a co-present attacker, it only stops *remote* injection. That's by design ("a determined nearby attacker can forge claims" — README). The defenses against flooding: (1) dedup per-epoch bucket cap (1024) — but an attacker can fill it (F5 blackout); (2) airtime: 1 frame/epoch/phone origination is an app convention, not enforced — an attacker can advertise arbitrary frames continuously; receivers pay verify cost per frame (S6 reject-cache now covers replays); (3) merge-by-text display collapses repeats; (4) low-confidence badge for small cells. But yes — a co-present adversary can inject fake LOCAL alerts; the protocol treats co-presence as the trust boundary and explicitly does not claim to stop in-crowd forgery. The corroboration hint + user judgment is the mitigation.

   Now back to **Symptom A (private not displaying)** — the most important one. Their echo shows the frame reached the peer and was relayed. Display requires successful trial decryption. Let me think about what changed in 0.16 that could break private display:

   1. **S1a key-continuity guard**: addContact refuses re-pair under existing label with different key... if they re-paired during testing with the SAME label, the contact now holds the NEW key while the other phone may hold... no wait, both re-paired together would match.

   2. **S5 salt-per-session**: salt zeroed on dialog close. If the pairing session ended BEFORE the peer scanned (session timeout too short?), the QR shown after session end... the QR encodes myPairSalt() which regenerates a NEW salt if mySalt==null — fine, as long as addContact uses the SAME salt that's in the displayed QR. The bug they caught (DisposableEffect) was exactly this class. Maybe there's a residual ordering bug: session ends → mySalt zeroed → addContact later calls myPairSalt(ctx) → generates NEW salt ≠ the salt the peer scanned → seed mismatch → **private messages never open, in one direction or both**. This is VERY plausible as a shipped bug! If Alice's session timed out between QR display and addContact completion, Alice derives seed with salt_A2 while Bob derives with salt_A1 (scanned). Mismatch → Bob→Alice messages fail to open (and Alice→Bob too since seed0 must be identical).

      But wait — pair_seed_v2(shared, my_salt, their_salt) sorts the salts, so seed0 = f(ECDH, {saltA, saltB}). If Alice uses salt_A2 ≠ salt_A1 (what Bob scanned), the SET differs → seed differs → permanent mismatch. Both directions fail. Exactly the reported symptom!

   3. **Anchor min() flaw** I derived above (uses now instead of own QR anchor) — pre-existing from 0.15, would have broken private all along. Did private work in their 0.15 testing? They tested "privatefix" 0.9... The 0.15 session history shows private messaging worked at some point ("bileichat-0.9-privatefix.apk"). So maybe the anchor logic worked in practice (scans within same epoch) and something in 0.16 broke it — pointing back to S5 salt lifecycle or S1a or SAS-flow changes.

   4. **candidateKeys/fastForward anchor re-alignment**: fastForwardChains advances to epoch-1. If K4 private stayed ±2, receiver chain window: chainEpoch = ownEpoch-1, prev = ownEpoch-2. Frame at ownEpoch (fresh): frameEpoch > chainEpoch → fast-forward 1 step → key ✓. Frame at ownEpoch-1: == chainEpoch ✓. Frame at ownEpoch-2: == prevEpoch ✓. Frame at ownEpoch+1 (sender clock fast): frameEpoch > chainEpoch → fast-forward 2 ✓. OK.

   5. **The sender side**: keyForSend(sealEpoch) advances chain to sealEpoch = own epoch. ✓.

   6. **Did they change `makePrivateFrame`/nonce/counter?** They deferred nonce/AAD changes. nextPrivateCounter unchanged presumably.

   7. **SAS confirm gating persist**: maybe contacts saved AFTER first send attempt... unlikely.

   Given uncertainty, the right move: reproduce with logs. The debug log distinguishes the gates: "private frame failed VDL", "not for us (or key mismatch): tried X keys across Y contacts", "epoch skew". The user should check the recipient's debug log. Also possible: the private frame arrives at the recipient, opens fine, but display is suppressed... no, private appends directly via appendMessage.

   Hmm wait — actually one more: **the recipient's own re-ingest**: the recipient relays the frame; the echo returns to sender; sender's dedup already has it. Fine.

   And another candidate: **K4 private ±2 vs sender re-airing**: PRIVATE_REAIRINGS=2 — fresh messages should pass.

   But actually — hold on, let me reconsider the user's own theory once more: "other phone recieves it but hasnt realyed yet. so my phone thinks no recipts so i send again, the other phone hears the same message again and thinks its a storm". They might be observing: broadcast message → recipient displays it fine → sender re-sends same text → recipient MERGES (repeats=2) — and maybe their build has a "flood/storm" UI badge? Or maybe they mean the recipient's phone doesn't RELAY the second copy instantly... 

   Actually you know what, I think their report mixes three different observations and the core complaint is: **private not displayed + receipt UX confusion on LOCAL**. Their "storm" theory is a misunderstanding I should correct: re-sending the same text creates a NEW frame (new epoch/mark/sig) — it's not deduped as duplicate, not treated as flood; the receiver merges it into one row with a repeat count. Nothing punishes repeats.

   But hmm — is there any path where the receiver "thinks it's a storm"? C8 bucket-full logs "possible flood" only at 1024 distinct frames/epoch. Not from 2 messages. The relay queue full log — no. B6 was removed. So no storm detection. Good — correct them clearly.

   Now: what do I DO? Build mode is on. The user asks: (1) help diagnose private-not-displayed, (2) correct their theory, (3) explain LOCAL + police flood question.

   For (1), I need to look at the CURRENT code — they implemented a lot of changes (S5 session lifecycle, S1a guard, SAS flow, T2 verdict mapping, F3 private echo). I should inspect the new PairStore.kt / pairing UI / MeshService.kt ingest to find the actual break. Since they changed code, I should re-read the relevant sections. The most likely culprits in order:
   - S5 salt session lifecycle bug (salt regenerated between QR display and addContact)
   - SAS flow gating persistence incorrectly (contact saved but with wrong anchor?)
   - The v3 anchor min() flaw (pre-existing)
   - candidateKeys/fastForward regression from chunked ratchet refactor (e.g., ratchetTo bug, or fastForward target changed, or storeUpdated not persisting)
   
   Let me actually read the current code. Priority: PairStore.kt (addContact, salt session, ratchetTo, candidateKeys, keyForSend), and the pairing dialog in MeshUi.kt, and the private branch of MeshService.kt.

   Also check: does the private frame REACH the trial-decrypt? The recipient's log would show. But I can also spot code bugs directly.

   Plan:
   1. Re-read PairStore.kt fully (it changed a lot).
   2. Read the pairing dialog changes in MeshUi.kt (SAS confirm flow, session lifecycle).
   3. Re-read MeshService.kt private branch + echo branch (F3 changes).
   4. Check git diff to see exactly what changed.
   
   Then diagnose, answer the LOCAL/flood questions, and fix the bug(s) found.

Let me start with git diff stat to see scope of changes.

Let me look at what actually changed in 0.16 — the symptom (relayed + two ticks but not displayed) means the frame passes sig/VDL/dedup on the recipient but trial decryption fails. Your re-send theory I can already correct: re-sending the same text originates a **brand-new frame** (new epoch, new mark, new signature) — it's not a dedup duplicate and nothing treats it as a storm; the receiver just merges it into the same row with a repeat count. Let me verify the real private path.

**Tool: bash**

**Input:**
```json
{
  "command": "git status --short && git log --oneline -5 && git diff --stat HEAD | tail -20"
}
```

**Output:**
```
 M android/app/build.gradle.kts
 M android/app/src/main/java/org/bileichat/mesh/BleRadio.kt
 M android/app/src/main/java/org/bileichat/mesh/Config.kt
 M android/app/src/main/java/org/bileichat/mesh/GattPlane.kt
 M android/app/src/main/java/org/bileichat/mesh/MainActivity.kt
 M android/app/src/main/java/org/bileichat/mesh/MeshService.kt
 M android/app/src/main/java/org/bileichat/mesh/MeshState.kt
 M android/app/src/main/java/org/bileichat/mesh/PairStore.kt
 M android/app/src/main/java/org/bileichat/mesh/ui/Drawer.kt
 M android/app/src/main/java/org/bileichat/mesh/ui/MeshUi.kt
 M android/app/src/main/java/org/bileichat/mesh/ui/Theme.kt
 M android/app/src/main/java/uniffi/mesh_core/mesh_core.kt
 M android/app/src/main/jniLibs/arm64-v8a/libmesh_core.so
 M android/app/src/main/jniLibs/armeabi-v7a/libmesh_core.so
 M android/app/src/main/jniLibs/x86/libmesh_core.so
 M android/app/src/main/jniLibs/x86_64/libmesh_core.so
 M mesh-core/Cargo.lock
 M mesh-core/Cargo.toml
 M mesh-core/src/crypto.rs
 M mesh-core/src/ffi.rs
 M mesh-core/src/pocp.rs
 M mesh-core/src/statemachine.rs
?? android/app/proguard-rules.pro
?? cockroachat/
?? mesh-core/tests/crypto_props.rs
?? mesh-core/tests/pocp_attestation.rs
fa25f61 chore: rename branding from cockroachat to BileiChat
5f6f883 feat: resolve security audit issues (A1-A3, B1-B10, C1-C9, D1-D6)
0f2bd30 feat: restore v0.5-unified Jetpack Compose UI (industrial AMOLED aesthetic) and update README screenshots
dd8eb1b docs: add UI screenshots for live (chat) and rig (field tool) flavors
faeb04f docs: update README with UI overhaul — dual build flavors (rig/live), XML Views, tier-colored chat bubbles
 .../src/main/java/org/bileichat/mesh/GattPlane.kt  |   98 +-
 .../main/java/org/bileichat/mesh/MainActivity.kt   |   46 +-
 .../main/java/org/bileichat/mesh/MeshService.kt    |  764 +++++++++--
 .../src/main/java/org/bileichat/mesh/MeshState.kt  |  110 +-
 .../src/main/java/org/bileichat/mesh/PairStore.kt  |  277 +++-
 .../src/main/java/org/bileichat/mesh/ui/Drawer.kt  |    8 +
 .../src/main/java/org/bileichat/mesh/ui/MeshUi.kt  | 1364 +++++++++++++-------
 .../src/main/java/org/bileichat/mesh/ui/Theme.kt   |   69 +-
 .../src/main/java/uniffi/mesh_core/mesh_core.kt    |  167 ++-
 .../app/src/main/jniLibs/arm64-v8a/libmesh_core.so |  Bin 973272 -> 990288 bytes
 .../src/main/jniLibs/armeabi-v7a/libmesh_core.so   |  Bin 765760 -> 780328 bytes
 android/app/src/main/jniLibs/x86/libmesh_core.so   |  Bin 1166684 -> 1185044 bytes
 .../app/src/main/jniLibs/x86_64/libmesh_core.so    |  Bin 1070656 -> 1086120 bytes
 mesh-core/Cargo.lock                               |    1 +
 mesh-core/Cargo.toml                               |    1 +
 mesh-core/src/crypto.rs                            |  134 +-
 mesh-core/src/ffi.rs                               |   65 +-
 mesh-core/src/pocp.rs                              |  109 +-
 mesh-core/src/statemachine.rs                      |  142 +-
 22 files changed, 2717 insertions(+), 763 deletions(-)

```

---

## Assistant (Build · Kimi K3 (2x usage) · 11.2s)

_Thinking:_

Big diff. Let me focus on the private-message path: PairStore.kt diff (addContact, salt session, ratchetTo, candidateKeys, keyForSend), and MeshService.kt private branch. Let me read the current PairStore.kt first.

**Tool: bash**

**Input:**
```json
{
  "command": "git diff android/app/src/main/java/org/bileichat/mesh/PairStore.kt"
}
```

**Output:**
```
diff --git a/android/app/src/main/java/org/bileichat/mesh/PairStore.kt b/android/app/src/main/java/org/bileichat/mesh/PairStore.kt
index 8f0ee8e..0cea1eb 100644
--- a/android/app/src/main/java/org/bileichat/mesh/PairStore.kt
+++ b/android/app/src/main/java/org/bileichat/mesh/PairStore.kt
@@ -34,7 +34,14 @@ data class Contact(
 }
 
 /** A parsed out-of-band pairing offer. [saltHex] non-null ⇒ v2 (forward-secret ratchet). */
-data class PairingOffer(val pkHex: String, val saltHex: String?)
+/**
+ * A scanned pairing payload.
+ *
+ * [anchorEpoch] is the epoch at which the OTHER phone generated its QR. It exists because the
+ * ratchet chain start must be AGREED, not observed locally — see PairStore.addContact.
+ * Null for legacy v1/v2 payloads, which carry no epoch.
+ */
+data class PairingOffer(val pkHex: String, val saltHex: String?, val anchorEpoch: UInt? = null)
 
 object PairStore {
     private const val PREFS_NAME = "mesh_pairing_v2"
@@ -120,6 +127,54 @@ object PairStore {
      *  other seconds apart still have overlapping key windows. See addContact. */
     private const val PAIR_EPOCH_BACKDATE = 3u
 
+    /** Steps per `pair_ratchet` call. Must stay at or under the core's 8192 cap
+     *  (mesh-core/src/crypto.rs:139). */
+    private const val RATCHET_CHUNK = 8000u
+
+    /** Epochs in [ms], for the config-derived span bounds below. */
+    private fun epochsIn(ms: Long): UInt =
+        (ms / MeshState.config.epochMs).coerceAtLeast(1L).toUInt()
+
+    /** Furthest back a scanned QR may anchor a chain. A hostile payload can name any epoch,
+     *  and the anchor is where catch-up starts counting from. */
+    private fun maxAnchorBackdate(): UInt = epochsIn(24L * 60 * 60 * 1000)
+
+    /** Longest catch-up we will compute. A week offline is ~60k BLAKE3 steps at a 10 s
+     *  epoch — milliseconds. Past this the chain is treated as unrecoverable. */
+    private fun maxRatchetSpan(): UInt = epochsIn(7L * 24 * 60 * 60 * 1000)
+
+    /**
+     * Advance a chain key across an arbitrary span.
+     *
+     * `pair_ratchet` refuses spans over 8192 (crypto.rs:139 — a DoS bound on wire-supplied
+     * epochs), and every chain-advancing path used to call it directly with an unbounded
+     * span. A pair that went 8192 epochs without exchanging a private message — 22.8 h at a
+     * 10 s epoch, i.e. pairing one evening and first using it the next day — blew the cap in
+     * both directions at once. Neither [keyForSend] nor [candidateKeys] can advance a chain
+     * without first succeeding, and [fastForwardChains] gave up on the same call, so nothing
+     * ever recovered: private messaging was permanently dead until the pair met in person
+     * again, with no UI signal at all.
+     *
+     * Walking the span in chunks is exact, not an approximation. The ratchet is a left fold
+     * over the epoch index sequence, so splitting it anywhere reproduces the same sequence —
+     * pinned by `ratchet_composes_at_any_split` in mesh-core/tests/crypto_props.rs.
+     *
+     * Refuses past [maxRatchetSpan] rather than grinding: chunking alone would turn a
+     * hostile QR naming anchor epoch 0 into ~170 M BLAKE3 steps on the pairing path.
+     */
+    private fun ratchetTo(key: ByteArray, from: UInt, to: UInt): ByteArray? {
+        if (to < from) return null
+        if (to - from > maxRatchetSpan()) return null
+        var k = key
+        var e = from
+        while (e < to) {
+            val next = minOf(to, e + RATCHET_CHUNK)
+            k = uniffi.mesh_core.pairRatchet(k, e, next) ?: return null
+            e = next
+        }
+        return k
+    }
+
     private fun myPairSalt(ctx: Context): ByteArray {
         mySalt?.let { return it }
         val s = ByteArray(PAIR_SALT_LEN)
@@ -128,6 +183,31 @@ object PairStore {
         return s
     }
 
+    /**
+     * S5: begin a pairing session. Call when the pairing screen opens.
+     *
+     * The salt is the entropy that makes a v2 chain seed unrecomputable from a seized
+     * long-term secret — that is the entire forward-secrecy claim. It was generated lazily
+     * and then never rotated or cleared, so it lived for the whole PROCESS: a phone left
+     * running all day held, in memory, the material to reconstruct the chain seed of every
+     * contact paired that day, and the pairing screen displayed it as selectable text.
+     * "Forward secret after process death" is a much weaker claim than the one being made.
+     *
+     * A session covers one face-to-face pairing sitting, which is the only window where the
+     * salt genuinely has to stay stable (both people scan the same displayed QR).
+     */
+    @Synchronized
+    fun beginPairingSession() {
+        endPairingSession()
+    }
+
+    /** S5: end a pairing session and zero the salt. Call when the pairing screen closes. */
+    @Synchronized
+    fun endPairingSession() {
+        mySalt?.fill(0)
+        mySalt = null
+    }
+
     private fun currentEpoch(): UInt =
         (System.currentTimeMillis() / MeshState.config.epochMs).toUInt()
 
@@ -135,8 +215,13 @@ object PairStore {
      * v2 QR payload: public key + per-pairing salt. Both public; the secret and the chain
      * keys never leave the device. The salt is fresh per pairing and rotated after each add.
      */
+    /**
+     * v3 adds the generating epoch. Both phones scan each other, so both end up holding both
+     * epochs and can pick the SAME chain anchor with min(); without it each side anchored on
+     * its own wall clock and the two ratchets never produced a common key (see addContact).
+     */
     fun qrPayload(ctx: Context): String =
-        "bileichat:key:v2:${myPublicHex(ctx)}:${myPairSalt(ctx).toHex()}"
+        "bileichat:key:v3:${myPublicHex(ctx)}:${myPairSalt(ctx).toHex()}:${currentEpoch()}"
 
     fun mySaltHex(ctx: Context): String = myPairSalt(ctx).toHex()
 
@@ -145,6 +230,17 @@ object PairStore {
         val trimmed = value.trim()
         val parts = trimmed.split(":")
         return when {
+            parts.size == 6 && parts[0].equals("bileichat", true) &&
+                parts[1].equals("key", true) && parts[2] == "v3" -> {
+                val pk = parts[3]
+                val salt = parts[4]
+                val epoch = parts[5].toUIntOrNull()
+                if (pk.length == 64 && pk.hexToBytesOrNull()?.size == 32 &&
+                    salt.length == PAIR_SALT_LEN * 2 &&
+                    salt.hexToBytesOrNull()?.size == PAIR_SALT_LEN &&
+                    epoch != null
+                ) PairingOffer(pk, salt, epoch) else null
+            }
             parts.size == 5 && parts[0].equals("bileichat", true) &&
                 parts[1].equals("key", true) && parts[2] == "v2" -> {
                 val pk = parts[3]
@@ -237,31 +333,116 @@ object PairStore {
             }
     }
 
+    /**
+     * A pairing that has completed key agreement but has NOT been stored.
+     *
+     * Exists so the user can be shown something to check before anything is committed. Holds
+     * key material, so it must not outlive the dialog — [PairStore.discardPending] drops it.
+     */
+    class PendingPairing internal constructor(
+        internal val contact: Contact,
+        /** SAS words both phones must display identically (S1b). */
+        val sasWords: List<String>,
+        /** True when this offer downgrades to the static v1 key with no forward secrecy. */
+        val legacy: Boolean,
+        /** True when a contact under this label already exists with a DIFFERENT key. */
+        val replacesExistingKey: Boolean
+    )
+
+    /** Outcome of [preparePairing]. */
+    sealed interface PairPrepare {
+        data class Error(val message: String) : PairPrepare
+        data class Confirm(val pending: PendingPairing) : PairPrepare
+    }
+
+    /**
+     * Derive a pairing and return it for confirmation. Stores NOTHING.
+     *
+     * Split from the commit because scanning a QR code authenticates nothing on its own: it
+     * proves only that a code was scanned. A relay that shows each side its own code ends up
+     * holding both halves and reading everything, and the old flow saved the contact
+     * immediately with no opportunity to notice. It also silently REPLACED an existing
+     * contact's key under the same label, so one re-scan of an attacker's code was enough.
+     */
+    @Synchronized
+    fun preparePairing(ctx: Context, label: String, offerRaw: String): PairPrepare {
+        val prepared = buildContact(ctx, label, offerRaw)
+        if (prepared is PairPrepare.Error) return prepared
+        return prepared
+    }
+
+    /** Store a pairing the user has confirmed. Returns an error string, or null on success. */
+    @Synchronized
+    fun commitPairing(ctx: Context, pending: PendingPairing): String? {
+        val contact = pending.contact
+        if (prefs(ctx) == null) {
+            memContacts.removeAll { it.label == contact.label }
+            memContacts.add(contact)
+        } else {
+            val updated = contacts(ctx).filter { it.label != contact.label } + contact
+            persist(ctx, updated)
+        }
+        contactCache = null
+        return null
+    }
+
     @Synchronized
-    fun addContact(ctx: Context, label: String, offerRaw: String): String? {
+    private fun buildContact(ctx: Context, label: String, offerRaw: String): PairPrepare {
         val trimmedLabel = label.trim()
-        if (trimmedLabel.isEmpty()) return "Contact name cannot be empty"
-        if (trimmedLabel.any { it == '\t' || it == '\n' || it == '\r' }) return "Contact name contains invalid characters"
-        if (trimmedLabel.length > 32) return "Contact name too long (max 32 chars)"
-        val offer = parsePairingOffer(offerRaw) ?: return "Invalid pairing key format"
-        val peerPub = offer.pkHex.hexToBytesOrNull() ?: return "Invalid public key"
-        if (peerPub.size != 32) return "Invalid public key size"
+        if (trimmedLabel.isEmpty()) return PairPrepare.Error("Contact name cannot be empty")
+        if (trimmedLabel.any { it == '\t' || it == '\n' || it == '\r' }) return PairPrepare.Error("Contact name contains invalid characters")
+        if (trimmedLabel.length > 32) return PairPrepare.Error("Contact name too long (max 32 chars)")
+        val offer = parsePairingOffer(offerRaw) ?: return PairPrepare.Error("Invalid pairing key format")
+        val peerPub = offer.pkHex.hexToBytesOrNull() ?: return PairPrepare.Error("Invalid public key")
+        if (peerPub.size != 32) return PairPrepare.Error("Invalid public key size")
         // D5: pairing with ourselves is never valid.
-        if (offer.pkHex.equals(myPublicHex(ctx), ignoreCase = true)) return "Pairing with your own key is not allowed"
+        if (offer.pkHex.equals(myPublicHex(ctx), ignoreCase = true)) return PairPrepare.Error("Pairing with your own key is not allowed")
 
-        val shared = uniffi.mesh_core.pairDerive(secret(ctx), peerPub) ?: return "Key agreement failed"
+        val shared = uniffi.mesh_core.pairDerive(secret(ctx), peerPub) ?: return PairPrepare.Error("Key agreement failed")
         val contact = if (offer.saltHex != null) {
             // v2: chain seed = f(ECDH, both salts). Salts are NOT stored — after this call
             // only the ratchet chain state survives, which is what gives seizure resistance.
-            val theirSalt = offer.saltHex.hexToBytesOrNull() ?: return "Invalid salt"
-            val seed0 = uniffi.mesh_core.pairSeedV2(shared, myPairSalt(ctx), theirSalt) ?: return "Derivation failed"
-            // Backdate the chain start. The two sides scan each other seconds apart, so each
-            // stamped its OWN local epoch: if Alice landed on epoch 100 and Bob on 102, a
-            // message Alice sent at 101 fell into candidateKeys' `else -> emptyList()` branch
-            // on Bob (101 != 102, no prevKey, 101 < 102) and was undecryptable forever, with
-            // no log line. Starting the chain a few epochs back makes the two windows overlap;
-            // ratcheting forward from there is one BLAKE3 step per epoch.
-            val start = currentEpoch()
+            val theirSalt = offer.saltHex.hexToBytesOrNull() ?: return PairPrepare.Error("Invalid salt")
+            val seed0 = uniffi.mesh_core.pairSeedV2(shared, myPairSalt(ctx), theirSalt) ?: return PairPrepare.Error("Derivation failed")
+            // The chain anchor must be AGREED, not observed locally.
+            //
+            // pair_ratchet mixes the epoch INDEX into every step
+            // (k_e = derive_key(k_{e-1} || e)), so the key at epoch E is a function of the
+            // whole index sequence from the anchor onward — i.e. of where the chain started.
+            // Both sides begin from the same seed0 (pair_seed_v2 is order-independent), but
+            // each used its OWN wall clock at scan time as the anchor. QR pairing is
+            // inherently sequential, so those clocks land in different 10 s epochs almost
+            // every time: Alice anchored at 100, Bob at 102, and from then on their chains
+            // produced different keys at EVERY epoch. Not a skew window — a permanent
+            // mismatch. Every v2 private message failed to open, forever, silently.
+            //
+            // Backdating both by a constant did not help: it shifted both anchors equally and
+            // left the difference intact. That was a misdiagnosis of this same bug.
+            //
+            // v3 puts the generating epoch in the QR, so each side holds both epochs and takes
+            // min() — same value on both phones, no clock agreement needed. The backdate is
+            // kept only to widen the retained prevKey window.
+            // The anchor comes off a scanned payload, so it is attacker-chosen. It is also
+            // where chain catch-up starts counting from — and catch-up now walks arbitrarily
+            // long spans in chunks rather than refusing them. Unclamped, a code naming epoch 0
+            // would turn every later fast-forward into ~170 M BLAKE3 steps. Reject stale
+            // anchors here so the chunking fix cannot be used as a CPU bomb.
+            val nowEpoch = currentEpoch()
+            offer.anchorEpoch?.let { anchor ->
+                val backdate = maxAnchorBackdate()
+                val oldest = if (nowEpoch >= backdate) nowEpoch - backdate else 0u
+                if (anchor < oldest) {
+                    return PairPrepare.Error("Pairing code is more than a day old — ask them to show a fresh one")
+                }
+            }
+            val start = offer.anchorEpoch?.let { minOf(it, nowEpoch) } ?: run {
+                MeshState.logDebug(
+                    "pairing with a pre-v3 code: no shared chain anchor, so private messages " +
+                        "will only open if both phones scanned within the same epoch — " +
+                        "re-pair once both are updated"
+                )
+                nowEpoch
+            }
             Contact(
                 trimmedLabel,
                 seed0,
@@ -273,20 +454,31 @@ object PairStore {
             Contact(trimmedLabel, shared, v2 = false)
         }
 
-        if (prefs(ctx) == null) {
-            memContacts.removeAll { it.label == trimmedLabel }
-            memContacts.add(contact)
-        } else {
-            val updated = contacts(ctx).filter { it.label != trimmedLabel } + contact
-            persist(ctx, updated)
-        }
-        contactCache = null
-        // NOTE: mySalt deliberately does NOT rotate here. The salt in the displayed QR
-        // must equal the salt used for every pairing made while that QR is shown — rotating
-        // on add would break sequential face-to-face pairing (the second scanner would get
-        // a different salt than the one they scanned). The salt is per-process only and
-        // never persisted, which is what preserves forward secrecy after process death.
-        return null
+        // S1b: the string both users compare. Bound to the ECDH output AND both identities,
+        // so a relay holding a separate secret with each side cannot make the two agree.
+        val sas = uniffi.mesh_core.pairSasWords(shared, myPublicHex(ctx).hexToBytesOrNull()!!, peerPub)
+            ?: return PairPrepare.Error("Could not compute the verification words")
+
+        // S1a: key continuity. Replacing an existing label's key used to happen silently, so
+        // one scan of a substituted code redirected every future message to someone else with
+        // no trace. Surfaced, never auto-refused — after per-session salts a legitimate
+        // re-pair also changes the key, so this fires for honest users too and the UI wording
+        // must not accuse anyone.
+        val existing = contacts(ctx).firstOrNull { it.label == trimmedLabel }
+        val replaces = existing != null && !existing.pairKey.contentEquals(contact.pairKey)
+
+        // NOTE: mySalt deliberately does NOT rotate here. The salt in the displayed QR must
+        // equal the salt used for every pairing made while that QR is shown — rotating on add
+        // would break sequential face-to-face pairing. It is dropped when the pairing SESSION
+        // ends (see endPairingSession), which is what bounds its lifetime.
+        return PairPrepare.Confirm(
+            PendingPairing(
+                contact = contact,
+                sasWords = sas,
+                legacy = !contact.v2,
+                replacesExistingKey = replaces
+            )
+        )
     }
 
     @Synchronized
@@ -320,7 +512,7 @@ object PairStore {
         val contact = contacts(ctx).firstOrNull { it.label == label } ?: return null
         if (!contact.v2) return contact.pairKey
         if (epoch <= contact.chainEpoch) return contact.pairKey
-        val advanced = uniffi.mesh_core.pairRatchet(contact.pairKey, contact.chainEpoch, epoch)
+        val advanced = ratchetTo(contact.pairKey, contact.chainEpoch, epoch)
             ?: return null
         val updated = contact.copy(
             pairKey = advanced,
@@ -342,7 +534,9 @@ object PairStore {
             contact.prevKey != null && frameEpoch == contact.prevEpoch -> listOf(contact.prevKey)
             frameEpoch > contact.chainEpoch -> {
                 // Sender is ahead of our stored chain — fast-forward (one-way, cheap).
-                uniffi.mesh_core.pairRatchet(contact.pairKey, contact.chainEpoch, frameEpoch)
+                // Normally one or two steps: [fastForwardChains] keeps chainEpoch pinned to
+                // epoch-1 every epoch, so this stays short even after a long offline gap.
+                ratchetTo(contact.pairKey, contact.chainEpoch, frameEpoch)
                     ?.let { listOf(it) } ?: emptyList()
             }
             else -> {
@@ -385,11 +579,14 @@ object PairStore {
         val target = epoch - 1u
         for (c in contacts(ctx)) {
             if (!c.v2 || target <= c.chainEpoch) continue
-            val advanced = uniffi.mesh_core.pairRatchet(c.pairKey, c.chainEpoch, target)
+            val advanced = ratchetTo(c.pairKey, c.chainEpoch, target)
             if (advanced == null) {
+                // Contact label deliberately omitted: the debug log is exportable, and a line
+                // naming who you are paired with is social-graph metadata a seized or shared
+                // export would hand over for free (same reason as [candidateKeys]).
                 MeshState.logDebug(
-                    "chain fast-forward for '" + c.label + "' failed: span " +
-                        (target - c.chainEpoch) + " epochs exceeds the ratchet cap"
+                    "chain fast-forward failed: span " + (target - c.chainEpoch) +
+                        " epochs exceeds the recoverable window — that pairing must be redone"
                 )
                 continue
             }
@@ -408,7 +605,7 @@ object PairStore {
     fun noteOpened(ctx: Context, label: String, frameEpoch: UInt) {
         val contact = contacts(ctx).firstOrNull { it.label == label } ?: return
         if (!contact.v2 || frameEpoch <= contact.chainEpoch) return
-        val advanced = uniffi.mesh_core.pairRatchet(contact.pairKey, contact.chainEpoch, frameEpoch)
+        val advanced = ratchetTo(contact.pairKey, contact.chainEpoch, frameEpoch)
             ?: return
         storeUpdated(ctx, contact.copy(
             pairKey = advanced,

```

---

## Assistant (Build · Kimi K3 (2x usage) · 79.7s)

_Thinking:_

I see the PairStore changes. Key observations:

1. **The v3 anchor bug I hypothesized is REAL and still present**: `val start = offer.anchorEpoch?.let { minOf(it, nowEpoch) }`. 

Let me re-derive: Alice's QR carries anchor a (generated when Alice's phone called qrPayload at some epoch a). Bob's QR carries anchor b.

- Alice scans Bob's QR at time t_A (her now). Alice's start = min(b, t_A).
- Bob scans Alice's QR at time t_B (his now). Bob's start = min(a, t_B).

For agreement we need min(b, t_A) == min(a, t_B).

Case: both QRs generated at roughly the same sitting, both scanned shortly after.
- Suppose a=100 (Alice's phone epoch when her QR was generated), b=102.
- Bob scans Alice at t_B=103: start_B = min(100, 103) = 100.
- Alice scans Bob at t_A=105: start_A = min(102, 105) = 102.
- **100 ≠ 102 → chains anchored differently → keys NEVER match → private messages never open, in both directions, permanently.**

Wait, but the comment says "both end up holding both epochs and can pick the SAME chain anchor with min()". The intent was clearly min(a, b) — the minimum of the TWO QR anchors. But the code computes min(scanned_anchor, local_now). local_now is NOT the phone's own QR anchor!

The phone's own QR anchor = the epoch when qrPayload was called (when the pairing dialog opened or when the QR was last rendered). If the dialog was opened at epoch a and the peer scans + the local user scans at t > a, then:
- The correct shared anchor would be min(a, b) — both phones know their own a and the scanned b.
- The code computes min(b, t_A) on Alice — which equals b if b ≤ t_A. And min(a, t_B) = a on Bob. So Alice anchors at b, Bob anchors at a. If a ≠ b → brick.

When does it accidentally work? When a == b (both QRs generated in the same 10s epoch) — plausible if both users open the dialog simultaneously, OR when one side's QR anchor ≥ the scan time on the other side... e.g., Alice opens dialog at 100, Bob opens at 100 (same epoch). Both anchors 100. Works. If Bob opens his dialog 30s after Alice (b=103, a=100): Alice scans Bob's QR at, say, 104: start_A = min(103, 104) = 103. Bob scans Alice's at 105: start_B = min(100, 105) = 100. MISMATCH. 

So the correct implementation: `start = min(myQrAnchorEpoch, offer.anchorEpoch)`. The phone must remember the anchor it put in its own QR. Currently qrPayload calls currentEpoch() fresh each time — the anchor isn't even stored! Each qrPayload call could produce a different epoch (every 10s the displayed QR changes if re-composed... actually qrPayload is called on composition/recomposition — the QR's epoch field CHANGES every epoch if recomposed!). Hmm, that adds more nondeterminism: the QR the peer scanned has whatever epoch was current at the last recomposition before scanning.

Hold on — but did private messaging ever work for the user? They said 0.9 was "privatefix". In older versions, the anchor was currentEpoch() at scan time on each side (v1/v2, no anchor in QR) — works if both scan within the same 10s epoch (face-to-face, likely) — mismatched by ±1 epoch otherwise → the old comment described exactly this failure. The v3 "fix" with min() as implemented only works when both QRs are generated in the same epoch. Sequential pairing (Alice shows QR, Bob scans; Bob shows QR, Alice scans) — the second QR shown was generated when Bob opened his dialog, which could be many epochs after Alice's. So v3 as implemented is STILL broken for the common sequential flow!

Wait, wait. Let me recheck: does the phone know its own QR anchor? `qrPayload` generates it fresh. If the UI recomposes every epoch (e.g., observing MeshState.stats?), the QR payload changes each epoch... and myPairSalt is stable within the session (good). The anchor in MY QR = whatever epoch was current at last recomposition. To implement min(ownAnchor, scannedAnchor), the anchor must be captured when the QR is generated and held stable for the session. Best: generate the anchor once per pairing session (like the salt): `sessionAnchorEpoch`. Store alongside mySalt. qrPayload uses the stored session anchor. buildContact: start = min(sessionAnchorEpoch, scannedAnchor).

Actually hmm, but is min() the right combiner even then? min(a, b) — both sides compute min over the same set {a, b} → agree. ✓. The backdate by PAIR_EPOCH_BACKDATE then widens prevKey window. So the fix: capture session anchor at session start (or first qrPayload call in the session), keep it stable, use min(sessionAnchor, offer.anchorEpoch).

Hmm wait, but there's a subtlety: if Alice's dialog stays open for 5 minutes (30 epochs), her anchor a is 30 epochs stale. start = min(a,b) = a. chainEpoch = a-3. Then ratchetTo catches up over 33 epochs — fine, cheap. ✓. And the maxAnchorBackdate clamp applies only to the SCANNED anchor; the session anchor is local and can be arbitrarily stale → chain catch-up span = now - (a-3) — bounded by maxRatchetSpan (7 days) in ratchetTo → fine.

2. **SAS computation uses `myPublicHex(ctx).hexToBytesOrNull()!!` and peerPub** — pairSasWords(shared, myPub, peerPub). The Rust pair_sas presumably sorts the two pubkeys internally. Both sides compute with (shared, ownPub, peerPub) — if the Rust sorts, both get the same SAS. Should be fine if implemented as spec'd. But note: shared is the same on both sides (ECDH). ✓.

3. **S5 session**: `beginPairingSession()` calls `endPairingSession()` — zeroes salt; a NEW salt is generated lazily on next myPairSalt() call. So session start = fresh salt on first use. QR encodes that salt; addContact uses myPairSalt() → same salt as long as session hasn't ended in between. The bug they caught was DisposableEffect ordering. Need to check the UI flow now: does anything call endPairingSession between QR display and commitPairing? E.g., DisposableEffect onDispose of the QR composable while the confirm dialog is still showing? If the QR composable leaves composition when the SAS confirm dialog appears (likely! the flow may switch from "show QR/scan" to "show SAS words"), onDispose → endPairingSession → salt zeroed → commitPairing → buildContact... wait, buildContact runs BEFORE the SAS dialog (preparePairing). preparePairing uses myPairSalt — if the session ended when the QR composable was disposed... Sequence: user opens dialog (session begins, salt S1, QR shows salt S1) → scans peer QR → preparePairing called → uses myPairSalt() = S1 (session still active?) → SAS dialog shows → user confirms → commitPairing (no salt use) ✓. But if preparePairing happens AFTER the QR composable disposed... depends on navigation. If the scan screen replaces the QR screen and disposal ends the session before preparePairing runs, myPairSalt() generates S2 ≠ S1 (what the peer scanned) → seed mismatch → **private messages never open**. This is a strong candidate. Need to read MeshUi.kt pairing flow.

Also — CRITICAL: preparePairing calls buildContact which computes seed0 using myPairSalt(ctx). If session ended and salt regenerated, mismatch. The peer's phone did the same with ITS salt. For the seeds to match, each side must use the salt it DISPLAYED in its QR.

So the two prime suspects:
- A: salt regenerated between QR display and preparePairing (S5 lifecycle) → seed0 mismatch.
- B: anchor min() uses now instead of own QR anchor → chainEpoch mismatch → keys at every epoch differ.

Both produce EXACTLY the reported symptom: frames relay, echo ticks, never displayed. And both are silent. The debug log on the receiver would show "private frame not for us (or key mismatch)".

Let me now look at the MeshUi pairing flow to check the session lifecycle, and MeshService private path. Let me also check what pairSasWords expects (argument order) in crypto.rs/ffi.rs — if the FFI does NOT sort the keys and instead concatenates in argument order, then Alice computes SAS(shared, pubA, pubB) and Bob computes SAS(shared, pubB, pubA) → different words → users see mismatch... but that's UX, not the display bug.

Let me read the relevant parts of MeshUi.kt (pairing flow) and MeshService.kt (private branch + F3 echo). The MeshUi diff is huge (1364 lines changed), so let me grep for the session calls and pairing flow.

The PairStore diff already reveals one strong suspect. Now let me check the pairing dialog/session lifecycle in the UI and the private receive path.

**Tool: grep**

**Input:**
```json
{
  "pattern": "beginPairingSession|endPairingSession|qrPayload|preparePairing|commitPairing|discardPending|PairingDialog",
  "path": "/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh"
}
```

**Output:**
```
Found 22 matches
/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/MainActivity.kt:
  Line 285:         override fun preparePairing(label: String, keyOrQr: String): PairStore.PairPrepare =

  Line 286:             PairStore.preparePairing(this@MainActivity, label, keyOrQr)

  Line 288:         override fun commitPairing(pending: PairStore.PendingPairing): String? {

  Line 289:             val err = PairStore.commitPairing(this@MainActivity, pending)

  Line 298:             if (active) PairStore.beginPairingSession() else PairStore.endPairingSession()

  Line 307:         override fun myQrPayload(): String = PairStore.qrPayload(this@MainActivity)


/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/PairStore.kt:
  Line 200:     fun beginPairingSession() {

  Line 201:         endPairingSession()

  Line 206:     fun endPairingSession() {

  Line 223:     fun qrPayload(ctx: Context): String =

  Line 340:      * key material, so it must not outlive the dialog — [PairStore.discardPending] drops it.

  Line 352:     /** Outcome of [preparePairing]. */

  Line 368:     fun preparePairing(ctx: Context, label: String, offerRaw: String): PairPrepare {

  Line 376:     fun commitPairing(ctx: Context, pending: PendingPairing): String? {

  Line 473:         // ends (see endPairingSession), which is what bounds its lifetime.


/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/ui/MeshUi.kt:
  Line 93:     /** Derive a pairing for confirmation. Stores nothing — see [PairStore.preparePairing]. */

  Line 94:     fun preparePairing(label: String, keyOrQr: String): PairStore.PairPrepare

  Line 96:     fun commitPairing(pending: PairStore.PendingPairing): String?

  Line 217:         PairingDialog(controller, onDismiss = { showPairing = false })

  Line 1088: private fun PairingDialog(controller: UiController, onDismiss: () -> Unit) {

  Line 1203:                 when (val r = controller.preparePairing(name, peerKey)) {

  Line 1220:                 val err = controller.commitPairing(p)

```

---

## Assistant (Build · Kimi K3 (2x usage) · 12.5s)

_Thinking:_

Let me read MainActivity around line 285-310 (the UiController implementation, session active callback) and MeshUi.kt PairingDialog (1088-1260) to see the session lifecycle and QR generation.

**Tool: read**

**Input:**
```json
{
  "filePath": "/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/MainActivity.kt",
  "offset": 270,
  "limit": 60
}
```

**Output:**
```
<path>/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/MainActivity.kt</path>
<type>file</type>
<content>
270:                     markHexPrefix = "🔒 me→${contact.label}",
271:                     rssi = null,
272:                     text = text,
273:                     mine = true,
274:                     tier = SendTier.PRIVATE,
275:                     contactLabel = contact.label,
276:                     sendState = SendState.SENDING
277:                 )
278:             )
279:             toast("Sealing (VDL takes a few seconds)…")
280:             return null
281:         }
282: 
283:         override fun contacts(): List<Contact> = PairStore.contacts(this@MainActivity)
284: 
285:         override fun preparePairing(label: String, keyOrQr: String): PairStore.PairPrepare =
286:             PairStore.preparePairing(this@MainActivity, label, keyOrQr)
287: 
288:         override fun commitPairing(pending: PairStore.PendingPairing): String? {
289:             val err = PairStore.commitPairing(this@MainActivity, pending)
290:             if (err == null) {
291:                 MeshState.contactsVersion.value += 1
292:                 toast("Paired with ${pending.contact.label}")
293:             }
294:             return err
295:         }
296: 
297:         override fun setPairingSessionActive(active: Boolean) {
298:             if (active) PairStore.beginPairingSession() else PairStore.endPairingSession()
299:         }
300: 
301:         override fun removeContact(label: String) {
302:             PairStore.removeContact(this@MainActivity, label)
303:             MeshState.contactsVersion.value += 1
304:         }
305: 
306:         override fun myPublicHex(): String = PairStore.myPublicHex(this@MainActivity)
307:         override fun myQrPayload(): String = PairStore.qrPayload(this@MainActivity)
308:         override fun mySaltHex(): String = PairStore.mySaltHex(this@MainActivity)
309: 
310:         override fun launchQrScanner(onKey: (String) -> Unit) {
311:             pendingQrCallback = onKey
312:             if (androidx.core.content.ContextCompat.checkSelfPermission(
313:                     this@MainActivity, android.Manifest.permission.CAMERA
314:                 ) == android.content.pm.PackageManager.PERMISSION_GRANTED
315:             ) {
316:                 launchScanner()
317:             } else {
318:                 cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
319:             }
320:         }
321: 
322:         override fun panicWipe() {
323:             MeshService.requestPanicWipe(this@MainActivity)
324:             toast("Wiped")
325:             finishAffinity()
326:         }
327: 
328:         override fun toast(msg: String) = this@MainActivity.toast(msg)
329:     }

(Showing lines 270-329 of 534. Use offset=330 to continue.)
</content>
```

**Tool: read**

**Input:**
```json
{
  "filePath": "/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/ui/MeshUi.kt",
  "offset": 1088,
  "limit": 190
}
```

**Output:**
```
<path>/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/ui/MeshUi.kt</path>
<type>file</type>
<content>
1088: private fun PairingDialog(controller: UiController, onDismiss: () -> Unit) {
1089:     val contactsVersion by MeshState.contactsVersion.collectAsStateWithLifecycle()
1090:     val contacts = remember(contactsVersion) { controller.contacts() }
1091:     var name by rememberSaveable { mutableStateOf("") }
1092:     var peerKey by rememberSaveable { mutableStateOf("") }
1093:     // A3: the QR carries our key + a pairing salt. The salt must stay STABLE for as long
1094:     // as this QR is displayed — a peer pairing with the scanned code derives the chain
1095:     // seed from exactly these two values, so rotating mid-session would break the pairing.
1096:     val myKey = remember { controller.myPublicHex() }
1097:     // S1b: a pairing awaiting the user's word comparison. Nothing is stored until they agree.
1098:     var pending by remember { mutableStateOf<PairStore.PendingPairing?>(null) }
1099:     // S5: the salt exists only while this screen is open.
1100:     //
1101:     // Started in a `remember` rather than in the DisposableEffect below, and the ordering is
1102:     // load-bearing: composition runs `remember` blocks top to bottom, but defers effect
1103:     // bodies until the whole composition has finished. Beginning the session in the effect
1104:     // would rotate the salt AFTER the QR had already been encoded from the previous one, so
1105:     // the code on screen would not match the salt used to finish the pairing — and that
1106:     // mismatch is silent and permanent, because the two phones then derive different chain
1107:     // seeds and no private message between them ever opens.
1108:     remember { controller.setPairingSessionActive(true) }
1109:     DisposableEffect(Unit) {
1110:         onDispose { controller.setPairingSessionActive(false) }
1111:     }
1112:     val qr = remember {
1113:         val size = 640
1114:         val matrix = QRCodeWriter().encode(
1115:             controller.myQrPayload(), BarcodeFormat.QR_CODE, size, size,
1116:             mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M)
1117:         )
1118:         Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).also { bmp ->
1119:             for (y in 0 until size) for (x in 0 until size) {
1120:                 bmp.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
1121:             }
1122:         }.asImageBitmap()
1123:     }
1124: 
1125:     AlertDialog(
1126:         onDismissRequest = onDismiss,
1127:         containerColor = Panel,
1128:         title = { Text("New private chat", style = sansTitle()) },
1129:         text = {
1130:             Column {
1131:                 Text(
1132:                     "Show your code and scan theirs, face to face. Both sides pair from the " +
1133:                         "same two codes. The pairing salt exists only while this screen is " +
1134:                         "open and is erased when you close it, so past messages stay " +
1135:                         "unrecoverable even if a phone is later seized. Keys never touch a " +
1136:                         "server.",
1137:                     style = sansSub(), lineHeight = 19.sp
1138:                 )
1139:                 Spacer(Modifier.height(12.dp))
1140:                 Surface(
1141:                     color = Color.White,
1142:                     shape = RoundedCornerShape(10.dp),
1143:                     modifier = Modifier.align(Alignment.CenterHorizontally)
1144:                 ) {
1145:                     Image(qr, contentDescription = "Your pairing QR", modifier = Modifier.size(170.dp).padding(8.dp))
1146:                 }
1147:                 Spacer(Modifier.height(10.dp))
1148:                 SelectionContainer {
1149:                     Text(myKey, style = monoMicro(TextBright), lineHeight = 14.sp)
1150:                 }
1151:                 // The salt is deliberately NOT displayed. It was shown here as selectable
1152:                 // text, which made the one value that keeps a seized phone from recomputing
1153:                 // past chain seeds trivially copyable off the screen. Nothing about pairing
1154:                 // requires a human to read it — it travels inside the QR.
1155:                 Spacer(Modifier.height(14.dp))
1156:                 HorizontalDivider(color = Hairline)
1157:                 Spacer(Modifier.height(12.dp))
1158:                 OutlinedTextField(
1159:                     value = name, onValueChange = { name = it },
1160:                     label = { Text("Contact name", style = sansSub()) },
1161:                     textStyle = sansBody(), singleLine = true,
1162:                     shape = RoundedCornerShape(10.dp),
1163:                     colors = darkFieldColors(), modifier = Modifier.fillMaxWidth()
1164:                 )
1165:                 Spacer(Modifier.height(8.dp))
1166:                 OutlinedTextField(
1167:                     value = peerKey, onValueChange = { peerKey = it },
1168:                     label = { Text("Their pairing code", style = sansSub()) },
1169:                     textStyle = sansBody(),
1170:                     shape = RoundedCornerShape(10.dp),
1171:                     colors = darkFieldColors(), modifier = Modifier.fillMaxWidth()
1172:                 )
1173:                 TextButton(onClick = { controller.launchQrScanner { peerKey = it } }) {
1174:                     Text("Scan their QR", style = sansSub(Accent))
1175:                 }
1176:                 if (contacts.isNotEmpty()) {
1177:                     Spacer(Modifier.height(6.dp))
1178:                     HorizontalDivider(color = Hairline)
1179:                     Spacer(Modifier.height(8.dp))
1180:                     Text("Paired contacts", style = sansMeta())
1181:                     contacts.forEach { c ->
1182:                         Row(
1183:                             Modifier.fillMaxWidth().padding(vertical = 5.dp),
1184:                             verticalAlignment = Alignment.CenterVertically
1185:                         ) {
1186:                             Text("🔒 ${c.label}", style = sansBody(), modifier = Modifier.weight(1f))
1187:                             Text(
1188:                                 if (c.v2) "forward secret" else "legacy",
1189:                                 style = sansMeta(if (c.v2) TierPrivate else TrustAmber),
1190:                                 modifier = Modifier.padding(end = 10.dp)
1191:                             )
1192:                             Text(
1193:                                 "Remove", style = sansMeta(PanicRed),
1194:                                 modifier = Modifier.clickable { controller.removeContact(c.label) }.padding(4.dp)
1195:                             )
1196:                         }
1197:                     }
1198:                 }
1199:             }
1200:         },
1201:         confirmButton = {
1202:             TextButton(onClick = {
1203:                 when (val r = controller.preparePairing(name, peerKey)) {
1204:                     is PairStore.PairPrepare.Error -> controller.toast(r.message)
1205:                     // Nothing is stored yet — the words have to match on both screens first.
1206:                     is PairStore.PairPrepare.Confirm -> pending = r.pending
1207:                 }
1208:             }) { Text("Continue", style = sansRowTitle(Accent)) }
1209:         },
1210:         dismissButton = {
1211:             TextButton(onClick = onDismiss) { Text("Close", style = sansSub()) }
1212:         }
1213:     )
1214: 
1215:     pending?.let { p ->
1216:         PairingConfirmDialog(
1217:             pending = p,
1218:             onCancel = { pending = null },
1219:             onConfirm = {
1220:                 val err = controller.commitPairing(p)
1221:                 if (err != null) controller.toast(err)
1222:                 pending = null
1223:                 name = ""; peerKey = ""
1224:             }
1225:         )
1226:     }
1227: }
1228: 
1229: /**
1230:  * The step that makes QR pairing mean anything.
1231:  *
1232:  * Scanning a code proves only that a code was scanned. Someone sitting between two phones can
1233:  * hand each of them its own code, ending up with a separate shared secret on each side and
1234:  * the ability to read everything — and because both sides "paired successfully", nothing
1235:  * looks wrong. Comparing the words closes that: a relay holds two different secrets, so the
1236:  * two screens cannot agree.
1237:  */
1238: @Composable
1239: private fun PairingConfirmDialog(
1240:     pending: PairStore.PendingPairing,
1241:     onCancel: () -> Unit,
1242:     onConfirm: () -> Unit
1243: ) {
1244:     AlertDialog(
1245:         onDismissRequest = onCancel,
1246:         containerColor = Panel,
1247:         title = { Text("Check these words match", style = sansTitle()) },
1248:         text = {
1249:             Column {
1250:                 Text(
1251:                     "Both phones must show the same four words. If they differ, someone is " +
1252:                         "relaying between you — stop and do not save this contact.",
1253:                     style = sansSub(), lineHeight = 19.sp
1254:                 )
1255:                 Spacer(Modifier.height(14.dp))
1256:                 Surface(
1257:                     color = PanelRaised,
1258:                     shape = RoundedCornerShape(10.dp),
1259:                     modifier = Modifier.fillMaxWidth()
1260:                 ) {
1261:                     Text(
1262:                         pending.sasWords.joinToString("  ") { it.uppercase() },
1263:                         style = sansTitle(Accent),
1264:                         modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
1265:                     )
1266:                 }
1267:                 if (pending.replacesExistingKey) {
1268:                     Spacer(Modifier.height(12.dp))
1269:                     // Deliberately not phrased as an attack: after per-session salts, an
1270:                     // ordinary re-pair with the same person also lands here.
1271:                     Text(
1272:                         "⚠ You already have a contact with this name, and its key is " +
1273:                             "different. Saving replaces it, and messages will go to whoever " +
1274:                             "holds the new key. Confirm in person that this is them.",
1275:                         style = sansSub(TrustAmber), lineHeight = 19.sp
1276:                     )
1277:                 }

(Showing lines 1088-1277 of 1309. Use offset=1278 to continue.)
</content>
```

---

## Assistant (Build · Kimi K3 (2x usage) · 79.1s)

_Thinking:_

The UI session lifecycle: `remember { controller.setPairingSessionActive(true) }` — session begins when the dialog composes, `DisposableEffect onDispose` ends it. The QR is encoded in the same composition (after the remember) — salt stable during dialog. preparePairing is called while the dialog is open (session active) — salt matches QR. Good — S5 lifecycle looks correct now.

But WAIT — the anchor. `qrPayload(ctx)` calls `currentEpoch()` AT QR GENERATION TIME (line 1112-1123, remember — computed once when the dialog opens). And `buildContact` computes `start = min(offer.anchorEpoch, nowEpoch)` where nowEpoch = scan time.

Now the two-phone sequential flow:
- Alice opens dialog at epoch 100. Her QR encodes anchor 100.
- Bob opens dialog at epoch 100. His QR encodes anchor 100.
- Alice scans Bob's QR (anchor 100) at epoch 101: start_A = min(100, 101) = 100.
- Bob scans Alice's QR (anchor 100) at epoch 102: start_B = min(100, 102) = 100. ✓ AGREE.

Works when both dialogs are opened in the same epoch. But:
- Alice opens at 100 (QR anchor 100). Bob opens at 103 (QR anchor 103).
- Alice scans Bob (anchor 103) at 104: start_A = min(103, 104) = 103.
- Bob scans Alice (anchor 100) at 105: start_B = min(100, 105) = 100.
- **103 ≠ 100 → chains anchored differently → keys never match → private messages never open.**

Each phone must instead compute min(own_QR_anchor, scanned_anchor). Alice: min(100, 103) = 100. Bob: min(103, 100) = 100. ✓.

The current code uses the LOCAL NOW instead of the LOCAL QR ANCHOR. Since now ≥ own anchor always, min(scanned, now) = scanned when scanned ≤ now (always, after clamp). So effectively start = scanned anchor (clamped to 1 day). Alice adopts Bob's anchor; Bob adopts Alice's anchor. They swap anchors instead of agreeing on one!

When does it accidentally work? When both QRs were generated in the same epoch (both anchors equal). In face-to-face pairing, users open dialogs seconds apart — a 10s epoch boundary crossing between the two dialog opens bricks the pair. Coin flip weighted by how close to the boundary they are — ~most of the time both dialogs open within the same 10s window or one behind... no wait — sequential flow: Alice opens dialog, shows QR; Bob scans it; Bob opens HIS dialog... actually BOTH need to open the dialog (each has to display their QR AND scan). Typical: both open the app dialog around the same time (anchors equal or ±1), then scan each other. If Alice opens at epoch 100.9 and Bob at 101.1 — anchors differ (100 vs 101) → brick. So ~a boundary crossing between the two opens → failure. That's maybe 10-30% of pairings in practice... The user is hitting it NOW (or always — did private EVER work for them post-v3? They said "private messages relayed but not displayed" — maybe their test pairing crossed a boundary).

Hmm wait, also `qr = remember { ... }` — the QR bitmap is computed ONCE per dialog composition. If the dialog stays open across an epoch boundary, the QR keeps the OLD anchor (stable — good for the peer). And the local phone's own anchor is never stored — buildContact can't use it. The fix: capture the session anchor once (e.g., in beginPairingSession or lazily in qrPayload within a session), expose it, and compute start = min(mySessionAnchor, offer.anchorEpoch).

But actually — hold on. Let me reconsider whether v3 EVER worked for them. The user tested 0.9 "privatefix" — that was before v3 (v3 exists in 0.15? The 0.15 code I read earlier already had v3 with `minOf(it, currentEpoch())` — yes! The ORIGINAL 0.15 PairStore.kt I read had exactly this code: `val start = offer.anchorEpoch?.let { minOf(it, currentEpoch()) }`. So this bug predates 0.16 — I even flagged the anchor mechanism in the audit? I flagged anchorEpoch=0 clamp. I did NOT catch the min(scanned, now) asymmetry. Damn — I derived it during the last message but let me double-check by re-reading the 0.15 code I have in context: line 299: `val start = offer.anchorEpoch?.let { minOf(it, currentEpoch()) } ?: ...`. Yes. Same bug.

Did private messaging work in their 0.15 testing? Unknown. They had "0.9-privatefix" — maybe v2-era, anchor = currentEpoch() on both sides at scan time; sequential scans within the same 10s epoch → works; ±1 epoch apart → the backdate-3 window... with v2, start=now at scan; Alice scans at 100 (start 97 after backdate), Bob scans at 101 (start 98). Chains anchored 97 vs 98 → keys differ at every epoch! Unless both scanned in the same epoch. The comment says "without it each side anchored on its own wall clock and the two ratchets never produced a common key". So v2 was broken the same way (permanent mismatch whenever scans landed in different epochs).

So private messaging has likely NEVER worked reliably across epoch boundaries — the v3 fix is simply incorrect: it needs min(own_anchor, scanned_anchor), not min(scanned_anchor, now). THIS is very likely the current bug: "private messages relayed but not displayed."

Actually — wait. Let me be careful. Is min(a_scanned, now_A) == min(b_scanned, now_B) possible in more cases? start_A = min(b, tA), start_B = min(a, tB) where tA ≥ b (Alice scans after Bob's QR was made) and tB ≥ a. So start_A = b, start_B = a. Equal iff a == b. So: works iff both QRs generated in the same epoch. Confirmed.

The fix:
1. Store the session anchor: `mySessionAnchor: UInt?` alongside mySalt. Set it in beginPairingSession (or on first qrPayload call in a session): anchor = currentEpoch(). Cleared in endPairingSession.
2. qrPayload uses the session anchor (stable for the session — the QR is already remembered once per dialog, but make it robust for recomposition/multiple QRs in one session).
3. buildContact: start = min(mySessionAnchor ?: nowEpoch, offer.anchorEpoch). 

Hmm — but there's a wrinkle: preparePairing is called while the dialog is open, session active, mySessionAnchor set. If somehow null (pairing via a different entry point?), fall back to nowEpoch.

Also the maxAnchorBackdate clamp stays on the scanned anchor. Should we also worry: my session anchor could be arbitrarily old (dialog open for a day?) — chain catch-up from anchor-3 to now is bounded by maxRatchetSpan (7d) in ratchetTo. Fine.

4. Regression test: is there an Android-side test? Probably not (no instrumentation tests in repo?). At minimum, add a comment and maybe a small pure-Kotlin helper `agreedAnchor(own, scanned) = min(own, scanned)` that's trivially verifiable. Actually the real fix could be tested via Rust? No — the anchor logic is Kotlin. Keep simple.

Now let me also check the OTHER candidate causes before concluding:

a) pairSasWords argument order — if Rust sorts internally, fine. Let me check crypto.rs pair_sas.

b) The private receive path in MeshService — check the diff for the private branch (K4 ±2 for private? candidateKeys? F3 echo registration?). The echo works (two ticks), which means the frame is being relayed by the peer — the peer relays regardless of decryption. So display failure is almost certainly key mismatch. But let me verify the private branch and the F3 changes quickly, plus check that preparePairing's salt usage is right (session active during prepare — yes, dialog open).

c) One more possibility: myPairSalt during preparePairing — session active, salt = QR salt ✓. But what about the CONFIRM dialog: `pending` holds the Contact (with seed0). commitPairing stores it. Fine.

d) Could the peer's phone derive a different seed0? peer uses ITS salt (from its QR) and my salt (scanned) — pairSeedV2 sorts salts → same set → same seed0. ✓ IF both use the displayed salts. Session lifecycle OK now.

So the anchor is the bug. Let me also check pairSasWords in Rust quickly and the MeshService private-branch diff to rule out other regressions. Also the user asked about LOCAL flooding — I'll answer after. And the "resending doesn't stop" — by design (B1/B2); explain.

Let me look at crypto.rs pair_sas diff and MeshService diff (private branch).

Found it. The anchor agreement in `buildContact` is wrong. Let me confirm against the Rust side and the MeshService private path to rule out competing causes.

**Tool: bash**

**Input:**
```json
{
  "command": "git diff mesh-core/src/crypto.rs | head -150"
}
```

**Output:**
```
diff --git a/mesh-core/src/crypto.rs b/mesh-core/src/crypto.rs
index b5884da..11eebab 100644
--- a/mesh-core/src/crypto.rs
+++ b/mesh-core/src/crypto.rs
@@ -6,6 +6,7 @@
 //! See README.md §2.
 
 use chacha20poly1305::{ChaCha20Poly1305, KeyInit, Nonce, aead::Aead};
+use zeroize::Zeroize;
 use ed25519_dalek::{Signature, Signer, SigningKey, VerifyingKey};
 
 pub const DOMAIN_SIG: &[u8] = b"mesh-core:v1:frame-sig";
@@ -121,7 +122,83 @@ pub fn pair_seed_v2(shared: &[u8; 32], salt_a: &[u8; 32], salt_b: &[u8; 32]) ->
     material[..32].copy_from_slice(shared);
     material[32..64].copy_from_slice(lo);
     material[64..].copy_from_slice(hi);
-    blake3::derive_key("mesh-core:v1:pairseed-v2", &material)
+    let out = blake3::derive_key("mesh-core:v1:pairseed-v2", &material);
+    // M5: this buffer holds the raw ECDH output. Zeroize rather than a plain assignment —
+    // the compiler is free to elide writes to a dead stack buffer.
+    material.zeroize();
+    out
+}
+
+// ---------------------------------------------------------------------------
+// Short Authentication String (SAS) for QR pairing
+// ---------------------------------------------------------------------------
+
+/// Syllable table: 16 onsets x 8 vowels x 16 codas = exactly 2048 = 2^11 words.
+///
+/// Generated rather than drawn from a real-word list on purpose. A 2048-entry English list
+/// would be an unreviewable blob shipped into a security-critical confirmation screen, and
+/// the only dictionary available here (Webster's 2nd) is dominated by words like "besnow" and
+/// "abeigh" — worse to compare than syllables, with no frequency data to filter by. This
+/// table is auditable at a glance, is exactly a power of two so no index is wasted or biased,
+/// and is pronounceable, so the string also works read aloud.
+///
+/// Letters that render or sound alike are left out (c/k, q, x, y, i/j overlap in codas).
+const SAS_ONSETS: [&str; 16] = [
+    "b", "d", "f", "g", "h", "j", "k", "l", "m", "n", "p", "r", "s", "t", "v", "z",
+];
+const SAS_VOWELS: [&str; 8] = ["a", "e", "i", "o", "u", "ee", "oo", "ai"];
+const SAS_CODAS: [&str; 16] = [
+    "b", "d", "f", "g", "k", "l", "m", "n", "p", "r", "s", "t", "v", "z", "ch", "sh",
+];
+
+/// Number of SAS words shown to the user. 4 words x 11 bits = 44 bits.
+pub const SAS_WORD_COUNT: usize = 4;
+
+/// Short Authentication String binding a completed ECDH to the two identities involved.
+///
+/// QR pairing authenticates NOTHING on its own: scanning a code proves only that some code
+/// was scanned, so a relay sitting between two phones can hand each of them its own key and
+/// read everything afterwards. Both sides display this string and the users compare it before
+/// the contact is saved. A man-in-the-middle holds two DIFFERENT shared secrets, so the two
+/// screens disagree.
+///
+/// 44 bits, not the 20 of a 6-digit code. The attack here is not offline: the adversary
+/// learns the first public key when it is displayed and can grind its own keypair to force a
+/// SAS collision *while the two users are still fumbling through a sequential scan*. 2^20
+/// ECDH-and-hash is under a second on one machine. 6 digits is the ZRTP number and it assumes
+/// a human VOICE is authenticating liveness; two screens carry no such signal.
+///
+/// The public keys are sorted so both sides derive the same value without knowing whose is
+/// whose. `shared` is the [`pair_derive`] output.
+pub fn pair_sas(shared: &[u8; 32], pk_a: &[u8; 32], pk_b: &[u8; 32]) -> [u8; 8] {
+    let (lo, hi) = if pk_a <= pk_b { (pk_a, pk_b) } else { (pk_b, pk_a) };
+    let mut material = [0u8; 96];
+    material[..32].copy_from_slice(shared);
+    material[32..64].copy_from_slice(lo);
+    material[64..].copy_from_slice(hi);
+    let mut k = blake3::derive_key("mesh-core:v1:pair-sas", &material);
+    let mut out = [0u8; 8];
+    out.copy_from_slice(&k[..8]);
+    material.zeroize();
+    k.zeroize();
+    out
+}
+
+/// Render a [`pair_sas`] value as [`SAS_WORD_COUNT`] words, 11 bits each.
+pub fn sas_words(sas: &[u8; 8]) -> Vec<String> {
+    let v = u64::from_be_bytes(*sas);
+    (0..SAS_WORD_COUNT)
+        .map(|i| {
+            // Take the top 44 bits, 11 at a time. Each 11-bit index splits 4/3/4.
+            let idx = ((v >> (64 - 11 * (i + 1))) & 0x7FF) as usize;
+            format!(
+                "{}{}{}",
+                SAS_ONSETS[(idx >> 7) & 0xF],
+                SAS_VOWELS[(idx >> 4) & 0x7],
+                SAS_CODAS[idx & 0xF]
+            )
+        })
+        .collect()
 }
 
 /// Advance a pair-chain key from `from_epoch` to `to_epoch` (A3 forward secrecy).
@@ -144,7 +221,13 @@ pub fn pair_ratchet(key: &[u8; 32], from_epoch: u32, to_epoch: u32) -> Option<[u
         let mut material = [0u8; 36];
         material[..32].copy_from_slice(&k);
         material[32..].copy_from_slice(&e.to_be_bytes());
-        k = blake3::derive_key("mesh-core:v1:pairratchet", &material);
+        let next = blake3::derive_key("mesh-core:v1:pairratchet", &material);
+        // M5: every intermediate here is a PAST epoch's key, and the whole point of the
+        // ratchet is that those become unrecoverable. Leaving them on the stack — which a
+        // long catch-up now fills thousands of copies of — undercuts exactly that claim.
+        material.zeroize();
+        k.zeroize();
+        k = next;
     }
     Some(k)
 }
@@ -311,6 +394,53 @@ mod tests {
         );
     }
 
+    #[test]
+    fn pair_sas_is_order_independent_and_binds_both_identities() {
+        let shared: [u8; 32] = [0x11; 32];
+        let pk_a: [u8; 32] = core::array::from_fn(|i| i as u8);
+        let pk_b: [u8; 32] = core::array::from_fn(|i| (255 - i) as u8);
+
+        // Both sides must derive the same string without knowing whose key is whose.
+        assert_eq!(pair_sas(&shared, &pk_a, &pk_b), pair_sas(&shared, &pk_b, &pk_a));
+
+        // A man-in-the-middle holds a DIFFERENT shared secret with each side. That is the
+        // whole mechanism: if this ever compared equal, the SAS would authenticate nothing.
+        let shared_mitm: [u8; 32] = [0x22; 32];
+        assert_ne!(pair_sas(&shared, &pk_a, &pk_b), pair_sas(&shared_mitm, &pk_a, &pk_b));
+
+        // Substituting an identity must also change it, so a relay cannot reuse one leg's
+        // string while presenting its own key to the other side.
+        let pk_m: [u8; 32] = [0x77; 32];
+        assert_ne!(pair_sas(&shared, &pk_a, &pk_b), pair_sas(&shared, &pk_a, &pk_m));
+    }
+
+    #[test]
+    fn sas_words_cover_the_table_exactly() {
+        // 16 onsets x 8 vowels x 16 codas = 2048 = 2^11, so every 11-bit index maps to a
+        // distinct word and none is unreachable. A table that was not a power of two would
+        // silently bias the string and cost entropy.
+        assert_eq!(SAS_ONSETS.len() * SAS_VOWELS.len() * SAS_CODAS.len(), 2048);
+
+        let words = sas_words(&[0u8; 8]);
+        assert_eq!(words.len(), SAS_WORD_COUNT);
+        assert_eq!(words, vec!["bab", "bab", "bab", "bab"]);
+
+        // All-ones in the top 44 bits selects the last entry of every column.
+        let words = sas_words(&[0xFF; 8]);
+        assert_eq!(words, vec!["zaish", "zaish", "zaish", "zaish"]);

```

**Tool: bash**

**Input:**
```json
{
  "command": "git diff android/app/src/main/java/org/bileichat/mesh/MeshService.kt | grep -n \"msgType\\|private\\|Private\\|K4\\|diff >\" | head -60"
}
```

**Output:**
```
10:+         * Extra airings of an already-sealed private frame, one per epoch rollover.
44:+         * K4 freshness half-window for LOCAL/BROADCAST, in epochs.
60:+         * K4 freshness half-window for PRIVATE, in epochs. Deliberately narrower.
64:+         * window, and [PRIVATE_REAIRINGS] is sized to it. A private frame admitted at 3–4
92:     private var currentPublicFrame: ByteArray? = null
93:     private var privateTransportActive = false
95:+    /** A sealed private frame awaiting its remaining re-airings (see [PRIVATE_REAIRINGS]). */
96:+    private class PendingPrivate(
101:+    private val pendingPrivateLock = Any()
102:+    private var pendingPrivate: PendingPrivate? = null
111:-    private data class OwnFrame(val epoch: UInt, val carriedText: Boolean)
112:+    /** [private] frames take a separate receipt path: they never set outgoingText and their
113:+     *  re-airings are counted by reairPendingPrivate, so the public conditions do not apply. */
114:+    private data class OwnFrame(
117:+        val private: Boolean = false
119:     private val ownHashesLock = Any()
120:     private val ownHashes = LinkedHashMap<String, OwnFrame>()
126:-    private val dedup = FfiDedup(4096u)
133:+    private val dedup = FfiDedup(8192u)
136:     private val trust = FfiTrust()
139:     private var lastBucketFullEpoch: UInt? = null
142:+    private var cleanSkewEpochs = 0
154:+    private val rejectedLock = Any()
155:+    private val rejectedHashes = LinkedHashSet<String>()
157:+    private fun isRejected(hashHex: String): Boolean =
160:+    private fun rememberRejected(hashHex: String) {
181:+    private val rxTotal = java.util.concurrent.atomic.AtomicInteger(0)
182:+    private val rxUndecodable = java.util.concurrent.atomic.AtomicInteger(0)
183:+    private val rxBadSig = java.util.concurrent.atomic.AtomicInteger(0)
184:+    private val rxSkewed = java.util.concurrent.atomic.AtomicInteger(0)
185:+    private val rxDuplicate = java.util.concurrent.atomic.AtomicInteger(0)
186:+    private val rxShown = java.util.concurrent.atomic.AtomicInteger(0)
190:+    private val rxReplayedReject = java.util.concurrent.atomic.AtomicInteger(0)
192:     // B8: prioritized relay queue. Priority: LOCAL echo (0) > regional (1) > private (2);
195:-    private data class RelayTask(val frame: ByteArray, val priority: Int, val seq: Long)
196:+    private data class RelayTask(
203:     private val relayQueueLock = Any()
204:     private val relayQueue = ArrayDeque<RelayTask>()
205:     private var relaySeq = 0L
210:-    private val recentTexts = HashMap<String, UInt>()
223:+        radio.onFrameOnAir = { isPrivate ->
224:+            if (isPrivate) {
225:+                MeshState.privateRowTs?.let { MeshState.markOutgoing(it, SendState.ON_AIR) }
262:             for (ps in MeshState.privateSends) {
268:-                    MeshState.logDebug("private send dropped: contact unknown or key ratchet failed")
271:-                val counter = PairStore.nextPrivateCounter(this@MeshService, epoch)
272:-                MeshState.logDebug("sealing private message (VDL solve, ~seconds of CPU)…")
275:-                    makePrivateFrame(seed, epoch, beaconSeed, pairKey, ps.text, counter.toULong())
280:+                // air, and the receiver's ±2 K4 gate drops it before it ever reaches the
281:+                // private branch. Silent on both ends, and it hits ONLY private, because
282:+                // private is the only tier that pays for a VDL witness.
293:+                        sealFailure = "private send dropped: contact unknown or key ratchet failed"
296:+                    val counter = PairStore.nextPrivateCounter(this@MeshService, sealEpoch)
297:+                    MeshState.logDebug("sealing private message (VDL solve, ~seconds of CPU)…")
301:+                        makePrivateFrame(seed, sealEpoch, beaconSeed, pairKey, ps.text, counter.toULong())
306:+                        sealFailure = "private seal failed (text > 47 bytes or bad key)"
322:+                    sealFailure = "private send dropped: this phone cannot solve the VDL witness " +
326:                     // C3: cap the window at 6 s. While the private frame uses the primary
328:                         val started = radio.advertisePrivateOnce(
345:                             privateTransportActive = false

```

---

## Assistant (Build · Kimi K3 (2x usage) · 12.5s)

_Thinking:_

The Rust SAS implementation looks correct (sorts keys, binds shared, order-independent, tested). The Kotlin side calls `pairSasWords(shared, myPublicHex, peerPub)` — order-independent internally, fine.

The anchor bug stands. Let me now verify the MeshService private branch quickly to rule out other display-path regressions, particularly the K4 private window, candidateKeys usage, and the display append. Let me grep the current MeshService private branch.

**Tool: grep**

**Input:**
```json
{
  "pattern": "K4_PRIVATE|K4_PUBLIC|halfWindow|windowFor|msgType.toInt\\(\\) == 3",
  "path": "/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/MeshService.kt"
}
```

**Output:**
```
Found 1 matches
/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/MeshService.kt:
  Line 1268:         if (wp != null && wp.msgType.toInt() == 3) {

```

---

## Assistant (Build · Kimi K3 (2x usage) · 9.0s)

**Tool: read**

**Input:**
```json
{
  "filePath": "/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/MeshService.kt",
  "limit": 120,
  "offset": 1100
}
```

**Output:**
```
<path>/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/MeshService.kt</path>
<type>file</type>
<content>
1100:         // this same window, and PRIVATE_REAIRINGS is sized to it. Admitting a private frame
1101:         // 3–4 epochs old would pass K4 and VDL, relay normally, and then open under no key —
1102:         // invisible on both ends, which is the failure mode this codebase keeps rediscovering.
1103:         //
1104:         // Widening this is only safe because dedup now decays on the LOCAL clock over a
1105:         // window that covers it (DEDUP_RETENTION_EPOCHS); replay protection moved from the
1106:         // gate onto dedup. Do NOT gate freshness on TTL instead — TTL is in the hop-mutable
1107:         // region excluded from the hash and signature, so it is attacker-settable, and
1108:         // trusting it would let anyone re-air an arbitrarily old frame by claiming one hop.
1109:         val isPrivateTier = wp?.msgType?.toInt() == 3
1110:         val freshnessWindow = if (isPrivateTier) PRIVATE_FRESHNESS_EPOCHS else PUBLIC_FRESHNESS_EPOCHS
1111:         val diff = frameEp.toLong() - ownEpoch.toLong()
1112:         if (diff > freshnessWindow || diff < -freshnessWindow) {
1113:             val pair = Pair(frameEp, ownEpoch)
1114:             if (pair != lastSkewPair) {
1115:                 lastSkewPair = pair
1116:                 MeshState.logDebug(
1117:                     "epoch skew: frame=$frameEp own=$ownEpoch (check epochMs match + clocks)"
1118:                 )
1119:             }
1120:             // Surface it: a skewed peer is invisible, and silence is indistinguishable from
1121:             // "nobody is around". diff is in epochs; report it in seconds, which is what the
1122:             // user can actually act on.
1123:             rxSkewed.incrementAndGet()
1124:             // S6: only PAST-stale frames are cached. Their epoch is fixed and our clock only
1125:             // moves forward, so they can never become admissible. A future-stamped frame is
1126:             // the opposite — it becomes legitimately fresh as we catch up, and caching it
1127:             // would blacklist a frame we are about to want.
1128:             if (diff < 0) rememberRejected(hashHex)
1129:             val offBySec = diff * cfg.epochMs / 1000L
1130:             // Word this as what we actually measured. All we know is that the frame's OWN
1131:             // epoch field is N seconds out of step with ours — which is a skewed clock OR a
1132:             // frame that took too long to build (the VDL solve) or to relay. Naming the clock
1133:             // as the cause sends the user to fix something that may be perfectly fine.
1134:             MeshState.clockSkewWarning.value =
1135:                 "⚠ FRAMES ARRIVING ${kotlin.math.abs(offBySec)}s " +
1136:                     (if (offBySec > 0) "AHEAD OF" else "BEHIND") +
1137:                     " THIS PHONE'S CLOCK — THEY ARE BEING DROPPED. CHECK THAT BOTH PHONES ARE " +
1138:                     "ON NETWORK TIME AND SET TO THE SAME EPOCH LENGTH. IF BOTH MATCH, THE " +
1139:                     "FRAME EITHER TOOK TOO LONG TO BUILD OR TRAVELLED TOO MANY HOPS."
1140:             return
1141:         }
1142: 
1143:         val mark = frameMark(bytes) ?: return
1144: 
1145:         // Send-and-listen (spec): a copy of OUR OWN frame coming back over the relay path
1146:         // means at least one peer relayed it. B1: that echo is NOT a delivery guarantee —
1147:         // a single adversarial device can forge it by relaying once and blackholing the
1148:         // rest. LOCAL therefore only drops to sparse re-airing; BROADCAST still stops at
1149:         // the next epoch boundary (its TTL does the propagating, not our re-origination).
1150:         // Our hash went into dedup at origination, so this check must run BEFORE the dedup
1151:         // gate. TTL sits outside the hashed region, so the relayed echo hashes identically.
1152:         val ownEcho = claimOwnEcho(hash)
1153:         if (ownEcho != null) {
1154:             MeshState.logDebug(
1155:                 "own echo heard (epoch=${ownEcho.epoch} text=${ownEcho.carriedText}" +
1156:                     (if (ownEcho.private) " private" else "") + ") — a peer relayed us"
1157:             )
1158:             if (ownEcho.private) {
1159:                 // Private echoes are judged on their own terms, because none of the public
1160:                 // machinery applies to them: a private send never touches outgoingText (it
1161:                 // rides the privateSends queue), so the text condition below is always false
1162:                 // for it, and its re-airings are counted by reairPendingPrivate rather than
1163:                 // by outgoingSetAtEpoch, so the BROADCAST cut-off must not fire either.
1164:                 // Registering the hash alone would therefore have ticked nothing.
1165:                 //
1166:                 // What this proves is weaker than it looks and the wording has to match: a
1167:                 // peer put the SEALED frame back on air. Any peer relays private frames —
1168:                 // they are opaque — so this says nothing about whether the recipient could
1169:                 // open it. There is no acknowledgement in this protocol and there will not
1170:                 // be one; an ack would tie a receiver to a sender.
1171:                 MeshState.privateRowTs?.let { MeshState.markOutgoing(it, SendState.ECHOED) }
1172:                 MeshState.receipt.value =
1173:                     "✓ private message relayed by a peer (not proof it was opened)"
1174:             } else {
1175:                 // Only a frame that actually carried the user's text is a delivery signal.
1176:                 // LOCAL airs empty presence frames between sparse re-broadcasts; echoing one
1177:                 // of those is liveness, not receipt.
1178:                 val textEcho = ownEcho.carriedText && MeshState.outgoingText.value.isNotEmpty()
1179:                 if (textEcho) {
1180:                     reflectionHeard = true
1181:                     // Second tick: a peer put our frame back on air. Still not "delivered" —
1182:                     // one adversarial device can relay once and blackhole everything after.
1183:                     MeshState.outgoingRowTs?.let { MeshState.markOutgoing(it, SendState.ECHOED) }
1184:                     MeshState.receipt.value =
1185:                         "✓ heard back once — a peer relayed it (not proof of delivery)"
1186:                     if (MeshState.outgoingTier.value != SendTier.LOCAL) {
1187:                         val repeat = cfg.messageRepeatEpochs.toLong()
1188:                         if (repeat > 0) {
1189:                             MeshState.outgoingSetAtEpoch =
1190:                                 (ownEpoch.toLong() + 1L - repeat).coerceAtLeast(0L).toUInt()
1191:                             MeshState.logDebug("reflection heard: mesh is carrying our broadcast; stopping re-origination")
1192:                         }
1193:                     }
1194:                 }
1195:             }
1196:         }
1197: 
1198:         // Presence: direct-RF liveness only, BEFORE the dedup gate. A frame counts only
1199:         // at its ORIGINATION TTL: relays decrement (regional/private) or clobber to 0
1200:         // (local), so ttl == origin TTL ⇔ straight from the originator. Relayed copies
1201:         // must not register the originator as "nearby" — including the relayed echo of
1202:         // our OWN frame, which would otherwise count us as our own neighbor.
1203:         // Deliberately no RSSI floor: any frame that decoded + verified is a real
1204:         // transmission. The −80 dBm config floor is a sketch/trust window, NOT a liveness
1205:         // window — applying it here made the count flicker at the boundary while messages
1206:         // kept flowing.
1207:         val localTtl = defaultTtlLocal().toInt()
1208:         val originTtl = defaultTtlRegional().toInt()
1209:         val direct = when (wp?.msgType?.toInt()) {
1210:             1 -> frameTtl(bytes)?.toInt() == localTtl
1211:             2, 3 -> frameTtl(bytes)?.toInt() == originTtl
1212:             else -> false
1213:         }
1214:         if (direct) MeshState.measurement.recordPresence(mark, frameEp)
1215: 
1216:         // Admission check only — the INSERT happens at the end, once we have actually acted
1217:         // on this frame. Inserting here meant a frame that transiently failed verification
1218:         // (empty local sketch at the start of an epoch, contacts not yet loaded) was stuck in
1219:         // the seen-set for the whole ~3-epoch window: every retransmission of those exact

(Showing lines 1100-1219 of 1757. Use offset=1220 to continue.)
</content>
```

---

## Assistant (Build · Kimi K3 (2x usage) · 10.0s)

_Thinking:_

The K4 gating and echo logic look right. Now let me see the private branch at line 1268 onward to check the trial-decrypt path.

**Tool: read**

**Input:**
```json
{
  "filePath": "/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/MeshService.kt",
  "limit": 110,
  "offset": 1260
}
```

**Output:**
```
<path>/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/MeshService.kt</path>
<type>file</type>
<content>
1260:         // frames above the RSSI floor count as direct-RF co-presence witnesses.
1261:         if (wp != null && wp.msgType.toInt() == 1 && rssi >= cfg.rssiFloorDbm) {
1262:             synchronized(marksLock) { localImmediateMarks.add(mark) }
1263:         }
1264: 
1265:         // K2: Tier-3 private (msgType 3). Trial-decrypt against every paired contact key.
1266:         // E3: iterate ALL contacts unconditionally (no early break) — the NUMBER of decrypt
1267:         // calls must not leak which contact index matched (timing side-channel).
1268:         if (wp != null && wp.msgType.toInt() == 3) {
1269:             if (!vdlCheckFrame(bytes)) {
1270:                 MeshState.logDebug("drop: private frame failed VDL proof-of-work check")
1271:                 // A bad PoW is a permanent property of these bytes — mark seen so a flood of
1272:                 // copies is rejected at the cheap gate instead of re-running the check.
1273:                 dedup.insertEpoch(hash, frameEp)
1274:                 return // invalid PoW: drop, do not relay
1275:             }
1276:             var privatePlaintext: String? = null
1277:             var privateLabel: String? = null
1278:             var contactsTried = 0
1279:             var keysTried = 0
1280:             for (contact in PairStore.contacts(this)) {
1281:                 contactsTried++
1282:                 // A3: v2 contacts try the epoch-ratcheted key for the frame's epoch
1283:                 // (fast-forwarding when the sender is ahead); v1 uses the static key.
1284:                 for (key in PairStore.candidateKeys(this, contact, frameEp)) {
1285:                     keysTried++
1286:                     val pt = openPrivateBodyOnly(bytes, key)
1287:                     if (pt != null && privatePlaintext == null) {
1288:                         privatePlaintext = pt
1289:                         privateLabel = contact.label
1290:                     }
1291:                 }
1292:             }
1293:             if (privatePlaintext == null) {
1294:                 // A private frame that reaches us, passes VDL, and opens under no key is the
1295:                 // single most confusing failure in the app: it is relayed normally, so the
1296:                 // mesh looks healthy, while the recipient's screen stays empty and the sender
1297:                 // gets no signal. Most often it is simply not addressed to us — but with a
1298:                 // divergent ratchet anchor it is EVERY frame, so the counts matter.
1299:                 MeshState.logDebug(
1300:                     "private frame not for us (or key mismatch): tried $keysTried key(s) " +
1301:                         "across $contactsTried contact(s) at epoch $frameEp"
1302:                 )
1303:             }
1304:             if (privatePlaintext != null) {
1305:                 // A3: persist any fast-forwarded chain state (past keys deleted). This does a
1306:                 // synchronous commit() to EncryptedSharedPreferences, so it must not run on
1307:                 // the BLE binder thread that delivered this frame — disk I/O there stalls
1308:                 // scan callback delivery for every other frame in flight.
1309:                 val label = privateLabel!!
1310:                 lifecycleScope.launch(Dispatchers.IO) {
1311:                     try {
1312:                         PairStore.noteOpened(this@MeshService, label, frameEp)
1313:                     } catch (e: Exception) {
1314:                         // No label: the debug log is exportable, and naming a contact on a
1315:                         // failure path is social-graph metadata a seized export hands over.
1316:                         MeshState.logDebug("noteOpened failed for a contact: ${e.message}")
1317:                     }
1318:                 }
1319:                 MeshState.appendMessage(
1320:                     MsgRow(
1321:                         tsMs = System.currentTimeMillis(),
1322:                         epoch = frameEp,
1323:                         markHexPrefix = "🔒 $privateLabel",
1324:                         rssi = rssi,
1325:                         text = privatePlaintext,
1326:                         mine = false,
1327:                         tier = SendTier.PRIVATE,
1328:                         direct = direct,
1329:                         contactLabel = privateLabel
1330:                     )
1331:                 )
1332:             }
1333:             // Relay regardless of whether we could decrypt (multi-hop delivery).
1334:             relayFrame(bytes)?.let { enqueueRelay(it, 3) }
1335:             // Acted on: relayed, and decrypted if it was for us. Safe to mark seen.
1336:             dedup.insertEpoch(hash, frameEp)
1337:             return
1338:         }
1339: 
1340:         // Public path (msgType 1/2).
1341:         //
1342:         // A1: a frame WITHOUT a witness is relay-only — NEVER displayed. Before this fix the
1343:         // witness check was skipped entirely when both fields were zero, so a remote van could
1344:         // inject a fake "TEAR GAS" that displayed as DIRECT · VERIFIED on every phone.
1345:         //
1346:         // Display rules:
1347:         //   LOCAL     — witness must be PoCP-Valid against our cell (current or previous
1348:         //               epoch sketch bucket). CellMismatch/Stale: dropped entirely.
1349:         //   BROADCAST — witness MAC must be valid. Jaccard outcome only feeds the badge:
1350:         //               co-present origin vs remote-cell claim. A2: corroboration counts
1351:         //               ONLY claims heard DIRECTLY (origination TTL) and is shown as a HINT,
1352:         //               never as a boolean unlock (a single nearby attacker can forge two
1353:         //               dissimilar claims — the old distinct≥2 display lock was security theater).
1354:         var displayOk = false
1355:         var relayOnly = false
1356:         var unjudged = false
1357:         var corroborations = 0u
1358:         var unattested = false
1359:         if (wp != null) {
1360:             val msgType = wp.msgType.toInt()
1361:             val hasWitness = wp.pocpWit.any { it != 0.toByte() } ||
1362:                 wp.divSketch.any { it != 0.toByte() }
1363:             if (!hasWitness) {
1364:                 relayOnly = true // A1: relay-only, never display
1365:                 // Every "the other phone relayed it but never showed it" report lands here.
1366:                 // An up-to-date peer never originates witnessless, so this now means the
1367:                 // sender is running an older build or genuinely had no cell to attest to.
1368:                 MeshState.logDebug(
1369:                     "relay-only: frame from ${mark.joinToString("") { "%02x".format(it) }.take(8)} " +

(Showing lines 1260-1369 of 1757. Use offset=1370 to continue.)
</content>
```

---

## Assistant (Build · Kimi K3 (2x usage) · 136.2s)

_Thinking:_

The private receive path is intact: K4 private ±2, VDL, candidateKeys, trial decrypt, append. The echo (two ticks) proves the peer relayed — which happens regardless of decryption ("Relay regardless of whether we could decrypt", line 1333-1334).

So the display failure is at trial decryption — keys don't match. And the root cause is the anchor agreement bug in `buildContact`:

`val start = offer.anchorEpoch?.let { minOf(it, nowEpoch) }`

- Alice's QR encodes anchor **a** (when her dialog opened). Bob's QR encodes anchor **b**.
- Alice scans Bob → start_A = min(b, t_A) = b (since t_A ≥ b).
- Bob scans Alice → start_B = min(a, t_B) = a (since t_B ≥ a).
- Agreement requires a == b: **both phones must have opened their pairing dialogs within the same 10-second epoch.** Otherwise the chains anchor at different epochs, and since the ratchet mixes the epoch index into every step, the two chains produce different keys at every epoch forever. Every private message fails to open, in both directions, silently. Frames relay fine (relay needs only sig+VDL), the echo tick fires (relay ≠ open), and the receiver logs "private frame not for us (or key mismatch): tried N key(s) across M contact(s)".

The comment in the code even describes the intent: "each side holds both epochs and takes min() — same value on both phones". But each side holds the OTHER's anchor and its own NOW — not its own QR anchor. The phone's own anchor is never stored anywhere. The min is over the wrong pair of values.

Fix: 
1. Store the session anchor alongside the session salt: `private var mySessionAnchor: UInt? = null`, set in `beginPairingSession()` (or lazily in qrPayload when null), cleared in `endPairingSession()`.
2. `qrPayload` uses the stored session anchor instead of calling `currentEpoch()` fresh. This also makes the QR payload stable across recompositions for the whole session (currently the QR bitmap is remembered once per dialog, so it's stable in practice — but making the anchor session-scoped removes the fragility entirely, including multiple sequential pairings in one session... wait, careful: in one session, the user might pair with multiple people sequentially. Each pairing uses min(mySessionAnchor, scannedAnchor). Peer does min(theirSessionAnchor, scannedAliceAnchor). Both = min of the two session anchors. Works for sequential pairings too. ✓)
3. `buildContact`: `start = min(mySessionAnchor ?: nowEpoch, offer.anchorEpoch)`.

Edge: preparePairing when no session is active (e.g., pairing initiated without opening the dialog? The only path is the dialog, so session is active; fallback to nowEpoch preserves old behavior).

Also: what about the anchor clamp? It rejects scanned anchors older than 1 day. Our own session anchor could be arbitrarily old if the dialog is open for a long time — chain catch-up from (start-3) to now is bounded by ratchetTo's maxRatchetSpan (7 days) → fine.

Now — is this REALLY the current failure? Evidence: the user reports private relayed, not displayed, two ticks. The two ticks are NEW in 0.16 (F3 private echo) — so the frame definitely reaches a peer and gets relayed. Display requires openPrivateBodyOnly to succeed. Key mismatch is the overwhelmingly likely cause, and the anchor bug produces exactly a permanent, bidirectional, silent key mismatch.

But wait — did the SAS words match when they paired? If the SAS matched, then shared + both pubkeys matched → ECDH fine, no MITM. The seed0 = f(shared, sorted salts) → also fine on both sides (salts from QRs, used correctly). So seed0 agrees. The ONLY remaining divergence is chainEpoch (anchor). ✓ Consistent.

One more check: is it possible their pairing succeeded because both dialogs opened in the same epoch, and something else is wrong? E.g., fastForwardChains advances BOTH chains to epoch-1 every epoch — chains with the same anchor advance identically → keys agree. keyForSend ratchets to sealEpoch; receiver candidateKeys ratchets to frameEp — same chain → same key. So if anchors agreed, private would work. The bug is exactly when anchors differ.

Also the user's earlier versions: 0.9-privatefix suggests private worked at some point (v1 static? or v2 with same-epoch scans). With v3 the bug manifests whenever the two dialogs open in different 10s epochs — very common in practice (one person fumbles with the camera for 15s). So "private messages relayed but not displayed" — matches perfectly.

Actually, hold on. Let me also double-check `qrPayload` stability across the session vs. the QR bitmap: `val qr = remember { ... myQrPayload() ... }` — computed once per dialog open. The anchor in the QR = epoch at dialog open. If the dialog stays open across epochs, the QR keeps the original anchor — good. The peer scans that anchor. So the peer's start = min(myQRanchor, theirNow) = myQRanchor (if my QR is older than their now — always). And my start = min(theirQRanchor, myNow) = theirQRanchor. So we swap anchors. Confirmed broken unless anchors equal.

Now the fix. Let me write it:

In PairStore:
```kotlin
/** S5/v3: the anchor epoch we put in our own QR this session. Both sides must anchor the
 *  chain at the SAME epoch: min() of the two QR anchors. The scanned code gives us theirs;
 *  this field is what lets us know our own. Null outside a pairing session. */
@Volatile private var mySessionAnchor: UInt? = null
```

beginPairingSession: endPairingSession() (which now also clears anchor). Anchor set lazily in qrPayload (when first QR is generated this session) — or eagerly in begin? beginPairingSession is called at dialog composition; qrPayload right after. Eager in begin is cleaner:

```kotlin
@Synchronized
fun beginPairingSession() {
    endPairingSession()
    mySessionAnchor = currentEpoch()
}
```

endPairingSession:
```kotlin
@Synchronized
fun endPairingSession() {
    mySalt?.fill(0)
    mySalt = null
    mySessionAnchor = null
}
```

qrPayload:
```kotlin
fun qrPayload(ctx: Context): String =
    "bileichat:key:v3:${myPublicHex(ctx)}:${myPairSalt(ctx).toHex()}:${sessionAnchorEpoch()}"
```
where sessionAnchorEpoch() returns mySessionAnchor ?: currentEpoch() (fallback for callers outside a session — keep QR usable).

Hmm — should sessionAnchorEpoch() also SET the field if null? If qrPayload is called without a session, each call would re-anchor at now — the old behavior. Better: if null, set it? No — outside a session there's no clear... The only caller is the dialog with an active session. Fallback: `mySessionAnchor ?: currentEpoch()` without setting is fine; in practice beginPairingSession always sets it first.

buildContact:
```kotlin
val start = offer.anchorEpoch?.let { scanned ->
    // min() over the TWO QR anchors — ours and theirs. NOT over (theirs, now):
    // min(theirs, now) is just their anchor, so each phone would anchor at the OTHER's
    // epoch, and when those differ the chains never produce a common key at any epoch.
    // This is the failure that looks exactly like "relayed, two ticks, never displayed".
    minOf(mySessionAnchor ?: nowEpoch, scanned)
} ?: run { ... nowEpoch }
```

Wait, one more subtlety: `min(mySessionAnchor, scannedAnchor)` — with backdate: chainEpoch = start - PAIR_EPOCH_BACKDATE. Both sides compute the same start → same chainEpoch → same chain. ✓

But ALSO: both sides need the same seed0 — yes (sorted salts + shared). ✓

Now, what about existing already-bricked contacts (paired with the anchor mismatch)? They have different chainEpoch AND effectively different chains (different index sequence from different anchors — even the same seed0 ratcheted from different anchor indices gives different keys). Those contacts can never agree — users must re-pair after the fix. Worth noting in the fix comment/changelog: re-pair required for contacts created on v3 builds.

Hmm wait — actually is that true? seed0 is the same on both sides. chainEpoch differs (97 vs 100, say). key_at_epoch_E = fold over (anchor+1..E). Different anchor → different sequence → different keys at every E. Yes, permanently divergent. Re-pair needed. The S1a key-continuity dialog will fire on re-pair (different derived key) — which is the designed UX for exactly this.

Now, are there other regressions to rule out? Let me think about whether the user's specific test could hit a different bug:

- They see two ticks (private echo) — F3 works.
- Recipient: log would show "private frame not for us (or key mismatch): tried N key(s) across M contact(s)". If M=0 (no contacts on the recipient!) — e.g., the SAS confirm never committed the contact on one side? commitPairing is called onConfirm. If Alice confirmed but Bob cancelled... Bob has no contact → tries 0 contacts → keysTried=0. The log distinguishes: "tried 0 key(s) across 0 contact(s)". Worth telling the user to check that log line. But assuming both confirmed, the anchor bug explains it.

Also — hmm, one more: `keyForSend` when epoch <= chainEpoch returns contact.pairKey (key AT chainEpoch). Suppose anchors agree at start s, chainEpoch = s-3. fastForwardChains steps to epoch-1 daily... fine.

What about the case where the SENDER's clock is BEHIND the receiver's? Frame stamped at receiver-1 → candidateKeys: frameEpoch == chainEpoch (receiver chain at epoch-1) → pairKey ✓. OK.

I'm confident the anchor bug is the primary defect. It's also consistent with the audit history: "v3 puts the generating epoch in the QR, so each side holds both epochs and takes min() — same value on both phones" — the design intent was min(own, scanned), the code implements min(scanned, now). The design comment is right; the implementation is wrong.

Let me also sanity check the user's other questions:

1. "Re-send theory / storm" — wrong: re-sending the same text builds a NEW frame (new epoch, new mark, new signature → new hash), so dedup doesn't suppress it and nothing treats it as a flood; the receiver merges it into one row with repeats=2. Nothing punishes re-sends. (Exception: byte-identical retransmissions are dedup'd — but the app never re-sends the same bytes for public; each origination is fresh.)

2. "Still says resending even when heard back once; stop prompt doesn't go away (LOCAL)" — by design: LOCAL keeps re-airing (sparse, every 4th epoch) until the 30-min cap or manual stop, because a single forged echo must not silence a danger alert (B1/B2). outgoingAiring stays true → the "still sending / stop" affordance stays. The receipt line ("✓ heard back once") is separate. So: not a bug, but the UX doesn't communicate "received AND still airing". Suggest copy change: after echo, change the composer status to "heard back — still re-airing every ~40s for up to 30 min; tap to stop". Or for chat-like use, maybe LOCAL chat should stop on echo? The design intent is danger alerts. For "local chat" (their usage!), the 30-min re-air of "hi" is annoying. Options: keep protocol, fix copy; or add a per-send "stop when heard" toggle; or stop re-airing on echo for user-typed chat-tier messages while keeping it for... hmm, but LOCAL is the chat tier in their app usage. The clean minimal fix: after first echo, switch the status text to make clear it's still airing and can be stopped. Actually re-reading their complaint: "it still says resending even when heard back once, that stop prompt doesnt go away" — the annoyance is the UI state, not the protocol. Minimal: update outgoingAiring/status copy post-echo. But careful — I shouldn't over-engineer; propose, ask? They're in build mode and asked "how does local chat work?" — explain + offer the UX tweak.

3. "How does local chat work? Can't the police just flood it cause no witnesses is allowed?" — Correction: witnesses ARE required for LOCAL display. A LOCAL frame without a witness is relay-only, never displayed. But: a co-present attacker (police in the crowd, within BLE range) hears the cell's marks and can construct a valid sketch → Valid witness → display. PoCP only stops REMOTE injection (and post-T2, the 1-byte grind). It does not and cannot stop an in-crowd forger — the README says this: "a determined nearby attacker can forge claims". Flooding protections: dedup bucket cap (1024/epoch — attacker can fill it: blackout, not spoofed display), merge-by-text collapse, low-confidence badge for small cells, and... rate: each frame needs a valid signature (free) — so yes, a nearby adversary CAN flood local chat with distinct validly-witnessed fake alerts. The display merge collapses identical text; distinct texts each get rows. The feed caps at 200 rows. So "flood with fake danger alerts" — possible for a co-present adversary; mitigations are corroboration hints + user judgment. A remote attacker (in a van 100m away, out of BLE range of the cell but within relay range) — post-T2, must pass inter≥2 against the receiver's cell: copy a full div_sketch from a relayed frame (S2, still open) → Valid. So even remote can do it via S2. Honest answer: yes.

4. Their question "phones are set to not send the same message when they hear the same message right?" — that's dedup: a phone relays a given frame-hash once; later identical copies are dropped. Right, but per-epoch and per-hash; re-originations are new hashes.

Now implement the fix. Files:
- PairStore.kt: add mySessionAnchor, set/clear in begin/end, use in qrPayload + buildContact.

Also — should I add a test? Android has no test infra visible (no instrumentation tests dir?). The Rust proptest pinned ratchet composition. For the anchor, a pure function `agreedAnchor(own, scanned)` — trivial. The real regression guard would be an instrumented test; repo doesn't have Android tests. I could extract a tiny internal helper and... without a test harness, adding a Kotlin unit test requires setting up test deps (junit) in gradle. Hmm — minimal: add the fix with a thorough comment. Maybe check if app/build.gradle.kts has test deps.

Also update the v3 doc comment in PairStore ("each side holds both epochs") — the comment is right, now the code matches it.

Let me also check MainActivity: does anything else call qrPayload (e.g., a widget)? grep showed only MainActivity:307 override. Fine.

One more consideration: `beginPairingSession` sets anchor eagerly. But what if the user opens the dialog, leaves it open 5 minutes (30 epochs), THEN the peer scans? Anchor is 30 epochs old → start = anchor; chainEpoch = anchor-3; ratchetTo catches up 30 epochs on first send/receive — trivial. And the 1-day clamp applies only to scanned anchors; the local one is ours, no clamp needed (bounded by ratchetTo maxRatchetSpan = 7 days; a dialog open 7 days is absurd).

But wait — there's an asymmetry risk: if the DIALOG is open for a very long time on ONE phone only. Alice's anchor=100 (dialog open 5 min), Bob's=130 (opened just now). start = min(100,130)=100 on both. ✓ Both ratchet from 97. Fine.

What if beginPairingSession is called but qrPayload generated AFTER an epoch tick? Same session anchor used → stable QR. The QR bitmap is remembered per dialog anyway. ✓

Now the "still says resending" UX — the user seems annoyed. Let me look at what the UI shows for outgoingAiring and receipt to propose the copy fix precisely. Let me grep the composer status in MeshUi.

Confirmed the mechanism. The receive path is fine — K4 ±2 private, VDL, trial-decrypt, append are all intact. The failure is upstream, in `PairStore.buildContact`: **the two phones never agree on a ratchet anchor.** Let me check the composer status UI for the LOCAL re-airing question, then fix.

**Tool: grep**

**Input:**
```json
{
  "pattern": "outgoingAiring|receipt",
  "path": "/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/ui/MeshUi.kt"
}
```

**Output:**
```
Found 4 matches
/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/ui/MeshUi.kt:
  Line 657:     val receipt by MeshState.receipt.collectAsStateWithLifecycle()

  Line 696:         val airing by MeshState.outgoingAiring.collectAsStateWithLifecycle()

  Line 721:         receipt?.let { note ->

  Line 874:  * There is no blue-tick equivalent and there will not be one: a read receipt needs an

```

---

## Assistant (Build · Kimi K3 (2x usage) · 9.1s)

**Tool: read**

**Input:**
```json
{
  "filePath": "/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/ui/MeshUi.kt",
  "limit": 75,
  "offset": 690
}
```

**Output:**
```
<path>/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/ui/MeshUi.kt</path>
<type>file</type>
<content>
690:         MessageList(msgs, convo, Modifier.weight(1f))
691: 
692:         // LOCAL re-originates every epoch until heard back, for up to 30 minutes. That is
693:         // correct for a danger alert and baffling for an ordinary message — it looked like
694:         // the app was stuck re-sending forever, with nothing on screen saying so and no way
695:         // out. Make it visible, and make it stoppable.
696:         val airing by MeshState.outgoingAiring.collectAsStateWithLifecycle()
697:         if (airing && convo !is Convo.Private) {
698:             Row(
699:                 Modifier
700:                     .fillMaxWidth()
701:                     .background(PanelRaised)
702:                     .padding(horizontal = 16.dp, vertical = 8.dp),
703:                 verticalAlignment = Alignment.CenterVertically
704:             ) {
705:                 Text(
706:                     if (convo == Convo.Local) "Still re-sending until a phone repeats it back (up to 30 min)"
707:                     else "Still re-sending for a few more seconds",
708:                     style = sansMeta(TextDim),
709:                     lineHeight = 15.sp,
710:                     modifier = Modifier.weight(1f)
711:                 )
712:                 Text(
713:                     "STOP",
714:                     style = sansMeta(PanicRed),
715:                     fontWeight = FontWeight.Bold,
716:                     modifier = Modifier.clickable { controller.stopSending() }.padding(8.dp)
717:                 )
718:             }
719:         }
720: 
721:         receipt?.let { note ->
722:             Text(
723:                 note,
724:                 style = sansMeta(if (note.startsWith("✓")) TierLocal else TrustAmber),
725:                 modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
726:             )
727:         }
728: 
729:         HorizontalDivider(color = Hairline)
730:         Composer(controller, convo, contact, onOpenPairing)
731:     }
732: }
733: 
734: @Composable
735: private fun ChatTopBar(convo: Convo, contact: Contact?, onBack: () -> Unit) {
736:     Row(
737:         Modifier.fillMaxWidth().height(56.dp).padding(end = 14.dp),
738:         verticalAlignment = Alignment.CenterVertically
739:     ) {
740:         // Back chevron, drawn — no icon dependency.
741:         Canvas(Modifier.size(44.dp).clickable(onClick = onBack).padding(14.dp)) {
742:             val stroke = 2.dp.toPx()
743:             val midY = size.height / 2f
744:             drawLine(TextBright, Offset(size.width, 0f), Offset(0f, midY), strokeWidth = stroke)
745:             drawLine(TextBright, Offset(0f, midY), Offset(size.width, size.height), strokeWidth = stroke)
746:         }
747:         Spacer(Modifier.width(2.dp))
748:         Avatar(convo, size = 36.dp)
749:         Spacer(Modifier.width(12.dp))
750:         Column(Modifier.weight(1f)) {
751:             Text(convo.title, style = sansRowTitle(), maxLines = 1, overflow = TextOverflow.Ellipsis)
752:             Text(
753:                 when (convo) {
754:                     Convo.Local -> "Room range · repeats until heard back"
755:                     Convo.Broadcast -> "Whole mesh · up to 8 hops"
756:                     is Convo.Private ->
757:                         if (contact == null) "Contact removed"
758:                         else if (contact.v2) "🔒 End-to-end encrypted · forward secret"
759:                         else "🔒 End-to-end encrypted · legacy pairing"
760:                 },
761:                 style = sansMeta(if (convo is Convo.Private && contact == null) PanicRed else TextDim),
762:                 maxLines = 1,
763:                 overflow = TextOverflow.Ellipsis
764:             )

(Showing lines 690-764 of 1309. Use offset=765 to continue.)
</content>
```

---

## Assistant (Build · Kimi K3 (2x usage) · 27.0s)

_Thinking:_

Now I understand the UX complaint: after the echo is heard ("✓ heard back once"), the LOCAL banner still says "Still re-sending until a phone repeats it back (up to 30 min)" — because LOCAL keeps sparse re-airing by design (B1/B2). The banner text doesn't acknowledge the echo. The user reads it as "the stop prompt doesn't go away."

The right minimal fix: once `reflectionHeard` is true, the banner should say the message WAS heard back and is now re-airing sparsely — or arguably stop the banner. Actually there's a receipt line below that shows "✓ heard back once". So the state is: banner "Still re-sending until a phone repeats it back" + receipt "✓ heard back once — a peer relayed it". Contradictory copy. Fix: banner text should switch after echo: "Heard back — still re-airing sparsely for up to 30 min (tap STOP to end)". Need a `reflectionHeard` state exposed to UI... MeshService holds it privately. There's `MeshState.receipt` and `outgoingAiring`. Simplest: add `MeshState.outgoingEchoed = MutableStateFlow(false)` set where reflectionHeard is set, cleared on new send; banner uses it to switch copy.

Now let me also implement the anchor fix. And answer the LOCAL/flood questions.

Plan:
1. Fix PairStore anchor agreement (session anchor + min(own, scanned)).
2. Add UI echo-aware banner copy (small).
3. Explain everything.

Let me look at the exact code sections to edit in PairStore.kt (current version). I have the diff; let me view the current relevant regions to edit precisely.

Now I have the full picture. Let me fix the root cause first — the anchor agreement bug in `PairStore`.

**The bug:** `buildContact` computes `start = min(scannedAnchor, now)`. But `now ≥ scannedAnchor` always, so this is just *the other phone's* anchor. Alice anchors at Bob's epoch, Bob anchors at Alice's. They only agree when both QRs were generated in the **same 10-second epoch** — otherwise the chains index from different starts, and since `pair_ratchet` mixes the epoch index into every step, the two chains produce different keys at *every* epoch, permanently. Frames relay fine (relay only needs sig+VDL), the new echo tick fires (relay ≠ open), and nothing displays. The design comment in the code says it correctly — "each side holds both epochs and takes min()" — but the code takes min over the wrong pair: the phone's own QR anchor is never stored anywhere.

**Tool: read**

**Input:**
```json
{
  "filePath": "/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/PairStore.kt",
  "limit": 70,
  "offset": 180
}
```

**Output:**
```
<path>/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/PairStore.kt</path>
<type>file</type>
<content>
180:         val s = ByteArray(PAIR_SALT_LEN)
181:         SecureRandom().nextBytes(s)
182:         mySalt = s
183:         return s
184:     }
185: 
186:     /**
187:      * S5: begin a pairing session. Call when the pairing screen opens.
188:      *
189:      * The salt is the entropy that makes a v2 chain seed unrecomputable from a seized
190:      * long-term secret — that is the entire forward-secrecy claim. It was generated lazily
191:      * and then never rotated or cleared, so it lived for the whole PROCESS: a phone left
192:      * running all day held, in memory, the material to reconstruct the chain seed of every
193:      * contact paired that day, and the pairing screen displayed it as selectable text.
194:      * "Forward secret after process death" is a much weaker claim than the one being made.
195:      *
196:      * A session covers one face-to-face pairing sitting, which is the only window where the
197:      * salt genuinely has to stay stable (both people scan the same displayed QR).
198:      */
199:     @Synchronized
200:     fun beginPairingSession() {
201:         endPairingSession()
202:     }
203: 
204:     /** S5: end a pairing session and zero the salt. Call when the pairing screen closes. */
205:     @Synchronized
206:     fun endPairingSession() {
207:         mySalt?.fill(0)
208:         mySalt = null
209:     }
210: 
211:     private fun currentEpoch(): UInt =
212:         (System.currentTimeMillis() / MeshState.config.epochMs).toUInt()
213: 
214:     /**
215:      * v2 QR payload: public key + per-pairing salt. Both public; the secret and the chain
216:      * keys never leave the device. The salt is fresh per pairing and rotated after each add.
217:      */
218:     /**
219:      * v3 adds the generating epoch. Both phones scan each other, so both end up holding both
220:      * epochs and can pick the SAME chain anchor with min(); without it each side anchored on
221:      * its own wall clock and the two ratchets never produced a common key (see addContact).
222:      */
223:     fun qrPayload(ctx: Context): String =
224:         "bileichat:key:v3:${myPublicHex(ctx)}:${myPairSalt(ctx).toHex()}:${currentEpoch()}"
225: 
226:     fun mySaltHex(ctx: Context): String = myPairSalt(ctx).toHex()
227: 
228:     /** Parse a scanned/typed pairing offer: v2 payload, legacy v1 payload, or raw 64-hex key. */
229:     fun parsePairingOffer(value: String): PairingOffer? {
230:         val trimmed = value.trim()
231:         val parts = trimmed.split(":")
232:         return when {
233:             parts.size == 6 && parts[0].equals("bileichat", true) &&
234:                 parts[1].equals("key", true) && parts[2] == "v3" -> {
235:                 val pk = parts[3]
236:                 val salt = parts[4]
237:                 val epoch = parts[5].toUIntOrNull()
238:                 if (pk.length == 64 && pk.hexToBytesOrNull()?.size == 32 &&
239:                     salt.length == PAIR_SALT_LEN * 2 &&
240:                     salt.hexToBytesOrNull()?.size == PAIR_SALT_LEN &&
241:                     epoch != null
242:                 ) PairingOffer(pk, salt, epoch) else null
243:             }
244:             parts.size == 5 && parts[0].equals("bileichat", true) &&
245:                 parts[1].equals("key", true) && parts[2] == "v2" -> {
246:                 val pk = parts[3]
247:                 val salt = parts[4]
248:                 if (pk.length == 64 && pk.hexToBytesOrNull()?.size == 32 &&
249:                     salt.length == PAIR_SALT_LEN * 2 &&

(Showing lines 180-249 of 669. Use offset=250 to continue.)
</content>
```

---

## Assistant (Build · Kimi K3 (2x usage) · 29.5s)

**Tool: edit**

**Input:**
```json
{
  "filePath": "/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/PairStore.kt",
  "oldString": "    /**\n     * S5: begin a pairing session. Call when the pairing screen opens.\n     *\n     * The salt is the entropy that makes a v2 chain seed unrecomputable from a seized\n     * long-term secret — that is the entire forward-secrecy claim. It was generated lazily\n     * and then never rotated or cleared, so it lived for the whole PROCESS: a phone left\n     * running all day held, in memory, the material to reconstruct the chain seed of every\n     * contact paired that day, and the pairing screen displayed it as selectable text.\n     * \"Forward secret after process death\" is a much weaker claim than the one being made.\n     *\n     * A session covers one face-to-face pairing sitting, which is the only window where the\n     * salt genuinely has to stay stable (both people scan the same displayed QR).\n     */\n    @Synchronized\n    fun beginPairingSession() {\n        endPairingSession()\n    }\n\n    /** S5: end a pairing session and zero the salt. Call when the pairing screen closes. */\n    @Synchronized\n    fun endPairingSession() {\n        mySalt?.fill(0)\n        mySalt = null\n    }",
  "newString": "    /**\n     * The anchor epoch THIS phone put in its own QR for the current pairing session.\n     *\n     * v3's shared-anchor scheme needs min(own QR anchor, scanned QR anchor) — the same two\n     * values on both phones. The scanned code carries theirs; this field is the only place\n     * ours exists. Anchoring on min(scanned, now) instead — as the first v3 implementation\n     * did — always collapses to the SCANNED anchor (now is never smaller), so each phone\n     * anchored at the OTHER's epoch: the chains agreed only when both QRs happened to be\n     * generated inside the same 10 s epoch. Because pair_ratchet folds the epoch index into\n     * every step, a one-epoch anchor difference means different keys at EVERY epoch,\n     * permanently — frames relay and echo normally, nothing ever opens, and the only trace\n     * is \"private frame not for us (or key mismatch)\" in the receiver's debug log.\n     *\n     * Session-scoped like the salt: the QR must stay stable for the whole sitting, and a\n     * fresh anchor per session keeps sequential pairings consistent (min of the two session\n     * anchors is symmetric). Null outside a session.\n     */\n    @Volatile private var mySessionAnchor: UInt? = null\n\n    /**\n     * S5: begin a pairing session. Call when the pairing screen opens.\n     *\n     * The salt is the entropy that makes a v2 chain seed unrecomputable from a seized\n     * long-term secret — that is the entire forward-secrecy claim. It was generated lazily\n     * and then never rotated or cleared, so it lived for the whole PROCESS: a phone left\n     * running all day held, in memory, the material to reconstruct the chain seed of every\n     * contact paired that day, and the pairing screen displayed it as selectable text.\n     * \"Forward secret after process death\" is a much weaker claim than the one being made.\n     *\n     * A session covers one face-to-face pairing sitting, which is the only window where the\n     * salt genuinely has to stay stable (both people scan the same displayed QR).\n     */\n    @Synchronized\n    fun beginPairingSession() {\n        endPairingSession()\n        mySessionAnchor = currentEpoch()\n    }\n\n    /** S5: end a pairing session and zero the salt. Call when the pairing screen closes. */\n    @Synchronized\n    fun endPairingSession() {\n        mySalt?.fill(0)\n        mySalt = null\n        mySessionAnchor = null\n    }"
}
```

**Output:**
```
Edit applied successfully.
```

---

## User

can you add a test button on the app? where it tests all the modules? like it waits for two phones to have same epoch and then starts the tests and generates a full log of all modules

---

## Assistant (Build · Kimi K3 (2x usage) · 137.1s)

_Thinking:_

The user wants a "test button" in the app that tests all modules — waits for two phones to have the same epoch, then runs tests and generates a full log of all modules.

Let me first finish what I was doing (I just edited PairStore.kt to add the session anchor). I still need to:
1. Complete the anchor fix: use `mySessionAnchor` in `qrPayload` and `buildContact` (min(own, scanned)).
2. Then build the test button feature.

Let me first complete the anchor fix, then design the self-test feature.

For the test button design, let me think about what it should do:
- A button in the app (probably in the Drawer / diagnostics section — the app has a debug log and export).
- When tapped: it runs a suite of self-tests across modules and generates a full log.
- "waits for two phones to have same epoch" — the user wants a synchronization point: both phones tap the button (roughly together), the test waits for the next epoch boundary (epoch = now/epochMs, so both phones cross together within ~1s), then runs tests so that both phones are in the same epoch and results are comparable.

What modules to test? The app's modules map to the README table: codec, crypto, message, pocp, beacon, private, vdl, statemachine (dedup), trust, ffi, plus Android-side: radio capability, GATT plane, pair store, sketch/measurement, config.

What can actually be tested in-app on a live phone?
1. Rust FFI roundtrip tests: make a message frame, decode it, verify signature, witness make/verify, private frame seal/open, VDL solve/verify at LOW difficulty (22 bits takes seconds — too slow for a quick test; but the FFI vdlCheckFrame uses production difficulty... solving at production difficulty takes ~seconds, acceptable for a one-shot test? makePrivateFrame does the solve internally — that's seconds of CPU. Maybe acceptable for one test).
2. Codec: 226-byte roundtrip via frameDecodes, frameEpoch, frameMark, etc.
3. Beacon: advance, entropy, epoch.
4. PoCP: observeMarks → sketch → divSketch → makeMessageFrameWithWitness → pocpVerifyWitnessLocal verdict.
5. Dedup: FfiDedup insert/duplicate/bucket.
6. Trust: FfiTrust record/count.
7. PairStore: contacts count, keyForSend/candidateKeys roundtrip for each v2 contact (ratchet composition), nextPrivateCounter monotonicity.
8. Radio: capabilityReport, extendedAdvCapable, codedPhySupported, advertisingActive.
9. GATT: server open, subscribers count, peers count.
10. Mesh sync: neighborsThisEpoch, local sketch size, rx counters.
11. The "wait for same epoch" part: wait until the next epoch boundary, log the epoch number, so logs from two phones can be aligned/compared. Maybe also a two-phone handshake over the mesh itself? That's complex. Simpler: the test waits for epoch boundary + small offset, runs, timestamps every line with epoch + ms-into-epoch. Both phones run it; logs can be diffed.

Where to put the button: Drawer.kt has EXPORT LOG etc. Let me read Drawer.kt and MeshState to see the debug log machinery and MainActivity controller interface (UiController in MeshUi.kt). The app architecture: MainActivity creates a UiController with overrides; MeshUi consumes it. I'll add `runSelfTest()` to UiController, implement in MainActivity, and the actual test logic in a new file `SelfTest.kt` that MeshService or MainActivity can call.

But wait — many modules live inside MeshService (dedup, trust, beacon, radio, gattPlane are private fields). The self-test needs access. Options:
- Put the test runner inside MeshService (has access to everything) and expose via a bind/broadcast or a static hook. The app architecture: MainActivity starts MeshService as a foreground service; is there a binder? Let me check how MainActivity communicates with MeshService — via MeshState shared flows mostly (outgoingText, privateSends channel, etc.). Simplest: add `MeshState.selfTestRequest = MutableStateFlow(0)`; MeshService's epoch loop or a dedicated coroutine collects it and runs the test, appending results to MeshState.debugLog and/or a dedicated test log that gets exported.

The user said "generates a full log of all modules" — output should go to the debug log (exportable via existing EXPORT LOG).

Design:
- New file `SelfTest.kt` with `runSelfTest(ctx, radio, beacon, dedup, trust, gattPlane, seed): List<String>` or appending to a log.
- Trigger: Drawer button "RUN SELF-TEST" → controller.runSelfTest() → increments MeshState.selfTestRequest → MeshService collector picks it up → waits for next epoch boundary (so two phones align) → runs tests → logs "SELF-TEST BEGIN epoch=N" ... results ... "SELF-TEST END".
- If service not running, show toast "start the mesh first".

What about "waits for two phones to have same epoch": epoch = now/epochMs from wall clock — both phones already share the epoch number if clocks agree. The test should: wait until the next epoch boundary (+ maybe 500ms), then log epoch + run. Both phones tap within a few seconds of each other; both wait for the same next boundary; tests run in the same epoch. Also log clock skew info.

Also should the self-test exercise the LIVE mesh path (send a frame and see if the other phone displays it)? That's the real end-to-end. But the user asked "tests all the modules ... generates a full log of all modules" — module-level diagnostics. I'll do module-level tests + record live stats (neighbors, rx counters, sketch size, GATT state) which is the closest to end-to-end without requiring coordination.

Actually, I could also add an optional ping: the self-test could originate a LOCAL frame with a distinctive text ("SELF-TEST PING <epoch>") and log whether an echo comes back within N epochs. That's genuinely useful for the two-phone scenario. Hmm — but injecting text into outgoingText interferes with the user's composed message. Keep it simpler: the test log includes "if both phones run this, compare logs". Maybe skip the ping — keep scope tight. Actually a passive check: log marks heard in the test epoch — if the other phone is also testing, its presence frame shows up. Fine.

Let me keep the scope reasonable:
- Section ENV: epoch, ms-into-epoch, config snapshot (epochMs, tau, minHearers, rssiFloor, codedPhy, advInterval, messageRepeatEpochs), build type (debuggable?), versionName.
- Section RADIO: capability report, ext adv capable, coded phy, advertising active, advMode, scanning state.
- Section GATT: server open?, subscribers, peers connected/connecting, tracked peers.
- Section CODEC/CRYPTO (FFI): make message frame, frameDecodes, frameVerifySelf, frameHash != null, frameEpoch == now epoch, witness make/verify (pocpVerifyWitnessLocal against own sketch) — need a sketch: build from measurement.localSketch or synthetic marks via observeMarks.
- Section POCP: observeMarks with synthetic marks → divSketch → witness → verdicts for matching/disjoint sketches (expect Valid/CellMismatch), verdict for empty.
- Section PRIVATE: makePrivateFrame + openPrivateBodyOnly roundtrip with a random pair key (this does a VDL solve — seconds! Hmm. makePrivateFrame solves at production difficulty 22 bits. On a phone ~2-10s. For a self-test that's acceptable if we warn "takes ~10s". Or skip the solve and test open_private_body via crypto directly? The FFI exports makePrivateFrame (with solve) and openPrivateBodyOnly. A roundtrip without solve isn't exposed via FFI. I'll include it — it's the single most valuable end-to-end crypto test — with a log line "VDL solve in progress, may take several seconds".)
- Section DEDUP: fresh/duplicate verdicts with synthetic hashes; epoch decay behavior (insert at epoch-3 → should be purged... actually decay is now local-epoch based; insert with old epoch → check with same hash → duplicate until decay. Keep simple: insert h1 → check h1 == DUPLICATE, check h2 == FRESH).
- Section TRUST: record two dissimilar claims, distinctCount == 2.
- Section BEACON: epoch, isLowEntropy, advance works.
- Section PAIRSTORE: secureStorageAvailable, contacts count (no labels!), per-contact: v2?, chainEpoch vs current epoch lag, keyForSend/candidateKeys roundtrip? keyForSend MUTATES (advances chain + persists). Don't mutate in a test. Instead: for each contact, candidateKeys(contact, currentEpoch) non-empty? That may fast-forward on the fly (no persist — candidateKeys doesn't persist, it's pure read + ephemeral ratchet). Safe-ish. And nextPrivateCounter monotonicity: call twice, check distinct — but that consumes counters... it's fine, it's monotonic by design, two calls is harmless.
- Section MESH STATE: neighborsThisEpoch, totalHeard, local sketch size, rx counters (total, undecodable, bad-sig, skewed, dup, shown, replayed-reject), relay queue depth, messages count.
- Section SYNC: epoch number at start, ms into epoch, wall clock ISO time — so two phones' logs can be aligned.

Wait, the user explicitly wants "waits for two phones to have same epoch and then starts the tests". Implementation: when triggered, compute ms until next epoch boundary; log "self-test armed, waiting Xms for epoch N boundary"; delay until boundary + ~250ms (let epoch-loop settle); then run. On both phones (clocks synced), tests start in the same epoch within ~1s of each other.

Where does the log go? MeshState.logDebug — capped at 200 lines, exportable via existing EXPORT LOG button (MainActivity:127-130 exports debugLog). The self-test will emit ~60-80 lines — fits, but will push out older lines. That's fine; or dedicate a separate `MeshState.selfTestLog = MutableStateFlow<List<String>>` and show it in a dialog with copy/share. Hmm, "generates a full log" — the export path already exists for debugLog. Simplest robust: log to debugLog (exported) AND keep a dedicated selfTestLog shown in a result dialog. Let me keep it simpler: debugLog only, plus a toast "self-test complete — export log to share". Actually a dedicated log is cleaner for "full log of all modules" and doesn't get evicted by the 200-cap mid-run. But then I need UI to display/export it. The Drawer has EXPORT LOG — I could make the export include the self-test section... 

Decision: `MeshState.selfTestLog = MutableStateFlow<List<String>>(emptyList())` + a dialog that shows it with a SHARE button (ACTION_SEND text/plain) — self-contained, no interference with debugLog. Plus mirror a one-line summary into debugLog. Actually — simpler and more consistent with the app: just use debugLog but bump... no. Dedicated log + dialog with share. The app has MainActivity.toast and exports; sharing via Intent is easy in MainActivity.

Hmm, let me look at how EXPORT LOG works to reuse the share mechanism. Let me read MainActivity around 120-135 and Drawer.kt.

Also: the test needs access to MeshService internals (radio, beacon, dedup, trust, gattPlane, seed, cfg). So the runner must live in MeshService (or receive those as params). I'll add to MeshService:

```kotlin
private fun runSelfTest() { ... }  // collects into MeshState.selfTestLog
```
triggered by collecting `MeshState.selfTestRequests` (MutableStateFlow<Int>).

UI: Drawer button "RUN SELF-TEST" → controller.runSelfTest() → MeshState.selfTestRequests.value += 1 (via a controller method implemented in MainActivity) + toast "armed — starts at next epoch boundary". When done: dialog shows the log. How does UI know it's done? MeshState.selfTestRunning = MutableStateFlow<Boolean> and selfTestLog updates; dialog can show live lines. Or simpler: Drawer menu item opens a "SELF-TEST" dialog showing current log + a RUN button inside it. That keeps everything in one place: open dialog → tap RUN → "waiting for epoch boundary…" appears → lines stream in → SHARE button. 

Files to touch:
1. `PairStore.kt` — finish anchor fix (qrPayload + buildContact use session anchor). [in progress]
2. `MeshState.kt` — add selfTestRequests, selfTestLog, selfTestRunning.
3. `MeshService.kt` — collector + runSelfTest() implementation.
4. `MeshUi.kt` — UiController method runSelfTest(); SelfTestDialog composable; Drawer button.
5. `MainActivity.kt` — implement controller.runSelfTest(), plus share intent for the log.
6. `Drawer.kt` — add menu entry (need to read Drawer.kt first).

Let me check Drawer.kt and the export/share mechanism in MainActivity, and UiController interface. Then write code, then build (assembleDebug) to verify compile.

Also I must not forget: finish the anchor fix in buildContact! The qrPayload should use session anchor. And the pairing comment fix.

Regarding the LOCAL banner UX ("stop prompt doesn't go away") — from previous message. Should I also do that? The user moved on to the test button request. I'll do the anchor fix + test button now; mention the banner tweak as a small included change or skip it? Minimal scope: user asked for test button. But the banner copy contradiction is a real UX bug they complained about. It's a 5-line change — include it: after echo heard, banner says "Heard back — still re-airing sparsely (up to 30 min)". Need MeshState.outgoingEchoed flag. I'll fold it in — small and directly answers their complaint. Actually let me keep focus; the anchor fix is the critical one. I'll do the banner tweak too since they explicitly complained about it last message ("that stop prompt doesnt go away"). It's cheap.

Hmm wait — also they asked "how does local chat work? can't the police just flood the local chat cause no witnesses is allowed?" — answer in text (no code).

Let me now:
1. Finish PairStore anchor fix (qrPayload + buildContact).
2. Read Drawer.kt, MainActivity export section, UiController interface region in MeshUi.kt.
3. Implement self-test.
4. Build.

First the PairStore edits.

Good timing — let me first finish the anchor fix (the cause of your private-messages-not-displaying bug), then build the self-test button.

**Tool: edit**

**Input:**
```json
{
  "filePath": "/home/Likun/projects/cockroachat/android/app/src/main/java/org/bileichat/mesh/PairStore.kt",
  "oldString": "    /**\n     * v3 adds the generating epoch. Both phones scan each other, so both end up holding both\n     * epochs and can pick the SAME chain anchor with min(); without it each side anchored on\n     * its own wall clock and the two ratchets never produced a common key (see addContact).\n     */\n    fun qrPayload(ctx: Context): String =\n        \"bileichat:key:v3:${myPublicHex(ctx)}:${myPairSalt(ctx).toHex()}:${currentEpoch()}\"",
  "newString": "    /**\n     * v3 adds the generating epoch. Both phones scan each other, so both end up holding both\n     * epochs and can pick the SAME chain anchor with min(); without it each side anchored on\n     * its own wall clock and the two ratchets never produced a common key (see addContact).\n     * The anchor is the SESSION anchor, not the current clock: the QR must stay stable for\n     * the whole sitting, and [buildContact] needs to know what we advertised.\n     */\n    fun qrPayload(ctx: Context): String =\n        \"bileichat:key:v3:${myPublicHex(ctx)}:${myPairSalt(ctx).toHex()}:${mySessionAnchor ?: currentEpoch()}\""
}
```

**Output:**
```
Edit applied successfully.
```

---

## Assistant (Build · Kimi K3 (2x usage))

---


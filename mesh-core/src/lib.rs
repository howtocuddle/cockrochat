//! mesh-core — the single, shared, security-critical core for the offline protest mesh.
//!
//! NON-NEGOTIABLE INVARIANTS (README.md §7 — enforced in code, see CONTRIBUTING):
//!   1. One codec, in Rust, shared. No parsing in the platform shims.
//!   2. Parse -> verify -> decide, in that order, always. Nothing relayed/rendered pre-validation.
//!   3. Fixed 194 B frame, no compression, no variable fields. Deviation => silent total drop.
//!   4. Danger-only on the wire. Never assert "safe." Silence != safe.
//!   5. Ephemeral keys, minimal persisted state, panic-wipe.
//!   6. Public plane is openly unencrypted — never label it E2E.
//!   7. Trust is per-message physical corroboration, never accumulated to an identity.
//!
//! Platform shims (Kotlin/Swift) own ONLY: BLE radio I/O, OS lifecycle/background, UI,
//! secure key storage, local clock. Everything else lives here.

// Generate the UniFFI scaffolding for this crate (proc-macro mode; no UDL file).
uniffi::setup_scaffolding!();

pub mod beacon;
pub mod codec;
pub mod crypto;
pub mod ffi;
pub mod pocp;
pub mod radio;
pub mod statemachine;
pub mod store;
pub mod trust;

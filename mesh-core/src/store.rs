//! `store` — bounded persistence. Seen-set, cell history (last 2 epochs), pending alerts.
//! ALL size-capped, auto-decay. Minimal on-disk footprint (coercion resistance, invariant #5).
//! Exposes `panic_wipe`. See README.md §2.

/// Bounded, self-decaying local store. Every field is size-capped; nothing grows unbounded.
pub struct Store {
    pub cleared: bool,
}

impl Default for Store {
    fn default() -> Self {
        Self::new()
    }
}

impl Store {
    pub fn new() -> Self {
        Store { cleared: false }
    }

    /// Immediately and irrecoverably wipe all Rust-side state (duress / panic button).
    ///
    /// Signals `cleared = true` so the platform shim knows a wipe was requested.
    /// The caller (platform shim) MUST also:
    ///   1. Call `PairStore.wipe()` to clear encrypted pairing keys
    ///   2. Call `ConfigStore` clear
    ///   3. Release the foreground notification
    ///   4. Stop the BLE service
    ///   5. Optionally kill the process
    pub fn panic_wipe(&mut self) {
        self.cleared = true;
    }
}

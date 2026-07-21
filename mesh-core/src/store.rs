//! `store` — bounded persistence. Seen-set, cell history (last 2 epochs), pending alerts.
//! ALL size-capped, auto-decay. Minimal on-disk footprint (coercion resistance, invariant #5).
//! Exposes `panic_wipe`. See mesh-build-plan.md §2.7.

/// Bounded, self-decaying local store. Every field is size-capped; nothing grows unbounded.
pub struct Store {
    _private: (),
}

impl Store {
    /// Immediately and irrecoverably wipe all persisted state (duress / panic button).
    pub fn panic_wipe(&mut self) {
        todo!("M4")
    }
}

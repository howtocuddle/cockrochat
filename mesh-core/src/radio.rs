//! `radio` — the BLE transport seam. Implemented by each platform shim (Kotlin/Swift); the
//! core never touches the radio directly. Transport = BLE 5 extended advertising, connectionless,
//! one 194 B AUX PDU per frame, Coded PHY on the frontier for range. See README.md §6.

use crate::codec::FRAME_LEN;

/// Scan callback: raw frame bytes + RSSI, handed up to the state machine (parse/verify/decide).
pub type ScanCallback = Box<dyn Fn(&[u8; FRAME_LEN], i8) + Send + 'static>;

/// The radio port the platform implements. Delivers raw frames + RSSI up to the state machine.
pub trait RadioPort {
    /// Advertise one frame (extended adv set, non-connectable preferred; see iOS bg caveat §3.2).
    fn advertise(&self, frame: &[u8; FRAME_LEN]);

    /// Register a scan callback receiving raw frame bytes + RSSI. Core does parse/verify/decide.
    fn on_scan(&self, cb: ScanCallback);

    /// Set scan/sleep duty cycle (battery vs latency trade-off).
    fn set_duty(&self, scan_ms: u32, sleep_ms: u32);
}

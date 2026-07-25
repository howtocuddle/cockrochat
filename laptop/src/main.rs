// bileichat-laptop — Linux laptop BLE mesh node.
//
// Advertises own frame as BLE extended advertising (SecondaryChannel::OneM)
// and scans for peer frames.  ALL frame origination and parsing goes through
// mesh-core — no hand-parsing of bytes anywhere in this file.

use bluer::{
    adv::{Advertisement, AdvertisementHandle, SecondaryChannel, Type as AdvType},
    AdapterEvent, DeviceEvent, DeviceProperty, DiscoveryFilter, DiscoveryTransport,
};
use chrono::Local;
use clap::Parser;
use futures::{pin_mut, stream::SelectAll, StreamExt};
use mesh_core::{
    codec::MsgType,
    message::{body_text, frame_hash, make_message_frame},
    pocp,
    statemachine::Dedup,
};
use std::{
    collections::{BTreeMap, BTreeSet, HashSet},
    time::Duration,
};
use tokio::{
    io::{AsyncBufReadExt, BufReader},
    signal,
    sync::{mpsc, Mutex},
};
use uuid::Uuid;

// ─── Protocol UUID ────────────────────────────────────────────────────────────
const MESH_UUID_STR: &str = "6c6f6361-6c6d-4573-6800-000000000001";

// ─── CLI ──────────────────────────────────────────────────────────────────────
#[derive(Parser, Debug)]
#[command(
    name = "bileichat-laptop",
    about = "BileiChat BLE mesh node (Linux laptop)"
)]
struct Args {
    /// Epoch length in milliseconds — MUST match phone default (10000)
    #[arg(long, default_value_t = 10000)]
    epoch_ms: u64,

    /// RSSI floor (dBm) for KMV sketch: neighbours below this are excluded
    #[arg(long, default_value_t = -80_i8)]
    rssi_floor: i8,

    /// Initial outgoing message text (max 63 bytes UTF-8)
    #[arg(long, default_value = "hello from laptop")]
    text: String,
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

fn now_unix_ms() -> u64 {
    std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap_or_default()
        .as_millis() as u64
}

fn current_epoch(epoch_ms: u64) -> u32 {
    (now_unix_ms() / epoch_ms) as u32
}

/// Build a bluer Advertisement carrying our frame as service data.
///
/// NOTE: `service_uuids` is deliberately omitted.  The service_data AD type
/// (0x24 for 128-bit UUID) already contains the full UUID, so duplicating it
/// in a separate UUID-list AD would cost 18 bytes — pushing the total packet
/// past the controller's MaxAdvLen of 251 bytes (226 B frame + AD framing =
/// 247 B, barely under).  Android's ScanFilter.setServiceData matches against
/// the service_data AD, not the UUID list, so this is transparent on the
/// phone side.  The laptop scanner's DiscoveryFilter.uuids is kept for
/// optional software pre-filtering.
fn make_advertisement(mesh_uuid: Uuid, frame_bytes: &[u8; 226]) -> Advertisement {
    let mut service_data = BTreeMap::new();
    service_data.insert(mesh_uuid, frame_bytes.to_vec());

    Advertisement {
        advertisement_type: AdvType::Broadcast,
        service_uuids: BTreeSet::new(),
        service_data,
        // SecondaryChannel::OneM instructs BlueZ / the controller to use an
        // extended (non-legacy) advertising PDU.  This is what the Android
        // scanner's setLegacy(false) requires, and it is the only way to fit
        // 226 bytes of service data into a single advertising packet.
        secondary_channel: Some(SecondaryChannel::OneM),
        // ~1-second interval.
        min_interval: Some(Duration::from_millis(1000)),
        max_interval: Some(Duration::from_millis(1020)),
        ..Default::default()
    }
}

/// Hex prefix: first 8 hex digits of a 16-byte array.
fn hex8(bytes: &[u8; 16]) -> String {
    bytes[..4]
        .iter()
        .fold(String::with_capacity(8), |mut s, b| {
            use std::fmt::Write;
            write!(s, "{b:02x}").unwrap();
            s
        })
}

// ─── Epoch observation row ────────────────────────────────────────────────────

struct NeighbourRow {
    /// The epoch field FROM THE FRAME, not arrival time — the phone buckets heard marks by
    /// frame epoch, so the laptop must too or boundary-straddling frames skew the τ comparison.
    epoch: u32,
    mark: [u8; 16],
    rssi: i8,
}

// ─── Main ─────────────────────────────────────────────────────────────────────

#[tokio::main(flavor = "multi_thread")]
async fn main() -> anyhow::Result<()> {
    let args = Args::parse();

    // Validate initial text up-front.
    if args.text.len() > 63 {
        eprintln!(
            "ERROR: --text is {} bytes; max is 63 bytes UTF-8",
            args.text.len()
        );
        std::process::exit(1);
    }

    // Random 32-byte device seed.
    let mut seed = [0u8; 32];
    getrandom::getrandom(&mut seed).expect("OS CSPRNG unavailable");

    // BlueZ session — kept alive for the whole run.
    let session = bluer::Session::new().await?;
    let adapter = session.default_adapter().await?;
    adapter.set_powered(true).await?;

    let powered = adapter.is_powered().await?;
    println!("=== bileichat-laptop ===");
    println!("Adapter : {}  powered={}", adapter.name(), powered);
    println!("Epoch ms: {}", args.epoch_ms);
    println!("RSSI floor: {} dBm", args.rssi_floor);
    println!("Initial text: {:?}", args.text);
    println!("Seed: {} (first 8 hex)", hex8(&seed[..16].try_into().unwrap()));
    println!();

    let mesh_uuid: Uuid = MESH_UUID_STR.parse().expect("hardcoded UUID is valid");
    let epoch_ms = args.epoch_ms;
    let rssi_floor = args.rssi_floor;

    // ── Channel: stdin sends new text strings to the adv-manager task ─────────
    // Buffer=1: if the adv-manager hasn't consumed yet, the next stdin write
    // will block briefly — acceptable for an interactive tool.
    let (text_tx, mut text_rx) = mpsc::channel::<String>(4);

    // ── Dedup set (capacity 4096) ─────────────────────────────────────────────
    let dedup = std::sync::Arc::new(Mutex::new(Dedup::new(4096)));

    // ── Per-epoch neighbour observations ─────────────────────────────────────
    let epoch_obs: std::sync::Arc<Mutex<Vec<NeighbourRow>>> =
        std::sync::Arc::new(Mutex::new(Vec::new()));

    // ── Advertisement-manager task ────────────────────────────────────────────
    // Owns the AdvertisementHandle.  Reacts to epoch rollovers and text changes.
    let adv_adapter = session.default_adapter().await?;
    let obs_for_ticker = epoch_obs.clone();
    let adv_task = {
        let initial_text = args.text.clone();
        tokio::spawn(async move {
            let mut current_text = initial_text;
            let mut last_epoch = current_epoch(epoch_ms);
            let mut handle: Option<AdvertisementHandle> = None;

            // Register first advertisement.
            handle = register_adv(
                &adv_adapter,
                mesh_uuid,
                &seed,
                last_epoch,
                &current_text,
                handle,
            )
            .await;

            loop {
                // How long until the next epoch boundary?
                let ms_into = now_unix_ms() % epoch_ms;
                let sleep_ms = epoch_ms - ms_into;

                tokio::select! {
                    // ── Epoch rollover ─────────────────────────────────────
                    () = tokio::time::sleep(Duration::from_millis(sleep_ms)) => {
                        let new_epoch = current_epoch(epoch_ms);
                        if new_epoch <= last_epoch {
                            // Slept less than needed; will retry next iteration.
                            continue;
                        }

                        // Take rows belonging to the just-ended epoch (by FRAME epoch, matching
                        // the phone's bucketing); keep newer rows for the next rollover, discard older.
                        let rows: Vec<NeighbourRow> = {
                            let mut obs = obs_for_ticker.lock().await;
                            let (ended, rest): (Vec<_>, Vec<_>) =
                                std::mem::take(&mut *obs).into_iter().partition(|r| r.epoch == last_epoch);
                            *obs = rest.into_iter().filter(|r| r.epoch > last_epoch).collect();
                            ended
                        };

                        // KMV sketch (seed = shared epoch number for cross-device comparability).
                        let marks: Vec<[u8; 16]> = rows.iter().map(|r| r.mark).collect();
                        let rssis: Vec<i8> = rows.iter().map(|r| r.rssi).collect();
                        let distinct = rows.iter().map(|r| r.mark).collect::<BTreeSet<_>>().len();
                        let sketch = pocp::observe(&marks, &rssis, last_epoch, rssi_floor);
                        let sketch_str: Vec<String> =
                            sketch.0.iter().map(|v| v.to_string()).collect();
                        println!(
                            "[epoch {last_epoch}] ended — {distinct} distinct neighbours heard | KMV: {}",
                            sketch_str.join(" ")
                        );

                        last_epoch = new_epoch;

                        // Re-register with rotated epoch (mark + ephemeral key change).
                        handle = register_adv(
                            &adv_adapter,
                            mesh_uuid,
                            &seed,
                            new_epoch,
                            &current_text,
                            handle,
                        )
                        .await;
                    },

                    // ── New text from stdin ────────────────────────────────
                    Some(new_text) = text_rx.recv() => {
                        current_text = new_text;
                        let epoch = current_epoch(epoch_ms);
                        handle = register_adv(
                            &adv_adapter,
                            mesh_uuid,
                            &seed,
                            epoch,
                            &current_text,
                            handle,
                        )
                        .await;
                    },
                }
            }
        })
    };

    // ── Scan task ─────────────────────────────────────────────────────────────
    let scan_adapter = session.default_adapter().await?;
    let scan_dedup = dedup.clone();
    let scan_obs = epoch_obs.clone();
    let scan_task = tokio::spawn(async move {
        let filter = DiscoveryFilter {
            transport: DiscoveryTransport::Le,
            duplicate_data: true, // re-report same device on ServiceData change
            uuids: {
                let mut s = HashSet::new();
                s.insert(mesh_uuid);
                s
            },
            ..Default::default()
        };

        if let Err(e) = scan_adapter.set_discovery_filter(filter).await {
            eprintln!(
                "WARNING: set_discovery_filter failed: {e}  (continuing without filter)"
            );
        }

        let device_events = match scan_adapter.discover_devices().await {
            Ok(s) => s,
            Err(e) => {
                eprintln!("ERROR: discover_devices failed: {e:#}");
                return;
            }
        };
        pin_mut!(device_events);

        // Change-event streams: one per discovered device, multiplexed.
        let mut change_streams: SelectAll<_> = SelectAll::new();

        loop {
            tokio::select! {
                Some(evt) = device_events.next() => {
                    if let AdapterEvent::DeviceAdded(addr) = evt {
                        if let Ok(dev) = scan_adapter.device(addr) {
                            // Subscribe to property changes.
                            if let Ok(stream) = dev.events().await {
                                change_streams.push(stream.map(move |e| (addr, e)));
                            }
                            // Also check service data that may already be present.
                            if let Ok(Some(sdata)) = dev.service_data().await {
                                if let Some(raw) = sdata.get(&mesh_uuid) {
                                    let rssi = dev.rssi().await.ok().flatten();
                                    on_frame(raw, rssi, &scan_dedup, &scan_obs, rssi_floor).await;
                                }
                            }
                        }
                    }
                },
                Some((addr, DeviceEvent::PropertyChanged(prop))) = change_streams.next() => {
                    if let DeviceProperty::ServiceData(sdata) = prop {
                        if let Some(raw) = sdata.get(&mesh_uuid) {
                            let rssi = match scan_adapter.device(addr) {
                                Ok(d) => d.rssi().await.ok().flatten(),
                                Err(_) => None,
                            };
                            on_frame(raw, rssi, &scan_dedup, &scan_obs, rssi_floor).await;
                        }
                    }
                },
                else => {
                    eprintln!("WARNING: scan event loop ended unexpectedly");
                    break;
                }
            }
        }
    });

    // ── Stdin task ────────────────────────────────────────────────────────────
    let stdin_task = tokio::spawn(async move {
        let stdin = BufReader::new(tokio::io::stdin());
        let mut lines = stdin.lines();
        while let Ok(Some(line)) = lines.next_line().await {
            let trimmed = line.trim().to_string();
            if trimmed.len() > 63 {
                eprintln!(
                    "ERROR: input is {} bytes; max 63 bytes UTF-8 — keeping previous text",
                    trimmed.len()
                );
                continue;
            }
            println!("[stdin] new outgoing text: {trimmed:?}");
            if text_tx.send(trimmed).await.is_err() {
                break;
            }
        }
    });

    // ── Wait for Ctrl-C ───────────────────────────────────────────────────────
    signal::ctrl_c().await?;
    println!("\nCtrl-C received — shutting down...");

    // Abort background tasks and wait briefly.
    adv_task.abort();
    scan_task.abort();
    stdin_task.abort();

    // The AdvertisementHandle is owned by adv_task; aborting it drops the
    // handle which triggers BlueZ unregistration via the oneshot in bluer.
    // Give BlueZ a moment to process the unregister call.
    tokio::time::sleep(Duration::from_millis(300)).await;
    println!("Done.");

    Ok(())
}

// ─── Re-register advertisement helper ────────────────────────────────────────
//
// Drops the old handle first (triggers BlueZ UnregisterAdvertisement), then
// registers the new one.  Any error is printed prominently and Ok(None) is
// returned so the caller can still continue running.
async fn register_adv(
    adapter: &bluer::Adapter,
    mesh_uuid: Uuid,
    seed: &[u8; 32],
    epoch: u32,
    text: &str,
    old_handle: Option<AdvertisementHandle>,
) -> Option<AdvertisementHandle> {
    // Drop old advertisement first.
    drop(old_handle);

    let frame = match make_message_frame(seed, epoch, seed, MsgType::RegionalPropagated, text) {
        Some(f) => f,
        None => {
            eprintln!("ERROR: make_message_frame returned None — text too long? ({} bytes)", text.len());
            return None;
        }
    };

    let adv = make_advertisement(mesh_uuid, &frame);
    match adapter.advertise(adv).await {
        Ok(h) => {
            println!("[adv] epoch={epoch} text={text:?} registered OK");
            Some(h)
        }
        Err(e) => {
            eprintln!("!!! ADVERTISEMENT REGISTRATION FAILED: {e:#}");
            eprintln!("!!! Checklist:");
            eprintln!("!!!   • bluetoothd ≥ 5.65 running?  (systemctl status bluetooth)");
            eprintln!("!!!   • adapter supports extended advertising?  (btmgmt info)");
            eprintln!("!!!   • running as root or with CAP_NET_ADMIN + CAP_NET_RAW?");
            None
        }
    }
}

// ─── Process one received frame ───────────────────────────────────────────────
async fn on_frame(
    raw: &[u8],
    rssi: Option<i16>,
    dedup: &Mutex<Dedup>,
    epoch_obs: &Mutex<Vec<NeighbourRow>>,
    rssi_floor: i8,
) {
    // Must be exactly 226 bytes — invariant #3: any deviation is a silent drop.
    let buf: [u8; 226] = match raw.try_into() {
        Ok(b) => b,
        Err(_) => return,
    };

    // Decode via mesh-core — invariant #1: one codec.
    let frame = match mesh_core::codec::decode(&buf) {
        Ok(f) => f,
        Err(_) => return,
    };

    // Dedup by frame hash (epoch-aware time-decaying eviction, E4).
    let hash = frame_hash(&buf);
    if !dedup.lock().await.check_and_insert_epoch(hash, frame.epoch) {
        return;
    }

    // Convert bluer's i16 RSSI to i8 (clamped).
    let rssi_i8: i8 = rssi
        .map(|r| r.clamp(i8::MIN as i16, i8::MAX as i16) as i8)
        .unwrap_or(rssi_floor);

    // Record in per-epoch observations (only if above RSSI floor).
    if rssi_i8 >= rssi_floor {
        epoch_obs
            .lock()
            .await
            .push(NeighbourRow { epoch: frame.epoch, mark: frame.mark, rssi: rssi_i8 });
    }

    // Print receive log line.
    let ts = Local::now().format("%H:%M:%S%.3f");
    let rssi_str = rssi
        .map(|r| format!("{r} dBm"))
        .unwrap_or_else(|| "? dBm".to_string());
    let mark_str = hex8(&frame.mark);
    let text_str = body_text(&frame).unwrap_or("<no text>");
    println!(
        "[{ts}] rssi={rssi_str} mark={mark_str} epoch={} text={text_str:?}",
        frame.epoch
    );
}

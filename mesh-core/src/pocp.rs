//! `pocp` — Proof-of-Co-Presence. Cell digest (fuzzy KMV over overheard marks) + spacetime
//! witness. Blocks the remote-van flood: you cannot forge co-presence you did not physically
//! observe. See mesh-build-plan.md §2.3. `tau` is MEASURED (RF-overlap rig §5), never guessed.

use std::collections::BTreeSet;

pub const KMV_K: usize = 16;

/// KMV sketch over truncated overheard marks within an RSSI window — one physical "cell".
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct CellSketch(pub [u64; 16]);

/// Result of checking a received witness against the locally-observed cell.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum WitVerdict {
    Valid,
    /// Cell does not match local observation => relocation/replay attack. Raise F4 ALARM.
    CellMismatch,
    Stale,
}

fn cell_key(seed: u32) -> [u8; 32] {
    blake3::derive_key("mesh-core:v1:pocp-cell", &seed.to_le_bytes())
}

fn mark_hash(key: &[u8; 32], mark: &[u8; 16]) -> u64 {
    let h = blake3::keyed_hash(key, mark);
    let b = h.as_bytes();
    u64::from_le_bytes(b[0..8].try_into().unwrap())
}

/// Build a cell sketch from marks overheard this epoch, RSSI-windowed and seed-bound.
pub fn observe(marks: &[[u8; 16]], rssi: &[i8], seed: u32, rssi_floor_dbm: i8) -> CellSketch {
    let key = cell_key(seed);
    let mut set: BTreeSet<u64> = BTreeSet::new();
    for (mark, r) in marks.iter().zip(rssi.iter()) {
        if *r < rssi_floor_dbm {
            continue;
        }
        set.insert(mark_hash(&key, mark));
    }
    let mut arr = [u64::MAX; 16];
    for (i, v) in set.iter().take(KMV_K).enumerate() {
        arr[i] = *v;
    }
    CellSketch(arr)
}

/// Jaccard similarity of two cell sketches in [0,1].
pub fn jaccard(a: &CellSketch, b: &CellSketch) -> f32 {
    let set_a: BTreeSet<u64> = a.0.iter().copied().filter(|v| *v != u64::MAX).collect();
    let set_b: BTreeSet<u64> = b.0.iter().copied().filter(|v| *v != u64::MAX).collect();
    let union: Vec<u64> = set_a.union(&set_b).copied().collect();
    if union.is_empty() {
        return 0.0;
    }
    let x_len = KMV_K.min(union.len());
    let x = &union[..x_len];
    let inter_in_x = x
        .iter()
        .filter(|v| set_a.contains(v) && set_b.contains(v))
        .count();
    inter_in_x as f32 / x_len as f32
}

/// Fuzzy cell match at measured threshold `tau`.
pub fn matches(a: &CellSketch, b: &CellSketch, tau: f32) -> bool {
    jaccard(a, b) >= tau
}

/// Spacetime witness: MAC_{KDF(cell || seed)} over the canonical message.
pub fn witness(_cell: &CellSketch, _seed: u32, _msg_canonical: &[u8]) -> [u8; 16] {
    todo!("M5")
}

/// Verify a received witness against the local cell at threshold `tau`.
pub fn verify_witness_local(
    _local: &CellSketch,
    _seed: u32,
    _msg: &[u8],
    _wit: &[u8; 16],
    _tau: f32,
) -> WitVerdict {
    todo!("M5")
}

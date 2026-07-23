package org.cockroachat.mesh

import org.json.JSONArray
import org.json.JSONObject
import uniffi.mesh_core.observeMarks
import java.util.concurrent.ConcurrentHashMap

data class Heard(
    val epoch: UInt,
    val markHex: String,
    val rssi: Int,
    val tsMs: Long
)

class Measurement {
    private val rowsLock = Any()

    // All raw rows, in insertion order (guarded by rowsLock)
    private val rows = ArrayDeque<Heard>()

    // Per-epoch set of distinct mark hex strings for fast neighbor counting
    private val epochMarks = ConcurrentHashMap<UInt, MutableSet<String>>()

    // Continuously-refreshed presence: updated on EVERY heard frame (before dedup), unlike
    // `rows`/`epochMarks` which are gated by dedup and feed the KMV sketch. Bounds memory.
    private val presence = ConcurrentHashMap<String, Pair<Int, Long>>()

    private companion object {
        const val MAX_ROWS = 4000
        const val MAX_EPOCHS = 32
        const val MAX_PRESENCE = 256
    }

    fun record(mark: ByteArray, rssi: Int, epoch: UInt) {
        val hex = mark.joinToString("") { "%02x".format(it) }
        val row = Heard(epoch, hex, rssi, System.currentTimeMillis())
        synchronized(rowsLock) {
            rows.addLast(row)
            while (rows.size > MAX_ROWS) rows.removeFirst()
        }
        epochMarks.computeIfAbsent(epoch) {
            java.util.Collections.synchronizedSet(HashSet<String>())
        }.add(hex)
        if (epochMarks.size > MAX_EPOCHS) {
            epochMarks.keys.minOrNull()?.let { epochMarks.remove(it) }
        }
    }

    /** Record a heard mark for presence. Called on every fresh in-window frame, before dedup. */
    fun recordPresence(mark: ByteArray, rssi: Int) {
        val hex = mark.joinToString("") { "%02x".format(it) }
        presence[hex] = rssi to System.currentTimeMillis()
        if (presence.size > MAX_PRESENCE) {
            presence.entries.minByOrNull { it.value.second }?.key?.let { presence.remove(it) }
        }
    }

    fun neighborsThisEpoch(epoch: UInt): Int {
        return epochMarks[epoch]?.size ?: 0
    }

    /**
     * Number of distinct rotating marks heard on DIRECT RF within a recent wall-clock window.
     *
     * Deliberately separate from [neighborsThisEpoch] (epoch buckets feed the KMV rig) and
     * deliberately NOT RSSI-filtered: any frame that decoded and verified is a real
     * transmission. The −80 dBm config floor is a *sketch/trust* window, not a liveness
     * window — applying it here made the count flicker whenever a peer's RSSI crossed the
     * boundary, while its messages kept flowing. Relayed copies are excluded upstream
     * (only undecremented-TTL frames reach [recordPresence]).
     */
    fun neighborsRecently(windowMs: Long): Int {
        val cutoff = System.currentTimeMillis() - windowMs
        return presence.count { (_, v) -> v.second >= cutoff }
    }

    fun totalHeard(): Int = synchronized(rowsLock) { rows.size }

    fun localSketch(epoch: UInt, seed: ByteArray, floorDbm: Int): List<ULong> {
        // Collect rows for this epoch
        val epochRows = synchronized(rowsLock) { rows.filter { it.epoch == epoch } }
        if (epochRows.isEmpty()) return emptyList()

        val marksFlat = epochRows.flatMap { row ->
            // decode hex back to 16 bytes
            (row.markHex.chunked(2).map { it.toInt(16).toByte() })
        }.toByteArray()

        val rssiList: List<Byte> = epochRows.map { it.rssi.toByte() }

        // The KMV seed MUST be a value all co-located devices agree on, so the SAME overheard mark
        // hashes to the SAME u64 on every phone — otherwise Jaccard is meaningless. The epoch is that
        // shared value. The device's private `seed` (its advertising identity) must NOT be used here.
        val sketchSeed: UInt = epoch

        return observeMarks(marksFlat, rssiList, sketchSeed, floorDbm.toByte())
    }

    fun exportJson(cfg: MeshConfig): String {
        val root = JSONObject()

        val cfgObj = JSONObject()
        cfgObj.put("epochMs", cfg.epochMs)
        cfgObj.put("tauThreshold", cfg.tauThreshold)
        cfgObj.put("rssiFloorDbm", cfg.rssiFloorDbm)
        cfgObj.put("codedPhy", cfg.codedPhy)
        cfgObj.put("advIntervalMs", cfg.advIntervalMs)
        root.put("config", cfgObj)

        val arr = JSONArray()
        val snapshot = synchronized(rowsLock) { rows.toList() }
        for (row in snapshot) {
            val obj = JSONObject()
            obj.put("epoch", row.epoch.toLong())
            obj.put("markHex", row.markHex)
            obj.put("rssi", row.rssi)
            obj.put("tsMs", row.tsMs)
            arr.put(obj)
        }
        root.put("heard", arr)

        return root.toString(2)
    }

    fun clear() {
        synchronized(rowsLock) { rows.clear() }
        epochMarks.clear()
        presence.clear()
    }
}

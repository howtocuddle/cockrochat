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

    // Direct-RF presence: marks heard at their origination TTL (relays excluded upstream),
    // bucketed by the frame's epoch. Marks rotate every epoch, so the previous wall-clock
    // window counted one physical phone 2–3 times (2–3 of its rotating marks in window).
    // Per-epoch buckets + max (NOT sum) cannot double-count: one device = one mark per
    // epoch. Relayed copies never land here (TTL-direct gate at the call site), so a
    // device two hops away is not "nearby".
    private val directMarks = ConcurrentHashMap<UInt, MutableSet<String>>()

    private companion object {
        const val MAX_ROWS = 4000
        const val MAX_EPOCHS = 32
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

    /** Record a DIRECT-RF mark for presence. Called before dedup, only for frames at
     *  their origination TTL (relays excluded). Bucketed by the frame's own epoch. */
    fun recordPresence(mark: ByteArray, epoch: UInt) {
        val hex = mark.joinToString("") { "%02x".format(it) }
        directMarks.computeIfAbsent(epoch) {
            java.util.Collections.synchronizedSet(HashSet<String>())
        }.add(hex)
        if (directMarks.size > MAX_EPOCHS) {
            directMarks.keys.minOrNull()?.let { directMarks.remove(it) }
        }
    }

    fun neighborsThisEpoch(epoch: UInt): Int {
        return epochMarks[epoch]?.size ?: 0
    }

    /**
     * Estimated nearby devices: max (not sum) of the direct-RF mark counts of the current
     * and adjacent epoch buckets. Adjacent buckets cover sender/receiver epoch skew and
     * one fully-missed epoch; zero requires two consecutive silent epochs (~20 s), the
     * same smoothing horizon as before but rotation-proof. Deliberately not RSSI-filtered:
     * any frame that decoded and verified is a real transmission — the −80 dBm config
     * floor is a sketch/trust window, not a liveness window.
     */
    fun neighborsDirect(epoch: UInt): Int {
        // epoch-1 wraps to UInt.MAX_VALUE at epoch 0; that bucket never exists → 0.
        return maxOf(
            directMarks[epoch]?.size ?: 0,
            directMarks[epoch - 1u]?.size ?: 0,
            directMarks[epoch + 1u]?.size ?: 0
        )
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

        // D6: mark sightings are RF-proximity evidence — say so inside the file itself.
        root.put(
            "warning",
            "CONTAINS RF-PROXIMITY DATA: mark sightings reveal which devices were " +
                "physically near this phone and when. Handle like location history."
        )

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
        directMarks.clear()
    }
}

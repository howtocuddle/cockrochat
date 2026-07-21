package org.cockroachat.mesh

import org.json.JSONArray
import org.json.JSONObject
import uniffi.mesh_core.observeMarks
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

data class Heard(
    val epoch: UInt,
    val markHex: String,
    val rssi: Int,
    val tsMs: Long
)

class Measurement {
    // All raw rows, in insertion order
    private val rows = CopyOnWriteArrayList<Heard>()

    // Per-epoch set of distinct mark hex strings for fast neighbor counting
    private val epochMarks = ConcurrentHashMap<UInt, MutableSet<String>>()

    fun record(mark: ByteArray, rssi: Int, epoch: UInt) {
        val hex = mark.joinToString("") { "%02x".format(it) }
        val row = Heard(epoch, hex, rssi, System.currentTimeMillis())
        rows.add(row)
        // ConcurrentHashMap.computeIfAbsent is not available for all ConcurrentHashMap
        // but putIfAbsent + get is safe here
        epochMarks.getOrPut(epoch) {
            // use a synchronized set to be safe
            java.util.Collections.synchronizedSet(mutableSetOf())
        }.add(hex)
    }

    fun neighborsThisEpoch(epoch: UInt): Int {
        return epochMarks[epoch]?.size ?: 0
    }

    fun totalHeard(): Int = rows.size

    fun localSketch(epoch: UInt, seed: ByteArray, floorDbm: Int): List<ULong> {
        // Collect rows for this epoch
        val epochRows = rows.filter { it.epoch == epoch }
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
        for (row in rows) {
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
        rows.clear()
        epochMarks.clear()
    }
}

package org.cockroachat.mesh

import android.content.Context

data class MeshConfig(
    val epochMs: Long = 10_000L,
    /** Minimum ms between beacon chain advances (acceleration cap). Default 240s real, 10s rig. */
    val beaconFloorMs: Long = 10_000L,
    /** Minimum distinct LocalImmediate marks required to produce beacon entropy. */
    val minHearers: Int = 3,
    val tauThreshold: Float = 0.5f,
    val rssiFloorDbm: Int = -80,
    val codedPhy: Boolean = true,
    val advIntervalMs: Long = 1000L,
    val scanLowLatency: Boolean = true,
    val messageRepeatEpochs: Int = 3
)

object ConfigStore {
    private const val PREFS_NAME = "mesh_cfg"
    private const val KEY_EPOCH_MS = "epochMs"
    private const val KEY_BEACON_FLOOR_MS = "beaconFloorMs"
    private const val KEY_MIN_HEARERS = "minHearers"
    private const val KEY_TAU = "tauThreshold"
    private const val KEY_RSSI_FLOOR = "rssiFloorDbm"
    private const val KEY_CODED_PHY = "codedPhy"
    private const val KEY_ADV_INTERVAL = "advIntervalMs"
    private const val KEY_SCAN_LOW_LATENCY = "scanLowLatency"
    private const val KEY_MESSAGE_REPEAT_EPOCHS = "messageRepeatEpochs"

    fun load(ctx: Context): MeshConfig {
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return MeshConfig(
            epochMs = prefs.getLong(KEY_EPOCH_MS, 10_000L),
            beaconFloorMs = prefs.getLong(KEY_BEACON_FLOOR_MS, 10_000L),
            minHearers = prefs.getInt(KEY_MIN_HEARERS, 3),
            tauThreshold = prefs.getFloat(KEY_TAU, 0.5f),
            rssiFloorDbm = prefs.getInt(KEY_RSSI_FLOOR, -80),
            codedPhy = prefs.getBoolean(KEY_CODED_PHY, true),
            advIntervalMs = prefs.getLong(KEY_ADV_INTERVAL, 1000L),
            scanLowLatency = prefs.getBoolean(KEY_SCAN_LOW_LATENCY, true),
            messageRepeatEpochs = prefs.getInt(KEY_MESSAGE_REPEAT_EPOCHS, 3)
        )
    }

    fun save(ctx: Context, cfg: MeshConfig) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_EPOCH_MS, cfg.epochMs)
            .putLong(KEY_BEACON_FLOOR_MS, cfg.beaconFloorMs)
            .putInt(KEY_MIN_HEARERS, cfg.minHearers)
            .putFloat(KEY_TAU, cfg.tauThreshold)
            .putInt(KEY_RSSI_FLOOR, cfg.rssiFloorDbm)
            .putBoolean(KEY_CODED_PHY, cfg.codedPhy)
            .putLong(KEY_ADV_INTERVAL, cfg.advIntervalMs)
            .putBoolean(KEY_SCAN_LOW_LATENCY, cfg.scanLowLatency)
            .putInt(KEY_MESSAGE_REPEAT_EPOCHS, cfg.messageRepeatEpochs)
            .apply()
    }

    fun clear(ctx: Context) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }
}

package org.cockroachat.mesh

import android.content.Context

data class MeshConfig(
    val epochMs: Long = 10_000L,
    val tauThreshold: Float = 0.5f,
    val rssiFloorDbm: Int = -80,
    val codedPhy: Boolean = true,
    val advIntervalMs: Long = 1000L
)

object ConfigStore {
    private const val PREFS_NAME = "mesh_cfg"
    private const val KEY_EPOCH_MS = "epochMs"
    private const val KEY_TAU = "tauThreshold"
    private const val KEY_RSSI_FLOOR = "rssiFloorDbm"
    private const val KEY_CODED_PHY = "codedPhy"
    private const val KEY_ADV_INTERVAL = "advIntervalMs"

    fun load(ctx: Context): MeshConfig {
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return MeshConfig(
            epochMs = prefs.getLong(KEY_EPOCH_MS, 10_000L),
            tauThreshold = prefs.getFloat(KEY_TAU, 0.5f),
            rssiFloorDbm = prefs.getInt(KEY_RSSI_FLOOR, -80),
            codedPhy = prefs.getBoolean(KEY_CODED_PHY, true),
            advIntervalMs = prefs.getLong(KEY_ADV_INTERVAL, 1000L)
        )
    }

    fun save(ctx: Context, cfg: MeshConfig) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_EPOCH_MS, cfg.epochMs)
            .putFloat(KEY_TAU, cfg.tauThreshold)
            .putInt(KEY_RSSI_FLOOR, cfg.rssiFloorDbm)
            .putBoolean(KEY_CODED_PHY, cfg.codedPhy)
            .putLong(KEY_ADV_INTERVAL, cfg.advIntervalMs)
            .apply()
    }
}

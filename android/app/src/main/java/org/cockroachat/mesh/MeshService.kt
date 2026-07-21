package org.cockroachat.mesh

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import uniffi.mesh_core.frameDecodes
import uniffi.mesh_core.frameEpoch
import uniffi.mesh_core.frameMark
import uniffi.mesh_core.makeTestFrame
import java.security.SecureRandom

class MeshService : LifecycleService() {

    private companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "mesh"
        const val CHANNEL_NAME = "Mesh BLE"
    }

    private lateinit var seed: ByteArray
    private lateinit var radio: BleRadio

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        startForeground(NOTIFICATION_ID, buildNotification("Starting…"))

        MeshState.running.value = true

        seed = ByteArray(32).also { SecureRandom().nextBytes(it) }
        radio = BleRadio(this)

        // Start scanning immediately
        radio.startScanning { bytes, rssi ->
            if (frameDecodes(bytes)) {
                val mark = frameMark(bytes)
                val epoch = frameEpoch(bytes)
                if (mark != null && epoch != null) {
                    MeshState.measurement.record(mark, rssi, epoch)
                }
            }
        }

        // Epoch loop
        lifecycleScope.launch {
            var lastEpoch = UInt.MAX_VALUE
            while (isActive) {
                val cfg = MeshState.config
                val nowMs = System.currentTimeMillis()
                val epoch = (nowMs / cfg.epochMs).toUInt()

                if (epoch != lastEpoch) {
                    lastEpoch = epoch
                    // Build and advertise a new frame for this epoch
                    val frame = makeTestFrame(seed, epoch)
                    if (frame != null) {
                        radio.startAdvertising(frame, cfg.codedPhy, cfg.advIntervalMs)
                    }
                }

                // Recompute stats and push to state
                val sketch = MeshState.measurement.localSketch(epoch, seed, cfg.rssiFloorDbm)
                val neighbors = MeshState.measurement.neighborsThisEpoch(epoch)
                val total = MeshState.measurement.totalHeard()

                val stats = Stats(
                    epoch = epoch,
                    neighborsThisEpoch = neighbors,
                    totalHeard = total,
                    localSketch = sketch,
                    advertising = true,
                    scanning = true,
                    codedPhyActive = cfg.codedPhy && radio.codedPhySupported(),
                    note = ""
                )
                MeshState.stats.value = stats

                // Update notification
                val notifText = "Epoch $epoch | neighbors=$neighbors | total=$total"
                val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notifManager.notify(NOTIFICATION_ID, buildNotification(notifText))

                delay(1_000L)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        radio.stop()
        MeshState.running.value = false
        MeshState.stats.value = MeshState.stats.value.copy(
            advertising = false,
            scanning = false
        )
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "BLE mesh background service"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mesh Radio")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }
}

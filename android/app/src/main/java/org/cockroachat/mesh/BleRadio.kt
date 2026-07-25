package org.cockroachat.mesh

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import java.util.UUID

class BleRadio(private val ctx: Context) {

    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("6c6f6361-6c6d-4573-6800-000000000001")
        val PARCEL_UUID = ParcelUuid(SERVICE_UUID)

        // BLE extended advertising interval units: 0.625 ms per unit
        // Valid range: 0x000020 (20ms) to 0xFFFFFF (~10485s)
        private const val INTERVAL_UNIT_MIN = 0x000020
        private const val INTERVAL_UNIT_MAX = 0xFFFFFF
    }

    private val bluetoothManager =
        ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter get() = bluetoothManager.adapter

    @Volatile private var currentAdvSet: AdvertisingSet? = null
    private var advCallback: AdvertisingSetCallback? = null
    private val scanCallbacks = mutableListOf<ScanCallback>()
    private var activeRelaySets = 0
    private var privateAdvActive = false

    // Advertising single-flight. startAdvertisingSet is asynchronous: the handle only
    // arrives in the callback, so currentAdvSet is null for a window after every start.
    // Callers (epoch rollover, send-path, tier switch, watchdog) can all fire within that
    // window — two overlapping hardware starts used to leave one set as an untracked
    // zombie broadcasting a stale frame forever (observed in field testing: both phones
    // kept re-airing their startup frame; K4 then dropped it as epoch-skewed and the mesh
    // looked dead). The lock serializes control; advStartInFlight collapses overlapping
    // starts into pendingFrame, applied when the set reports started; and any set that
    // reports started AFTER being superseded/stopped immediately stops itself.
    private val advLock = Any()
    @Volatile private var advStartInFlight = false
    private var pendingFrame: ByteArray? = null

    // Scan self-healing: a failed scan is dead until restarted (controller resource
    // exhaustion, stack hiccup). Without a restart, frame reception silently stops and
    // presence collapses to 0 while advertising keeps working.
    @Volatile private var wantScanning = false
    private var lastLowLatency = false
    private var lastOnLegacyPeer: ((BluetoothDevice, Int) -> Unit)? = null
    private var lastOnFrame: ((ByteArray, Int) -> Unit)? = null
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    /** Called with a human-readable debug line on notable events. */
    var onDebug: ((String) -> Unit)? = null

    /** Human-readable current advertising mode, shown in the stats pane. */
    @Volatile
    var advMode: String = "off"

    /** One-line adapter capability summary for the debug log. */
    @SuppressLint("MissingPermission")
    fun capabilityReport(): String {
        val a = adapter ?: return "radio: no bluetooth adapter"
        return try {
            "radio: enabled=${a.isEnabled} extAdv=${a.isLeExtendedAdvertisingSupported} " +
                "codedPhy=${a.isLeCodedPhySupported} maxAdvData=${a.leMaximumAdvertisingDataLength}"
        } catch (e: Exception) {
            "radio: capability query failed: ${e.message}"
        }
    }

    fun isSupported(): Boolean {
        return adapter != null && adapter.isEnabled && adapter.isLeExtendedAdvertisingSupported
    }

    fun codedPhySupported(): Boolean {
        return adapter != null && adapter.isLeCodedPhySupported
    }

    @SuppressLint("MissingPermission")
    fun startAdvertising(frame: ByteArray, codedPhy: Boolean, advIntervalMs: Long) {
        synchronized(advLock) {
            if (advStartInFlight) {
                // A start is already in flight; coalesce into pendingFrame so the newest
                // frame goes on air when the pending set reports started.
                pendingFrame = frame
                onDebug?.invoke("adv start coalesced: start already in flight")
                return
            }
            // Legacy fallback carries no frame data (frames flow over GATT): restarting
            // the beacon every epoch is pure churn. Keep it running.
            if (advMode == "legacy-uuid(gatt)" && currentAdvSet != null) return
            try {
                // Stop any prior advertising set
                stopAdvertisingLocked()

                val advertiser = adapter?.bluetoothLeAdvertiser ?: return

                // Convert ms to interval units (0.625 ms each), clamp to valid range
                val intervalUnits = ((advIntervalMs * 1000L) / 625L)
                    .toInt()
                    .coerceIn(INTERVAL_UNIT_MIN, INTERVAL_UNIT_MAX)

                // Frame (226 B) + UUID + AD framing overhead needs ~260 B of adv capacity.
                // Adapters that can't fit it (or can't do extended adv at all) get the legacy
                // fallback: a connectable UUID-only beacon; frames then flow over GATT.
                val extCapable = try {
                    adapter.isLeExtendedAdvertisingSupported && adapter.leMaximumAdvertisingDataLength >= 260
                } catch (e: Exception) {
                    false
                }
                if (!extCapable) {
                    startLegacyFallbackLocked(advertiser, intervalUnits)
                    return
                }

                val useCoded = codedPhy && codedPhySupported()
                val phy = if (useCoded) BluetoothDevice.PHY_LE_CODED else BluetoothDevice.PHY_LE_1M
                advMode = if (useCoded) "ext+coded" else "ext-1M"

                val params = AdvertisingSetParameters.Builder()
                    .setLegacyMode(false)
                    .setConnectable(false)
                    .setScannable(false)
                    .setInterval(intervalUnits)
                    .setPrimaryPhy(phy)
                    .setSecondaryPhy(phy)
                    .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
                    .build()

                val data = AdvertiseData.Builder()
                    // Advertise the service UUID too: the scanner's ScanFilter matches on the Service
                    // UUID AD field, which is distinct from the Service Data field carrying the frame.
                    // Without this the filtered scan sees nothing.
                    .addServiceUuid(PARCEL_UUID)
                    .addServiceData(PARCEL_UUID, frame)
                    .setIncludeDeviceName(false)
                    .build()

                val cb = object : AdvertisingSetCallback() {
                    override fun onAdvertisingSetStarted(
                        advertisingSet: AdvertisingSet?,
                        txPower: Int,
                        status: Int
                    ) {
                        val ok = status == ADVERTISE_SUCCESS
                        var superseded = false
                        synchronized(advLock) {
                            advStartInFlight = false
                            if (advCallback === this) {
                                currentAdvSet = if (ok) advertisingSet else null
                            } else {
                                superseded = true
                            }
                        }
                        if (superseded) {
                            // Started after being replaced/stopped while in flight. Kill it
                            // immediately — an untracked set would otherwise stay on air
                            // forever re-broadcasting its stale frame.
                            try {
                                adapter?.bluetoothLeAdvertiser?.stopAdvertisingSet(this)
                            } catch (e: Exception) {
                                onDebug?.invoke("superseded adv set stop failed: ${e.message}")
                            }
                            onDebug?.invoke("adv set started after supersede — stopped it (zombie prevented)")
                            return
                        }
                        onDebug?.invoke("adv set started: status=$status (${if (ok) "ok" else "failed"})")
                        if (ok && advertisingSet != null) {
                            val pending = synchronized(advLock) {
                                val p = pendingFrame
                                pendingFrame = null
                                p
                            }
                            if (pending != null) {
                                try {
                                    advertisingSet.setAdvertisingData(
                                        AdvertiseData.Builder()
                                            .addServiceUuid(PARCEL_UUID)
                                            .addServiceData(PARCEL_UUID, pending)
                                            .setIncludeDeviceName(false)
                                            .build()
                                    )
                                } catch (e: Exception) {
                                    onDebug?.invoke("pending frame apply failed: ${e.message}")
                                }
                            }
                        }
                        if (status == ADVERTISE_FAILED_DATA_TOO_LARGE) {
                            synchronized(advLock) {
                                stopAdvertisingLocked()
                                adapter?.bluetoothLeAdvertiser?.let { startLegacyFallbackLocked(it, intervalUnits) }
                            }
                        }
                    }

                    override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet?) {
                        synchronized(advLock) {
                            if (advCallback === this && currentAdvSet == advertisingSet) {
                                currentAdvSet = null
                            }
                        }
                        onDebug?.invoke("adv set stopped")
                    }
                }
                advCallback = cb
                advStartInFlight = true

                advertiser.startAdvertisingSet(params, data, null, null, null, cb)
            } catch (e: SecurityException) {
                advStartInFlight = false
                onDebug?.invoke("startAdvertising SecurityException: ${e.message}")
            } catch (e: Exception) {
                advStartInFlight = false
                onDebug?.invoke("startAdvertising exception: ${e.message}")
            }
        }
    }

    /**
     * Legacy-advertising fallback for adapters that can't carry the 226-byte frame.
     * Advertises a connectable UUID-only beacon; peers see the mesh UUID with no
     * service data and pull/push frames over the GATT plane instead.
     * Caller must hold advLock.
     */
    @SuppressLint("MissingPermission")
    private fun startLegacyFallbackLocked(
        advertiser: android.bluetooth.le.BluetoothLeAdvertiser,
        intervalUnits: Int
    ) {
        try {
            advMode = "legacy-uuid(gatt)"
            val params = AdvertisingSetParameters.Builder()
                .setLegacyMode(true)
                .setConnectable(true)
                .setScannable(true)
                .setInterval(intervalUnits)
                .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
                .build()
            val data = AdvertiseData.Builder()
                .addServiceUuid(PARCEL_UUID)
                .setIncludeDeviceName(false)
                .build()
            val cb = object : AdvertisingSetCallback() {
                override fun onAdvertisingSetStarted(
                    advertisingSet: AdvertisingSet?,
                    txPower: Int,
                    status: Int
                ) {
                    val ok = status == ADVERTISE_SUCCESS
                    var superseded = false
                    synchronized(advLock) {
                        advStartInFlight = false
                        if (advCallback === this) {
                            currentAdvSet = if (ok) advertisingSet else null
                        } else {
                            superseded = true
                        }
                    }
                    if (superseded) {
                        try {
                            adapter?.bluetoothLeAdvertiser?.stopAdvertisingSet(this)
                        } catch (e: Exception) {
                            onDebug?.invoke("superseded legacy set stop failed: ${e.message}")
                        }
                        return
                    }
                    onDebug?.invoke("legacy adv started: status=$status (${if (ok) "ok" else "failed"})")
                }

                override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet?) {
                    synchronized(advLock) {
                        if (advCallback === this && currentAdvSet == advertisingSet) {
                            currentAdvSet = null
                        }
                    }
                    onDebug?.invoke("legacy adv stopped")
                }
            }
            advCallback = cb
            advStartInFlight = true
            advertiser.startAdvertisingSet(params, data, null, null, null, cb)
        } catch (e: SecurityException) {
            advStartInFlight = false
            onDebug?.invoke("legacy adv SecurityException: ${e.message}")
        } catch (e: Exception) {
            advStartInFlight = false
            onDebug?.invoke("legacy adv exception: ${e.message}")
        }
    }

    /**
     * REMOVED (field-observed 2026-07-24): AdvertisingSet.setAdvertisingData() is broken on
     * real stacks. On a Samsung it silently killed the set (off air, no callback, handle
     * still valid); on MIUI it silently no-opped (set kept broadcasting stale data). Both
     * returned success, so neither the caller nor the watchdog could detect the failure.
     * Every epoch now uses a full stop+start via startAdvertising(), which both stacks
     * demonstrably honor — the peer hears those frames.
     */

    /**
     * [onFrame] fires for every mesh advertisement carrying a frame in service data.
     * [onLegacyPeer] fires for mesh-UUID scan results with NO service data — a peer whose radio
     * cannot do extended advertising and therefore needs the GATT fallback plane. Peers with
     * working extended advertising are NOT surfaced (connecting to them adds only radio churn).
     */
    @SuppressLint("MissingPermission")
    fun startScanning(
        lowLatency: Boolean,
        onLegacyPeer: ((BluetoothDevice, Int) -> Unit)? = null,
        onFrame: (ByteArray, Int) -> Unit
    ) {
        try {
            stopScanning()
            // Mark intent AFTER stopScanning (which clears it) so failure-restarts work.
            wantScanning = true
            lastLowLatency = lowLatency
            lastOnLegacyPeer = onLegacyPeer
            lastOnFrame = onFrame

            val scanner = adapter?.bluetoothLeScanner ?: return

            val filter = ScanFilter.Builder()
                .setServiceUuid(PARCEL_UUID)
                .build()

            val settings = ScanSettings.Builder()
                .setLegacy(false)
                .setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
                .setScanMode(if (lowLatency) ScanSettings.SCAN_MODE_LOW_LATENCY else ScanSettings.SCAN_MODE_BALANCED)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build()

            // [legacyPeerEvents]: only the legacy scan may classify a UUID-without-service-data
            // result as a GATT-fallback peer. The extended scan also produces such results
            // (truncated/unsynced ext records, relay sets mid-teardown) from peers that have
            // WORKING extended advertising — GATT-connecting to them is pure radio churn and
            // on single-set-class controllers costs us our own advertising slot.
            fun callback(legacyPeerEvents: Boolean): ScanCallback = object : ScanCallback() {
                private fun handle(result: ScanResult) {
                    val bytes = result.scanRecord?.getServiceData(PARCEL_UUID)
                    if (bytes != null) {
                        onFrame(bytes, result.rssi)
                    } else if (legacyPeerEvents) {
                        // Mesh UUID but no frame payload: extended-adv-incapable peer.
                        onLegacyPeer?.invoke(result.device, result.rssi)
                    }
                }

                override fun onScanResult(callbackType: Int, result: ScanResult) = handle(result)

                override fun onBatchScanResults(results: MutableList<ScanResult>) {
                    for (result in results) handle(result)
                }

                override fun onScanFailed(errorCode: Int) {
                    onDebug?.invoke("scan failed: errorCode=$errorCode — restarting in 2s")
                    // A failed scan delivers nothing until restarted. Without this the
                    // mesh silently went deaf and presence decayed to 0.
                    mainHandler.postDelayed({
                        val frameCb = lastOnFrame
                        if (wantScanning && frameCb != null) {
                            startScanning(lastLowLatency, lastOnLegacyPeer, frameCb)
                        }
                    }, 2_000L)
                }
            }
            val extendedCallback = callback(legacyPeerEvents = false)
            scanCallbacks += extendedCallback
            scanner.startScan(listOf(filter), settings, extendedCallback)

            // The extended scan above does not return legacy advertisements.  Start a second
            // legacy-only scan so UUID-only GATT fallback beacons are actually discovered.
            // (Android's setLegacy(true) explicitly restricts results to Bluetooth 4.2-style
            // advertisements.)  The callbacks are distinct so each scan can be stopped cleanly.
            val legacySettings = ScanSettings.Builder()
                .setLegacy(true)
                .setScanMode(if (lowLatency) ScanSettings.SCAN_MODE_LOW_LATENCY else ScanSettings.SCAN_MODE_BALANCED)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build()
            val legacyCallback = callback(legacyPeerEvents = true)
            scanCallbacks += legacyCallback
            scanner.startScan(listOf(filter), legacySettings, legacyCallback)
        } catch (e: SecurityException) {
            onDebug?.invoke("startScanning SecurityException: ${e.message}")
        }
    }

    /** B8: true while a hardware relay slot is free. The service's relay queue drains only
     *  when this holds — frames WAIT for a slot instead of being silently dropped. */
    fun relayCapacityAvailable(): Boolean = activeRelaySets < 2

    /**
     * One-shot relay advertisement. [codedPhy] (C6): honor the configured PHY so relayed
     * frames reach the same long-range frontier as originations — previously relays were
     * hardcoded to 1M and died at the edge of coded-PHY range.
     */
    @SuppressLint("MissingPermission")
    fun advertiseRelayOnce(frame: ByteArray, durationMs: Long, codedPhy: Boolean = false) {
        if (activeRelaySets >= 2) {
            onDebug?.invoke("relay skipped: 2 relay sets already active")
            return
        }
        try {
            val advertiser = adapter?.bluetoothLeAdvertiser ?: return
            val useCoded = codedPhy && codedPhySupported()
            val phy = if (useCoded) BluetoothDevice.PHY_LE_CODED else BluetoothDevice.PHY_LE_1M
            val params = AdvertisingSetParameters.Builder()
                .setLegacyMode(false)
                .setConnectable(false)
                .setScannable(false)
                .setInterval(AdvertisingSetParameters.INTERVAL_LOW)
                .setPrimaryPhy(phy)
                .setSecondaryPhy(phy)
                .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
                .build()
            val data = AdvertiseData.Builder()
                .addServiceUuid(PARCEL_UUID)
                .addServiceData(PARCEL_UUID, frame)
                .setIncludeDeviceName(false)
                .build()
            val cb = object : AdvertisingSetCallback() {}
            activeRelaySets++
            advertiser.startAdvertisingSet(params, data, null, null, null, cb)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    advertiser.stopAdvertisingSet(cb)
                } catch (e: Exception) {
                    onDebug?.invoke("relay stop failed: ${e.message}")
                }
                activeRelaySets--
            }, durationMs)
        } catch (e: SecurityException) {
            activeRelaySets--
            onDebug?.invoke("relay adv denied: ${e.message}")
        } catch (e: Exception) {
            activeRelaySets--
            onDebug?.invoke("relay adv failed: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun advertisePrivateOnce(
        frame: ByteArray,
        durationMs: Long,
        restoreFrame: ByteArray,
        codedPhy: Boolean,
        advIntervalMs: Long,
        onRestored: () -> Unit
    ): Boolean {
        if (privateAdvActive) {
            onDebug?.invoke("private adv skipped: one already active")
            return false
        }
        /*
         * Do not open a second AdvertisingSet here.  Most phone controllers expose one set, so
         * the previous implementation successfully sealed the private frame but could not put it
         * on air while the regular public advertisement was active.  Reuse that set instead —
         * with a FULL stop+start, because setAdvertisingData() is silently broken on real
         * stacks (see note above).  In legacy/GATT mode startAdvertising is a no-op (beacon
         * already running) and MeshService transports the frame over GATT.
         */
        privateAdvActive = true
        startAdvertising(frame, codedPhy, advIntervalMs)
        onDebug?.invoke("private frame using primary advertising set")
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            privateAdvActive = false
            startAdvertising(restoreFrame, codedPhy, advIntervalMs)
            onRestored()
        }, durationMs)
        return true
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        stopAdvertising()
        stopScanning()
    }

    @SuppressLint("MissingPermission")
    fun stopAdvertising() {
        synchronized(advLock) {
            stopAdvertisingLocked()
        }
    }

    /** Caller must hold advLock. */
    @SuppressLint("MissingPermission")
    private fun stopAdvertisingLocked() {
        try {
            pendingFrame = null
            val cb = advCallback
            if (cb != null) {
                adapter?.bluetoothLeAdvertiser?.stopAdvertisingSet(cb)
            }
            advCallback = null
            currentAdvSet = null
            advStartInFlight = false
            advMode = "off"
        } catch (e: SecurityException) {
            onDebug?.invoke("stopAdvertising SecurityException: ${e.message}")
        } catch (e: Exception) {
            onDebug?.invoke("stopAdvertising exception: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopScanning() {
        wantScanning = false
        try {
            val scanner = adapter?.bluetoothLeScanner ?: return
            for (cb in scanCallbacks) scanner.stopScan(cb)
            scanCallbacks.clear()
        } catch (e: SecurityException) {
            onDebug?.invoke("stopScanning SecurityException: ${e.message}")
        }
    }

    /**
     * True while our own frame is believed to be on air (or a start is in flight). The
     * controller can reclaim a hardware advertising set (relay bursts, GATT connections,
     * stack hiccups) — the service watchdog uses this to re-advertise within ~1 s instead
     * of waiting for the next epoch rollover. Counting in-flight starts is essential:
     * without it the watchdog fired inside the async start window and double-started.
     */
    fun advertisingActive(): Boolean = advStartInFlight || currentAdvSet != null
}

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

    private var currentAdvSet: AdvertisingSet? = null
    private var advCallback: AdvertisingSetCallback? = null
    private var scanCallback: ScanCallback? = null

    fun isSupported(): Boolean {
        return adapter != null && adapter.isEnabled && adapter.isLeExtendedAdvertisingSupported
    }

    fun codedPhySupported(): Boolean {
        return adapter != null && adapter.isLeCodedPhySupported
    }

    @SuppressLint("MissingPermission")
    fun startAdvertising(frame: ByteArray, codedPhy: Boolean, advIntervalMs: Long) {
        try {
            // Stop any prior advertising set
            stopAdvertising()

            val advertiser = adapter?.bluetoothLeAdvertiser ?: return

            // Convert ms to interval units (0.625 ms each), clamp to valid range
            val intervalUnits = ((advIntervalMs * 1000L) / 625L)
                .toInt()
                .coerceIn(INTERVAL_UNIT_MIN, INTERVAL_UNIT_MAX)

            val useCoded = codedPhy && codedPhySupported()
            val phy = if (useCoded) BluetoothDevice.PHY_LE_CODED else BluetoothDevice.PHY_LE_1M

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
                    currentAdvSet = advertisingSet
                }

                override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet?) {
                    if (currentAdvSet == advertisingSet) {
                        currentAdvSet = null
                    }
                }
            }
            advCallback = cb

            advertiser.startAdvertisingSet(params, data, null, null, null, cb)
        } catch (e: SecurityException) {
            // Permission not granted; silently skip
        } catch (e: Exception) {
            // Device may not support extended advertising; silently skip
        }
    }

    @SuppressLint("MissingPermission")
    fun startScanning(onFrame: (ByteArray, Int) -> Unit) {
        try {
            stopScanning()

            val scanner = adapter?.bluetoothLeScanner ?: return

            val filter = ScanFilter.Builder()
                .setServiceUuid(PARCEL_UUID)
                .build()

            val settings = ScanSettings.Builder()
                .setLegacy(false)
                .setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build()

            val cb = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    val bytes = result.scanRecord?.getServiceData(PARCEL_UUID) ?: return
                    onFrame(bytes, result.rssi)
                }

                override fun onBatchScanResults(results: MutableList<ScanResult>) {
                    for (result in results) {
                        val bytes = result.scanRecord?.getServiceData(PARCEL_UUID) ?: continue
                        onFrame(bytes, result.rssi)
                    }
                }

                override fun onScanFailed(errorCode: Int) {
                    // Scanning failed; could log here
                }
            }
            scanCallback = cb

            scanner.startScan(listOf(filter), settings, cb)
        } catch (e: SecurityException) {
            // Permission not granted; silently skip
        }
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        stopAdvertising()
        stopScanning()
    }

    @SuppressLint("MissingPermission")
    private fun stopAdvertising() {
        try {
            val cb = advCallback ?: return
            adapter?.bluetoothLeAdvertiser?.stopAdvertisingSet(cb)
            advCallback = null
            currentAdvSet = null
        } catch (e: SecurityException) {
            // Swallow
        } catch (e: Exception) {
            // Swallow
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopScanning() {
        try {
            val cb = scanCallback ?: return
            adapter?.bluetoothLeScanner?.stopScan(cb)
            scanCallback = null
        } catch (e: SecurityException) {
            // Swallow
        }
    }
}

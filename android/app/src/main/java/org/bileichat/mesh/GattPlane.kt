package org.bileichat.mesh

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * GATT fallback transport for the bileichat BLE mesh.
 *
 * Provides the same wire contract as the extended-advertising plane but over GATT connections,
 * so phones that do not support BLE 5 extended advertising can still exchange 226-byte frames.
 *
 * Wire contract (shared with the laptop client — do NOT change UUIDs):
 *   Service  : 6c6f6361-6c6d-4573-6800-000000000001  (same as advertising service UUID)
 *   FRAME_TX : 6c6f6361-6c6d-4573-6800-000000000002  READ | NOTIFY  — our current outgoing frame
 *   FRAME_RX : 6c6f6361-6c6d-4573-6800-000000000003  WRITE | WRITE_NO_RESPONSE — peers send here
 *   CCCD     : 00002902-0000-1000-8000-00805f9b34fb
 *
 * Invariants enforced:
 *   - Received byte arrays that are not exactly 226 bytes are silently dropped.
 *   - No frame bytes are parsed in Kotlin; parsing is done by Rust core via onFrame callback.
 *   - Nothing is labeled encrypted or private.
 */
@SuppressLint("MissingPermission")
class GattPlane(
    private val ctx: Context,
    /** Called with (frameBytes, rssi) when a valid 226-byte frame arrives from a peer. */
    private val onFrame: (ByteArray, Int) -> Unit,
    private val onDebug: (String) -> Unit
) {
    companion object {
        val SERVICE_UUID: UUID        = UUID.fromString("6c6f6361-6c6d-4573-6800-000000000001")
        val CHAR_FRAME_TX: UUID       = UUID.fromString("6c6f6361-6c6d-4573-6800-000000000002")
        val CHAR_FRAME_RX: UUID       = UUID.fromString("6c6f6361-6c6d-4573-6800-000000000003")
        val CCCD_UUID: UUID           = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        private const val FRAME_SIZE = 226
        private const val MTU_REQUEST = 247
        /** Minimum MTU that fits a 226-byte ATT notification (ATT overhead = 3 bytes). */
        private const val MTU_MIN_FOR_NOTIFY = 229
        /** Maximum simultaneous outbound GATT client connections. */
        private const val MAX_PEERS = 3
        /** Maximum tracked peer entries in the peers map (bounds memory). */
        private const val MAX_TRACKED_PEERS = 32
        /** Maximum entries in the scan-RSSI cache. */
        private const val MAX_SCAN_RSSI = 64
        /** Reconnect back-off after disconnection (ms). */
        private const val RECONNECT_BACKOFF_MS = 5_000L
        /** RSSI poll interval per connection (ms). */
        private const val RSSI_POLL_MS = 5_000L

        // ---- B9: exhaustion caps ----
        /** Maximum simultaneous subscribed centrals on our server. */
        private const val MAX_SUBSCRIBERS = 4
        /** Centrals with no GATT activity for this long are disconnected (ms). */
        private const val CENTRAL_IDLE_TIMEOUT_MS = 60_000L
        /** A legacy peer must beacon persistently for this long before we spend a client
         *  slot on it (three cheap dongles flashing the UUID must not exhaust slots). */
        private const val MIN_PEER_AGE_MS = 8_000L
        /** Give up on a connectGatt that never reports a state change (ms). autoConnect=false
         *  has no stack-level timeout, so without this a stuck attempt holds a slot forever. */
        private const val CONNECT_TIMEOUT_MS = 10_000L

        // ---- C5: app-level chunking for small MTUs ----
        /** First byte of a chunk write (distinguishes chunks from whole-frame writes). */
        private const val CHUNK_MAGIC: Byte = 0xA5.toByte()
        /** Stale partial chunk assemblies are dropped after this long (ms). */
        private const val CHUNK_BUFFER_TTL_MS = 10_000L
    }

    private val bluetoothManager =
        ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    // ---- Server side -------------------------------------------------------

    private var gattServer: BluetoothGattServer? = null
    /** Devices that have enabled notifications on FRAME_TX (address -> device). */
    private val notifySubscribers = ConcurrentHashMap<String, BluetoothDevice>()
    /**
     * Per-device accumulation buffer for prepared (long) writes on FRAME_RX.
     * Key = device address.
     */
    private val preparedWriteBuffers = ConcurrentHashMap<String, ByteArray>()

    // B9: every connected central (subscribed or not) with its last-activity timestamp;
    // a watchdog disconnects idlers so cheap dongles can't hold the server open.
    private val centralActivity = ConcurrentHashMap<String, Pair<BluetoothDevice, Long>>()
    private var centralWatchdog: java.util.Timer? = null

    // C5: negotiated ATT MTU per central (server side) — the chunk stride for reassembly.
    private val centralMtu = ConcurrentHashMap<String, Int>()
    // C5: partial chunk assemblies per central (buffer, last-update-ms).
    private val chunkBuffers = ConcurrentHashMap<String, Pair<ByteArray, Long>>()

    // ---- Client side -------------------------------------------------------

    /** address -> PeerState */
    private val peers = ConcurrentHashMap<String, PeerState>()

    // B9: first-sighting timestamp per candidate peer — a peer must beacon persistently
    // for MIN_PEER_AGE_MS before we spend a connection slot on it.
    private val pendingPeers = ConcurrentHashMap<String, Long>()

    /** addr -> (rssi, tsMs) scan-RSSI cache for server-side frame RSSI resolution. Capped at MAX_SCAN_RSSI. */
    private val scanRssi = ConcurrentHashMap<String, Pair<Int, Long>>()

    /** The current outgoing 226-byte frame. Setting it notifies subscribers and writes to peers. */
    @Volatile
    var currentFrame: ByteArray = ByteArray(FRAME_SIZE)
        set(value) {
            field = value
            notifySubscribers(value)
            writeToPeers(value)
        }

    /**
     * B7: push a RELAYED frame to GATT peers without touching [currentFrame] (reads must
     * keep returning OUR outgoing frame). Legacy phones — the fallback's entire reason to
     * exist — previously received only originations, never multi-hop traffic.
     */
    fun relayOnce(frame: ByteArray) {
        notifySubscribers(frame)
        writeToPeers(frame)
    }

    // ---- Lifecycle ---------------------------------------------------------

    /** Open the GATT server. Call once from MeshService.onStartCommand. */
    fun start() {
        openServer()
        // B9: idle-central watchdog — a central that never subscribes/reads/writes still
        // holds a connection slot; disconnect it after CENTRAL_IDLE_TIMEOUT_MS.
        val timer = java.util.Timer("gatt-central-watchdog", true)
        centralWatchdog = timer
        timer.scheduleAtFixedRate(object : java.util.TimerTask() {
            override fun run() {
                val now = System.currentTimeMillis()
                for ((addr, pair) in centralActivity) {
                    // A SUBSCRIBED central is a healthy listener, not an idle squatter.
                    // Notifications are server-initiated, so a legacy phone that connects,
                    // subscribes and then quietly receives produces no server-side ATT
                    // traffic at all — touchCentral() fires on reads/writes/descriptor
                    // writes only, never on notifySubscribers(). Every such peer was
                    // therefore evicted 60 s after subscribing, and reconnecting cost
                    // RECONNECT_BACKOFF plus a fresh MIN_PEER_AGE_MS dwell: roughly 13 s
                    // deaf every minute, on exactly the phones this fallback plane exists
                    // to carry.
                    if (notifySubscribers.containsKey(addr)) continue
                    if (now - pair.second > CENTRAL_IDLE_TIMEOUT_MS) {
                        try {
                            gattServer?.cancelConnection(pair.first)
                            onDebug("gatt server: evicted idle central $addr")
                        } catch (e: Exception) {
                            onDebug("gatt server: idle evict $addr failed: ${e.message}")
                        }
                        centralActivity.remove(addr)
                    }
                }
            }
        }, 30_000L, 30_000L)
    }

    /** Close GATT server and all client connections. Call from MeshService.onDestroy. */
    fun stop() {
        try {
            centralWatchdog?.cancel()
            centralWatchdog = null

            // Close all client GATTs.
            for ((addr, state) in peers) {
                try {
                    // Cancel the per-peer RSSI timer and clear the liveness flags FIRST.
                    // The timer's self-cancel guard is `!state.connected || state.gatt == null`,
                    // and stop() used to leave both untrue while readRemoteRssi() on a closed
                    // BluetoothGatt merely returns false without throwing — so every peer
                    // leaked a daemon Timer waking every 5 s until process death, including
                    // after a panic wipe.
                    state.rssiPollHandle?.cancel()
                    state.rssiPollHandle = null
                    state.connected = false
                    state.connecting = false
                    state.gatt?.close()
                    state.gatt = null
                } catch (e: Exception) {
                    onDebug("gatt close[$addr] exception: ${e.message}")
                }
            }
            peers.clear()
            pendingPeers.clear()

            gattServer?.close()
            gattServer = null
            notifySubscribers.clear()
            centralActivity.clear()
            centralMtu.clear()
            chunkBuffers.clear()
            onDebug("gatt plane stopped")
        } catch (e: SecurityException) {
            onDebug("stop SecurityException: ${e.message}")
        } catch (e: Exception) {
            onDebug("stop exception: ${e.message}")
        }
    }

    /**
     * Called by MeshService from the scan callback for every scan result that advertises our
     * service UUID (with or without service data). If we are not already connected/connecting
     * to this device and we are under the peer cap, initiate a GATT connection.
     */
    fun onPeerSeen(device: BluetoothDevice, rssi: Int) {
        val addr = device.address
        // Update scan-RSSI cache for server-side frame RSSI resolution
        cacheScanRssi(addr, rssi)
        val existing = peers[addr]
        // Update cached RSSI even for connected peers
        if (existing != null) {
            existing.lastRssi = rssi
        }

        if (existing != null && existing.connected) return
        if (existing != null && existing.connecting) return
        // Enforce reconnect back-off
        if (existing != null) {
            val elapsed = System.currentTimeMillis() - existing.lastDisconnectMs
            if (elapsed < RECONNECT_BACKOFF_MS) return
        }
        // B9: require persistent beaconing before spending a slot. First sighting just
        // starts the clock; the connect happens on a later sighting (scan cadence ~1 s).
        val now = System.currentTimeMillis()
        val firstSeen = pendingPeers.putIfAbsent(addr, now)
        if (firstSeen == null) {
            if (pendingPeers.size > 32) pendingPeers.entries.minByOrNull { it.value }?.key?.let { pendingPeers.remove(it) }
            return
        }
        if (now - firstSeen < MIN_PEER_AGE_MS) return
        // Peer cap
        val activeCount = peers.values.count { it.connected || it.connecting }
        if (activeCount >= MAX_PEERS) return

        pendingPeers.remove(addr)
        connectPeer(device, rssi)
    }

    // ---- Server implementation ---------------------------------------------

    private fun openServer() {
        try {
            val service = BluetoothGattService(
                SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            )

            // FRAME_TX: READ | NOTIFY
            val frameTx = BluetoothGattCharacteristic(
                CHAR_FRAME_TX,
                BluetoothGattCharacteristic.PROPERTY_READ or
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ
            )
            val cccd = BluetoothGattDescriptor(
                CCCD_UUID,
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
            )
            frameTx.addDescriptor(cccd)

            // FRAME_RX: WRITE | WRITE_NO_RESPONSE
            val frameRx = BluetoothGattCharacteristic(
                CHAR_FRAME_RX,
                BluetoothGattCharacteristic.PROPERTY_WRITE or
                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE
            )

            service.addCharacteristic(frameTx)
            service.addCharacteristic(frameRx)

            val server = bluetoothManager.openGattServer(ctx, serverCallback)
            if (server == null) {
                onDebug("gatt server: openGattServer returned null")
                return
            }
            server.addService(service)
            gattServer = server
            onDebug("gatt server: opened")
        } catch (e: SecurityException) {
            onDebug("openServer SecurityException: ${e.message}")
        } catch (e: Exception) {
            onDebug("openServer exception: ${e.message}")
        }
    }

    private val serverCallback = object : BluetoothGattServerCallback() {

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            val addr = device.address
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                notifySubscribers.remove(addr)
                preparedWriteBuffers.remove(addr)
                centralActivity.remove(addr)
                centralMtu.remove(addr)
                chunkBuffers.remove(addr)
                onDebug("gatt server: central $addr disconnected (status=$status)")
            } else if (newState == BluetoothProfile.STATE_CONNECTED) {
                centralActivity[addr] = Pair(device, System.currentTimeMillis())
                onDebug("gatt server: central $addr connected")
            }
        }

        override fun onMtuChanged(device: BluetoothDevice, mtu: Int) {
            centralMtu[device.address] = mtu
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            touchCentral(device)
            if (characteristic.uuid != CHAR_FRAME_TX) {
                gattServer?.sendResponse(device, requestId,
                    BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED, 0, null)
                return
            }
            val frame = currentFrame
            val data = if (offset < frame.size) frame.copyOfRange(offset, frame.size) else ByteArray(0)
            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, data)
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            if (characteristic.uuid != CHAR_FRAME_RX) {
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId,
                        BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED, 0, null)
                }
                return
            }
            if (value == null) {
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null)
                }
                return
            }
            touchCentral(device)

            if (preparedWrite) {
                // Accumulate for long write; enforce frame bound
                val addr = device.address
                if (offset < 0 || offset + value.size > FRAME_SIZE) {
                    preparedWriteBuffers.remove(addr)
                    if (responseNeeded) {
                        gattServer?.sendResponse(device, requestId,
                            BluetoothGatt.GATT_INVALID_OFFSET, offset, null)
                    }
                    return
                }
                val current = preparedWriteBuffers.getOrDefault(addr, ByteArray(0))
                val needed = offset + value.size
                val buf = if (current.size < needed) current.copyOf(needed) else current
                value.copyInto(buf, offset)
                preparedWriteBuffers[addr] = buf
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null)
                }
            } else {
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null)
                }
                // Immediate write: enforce 226-byte invariant
                if (value.size == FRAME_SIZE) {
                    val rssi = resolveServerRssi(device.address)
                    onFrame(value, rssi)
                } else if (value.size >= 2 && value[0] == CHUNK_MAGIC) {
                    // C5: app-level chunk from a small-MTU peer — reassemble.
                    handleChunk(device, value)
                }
                // Silently drop anything else (invariant)
            }
        }

        /** C5: accumulate one chunk; emit the frame when 226 bytes have assembled. */
        private fun handleChunk(device: BluetoothDevice, value: ByteArray) {
            val addr = device.address
            val now = System.currentTimeMillis()
            val stride = ((centralMtu[addr] ?: 23) - 5).coerceAtLeast(8)
            val seq = value[1].toInt() and 0xFF
            val offset = seq * stride
            if (offset >= FRAME_SIZE) {
                chunkBuffers.remove(addr)
                return
            }
            val stale = chunkBuffers[addr]
            val buf = if (stale == null || now - stale.second > CHUNK_BUFFER_TTL_MS) {
                ByteArray(FRAME_SIZE)
            } else stale.first
            val n = minOf(value.size - 2, FRAME_SIZE - offset)
            value.copyInto(buf, offset, 2, 2 + n)
            val assembledEnd = offset + n
            if (assembledEnd >= FRAME_SIZE) {
                chunkBuffers.remove(addr)
                val rssi = resolveServerRssi(addr)
                onFrame(buf, rssi)
            } else {
                chunkBuffers[addr] = Pair(buf, now)
            }
        }

        override fun onExecuteWrite(device: BluetoothDevice, requestId: Int, execute: Boolean) {
            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
            if (!execute) {
                preparedWriteBuffers.remove(device.address)
                return
            }
            val buf = preparedWriteBuffers.remove(device.address) ?: return
            if (buf.size == FRAME_SIZE) {
                val rssi = resolveServerRssi(device.address)
                onFrame(buf, rssi)
            }
            // Silently drop non-226-byte buffers (invariant)
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
            }
            if (descriptor.uuid != CCCD_UUID) return
            touchCentral(device)
            val addr = device.address
            val enabled = value != null &&
                value.size >= 2 &&
                value[0] == BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE[0] &&
                value[1] == BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE[1]
            if (enabled) {
                // B9: bound the subscriber set — beyond the cap, refuse and drop the central.
                if (!notifySubscribers.containsKey(addr) && notifySubscribers.size >= MAX_SUBSCRIBERS) {
                    onDebug("gatt server: subscriber cap reached — refusing $addr")
                    try {
                        gattServer?.cancelConnection(device)
                    } catch (e: Exception) {
                        onDebug("gatt server: refuse $addr failed: ${e.message}")
                    }
                    return
                }
                notifySubscribers[addr] = device
                onDebug("gatt server: $addr subscribed to FRAME_TX")
            } else {
                notifySubscribers.remove(addr)
                onDebug("gatt server: $addr unsubscribed from FRAME_TX")
            }
        }
    }

    /** B9: refresh a central's last-activity timestamp (read/write/descriptor traffic). */
    private fun touchCentral(device: BluetoothDevice) {
        centralActivity[device.address] = Pair(device, System.currentTimeMillis())
    }

    /** Resolve RSSI for a server-side frame: connection RSSI if tracked, else scan cache, else -127. */
    private fun resolveServerRssi(addr: String): Int {
        val peer = peers[addr]
        if (peer != null && peer.lastRssi != -127) return peer.lastRssi
        return scanRssi[addr]?.first ?: -127
    }

    /** Push [frame] to all subscribed centrals via GATT notification. */
    private fun notifySubscribers(frame: ByteArray) {
        val server = gattServer ?: return
        val service = server.getService(SERVICE_UUID) ?: return
        val char = service.getCharacteristic(CHAR_FRAME_TX) ?: return
        for ((addr, device) in notifySubscribers) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // API 33+: pass value directly, avoids deprecated value setter
                    val result = server.notifyCharacteristicChanged(device, char, false, frame)
                    if (result != BluetoothGatt.GATT_SUCCESS) {
                        onDebug("gatt server: notify $addr result=$result")
                    }
                } else {
                    @Suppress("DEPRECATION")
                    char.value = frame
                    @Suppress("DEPRECATION")
                    val ok = server.notifyCharacteristicChanged(device, char, false)
                    if (!ok) onDebug("gatt server: notify $addr returned false")
                }
            } catch (e: SecurityException) {
                onDebug("notifySubscribers[$addr] SecurityException: ${e.message}")
            } catch (e: Exception) {
                onDebug("notifySubscribers[$addr] exception: ${e.message}")
            }
        }
    }

    // ---- Client implementation ---------------------------------------------

    /** Per-peer connection state. */
    private inner class PeerState(val address: String) {
        @Volatile var gatt: BluetoothGatt? = null
        @Volatile var connected = false
        @Volatile var connecting = false
        @Volatile var lastRssi: Int = -127
        @Volatile var lastDisconnectMs: Long = 0L
        /** MTU negotiated for this connection. */
        @Volatile var mtu: Int = 23
        /** True once FRAME_TX notifications have been enabled. */
        @Volatile var notifyEnabled = false
        /** True once the initial FRAME_TX read is done. */
        @Volatile var initialReadDone = false
        /** True once we have written our own frame to FRAME_RX. */
        @Volatile var initialWriteDone = false

        // C5: serialized chunk writes for small MTUs (one ATT write in flight per peer).
        val writeLock = Any()
        val pendingWrites = java.util.ArrayDeque<ByteArray>()
        @Volatile var writeInFlight = false

        // Running RSSI poll: cancelled by clearing gatt reference
        @Volatile var rssiPollHandle: java.util.Timer? = null
    }

    /** Update the scan-RSSI cache, evicting the oldest entry when over cap. */
    private fun cacheScanRssi(addr: String, rssi: Int) {
        scanRssi[addr] = Pair(rssi, System.currentTimeMillis())
        if (scanRssi.size > MAX_SCAN_RSSI) {
            val oldest = scanRssi.entries.minByOrNull { it.value.second }?.key
            if (oldest != null && oldest != addr) scanRssi.remove(oldest)
        }
    }

    private fun connectPeer(device: BluetoothDevice, rssi: Int) {
        val addr = device.address
        // Bound tracked peers: evict least-recently-active before inserting a new one
        if (!peers.containsKey(addr) && peers.size >= MAX_TRACKED_PEERS) {
            val oldest = peers.entries
                .filter { !it.value.connected && !it.value.connecting }
                .minByOrNull { it.value.lastDisconnectMs }
                ?: peers.entries.minByOrNull { it.value.lastDisconnectMs }
            oldest?.let { peers.remove(it.key) }
        }
        val state = PeerState(addr).also {
            it.lastRssi = rssi
            it.connecting = true
        }
        peers[addr] = state
        onDebug("gatt client: connecting to $addr")
        try {
            val gatt = device.connectGatt(ctx, false, makeClientCallback(state),
                BluetoothDevice.TRANSPORT_LE)
            state.gatt = gatt
            if (gatt == null) {
                // connectGatt can return null. Nothing resets `connecting` in that case, so
                // the slot was held forever by a connection that never existed.
                onDebug("connectGatt[$addr] returned null — freeing slot")
                state.connecting = false
                peers.remove(addr)
                return
            }
            // With autoConnect=false there is no stack-level connect timeout: on a marginal
            // link onConnectionStateChange may take ~30 s or never fire at all. The peer cap
            // counts `connected || connecting`, so a few stuck attempts burned every client
            // slot until process death — a cheap dongle that beacons past MIN_PEER_AGE_MS and
            // then goes quiet could pin them deliberately.
            mainHandler.postDelayed({
                if (state.connecting && !state.connected) {
                    onDebug("gatt client: connect to $addr timed out — freeing slot")
                    state.connecting = false
                    try {
                        state.gatt?.disconnect()
                        state.gatt?.close()
                    } catch (e: Exception) {
                        onDebug("connect-timeout close[$addr] failed: ${e.message}")
                    }
                    state.gatt = null
                    peers.remove(addr)
                }
            }, CONNECT_TIMEOUT_MS)
        } catch (e: SecurityException) {
            onDebug("connectGatt[$addr] SecurityException: ${e.message}")
            state.connecting = false
            peers.remove(addr)
        } catch (e: Exception) {
            onDebug("connectGatt[$addr] exception: ${e.message}")
            state.connecting = false
            peers.remove(addr)
        }
    }

    private fun makeClientCallback(state: PeerState) = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val addr = state.address
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                state.connected = true
                state.connecting = false
                onDebug("gatt client: connected to $addr (status=$status)")
                try {
                    gatt.requestMtu(MTU_REQUEST)
                } catch (e: SecurityException) {
                    onDebug("requestMtu[$addr] SecurityException: ${e.message}")
                    disconnectPeer(state)
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                onDebug("gatt client: disconnected from $addr (status=$status)")
                disconnectPeer(state)
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            val addr = state.address
            state.mtu = mtu
            if (mtu < MTU_MIN_FOR_NOTIFY) {
                onDebug("gatt client: $addr MTU=$mtu < $MTU_MIN_FOR_NOTIFY; chunking writes + polling reads")
            } else {
                onDebug("gatt client: $addr MTU=$mtu ok")
            }
            try {
                gatt.discoverServices()
            } catch (e: SecurityException) {
                onDebug("discoverServices[$addr] SecurityException: ${e.message}")
                disconnectPeer(state)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val addr = state.address
            if (status != BluetoothGatt.GATT_SUCCESS) {
                onDebug("gatt client: $addr discoverServices failed status=$status")
                disconnectPeer(state)
                return
            }
            val service = gatt.getService(SERVICE_UUID)
            if (service == null) {
                onDebug("gatt client: $addr service not found — not a mesh peer")
                disconnectPeer(state)
                return
            }
            val frameTx = service.getCharacteristic(CHAR_FRAME_TX)
            if (frameTx == null) {
                onDebug("gatt client: $addr FRAME_TX characteristic not found")
                disconnectPeer(state)
                return
            }
            // Step 1: enable notifications
            try {
                val ok = gatt.setCharacteristicNotification(frameTx, true)
                if (!ok) {
                    onDebug("gatt client: $addr setCharacteristicNotification failed")
                }
                val cccd = frameTx.getDescriptor(CCCD_UUID)
                if (cccd != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        gatt.writeDescriptor(cccd,
                            BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    } else {
                        @Suppress("DEPRECATION")
                        cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        @Suppress("DEPRECATION")
                        gatt.writeDescriptor(cccd)
                    }
                } else {
                    onDebug("gatt client: $addr CCCD not found; skipping notify enable")
                    doInitialRead(gatt, state)
                }
            } catch (e: SecurityException) {
                onDebug("enableNotify[$addr] SecurityException: ${e.message}")
                disconnectPeer(state)
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            val addr = state.address
            if (descriptor.uuid == CCCD_UUID) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    state.notifyEnabled = true
                    onDebug("gatt client: $addr FRAME_TX notify enabled")
                } else {
                    onDebug("gatt client: $addr CCCD write failed status=$status")
                }
                // Step 2: initial read regardless of notify outcome
                doInitialRead(gatt, state)
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            val addr = state.address
            if (characteristic.uuid != CHAR_FRAME_TX) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                @Suppress("DEPRECATION")
                val value = characteristic.value
                if (value != null && value.size == FRAME_SIZE) {
                    onFrame(value, state.lastRssi)
                }
                // Silently drop non-226-byte (invariant)
            } else {
                onDebug("gatt client: $addr FRAME_TX read failed status=$status")
            }
            if (!state.initialReadDone) {
                state.initialReadDone = true
                // Step 3: write our frame to FRAME_RX
                doInitialWrite(gatt, state)
            }
        }

        // API 33+ override with value parameter
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            val addr = state.address
            if (characteristic.uuid != CHAR_FRAME_TX) return
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (value.size == FRAME_SIZE) {
                    onFrame(value, state.lastRssi)
                }
            } else {
                onDebug("gatt client: $addr FRAME_TX read failed status=$status")
            }
            if (!state.initialReadDone) {
                state.initialReadDone = true
                doInitialWrite(gatt, state)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid != CHAR_FRAME_TX) return
            @Suppress("DEPRECATION")
            val value = characteristic.value ?: return
            if (value.size == FRAME_SIZE) {
                onFrame(value, state.lastRssi)
            }
            // Silently drop non-226-byte (invariant)
        }

        // API 33+ override with value parameter
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid != CHAR_FRAME_TX) return
            if (value.size == FRAME_SIZE) {
                onFrame(value, state.lastRssi)
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            val addr = state.address
            if (characteristic.uuid == CHAR_FRAME_RX) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    onDebug("gatt client: $addr FRAME_RX write failed status=$status")
                    synchronized(state.writeLock) {
                        state.pendingWrites.clear()
                        state.writeInFlight = false
                    }
                    // Start the poll anyway. Sending and receiving are independent: for a
                    // small-MTU peer (<229) notifications cannot carry a whole frame, so the
                    // RSSI poll's read-back IS the only receive path. Gating it on the first
                    // write succeeding meant one transient GATT_BUSY left the link "connected"
                    // but permanently unable to hear that peer, with no retry.
                    if (!state.initialWriteDone) {
                        state.initialWriteDone = true
                        onDebug("gatt client: $addr initial write failed; starting RSSI poll anyway")
                        startRssiPoll(gatt, state)
                    }
                    return
                }
                // C5: continue a chunk chain, or finish.
                val more = synchronized(state.writeLock) {
                    if (state.pendingWrites.isNotEmpty()) {
                        val service = gatt.getService(SERVICE_UUID)
                        val char = service?.getCharacteristic(CHAR_FRAME_RX)
                        if (char != null) writeNextChunkLocked(gatt, state, char)
                        true
                    } else {
                        state.writeInFlight = false
                        false
                    }
                }
                if (!more && !state.initialWriteDone) {
                    state.initialWriteDone = true
                    onDebug("gatt client: $addr initial FRAME_RX write ok; starting RSSI poll")
                    startRssiPoll(gatt, state)
                }
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                state.lastRssi = rssi
            }
        }
    }

    private fun doInitialRead(gatt: BluetoothGatt, state: PeerState) {
        val addr = state.address
        val service = gatt.getService(SERVICE_UUID) ?: return
        val char = service.getCharacteristic(CHAR_FRAME_TX) ?: return
        try {
            val ok = gatt.readCharacteristic(char)
            if (!ok) {
                onDebug("gatt client: $addr readCharacteristic returned false")
                // Proceed to write anyway
                state.initialReadDone = true
                doInitialWrite(gatt, state)
            }
        } catch (e: SecurityException) {
            onDebug("readCharacteristic[$addr] SecurityException: ${e.message}")
            disconnectPeer(state)
        }
    }

    private fun doInitialWrite(gatt: BluetoothGatt, state: PeerState) {
        writeFrameToGatt(gatt, state, currentFrame)
    }

    /** Write [frame] to the FRAME_RX characteristic of a connected peer. */
    private fun writeFrameToGatt(gatt: BluetoothGatt, state: PeerState, frame: ByteArray) {
        val addr = state.address
        val service = gatt.getService(SERVICE_UUID) ?: return
        val char = service.getCharacteristic(CHAR_FRAME_RX) ?: return
        // C5: below MTU_MIN_FOR_NOTIFY a 226-byte write just fails — chunk the frame into
        // (mtu-5)-byte payloads with a 2-byte header; the peer's server reassembles.
        if (state.mtu < MTU_MIN_FOR_NOTIFY) {
            enqueueChunkedWrite(gatt, state, char, frame)
            return
        }
        writeOne(gatt, state, char, frame)
    }

    private fun enqueueChunkedWrite(
        gatt: BluetoothGatt,
        state: PeerState,
        char: BluetoothGattCharacteristic,
        frame: ByteArray
    ) {
        val payload = (state.mtu - 5).coerceAtLeast(8)
        synchronized(state.writeLock) {
            var offset = 0
            var seq = 0
            while (offset < frame.size) {
                val n = minOf(payload, frame.size - offset)
                val chunk = ByteArray(2 + n)
                chunk[0] = CHUNK_MAGIC
                chunk[1] = seq.toByte()
                frame.copyInto(chunk, 2, offset, offset + n)
                state.pendingWrites.add(chunk)
                offset += n
                seq++
            }
            if (!state.writeInFlight) {
                state.writeInFlight = true
                writeNextChunkLocked(gatt, state, char)
            }
        }
    }

    /** Caller must hold state.writeLock. */
    private fun writeNextChunkLocked(
        gatt: BluetoothGatt,
        state: PeerState,
        char: BluetoothGattCharacteristic
    ) {
        val next = state.pendingWrites.poll()
        if (next == null) {
            state.writeInFlight = false
            return
        }
        val ok = writeOne(gatt, state, char, next)
        if (!ok) {
            state.pendingWrites.clear()
            state.writeInFlight = false
        }
    }

    /** Fire one ATT write. Returns false when the stack refused it outright. */
    private fun writeOne(
        gatt: BluetoothGatt,
        state: PeerState,
        char: BluetoothGattCharacteristic,
        data: ByteArray
    ): Boolean {
        val addr = state.address
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val result = gatt.writeCharacteristic(
                    char, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                if (result != BluetoothGatt.GATT_SUCCESS) {
                    onDebug("gatt client: $addr writeCharacteristic result=$result")
                }
                result == BluetoothGatt.GATT_SUCCESS
            } else {
                @Suppress("DEPRECATION")
                char.value = data
                @Suppress("DEPRECATION")
                char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                @Suppress("DEPRECATION")
                val ok = gatt.writeCharacteristic(char)
                if (!ok) onDebug("gatt client: $addr writeCharacteristic returned false")
                ok
            }
        } catch (e: SecurityException) {
            onDebug("writeCharacteristic[$addr] SecurityException: ${e.message}")
            false
        } catch (e: Exception) {
            onDebug("writeCharacteristic[$addr] exception: ${e.message}")
            false
        }
    }

    /** Start a periodic RSSI read on a 5 s cadence for [state]'s connection. */
    private fun startRssiPoll(gatt: BluetoothGatt, state: PeerState) {
        val timer = java.util.Timer("rssi-${state.address}", true)
        state.rssiPollHandle = timer
        timer.scheduleAtFixedRate(object : java.util.TimerTask() {
            override fun run() {
                if (!state.connected || state.gatt == null) {
                    cancel()
                    return
                }
                try {
                    gatt.readRemoteRssi()
                    // C5: below MTU_MIN_FOR_NOTIFY the peer's 226-byte notifications never
                    // arrive — poll-read FRAME_TX instead (server supports offset reads,
                    // so the stack's blob-read assembles the full frame).
                    // Skip the read while a chunked write is in flight: Android serializes
                    // one GATT operation per connection, so an overlapping read makes the
                    // next writeCharacteristic return false and writeNextChunkLocked drops
                    // the whole queue — losing that frame to this peer entirely. The next
                    // tick 5 s later picks the read back up.
                    val busy = synchronized(state.writeLock) { state.writeInFlight }
                    if (state.mtu < MTU_MIN_FOR_NOTIFY && !busy) {
                        val char = gatt.getService(SERVICE_UUID)?.getCharacteristic(CHAR_FRAME_TX)
                        if (char != null) gatt.readCharacteristic(char)
                    }
                } catch (e: SecurityException) {
                    onDebug("readRemoteRssi[${state.address}] SecurityException: ${e.message}")
                    cancel()
                } catch (e: Exception) {
                    cancel()
                }
            }
        }, RSSI_POLL_MS, RSSI_POLL_MS)
    }

    /** Write the current frame to FRAME_RX on every connected peer. */
    private fun writeToPeers(frame: ByteArray) {
        for ((_, state) in peers) {
            if (!state.connected) continue
            val gatt = state.gatt ?: continue
            writeFrameToGatt(gatt, state, frame)
        }
    }

    private fun disconnectPeer(state: PeerState) {
        state.rssiPollHandle?.cancel()
        state.rssiPollHandle = null
        state.connected = false
        state.connecting = false
        state.notifyEnabled = false
        state.initialReadDone = false
        state.initialWriteDone = false
        synchronized(state.writeLock) {
            state.pendingWrites.clear()
            state.writeInFlight = false
        }
        state.lastDisconnectMs = System.currentTimeMillis()
        try {
            state.gatt?.close()
        } catch (e: Exception) {
            onDebug("gatt close[${state.address}] exception: ${e.message}")
        }
        state.gatt = null
        // Keep state in map so back-off timer works; onPeerSeen will reconnect after back-off
        onDebug("gatt client: ${state.address} disconnected; back-off ${RECONNECT_BACKOFF_MS}ms")
    }
}

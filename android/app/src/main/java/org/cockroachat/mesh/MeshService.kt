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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uniffi.mesh_core.BeaconFfi
import uniffi.mesh_core.FfiDedup
import uniffi.mesh_core.FfiTrust
import uniffi.mesh_core.beaconEntropy
import uniffi.mesh_core.defaultTtlRegional
import uniffi.mesh_core.frameBodyText
import uniffi.mesh_core.frameDecodes
import uniffi.mesh_core.frameEpoch
import uniffi.mesh_core.frameHash
import uniffi.mesh_core.frameMark
import uniffi.mesh_core.frameTtl
import uniffi.mesh_core.frameVerifySelf
import uniffi.mesh_core.frameWitnessParts
import uniffi.mesh_core.makeMessageFrame
import uniffi.mesh_core.makeMessageFrameWithWitness
import uniffi.mesh_core.makePrivateFrame
import uniffi.mesh_core.openPrivateFrame
import uniffi.mesh_core.panicWipe
import uniffi.mesh_core.pocpSketchToDivSketch
import uniffi.mesh_core.pocpVerifyWitnessLocal
import uniffi.mesh_core.relayFrame
import uniffi.mesh_core.wasPanicWiped
import java.security.SecureRandom

class MeshService : LifecycleService() {

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "mesh"
        const val CHANNEL_NAME = "Mesh BLE"

        /** Start the service with this action to trigger an immediate panic wipe. */
        const val ACTION_PANIC = "org.cockroachat.mesh.ACTION_PANIC"

        // Private-send nonce counter. Own prefs file (NOT PairStore's "mesh_pairing_v2");
        // performPanicWipe deletes this file explicitly.
        const val PAIR_PREFS_NAME = "mesh_pairing"
        const val PRIVATE_COUNTER_KEY = "privateSendCounter"

        /**
         * Trigger a panic wipe from anywhere. Sets the Rust flag (the running service's
         * epoch-loop poller picks it up within 1 s) AND starts the service with
         * ACTION_PANIC so the wipe also runs when the service is not currently running.
         */
        fun requestPanicWipe(ctx: Context) {
            panicWipe()
            val intent = Intent(ctx, MeshService::class.java).setAction(ACTION_PANIC)
            try {
                ctx.startService(intent)
            } catch (_: Exception) {
                // Background-start restrictions: the Rust flag is set; any future
                // service start checks it below before doing anything else.
            }
        }
    }

    private lateinit var seed: ByteArray
    private lateinit var beacon: BeaconFfi
    private lateinit var radio: BleRadio
    private lateinit var gattPlane: GattPlane

    /** The normal public frame restored after a temporary private transmission. */
    private var currentPublicFrame: ByteArray? = null
    private var privateTransportActive = false

    /** Hash of our currently-advertised public frame — used to hear our own reflection
     *  coming back through the mesh (send-and-listen). Cleared after one reaction. */
    private var ownFrameHash: ByteArray? = null

    /** LocalImmediate marks heard this epoch (for beacon entropy collection).
     *  Guarded by [marksLock]: ingest runs on BLE binder threads, the epoch loop on main. */
    private val marksLock = Any()
    private val localImmediateMarks = mutableListOf<ByteArray>()

    // Per-service-start dedup table: catches the same frame arriving many times per epoch
    // via extended advertising or GATT (normal behaviour — not a protocol error).
    private val dedup = FfiDedup(4096u)

    // H2: per-service-start trust accumulator for multi-locale diversity
    private val trust = FfiTrust()

    // Rate-limit epoch-skew log: only log when the (frameEpoch, ownEpoch) pair changes.
    private var lastSkewPair: Pair<UInt, UInt>? = null

    // Track whether the first frame of the current epoch has been logged.
    private var firstFrameEpoch: UInt? = null

    // Repeated-text suppression: maps text -> ownEpoch when last seen (display-only).
    // Guarded by itself: ingestFrame runs concurrently on BLE binder threads.
    private val recentTexts = HashMap<String, UInt>()

    // K9: guard against duplicate onStartCommand initialization (MainActivity + ChatActivity
    // can both startForegroundService on the live instance).
    private var started = false

    // K3: panic wipe is one-shot; subsequent triggers are no-ops.
    private var wiped = false

    // R1 mitigation: div_sketch reuse across distinct sender marks within one epoch is a
    // copy-attack signal (see pocp.rs "RESIDUAL GAP"). Soft response: log only — tiny cells
    // can legitimately produce identical sketches. Keyed by div_sketch hex → first mark hex.
    private val sketchSeenLock = Any()
    private val sketchSeen = HashMap<String, String>()
    private var sketchSeenEpoch: UInt = 0u

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // K3: explicit panic request — wipe immediately, whether or not we were running.
        // Also honors a wipe requested while the service was stopped (Rust flag survived).
        if (intent?.action == ACTION_PANIC || wasPanicWiped()) {
            startForeground(NOTIFICATION_ID, buildNotification("Wiping…"))
            performPanicWipe()
            return START_NOT_STICKY
        }

        // K9: duplicate starts (MainActivity btnStart + ChatActivity swRun both call
        // startForegroundService on the live instance) must not re-init identity,
        // collectors, or the epoch loop.
        if (started) return START_STICKY
        started = true

        startForeground(NOTIFICATION_ID, buildNotification("Starting…"))

        MeshState.running.value = true
        MeshState.logDebug("service started")

        seed = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val beaconSeed0 = ByteArray(32).also { SecureRandom().nextBytes(it) }
        beacon = BeaconFfi(beaconSeed0)
        radio = BleRadio(this)

        // Wire debug callback before any radio operations
        radio.onDebug = { MeshState.logDebug(it) }

        // Log radio capability once at start
        MeshState.logDebug(radio.capabilityReport())

        // Set up GATT fallback plane
        gattPlane = GattPlane(
            ctx = this,
            onFrame = { bytes, rssi -> ingestFrame(bytes, rssi) },
            onDebug = { MeshState.logDebug(it) }
        )
        gattPlane.start()

        // Start scanning — frames go to ingest; mesh peers without frame data (no extended
        // advertising support) are handed to the GATT fallback plane for connection.
        val cfg = MeshState.config
        radio.startScanning(
            cfg.scanLowLatency,
            onLegacyPeer = { device, rssi -> gattPlane.onPeerSeen(device, rssi) }
        ) { bytes, rssi ->
            ingestFrame(bytes, rssi)
        }

        // Collect outgoing text changes: rebuild frame and re-advertise immediately on change
        lifecycleScope.launch {
            MeshState.outgoingText.collect { text ->
                val cfg2 = MeshState.config
                val nowMs = System.currentTimeMillis()
                val epoch = (nowMs / cfg2.epochMs).toUInt()
                if (text.isNotEmpty()) {
                    MeshState.outgoingSetAtEpoch = epoch
                } else {
                    MeshState.outgoingSetAtEpoch = null
                }
                rebuildAndAdvertise(epoch, cfg2, text)
            }
        }

        // Tier switch (Local/Broadcast) takes effect immediately, not at the next epoch:
        // rebuild the current frame with the new TTL as soon as the tier changes.
        lifecycleScope.launch {
            MeshState.outgoingTier.collect {
                val cfg2 = MeshState.config
                val epoch = (System.currentTimeMillis() / cfg2.epochMs).toUInt()
                rebuildAndAdvertise(epoch, cfg2, MeshState.outgoingText.value)
            }
        }

        // Private (Tier-3) send: one-shot. Solving the VDL witness blocks for seconds, so it
        // runs on the default dispatcher. The sealed frame is advertised for a window; relays
        // with a valid witness carry it regionally (no per-epoch re-solve).
        //
        // Nonce safety: a monotonic counter is loaded, incremented, and persisted before each
        // private send. The counter goes into div_sketch[0..8] and forms the AEAD nonce suffix
        // (epoch_be || counter_be), preventing nonce reuse under the same pair key within an epoch.
        lifecycleScope.launch {
            MeshState.outgoingPrivate.collect { ps ->
                if (ps == null) return@collect
                val cfg2 = MeshState.config
                val prefs = getSharedPreferences(PAIR_PREFS_NAME, Context.MODE_PRIVATE)
                val counter = (prefs.getLong(PRIVATE_COUNTER_KEY, 0L) + 1L).also {
                    prefs.edit().putLong(PRIVATE_COUNTER_KEY, it).commit()
                }
                MeshState.logDebug("sealing private message (VDL solve, ~seconds of CPU)… counter=$counter")
                val frame = withContext(Dispatchers.Default) {
                    val epoch = (System.currentTimeMillis() / cfg2.epochMs).toUInt()
                    val beaconSeed = beacon.seed()
                    makePrivateFrame(seed, epoch, beaconSeed, ps.pairKey, ps.text, counter.toULong())
                }
                if (frame != null) {
                    val windowMs = maxOf(cfg2.messageRepeatEpochs.toLong() * cfg2.epochMs, 6_000L)
                    // A phone usually supports one advertising set.  Reuse the primary set for
                    // this window (rather than opening a second one), and push the same frame to
                    // GATT peers so legacy-advertising devices receive private messages too.
                    val restoreFrame = currentPublicFrame
                    if (restoreFrame == null) {
                        MeshState.logDebug("private send delayed: public advertising is not ready")
                    } else {
                        privateTransportActive = true
                        val started = radio.advertisePrivateOnce(
                            frame = frame,
                            durationMs = windowMs,
                            restoreFrame = restoreFrame,
                            codedPhy = cfg2.codedPhy,
                            advIntervalMs = cfg2.advIntervalMs
                        ) {
                            privateTransportActive = false
                            gattPlane.currentFrame = currentPublicFrame ?: restoreFrame
                            MeshState.logDebug("private advertising window ended; public frame restored")
                        }
                        if (started) {
                            gattPlane.currentFrame = frame
                        } else {
                            privateTransportActive = false
                            MeshState.logDebug("private send skipped: another private window is active")
                        }
                    }
                    // Insert our own frame hash so the relayed echo doesn't come back as incoming.
                    val ownEpoch = (System.currentTimeMillis() / cfg2.epochMs).toUInt()
                    frameHash(frame)?.let { dedup.checkAndInsertEpoch(it, ownEpoch) }
                    MeshState.logDebug("private message sealed + advertised (${windowMs}ms window)")
                } else {
                    MeshState.logDebug("private seal failed (text > 47 bytes or bad key)")
                }
                MeshState.outgoingPrivate.value = null
            }
        }

        // Epoch loop
        lifecycleScope.launch {
            var lastEpoch = UInt.MAX_VALUE
            // Grace period before the advertising watchdog may fire: the very first
            // startAdvertisingSet is async, and firing inside that window double-started
            // the hardware set (frozen-frame bug).
            var lastAdvRestartMs = System.currentTimeMillis()
            while (isActive) {
                val cfg = MeshState.config
                val nowMs = System.currentTimeMillis()
                val epoch = (nowMs / cfg.epochMs).toUInt()

                if (epoch != lastEpoch) {
                    lastEpoch = epoch

                    // M5b: advance beacon chain with LocalImmediate entropy.
                    // If too few hearers, fallback to zero-entropy chaining.
                    val nowMs = System.currentTimeMillis()
                    val marksFlat = synchronized(marksLock) {
                        val flat = localImmediateMarks.flatMap { it.toList() }.toByteArray()
                        localImmediateMarks.clear()
                        flat
                    }
                    val ent = beaconEntropy(marksFlat, cfg.minHearers.toUInt())
                    val advanced = if (ent != null) {
                        beacon.advance(ent, nowMs.toULong(), cfg.beaconFloorMs.toULong())
                    } else {
                        beacon.advanceFallback(nowMs.toULong(), cfg.beaconFloorMs.toULong())
                    }
                    if (advanced) {
                        MeshState.logDebug(
                            "beacon advanced: epoch=${beacon.epoch()} low_entropy=${beacon.isLowEntropy()}"
                        )
                    }

                    // Outgoing auto-expire
                    val text = MeshState.outgoingText.value
                    if (text.isNotEmpty()) {
                        val setAt = MeshState.outgoingSetAtEpoch
                        if (setAt != null && epoch >= setAt && epoch - setAt >= cfg.messageRepeatEpochs.toUInt()) {
                            MeshState.outgoingText.value = ""
                            MeshState.logDebug("outgoing message expired after ${cfg.messageRepeatEpochs} epochs")
                        }
                    }

                    // Build and advertise a new frame for this epoch. Always a full
                    // stop+start of the advertising set: setAdvertisingData() is silently
                    // broken on real stacks (field-observed: off-air on Samsung, stale-on-
                    // air on MIUI, success returned both times), while stop+start frames
                    // were demonstrably heard by the peer.
                    val currentText = MeshState.outgoingText.value
                    rebuildAndAdvertise(epoch, cfg, currentText)

                    // Log epoch rollover with neighbor/total counts
                    val neighbors = MeshState.measurement.neighborsThisEpoch(epoch)
                    val total = MeshState.measurement.totalHeard()
                    MeshState.logDebug(
                        "epoch rollover: epoch=$epoch neighbors=$neighbors total=$total"
                    )
                }

                // Recompute stats and push to state
                val sketch = MeshState.measurement.localSketch(epoch, seed, cfg.rssiFloorDbm)
                // Presence must not depend on the remote device sharing our epoch.  The KMV rig
                // keeps its epoch buckets below, but the user-facing nearby count is a recent
                // direct-RF observation window.  A rotating mark can make a peer count twice at
                // an epoch boundary; showing zero for a peer that is actively delivering frames
                // is substantially worse and was the observed failure mode.
                val neighbors = maxOf(
                    MeshState.measurement.neighborsThisEpoch(epoch),
                    MeshState.measurement.neighborsThisEpoch(epoch - 1u),
                    MeshState.measurement.neighborsThisEpoch(epoch + 1u),
                    MeshState.measurement.neighborsRecently(
                        windowMs = maxOf(cfg.epochMs * 2L, 15_000L)
                    )
                )
                val total = MeshState.measurement.totalHeard()

                val stats = Stats(
                    epoch = epoch,
                    neighborsThisEpoch = neighbors,
                    totalHeard = total,
                    localSketch = sketch,
                    advertising = true,
                    scanning = true,
                    codedPhyActive = cfg.codedPhy && radio.codedPhySupported(),
                    note = radio.advMode
                )
                MeshState.stats.value = stats

                // Update notification
                val notifText = "Epoch $epoch | neighbors=$neighbors | total=$total"
                val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notifManager.notify(NOTIFICATION_ID, buildNotification(notifText))

                // B1: check panic-wipe flag (set by Rust core or by panic-wipe button).
                if (wasPanicWiped()) {
                    performPanicWipe()
                    return@launch  // exit the epoch loop
                }

                // Advertising self-heal: the controller can reclaim the hardware set (relay
                // bursts, GATT connections, stack hiccups). The per-epoch rebuild would take
                // up to a full epoch to notice — re-advertise within ~1 s instead.
                // Rate-limited: a dead/off BT stack must not spam restart attempts.
                if (!privateTransportActive && !radio.advertisingActive() &&
                    nowMs - lastAdvRestartMs >= 5_000L
                ) {
                    lastAdvRestartMs = nowMs
                    MeshState.logDebug("advertising set inactive — re-advertising current frame")
                    rebuildAndAdvertise(epoch, cfg, MeshState.outgoingText.value)
                }

                delay(1_000L)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        radio.stop()
        gattPlane.stop()
        MeshState.running.value = false
        MeshState.stats.value = MeshState.stats.value.copy(
            advertising = false,
            scanning = false
        )
        MeshState.logDebug("service stopped")
        super.onDestroy()
    }

    /**
     * Single ingest path for received frames, shared by the BLE scan callback and GattPlane.
     *
     * Invariants enforced:
     *   - Byte arrays that are not exactly 226 bytes are silently dropped (checked by frameDecodes
     *     in Rust core, which returns false for any length != 226).
     *   - No frame bytes are parsed in Kotlin; all interpretation is done by Rust core functions.
     */
    private fun ingestFrame(bytes: ByteArray, rssi: Int) {
        val cfg = MeshState.config
        val nowMs = System.currentTimeMillis()
        val ownEpoch = (nowMs / cfg.epochMs).toUInt()

        if (!frameDecodes(bytes)) return
        if (!frameVerifySelf(bytes)) return
        val hash = frameHash(bytes)
        val frameEp = frameEpoch(bytes)
        if (hash == null || frameEp == null) return

        // K4: epoch freshness — reject frames whose epoch is more than 2 buckets from ours
        // (before dedup, so a stale/future frame never occupies a dedup slot).
        val diff = frameEp.toLong() - ownEpoch.toLong()
        if (diff > 2 || diff < -2) {
            val pair = Pair(frameEp, ownEpoch)
            if (pair != lastSkewPair) {
                lastSkewPair = pair
                MeshState.logDebug(
                    "epoch skew: frame=$frameEp own=$ownEpoch (check epochMs match + clocks)"
                )
            }
            return
        }

        val mark = frameMark(bytes) ?: return
        val wp = frameWitnessParts(bytes)

        // Send-and-listen (spec): a copy of OUR OWN frame coming back over the relay path
        // means the mesh is carrying it — stop re-originating at the next epoch boundary
        // instead of repeating blindly. Our hash went into dedup at origination, so this
        // check must run BEFORE the dedup gate. TTL sits outside the hashed region, so the
        // relayed echo hashes identically to our original.
        val ownHash = ownFrameHash
        if (ownHash != null && hash.contentEquals(ownHash)) {
            ownFrameHash = null // react once per origination
            val repeat = cfg.messageRepeatEpochs.toLong()
            if (repeat > 0 && MeshState.outgoingText.value.isNotEmpty()) {
                MeshState.outgoingSetAtEpoch =
                    (ownEpoch.toLong() + 1L - repeat).coerceAtLeast(0L).toUInt()
                MeshState.logDebug("reflection heard: mesh is carrying our message; stopping re-origination")
            }
        }

        // Presence: direct-RF liveness only, BEFORE the dedup gate. LocalImmediate frames
        // are never relayed → always direct. Regional/Private count only at the ORIGINATION
        // TTL (relays always decrement, so ttl == default_ttl_regional ⇔ straight from the
        // originator); relayed copies must not register the originator as "nearby".
        // Deliberately no RSSI floor: any frame that decoded + verified is a real
        // transmission. The −80 dBm config floor is a sketch/trust window, NOT a liveness
        // window — applying it here made the count flicker at the boundary while messages
        // kept flowing.
        val originTtl = defaultTtlRegional().toInt()
        val direct = when (wp?.msgType?.toInt()) {
            1 -> true // LocalImmediate: never relayed → always direct
            2, 3 -> frameTtl(bytes)?.toInt() == originTtl
            else -> false
        }
        if (direct) MeshState.measurement.recordPresence(mark, rssi)

        if (!dedup.checkAndInsertEpoch(hash, frameEp)) return

        // Log first frame heard each epoch
        if (firstFrameEpoch != ownEpoch) {
            firstFrameEpoch = ownEpoch
            MeshState.logDebug("first frame heard in epoch $ownEpoch")
        }

        MeshState.measurement.record(mark, rssi, frameEp)

        // K7 + K5: collect LocalImmediate marks for beacon entropy. Only LocalImmediate
        // frames above the RSSI floor count as direct-RF co-presence witnesses.
        if (wp != null && wp.msgType.toInt() == 1 && rssi >= cfg.rssiFloorDbm) {
            synchronized(marksLock) { localImmediateMarks.add(mark) }
        }

        // K2: Tier-3 private (msgType 3). Trial-decrypt against every paired contact key.
        // E3: iterate ALL contacts unconditionally (no early break) — the NUMBER of decrypt
        // calls must not leak which contact index matched (timing side-channel).
        if (wp != null && wp.msgType.toInt() == 3) {
            var privatePlaintext: String? = null
            var privateLabel: String? = null
            for (contact in PairStore.contacts(this)) {
                val pt = openPrivateFrame(bytes, contact.pairKey)
                if (pt != null && privatePlaintext == null) {
                    privatePlaintext = pt
                    privateLabel = contact.label
                }
            }
            if (privatePlaintext != null) {
                MeshState.appendMessage(
                    MsgRow(
                        tsMs = System.currentTimeMillis(),
                        epoch = frameEp,
                        markHexPrefix = "🔒 $privateLabel",
                        rssi = rssi,
                        text = privatePlaintext,
                        mine = false,
                        tier = SendTier.PRIVATE
                    )
                )
            }
            // Relay regardless of whether we could decrypt (multi-hop delivery).
            relayFrame(bytes)?.let { radio.advertiseRelayOnce(it, 2000L) }
            return
        }

        // Public path (msgType 1/2).
        var pocpOk = true
        var relayOnly = false
        if (wp != null) {
            val localSketch = MeshState.stats.value?.localSketch ?: emptyList<ULong>()
            val hasWitness = wp.pocpWit.any { it != 0.toByte() } ||
                wp.divSketch.any { it != 0.toByte() }
            if (hasWitness) {
                if (localSketch.isNotEmpty()) {
                    val verdict = pocpVerifyWitnessLocal(
                        localSketch,
                        wp.divSketch,
                        wp.epoch,
                        wp.framePrefix,
                        wp.pocpWit,
                        cfg.tauThreshold,
                    )
                    when (verdict.toInt()) {
                        0 -> {} // Valid
                        1 -> if (wp.msgType.toInt() == 2) relayOnly = true else pocpOk = false // CellMismatch
                        else -> pocpOk = false // Stale / bad MAC
                    }
                } else {
                    pocpOk = false // no local sketch → cannot verify → drop
                }

                // R1: soft detection of div_sketch reuse across distinct marks (copy signal).
                synchronized(sketchSeenLock) {
                    if (sketchSeenEpoch != frameEp) {
                        sketchSeen.clear()
                        sketchSeenEpoch = frameEp
                    }
                    val divHex = wp.divSketch.joinToString("") { "%02x".format(it) }
                    val markHex = mark.joinToString("") { "%02x".format(it) }
                    val prev = sketchSeen[divHex]
                    if (prev == null) {
                        sketchSeen[divHex] = markHex
                    } else if (prev != markHex) {
                        MeshState.logDebug("R1: div_sketch reuse across distinct marks (copy signal)")
                    }
                }
            }

            // H2: BroadcastCHAT multi-locale diversity gate — only when it would display.
            if (pocpOk && !relayOnly && wp.msgType.toInt() == 2) {
                val distinct = trust.recordVerification(wp.bodyHash, wp.divSketch, cfg.tauThreshold)
                if (distinct < 2u) relayOnly = true // insufficient corroboration: relay, don't display
            }
        }

        // Relay if the frame is either displayable or relay-only.
        if (pocpOk || relayOnly) {
            relayFrame(bytes)?.let { radio.advertiseRelayOnce(it, 2000L) }
        }

        // Display only when fully verified and not relay-only.
        if (pocpOk && !relayOnly) {
            val text = frameBodyText(bytes)
            if (!text.isNullOrEmpty()) {
                var suppress = false
                synchronized(recentTexts) {
                    val prevEpoch = recentTexts[text]
                    suppress = prevEpoch != null &&
                        ownEpoch >= prevEpoch &&
                        ownEpoch - prevEpoch <= 3u
                    recentTexts[text] = ownEpoch
                    if (recentTexts.size > 64) {
                        val iter = recentTexts.iterator()
                        while (iter.hasNext()) {
                            val e = iter.next()
                            if (ownEpoch >= e.value && ownEpoch - e.value > 6u) iter.remove()
                        }
                    }
                }
                if (!suppress) {
                    val markHex = mark.joinToString("") { "%02x".format(it) }
                    val tier = if (wp?.msgType?.toInt() == 1) SendTier.LOCAL else SendTier.BROADCAST
                    MeshState.appendMessage(
                        MsgRow(
                            tsMs = System.currentTimeMillis(),
                            epoch = frameEp,
                            markHexPrefix = markHex.take(8),
                            rssi = rssi,
                            text = text,
                            mine = false,
                            tier = tier
                        )
                    )
                }
            }
        }
    }

    /**
     * Build a message frame for [epoch] carrying [text], start advertising it, and push it to the
     * GATT plane (triggers notify + writes to connected peers).
     * If [text] is too long (> 63 UTF-8 bytes) the fact is logged and the frame falls back
     * to empty text via makeMessageFrame with an empty string.
     */
    private fun rebuildAndAdvertise(epoch: UInt, cfg: MeshConfig, text: String) {
        val effectiveText = if (text.toByteArray(Charsets.UTF_8).size > 63) {
            MeshState.logDebug(
                "outgoing text too long (${text.toByteArray(Charsets.UTF_8).size} UTF-8 bytes, max 63); " +
                    "falling back to empty"
            )
            ""
        } else {
            text
        }

        val beaconSeed = beacon.seed()
        val localImmediate = MeshState.outgoingTier.value == SendTier.LOCAL
        val ttl: UByte = if (localImmediate) 0u else 8u
        // H1: include PoCP witness so receivers can verify physical co-presence.
        // Falls back to bare makeMessageFrame when the local sketch is unavailable.
        val sketch = MeshState.measurement.localSketch(epoch, seed, cfg.rssiFloorDbm)
        val divSketch = pocpSketchToDivSketch(sketch)
        val frame = if (divSketch != null) {
            makeMessageFrameWithWitness(seed, epoch, beaconSeed, localImmediate, effectiveText, ttl, divSketch)
        } else {
            makeMessageFrame(seed, epoch, beaconSeed, localImmediate, effectiveText)
        }
        if (frame != null) {
            currentPublicFrame = frame
            // Insert our own frame's hash into dedup: a relayed copy of our frame comes back
            // with TTL decremented, but TTL sits in the hop-mutable region excluded from the
            // frame hash — so the echo has OUR hash and dedup drops it instead of showing our
            // own message as incoming.
            ownFrameHash = frameHash(frame)
            ownFrameHash?.let { dedup.checkAndInsertEpoch(it, epoch) }
            if (!privateTransportActive) {
                // Full stop+start every epoch — see BleRadio note on setAdvertisingData.
                radio.startAdvertising(frame, cfg.codedPhy, cfg.advIntervalMs)
                // Push to GATT plane: notifies subscribed centrals and writes to connected peripherals.
                gattPlane.currentFrame = frame
            }
        } else {
            MeshState.logDebug("frame origination returned null for epoch=$epoch")
        }
    }

    /**
     * B1: emergency panic-wipe. Clears all persisted key material, configuration, measurement
     * data, and measurement export files. Then stops the BLE service and removes the persistent
     * notification. After this call the device is sterile (no trace of mesh activity remains on
     * the filesystem).
     *
     * Call from the Rust panic flag poller (epoch loop) or the UI panic button.
     */
    private fun performPanicWipe() {
        // K3: one-shot — subsequent triggers are no-ops.
        if (wiped) return
        wiped = true
        MeshState.logDebug("!!! PANIC WIPE initiated")
        try {
            // Clear Rust in-memory state (the flag was already set; we call the function).
            panicWipe()

            // Clear Android persisted state.
            PairStore.wipe(this)
            ConfigStore.clear(this)
            // Private-send nonce counter file and crash log.
            getSharedPreferences(PAIR_PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
            getSharedPreferences("crash_log", Context.MODE_PRIVATE).edit().clear().commit()

            // Clear in-memory measurement data.
            MeshState.measurement.clear()
            MeshState.messages.value = emptyList()
            MeshState.debugLog.value = listOf("!!! PANIC WIPE at ${System.currentTimeMillis()}")
            MeshState.outgoingText.value = ""
            MeshState.outgoingPrivate.value = null

            // Zeroize in-memory secrets. seed is lateinit — a cold-start ACTION_PANIC
            // (service never fully started) reaches here before seed is assigned.
            if (::seed.isInitialized) seed.fill(0)
            currentPublicFrame?.fill(0)
            currentPublicFrame = null
            synchronized(marksLock) {
                localImmediateMarks.forEach { it.fill(0) }
                localImmediateMarks.clear()
            }

            // Stop radio and GATT.
            radio.stop()
            gattPlane.stop()

            // Remove foreground notification and stop the service.
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } catch (e: Exception) {
            android.util.Log.e("MeshService", "panic-wipe error: ${e.message}")
            // Kill process if cleanup fails — any residual data is an unacceptable risk.
            android.os.Process.killProcess(android.os.Process.myPid())
        }
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

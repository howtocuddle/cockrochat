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
import uniffi.mesh_core.defaultTtlLocal
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
import uniffi.mesh_core.openPrivateBodyOnly
import uniffi.mesh_core.panicWipe
import uniffi.mesh_core.pocpSketchToDivSketch
import uniffi.mesh_core.pocpVerifyWitnessLocal
import uniffi.mesh_core.relayFrame
import uniffi.mesh_core.vdlCheckFrame
import uniffi.mesh_core.wasPanicWiped
import java.security.SecureRandom

class MeshService : LifecycleService() {

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "mesh"
        const val CHANNEL_NAME = "Mesh BLE"

        /** Start the service with this action to trigger an immediate panic wipe. */
        const val ACTION_PANIC = "org.cockroachat.mesh.ACTION_PANIC"

        // Legacy plaintext counter prefs file (B4/C9: counter moved into PairStore's
        // encrypted store with a per-epoch random base). performPanicWipe still deletes
        // this file explicitly to erase traces left by older installs.
        const val PAIR_PREFS_NAME = "mesh_pairing"

        /** B2: hard cap on LOCAL re-broadcast lifetime. An unheard local alert must not
         *  scream every epoch forever (battery + stale-danger re-airing hours later). */
        const val LOCAL_REBROADCAST_WINDOW_MS = 30 * 60_000L

        /** B1/B2: after the first reflected echo, LOCAL messages re-air sparsely (every
         *  Nth epoch) instead of every epoch — a single forged echo can no longer silence
         *  the alert, but battery use stays bounded until the hard cap. */
        const val LOCAL_SPARSE_EVERY_N_EPOCHS = 4L

        /** B8: relay queue bound; lowest-priority tasks are evicted when full. */
        const val RELAY_QUEUE_CAP = 64

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

    /** Set when a relayed echo of our frame is heard (receipt). B1: an echo proves only
     *  that ONE (possibly adversarial) peer relayed us once — LOCAL no longer hard-stops
     *  on it; it switches to sparse re-airing until [LOCAL_REBROADCAST_WINDOW_MS] passes.
     *  Reset when new outgoing text is composed. */
    @Volatile
    private var reflectionHeard = false

    /** Epoch at which the first echo was heard (LOCAL sparse re-air anchor). */
    @Volatile
    private var echoEpoch: UInt? = null

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

    // B8: prioritized relay queue. Priority: LOCAL echo (0) > regional (1) > private (2);
    // FIFO within a class. Drained by a service coroutine whenever the radio has a free
    // hardware slot — frames WAIT instead of being silently dropped (B8 starvation fix).
    private data class RelayTask(val frame: ByteArray, val priority: Int, val seq: Long)
    private val relayQueueLock = Any()
    private val relayQueue = ArrayDeque<RelayTask>()
    private var relaySeq = 0L

    // Repeated-text suppression (display-only). B6: keyed by (text, sender-mark prefix) —
    // an attacker pre-broadcasting the same words can no longer suppress the REAL alert
    // from a different sender. Guarded by itself: ingestFrame runs concurrently.
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
                    reflectionHeard = false // new message → wait for a fresh receipt
                    echoEpoch = null
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

        // Private (Tier-3) send queue (C4). Solving the VDL witness blocks for seconds, so it
        // runs on the default dispatcher; queued sends are processed sequentially. The sealed
        // frame is advertised for a short window; relays with a valid witness carry it
        // regionally (no per-epoch re-solve).
        //
        // A3: the pair key is resolved AND epoch-ratcheted here (v2 contacts) — key material
        // never rides the queue. B4/C9: the nonce counter uses a per-epoch random base from
        // the encrypted store (cross-epoch unlinkability; no plaintext send-volume leak).
        lifecycleScope.launch {
            for (ps in MeshState.privateSends) {
                val cfg2 = MeshState.config
                val epoch = (System.currentTimeMillis() / cfg2.epochMs).toUInt()
                val pairKey = PairStore.keyForSend(this@MeshService, ps.label, epoch)
                if (pairKey == null) {
                    MeshState.logDebug("private send dropped: contact '${ps.label}' unknown or key ratchet failed")
                    continue
                }
                val counter = PairStore.nextPrivateCounter(this@MeshService, epoch)
                MeshState.logDebug("sealing private message → ${ps.label} (VDL solve, ~seconds of CPU)…")
                val frame = withContext(Dispatchers.Default) {
                    val beaconSeed = beacon.seed()
                    makePrivateFrame(seed, epoch, beaconSeed, pairKey, ps.text, counter.toULong())
                }
                if (frame != null) {
                    // C3: cap the window at 6 s. While the private frame uses the primary
                    // advertising set our public presence frame is OFF the air — a 30 s
                    // window made us vanish from neighbors' sketches and cascaded
                    // CellMismatch drops across the cell.
                    val windowMs = minOf(cfg2.messageRepeatEpochs.toLong() * cfg2.epochMs, 6_000L)
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
            }
        }

        // B7/B8: relay queue drain. Relay tasks wait for a free hardware advertising slot
        // (previously frames were silently dropped when 2 relay sets were active — in a
        // busy crowd that starved propagation of real alerts). Every relayed frame also
        // goes to the GATT plane so legacy phones receive multi-hop traffic too (B7).
        lifecycleScope.launch {
            while (isActive) {
                val task = synchronized(relayQueueLock) {
                    if (radio.relayCapacityAvailable()) relayQueue.removeFirstOrNull() else null
                }
                if (task != null) {
                    radio.advertiseRelayOnce(task.frame, 2000L, MeshState.config.codedPhy)
                    gattPlane.relayOnce(task.frame)
                } else {
                    delay(250L)
                }
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
                        if (MeshState.outgoingTier.value == SendTier.LOCAL) {
                            // B2: LOCAL re-originates every epoch until heard back, but a
                            // hard 30-minute cap ends it regardless — alone, a phone would
                            // otherwise scream a stale danger alert every epoch forever.
                            val setAt = MeshState.outgoingSetAtEpoch
                            val maxAge = (LOCAL_REBROADCAST_WINDOW_MS / cfg.epochMs).toUInt().coerceAtLeast(1u)
                            if (setAt != null && epoch >= setAt && epoch - setAt >= maxAge) {
                                MeshState.outgoingText.value = ""
                                MeshState.receipt.value =
                                    "local broadcast stopped after 30 min — re-send if still relevant"
                                MeshState.logDebug("local message expired (30 min re-broadcast cap)")
                            } else if (reflectionHeard && echoEpoch == null) {
                                // B1: the echo switches us to sparse re-airing; it does NOT
                                // stop the message (a single forged echo must not silence it).
                                echoEpoch = epoch
                                MeshState.logDebug("local echo heard — switching to sparse re-airing")
                            }
                        } else {
                            val setAt = MeshState.outgoingSetAtEpoch
                            if (setAt != null && epoch >= setAt && epoch - setAt >= cfg.messageRepeatEpochs.toUInt()) {
                                MeshState.outgoingText.value = ""
                                if (MeshState.receipt.value == null) {
                                    MeshState.receipt.value =
                                        "broadcast stopped after ${cfg.messageRepeatEpochs} epochs — no echo heard"
                                }
                                MeshState.logDebug("outgoing message expired after ${cfg.messageRepeatEpochs} epochs")
                            }
                        }
                    }

                    // Build and advertise a new frame for this epoch. Always a full
                    // stop+start of the advertising set: setAdvertisingData() is silently
                    // broken on real stacks (field-observed: off-air on Samsung, stale-on-
                    // air on MIUI, success returned both times), while stop+start frames
                    // were demonstrably heard by the peer.
                    // B1/B2: after the first echo, LOCAL airs the text only every
                    // LOCAL_SPARSE_EVERY_N_EPOCHS-th epoch (presence frame still rotates).
                    val rawText = MeshState.outgoingText.value
                    val sparseEcho = echoEpoch
                    val currentText = if (
                        rawText.isNotEmpty() &&
                        MeshState.outgoingTier.value == SendTier.LOCAL &&
                        sparseEcho != null &&
                        epoch >= sparseEcho &&
                        (epoch - sparseEcho).toLong() % LOCAL_SPARSE_EVERY_N_EPOCHS != 0L
                    ) "" else rawText
                    rebuildAndAdvertise(epoch, cfg, currentText)

                    // Log epoch rollover with neighbor/total counts
                    val neighbors = MeshState.measurement.neighborsDirect(epoch)
                    val total = MeshState.measurement.totalHeard()
                    MeshState.logDebug(
                        "epoch rollover: epoch=$epoch neighbors=$neighbors total=$total"
                    )
                }

                // Recompute stats and push to state
                val sketch = MeshState.measurement.localSketch(epoch, seed, cfg.rssiFloorDbm)
                // Presence: direct-RF devices counted per epoch bucket. Marks rotate every
                // epoch, so a 15–20 s wall-clock window counted one phone 2–3 times; max
                // over adjacent per-epoch buckets can't double-count (one device = one
                // mark per epoch) and tolerates one fully-missed epoch.
                val neighbors = MeshState.measurement.neighborsDirect(epoch)
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

    /** B8: enqueue a relay task with tier priority, evicting the lowest-priority queued
     *  task when the queue is full (never the new LOCAL echo). */
    private fun enqueueRelay(frame: ByteArray, msgType: Int) {
        val prio = when (msgType) { 1 -> 0; 2 -> 1; else -> 2 }
        synchronized(relayQueueLock) {
            if (relayQueue.size >= RELAY_QUEUE_CAP) {
                val worstIdx = relayQueue.indices.maxByOrNull { relayQueue[it].priority }
                if (worstIdx != null && relayQueue[worstIdx].priority > prio) {
                    relayQueue.removeAt(worstIdx)
                } else {
                    MeshState.logDebug("relay queue full — dropping relay task (prio $prio)")
                    return
                }
            }
            relayQueue.addLast(RelayTask(frame, prio, relaySeq++))
        }
    }

    /**
     * A1/C2: PoCP verification accepting the frame's own epoch sketch OR the previous
     * epoch's completed sketch. Marks rotate every epoch, so a sketch built from epoch N-1
     * marks only ever matches the verifier's N-1 bucket — at rollover an honest sender
     * signs that completed sketch with witness seed N-1, and we must try both.
     *
     * Returns 0 = Valid, 1 = CellMismatch (witness MAC valid but sketches disjoint —
     * an honestly remote cell), 2 = Stale (bad MAC / unverifiable).
     */
    private fun verifyPocpAcrossRollover(
        frameEp: UInt,
        divSketch: ByteArray,
        prefix: ByteArray,
        wit: ByteArray,
        cfg: MeshConfig
    ): Int {
        var macValid = false
        val sketchCur = MeshState.measurement.localSketch(frameEp, seed, cfg.rssiFloorDbm)
        if (sketchCur.isNotEmpty()) {
            val v = pocpVerifyWitnessLocal(sketchCur, divSketch, frameEp, prefix, wit, cfg.tauThreshold).toInt()
            if (v == 0) return 0
            if (v == 1) macValid = true
        }
        if (frameEp > 0u) {
            val sketchPrev = MeshState.measurement.localSketch(frameEp - 1u, seed, cfg.rssiFloorDbm)
            if (sketchPrev.isNotEmpty()) {
                val v = pocpVerifyWitnessLocal(sketchPrev, divSketch, frameEp - 1u, prefix, wit, cfg.tauThreshold).toInt()
                if (v == 0) return 0
                if (v == 1) macValid = true
            }
        }
        return if (macValid) 1 else 2
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
        // means at least one peer relayed it. B1: that echo is NOT a delivery guarantee —
        // a single adversarial device can forge it by relaying once and blackholing the
        // rest. LOCAL therefore only drops to sparse re-airing; BROADCAST still stops at
        // the next epoch boundary (its TTL does the propagating, not our re-origination).
        // Our hash went into dedup at origination, so this check must run BEFORE the dedup
        // gate. TTL sits outside the hashed region, so the relayed echo hashes identically.
        val ownHash = ownFrameHash
        if (ownHash != null && hash.contentEquals(ownHash)) {
            ownFrameHash = null // react once per origination
            reflectionHeard = true
            if (MeshState.outgoingText.value.isNotEmpty()) {
                MeshState.receipt.value =
                    "✓ heard back once — a peer relayed it (not proof of delivery)"
            }
            if (MeshState.outgoingTier.value != SendTier.LOCAL) {
                val repeat = cfg.messageRepeatEpochs.toLong()
                if (repeat > 0 && MeshState.outgoingText.value.isNotEmpty()) {
                    MeshState.outgoingSetAtEpoch =
                        (ownEpoch.toLong() + 1L - repeat).coerceAtLeast(0L).toUInt()
                    MeshState.logDebug("reflection heard: mesh is carrying our broadcast; stopping re-origination")
                }
            }
        }

        // Presence: direct-RF liveness only, BEFORE the dedup gate. A frame counts only
        // at its ORIGINATION TTL: relays decrement (regional/private) or clobber to 0
        // (local), so ttl == origin TTL ⇔ straight from the originator. Relayed copies
        // must not register the originator as "nearby" — including the relayed echo of
        // our OWN frame, which would otherwise count us as our own neighbor.
        // Deliberately no RSSI floor: any frame that decoded + verified is a real
        // transmission. The −80 dBm config floor is a sketch/trust window, NOT a liveness
        // window — applying it here made the count flicker at the boundary while messages
        // kept flowing.
        val localTtl = defaultTtlLocal().toInt()
        val originTtl = defaultTtlRegional().toInt()
        val direct = when (wp?.msgType?.toInt()) {
            1 -> frameTtl(bytes)?.toInt() == localTtl
            2, 3 -> frameTtl(bytes)?.toInt() == originTtl
            else -> false
        }
        if (direct) MeshState.measurement.recordPresence(mark, frameEp)

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
            if (!vdlCheckFrame(bytes)) return // invalid PoW: drop, do not relay
            var privatePlaintext: String? = null
            var privateLabel: String? = null
            for (contact in PairStore.contacts(this)) {
                // A3: v2 contacts try the epoch-ratcheted key for the frame's epoch
                // (fast-forwarding when the sender is ahead); v1 uses the static key.
                for (key in PairStore.candidateKeys(this, contact, frameEp)) {
                    val pt = openPrivateBodyOnly(bytes, key)
                    if (pt != null && privatePlaintext == null) {
                        privatePlaintext = pt
                        privateLabel = contact.label
                    }
                }
            }
            if (privatePlaintext != null) {
                // A3: persist any fast-forwarded chain state (past keys deleted).
                PairStore.noteOpened(this, privateLabel!!, frameEp)
                MeshState.appendMessage(
                    MsgRow(
                        tsMs = System.currentTimeMillis(),
                        epoch = frameEp,
                        markHexPrefix = "🔒 $privateLabel",
                        rssi = rssi,
                        text = privatePlaintext,
                        mine = false,
                        tier = SendTier.PRIVATE,
                        direct = direct
                    )
                )
            }
            // Relay regardless of whether we could decrypt (multi-hop delivery).
            relayFrame(bytes)?.let { enqueueRelay(it, 3) }
            return
        }

        // Public path (msgType 1/2).
        //
        // A1: a frame WITHOUT a witness is relay-only — NEVER displayed. Before this fix the
        // witness check was skipped entirely when both fields were zero, so a remote van could
        // inject a fake "TEAR GAS" that displayed as DIRECT · VERIFIED on every phone.
        //
        // Display rules:
        //   LOCAL     — witness must be PoCP-Valid against our cell (current or previous
        //               epoch sketch bucket). CellMismatch/Stale: dropped entirely.
        //   BROADCAST — witness MAC must be valid. Jaccard outcome only feeds the badge:
        //               co-present origin vs remote-cell claim. A2: corroboration counts
        //               ONLY claims heard DIRECTLY (origination TTL) and is shown as a HINT,
        //               never as a boolean unlock (a single nearby attacker can forge two
        //               dissimilar claims — the old distinct≥2 display lock was security theater).
        var displayOk = false
        var relayOnly = false
        var corroborations = 0u
        if (wp != null) {
            val msgType = wp.msgType.toInt()
            val hasWitness = wp.pocpWit.any { it != 0.toByte() } ||
                wp.divSketch.any { it != 0.toByte() }
            if (!hasWitness) {
                relayOnly = true // A1: relay-only, never display
            } else {
                when (val verdict = verifyPocpAcrossRollover(wp.epoch, wp.divSketch, wp.framePrefix, wp.pocpWit, cfg)) {
                    0 -> displayOk = true // Valid: co-present with our cell
                    1 -> if (msgType == 2) displayOk = true // honest remote-cell broadcast
                    // else: Stale MAC, or CellMismatch on LOCAL — no display, no relay
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

                // A2: corroboration hint for broadcast — direct-heard claims only.
                if (displayOk && msgType == 2) {
                    corroborations = if (direct) {
                        trust.recordVerification(wp.bodyHash, wp.divSketch, cfg.tauThreshold)
                    } else {
                        trust.distinctCount(wp.bodyHash)
                    }
                }
            }
        }

        // Relay if the frame is either displayable or relay-only.
        if (displayOk || relayOnly) {
            relayFrame(bytes)?.let { enqueueRelay(it, wp?.msgType?.toInt() ?: 2) }
        }

        // Display only when verified and not relay-only.
        if (displayOk) {
            val text = frameBodyText(bytes)
            if (!text.isNullOrEmpty()) {
                // B6: suppression keyed by (text, sender-mark prefix) — an attacker echoing
                // the same words cannot suppress the real alert from a different sender.
                val markHex = mark.joinToString("") { "%02x".format(it) }
                val suppressKey = "$text|${markHex.take(8)}"
                var suppress = false
                synchronized(recentTexts) {
                    val prevEpoch = recentTexts[suppressKey]
                    suppress = prevEpoch != null &&
                        ownEpoch >= prevEpoch &&
                        ownEpoch - prevEpoch <= 3u
                    recentTexts[suppressKey] = ownEpoch
                    if (recentTexts.size > 64) {
                        val iter = recentTexts.iterator()
                        while (iter.hasNext()) {
                            val e = iter.next()
                            if (ownEpoch >= e.value && ownEpoch - e.value > 6u) iter.remove()
                        }
                    }
                }
                if (!suppress) {
                    val tier = if (wp?.msgType?.toInt() == 1) SendTier.LOCAL else SendTier.BROADCAST
                    MeshState.appendMessage(
                        MsgRow(
                            tsMs = System.currentTimeMillis(),
                            epoch = frameEp,
                            markHexPrefix = markHex.take(8),
                            rssi = rssi,
                            text = text,
                            mine = false,
                            tier = tier,
                            direct = direct,
                            corroborations = corroborations.toInt()
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
        // TTL from the Rust core (invariant #1): local = 1 (relayable once so the
        // reflection receipt can come back), regional/private = 8.
        val ttl: UByte = if (localImmediate) defaultTtlLocal().toUByte() else defaultTtlRegional().toUByte()
        // A1/C2: sign the current epoch's sketch; at rollover — when nothing has been heard
        // yet this epoch — sign the PREVIOUS epoch's completed sketch with witness seed
        // epoch-1 (receivers accept both). Only when neither epoch heard a single mark do
        // we originate witnessless (relay-only at receivers — a device that hears nobody
        // has nobody to display to, so nothing is lost).
        val sketchCur = MeshState.measurement.localSketch(epoch, seed, cfg.rssiFloorDbm)
        val sketchPrev = if (sketchCur.isEmpty()) {
            MeshState.measurement.localSketch(epoch - 1u, seed, cfg.rssiFloorDbm)
        } else {
            emptyList()
        }
        val (sketch, witEpoch) = when {
            sketchCur.isNotEmpty() -> sketchCur to epoch
            sketchPrev.isNotEmpty() -> sketchPrev to (epoch - 1u)
            else -> sketchCur to epoch
        }
        val divSketch = if (sketch.isNotEmpty()) pocpSketchToDivSketch(sketch) else null
        val frame = if (divSketch != null) {
            makeMessageFrameWithWitness(seed, epoch, beaconSeed, localImmediate, effectiveText, ttl, divSketch, witEpoch)
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
            // C7: zero the live beacon seed too — previously it stayed in Rust memory,
            // recoverable until process exit.
            if (::beacon.isInitialized) beacon.wipe()

            // Clear Android persisted state.
            PairStore.wipe(this)
            ConfigStore.clear(this)
            // Legacy plaintext counter file (older installs) and crash log.
            getSharedPreferences(PAIR_PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
            getSharedPreferences("crash_log", Context.MODE_PRIVATE).edit().clear().commit()

            // Clear in-memory measurement data.
            MeshState.measurement.clear()
            MeshState.messages.value = emptyList()
            MeshState.debugLog.value = listOf("!!! PANIC WIPE at ${System.currentTimeMillis()}")
            MeshState.outgoingText.value = ""
            // C7: force the UI to drop remembered Contact objects (they hold pair keys until
            // GC — a documented JVM limit; recomposition to an empty list is the best we can do).
            MeshState.contactsVersion.value += 1

            // Zeroize in-memory secrets. seed is lateinit — a cold-start ACTION_PANIC
            // (service never fully started) reaches here before seed is assigned.
            if (::seed.isInitialized) seed.fill(0)
            currentPublicFrame?.fill(0)
            currentPublicFrame = null
            synchronized(relayQueueLock) { relayQueue.clear() }
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

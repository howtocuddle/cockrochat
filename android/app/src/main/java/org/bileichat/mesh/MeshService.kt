package org.bileichat.mesh

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
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
import uniffi.mesh_core.FfiDedupVerdict
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
        const val ACTION_PANIC = "org.bileichat.mesh.ACTION_PANIC"

        // Legacy plaintext counter prefs file (B4/C9: counter moved into PairStore's
        // encrypted store with a per-epoch random base). performPanicWipe still deletes
        // this file explicitly to erase traces left by older installs.
        const val PAIR_PREFS_NAME = "mesh_pairing"

        /** B2: hard cap on LOCAL re-broadcast lifetime. An unheard local alert must not
         *  scream every epoch forever (battery + stale-danger re-airing hours later). */
        const val LOCAL_REBROADCAST_WINDOW_MS = 30 * 60_000L

        /**
         * B1/B2: post-echo re-airing schedule for LOCAL, as
         * `(elapsed since the first echo) to (target interval between airings)`, both ms.
         * The first matching tier wins; the last entry is the floor and must be unbounded.
         *
         * The echo changes only HOW OFTEN we re-air. It must never shorten the message's
         * LIFETIME, which stays [LOCAL_REBROADCAST_WINDOW_MS] and is ended only by the clock
         * or by the user (STOP). That distinction is the whole security property here:
         *
         *  - An echo is a byte-identical replay of our own frame. TTL sits outside the hashed
         *    region and [claimOwnEcho] matches on the frame hash, so ANY device that overheard
         *    us can mint one at zero cost — this is replay, not forgery. It is therefore an
         *    attacker-controlled signal and nothing safety-critical may key off it.
         *  - LOCAL is TTL 1. Re-origination is not redundancy, it is the ONLY propagation
         *    mechanism the tier has. Letting an echo end it means one adjacent device that
         *    re-airs our frame once and relays nothing onward deletes the message mesh-wide.
         *    (BROADCAST is TTL 8 and propagates without us, which is why it may stop on echo.)
         *  - The long window is not only for "nobody heard me". It is for the phone that walks
         *    into range at minute 25 — the case the UI banner names in so many words.
         *
         * Backing off the interval instead keeps all of that: an attacker's replay buys a
         * quieter phone, never a silent one, while airtime over the 30 minutes drops from ~45
         * airings to ~11 at the default 10 s epoch.
         *
         * Expressed in ms, not epoch counts, because [MeshConfig.epochMs] ranges over
         * 5 s..120 s (Config.EPOCH_RANGE); an epoch-count constant means a wildly different
         * wall-clock schedule at each end of that range.
         */
        val LOCAL_ECHO_BACKOFF: List<Pair<Long, Long>> = listOf(
            2 * 60_000L to 40_000L,
            10 * 60_000L to 2 * 60_000L,
            Long.MAX_VALUE to 5 * 60_000L
        )

        /** B8: relay queue bound; lowest-priority tasks are evicted when full. */
        const val RELAY_QUEUE_CAP = 64

        /**
         * Extra airings of an already-sealed private frame, one per epoch rollover.
         *
         * Bounded by the receiver's ±2-epoch freshness gate: a frame sealed at epoch E stops
         * being accepted after E+2, so two re-airings is the most that can still be opened.
         */
        const val PRIVATE_REAIRINGS = 2

        /** Re-stamp + re-solve attempts when a VDL solve overruns the epoch it was stamped in. */
        const val PRIVATE_SEAL_ATTEMPTS = 3

        /**
         * Max delay after an epoch boundary before re-originating, drawn fresh each epoch.
         *
         * Kept well under one epoch so the frame is still stamped with the epoch it was built
         * for. Re-drawn rather than a fixed per-device offset: a fixed offset would let two
         * devices that happen to land close together collide with each other forever.
         */
        const val TX_JITTER_MAX_MS = 1_200L

        /** Max delay before airing a queued relay, so two relayers don't answer in unison. */
        const val RELAY_JITTER_MAX_MS = 350L

        /** Airings of one relayed frame when we are the only possible relayer. */
        const val RELAY_REPEATS_MAX = 3

        /**
         * Gap between repeat airings of the same relayed frame.
         *
         * Wide enough that a repeat lands in genuinely different radio conditions rather than
         * inside the same interference burst, and 3 airings still fit inside one 10 s epoch.
         */
        const val RELAY_REPEAT_SPACING_MS = 2_600L

        /**
         * K4 freshness half-window for LOCAL/BROADCAST, in epochs.
         *
         * 4 (≈40 s at a 10 s epoch) covers an 8-hop chain at the 2–4 s per hop the relay path
         * actually costs. At 2 the wall sat inside the mesh's own advertised TTL, so the outer
         * hops were dropped as skewed while behaving correctly. Safe only because dedup now
         * decays on the local clock across a wider window (DEDUP_RETENTION_EPOCHS = 6).
         */
        /** Clean epochs required before the skew banner is retired (C10). */
        const val SKEW_CLEAR_EPOCHS = 3

        /** S6: bounded reject cache. Small — it only has to absorb a repeating flood. */
        const val REJECT_CACHE_CAP = 512

        const val PUBLIC_FRESHNESS_EPOCHS = 4L

        /**
         * K4 freshness half-window for PRIVATE, in epochs. Deliberately narrower.
         *
         * Bounded by the key schedule, not by transport: contacts retain exactly one previous
         * epoch key, [PairStore.fastForwardChains] pins the chain to epoch-1 against this
         * window, and [PRIVATE_REAIRINGS] is sized to it. A private frame admitted at 3–4
         * epochs old would verify, relay, and then decrypt under no key at all.
         */
        const val PRIVATE_FRESHNESS_EPOCHS = 2L

        /** verifyPocpAcrossRollover: no local sketch exists for any candidate bucket, so the
         *  frame could not be judged at all. Distinct from Stale (a real MAC failure) because
         *  it is transient — the same frame may verify moments later. */
        const val POCP_NO_LOCAL_SKETCH = 3

        /**
         * Core verdict: the witness verifies and the cells touch on exactly ONE element.
         *
         * Displayable but NOT co-presence evidence — it is simultaneously what a single-byte
         * forgery looks like and what an honest phone that has just started scanning
         * legitimately claims, and nothing at this layer can separate them. Shown with the
         * low-confidence badge rather than either trusted or dropped.
         *
         * 4, not 3: [POCP_NO_LOCAL_SKETCH] already owns 3 as a locally synthesised sentinel,
         * and the `else` arm below treats anything unrecognised as Stale — so numbering this 3
         * would have silently dropped every one of these frames.
         */
        const val POCP_UNATTESTED = 4

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

    /** A sealed private frame awaiting its remaining re-airings (see [PRIVATE_REAIRINGS]). */
    private class PendingPrivate(
        val frame: ByteArray,
        val sealedEpoch: UInt,
        var airingsLeft: Int
    )
    private val pendingPrivateLock = Any()
    private var pendingPrivate: PendingPrivate? = null

    /**
     * P4: hashes of our RECENTLY-originated public frames — used to hear our own reflection
     * coming back through the mesh (send-and-listen), keyed by hex hash.
     *
     * This was a single slot, overwritten by every rebuildAndAdvertise (epoch rollover, tier
     * switch, outgoing-text change, advertising self-heal). Relays are queued behind a 250 ms
     * poll and aired for 2 s, so an echo that crossed an epoch boundary was compared against
     * a DIFFERENT hash and the receipt never fired — the sender kept re-transmitting until
     * "no echo heard", even though the receiver had displayed the message. Keeping a few
     * epochs of hashes closes that window.
     *
     * Entries older than [ownHashRetentionEpochs] are evicted.
     * Guarded by [ownHashesLock]: written on the service coroutine, read on BLE binder threads.
     */
    /** [private] frames take a separate receipt path: they never set outgoingText and their
     *  re-airings are counted by reairPendingPrivate, so the public conditions do not apply. */
    private data class OwnFrame(
        val epoch: UInt,
        val carriedText: Boolean,
        val private: Boolean = false
    )
    private val ownHashesLock = Any()
    private val ownHashes = LinkedHashMap<String, OwnFrame>()

    /** Hashes we have already reacted to, so the receipt still fires once per origination. */
    private val ownHashesAcked = HashSet<String>()

    /** Set when a relayed echo of our frame is heard (receipt). B1: an echo proves only
     *  that ONE (possibly adversarial) peer relayed us once — LOCAL no longer hard-stops
     *  on it, it only backs off the re-airing interval ([LOCAL_ECHO_BACKOFF]) and still runs
     *  the full [LOCAL_REBROADCAST_WINDOW_MS].
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
    /**
     * Sized against the widened retention window, not the old one: the per-epoch sub-cap is
     * 1024 and entries now live ~6 epochs, so the worst legitimate occupancy is 6144. At 4096
     * a busy mesh would evict live entries by capacity before time-decay ever ran, which is
     * the eviction-window weakness the sub-cap exists to prevent. ~400 KB.
     */
    private val dedup = FfiDedup(8192u)

    // H2: per-service-start trust accumulator for multi-locale diversity
    private val trust = FfiTrust()

    // Rate-limit epoch-skew log: only log when the (frameEpoch, ownEpoch) pair changes.
    private var lastSkewPair: Pair<UInt, UInt>? = null

    // Track whether the first frame of the current epoch has been logged.
    private var firstFrameEpoch: UInt? = null

    // Rate-limit the dedup bucket-full warning to once per epoch.
    @Volatile
    private var lastBucketFullEpoch: UInt? = null

    /** Consecutive rollovers with no skewed frame, for retiring the skew banner (C10). */
    private var cleanSkewEpochs = 0

    /**
     * S6: hashes of frames already judged unverifiable, so a replay costs a lookup instead of
     * a fresh Ed25519 verify.
     *
     * Keying this on the frame hash is only safe because the signature (wire bytes 150..214)
     * sits INSIDE the hashed region — `frame_hash` is blake3 over `buf[0..214]`. Tampering
     * with a signature therefore changes the hash, so an attacker cannot blacklist a
     * legitimate frame by replaying a corrupted copy of it. If the hashed region ever stops
     * covering the signature, this cache becomes a censorship primitive and must be removed.
     */
    private val rejectedLock = Any()
    private val rejectedHashes = LinkedHashSet<String>()

    private fun isRejected(hashHex: String): Boolean =
        synchronized(rejectedLock) { rejectedHashes.contains(hashHex) }

    private fun rememberRejected(hashHex: String) {
        synchronized(rejectedLock) {
            rejectedHashes.remove(hashHex)
            rejectedHashes.add(hashHex)
            while (rejectedHashes.size > REJECT_CACHE_CAP) {
                val it = rejectedHashes.iterator()
                it.next()
                it.remove()
            }
        }
    }

    /**
     * Per-epoch receive counters, reported at every rollover.
     *
     * Every early gate in ingestFrameInner is a bare `return`: a frame that fails to decode,
     * fails signature verification, or is a duplicate leaves NO trace. That made "our radio
     * is deaf" and "we are hearing plenty and rejecting all of it" produce byte-identical
     * logs — a phone sitting at neighbors=0 could not be told apart from a phone whose
     * scanner never started. One counted line per epoch distinguishes them.
     */
    private val rxTotal = java.util.concurrent.atomic.AtomicInteger(0)
    private val rxUndecodable = java.util.concurrent.atomic.AtomicInteger(0)
    private val rxBadSig = java.util.concurrent.atomic.AtomicInteger(0)
    private val rxSkewed = java.util.concurrent.atomic.AtomicInteger(0)
    private val rxDuplicate = java.util.concurrent.atomic.AtomicInteger(0)
    private val rxShown = java.util.concurrent.atomic.AtomicInteger(0)

    /** Frames dropped by the S6 reject cache — a replayed forgery or a stale re-air. A high
     *  number next to a low bad-sig count means someone is repeating the same bad frame. */
    private val rxReplayedReject = java.util.concurrent.atomic.AtomicInteger(0)

    // B8: prioritized relay queue. Priority: LOCAL echo (0) > regional (1) > private (2);
    // FIFO within a class. Drained by a service coroutine whenever the radio has a free
    // hardware slot — frames WAIT instead of being silently dropped (B8 starvation fix).
    private data class RelayTask(
        val frame: ByteArray,
        val priority: Int,
        val seq: Long,
        /** Airings still owed for this frame (see [relayRepeatsForDensity]). */
        val repeats: Int = 1
    )
    private val relayQueueLock = Any()
    private val relayQueue = ArrayDeque<RelayTask>()
    private var relaySeq = 0L


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

    private lateinit var wakeLock: PowerManager.WakeLock

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Prepare the wake lock but do NOT acquire it yet — if the start aborts before
        // startForeground succeeds, an acquired lock would outlive the failed service.
        // Acquired in onStartCommand once the service is genuinely in the foreground.
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "bileichat:mesh")
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

        // P1: startForeground throws SecurityException on API 34+ when the connectedDevice
        // FGS type is claimed without a Bluetooth permission. Uncaught, that killed the
        // process the moment the service started — indistinguishable from "the app quit".
        try {
            startForeground(NOTIFICATION_ID, buildNotification("Starting…"))
        } catch (e: Exception) {
            MeshState.logDebug("startForeground failed: ${e.message}")
            MeshState.running.value = false
            stopSelf()
            return START_NOT_STICKY
        }
        started = true

        // Now in the foreground: keep the CPU awake so Doze cannot suspend BLE scans and
        // advertisements. Failure here must not abort the mesh.
        try {
            if (!wakeLock.isHeld) wakeLock.acquire()
        } catch (e: Exception) {
            MeshState.logDebug("wake lock acquire failed: ${e.message}")
        }

        MeshState.running.value = true
        MeshState.logDebug("service started")

        seed = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val beaconSeed0 = ByteArray(32).also { SecureRandom().nextBytes(it) }
        beacon = BeaconFfi(beaconSeed0)
        radio = BleRadio(this)

        // Wire debug callback before any radio operations
        radio.onDebug = { MeshState.logDebug(it) }
        // One tick, fired when the STACK confirms the frame is on air — not when we asked it
        // to be. startAdvertising is async and can be refused, so ticking at the call site
        // showed "sent" for frames the controller never accepted. markOutgoing is monotonic,
        // so the empty presence frames LOCAL airs between sparse re-broadcasts cannot pull a
        // row backwards, and the public branch is gated on there being text in flight at all.
        radio.onFrameOnAir = { isPrivate ->
            if (isPrivate) {
                MeshState.privateRowTs?.let { MeshState.markOutgoing(it, SendState.ON_AIR) }
            } else if (MeshState.outgoingText.value.isNotEmpty()) {
                MeshState.outgoingRowTs?.let { MeshState.markOutgoing(it, SendState.ON_AIR) }
            }
        }

        // Log radio capability once at start
        MeshState.logDebug(radio.capabilityReport())
        if (!radio.extendedAdvCapable()) {
            // P6: this phone cannot put a 226-byte frame in an advertisement. It stays a full
            // mesh member, but every frame it sends OR relays travels over the GATT plane,
            // which needs a connection (bounded by GattPlane.MAX_PEERS) rather than a
            // broadcast. Say so plainly — this was previously invisible.
            MeshState.logDebug(
                "NOTE: no extended advertising on this adapter — frames and relays travel " +
                    "over the GATT plane only (connection-based, fewer simultaneous peers)"
            )
        }

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
                    MeshState.outgoingEchoed.value = false
                    echoEpoch = null
                } else {
                    MeshState.outgoingSetAtEpoch = null
                }
                // Drives the "still sending" indicator. LOCAL keeps re-originating for up to
                // 30 minutes until it is heard back (B2), which is right for a danger alert
                // and alarming for "hi" — so the user has to be able to see it and stop it.
                MeshState.outgoingAiring.value = text.isNotEmpty()
                rebuildAndAdvertise(epoch, cfg2, text)
            }
        }

        // Self-test runner. Waits for the next epoch boundary before starting so that two
        // phones triggered within a few seconds of each other produce reports stamped with the
        // SAME epoch — marks rotate per epoch, so that is the only way the two mark lists are
        // comparable. Runs on Default: the private-frame check solves a real VDL witness.
        lifecycleScope.launch {
            MeshState.selfTestRequests.collect { n ->
                if (n == 0 || MeshState.selfTestRunning.value) return@collect
                MeshState.selfTestRunning.value = true
                try {
                    val cfg2 = MeshState.config
                    MeshState.selfTestLog.value = emptyList()
                    val waitMs = cfg2.epochMs - (System.currentTimeMillis() % cfg2.epochMs)
                    MeshState.logSelfTest(
                        "Armed. Waiting ${waitMs}ms for the next epoch boundary so both " +
                            "phones' reports line up…"
                    )
                    delay(waitMs + 150)
                    withContext(Dispatchers.Default) {
                        SelfTest.run(
                            ctx = this@MeshService,
                            cfg = MeshState.config,
                            seed = seed,
                            beaconSeed = beacon.seed(),
                            radio = radio,
                            gattPlane = gattPlane
                        ) { line -> MeshState.logSelfTest(line) }
                    }
                } catch (e: Exception) {
                    MeshState.logSelfTest("SELF-TEST ABORTED: ${e.message}")
                } finally {
                    MeshState.selfTestRunning.value = false
                }
            }
        }

        // Tier switch (Local/Broadcast) takes effect immediately, not at the next epoch:
        // rebuild the current frame with the new TTL as soon as the tier changes.
        lifecycleScope.launch {
            MeshState.outgoingTier.collect {
                val cfg2 = MeshState.config
                val epoch = (System.currentTimeMillis() / cfg2.epochMs).toUInt()
                // Only the empty (presence) frame follows a tier change. outgoingText holds
                // the text of a message ALREADY SENT — it stays set for messageRepeatEpochs
                // so the re-airings can run. Rebuilding it here re-originated that message at
                // the new tier: send on BROADCAST, tap LOCAL to read within ~30 s, and your
                // broadcast went back out as a LOCAL message. The composer keeps its own draft
                // state and clears on send, so nothing being typed is lost by skipping this.
                if (MeshState.outgoingText.value.isNotEmpty()) return@collect
                rebuildAndAdvertise(epoch, cfg2, "")
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
                // The frame's epoch is stamped BEFORE the VDL solve, and the solve is a
                // randomized search (~2^22 hashes, exponentially distributed — the mean is a
                // few seconds on a phone but the tail is long). On a slow phone or an unlucky
                // search the finished frame is already several epochs old when it reaches the
                // air, and the receiver's ±2 K4 gate drops it before it ever reaches the
                // private branch. Silent on both ends, and it hits ONLY private, because
                // private is the only tier that pays for a VDL witness.
                //
                // So: measure the solve, re-check the clock, and re-stamp rather than putting
                // a frame on air that is already guaranteed to be rejected.
                var frame: ByteArray? = null
                var sealFailure: String? = null
                for (attempt in 1..PRIVATE_SEAL_ATTEMPTS) {
                    val sealEpoch = (System.currentTimeMillis() / cfg2.epochMs).toUInt()
                    val pairKey = PairStore.keyForSend(this@MeshService, ps.label, sealEpoch)
                    if (pairKey == null) {
                        // No label in the log: it is exportable and would name your contacts.
                        sealFailure = "private send dropped: contact unknown or key ratchet failed"
                        break
                    }
                    val counter = PairStore.nextPrivateCounter(this@MeshService, sealEpoch)
                    MeshState.logDebug("sealing private message (VDL solve, ~seconds of CPU)…")
                    val startedAt = System.currentTimeMillis()
                    val candidate = withContext(Dispatchers.Default) {
                        val beaconSeed = beacon.seed()
                        makePrivateFrame(seed, sealEpoch, beaconSeed, pairKey, ps.text, counter.toULong())
                    }
                    val solveMs = System.currentTimeMillis() - startedAt
                    val drift = (System.currentTimeMillis() / cfg2.epochMs).toLong() - sealEpoch.toLong()
                    if (candidate == null) {
                        sealFailure = "private seal failed (text > 47 bytes or bad key)"
                        break
                    }
                    // Budget one epoch normally: the frame still has to reach the air, cross a
                    // relay hop, and leave the re-airings room inside the gate. On the last
                    // attempt take anything the gate would still accept rather than nothing.
                    val budget = if (attempt == PRIVATE_SEAL_ATTEMPTS) 2L else 1L
                    if (drift <= budget) {
                        MeshState.logDebug("VDL solve ${solveMs}ms, epoch drift $drift")
                        frame = candidate
                        break
                    }
                    MeshState.logDebug(
                        "VDL solve ${solveMs}ms — frame stamped $drift epochs stale, outside the " +
                            "receiver's ±2 gate; re-sealing (attempt $attempt/$PRIVATE_SEAL_ATTEMPTS)"
                    )
                    sealFailure = "private send dropped: this phone cannot solve the VDL witness " +
                        "within 2 epochs (last solve ${solveMs}ms vs ${cfg2.epochMs}ms epoch)"
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
                            restoreFrame = { currentPublicFrame },
                            codedPhy = cfg2.codedPhy,
                            advIntervalMs = cfg2.advIntervalMs
                        ) {
                            privateTransportActive = false
                            gattPlane.currentFrame = currentPublicFrame ?: restoreFrame
                            MeshState.logDebug("private advertising window ended; public frame restored")
                        }
                        if (started) {
                            gattPlane.currentFrame = frame
                            // The one tick is fired by radio.onFrameOnAir when the stack
                            // confirms the set started — `started` here only means the request
                            // was accepted for dispatch. The second tick arrives if a peer
                            // relays it back (registered with rememberOwnFrame just below).
                        } else {
                            privateTransportActive = false
                            MeshState.logDebug("private send skipped: another private window is active")
                        }
                    }
                    // Insert our own frame hash so the relayed echo doesn't come back as incoming.
                    val ownEpoch = (System.currentTimeMillis() / cfg2.epochMs).toUInt()
                    frameHash(frame)?.let {
                        // C3: register it as ours so a relayed echo is RECOGNISED rather than
                        // silently swallowed by dedup. Private was the one tier that never did
                        // this, so it could never earn a second tick — the tier where a sender
                        // most wants to know something moved. Re-airings reuse these exact
                        // bytes, so they carry the same hash and claimOwnEcho stays one-shot.
                        rememberOwnFrame(it, ownEpoch, cfg2, carriedText = true, private = true)
                        dedup.checkAndInsertEpoch(it, ownEpoch, ownEpoch)
                    }
                    // Queue re-airings. A private message got ONE 6 s window and was never
                    // repeated, while LOCAL/BROADCAST re-originate every epoch for
                    // messageRepeatEpochs — so a receiver that missed that single window (a
                    // scan gap, a relay burst, a busy epoch) lost the message permanently and
                    // silently, and the sender had no receipt to notice. The sealed bytes are
                    // reused verbatim: no second VDL solve, and the frame's own epoch stays
                    // valid inside the receiver's ±2-epoch freshness gate.
                    synchronized(pendingPrivateLock) {
                        pendingPrivate = PendingPrivate(frame, ownEpoch, airingsLeft = PRIVATE_REAIRINGS)
                    }
                    MeshState.logDebug(
                        "private message sealed + advertised (${windowMs}ms window, " +
                            "$PRIVATE_REAIRINGS re-airings queued)"
                    )
                } else {
                    MeshState.logDebug(sealFailure ?: "private seal failed")
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
                    // Same lockstep problem as origination: both phones hear the other's new
                    // frame at the same instant and would answer with a relay burst at the
                    // same instant. Decorrelate them.
                    delay(kotlin.random.Random.nextLong(RELAY_JITTER_MAX_MS))
                    // P6: on adapters without extended advertising the over-the-air relay is
                    // impossible; the GATT plane is then the ONLY multi-hop path, so it runs
                    // unconditionally. Pace the loop so a legacy phone doesn't spin the queue.
                    val onAir = radio.advertiseRelayOnce(task.frame, 2000L, MeshState.config.codedPhy)
                    gattPlane.relayOnce(task.frame)
                    // Owe another airing? Re-queue it later in the epoch rather than looping
                    // here, so higher-priority relays (a LOCAL danger echo) still overtake it.
                    // The frame is unchanged, so it keeps its original epoch and stays inside
                    // the receiver's ±2 freshness gate; peers that already saw it drop the
                    // copy at the cheap dedup gate, so this costs airtime and nothing else.
                    if (task.repeats > 1) {
                        launch {
                            delay(RELAY_REPEAT_SPACING_MS + kotlin.random.Random.nextLong(RELAY_JITTER_MAX_MS))
                            synchronized(relayQueueLock) {
                                if (relayQueue.size < RELAY_QUEUE_CAP) {
                                    relayQueue.addLast(
                                        task.copy(seq = relaySeq++, repeats = task.repeats - 1)
                                    )
                                }
                            }
                        }
                    }
                    if (!onAir) delay(250L)
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
                            val echoedAt = echoEpoch
                            if (setAt != null && epoch >= setAt && epoch - setAt >= maxAge) {
                                MeshState.outgoingText.value = ""
                                MeshState.receipt.value =
                                    "local broadcast stopped after 30 min — re-send if still relevant"
                                MeshState.logDebug("local message expired (30 min re-broadcast cap)")
                            } else if (reflectionHeard && echoedAt == null) {
                                // B1: the echo backs off the re-airing INTERVAL (see
                                // LOCAL_ECHO_BACKOFF) and nothing else. It must not shorten the
                                // lifetime — an echo is a zero-cost replay of our own frame, and
                                // LOCAL is TTL 1, so any lifetime rule keyed on it hands an
                                // adjacent device a one-frame mesh-wide delete. Only the 30-min
                                // cap above and the user's STOP end a LOCAL message.
                                echoEpoch = epoch
                                MeshState.logDebug("local echo heard — backing off re-airing interval")
                            }
                        } else {
                            val setAt = MeshState.outgoingSetAtEpoch
                            if (setAt != null && epoch >= setAt && epoch - setAt >= cfg.messageRepeatEpochs.toUInt()) {
                                MeshState.outgoingText.value = ""
                                if (MeshState.receipt.value == null) {
                                    // State the measurement, not an outcome. All we know is
                                    // that no peer's relay of our frame came back — which is
                                    // ordinary when there is only one peer to do the relaying,
                                    // and was field-observed reading as "it never arrived"
                                    // while the other phone had the message on screen.
                                    val alone = MeshState.stats.value.neighborsThisEpoch <= 1
                                    MeshState.receipt.value = if (alone) {
                                        "sent — no relay echo came back, which is normal with " +
                                            "only one phone nearby. It does not mean the message was missed."
                                    } else {
                                        "sent — no relay echo came back after " +
                                            "${cfg.messageRepeatEpochs} epochs. Delivery is unconfirmed, not ruled out."
                                    }
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
                    // Transmit jitter. Every phone derives its epoch from the wall clock, so
                    // two phones in sync cross the boundary within milliseconds of each other
                    // — and then both tear down their advertising set, re-originate, and relay
                    // whatever they just heard, all inside the same slice of the epoch. In
                    // lockstep they talk over each other there every single time.
                    //
                    // A missed relay is expensive: dedup lets a peer relay a given frame only
                    // ONCE per epoch (every later copy that epoch is a DUPLICATE), so one
                    // collision costs a full epoch of waiting for the echo — which is exactly
                    // the "receipt eventually arrived, but late" symptom.
                    launch {
                        delay(kotlin.random.Random.nextLong(TX_JITTER_MAX_MS))
                        // Read the outgoing text AFTER the jitter, never before it.
                        //
                        // outgoingText is cleared from other threads while this delay runs —
                        // by the STOP button (MainActivity.stopSending, UI thread) and by a
                        // BROADCAST echo (ingestFrame, BLE binder thread). A value captured
                        // before the delay puts the just-cleared text straight back on air for
                        // the remainder of the epoch, with the UI already showing it gone. The
                        // window is the full TX_JITTER_MAX_MS at the start of every epoch, and
                        // echoes of the PREVIOUS epoch's frame land squarely in it.
                        rebuildAndAdvertise(epoch, cfg, textToAirThisEpoch(epoch, cfg))
                    }

                    // Step v2 pair chains with the clock so a long idle period can never
                    // exceed the ratchet span cap and brick private messaging (see
                    // PairStore.fastForwardChains). One BLAKE3 per contact per epoch.
                    // On IO: this touches EncryptedSharedPreferences.
                    launch(Dispatchers.IO) {
                        try {
                            PairStore.fastForwardChains(this@MeshService, epoch)
                        } catch (e: Exception) {
                            MeshState.logDebug("chain fast-forward failed: ${e.message}")
                        }
                    }

                    // Log epoch rollover with neighbor/total counts
                    val neighbors = MeshState.measurement.neighborsDirect(epoch)
                    val total = MeshState.measurement.totalHeard()
                    MeshState.logDebug(
                        "epoch rollover: epoch=$epoch neighbors=$neighbors total=$total"
                    )

                    reairPendingPrivate(epoch, cfg)

                    // Receive accounting for the epoch just ended. "rx=0" means the radio
                    // delivered nothing at all — a scan/permission/hardware problem, NOT a
                    // protocol rejection. Any non-zero rx with nothing shown means frames are
                    // arriving and being rejected, and the breakdown says at which gate.
                    val rx = rxTotal.getAndSet(0)
                    val bad = rxBadSig.getAndSet(0)
                    val undec = rxUndecodable.getAndSet(0)
                    val skew = rxSkewed.getAndSet(0)
                    val dup = rxDuplicate.getAndSet(0)
                    val shown = rxShown.getAndSet(0)
                    val recached = rxReplayedReject.getAndSet(0)
                    MeshState.logDebug(
                        "rx: $rx frames (undecodable=$undec bad-sig=$bad skewed=$skew " +
                            "replayed-reject=$recached dup=$dup shown=$shown)" +
                            if (rx == 0) " — RADIO DELIVERED NOTHING: check scanning/permissions" else ""
                    )

                    // C10: retire the skew banner only after several CLEAN epochs. A skewed
                    // peer re-airs every epoch, so a single quiet epoch is not evidence it has
                    // gone away — but a run of them is.
                    if (skew > 0) {
                        cleanSkewEpochs = 0
                    } else if (MeshState.clockSkewWarning.value != null) {
                        cleanSkewEpochs += 1
                        if (cleanSkewEpochs >= SKEW_CLEAR_EPOCHS) {
                            MeshState.clockSkewWarning.value = null
                            lastSkewPair = null
                            cleanSkewEpochs = 0
                        }
                    }
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

                // Update notification.
                //
                // Deliberately says nothing about what the radio can hear. This line renders
                // on the LOCK SCREEN, so a live neighbour count told anyone holding the phone
                // — or just looking at it on a table, or a police officer who has picked it
                // up — how many mesh users are within radio range, and watching it move says
                // whether a group is gathering or dispersing. The app's own UI is behind the
                // lock screen and is the right place for that.
                val notifText = "Running"
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
        // P1: these are lateinit and are only assigned partway through onStartCommand. If the
        // start aborted before that (permission failure, radio init throw), touching them here
        // threw UninitializedPropertyAccessException and MASKED the original crash.
        if (::radio.isInitialized) {
            try { radio.stop() } catch (e: Exception) { MeshState.logDebug("radio.stop failed: ${e.message}") }
        }
        if (::gattPlane.isInitialized) {
            try { gattPlane.stop() } catch (e: Exception) { MeshState.logDebug("gattPlane.stop failed: ${e.message}") }
        }
        if (::wakeLock.isInitialized && wakeLock.isHeld) wakeLock.release()
        MeshState.running.value = false
        MeshState.stats.value = MeshState.stats.value.copy(
            advertising = false,
            scanning = false
        )
        MeshState.logDebug("service stopped")
        super.onDestroy()
    }

    /** P4: how many epochs of our own frame hashes stay eligible for a reflection receipt.
     *  Must outlive the relay path (250 ms queue poll + 2 s air time) across a rollover. */
    private fun ownHashRetentionEpochs(cfg: MeshConfig): UInt =
        (cfg.messageRepeatEpochs.toUInt() + 1u).coerceAtLeast(2u)

    /** P4: remember a frame we just originated so its relayed echo is recognisable.
     *  [carriedText] separates a real message from the empty presence frames LOCAL airs
     *  between sparse re-broadcasts — echoing a presence frame is not a delivery receipt. */
    private fun rememberOwnFrame(
        hash: ByteArray,
        epoch: UInt,
        cfg: MeshConfig,
        carriedText: Boolean,
        private: Boolean = false
    ) {
        val hex = hash.joinToString("") { "%02x".format(it) }
        val retain = ownHashRetentionEpochs(cfg)
        synchronized(ownHashesLock) {
            ownHashes[hex] = OwnFrame(epoch, carriedText, private)
            val iter = ownHashes.entries.iterator()
            while (iter.hasNext()) {
                val e = iter.next()
                if (epoch >= e.value.epoch && epoch - e.value.epoch > retain) {
                    iter.remove()
                    ownHashesAcked.remove(e.key)
                }
            }
            // Belt and braces: bound the map even if epochs run backwards (clock change).
            while (ownHashes.size > 16) {
                val oldest = ownHashes.keys.first()
                ownHashes.remove(oldest)
                ownHashesAcked.remove(oldest)
            }
        }
    }

    /**
     * P4: claim [hash] as the echo of one of our own recent originations, at most once per
     * origination. Returns null when it is not ours; otherwise the remembered frame, whose
     * [OwnFrame.carriedText] says whether a delivery receipt is warranted.
     */
    private fun claimOwnEcho(hash: ByteArray): OwnFrame? {
        val hex = hash.joinToString("") { "%02x".format(it) }
        synchronized(ownHashesLock) {
            val own = ownHashes[hex] ?: return null
            return if (ownHashesAcked.add(hex)) own else null
        }
    }

    /**
     * How many times to air one relayed frame, spread across the epoch.
     *
     * A peer relays a given frame only ONCE per epoch — every later copy it hears that epoch
     * is a dedup DUPLICATE — so the originator's delivery receipt hangs on catching a single
     * ~2 s burst. Jitter stops the two devices firing in unison but adds no second chance:
     * one lost burst still costs a full epoch.
     *
     * Redundancy is spent where it is actually needed. With one neighbour there is exactly
     * one possible relayer and no path diversity at all, so repeat; in a crowd many peers
     * relay the same frame independently, which is better diversity than any single phone
     * repeating itself, so fall back to one airing and keep the airtime. This scales DOWN
     * with density, so a dense protest cannot be turned into a relay storm.
     */
    private fun relayRepeatsForDensity(): Int {
        val neighbors = MeshState.stats.value.neighborsThisEpoch
        return when {
            neighbors <= 1 -> RELAY_REPEATS_MAX
            neighbors <= 3 -> 2
            else -> 1
        }
    }

    /** B8: enqueue a relay task with tier priority, evicting the lowest-priority queued
     *  task when the queue is full (never the new LOCAL echo). */
    private fun enqueueRelay(frame: ByteArray, msgType: Int, repeats: Int = relayRepeatsForDensity()) {
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
            relayQueue.addLast(RelayTask(frame, prio, relaySeq++, repeats.coerceAtLeast(1)))
        }
    }

    /**
     * A1/C2: PoCP verification accepting the frame's own epoch sketch OR the previous
     * epoch's completed sketch. Marks rotate every epoch, so a sketch built from epoch N-1
     * marks only ever matches the verifier's N-1 bucket — at rollover an honest sender
     * signs that completed sketch with witness seed N-1, and we must try both.
     *
     * Returns 0 = Valid, 1 = CellMismatch (witness MAC valid but sketches disjoint —
     * an honestly remote cell), 2 = Stale (bad MAC / unverifiable),
     * [POCP_NO_LOCAL_SKETCH] = we hold no sketch for any candidate bucket, so no verdict is
     * possible. That last case is NOT a judgement about the frame: it means we have not yet
     * heard anything in those epochs. Callers must not cache it as a decision.
     */
    private fun verifyPocpAcrossRollover(
        frameEp: UInt,
        divSketch: ByteArray,
        prefix: ByteArray,
        wit: ByteArray,
        cfg: MeshConfig
    ): Int {
        var best = -1
        // Candidate buckets: the frame's own epoch, the previous one (marks rotate, so a
        // sketch signed at a rollover only matches the N-1 bucket), and the NEXT one — which
        // covers a receiver running one epoch BEHIND the sender. Sketches are bucketed by the
        // frame's own epoch field, so PoCP is more skew-sensitive than the ±2-epoch K4 gate:
        // without the +1 bucket a receiver with a slow clock silently CellMismatched
        // everything from a faster peer.
        val candidates = buildList {
            add(frameEp)
            if (frameEp > 0u) add(frameEp - 1u)
            add(frameEp + 1u)
        }
        for (ep in candidates) {
            val sketch = MeshState.measurement.localSketch(ep, seed, cfg.rssiFloorDbm)
            if (sketch.isEmpty()) continue
            val v = pocpVerifyWitnessLocal(sketch, divSketch, ep, prefix, wit, cfg.tauThreshold).toInt()
            if (v == 0) return 0
            // Keep the most favourable verdict any candidate bucket produced. Taking the last
            // one would let a bucket the sender never meant to be judged against (epoch+1, say)
            // overwrite a better answer from the bucket it actually signed.
            if (best < 0 || pocpRank(v) < pocpRank(best)) best = v
        }
        return if (best >= 0) best else POCP_NO_LOCAL_SKETCH
    }

    /** Preference order across candidate buckets: Valid > Unattested > CellMismatch > Stale. */
    private fun pocpRank(verdict: Int): Int = when (verdict) {
        0 -> 0
        POCP_UNATTESTED -> 1
        1 -> 2
        2 -> 3
        else -> 4
    }

    /**
     * Single ingest path for received frames, shared by the BLE scan callback and GattPlane.
     *
     * Invariants enforced:
     *   - Byte arrays that are not exactly 226 bytes are silently dropped (checked by frameDecodes
     *     in Rust core, which returns false for any length != 226).
     *   - No frame bytes are parsed in Kotlin; all interpretation is done by Rust core functions.
     */
    /**
     * Crash barrier. ingestFrame runs on BLE binder threads and on the GATT callback thread;
     * an exception anywhere inside it (FFI edge case, encrypted-prefs I/O, OOM on a malformed
     * frame) propagated straight to the default uncaught handler and killed the process. One
     * bad frame from any nearby device could take the app down.
     */
    private fun ingestFrame(bytes: ByteArray, rssi: Int) {
        try {
            ingestFrameInner(bytes, rssi)
        } catch (e: Throwable) {
            MeshState.logDebug("ingestFrame failed (frame dropped): ${e::class.java.simpleName}: ${e.message}")
        }
    }

    private fun ingestFrameInner(bytes: ByteArray, rssi: Int) {
        val cfg = MeshState.config
        val nowMs = System.currentTimeMillis()
        val ownEpoch = (nowMs / cfg.epochMs).toUInt()

        rxTotal.incrementAndGet()
        if (!frameDecodes(bytes)) {
            rxUndecodable.incrementAndGet()
            return
        }
        val hash = frameHash(bytes)
        val frameEp = frameEpoch(bytes)
        if (hash == null || frameEp == null) {
            rxUndecodable.incrementAndGet()
            return
        }
        val hashHex = hash.joinToString("") { "%02x".format(it) }
        // S6: frames already judged unverifiable are dropped on a map lookup. Both rejection
        // paths below return BEFORE anything is inserted into dedup, so without this the same
        // bytes replayed cost a full Ed25519 verify every single time, for as long as the
        // attacker keeps sending them — over BLE and over unauthenticated GATT writes alike.
        if (isRejected(hashHex)) {
            rxReplayedReject.incrementAndGet()
            return
        }
        if (!frameVerifySelf(bytes)) {
            // Signature failure on a well-formed frame is NOT normal background noise: it
            // means a peer is transmitting frames this build cannot authenticate (version
            // skew, corrupted reassembly over GATT, or forgery). Counted, not logged per
            // frame, so a flood cannot spam the log.
            rxBadSig.incrementAndGet()
            rememberRejected(hashHex)
            return
        }

        // Hoisted above the K4 gate: the freshness window is tier-dependent, so the gate has
        // to know the message type. One decode serves both.
        val wp = frameWitnessParts(bytes)

        // K4: epoch freshness — reject frames stamped too far from our own epoch (before
        // dedup, so a stale/future frame never occupies a dedup slot).
        //
        // The window is WIDER FOR PUBLIC TIERS than for private, and the asymmetry is not
        // cosmetic. At ±2 (20 s at a 10 s epoch) the wall sat well inside the time an 8-hop
        // regional chain actually needs — roughly 2–4 s per hop once queueing, jitter, the
        // ~2 s airing and scan latency are counted — so the outer hops of the mesh the TTL
        // advertises were being dropped as "skewed" while working exactly as designed.
        //
        // Private stays at ±2 because its key schedule cannot follow: the ratchet retains
        // only ONE previous epoch key, fastForwardChains pins the chain to epoch-1 against
        // this same window, and PRIVATE_REAIRINGS is sized to it. Admitting a private frame
        // 3–4 epochs old would pass K4 and VDL, relay normally, and then open under no key —
        // invisible on both ends, which is the failure mode this codebase keeps rediscovering.
        //
        // Widening this is only safe because dedup now decays on the LOCAL clock over a
        // window that covers it (DEDUP_RETENTION_EPOCHS); replay protection moved from the
        // gate onto dedup. Do NOT gate freshness on TTL instead — TTL is in the hop-mutable
        // region excluded from the hash and signature, so it is attacker-settable, and
        // trusting it would let anyone re-air an arbitrarily old frame by claiming one hop.
        val isPrivateTier = wp?.msgType?.toInt() == 3
        val freshnessWindow = if (isPrivateTier) PRIVATE_FRESHNESS_EPOCHS else PUBLIC_FRESHNESS_EPOCHS
        val diff = frameEp.toLong() - ownEpoch.toLong()
        if (diff > freshnessWindow || diff < -freshnessWindow) {
            val pair = Pair(frameEp, ownEpoch)
            if (pair != lastSkewPair) {
                lastSkewPair = pair
                MeshState.logDebug(
                    "epoch skew: frame=$frameEp own=$ownEpoch (check epochMs match + clocks)"
                )
            }
            // Surface it: a skewed peer is invisible, and silence is indistinguishable from
            // "nobody is around". diff is in epochs; report it in seconds, which is what the
            // user can actually act on.
            rxSkewed.incrementAndGet()
            // S6: only PAST-stale frames are cached. Their epoch is fixed and our clock only
            // moves forward, so they can never become admissible. A future-stamped frame is
            // the opposite — it becomes legitimately fresh as we catch up, and caching it
            // would blacklist a frame we are about to want.
            if (diff < 0) rememberRejected(hashHex)
            val offBySec = diff * cfg.epochMs / 1000L
            // Word this as what we actually measured. All we know is that the frame's OWN
            // epoch field is N seconds out of step with ours — which is a skewed clock OR a
            // frame that took too long to build (the VDL solve) or to relay. Naming the clock
            // as the cause sends the user to fix something that may be perfectly fine.
            MeshState.clockSkewWarning.value =
                "⚠ FRAMES ARRIVING ${kotlin.math.abs(offBySec)}s " +
                    (if (offBySec > 0) "AHEAD OF" else "BEHIND") +
                    " THIS PHONE'S CLOCK — THEY ARE BEING DROPPED. CHECK THAT BOTH PHONES ARE " +
                    "ON NETWORK TIME AND SET TO THE SAME EPOCH LENGTH. IF BOTH MATCH, THE " +
                    "FRAME EITHER TOOK TOO LONG TO BUILD OR TRAVELLED TOO MANY HOPS."
            return
        }

        val mark = frameMark(bytes) ?: return

        // Send-and-listen (spec): a copy of OUR OWN frame coming back over the relay path
        // means at least one peer relayed it. B1: that echo is NOT a delivery guarantee —
        // a single adversarial device can forge it by relaying once and blackholing the
        // rest. LOCAL therefore only backs off its re-airing interval; BROADCAST stops, which
        // it can afford because its TTL does the propagating, not our re-origination.
        // Our hash went into dedup at origination, so this check must run BEFORE the dedup
        // gate. TTL sits outside the hashed region, so the relayed echo hashes identically.
        val ownEcho = claimOwnEcho(hash)
        if (ownEcho != null) {
            MeshState.logDebug(
                "own echo heard (epoch=${ownEcho.epoch} text=${ownEcho.carriedText}" +
                    (if (ownEcho.private) " private" else "") + ") — a peer relayed us"
            )
            if (ownEcho.private) {
                // Private echoes are judged on their own terms, because none of the public
                // machinery applies to them: a private send never touches outgoingText (it
                // rides the privateSends queue), so the text condition below is always false
                // for it, and its re-airings are counted by reairPendingPrivate rather than
                // by outgoingSetAtEpoch, so the BROADCAST cut-off must not fire either.
                // Registering the hash alone would therefore have ticked nothing.
                //
                // What this proves is weaker than it looks and the wording has to match: a
                // peer put the SEALED frame back on air. Any peer relays private frames —
                // they are opaque — so this says nothing about whether the recipient could
                // open it. There is no acknowledgement in this protocol and there will not
                // be one; an ack would tie a receiver to a sender.
                MeshState.privateRowTs?.let { MeshState.markOutgoing(it, SendState.ECHOED) }
                MeshState.receipt.value =
                    "✓ private message relayed by a peer (not proof it was opened)"
            } else {
                // Only a frame that actually carried the user's text is a delivery signal.
                // LOCAL airs empty presence frames between sparse re-broadcasts; echoing one
                // of those is liveness, not receipt.
                val textEcho = ownEcho.carriedText && MeshState.outgoingText.value.isNotEmpty()
                if (textEcho) {
                    reflectionHeard = true
                    MeshState.outgoingEchoed.value = true
                    // Second tick: a peer put our frame back on air. Still not "delivered" —
                    // one adversarial device can relay once and blackhole everything after.
                    MeshState.outgoingRowTs?.let { MeshState.markOutgoing(it, SendState.ECHOED) }
                    MeshState.receipt.value =
                        "✓ heard back once — a peer relayed it (not proof of delivery)"
                    if (MeshState.outgoingTier.value != SendTier.LOCAL) {
                        // Clear NOW, not at the next epoch boundary.
                        //
                        // This used to rewind outgoingSetAtEpoch so that the expiry check at
                        // the top of the epoch loop would fire. That check only runs on the
                        // boundary tick, so the text kept airing for the remainder of the
                        // current epoch — up to a full epochMs (10 s by default) after the ✓✓
                        // had already rendered from the same echo. Field-observed as "the
                        // message takes some time to disappear after the relay came back".
                        //
                        // The outgoingText collector re-advertises on every change, so
                        // clearing here also pulls the frame off air immediately instead of
                        // airing one more dead copy, and it nulls outgoingSetAtEpoch — which
                        // keeps the boundary expiry from firing again and overwriting the
                        // accurate "heard back once" receipt set just above.
                        //
                        // LOCAL is excluded on purpose and the asymmetry is deliberate:
                        // BROADCAST is TTL 8 and propagates without us, so re-origination there
                        // is redundancy and an echo may end it. LOCAL is TTL 1 — re-origination
                        // is its ONLY propagation mechanism — so an echo must not end it, only
                        // slow it (LOCAL_ECHO_BACKOFF).
                        MeshState.outgoingText.value = ""
                        MeshState.logDebug("reflection heard: mesh is carrying our broadcast; stopped re-origination")
                    }
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

        // Admission check only — the INSERT happens at the end, once we have actually acted
        // on this frame. Inserting here meant a frame that transiently failed verification
        // (empty local sketch at the start of an epoch, contacts not yet loaded) was stuck in
        // the seen-set for the whole ~3-epoch window: every retransmission of those exact
        // bytes was dropped here, so the loss could never self-heal.
        // frameEp buckets the entry (untrusted, from the wire); ownEpoch drives time-decay.
        when (dedup.checkEpoch(hash, frameEp, ownEpoch)) {
            FfiDedupVerdict.FRESH -> Unit
            FfiDedupVerdict.DUPLICATE -> {
                rxDuplicate.incrementAndGet()
                return
            }
            FfiDedupVerdict.BUCKET_FULL -> {
                // C8 anti-eviction sub-cap hit. This is NOT a duplicate: a fresh, validly
                // signed frame is being refused, and while it lasts nothing stamped with this
                // epoch is displayed, relayed, or measured. 1024 signed frames is cheap to
                // produce, so treat a sustained occurrence as a jamming signal.
                if (lastBucketFullEpoch != frameEp) {
                    lastBucketFullEpoch = frameEp
                    MeshState.logDebug(
                        "dedup bucket for epoch $frameEp is FULL — further frames stamped with " +
                            "this epoch are being refused (not displayed, not relayed); possible flood"
                    )
                }
                return
            }
        }

        // NOTE: the skew banner is deliberately NOT cleared here. One good frame only proves
        // that ONE peer agrees with us — in a mixed group the banner flapped off the instant
        // any healthy phone was heard, while the skewed phone stayed completely partitioned
        // and invisible. Clearing is now driven by whole epochs with no skewed frames at all
        // (see the rollover accounting), which is the condition that actually means "nobody
        // nearby is being dropped".

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
            if (!vdlCheckFrame(bytes)) {
                MeshState.logDebug("drop: private frame failed VDL proof-of-work check")
                // A bad PoW is a permanent property of these bytes — mark seen so a flood of
                // copies is rejected at the cheap gate instead of re-running the check.
                dedup.insertEpoch(hash, frameEp)
                return // invalid PoW: drop, do not relay
            }
            var privatePlaintext: String? = null
            var privateLabel: String? = null
            var contactsTried = 0
            var keysTried = 0
            for (contact in PairStore.contacts(this)) {
                contactsTried++
                // A3: v2 contacts try the epoch-ratcheted key for the frame's epoch
                // (fast-forwarding when the sender is ahead); v1 uses the static key.
                for (key in PairStore.candidateKeys(this, contact, frameEp)) {
                    keysTried++
                    val pt = openPrivateBodyOnly(bytes, key)
                    if (pt != null && privatePlaintext == null) {
                        privatePlaintext = pt
                        privateLabel = contact.label
                    }
                }
            }
            if (privatePlaintext == null) {
                // A private frame that reaches us, passes VDL, and opens under no key is the
                // single most confusing failure in the app: it is relayed normally, so the
                // mesh looks healthy, while the recipient's screen stays empty and the sender
                // gets no signal. Most often it is simply not addressed to us — but with a
                // divergent ratchet anchor it is EVERY frame, so the counts matter.
                MeshState.logDebug(
                    "private frame not for us (or key mismatch): tried $keysTried key(s) " +
                        "across $contactsTried contact(s) at epoch $frameEp"
                )
            }
            if (privatePlaintext != null) {
                // A3: persist any fast-forwarded chain state (past keys deleted). This does a
                // synchronous commit() to EncryptedSharedPreferences, so it must not run on
                // the BLE binder thread that delivered this frame — disk I/O there stalls
                // scan callback delivery for every other frame in flight.
                val label = privateLabel!!
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        PairStore.noteOpened(this@MeshService, label, frameEp)
                    } catch (e: Exception) {
                        // No label: the debug log is exportable, and naming a contact on a
                        // failure path is social-graph metadata a seized export hands over.
                        MeshState.logDebug("noteOpened failed for a contact: ${e.message}")
                    }
                }
                MeshState.appendMessage(
                    MsgRow(
                        tsMs = System.currentTimeMillis(),
                        epoch = frameEp,
                        markHexPrefix = "🔒 $privateLabel",
                        rssi = rssi,
                        text = privatePlaintext,
                        mine = false,
                        tier = SendTier.PRIVATE,
                        direct = direct,
                        contactLabel = privateLabel
                    )
                )
            }
            // Relay regardless of whether we could decrypt (multi-hop delivery).
            relayFrame(bytes)?.let { enqueueRelay(it, 3) }
            // Acted on: relayed, and decrypted if it was for us. Safe to mark seen.
            dedup.insertEpoch(hash, frameEp)
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
        var unjudged = false
        var corroborations = 0u
        var unattested = false
        if (wp != null) {
            val msgType = wp.msgType.toInt()
            val hasWitness = wp.pocpWit.any { it != 0.toByte() } ||
                wp.divSketch.any { it != 0.toByte() }
            if (!hasWitness) {
                relayOnly = true // A1: relay-only, never display
                // Every "the other phone relayed it but never showed it" report lands here.
                // An up-to-date peer never originates witnessless, so this now means the
                // sender is running an older build or genuinely had no cell to attest to.
                MeshState.logDebug(
                    "relay-only: frame from ${mark.joinToString("") { "%02x".format(it) }.take(8)} " +
                        "carries NO PoCP witness — relayed but not displayed (sender heard nobody?)"
                )
            } else {
                when (val verdict = verifyPocpAcrossRollover(wp.epoch, wp.divSketch, wp.framePrefix, wp.pocpWit, cfg)) {
                    0 -> displayOk = true // Valid: co-present with our cell
                    POCP_UNATTESTED -> {
                        // The cells touch on exactly one element. Show it, badged unverified.
                        //
                        // Dropping this is what broke cold-start LOCAL delivery: a phone whose
                        // scanner has just started claims a sketch holding only its own mark,
                        // which scores 1/N against an established peer's cell and fell under
                        // tau for any crowd of 4 or more. Its danger alerts were discarded by
                        // exactly the people best placed to act on them, and the denser the
                        // crowd the more certain the drop. A single-byte forgery is
                        // indistinguishable from it here, so it cannot be trusted either —
                        // hence displayed, never attested.
                        displayOk = true
                        unattested = true
                        MeshState.logDebug(
                            "unattested: one-element cell overlap type=$msgType frameEpoch=$frameEp " +
                                "— displayed but NOT co-presence evidence"
                        )
                    }
                    1 -> {
                        if (msgType == 2) {
                            displayOk = true // honest remote-cell broadcast
                        } else {
                            // LOCAL from a cell that doesn't overlap ours: dropped entirely.
                            MeshState.logDebug(
                                "drop LOCAL: PoCP CellMismatch (jaccard < tau=${cfg.tauThreshold}) — " +
                                    "our epoch-$frameEp sketch has " +
                                    "${MeshState.measurement.sketchFill(frameEp, seed, cfg.rssiFloorDbm)} marks"
                            )
                        }
                    }
                    POCP_NO_LOCAL_SKETCH -> {
                        // We hold no sketch for any candidate bucket, so this is not a
                        // judgement about the frame — we simply have not heard anything yet.
                        // Leave it out of the seen-set so a re-air can still be displayed.
                        unjudged = true
                        MeshState.logDebug(
                            "defer: no local sketch for epochs ${frameEp - 1u}..${frameEp + 1u} — " +
                                "cannot judge this frame yet, leaving it eligible for retry"
                        )
                    }
                    else -> {
                        // Stale: the witness MAC did not verify against any sketch bucket.
                        MeshState.logDebug(
                            "drop: PoCP verdict=$verdict (stale/unverifiable witness) type=$msgType " +
                                "frameEpoch=$frameEp ownEpoch=$ownEpoch — no display, no relay"
                        )
                    }
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

        // Relay if the frame is either displayable or relay-only — EXCEPT an unattested LOCAL,
        // which is displayed but never amplified.
        //
        // The witness MAC is keyed on public material (witness_key(div_sketch, seed)), so a
        // one-element cell overlap is mintable by a device that heard nothing: ~255 self-signed
        // frames sweeping div_sketch = [b, 0xFF...] land one byte in the victim's truncated
        // cell. Showing that badged "unverified" is the T2 cold-start fix and is deliberate —
        // relaying it is not, and would hand such an attacker a free extra hop of honest reach.
        //
        // BROADCAST is untouched: it already relays on the strictly weaker CellMismatch
        // verdict, so refusing to relay the better-attested Unattested there would be backwards.
        //
        // Cost: a cold-start phone's LOCAL alert reaches direct neighbours only (one hop, not
        // two) until its cell fills — about one epoch. That is the intended trade, not a
        // regression to "fix" later.
        val relayType = wp?.msgType?.toInt() ?: 2
        val amplify = relayOnly || (displayOk && !(unattested && relayType == 1))
        if (amplify) {
            relayFrame(bytes)?.let { enqueueRelay(it, relayType) }
        }

        // Mark seen only now, and only if we reached an actual decision. A frame we could
        // not judge (no local sketch for any candidate epoch) stays out of the seen-set so a
        // re-air moments later — once we have heard someone — can still be displayed.
        // Inserting before the decision meant one transient failure suppressed those exact
        // bytes for the whole ~3-epoch dedup window and the loss never self-healed.
        if (!unjudged) dedup.insertEpoch(hash, frameEp)

        // Display only when verified and not relay-only.
        if (displayOk) {
            val text = frameBodyText(bytes)
            if (!text.isNullOrEmpty()) {
                val markHex = mark.joinToString("") { "%02x".format(it) }
                rxShown.incrementAndGet()
                val tier = if (wp?.msgType?.toInt() == 1) SendTier.LOCAL else SendTier.BROADCAST
                // How much evidence the co-presence check actually had. A verified
                // witness against a 2-3 mark cell is cheaply forgeable by someone who
                // was never there (see the pocp module header), so the UI must not
                // present it as proof.
                // Filled slots, not list length. localSketch() always returns KMV_K=16 entries
                // with ULong.MAX_VALUE for empty, so `.size` was the constant 16 and this
                // comparison could never fire — the "weak cell" badge was unreachable.
                val cellSize = MeshState.measurement
                    .sketchFill(frameEp, seed, MeshState.config.rssiFloorDbm)
                // Re-airings of the same alert collapse into one row with a count. The old
                // (text, mark-prefix) suppression key rotated every epoch — exactly as fast
                // as the sender re-aired — so it never suppressed anything and one message
                // rendered as messageRepeatEpochs identical rows.
                MeshState.appendOrMergeIncoming(
                    MsgRow(
                        tsMs = System.currentTimeMillis(),
                        epoch = frameEp,
                        markHexPrefix = markHex.take(8),
                        rssi = rssi,
                        text = text,
                        mine = false,
                        tier = tier,
                        direct = direct,
                        corroborations = corroborations.toInt(),
                        lowConfidenceCell = cellSize < MIN_TRUSTWORTHY_CELL,
                        unattested = unattested
                    )
                )
            }
        }
    }

    /**
     * Re-air a still-fresh sealed private frame, once per epoch rollover.
     *
     * Private used to be strictly one-shot: a single ~6 s advertising window, no repeats, no
     * receipt. Everything else on the mesh re-originates for messageRepeatEpochs, so private
     * was by far the easiest message to lose — and losing it was completely silent on both
     * ends. Re-airing the already-sealed bytes costs no second VDL solve.
     */
    private fun reairPendingPrivate(epoch: UInt, cfg: MeshConfig) {
        val pending = synchronized(pendingPrivateLock) {
            val p = pendingPrivate ?: return
            // Past the receiver's freshness gate, or out of attempts: drop it.
            if (p.airingsLeft <= 0 || epoch < p.sealedEpoch || epoch - p.sealedEpoch > 2u) {
                pendingPrivate = null
                return
            }
            p.airingsLeft -= 1
            if (p.airingsLeft <= 0) pendingPrivate = null
            p
        }
        val restoreFrame = currentPublicFrame ?: return
        if (privateTransportActive) return
        val windowMs = minOf(cfg.messageRepeatEpochs.toLong() * cfg.epochMs, 6_000L)
        privateTransportActive = true
        val started = radio.advertisePrivateOnce(
            frame = pending.frame,
            durationMs = windowMs,
            restoreFrame = { currentPublicFrame },
            codedPhy = cfg.codedPhy,
            advIntervalMs = cfg.advIntervalMs
        ) {
            privateTransportActive = false
            gattPlane.currentFrame = currentPublicFrame ?: restoreFrame
        }
        if (started) {
            gattPlane.currentFrame = pending.frame
            MeshState.logDebug("private frame re-aired (${pending.airingsLeft} left)")
        } else {
            privateTransportActive = false
        }
    }

    /**
     * The outgoing text this epoch's frame should carry — "" meaning air a presence-only frame.
     *
     * Reads live state on purpose, so it must be called at the moment the frame is actually
     * built (after the TX jitter), not at the epoch boundary. See the call site.
     *
     * Everything except LOCAL-after-echo airs the text on every epoch. LOCAL that has heard
     * itself relayed backs the interval off along [LOCAL_ECHO_BACKOFF] — the message keeps its
     * full [LOCAL_REBROADCAST_WINDOW_MS] lifetime, it just gets quieter. Read the constant's
     * comment before changing this; the split between "interval" and "lifetime" is load-bearing.
     */
    private fun textToAirThisEpoch(epoch: UInt, cfg: MeshConfig): String {
        val raw = MeshState.outgoingText.value
        if (raw.isEmpty()) return ""
        if (MeshState.outgoingTier.value != SendTier.LOCAL) return raw
        // No echo yet: an unheard LOCAL alert re-airs every single epoch.
        val echoedAt = echoEpoch ?: return raw
        // Guards a UInt underflow if the wall clock jumped backwards past the anchor.
        if (epoch < echoedAt) return raw
        val sinceEpochs = (epoch - echoedAt).toLong()
        val sinceMs = sinceEpochs * cfg.epochMs
        // Last tier is unbounded, so first() always matches.
        val targetIntervalMs = LOCAL_ECHO_BACKOFF.first { sinceMs < it.first }.second
        val period = (targetIntervalMs / cfg.epochMs).coerceAtLeast(1L)
        return if (sinceEpochs % period == 0L) raw else ""
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
        // P2/P3/P5: register OUR OWN mark for this epoch before building the sketch. The mark
        // is a pure function of (seed, beaconSeed), so the unwitnessed frame built here has
        // the same mark as the witnessed frame built below — we build it once to read the
        // mark out, and reuse it as the fallback if witnessed construction fails.
        //
        // Why this matters: a cell is "the devices in RF range of each other, INCLUDING me".
        // Without self-inclusion a phone that had heard nobody yet produced an EMPTY sketch
        // and originated a WITNESSLESS frame, which every receiver relays but never displays
        // (the relayOnly branch in ingestFrame) — while the originator still heard its own
        // relayed echo and printed a delivery receipt. Blank screen on one phone, "heard
        // back once" on the other. Self-inclusion also fixes the two-device case: A held
        // {mark_B} and B held {mark_A}, which are disjoint, so LOCAL could never display.
        val baseFrame = makeMessageFrame(seed, epoch, beaconSeed, localImmediate, effectiveText)
        if (baseFrame != null) {
            frameMark(baseFrame)?.let { MeshState.measurement.recordSelf(it, epoch) }
        }

        // A1/C2: sign whichever cell view is RICHER — the current epoch's bucket or the
        // previous epoch's completed one (receivers try both, plus epoch+1, so either
        // verifies).
        //
        // This used to key off "is the current sketch empty", which worked only because a
        // freshly rolled-over bucket WAS empty. Now that our own mark is always in it, the
        // current bucket is never empty, so that test would always pick it — and immediately
        // after a rollover it holds nothing but our own mark. A receiver in a crowd has a
        // fuller bucket, so a one-element claim scores 1/N and falls under tau: honest frames
        // would CellMismatch for the first moments of every epoch, worse the denser the crowd.
        // Comparing sizes keeps the original rollover intent and the self-inclusion guarantee.
        val sketchCur = MeshState.measurement.localSketch(epoch, seed, cfg.rssiFloorDbm)
        val sketchPrev = MeshState.measurement.localSketch(epoch - 1u, seed, cfg.rssiFloorDbm)
        val (sketch, witEpoch) = if (sketchPrev.size > sketchCur.size) {
            sketchPrev to (epoch - 1u)
        } else {
            sketchCur to epoch
        }
        val divSketch = if (sketch.isNotEmpty()) pocpSketchToDivSketch(sketch) else null
        if (divSketch == null) {
            // Unreachable once recordSelf has run for this epoch — a self-inclusive sketch
            // is never empty. Loud, because the resulting frame is relay-only at receivers.
            MeshState.logDebug(
                "WITNESSLESS origination epoch=$epoch (own mark missing) — receivers will " +
                    "relay this frame but NOT display it"
            )
        }
        val frame = (
            if (divSketch != null) {
                makeMessageFrameWithWitness(
                    seed, epoch, beaconSeed, localImmediate, effectiveText, ttl, divSketch, witEpoch
                )
            } else {
                null
            }
            ) ?: baseFrame
        if (frame != null) {
            currentPublicFrame = frame
            // Insert our own frame's hash into dedup: a relayed copy of our frame comes back
            // with TTL decremented, but TTL sits in the hop-mutable region excluded from the
            // frame hash — so the echo has OUR hash and dedup drops it instead of showing our
            // own message as incoming.
            frameHash(frame)?.let {
                rememberOwnFrame(it, epoch, cfg, carriedText = effectiveText.isNotEmpty())
                dedup.checkAndInsertEpoch(it, epoch, epoch)
            }
            if (!privateTransportActive) {
                // Full stop+start every epoch — see BleRadio note on setAdvertisingData.
                radio.startAdvertising(frame, cfg.codedPhy, cfg.advIntervalMs)
                // Push to GATT plane: notifies subscribed centrals and writes to connected peripherals.
                gattPlane.currentFrame = frame
                // The one tick fires from radio.onFrameOnAir once the stack CONFIRMS the set
                // started. Ticking here would claim "on air" for a start the controller may
                // still refuse.
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
            // Radio FIRST, before anything is zeroed. radio.stop() also cancels a pending
            // private-window restore; if that runnable were still armed while we zeroed the
            // frames below, it would put 226 zero bytes of mesh service data back on air
            // after the wipe. Silencing the transmitter is the part an RF observer can see,
            // so it must not wait behind key zeroization.
            if (::radio.isInitialized) radio.stop()
            if (::gattPlane.isInitialized) gattPlane.stop()

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
            // The self-test report holds `heard marks` / `direct marks` — the per-epoch
            // pseudonyms of every device that was physically nearby — plus a stable pair
            // fingerprint per contact. That is the same co-presence and social-graph evidence
            // measurement.clear() destroys one line above. It lives on the MeshState singleton
            // and stopSelf() does not kill the process, so without this it survives the wipe,
            // keeps rendering in the drawer, and SHARE REPORT still works.
            MeshState.selfTestLog.value = emptyList()
            MeshState.selfTestRunning.value = false
            MeshState.outgoingEchoed.value = false

            // S12: the private send path holds state none of the above reaches. The queue is
            // the important one — it is the only place a message the user typed still exists
            // in the CLEAR once its composer row is gone, and a wipe triggered mid-send (the
            // realistic case) is exactly when something is sitting in it.
            while (MeshState.privateSends.tryReceive().isSuccess) { /* drop plaintext */ }
            synchronized(pendingPrivateLock) {
                pendingPrivate?.frame?.fill(0)
                pendingPrivate = null
            }
            // Sealed frames we originated, and the marks that identify them as ours.
            synchronized(ownHashesLock) {
                ownHashes.clear()
                ownHashesAcked.clear()
            }
            MeshState.outgoingRowTs = null
            MeshState.privateRowTs = null
            MeshState.outgoingAiring.value = false
            MeshState.receipt.value = null
            // C7: force the UI to drop remembered Contact objects (they hold pair keys until
            // GC — a documented JVM limit; recomposition to an empty list is the best we can do).
            MeshState.contactsVersion.value += 1

            // Zeroize in-memory secrets. seed is lateinit — a cold-start ACTION_PANIC
            // (service never fully started) reaches here before seed is assigned.
            if (::seed.isInitialized) seed.fill(0)
            currentPublicFrame?.fill(0)
            currentPublicFrame = null
            synchronized(relayQueueLock) {
                // Queued relays are other people's frames, held in full. Zero before dropping.
                relayQueue.forEach { it.frame.fill(0) }
                relayQueue.clear()
            }
            synchronized(marksLock) {
                localImmediateMarks.forEach { it.fill(0) }
                localImmediateMarks.clear()
            }

            // Radio/GATT were already stopped at the top of the wipe.

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
            // Keep the body off the lock screen entirely. A foreground service must show
            // SOMETHING, but nothing it shows needs to be readable before unlocking.
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()
    }
}

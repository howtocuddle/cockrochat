package org.bileichat.mesh

import kotlinx.coroutines.flow.MutableStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Stats(
    val epoch: UInt,
    val neighborsThisEpoch: Int,
    val totalHeard: Int,
    val localSketch: List<ULong>,
    val advertising: Boolean,
    val scanning: Boolean,
    val codedPhyActive: Boolean,
    val note: String = ""
)

data class MsgRow(
    val tsMs: Long,
    val epoch: UInt,
    val markHexPrefix: String, // first 8 hex chars
    val rssi: Int?,
    val text: String,
    val mine: Boolean,
    val tier: SendTier = SendTier.BROADCAST,
    /** True when the frame arrived at its origination TTL — straight off the sender's
     *  radio, no relay hop. Drives the per-message trust meter. */
    val direct: Boolean = false,
    /** A2: distinct DIRECT-heard claims for this alert body. A HINT for the user —
     *  never a proof (a determined nearby attacker can forge claims). */
    val corroborations: Int = 0,
    /** For PRIVATE messages: the paired contact label (e.g. "ALICE"). Null for non-private. */
    val contactLabel: String? = null,
    /**
     * True when the PoCP witness verified against a local cell holding fewer than
     * [MIN_TRUSTWORTHY_CELL] marks. The witness MAC key is derived from public material, so
     * co-presence rests entirely on the Jaccard ratio — and a one-element claim scores 1/N,
     * clearing tau for any cell of 3 or fewer. An attacker who has never been near the cell
     * can sweep all 256 single-byte sketches and land 2-3 accepted forgeries. Verification
     * at that size is not evidence, so the badge must not claim it is.
     */
    val lowConfidenceCell: Boolean = false,
    /**
     * The sender's claimed cell touched ours on exactly ONE element (core verdict
     * Unattested). Distinct from [lowConfidenceCell]: our own cell may be perfectly large and
     * healthy — it is the OVERLAP that carries no weight. Shown separately because "weak
     * cell" would misdescribe it, and the two warnings mean different things to a reader.
     */
    val unattested: Boolean = false,
    /** How many times this exact alert has been heard (re-airings collapse into one row). */
    val repeats: Int = 1,
    /** Transmission state of one of OUR messages. Null on received rows. */
    val sendState: SendState? = null
)

/**
 * WhatsApp-style delivery state for a message we sent.
 *
 * Deliberately stops short of "delivered". Nothing in this protocol can prove a specific
 * device received a specific message — there is no acknowledgement, by design, because an
 * ack would tie a receiver to a sender and undo the unlinkability the rotating marks exist
 * to provide. So the two ticks mean what the radio can actually witness.
 */
enum class SendState {
    /** Queued: the frame is being built (a private frame is solving its VDL witness). */
    SENDING,

    /** One tick: the frame is on air from this phone. */
    ON_AIR,

    /**
     * Two ticks: we heard our own frame come back, so a peer relayed it. Not proof of
     * delivery — one adversarial device can relay once and blackhole the rest.
     *
     * Weaker still on PRIVATE: relays cannot read a sealed frame, so every peer relays it
     * regardless of who it is addressed to. Two ticks there means "the mesh carried it",
     * never "the recipient could open it".
     */
    ECHOED
}

/** Below this many marks in our own cell, a verified witness is not meaningful evidence. */
const val MIN_TRUSTWORTHY_CELL = 4

enum class SendTier { LOCAL, BROADCAST, PRIVATE }

/** A queued private (Tier-3) message: recipient label + plaintext. The pair key is
 *  resolved (and ratcheted, A3) by the service at seal time — no key material rides
 *  the queue. */
data class PrivateSend(
    val label: String,
    val text: String
)

object MeshState {
    val running = MutableStateFlow(false)
    val stats = MutableStateFlow(
        Stats(
            epoch = 0u,
            neighborsThisEpoch = 0,
            totalHeard = 0,
            localSketch = emptyList(),
            advertising = false,
            scanning = false,
            codedPhyActive = false
        )
    )
    val measurement = Measurement()

    @Volatile
    var config: MeshConfig = MeshConfig()

    // Message feed — newest last, capped at 200 rows
    val messages = MutableStateFlow<List<MsgRow>>(emptyList())

    // Called from the BLE scan (binder) thread and the main thread; synchronized so the
    // read-modify-write on the StateFlow can't drop rows.
    @Synchronized
    fun appendMessage(row: MsgRow) {
        val current = messages.value
        val updated = if (current.size >= 200) current.drop(1) + row else current + row
        messages.value = updated
    }

    /**
     * Append an incoming alert, or collapse it into a recent identical one.
     *
     * A sender re-airs the same text every epoch for `messageRepeatEpochs`, and the frame
     * carries a fresh epoch (so a fresh hash — dedup can't catch it) AND a fresh mark, because
     * marks rotate per epoch for unlinkability. The display-side suppression key was
     * `text|markPrefix`, so it rotated at exactly the same rate as the repeats and never
     * matched: one sent message showed up as three identical rows.
     *
     * Merging instead of suppressing also keeps B6 honest — an attacker pre-broadcasting the
     * same words can no longer hide the real alert, because nothing is ever hidden; the row
     * is shown once with a repeat count, and the most trustworthy observation wins each field.
     */
    @Synchronized
    fun appendOrMergeIncoming(row: MsgRow, withinEpochs: UInt = 4u) {
        val current = messages.value
        val idx = current.indexOfLast {
            !it.mine && it.text == row.text && it.tier == row.tier &&
                row.epoch >= it.epoch && row.epoch - it.epoch <= withinEpochs
        }
        if (idx < 0) {
            appendMessage(row)
            return
        }
        val old = current[idx]
        // Keep the FIRST-heard timestamp and list position; upgrade the trust fields, since a
        // later copy arriving direct, or judged against a bigger cell, is better evidence.
        messages.value = current.toMutableList().also {
            it[idx] = old.copy(
                repeats = old.repeats + 1,
                direct = old.direct || row.direct,
                rssi = row.rssi ?: old.rssi,
                corroborations = maxOf(old.corroborations, row.corroborations),
                lowConfidenceCell = old.lowConfidenceCell && row.lowConfidenceCell,
                unattested = old.unattested && row.unattested
            )
        }
    }

    /**
     * [MsgRow.tsMs] of the message we are currently transmitting, so its ticks can be
     * upgraded in place. Single slot because [outgoingText] is a single slot.
     */
    @Volatile
    var outgoingRowTs: Long? = null

    /** Same, for the private send queue — private frames are sealed on their own path. */
    @Volatile
    var privateRowTs: Long? = null

    /**
     * True while a message of ours is still being re-aired.
     *
     * Surfaced because LOCAL re-originates every epoch for up to
     * LOCAL_REBROADCAST_WINDOW_MS (30 min) until it is heard back — correct for a danger
     * alert, but it was completely invisible, so an ordinary message looked like it was
     * "sending forever" with no way to stop it.
     */
    val outgoingAiring = MutableStateFlow(false)

    /**
     * True once a peer has relayed our current message back to us.
     *
     * Split from [outgoingAiring] because on LOCAL the two are simultaneously true and the UI
     * read as self-contradictory: the receipt line said "heard back once" while the banner
     * above it still said "still re-sending until a phone repeats it back". Both were correct
     * — LOCAL drops to SPARSE re-airing after an echo rather than stopping, since one forged
     * echo must not be able to silence a danger alert — but the banner has to say which of
     * the two states it is in.
     */
    val outgoingEchoed = MutableStateFlow(false)

    /** Upgrade the ticks on one of our rows. Monotonic: state never moves backwards. */
    @Synchronized
    fun markOutgoing(ts: Long, state: SendState) {
        val current = messages.value
        val idx = current.indexOfLast { it.mine && it.tsMs == ts }
        if (idx < 0) return
        val old = current[idx]
        if (old.sendState != null && old.sendState.ordinal >= state.ordinal) return
        messages.value = current.toMutableList().also { it[idx] = old.copy(sendState = state) }
    }

    // Outgoing message text
    val outgoingText = MutableStateFlow("")

    /**
     * Bumped on every explicit send. MutableStateFlow conflates equal values, so re-sending
     * the SAME text was a silent no-op: the collector in MeshService never fired, no frame
     * was ever built, and outgoingSetAtEpoch/reflectionHeard were never reset — yet the
     * user's own bubble was still appended, so the message looked sent. Sending "HELP"
     * twice in a row transmitted once.
     */
    val outgoingRevision = MutableStateFlow(0)

    /** Tier the NEXT outgoing message is sent at. Changing this re-originates the current
     *  frame with a new TTL and msgType, so it must only change on an explicit send-tier
     *  choice — never as a side effect of reading a different feed. */
    val outgoingTier = MutableStateFlow(SendTier.BROADCAST)

    /** Tier whose feed is currently VISIBLE. Split from [outgoingTier]: the tab bar drove
     *  both, so switching tabs to read LOCAL traffic mutated how an in-flight broadcast was
     *  being transmitted. Selecting a tab still sets the send tier to match (that is the
     *  intuitive behaviour) — but a bare view change no longer does. */
    val viewTier = MutableStateFlow(SendTier.BROADCAST)

    // C4: private-send QUEUE (was a single-slot StateFlow — two quick sends overwrote each
    // other and the reset could erase a send queued during the VDL solve). The service
    // consumes sequentially; trySend failure means the queue is full.
    val privateSends = kotlinx.coroutines.channels.Channel<PrivateSend>(capacity = 8)

    /** Delivery-receipt notice shown above the composer ("heard back once", "stopped").
     *  Null = nothing to show. Set by the service on reflection/expiry; cleared when a
     *  new message is composed. B1: wording must never imply guaranteed delivery. */
    val receipt = MutableStateFlow<String?>(null)

    /** Bumped whenever the pairing contact list changes so the UI recomposes. */
    val contactsVersion = MutableStateFlow(0)

    /** D4: false when the TEE-backed encrypted store is unavailable and pairings live in
     *  memory only (die on process death). Surfaced as a banner, not just a log line. */
    val secureStorageOk = MutableStateFlow(true)

    /**
     * Set when frames are being dropped because the sender's epoch is more than ±2 buckets
     * from ours (K4). Mismatched clocks or a mismatched epochMs partition the mesh totally
     * and silently — a rate-limited debug line was the only trace, so a user just saw "no
     * messages" with no reason. Cleared once traffic verifies again.
     */
    val clockSkewWarning = MutableStateFlow<String?>(null)

    @Volatile
    var outgoingSetAtEpoch: UInt? = null

    // ---- Self-test ---------------------------------------------------------------------

    /**
     * Bumped to ask the service to run a full module self-test.
     *
     * A counter, not a boolean: MutableStateFlow conflates equal values, so a second run
     * request would be silently dropped (the same trap [outgoingRevision] documents).
     */
    val selfTestRequests = MutableStateFlow(0)

    /** True from the moment a run is armed until its report is complete. */
    val selfTestRunning = MutableStateFlow(false)

    /**
     * The report, oldest line FIRST — it is read top to bottom, unlike [debugLog].
     *
     * Kept out of [debugLog] deliberately: a run emits ~80 lines, which would evict most of
     * the 200-line ring buffer and destroy the live trace you may be running the test to
     * explain.
     */
    val selfTestLog = MutableStateFlow<List<String>>(emptyList())

    @Synchronized
    fun logSelfTest(line: String) {
        selfTestLog.value = selfTestLog.value + line
    }

    // Debug log — newest first, capped at 200 lines
    val debugLog = MutableStateFlow<List<String>>(emptyList())

    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US)

    @Synchronized
    fun logDebug(line: String) {
        val ts = timeFmt.format(Date())
        val entry = "$ts $line"
        val current = debugLog.value
        val updated = if (current.size >= 200) listOf(entry) + current.dropLast(1) else listOf(entry) + current
        debugLog.value = updated
    }
}

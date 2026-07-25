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
    val lowConfidenceCell: Boolean = false
)

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

package org.cockroachat.mesh

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
    val corroborations: Int = 0
)

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

    val outgoingTier = MutableStateFlow(SendTier.BROADCAST)

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

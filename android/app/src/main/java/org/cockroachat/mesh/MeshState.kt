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
    val tier: SendTier = SendTier.BROADCAST
)

enum class SendTier { LOCAL, BROADCAST, PRIVATE }

/** A queued private (Tier-3) message: recipient's derived pair key + plaintext + display label. */
data class PrivateSend(
    val pairKey: ByteArray,
    val text: String,
    val label: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PrivateSend) return false
        return pairKey.contentEquals(other.pairKey) && text == other.text && label == other.label
    }
    override fun hashCode(): Int {
        var result = pairKey.contentHashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + label.hashCode()
        return result
    }
}

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

    // A one-shot private message request. The service consumes it (VDL solve + seal + advertise)
    // then resets it to null. Non-null means "a private send is queued or in progress".
    val outgoingPrivate = MutableStateFlow<PrivateSend?>(null)

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

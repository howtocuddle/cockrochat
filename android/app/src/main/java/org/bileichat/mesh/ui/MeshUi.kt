package org.bileichat.mesh.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.launch
import org.bileichat.mesh.Contact
import org.bileichat.mesh.MeshConfig
import org.bileichat.mesh.MeshState
import org.bileichat.mesh.MsgRow
import org.bileichat.mesh.PairStore
import org.bileichat.mesh.SendState
import org.bileichat.mesh.SendTier
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Everything the UI needs from the activity (context-bound operations). */
interface UiController {
    val versionName: String
    fun setMeshRunning(on: Boolean)
    fun applyConfig(cfg: MeshConfig)
    fun exportLog()
    fun clearLog()
    fun exportMeasurements()
    fun copySketch()
    fun compareSketch(peerText: String): String
    fun sendPublic(text: String): String?
    fun sendPrivate(contact: Contact, text: String): String?
    /** Stop re-airing the message currently in flight (LOCAL repeats for up to 30 min). */
    fun stopSending()
    fun contacts(): List<Contact>
    /** Derive a pairing for confirmation. Stores nothing — see [PairStore.preparePairing]. */
    fun preparePairing(label: String, keyOrQr: String): PairStore.PairPrepare
    /** Store a pairing the user confirmed by comparing the words on both screens. */
    fun commitPairing(pending: PairStore.PendingPairing): String?
    /** Pairing screen opened/closed — bounds the lifetime of the per-pairing salt (S5). */
    fun setPairingSessionActive(active: Boolean)
    /** Run every module check against this device. See [SelfTest]. */
    fun runSelfTest()
    /** Share the last self-test report. */
    fun shareSelfTest()
    fun removeContact(label: String)
    fun myPublicHex(): String
    fun myQrPayload(): String
    fun mySaltHex(): String
    fun launchQrScanner(onKey: (String) -> Unit)
    fun panicWipe()
    /** P1: Doze bypass, offered from the drawer. Requesting this during startup used to
     *  background the activity mid-permission-chain and kill the process. */
    fun requestBatteryBypass()
    fun toast(msg: String)
}

private val timeFmt = SimpleDateFormat("HH:mm", Locale.US)

// ---------------------------------------------------------------------------
// Conversations
//
// The mesh has three TIERS, but a messenger has CONVERSATIONS. Local and Broadcast are
// tiers with no counterparty, so they are pinned rows; every paired contact is its own
// private conversation. Opening one sets both the visible feed and the send tier, so the
// thing you are looking at is always the thing you are writing to — which is what makes a
// per-contact composer safe to build without a recipient dropdown.
// ---------------------------------------------------------------------------

private sealed interface Convo {
    data object Local : Convo
    data object Broadcast : Convo
    data class Private(val label: String) : Convo
}

private val Convo.tier: SendTier
    get() = when (this) {
        Convo.Local -> SendTier.LOCAL
        Convo.Broadcast -> SendTier.BROADCAST
        is Convo.Private -> SendTier.PRIVATE
    }

private val Convo.title: String
    get() = when (this) {
        Convo.Local -> "Local"
        Convo.Broadcast -> "Broadcast"
        is Convo.Private -> label
    }

/** Stable string form so the open conversation survives rotation via rememberSaveable. */
private val Convo.key: String
    get() = when (this) {
        Convo.Local -> "local"
        Convo.Broadcast -> "broadcast"
        is Convo.Private -> "p:$label"
    }

private fun convoFromKey(key: String?): Convo? = when {
    key == null -> null
    key == "local" -> Convo.Local
    key == "broadcast" -> Convo.Broadcast
    key.startsWith("p:") -> Convo.Private(key.removePrefix("p:"))
    else -> null
}

/** Does this received/sent row belong in this conversation? */
private fun Convo.owns(row: MsgRow): Boolean = when (this) {
    Convo.Local -> row.tier == SendTier.LOCAL
    Convo.Broadcast -> row.tier == SendTier.BROADCAST
    is Convo.Private -> row.tier == SendTier.PRIVATE &&
        row.contactLabel.equals(label, ignoreCase = true)
}

// ---------------------------------------------------------------------------
// Root
// ---------------------------------------------------------------------------

@Composable
fun MeshUi(controller: UiController) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var openKey by rememberSaveable { mutableStateOf<String?>(null) }
    var showPairing by rememberSaveable { mutableStateOf(false) }
    val open = convoFromKey(openKey)

    // Read-marks live here, above navigation, so leaving a chat and coming back does not
    // resurrect a badge for messages already seen.
    val seenCounts = remember { mutableStateMapOf<String, Int>() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = androidx.compose.ui.graphics.RectangleShape,
                drawerContainerColor = Panel,
                modifier = Modifier.width(330.dp)
            ) {
                DrawerPane(controller)
            }
        }
    ) {
        if (open == null) {
            ConversationListScreen(
                controller = controller,
                seenCounts = seenCounts,
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onOpenConvo = { openKey = it.key },
                onOpenPairing = { showPairing = true }
            )
        } else {
            BackHandler { openKey = null }
            ChatScreen(
                controller = controller,
                convo = open,
                seenCounts = seenCounts,
                onBack = { openKey = null },
                onOpenPairing = { showPairing = true }
            )
        }
    }

    if (showPairing) {
        PairingDialog(controller, onDismiss = { showPairing = false })
    }
}

// ---------------------------------------------------------------------------
// Fuzzy search
// ---------------------------------------------------------------------------

/**
 * Subsequence fuzzy match, returning a score or null when [query] does not match at all.
 *
 * Rewards matches that are contiguous and that start on a word boundary, so typing "gate"
 * ranks "police at the gate" above a text that merely happens to contain g, a, t, e in
 * order. Case-insensitive; an empty query matches nothing (the caller shows the normal list).
 */
private fun fuzzyScore(text: String, query: String): Int? {
    if (query.isBlank()) return null
    val t = text.lowercase()
    val q = query.lowercase().filterNot { it.isWhitespace() }
    if (q.isEmpty()) return null
    var ti = 0
    var score = 0
    var streak = 0
    for (qc in q) {
        var found = -1
        var i = ti
        while (i < t.length) {
            if (t[i] == qc) { found = i; break }
            i++
        }
        if (found < 0) return null
        // Contiguous run, or landing at the start of a word, is a much better signal than
        // an incidental letter far away.
        streak = if (found == ti) streak + 1 else 0
        score += streak * 4
        if (found == 0 || (found > 0 && !t[found - 1].isLetterOrDigit())) score += 6
        score -= (found - ti).coerceAtMost(12)
        ti = found + 1
    }
    // Prefer shorter texts: the same match in a short label is more likely the thing meant.
    return score + (40 - t.length.coerceAtMost(40))
}

/** A collapsible result group, like a section header you can fold away. */
@Composable
private fun CategoryTile(
    title: String,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .background(Panel)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title.uppercase(), style = sansMeta(TextDim), fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text("$count", style = sansMeta(Accent), fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            // Chevron, drawn — rotates by swapping endpoints rather than pulling in an icon.
            Canvas(Modifier.size(12.dp)) {
                val s = 1.8.dp.toPx()
                if (expanded) {
                    drawLine(TextDim, Offset(0f, size.height * 0.68f), Offset(size.width / 2f, size.height * 0.32f), strokeWidth = s)
                    drawLine(TextDim, Offset(size.width / 2f, size.height * 0.32f), Offset(size.width, size.height * 0.68f), strokeWidth = s)
                } else {
                    drawLine(TextDim, Offset(0f, size.height * 0.32f), Offset(size.width / 2f, size.height * 0.68f), strokeWidth = s)
                    drawLine(TextDim, Offset(size.width / 2f, size.height * 0.68f), Offset(size.width, size.height * 0.32f), strokeWidth = s)
                }
            }
        }
        if (expanded) content()
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        placeholder = { Text("Search messages and contacts", style = sansSub(TextDim)) },
        textStyle = sansBody(),
        singleLine = true,
        shape = RoundedCornerShape(22.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Accent,
            unfocusedBorderColor = Hairline,
            cursorColor = Accent
        ),
        trailingIcon = if (query.isNotEmpty()) {
            {
                Canvas(Modifier.size(30.dp).clickable { onQuery("") }.padding(9.dp)) {
                    val s = 1.8.dp.toPx()
                    drawLine(TextDim, Offset(0f, 0f), Offset(size.width, size.height), strokeWidth = s)
                    drawLine(TextDim, Offset(size.width, 0f), Offset(0f, size.height), strokeWidth = s)
                }
            }
        } else null
    )
}

// ---------------------------------------------------------------------------
// Conversation list
// ---------------------------------------------------------------------------

@Composable
private fun ConversationListScreen(
    controller: UiController,
    seenCounts: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Int>,
    onOpenDrawer: () -> Unit,
    onOpenConvo: (Convo) -> Unit,
    onOpenPairing: () -> Unit
) {
    val allMsgs by MeshState.messages.collectAsStateWithLifecycle()
    val stats by MeshState.stats.collectAsStateWithLifecycle()
    val running by MeshState.running.collectAsStateWithLifecycle()
    val storageOk by MeshState.secureStorageOk.collectAsStateWithLifecycle()
    val skewWarning by MeshState.clockSkewWarning.collectAsStateWithLifecycle()
    val contactsVersion by MeshState.contactsVersion.collectAsStateWithLifecycle()
    val contacts = remember(contactsVersion) { controller.contacts() }
    var query by rememberSaveable { mutableStateOf("") }

    val convos = remember(contacts) {
        buildList {
            add(Convo.Broadcast)
            add(Convo.Local)
            contacts.forEach { add(Convo.Private(it.label)) }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .safeDrawingPadding()
    ) {
        ListTopBar(
            neighbors = stats.neighborsThisEpoch,
            running = running,
            onOpenDrawer = onOpenDrawer,
            onTogglePower = { controller.setMeshRunning(it) },
            onNewChat = onOpenPairing
        )

        // D4 / clock skew: both partition the mesh silently, so they stay on the home
        // screen rather than only inside a conversation the user may never open.
        if (!storageOk) {
            Banner("Secure storage unavailable — pairings live in memory only and die with the app")
        }
        skewWarning?.let { Banner(it) }

        SearchField(query) { query = it }

        if (query.isNotBlank()) {
            SearchResults(
                query = query,
                convos = convos,
                allMsgs = allMsgs,
                onOpenConvo = onOpenConvo,
                modifier = Modifier.weight(1f)
            )
            return@Column
        }

        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            items(convos.size, key = { convos[it].key }) { i ->
                val convo = convos[i]
                val rows = allMsgs.filter { convo.owns(it) }
                val last = rows.lastOrNull()
                val received = rows.count { !it.mine }
                val unread = (received - (seenCounts[convo.key] ?: 0)).coerceAtLeast(0)
                ConversationRow(
                    convo = convo,
                    last = last,
                    unread = unread,
                    onClick = { onOpenConvo(convo) }
                )
                HorizontalDivider(color = Hairline, modifier = Modifier.padding(start = 76.dp))
            }
            if (contacts.isEmpty()) {
                item {
                    Text(
                        "No paired contacts yet. Tap + to exchange codes with someone " +
                            "face to face — private messages are end-to-end encrypted and " +
                            "only work with a contact you have paired.",
                        style = sansSub(),
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Fuzzy results, grouped into collapsible tiles.
 *
 * Conversations (which includes contacts) and messages are separate categories because they
 * answer different questions — "where do I write to X" versus "what was said about X".
 */
@Composable
private fun SearchResults(
    query: String,
    convos: List<Convo>,
    allMsgs: List<MsgRow>,
    onOpenConvo: (Convo) -> Unit,
    modifier: Modifier = Modifier
) {
    val convoHits = remember(query, convos) {
        convos.mapNotNull { c -> fuzzyScore(c.title, query)?.let { c to it } }
            .sortedByDescending { it.second }
            .map { it.first }
    }
    val msgHits = remember(query, allMsgs) {
        allMsgs.mapNotNull { m -> fuzzyScore(m.text, query)?.let { m to it } }
            .sortedByDescending { it.second }
            .take(60)
            .map { it.first }
    }
    var convosOpen by rememberSaveable { mutableStateOf(true) }
    var msgsOpen by rememberSaveable { mutableStateOf(true) }

    if (convoHits.isEmpty() && msgHits.isEmpty()) {
        Column(
            modifier.fillMaxSize().padding(horizontal = 40.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Nothing matches “$query”", style = sansSub(), lineHeight = 20.sp)
        }
        return
    }

    LazyColumn(modifier.fillMaxWidth()) {
        item {
            CategoryTile("Conversations", convoHits.size, convosOpen, { convosOpen = !convosOpen }) {
                Column {
                    convoHits.forEach { c ->
                        ConversationRow(
                            convo = c,
                            last = allMsgs.lastOrNull { c.owns(it) },
                            unread = 0,
                            onClick = { onOpenConvo(c) }
                        )
                        HorizontalDivider(color = Hairline, modifier = Modifier.padding(start = 76.dp))
                    }
                }
            }
        }
        item {
            CategoryTile("Messages", msgHits.size, msgsOpen, { msgsOpen = !msgsOpen }) {
                Column {
                    msgHits.forEach { m ->
                        // Route the row back to the conversation it belongs to, so a search
                        // hit is a way in and not a dead end.
                        val owner = convos.firstOrNull { it.owns(m) }
                        MessageHit(m, owner) { owner?.let(onOpenConvo) }
                        HorizontalDivider(color = Hairline, modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageHit(row: MsgRow, owner: Convo?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = owner != null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(row.text, style = sansBody(), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(
                listOfNotNull(
                    owner?.title ?: row.tier.name.lowercase(),
                    if (row.mine) "you" else null
                ).joinToString(" · "),
                style = sansMeta()
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(timeFmt.format(Date(row.tsMs)), style = sansMeta())
    }
}

@Composable
private fun ConversationRow(convo: Convo, last: MsgRow?, unread: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(convo, size = 46.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(convo.title, style = sansRowTitle(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(
                last?.let { (if (it.mine) "You: " else "") + it.text }
                    ?: when (convo) {
                        Convo.Local -> "Room range, about 30 m"
                        Convo.Broadcast -> "Whole mesh, up to 8 hops"
                        is Convo.Private -> "End-to-end encrypted"
                    },
                style = sansSub(if (unread > 0) TextBright else TextDim),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                last?.let { timeFmt.format(Date(it.tsMs)) } ?: "",
                style = sansMeta(if (unread > 0) Accent else TextDim)
            )
            if (unread > 0) {
                Spacer(Modifier.height(5.dp))
                Text(
                    if (unread > 99) "99+" else "$unread",
                    style = sansMeta(Color.White),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Accent, RoundedCornerShape(50))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/** Circle avatar: a glyph for the two mesh-wide tiers, an initial for a paired contact. */
@Composable
private fun Avatar(convo: Convo, size: androidx.compose.ui.unit.Dp) {
    val color = tierColor(convo.tier)
    Box(
        Modifier
            .size(size)
            .background(color.copy(alpha = 0.20f), RoundedCornerShape(50)),
        contentAlignment = Alignment.Center
    ) {
        when (convo) {
            Convo.Broadcast -> Text("📡", fontSize = (size.value * 0.42f).sp)
            Convo.Local -> Text("📍", fontSize = (size.value * 0.42f).sp)
            is Convo.Private -> Text(
                convo.label.take(1).uppercase(),
                style = sansTitle(color),
                fontSize = (size.value * 0.40f).sp
            )
        }
    }
}

@Composable
private fun ListTopBar(
    neighbors: Int,
    running: Boolean,
    onOpenDrawer: () -> Unit,
    onTogglePower: (Boolean) -> Unit,
    onNewChat: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(
            Modifier.size(36.dp).clickable(onClick = onOpenDrawer).padding(9.dp)
        ) {
            val stroke = 2.dp.toPx()
            for (i in 0..2) {
                val y = size.height * (0.22f + 0.28f * i)
                drawLine(TextBright, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke)
            }
        }
        Spacer(Modifier.width(8.dp))
        Text("BileiChat", style = sansTitle())
        Spacer(Modifier.weight(1f))
        DetectorMeter(neighbors, running)
        Spacer(Modifier.width(12.dp))
        // "+" — the only way to reach pairing now that the composer has no contact picker.
        Canvas(Modifier.size(34.dp).clickable(onClick = onNewChat).padding(9.dp)) {
            val stroke = 2.dp.toPx()
            drawLine(TextBright, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), strokeWidth = stroke)
            drawLine(TextBright, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), strokeWidth = stroke)
        }
        Spacer(Modifier.width(4.dp))
        Switch(
            checked = running,
            onCheckedChange = onTogglePower,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Accent.copy(alpha = 0.40f),
                checkedThumbColor = Accent,
                uncheckedTrackColor = PanelRaised,
                uncheckedThumbColor = TextDim,
                uncheckedBorderColor = Hairline
            )
        )
    }
}

@Composable
private fun Banner(text: String) {
    Text(
        "⚠  $text",
        style = sansMeta(TrustAmber),
        lineHeight = 16.sp,
        modifier = Modifier
            .fillMaxWidth()
            .background(TrustAmber.copy(alpha = 0.10f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

// ---------------------------------------------------------------------------
// Chat screen
// ---------------------------------------------------------------------------

@Composable
private fun ChatScreen(
    controller: UiController,
    convo: Convo,
    seenCounts: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Int>,
    onBack: () -> Unit,
    onOpenPairing: () -> Unit
) {
    val allMsgs by MeshState.messages.collectAsStateWithLifecycle()
    val receipt by MeshState.receipt.collectAsStateWithLifecycle()
    val storageOk by MeshState.secureStorageOk.collectAsStateWithLifecycle()
    val skewWarning by MeshState.clockSkewWarning.collectAsStateWithLifecycle()
    val contactsVersion by MeshState.contactsVersion.collectAsStateWithLifecycle()
    val contacts = remember(contactsVersion) { controller.contacts() }

    // Opening a conversation is what selects the tier — for BOTH reading and sending.
    // They are still separate values in MeshState (nothing else may retarget a send), but
    // here they legitimately move together: you are looking at the thread you write to.
    LaunchedEffect(convo) {
        MeshState.viewTier.value = convo.tier
        MeshState.outgoingTier.value = convo.tier
    }

    val msgs = remember(allMsgs, convo) { allMsgs.filter { convo.owns(it) } }
    LaunchedEffect(msgs.size) { seenCounts[convo.key] = msgs.count { !it.mine } }

    // A private conversation whose contact was removed elsewhere must not stay writable.
    val contact = (convo as? Convo.Private)?.let { p ->
        contacts.firstOrNull { it.label.equals(p.label, ignoreCase = true) }
    }

    Column(
        Modifier.fillMaxSize().background(AmoledBlack).safeDrawingPadding()
    ) {
        ChatTopBar(convo, contact, onBack)
        HorizontalDivider(color = Hairline)

        if (!storageOk && convo is Convo.Private) {
            Banner("Secure storage unavailable — this pairing dies with the app")
        }
        skewWarning?.let { Banner(it) }

        MessageList(msgs, convo, Modifier.weight(1f))

        // LOCAL re-originates every epoch until heard back, for up to 30 minutes. That is
        // correct for a danger alert and baffling for an ordinary message — it looked like
        // the app was stuck re-sending forever, with nothing on screen saying so and no way
        // out. Make it visible, and make it stoppable.
        val airing by MeshState.outgoingAiring.collectAsStateWithLifecycle()
        if (airing && convo !is Convo.Private) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(PanelRaised)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val echoed by MeshState.outgoingEchoed.collectAsStateWithLifecycle()
                Text(
                    when {
                        // LOCAL does not stop when it is heard back, it slows down — one
                        // forged echo must not be able to silence a danger alert. Saying
                        // "still re-sending until a phone repeats it back" after the receipt
                        // line already said it WAS repeated back reads as a stuck app.
                        convo == Convo.Local && echoed ->
                            "Heard back — still repeating occasionally in case someone new arrives"
                        convo == Convo.Local ->
                            "Still re-sending until a phone repeats it back (up to 30 min)"
                        else -> "Still re-sending for a few more seconds"
                    },
                    style = sansMeta(TextDim),
                    lineHeight = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "STOP",
                    style = sansMeta(PanicRed),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { controller.stopSending() }.padding(8.dp)
                )
            }
        }

        receipt?.let { note ->
            Text(
                note,
                style = sansMeta(if (note.startsWith("✓")) TierLocal else TrustAmber),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        HorizontalDivider(color = Hairline)
        Composer(controller, convo, contact, onOpenPairing)
    }
}

@Composable
private fun ChatTopBar(convo: Convo, contact: Contact?, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(end = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back chevron, drawn — no icon dependency.
        Canvas(Modifier.size(44.dp).clickable(onClick = onBack).padding(14.dp)) {
            val stroke = 2.dp.toPx()
            val midY = size.height / 2f
            drawLine(TextBright, Offset(size.width, 0f), Offset(0f, midY), strokeWidth = stroke)
            drawLine(TextBright, Offset(0f, midY), Offset(size.width, size.height), strokeWidth = stroke)
        }
        Spacer(Modifier.width(2.dp))
        Avatar(convo, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(convo.title, style = sansRowTitle(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                when (convo) {
                    Convo.Local -> "Room range · repeats until heard back"
                    Convo.Broadcast -> "Whole mesh · up to 8 hops"
                    is Convo.Private ->
                        if (contact == null) "Contact removed"
                        else if (contact.v2) "🔒 End-to-end encrypted · forward secret"
                        else "🔒 End-to-end encrypted · legacy pairing"
                },
                style = sansMeta(if (convo is Convo.Private && contact == null) PanicRed else TextDim),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Messages
// ---------------------------------------------------------------------------

@Composable
private fun MessageList(msgs: List<MsgRow>, convo: Convo, modifier: Modifier = Modifier) {
    if (msgs.isEmpty()) {
        Column(
            modifier.fillMaxSize().padding(horizontal = 40.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Avatar(convo, size = 64.dp)
            Spacer(Modifier.height(14.dp))
            Text(
                when (convo) {
                    Convo.Local -> "Messages from devices in your immediate vicinity appear here."
                    Convo.Broadcast -> "Mesh broadcasts, relayed up to 8 hops, appear here."
                    is Convo.Private -> "Messages you exchange with ${convo.label} appear here. " +
                        "Only the two of you can read them."
                },
                style = sansSub(),
                lineHeight = 20.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        return
    }
    val listState = rememberLazyListState()
    LaunchedEffect(msgs.size) { if (msgs.isNotEmpty()) listState.animateScrollToItem(msgs.size - 1) }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(msgs.size, key = { "${msgs[it].tsMs}-$it" }) { i ->
            Bubble(msgs[i])
        }
    }
}

@Composable
private fun Bubble(row: MsgRow) {
    val align = if (row.mine) Alignment.End else Alignment.Start
    val bg = if (row.mine) BubbleOut else BubbleIn
    val fg = if (row.mine) OnBubbleOut else TextBright
    val metaColor = if (row.mine) OnBubbleOut.copy(alpha = 0.65f) else TextDim

    // /me action text detection
    val isAction = row.text.startsWith("/me ", ignoreCase = true)
    val displayText =
        if (isAction) "✦ ${row.text.removePrefix("/me ").removePrefix("/ME ")}" else row.text

    Column(Modifier.fillMaxWidth(), horizontalAlignment = align) {
        Surface(
            color = bg,
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (row.mine) 16.dp else 5.dp,
                bottomEnd = if (row.mine) 5.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(Modifier.padding(horizontal = 13.dp, vertical = 8.dp)) {
                Text(
                    displayText,
                    style = sansBody(fg),
                    fontStyle = if (isAction) FontStyle.Italic else FontStyle.Normal
                )
                Spacer(Modifier.height(3.dp))
                // Time and provenance ride INSIDE the bubble as quiet secondary text, the
                // way a messenger carries its timestamp — but the provenance itself is kept
                // in full. On an open mesh anyone can inject a frame, so how a message
                // reached you is not decoration; it is the only thing separating a real
                // alert from a forged one.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(timeFmt.format(Date(row.tsMs)), style = sansMeta(metaColor))
                    row.sendState?.let {
                        Spacer(Modifier.width(5.dp))
                        Ticks(it)
                    }
                    if (row.repeats > 1) {
                        // Senders re-air the same alert each epoch; those copies collapse
                        // into this row. The count is itself weak corroboration.
                        Spacer(Modifier.width(6.dp))
                        Text("heard ×${row.repeats}", style = sansMeta(metaColor))
                    }
                    if (!row.mine) {
                        Spacer(Modifier.width(6.dp))
                        TrustLine(row)
                    }
                }
            }
        }
    }
}

/**
 * WhatsApp-style ticks, with meanings this protocol can actually stand behind.
 *
 *   clock — queued; the frame is still being built (a private frame is solving its VDL).
 *   ✓     — on air from this phone.
 *   ✓✓    — we heard our own frame come back, so a peer relayed it.
 *
 * There is no blue-tick equivalent and there will not be one: a read receipt needs an
 * acknowledgement, and an acknowledgement ties a receiver to a sender — exactly what the
 * rotating marks exist to prevent. Two ticks is the ceiling this design permits.
 */
@Composable
private fun Ticks(state: SendState) {
    val color = when (state) {
        SendState.SENDING -> OnBubbleOut.copy(alpha = 0.45f)
        SendState.ON_AIR -> OnBubbleOut.copy(alpha = 0.75f)
        SendState.ECHOED -> Color(0xFF8FD3FF)
    }
    if (state == SendState.SENDING) {
        // Clock face, drawn — matches the "queued, not yet on air" idiom.
        Canvas(Modifier.size(11.dp)) {
            val r = size.minDimension / 2f
            val c = Offset(r, r)
            drawCircle(color, radius = r - 0.5.dp.toPx(), center = c, style = androidx.compose.ui.graphics.drawscope.Stroke(1.2.dp.toPx()))
            drawLine(color, c, Offset(r, r * 0.45f), strokeWidth = 1.2.dp.toPx())
            drawLine(color, c, Offset(r * 1.5f, r), strokeWidth = 1.2.dp.toPx())
        }
        return
    }
    val double = state == SendState.ECHOED
    Canvas(Modifier.size(width = if (double) 16.dp else 11.dp, height = 11.dp)) {
        val stroke = 1.6.dp.toPx()
        fun tick(xOffset: Float) {
            val h = size.height
            drawLine(color, Offset(xOffset, h * 0.55f), Offset(xOffset + h * 0.28f, h * 0.82f), strokeWidth = stroke)
            drawLine(color, Offset(xOffset + h * 0.28f, h * 0.82f), Offset(xOffset + h * 0.82f, h * 0.2f), strokeWidth = stroke)
        }
        tick(0f)
        if (double) tick(size.height * 0.42f)
    }
}

/**
 * Per-message provenance (B10: every label maps to a property that was ACTUALLY checked).
 *   Direct   — arrived at origination TTL: the sender's radio is physically near you.
 *   Relayed  — carried by mesh hops: content verified, sender may be far away.
 *   LOCAL    — PoCP co-presence with our cell verified ("co-present"), or "weak cell" when
 *              our own cell held fewer than MIN_TRUSTWORTHY_CELL marks and the check
 *              therefore proves little.
 *   BROADCAST— witness MAC valid; the number counts DISTINCT claims heard DIRECTLY. A hint,
 *              not a proof — a determined nearby attacker can forge claims (A2).
 *   PRIVATE  — the body AEAD-opened under a paired key: only that contact could write it.
 */
@Composable
private fun TrustLine(row: MsgRow) {
    val path = if (row.direct) "direct" else "relayed"
    // "unverified" and "weak cell" are different claims and must not be collapsed: the first
    // says THEIR overlap with us was a single element (forgeable, and also what an honest
    // just-started phone looks like); the second says OUR cell was too small to judge with.
    // Unverified is the stronger warning, so it wins when both apply.
    val proof = when (row.tier) {
        SendTier.PRIVATE -> null
        SendTier.LOCAL ->
            if (row.unattested) "unverified"
            else if (row.lowConfidenceCell) "weak cell"
            else "co-present"
        SendTier.BROADCAST ->
            if (row.unattested) "unverified"
            else if (row.lowConfidenceCell) "weak cell"
            else if (row.corroborations > 0) "${row.corroborations} nearby claims"
            else "single origin"
    }
    // Amber whenever the check was weak — the colour is the part read at a glance, so it
    // must not stay neutral while the text says the evidence was thin.
    val weak = (row.lowConfidenceCell || row.unattested) && row.tier != SendTier.PRIVATE
    val color = if (weak) TrustAmber else TextDim
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(5.dp)
                .background(if (row.direct && !weak) TierLocal else color, RoundedCornerShape(50))
        )
        Spacer(Modifier.width(4.dp))
        Text(
            listOfNotNull(path, proof).joinToString(" · "),
            style = sansMeta(color)
        )
    }
}

/** 3-bar signal-style meter, kept for the detector readout and the drawer. */
@Composable
fun MeterBars(filled: Int, color: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in 0..2) {
            Box(
                Modifier
                    .width(3.dp)
                    .height((5 + i * 3).dp)
                    .background(if (i < filled) color else Hairline)
            )
        }
    }
}

/** Live proximity readout: how many devices' frames arrive direct (no relay hop). */
@Composable
fun DetectorMeter(count: Int, running: Boolean) {
    val active = running && count > 0
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(13.dp)) {
            val c = Offset(size.width / 2f, size.height / 2f)
            drawCircle(if (active) TierLocal else TextDim, radius = 2.dp.toPx(), center = c)
            val arcColor = if (active) TierLocal.copy(alpha = 0.7f) else Hairline
            drawArc(arcColor, -65f, 130f, false, topLeft = Offset(1f, 1f), size = size)
            drawArc(arcColor.copy(alpha = 0.5f), -45f, 90f, false)
        }
        Spacer(Modifier.width(5.dp))
        Text(
            if (running) "$count nearby" else "radio off",
            style = sansMeta(if (active) TierLocal else TextDim)
        )
    }
}

// ---------------------------------------------------------------------------
// Composer
// ---------------------------------------------------------------------------

@Composable
private fun Composer(
    controller: UiController,
    convo: Convo,
    contact: Contact?,
    onOpenPairing: () -> Unit
) {
    // Draft is per conversation: switching threads must not carry your half-typed message
    // into a different one — least of all from Broadcast into a private chat.
    var text by rememberSaveable(convo.key) { mutableStateOf("") }
    val private = convo is Convo.Private
    val limit = if (private) 47 else 63
    val bytes = text.toByteArray(Charsets.UTF_8).size
    val blocked = private && contact == null

    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
        if (blocked) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "This contact is no longer paired.",
                    style = sansSub(TrustAmber),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "PAIR",
                    style = sansMeta(Accent),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onOpenPairing).padding(8.dp)
                )
            }
            return@Column
        }
        Row(verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message", style = sansBody(TextDim)) },
                textStyle = sansBody(),
                maxLines = 4,
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Hairline,
                    cursorColor = Accent
                ),
                // Only surface the byte budget as it gets close. A frame body is small and
                // fixed, so the limit is real — but a permanent counter on every keystroke
                // is exactly the kind of instrument-panel noise this screen does not need.
                supportingText = if (bytes > limit - 12) {
                    { Text("$bytes/$limit", style = sansMeta(if (bytes > limit) PanicRed else TextDim)) }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(46.dp)
                    .background(if (text.isBlank()) PanelRaised else Accent, RoundedCornerShape(50))
                    .clickable {
                        val body = text.trim()
                        if (body.isEmpty()) return@clickable
                        val err = if (contact != null) {
                            controller.sendPrivate(contact, body)
                        } else {
                            controller.sendPublic(body)
                        }
                        if (err != null) controller.toast(err) else text = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                // Paper-plane-ish arrow, drawn.
                Canvas(Modifier.size(20.dp)) {
                    val c = if (text.isBlank()) TextDim else Color.White
                    val stroke = 2.dp.toPx()
                    drawLine(c, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), strokeWidth = stroke)
                    drawLine(c, Offset(size.width * 0.55f, size.height * 0.15f), Offset(size.width, size.height / 2f), strokeWidth = stroke)
                    drawLine(c, Offset(size.width * 0.55f, size.height * 0.85f), Offset(size.width, size.height / 2f), strokeWidth = stroke)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Pairing dialog
// ---------------------------------------------------------------------------

@Composable
private fun PairingDialog(controller: UiController, onDismiss: () -> Unit) {
    val contactsVersion by MeshState.contactsVersion.collectAsStateWithLifecycle()
    val contacts = remember(contactsVersion) { controller.contacts() }
    var name by rememberSaveable { mutableStateOf("") }
    var peerKey by rememberSaveable { mutableStateOf("") }
    // A3: the QR carries our key + a pairing salt. The salt must stay STABLE for as long
    // as this QR is displayed — a peer pairing with the scanned code derives the chain
    // seed from exactly these two values, so rotating mid-session would break the pairing.
    val myKey = remember { controller.myPublicHex() }
    // S1b: a pairing awaiting the user's word comparison. Nothing is stored until they agree.
    var pending by remember { mutableStateOf<PairStore.PendingPairing?>(null) }
    // S5: the salt exists only while this screen is open.
    //
    // Started in a `remember` rather than in the DisposableEffect below, and the ordering is
    // load-bearing: composition runs `remember` blocks top to bottom, but defers effect
    // bodies until the whole composition has finished. Beginning the session in the effect
    // would rotate the salt AFTER the QR had already been encoded from the previous one, so
    // the code on screen would not match the salt used to finish the pairing — and that
    // mismatch is silent and permanent, because the two phones then derive different chain
    // seeds and no private message between them ever opens.
    remember { controller.setPairingSessionActive(true) }
    DisposableEffect(Unit) {
        onDispose { controller.setPairingSessionActive(false) }
    }
    val qr = remember {
        val size = 640
        val matrix = QRCodeWriter().encode(
            controller.myQrPayload(), BarcodeFormat.QR_CODE, size, size,
            mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M)
        )
        Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).also { bmp ->
            for (y in 0 until size) for (x in 0 until size) {
                bmp.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }.asImageBitmap()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        title = { Text("New private chat", style = sansTitle()) },
        text = {
            Column {
                Text(
                    "Show your code and scan theirs, face to face. Both sides pair from the " +
                        "same two codes. The pairing salt exists only while this screen is " +
                        "open and is erased when you close it, so past messages stay " +
                        "unrecoverable even if a phone is later seized. Keys never touch a " +
                        "server.",
                    style = sansSub(), lineHeight = 19.sp
                )
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Image(qr, contentDescription = "Your pairing QR", modifier = Modifier.size(170.dp).padding(8.dp))
                }
                Spacer(Modifier.height(10.dp))
                SelectionContainer {
                    Text(myKey, style = monoMicro(TextBright), lineHeight = 14.sp)
                }
                // The salt is deliberately NOT displayed. It was shown here as selectable
                // text, which made the one value that keeps a seized phone from recomputing
                // past chain seeds trivially copyable off the screen. Nothing about pairing
                // requires a human to read it — it travels inside the QR.
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = Hairline)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Contact name", style = sansSub()) },
                    textStyle = sansBody(), singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = darkFieldColors(), modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = peerKey, onValueChange = { peerKey = it },
                    label = { Text("Their pairing code", style = sansSub()) },
                    textStyle = sansBody(),
                    shape = RoundedCornerShape(10.dp),
                    colors = darkFieldColors(), modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = { controller.launchQrScanner { peerKey = it } }) {
                    Text("Scan their QR", style = sansSub(Accent))
                }
                if (contacts.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider(color = Hairline)
                    Spacer(Modifier.height(8.dp))
                    Text("Paired contacts", style = sansMeta())
                    contacts.forEach { c ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔒 ${c.label}", style = sansBody(), modifier = Modifier.weight(1f))
                            Text(
                                if (c.v2) "forward secret" else "legacy",
                                style = sansMeta(if (c.v2) TierPrivate else TrustAmber),
                                modifier = Modifier.padding(end = 10.dp)
                            )
                            Text(
                                "Remove", style = sansMeta(PanicRed),
                                modifier = Modifier.clickable { controller.removeContact(c.label) }.padding(4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when (val r = controller.preparePairing(name, peerKey)) {
                    is PairStore.PairPrepare.Error -> controller.toast(r.message)
                    // Nothing is stored yet — the words have to match on both screens first.
                    is PairStore.PairPrepare.Confirm -> pending = r.pending
                }
            }) { Text("Continue", style = sansRowTitle(Accent)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", style = sansSub()) }
        }
    )

    pending?.let { p ->
        PairingConfirmDialog(
            pending = p,
            onCancel = { pending = null },
            onConfirm = {
                val err = controller.commitPairing(p)
                if (err != null) controller.toast(err)
                pending = null
                name = ""; peerKey = ""
            }
        )
    }
}

/**
 * The step that makes QR pairing mean anything.
 *
 * Scanning a code proves only that a code was scanned. Someone sitting between two phones can
 * hand each of them its own code, ending up with a separate shared secret on each side and
 * the ability to read everything — and because both sides "paired successfully", nothing
 * looks wrong. Comparing the words closes that: a relay holds two different secrets, so the
 * two screens cannot agree.
 */
@Composable
private fun PairingConfirmDialog(
    pending: PairStore.PendingPairing,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = Panel,
        title = { Text("Check these words match", style = sansTitle()) },
        text = {
            Column {
                Text(
                    "Both phones must show the same four words. If they differ, someone is " +
                        "relaying between you — stop and do not save this contact.",
                    style = sansSub(), lineHeight = 19.sp
                )
                Spacer(Modifier.height(14.dp))
                Surface(
                    color = PanelRaised,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        pending.sasWords.joinToString("  ") { it.uppercase() },
                        style = sansTitle(Accent),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    )
                }
                if (pending.replacesExistingKey) {
                    Spacer(Modifier.height(12.dp))
                    // Deliberately not phrased as an attack: after per-session salts, an
                    // ordinary re-pair with the same person also lands here.
                    Text(
                        "⚠ You already have a contact with this name, and its key is " +
                            "different. Saving replaces it, and messages will go to whoever " +
                            "holds the new key. Confirm in person that this is them.",
                        style = sansSub(TrustAmber), lineHeight = 19.sp
                    )
                }
                if (pending.legacy) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "⚠ This code is an old format with NO forward secrecy. If this phone " +
                            "is later seized, past messages with this contact can be read. " +
                            "Ask them to update and pair again.",
                        style = sansSub(TrustAmber), lineHeight = 19.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("They match — save", style = sansRowTitle(Accent))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel", style = sansSub()) }
        }
    )
}

@Composable
fun darkFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Accent,
    unfocusedBorderColor = Hairline,
    focusedTextColor = TextBright,
    unfocusedTextColor = TextBright,
    cursorColor = Accent,
    focusedLabelColor = Accent,
    unfocusedLabelColor = TextDim
)

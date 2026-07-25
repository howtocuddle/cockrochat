package org.bileichat.mesh.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
    fun contacts(): List<Contact>
    fun addContact(label: String, keyOrQr: String): String?
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

@Composable
fun MeshUi(controller: UiController) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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
        ChatPane(controller, onOpenDrawer = { scope.launch { drawerState.open() } })
    }
}

// ---------------------------------------------------------------------------
// Chat pane
// ---------------------------------------------------------------------------

@Composable
fun ChatPane(controller: UiController, onOpenDrawer: () -> Unit) {
    val allMsgs by MeshState.messages.collectAsStateWithLifecycle()
    val stats by MeshState.stats.collectAsStateWithLifecycle()
    val running by MeshState.running.collectAsStateWithLifecycle()
    val receipt by MeshState.receipt.collectAsStateWithLifecycle()
    val storageOk by MeshState.secureStorageOk.collectAsStateWithLifecycle()
    val skewWarning by MeshState.clockSkewWarning.collectAsStateWithLifecycle()
    // The tab selects which feed is VISIBLE; it no longer decides how the next message is
    // sent. Those were the same value, so opening the LOCAL tab to read a local alert
    // silently re-originated any in-flight broadcast at a different TTL and msgType.
    val tier by MeshState.viewTier.collectAsStateWithLifecycle()
    var showPairing by rememberSaveable { mutableStateOf(false) }

    // Private contact filter: null = ALL contacts
    var privateFilter by rememberSaveable { mutableStateOf<String?>(null) }

    // Filter messages by the currently selected tier tab
    val msgs = remember(allMsgs, tier, privateFilter) {
        allMsgs.filter { row ->
            row.tier == tier && (
                tier != SendTier.PRIVATE || privateFilter == null ||
                row.contactLabel.equals(privateFilter, ignoreCase = true)
            )
        }
    }

    // Unread counts per tab. A received LOCAL message used to land in state and be
    // completely invisible while the user sat on BROADCAST, with nothing to indicate it
    // had arrived — for a tier meant to carry immediate danger alerts.
    val seenCounts = remember { mutableStateMapOf<SendTier, Int>() }
    val unread = remember(allMsgs, tier) {
        SendTier.entries.associateWith { t ->
            val total = allMsgs.count { it.tier == t && !it.mine }
            (total - (seenCounts[t] ?: 0)).coerceAtLeast(0)
        }
    }
    LaunchedEffect(allMsgs, tier) {
        // Everything in the visible tab counts as read.
        seenCounts[tier] = allMsgs.count { it.tier == tier && !it.mine }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(AmoledBlack)
            // targetSdk 35 means Android 15 forces edge-to-edge and the app cannot opt out,
            // so the window now extends under the status bar, the navigation bar, AND the
            // display cutout. statusBarsPadding() only cleared the first of those: on phones
            // with a punch-hole or notch camera the cutout is taller than the status bar, so
            // it sat on top of the message text. The navigation bar overlapped the composer
            // for the same reason. safeDrawing is systemBars + displayCutout + IME, which
            // covers all three (and lifts the composer above the keyboard). Applied AFTER
            // background() so black still paints edge to edge behind the system bars.
            .safeDrawingPadding()
    ) {
        TopBar(
            neighbors = stats.neighborsThisEpoch,
            running = running,
            onOpenDrawer = onOpenDrawer,
            onTogglePower = { controller.setMeshRunning(it) }
        )
        HorizontalDivider(color = Hairline)

        // Tier tab bar — switches which feed is visible
        TierTabBar(tier, unread)
        HorizontalDivider(color = Hairline)

        // Private contact filter chips (only visible in PRIVATE tab)
        if (tier == SendTier.PRIVATE) {
            val contactsVersion by MeshState.contactsVersion.collectAsStateWithLifecycle()
            val contacts = remember(contactsVersion) { controller.contacts() }
            ContactFilterBar(
                contacts = contacts,
                selected = privateFilter,
                onSelect = { privateFilter = it }
            )
            HorizontalDivider(color = Hairline)
        }

        // D4: TEE-backed encrypted store unavailable — pairings are memory-only and die
        // with the process. The user must HEAR this, not find it in a log after the fact.
        if (!storageOk) {
            Text(
                "⚠ SECURE STORAGE UNAVAILABLE — pairings live in memory only and die with the app",
                style = monoMicro(TrustAmber),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TrustAmber.copy(alpha = 0.10f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )
            HorizontalDivider(color = Hairline)
        }

        // Clock skew silently partitions the mesh: frames outside the ±2-epoch freshness
        // window are dropped before anything else, so a skewed peer is simply invisible.
        skewWarning?.let { warn ->
            Text(
                warn,
                style = monoMicro(TrustAmber),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TrustAmber.copy(alpha = 0.10f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )
            HorizontalDivider(color = Hairline)
        }

        MessageList(msgs, tier, Modifier.weight(1f))

        receipt?.let { note ->
            HorizontalDivider(color = Hairline)
            Text(
                note,
                style = monoMicro(if (note.startsWith("✓")) TierLocal else TrustAmber),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        HorizontalDivider(color = Hairline)
        Composer(controller, onOpenPairing = { showPairing = true })
    }

    if (showPairing) {
        PairingDialog(controller, onDismiss = { showPairing = false })
    }
}

// ---------------------------------------------------------------------------
// Tier Tab Bar — switches which feed is visible
// ---------------------------------------------------------------------------

@Composable
private fun TierTabBar(activeTier: SendTier, unread: Map<SendTier, Int> = emptyMap()) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Panel)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SendTier.entries.forEach { t ->
            val active = t == activeTier
            val c = tierColor(t)
            val pending = unread[t] ?: 0
            Box(
                Modifier
                    .weight(1f)
                    .background(
                        if (active) c.copy(alpha = 0.18f) else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .then(
                        if (active) Modifier.border(1.dp, c.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        else Modifier
                    )
                    .clickable {
                        // Selecting a tab switches the visible feed AND the send tier, which
                        // is what a user expects from tapping it. The difference from before
                        // is that the two are now separate values, so nothing else in the app
                        // can change how a message is sent just by changing what is shown.
                        MeshState.viewTier.value = t
                        MeshState.outgoingTier.value = t
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        t.name,
                        style = monoLabel(if (active) c else TextDim),
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                    )
                    if (!active && pending > 0) {
                        Text(
                            if (pending > 9) "9+" else "$pending",
                            style = monoMicro(AmoledBlack),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(c, RoundedCornerShape(50))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Private Contact Filter Chips
// ---------------------------------------------------------------------------

@Composable
private fun ContactFilterBar(
    contacts: List<Contact>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Panel)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // "ALL" chip
        FilterChip(label = "ALL", active = selected == null, color = TierPrivate) {
            onSelect(null)
        }
        // Per-contact chips
        contacts.forEach { c ->
            FilterChip(
                label = c.label.uppercase(),
                active = selected.equals(c.label, ignoreCase = true),
                color = TierPrivate
            ) {
                onSelect(c.label)
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, active: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .background(
                if (active) color.copy(alpha = 0.18f) else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .border(
                1.dp,
                if (active) color.copy(alpha = 0.6f) else Hairline,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            style = monoMicro(if (active) color else TextDim),
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// ---------------------------------------------------------------------------
// Top bar
// ---------------------------------------------------------------------------

@Composable
private fun TopBar(neighbors: Int, running: Boolean, onOpenDrawer: () -> Unit, onTogglePower: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(58.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hamburger — drawn, no icon dependency.
        Canvas(
            Modifier
                .size(36.dp)
                .clickable(onClick = onOpenDrawer)
                .padding(8.dp)
        ) {
            val w = size.width
            val stroke = 2.dp.toPx()
            for (i in 0..2) {
                val y = size.height * (0.22f + 0.28f * i)
                drawLine(TextBright, Offset(0f, y), Offset(w, y), strokeWidth = stroke)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text("BILEICHAT", style = monoLabel())
            Text("BLE MESH · NO SERVERS", style = monoMicro())
        }
        Spacer(Modifier.weight(1f))
        DetectorMeter(neighbors, running)
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = running,
            onCheckedChange = onTogglePower,
            colors = SwitchDefaults.colors(
                checkedTrackColor = TierLocal.copy(alpha = 0.35f),
                checkedThumbColor = TierLocal,
                uncheckedTrackColor = PanelRaised,
                uncheckedThumbColor = TextDim,
                uncheckedBorderColor = Hairline
            )
        )
    }
}

/** Live proximity readout: how many devices' frames arrive direct (no relay hop). */
@Composable
fun DetectorMeter(count: Int, running: Boolean) {
    val active = running && count > 0
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(14.dp)) {
                val c = Offset(size.width / 2f, size.height / 2f)
                drawCircle(if (active) TierLocal else TextDim, radius = 2.dp.toPx(), center = c)
                val arcColor = if (active) TierLocal.copy(alpha = 0.7f) else Hairline
                drawArc(arcColor, -65f, 130f, false, topLeft = Offset(1f, 1f), size = size)
                drawArc(arcColor.copy(alpha = 0.5f), -45f, 90f, false)
            }
            Spacer(Modifier.width(5.dp))
            Text(
                if (running) "$count" else "—",
                style = monoBody(if (active) TierLocal else TextDim),
                fontWeight = FontWeight.Bold
            )
        }
        Text(if (running) "NEARBY" else "RADIO OFF", style = monoMicro())
    }
}

/** 3-bar signal-style meter used by the detector and the per-message trust badge. */
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

// ---------------------------------------------------------------------------
// Messages
// ---------------------------------------------------------------------------

@Composable
private fun MessageList(msgs: List<MsgRow>, tier: SendTier, modifier: Modifier = Modifier) {
    if (msgs.isEmpty()) {
        val c = tierColor(tier)
        Column(
            modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("NO ${tier.name} TRAFFIC YET", style = monoLabel(TextDim))
            Spacer(Modifier.height(6.dp))
            Text(
                when (tier) {
                    SendTier.LOCAL -> "Messages from devices in your\nimmediate vicinity appear here."
                    SendTier.BROADCAST -> "Mesh broadcasts relayed up to\n8 hops appear here."
                    SendTier.PRIVATE -> "End-to-end encrypted messages\nfrom paired contacts appear here."
                },
                style = monoMicro(),
                lineHeight = 16.sp
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
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(msgs.size, key = { "${msgs[it].tsMs}-${it}" }) { i ->
            Bubble(msgs[i])
        }
    }
}

@Composable
private fun Bubble(row: MsgRow) {
    val color = tierColor(row.tier)
    val align = if (row.mine) Alignment.End else Alignment.Start
    val tierName = when (row.tier) {
        SendTier.LOCAL -> "LOCAL"
        SendTier.BROADCAST -> "BROADCAST"
        SendTier.PRIVATE -> "PRIVATE"
    }

    // /me action text detection
    val isAction = row.text.startsWith("/me ", ignoreCase = true)
    val displayText = if (isAction) "✦ ${row.text.removePrefix("/me ").removePrefix("/ME ")}" else row.text

    Column(Modifier.fillMaxWidth(), horizontalAlignment = align) {
        Surface(
            color = if (row.mine) color.copy(alpha = 0.16f) else Panel,
            shape = RoundedCornerShape(
                topStart = 14.dp, topEnd = 14.dp,
                bottomStart = if (row.mine) 14.dp else 3.dp,
                bottomEnd = if (row.mine) 3.dp else 14.dp
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp, if (row.mine) color.copy(alpha = 0.55f) else Hairline
            ),
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (!row.mine) {
                    Text(
                        row.markHexPrefix + (row.rssi?.let { " · ${it}dBm" } ?: ""),
                        style = monoMicro(color)
                    )
                    Spacer(Modifier.height(3.dp))
                }
                Text(
                    displayText,
                    color = if (isAction) color else TextBright,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontStyle = if (isAction) FontStyle.Italic else FontStyle.Normal
                )
            }
        }
        // Footer: time, tier, and (for received frames) the trust meter.
        Row(
            Modifier.padding(top = 3.dp, start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(timeFmt.format(Date(row.tsMs)), style = monoMicro())
            Spacer(Modifier.width(8.dp))
            Text(tierName, style = monoMicro(color))
            if (!row.mine) {
                Spacer(Modifier.width(8.dp))
                TrustMeter(row, color)
            }
        }
    }
}

/**
 * Per-message trust badge (B10: every label maps to a property that was ACTUALLY checked).
 *   ▮▮▮ DIRECT  — arrived at origination TTL: the sender's radio is physically near you.
 *   ▮▮  RELAYED — carried by mesh hops: content verified, sender may be far away.
 *   LOCAL      — PoCP co-presence with our cell was verified ("CO-PRESENT"), or
 *                "LOW-CONFIDENCE CELL" when our own cell held fewer than
 *                MIN_TRUSTWORTHY_CELL marks and the check therefore proves little.
 *   BROADCAST  — witness MAC is valid; the number counts DISTINCT claims heard DIRECTLY
 *                (origination TTL). It is a hint, not a proof — a determined nearby
 *                attacker can forge claims (A2).
 *   PRIVATE    — the body AEAD-opened under a paired key: only that contact could write it.
 */
@Composable
private fun TrustMeter(row: MsgRow, color: Color) {
    val bars = if (row.direct) 3 else 2
    val barColor = if (row.direct) color else TrustAmber
    val path = if (row.direct) "DIRECT" else "RELAYED"
    val proof = when (row.tier) {
        SendTier.PRIVATE -> "E2E"
        // Our own cell was too small for the co-presence check to mean anything: at 3 or
        // fewer marks a remote attacker can grind a passing witness without ever having
        // been here. Say so rather than printing CO-PRESENT.
        SendTier.LOCAL -> if (row.lowConfidenceCell) "LOW-CONFIDENCE CELL" else "CO-PRESENT"
        SendTier.BROADCAST ->
            if (row.lowConfidenceCell) "LOW-CONFIDENCE CELL"
            else if (row.corroborations > 0) "${row.corroborations} NEARBY CLAIMS"
            else "UNCORROBORATED ORIGIN"
    }
    // Amber for a low-confidence cell even on a direct frame — the badge colour is the part
    // read at a glance, so it must not stay green while the text says the check was weak.
    val proofColor = if (row.lowConfidenceCell && row.tier != SendTier.PRIVATE) TrustAmber else barColor
    Row(verticalAlignment = Alignment.CenterVertically) {
        MeterBars(bars, barColor)
        Spacer(Modifier.width(5.dp))
        Text("$path · $proof", style = monoMicro(proofColor))
    }
}

// ---------------------------------------------------------------------------
// Composer
// ---------------------------------------------------------------------------

@Composable
private fun Composer(controller: UiController, onOpenPairing: () -> Unit) {
    val tier by MeshState.outgoingTier.collectAsStateWithLifecycle()
    val contactsVersion by MeshState.contactsVersion.collectAsStateWithLifecycle()
    val contacts = remember(contactsVersion) { controller.contacts() }
    var text by rememberSaveable { mutableStateOf("") }
    var selectedLabel by rememberSaveable { mutableStateOf<String?>(null) }
    // Fail CLOSED when an explicit selection no longer resolves. This used to fall back to
    // contacts.first(), so if the chosen contact was removed (or the list reordered) between
    // composition and SEND, an end-to-end message silently went to a DIFFERENT paired
    // contact. Only the no-selection-yet case may default.
    val selected = if (selectedLabel == null) {
        contacts.firstOrNull()
    } else {
        contacts.firstOrNull { it.label == selectedLabel }
    }

    val limit = if (tier == SendTier.PRIVATE) 47 else 63
    val bytes = text.toByteArray(Charsets.UTF_8).size

    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        // Tier hint line
        Text(
            when (tier) {
                SendTier.LOCAL -> "Room range (~30 m). Repeats until heard back."
                SendTier.BROADCAST -> "Whole mesh, up to 8 hops. Repeats for 3 epochs."
                SendTier.PRIVATE -> "End-to-end encrypted to one paired contact."
            },
            style = monoMicro(),
            modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
        )

        if (tier == SendTier.PRIVATE) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                var menuOpen by remember { mutableStateOf(false) }
                Box {
                    Text(
                        selected?.label?.uppercase() ?: "NO CONTACT",
                        style = monoLabel(if (selected != null) TierPrivate else TrustAmber),
                        modifier = Modifier
                            .border(1.dp, if (selected != null) TierPrivate.copy(alpha = 0.5f) else Hairline, RoundedCornerShape(6.dp))
                            .clickable { menuOpen = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        contacts.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.label, color = TextBright) },
                                onClick = { selectedLabel = c.label; menuOpen = false }
                            )
                        }
                        if (contacts.isEmpty()) {
                            DropdownMenuItem(text = { Text("(pair first)", color = TextDim) }, onClick = { menuOpen = false })
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text("PAIR", style = monoMicro(TierPrivate), modifier = Modifier.clickable(onClick = onOpenPairing).padding(6.dp))
            }
            Spacer(Modifier.height(4.dp))
        }

        Row(verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message…", color = TextDim, fontSize = 14.sp) },
                textStyle = androidx.compose.ui.text.TextStyle(color = TextBright, fontSize = 15.sp),
                maxLines = 3,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = tierColor(tier),
                    unfocusedBorderColor = Hairline,
                    cursorColor = tierColor(tier)
                ),
                supportingText = {
                    Text(
                        "$bytes/$limit",
                        style = monoMicro(if (bytes > limit) PanicRed else TextDim)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .border(1.dp, tierColor(tier), RoundedCornerShape(10.dp))
                    .clickable {
                        val body = text.trim()
                        if (body.isEmpty()) return@clickable
                        val err = if (tier == SendTier.PRIVATE) {
                            val c = selected
                            when {
                                c != null -> controller.sendPrivate(c, body)
                                // Stale selection: refuse rather than silently re-target.
                                selectedLabel != null -> "That contact no longer exists — pick another"
                                else -> "Pair with a contact first"
                            }
                        } else {
                            controller.sendPublic(body)
                        }
                        if (err != null) controller.toast(err) else text = ""
                    }
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("SEND", style = monoLabel(tierColor(tier)))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Pairing dialog (private tier)
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
    val mySalt = remember { controller.mySaltHex() }
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
        title = { Text("PAIRING · PRIVATE TIER", style = monoLabel(TierPrivate)) },
        text = {
            Column {
                Text(
                    "Share YOUR code out-of-band and add theirs — both sides pair from the " +
                        "SAME two codes. The QR carries your key + a salt; salts live only in " +
                        "memory and are gone once the app closes, so past messages stay " +
                        "unrecoverable even if a phone is later seized (forward secrecy). " +
                        "Keys never touch a server.",
                    style = monoMicro(), lineHeight = 15.sp
                )
                Spacer(Modifier.height(10.dp))
                Surface(color = Color.White, shape = RoundedCornerShape(8.dp), modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Image(qr, contentDescription = "Your pairing QR", modifier = Modifier.size(170.dp).padding(8.dp))
                }
                Spacer(Modifier.height(8.dp))
                SelectionContainer {
                    Text(myKey, style = monoMicro(TextBright), lineHeight = 14.sp)
                }
                Spacer(Modifier.height(4.dp))
                SelectionContainer {
                    Text("SALT $mySalt", style = monoMicro(TierPrivate), lineHeight = 14.sp)
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Hairline)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Contact name", style = monoMicro()) },
                    textStyle = monoBody(), singleLine = true,
                    colors = darkFieldColors(), modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = peerKey, onValueChange = { peerKey = it },
                    label = { Text("Their pairing code (QR or hex)", style = monoMicro()) },
                    textStyle = monoBody(),
                    colors = darkFieldColors(), modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = { controller.launchQrScanner { peerKey = it } }) {
                    Text("SCAN THEIR QR", style = monoMicro(TierPrivate))
                }
                if (contacts.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider(color = Hairline)
                    Spacer(Modifier.height(6.dp))
                    Text("PAIRED CONTACTS", style = monoMicro())
                    contacts.forEach { c ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🔒 ${c.label}", style = monoBody(), modifier = Modifier.weight(1f))
                            Text(
                                if (c.v2) "FS" else "LEGACY",
                                style = monoMicro(if (c.v2) TierPrivate else TrustAmber),
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                "REMOVE", style = monoMicro(PanicRed),
                                modifier = Modifier.clickable { controller.removeContact(c.label) }.padding(4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val err = controller.addContact(name, peerKey)
                if (err == null) {
                    name = ""; peerKey = ""
                } else {
                    controller.toast(err)
                }
            }) { Text("ADD CONTACT", style = monoLabel(TierPrivate)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CLOSE", style = monoMicro()) }
        }
    )
}

@Composable
fun darkFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TierPrivate,
    unfocusedBorderColor = Hairline,
    focusedTextColor = TextBright,
    unfocusedTextColor = TextBright,
    cursorColor = TierPrivate,
    focusedLabelColor = TierPrivate,
    unfocusedLabelColor = TextDim
)

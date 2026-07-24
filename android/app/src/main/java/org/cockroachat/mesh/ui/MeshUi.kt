package org.cockroachat.mesh.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import org.cockroachat.mesh.Contact
import org.cockroachat.mesh.MeshConfig
import org.cockroachat.mesh.MeshState
import org.cockroachat.mesh.MsgRow
import org.cockroachat.mesh.SendTier
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
    fun addContact(label: String, keyOrQr: String): Boolean
    fun removeContact(label: String)
    fun myPublicHex(): String
    fun myQrPayload(): String
    fun launchQrScanner(onKey: (String) -> Unit)
    fun panicWipe()
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
    val msgs by MeshState.messages.collectAsStateWithLifecycle()
    val stats by MeshState.stats.collectAsStateWithLifecycle()
    val running by MeshState.running.collectAsStateWithLifecycle()
    val receipt by MeshState.receipt.collectAsStateWithLifecycle()
    var showPairing by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(AmoledBlack)
    ) {
        TopBar(
            neighbors = stats.neighborsThisEpoch,
            running = running,
            onOpenDrawer = onOpenDrawer,
            onTogglePower = { controller.setMeshRunning(it) }
        )
        HorizontalDivider(color = Hairline)

        MessageList(msgs, Modifier.weight(1f))

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
            Text("COCKROACHAT", style = monoLabel())
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
private fun MessageList(msgs: List<MsgRow>, modifier: Modifier = Modifier) {
    if (msgs.isEmpty()) {
        Column(
            modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("NO TRAFFIC YET", style = monoLabel(TextDim))
            Spacer(Modifier.height(6.dp))
            Text(
                "Turn the radio on. Frames from nearby\ndevices appear here.",
                style = monoMicro(),
                lineHeight = 16.sp
            )
        }
        return
    }
    val listState = rememberLazyListState()
    LaunchedEffect(msgs.size) { listState.animateScrollToItem(msgs.size - 1) }
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
                Text(row.text, color = TextBright, fontSize = 15.sp, lineHeight = 20.sp)
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
 * Per-message trust badge. Every displayed frame already passed self-verify (PoW) and the
 * PoCP co-presence gate; what varies is HOW it arrived:
 *   ▮▮▮ DIRECT  — straight off the sender's radio: the sender is physically near you.
 *   ▮▮  RELAYED — carried by mesh hops: content verified, sender may be far away.
 * Private frames add E2E: only the paired contact could have produced readable text.
 */
@Composable
private fun TrustMeter(row: MsgRow, color: Color) {
    val bars = if (row.direct) 3 else 2
    val barColor = if (row.direct) color else TrustAmber
    val path = if (row.direct) "DIRECT" else "RELAYED"
    val proof = when (row.tier) {
        SendTier.PRIVATE -> "E2E"
        SendTier.BROADCAST -> "CORROBORATED"
        SendTier.LOCAL -> "VERIFIED"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        MeterBars(bars, barColor)
        Spacer(Modifier.width(5.dp))
        Text("$path · $proof", style = monoMicro(barColor))
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
    val selected = contacts.firstOrNull { it.label == selectedLabel } ?: contacts.firstOrNull()

    val limit = if (tier == SendTier.PRIVATE) 47 else 63
    val bytes = text.toByteArray(Charsets.UTF_8).size

    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        // Tier selector — segmented, color-coded.
        Row(
            Modifier
                .fillMaxWidth()
                .border(1.dp, Hairline, RoundedCornerShape(8.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            SendTier.entries.forEach { t ->
                val active = t == tier
                val c = tierColor(t)
                Box(
                    Modifier
                        .weight(1f)
                        .background(if (active) c.copy(alpha = 0.18f) else Color.Transparent, RoundedCornerShape(6.dp))
                        .clickable { MeshState.outgoingTier.value = t }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(t.name, style = monoMicro(if (active) c else TextDim), fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            when (tier) {
                SendTier.LOCAL -> "Room range (~30 m). Repeats until a peer confirms receipt."
                SendTier.BROADCAST -> "Whole mesh, up to 8 hops. Repeats for 3 epochs."
                SendTier.PRIVATE -> "End-to-end encrypted to one paired contact."
            },
            style = monoMicro(),
            modifier = Modifier.padding(start = 2.dp)
        )

        if (tier == SendTier.PRIVATE) {
            Spacer(Modifier.height(6.dp))
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
        }

        Spacer(Modifier.height(6.dp))
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
                            if (c == null) "Pair with a contact first" else controller.sendPrivate(c, body)
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
    val myKey = remember { controller.myPublicHex() }
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
                    "Share YOUR key out-of-band (QR photo, paper, another channel). " +
                        "Add their key to pair. Keys never touch a server.",
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
                    label = { Text("Their pairing key (hex)", style = monoMicro()) },
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
                if (controller.addContact(name, peerKey)) {
                    name = ""; peerKey = ""
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

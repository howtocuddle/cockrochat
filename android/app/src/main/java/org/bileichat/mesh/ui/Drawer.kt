package org.bileichat.mesh.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bileichat.mesh.MeshConfig
import org.bileichat.mesh.MeshState
import org.bileichat.mesh.SendTier

/**
 * Left drawer: GUIDE (when to use which tier + trust legend), DETECTOR (nearby devices),
 * SETTINGS (every tunable parameter), DIAGNOSTICS (the old rig toolset), PANIC.
 */
@Composable
fun DrawerPane(controller: UiController) {
    var guideOpen by rememberSaveable { mutableStateOf(false) }
    var detectorOpen by rememberSaveable { mutableStateOf(false) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var diagOpen by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            // Same edge-to-edge fix as ChatPane: clear the cutout camera and the navigation
            // bar, not just the status bar. The drawer has text fields, so IME padding
            // (included in safeDrawing) keeps them above the keyboard too.
            .safeDrawingPadding()
            .padding(bottom = 24.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Text("BILEICHAT", style = monoLabel(), modifier = Modifier.padding(horizontal = 16.dp))
        Text("CONTROL PANEL", style = monoMicro(), modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(10.dp))

        Section("GUIDE — WHICH TIER, WHEN", guideOpen) { guideOpen = !guideOpen }
        if (guideOpen) GuideSection()

        Section("DETECTOR — WHO IS NEAR YOU", detectorOpen) { detectorOpen = !detectorOpen }
        if (detectorOpen) DetectorSection()

        Section("SETTINGS — ALL PARAMETERS", settingsOpen) { settingsOpen = !settingsOpen }
        if (settingsOpen) SettingsSection(controller)

        Section("DIAGNOSTICS — RIG TOOLS", diagOpen) { diagOpen = !diagOpen }
        if (diagOpen) DiagnosticsSection(controller)

        Spacer(Modifier.height(14.dp))
        PanicButton(controller)
        Spacer(Modifier.height(14.dp))
        Text(
            "v${controller.versionName} · AMOLED INDUSTRIAL",
            style = monoMicro(),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun Section(title: String, open: Boolean, onToggle: () -> Unit) {
    Column {
        HorizontalDivider(color = Hairline)
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = monoLabel(), modifier = Modifier.weight(1f))
            Text(if (open) "−" else "+", style = monoLabel(TextDim))
        }
        HorizontalDivider(color = Hairline)
    }
}

// ---------------------------------------------------------------------------
// Guide
// ---------------------------------------------------------------------------

@Composable
private fun GuideSection() {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GuideCard(
            tier = SendTier.LOCAL,
            useWhen = "People physically around you: same street, same crowd, same building.",
            reach = "Radio range only (~10–30 m). Repeats until a peer echoes it back, then sparsely for up to 30 min. An echo is a hint, not a delivery guarantee.",
            trust = "HIGH. The sender proved co-presence with your radio cell (PoCP). Witnessless frames are relay-only and never shown."
        )
        GuideCard(
            tier = SendTier.BROADCAST,
            useWhen = "Reaching people beyond radio range; announcements to the whole area.",
            reach = "Carried by the mesh up to 8 hops. Shows with a corroboration counter: distinct claims heard DIRECTLY from nearby devices.",
            trust = "MEDIUM. Signed + witness-bound, but the counter is a HINT, not a proof — a determined nearby attacker can forge claims. The sender can be many hops away."
        )
        GuideCard(
            tier = SendTier.PRIVATE,
            useWhen = "Content meant for one person only. Pair out-of-band first (QR + salt).",
            reach = "Whole mesh, like broadcast — but only the paired contact can read it.",
            trust = "HIGH content. Only the contact can read or write it. v2 pairings ratchet keys every epoch: a seized phone exposes at most the current and previous epoch. Relays see that a private frame passed, not what it says."
        )

        HorizontalDivider(color = Hairline)
        Text("TRUST METER — HOW A MESSAGE ARRIVED", style = monoMicro(TextBright))
        LegendRow(3, TierLocal, "DIRECT", "Arrived at origination TTL — straight off the sender's radio, physically near you.")
        LegendRow(2, TrustAmber, "RELAYED", "Carried through mesh hops. Content still verified; sender may be far.")
        LegendRow(3, TierPrivate, "E2E", "End-to-end encrypted: only the paired contact could produce it.")
        Text(
            "Every displayed frame passed a signature check. LOCAL also proved co-presence " +
                "(PoCP). BROADCAST carries a bound witness — its claim counter covers only " +
                "devices heard directly and is a hint, never a guarantee. PRIVATE is " +
                "end-to-end encrypted and proof-of-work gated.",
            style = monoMicro(), lineHeight = 15.sp
        )
    }
}

@Composable
private fun GuideCard(tier: SendTier, useWhen: String, reach: String, trust: String) {
    val c = tierColor(tier)
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, Hairline, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Text(tier.name, style = monoLabel(c))
        Spacer(Modifier.height(6.dp))
        Text("USE WHEN", style = monoMicro(c))
        Text(useWhen, color = TextBright, fontSize = 13.sp, lineHeight = 17.sp)
        Spacer(Modifier.height(5.dp))
        Text("REACH", style = monoMicro(c))
        Text(reach, color = TextDim, fontSize = 12.sp, lineHeight = 16.sp)
        Spacer(Modifier.height(5.dp))
        Text("TRUST LEVEL", style = monoMicro(c))
        Text(trust, color = TextDim, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun LegendRow(bars: Int, color: androidx.compose.ui.graphics.Color, name: String, desc: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MeterBars(bars, color)
        Spacer(Modifier.padding(start = 8.dp))
        Column {
            Text(name, style = monoMicro(color))
            Text(desc, color = TextDim, fontSize = 11.sp, lineHeight = 14.sp)
        }
    }
}

// ---------------------------------------------------------------------------
// Detector
// ---------------------------------------------------------------------------

@Composable
private fun DetectorSection() {
    val stats by MeshState.stats.collectAsStateWithLifecycle()
    val running by MeshState.running.collectAsStateWithLifecycle()
    val n = stats.neighborsThisEpoch

    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (running) "$n" else "—",
                    style = monoLabel(if (n > 0) TierLocal else TextDim),
                    fontSize = 34.sp
                )
                Text("DEVICES ON YOUR SIGNAL", style = monoMicro())
            }
            MeterBars(
                filled = when { !running || n == 0 -> 0; n <= 2 -> 1; n <= 5 -> 2; else -> 3 },
                color = if (n > 0) TierLocal else TextDim
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Frames arriving at their origination TTL — no relay hop. " +
                "These radios are physically within range of yours, right now.",
            style = monoMicro(), lineHeight = 15.sp
        )
        Spacer(Modifier.height(10.dp))
        StatRow("STATUS", if (running) "RADIO ON" else "RADIO OFF")
        StatRow("EPOCH", "${stats.epoch}")
        StatRow("FRAMES RX (SESSION)", "${stats.totalHeard}")
        StatRow("SKETCH CELLS", "${stats.localSketch.size}")
        StatRow("PHY", if (stats.codedPhyActive) "CODED (LONG RANGE)" else "LEGACY 1M")
        if (stats.note.isNotEmpty()) StatRow("ADV MODE", stats.note.uppercase())
    }
}

@Composable
private fun StatRow(k: String, v: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(k, style = monoMicro(), modifier = Modifier.weight(1f))
        Text(v, style = monoMicro(TextBright))
    }
}

// ---------------------------------------------------------------------------
// Settings — every tunable parameter
// ---------------------------------------------------------------------------

@Composable
private fun SettingsSection(controller: UiController) {
    val cfg = MeshState.config
    var epochMs by rememberSaveable { mutableStateOf(cfg.epochMs.toString()) }
    var beaconFloorMs by rememberSaveable { mutableStateOf(cfg.beaconFloorMs.toString()) }
    var minHearers by rememberSaveable { mutableStateOf(cfg.minHearers.toString()) }
    var tau by rememberSaveable { mutableStateOf(cfg.tauThreshold.toString()) }
    var rssiFloor by rememberSaveable { mutableStateOf(cfg.rssiFloorDbm.toString()) }
    var advInterval by rememberSaveable { mutableStateOf(cfg.advIntervalMs.toString()) }
    var repeatEpochs by rememberSaveable { mutableStateOf(cfg.messageRepeatEpochs.toString()) }
    var codedPhy by rememberSaveable { mutableStateOf(cfg.codedPhy) }
    var lowLatency by rememberSaveable { mutableStateOf(cfg.scanLowLatency) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // C1: out-of-range values are clamped to safe ranges on apply (τ=0 would match
        // everything; minHearers=0 makes the beacon constant-entropy; mismatched epochMs
        // silently partitions the mesh).
        Text("EXPERT — values are clamped to safe ranges on apply", style = monoMicro(TrustAmber))
        ParamField("EPOCH LENGTH (MS)", epochMs) { epochMs = it }
        // C5: there is no handshake and none is planned — the epoch INDEX is derived as
        // now/epochMs, so two phones on different values diverge linearly and stop being able
        // to hear each other entirely within about a minute. The freshness banner does fire,
        // but it names clocks first, so the setting has to warn for itself.
        Text(
            "must match on every phone — a different value silently splits the mesh",
            style = monoMicro(TrustAmber)
        )
        ParamField("BEACON FLOOR (MS)", beaconFloorMs) { beaconFloorMs = it }
        ParamField("MIN HEARERS (BEACON ENTROPY)", minHearers) { minHearers = it }
        ParamField("TAU THRESHOLD (CELL MATCH)", tau) { tau = it }
        ParamField("RSSI FLOOR (DBM)", rssiFloor) { rssiFloor = it }
        ParamField("ADV INTERVAL (MS)", advInterval) { advInterval = it }
        ParamField("MESSAGE REPEAT (EPOCHS)", repeatEpochs) { repeatEpochs = it }

        ParamSwitch("CODED PHY (LONG RANGE)", codedPhy) { codedPhy = it }
        ParamSwitch("LOW-LATENCY SCAN", lowLatency) { lowLatency = it }

        error?.let { Text(it, style = monoMicro(PanicRed)) }

        Box(
            Modifier
                .fillMaxWidth()
                .background(TierLocal.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .border(1.dp, TierLocal, RoundedCornerShape(8.dp))
                .clickable {
                    val parsed = MeshConfig(
                        epochMs = epochMs.toLongOrNull() ?: return@clickable run { error = "epoch ms: not a number" },
                        beaconFloorMs = beaconFloorMs.toLongOrNull() ?: return@clickable run { error = "beacon floor: not a number" },
                        minHearers = minHearers.toIntOrNull() ?: return@clickable run { error = "min hearers: not a number" },
                        tauThreshold = tau.toFloatOrNull() ?: return@clickable run { error = "tau: not a number" },
                        rssiFloorDbm = rssiFloor.toIntOrNull() ?: return@clickable run { error = "rssi floor: not a number" },
                        codedPhy = codedPhy,
                        advIntervalMs = advInterval.toLongOrNull() ?: return@clickable run { error = "adv interval: not a number" },
                        scanLowLatency = lowLatency,
                        messageRepeatEpochs = repeatEpochs.toIntOrNull() ?: return@clickable run { error = "repeat epochs: not a number" }
                    )
                    error = null
                    controller.applyConfig(parsed)
                }
                .padding(vertical = 13.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("APPLY PARAMETERS", style = monoLabel(TierLocal))
        }
    }
}

@Composable
private fun ParamField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, style = monoMicro()) },
        textStyle = monoBody(),
        singleLine = true,
        colors = OutlinedTextFieldDefaultsColors(),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ParamSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = monoMicro(TextBright), modifier = Modifier.weight(1f))
        Switch(
            checked = checked, onCheckedChange = onChange,
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

@Composable
private fun OutlinedTextFieldDefaultsColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedBorderColor = TierLocal,
    unfocusedBorderColor = Hairline,
    focusedTextColor = TextBright,
    unfocusedTextColor = TextBright,
    cursorColor = TierLocal,
    focusedLabelColor = TierLocal,
    unfocusedLabelColor = TextDim
)

// ---------------------------------------------------------------------------
// Diagnostics (merged rig toolset)
// ---------------------------------------------------------------------------

@Composable
private fun DiagnosticsSection(controller: UiController) {
    val log by MeshState.debugLog.collectAsStateWithLifecycle()
    var peerSketch by rememberSaveable { mutableStateOf("") }
    var verdict by remember { mutableStateOf<String?>(null) }

    val selfTestRunning by MeshState.selfTestRunning.collectAsStateWithLifecycle()

    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Everything this app does only really runs on a phone: cargo test exercises the core
        // on a desktop, and R8 can break the UniFFI/JNA bridge at runtime with no compile-time
        // trace at all. This is the only check that covers the device.
        DiagButton(
            if (selfTestRunning) "SELF-TEST RUNNING…" else "RUN SELF-TEST (ALL MODULES)",
            Modifier.fillMaxWidth()
        ) { if (!selfTestRunning) controller.runSelfTest() }
        Text(
            "Starts at the next epoch boundary. Run it on BOTH phones within a few seconds of " +
                "each other and the two reports carry the same epoch, so the mark and pairing " +
                "lines can be compared directly.",
            style = monoMicro(TextDim)
        )

        val selfTest by MeshState.selfTestLog.collectAsStateWithLifecycle()
        if (selfTest.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DiagButton("SHARE REPORT", Modifier.weight(1f)) { controller.shareSelfTest() }
                DiagButton("CLEAR REPORT", Modifier.weight(1f)) {
                    MeshState.selfTestLog.value = emptyList()
                }
            }
            SelectionContainer {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Panel)
                        .padding(8.dp)
                ) {
                    // Oldest first: this is a report, read top to bottom, unlike the live log.
                    selfTest.forEach { line ->
                        Text(
                            line,
                            style = monoMicro(
                                when {
                                    line.startsWith("[FAIL]") -> PanicRed
                                    line.startsWith("[PASS]") -> TierLocal
                                    line.startsWith("RESULT:") || line.startsWith("BILEICHAT") ->
                                        TextBright
                                    else -> TextDim
                                }
                            ),
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DiagButton("EXPORT LOG", Modifier.weight(1f)) { controller.exportLog() }
            DiagButton("CLEAR LOG", Modifier.weight(1f)) { controller.clearLog() }
        }
        // The measurement export already carried a privacy warning; the log export did not,
        // even though it records epochs, presence counts and message activity timings.
        Text(
            "Exports leave the app. The log records timings and mesh activity — treat it as " +
                "sensitive and delete it after use.",
            style = monoMicro(TrustAmber)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DiagButton("EXPORT DATA", Modifier.weight(1f)) { controller.exportMeasurements() }
            DiagButton("COPY SKETCH", Modifier.weight(1f)) { controller.copySketch() }
        }
        // P1: moved out of startup — launching this Settings screen from onCreate backgrounded
        // the activity mid-permission-chain and killed the process before permissions showed.
        DiagButton("KEEP ALIVE IN DOZE", Modifier.fillMaxWidth()) { controller.requestBatteryBypass() }

        Text("COMPARE PEER SKETCH (SAME CELL?)", style = monoMicro())
        OutlinedTextField(
            value = peerSketch,
            onValueChange = { peerSketch = it },
            textStyle = monoBody(),
            placeholder = { Text("paste peer sketch…", style = monoMicro()) },
            colors = OutlinedTextFieldDefaultsColors(),
            modifier = Modifier.fillMaxWidth()
        )
        DiagButton("COMPARE", Modifier.fillMaxWidth()) {
            verdict = controller.compareSketch(peerSketch)
        }
        verdict?.let { Text(it, style = monoMicro(TierLocal), lineHeight = 15.sp) }

        Text("DEBUG LOG (NEWEST FIRST)", style = monoMicro())
        Column(
            Modifier
                .fillMaxWidth()
                .border(1.dp, Hairline, RoundedCornerShape(6.dp))
                .padding(8.dp)
        ) {
            log.take(10).forEach { line ->
                Text(line, style = monoMicro(), lineHeight = 14.sp)
            }
            if (log.isEmpty()) Text("(empty)", style = monoMicro())
        }
    }
}

@Composable
private fun DiagButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        color = PanelRaised,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Hairline),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Text(
            label,
            style = monoMicro(TextBright),
            modifier = Modifier.padding(vertical = 11.dp, horizontal = 8.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Panic
// ---------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PanicButton(controller: UiController) {
    Surface(
        color = PanicRed.copy(alpha = 0.10f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PanicRed),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .combinedClickable(
                onClick = { controller.toast("HOLD to wipe all keys, contacts, and data") },
                onLongClick = { controller.panicWipe() }
            )
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("HOLD TO WIPE — PANIC", style = monoLabel(PanicRed))
            Text("Erases pairing keys, contacts, config, and logs. Irreversible.", style = monoMicro())
        }
    }
}

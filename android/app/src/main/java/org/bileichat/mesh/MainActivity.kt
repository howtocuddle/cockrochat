package org.bileichat.mesh

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import org.bileichat.mesh.ui.MeshTheme
import org.bileichat.mesh.ui.MeshUi
import org.bileichat.mesh.ui.UiController
import uniffi.mesh_core.jaccardSketch

/**
 * Single unified activity: messaging UI + left settings drawer (Compose).
 * Replaces the old MainActivity (rig) / ChatActivity (live) pair.
 */
class MainActivity : ComponentActivity() {

    /** Receives the key from the QR scanner while the pairing dialog is open. */
    private var pendingQrCallback: ((String) -> Unit)? = null

    /** P1: set when a service start was requested while the activity was backgrounded
     *  (API 31+ forbids it); retried from onStart. */
    private var startPending = false

    /** P1: consecutive permission denials — the second one means "don't ask again". */
    private var permissionDenials = 0

    private val scanQrLauncher =
        registerForActivityResult(ScanContract()) { result ->
            // A3: pass the RAW payload through — the v2 salt inside it is required for the
            // forward-secret chain seed; stripping it would silently degrade to a static key.
            val payload = result.contents?.trim() ?: return@registerForActivityResult
            if (PairStore.parsePairingOffer(payload) == null) {
                toast("That QR is not a BileiChat pairing key")
            } else {
                pendingQrCallback?.invoke(payload)
                toast("Pairing key scanned")
            }
        }

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchScanner()
            else toast("Camera permission is needed to scan a pairing QR")
        }

    /**
     * P1: BLE permission result.
     *
     * Two bugs lived here. `results.values.all { it }` is TRUE for an empty map, and
     * RequestMultiplePermissions returns an empty map whenever the dialog is cancelled —
     * so cancelling took the SUCCESS branch, started the foreground service with no
     * Bluetooth permission at all, and Android 14+ killed the process with a
     * SecurityException (foregroundServiceType="connectedDevice" requires one of the BT
     * permissions). That is the "app quits before it can show permissions" report.
     *
     * Second, POST_NOTIFICATIONS was treated as mandatory, so denying only the notification
     * prompt blocked the entire mesh.
     */
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.isEmpty()) {
                // Dialog dismissed/cancelled — NOT a grant.
                toast("Permission request was dismissed — tap the radio switch to try again")
                MeshState.running.value = false
                return@registerForActivityResult
            }
            val missing = requiredBlePermissions().filter { results[it] == false }
            if (missing.isEmpty()) {
                if (results[android.Manifest.permission.POST_NOTIFICATIONS] == false) {
                    // Optional: the mesh runs fine, the user just won't see the status.
                    toast("Running without notifications — the mesh status bar will be hidden")
                }
                ensureBluetoothThenStart()
            } else {
                permissionDenials += 1
                if (permissionDenials >= 2) {
                    toast("Bluetooth permission is permanently denied — enable it in Settings")
                    openAppSettings()
                } else {
                    toast("BileiChat needs Bluetooth permission to reach nearby phones")
                }
                MeshState.running.value = false
            }
        }

    private val enableBtLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                doStartService()
            } else {
                toast("Bluetooth must be enabled for the mesh")
                MeshState.running.value = false
            }
        }

    /** Process slash commands. Returns true if the text was a command (consumed). */
    private fun handleSlashCommand(text: String): Boolean {
        val trimmed = text.trim()
        if (!trimmed.startsWith("/")) return false
        val parts = trimmed.split(" ", limit = 2)
        val cmd = parts[0].lowercase()
        when (cmd) {
            "/clear" -> {
                MeshState.messages.value = emptyList()
                toast("Chat cleared")
            }
            "/wipe", "/panic" -> {
                MeshService.requestPanicWipe(this)
                toast("Wiped")
                finishAffinity()
            }
            "/export" -> {
                val log = MeshState.debugLog.value.asReversed().joinToString("\n")
                if (log.isEmpty()) { toast("Log is empty") } else share(log, "mesh_debug_log.txt", "text/plain")
            }
            "/help" -> {
                MeshState.appendMessage(
                    MsgRow(
                        tsMs = System.currentTimeMillis(),
                        epoch = 0u,
                        markHexPrefix = "SYSTEM",
                        rssi = null,
                        text = "COMMANDS: /clear · /export · /wipe · /me <action> · /help",
                        mine = false,
                        tier = MeshState.outgoingTier.value
                    )
                )
            }
            "/me" -> return false // handled inline by the caller as action text
            else -> {
                toast("Unknown command: $cmd — try /help")
            }
        }
        return true
    }

    private val controller = object : UiController {
        override val versionName: String
            get() = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(packageName, 0)
                }.versionName ?: "?"
            } catch (_: Exception) { "?" }

        override fun setMeshRunning(on: Boolean) {
            if (on) startPermissionChain() else stopService(Intent(this@MainActivity, MeshService::class.java))
        }

        override fun requestBatteryBypass() = requestBatteryOptimizationBypass()

        override fun applyConfig(cfg: MeshConfig) {
            val clean = cfg.sanitized() // C1: clamp footguns before they reach the engine
            ConfigStore.save(this@MainActivity, clean)
            MeshState.config = clean
            if (clean != cfg) toast("Config applied (values clamped to safe ranges)")
            else toast("Config applied")
        }

        override fun exportLog() {
            val text = MeshState.debugLog.value.asReversed().joinToString("\n")
            if (text.isEmpty()) { toast("Log is empty"); return }
            share(text, "mesh_debug_log.txt", "text/plain")
        }

        override fun clearLog() { MeshState.debugLog.value = emptyList() }

        override fun exportMeasurements() {
            // D6: this file reveals who was physically near this device and when.
            toast("Export contains RF-proximity data — share carefully")
            share(MeshState.measurement.exportJson(MeshState.config), "mesh_measurements.json", "application/json")
        }

        override fun copySketch() {
            val sketch = MeshState.stats.value.localSketch
            if (sketch.isEmpty()) { toast("No local sketch yet"); return }
            val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cb.setPrimaryClip(ClipData.newPlainText("mesh_sketch", sketch.joinToString(" ") { it.toString() }))
            toast("Sketch copied")
        }

        override fun compareSketch(peerText: String): String {
            val peer = try {
                peerText.split(Regex("[,\\s]+")).filter { it.isNotBlank() }.map { it.trim().toULong() }
            } catch (_: NumberFormatException) {
                return "Could not parse — use space/comma-separated numbers."
            }
            if (peer.isEmpty()) return "Paste a peer sketch first."
            val local = MeshState.stats.value.localSketch
            if (local.isEmpty()) return "No local sketch yet — turn the radio on and wait an epoch."
            val sim = jaccardSketch(local, peer)
            val tau = MeshState.config.tauThreshold
            val verdict = if (sim >= tau) "SAME CELL (≥ τ)" else "DIFFERENT CELL (< τ)"
            return "Jaccard = %.4f · τ = %.2f · %s".format(sim, tau, verdict)
        }

        override fun sendPublic(text: String): String? {
            if (handleSlashCommand(text)) return null
            if (text.toByteArray(Charsets.UTF_8).size > 63) return "Too long (max 63 UTF-8 bytes)"
            MeshState.receipt.value = null
            MeshState.outgoingText.value = text
            // Set text first, then bump — the service collects both, so an identical re-send
            // still re-originates instead of being conflated away.
            MeshState.outgoingRevision.value += 1
            MeshState.appendMessage(
                MsgRow(
                    tsMs = System.currentTimeMillis(),
                    epoch = 0u,
                    markHexPrefix = "me",
                    rssi = null,
                    text = text,
                    mine = true,
                    tier = if (MeshState.outgoingTier.value == SendTier.LOCAL) SendTier.LOCAL else SendTier.BROADCAST
                )
            )
            return null
        }

        override fun sendPrivate(contact: Contact, text: String): String? {
            if (handleSlashCommand(text)) return null
            if (text.toByteArray(Charsets.UTF_8).size > 47) return "Too long (max 47 UTF-8 bytes)"
            if (!MeshState.running.value) return "Turn the radio on first"
            MeshState.receipt.value = null
            // C4: queue (no key material in the queue — the service ratchets at seal time).
            val result = MeshState.privateSends.trySend(PrivateSend(contact.label, text))
            if (result.isFailure) return "Send queue full — wait for the current private send"
            MeshState.appendMessage(
                MsgRow(
                    tsMs = System.currentTimeMillis(),
                    epoch = 0u,
                    markHexPrefix = "🔒 me→${contact.label}",
                    rssi = null,
                    text = text,
                    mine = true,
                    tier = SendTier.PRIVATE,
                    contactLabel = contact.label
                )
            )
            toast("Sealing (VDL takes a few seconds)…")
            return null
        }

        override fun contacts(): List<Contact> = PairStore.contacts(this@MainActivity)

        override fun addContact(label: String, keyOrQr: String): String? {
            val err = PairStore.addContact(this@MainActivity, label, keyOrQr)
            if (err == null) {
                MeshState.contactsVersion.value += 1
                toast("Paired with ${label.trim()}")
            }
            return err
        }

        override fun removeContact(label: String) {
            PairStore.removeContact(this@MainActivity, label)
            MeshState.contactsVersion.value += 1
        }

        override fun myPublicHex(): String = PairStore.myPublicHex(this@MainActivity)
        override fun myQrPayload(): String = PairStore.qrPayload(this@MainActivity)
        override fun mySaltHex(): String = PairStore.mySaltHex(this@MainActivity)

        override fun launchQrScanner(onKey: (String) -> Unit) {
            pendingQrCallback = onKey
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this@MainActivity, android.Manifest.permission.CAMERA
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                launchScanner()
            } else {
                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }

        override fun panicWipe() {
            MeshService.requestPanicWipe(this@MainActivity)
            toast("Wiped")
            finishAffinity()
        }

        override fun toast(msg: String) = this@MainActivity.toast(msg)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // C2: prevent screenshots and screen recording (state-actor threat model)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.BLACK
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.BLACK

        MeshState.config = ConfigStore.load(this)
        // D4: say it in the UI when the TEE-backed store is unavailable (pairings would
        // silently die with the process otherwise).
        MeshState.secureStorageOk.value = PairStore.secureStorageAvailable(this)

        setContent {
            MeshTheme {
                MeshUi(controller)
            }
        }

        // ── Startup chain ──
        // P1: strictly sequenced, one system dialog at a time.
        //
        //   permissions ─▶ Bluetooth enable ─▶ start service
        //
        // The old order was the cause of the "app quits before it can show permissions" bug.
        // It ran requestBatteryOptimizationBypass() FIRST, which launches a Settings activity
        // and backgrounds this one, then immediately asked to enable Bluetooth, then asked for
        // permissions — three system activities racing out of one onCreate. Two failures fell
        // out of that: starting a foreground service while backgrounded throws
        // ForegroundServiceStartNotAllowedException on API 31+, and on API 31+
        // ACTION_REQUEST_ENABLE itself requires BLUETOOTH_CONNECT, which had not been granted
        // yet — so it returned RESULT_CANCELED and the permission dialog was NEVER reached on
        // a fresh install with Bluetooth off.
        //
        // Battery-optimization bypass is no longer requested here; it is offered from the
        // drawer once the mesh is actually running.
        startPermissionChain()
    }

    /** Step 1 of the startup chain: BLE runtime permissions. */
    private fun startPermissionChain() {
        if (!packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_BLUETOOTH_LE)) {
            toast("This device has no Bluetooth LE — the mesh cannot run here")
            MeshState.running.value = false
            return
        }
        val needed = (requiredBlePermissions() + optionalPermissions()).filter {
            androidx.core.content.ContextCompat.checkSelfPermission(this, it) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (needed.isEmpty()) {
            ensureBluetoothThenStart()
        } else {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    /** Runtime permissions without which the mesh genuinely cannot function. */
    private fun requiredBlePermissions(): List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_ADVERTISE,
                android.Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            // Pre-31 BLE scanning is gated on location; BLUETOOTH/BLUETOOTH_ADMIN are
            // install-time permissions and need no runtime request.
            listOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }

    /** Nice to have, never a blocker. */
    private fun optionalPermissions(): List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyList()
        }

    /**
     * Step 2: make sure the adapter is on, then start. Reached only once BLUETOOTH_CONNECT
     * is held, which is what ACTION_REQUEST_ENABLE requires on API 31+.
     */
    private fun ensureBluetoothThenStart() {
        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = btManager?.adapter
        if (adapter == null) {
            toast("No Bluetooth adapter on this device")
            MeshState.running.value = false
            return
        }
        if (!adapter.isEnabled) {
            try {
                enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            } catch (e: Exception) {
                // Some OEM ROMs throw instead of returning a result.
                toast("Could not open the Bluetooth prompt — turn Bluetooth on manually")
                MeshState.running.value = false
            }
            return
        }
        doStartService()
    }

    /** Ask the system to ignore battery optimizations for this app (Doze bypass).
     *  Offered from the drawer AFTER the mesh is running — never during startup, where it
     *  used to background the activity mid-permission-chain. */
    private fun requestBatteryOptimizationBypass() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(packageName)) {
            toast("Battery optimization is already disabled for BileiChat")
            return
        }
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            toast("This ROM blocks the battery-optimization prompt — disable it in Settings")
        }
    }

    private fun openAppSettings() {
        try {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        } catch (_: Exception) {
            toast("Could not open Settings")
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    private fun launchScanner() {
        scanQrLauncher.launch(
            ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt("Scan a BileiChat pairing QR")
                .setBeepEnabled(false)
                .setOrientationLocked(false)
        )
    }

    /**
     * Step 3: start the foreground service.
     *
     * P1: every failure here used to be an uncaught exception on the main thread, i.e. a
     * process kill that looked like "the app quit on its own":
     *   - SecurityException when the connectedDevice FGS type is started without a
     *     Bluetooth permission (API 34+),
     *   - ForegroundServiceStartNotAllowedException when started from the background
     *     (API 31+), which is exactly what happened while a Settings screen was on top.
     * Both are now reported to the user and leave the app alive and retryable.
     */
    private fun doStartService() {
        if (!lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) {
            // Starting a foreground service from the background is not allowed on API 31+.
            // Defer to onStart, which re-runs the chain.
            startPending = true
            return
        }
        val intent = Intent(this, MeshService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: SecurityException) {
            toast("Android refused to start the mesh service (missing Bluetooth permission)")
            MeshState.logDebug("startForegroundService SecurityException: ${e.message}")
            MeshState.running.value = false
        } catch (e: Exception) {
            toast("Could not start the mesh service — reopen the app and try again")
            MeshState.logDebug("startForegroundService failed: ${e.message}")
            MeshState.running.value = false
        }
    }

    override fun onStart() {
        super.onStart()
        if (startPending) {
            startPending = false
            doStartService()
        }
    }

    private fun share(text: String, subject: String, mimeType: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        startActivity(Intent.createChooser(intent, "Share"))
    }
}

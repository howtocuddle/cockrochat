package org.cockroachat.mesh

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import org.cockroachat.mesh.ui.MeshTheme
import org.cockroachat.mesh.ui.MeshUi
import org.cockroachat.mesh.ui.UiController
import uniffi.mesh_core.jaccardSketch

/**
 * Single unified activity: messaging UI + left settings drawer (Compose).
 * Replaces the old MainActivity (rig) / ChatActivity (live) pair.
 */
class MainActivity : ComponentActivity() {

    /** Receives the key from the QR scanner while the pairing dialog is open. */
    private var pendingQrCallback: ((String) -> Unit)? = null

    private val scanQrLauncher =
        registerForActivityResult(ScanContract()) { result ->
            // A3: pass the RAW payload through — the v2 salt inside it is required for the
            // forward-secret chain seed; stripping it would silently degrade to a static key.
            val payload = result.contents?.trim() ?: return@registerForActivityResult
            if (PairStore.parsePairingOffer(payload) == null) {
                toast("That QR is not a Cockroachat pairing key")
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

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.values.all { it }) {
                doStartService()
            } else {
                toast("BLE permissions required to start")
                MeshState.running.value = false
            }
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
            if (on) requestPermissionsAndStart() else stopService(Intent(this@MainActivity, MeshService::class.java))
        }

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
            if (text.toByteArray(Charsets.UTF_8).size > 63) return "Too long (max 63 UTF-8 bytes)"
            MeshState.receipt.value = null
            MeshState.outgoingText.value = text
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
                    tier = SendTier.PRIVATE
                )
            )
            toast("Sealing (VDL takes a few seconds)…")
            return null
        }

        override fun contacts(): List<Contact> = PairStore.contacts(this@MainActivity)

        override fun addContact(label: String, keyOrQr: String): Boolean {
            val ok = PairStore.addContact(this@MainActivity, label, keyOrQr)
            if (ok) {
                MeshState.contactsVersion.value += 1
                toast("Paired with ${label.trim()}")
            } else {
                toast("Bad name or key (pairing with your own key is not allowed)")
            }
            return ok
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
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    private fun launchScanner() {
        scanQrLauncher.launch(
            ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt("Scan a Cockroachat pairing QR")
                .setBeepEnabled(false)
                .setOrientationLocked(false)
        )
    }

    private fun requestPermissionsAndStart() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms += android.Manifest.permission.BLUETOOTH_SCAN
            perms += android.Manifest.permission.BLUETOOTH_ADVERTISE
            perms += android.Manifest.permission.BLUETOOTH_CONNECT
        } else {
            perms += android.Manifest.permission.ACCESS_FINE_LOCATION
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms += android.Manifest.permission.POST_NOTIFICATIONS
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    private fun doStartService() {
        val intent = Intent(this, MeshService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
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

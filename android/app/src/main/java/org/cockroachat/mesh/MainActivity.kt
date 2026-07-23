package org.cockroachat.mesh

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import uniffi.mesh_core.jaccardSketch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    // Views
    private lateinit var tvStats: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnOpenChat: Button
    private lateinit var btnPanic: Button
    private lateinit var etEpoch: EditText
    private lateinit var etTau: EditText
    private lateinit var etFloor: EditText
    private lateinit var etAdv: EditText
    private lateinit var swCoded: SwitchCompat
    private lateinit var btnApply: Button
    private lateinit var etPeer: EditText
    private lateinit var btnCompare: Button
    private lateinit var tvJaccard: TextView
    private lateinit var btnCopySketch: Button
    private lateinit var btnExport: Button

    // Message UI
    private lateinit var svMessages: NestedScrollView
    private lateinit var tvMessages: TextView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button

    // Debug log UI
    private lateinit var svDebugLog: NestedScrollView
    private lateinit var tvDebugLog: TextView
    private lateinit var btnClearLog: Button
    private lateinit var btnExportLog: Button

    // Holds the local sketch for comparison / copy
    @Volatile
    private var localSketch: List<ULong> = emptyList()

    private val msgTimeFmt = SimpleDateFormat("HH:mm:ss", Locale.US)

    private val panicHoldHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var panicHoldRunnable: Runnable? = null

    private companion object { const val PANIC_HOLD_MS = 1500L }

    // Permission launcher
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val allGranted = results.values.all { it }
            if (allGranted) {
                doStartService()
            } else {
                Toast.makeText(this, "BLE permissions required to start", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // C2: prevent screenshots and screen recording (state-actor threat model)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        // Load persisted config into state
        val cfg = ConfigStore.load(this)
        MeshState.config = cfg

        bindViews()
        populateConfigFields(cfg)
        setupListeners()
        observeState()

        // Stamp the build version into the title: rig/live and old/new builds look
        // identical in the launcher, and field mix-ups have already cost a test cycle.
        try {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            findViewById<TextView>(R.id.tvTitle).text = "BLE Mesh Field Tool  v${info.versionName}"
        } catch (e: Exception) {
            // Leave the default title.
        }
    }

    private fun bindViews() {
        tvStats = findViewById(R.id.tvStats)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnOpenChat = findViewById(R.id.btnOpenChat)
        btnPanic = findViewById(R.id.btnPanic)
        etEpoch = findViewById(R.id.etEpoch)
        etTau = findViewById(R.id.etTau)
        etFloor = findViewById(R.id.etFloor)
        etAdv = findViewById(R.id.etAdv)
        swCoded = findViewById(R.id.swCoded)
        btnApply = findViewById(R.id.btnApply)
        etPeer = findViewById(R.id.etPeer)
        btnCompare = findViewById(R.id.btnCompare)
        tvJaccard = findViewById(R.id.tvJaccard)
        btnCopySketch = findViewById(R.id.btnCopySketch)
        btnExport = findViewById(R.id.btnExport)

        svMessages = findViewById(R.id.svMessages)
        tvMessages = findViewById(R.id.tvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)

        svDebugLog = findViewById(R.id.svDebugLog)
        tvDebugLog = findViewById(R.id.tvDebugLog)
        btnClearLog = findViewById(R.id.btnClearLog)
        btnExportLog = findViewById(R.id.btnExportLog)
    }

    private fun populateConfigFields(cfg: MeshConfig) {
        etEpoch.setText(cfg.epochMs.toString())
        etTau.setText(cfg.tauThreshold.toString())
        etFloor.setText(cfg.rssiFloorDbm.toString())
        etAdv.setText(cfg.advIntervalMs.toString())
        swCoded.isChecked = cfg.codedPhy
    }

    private fun setupListeners() {
        btnStart.setOnClickListener { requestPermissionsAndStart() }

        btnStop.setOnClickListener {
            stopService(Intent(this, MeshService::class.java))
        }

        btnOpenChat.setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }

        btnPanic.text = "HOLD ⇢ WIPE"
        btnPanic.setOnTouchListener { v, ev ->
            when (ev.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    v.alpha = 0.6f
                    val r = Runnable {
                        panicHoldRunnable = null
                        v.alpha = 1f
                        MeshService.requestPanicWipe(this@MainActivity)
                        Toast.makeText(this@MainActivity, "Wiped", Toast.LENGTH_SHORT).show()
                        finishAffinity()
                    }
                    panicHoldRunnable = r
                    panicHoldHandler.postDelayed(r, PANIC_HOLD_MS)
                    Toast.makeText(this@MainActivity, "Hold to wipe…", Toast.LENGTH_SHORT).show()
                    true
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    v.alpha = 1f
                    val pending = panicHoldRunnable
                    if (pending != null) {
                        panicHoldHandler.removeCallbacks(pending)
                        panicHoldRunnable = null
                        Toast.makeText(this@MainActivity, "Hold ~1.5s to wipe", Toast.LENGTH_SHORT).show()
                    }
                    v.performClick()
                    true
                }
                else -> false
            }
        }

        btnApply.setOnClickListener {
            val cfg = readConfigFromFields() ?: return@setOnClickListener
            ConfigStore.save(this, cfg)
            MeshState.config = cfg
            Toast.makeText(this, "Config applied", Toast.LENGTH_SHORT).show()
        }

        btnCompare.setOnClickListener {
            val peerText = etPeer.text.toString().trim()
            if (peerText.isEmpty()) {
                tvJaccard.text = "Paste a peer sketch first."
                return@setOnClickListener
            }
            val peerSketch = parsePeerSketch(peerText)
            if (peerSketch == null) {
                tvJaccard.text = "Could not parse peer sketch. Use space- or comma-separated ULong values."
                return@setOnClickListener
            }
            val local = localSketch
            if (local.isEmpty()) {
                tvJaccard.text = "No local sketch yet — start the service and wait for a full epoch."
                return@setOnClickListener
            }
            val sim = jaccardSketch(local, peerSketch)
            val tau = MeshState.config.tauThreshold
            val verdict = if (sim >= tau) "SAME CELL (≥ τ)" else "DIFFERENT CELL (< τ)"
            tvJaccard.text = "Jaccard = %.4f  |  τ = %.2f  |  %s".format(sim, tau, verdict)
        }

        btnCopySketch.setOnClickListener {
            val sketch = localSketch
            if (sketch.isEmpty()) {
                Toast.makeText(this, "No local sketch yet", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val text = sketch.joinToString(" ") { it.toString() }
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("mesh_sketch", text))
            Toast.makeText(this, "Sketch copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        btnExport.setOnClickListener {
            val cfg = MeshState.config
            val json = MeshState.measurement.exportJson(cfg)
            shareJson(json)
        }

        btnSend.setOnClickListener {
            val raw = etMessage.text.toString()
            if (raw.isEmpty()) {
                Toast.makeText(this, "Message is empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val bytes = raw.toByteArray(Charsets.UTF_8)
            if (bytes.size > 63) {
                Toast.makeText(
                    this,
                    "Message too long (${bytes.size} bytes, max 63 UTF-8 bytes)",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            // Update outgoing text — service will rebuild the frame immediately via collect
            MeshState.outgoingText.value = raw
            // Append to local message feed as "mine"
            val cfg = MeshState.config
            val nowMs = System.currentTimeMillis()
            val epoch = (nowMs / cfg.epochMs).toUInt()
            MeshState.appendMessage(
                MsgRow(
                    tsMs = nowMs,
                    epoch = epoch,
                    markHexPrefix = "me",
                    rssi = null,
                    text = raw,
                    mine = true,
                    tier = SendTier.BROADCAST
                )
            )
            etMessage.setText("")
        }

        btnExportLog.setOnClickListener {
            val logText = tvDebugLog.text.toString()
            if (logText.isEmpty()) {
                Toast.makeText(this, "Nothing to export — log is empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            shareText(logText, "mesh_debug_log.txt", "text/plain")
        }

        btnClearLog.setOnClickListener {
            MeshState.debugLog.value = emptyList()
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    MeshState.stats.collect { stats ->
                        localSketch = stats.localSketch
                        updateStatsView(stats)
                    }
                }
                launch {
                    MeshState.running.collect { running ->
                        btnStart.isEnabled = !running
                        btnStop.isEnabled = running
                    }
                }
                launch {
                    MeshState.messages.collect { msgs ->
                        updateMessagesView(msgs)
                    }
                }
                launch {
                    MeshState.debugLog.collect { log ->
                        tvDebugLog.text = log.joinToString("\n")
                    }
                }
            }
        }
    }

    private fun updateStatsView(stats: Stats) {
        val sketchPreview = if (stats.localSketch.isEmpty()) {
            "(empty)"
        } else {
            stats.localSketch.take(4).joinToString(" ") { "%016x".format(it.toLong()) } +
                if (stats.localSketch.size > 4) " …" else ""
        }

        tvStats.text = buildString {
            appendLine("Running   : ${MeshState.running.value}")
            appendLine("Epoch     : ${stats.epoch}")
            appendLine("Neighbors : ${stats.neighborsThisEpoch} (this epoch)")
            appendLine("Total Rx  : ${stats.totalHeard}")
            appendLine("Adv       : ${stats.advertising}")
            appendLine("Scan      : ${stats.scanning}")
            appendLine("Coded PHY : ${stats.codedPhyActive}")
            append    ("Sketch[0..3]: $sketchPreview")
            if (stats.note.isNotEmpty()) {
                appendLine()
                append("Note: ${stats.note}")
            }
        }
    }

    private fun updateMessagesView(msgs: List<MsgRow>) {
        tvMessages.text = msgs.joinToString("\n") { row ->
            val ts = msgTimeFmt.format(Date(row.tsMs))
            val rssiStr = if (row.rssi != null) " (${row.rssi}dBm)" else ""
            val tierTag = when (row.tier) {
                SendTier.LOCAL -> "[L]"
                SendTier.BROADCAST -> "[B]"
                SendTier.PRIVATE -> "[P]"
            }
            "$ts $tierTag [${row.markHexPrefix}]$rssiStr ${row.text}"
        }
        // Auto-scroll to bottom (newest last)
        svMessages.post { svMessages.fullScroll(View.FOCUS_DOWN) }
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

    private fun readConfigFromFields(): MeshConfig? {
        return try {
            MeshConfig(
                epochMs = etEpoch.text.toString().toLong(),
                tauThreshold = etTau.text.toString().toFloat(),
                rssiFloorDbm = etFloor.text.toString().toInt(),
                codedPhy = swCoded.isChecked,
                advIntervalMs = etAdv.text.toString().toLong()
            )
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Invalid config value: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun parsePeerSketch(text: String): List<ULong>? {
        return try {
            text.split(Regex("[,\\s]+"))
                .filter { it.isNotBlank() }
                .map { it.trim().toULong() }
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun shareJson(json: String) {
        shareText(json, "mesh_measurements.json", "application/json")
    }

    private fun shareText(text: String, subject: String, mimeType: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        startActivity(Intent.createChooser(intent, "Share"))
    }
}

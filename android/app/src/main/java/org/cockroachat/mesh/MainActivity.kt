package org.cockroachat.mesh

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import uniffi.mesh_core.jaccardSketch

class MainActivity : AppCompatActivity() {

    // Views
    private lateinit var tvStats: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
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

    // Holds the local sketch for comparison / copy
    @Volatile
    private var localSketch: List<ULong> = emptyList()

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

        // Load persisted config into state
        val cfg = ConfigStore.load(this)
        MeshState.config = cfg

        bindViews()
        populateConfigFields(cfg)
        setupListeners()
        observeState()
    }

    private fun bindViews() {
        tvStats = findViewById(R.id.tvStats)
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
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
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_TEXT, json)
            putExtra(Intent.EXTRA_SUBJECT, "mesh_measurements.json")
        }
        startActivity(Intent.createChooser(intent, "Share measurements"))
    }
}

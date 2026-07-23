package org.cockroachat.mesh

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Bitmap
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import android.os.Bundle
import android.widget.ImageView
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class ChatActivity : AppCompatActivity() {

    private lateinit var swRun: SwitchCompat
    private lateinit var tvChatStatus: TextView
    private lateinit var rgTier: RadioGroup
    private lateinit var rbLocal: RadioButton
    private lateinit var rbBroadcast: RadioButton
    private lateinit var rbPrivate: RadioButton
    private lateinit var rowPrivate: LinearLayout
    private lateinit var spContact: Spinner
    private lateinit var btnPair: Button
    private lateinit var svChat: ScrollView
    private lateinit var llChat: LinearLayout
    private lateinit var etChat: EditText
    private lateinit var btnChatSend: Button
    private lateinit var btnPanic: Button
    private lateinit var tvTierHint: TextView

    // Backing list for the recipient spinner (parallel to the adapter entries).
    private var contacts: List<Contact> = emptyList()

    // Guard against observer-triggered listener loop
    private var suppressSwitch = false
    /** The pairing dialog's input field, populated when the built-in scanner returns. */
    private var pairingPeerKeyInput: EditText? = null

    private val panicHoldHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var panicHoldRunnable: Runnable? = null

    private companion object { const val PANIC_HOLD_MS = 1500L }

    private val scanQrLauncher =
        registerForActivityResult(ScanContract()) { result ->
            val payload = result.contents ?: return@registerForActivityResult
            val key = PairStore.publicKeyFromQrOrHex(payload)
            if (key == null) {
                Toast.makeText(this, "That QR is not a Cockroach Chat pairing key", Toast.LENGTH_LONG).show()
            } else {
                pairingPeerKeyInput?.setText(key)
                Toast.makeText(this, "Pairing key scanned", Toast.LENGTH_SHORT).show()
            }
        }

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchPairingScanner()
            else Toast.makeText(this, "Camera permission is needed to scan a pairing QR", Toast.LENGTH_LONG).show()
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val allGranted = results.values.all { it }
            if (allGranted) {
                doStartService()
            } else {
                Toast.makeText(this, "BLE permissions required to start", Toast.LENGTH_LONG).show()
                suppressSwitch = true
                swRun.isChecked = false
                suppressSwitch = false
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // C2: prevent screenshots and screen recording (state-actor threat model)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)

        val cfg = ConfigStore.load(this)
        MeshState.config = cfg

        bindViews()
        setupListeners()
        refreshContacts()
        observeState()
    }

    private fun bindViews() {
        swRun = findViewById(R.id.swRun)
        tvChatStatus = findViewById(R.id.tvChatStatus)
        rgTier = findViewById(R.id.rgTier)
        rbLocal = findViewById(R.id.rbLocal)
        rbBroadcast = findViewById(R.id.rbBroadcast)
        rbPrivate = findViewById(R.id.rbPrivate)
        rowPrivate = findViewById(R.id.rowPrivate)
        spContact = findViewById(R.id.spContact)
        btnPair = findViewById(R.id.btnPair)
        svChat = findViewById(R.id.svChat)
        llChat = findViewById(R.id.llChat)
        etChat = findViewById(R.id.etChat)
        btnChatSend = findViewById(R.id.btnChatSend)
        btnPanic = findViewById(R.id.btnPanic)
        tvTierHint = findViewById(R.id.tvTierHint)
    }

    private fun setupListeners() {
        swRun.setOnCheckedChangeListener { _, isChecked ->
            if (suppressSwitch) return@setOnCheckedChangeListener
            if (isChecked) {
                requestPermissionsAndStart()
            } else {
                stopService(Intent(this, MeshService::class.java))
            }
        }

        rgTier.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbLocal -> MeshState.outgoingTier.value = SendTier.LOCAL
                R.id.rbBroadcast -> MeshState.outgoingTier.value = SendTier.BROADCAST
                R.id.rbPrivate -> MeshState.outgoingTier.value = SendTier.PRIVATE
            }
            rowPrivate.visibility =
                if (checkedId == R.id.rbPrivate) View.VISIBLE else View.GONE
            tvTierHint.text = when (checkedId) {
                R.id.rbLocal -> "Locale — direct radio range only (~10–30 m). Never relayed."
                R.id.rbPrivate -> "Private — end-to-end encrypted to one paired contact."
                else -> "Broadcast — relayed region-wide; displayed only when ≥2 nearby cells corroborate."
            }
        }

        btnPanic.text = "HOLD ⇢ WIPE"
        btnPanic.setOnTouchListener { v, ev ->
            when (ev.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    v.alpha = 0.6f
                    val r = Runnable {
                        panicHoldRunnable = null
                        v.alpha = 1f
                        MeshService.requestPanicWipe(this@ChatActivity)
                        Toast.makeText(this@ChatActivity, "Wiped", Toast.LENGTH_SHORT).show()
                        finishAffinity()
                    }
                    panicHoldRunnable = r
                    panicHoldHandler.postDelayed(r, PANIC_HOLD_MS)
                    Toast.makeText(this@ChatActivity, "Hold to wipe…", Toast.LENGTH_SHORT).show()
                    true
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    v.alpha = 1f
                    val pending = panicHoldRunnable
                    if (pending != null) {
                        panicHoldHandler.removeCallbacks(pending)
                        panicHoldRunnable = null
                        Toast.makeText(this@ChatActivity, "Hold ~1.5s to wipe", Toast.LENGTH_SHORT).show()
                    }
                    v.performClick()
                    true
                }
                else -> false
            }
        }

        btnPair.setOnClickListener { showPairingDialog() }

        btnChatSend.setOnClickListener {
            val text = etChat.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener

            if (MeshState.outgoingTier.value == SendTier.PRIVATE) {
                sendPrivate(text)
            } else {
                sendPublic(text)
            }
        }

        tvTierHint.text = "Broadcast — relayed region-wide; displayed only when ≥2 nearby cells corroborate."
    }

    private fun sendPublic(text: String) {
        if (text.toByteArray(Charsets.UTF_8).size > 63) {
            Toast.makeText(this, "Too long (max 63 bytes)", Toast.LENGTH_SHORT).show()
            return
        }
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
        etChat.setText("")
    }

    private fun sendPrivate(text: String) {
        // Private bodies are AEAD ciphertext with a smaller usable payload than the public plane.
        if (text.toByteArray(Charsets.UTF_8).size > 47) {
            Toast.makeText(this, "Private message too long (max 47 bytes)", Toast.LENGTH_SHORT).show()
            return
        }
        val idx = spContact.selectedItemPosition
        if (idx < 0 || idx >= contacts.size) {
            Toast.makeText(this, "Pair with a contact first", Toast.LENGTH_LONG).show()
            return
        }
        if (!MeshState.running.value) {
            Toast.makeText(this, "Turn the mesh on first", Toast.LENGTH_SHORT).show()
            return
        }
        val contact = contacts[idx]
        MeshState.outgoingPrivate.value = PrivateSend(contact.pairKey, text, contact.label)
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
        etChat.setText("")
        Toast.makeText(this, "Sealing (VDL takes a few seconds)…", Toast.LENGTH_SHORT).show()
    }

    private fun refreshContacts() {
        contacts = PairStore.contacts(this)
        val labels = if (contacts.isEmpty()) listOf("(no contacts — tap Pair)") else contacts.map { it.label }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spContact.adapter = adapter
    }

    private fun showPairingDialog() {
        val myKey = PairStore.myPublicHex(this)
        val qrPayload = PairStore.qrPayload(this)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16f * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }

        val myKeyLabel = TextView(this).apply {
            text = "Your pairing key (share out-of-band — QR photo, paper, another channel):"
            textSize = 12f
        }
        val myKeyView = TextView(this).apply {
            text = myKey
            typeface = android.graphics.Typeface.MONOSPACE
            textSize = 12f
            setTextIsSelectable(true)
            setPadding(0, 8, 0, 8)
        }
        val qrImage = ImageView(this).apply {
            setImageBitmap(makePairingQr(qrPayload))
            contentDescription = "Your Cockroach Chat pairing QR code"
            adjustViewBounds = true
            val size = (240f * resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply { gravity = Gravity.CENTER_HORIZONTAL }
        }
        val btnCopy = Button(this).apply {
            text = "Copy my key"
            setOnClickListener {
                val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cb.setPrimaryClip(ClipData.newPlainText("pairing_key", myKey))
                Toast.makeText(this@ChatActivity, "Copied", Toast.LENGTH_SHORT).show()
            }
        }
        val etLabel = EditText(this).apply { hint = "Contact name" }
        val etPeerKey = EditText(this).apply {
            hint = "Scan or paste their pairing key"
            typeface = android.graphics.Typeface.MONOSPACE
        }
        val btnScan = Button(this).apply {
            text = "Scan their QR"
            setOnClickListener { requestPairingCamera() }
        }

        container.addView(myKeyLabel)
        container.addView(qrImage)
        container.addView(myKeyView)
        container.addView(btnCopy)
        container.addView(TextView(this).apply {
            text = "\nAdd a contact:"
            textSize = 12f
        })
        container.addView(etLabel)
        container.addView(etPeerKey)
        container.addView(btnScan)

        pairingPeerKeyInput = etPeerKey

        AlertDialog.Builder(this)
            .setTitle("Pairing 🔒")
            .setView(container)
            .setPositiveButton("Add contact") { _, _ ->
                val label = etLabel.text.toString().trim()
                val peer = etPeerKey.text.toString()
                if (PairStore.addContact(this, label, peer)) {
                    refreshContacts()
                    Toast.makeText(this, "Paired with $label", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Bad name or key", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Close", null)
            .create()
            .also { dialog ->
                dialog.setOnDismissListener { pairingPeerKeyInput = null }
                dialog.show()
            }
    }

    private fun requestPairingCamera() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            launchPairingScanner()
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun launchPairingScanner() {
        scanQrLauncher.launch(
            ScanOptions()
                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                .setPrompt("Scan a Cockroach Chat pairing QR")
                .setBeepEnabled(false)
                .setOrientationLocked(false)
        )
    }

    private fun makePairingQr(payload: String): Bitmap {
        val size = (240f * resources.displayMetrics.density).toInt()
        val matrix = QRCodeWriter().encode(
            payload,
            BarcodeFormat.QR_CODE,
            size,
            size,
            mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M)
        )
        return Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).also { bitmap ->
            for (y in 0 until size) {
                for (x in 0 until size) {
                    bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    MeshState.messages.collect { msgs ->
                        rebuildBubbles(msgs)
                    }
                }
                launch {
                    MeshState.stats.collect { stats ->
                        tvChatStatus.text =
                            "epoch ${stats.epoch} · ${stats.neighborsThisEpoch} nearby · rx ${stats.totalHeard}"
                    }
                }
                launch {
                    MeshState.running.collect { running ->
                        if (swRun.isChecked != running) {
                            suppressSwitch = true
                            swRun.isChecked = running
                            suppressSwitch = false
                        }
                    }
                }
            }
        }
    }

    private fun rebuildBubbles(msgs: List<MsgRow>) {
        llChat.removeAllViews()
        for (row in msgs) {
            llChat.addView(bubble(row))
        }
        svChat.post { svChat.fullScroll(View.FOCUS_DOWN) }
    }

    private fun bubble(row: MsgRow): TextView {
        val dp8 = (8f * resources.displayMetrics.density + 0.5f).toInt()
        val dp24px = (24f * resources.displayMetrics.density + 0.5f).toInt()

        val (bright, dim) = when (row.tier) {
            SendTier.PRIVATE -> R.color.tier_private to R.color.tier_private_dim
            SendTier.LOCAL -> R.color.tier_local to R.color.tier_local_dim
            SendTier.BROADCAST -> R.color.tier_broadcast to R.color.tier_broadcast_dim
        }
        val color = androidx.core.content.ContextCompat.getColor(this, if (row.mine) bright else dim)

        val bg = GradientDrawable().apply {
            cornerRadius = 24f * resources.displayMetrics.density
            setColor(color)
        }

        val tv = TextView(this).apply {
            text = if (row.mine) {
                row.text
            } else {
                "[${row.markHexPrefix}${row.rssi?.let { " ${it}dBm" } ?: ""}]\n${row.text}"
            }
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(dp24px, dp8, dp24px, dp8)
            background = bg
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(dp8, dp8, dp8, dp8)
            gravity = if (row.mine) Gravity.END else Gravity.START
        }
        tv.layoutParams = params

        return tv
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
}

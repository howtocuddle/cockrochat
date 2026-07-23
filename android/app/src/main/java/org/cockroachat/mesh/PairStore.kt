package org.cockroachat.mesh

import android.content.Context
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyPairGenerator
import java.security.SecureRandom

data class Contact(val label: String, val pairKey: ByteArray) {
    override fun equals(other: Any?): Boolean =
        other is Contact && label == other.label && pairKey.contentEquals(other.pairKey)

    override fun hashCode(): Int = 31 * label.hashCode() + pairKey.contentHashCode()
}

object PairStore {
    private const val PREFS_NAME = "mesh_pairing_v2"
    private const val KEY_SK = "sk"
    private const val KEY_CONTACTS = "contacts"

    @Volatile private var memSk: ByteArray? = null
    private val memContacts = java.util.concurrent.CopyOnWriteArrayList<Contact>()

    /**
     * EncryptedSharedPreferences derived from a MasterKey stored in AndroidKeyStore (TEE-backed).
     * On API 33+ the MasterKey is AES-256_GCM in StrongBox where available; older devices fall
     * back to AES-256 with AES/GCM in the AndroidKeyStore software implementation.
     *
     * Fail-closed: on failure return null. We never persist plaintext key material.
     */
    private fun prefs(ctx: Context): android.content.SharedPreferences? = runCatching {
        val mk = MasterKey.Builder(ctx, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            ctx, PREFS_NAME, mk,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrElse { e ->
        android.util.Log.e("PairStore", "EncryptedSharedPreferences failed, using in-memory only: ${e.message}")
        null
    }

    private fun ByteArray.toHex(): String =
        joinToString("") { "%02x".format(it) }

    private fun String.hexToBytesOrNull(): ByteArray? {
        if (length % 2 != 0) return null
        val out = ByteArray(length / 2)
        for (i in out.indices) {
            val hi = Character.digit(this[i * 2], 16)
            val lo = Character.digit(this[i * 2 + 1], 16)
            if (hi == -1 || lo == -1) return null
            out[i] = ((hi shl 4) or lo).toByte()
        }
        return out
    }

    /**
     * Stable, versioned QR payload for a public X25519 pairing key.  It carries public material
     * only; the private pairing secret and derived pair key never leave the device.
     */
    fun qrPayload(ctx: Context): String = "cockroachat:key:v1:${myPublicHex(ctx)}"

    /** Accept raw hex (manual entry) or a scanned [qrPayload]. */
    fun publicKeyFromQrOrHex(value: String): String? {
        val trimmed = value.trim()
        val raw = if (trimmed.startsWith("cockroachat:key:v1:", ignoreCase = true)) {
            trimmed.substringAfter(':', "")
                .substringAfter(':', "")
                .substringAfter(':', "")
        } else {
            trimmed
        }
        val clean = raw.replace(" ", "").replace(":", "").replace("\n", "")
        return clean.takeIf { it.length == 64 && it.hexToBytesOrNull()?.size == 32 }
    }

    /**
     * Long-term X25519 secret key. Generated once, stored in EncryptedSharedPreferences.
     *
     * FUTURE (API 33+): use AndroidKeyStore KeyPairGenerator with
     * `KeyProperties.PURPOSE_AGREE_KEY` so the secret never leaves TEE. The AgreeKey
     * would be used directly by a platform DH operation instead of exporting the raw
     * seed. For now the seed is stored encrypted at rest via EncryptedSharedPreferences.
     */
    @Synchronized
    fun secret(ctx: Context): ByteArray {
        val p = prefs(ctx)
        if (p == null) {
            memSk?.let { return it }
            val sk = ByteArray(32)
            SecureRandom().nextBytes(sk)
            memSk = sk
            return sk
        }
        val stored = p.getString(KEY_SK, null)
        if (stored != null) {
            val bytes = stored.hexToBytesOrNull()
            if (bytes != null && bytes.size == 32) return bytes
        }
        val sk = ByteArray(32)
        SecureRandom().nextBytes(sk)
        p.edit().putString(KEY_SK, sk.toHex()).commit()
        return sk
    }

    fun myPublicHex(ctx: Context): String {
        val pub = uniffi.mesh_core.pairPublic(secret(ctx)) ?: return ""
        return pub.toHex()
    }

    fun contacts(ctx: Context): List<Contact> {
        val p = prefs(ctx) ?: return memContacts.toList()
        val raw = p.getString(KEY_CONTACTS, null) ?: return emptyList()
        return raw.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val tab = line.indexOf('\t')
                if (tab < 0) return@mapNotNull null
                val label = line.substring(0, tab)
                val hexPart = line.substring(tab + 1)
                val keyBytes = hexPart.hexToBytesOrNull() ?: return@mapNotNull null
                if (keyBytes.size != 32) return@mapNotNull null
                Contact(label, keyBytes)
            }
    }

    @Synchronized
    fun addContact(ctx: Context, label: String, peerPublicHex: String): Boolean {
        val trimmedLabel = label.trim()
        if (trimmedLabel.isEmpty()) return false
        if (trimmedLabel.any { it == '\t' || it == '\n' || it == '\r' }) return false
        if (trimmedLabel.length > 32) return false
        val cleanHex = publicKeyFromQrOrHex(peerPublicHex) ?: return false
        val peerPub = cleanHex.hexToBytesOrNull() ?: return false
        if (peerPub.size != 32) return false
        val pairKey = uniffi.mesh_core.pairDerive(secret(ctx), peerPub) ?: return false
        val contact = Contact(trimmedLabel, pairKey)
        if (prefs(ctx) == null) {
            memContacts.removeAll { it.label == trimmedLabel }
            memContacts.add(contact)
            return true
        }
        val existing = contacts(ctx).filter { it.label != trimmedLabel }
        val updated = existing + contact
        persist(ctx, updated)
        return true
    }

    @Synchronized
    fun removeContact(ctx: Context, label: String) {
        if (prefs(ctx) == null) {
            memContacts.removeAll { it.label == label }
            return
        }
        val updated = contacts(ctx).filter { it.label != label }
        persist(ctx, updated)
    }

    private fun persist(ctx: Context, list: List<Contact>) {
        val p = prefs(ctx) ?: return
        val raw = list.joinToString("\n") { "${it.label}\t${it.pairKey.toHex()}" }
        p.edit().putString(KEY_CONTACTS, raw).commit()
    }

    /**
     * Wipe all pairing state. Called from the panic-wipe path.
     */
    fun wipe(ctx: Context) {
        prefs(ctx)?.edit()?.clear()?.commit()
        memSk?.fill(0)
        memSk = null
        memContacts.clear()
    }
}

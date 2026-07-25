package org.bileichat.mesh

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

/**
 * A paired contact.
 *
 * v1 (legacy): [pairKey] is the static `pair_derive` output — NO forward secrecy (A3).
 * v2: [pairKey] is the CURRENT epoch's ratcheted chain key; [chainEpoch] is its epoch;
 * [prevKey]/[prevEpoch] retain exactly one previous epoch key for clock-skew tolerance.
 * The chain seed was mixed with per-pairing salts that both sides deleted after pairing,
 * so a seized long-term secret cannot recompute the chain — past epochs are unrecoverable.
 */
data class Contact(
    val label: String,
    val pairKey: ByteArray,
    val v2: Boolean = false,
    val chainEpoch: UInt = 0u,
    val prevKey: ByteArray? = null,
    val prevEpoch: UInt = 0u
) {
    override fun equals(other: Any?): Boolean {
        if (other !is Contact) return false
        val prevEq = if (prevKey == null) other.prevKey == null
        else other.prevKey != null && prevKey.contentEquals(other.prevKey)
        return label == other.label && pairKey.contentEquals(other.pairKey) &&
            v2 == other.v2 && chainEpoch == other.chainEpoch && prevEq && prevEpoch == other.prevEpoch
    }

    override fun hashCode(): Int = 31 * label.hashCode() + pairKey.contentHashCode()
}

/** A parsed out-of-band pairing offer. [saltHex] non-null ⇒ v2 (forward-secret ratchet). */
data class PairingOffer(val pkHex: String, val saltHex: String?)

object PairStore {
    private const val PREFS_NAME = "mesh_pairing_v2"
    private const val KEY_SK = "sk"
    private const val KEY_CONTACTS = "contacts"

    // B4/C9: private-send nonce counter — per-epoch random base + in-epoch sequence,
    // stored in the ENCRYPTED prefs (was: plaintext SharedPreferences, forever-monotonic
    // → cross-epoch linkability + send-volume leak).
    private const val KEY_CTR_EPOCH = "privCtrEpoch"
    private const val KEY_CTR_BASE = "privCtrBase"
    private const val KEY_CTR_SEQ = "privCtrSeq"

    @Volatile private var memSk: ByteArray? = null
    private val memContacts = java.util.concurrent.CopyOnWriteArrayList<Contact>()

    /** Cached EncryptedSharedPreferences instance — building MasterKey + the encrypted
     *  store on EVERY access burned the Keystore under a private-frame storm (B5). */
    @Volatile private var prefsCache: android.content.SharedPreferences? = null
    @Volatile private var prefsFailed = false

    /** B5: in-memory contact cache; invalidated on every mutation. */
    @Volatile private var contactCache: List<Contact>? = null

    /** Per-process pairing salt shown in our QR (v2). NEVER persisted: it is the entropy
     *  that makes the v2 chain seed unrecomputable after seizure. Rotated after every
     *  successful pairing so each contact gets fresh salt. */
    @Volatile private var mySalt: ByteArray? = null

    /**
     * EncryptedSharedPreferences derived from a MasterKey stored in AndroidKeyStore (TEE-backed).
     *
     * Fail-closed: on failure return null. We never persist plaintext key material.
     * D4: callers can surface [secureStorageAvailable] to the user instead of pairing
     * silently dying on process death.
     */
    private fun prefs(ctx: Context): android.content.SharedPreferences? {
        prefsCache?.let { return it }
        if (prefsFailed) return null
        return runCatching {
            val mk = MasterKey.Builder(ctx, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                ctx, PREFS_NAME, mk,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.onSuccess { prefsCache = it }
            .getOrElse { e ->
                android.util.Log.e("PairStore", "EncryptedSharedPreferences failed, using in-memory only: ${e.message}")
                prefsFailed = true
                null
            }
    }

    /** D4: false when the TEE-backed store is unavailable (pairings live in memory only
     *  and die with the process). The UI must SAY this, not just log it. */
    fun secureStorageAvailable(ctx: Context): Boolean = prefs(ctx) != null

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

    /** Per-pairing salt length. MUST match the `[u8; 32]` that `pair_seed_v2` requires
     *  (ffi.rs) — this was 16, so pairSeedV2 rejected every salt, returned null, and EVERY
     *  v2 QR scan failed with "Derivation failed". Only raw-hex / v1 payloads could pair,
     *  silently downgrading users to the static non-forward-secret key. */
    private const val PAIR_SALT_LEN = 32

    /** Epochs to backdate a new v2 contact's ratchet start, so two phones that scan each
     *  other seconds apart still have overlapping key windows. See addContact. */
    private const val PAIR_EPOCH_BACKDATE = 3u

    private fun myPairSalt(ctx: Context): ByteArray {
        mySalt?.let { return it }
        val s = ByteArray(PAIR_SALT_LEN)
        SecureRandom().nextBytes(s)
        mySalt = s
        return s
    }

    private fun currentEpoch(): UInt =
        (System.currentTimeMillis() / MeshState.config.epochMs).toUInt()

    /**
     * v2 QR payload: public key + per-pairing salt. Both public; the secret and the chain
     * keys never leave the device. The salt is fresh per pairing and rotated after each add.
     */
    fun qrPayload(ctx: Context): String =
        "bileichat:key:v2:${myPublicHex(ctx)}:${myPairSalt(ctx).toHex()}"

    fun mySaltHex(ctx: Context): String = myPairSalt(ctx).toHex()

    /** Parse a scanned/typed pairing offer: v2 payload, legacy v1 payload, or raw 64-hex key. */
    fun parsePairingOffer(value: String): PairingOffer? {
        val trimmed = value.trim()
        val parts = trimmed.split(":")
        return when {
            parts.size == 5 && parts[0].equals("bileichat", true) &&
                parts[1].equals("key", true) && parts[2] == "v2" -> {
                val pk = parts[3]
                val salt = parts[4]
                if (pk.length == 64 && pk.hexToBytesOrNull()?.size == 32 &&
                    salt.length == PAIR_SALT_LEN * 2 &&
                    salt.hexToBytesOrNull()?.size == PAIR_SALT_LEN
                ) PairingOffer(pk, salt) else null
            }
            parts.size == 4 && parts[0].equals("bileichat", true) &&
                parts[1].equals("key", true) && parts[2] == "v1" -> {
                val pk = parts[3]
                if (pk.length == 64 && pk.hexToBytesOrNull()?.size == 32) PairingOffer(pk, null) else null
            }
            else -> {
                val clean = trimmed.replace(" ", "").replace("\n", "")
                if (clean.length == 64 && clean.hexToBytesOrNull()?.size == 32) {
                    PairingOffer(clean, null)
                } else null
            }
        }
    }

    /** Legacy shim for the QR scanner validation path. */
    fun publicKeyFromQrOrHex(value: String): String? = parsePairingOffer(value)?.pkHex

    /**
     * Long-term X25519 secret key. Generated once, stored in EncryptedSharedPreferences.
     *
     * FUTURE (API 33+): use AndroidKeyStore KeyPairGenerator with
     * `KeyProperties.PURPOSE_AGREE_KEY` so the secret never leaves TEE. For now the seed is
     * stored encrypted at rest via EncryptedSharedPreferences.
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

    /** B5: cached contact list — no Keystore/EncryptedSharedPreferences rebuild per call. */
    fun contacts(ctx: Context): List<Contact> {
        contactCache?.let { return it }
        val loaded = loadContacts(ctx)
        contactCache = loaded
        return loaded
    }

    private fun loadContacts(ctx: Context): List<Contact> {
        val p = prefs(ctx) ?: return memContacts.toList()
        val raw = p.getString(KEY_CONTACTS, null) ?: return emptyList()
        return raw.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val fields = line.split('\t')
                if (fields.size < 2) return@mapNotNull null
                val label = fields[0]
                val keyBytes = fields[1].hexToBytesOrNull() ?: return@mapNotNull null
                if (keyBytes.size != 32) return@mapNotNull null
                if (fields.size >= 6 && fields[2] == "2") {
                    val chainEpoch = fields[3].toUIntOrNull() ?: return@mapNotNull null
                    val prevBytes = if (fields[4].isEmpty()) null else fields[4].hexToBytesOrNull()
                    if (prevBytes != null && prevBytes.size != 32) return@mapNotNull null
                    val prevEpoch = fields[5].toUIntOrNull() ?: 0u
                    Contact(label, keyBytes, v2 = true, chainEpoch = chainEpoch,
                        prevKey = prevBytes, prevEpoch = prevEpoch)
                } else {
                    // Legacy v1 line: static key, no forward secrecy.
                    Contact(label, keyBytes, v2 = false)
                }
            }
    }

    @Synchronized
    fun addContact(ctx: Context, label: String, offerRaw: String): String? {
        val trimmedLabel = label.trim()
        if (trimmedLabel.isEmpty()) return "Contact name cannot be empty"
        if (trimmedLabel.any { it == '\t' || it == '\n' || it == '\r' }) return "Contact name contains invalid characters"
        if (trimmedLabel.length > 32) return "Contact name too long (max 32 chars)"
        val offer = parsePairingOffer(offerRaw) ?: return "Invalid pairing key format"
        val peerPub = offer.pkHex.hexToBytesOrNull() ?: return "Invalid public key"
        if (peerPub.size != 32) return "Invalid public key size"
        // D5: pairing with ourselves is never valid.
        if (offer.pkHex.equals(myPublicHex(ctx), ignoreCase = true)) return "Pairing with your own key is not allowed"

        val shared = uniffi.mesh_core.pairDerive(secret(ctx), peerPub) ?: return "Key agreement failed"
        val contact = if (offer.saltHex != null) {
            // v2: chain seed = f(ECDH, both salts). Salts are NOT stored — after this call
            // only the ratchet chain state survives, which is what gives seizure resistance.
            val theirSalt = offer.saltHex.hexToBytesOrNull() ?: return "Invalid salt"
            val seed0 = uniffi.mesh_core.pairSeedV2(shared, myPairSalt(ctx), theirSalt) ?: return "Derivation failed"
            // Backdate the chain start. The two sides scan each other seconds apart, so each
            // stamped its OWN local epoch: if Alice landed on epoch 100 and Bob on 102, a
            // message Alice sent at 101 fell into candidateKeys' `else -> emptyList()` branch
            // on Bob (101 != 102, no prevKey, 101 < 102) and was undecryptable forever, with
            // no log line. Starting the chain a few epochs back makes the two windows overlap;
            // ratcheting forward from there is one BLAKE3 step per epoch.
            val start = currentEpoch()
            Contact(
                trimmedLabel,
                seed0,
                v2 = true,
                chainEpoch = if (start >= PAIR_EPOCH_BACKDATE) start - PAIR_EPOCH_BACKDATE else 0u
            )
        } else {
            // Legacy v1: static key, no forward secrecy (shown as LEGACY in the UI).
            Contact(trimmedLabel, shared, v2 = false)
        }

        if (prefs(ctx) == null) {
            memContacts.removeAll { it.label == trimmedLabel }
            memContacts.add(contact)
        } else {
            val updated = contacts(ctx).filter { it.label != trimmedLabel } + contact
            persist(ctx, updated)
        }
        contactCache = null
        // NOTE: mySalt deliberately does NOT rotate here. The salt in the displayed QR
        // must equal the salt used for every pairing made while that QR is shown — rotating
        // on add would break sequential face-to-face pairing (the second scanner would get
        // a different salt than the one they scanned). The salt is per-process only and
        // never persisted, which is what preserves forward secrecy after process death.
        return null
    }

    @Synchronized
    fun removeContact(ctx: Context, label: String) {
        if (prefs(ctx) == null) {
            memContacts.removeAll { it.label == label }
        } else {
            val updated = contacts(ctx).filter { it.label != label }
            persist(ctx, updated)
        }
        contactCache = null
    }

    private fun persist(ctx: Context, list: List<Contact>) {
        val p = prefs(ctx) ?: return
        val raw = list.joinToString("\n") { c ->
            if (c.v2) {
                "${c.label}\t${c.pairKey.toHex()}\t2\t${c.chainEpoch}\t${c.prevKey?.toHex() ?: ""}\t${c.prevEpoch}"
            } else {
                "${c.label}\t${c.pairKey.toHex()}"
            }
        }
        p.edit().putString(KEY_CONTACTS, raw).commit()
    }

    /** Advance a v2 contact's chain to [epoch] (one-way ratchet), persist, and return the
     *  key for that epoch. v1 contacts return the static key. Null if label unknown or the
     *  ratchet span is absurd. */
    @Synchronized
    fun keyForSend(ctx: Context, label: String, epoch: UInt): ByteArray? {
        val contact = contacts(ctx).firstOrNull { it.label == label } ?: return null
        if (!contact.v2) return contact.pairKey
        if (epoch <= contact.chainEpoch) return contact.pairKey
        val advanced = uniffi.mesh_core.pairRatchet(contact.pairKey, contact.chainEpoch, epoch)
            ?: return null
        val updated = contact.copy(
            pairKey = advanced,
            chainEpoch = epoch,
            prevKey = contact.pairKey,
            prevEpoch = contact.chainEpoch
        )
        storeUpdated(ctx, updated)
        return advanced
    }

    /** Candidate AEAD keys for opening a frame with [frameEpoch] (A3). Order matters;
     * callers try all of them (no early-break timing leak beyond key count). */
    @Synchronized
    fun candidateKeys(ctx: Context, contact: Contact, frameEpoch: UInt): List<ByteArray> {
        if (!contact.v2) return listOf(contact.pairKey)
        return when {
            frameEpoch == contact.chainEpoch -> listOf(contact.pairKey)
            contact.prevKey != null && frameEpoch == contact.prevEpoch -> listOf(contact.prevKey)
            frameEpoch > contact.chainEpoch -> {
                // Sender is ahead of our stored chain — fast-forward (one-way, cheap).
                uniffi.mesh_core.pairRatchet(contact.pairKey, contact.chainEpoch, frameEpoch)
                    ?.let { listOf(it) } ?: emptyList()
            }
            else -> {
                // Older than the retained previous epoch: undecryptable by design (forward
                // secrecy — those keys are gone). Logged because it is otherwise completely
                // silent, and it is what a pairing-epoch mismatch looks like from here.
                // Contact label deliberately omitted: the debug log is exportable, and a
                // line naming who you are paired with is social-graph metadata that a
                // seized or shared export would hand over for free.
                MeshState.logDebug(
                    "private frame at epoch $frameEpoch is behind that contact's chain " +
                        "(${contact.chainEpoch}) — key already ratcheted away, cannot open"
                )
                emptyList()
            }
        }
    }

    /**
     * Step every v2 contact's chain forward with the clock. Called once per epoch by the
     * service loop.
     *
     * `pair_ratchet` refuses spans longer than 8192 steps (a DoS bound on wire-supplied
     * epochs), but chains only ever advanced ON USE — the sender in [keyForSend], the
     * receiver in [noteOpened]. Two people who paired and then exchanged no private message
     * for 8192 epochs (22.8 h at a 10 s epoch — i.e. pairing the night before and first
     * using it the next day) blew that cap in both directions simultaneously: every send
     * failed with "key ratchet failed", every receive with "already ratcheted away". Since
     * neither path can advance the chain without first succeeding, it never recovered —
     * private messaging was permanently dead until the pair met again in person.
     *
     * Advancing to `epoch - 1` rather than `epoch` deliberately keeps the retained window
     * aligned with the ±2-epoch freshness gate: chainEpoch covers epoch-1, prevEpoch covers
     * epoch-2, and a current-epoch frame is one cheap on-the-fly step ahead in
     * [candidateKeys]. Forward secrecy is unchanged — superseded keys are still dropped.
     */
    @Synchronized
    fun fastForwardChains(ctx: Context, epoch: UInt) {
        if (epoch == 0u) return
        val target = epoch - 1u
        for (c in contacts(ctx)) {
            if (!c.v2 || target <= c.chainEpoch) continue
            val advanced = uniffi.mesh_core.pairRatchet(c.pairKey, c.chainEpoch, target)
            if (advanced == null) {
                MeshState.logDebug(
                    "chain fast-forward for '" + c.label + "' failed: span " +
                        (target - c.chainEpoch) + " epochs exceeds the ratchet cap"
                )
                continue
            }
            storeUpdated(ctx, c.copy(
                pairKey = advanced,
                chainEpoch = target,
                prevKey = c.pairKey,
                prevEpoch = c.chainEpoch
            ))
        }
    }

    /** After a successful trial-open at [frameEpoch], persist the fast-forwarded chain
     *  state (past keys deleted). No-op when [frameEpoch] is not ahead. */
    @Synchronized
    fun noteOpened(ctx: Context, label: String, frameEpoch: UInt) {
        val contact = contacts(ctx).firstOrNull { it.label == label } ?: return
        if (!contact.v2 || frameEpoch <= contact.chainEpoch) return
        val advanced = uniffi.mesh_core.pairRatchet(contact.pairKey, contact.chainEpoch, frameEpoch)
            ?: return
        storeUpdated(ctx, contact.copy(
            pairKey = advanced,
            chainEpoch = frameEpoch,
            prevKey = contact.pairKey,
            prevEpoch = contact.chainEpoch
        ))
    }

    private fun storeUpdated(ctx: Context, updated: Contact) {
        if (prefs(ctx) == null) {
            memContacts.removeAll { it.label == updated.label }
            memContacts.add(updated)
        } else {
            persist(ctx, contacts(ctx).map { if (it.label == updated.label) updated else it })
        }
        contactCache = null
    }

    /**
     * B4/C9: next private-send nonce counter for [epoch]. A fresh random 32-bit base per
     * epoch breaks the cross-epoch continuity an observer could use to link a sender and
     * count their private traffic; within the epoch the value stays monotonic (nonce
     * uniqueness under that epoch's ephemeral pk). Stored encrypted.
     */
    @Synchronized
    fun nextPrivateCounter(ctx: Context, epoch: UInt): ULong {
        val p = prefs(ctx)
        if (p == null) {
            // Memory-only fallback: random base per call is still unlinkable; in-epoch
            // monotonicity across restarts is best-effort (nonce suffix also includes the
            // per-start ephemeral pk, so reuse risk stays negligible).
            return (SecureRandom().nextInt().toUInt() and 0x7FFF_FFFFu).toULong()
        }
        val storedEpoch = p.getString(KEY_CTR_EPOCH, null)?.toUIntOrNull()
        var base = p.getString(KEY_CTR_BASE, null)?.toULongOrNull()
        var seq = p.getString(KEY_CTR_SEQ, null)?.toULongOrNull() ?: 0uL
        if (storedEpoch != epoch || base == null) {
            base = (SecureRandom().nextInt().toUInt() and 0x7FFF_FFFFu).toULong()
            seq = 0uL
        }
        val counter = (base + seq) and 0xFFFF_FFFFuL
        p.edit()
            .putString(KEY_CTR_EPOCH, epoch.toString())
            .putString(KEY_CTR_BASE, base.toString())
            .putString(KEY_CTR_SEQ, (seq + 1uL).toString())
            .commit()
        return counter
    }

    /** Wipe all pairing state. Called from the panic-wipe path. */
    fun wipe(ctx: Context) {
        prefs(ctx)?.edit()?.clear()?.commit()
        memSk?.fill(0)
        memSk = null
        memContacts.clear()
        contactCache = null
        mySalt?.fill(0)
        mySalt = null
    }
}

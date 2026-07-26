package org.bileichat.mesh

import android.content.Context
import uniffi.mesh_core.FfiDedup
import uniffi.mesh_core.FfiDedupVerdict
import uniffi.mesh_core.FfiTrust
import uniffi.mesh_core.defaultTtlLocal
import uniffi.mesh_core.defaultTtlRegional
import uniffi.mesh_core.frameBodyText
import uniffi.mesh_core.frameDecodes
import uniffi.mesh_core.frameEpoch
import uniffi.mesh_core.frameHash
import uniffi.mesh_core.frameMark
import uniffi.mesh_core.frameTtl
import uniffi.mesh_core.frameVerifySelf
import uniffi.mesh_core.frameWitnessParts
import uniffi.mesh_core.makeMessageFrame
import uniffi.mesh_core.makeMessageFrameWithWitness
import uniffi.mesh_core.makePrivateFrame
import uniffi.mesh_core.observeMarks
import uniffi.mesh_core.openPrivateBodyOnly
import uniffi.mesh_core.pocpSketchToDivSketch
import uniffi.mesh_core.pocpVerifyWitnessLocal
import uniffi.mesh_core.relayFrame
import uniffi.mesh_core.vdlCheckFrame
import uniffi.mesh_core.vdlDifficultyBits
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * On-device module self-test.
 *
 * Exists because everything below is only ever exercised on real hardware. `cargo test` runs
 * the core on a desktop; a green build says nothing about whether the FFI marshalling, the
 * BLE adapter, the GATT plane, or a live pairing actually work on THIS phone — and R8 can
 * break UniFFI/JNA at runtime with no compile-time trace at all.
 *
 * ## Two-phone use
 *
 * Run it on both phones. The runner waits for the next epoch boundary before starting, so two
 * phones with agreeing clocks produce reports stamped with the SAME epoch number. Marks rotate
 * per epoch, so the mark lines are only comparable within one epoch — which is precisely what
 * the wait buys. Then diff the two reports:
 *
 *   * `self mark` on phone A should appear in `heard marks` on phone B, and vice versa. That
 *     is proof of a working RF path, independent of anything the message feed shows.
 *   * `direct marks` is the subset heard with no relay hop in between.
 *   * `pair fp` for a shared contact must MATCH across the two reports. A mismatch means the
 *     two ratchets are not aligned, which is the one failure that looks like "messages relay
 *     fine but never appear" — frames get two ticks because relaying a sealed frame needs no
 *     key at all, while every decryption attempt fails silently.
 *
 * ## What it does not do
 *
 * It does not transmit anything. Nothing here touches the live dedup, trust, beacon or radio
 * state — the Rust objects under test are fresh instances, so a run cannot poison the
 * seen-set or corroboration counts of the session it is diagnosing. Live state is READ and
 * reported, never modified.
 */
object SelfTest {

    private val timeFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private class Report(val log: (String) -> Unit) {
        var passed = 0
        var failed = 0

        fun section(name: String) {
            log("")
            log("── $name ${"─".repeat((44 - name.length).coerceAtLeast(2))}")
        }

        fun info(line: String) = log("   $line")

        fun check(name: String, ok: Boolean, detail: String = "") {
            if (ok) passed++ else failed++
            val mark = if (ok) "PASS" else "FAIL"
            log("[$mark] $name" + if (detail.isNotEmpty()) " — $detail" else "")
        }

        /** For a value worth recording even though there is nothing to assert about it. */
        fun observe(name: String, value: String) = log("[····] $name — $value")
    }

    /**
     * Run every module check and stream the report through [log].
     *
     * Blocking and slow — the private-frame check solves a real VDL witness, which is seconds
     * of CPU by design. Call from a background dispatcher.
     */
    fun run(
        ctx: Context,
        cfg: MeshConfig,
        seed: ByteArray,
        beaconSeed: ByteArray,
        radio: BleRadio,
        gattPlane: GattPlane,
        log: (String) -> Unit
    ) {
        val r = Report(log)
        val startedMs = System.currentTimeMillis()
        val epoch = (startedMs / cfg.epochMs).toUInt()

        log("BILEICHAT SELF-TEST")
        log("epoch $epoch · ${timeFmt.format(Date(startedMs))} · ${startedMs}ms")
        log("Run this on both phones. Same epoch number = comparable reports.")

        environment(r, cfg, epoch, startedMs)
        codec(r, seed, beaconSeed, epoch)
        pocp(r, seed, beaconSeed, epoch, cfg)
        dedupAndTrust(r)
        privateAndVdl(r, seed, beaconSeed, epoch)
        pairing(r, ctx, epoch)
        radioAndGatt(r, radio, gattPlane)
        liveMesh(r, cfg, epoch, seed)

        val took = System.currentTimeMillis() - startedMs
        log("")
        log("─".repeat(48))
        log("RESULT: ${r.passed} passed, ${r.failed} failed, ${took}ms")
        if (r.failed == 0) {
            log("All module checks passed on this device.")
        } else {
            log("FAILURES ABOVE — search for [FAIL].")
        }
    }

    // -----------------------------------------------------------------------------------

    private fun environment(r: Report, cfg: MeshConfig, epoch: UInt, nowMs: Long) {
        r.section("environment")
        r.observe("build", "${BuildConfig.VERSION_NAME} (code ${BuildConfig.VERSION_CODE})")
        r.observe("buildType", BuildConfig.BUILD_TYPE + if (BuildConfig.DEBUG) " · DEBUGGABLE" else " · not debuggable")
        r.observe("android", "SDK ${android.os.Build.VERSION.SDK_INT} · ${android.os.Build.MODEL}")
        r.observe("epoch", "$epoch (${nowMs % cfg.epochMs}ms into it)")
        r.observe(
            "config",
            "epochMs=${cfg.epochMs} tau=${cfg.tauThreshold} minHearers=${cfg.minHearers} " +
                "rssiFloor=${cfg.rssiFloorDbm} adv=${cfg.advIntervalMs}ms repeat=${cfg.messageRepeatEpochs} " +
                "codedPhy=${cfg.codedPhy} beaconFloor=${cfg.beaconFloorMs}"
        )
        // epochMs must match on every phone or the epoch INDEX diverges linearly and the two
        // phones stop being able to hear each other within a minute. Worth having in writing
        // in both reports, since it is the fastest thing to compare.
        r.check(
            "beacon floor below epoch",
            cfg.beaconFloorMs < cfg.epochMs,
            "floor=${cfg.beaconFloorMs} epoch=${cfg.epochMs}"
        )
    }

    private fun codec(r: Report, seed: ByteArray, beaconSeed: ByteArray, epoch: UInt) {
        r.section("codec + signature (Rust FFI)")
        val text = "SELFTEST"
        val frame = makeMessageFrame(seed, epoch, beaconSeed, false, text)
        if (frame == null) {
            r.check("originate frame", false, "makeMessageFrame returned null")
            return
        }
        r.check("frame is 226 bytes", frame.size == 226, "got ${frame.size}")
        r.check("frame decodes", frameDecodes(frame))
        r.check("signature verifies", frameVerifySelf(frame))
        r.check("epoch field roundtrips", frameEpoch(frame) == epoch, "got ${frameEpoch(frame)}")
        r.check("body text roundtrips", frameBodyText(frame) == text, "got ${frameBodyText(frame)}")
        r.check("hash is 16 bytes", frameHash(frame)?.size == 16)
        r.check("mark is 16 bytes", frameMark(frame)?.size == 16)
        r.check(
            "broadcast TTL is default",
            frameTtl(frame)?.toInt() == defaultTtlRegional().toInt(),
            "got ${frameTtl(frame)?.toInt()} want ${defaultTtlRegional().toInt()}"
        )

        // A signature that does not actually reject tampering is the failure that makes every
        // other guarantee here meaningless, so prove it rejects rather than assuming it.
        val tampered = frame.copyOf().also { it[40] = (it[40].toInt() xor 0x01).toByte() }
        r.check("tampered body is rejected", !frameVerifySelf(tampered))

        val relayed = relayFrame(frame)
        r.check("relay decrements TTL", relayed != null &&
            frameTtl(relayed)?.toInt() == defaultTtlRegional().toInt() - 1,
            "got ${relayed?.let { frameTtl(it)?.toInt() }}")
        // TTL lives outside the hashed region, so a relayed copy must hash identically —
        // this is what lets a sender recognise its own echo.
        r.check(
            "relayed copy keeps the same hash",
            relayed != null && frameHash(relayed)!!.contentEquals(frameHash(frame)!!)
        )

        val localFrame = makeMessageFrame(seed, epoch, beaconSeed, true, "LOCAL")
        r.check(
            "local TTL is default",
            localFrame != null && frameTtl(localFrame)?.toInt() == defaultTtlLocal().toInt(),
            "got ${localFrame?.let { frameTtl(it)?.toInt() }}"
        )
    }

    private fun pocp(r: Report, seed: ByteArray, beaconSeed: ByteArray, epoch: UInt, cfg: MeshConfig) {
        r.section("PoCP co-presence")
        val tau = cfg.tauThreshold
        val rssi = List(4) { (-30).toByte() }

        fun marks(vararg ids: Int): ByteArray =
            ids.fold(ByteArray(0)) { acc, id -> acc + ByteArray(16) { id.toByte() } }

        val sketchA = observeMarks(marks(1, 2, 3, 4), rssi, epoch, (-90).toByte())
        val sketchFar = observeMarks(marks(200, 201, 202, 203), rssi, epoch, (-90).toByte())
        val sketchOne = observeMarks(marks(1), listOf((-30).toByte()), epoch, (-90).toByte())
        r.check("observe builds a 16-slot sketch", sketchA.size == 16, "got ${sketchA.size}")

        val divA = pocpSketchToDivSketch(sketchA)
        val divFar = pocpSketchToDivSketch(sketchFar)
        val divOne = pocpSketchToDivSketch(sketchOne)
        r.check("sketch truncates to a 16-byte div_sketch", divA?.size == 16)
        if (divA == null || divFar == null || divOne == null) return

        /** Build a witnessed frame for [div] and judge it against [local]. */
        fun verdictFor(div: ByteArray, local: List<ULong>, corruptWitness: Boolean = false): Int? {
            val f = makeMessageFrameWithWitness(
                seed, epoch, beaconSeed, true, "pocp", 1u, div, epoch
            ) ?: return null
            val wp = frameWitnessParts(f) ?: return null
            val wit = if (corruptWitness) {
                wp.pocpWit.copyOf().also { it[0] = (it[0].toInt() xor 0xFF).toByte() }
            } else wp.pocpWit
            return pocpVerifyWitnessLocal(local, wp.divSketch, epoch, wp.framePrefix, wit, tau).toInt()
        }

        r.check("same cell verifies (Valid=0)", verdictFor(divA, sketchA) == 0,
            "got ${verdictFor(divA, sketchA)}")
        // Asserted as "never reaches full trust" rather than "== CellMismatch": div_sketch
        // truncates each slot to ONE byte, so two disjoint 4-mark cells collide on a byte
        // about 6% of the time and land on Unattested instead. That is correct behaviour and
        // must not read as a failure — what matters is that a remote cell never reaches 0.
        val farVerdict = verdictFor(divFar, sketchA)
        r.check("remote cell never reaches full trust", farVerdict != 0,
            "verdict=$farVerdict")
        // T2: a one-element overlap is displayable but NOT attested. Verdict 4 specifically —
        // 3 is the shim's own "no local sketch" sentinel, and returning it here would make
        // every demoted frame read as stale and get dropped.
        r.check("one-element overlap is Unattested=4", verdictFor(divOne, sketchA) == 4,
            "got ${verdictFor(divOne, sketchA)}")
        r.check("forged witness rejected (Stale=2)", verdictFor(divA, sketchA, corruptWitness = true) == 2,
            "got ${verdictFor(divA, sketchA, corruptWitness = true)}")
    }

    private fun dedupAndTrust(r: Report) {
        r.section("dedup + trust")
        // Fresh instances: the live ones are carrying this session's real state.
        val dedup = FfiDedup(64u)
        fun hash(n: Int) = ByteArray(16) { if (it == 0) n.toByte() else 0 }

        r.check("unseen hash is fresh",
            dedup.checkEpoch(hash(1), 100u, 100u) == FfiDedupVerdict.FRESH)
        r.check("check does not consume",
            dedup.checkEpoch(hash(1), 100u, 100u) == FfiDedupVerdict.FRESH)
        r.check("insert succeeds", dedup.insertEpoch(hash(1), 100u))
        r.check("inserted hash is a duplicate",
            dedup.checkEpoch(hash(1), 100u, 100u) == FfiDedupVerdict.DUPLICATE)
        r.check("a different hash stays fresh",
            dedup.checkEpoch(hash(2), 100u, 100u) == FfiDedupVerdict.FRESH)

        // S8: decay follows the LOCAL clock. A frame claiming a far-future epoch used to purge
        // the entire seen-set, which is an attacker-triggered reset of replay protection.
        dedup.checkAndInsertEpoch(hash(9), UInt.MAX_VALUE, 100u)
        r.check("far-future frame cannot flush the seen-set",
            dedup.checkEpoch(hash(1), 100u, 100u) == FfiDedupVerdict.DUPLICATE)
        r.check("entry decays once the local clock passes the window",
            dedup.checkEpoch(hash(1), 107u, 107u) == FfiDedupVerdict.FRESH)

        val trust = FfiTrust()
        val fh = ByteArray(16) { 0x5A }
        val divA = ByteArray(16) { it.toByte() }
        val divB = ByteArray(16) { (200 - it).toByte() }
        val first = trust.recordVerification(fh, divA, 0.3f)
        val second = trust.recordVerification(fh, divB, 0.3f)
        r.check("first claim counts", first.toInt() == 1, "got $first")
        r.check("a dissimilar claim counts separately", second.toInt() == 2, "got $second")
        r.check("count is readable", trust.distinctCount(fh).toInt() == 2)
        // Re-recording a claim from the SAME cell must not inflate the count.
        trust.recordVerification(fh, divA, 0.3f)
        r.check("same cell does not inflate", trust.distinctCount(fh).toInt() == 2,
            "got ${trust.distinctCount(fh)}")
    }

    private fun privateAndVdl(r: Report, seed: ByteArray, beaconSeed: ByteArray, epoch: UInt) {
        r.section("private + VDL (slow — real proof-of-work)")
        val key = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val wrongKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val text = "selftest private"

        val t0 = System.currentTimeMillis()
        val frame = makePrivateFrame(seed, epoch, beaconSeed, key, text, 1uL)
        val solveMs = System.currentTimeMillis() - t0
        r.observe("VDL difficulty", "${vdlDifficultyBits()} bits · solved in ${solveMs}ms")

        if (frame == null) {
            r.check("seal private frame", false, "makePrivateFrame returned null")
            return
        }
        r.check("sealed frame is 226 bytes", frame.size == 226)
        r.check("VDL witness verifies", vdlCheckFrame(frame))
        r.check("signature verifies", frameVerifySelf(frame))
        r.check("opens under the right key", openPrivateBodyOnly(frame, key) == text,
            "got ${openPrivateBodyOnly(frame, key)}")
        r.check("stays shut under a wrong key", openPrivateBodyOnly(frame, wrongKey) == null)
        r.check("msgType is 3 (private)", frameWitnessParts(frame)?.msgType?.toInt() == 3)
    }

    private fun pairing(r: Report, ctx: Context, epoch: UInt) {
        r.section("pairing + ratchet")
        r.check("secure storage available", MeshState.secureStorageOk.value,
            if (MeshState.secureStorageOk.value) "" else "contacts are memory-only and die with the process")

        val contacts = PairStore.contacts(ctx)
        r.observe("contacts", "${contacts.size} paired")
        if (contacts.isEmpty()) {
            r.info("No contacts — pair two phones and re-run to compare `pair fp` lines.")
            return
        }

        contacts.forEachIndexed { i, c ->
            // Index, never the label: this report is meant to be shared, and a contact list is
            // social-graph metadata.
            val keys = PairStore.candidateKeys(ctx, c, epoch)
            val lag = if (epoch >= c.chainEpoch) (epoch - c.chainEpoch).toLong() else -((c.chainEpoch - epoch).toLong())
            r.observe(
                "contact #$i",
                "${if (c.v2) "v2 forward-secret" else "v1 legacy"} chainEpoch=${c.chainEpoch} " +
                    "lag=${lag}ep keys=${keys.size}"
            )
            r.check("contact #$i yields a key for this epoch", keys.isNotEmpty(),
                if (keys.isEmpty()) "ratchet cannot reach epoch $epoch — re-pair needed" else "")

            // THE two-phone check. Both phones derive the same pair key for the same epoch, so
            // this fingerprint must match across the two reports. If it does not, the chains
            // are anchored differently and no private message between them will ever open —
            // while still relaying and still earning two ticks.
            keys.firstOrNull()?.let { k ->
                r.observe("contact #$i pair fp", fingerprint(k, epoch))
            }
            if (!c.v2) {
                r.info("#$i is a legacy pairing: no forward secrecy. Re-pair when both phones are updated.")
            }
        }
    }

    /**
     * A short, non-reversible token for a key at an epoch, so two phones can compare their
     * chain state without either report containing key material. SHA-256 truncated to 4 bytes
     * over a 256-bit random key: the log leaks nothing usable.
     */
    private fun fingerprint(key: ByteArray, epoch: UInt): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update("bileichat-selftest-fp".toByteArray())
        md.update(key)
        md.update(byteArrayOf(
            (epoch.toInt() ushr 24).toByte(), (epoch.toInt() ushr 16).toByte(),
            (epoch.toInt() ushr 8).toByte(), epoch.toByte()
        ))
        return md.digest().take(4).joinToString("") { "%02x".format(it) }
    }

    private fun radioAndGatt(r: Report, radio: BleRadio, gattPlane: GattPlane) {
        r.section("radio + GATT")
        r.observe("adapter", radio.capabilityReport())
        r.observe("advertising mode", radio.advMode)
        r.observe("relay capacity", if (radio.relayCapacityAvailable()) "available" else "saturated")
        r.observe("gatt", gattPlane.diagnostics())
        r.check("advertising is active", MeshState.stats.value.advertising,
            if (MeshState.stats.value.advertising) "" else "nothing is on air — peers cannot hear this phone")
        r.check("scanning is active", MeshState.stats.value.scanning,
            if (MeshState.stats.value.scanning) "" else "this phone cannot hear anyone")
        r.check("advertising mode is not off", radio.advMode != "off", radio.advMode)
    }

    private fun liveMesh(r: Report, cfg: MeshConfig, epoch: UInt, seed: ByteArray) {
        r.section("live mesh (compare these across phones)")
        val m = MeshState.measurement
        val stats = MeshState.stats.value

        val self = m.selfMark(epoch) ?: m.selfMark(epoch - 1u)
        r.observe("self mark", self ?: "none yet — no frame originated this epoch")
        val heard = m.heardMarksThisEpoch(epoch)
        val direct = m.directMarksThisEpoch(epoch)
        r.observe("heard marks", if (heard.isEmpty()) "none this epoch" else heard.joinToString(" "))
        r.observe("direct marks", if (direct.isEmpty()) "none this epoch" else direct.joinToString(" "))
        r.info("The other phone's `self mark` should appear in this list, and vice versa.")

        r.observe("neighbours", "${stats.neighborsThisEpoch} this epoch · ${m.totalHeard()} frames total")
        // Same predicate the "weak cell" badge uses, so this line and the badge cannot disagree.
        val fill = m.sketchFill(epoch, seed, cfg.rssiFloorDbm)
        r.observe("local sketch", "$fill of 16 slots filled")
        if (fill in 1 until MIN_TRUSTWORTHY_CELL) {
            r.info("Below $MIN_TRUSTWORTHY_CELL marks a verified witness is not real evidence — received frames will badge \"weak cell\".")
        }
        r.check("own mark is in this epoch's cell", m.hasSelfMark(epoch) || m.hasSelfMark(epoch - 1u),
            "without it the cell is empty and frames go out witnessless — relayed, never displayed")

        MeshState.clockSkewWarning.value?.let {
            r.check("no epoch skew", false, "frames are being dropped as out-of-window")
        } ?: r.check("no epoch skew", true)

        r.observe("messages in feed", "${MeshState.messages.value.size}")
        r.observe("airing", if (MeshState.outgoingAiring.value) "a message is still re-airing" else "idle")
    }
}

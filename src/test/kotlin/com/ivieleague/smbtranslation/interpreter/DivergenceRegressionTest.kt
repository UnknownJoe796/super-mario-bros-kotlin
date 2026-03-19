// by Claude - Replays divergence snapshots captured by ShadowValidator as deterministic
// regression tests. Each snapshot contains the pre-invocation RAM state from a live game
// session where interpreter and Kotlin diverged. Run the game with shadow validation
// enabled to generate snapshots in build/divergence-snapshots/.
package com.ivieleague.smbtranslation.interpreter

import com.ivieleague.smbtranslation.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.io.File

class DivergenceRegressionTest {

    /** Maps shadow validator function names to Kotlin invocations. */
    private val functionDispatch: Map<String, (System) -> Unit> = mapOf(
        // operModeExecutionTree.kt
        "opermodeexecutiontree" to { it.operModeExecutionTree() },
        "gamemode" to { it.gameMode() },
        "titlescreenmode" to { it.titleScreenMode() },
        "victorymode" to { it.victoryMode() },
        "gameovermode" to { it.gameOverMode() },
        // gameMode.kt
        "gameroutines" to { it.gameRoutines() },
        "updscrollvar" to { it.updScrollVar() },
        "blockobjmtUpdater" to { it.blockObjMTUpdater() },
        "rungametimer" to { it.runGameTimer() },
        // Various game subsystems
        "procfireballBubble" to { it.procFireballBubble() },
        "floateynumbersroutine" to { it.floateyNumbersRoutine(it.ram.objectOffset) },
        "playergfxhandler" to { it.playerGfxHandler() },
        "blockobjectscore" to { it.blockObjectsCore() },
        "miscobjectscore" to { it.miscObjectsCore() },
        "processcannons" to { it.processCannons() },
        "processwhirlpools" to { it.processWhirlpools() },
        "flagpoleroutine" to { it.flagpoleRoutine() },
        "colorrotation" to { it.colorRotation() },
        // Enemy subsystems (enemyBehaviors.kt)
        "getenemyoffscreenbits" to { it.getEnemyOffscreenBits() },
        "relativeenemyposition" to { it.relativeEnemyPosition() },
        "getenemyboundbox" to { it.getEnemyBoundBox() },
        "enemytobgcollisiondet" to { it.enemyToBGCollisionDet() },
        "enemiescollision" to { it.enemiesCollision() },
        "playerenemycollision" to { it.playerEnemyCollision() },
        "enemymovementsubs" to { it.enemyMovementSubs() },
        "offscreenboundscheck" to { it.offscreenBoundsCheck() },
        // Player subsystems (gameMode.kt)
        "getplayeroffscreenbits" to { it.getPlayerOffscreenBits() },
        "relativeplayerposition" to { it.relativePlayerPosition() },
    )

    /** Addresses excluded from comparison — same as ShadowValidator. */
    private val excludedAddresses: Set<Int> = buildSet {
        addAll(0x200..0x2FF)    // OAM sprite data
        addAll(0x300..0x3AC)    // VRAM buffer 1
        addAll(0x4AC..0x4FF)    // Bounding box coordinates
        addAll(0x500..0x7FF)    // Block buffers, display digits, sound queues
        add(0xFF); add(0xFE); add(0xFD); add(0xFC)  // Sound queues
        // VRAM buffer overflow: NES physically writes past buffer into adjacent game RAM
        add(0x00E3)             // sprObjYPos[21] — unused slot
        add(0x03BE)             // relYPos[6] — past VRAM buffer 1 end
        add(0x03EC)             // blockRepFlags[0]
        add(0x03ED)             // blockRepFlags[1]
        add(0x03F0)             // blockResidualCounter
        // Unused SprObject slots (gaps in flat array, written by VRAM overflow)
        add(0x0078)             // sprObjPageLoc[11]
        add(0x0091)             // sprObjXPos[11]
        add(0x00D9)             // sprObjYPos[11]
        // ObjectOffset ($08) is a transient register; composite functions leave it at
        // different values depending on how many objects were processed
        add(0x0008)             // objectOffset
    }

    data class SnapshotMeta(
        val function: String,
        val address: Int,
        val frame: Long,
        val objectOffset: Int,
        val diffCount: Int,
        val diffs: List<String>,
    )

    private fun loadMeta(metaFile: File): SnapshotMeta {
        val props = mutableMapOf<String, String>()
        val diffs = mutableListOf<String>()
        for (line in metaFile.readLines()) {
            val eq = line.indexOf('=')
            if (eq < 0) continue
            val key = line.substring(0, eq)
            val value = line.substring(eq + 1)
            if (key.startsWith("diff.")) {
                diffs.add(value)
            } else {
                props[key] = value
            }
        }
        return SnapshotMeta(
            function = props["function"] ?: error("Missing function in $metaFile"),
            address = props["address"]?.toInt() ?: error("Missing address in $metaFile"),
            frame = props["frame"]?.toLong() ?: 0,
            objectOffset = props["objectOffset"]?.toInt() ?: 0,
            diffCount = props["diffCount"]?.toInt() ?: 0,
            diffs = diffs,
        )
    }

    @TestFactory
    fun `divergence regressions`(): List<DynamicTest> {
        val snapshotDir = File("build/divergence-snapshots")
        if (!snapshotDir.exists() || snapshotDir.listFiles()?.isEmpty() != false) {
            println("No divergence snapshots found in build/divergence-snapshots/")
            println("Run the game with shadow validation enabled to generate snapshots.")
            return listOf(DynamicTest.dynamicTest("no snapshots") {
                println("Skipped — no divergence snapshots available")
            })
        }

        val romFile = File("smb.nes")
        if (!romFile.exists()) {
            return listOf(DynamicTest.dynamicTest("no ROM") {
                println("Skipped — smb.nes not found")
            })
        }

        val binFiles = snapshotDir.listFiles { f -> f.extension == "bin" }
            ?.sortedBy { it.name }
            ?: return emptyList()

        return binFiles.map { binFile ->
            val metaFile = File(binFile.path.replace(".bin", ".meta"))
            val meta = loadMeta(metaFile)
            val testName = "${meta.function}_frame${meta.frame} (${meta.diffCount} original diffs)"

            DynamicTest.dynamicTest(testName) {
                runSnapshotRegression(binFile, meta)
            }
        }
    }

    /**
     * Synthetic end-to-end test: creates a snapshot file from a known-good function
     * (relativePlayerPosition), then replays it through the regression pipeline.
     * Verifies the save→load→replay machinery works without needing a live game run.
     */
    @Test
    fun `snapshot round-trip smoke test`() {
        val romFile = File("smb.nes")
        if (!romFile.exists()) {
            println("Skipped — smb.nes not found")
            return
        }

        val comp = SubroutineComparison.create("smb.nes")
        val funcName = "relativeplayerposition"
        val meta = comp.functionMetadata[funcName]
            ?: error("relativeplayerposition not found in metadata")

        // Set up a deterministic RAM state
        val random = java.util.Random(12345)
        comp.randomizeRam(random)
        comp.interpreter.cpu.X = 0x00u
        comp.interpreter.cpu.SP = 0xFFu
        comp.interpreter.memory.writeByte(0x08, 0x00u) // objectOffset = 0

        // Capture the RAM state as a snapshot
        val ramDump = ByteArray(2048)
        for (addr in 0 until 2048) {
            ramDump[addr] = comp.interpreter.memory.readByte(addr).toByte()
        }

        // Write snapshot files to a temp directory
        val tmpDir = File("build/test-snapshots").also { it.mkdirs() }
        val binFile = File(tmpDir, "test_roundtrip.bin")
        val metaFile = File(tmpDir, "test_roundtrip.meta")
        try {
            binFile.writeBytes(ramDump)
            metaFile.writeText(buildString {
                appendLine("function=$funcName")
                appendLine("address=${meta.address}")
                appendLine("frame=0")
                appendLine("objectOffset=0")
                appendLine("diffCount=0")
            })

            // Replay through the regression pipeline
            val snapshotMeta = loadMeta(metaFile)
            runSnapshotRegression(binFile, snapshotMeta)
            println("Round-trip smoke test PASSED")
        } finally {
            binFile.delete()
            metaFile.delete()
            tmpDir.delete()
        }
    }

    private fun runSnapshotRegression(binFile: File, meta: SnapshotMeta) {
        val dispatch = functionDispatch[meta.function]
        if (dispatch == null) {
            println("SKIP: No dispatch for function '${meta.function}' — add it to functionDispatch")
            return
        }

        // Load ROM and create comparison harness
        val comp = SubroutineComparison.create("smb.nes")

        // Load saved RAM state into interpreter
        val savedRam = binFile.readBytes()
        require(savedRam.size == 2048) { "Expected 2048-byte RAM dump, got ${savedRam.size}" }
        for (addr in 0 until 2048) {
            comp.interpreter.memory.writeByte(addr, savedRam[addr].toUByte())
        }

        // Set CPU registers
        comp.interpreter.cpu.X = meta.objectOffset.toUByte()
        comp.interpreter.cpu.SP = 0xFFu
        comp.interpreter.cpu.C = false
        comp.interpreter.cpu.A = 0x00u
        comp.interpreter.cpu.Y = 0x00u

        // Run interpreter subroutine
        val interpSnap = comp.runInterpreterSubroutine(meta.address)
            .normalizeBooleans()

        // Create Kotlin System from the same initial state
        // Re-load the saved RAM into interpreter (runInterpreterSubroutine mutated it)
        for (addr in 0 until 2048) {
            comp.interpreter.memory.writeByte(addr, savedRam[addr].toUByte())
        }
        val system = System()
        comp.syncRamToSystem(system)

        // Run Kotlin translation
        dispatch(system)

        // Capture and compare
        val ktSnap = RamSnapshot.capture(system.ram)
        val diffs = interpSnap.diff(ktSnap)
            .filter { it.address !in excludedAddresses && it.address != 0x0770 }

        if (diffs.isNotEmpty()) {
            println("${meta.function} @ frame ${meta.frame}: ${diffs.size} diffs")
            for (d in diffs.take(20)) println("  $d")
            if (diffs.size > 20) println("  ... and ${diffs.size - 20} more")

            println("\nOriginal divergences from shadow validator:")
            for (d in meta.diffs.take(10)) println("  $d")
        }

        val diffDetail = diffs.take(10).joinToString("; ") { it.toString() }
        assertEquals(0, diffs.size,
            "${meta.function} @ frame ${meta.frame}: ${diffs.size} divergences: $diffDetail")
    }
}

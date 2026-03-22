package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.nes.PpuRenderer
import com.ivieleague.smbtranslation.utils.JoypadBits
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Surface
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test

class VisualVerificationTest {

    private fun snapshot(system: System, name: String) {
        val scale = 2
        val surface = Surface.makeRasterN32Premul(256 * scale, 240 * scale)
        PpuRenderer.render(surface.canvas, system.ppu, scale, scrollStartY = 32)
        val data = surface.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG)
            ?: error("Failed to encode PNG")
        val outPath = Path.of("build", "screenshots", "$name.png")
        Files.createDirectories(outPath.parent)
        Files.write(outPath, data.bytes)
    }

    private fun countNtTiles(system: System, ntIdx: Int): Int {
        var count = 0
        for (ty in 0 until 30) for (tx in 0 until 32) {
            val tile = system.ppu.backgroundTiles[ntIdx][tx, ty]
            if (tile.pattern.bits.any { it != 0.toByte() }) count++
        }
        return count
    }

    @Test
    fun titleScreenProgression() {
        val system = System()
        system.start()

        val log = StringBuilder()
        for (i in 1..200) {
            val r = system.ram
            val buf2Before = r.vRAMBuffer2.toList()
            val buf1Before = r.vRAMBuffer1.toList()
            val ctrlBefore = r.vRAMBufferAddrCtrl

            system.nonMaskableInterrupt()

            val srt = r.screenRoutineTask
            val nt0 = countNtTiles(system, 0)
            val nt1 = countNtTiles(system, 1)

            // Log every frame during area parser phase (srt=8) and nearby
            if (srt.ordinal in 6..13 || i <= 10 || i % 30 == 0) {
                log.appendLine("F$i: srt=$srt nt0=$nt0 nt1=$nt1 ctrl=$ctrlBefore " +
                    "buf1=${buf1Before.size} buf2=${buf2Before.size} " +
                    "colSets=${r.columnSets} aptNum=${r.areaParserTaskNum} " +
                    "ntAddr=\$${(r.currentNTAddrHigh.toInt() and 0xFF).toString(16)}${(r.currentNTAddrLow.toInt() and 0xFF).toString(16).padStart(2, '0')}")

                // During area parser, dump buffer contents
                if (buf2Before.isNotEmpty() && ctrlBefore.toInt() == 6) {
                    for (upd in buf2Before) {
                        log.appendLine("  buf2: $upd")
                    }
                }
                if (buf1Before.isNotEmpty() && ctrlBefore.toInt() != 6) {
                    for (upd in buf1Before) {
                        log.appendLine("  buf1: $upd")
                    }
                }
            }

            if (i == 1 || i == 10 || i == 30 || i == 120 || i == 180 || i == 200) {
                snapshot(system, "frame-$i")
            }
        }
        val logPath = Path.of("build", "screenshots", "title-log.txt")
        Files.createDirectories(logPath.parent)
        Files.writeString(logPath, log.toString())
    }

    @Test
    fun demoModeProgression() {
        val system = System()
        system.start()

        // Run to title screen
        repeat(180) { system.nonMaskableInterrupt() }
        snapshot(system, "demo-000-title")

        // Press Start to begin game, then release
        system.inputs.joypadPort1 = JoypadBits(0b00010000.toByte()) // Start
        system.nonMaskableInterrupt()
        system.inputs.joypadPort1 = JoypadBits(0)

        // Run game frames — 900 frames (15 sec)
        val log = StringBuilder()
        for (i in 1..900) {
            // Capture buffer sizes BEFORE NMI (to see what's about to be applied)
            val buf1Before = system.ram.vRAMBuffer1.size
            val buf2Before = system.ram.vRAMBuffer2.size
            val ctrlBefore = system.ram.vRAMBufferAddrCtrl

            system.nonMaskableInterrupt()

            // Detailed logging: every frame for first 40, then every 10 until 200, then every 30
            if (i <= 40 || (i <= 200 && i % 10 == 0) || i % 30 == 0) {
                val r = system.ram
                val p = system.ppu
                val bgColor = p.universalBackgroundColor
                val nt0 = countNtTiles(system, 0)
                val nt1 = countNtTiles(system, 1)
                log.appendLine("F$i: mode=${r.operMode} task=${r.operModeTask} " +
                    "srt=${r.screenRoutineTask} " +
                    "nt0=$nt0 nt1=$nt1 " +
                    "buf1=$buf1Before→${r.vRAMBuffer1.size} buf2=$buf2Before→${r.vRAMBuffer2.size} " +
                    "ctrl=$ctrlBefore→${r.vRAMBufferAddrCtrl} " +
                    "scrollX=${p.scrollX} ntBase=${p.control.baseNametableAddress} " +
                    "bgColor=0x${bgColor.argb.toUInt().toString(16)} " +
                    "pY=${r.sprObjYPos[0].toInt() and 0xFF} pX=${r.sprObjXPos[0].toInt() and 0xFF} " +
                    "pState=${r.playerState} lives=${r.numberofLives} " +
                    "colSets=${r.columnSets} aptNum=${r.areaParserTaskNum}")
            }
            // At frame 155 (after area parser), dump block buffer
            if (i == 155) {
                // blockBuffer1 is 0xd0 bytes, organized as 16 columns × 13 rows
                // Check ground rows (11-12) for each column
                val bb1 = system.ram.blockBuffer1
                val bb2 = system.ram.blockBuffer2
                val groundLine = StringBuilder("BlockBuf ground: ")
                for (col in 0 until 16) {
                    val row11 = bb1[col * 0x0d + 11].toInt() and 0xFF
                    val row12 = bb1[col * 0x0d + 12].toInt() and 0xFF
                    groundLine.append("c$col=($row11,$row12) ")
                }
                log.appendLine(groundLine)
                val buf2Line = StringBuilder("BlockBuf2 ground: ")
                for (col in 0 until 8) {
                    val row11 = bb2[col * 0x0d + 11].toInt() and 0xFF
                    val row12 = bb2[col * 0x0d + 12].toInt() and 0xFF
                    buf2Line.append("c$col=($row11,$row12) ")
                }
                log.appendLine(buf2Line)
            }
            if (i % 60 == 0) snapshot(system, "demo-${"%03d".format(i)}-game")
        }
        val logPath = Path.of("build", "screenshots", "demo-log.txt")
        Files.writeString(logPath, log.toString())
    }
}

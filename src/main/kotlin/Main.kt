// by Claude - Main entry point: opens a window and runs the game at 60fps
package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.nes.PpuRenderer
import com.ivieleague.smbtranslation.utils.JoypadBits
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkikoRenderDelegate
import java.awt.Dimension
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import javax.swing.JFrame
import javax.swing.SwingUtilities

fun main() {
    val system = System()
    // Select game variant: -Dsmb.variant=smb2j for The Lost Levels
    if (java.lang.System.getProperty("smb.variant") == "smb2j") {
        system.variant = GameVariant.SMB2J
        system.romData = Smb2jRomData
    }
    // Select character: -Dsmb.character=luigi
    if (java.lang.System.getProperty("smb.character") == "luigi") {
        system.character = Character.Luigi
        system.ram.selectedPlayer = 1
    }
    if (java.lang.System.getProperty("smb.shadow") == "true") {
        system.shadow = com.ivieleague.smbtranslation.interpreter.ShadowValidator.create("smb.nes")
    }
    val audioOutput = com.ivieleague.smbtranslation.nes.ApuAudioOutput()
    system.audioOutput = audioOutput
    if (java.lang.System.getProperty("smb.audio") != "false") {
        try { audioOutput.start() } catch (e: Exception) {
            java.lang.System.err.println("Audio init failed: ${e.message}")
        }
    }
    Runtime.getRuntime().addShutdownHook(Thread {
        system.shadow?.close()
        audioOutput.stop()
    })
    val pressedKeys: MutableSet<Int> = Collections.newSetFromMap(ConcurrentHashMap<Int, Boolean>())

    // Lock shared between game thread and render thread to protect PPU state access
    val frameLock = Object()

    SwingUtilities.invokeLater {
        val skiaLayer = SkiaLayer()
        skiaLayer.renderDelegate = object : SkikoRenderDelegate {
            override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
                synchronized(frameLock) {
                    val currentScale = (width.toFloat() / 256f).coerceAtMost(height.toFloat() / 240f)
                    PpuRenderer.render(canvas, system.ppu, currentScale.toInt(), scrollStartY = 32)
                }
            }
        }

        val frame = JFrame("Super Mario Bros")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        val scale = 3
        val width = 256 * scale
        val height = 240 * scale
        skiaLayer.preferredSize = Dimension(width, height)
        frame.contentPane.add(skiaLayer)
        frame.pack()
        frame.setLocationRelativeTo(null)

        // Use KeyboardFocusManager to capture key events regardless of which
        // component has focus. A KeyListener on JFrame stops receiving events
        // once the SkiaLayer (heavyweight component) takes focus.
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(
            KeyEventDispatcher { e ->
                when (e.id) {
                    KeyEvent.KEY_PRESSED -> pressedKeys.add(e.keyCode)
                    KeyEvent.KEY_RELEASED -> pressedKeys.remove(e.keyCode)
                }
                false
            }
        )

        frame.isVisible = true

        // Start the dedicated game thread after the window is visible
        Thread({
            var startAction: (() -> Unit)? = { system.start() }
            var frameCount = 0
            var lastDisableScreenFlag = true
            var lastPpuMask = 0.toByte()
            var lastNmiTime = java.lang.System.nanoTime()
            val nmiIntervalNs = 1_000_000_000L / 60

            while (true) {
                val now = java.lang.System.nanoTime()
                val turbo = KeyEvent.VK_TAB in pressedKeys

                if (!turbo) {
                    // Spin-wait until the next frame is due
                    val deadline = lastNmiTime + nmiIntervalNs
                    if (now < deadline) {
                        // Coarse sleep to avoid burning CPU, then spin for precision
                        val remaining = deadline - now
                        if (remaining > 2_000_000L) {
                            Thread.sleep((remaining - 1_500_000L) / 1_000_000L)
                        }
                        // Spin-wait for the final stretch
                        @Suppress("ControlFlowWithEmptyBody")
                        while (java.lang.System.nanoTime() < deadline) { /* spin */ }
                    }
                    lastNmiTime += nmiIntervalNs
                    // Catch up but don't spiral -- cap to one frame behind
                    val afterWait = java.lang.System.nanoTime()
                    if (afterWait - lastNmiTime > nmiIntervalNs) {
                        lastNmiTime = afterWait
                    }
                } else {
                    lastNmiTime = now
                }

                // Sample input
                system.inputs.joypadPort1 = buildJoypadBits(pressedKeys)

                // Run game frame under lock so render thread doesn't read partial PPU state
                synchronized(frameLock) {
                    val action = startAction
                    if (action != null) {
                        try {
                            startAction = null
                            action()
                        } catch (delay: com.ivieleague.smbtranslation.utils.FrameDelay) {
                            startAction = delay.nextAction
                            if (frameCount % 60 == 0) {
                                java.lang.System.err.println("Initializing: Frame delay encountered, resuming in next frame...")
                            }
                        }
                    } else {
                        system.nonMaskableInterrupt()
                        if (!turbo) audioOutput.outputFrame(system.apu)
                    }

                    if (system.ram.disableScreenFlag != lastDisableScreenFlag) {
                        lastDisableScreenFlag = system.ram.disableScreenFlag
                        java.lang.System.out.println("Screen status changed: disableScreenFlag = $lastDisableScreenFlag")
                    }
                    if (system.ppu.mask.byte != lastPpuMask) {
                        lastPpuMask = system.ppu.mask.byte
                        java.lang.System.out.println("PPU Mask changed: backgroundEnabled=${system.ppu.mask.backgroundEnabled}, spriteEnabled=${system.ppu.mask.spriteEnabled}")
                    }
                }

                frameCount++

                // Tell Skia to repaint with the new frame
                SwingUtilities.invokeLater { skiaLayer.needRedraw() }
            }
        }, "game-loop").apply {
            isDaemon = true
            start()
        }
    }
}

private fun buildJoypadBits(keys: Set<Int>): JoypadBits {
    var bits = 0
    // NES joypad bit layout: A B Select Start Up Down Left Right
    if (KeyEvent.VK_X in keys) bits = bits or 0b10000000       // A
    if (KeyEvent.VK_Z in keys) bits = bits or 0b01000000       // B
    if (KeyEvent.VK_SHIFT in keys) bits = bits or 0b00100000   // Select
    if (KeyEvent.VK_ENTER in keys) bits = bits or 0b00010000   // Start
    if (KeyEvent.VK_UP in keys) bits = bits or 0b00001000      // Up
    if (KeyEvent.VK_DOWN in keys) bits = bits or 0b00000100    // Down
    if (KeyEvent.VK_LEFT in keys) bits = bits or 0b00000010    // Left
    if (KeyEvent.VK_RIGHT in keys) bits = bits or 0b00000001   // Right
    return JoypadBits(bits.toByte())
}

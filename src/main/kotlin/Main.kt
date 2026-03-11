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
    system.start()

    val scale = 3
    val width = 256 * scale
    val height = 240 * scale

    val pressedKeys: MutableSet<Int> = Collections.newSetFromMap(ConcurrentHashMap<Int, Boolean>())
    var lastNmiTime = java.lang.System.nanoTime()
    val nmiIntervalNs = 1_000_000_000L / 60

    SwingUtilities.invokeLater {
        val skiaLayer = SkiaLayer()
        skiaLayer.renderDelegate = object : SkikoRenderDelegate {
            override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
                val now = java.lang.System.nanoTime()
                if (now - lastNmiTime >= nmiIntervalNs) {
                    lastNmiTime += nmiIntervalNs
                    // Catch up but don't spiral — cap to one frame behind
                    if (now - lastNmiTime > nmiIntervalNs) {
                        lastNmiTime = now
                    }

                    // Build joypad bits from currently pressed keys.
                    // readJoypads() inside NMI transfers these into ram.savedJoypadBits.
                    system.inputs.joypadPort1 = buildJoypadBits(pressedKeys)

                    system.nonMaskableInterrupt()
                }

                PpuRenderer.render(canvas, system.ppu, scale, scrollStartY = 16)
                skiaLayer.needRedraw()
            }
        }

        val frame = JFrame("Super Mario Bros")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
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
        skiaLayer.needRedraw()
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

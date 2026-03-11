package com.ivieleague.smbtranslation.nes

import com.ivieleague.smbtranslation.utils.get
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect

object PpuRenderer {
    /**
     * Renders a simple view of the current PPU state (background + sprites) to the provided Skia canvas.
     *
     * Implemented:
     * - Background tiles from nametable 0 (32x30 tiles, 8x8 pixels each)
     * - Global background color fill (ppu.backgroundColor)
     * - Sprites (8x8 only), including palette selection, horizontal/vertical flip, and behind-background priority
     * - Tile/sprite palettes: color index 0 is transparent for sprites; for background, CI=0 uses the global background color
     * - Optional integer scaling for crisp pixels
     * - Horizontal and vertical scrolling with configurable Y start coordinate
     *
     * Not yet implemented:
     * - 8x16 sprites, masking, emphasis, greyscale, sprite zero hit timing, overflow, left-8px mask
     *
     * @param canvas The Skia canvas to render to
     * @param ppu The PPU state to render
     * @param scale The integer scaling factor for pixels
     * @param scrollStartY The Y coordinate (in pixels) at which scrolling begins to take effect.
     *                     Rows above this Y coordinate are not scrolled (useful for status bars).
     *                     In SMB, this is typically 16 (2 rows of 8 pixels) to preserve the score/lives display.
     */
    fun render(canvas: Canvas, ppu: PictureProcessingUnit, scale: Int = 2, scrollStartY: Int = 0) {
        val tileSize = 8
        val screenW = 256
        val screenH = 240
        val paint = Paint()

        // Nametable X offset from PPU control register bit 0
        val ntOffsetX = (ppu.control.baseNametableAddress.toInt() and 1) * 256

        // Fill with PPU universal background color ($3F00)
        val backgroundColor = ppu.universalBackgroundColor
        paint.color = backgroundColor.argb
        canvas.drawRect(
            Rect.Companion.makeXYWH(0f, 0f, (screenW * scale).toFloat(), (screenH * scale).toFloat()),
            paint
        )

        // Look up background tile + color index at a screen pixel
        fun bgLookup(sx: Int, sy: Int): Pair<Tile, Byte> {
            val vx: Int
            val vy: Int
            if (sy >= scrollStartY) {
                vx = (sx + ppu.scrollX + ntOffsetX) % 512
                vy = sy + ppu.scrollY
            } else {
                vx = sx
                vy = sy
            }
            val ntIdx = (vx / 256) and 1
            val tx = (vx and 255) / tileSize
            val ty = (vy / tileSize).coerceIn(0, 29)
            val tile = ppu.backgroundTiles[ntIdx][tx, ty]
            return tile to tile.pattern.colorIndex(vx % tileSize, vy % tileSize)
        }

        // Draw background — iterate over screen pixels, look up from correct nametable
        for (sy in 0 until screenH) {
            for (sx in 0 until screenW) {
                val (tile, ci) = bgLookup(sx, sy)
                if (ci == 0.toByte()) continue // bg color already filled
                paint.color = tile.palette.colors[ci].argb
                canvas.drawRect(
                    Rect.Companion.makeXYWH(
                        (sx * scale).toFloat(), (sy * scale).toFloat(),
                        scale.toFloat(), scale.toFloat()
                    ), paint
                )
            }
        }

        // Draw sprites
        for (i in 0 until ppu.sprites.size) {
            val spr = ppu.sprites[i]
            val xBase = spr.x.toInt()
            val yBase = spr.y.toInt()
            val flags = spr.attributes
            val paletteIndex = (flags.palette.toInt() and 0x03)
            val palette = ppu.spritePalettes.getOrNull(paletteIndex)
            val flipH = flags.flipHorizontal
            val flipV = flags.flipVertical
            val behind = flags.behindBackground

            // Skip if palette is missing
            if (palette == null) continue

            for (py in 0 until tileSize) {
                val srcY = if (flipV) (tileSize - 1 - py) else py
                val y = yBase + py
                for (px in 0 until tileSize) {
                    val srcX = if (flipH) (tileSize - 1 - px) else px
                    val x = xBase + px

                    val ci = spr.pattern.colorIndex(srcX, srcY)
                    if (ci == 0.toByte()) continue // transparent sprite pixel

                    if (behind) {
                        // If behind background, only draw where background is CI 0
                        val (_, bgCi) = bgLookup(x, y)
                        if (bgCi != 0.toByte()) continue
                    }

                    val colorIdx = ci
                    val argb = palette.colors[colorIdx].argb
                    paint.color = argb
                    canvas.drawRect(
                        Rect.Companion.makeXYWH((x * scale).toFloat(), (y * scale).toFloat(), scale.toFloat(), scale.toFloat()),
                        paint
                    )
                }
            }
        }
    }
}
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
     *                     In SMB, this is 32 (4 rows of 8 pixels) — the status bar occupies nametable rows 2-3
     *                     (rows 0-1 are empty NES overscan), so the unscrolled region must cover all 4 rows.
     */
    fun render(canvas: Canvas, ppu: PictureProcessingUnit, scale: Int = 2, scrollStartY: Int = 0) {
        val tileSize = 8
        val screenW = 256
        val screenH = 240
        val paint = Paint()

        // Fill with PPU universal background color ($3F00)
        val backgroundColor = ppu.universalBackgroundColor
        paint.color = backgroundColor.argb
        canvas.drawRect(
            Rect.Companion.makeXYWH(0f, 0f, (screenW * scale).toFloat(), (screenH * scale).toFloat()),
            paint
        )

        /**
         * Resolves the nametable index and local tile coordinates for a given virtual (vx, vy).
         * SMB uses vertical mirroring:
         * - Nametables at $2000 and $2800 are mirrored (physical index 0)
         * - Nametables at $2400 and $2C00 are mirrored (physical index 1)
         * This results in a 512x240 logical space mirrored vertically.
         */
        fun bgLookup(sx: Int, sy: Int): Pair<Tile, Byte> {
            val vx: Int
            val vy: Int
            val baseNt: Int = ppu.control.baseNametableAddress.toInt()
            if (sy >= scrollStartY) {
                // Combine base nametable address with scroll registers for full 15-bit internal scroll position
                // Bit 0 of baseNt is X scroll (bit 8), Bit 1 of baseNt is Y scroll (bit 8)
                vx = (sx + ppu.scrollX + (baseNt and 0x01) * 256) % 512
                vy = (sy + ppu.scrollY + (baseNt and 0x02) / 2 * 240) % 480
            } else {
                // Status bar usually fixed at Nametable 0
                vx = sx
                vy = sy
            }

            // Vertical mirroring: physical nametable is based on X coordinate only
            val ntIdx = (vx / 256) and 1
            val tx = (vx and 255) / tileSize
            val ty = (vy % 240) / tileSize
            
            val tile = ppu.backgroundTiles[ntIdx][tx, ty]
            return tile to tile.pattern.colorIndex(vx % tileSize, vy % tileSize)
        }

        // Draw background — iterate over screen pixels, look up from correct nametable
        if (ppu.mask.backgroundEnabled) {
            for (sy in 0 until screenH) {
                val hideLeft = !ppu.mask.showLeftBackground
                for (sx in 0 until screenW) {
                    if (hideLeft && sx < 8) continue
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
        }

        // Draw sprites (skip sprite 0 — it exists solely for NES sprite-0 hit detection,
        // which is irrelevant in the Kotlin translation; scroll splitting is handled by scrollStartY)
        if (ppu.mask.spriteEnabled) {
            for (i in ppu.sprites.size - 1 downTo 1) {
                val spr = ppu.sprites[i]
                val xBase = spr.x.toUByte().toInt()
                val yBase = spr.y.toUByte().toInt()
                if (yBase >= 240) continue
                val flags = spr.attributes
                val paletteIndex = flags.palette.toInt()
                val palette = ppu.spritePalettes.getOrNull(paletteIndex)
                val flipH = flags.flipHorizontal
                val flipV = flags.flipVertical
                val behind = flags.behindBackground
                val hideLeft = !ppu.mask.showLeftSprites

                // Skip if palette is missing
                if (palette == null) continue

                for (py in 0 until tileSize) {
                    val srcY = if (flipV) (tileSize - 1 - py) else py
                    val y = yBase + py + 1
                    if (y >= 240) continue
                    for (px in 0 until tileSize) {
                        val srcX = if (flipH) (tileSize - 1 - px) else px
                        val x = xBase + px
                        if (x >= 256) continue
                        if (hideLeft && x < 8) continue

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
}
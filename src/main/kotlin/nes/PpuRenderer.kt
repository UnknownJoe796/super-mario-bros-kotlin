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
        val nt: NesNametable = ppu.backgroundTiles[0]
        val paint = Paint()

        // Fill with PPU background color
        val backgroundColor = ppu.backgroundPalettes[0].colors[0]
        paint.color = backgroundColor.argb
        println("Filling with back color ${backgroundColor}")
        canvas.drawRect(
            Rect.Companion.makeXYWH(
                0f,
                0f,
                (nt.width * tileSize * scale).toFloat(),
                (nt.height * tileSize * scale).toFloat()
            ),
            paint
        )

        // Helper to sample the background color index at a screen pixel (0..3)
        // Takes screen coordinates and maps back to tile coordinates considering scroll
        fun backgroundColorIndexAt(screenX: Int, screenY: Int): Int {
            // Map screen coordinates back to tile coordinates
            val tileX = if (screenY >= scrollStartY) {
                screenX + ppu.scrollX
            } else {
                screenX
            }
            val tileY = if (screenY >= scrollStartY) {
                screenY + ppu.scrollY
            } else {
                screenY
            }

            if (tileX !in 0 until nt.width * tileSize || tileY !in 0 until nt.height * tileSize) return 0
            val tx = tileX / tileSize
            val ty = tileY / tileSize
            val ix = tileX % tileSize
            val iy = tileY % tileSize
            val tile: Tile = nt[tx, ty] ?: return 0
            return tile.pattern.colorIndex(ix, iy).toInt()
        }

        // Draw background tiles (CI 0 already covered by bg fill)
        for (ty in 0 until nt.height) {
            for (tx in 0 until nt.width) {
                val tile: Tile = nt[tx, ty]
                for (py in 0 until tileSize) {
                    for (px in 0 until tileSize) {
                        val ci = tile.pattern.colorIndex(px, py) // 0..3
                        if (ci == 0.toByte()) continue // already bg color
                        val colorIdx = ci
                        val argb = tile.palette.colors[colorIdx].argb
                        paint.color = argb

                        // Calculate base screen position
                        val baseX = tx * tileSize + px
                        val baseY = ty * tileSize + py

                        // Apply scroll offset only if Y >= scrollStartY
                        val screenX = if (baseY >= scrollStartY) {
                            baseX - ppu.scrollX
                        } else {
                            baseX
                        }
                        val screenY = if (baseY >= scrollStartY) {
                            baseY - ppu.scrollY
                        } else {
                            baseY
                        }

                        // Skip if scrolled off screen
                        if (screenX < 0 || screenY < 0 ||
                            screenX >= nt.width * tileSize ||
                            screenY >= nt.height * tileSize) continue

                        val x = screenX * scale
                        val y = screenY * scale
                        canvas.drawRect(Rect.Companion.makeXYWH(x.toFloat(), y.toFloat(), scale.toFloat(), scale.toFloat()), paint)
                    }
                }
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
                        val bgCi = backgroundColorIndexAt(x, y)
                        if (bgCi != 0) continue
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
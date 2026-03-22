package com.ivieleague.smbtranslation.nes

import com.ivieleague.smbtranslation.utils.get
import org.jetbrains.skia.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

object PpuRenderer {
    private const val SCREEN_W = 256
    private const val SCREEN_H = 240
    private const val TILE_SIZE = 8

    // Reusable pixel buffer (ARGB ints)
    private val pixels = IntArray(SCREEN_W * SCREEN_H)

    // Reusable byte buffer for Skia N32 format (BGRA on little-endian)
    private val byteBuffer = ByteArray(SCREEN_W * SCREEN_H * 4)

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
        val bgArgb = ppu.universalBackgroundColor.argb

        // Fill entire pixel buffer with universal background color
        pixels.fill(bgArgb)

        // Pre-extract scroll values for the hot loop
        val baseNt = ppu.control.baseNametableAddress.toInt()
        val scrollX = ppu.scrollX
        val scrollY = ppu.scrollY
        val ntXBit = (baseNt and 0x01) * 256
        val ntYBit = (baseNt and 0x02) / 2 * 240

        /**
         * Resolves the background color index at a given screen pixel.
         * Returns the Tile and color index byte. SMB uses vertical mirroring.
         */
        fun bgLookup(sx: Int, sy: Int): Pair<Tile, Byte> {
            val vx: Int
            val vy: Int
            if (sy >= scrollStartY) {
                vx = (sx + scrollX + ntXBit) % 512
                vy = (sy + scrollY + ntYBit) % 480
            } else {
                vx = sx
                vy = sy
            }

            val ntIdx = (vx / 256) and 1
            val tx = (vx and 255) / TILE_SIZE
            val ty = (vy % 240) / TILE_SIZE

            val tile = ppu.backgroundTiles[ntIdx][tx, ty]
            return tile to tile.pattern.colorIndex(vx % TILE_SIZE, vy % TILE_SIZE)
        }

        // Draw background
        if (ppu.mask.backgroundEnabled) {
            val hideLeft = !ppu.mask.showLeftBackground
            for (sy in 0 until SCREEN_H) {
                val rowOffset = sy * SCREEN_W
                for (sx in 0 until SCREEN_W) {
                    if (hideLeft && sx < 8) continue
                    val (tile, ci) = bgLookup(sx, sy)
                    if (ci == 0.toByte()) continue // bg color already filled
                    pixels[rowOffset + sx] = tile.palette.colors[ci].argb
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
                val palette = ppu.spritePalettes.getOrNull(paletteIndex) ?: continue
                val flipH = flags.flipHorizontal
                val flipV = flags.flipVertical
                val behind = flags.behindBackground
                val hideLeft = !ppu.mask.showLeftSprites

                for (py in 0 until TILE_SIZE) {
                    val srcY = if (flipV) (TILE_SIZE - 1 - py) else py
                    val y = yBase + py + 1
                    if (y >= 240) continue
                    val rowOffset = y * SCREEN_W
                    for (px in 0 until TILE_SIZE) {
                        val srcX = if (flipH) (TILE_SIZE - 1 - px) else px
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

                        pixels[rowOffset + x] = palette.colors[ci].argb
                    }
                }
            }
        }

        // Convert ARGB int array to byte array in Skia N32 format.
        // On little-endian (all modern platforms), N32 is BGRA byte order.
        // ARGB int 0xAARRGGBB stored as bytes: [BB, GG, RR, AA] = BGRA.
        val buf = ByteBuffer.wrap(byteBuffer).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer()
        buf.put(pixels)

        // Create image from pixel data and draw scaled with nearest-neighbor filtering
        val imageInfo = ImageInfo.makeN32Premul(SCREEN_W, SCREEN_H)
        val image = Image.makeRaster(imageInfo, byteBuffer, SCREEN_W * 4)
        canvas.drawImageRect(
            image,
            Rect.makeXYWH(0f, 0f, SCREEN_W.toFloat(), SCREEN_H.toFloat()),
            Rect.makeXYWH(0f, 0f, (SCREEN_W * scale).toFloat(), (SCREEN_H * scale).toFloat()),
            FilterMipmap(FilterMode.NEAREST, MipmapMode.NONE),
            null,
            true
        )
    }
}

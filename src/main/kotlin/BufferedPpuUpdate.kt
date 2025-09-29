package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.nes.FiveBits
import com.ivieleague.smbtranslation.nes.Pattern
import com.ivieleague.smbtranslation.nes.PictureProcessingUnit
import com.ivieleague.smbtranslation.nes.TwoBits
import com.ivieleague.smbtranslation.nes.Color
import com.ivieleague.smbtranslation.nes.DirectPalette
import com.ivieleague.smbtranslation.nes.Palette
import com.ivieleague.smbtranslation.utils.VramBufferControl

private fun applyAttributeCell(ppu: PictureProcessingUnit, nametable: TwoBits, ax: FiveBits, ay: FiveBits, value: Byte) {
    val nt = ppu.backgroundTiles[nametable.toInt() and 0x01]
    val baseX = (ax.toInt() and 0x1F) * 4
    val baseY = (ay.toInt() and 0x1F) * 4
    fun paletteForQuadrant(q: Int): Palette {
        val idx = (value.toInt() shr (q * 2)) and 0x03
        return ppu.backgroundPalettes[idx]
    }
    // Top-left quadrant (bits 0-1): tiles (0..1, 0..1)
    val palTL = paletteForQuadrant(0).also { println("PAL: $it") }
    for (dy in 0..1) for (dx in 0..1) {
        if (baseX + dx in 0 until 32 && baseY + dy in 0 until 30) {
            val t = nt[baseX + dx, baseY + dy]
            nt[baseX + dx, baseY + dy] = t.copy(palette = palTL)
        }
    }
    // Top-right quadrant (bits 2-3): (2..3, 0..1)
    val palTR = paletteForQuadrant(1).also { println("PAL: $it") }
    for (dy in 0..1) for (dx in 2..3) {
        if (baseX + dx in 0 until 32 && baseY + dy in 0 until 30) {
            val t = nt[baseX + dx, baseY + dy]
            nt[baseX + dx, baseY + dy] = t.copy(palette = palTR)
        }
    }
    // Bottom-left quadrant (bits 4-5): (0..1, 2..3)
    val palBL = paletteForQuadrant(2).also { println("PAL: $it") }
    for (dy in 2..3) for (dx in 0..1) {
        if (baseX + dx in 0 until 32 && baseY + dy in 0 until 30) {
            val t = nt[baseX + dx, baseY + dy]
            nt[baseX + dx, baseY + dy] = t.copy(palette = palBL)
        }
    }
    // Bottom-right quadrant (bits 6-7): (2..3, 2..3)
    val palBR = paletteForQuadrant(3).also { println("PAL: $it") }
    for (dy in 2..3) for (dx in 2..3) {
        if (baseX + dx in 0 until 32 && baseY + dy in 0 until 30) {
            val t = nt[baseX + dx, baseY + dy]
            nt[baseX + dx, baseY + dy] = t.copy(palette = palBR)
        }
    }
}

sealed class BufferedPpuUpdate {
    abstract operator fun invoke(ppu: PictureProcessingUnit)

    data class BackgroundPatternString(
        val nametable: TwoBits,
        val x: FiveBits,
        val y: FiveBits,
        val drawVertically: Boolean,
        val patterns: List<Pattern>
    ) : BufferedPpuUpdate() {
        override fun invoke(ppu: PictureProcessingUnit) {
            var x = x
            var y = y
            for (pattern in patterns) {
                val nt = ppu.backgroundTiles[nametable.toInt() and 0x01]
                val existing = nt.get(
                    x = x.toInt(),
                    y = y.toInt(),
                )
                nt.set(
                    x = x.toInt(),
                    y = y.toInt(),
                    value = existing.copy(pattern = pattern)
                )
                if (drawVertically) y++ else x++
            }
        }
    }

    data class BackgroundPatternRepeat(
        val nametable: TwoBits,
        val x: FiveBits,
        val y: FiveBits,
        val drawVertically: Boolean,
        val pattern: Pattern,
        val repetitions: Int,
    ) : BufferedPpuUpdate() {
        override fun invoke(ppu: PictureProcessingUnit) {
            var x = x
            var y = y
            repeat(repetitions) {
                val nt = ppu.backgroundTiles[nametable.toInt() and 0x01]
                val existing = nt.get(
                    x = x.toInt(),
                    y = y.toInt(),
                )
                nt.set(
                    x = x.toInt(),
                    y = y.toInt(),
                    value = existing.copy(pattern = pattern)
                )
                if (drawVertically) y++ else x++
            }
        }
    }

    data class BackgroundSetPalette(
        val index: TwoBits,
        val colors: List<Color>
    ): BufferedPpuUpdate() {
        override fun invoke(ppu: PictureProcessingUnit) {
            ppu.backgroundPalettes[index.toInt() and 0x03].palette = DirectPalette(colors.toTypedArray())
        }
    }

    data class SpriteSetPalette(
        val index: TwoBits,
        val colors: List<Color>
    ): BufferedPpuUpdate() {
        override fun invoke(ppu: PictureProcessingUnit) {
            ppu.spritePalettes[index.toInt() and 0x03].palette = DirectPalette(colors.toTypedArray())
        }
    }

    /**
     * Attribute table updates: each byte controls palettes for a 4x4 tile block via four 2-bit fields.
     * We expose two operations: literal string of attribute bytes and repeated byte.
     * Coordinates (ax, ay) are in attribute-cell space: 0..7 by 0..7 within a nametable.
     */
    data class BackgroundAttributeString(
        val nametable: TwoBits,
        val ax: FiveBits,
        val ay: FiveBits,
        val drawVertically: Boolean,
        val values: List<Byte>,
    ) : BufferedPpuUpdate() {
        override fun invoke(ppu: PictureProcessingUnit) {
            var ax = ax.toInt() and 0x1F
            var ay = ay.toInt() and 0x1F
            for (v in values) {
                applyAttributeCell(ppu, nametable, ax.toByte(), ay.toByte(), v)
                if (drawVertically) {
                    ay++
                    if (ay >= 8) { ay = 0; ax = (ax + 1) % 8 }
                } else {
                    ax++
                    if (ax >= 8) { ax = 0; ay = (ay + 1) % 8 }
                }
            }
        }
    }

    data class BackgroundAttributeRepeat(
        val nametable: TwoBits,
        val ax: FiveBits,
        val ay: FiveBits,
        val drawVertically: Boolean,
        val value: Byte,
        val repetitions: Int,
    ) : BufferedPpuUpdate() {
        override fun invoke(ppu: PictureProcessingUnit) {
            var ax = this.ax.toInt() and 0x1F
            var ay = this.ay.toInt() and 0x1F
            repeat(repetitions) {
                applyAttributeCell(ppu, nametable, ax.toByte(), ay.toByte(), value)
                if (drawVertically) {
                    ay++
                    if (ay >= 8) { ay = 0; ax = (ax + 1) % 8 }
                } else {
                    ax++
                    if (ax >= 8) { ax = 0; ay = (ay + 1) % 8 }
                }
            }
        }
    }

    companion object Parser {
        /**
         * Parses a Super Mario Bros style VRAM update buffer into a sequence of high-level PPU updates.
         *
         * Background
         * - SMB builds “VRAM update buffers” that UpdateScreen processes once per NMI.
         * - Each record in the buffer is: [addr_hi, addr_lo, control, data...]. A record with addr_hi=0 terminates the buffer.
         * - The control byte is encoded by VramBufferControl:
         *   - bit7 (drawVertically): if true, writes advance by 32 bytes between items (down a column). If false, they advance by 1 (across a row).
         *   - bit6 (repeat): if true, the next byte is replicated ‘length’ times; otherwise, ‘length’ bytes follow literally.
         *   - bits0..5 (length): number of bytes to emit or number of repetitions (0..63). We mask to 0x3F defensively.
         *
         * Address ranges we currently understand:
         * - $2000-$23BF, $2400-$27BF, $2800-$2BBF, $2C00-$2FBF (nametable tile area): we emit BackgroundPatternString or BackgroundPatternRepeat.
         * - $3F00-$3F1F (palette RAM): we coalesce aligned 4-byte chunks into BackgroundSetPalette/SpriteSetPalette for clarity and tests.
         *   If writes are misaligned or partial, we fall back to PaletteBytesWrite to preserve intent.
         * - Other ranges are currently not supported and will throw for visibility during development.
         */
        fun parseVramBuffer(ppu: PictureProcessingUnit, bytes: ByteArray): MutableVBuffer {
            val out = MutableVBuffer()
            var i = 0 // cursor into the buffer
            while (i < bytes.size) {
                // Read record header: address high, address low, control
                val hi = (bytes[i].toInt() and 0xFF)
                if (hi == 0x00) break // addr_hi==0 signals end of buffer
                if (i + 2 >= bytes.size) break // incomplete header; bail gracefully
                val lo = bytes[i + 1].toInt() and 0xFF
                val addr = (hi shl 8) or lo
                val ctrl = VramBufferControl(bytes[i + 2])
                i += 3

                // Extract control flags
                val length = ctrl.length.toInt() and 0x3F // mask to 6 bits (some data uses values up to 0x20)
                val drawVert = ctrl.drawVertically
                val repeat = ctrl.repeat

                // Fetch data payload according to repeat flag
                fun readDataBlock(): ByteArray {
                    return if (repeat) {
                        // Repeat mode: replicate the single following byte 'length' times.
                        if (i >= bytes.size) byteArrayOf() else ByteArray(length) { bytes[i] }
                    } else {
                        // Literal mode: copy the next 'length' bytes.
                        val end = (i + length).coerceAtMost(bytes.size)
                        bytes.copyOfRange(i, end)
                    }
                }

                val data = readDataBlock()
                // Advance cursor: in repeat mode we consumed only 1 byte; otherwise we consumed 'length'
                if (!repeat) i += length else i += if (length > 0) 1 else 0

                // Palette writes ($3F00-$3F1F): emit palette updates in 4-byte quads when aligned.
                if (addr in 0x3F00..0x3F1F) {
                    var palAddr = addr
                    var di = 0 // index into 'data'
                    while (di < data.size && palAddr in 0x3F00..0x3F1F) {
                        val offsetInQuad = palAddr and 0x3 // 0..3 within a palette quad
                        val remaining = data.size - di
                        // Only emit full 4-byte palette updates that start on a 4-byte boundary
                        if (offsetInQuad == 0 && remaining >= 4) {
                            val colors = listOf(
                                Color(data[di + 0]),
                                Color(data[di + 1]),
                                Color(data[di + 2]),
                                Color(data[di + 3]),
                            )
                            // $3F00-$3F0F = background palettes, $3F10-$3F1F = sprite palettes
                            val isSprite = palAddr >= 0x3F10
                            val index = if (isSprite) {
                                ((palAddr - 0x3F10) / 4).toByte()
                            } else {
                                ((palAddr - 0x3F00) / 4).toByte()
                            }
                            if (isSprite) {
                                out.add(SpriteSetPalette(index = index, colors = colors))
                            } else {
                                out.add(BackgroundSetPalette(index = index, colors = colors))
                            }
                            palAddr += 4
                            di += 4
                        } else throw IllegalArgumentException("unaligned palette write")
                    }
                    // Done with this record; continue to next
                    continue
                }

                // Compute nametable-relative offset (lower 10 bits of $2000-$2FFF)
                val ntOffset = addr and 0x3FF
                when (ntOffset) {
                    // $2000-$23BF, $2400-$27BF, $2800-$2BBF, $2C00-$2FBF: nametable tile area (960 bytes)
                    in 0x000..0x3BF -> {
                        // Which nametable? Each is 0x400 bytes wide. Map $2000/$2400/$2800/$2C00 -> 0..3
                        val ntIndex = ((addr - 0x2000) / 0x400) and 0x03
                        // Position within the nametable in bytes (0..0x3FF)
                        val startInNt = (addr - 0x2000) % 0x400
                        // Convert to tile coordinates (32x30 visible area)
                        val startX = startInNt % 32
                        val startY = startInNt / 32
                        if (repeat && data.isNotEmpty()) {
                            // Repeat: one tile ID replicated 'length' times along X or Y depending on drawVert
                            val pat = ppu.originalRomBackgrounds[data[0].toInt() and 0xFF]
                            out.add(
                                BackgroundPatternRepeat(
                                    nametable = ntIndex.toByte(),
                                    x = startX.toByte(),
                                    y = startY.toByte(),
                                    drawVertically = drawVert,
                                    pattern = pat,
                                    repetitions = length
                                )
                            )
                        } else if (data.isNotEmpty()) {
                            // Literal: sequence of tile IDs placed across or down depending on drawVert
                            val patterns = data.map { ppu.originalRomBackgrounds[it.toInt() and 0xFF] }
                            out.add(
                                BackgroundPatternString(
                                    nametable = ntIndex.toByte(),
                                    x = startX.toByte(),
                                    y = startY.toByte(),
                                    drawVertically = drawVert,
                                    patterns = patterns
                                )
                            )
                        }
                    }
                    // $23C0-$23FF (+ mirrors) attribute tables.
                    in 0x3C0..0x3FF -> {
                        val ntIndex = ((addr - 0x2000) / 0x400) and 0x03
                        val startInNt = (addr - 0x2000) % 0x400
                        val attrIndex = startInNt - 0x3C0 // 0..63
                        val startAx = (attrIndex % 8)
                        val startAy = (attrIndex / 8)
                        if (data.isNotEmpty()) {
                            if (repeat) {
                                out.add(
                                    BackgroundAttributeRepeat(
                                        nametable = ntIndex.toByte(),
                                        ax = startAx.toByte(),
                                        ay = startAy.toByte(),
                                        drawVertically = drawVert,
                                        value = data[0],
                                        repetitions = length,
                                    )
                                )
                            } else {
                                out.add(
                                    BackgroundAttributeString(
                                        nametable = ntIndex.toByte(),
                                        ax = startAx.toByte(),
                                        ay = startAy.toByte(),
                                        drawVertically = drawVert,
                                        values = data.toList(),
                                    )
                                )
                            }
                        }
                    }
                    else -> {
                        // Unsupported VRAM range for now: fail fast to catch unexpected data during porting.
                        throw IllegalArgumentException("Don't know how to write this; writes to an illegal space ${addr.toString(16)}")
                    }
                }
            }
            return out
        }
    }
}

typealias MutableVBuffer = ArrayList<BufferedPpuUpdate>
typealias VBuffer = List<BufferedPpuUpdate>

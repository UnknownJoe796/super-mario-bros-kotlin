// Ported from decompiler-6502-kotlin by Claude
package com.ivieleague.smbtranslation.interpreter

import java.io.File

/**
 * NES ROM loader for iNES format (.nes files)
 *
 * iNES format:
 * - Bytes 0-3: Header "NES\x1A"
 * - Byte 4: Number of 16KB PRG-ROM banks
 * - Byte 5: Number of 8KB CHR-ROM banks
 * - Byte 6: Flags 6 (mapper low nibble, mirroring, etc.)
 * - Byte 7: Flags 7 (mapper high nibble, etc.)
 * - Bytes 8-15: Size info or zeros
 * - PRG-ROM data (16KB per bank)
 * - CHR-ROM data (8KB per bank)
 */
class NESLoader {

    data class NESRom(
        val prgRom: ByteArray,
        val chrRom: ByteArray,
        val mapper: Int,
        val mirroring: Mirroring,
        val hasBattery: Boolean,
        val hasTrainer: Boolean
    ) {
        enum class Mirroring { HORIZONTAL, VERTICAL, FOUR_SCREEN }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is NESRom) return false
            return prgRom.contentEquals(other.prgRom) &&
                   chrRom.contentEquals(other.chrRom) &&
                   mapper == other.mapper
        }

        override fun hashCode(): Int {
            var result = prgRom.contentHashCode()
            result = 31 * result + chrRom.contentHashCode()
            result = 31 * result + mapper
            return result
        }
    }

    companion object {
        private const val HEADER_SIZE = 16
        private const val TRAINER_SIZE = 512
        private const val PRG_BANK_SIZE = 16384  // 16KB
        private const val CHR_BANK_SIZE = 8192   // 8KB

        /**
         * Load an iNES ROM file
         */
        fun load(file: File): NESRom {
            val data = file.readBytes()
            return load(data)
        }

        /**
         * Load from raw bytes
         */
        fun load(data: ByteArray): NESRom {
            require(data.size >= HEADER_SIZE) { "File too small to be a valid NES ROM" }

            // Verify header
            require(data[0].toInt() == 0x4E && data[1].toInt() == 0x45 &&
                    data[2].toInt() == 0x53 && data[3].toInt() == 0x1A) {
                "Invalid NES header - expected 'NES\\x1A'"
            }

            val prgBanks = data[4].toInt() and 0xFF
            val chrBanks = data[5].toInt() and 0xFF
            val flags6 = data[6].toInt() and 0xFF
            val flags7 = data[7].toInt() and 0xFF

            val mapper = ((flags6 shr 4) and 0x0F) or (flags7 and 0xF0)
            val mirroring = when {
                (flags6 and 0x08) != 0 -> NESRom.Mirroring.FOUR_SCREEN
                (flags6 and 0x01) != 0 -> NESRom.Mirroring.VERTICAL
                else -> NESRom.Mirroring.HORIZONTAL
            }
            val hasBattery = (flags6 and 0x02) != 0
            val hasTrainer = (flags6 and 0x04) != 0

            var offset = HEADER_SIZE
            if (hasTrainer) {
                offset += TRAINER_SIZE
            }

            val prgSize = prgBanks * PRG_BANK_SIZE
            val chrSize = chrBanks * CHR_BANK_SIZE

            require(data.size >= offset + prgSize + chrSize) {
                "ROM file truncated - expected ${offset + prgSize + chrSize} bytes, got ${data.size}"
            }

            val prgRom = data.copyOfRange(offset, offset + prgSize)
            offset += prgSize

            val chrRom = if (chrSize > 0) {
                data.copyOfRange(offset, offset + chrSize)
            } else {
                ByteArray(0)
            }

            return NESRom(
                prgRom = prgRom,
                chrRom = chrRom,
                mapper = mapper,
                mirroring = mirroring,
                hasBattery = hasBattery,
                hasTrainer = hasTrainer
            )
        }

        /**
         * Load ROM into interpreter memory (NROM mapper 0)
         *
         * SMB uses mapper 0 with:
         * - 32KB PRG-ROM at $8000-$FFFF
         * - 8KB CHR-ROM for PPU
         */
        fun loadIntoMemory(rom: NESRom, memory: Memory6502) {
            require(rom.mapper == 0) { "Only NROM (mapper 0) is currently supported" }

            when (rom.prgRom.size) {
                PRG_BANK_SIZE -> {
                    // 16KB - mirror at $8000 and $C000
                    for (i in 0 until PRG_BANK_SIZE) {
                        val b = rom.prgRom[i].toUByte()
                        memory.writeByte(0x8000 + i, b)
                        memory.writeByte(0xC000 + i, b)
                    }
                }
                PRG_BANK_SIZE * 2 -> {
                    // 32KB - load at $8000-$FFFF
                    for (i in 0 until PRG_BANK_SIZE * 2) {
                        memory.writeByte(0x8000 + i, rom.prgRom[i].toUByte())
                    }
                }
                else -> error("Unexpected PRG-ROM size: ${rom.prgRom.size}")
            }
        }
    }
}

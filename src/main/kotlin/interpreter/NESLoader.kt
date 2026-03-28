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

        // ---- FDS (Famicom Disk System) support ----

        data class FDSFile(val id: Int, val name: String, val loadAddress: Int, val data: ByteArray) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is FDSFile) return false
                return id == other.id && loadAddress == other.loadAddress && data.contentEquals(other.data)
            }
            override fun hashCode(): Int = 31 * id + data.contentHashCode()
        }

        data class FDSDisk(val files: List<FDSFile>)

        private const val FDS_HEADER_SIZE = 16
        private const val FDS_SIDE_SIZE = 65500
        private const val FDS_DISK_INFO_SIZE = 56   // Block type 1
        private const val FDS_FILE_COUNT_SIZE = 2    // Block type 2
        private const val FDS_FILE_HEADER_SIZE = 16  // Block type 3

        /**
         * Parse an FDS disk image (.fds file).
         * Supports both FWNES-headered (16-byte header + side data) and raw formats.
         */
        fun loadFDS(file: File): FDSDisk {
            val data = file.readBytes()
            return loadFDS(data)
        }

        fun loadFDS(data: ByteArray): FDSDisk {
            // Check for FWNES header: "FDS\x1A"
            val hasHeader = data.size >= 4 &&
                data[0].toInt() == 0x46 && data[1].toInt() == 0x44 &&
                data[2].toInt() == 0x53 && data[3].toInt() == 0x1A
            val sideStart = if (hasHeader) FDS_HEADER_SIZE else 0

            require(data.size >= sideStart + FDS_SIDE_SIZE) {
                "FDS image too small: ${data.size} bytes (expected >= ${sideStart + FDS_SIDE_SIZE})"
            }

            // Parse side 1 (SMB2J is single-sided)
            val files = mutableListOf<FDSFile>()
            var offset = sideStart

            // Block 1: Disk info (56 bytes, starts with $01)
            require(data[offset].toInt() and 0xFF == 0x01) { "Expected block type 1, got ${data[offset]}" }
            offset += FDS_DISK_INFO_SIZE

            // Block 2: File count (2 bytes, starts with $02)
            require(data[offset].toInt() and 0xFF == 0x02) { "Expected block type 2, got ${data[offset]}" }
            val fileCount = data[offset + 1].toInt() and 0xFF
            offset += FDS_FILE_COUNT_SIZE

            // Block 3+4 pairs: file header + file data
            for (i in 0 until fileCount) {
                if (offset >= data.size) break
                // Block 3: File header (16 bytes, starts with $03)
                if (data[offset].toInt() and 0xFF != 0x03) break
                val fileId = data[offset + 1].toInt() and 0xFF
                val fileName = String(data, offset + 2, 8).trimEnd('\u0000')
                val loadAddr = (data[offset + 11].toInt() and 0xFF) or
                    ((data[offset + 12].toInt() and 0xFF) shl 8)
                val fileSize = (data[offset + 13].toInt() and 0xFF) or
                    ((data[offset + 14].toInt() and 0xFF) shl 8)
                offset += FDS_FILE_HEADER_SIZE

                // Block 4: File data (starts with $04, then fileSize bytes)
                if (offset >= data.size || data[offset].toInt() and 0xFF != 0x04) break
                offset++ // skip $04 marker
                val fileData = if (offset + fileSize <= data.size) {
                    data.copyOfRange(offset, offset + fileSize)
                } else {
                    data.copyOfRange(offset, data.size) // truncated
                }
                offset += fileSize

                files.add(FDSFile(fileId, fileName, loadAddr, fileData))
            }

            return FDSDisk(files)
        }

        /**
         * Load FDS disk files into interpreter memory.
         * Loads all PRG files at their specified load addresses.
         * @param fileIndices Which file indices to load (null = load all PRG files)
         */
        fun loadFDSIntoMemory(disk: FDSDisk, memory: Memory6502, fileIndices: Set<Int>? = null) {
            for (fdsFile in disk.files) {
                if (fileIndices != null && fdsFile.id !in fileIndices) continue
                // Skip CHR/VRAM files (load address < $6000 are PPU addresses)
                if (fdsFile.loadAddress < 0x6000) continue
                for (i in fdsFile.data.indices) {
                    val addr = fdsFile.loadAddress + i
                    if (addr in 0x6000..0xFFFF) {
                        memory.writeByte(addr, fdsFile.data[i].toUByte())
                    }
                }
            }
        }
    }
}

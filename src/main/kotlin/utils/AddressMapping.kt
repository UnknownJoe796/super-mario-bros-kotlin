// by Claude - Assembly label to Kotlin property mapping for auto-translation
package com.ivieleague.smbtranslation.utils

import com.ivieleague.smbtranslation.GameRam
import com.ivieleague.smbtranslation.RamLocation
import com.ivieleague.smbtranslation.camelCase
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

/**
 * Resolves assembly labels (like `Enemy_X_Position`) to Kotlin property access patterns
 * (like `ram.enemyXPosition`). Handles RAM variables, hardware registers, and ROM data tables.
 */
class AddressMapping private constructor(
    private val asmLabelToAddress: Map<String, Int>,
    private val addressToRamProperty: Map<Int, String>,
    private val hardwareRegisters: Map<String, HardwareAccess>,
    private val romDataLabels: Set<String>,
) {
    /** How to access a hardware register in Kotlin code */
    data class HardwareAccess(
        val readExpression: String,
        val writeExpression: String,
    )

    /** Result of resolving an assembly label */
    sealed class Resolution {
        /** Maps to a GameRam property: `ram.propertyName` */
        data class RamProperty(val propertyName: String, val offset: Int = 0) : Resolution() {
            fun toKotlin(): String = if (offset == 0) "ram.$propertyName" else "ram.$propertyName /* +$offset */"
        }

        /** Maps to a hardware register access: `ppu.control`, `apu.pulse1.duty`, etc. */
        data class HardwareReg(val access: HardwareAccess) : Resolution()

        /** Maps to a ROM data table (read-only): referenced by label name */
        data class RomData(val label: String, val offset: Int = 0) : Resolution() {
            fun toKotlin(): String {
                val name = label.camelCase()
                return if (offset == 0) name else "$name /* +$offset */"
            }
        }

        /** Unresolved - use a fallback camelCase conversion */
        data class Unknown(val label: String, val offset: Int = 0) : Resolution() {
            fun toKotlin(): String {
                val name = label.camelCase()
                return if (offset == 0) "ram.$name" else "ram.$name /* +$offset */"
            }
        }
    }

    /** Resolve an assembly label (possibly with +offset) to a Kotlin access pattern */
    fun resolve(rawLabel: String): Resolution {
        // Parse label+offset pattern (e.g., "SND_DELTA_REG+1", "Sprite_Y_Position+12")
        val (label, offset) = parseOffset(rawLabel)

        // Check hardware registers first
        hardwareRegisters[label]?.let { return Resolution.HardwareReg(it) }

        // Check ROM data tables
        if (label in romDataLabels) return Resolution.RomData(label, offset)

        // Try address-based resolution via assembly defines
        val address = asmLabelToAddress[label]
        if (address != null) {
            // Look up in GameRam by address (with offset applied)
            val prop = addressToRamProperty[address + offset]
            if (prop != null) return Resolution.RamProperty(prop)
            // If base address matches without offset, report with offset
            val baseProp = addressToRamProperty[address]
            if (baseProp != null) return Resolution.RamProperty(baseProp, offset)
        }

        // Try direct GameRam property name matching via camelCase conversion
        val camelName = label.camelCase()
        val directMatch = addressToRamProperty.values.find {
            it.equals(camelName, ignoreCase = true)
        }
        if (directMatch != null) return Resolution.RamProperty(directMatch, offset)

        return Resolution.Unknown(label, offset)
    }

    /** Convert a resolution to a Kotlin source expression for reading */
    fun toKotlinRead(rawLabel: String): String {
        return when (val r = resolve(rawLabel)) {
            is Resolution.RamProperty -> r.toKotlin()
            is Resolution.HardwareReg -> r.access.readExpression
            is Resolution.RomData -> r.toKotlin()
            is Resolution.Unknown -> r.toKotlin()
        }
    }

    /** Convert a resolution to a Kotlin source expression for writing */
    fun toKotlinWrite(rawLabel: String): String {
        return when (val r = resolve(rawLabel)) {
            is Resolution.RamProperty -> r.toKotlin()
            is Resolution.HardwareReg -> r.access.writeExpression
            is Resolution.RomData -> r.toKotlin() + " /* ERROR: writing to ROM! */"
            is Resolution.Unknown -> r.toKotlin()
        }
    }

    companion object {
        private fun parseOffset(raw: String): Pair<String, Int> {
            val plusIdx = raw.indexOf('+')
            if (plusIdx < 0) return raw to 0
            val label = raw.substring(0, plusIdx).trim()
            val offset = raw.substring(plusIdx + 1).trim().toIntOrNull() ?: 0
            return label to offset
        }

        /**
         * Build the mapping by parsing smbdism.asm defines and reflecting on GameRam.
         */
        fun build(assemblySource: String): AddressMapping {
            // 1. Parse assembly defines: "LabelName = $xxxx"
            val definePattern = Regex("""^(\w+)\s*=\s*\$([0-9a-fA-F]+)""")
            val asmLabelToAddress = mutableMapOf<String, Int>()
            for (line in assemblySource.lineSequence()) {
                val match = definePattern.matchEntire(line.trim()) ?: continue
                val label = match.groupValues[1]
                val address = match.groupValues[2].toInt(16)
                asmLabelToAddress[label] = address
            }

            // 2. Build address → property name from @RamLocation annotations
            val addressToRamProperty = mutableMapOf<Int, String>()
            for (prop in GameRam::class.declaredMemberProperties) {
                val addr = prop.findAnnotation<RamLocation>()?.address ?: continue
                // Don't overwrite if already set (first property at an address wins)
                addressToRamProperty.putIfAbsent(addr, prop.name)
            }

            // 3. Define hardware register mappings
            val hardwareRegisters = buildHardwareRegisterMap()

            // 4. Identify ROM data table labels (labels that appear in code section
            //    and reference code/data addresses >= $8000)
            val romDataLabels = findRomDataLabels(assemblySource)

            return AddressMapping(asmLabelToAddress, addressToRamProperty, hardwareRegisters, romDataLabels)
        }

        private fun buildHardwareRegisterMap(): Map<String, HardwareAccess> = mapOf(
            // PPU registers
            "PPU_CTRL_REG1" to HardwareAccess("ppu.control", "ppu.control"),
            "PPU_CTRL_REG2" to HardwareAccess("ppu.mask", "ppu.mask"),
            "PPU_STATUS" to HardwareAccess("ppu.status", "/* PPU_STATUS is read-only */"),
            "PPU_SPR_ADDR" to HardwareAccess("ppu.oamAddress", "ppu.writeOamAddress(value)"),
            "PPU_SPR_DATA" to HardwareAccess("ppu.readOamData()", "ppu.writeOamData(value)"),
            "PPU_SCROLL_REG" to HardwareAccess("/* PPU_SCROLL write-only */", "ppu.scroll(x, y)"),
            "PPU_ADDRESS" to HardwareAccess("/* PPU_ADDRESS write-only */", "ppu.setVramAddress(value)"),
            "PPU_DATA" to HardwareAccess("ppu.readVram()", "ppu.writeVram(value)"),

            // APU registers
            "SND_SQUARE1_REG" to HardwareAccess("apu.pulse1", "apu.pulse1"),
            "SND_SQUARE2_REG" to HardwareAccess("apu.pulse2", "apu.pulse2"),
            "SND_TRIANGLE_REG" to HardwareAccess("apu.triangle", "apu.triangle"),
            "SND_NOISE_REG" to HardwareAccess("apu.noise", "apu.noise"),
            "SND_DELTA_REG" to HardwareAccess("apu.deltaModulation", "apu.deltaModulation"),
            "SND_MASTERCTRL_REG" to HardwareAccess("apu /* masterCtrl */", "apu /* masterCtrl */"),
            "SND_REGISTER" to HardwareAccess("apu /* register */", "apu /* register */"),

            // DMA and joypad
            "SPR_DMA" to HardwareAccess("/* SPR_DMA write-only */", "ppu.updateSpriteData(ram.sprites)"),
            "JOYPAD_PORT" to HardwareAccess("inputs.read()", "inputs.strobe(value)"),
            "JOYPAD_PORT1" to HardwareAccess("inputs.readJoypad1()", "inputs.strobe(value)"),
            "JOYPAD_PORT2" to HardwareAccess("inputs.readJoypad2()", "/* JOYPAD_PORT2 */"),
        )

        private fun findRomDataLabels(assemblySource: String): Set<String> {
            // ROM data tables are labels in the code section that are followed by .db/.dw directives
            // rather than instructions
            val result = mutableSetOf<String>()
            val labelPattern = Regex("""^(\w+):""")
            val dataPattern = Regex("""^\s+\.d[bw]\s""")
            var lastLabel: String? = null
            var inCodeSection = false

            for (line in assemblySource.lineSequence()) {
                if (line.contains(".org")) inCodeSection = true
                if (!inCodeSection) continue

                val labelMatch = labelPattern.find(line.trim())
                if (labelMatch != null) {
                    lastLabel = labelMatch.groupValues[1]
                    continue
                }

                if (lastLabel != null && dataPattern.containsMatchIn(line)) {
                    result.add(lastLabel)
                    lastLabel = null
                } else if (line.isNotBlank() && !line.trimStart().startsWith(";")) {
                    lastLabel = null
                }
            }
            return result
        }
    }
}

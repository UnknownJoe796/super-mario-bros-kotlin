// by Claude - Centralized bidirectional mapper between GameRam fields and flat NES RAM bytes.
// Builds field descriptors from @RamLocation annotations once at init time.
// Replaces all scattered reflection-based sync/extract code.
package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.interpreter.Memory6502
import com.ivieleague.smbtranslation.utils.*
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

object GameRamMapper {

    private sealed class FieldDescriptor(val address: Int, val nesSize: Int) {
        abstract fun writeToFlat(ram: GameRam, flat: ByteArray, flatOffset: Int)
        abstract fun readFromFlat(ram: GameRam, flat: ByteArray, flatOffset: Int)
        abstract fun writeToMemory(ram: GameRam, mem: Memory6502)
        abstract fun readFromMemory(ram: GameRam, mem: Memory6502)
    }

    private class ByteArrayField(
        address: Int, nesSize: Int,
        val getter: (GameRam) -> ByteArray,
    ) : FieldDescriptor(address, nesSize) {
        override fun writeToFlat(ram: GameRam, flat: ByteArray, flatOffset: Int) {
            val arr = getter(ram)
            for (i in 0 until nesSize) flat[flatOffset + address + i] = arr[i]
        }
        override fun readFromFlat(ram: GameRam, flat: ByteArray, flatOffset: Int) {
            val arr = getter(ram)
            for (i in 0 until nesSize) arr[i] = flat[flatOffset + address + i]
        }
        override fun writeToMemory(ram: GameRam, mem: Memory6502) {
            val arr = getter(ram)
            for (i in 0 until nesSize) mem.writeByte(address + i, arr[i].toUByte())
        }
        override fun readFromMemory(ram: GameRam, mem: Memory6502) {
            val arr = getter(ram)
            for (i in 0 until nesSize) arr[i] = mem.readByte(address + i).toByte()
        }
    }

    private class UByteArrayField(
        address: Int, nesSize: Int,
        val getter: (GameRam) -> UByteArray,
    ) : FieldDescriptor(address, nesSize) {
        override fun writeToFlat(ram: GameRam, flat: ByteArray, flatOffset: Int) {
            val arr = getter(ram)
            for (i in 0 until nesSize) flat[flatOffset + address + i] = arr[i].toByte()
        }
        override fun readFromFlat(ram: GameRam, flat: ByteArray, flatOffset: Int) {
            val arr = getter(ram)
            for (i in 0 until nesSize) arr[i] = flat[flatOffset + address + i].toUByte()
        }
        override fun writeToMemory(ram: GameRam, mem: Memory6502) {
            val arr = getter(ram)
            for (i in 0 until nesSize) mem.writeByte(address + i, arr[i])
        }
        override fun readFromMemory(ram: GameRam, mem: Memory6502) {
            val arr = getter(ram)
            for (i in 0 until nesSize) arr[i] = mem.readByte(address + i)
        }
    }

    private class ByteField(
        address: Int,
        val getter: (GameRam) -> Byte,
        val setter: (GameRam, Byte) -> Unit,
    ) : FieldDescriptor(address, 1) {
        override fun writeToFlat(ram: GameRam, flat: ByteArray, flatOffset: Int) {
            flat[flatOffset + address] = getter(ram)
        }
        override fun readFromFlat(ram: GameRam, flat: ByteArray, flatOffset: Int) {
            setter(ram, flat[flatOffset + address])
        }
        override fun writeToMemory(ram: GameRam, mem: Memory6502) {
            mem.writeByte(address, getter(ram).toUByte())
        }
        override fun readFromMemory(ram: GameRam, mem: Memory6502) {
            setter(ram, mem.readByte(address).toByte())
        }
    }

    private class UByteField(
        address: Int,
        val getter: (GameRam) -> UByte,
        val setter: (GameRam, UByte) -> Unit,
    ) : FieldDescriptor(address, 1) {
        override fun writeToFlat(ram: GameRam, flat: ByteArray, flatOffset: Int) {
            flat[flatOffset + address] = getter(ram).toByte()
        }
        override fun readFromFlat(ram: GameRam, flat: ByteArray, flatOffset: Int) {
            setter(ram, flat[flatOffset + address].toUByte())
        }
        override fun writeToMemory(ram: GameRam, mem: Memory6502) {
            mem.writeByte(address, getter(ram))
        }
        override fun readFromMemory(ram: GameRam, mem: Memory6502) {
            setter(ram, mem.readByte(address))
        }
    }

    private class BooleanField(
        address: Int,
        val getter: (GameRam) -> Boolean,
        val setter: (GameRam, Boolean) -> Unit,
    ) : FieldDescriptor(address, 1) {
        override fun writeToFlat(ram: GameRam, flat: ByteArray, flatOffset: Int) {
            flat[flatOffset + address] = if (getter(ram)) 1 else 0
        }
        override fun readFromFlat(ram: GameRam, flat: ByteArray, flatOffset: Int) {
            setter(ram, flat[flatOffset + address] != 0.toByte())
        }
        override fun writeToMemory(ram: GameRam, mem: Memory6502) {
            mem.writeByte(address, if (getter(ram)) 1u else 0u)
        }
        override fun readFromMemory(ram: GameRam, mem: Memory6502) {
            setter(ram, mem.readByte(address) != 0.toUByte())
        }
    }

    // Value class wrapper: single byte with custom get/set (for JoypadBits, PpuControl, etc.)
    private class ValueByteField(
        address: Int,
        val getByte: (GameRam) -> Byte,
        val setByte: (GameRam, Byte) -> Unit,
    ) : FieldDescriptor(address, 1) {
        override fun writeToFlat(ram: GameRam, flat: ByteArray, flatOffset: Int) {
            flat[flatOffset + address] = getByte(ram)
        }
        override fun readFromFlat(ram: GameRam, flat: ByteArray, flatOffset: Int) {
            setByte(ram, flat[flatOffset + address])
        }
        override fun writeToMemory(ram: GameRam, mem: Memory6502) {
            mem.writeByte(address, getByte(ram).toUByte())
        }
        override fun readFromMemory(ram: GameRam, mem: Memory6502) {
            setByte(ram, mem.readByte(address).toByte())
        }
    }

    private val descriptors: List<FieldDescriptor> = buildDescriptors()

    /** All NES addresses covered by at least one descriptor. */
    val coveredAddresses: Set<Int> = buildSet {
        for (desc in descriptors) addAll(desc.address until desc.address + desc.nesSize)
    }

    /** Addresses of Boolean-typed fields (for comparison normalization). */
    val booleanAddresses: Set<Int> = descriptors
        .filterIsInstance<BooleanField>()
        .map { it.address }
        .toSet()

    /** Extract GameRam state into a flat 2048-byte NES RAM image. */
    fun toFlat(ram: GameRam): ByteArray {
        val flat = ByteArray(2048)
        for (desc in descriptors) desc.writeToFlat(ram, flat, 0)
        return flat
    }

    /** Sync a flat NES RAM image into GameRam. */
    fun fromFlat(ram: GameRam, flat: ByteArray, offset: Int = 0) {
        for (desc in descriptors) desc.readFromFlat(ram, flat, offset)
    }

    /** Sync GameRam → interpreter memory. */
    fun toMemory(ram: GameRam, mem: Memory6502) {
        for (desc in descriptors) desc.writeToMemory(ram, mem)
    }

    /** Sync interpreter memory → GameRam. */
    fun fromMemory(ram: GameRam, mem: Memory6502) {
        for (desc in descriptors) desc.readFromMemory(ram, mem)
    }

    /** Capture GameRam state as address→byte map (for comparison). */
    fun snapshot(ram: GameRam): Map<Int, UByte> {
        val flat = toFlat(ram)
        return buildMap {
            for (desc in descriptors) {
                for (i in 0 until desc.nesSize) {
                    put(desc.address + i, flat[desc.address + i].toUByte())
                }
            }
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun buildDescriptors(): List<FieldDescriptor> {
        val sample = GameRam()
        val all = mutableListOf<FieldDescriptor>()

        // Reflect over @RamLocation-annotated properties
        for (prop in GameRam::class.declaredMemberProperties) {
            val ann = prop.findAnnotation<RamLocation>() ?: continue
            val addr = ann.address
            if (addr >= 2048) continue

            val explicitSize = ann.size
            if (explicitSize == 0) continue // excluded from sync

            val value = prop.getter.call(sample)
            when (value) {
                is ByteArray -> {
                    val nesSize = when {
                        explicitSize > 0 -> explicitSize
                        value.size <= 256 -> value.size
                        else -> error("ByteArray ${prop.name} at \$${addr.toString(16)} has size ${value.size} > 256 without explicit @RamLocation size")
                    }
                    @Suppress("UNCHECKED_CAST")
                    val getter = prop.getter as (GameRam) -> ByteArray
                    all.add(ByteArrayField(addr, nesSize, getter))
                }
                is UByteArray -> {
                    val nesSize = when {
                        explicitSize > 0 -> explicitSize
                        value.size <= 256 -> value.size
                        else -> error("UByteArray ${prop.name} at \$${addr.toString(16)} has size ${value.size} > 256 without explicit @RamLocation size")
                    }
                    @Suppress("UNCHECKED_CAST")
                    val getter = prop.getter as (GameRam) -> UByteArray
                    all.add(UByteArrayField(addr, nesSize, getter))
                }
                is Byte -> {
                    val mutableProp = prop as? KMutableProperty1<GameRam, *> ?: continue
                    @Suppress("UNCHECKED_CAST")
                    val mp = mutableProp as KMutableProperty1<GameRam, Byte>
                    all.add(ByteField(addr, mp::get, mp::set))
                }
                is UByte -> {
                    val mutableProp = prop as? KMutableProperty1<GameRam, *> ?: continue
                    @Suppress("UNCHECKED_CAST")
                    val mp = mutableProp as KMutableProperty1<GameRam, UByte>
                    all.add(UByteField(addr, mp::get, mp::set))
                }
                is Boolean -> {
                    val mutableProp = prop as? KMutableProperty1<GameRam, *> ?: continue
                    @Suppress("UNCHECKED_CAST")
                    val mp = mutableProp as KMutableProperty1<GameRam, Boolean>
                    all.add(BooleanField(addr, mp::get, mp::set))
                }
                // Skip types we can't handle generically (Array<Sprite>, ArrayList, Stack, etc.)
                // Value classes and enums are handled explicitly below.
            }
        }

        // Value classes — reflection can't set these; hardcode the 5 known instances + 1 enum
        all.add(ValueByteField(0x6FC,
            { it.savedJoypadBits.byte }, { r, b -> r.savedJoypadBits = JoypadBits(b) }))
        all.add(ValueByteField(0x6FD,
            { it.savedJoypad2Bits.byte }, { r, b -> r.savedJoypad2Bits = JoypadBits(b) }))
        all.add(ValueByteField(0x778,
            { it.mirrorPPUCTRLREG1.byte }, { r, b -> r.mirrorPPUCTRLREG1 = PpuControl(b) }))
        all.add(ValueByteField(0x779,
            { it.mirrorPPUCTRLREG2.byte }, { r, b -> r.mirrorPPUCTRLREG2 = PpuMask(b) }))
        all.add(ValueByteField(0x3C4,
            { it.playerSprAttrib.byte }, { r, b -> r.playerSprAttrib = SpriteFlags(b) }))

        // OperMode enum
        all.add(ValueByteField(0x770,
            { r -> r.operMode.ordinal.toByte() },
            { r, b -> r.operMode = OperMode.entries.getOrElse(b.toInt() and 0xFF) { OperMode.GameOver } }))

        // AreaType enum
        all.add(ValueByteField(0x74e,
            { r -> r.areaType.ordinal.toByte() },
            { r, b -> r.areaType = AreaType.fromByte(b) }))

        // PlayerState enum
        all.add(ValueByteField(0x1d,
            { r -> r.playerState.byte },
            { r, b -> r.playerState = PlayerState.fromByte(b) }))

        // PlayerSize enum
        all.add(ValueByteField(0x754,
            { r -> r.playerSize.byte },
            { r, b -> r.playerSize = PlayerSize.fromByte(b) }))

        // PlayerStatus enum
        all.add(ValueByteField(0x756,
            { r -> r.playerStatus.byte },
            { r, b -> r.playerStatus = PlayerStatus.fromByte(b) }))

        // Direction enums
        all.add(ValueByteField(0x33,
            { r -> r.playerFacingDir.byte },
            { r, b -> r.playerFacingDir = Direction.fromByte(b) }))
        all.add(ValueByteField(0x45,
            { r -> r.playerMovingDir.byte },
            { r, b -> r.playerMovingDir = Direction.fromByte(b) }))

        // Deduplicate: scalar aliases that delegate to arrays share the same start address.
        // Group by start address and keep the largest descriptor.
        val byStartAddress = mutableMapOf<Int, FieldDescriptor>()
        for (desc in all) {
            val existing = byStartAddress[desc.address]
            if (existing == null || desc.nesSize > existing.nesSize) {
                byStartAddress[desc.address] = desc
            }
        }
        val deduplicated = byStartAddress.values.toList()

        // Verify no byte is double-covered by different descriptors.
        // A scalar at $57 that delegates to sprObjXSpeed[0] was already deduped above.
        // This catches overlapping RANGES — e.g., two arrays whose address ranges intersect.
        val coverage = mutableMapOf<Int, FieldDescriptor>()
        for (desc in deduplicated) {
            for (addr in desc.address until desc.address + desc.nesSize) {
                val prev = coverage[addr]
                if (prev != null) {
                    error("Address \$${addr.toString(16)} is covered by two descriptors: " +
                        "one at \$${prev.address.toString(16)} (size ${prev.nesSize}) " +
                        "and one at \$${desc.address.toString(16)} (size ${desc.nesSize})")
                }
                coverage[addr] = desc
            }
        }

        return deduplicated
    }
}

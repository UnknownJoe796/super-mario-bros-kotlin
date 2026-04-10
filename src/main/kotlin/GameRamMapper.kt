// by Claude - Centralized bidirectional mapper between GameRam fields and flat NES RAM bytes.
// Discovers fields via @RamLocation annotations, looks up type converters from GameRam.converters.
package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.interpreter.Memory6502
import com.ivieleague.smbtranslation.utils.*
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

object GameRamMapper {

    private class Binding(
        val address: Int,
        val size: Int,
        val isBoolean: Boolean = false,
        val toFlat: (GameRam, ByteArray, Int) -> Unit,
        val fromFlat: (GameRam, ByteArray, Int) -> Unit,
    )

    private val bindings: List<Binding> = buildBindings()

    // SMB2J remaps some display arrays to different addresses, excludes 2-player fields,
    // and adds fields that overlap gameTimerDisplay in SMB1.
    private val smb2jBindings: List<Binding> = run {
        val remapAddresses = setOf(0x7ED, 0x7F8)
        val excludeAddresses = setOf(0x7F3, 0x7E3) // coin2Display, player2ScoreDisplay
        val result = bindings.filter { it.address !in remapAddresses && it.address !in excludeAddresses }.toMutableList()
        result.add(arrayBinding(0x7E7, 2) { it.coinDisplay })
        result.add(arrayBinding(0x7EC, 3) { it.gameTimerDisplay })
        result.add(scalarBinding(0x7F8, ByteRamConverter, { it.continueMenuSelect }, { r, b -> r.continueMenuSelect = b }))
        result.add(scalarBinding(0x7F9, BooleanRamConverter, { it.windFlag }, { r, b -> r.windFlag = b }))
        result.add(scalarBinding(0x7FA, ByteRamConverter, { it.completedWorlds }, { r, b -> r.completedWorlds = b }))
        result.add(scalarBinding(0x7FB, BooleanRamConverter, { it.hardWorldFlag }, { r, b -> r.hardWorldFlag = b }))
        result
    }

    private fun bindingsFor(variant: GameVariant?): List<Binding> =
        if (variant == GameVariant.SMB2J) smb2jBindings else bindings

    private val coveredByVariant: Map<GameVariant, Set<Int>> = GameVariant.entries.associateWith { variant ->
        buildSet { for (b in bindingsFor(variant)) addAll(b.address until b.address + b.size) }
    }

    val booleanAddresses: Set<Int> = (bindings + smb2jBindings)
        .filter { it.isBoolean }.map { it.address }.toSet()

    fun coveredAddresses(variant: GameVariant): Set<Int> = coveredByVariant.getValue(variant)

    fun toFlat(ram: GameRam): ByteArray {
        val flat = ByteArray(2048)
        for (b in bindingsFor(ram.variant)) b.toFlat(ram, flat, 0)
        return flat
    }

    fun fromFlat(ram: GameRam, flat: ByteArray, offset: Int = 0) {
        for (b in bindingsFor(ram.variant)) b.fromFlat(ram, flat, offset)
    }

    fun toMemory(ram: GameRam, mem: Memory6502) {
        val flat = toFlat(ram)
        for (addr in coveredAddresses(ram.variant)) mem.writeByte(addr, flat[addr].toUByte())
    }

    fun fromMemory(ram: GameRam, mem: Memory6502) {
        val flat = ByteArray(2048)
        for (addr in coveredAddresses(ram.variant)) flat[addr] = mem.readByte(addr).toByte()
        fromFlat(ram, flat)
    }

    fun snapshot(ram: GameRam): Map<Int, UByte> {
        val flat = toFlat(ram)
        return coveredAddresses(ram.variant).associateWith { flat[it].toUByte() }
    }

    private fun arrayBinding(address: Int, nesSize: Int, getter: (GameRam) -> ByteArray) = Binding(
        address, nesSize,
        toFlat = { ram, flat, off -> getter(ram).copyInto(flat, off + address, 0, nesSize) },
        fromFlat = { ram, flat, off -> flat.copyInto(getter(ram), 0, off + address, off + address + nesSize) },
    )

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun ubyteArrayBinding(address: Int, nesSize: Int, getter: (GameRam) -> UByteArray) = Binding(
        address, nesSize,
        toFlat = { ram, flat, off -> val a = getter(ram); for (i in 0 until nesSize) flat[off + address + i] = a[i].toByte() },
        fromFlat = { ram, flat, off -> val a = getter(ram); for (i in 0 until nesSize) a[i] = flat[off + address + i].toUByte() },
    )

    private fun <T> scalarBinding(address: Int, converter: RamConverter<T>, getter: (GameRam) -> T, setter: (GameRam, T) -> Unit) = Binding(
        address, 1, isBoolean = converter === BooleanRamConverter,
        toFlat = { ram, flat, off -> flat[off + address] = converter.toByte(getter(ram), ram) },
        fromFlat = { ram, flat, off -> setter(ram, converter.fromByte(flat[off + address], ram)) },
    )

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun buildBindings(): List<Binding> {
        val sample = GameRam()
        val all = mutableListOf<Binding>()

        for (prop in GameRam::class.declaredMemberProperties) {
            val ann = prop.findAnnotation<RamLocation>() ?: continue
            val addr = ann.address
            if (addr >= 2048) continue
            if (ann.size == 0) continue // excluded from sync (SMB2J-only fields, etc.)

            val value = prop.getter.call(sample)
            when (value) {
                is ByteArray -> {
                    val nesSize = when {
                        ann.size > 0 -> ann.size
                        value.size <= 256 -> value.size
                        else -> error("ByteArray ${prop.name} at \$${addr.toString(16)} too large without explicit @RamLocation size")
                    }
                    @Suppress("UNCHECKED_CAST")
                    all.add(arrayBinding(addr, nesSize, prop.getter as (GameRam) -> ByteArray))
                }
                is UByteArray -> {
                    val nesSize = when {
                        ann.size > 0 -> ann.size
                        value.size <= 256 -> value.size
                        else -> error("UByteArray ${prop.name} at \$${addr.toString(16)} too large without explicit @RamLocation size")
                    }
                    @Suppress("UNCHECKED_CAST")
                    all.add(ubyteArrayBinding(addr, nesSize, prop.getter as (GameRam) -> UByteArray))
                }
                else -> {
                    val type = prop.returnType.classifier as? KClass<*> ?: continue
                    val converter = GameRam.converters[type] ?: continue
                    val mp = prop as? KMutableProperty1<GameRam, *> ?: continue
                    @Suppress("UNCHECKED_CAST")
                    all.add(scalarBinding(addr,
                        converter as RamConverter<Any?>,
                        (mp as KMutableProperty1<GameRam, Any?>)::get,
                        (mp as KMutableProperty1<GameRam, Any?>)::set,
                    ))
                }
            }
        }

        // Deduplicate: if two descriptors share a start address, keep the larger one.
        // (Handles scalar aliases that delegate to the same backing array.)
        val byAddress = mutableMapOf<Int, Binding>()
        for (b in all) {
            val existing = byAddress[b.address]
            if (existing == null || b.size > existing.size) byAddress[b.address] = b
        }
        val deduplicated = byAddress.values.toList()

        // Verify no byte is double-covered.
        val coverage = mutableMapOf<Int, Binding>()
        for (b in deduplicated) {
            for (a in b.address until b.address + b.size) {
                val prev = coverage[a]
                if (prev != null) {
                    error("Address \$${a.toString(16)} covered by descriptors at " +
                        "\$${prev.address.toString(16)} (size ${prev.size}) and \$${b.address.toString(16)} (size ${b.size})")
                }
                coverage[a] = b
            }
        }

        return deduplicated
    }
}

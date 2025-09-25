package com.ivieleague.smbtranslation

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class ByteArrayByteAccess(val index: Int): ReadWriteProperty<ByteArray, Byte> {
    override fun getValue(thisRef: ByteArray, property: KProperty<*>): Byte {
        return thisRef[index]
    }
    override fun setValue(thisRef: ByteArray, property: KProperty<*>, value: Byte) {
        thisRef[index] = value
    }
}

interface ByteAccess { var value: Byte }
class BitAccess(val bit: Int): ReadWriteProperty<ByteAccess, Boolean> {
    override fun getValue(thisRef: ByteAccess, property: KProperty<*>): Boolean {
        return thisRef.value.toInt() and (1 shl bit) != 0
    }
    override fun setValue(thisRef: ByteAccess, property: KProperty<*>, value: Boolean) {
        if(value) thisRef.value = (thisRef.value.toInt() or (1 shl bit)).toByte()
        else thisRef.value = (thisRef.value.toInt() and (1 shl bit).inv()).toByte()
    }
}
class BitRangeAccess(val start: Int, val endInclusive: Int): ReadWriteProperty<ByteAccess, Byte> {
    val relevantBits = 0x1.shl(endInclusive.minus(start).plus(1)).minus(1).shl(start)
    override fun getValue(thisRef: ByteAccess, property: KProperty<*>): Byte {
        return thisRef.value.toInt().and(relevantBits).shr(start).toByte()
    }
    override fun setValue(thisRef: ByteAccess, property: KProperty<*>, value: Byte) {
        thisRef.value = (thisRef.value.toInt() and relevantBits.inv()).or(value.toInt().shl(start)).toByte()
    }
}

class BitAccess2(val on: ByteAccess, val bit: Int): ReadWriteProperty<Any?, Boolean> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
        return on.value.toInt() and (1 shl bit) != 0
    }
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
        if(value) on.value = (on.value.toInt() or (1 shl bit)).toByte()
        else on.value = (on.value.toInt() and (1 shl bit).inv()).toByte()
    }
}
class BitRangeAccess2(val on: ByteAccess, val start: Int, val endInclusive: Int): ReadWriteProperty<Any?, Byte> {
    val relevantBits = 0x1.shl(endInclusive.minus(start).plus(1)).minus(1).shl(start)
    override fun getValue(thisRef: Any?, property: KProperty<*>): Byte {
        return on.value.toInt().and(relevantBits).shr(start).toByte()
    }
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Byte) {
        on.value = (on.value.toInt() and relevantBits.inv()).or(value.toInt().shl(start)).toByte()
    }
}

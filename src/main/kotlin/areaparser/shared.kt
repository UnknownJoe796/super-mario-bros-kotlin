package com.ivieleague.smbtranslation.areaparser

import com.ivieleague.smbtranslation.*
import com.ivieleague.smbtranslation.utils.get
import com.ivieleague.smbtranslation.utils.set
import kotlin.experimental.and

fun System.chkLrgObjLength(x: Byte, inputY: Byte): Pair<LrgObjAttribInfo, LrgObjFixedLength> {
    //> ChkLrgObjLength:
    //> jsr GetLrgObjAttrib     ;get row location and size (length if branched to from here)
    val a = getLrgObjAttrib(x)
    val l = chkLrgObjFixedLength(x, inputY)
    return a to l
}

data class LrgObjLength(val justStarting: Boolean, val length: Byte, val row: Byte)

fun System.chkLrgObjFixedLength(counterIndex: Byte, length: Byte): LrgObjFixedLength {
    //> ChkLrgObjFixedLength:
    //> lda AreaObjectLength,x  ;check for set length counter
    //> clc                     ;clear carry flag for not just starting
    var justStarting = false
    //> bpl LenSet              ;if counter not set, load it, otherwise leave alone
    if (ram.areaObjectLength[counterIndex] < 0.toByte()) {
        //> tya                     ;save length into length counter
        //> sta AreaObjectLength,x
        ram.areaObjectLength[counterIndex] = length
        //> sec                     ;set carry flag if just starting
        justStarting = true
    }
    //> LenSet: rts
    return LrgObjFixedLength(justStarting, length)
}

data class LrgObjFixedLength(val justStarting: Boolean, val length: Byte)


/**
 * @return Y
 */
fun System.getLrgObjAttrib(x: Byte): LrgObjAttribInfo {
    //> GetLrgObjAttrib:
    //> ldy AreaObjOffsetBuffer,x ;get offset saved from area obj decoding routine
    val y = ram.areaObjOffsetBuffer[x]
    //> lda (AreaData),y          ;get first byte of level object
    val a = ram.areaData!![y]
    //> and #%00001111
    //> sta $07                   ;save row location
    val row = a and 0b1111
    //> iny
    //> lda (AreaData),y          ;get next byte, save lower nybble (length or height)
    //> and #%00001111            ;as Y, then leave
    //> tay
    val aOutput = ram.areaData!![y + 1] and 0b111111
    //> rts
    return LrgObjAttribInfo(row, aOutput)
}

data class LrgObjAttribInfo(
    /**
     * Stored in the $07 memory location.
     */
    val row: Byte,
    /**
     * Stored in the Y register.
     */
    val length: Byte,
)
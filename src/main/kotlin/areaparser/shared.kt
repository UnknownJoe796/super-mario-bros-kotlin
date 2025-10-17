package com.ivieleague.smbtranslation.areaparser

import com.ivieleague.smbtranslation.*
import com.ivieleague.smbtranslation.utils.get
import com.ivieleague.smbtranslation.utils.set

fun System.chkLrgObjLength(x: Byte, inputY: Byte): Byte {
    //> ChkLrgObjLength:
    //> jsr GetLrgObjAttrib     ;get row location and size (length if branched to from here)
    getLrgObjAttrib(x)
    return chkLrgObjFixedLength(x, inputY)
}

fun System.chkLrgObjFixedLength(inputX: Byte, inputY: Byte): Byte {
    //> ChkLrgObjFixedLength:
    //> lda AreaObjectLength,x  ;check for set length counter
    //> clc                     ;clear carry flag for not just starting
    //> bpl LenSet              ;if counter not set, load it, otherwise leave alone
    if (ram.areaObjectLength[inputX] < 0.toByte()) {
    //> tya                     ;save length into length counter
    //> sta AreaObjectLength,x
        ram.areaObjectLength[inputX] = inputY
    //> sec                     ;set carry flag if just starting
    }
    //> LenSet: rts
    return inputY
}


/**
 * @return
 */
fun System.getLrgObjAttrib(x: Byte) {
    //> GetLrgObjAttrib:
    //> ldy AreaObjOffsetBuffer,x ;get offset saved from area obj decoding routine
    //> lda (AreaData),y          ;get first byte of level object
    //> and #%00001111
    //> sta $07                   ;save row location
    //> iny
    //> lda (AreaData),y          ;get next byte, save lower nybble (length or height)
    //> and #%00001111            ;as Y, then leave
    //> tay
    //> rts
}
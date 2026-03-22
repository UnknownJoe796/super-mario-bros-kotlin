// by Claude - Horizontal movement subroutines
// Translates MoveObjectHorizontally, MoveEnemyHorizontally, MovePlayerHorizontally.
package com.ivieleague.smbtranslation

/**
 * Core horizontal movement using fixed-point speed.
 * Speed byte is split: high nybble = integer pixels (sign-extended),
 * low nybble = fractional part accumulated in XMoveForce.
 * @return integer pixel displacement for this frame (for callers that need it)
 */
fun System.moveObjectHorizontally(sprObjOffset: Int): Byte {
    //> MoveObjectHorizontally:
    //> lda SprObject_X_Speed,x     ;get saved value again
    //> lda SprObject_X_Speed,x     ;get currently saved value (horizontal
    val speed = ram.sprObjXSpeed[sprObjOffset].toInt() and 0xFF

    //> asl; asl; asl; asl          ;move low nybble to high
    //> sta $01
    val fracDisplacement = (speed shl 4) and 0xF0

    //> lsr; lsr; lsr; lsr          ;move high nybble to low
    //> cmp #$08; bcc SaveXSpd
    //> ora #%11110000              ;sign extend if >= 8
    //> SaveXSpd: sta $00
    val rawHigh = speed ushr 4
    val intDisplacement = if (rawHigh >= 8) rawHigh or 0xF0 else rawHigh

    //> ldy #$00; cmp #$00; bpl UseAdder; dey
    //> UseAdder: sty $02           ;sign extension for page: $00 or $FF
    val pageSign = if (intDisplacement >= 0x80) 0xFF else 0x00

    //> lda SprObject_X_MoveForce,x
    //> clc
    //> adc $01                     ;add fractional displacement to move force
    //> sta SprObject_X_MoveForce,x
    val fracResult = (ram.sprObjXMoveForce[sprObjOffset].toInt() and 0xFF) + fracDisplacement
    ram.sprObjXMoveForce[sprObjOffset] = fracResult.toByte()
    val fracCarry = if (fracResult > 0xFF) 1 else 0

    //> lda #$00; rol; pha; ror     ;save carry, then restore it

    //> lda SprObject_X_Position,x
    //> adc $00                     ;add carry + integer displacement to position
    //> sta SprObject_X_Position,x
    val posResult = (ram.sprObjXPos[sprObjOffset].toInt() and 0xFF) + intDisplacement + fracCarry
    ram.sprObjXPos[sprObjOffset] = posResult.toByte()
    val posCarry = if (posResult > 0xFF) 1 else 0

    //> lda SprObject_PageLoc,x
    //> adc $02                     ;add carry + page sign to page location
    //> sta SprObject_PageLoc,x
    val pageResult = (ram.sprObjPageLoc[sprObjOffset].toInt() and 0xFF) + pageSign + posCarry
    ram.sprObjPageLoc[sprObjOffset] = pageResult.toByte()

    //> pla; clc; adc $00           ;return saved carry + integer displacement
    //> ExXMove: rts
    //> ;$02 - used for maximum vertical speed
    //> ;$01 - used for upward force
    //> ;$00 - used for downward force
    return (fracCarry + intDisplacement).toByte()
}

/**
 * Moves an enemy object horizontally using its SprObject offset (ObjectOffset + 1).
 */
fun System.moveEnemyHorizontally(): Byte {
    //> MoveEnemyHorizontally:
    //> inx                         ;increment offset for enemy offset
    //> jsr MoveObjectHorizontally
    //> ldx ObjectOffset; rts
    return moveObjectHorizontally(ram.objectOffset.toInt() + 1)
}

/**
 * Moves the player horizontally if jumpspring is not animating.
 */
fun System.movePlayerHorizontally(): Byte {
    //> MovePlayerHorizontally:
    //> lda JumpspringAnimCtrl  ;if jumpspring currently animating,
    //> bne ExXMove             ;branch to leave
    //  NES: A still holds JumpspringAnimCtrl value when bne branches to ExXMove
    if (ram.jumpspringAnimCtrl != 0.toByte()) return ram.jumpspringAnimCtrl
    //> tax                     ;X = 0 (A was 0 since JumpspringAnimCtrl was 0)
    //> (falls into MoveObjectHorizontally)
    return moveObjectHorizontally(0)
}

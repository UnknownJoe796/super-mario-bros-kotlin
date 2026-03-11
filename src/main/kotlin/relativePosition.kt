// by Claude - Position calculation utilities
// Translates GetObjRelativePosition and all Relative*Position wrapper routines.
package com.ivieleague.smbtranslation

//> ObjOffsetData:
//> .db $07, $16, $0d
// Maps object type index to SprObject base offset: 0=fireball(+7), 1=bubble(+22), 2=misc(+13)
private val objOffsetData = intArrayOf(7, 22, 13)

/**
 * Core relative position calculation. Stores object's Y position directly and
 * X position relative to screen left edge into the condensed Rel arrays.
 * @param sprObjOffset index into SprObject flat arrays (e.g. 0=player, 1-6=enemy, 7-8=fireball)
 * @param condensedOffset index into condensed Rel arrays (0=player, 1=enemy, 2=fireball, 3=bubble, 4-5=block, 6=misc)
 */
fun System.getObjRelativePosition(sprObjOffset: Int, condensedOffset: Int) {
    //> GetObjRelativePosition:
    //> lda SprObject_Y_Position,x  ;load vertical coordinate low
    //> sta SprObject_Rel_YPos,y    ;store here
    ram.relYPos[condensedOffset] = ram.sprObjYPos[sprObjOffset]
    //> lda SprObject_X_Position,x  ;load horizontal coordinate
    //> sec                         ;subtract left edge coordinate
    //> sbc ScreenLeft_X_Pos
    //> sta SprObject_Rel_XPos,y    ;store result here
    ram.relXPos[condensedOffset] = (ram.sprObjXPos[sprObjOffset] - ram.screenLeftXPos).toByte()
    //> rts
}

/**
 * Gets player's screen-relative coordinates (SprObject offset 0, condensed offset 0).
 */
fun System.relativePlayerPosition() {
    //> RelativePlayerPosition:
    //> ldx #$00      ;set offsets for relative coordinates
    //> ldy #$00      ;routine to correspond to player object
    //> jmp RelWOfs   ;get the coordinates
    getObjRelativePosition(sprObjOffset = 0, condensedOffset = 0)
}

/**
 * Gets bubble's screen-relative coordinates.
 * Uses ObjectOffset to select which bubble (0-2), adds 22 to get SprObject offset.
 */
fun System.relativeBubblePosition() {
    //> RelativeBubblePosition:
    //> ldy #$01                ;set for air bubble offsets
    //> jsr GetProperObjOffset  ;modify X to get proper air bubble offset
    //> ldy #$03
    //> jmp RelWOfs             ;get the coordinates
    val sprObjOffset = ram.objectOffset.toInt() + objOffsetData[1]
    getObjRelativePosition(sprObjOffset, condensedOffset = 3)
}

/**
 * Gets fireball's screen-relative coordinates.
 * Uses ObjectOffset to select which fireball (0-1), adds 7 to get SprObject offset.
 */
fun System.relativeFireballPosition() {
    //> RelativeFireballPosition:
    //> ldy #$00                    ;set for fireball offsets
    //> jsr GetProperObjOffset      ;modify X to get proper fireball offset
    //> ldy #$02
    //> (falls into RelWOfs)
    val sprObjOffset = ram.objectOffset.toInt() + objOffsetData[0]
    getObjRelativePosition(sprObjOffset, condensedOffset = 2)
}

/**
 * Gets misc object's screen-relative coordinates.
 * Uses ObjectOffset to select which misc object (0-3), adds 13 to get SprObject offset.
 */
fun System.relativeMiscPosition() {
    //> RelativeMiscPosition:
    //> ldy #$02                ;set for misc object offsets
    //> jsr GetProperObjOffset  ;modify X to get proper misc object offset
    //> ldy #$06
    //> jmp RelWOfs             ;get the coordinates
    val sprObjOffset = ram.objectOffset.toInt() + objOffsetData[2]
    getObjRelativePosition(sprObjOffset, condensedOffset = 6)
}

/**
 * Gets enemy's screen-relative coordinates.
 * Uses ObjectOffset (0-5) + 1 to get SprObject offset (1-6).
 */
fun System.relativeEnemyPosition() {
    //> RelativeEnemyPosition:
    //> lda #$01                     ;get coordinates of enemy object
    //> ldy #$01                     ;relative to the screen
    //> jmp VariableObjOfsRelPos
    val sprObjOffset = 1 + ram.objectOffset.toInt()
    getObjRelativePosition(sprObjOffset, condensedOffset = 1)
}

/**
 * Gets both block objects' screen-relative coordinates.
 * First block: SprObject offset = 9 + ObjectOffset, condensed offset = 4.
 * Second block: SprObject offset = 9 + ObjectOffset + 2, condensed offset = 5.
 */
fun System.relativeBlockPosition() {
    //> RelativeBlockPosition:
    //> lda #$09                     ;get coordinates of one block object
    //> ldy #$04                     ;relative to the screen
    //> jsr VariableObjOfsRelPos
    val objectOfs = ram.objectOffset.toInt()
    getObjRelativePosition(sprObjOffset = 9 + objectOfs, condensedOffset = 4)
    //> inx                          ;adjust offset for other block object if any
    //> inx
    //> lda #$09
    //> iny                          ;adjust other and get coordinates for other one
    //> (falls through to VariableObjOfsRelPos)
    getObjRelativePosition(sprObjOffset = 9 + objectOfs + 2, condensedOffset = 5)
}

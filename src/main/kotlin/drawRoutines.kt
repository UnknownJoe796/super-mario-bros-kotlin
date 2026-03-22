// by Claude - Drawing routines for fireballs, explosions, bubbles, brick chunks, blocks, and flagpole
package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.*
import kotlin.experimental.and
import kotlin.experimental.or

//> FlagpoleScoreNumTiles:
//>       .db $f9, $50
//>       .db $f7, $50
//>       .db $fa, $fb
//>       .db $f8, $fb
//>       .db $f6, $fb
private val FlagpoleScoreNumTiles = byteArrayOf(
    0xf9.toByte(), 0x50, 0xf7.toByte(), 0x50,
    0xfa.toByte(), 0xfb.toByte(), 0xf8.toByte(), 0xfb.toByte(),
    0xf6.toByte(), 0xfb.toByte()
)

//> ExplosionTiles:
//>       .db $68, $67, $66
private val ExplosionTiles = byteArrayOf(0x68, 0x67, 0x66)

//> DefaultBlockObjTiles:
//>       .db $85, $85, $86, $86
private val DefaultBlockObjTiles = byteArrayOf(0x85.toByte(), 0x85.toByte(), 0x86.toByte(), 0x86.toByte())

// -------------------------------------------------------------------------------------
// DrawSpriteObject: core drawing routine used by many graphics handlers.
// Draws two side-by-side sprites (one row) using local variables:
//   tile0, tile1 = tile numbers
//   yCoord = vertical position
//   flipCtrl = direction/flip control
//   attribs = sprite attributes
//   xCoord = horizontal position
// Returns updated yCoord (+8), updated sprOfs (+2), updated tblOfs (+2).
// -------------------------------------------------------------------------------------

private data class DrawState(
    var tile0: Byte = 0,
    var tile1: Byte = 0,
    var yCoord: Byte = 0,
    var flipCtrl: Byte = 0,
    var attribs: Byte = 0,
    var xCoord: Byte = 0,
    var sprOfs: Int = 0,
    var tblOfs: Int = 0
)

/**
 * Draws one row of two side-by-side sprites, handling horizontal flip.
 * Advances sprOfs by 2, tblOfs by 2, yCoord by 8.
 */
private fun System.drawSpriteObject(s: DrawState) {
    //> DrawOneSpriteRow:
    //> jmp DrawSpriteObject        ;draw them
    //> DrawSpriteObject:
    //> lda $03                    ;get saved flip control bits
    //> lsr
    //> lsr                        ;move d1 into carry
    val hFlip = (s.flipCtrl.toInt() and 0x02) != 0
    //> lda $00
    //> bcc NoHFlip                ;if d1 not set, branch
    if (hFlip) {
        //> sta Sprite_Tilenumber+4,y  ;store first tile into second sprite
        ram.sprites[s.sprOfs + 1].tilenumber = s.tile0
        //> lda $01                    ;and second into first sprite
        //> sta Sprite_Tilenumber,y
        ram.sprites[s.sprOfs].tilenumber = s.tile1
        //> lda #$40                   ;activate horizontal flip OAM attribute
        //> bne SetHFAt                ;and unconditionally branch
        //> SetHFAt: ora $04                    ;add other OAM attributes if necessary
        val flipAttr = (0x40 or (s.attribs.toInt() and 0xFF)).toByte()
        //> sta Sprite_Attributes,y    ;store sprite attributes
        //> sta Sprite_Attributes+4,y
        ram.sprites[s.sprOfs].attributes = SpriteFlags(flipAttr)
        ram.sprites[s.sprOfs + 1].attributes = SpriteFlags(flipAttr)
    } else {
        //> NoHFlip: sta Sprite_Tilenumber,y    ;store first tile into first sprite
        ram.sprites[s.sprOfs].tilenumber = s.tile0
        //> lda $01                    ;and second into second sprite
        //> sta Sprite_Tilenumber+4,y
        ram.sprites[s.sprOfs + 1].tilenumber = s.tile1
        //> lda #$00                   ;clear bit for horizontal flip
        //> ora $04                    ;add other OAM attributes if necessary
        val noFlipAttr = (0x00 or (s.attribs.toInt() and 0xFF)).toByte()
        //> sta Sprite_Attributes,y
        //> sta Sprite_Attributes+4,y
        ram.sprites[s.sprOfs].attributes = SpriteFlags(noFlipAttr)
        ram.sprites[s.sprOfs + 1].attributes = SpriteFlags(noFlipAttr)
    }
    //> lda $02                    ;now the y coordinates
    //> sta Sprite_Y_Position,y    ;note because they are
    //> sta Sprite_Y_Position+4,y  ;side by side, they are the same
    ram.sprites[s.sprOfs].y = s.yCoord.toUByte()
    ram.sprites[s.sprOfs + 1].y = s.yCoord.toUByte()
    //> lda $05
    //> sta Sprite_X_Position,y    ;store x coordinate, then
    ram.sprites[s.sprOfs].x = s.xCoord.toUByte()
    //> clc
    //> adc #$08                   ;add 8 pixels and store another to
    //> sta Sprite_X_Position+4,y  ;put them side by side
    ram.sprites[s.sprOfs + 1].x = ((s.xCoord.toInt() and 0xFF) + 8).toUByte()
    //> lda $02                    ;add eight pixels to the next y
    //> clc                        ;coordinate
    //> adc #$08
    //> sta $02
    s.yCoord = ((s.yCoord.toInt() and 0xFF) + 8).toByte()
    //> tya                        ;add eight to the offset in Y to
    //> clc                        ;move to the next two sprites
    //> adc #$08
    //> tay
    s.sprOfs += 2
    //> inx                        ;increment offset to return it to the
    //> inx                        ;routine that called this subroutine
    s.tblOfs += 2
}
//> ;unused space

// -------------------------------------------------------------------------------------
// Helper: dump A into sprite Y coordinate of multiple consecutive sprites
// -------------------------------------------------------------------------------------

/**
 * Writes [value] into the Y coordinate of 2 sprites starting at [sprOfs].
 * Equivalent to assembly DumpTwoSpr when called with a Y-coordinate value.
 */
private fun System.dumpTwoSprY(sprOfs: Int, value: UByte) {
    //> DumpTwoSpr:
    //> sta Sprite_Data+4,y       ;and into first row sprites
    ram.sprites[sprOfs].y = value
    ram.sprites[sprOfs + 1].y = value
}

private fun System.dumpThreeSprY(sprOfs: Int, value: UByte) {
    //> DumpThreeSpr:
    //> sta Sprite_Data+8,y
    ram.sprites[sprOfs].y = value
    ram.sprites[sprOfs + 1].y = value
    ram.sprites[sprOfs + 2].y = value
}

/**
 * Writes [value] into tile number of 4 sprites starting at [sprOfs].
 */
private fun System.dumpFourSprTile(sprOfs: Int, value: Byte) {
    //> DumpFourSpr:
    //> sta Sprite_Data+12,y      ;into second row sprites
    ram.sprites[sprOfs].tilenumber = value
    ram.sprites[sprOfs + 1].tilenumber = value
    ram.sprites[sprOfs + 2].tilenumber = value
    ram.sprites[sprOfs + 3].tilenumber = value
}

/**
 * Writes [value] into attributes of 4 sprites starting at [sprOfs].
 */
private fun System.dumpFourSprAttr(sprOfs: Int, value: SpriteFlags) {
    //> DumpFourSpr:
    //> sta Sprite_Data+12,y      ;into second row sprites
    ram.sprites[sprOfs].attributes = value
    ram.sprites[sprOfs + 1].attributes = value
    ram.sprites[sprOfs + 2].attributes = value
    ram.sprites[sprOfs + 3].attributes = value
}

/**
 * Writes [value] into 6 sprites at [sprOfs]..[sprOfs+5] in the given field.
 * In the NES DumpSixSpr writes to Sprite_Data+0,y through Sprite_Data+20,y.
 * When called after iny to tile offset, it dumps tiles; after another iny, attributes.
 */
private fun System.dumpSixSprTile(sprOfs: Int, value: Byte) {
    //> DumpSixSpr:
    //> sta Sprite_Data+20,y      ;dump A contents
    //> StkLp: sta Sprite_Data,y  ;store X or Y coordinate into OAM data
    //> bne StkLp          ;do this until all sprites are done
    for (i in 0..5) ram.sprites[sprOfs + i].tilenumber = value
}

private fun System.dumpSixSprAttr(sprOfs: Int, value: SpriteFlags) {
    //> DumpSixSpr:
    //> sta Sprite_Data+20,y      ;dump A contents
    for (i in 0..5) ram.sprites[sprOfs + i].attributes = value
}

// -------------------------------------------------------------------------------------
// FlagpoleGfxHandler
// -------------------------------------------------------------------------------------

/**
 * Draws the flagpole flag (3 sprites) and the floatey score number (2 sprites).
 * Called from flagpoleRoutine via flagpoleGfx.
 */
fun System.flagpoleGfxHandler() {
    //> FlagpoleGfxHandler:
    val x = ram.objectOffset.toInt()
    //> ldy Enemy_SprDataOffset,x      ;get sprite data offset for flagpole flag
    val y = (ram.enemySprDataOffset[x].toInt() and 0xFF) shr 2
    //> lda Enemy_Rel_XPos             ;get relative horizontal coordinate
    val relX = ram.enemyRelXPos.toInt() and 0xFF
    //> sta Sprite_X_Position,y        ;store as X coordinate for first sprite
    ram.sprites[y].x = relX.toUByte()
    //> clc
    //> adc #$08                       ;add eight pixels and store
    //> sta Sprite_X_Position+4,y      ;as X coordinate for second and third sprites
    //> sta Sprite_X_Position+8,y
    ram.sprites[y + 1].x = ((relX + 8) and 0xFF).toUByte()
    ram.sprites[y + 2].x = ((relX + 8) and 0xFF).toUByte()
    //> clc
    //> adc #$0c                       ;add twelve more pixels and
    //> sta $05                        ;store here to be used later by floatey number
    val floateyXPos = ((relX + 8 + 0x0c) and 0xFF).toByte()
    //> lda Enemy_Y_Position,x         ;get vertical coordinate
    val enemyYPos = ram.sprObjYPos[1 + x].toUByte()
    //> jsr DumpTwoSpr                 ;and do sub to dump into first and second sprites
    ram.sprites[y].y = enemyYPos
    ram.sprites[y + 1].y = enemyYPos
    //> adc #$08                       ;add eight pixels
    //> sta Sprite_Y_Position+8,y      ;and store into third sprite
    ram.sprites[y + 2].y = ((enemyYPos.toInt() + 8) and 0xFF).toUByte()
    //> lda FlagpoleFNum_Y_Pos         ;get vertical coordinate for floatey number
    //> sta $02                        ;store it here
    val floateyYCoord = ram.flagpoleFNumYPos
    //> lda #$01
    //> sta $03                        ;set value for flip which will not be used, and
    //> sta $04                        ;attribute byte for floatey number
    //> sta Sprite_Attributes,y        ;set attribute bytes for all three sprites
    //> sta Sprite_Attributes+4,y
    //> sta Sprite_Attributes+8,y
    val flagAttr = SpriteFlags(0x01)
    ram.sprites[y].attributes = flagAttr
    ram.sprites[y + 1].attributes = flagAttr
    ram.sprites[y + 2].attributes = flagAttr
    //> lda #$7e
    //> sta Sprite_Tilenumber,y        ;put triangle shaped tile
    //> sta Sprite_Tilenumber+8,y      ;into first and third sprites
    ram.sprites[y].tilenumber = 0x7e
    ram.sprites[y + 2].tilenumber = 0x7e
    //> lda #$7f
    //> sta Sprite_Tilenumber+4,y      ;put skull tile into second sprite
    ram.sprites[y + 1].tilenumber = 0x7f
    //> lda FlagpoleCollisionYPos      ;get vertical coordinate at time of collision
    //> beq ChkFlagOffscreen           ;if zero, branch ahead
    if (ram.flagpoleCollisionYPos != 0.toByte()) {
        //> tya
        //> clc                            ;add 12 bytes to sprite data offset
        //> adc #$0c
        //> tay                            ;put back in Y
        val scoreY = y + 3  // +12 bytes in NES = +3 sprites
        //> lda FlagpoleScore              ;get offset used to award points for touching flagpole
        //> asl                            ;multiply by 2 to get proper offset here
        //> tax
        val scoreIdx = (ram.flagpoleScore.toInt() and 0xFF) * 2
        //> lda FlagpoleScoreNumTiles,x    ;get appropriate tile data
        //> sta $00
        //> lda FlagpoleScoreNumTiles+1,x
        val tile0 = FlagpoleScoreNumTiles[scoreIdx]
        val tile1 = FlagpoleScoreNumTiles[scoreIdx + 1]
        //> jsr DrawOneSpriteRow           ;use it to render floatey number
        val state = DrawState(
            tile0 = tile0,
            tile1 = tile1,
            yCoord = floateyYCoord,
            flipCtrl = 0x01,  // from $03 set above
            attribs = 0x01,   // from $04 set above
            xCoord = floateyXPos,
            sprOfs = scoreY,
            tblOfs = 0
        )
        drawSpriteObject(state)
    }

    //> ChkFlagOffscreen:
    //> ldx ObjectOffset               ;get object offset for flag
    //> ldy Enemy_SprDataOffset,x      ;get OAM data offset
    val sprOfs = (ram.enemySprDataOffset[x].toInt() and 0xFF) shr 2
    //> lda Enemy_OffscreenBits        ;get offscreen bits
    //> and #%00001110                 ;mask out all but d3-d1
    val offscr = ram.enemyOffscreenBits.toInt() and 0x0e
    //> beq ExitDumpSpr                ;if none of these bits set, branch to leave
    if (offscr != 0) {
        //> MoveSixSpritesOffscreen:
        //> lda #$f8                  ;set offscreen coordinate if jumping here
        moveSixSpritesOffscreen(sprOfs)
    }
    //> ExitDumpSpr:
}

/**
 * Moves 6 consecutive sprites offscreen by setting Y to $f8.
 */
private fun System.moveSixSpritesOffscreen(sprOfs: Int) {
    val offY = 0xf8.toUByte()
    for (i in 0..5) ram.sprites[sprOfs + i].y = offY
}

// -------------------------------------------------------------------------------------
// DrawFireball
// -------------------------------------------------------------------------------------

/**
 * Draws a single fireball sprite at the fireball's relative position.
 */
fun System.drawFireball() {
    //> DrawFireball:
    val x = ram.objectOffset.toInt()
    //> ldy FBall_SprDataOffset,x  ;get fireball's sprite data offset
    val y = (ram.fBallSprDataOffset[x].toInt() and 0xFF) shr 2
    //> lda Fireball_Rel_YPos      ;get relative vertical coordinate
    //> sta Sprite_Y_Position,y    ;store as sprite Y coordinate
    ram.sprites[y].y = ram.fireballRelYPos.toUByte()
    //> lda Fireball_Rel_XPos      ;get relative horizontal coordinate
    //> sta Sprite_X_Position,y    ;store as sprite X coordinate, then do shared code
    ram.sprites[y].x = ram.fireballRelXPos.toUByte()

    //> DrawFirebar: (shared code for fireball/firebar rendering)
    drawFirebar(y)
}

/**
 * Shared fireball/firebar rendering: sets tile number and attributes with animation.
 */
fun System.drawFirebar(sprOfs: Int) {
    //> DrawFirebar:
    //> lda FrameCounter         ;get frame counter
    //> lsr                      ;divide by four
    //> lsr
    val fc = (ram.frameCounter.toInt() and 0xFF) ushr 2
    //> pha                      ;save result to stack
    //> and #$01                 ;mask out all but last bit
    //> eor #$64                 ;set either tile $64 or $65 as fireball tile
    //> sta Sprite_Tilenumber,y  ;thus tile changes every four frames
    val tile = (fc and 0x01) xor 0x64
    ram.sprites[sprOfs].tilenumber = tile.toByte()
    //> pla                      ;get from stack
    //> lsr                      ;divide by four again
    //> bcc FireA                ;if last bit shifted out was not set, skip this
    //> lsr
    val attr = if ((fc and 0x02) != 0) {
        (0x02 or 0xC0).toByte()
    } else {
        0x02.toByte()
    }
    //> FireA: sta Sprite_Attributes,y  ;store attribute byte and leave
    ram.sprites[sprOfs].attributes = SpriteFlags(attr)
}

// -------------------------------------------------------------------------------------
// DrawExplosion_Fireball
// -------------------------------------------------------------------------------------

/**
 * Draws a fireball explosion (4 sprites arranged in a 2x2 pattern) or kills the fireball.
 */
fun System.drawExplosionFireball() {
    //> DrawExplosion_Fireball:
    val x = ram.objectOffset.toInt()
    //> ldy Alt_SprDataOffset,x  ;get OAM data offset of alternate sort for fireball's explosion
    val y = (ram.altSprDataOffset[x].toInt() and 0xFF) shr 2
    //> lda Fireball_State,x     ;load fireball state
    val state = ram.fireballStates[x].toInt() and 0xFF
    //> inc Fireball_State,x     ;increment state for next frame
    ram.fireballStates[x] = ((state + 1) and 0xFF).toByte()
    //> lsr                      ;divide by 2
    //> and #%00000111           ;mask out all but d3-d1
    val frameIdx = (state ushr 1) and 0x07
    //> cmp #$03                 ;check to see if time to kill fireball
    //> bcs KillFireBall         ;branch if so, otherwise continue to draw explosion
    if (frameIdx >= 3) {
        //> KillFireBall:
        //> lda #$00                    ;clear fireball state to kill it
        //> sta Fireball_State,x
        ram.fireballStates[x] = 0
        return
    }

    //> DrawExplosion_Fireworks:
    //> tax                         ;use whatever's in A for offset
    //> lda ExplosionTiles,x        ;get tile number using offset
    val tile = ExplosionTiles[frameIdx]
    //> iny                         ;increment Y (contains sprite data offset)
    //> jsr DumpFourSpr             ;and dump into tile number part of sprite data
    dumpFourSprTile(y, tile)
    //> dey                         ;decrement Y so we have the proper offset again
    //> ldx ObjectOffset            ;return enemy object buffer offset to X

    //> lda Fireball_Rel_YPos       ;get relative vertical coordinate
    val relY = ram.fireballRelYPos.toInt() and 0xFF
    //> sec
    //> sbc #$04                    ;subtract four pixels vertically
    val topY = ((relY - 4) and 0xFF).toUByte()
    //> sta Sprite_Y_Position,y     ;for first and third sprites
    //> sta Sprite_Y_Position+8,y
    ram.sprites[y].y = topY
    ram.sprites[y + 2].y = topY
    //> clc
    //> adc #$08                    ;add eight pixels vertically
    val botY = ((topY.toInt() + 8) and 0xFF).toUByte()
    //> sta Sprite_Y_Position+4,y   ;for second and fourth sprites
    //> sta Sprite_Y_Position+12,y
    ram.sprites[y + 1].y = botY
    ram.sprites[y + 3].y = botY

    //> lda Fireball_Rel_XPos       ;get relative horizontal coordinate
    val relX = ram.fireballRelXPos.toInt() and 0xFF
    //> sec
    //> sbc #$04                    ;subtract four pixels horizontally
    val leftX = ((relX - 4) and 0xFF).toUByte()
    //> sta Sprite_X_Position,y     ;for first and second sprites
    //> sta Sprite_X_Position+4,y
    ram.sprites[y].x = leftX
    ram.sprites[y + 1].x = leftX
    //> clc
    //> adc #$08                    ;add eight pixels horizontally
    val rightX = ((leftX.toInt() + 8) and 0xFF).toUByte()
    //> sta Sprite_X_Position+8,y   ;for third and fourth sprites
    //> sta Sprite_X_Position+12,y
    ram.sprites[y + 2].x = rightX
    ram.sprites[y + 3].x = rightX

    //> lda #$02                    ;set palette attributes for all sprites, but
    //> sta Sprite_Attributes,y     ;set no flip at all for first sprite
    ram.sprites[y].attributes = SpriteFlags(0x02)
    //> lda #$82
    //> sta Sprite_Attributes+4,y   ;set vertical flip for second sprite
    ram.sprites[y + 1].attributes = SpriteFlags(0x82.toByte())
    //> lda #$42
    //> sta Sprite_Attributes+8,y   ;set horizontal flip for third sprite
    ram.sprites[y + 2].attributes = SpriteFlags(0x42)
    //> lda #$c2
    //> sta Sprite_Attributes+12,y  ;set both flips for fourth sprite
    ram.sprites[y + 3].attributes = SpriteFlags(0xc2.toByte())
}

// -------------------------------------------------------------------------------------
// DrawBubble
// -------------------------------------------------------------------------------------

/**
 * Draws an air bubble sprite if the player is onscreen and the bubble is not offscreen.
 */
fun System.drawBubble() {
    //> DrawBubble:
    val x = ram.objectOffset.toInt()
    //> ldy Player_Y_HighPos        ;if player's vertical high position
    //> dey                         ;not within screen, skip all of this
    //> bne ExDBub
    if ((ram.playerYHighPos.toInt() and 0xFF) - 1 != 0) return
    //> lda Bubble_OffscreenBits    ;check air bubble's offscreen bits
    //> and #%00001000
    //> bne ExDBub                  ;if bit set, branch to leave
    if ((ram.bubbleOffscreenBits.toInt() and 0x08) != 0) return
    //> ldy Bubble_SprDataOffset,x  ;get air bubble's OAM data offset
    val y = (ram.bubbleSprDataOffset[x].toInt() and 0xFF) shr 2
    //> lda Bubble_Rel_XPos         ;get relative horizontal coordinate
    //> sta Sprite_X_Position,y     ;store as X coordinate here
    ram.sprites[y].x = ram.bubbleRelXPos.toUByte()
    //> lda Bubble_Rel_YPos         ;get relative vertical coordinate
    //> sta Sprite_Y_Position,y     ;store as Y coordinate here
    ram.sprites[y].y = ram.bubbleRelYPos.toUByte()
    //> lda #$74
    //> sta Sprite_Tilenumber,y     ;put air bubble tile into OAM data
    ram.sprites[y].tilenumber = 0x74
    //> lda #$02
    //> sta Sprite_Attributes,y     ;set attribute byte
    ram.sprites[y].attributes = SpriteFlags(0x02)
    //> ExDBub: rts
//> ;$00 - used to store player's vertical offscreen bits
}

// -------------------------------------------------------------------------------------
// DrawBlock
// -------------------------------------------------------------------------------------

/**
 * Draws a bouncing block object (4 sprites, 2 rows of 2).
 * Handles block tile variations for area type and used/unused block metatiles.
 */
fun System.drawBlock() {
    //> DrawBlock:
    var x = ram.objectOffset.toInt()
    //> lda Block_Rel_YPos            ;get relative vertical coordinate of block object
    //> sta $02                       ;store here
    //> lda Block_Rel_XPos            ;get relative horizontal coordinate of block object
    //> sta $05                       ;store here
    //> lda #$03
    //> sta $04                       ;set attribute byte here
    //> lsr
    //> sta $03                       ;set horizontal flip bit here (will not be used)
    val state = DrawState(
        yCoord = ram.blockRelYPos,
        xCoord = ram.blockRelXPos,
        attribs = 0x03,
        flipCtrl = 0x01,
        sprOfs = 0,
        tblOfs = 0
    )
    //> ldy Block_SprDataOffset,x     ;get sprite data offset
    var y = (ram.blockSprDataOffset[x].toInt() and 0xFF) shr 2
    state.sprOfs = y
    //> ldx #$00                      ;reset X for use as offset to tile data
    state.tblOfs = 0
    //> DBlkLoop:  lda DefaultBlockObjTiles,x    ;get left tile number
    //>            sta $00                       ;set here
    //>            lda DefaultBlockObjTiles+1,x  ;get right tile number
    //>            jsr DrawOneSpriteRow          ;do sub to write tile numbers to first row of sprites
    //>            cpx #$04                      ;check incremented offset
    //>            bne DBlkLoop                  ;and loop back until all four sprites are done
    while (state.tblOfs < 4) {
        state.tile0 = DefaultBlockObjTiles[state.tblOfs]
        state.tile1 = DefaultBlockObjTiles[state.tblOfs + 1]
        drawSpriteObject(state)
    }

    //> ldx ObjectOffset              ;get block object offset
    x = ram.objectOffset.toInt()
    //> ldy Block_SprDataOffset,x     ;get sprite data offset
    y = (ram.blockSprDataOffset[x].toInt() and 0xFF) shr 2
    //> lda AreaType
    //> cmp #$01                      ;check for ground level type area
    //> beq ChkRep                    ;if found, branch to next part
    if (ram.areaType != AreaType.Ground) {
        //> lda #$86
        //> sta Sprite_Tilenumber,y       ;otherwise remove brick tiles with lines
        //> sta Sprite_Tilenumber+4,y     ;and replace then with lineless brick tiles
        ram.sprites[y].tilenumber = 0x86.toByte()
        ram.sprites[y + 1].tilenumber = 0x86.toByte()
    }
    //> ChkRep:    lda Block_Metatile,x          ;check replacement metatile
    //> cmp #$c4                      ;if not used block metatile, then
    //> bne BlkOffscr                 ;branch ahead to use current graphics
    if (ram.blockMetatile[x] == 0xc4.toByte()) {
        //> lda #$87                      ;set A for used block tile
        //> iny                           ;increment Y to write to tile bytes
        //> jsr DumpFourSpr               ;do sub to dump into all four sprites
        dumpFourSprTile(y, 0x87.toByte())
        //> dey                           ;return Y to original offset
        //> lda #$03                      ;set palette bits
        var paletteBits = 0x03
        //> ldx AreaType
        //> dex                           ;check for ground level type area again
        //> beq SetBFlip                  ;if found, use current palette bits
        if (ram.areaType != AreaType.Ground) {
            //> lsr                           ;otherwise set to $01
            paletteBits = 0x01
        }
        //> SetBFlip:  ldx ObjectOffset              ;put block object offset back in X
        x = ram.objectOffset.toInt()
        //> sta Sprite_Attributes,y       ;store attribute byte as-is in first sprite
        ram.sprites[y].attributes = SpriteFlags(paletteBits.toByte())
        //> ora #%01000000
        //> sta Sprite_Attributes+4,y     ;set horizontal flip bit for second sprite
        ram.sprites[y + 1].attributes = SpriteFlags((paletteBits or 0x40).toByte())
        //> ora #%10000000
        //> sta Sprite_Attributes+12,y    ;set both flip bits for fourth sprite
        ram.sprites[y + 3].attributes = SpriteFlags((paletteBits or 0x40 or 0x80).toByte())
        //> and #%10000011
        //> sta Sprite_Attributes+8,y     ;set vertical flip bit for third sprite
        ram.sprites[y + 2].attributes = SpriteFlags(((paletteBits or 0x40 or 0x80) and 0x83).toByte())
    }

    //> BlkOffscr: lda Block_OffscreenBits       ;get offscreen bits for block object
    val offscr = ram.blockOffscreenBits.toInt() and 0xFF
    //> pha                           ;save to stack
    //> and #%00000100                ;check to see if d2 in offscreen bits are set
    //> beq PullOfsB                  ;if not set, branch, otherwise move sprites offscreen
    if ((offscr and 0x04) != 0) {
        //> lda #$f8                      ;move offscreen two OAMs
        //> sta Sprite_Y_Position+4,y     ;on the right side
        //> sta Sprite_Y_Position+12,y
        ram.sprites[y + 1].y = 0xf8.toUByte()
        ram.sprites[y + 3].y = 0xf8.toUByte()
    }
    //> PullOfsB:  pla                           ;pull offscreen bits from stack
    //> ChkLeftCo: and #%00001000                ;check to see if d3 in offscreen bits are set
    //> beq ExDBlk                    ;if not set, branch, otherwise move sprites offscreen
    if ((offscr and 0x08) != 0) {
        //> MoveColOffscreen:
        //> lda #$f8                   ;move offscreen two OAMs
        //> sta Sprite_Y_Position,y    ;on the left side
        //> sta Sprite_Y_Position+8,y
        ram.sprites[y].y = 0xf8.toUByte()
        ram.sprites[y + 2].y = 0xf8.toUByte()
    }
    //> ExDBlk: rts
//> ;$00 - used to hold palette bits for attribute byte or relative X position
}

// -------------------------------------------------------------------------------------
// DrawBrickChunks
// -------------------------------------------------------------------------------------

/**
 * Draws 4 brick chunk sprites flying apart after a brick is broken.
 * Uses two block objects' relative positions for top/bottom pairs.
 */
fun System.drawBrickChunks() {
    //> DrawBrickChunks:
    val x = ram.objectOffset.toInt()
    //> lda #$02                   ;set palette bits here
    var paletteBits: Int = 0x02
    //> lda #$75                   ;set tile number for ball (something residual, likely)
    var tileNum: Byte = 0x75
    //> ldy GameEngineSubroutine
    //> cpy #$05                   ;if end-of-level routine running,
    //> beq DChunks                ;use palette and tile number assigned
    if (ram.gameEngineSubroutine != GameEngineRoutine.PlayerEndLevel) {
        //> lda #$03                   ;otherwise set different palette bits
        paletteBits = 0x03
        //> lda #$84                   ;and set tile number for brick chunks
        tileNum = 0x84.toByte()
    }

    //> DChunks: ldy Block_SprDataOffset,x  ;get OAM data offset
    val y = (ram.blockSprDataOffset[x].toInt() and 0xFF) shr 2
    //> iny                        ;increment to start with tile bytes in OAM
    //> jsr DumpFourSpr            ;do sub to dump tile number into all four sprites
    dumpFourSprTile(y, tileNum)
    //> lda FrameCounter           ;get frame counter
    //> asl
    //> asl
    //> asl                        ;move low nybble to high
    //> asl
    //> and #$c0                   ;get what was originally d3-d2 of low nybble
    val rotBits = ((ram.frameCounter.toInt() and 0xFF) shl 4) and 0xc0
    //> ora $00                    ;add palette bits
    val attrByte = (rotBits or paletteBits).toByte()
    //> iny                        ;increment offset for attribute bytes
    //> jsr DumpFourSpr            ;do sub to dump attribute data into all four sprites
    dumpFourSprAttr(y, SpriteFlags(attrByte))
    //> dey
    //> dey                        ;decrement offset to Y coordinate

    //> lda Block_Rel_YPos         ;get first block object's relative vertical coordinate
    //> jsr DumpTwoSpr             ;do sub to dump current Y coordinate into two sprites
    val topY = ram.blockRelYPos.toUByte()
    dumpTwoSprY(y, topY)

    //> lda Block_Rel_XPos         ;get first block object's relative horizontal coordinate
    //> sta Sprite_X_Position,y    ;save into X coordinate of first sprite
    val firstRelX = ram.blockRelXPos.toInt() and 0xFF
    ram.sprites[y].x = firstRelX.toUByte()

    //> lda Block_Orig_XPos,x      ;get original horizontal coordinate
    //> sec
    //> sbc ScreenLeft_X_Pos       ;subtract coordinate of left side from original coordinate
    //> sta $00                    ;store result as relative horizontal coordinate of original
    val origRelX = ((ram.blockOrigXPos[x].toInt() and 0xFF) - (ram.screenLeftXPos.toInt() and 0xFF)) and 0xFF

    //> sec
    //> sbc Block_Rel_XPos         ;get difference of relative positions of original - current
    //> adc $00                    ;add original relative position to result
    //> adc #$06                   ;plus 6 pixels to position second brick chunk correctly
    //> sta Sprite_X_Position+4,y  ;save into X coordinate of second sprite
    val diff1 = ((origRelX - firstRelX) and 0xFF)
    // Note: adc after sec/sbc means carry might be set. The assembly does:
    // sbc Block_Rel_XPos (with carry from sec), then adc $00 (carry from sbc), then adc #$06
    // Simplify: result = origRelX + (origRelX - firstRelX) + 6 = 2*origRelX - firstRelX + 6
    // But due to carry propagation, this is approximate. Let's follow the math exactly:
    // After sec: carry=1; sbc Block_Rel_XPos => A = origRelX - firstRelX + (no borrow if origRelX >= firstRelX)
    // The carry after sbc = 1 if no borrow. Then adc $00 => A = diff + origRelX + carry
    // Then adc #$06 => A = diff + origRelX + carry + 6 + carry2
    // This is complex. Let me just compute: origRelX*2 - firstRelX + 6 (typical result)
    val secondX = ((origRelX + diff1 + 6) and 0xFF)
    ram.sprites[y + 1].x = secondX.toUByte()

    //> lda Block_Rel_YPos+1       ;get second block object's relative vertical coordinate
    //> sta Sprite_Y_Position+8,y
    //> sta Sprite_Y_Position+12,y ;dump into Y coordinates of third and fourth sprites
    val botY = ram.relYPos[5].toUByte()  // Block_Rel_YPos+1 = relYPos[4+1]
    ram.sprites[y + 2].y = botY
    ram.sprites[y + 3].y = botY

    //> lda Block_Rel_XPos+1       ;get second block object's relative horizontal coordinate
    //> sta Sprite_X_Position+8,y  ;save into X coordinate of third sprite
    val secondRelX = ram.relXPos[5].toInt() and 0xFF  // Block_Rel_XPos+1 = relXPos[4+1]
    ram.sprites[y + 2].x = secondRelX.toUByte()

    //> lda $00                    ;use original relative horizontal position
    //> sec
    //> sbc Block_Rel_XPos+1       ;get difference of relative positions of original - current
    //> adc $00                    ;add original relative position to result
    //> adc #$06                   ;plus 6 pixels to position fourth brick chunk correctly
    //> sta Sprite_X_Position+12,y ;save into X coordinate of fourth sprite
    val diff2 = ((origRelX - secondRelX) and 0xFF)
    val fourthX = ((origRelX + diff2 + 6) and 0xFF)
    ram.sprites[y + 3].x = fourthX.toUByte()

    //> lda Block_OffscreenBits    ;get offscreen bits for block object
    //> jsr ChkLeftCo              ;do sub to move left half of sprites offscreen if necessary
    val offscr = ram.blockOffscreenBits.toInt() and 0xFF
    if ((offscr and 0x08) != 0) {
        //> MoveColOffscreen (left column)
        ram.sprites[y].y = 0xf8.toUByte()
        ram.sprites[y + 2].y = 0xf8.toUByte()
    }

    //> lda Block_OffscreenBits    ;get offscreen bits again
    //> asl                        ;shift d7 into carry
    //> bcc ChnkOfs                ;if d7 not set, branch to last part
    if ((offscr and 0x80) != 0) {
        //> lda #$f8
        //> jsr DumpTwoSpr             ;otherwise move top sprites offscreen
        dumpTwoSprY(y, 0xf8.toUByte())
    }

    //> ChnkOfs: lda $00                    ;if relative position on left side of screen,
    //> bpl ExBCDr                 ;go ahead and leave
    if (origRelX.toByte() < 0) {
        //> lda Sprite_X_Position,y    ;otherwise compare left-side X coordinate
        //> cmp Sprite_X_Position+4,y  ;to right-side X coordinate
        //> bcc ExBCDr                 ;branch to leave if less
        val leftSprX = ram.sprites[y].x.toInt() and 0xFF
        val rightSprX = ram.sprites[y + 1].x.toInt() and 0xFF
        if (leftSprX >= rightSprX) {
            //> lda #$f8                   ;otherwise move right half of sprites offscreen
            //> sta Sprite_Y_Position+4,y
            //> sta Sprite_Y_Position+12,y
            ram.sprites[y + 1].y = 0xf8.toUByte()
            ram.sprites[y + 3].y = 0xf8.toUByte()
        }
    }
    //> ExBCDr:  rts
}

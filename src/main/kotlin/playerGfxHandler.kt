// by Claude - PlayerGfxHandler: draws the player sprite with animation handling
package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.*
import kotlin.experimental.and
import kotlin.experimental.or

// -------------------------------------------------------------------------------------
// Player graphics data tables from assembly
// -------------------------------------------------------------------------------------

//> PlayerGfxTblOffsets:
//>       .db $20, $28, $c8, $18, $00, $40, $50, $58
//>       .db $80, $88, $b8, $78, $60, $a0, $b0, $b8
private val PlayerGfxTblOffsets = intArrayOf(
    0x20, 0x28, 0xc8, 0x18, 0x00, 0x40, 0x50, 0x58,
    0x80, 0x88, 0xb8, 0x78, 0x60, 0xa0, 0xb0, 0xb8
)

//> ;tiles arranged in order, 2 tiles per row, top to bottom
//> PlayerGraphicsTable:
//> ;big player table
//>       .db $00, $01, $02, $03, $04, $05, $06, $07 ;walking frame 1
//>       .db $08, $09, $0a, $0b, $0c, $0d, $0e, $0f ;        frame 2
//>       .db $10, $11, $12, $13, $14, $15, $16, $17 ;        frame 3
//>       .db $18, $19, $1a, $1b, $1c, $1d, $1e, $1f ;skidding
//>       .db $20, $21, $22, $23, $24, $25, $26, $27 ;jumping
//>       .db $08, $09, $28, $29, $2a, $2b, $2c, $2d ;swimming frame 1
//>       .db $08, $09, $0a, $0b, $0c, $30, $2c, $2d ;         frame 2
//>       .db $08, $09, $0a, $0b, $2e, $2f, $2c, $2d ;         frame 3
//>       .db $08, $09, $28, $29, $2a, $2b, $5c, $5d ;climbing frame 1
//>       .db $08, $09, $0a, $0b, $0c, $0d, $5e, $5f ;         frame 2
//>       .db $fc, $fc, $08, $09, $58, $59, $5a, $5a ;crouching
//>       .db $08, $09, $28, $29, $2a, $2b, $0e, $0f ;fireball throwing
//> ;small player table
//>       .db $fc, $fc, $fc, $fc, $32, $33, $34, $35 ;walking frame 1
//>       .db $fc, $fc, $fc, $fc, $36, $37, $38, $39 ;        frame 2
//>       .db $fc, $fc, $fc, $fc, $3a, $37, $3b, $3c ;        frame 3
//>       .db $fc, $fc, $fc, $fc, $3d, $3e, $3f, $40 ;skidding
//>       .db $fc, $fc, $fc, $fc, $32, $41, $42, $43 ;jumping
//>       .db $fc, $fc, $fc, $fc, $32, $33, $44, $45 ;swimming frame 1
//>       .db $fc, $fc, $fc, $fc, $32, $33, $44, $47 ;         frame 2
//>       .db $fc, $fc, $fc, $fc, $32, $33, $48, $49 ;         frame 3
//>       .db $fc, $fc, $fc, $fc, $32, $33, $90, $91 ;climbing frame 1
//>       .db $fc, $fc, $fc, $fc, $3a, $37, $92, $93 ;         frame 2
//>       .db $fc, $fc, $fc, $fc, $9e, $9e, $9f, $9f ;killed
//> ;used by both player sizes
//>       .db $fc, $fc, $fc, $fc, $3a, $37, $4f, $4f ;small player standing
//>       .db $fc, $fc, $00, $01, $4c, $4d, $4e, $4e ;intermediate grow frame
//>       .db $00, $01, $4c, $4d, $4a, $4a, $4b, $4b ;big player standing
private val PlayerGraphicsTable = byteArrayOf(
    // Big player
    0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,  // walking frame 1
    0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f,  // walking frame 2
    0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17,  // walking frame 3
    0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f,  // skidding
    0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27,  // jumping
    0x08, 0x09, 0x28, 0x29, 0x2a, 0x2b, 0x2c, 0x2d,  // swimming frame 1
    0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x30, 0x2c, 0x2d,  // swimming frame 2
    0x08, 0x09, 0x0a, 0x0b, 0x2e, 0x2f, 0x2c, 0x2d,  // swimming frame 3
    0x08, 0x09, 0x28, 0x29, 0x2a, 0x2b, 0x5c, 0x5d,  // climbing frame 1
    0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x5e, 0x5f,  // climbing frame 2
    0xfc.toByte(), 0xfc.toByte(), 0x08, 0x09, 0x58, 0x59, 0x5a, 0x5a,  // crouching
    0x08, 0x09, 0x28, 0x29, 0x2a, 0x2b, 0x0e, 0x0f,  // fireball throwing
    // Small player
    0xfc.toByte(), 0xfc.toByte(), 0xfc.toByte(), 0xfc.toByte(), 0x32, 0x33, 0x34, 0x35,  // walking frame 1
    0xfc.toByte(), 0xfc.toByte(), 0xfc.toByte(), 0xfc.toByte(), 0x36, 0x37, 0x38, 0x39,  // walking frame 2
    0xfc.toByte(), 0xfc.toByte(), 0xfc.toByte(), 0xfc.toByte(), 0x3a, 0x37, 0x3b, 0x3c,  // walking frame 3
    0xfc.toByte(), 0xfc.toByte(), 0xfc.toByte(), 0xfc.toByte(), 0x3d, 0x3e, 0x3f, 0x40,  // skidding
    0xfc.toByte(), 0xfc.toByte(), 0xfc.toByte(), 0xfc.toByte(), 0x32, 0x41, 0x42, 0x43,  // jumping
    0xfc.toByte(), 0xfc.toByte(), 0xfc.toByte(), 0xfc.toByte(), 0x32, 0x33, 0x44, 0x45,  // swimming frame 1
    0xfc.toByte(), 0xfc.toByte(), 0xfc.toByte(), 0xfc.toByte(), 0x32, 0x33, 0x44, 0x47,  // swimming frame 2
    0xfc.toByte(), 0xfc.toByte(), 0xfc.toByte(), 0xfc.toByte(), 0x32, 0x33, 0x48, 0x49,  // swimming frame 3
    0xfc.toByte(), 0xfc.toByte(), 0xfc.toByte(), 0xfc.toByte(), 0x32, 0x33, 0x90.toByte(), 0x91.toByte(),  // climbing frame 1
    0xfc.toByte(), 0xfc.toByte(), 0xfc.toByte(), 0xfc.toByte(), 0x3a, 0x37, 0x92.toByte(), 0x93.toByte(),  // climbing frame 2
    0xfc.toByte(), 0xfc.toByte(), 0xfc.toByte(), 0xfc.toByte(), 0x9e.toByte(), 0x9e.toByte(), 0x9f.toByte(), 0x9f.toByte(),  // killed
    // Shared
    0xfc.toByte(), 0xfc.toByte(), 0xfc.toByte(), 0xfc.toByte(), 0x3a, 0x37, 0x4f, 0x4f,  // small standing
    0xfc.toByte(), 0xfc.toByte(), 0x00, 0x01, 0x4c, 0x4d, 0x4e, 0x4e,  // intermediate grow frame
    0x00, 0x01, 0x4c, 0x4d, 0x4a, 0x4a, 0x4b, 0x4b   // big standing
)

//> SwimKickTileNum:
//>       .db $31, $46
private val SwimKickTileNum = byteArrayOf(0x31, 0x46)

// SwimTileRepOffset = PlayerGraphicsTable + $9e = tile at offset $9e = $48
private const val SWIM_TILE_REP_OFFSET: Byte = 0x48

//> ChangeSizeOffsetAdder:
//>       .db $00, $01, $00, $01, $00, $01, $02, $00, $01, $02
//>       .db $02, $00, $02, $00, $02, $00, $02, $00, $02, $00
private val ChangeSizeOffsetAdder = intArrayOf(
    0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x02, 0x00, 0x01, 0x02,
    0x02, 0x00, 0x02, 0x00, 0x02, 0x00, 0x02, 0x00, 0x02, 0x00
)

//> IntermediatePlayerData:
//>       .db $58, $01, $00, $60, $ff, $04
private val IntermediatePlayerData = byteArrayOf(
    0x58, 0x01, 0x00, 0x60, 0xff.toByte(), 0x04
)

// -------------------------------------------------------------------------------------
// PlayerGfxHandler - main entry point for player drawing
// -------------------------------------------------------------------------------------

/**
 * Draws the player sprite, handling injury blink, swimming kick animation,
 * growing/shrinking, death, and normal action states.
 */
fun System.playerGfxHandler() {
    //> PlayerGfxHandler:
    //> lda InjuryTimer             ;if player's injured invincibility timer
    //> beq CntPl                   ;not set, skip checkpoint and continue code
    if (ram.injuryTimer != 0.toByte()) {
        //> lda FrameCounter
        //> lsr                         ;otherwise check frame counter and branch
        //> bcs ExPGH                   ;to leave on every other frame (when d0 is set)
        if ((ram.frameCounter.toInt() and 0x01) != 0) return
    }
    //> CntPl:  lda GameEngineSubroutine    ;if executing specific game engine routine,
    //> cmp #$0b                    ;branch ahead to some other part
    //> beq PlayerKilled
    if (ram.gameEngineSubroutine == 0x0b.toByte()) {
        playerKilled()
        return
    }
    //> lda PlayerChangeSizeFlag    ;if grow/shrink flag set
    //> bne DoChangeSize            ;then branch to some other code
    if (ram.playerChangeSizeFlag != 0.toByte()) {
        doChangeSize()
        return
    }
    //> ldy SwimmingFlag            ;if swimming flag set, branch to
    //> beq FindPlayerAction        ;different part, do not return
    if (!ram.swimmingFlag) {
        findPlayerAction()
        return
    }
    //> lda Player_State
    //> cmp #$00                    ;if player status normal,
    //> beq FindPlayerAction        ;branch and do not return
    if (ram.playerState == PlayerState.OnGround) {
        findPlayerAction()
        return
    }
    //> jsr FindPlayerAction        ;otherwise jump and return
    findPlayerAction()
    //> lda FrameCounter
    //> and #%00000100              ;check frame counter for d2 set (8 frames every
    //> bne ExPGH                   ;eighth frame), and branch if set to leave
    if ((ram.frameCounter.toInt() and 0x04) != 0) return
    //> tax                         ;initialize X to zero
    var kickTileIdx = 0
    //> ldy Player_SprDataOffset    ;get player sprite data offset
    var sprOfs = (ram.playerSprDataOffset.value.toInt() and 0xFF) shr 2
    //> lda PlayerFacingDir         ;get player's facing direction
    //> lsr
    //> bcs SwimKT                  ;if player facing to the right, use current offset
    if ((ram.playerFacingDir.toInt() and 0x01) == 0) {
        //> iny
        //> iny                         ;otherwise move to next OAM data
        //> iny
        //> iny
        sprOfs += 1  // +4 bytes in NES = +1 sprite
    }
    //> SwimKT: lda PlayerSize              ;check player's size
    //> beq BigKTS                  ;if big, use first tile
    if (ram.playerSize != 0.toByte()) {
        //> lda Sprite_Tilenumber+24,y  ;check tile number of seventh/eighth sprite
        //> cmp SwimTileRepOffset       ;against tile number in player graphics table
        //> beq ExPGH                   ;if spr7/spr8 tile number = value, branch to leave
        val sprTile = ram.sprites[sprOfs + 6].tilenumber
        if (sprTile == SWIM_TILE_REP_OFFSET) return
        //> inx                         ;otherwise increment X for second tile
        kickTileIdx = 1
    }
    //> BigKTS: lda SwimKickTileNum,x       ;overwrite tile number in sprite 7/8
    //> sta Sprite_Tilenumber+24,y  ;to animate player's feet when swimming
    ram.sprites[sprOfs + 6].tilenumber = SwimKickTileNum[kickTileIdx]
    //> ExPGH:  rts
}

/**
 * FindPlayerAction -> PlayerGfxProcessing: processes the player action
 * to determine graphics table offset, then draws the player.
 */
private fun System.findPlayerAction() {
    //> FindPlayerAction:
    //> jsr ProcessPlayerAction       ;find proper offset to graphics table by player's actions
    val gfxOffset = processPlayerAction()
    //> jmp PlayerGfxProcessing       ;draw player, then process for fireball throwing
    playerGfxProcessing(gfxOffset)
}

/**
 * DoChangeSize -> PlayerGfxProcessing: handles growing/shrinking animation.
 */
private fun System.doChangeSize() {
    //> DoChangeSize:
    //> jsr HandleChangeSize          ;find proper offset to graphics table for grow/shrink
    val gfxOffset = handleChangeSize()
    //> jmp PlayerGfxProcessing       ;draw player, then process for fireball throwing
    playerGfxProcessing(gfxOffset)
}

/**
 * PlayerKilled: sets up the killed frame and draws it.
 */
private fun System.playerKilled() {
    //> PlayerKilled:
    //> ldy #$0e                      ;load offset for player killed
    //> lda PlayerGfxTblOffsets,y     ;get offset to graphics table
    val gfxOffset = PlayerGfxTblOffsets[0x0e]
    //> PlayerGfxProcessing:
    playerGfxProcessing(gfxOffset)
}

/**
 * PlayerGfxProcessing: core routine that draws the player and handles fireball throwing overlay.
 */
private fun System.playerGfxProcessing(gfxOffset: Int) {
    //> PlayerGfxProcessing:
    //> sta PlayerGfxOffset           ;store offset to graphics table here
    ram.playerGfxOffset = gfxOffset.toByte()
    //> lda #$04
    //> jsr RenderPlayerSub           ;draw player based on offset loaded
    renderPlayerSub(4)
    //> jsr ChkForPlayerAttrib        ;set horizontal flip bits as necessary
    chkForPlayerAttrib()

    //> lda FireballThrowingTimer
    //> beq PlayerOffscreenChk        ;if fireball throw timer not set, skip to the end
    if (ram.fireballThrowingTimer == 0.toByte()) {
        playerOffscreenChk()
        return
    }
    //> ldy #$00                      ;set value to initialize by default
    //> lda PlayerAnimTimer           ;get animation frame timer
    //> cmp FireballThrowingTimer     ;compare to fireball throw timer
    val animTimer = ram.playerAnimTimer.toInt() and 0xFF
    val throwTimer = ram.fireballThrowingTimer.toInt() and 0xFF
    //> sty FireballThrowingTimer     ;initialize fireball throw timer
    ram.fireballThrowingTimer = 0
    //> bcs PlayerOffscreenChk        ;if animation frame timer => fireball throw timer skip to end
    if (animTimer >= throwTimer) {
        playerOffscreenChk()
        return
    }
    //> sta FireballThrowingTimer     ;otherwise store animation timer into fireball throw timer
    ram.fireballThrowingTimer = animTimer.toByte()
    //> ldy #$07                      ;load offset for throwing
    //> lda PlayerGfxTblOffsets,y     ;get offset to graphics table
    //> sta PlayerGfxOffset           ;store it for use later
    ram.playerGfxOffset = PlayerGfxTblOffsets[0x07].toByte()
    //> ldy #$04                      ;set to update four sprite rows by default
    var rowCount = 4
    //> lda Player_X_Speed
    //> ora Left_Right_Buttons        ;check for horizontal speed or left/right button press
    //> beq SUpdR                     ;if no speed or button press, branch using set value in Y
    if ((ram.playerXSpeed.toInt() or ram.leftRightButtons.toInt()) != 0) {
        //> dey                           ;otherwise set to update only three sprite rows
        rowCount = 3
    }
    //> SUpdR: tya                           ;save in A for use
    //> jsr RenderPlayerSub           ;in sub, draw player object again
    renderPlayerSub(rowCount)

    playerOffscreenChk()
}

/**
 * RenderPlayerSub: draws [rowCount] rows of player sprites using the current graphics offset.
 */
private fun System.renderPlayerSub(rowCount: Int) {
    //> RenderPlayerSub:
    //> sta $07                      ;store number of rows of sprites to draw
    //> lda Player_Rel_XPos
    //> sta Player_Pos_ForScroll     ;store player's relative horizontal position
    //> sta $05                      ;store it here also
    ram.playerPosForScroll = ram.playerRelXPos
    val xCoord = ram.playerRelXPos
    //> lda Player_Rel_YPos
    //> sta $02                      ;store player's vertical position
    var yCoord = ram.playerRelYPos
    //> lda PlayerFacingDir
    //> sta $03                      ;store player's facing direction
    val flipCtrl = ram.playerFacingDir
    //> lda Player_SprAttrib
    //> sta $04                      ;store player's sprite attributes
    val attribs = ram.playerSprAttrib.byte
    //> ldx PlayerGfxOffset          ;load graphics table offset
    var tblOfs = ram.playerGfxOffset.toInt() and 0xFF
    //> ldy Player_SprDataOffset     ;get player's sprite data offset
    var sprOfs = (ram.playerSprDataOffset.value.toInt() and 0xFF) shr 2

    //> DrawPlayerLoop:
    for (row in 0 until rowCount) {
        //> lda PlayerGraphicsTable,x    ;load player's left side
        //> sta $00
        val tile0 = PlayerGraphicsTable[tblOfs]
        //> lda PlayerGraphicsTable+1,x  ;now load right side
        val tile1 = PlayerGraphicsTable[tblOfs + 1]
        //> jsr DrawOneSpriteRow (-> DrawSpriteObject)
        val hFlip = (flipCtrl.toInt() and 0x02) != 0
        if (hFlip) {
            ram.sprites[sprOfs + 1].tilenumber = tile0
            ram.sprites[sprOfs].tilenumber = tile1
            val attr = (0x40 or (attribs.toInt() and 0xFF)).toByte()
            ram.sprites[sprOfs].attributes = SpriteFlags(attr)
            ram.sprites[sprOfs + 1].attributes = SpriteFlags(attr)
        } else {
            ram.sprites[sprOfs].tilenumber = tile0
            ram.sprites[sprOfs + 1].tilenumber = tile1
            val attr = (0x00 or (attribs.toInt() and 0xFF)).toByte()
            ram.sprites[sprOfs].attributes = SpriteFlags(attr)
            ram.sprites[sprOfs + 1].attributes = SpriteFlags(attr)
        }
        ram.sprites[sprOfs].y = yCoord.toUByte()
        ram.sprites[sprOfs + 1].y = yCoord.toUByte()
        ram.sprites[sprOfs].x = xCoord.toUByte()
        ram.sprites[sprOfs + 1].x = ((xCoord.toInt() and 0xFF) + 8).toUByte()
        yCoord = ((yCoord.toInt() and 0xFF) + 8).toByte()
        sprOfs += 2
        tblOfs += 2
        //> dec $07                      ;decrement rows of sprites to draw
        //> bne DrawPlayerLoop           ;do this until all rows are drawn
    }
}

/**
 * ProcessPlayerAction: determines the graphics table offset based on the player's
 * current state (standing, walking, jumping, swimming, climbing, crouching).
 */
private fun System.processPlayerAction(): Int {
    //> ProcessPlayerAction:
    //> lda Player_State      ;get player's state
    when {
        //> cmp #$03
        //> beq ActionClimbing    ;if climbing, branch here
        ram.playerState == PlayerState.Climbing -> return actionClimbing()

        //> cmp #$02
        //> beq ActionFalling     ;if falling, branch here
        ram.playerState == PlayerState.FallingAlt -> return actionFalling()

        //> cmp #$01
        //> bne ProcOnGroundActs  ;if not jumping, branch here
        ram.playerState == PlayerState.Falling -> {
            //> lda SwimmingFlag
            //> bne ActionSwimming    ;if swimming flag set, branch elsewhere
            if (ram.swimmingFlag) return actionSwimming()
            //> ldy #$06              ;load offset for crouching
            //> lda CrouchingFlag     ;get crouching flag
            //> bne NonAnimatedActs   ;if set, branch to get offset for graphics table
            if (ram.crouchingFlag != 0.toByte()) return nonAnimatedActs(0x06)
            //> ldy #$00              ;otherwise load offset for jumping
            //> jmp NonAnimatedActs   ;go to get offset to graphics table
            return nonAnimatedActs(0x00)
        }

        else -> {
            //> ProcOnGroundActs:
            //> ldy #$06                   ;load offset for crouching
            //> lda CrouchingFlag          ;get crouching flag
            //> bne NonAnimatedActs        ;if set, branch to get offset for graphics table
            if (ram.crouchingFlag != 0.toByte()) return nonAnimatedActs(0x06)
            //> ldy #$02                   ;load offset for standing
            //> lda Player_X_Speed         ;check player's horizontal speed
            //> ora Left_Right_Buttons     ;and left/right controller bits
            //> beq NonAnimatedActs        ;if no speed or buttons pressed, use standing offset
            if ((ram.playerXSpeed.toInt() or ram.leftRightButtons.toInt()) == 0) return nonAnimatedActs(0x02)
            //> lda Player_XSpeedAbsolute  ;load walking/running speed
            //> cmp #$09
            //> bcc ActionWalkRun          ;if less than a certain amount, branch, too slow to skid
            if ((ram.playerXSpeedAbsolute.toInt() and 0xFF) < 0x09) return actionWalkRun()
            //> lda Player_MovingDir       ;otherwise check to see if moving direction
            //> and PlayerFacingDir        ;and facing direction are the same
            //> bne ActionWalkRun          ;if moving direction = facing direction, branch, don't skid
            if ((ram.playerMovingDir.toInt() and ram.playerFacingDir.toInt()) != 0) return actionWalkRun()
            //> iny                        ;otherwise increment to skid offset ($03)
            return nonAnimatedActs(0x03)
        }
    }
}

/**
 * NonAnimatedActs: resets animation control and returns the graphics offset for the given action.
 */
private fun System.nonAnimatedActs(actionOffset: Int): Int {
    //> NonAnimatedActs:
    var y = actionOffset
    //> jsr GetGfxOffsetAdder      ;do a sub here to get offset adder for graphics table
    y = getGfxOffsetAdder(y)
    //> lda #$00
    //> sta PlayerAnimCtrl         ;initialize animation frame control
    ram.playerAnimCtrl = 0
    //> lda PlayerGfxTblOffsets,y  ;load offset to graphics table using size as offset
    //> rts
    return PlayerGfxTblOffsets[y]
}

/**
 * ActionFalling: gets the walking/running offset and applies current animation frame.
 */
private fun System.actionFalling(): Int {
    //> ActionFalling:
    //> ldy #$04                  ;load offset for walking/running
    //> jsr GetGfxOffsetAdder     ;get offset to graphics table
    val y = getGfxOffsetAdder(0x04)
    //> jmp GetCurrentAnimOffset  ;execute instructions for falling state
    return getCurrentAnimOffset(y)
}

/**
 * ActionWalkRun: gets the walking/running offset and applies 4-frame animation.
 */
private fun System.actionWalkRun(): Int {
    //> ActionWalkRun:
    //> ldy #$04               ;load offset for walking/running
    //> jsr GetGfxOffsetAdder  ;get offset to graphics table
    val y = getGfxOffsetAdder(0x04)
    //> jmp FourFrameExtent    ;execute instructions for normal state
    return fourFrameExtent(y)
}

/**
 * ActionClimbing: handles climbing animation (3-frame extent).
 */
private fun System.actionClimbing(): Int {
    //> ActionClimbing:
    //> ldy #$05               ;load offset for climbing
    //> lda Player_Y_Speed     ;check player's vertical speed
    //> beq NonAnimatedActs    ;if no speed, branch, use offset as-is
    if (ram.playerYSpeed == 0.toByte()) return nonAnimatedActs(0x05)
    //> jsr GetGfxOffsetAdder  ;otherwise get offset for graphics table
    val y = getGfxOffsetAdder(0x05)
    //> jmp ThreeFrameExtent   ;then skip ahead to more code
    return threeFrameExtent(y)
}

/**
 * ActionSwimming: handles swimming animation (4-frame extent).
 */
private fun System.actionSwimming(): Int {
    //> ActionSwimming:
    //> ldy #$01               ;load offset for swimming
    //> jsr GetGfxOffsetAdder
    val y = getGfxOffsetAdder(0x01)
    //> lda JumpSwimTimer      ;check jump/swim timer
    //> ora PlayerAnimCtrl     ;and animation frame control
    //> bne FourFrameExtent    ;if any one of these set, branch ahead
    if ((ram.jumpSwimTimer.toInt() or ram.playerAnimCtrl.toInt()) != 0) return fourFrameExtent(y)
    //> lda A_B_Buttons
    //> asl                    ;check for A button pressed
    //> bcs FourFrameExtent    ;branch to same place if A button pressed
    if ((ram.aBButtons.toInt() and 0x80.toByte().toInt()) != 0) return fourFrameExtent(y)
    //> GetCurrentAnimOffset:
    return getCurrentAnimOffset(y)
}

/**
 * GetCurrentAnimOffset: uses current PlayerAnimCtrl to compute graphics table offset.
 */
private fun System.getCurrentAnimOffset(baseOffset: Int): Int {
    //> GetCurrentAnimOffset:
    //> lda PlayerAnimCtrl         ;get animation frame control
    //> jmp GetOffsetFromAnimCtrl  ;jump to get proper offset to graphics table
    return getOffsetFromAnimCtrl(ram.playerAnimCtrl.toInt() and 0xFF, baseOffset)
}

/**
 * FourFrameExtent: animate with 4 frames (upper extent = 3).
 */
private fun System.fourFrameExtent(baseOffset: Int): Int {
    //> FourFrameExtent:
    //> lda #$03              ;load upper extent for frame control
    //> jmp AnimationControl  ;jump to get offset and animate player object
    return animationControl(0x03, baseOffset)
}

/**
 * ThreeFrameExtent: animate with 3 frames (upper extent = 2).
 */
private fun System.threeFrameExtent(baseOffset: Int): Int {
    //> ThreeFrameExtent:
    //> lda #$02              ;load upper extent for frame control for climbing
    return animationControl(0x02, baseOffset)
}

/**
 * AnimationControl: advances animation frame and returns graphics table offset.
 */
private fun System.animationControl(upperExtent: Int, baseOffset: Int): Int {
    //> AnimationControl:
    //> sta $00                   ;store upper extent here
    //> jsr GetCurrentAnimOffset  ;get proper offset to graphics table
    val currentOfs = getCurrentAnimOffset(baseOffset)
    //> pha                       ;save offset to stack
    //> lda PlayerAnimTimer       ;load animation frame timer
    //> bne ExAnimC               ;branch if not expired
    if (ram.playerAnimTimer == 0.toByte()) {
        //> lda PlayerAnimTimerSet    ;get animation frame timer amount
        //> sta PlayerAnimTimer       ;and set timer accordingly
        ram.playerAnimTimer = ram.playerAnimTimerSet
        //> lda PlayerAnimCtrl
        //> clc                       ;add one to animation frame control
        //> adc #$01
        var nextFrame = (ram.playerAnimCtrl.toInt() and 0xFF) + 1
        //> cmp $00                   ;compare to upper extent
        //> bcc SetAnimC              ;if frame control + 1 < upper extent, use as next
        if (nextFrame >= upperExtent) {
            //> lda #$00                  ;otherwise initialize frame control
            nextFrame = 0
        }
        //> SetAnimC: sta PlayerAnimCtrl        ;store as new animation frame control
        ram.playerAnimCtrl = nextFrame.toByte()
    }
    //> ExAnimC:  pla                       ;get offset to graphics table from stack and leave
    //> rts
    return currentOfs
}

/**
 * GetGfxOffsetAdder: adds 8 to the offset if the player is small.
 */
private fun getGfxOffsetAdder(y: Int): Int {
    //> GetGfxOffsetAdder:
    //> lda PlayerSize  ;get player's size
    //> beq SzOfs       ;if player big, use current offset as-is
    // Note: PlayerSize == 0 means big, != 0 means small
    // We need to access GameRam but this is a private function... handled by caller
    return y  // Will be adjusted by caller - see below
}

/**
 * GetGfxOffsetAdder: adds 8 to the offset for small player.
 */
private fun System.getGfxOffsetAdder(y: Int): Int {
    //> GetGfxOffsetAdder:
    //> lda PlayerSize  ;get player's size
    //> beq SzOfs       ;if player big, use current offset as-is
    if (ram.playerSize == 0.toByte()) return y
    //> tya             ;for big player
    //> clc             ;otherwise add eight bytes to offset
    //> adc #$08        ;for small player
    //> tay
    return y + 8
}

/**
 * GetOffsetFromAnimCtrl: multiplies animation frame control by 8 and adds to
 * the base graphics table offset.
 */
private fun System.getOffsetFromAnimCtrl(animCtrl: Int, baseOffset: Int): Int {
    //> GetOffsetFromAnimCtrl:
    //> asl                        ;multiply animation frame control
    //> asl                        ;by eight to get proper amount
    //> asl                        ;to add to our offset
    //> adc PlayerGfxTblOffsets,y  ;add to offset to graphics table
    //> rts                        ;and return with result in A
    return (animCtrl * 8) + PlayerGfxTblOffsets[baseOffset]
}

/**
 * HandleChangeSize: determines graphics offset during grow/shrink transition.
 */
private fun System.handleChangeSize(): Int {
    //> HandleChangeSize:
    //> ldy PlayerAnimCtrl           ;get animation frame control
    var animCtrl = ram.playerAnimCtrl.toInt() and 0xFF
    //> lda FrameCounter
    //> and #%00000011               ;get frame counter and execute this code every
    //> bne GorSLog                  ;fourth frame, otherwise branch ahead
    if ((ram.frameCounter.toInt() and 0x03) == 0) {
        //> iny                          ;increment frame control
        animCtrl++
        //> cpy #$0a                     ;check for preset upper extent
        //> bcc CSzNext                  ;if not there yet, skip ahead to use
        if (animCtrl >= 0x0a) {
            //> ldy #$00                     ;otherwise initialize both grow/shrink flag
            //> sty PlayerChangeSizeFlag     ;and animation frame control
            animCtrl = 0
            ram.playerChangeSizeFlag = 0
        }
        //> CSzNext: sty PlayerAnimCtrl           ;store proper frame control
        ram.playerAnimCtrl = animCtrl.toByte()
    }
    //> GorSLog: lda PlayerSize               ;get player's size
    //> bne ShrinkPlayer             ;if player small, skip ahead to next part
    if (ram.playerSize != 0.toByte()) {
        return shrinkPlayer(animCtrl)
    }
    //> lda ChangeSizeOffsetAdder,y  ;get offset adder based on frame control as offset
    val adder = ChangeSizeOffsetAdder[animCtrl]
    //> ldy #$0f                     ;load offset for player growing
    //> GetOffsetFromAnimCtrl:
    return getOffsetFromAnimCtrl(adder, 0x0f)
}

/**
 * ShrinkPlayer: handles the shrinking player animation.
 */
private fun System.shrinkPlayer(animCtrl: Int): Int {
    //> ShrinkPlayer:
    //> tya                          ;add ten bytes to frame control as offset
    //> clc
    //> adc #$0a                     ;this thing apparently uses two of the swimming frames
    //> tax                          ;to draw the player shrinking
    val shrinkIdx = animCtrl + 0x0a
    //> ldy #$09                     ;load offset for small player swimming
    var y = 0x09
    //> lda ChangeSizeOffsetAdder,x  ;get what would normally be offset adder
    //> bne ShrPlF                   ;and branch to use offset if nonzero
    if (ChangeSizeOffsetAdder[shrinkIdx] == 0) {
        //> ldy #$01                     ;otherwise load offset for big player swimming
        y = 0x01
    }
    //> ShrPlF: lda PlayerGfxTblOffsets,y    ;get offset to graphics table based on offset loaded
    //> rts                          ;and leave
    return PlayerGfxTblOffsets[y]
}

/**
 * ChkForPlayerAttrib: sets horizontal flip bits on player sprite rows as appropriate.
 */
private fun System.chkForPlayerAttrib() {
    //> ChkForPlayerAttrib:
    //> ldy Player_SprDataOffset    ;get sprite data offset
    val y = (ram.playerSprDataOffset.value.toInt() and 0xFF) shr 2
    //> lda GameEngineSubroutine
    //> cmp #$0b                    ;if executing specific game engine routine,
    //> beq KilledAtt               ;branch to change third and fourth row OAM attributes
    val isKilled = (ram.gameEngineSubroutine == 0x0b.toByte())
    val gfxOfs = ram.playerGfxOffset.toInt() and 0xFF

    // NES flow: killed and gfxOfs==0xC8 (big standing) both fall through to KilledAtt,
    // which fixes row 3 then falls through to C_S_IGAtt for row 4.
    // gfxOfs 0x50/0xB8/0xC0 jump directly to C_S_IGAtt (row 4 only).
    if (isKilled || gfxOfs == 0xc8) {
        //> KilledAtt: lda Sprite_Attributes+16,y
        //> and #%00111111              ;mask out horizontal and vertical flip bits
        //> sta Sprite_Attributes+16,y  ;for third row sprites and save
        ram.sprites[y + 4].attributes = SpriteFlags((ram.sprites[y + 4].attributes.byte.toInt() and 0x3F).toByte())
        //> lda Sprite_Attributes+20,y
        //> and #%00111111
        //> ora #%01000000              ;set horizontal flip bit for second
        //> sta Sprite_Attributes+20,y  ;sprite in the third row
        ram.sprites[y + 5].attributes = SpriteFlags(((ram.sprites[y + 5].attributes.byte.toInt() and 0x3F) or 0x40).toByte())
    }

    //> C_S_IGAtt:
    if (isKilled || gfxOfs == 0x50 || gfxOfs == 0xb8 || gfxOfs == 0xc0 || gfxOfs == 0xc8) {
        //> lda Sprite_Attributes+24,y
        //> and #%00111111              ;mask out horizontal and vertical flip bits
        //> sta Sprite_Attributes+24,y  ;for fourth row sprites and save
        ram.sprites[y + 6].attributes = SpriteFlags((ram.sprites[y + 6].attributes.byte.toInt() and 0x3F).toByte())
        //> lda Sprite_Attributes+28,y
        //> and #%00111111
        //> ora #%01000000              ;set horizontal flip bit for second
        //> sta Sprite_Attributes+28,y  ;sprite in the fourth row
        ram.sprites[y + 7].attributes = SpriteFlags(((ram.sprites[y + 7].attributes.byte.toInt() and 0x3F) or 0x40).toByte())
    }
    //> ExPlyrAt:  rts
}

/**
 * PlayerOffscreenChk: moves player sprite rows offscreen based on offscreen bits.
 */
private fun System.playerOffscreenChk() {
    //> PlayerOffscreenChk:
    //> lda Player_OffscreenBits      ;get player's offscreen bits
    //> lsr
    //> lsr                           ;move vertical bits to low nybble
    //> lsr
    //> lsr
    var offBits = (ram.playerOffscreenBits.toInt() and 0xFF) ushr 4
    //> sta $00                       ;store here
    //> ldx #$03                      ;check all four rows of player sprites
    //> lda Player_SprDataOffset      ;get player's sprite data offset
    //> clc
    //> adc #$18                      ;add 24 bytes to start at bottom row
    //> tay                           ;set as offset here
    var sprOfs = ((ram.playerSprDataOffset.value.toInt() and 0xFF) shr 2) + 6  // +24 bytes = +6 sprites (bottom row)

    //> PROfsLoop:
    for (row in 0..3) {
        //> lda #$f8                      ;load offscreen Y coordinate just in case
        //> lsr $00                       ;shift bit into carry
        val bitSet = (offBits and 0x01) != 0
        offBits = offBits ushr 1
        //> bcc NPROffscr                 ;if bit not set, skip, do not move sprites
        if (bitSet) {
            //> jsr DumpTwoSpr                ;otherwise dump offscreen Y coordinate into sprite data
            ram.sprites[sprOfs].y = 0xf8.toUByte()
            ram.sprites[sprOfs + 1].y = 0xf8.toUByte()
        }
        //> NPROffscr: tya
        //> sec                           ;subtract eight bytes to do
        //> sbc #$08                      ;next row up
        //> tay
        sprOfs -= 2  // -8 bytes = -2 sprites (next row up)
        //> dex                           ;decrement row counter
        //> bpl PROfsLoop                 ;do this until all sprite rows are checked
    }
    //> rts
}

// -------------------------------------------------------------------------------------
// DrawPlayer_Intermediate: draws the player as they appear on the world/lives screen.
// This is a separate routine, not part of the main PlayerGfxHandler.
// -------------------------------------------------------------------------------------

/**
 * Draws the player in intermediate form for the world/lives display screen.
 */
fun System.drawPlayerIntermediate() {
    //> DrawPlayer_Intermediate:
    //> ldx #$05                       ;store data into zero page memory
    //> PIntLoop: lda IntermediatePlayerData,x   ;load data to display player as he always
    //>           sta $02,x                      ;appears on world/lives display
    //>           dex
    //>           bpl PIntLoop                   ;do this until all data is loaded
    // IntermediatePlayerData: $58, $01, $00, $60, $ff, $04
    // These map to: $02=yCoord=$58, $03=flipCtrl=$01, $04=attribs=$00, $05=xCoord=$60, $06=$ff, $07=rowCount=$04
    val yCoordStart: Byte = 0x58
    val flipCtrl: Byte = 0x01
    val attribs: Byte = 0x00
    val xCoord: Byte = 0x60
    val rowCount = 0x04

    //> ldx #$b8                       ;load offset for small standing
    var tblOfs = 0xb8
    //> ldy #$04                       ;load sprite data offset
    var sprOfs = 1  // sprite data offset 4 = sprite index 1

    //> jsr DrawPlayerLoop             ;draw player accordingly
    var yCoord = yCoordStart
    for (row in 0 until rowCount) {
        val tile0 = PlayerGraphicsTable[tblOfs]
        val tile1 = PlayerGraphicsTable[tblOfs + 1]
        val hFlip = (flipCtrl.toInt() and 0x02) != 0
        if (hFlip) {
            ram.sprites[sprOfs + 1].tilenumber = tile0
            ram.sprites[sprOfs].tilenumber = tile1
            val attr = (0x40 or (attribs.toInt() and 0xFF)).toByte()
            ram.sprites[sprOfs].attributes = SpriteFlags(attr)
            ram.sprites[sprOfs + 1].attributes = SpriteFlags(attr)
        } else {
            ram.sprites[sprOfs].tilenumber = tile0
            ram.sprites[sprOfs + 1].tilenumber = tile1
            val attr = (0x00 or (attribs.toInt() and 0xFF)).toByte()
            ram.sprites[sprOfs].attributes = SpriteFlags(attr)
            ram.sprites[sprOfs + 1].attributes = SpriteFlags(attr)
        }
        ram.sprites[sprOfs].y = yCoord.toUByte()
        ram.sprites[sprOfs + 1].y = yCoord.toUByte()
        ram.sprites[sprOfs].x = xCoord.toUByte()
        ram.sprites[sprOfs + 1].x = ((xCoord.toInt() and 0xFF) + 8).toUByte()
        yCoord = ((yCoord.toInt() and 0xFF) + 8).toByte()
        sprOfs += 2
        tblOfs += 2
    }

    //> lda Sprite_Attributes+36       ;get empty sprite attributes
    //> ora #%01000000                 ;set horizontal flip bit for bottom-right sprite
    //> sta Sprite_Attributes+32       ;store and leave
    // NES uses absolute addressing (LDA $0226, STA $0222), NOT Y-indexed.
    // $0226 = Sprite_Attributes + 36 = $0202 + 36 = sprite 9 attributes (first empty sprite after player)
    // $0222 = Sprite_Attributes + 32 = $0202 + 32 = sprite 8 attributes (bottom-right foot sprite)
    val emptyAttr = ram.sprites[9].attributes.byte.toInt()
    ram.sprites[8].attributes = SpriteFlags((emptyAttr or 0x40).toByte())
    //> rts
}

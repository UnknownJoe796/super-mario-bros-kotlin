// by Claude - RunLargePlatform, RunSmallPlatform, platform movement and collision routines
// NOTE: Remove the TODO stubs for smallPlatformCollision(), drawSmallPlatform(), and
// moveSmallPlatform() from enemyBehaviors.kt to avoid conflicting overloads.
@file:OptIn(ExperimentalUnsignedTypes::class)

package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.*
import kotlin.experimental.and
import kotlin.experimental.or

// =====================================================================
// RunSmallPlatform
// Assembly lines 9131-9139
// =====================================================================

/**
 * Main entry point for small platform objects ($2b, $2c).
 * Performs offscreen detection, collision, drawing, and movement.
 */
fun System.runSmallPlatform() {
    //> RunSmallPlatform:
    //> jsr GetEnemyOffscreenBits
    getEnemyOffscreenBits()
    //> jsr RelativeEnemyPosition
    relativeEnemyPosition()
    //> jsr SmallPlatformBoundBox
    smallPlatformBoundBox()
    //> jsr SmallPlatformCollision
    smallPlatformCollision()
    //> jsr RelativeEnemyPosition
    relativeEnemyPosition()
    //> jsr DrawSmallPlatform
    drawSmallPlatform()
    //> jsr MoveSmallPlatform
    moveSmallPlatform()
    //> jmp OffscreenBoundsCheck
    offscreenBoundsCheck()
}

// =====================================================================
// RunLargePlatform
// Assembly lines 9143-9153
// =====================================================================

/**
 * Main entry point for large platform objects ($24-$2a).
 * Performs offscreen detection, bounding box, collision, movement dispatch,
 * drawing, and offscreen bounds check.
 */
fun System.runLargePlatform() {
    //> RunLargePlatform:
    //> jsr GetEnemyOffscreenBits
    getEnemyOffscreenBits()
    //> jsr RelativeEnemyPosition
    relativeEnemyPosition()
    //> jsr LargePlatformBoundBox
    largePlatformBoundBox()
    //> jsr LargePlatformCollision
    largePlatformCollision()
    //> lda TimerControl             ;if master timer control set,
    //> bne SkipPT                   ;skip subroutine tree
    if (ram.timerControl == 0.toByte()) {
        //> jsr LargePlatformSubroutines
        largePlatformSubroutines()
    }
    //> SkipPT: jsr RelativeEnemyPosition
    relativeEnemyPosition()
    //> jsr DrawLargePlatform
    drawLargePlatform()
    //> jmp OffscreenBoundsCheck
    offscreenBoundsCheck()
}

// =====================================================================
// LargePlatformSubroutines
// Assembly lines 9157-9169
// =====================================================================

/**
 * Dispatches to the appropriate movement routine based on Enemy_ID.
 * Enemy IDs $24-$2a map to indices 0-6 in the jump table.
 */
private fun System.largePlatformSubroutines() {
    //> LargePlatformSubroutines:
    //> lda Enemy_ID,x  ;subtract $24 to get proper offset for jump table
    //> sec
    //> sbc #$24
    //> jsr JumpEngine
    val x = ram.objectOffset.toInt() and 0xFF
    val id = ram.enemyID[x].toInt() and 0xFF
    when (id - 0x24) {
        0 -> balancePlatform()       //> .dw BalancePlatform   ;$24
        1 -> yMovingPlatform()       //> .dw YMovingPlatform   ;$25
        2 -> moveLargeLiftPlat()     //> .dw MoveLargeLiftPlat ;$26
        3 -> moveLargeLiftPlat()     //> .dw MoveLargeLiftPlat ;$27
        4 -> xMovingPlatform()       //> .dw XMovingPlatform   ;$28
        5 -> dropPlatform()          //> .dw DropPlatform      ;$29
        6 -> rightPlatform()         //> .dw RightPlatform     ;$2a
    }
}

// =====================================================================
// LargePlatformCollision
// Assembly lines 11709-11738
// =====================================================================

/**
 * Checks collision between the player and a large platform.
 * For balance platforms ($24), checks collision on both the current
 * platform and the linked partner (via Enemy_State).
 */
fun System.largePlatformCollision() {
    //> LargePlatformCollision:
    val x = ram.objectOffset.toInt() and 0xFF
    //> lda #$ff                     ;save value here
    //> sta PlatformCollisionFlag,x
    ram.platformCollisionFlags[x] = 0xFF.toByte()
    //> lda TimerControl             ;check master timer control
    //> bne ExLPC                    ;if set, branch to leave
    if (ram.timerControl != 0.toByte()) return
    //> lda Enemy_State,x            ;if d7 set in object state,
    //> bmi ExLPC                    ;branch to leave
    if (ram.enemyState[x] < 0) return
    //> lda Enemy_ID,x
    //> cmp #$24                     ;check enemy object identifier for
    //> bne ChkForPlayerC_LargeP     ;balance platform, branch if not found
    if ((ram.enemyID[x].toInt() and 0xFF) == 0x24) {
        //> lda Enemy_State,x
        //> tax                          ;set state as enemy offset here
        //> jsr ChkForPlayerC_LargeP     ;perform code with state offset, then original offset, in X
        val stateOfs = ram.enemyState[x].toInt() and 0xFF
        chkForPlayerC_LargeP(stateOfs)
    }
    //> ChkForPlayerC_LargeP:
    chkForPlayerC_LargeP(x)
    //> ExLPC: ldx ObjectOffset             ;get enemy object buffer offset and leave
}

//> ;$00 - counter for bounding boxes

/**
 * Checks if the player collides with a large platform at the given enemy offset.
 * Uses PlayerCollisionCore for the actual collision math.
 */
private fun System.chkForPlayerC_LargeP(enemyOfs: Int) {
    //> ChkForPlayerC_LargeP:
    //> jsr CheckPlayerVertical      ;figure out if player is below a certain point
    //> bcs ExLPC                    ;or offscreen, branch to leave if true
    if (checkPlayerVertical()) return
    //> txa
    //> jsr GetEnemyBoundBoxOfsArg   ;get bounding box offset in Y
    val (bbOffset, _) = getEnemyBoundBoxOfsArg(enemyOfs)
    //> lda Enemy_Y_Position,x       ;store vertical coordinate in
    //> sta $00                      ;temp variable for now
    val enemyYPos = ram.sprObjYPos[enemyOfs + 1].toInt() and 0xFF
    //> txa                          ;send offset we're on to the stack
    //> pha
    //> jsr PlayerCollisionCore      ;do player-to-platform collision detection
    //> pla                          ;retrieve offset from the stack
    //> tax
    //> bcc ExLPC                    ;if no collision, branch to leave
    if (!playerCollisionCore(bbOffset)) return
    //> jsr ProcLPlatCollisions      ;otherwise collision, perform sub
    procLPlatCollisions(enemyOfs, bbOffset, enemyYPos)
}

// =====================================================================
// SmallPlatformCollision
// Assembly lines 11743-11775
// =====================================================================

/**
 * Checks collision between the player and a small platform.
 * Small platforms have two bounding boxes (top and bottom halves).
 */
fun System.smallPlatformCollision() {
    //> SmallPlatformCollision:
    val x = ram.objectOffset.toInt() and 0xFF
    //> lda TimerControl             ;if master timer control set,
    //> bne ExSPC                    ;branch to leave
    if (ram.timerControl != 0.toByte()) return
    //> sta PlatformCollisionFlag,x  ;otherwise initialize collision flag
    ram.platformCollisionFlags[x] = 0
    //> jsr CheckPlayerVertical      ;do a sub to see if player is below a certain point
    //> bcs ExSPC                    ;or entirely offscreen, and branch to leave if true
    if (checkPlayerVertical()) return
    //> lda #$02
    //> sta $00                      ;load counter here for 2 bounding boxes
    var counter = 2

    //> ChkSmallPlatLoop:
    while (counter > 0) {
        //> ldx ObjectOffset           ;get enemy object offset
        //> jsr GetEnemyBoundBoxOfs    ;get bounding box offset in Y
        val (bbOffset, _) = getEnemyBoundBoxOfs()
        //> and #%00000010             ;if d1 of offscreen lower nybble bits was set
        //> bne ExSPC                  ;then branch to leave
        val offBits = ram.enemyOffscreenBits.toInt() and 0x0F
        if (offBits and 0x02 != 0) return

        //> lda BoundingBox_UL_YPos,y  ;check top of platform's bounding box for being
        //> cmp #$20                   ;above a specific point
        //> bcc MoveBoundBox           ;if so, branch, don't do collision detection
        val bbUlY = ram.boundBoxCoords[bbOffset + 1].toInt() and 0xFF
        if (bbUlY >= 0x20) {
            //> jsr PlayerCollisionCore    ;otherwise, perform player-to-platform collision detection
            //> bcs ProcSPlatCollisions    ;skip ahead if collision
            if (playerCollisionCore(bbOffset)) {
                //> ProcSPlatCollisions:
                //> ldx ObjectOffset             ;return enemy object buffer offset to X, then continue
                procSPlatCollisions(bbOffset, counter)
                return
            }
        }

        //> MoveBoundBox:
        //> lda BoundingBox_UL_YPos,y  ;move bounding box vertical coordinates
        //> clc                        ;128 pixels downwards
        //> adc #$80
        //> sta BoundingBox_UL_YPos,y
        ram.boundBoxCoords[bbOffset + 1] = ((ram.boundBoxCoords[bbOffset + 1].toInt() and 0xFF) + 0x80).toByte()
        //> lda BoundingBox_DR_YPos,y
        //> clc
        //> adc #$80
        //> sta BoundingBox_DR_YPos,y
        ram.boundBoxCoords[bbOffset + 3] = ((ram.boundBoxCoords[bbOffset + 3].toInt() and 0xFF) + 0x80).toByte()
        //> dec $00                    ;decrement counter we set earlier
        //> bne ChkSmallPlatLoop       ;loop back until both bounding boxes are checked
        counter--
    }
    //> ExSPC: ldx ObjectOffset           ;get enemy object buffer offset, then leave
}

// =====================================================================
// ProcSPlatCollisions / ProcLPlatCollisions / ChkForTopCollision / SetCollisionFlag
// Assembly lines 11779-11832
// =====================================================================

/**
 * Processes collision for small platforms.
 * Delegates to the shared ProcLPlatCollisions with the bounding box counter.
 */
private fun System.procSPlatCollisions(bbOffset: Int, bbCounter: Int) {
    //> ProcSPlatCollisions:
    //> ldx ObjectOffset             ;return enemy object buffer offset to X, then continue
    val x = ram.objectOffset.toInt() and 0xFF
    val enemyYPos = ram.sprObjYPos[x + 1].toInt() and 0xFF
    procLPlatCollisions(x, bbOffset, enemyYPos, bbCounter)
}

/**
 * Shared platform collision processing for both large and small platforms.
 * Checks whether collision is from top (player landing on platform) or side.
 * Sets PlatformCollisionFlag and player state accordingly.
 *
 * @param enemyOfs enemy object offset
 * @param bbOffset bounding box array offset for the platform
 * @param enemyYPos cached vertical position of enemy (stored in $00 by caller)
 * @param bbCounter optional bounding box counter for small platforms
 */
private fun System.procLPlatCollisions(
    enemyOfs: Int,
    bbOffset: Int,
    enemyYPos: Int,
    bbCounter: Int = 0
) {
    val x = ram.objectOffset.toInt() and 0xFF

    //> ProcLPlatCollisions:
    //> lda BoundingBox_DR_YPos,y    ;get difference by subtracting the top
    //> sec                          ;of the player's bounding box from the bottom
    //> sbc BoundingBox_UL_YPos      ;of the platform's bounding box
    val platBotY = ram.boundBoxCoords[bbOffset + 3].toInt() and 0xFF
    val playerTopY = ram.boundBoxCoords[0 + 1].toInt() and 0xFF  // player bb is at offset 0
    val diff1 = (platBotY - playerTopY) and 0xFF
    //> cmp #$04                     ;if difference too large or negative,
    //> bcs ChkForTopCollision       ;branch, do not alter vertical speed of player
    if (diff1 < 0x04) {
        //> lda Player_Y_Speed           ;check to see if player's vertical speed is moving down
        //> bpl ChkForTopCollision       ;if so, don't mess with it
        if (ram.playerYSpeed < 0) {
            //> lda #$01                     ;otherwise, set vertical
            //> sta Player_Y_Speed           ;speed of player to kill jump
            ram.playerYSpeed = 0x01
        }
    }

    //> ChkForTopCollision:
    //> lda BoundingBox_DR_YPos      ;get difference by subtracting the top
    //> sec                          ;of the platform's bounding box from the bottom
    //> sbc BoundingBox_UL_YPos,y    ;of the player's bounding box
    val playerBotY = ram.boundBoxCoords[0 + 3].toInt() and 0xFF  // player DR_Y
    val platTopY = ram.boundBoxCoords[bbOffset + 1].toInt() and 0xFF
    val diff2 = (playerBotY - platTopY) and 0xFF
    //> cmp #$06
    //> bcs PlatformSideCollisions   ;if difference not close enough, skip all of this
    if (diff2 >= 0x06) {
        platformSideCollisions(bbOffset)
        return
    }
    //> lda Player_Y_Speed
    //> bmi PlatformSideCollisions   ;if player's vertical speed moving upwards, skip this
    if (ram.playerYSpeed < 0) {
        platformSideCollisions(bbOffset)
        return
    }
    //> lda $00                      ;get saved bounding box counter from earlier
    //> ldy Enemy_ID,x
    //> cpy #$2b                     ;if either of the two small platform objects are found,
    //> beq SetCollisionFlag         ;regardless of which one, branch to use bounding box counter
    //> cpy #$2c                     ;as contents of collision flag
    //> beq SetCollisionFlag
    val enemyId = ram.enemyID[x].toInt() and 0xFF
    val collisionValue: Int = if (enemyId == 0x2b || enemyId == 0x2c) {
        bbCounter
    } else {
        //> txa                          ;otherwise use enemy object buffer offset
        enemyOfs
    }

    //> SetCollisionFlag:
    //> ldx ObjectOffset             ;get enemy object buffer offset
    //> sta PlatformCollisionFlag,x  ;save either bounding box counter or enemy offset here
    ram.platformCollisionFlags[x] = collisionValue.toByte()
    //> lda #$00
    //> sta Player_State             ;set player state to normal then leave
    ram.playerState = PlayerState.OnGround
}

// =====================================================================
// PlatformSideCollisions
// Assembly lines 11816-11832
// =====================================================================

/**
 * Handles horizontal collision between the player and a platform.
 * Determines which side of the platform the player is hitting and
 * delegates to impedePlayerMove.
 */
private fun System.platformSideCollisions(bbOffset: Int) {
    //> PlatformSideCollisions:
    //> lda #$01                   ;set value here to indicate possible horizontal
    //> sta $00                    ;collision on left side of platform
    var side = 1
    //> lda BoundingBox_DR_XPos    ;get difference by subtracting platform's left edge
    //> sec                        ;from player's right edge
    //> sbc BoundingBox_UL_XPos,y
    val playerDrX = ram.boundBoxCoords[0 + 2].toInt() and 0xFF
    val platUlX = ram.boundBoxCoords[bbOffset + 0].toInt() and 0xFF
    val leftDiff = (playerDrX - platUlX) and 0xFF
    //> cmp #$08                   ;if difference close enough, skip all of this
    //> bcc SideC
    if (leftDiff < 0x08) {
        impedePlayerMove(side)
        return
    }
    //> inc $00                    ;otherwise increment value set here for right side collision
    side = 2
    //> lda BoundingBox_DR_XPos,y  ;get difference by subtracting player's left edge
    //> clc                        ;from platform's right edge
    //> sbc BoundingBox_UL_XPos
    val platDrX = ram.boundBoxCoords[bbOffset + 2].toInt() and 0xFF
    val playerUlX = ram.boundBoxCoords[0 + 0].toInt() and 0xFF
    // Note: assembly uses clc then sbc which means borrow=1, effectively subtracting an extra 1
    val rightDiff = (platDrX - playerUlX - 1) and 0xFF
    //> cmp #$09                   ;if difference not close enough, skip subroutine
    //> bcs NoSideC                ;and instead branch to leave (no collision)
    if (rightDiff >= 0x09) return
    //> SideC: jsr ImpedePlayerMove       ;deal with horizontal collision
    //> NoSideC: ldx ObjectOffset           ;return with enemy object buffer offset
    impedePlayerMove(side)
}

// =====================================================================
// ImpedePlayerMove
// Assembly lines 12318-12351
// =====================================================================

/**
 * Handles the mechanical effects of a horizontal collision with a platform.
 * Pushes the player out of the platform and zeroes horizontal speed.
 *
 * @param side 1 = left side collision, 2 = right side collision
 */
private fun System.impedePlayerMove(side: Int) {
    //> ImpedePlayerMove:
    //> lda #$00                  ;initialize value here
    var displacement = 0
    //> ldy Player_X_Speed        ;get player's horizontal speed
    val playerSpeed = ram.playerXSpeed
    //> ldx $00                   ;check value set earlier for
    //> dex                       ;left side collision
    if (side == 1) {
        //> bne RImpd                 ;if right side collision, skip this part
        //> inx                       ;return value to X (X=1 for left)
        //> cpy #$00                  ;if player moving to the left,
        //> bmi ExIPM                 ;branch to invert bit and leave
        if (playerSpeed < 0) {
            //> ExIPM: txa; eor #$ff; and Player_CollisionBits; sta Player_CollisionBits
            ram.playerCollisionBits = (ram.playerCollisionBits.toInt() and (1 xor 0xFF)).toByte()
            return
        }
        //> lda #$ff                  ;otherwise load A with value to be used later
        displacement = 0xFF
    } else {
        //> RImpd: ldx #$02                  ;return $02 to X
        //> cpy #$01                  ;if player moving to the right,
        //> bpl ExIPM                 ;branch to invert bit and leave
        if (playerSpeed >= 1) {
            //> ExIPM: txa; eor #$ff; and Player_CollisionBits; sta Player_CollisionBits
            ram.playerCollisionBits = (ram.playerCollisionBits.toInt() and (2 xor 0xFF)).toByte()
            return
        }
        //> lda #$01                  ;otherwise load A with value to be used here
        displacement = 0x01
    }

    //> NXSpd: ldy #$10
    //> sty SideCollisionTimer    ;set timer of some sort
    ram.sideCollisionTimer = 0x10
    //> ldy #$00
    //> sty Player_X_Speed        ;nullify player's horizontal speed
    ram.playerXSpeed = 0
    //> cmp #$00                  ;if value set in A not set to $ff,
    //> bpl PlatF                 ;branch ahead, do not decrement Y
    val highBits = if (displacement >= 0x80) 0xFF else 0x00
    //> PlatF: sty $00                   ;store Y as high bits of horizontal adder
    //> clc
    //> adc Player_X_Position     ;add contents of A to player's horizontal
    //> sta Player_X_Position     ;position to move player left or right
    val newPosX = (ram.playerXPosition.toInt() and 0xFF) + displacement
    ram.playerXPosition = (newPosX and 0xFF).toUByte()
    //> lda Player_PageLoc
    //> adc $00                   ;add high bits and carry to
    //> sta Player_PageLoc        ;page location if necessary
    val pageCarry = if (newPosX > 0xFF) 1 else 0
    ram.playerPageLoc = ((ram.playerPageLoc.toInt() and 0xFF) + highBits + pageCarry).toByte()
    //> ExIPM: txa                       ;invert contents of X
    //> eor #$ff
    //> and Player_CollisionBits  ;mask out bit that was set here
    //> sta Player_CollisionBits  ;store to clear bit
    ram.playerCollisionBits = (ram.playerCollisionBits.toInt() and (side xor 0xFF)).toByte()
}

// =====================================================================
// CheckPlayerVertical
// Assembly lines 11867-11877
// =====================================================================

/**
 * Checks whether the player is below the screen or completely offscreen.
 * @return true if player is offscreen or below threshold (carry set), false otherwise
 */
private fun System.checkPlayerVertical(): Boolean {
    //> CheckPlayerVertical:
    //> lda Player_OffscreenBits  ;if player object is completely offscreen
    //> cmp #$f0                  ;vertically, leave this routine
    //> bcs ExCPV
    val offscreenBits = ram.playerOffscreenBits.toInt() and 0xFF
    if (offscreenBits >= 0xF0) return true
    //> ldy Player_Y_HighPos      ;if player high vertical byte is not
    //> dey                       ;within the screen, leave this routine
    //> bne ExCPV
    val yHigh = (ram.playerYHighPos.toInt() and 0xFF) - 1
    if (yHigh != 0) return true
    //> lda Player_Y_Position     ;if on the screen, check to see how far down
    //> cmp #$d0                  ;the player is vertically
    val playerY = ram.playerYPosition.toInt() and 0xFF
    //> ExCPV: rts                ;carry set if Y >= $d0
    return playerY >= 0xD0
}

// PlayerCollisionCore is defined in collisionDetection.kt as:
//   fun System.playerCollisionCore(bbOffsetY: Int): Boolean
// We use that existing implementation throughout this file.

// =====================================================================
// PositionPlayerOnVPlat
// Assembly lines 11846-11863
// =====================================================================

/**
 * Positions the player vertically on top of a platform.
 * Subtracts 32 pixels from the platform's Y position (for player height)
 * and sets the player's position accordingly. Zeros vertical speed and force.
 *
 * @param enemyOfs enemy object offset to read position from (defaults to objectOffset)
 */
fun System.positionPlayerOnVPlat(enemyOfs: Int = ram.objectOffset.toInt() and 0xFF) {
    //> PositionPlayerOnVPlat:
    //> lda Enemy_Y_Position,x    ;get vertical coordinate
    val enemyY = ram.sprObjYPos[enemyOfs + 1].toInt() and 0xFF
    //> ldy GameEngineSubroutine
    //> cpy #$0b                  ;if certain routine being executed on this frame,
    //> beq ExPlPos               ;skip all of this
    if (ram.gameEngineSubroutine == GameEngineRoutine.PlayerDeath) return
    //> ldy Enemy_Y_HighPos,x
    //> cpy #$01                  ;if vertical high byte offscreen, skip this
    //> bne ExPlPos
    val enemyYHigh = ram.sprObjYHighPos[enemyOfs + 1].toInt() and 0xFF
    if (enemyYHigh != 0x01) return
    //> sec                       ;subtract 32 pixels from vertical coordinate
    //> sbc #$20                  ;for the player object's height
    //> sta Player_Y_Position     ;save as player's new vertical coordinate
    val newPlayerY = (enemyY - 0x20) and 0xFF
    ram.playerYPosition = newPlayerY.toUByte()
    //> tya
    //> sbc #$00                  ;subtract borrow and store as player's
    //> sta Player_Y_HighPos      ;new vertical high byte
    val borrow = if (enemyY < 0x20) 1 else 0
    ram.playerYHighPos = (enemyYHigh - borrow).toByte()
    //> lda #$00
    //> sta Player_Y_Speed        ;initialize vertical speed and low byte of force
    //> sta Player_Y_MoveForce    ;and then leave
    ram.playerYSpeed = 0
    ram.playerYMoveForce = 0
    //> ExPlPos: rts
}

// =====================================================================
// PositionPlayerOnS_Plat (small platform variant)
// Assembly lines 11836-11844
// =====================================================================

//> PlayerPosSPlatData:
//>       .db $80, $00
private val playerPosSPlatData = intArrayOf(0x80, 0x00)

/**
 * Positions the player on a small platform using the bounding box counter
 * as an offset to position data.
 * Falls through into PositionPlayerOnVPlat (via BIT instruction skip).
 */
private fun System.positionPlayerOnS_Plat(bbCounter: Int) {
    //> PositionPlayerOnS_Plat:
    //> tay                        ;use bounding box counter saved in collision flag
    //> lda Enemy_Y_Position,x     ;for offset
    //> clc                        ;add positioning data using offset to the vertical
    //> adc PlayerPosSPlatData-1,y ;coordinate
    val x = ram.objectOffset.toInt() and 0xFF
    val enemyY = ram.sprObjYPos[x + 1].toInt() and 0xFF
    val adjustment = playerPosSPlatData[(bbCounter - 1).coerceIn(0, 1)]
    val adjustedY = (enemyY + adjustment) and 0xFF
    // Temporarily set Enemy_Y_Position to the adjusted value for PositionPlayerOnVPlat
    val savedY = ram.sprObjYPos[x + 1]
    ram.sprObjYPos[x + 1] = adjustedY.toByte()
    //> .db $2c                    ;BIT instruction opcode - skip next instruction
    //> PositionPlayerOnVPlat:     ;falls through to PositionPlayerOnVPlat
    positionPlayerOnVPlat(x)
    ram.sprObjYPos[x + 1] = savedY  // restore original value
}

// =====================================================================
// PositionPlayerOnHPlat
// Assembly lines 10934-10948
// =====================================================================

/**
 * Positions the player horizontally on a moving platform.
 * Adds the horizontal displacement ($00 from MoveWithXMCntrs) to the player's
 * position, then positions vertically via PositionPlayerOnVPlat.
 *
 * @param hDisplacement the horizontal displacement to apply
 */
private fun System.positionPlayerOnHPlat(hDisplacement: Byte) {
    //> PositionPlayerOnHPlat:
    //> lda Player_X_Position
    //> clc                       ;add saved value from second subroutine to
    //> adc $00                   ;current player's position to position
    //> sta Player_X_Position     ;player accordingly in horizontal position
    val newPosX = (ram.playerXPosition.toInt() and 0xFF) + (hDisplacement.toInt() and 0xFF)
    ram.playerXPosition = (newPosX and 0xFF).toUByte()
    //> lda Player_PageLoc        ;get player's page location
    //> ldy $00                   ;check to see if saved value here is positive or negative
    //> bmi PPHSubt               ;if negative, branch to subtract
    if (hDisplacement < 0) {
        //> PPHSubt: sbc #$00                  ;subtract borrow from page location
        val borrow = if (newPosX < (ram.playerXPosition.toInt() and 0xFF)) 1 else 0 // can't borrow on addition
        // For negative displacement: if position wrapped around (newPosX < 0), borrow from page
        val pageBorrow = if ((ram.playerXPosition.toInt() and 0xFF) + (hDisplacement.toInt() and 0xFF) < (ram.playerXPosition.toInt() and 0xFF)) 0 else 1
        // Actually, simpler: if hDisplacement is negative, the carry from the addition tells us
        // On 6502: clc + adc means carry is set if result > 0xFF
        val carry = if (newPosX > 0xFF) 1 else 0
        // sbc #$00 with carry from the adc
        ram.playerPageLoc = ((ram.playerPageLoc.toInt() and 0xFF) - (1 - carry)).toByte()
    } else {
        //> jmp SetPVar               ;jump to skip subtraction
        //> adc #$00                  ;otherwise add carry to page location
        val carry = if (newPosX > 0xFF) 1 else 0
        ram.playerPageLoc = ((ram.playerPageLoc.toInt() and 0xFF) + carry).toByte()
    }
    //> SetPVar: sta Player_PageLoc        ;save result to player's page location
    //> sty Platform_X_Scroll     ;put saved value from second sub here to be used later
    ram.platformXScroll = hDisplacement
    //> jsr PositionPlayerOnVPlat ;position player vertically and appropriately
    positionPlayerOnVPlat()
    //> ExXMP: rts
}

// =====================================================================
// BalancePlatform (pulley/balance platform)
// Assembly lines 10667-10804
// =====================================================================

/**
 * Handles the paired balance/pulley platform logic.
 * When one platform goes down, the other goes up. If a platform reaches
 * too high while the player is standing on it, both platforms start falling.
 * Also handles drawing/erasing the connecting rope in the VRAM buffer.
 */
fun System.balancePlatform() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> BalancePlatform:
    //> lda Enemy_Y_HighPos,x       ;check high byte of vertical position
    //> cmp #$03
    //> bne DoBPl
    if ((ram.sprObjYHighPos[x + 1].toInt() and 0xFF) == 0x03) {
        //> jmp EraseEnemyObject        ;if far below screen, kill the object
        eraseEnemyObject(x)
        return
    }

    //> DoBPl: lda Enemy_State,x           ;get object's state (set to $ff or other platform offset)
    //> bpl CheckBalPlatform        ;if doing other balance platform, branch to leave
    if (ram.enemyState[x] < 0) return  // d7 set = $ff = this is the secondary platform

    //> CheckBalPlatform:
    //> tay                         ;save offset from state as Y
    val otherPlatOfs = ram.enemyState[x].toInt() and 0xFF

    //> lda PlatformCollisionFlag,x ;get collision flag of platform
    //> sta $00                     ;store here
    val collisionFlag = ram.platformCollisionFlags[x]

    //> lda Enemy_MovingDir,x       ;get moving direction
    //> beq ChkForFall
    if (ram.enemyMovingDirs[x] != 0.toByte()) {
        //> jmp PlatformFall            ;if set, jump here (platforms are falling)
        platformFall(x, otherPlatOfs)
        return
    }

    //> ChkForFall:
    //> lda #$2d                    ;check if platform is above a certain point
    //> cmp Enemy_Y_Position,x
    val thisY = ram.sprObjYPos[x + 1].toInt() and 0xFF
    val otherY = ram.sprObjYPos[otherPlatOfs + 1].toInt() and 0xFF

    if (0x2d >= thisY) {
        //> bcc ChkOtherForFall         ;if not, branch elsewhere
        //> (carry set means 0x2d >= thisY)
        //> cpy $00                     ;if collision flag is set to same value as
        //> beq MakePlatformFall        ;enemy state, branch to make platforms fall
        if (otherPlatOfs == (collisionFlag.toInt() and 0xFF)) {
            //> MakePlatformFall:
            //> jmp InitPlatformFall        ;make platforms fall
            initPlatformFall(x, otherPlatOfs)
            return
        }
        //> clc
        //> adc #$02                    ;otherwise add 2 pixels to vertical position
        //> sta Enemy_Y_Position,x      ;of current platform and branch elsewhere
        ram.sprObjYPos[x + 1] = ((thisY + 2) and 0xFF).toByte()
        //> jmp StopPlatforms           ;to make platforms stop
        stopPlatforms(x, otherPlatOfs)
        return
    }

    //> ChkOtherForFall:
    //> cmp Enemy_Y_Position,y      ;check if other platform is above a certain point
    if (0x2d >= otherY) {
        //> bcc ChkToMoveBalPlat        ;if not, branch elsewhere
        //> (carry set means 0x2d >= otherY)
        //> cpx $00                     ;if collision flag is set to same value as
        //> beq MakePlatformFall        ;enemy state, branch to make platforms fall
        if (x == (collisionFlag.toInt() and 0xFF)) {
            initPlatformFall(x, otherPlatOfs)
            return
        }
        //> clc
        //> adc #$02                    ;otherwise add 2 pixels to vertical position
        //> sta Enemy_Y_Position,y      ;of other platform and branch elsewhere
        ram.sprObjYPos[otherPlatOfs + 1] = ((otherY + 2) and 0xFF).toByte()
        //> jmp StopPlatforms           ;jump to stop movement and do not return
        stopPlatforms(x, otherPlatOfs)
        return
    }

    //> ChkToMoveBalPlat:
    //> lda Enemy_Y_Position,x      ;save vertical position to stack
    //> pha
    val savedY = ram.sprObjYPos[x + 1]

    //> lda PlatformCollisionFlag,x ;get collision flag
    //> bpl ColFlg                  ;branch if collision (d7 clear)
    if (collisionFlag < 0) {
        // No collision (d7 set = $ff = no collision)
        //> lda Enemy_Y_MoveForce,x
        //> clc                         ;add $05 to contents of moveforce
        //> adc #$05
        //> sta $00                     ;store here
        val forceResult = (ram.sprObjYMoveForce[x + 1].toInt() and 0xFF) + 0x05
        val forceVal = forceResult and 0xFF
        //> lda Enemy_Y_Speed,x
        //> adc #$00                    ;add carry to vertical speed
        val speedCarry = if (forceResult > 0xFF) 1 else 0
        val speedResult = (ram.sprObjYSpeed[x + 1].toInt() and 0xFF) + speedCarry
        val speed = speedResult.toByte()

        //> bmi PlatDn                  ;branch if moving downwards
        if (speed < 0) {
            movePlatformDown(x)
        } else if (speed > 0) {
            //> bne PlatUp                  ;branch elsewhere if moving upwards
            movePlatformUp(x)
        } else {
            //> lda $00
            //> cmp #$0b                    ;check if there's still a little force left
            //> bcc PlatSt                  ;if not enough, branch to stop movement
            if (forceVal < 0x0b) {
                stopPlatforms(x, otherPlatOfs)
            } else {
                //> bcs PlatUp                  ;otherwise keep branch to move upwards
                movePlatformUp(x)
            }
        }
    } else {
        //> ColFlg: cmp ObjectOffset            ;if collision flag matches
        //> beq PlatDn                  ;current enemy object offset, branch
        //> PlatDn: jsr MovePlatformDown        ;do a sub to move downwards
        //> jmp DoOtherPlatform         ;jump ahead to remaining code
        //> PlatSt: jsr StopPlatforms           ;do a sub to stop movement
        //> jmp DoOtherPlatform         ;jump ahead to remaining code
        //> PlatUp: jsr MovePlatformUp          ;do a sub to move upwards
        if ((collisionFlag.toInt() and 0xFF) == x) {
            movePlatformDown(x)
        } else {
            movePlatformUp(x)
        }
    }

    //> DoOtherPlatform:
    //> ldy Enemy_State,x           ;get offset of other platform
    //> pla                         ;get old vertical coordinate from stack
    //> sec
    //> SixSpriteStacker:
    //> sbc Enemy_Y_Position,x      ;get difference of old vs. new coordinate
    val oldY = savedY.toInt() and 0xFF
    val newY = ram.sprObjYPos[x + 1].toInt() and 0xFF
    val yDiff = (oldY - newY) and 0xFF

    //> clc
    //> adc Enemy_Y_Position,y      ;add difference to vertical coordinate of other
    //> sta Enemy_Y_Position,y      ;platform to move it in the opposite direction
    val otherNewY = ((ram.sprObjYPos[otherPlatOfs + 1].toInt() and 0xFF) + yDiff) and 0xFF
    ram.sprObjYPos[otherPlatOfs + 1] = otherNewY.toByte()

    //> lda PlatformCollisionFlag,x ;if no collision, skip this part here
    //> bmi DrawEraseRope
    if (ram.platformCollisionFlags[x] >= 0) {
        //> tax                         ;put offset which collision occurred here
        //> jsr PositionPlayerOnVPlat   ;and use it to position player accordingly
        positionPlayerOnVPlat(ram.platformCollisionFlags[x].toInt() and 0xFF)
    }

    //> DrawEraseRope:
    // The rope drawing is a VRAM buffer operation. Since the Kotlin port uses
    // a different rendering approach, we stub this with a TODO.
    // The rope tiles would be written to VRAM_Buffer1 to show/hide connecting rope.
    drawEraseRope(x, otherPlatOfs)
}

/**
 * Draws or erases the rope tiles connecting two balance platforms in the VRAM buffer.
 * This is a graphical detail that writes name table tiles for the rope.
 */
private fun System.drawEraseRope(x: Int, otherPlatOfs: Int) {
    //> DrawEraseRope:
    //> ldy ObjectOffset            ;get enemy object offset
    //> lda Enemy_Y_Speed,y         ;check to see if current platform is
    //> ora Enemy_Y_MoveForce,y     ;moving at all
    //> beq ExitRp                  ;if not, skip all of this and branch to leave
    val speed = ram.sprObjYSpeed[x + 1]
    val force = ram.sprObjYMoveForce[x + 1]
    if (speed == 0.toByte() && force == 0.toByte()) return

    //> bcs ExitRp                  ;and skip this, branch to leave
    //> jsr SetupPlatformRope       ;do a sub to figure out where to put new bg tiles
    //> bmi EraseR1                 ;to do something else
    //> lda #$a2
    //> lda #$a3                    ;and right sides of rope in vram buffer
    //> jmp OtherRope               ;jump to skip this part
    //> EraseR1: lda #$24                    ;put blank tiles in vram buffer
    //> OtherRope:
    //> jsr SetupPlatformRope       ;do sub again to figure out where to put bg tiles
    //> sta VRAM_Buffer1+7,x        ;set length again for 2 bytes
    //> bpl EraseR2                 ;if moving upwards (note inversion earlier), skip this
    //> lda #$a2
    //> sta VRAM_Buffer1+8,x        ;otherwise put tile numbers for left
    //> lda #$a3                    ;and right sides of rope in vram
    //> sta VRAM_Buffer1+9,x        ;transfer buffer
    //> jmp EndRp                   ;jump to skip this part
    //> EraseR2: lda #$24                    ;put blank tiles in vram buffer
    //> sta VRAM_Buffer1+8,x        ;to erase rope
    //> sta VRAM_Buffer1+9,x
    //> EndRp:   lda #$00                    ;put null terminator at the end
    //> sta VRAM_Buffer1+10,x
    //> ExitRp:  ldx ObjectOffset            ;get enemy object buffer offset and leave

    // TODO: Implement VRAM buffer rope drawing.
    // The assembly writes rope tile data ($a2, $a3) or blank tiles ($24) into VRAM_Buffer1
    // based on platform movement direction to visually connect the two balance platforms.
    // This requires SetupPlatformRope which calculates name table addresses from
    // platform positions. Skipped for now as it's purely cosmetic and depends on
    // the VRAM buffer system.

    //> SetupPlatformRope:
    //> ldx SecondaryHardMode   ;if secondary hard mode flag set,
    //> bne GetLRp              ;use coordinate as-is
    //> GetLRp: pha                     ;save modified horizontal coordinate to stack
    //> ldx Enemy_Y_Position,y  ;get vertical coordinate
    //> bpl GetHRp              ;skip this part if moving downwards or not at all
    //> GetHRp: txa                     ;move vertical coordinate to A
    //> and #%11100000          ;mask out low nybble and LSB of high nybble
    //> cmp #$e8                ;if vertical position not below the
    //> bcc ExPRp               ;bottom of the screen, we're done, branch to leave
    //> ExPRp:  rts                     ;leave!
}

// =====================================================================
// InitPlatformFall / StopPlatforms / PlatformFall
// Assembly lines 10860-10892
// =====================================================================

/**
 * Initiates the falling sequence for both balance platforms.
 * Awards 1000 points and sets the falling direction flag.
 */
private fun System.initPlatformFall(x: Int, otherPlatOfs: Int) {
    //> InitPlatformFall:
    //> tya                        ;move offset of other platform from Y to X
    //> tax
    val savedObjectOffset = ram.objectOffset
    ram.objectOffset = otherPlatOfs.toByte()
    //> jsr GetEnemyOffscreenBits  ;get offscreen bits
    getEnemyOffscreenBits()
    //> lda #$06
    //> jsr SetupFloateyNumber     ;award 1000 points to player
    setupFloateyNumber(otherPlatOfs, 0x06)
    //> lda Player_Rel_XPos
    //> sta FloateyNum_X_Pos,x     ;put floatey number coordinates where player is
    ram.floateyNumXPos[otherPlatOfs] = ram.playerRelXPos.toUByte()
    //> lda Player_Y_Position
    //> sta FloateyNum_Y_Pos,x
    ram.floateyNumYPos[otherPlatOfs] = ram.playerYPosition
    //> lda #$01                   ;set moving direction as flag for
    //> sta Enemy_MovingDir,x      ;falling platforms
    ram.enemyMovingDirs[otherPlatOfs] = 0x01
    ram.objectOffset = savedObjectOffset

    //> (falls through to StopPlatforms)
    stopPlatforms(x, otherPlatOfs)
}

/**
 * Stops vertical movement on both platforms by zeroing speed and force.
 */
private fun System.stopPlatforms(x: Int, otherPlatOfs: Int) {
    //> StopPlatforms:
    //> jsr InitVStf             ;initialize vertical speed and low byte
    //> sta Enemy_Y_Speed,y      ;for both platforms and leave
    //> sta Enemy_Y_MoveForce,y
    // InitVStf zeros speed and force for enemy at X
    ram.sprObjYSpeed[x + 1] = 0
    ram.sprObjYMoveForce[x + 1] = 0
    ram.sprObjYSpeed[otherPlatOfs + 1] = 0
    ram.sprObjYMoveForce[otherPlatOfs + 1] = 0
}

/**
 * Makes both balance platforms fall, then positions the player if standing on one.
 */
private fun System.platformFall(x: Int, otherPlatOfs: Int) {
    //> PlatformFall:
    //> tya                         ;save offset for other platform to stack
    //> pha
    //> jsr MoveFallingPlatform     ;make current platform fall
    val savedOffset = ram.objectOffset
    ram.objectOffset = x.toByte()
    moveFallingPlatform()
    //> pla
    //> tax                         ;pull offset from stack and save to X
    //> jsr MoveFallingPlatform     ;make other platform fall
    ram.objectOffset = otherPlatOfs.toByte()
    moveFallingPlatform()
    ram.objectOffset = savedOffset
    //> ldx ObjectOffset
    //> lda PlatformCollisionFlag,x ;if player not standing on either platform,
    //> bmi ExPF                    ;skip this part
    val collFlag = ram.platformCollisionFlags[x]
    if (collFlag >= 0) {
        //> tax                         ;transfer collision flag offset as offset to X
        //> jsr PositionPlayerOnVPlat   ;and position player appropriately
        positionPlayerOnVPlat(collFlag.toInt() and 0xFF)
    }
    //> ExPF: ldx ObjectOffset            ;get enemy object buffer offset and leave
}

// =====================================================================
// YMovingPlatform
// Assembly lines 10896-10922
// =====================================================================

/**
 * Handles vertically oscillating platforms ($25).
 * Platform moves up from its top position, then reverses at the center position.
 * Uses YPlatformTopYPos (stored in sprObjXMoveForce) and
 * YPlatformCenterYPos (stored in sprObjXSpeed) as position limits.
 */
fun System.yMovingPlatform() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> YMovingPlatform:
    //> lda Enemy_Y_Speed,x          ;if platform moving up or down, skip ahead to
    //> ora Enemy_Y_MoveForce,x      ;check on other position
    //> bne ChkYCenterPos
    val speed = ram.sprObjYSpeed[x + 1]
    val force = ram.sprObjYMoveForce[x + 1]
    if (speed == 0.toByte() && force == 0.toByte()) {
        //> sta Enemy_YMF_Dummy,x        ;initialize dummy variable
        ram.sprObjYMFDummy[x + 1] = 0
        //> lda Enemy_Y_Position,x
        //> cmp YPlatformTopYPos,x       ;if current vertical position => top position, branch
        //> bcs ChkYCenterPos            ;ahead of all this
        val currentY = ram.sprObjYPos[x + 1].toInt() and 0xFF
        // YPlatformTopYPos,x at $0401+x = sprObjXMoveForce[x+1]
        val topYPos = ram.sprObjXMoveForce[x + 1].toInt() and 0xFF
        if (currentY < topYPos) {
            //> lda FrameCounter
            //> and #%00000111               ;check for every eighth frame
            //> bne SkipIY
            if ((ram.frameCounter.toInt() and 0x07) == 0) {
                //> inc Enemy_Y_Position,x       ;increase vertical position every eighth frame
                ram.sprObjYPos[x + 1] = (currentY + 1).toByte()
            }
            //> SkipIY: jmp ChkYPCollision           ;skip ahead to last part
            chkYPCollision(x)
            return
        }
    }

    //> ChkYCenterPos:
    //> lda Enemy_Y_Position,x       ;if current vertical position < central position, branch
    //> cmp YPlatformCenterYPos,x    ;to slow ascent/move downwards
    val currentY = ram.sprObjYPos[x + 1].toInt() and 0xFF
    // YPlatformCenterYPos,x at $58+x = sprObjXSpeed[x+1]
    val centerYPos = ram.sprObjXSpeed[x + 1].toInt() and 0xFF
    if (currentY < centerYPos) {
        //> bcc YMDown
        //> YMDown: jsr MovePlatformDown         ;start slowing ascent/moving downwards
        movePlatformDown(x)
    } else {
        //> jsr MovePlatformUp           ;otherwise start slowing descent/moving upwards
        movePlatformUp(x)
    }

    //> ChkYPCollision:
    chkYPCollision(x)
}

/**
 * Checks if the player is standing on a vertically-moving platform
 * and positions them accordingly.
 */
private fun System.chkYPCollision(x: Int) {
    //> ChkYPCollision:
    //> lda PlatformCollisionFlag,x  ;if collision flag not set here, branch
    //> bmi ExYPl                    ;to leave
    if (ram.platformCollisionFlags[x] < 0) return
    //> jsr PositionPlayerOnVPlat    ;otherwise position player appropriately
    positionPlayerOnVPlat(x)
    //> ExYPl: rts
}

//> ;$00 - used as adder to position player hotizontally

// =====================================================================
// XMovingPlatform
// Assembly lines 10927-10948
// =====================================================================

/**
 * Handles horizontally oscillating platforms ($28).
 * Uses counter-based movement to create back-and-forth motion.
 */
fun System.xMovingPlatform() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> XMovingPlatform:
    //> lda #$0e                     ;load preset maximum value for secondary counter
    //> jsr XMoveCntr_Platform       ;do a sub to increment counters for movement
    xMoveCntrPlatform(x, 0x0e)
    //> jsr MoveWithXMCntrs          ;do a sub to move platform accordingly, and return value
    val hDisplacement = moveWithXMCntrs(x)
    //> lda PlatformCollisionFlag,x  ;if no collision with player,
    //> bmi ExXMP                    ;branch ahead to leave
    if (ram.platformCollisionFlags[x] < 0) return

    //> PositionPlayerOnHPlat:
    positionPlayerOnHPlat(hDisplacement)
}

// =====================================================================
// XMoveCntr_Platform / MoveWithXMCntrs
// Assembly lines 9423-9461
// =====================================================================

/**
 * Increments the primary/secondary movement counters for X-axis oscillation.
 * Called every fourth frame. The secondary counter oscillates between 0 and
 * [maxSecondary], with the primary counter tracking direction changes.
 *
 * @param x enemy object offset
 * @param maxSecondary the maximum value for the secondary counter
 */
private fun System.xMoveCntrPlatform(x: Int, maxSecondary: Int) {
    //> XMoveCntr_Platform:
    //> sta $01                     ;store value here
    //> lda FrameCounter
    //> and #%00000011              ;branch to leave if not on
    //> bne NoIncXM                 ;every fourth frame
    if ((ram.frameCounter.toInt() and 0x03) != 0) return

    // XMovePrimaryCounter,x at $a0+x = sprObjYSpeed[x+1]
    // XMoveSecondaryCounter,x at $58+x = sprObjXSpeed[x+1]
    //> ldy XMoveSecondaryCounter,x ;get secondary counter
    val secondaryCtr = ram.sprObjXSpeed[x + 1].toInt() and 0xFF
    //> lda XMovePrimaryCounter,x   ;get primary counter
    val primaryCtr = ram.sprObjYSpeed[x + 1].toInt() and 0xFF
    //> lsr
    //> bcs DecSeXM                 ;if d0 of primary counter set, branch elsewhere
    if (primaryCtr and 0x01 != 0) {
        //> DecSeXM: tya                         ;put secondary counter in A
        //> beq IncPXM                  ;if secondary counter at zero, branch back
        if (secondaryCtr == 0) {
            //> IncPXM: inc XMovePrimaryCounter,x   ;increment primary counter and leave
            ram.sprObjYSpeed[x + 1] = ((primaryCtr + 1) and 0xFF).toByte()
        } else {
            //> dec XMoveSecondaryCounter,x ;otherwise decrement secondary counter and leave
            ram.sprObjXSpeed[x + 1] = ((secondaryCtr - 1) and 0xFF).toByte()
        }
    } else {
        //> cpy $01                     ;compare secondary counter to preset maximum value
        //> beq IncPXM                  ;if equal, branch ahead of this part
        if (secondaryCtr == maxSecondary) {
            //> IncPXM: inc XMovePrimaryCounter,x   ;increment primary counter and leave
            ram.sprObjYSpeed[x + 1] = ((primaryCtr + 1) and 0xFF).toByte()
        } else {
            //> inc XMoveSecondaryCounter,x ;increment secondary counter and leave
            //> NoIncXM: rts
            ram.sprObjXSpeed[x + 1] = ((secondaryCtr + 1) and 0xFF).toByte()
        }
    }
}

/**
 * Moves the platform horizontally based on the movement counters.
 * The primary counter's d1 bit determines direction, and the secondary
 * counter determines speed magnitude.
 *
 * @return horizontal displacement byte (signed) for use by PositionPlayerOnHPlat
 */
private fun System.moveWithXMCntrs(x: Int): Byte {
    //> MoveWithXMCntrs:
    //> lda XMoveSecondaryCounter,x  ;save secondary counter to stack
    //> pha
    val savedSecondary = ram.sprObjXSpeed[x + 1]
    //> ldy #$01                     ;set value here by default
    //> lda XMovePrimaryCounter,x
    //> and #%00000010               ;if d1 of primary counter is
    //> bne XMRight                  ;set, branch ahead of this part here
    val primaryCtr = ram.sprObjYSpeed[x + 1].toInt() and 0xFF
    if (primaryCtr and 0x02 == 0) {
        //> lda XMoveSecondaryCounter,x
        //> eor #$ff                     ;otherwise change secondary
        //> clc                          ;counter to two's compliment
        //> adc #$01
        //> sta XMoveSecondaryCounter,x
        val secondary = ram.sprObjXSpeed[x + 1].toInt() and 0xFF
        val negated = ((secondary xor 0xFF) + 1) and 0xFF
        ram.sprObjXSpeed[x + 1] = negated.toByte()
        //> ldy #$02                     ;load alternate value here
        ram.enemyMovingDirs[x] = 0x02
    } else {
        //> XMRight: sty Enemy_MovingDir,x        ;store as moving direction
        ram.enemyMovingDirs[x] = 0x01
    }
    //> jsr MoveEnemyHorizontally
    val hDisplacement = moveEnemyHorizontally()
    //> sta $00                      ;save value obtained from sub here
    //> pla                          ;get secondary counter from stack
    //> sta XMoveSecondaryCounter,x  ;and return to original place
    ram.sprObjXSpeed[x + 1] = savedSecondary
    return hDisplacement
}

//> ;$00 - residual value from sub

// =====================================================================
// DropPlatform
// Assembly lines 10952-10957
// =====================================================================

/**
 * Handles falling platforms ($29) that drop when the player stands on them.
 * Only moves if the player is actually on the platform.
 */
fun System.dropPlatform() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> DropPlatform:
    //> lda PlatformCollisionFlag,x  ;if no collision between platform and player
    //> bmi ExDPl                    ;occurred, just leave without moving anything
    if (ram.platformCollisionFlags[x] < 0) return
    //> jsr MoveDropPlatform         ;otherwise do a sub to move platform down very quickly
    moveDropPlatform()
    //> jsr PositionPlayerOnVPlat    ;do a sub to position player appropriately
    positionPlayerOnVPlat(x)
    //> ExDPl: rts
}

/**
 * Moves the drop platform downward with heavy gravity.
 * Uses downForce=$7f and maxSpeed=$02.
 */
private fun System.moveDropPlatform() {
    //> MoveDropPlatform:
    //> ldy #$7f      ;set movement amount for drop platform
    //> bne SetMdMax  ;skip ahead of other value set here
    //> SetMdMax: lda #$02         ;set maximum speed in A
    //> SetXMoveAmt: sty $00; inx; jsr ImposeGravitySprObj
    val x = ram.objectOffset.toInt() and 0xFF
    imposeGravitySprObj(sprObjOffset = x + 1, downForce = 0x7f, maxSpeed = 0x02)
}

// =====================================================================
// RightPlatform
// Assembly lines 10962-10970
// =====================================================================

/**
 * Handles the right-moving platform ($2a).
 * Always moves right and accelerates to speed $10 when player is standing on it.
 */
fun System.rightPlatform() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> RightPlatform:
    //> jsr MoveEnemyHorizontally     ;move platform with current horizontal speed, if any
    val hDisplacement = moveEnemyHorizontally()
    //> sta $00                       ;store saved value here (residual code)
    //> lda PlatformCollisionFlag,x   ;check collision flag, if no collision between player
    //> bmi ExRPl                     ;and platform, branch ahead, leave speed unaltered
    if (ram.platformCollisionFlags[x] < 0) return
    //> lda #$10
    //> sta Enemy_X_Speed,x           ;otherwise set new speed (gets moving if motionless)
    ram.sprObjXSpeed[x + 1] = 0x10
    //> jsr PositionPlayerOnHPlat     ;use saved value from earlier sub to position player
    positionPlayerOnHPlat(hDisplacement)
    //> ExRPl: rts
}

// =====================================================================
// MoveLargeLiftPlat / MoveSmallPlatform / MoveLiftPlatforms
// Assembly lines 10974-10998
// =====================================================================

/**
 * Movement routine for large lift platforms ($26, $27).
 * Applies lift movement then checks for player collision on vertical axis.
 */
fun System.moveLargeLiftPlat() {
    //> MoveLargeLiftPlat:
    //> jsr MoveLiftPlatforms  ;execute common to all large and small lift platforms
    moveLiftPlatforms()
    //> jmp ChkYPCollision     ;branch to position player correctly
    chkYPCollision(ram.objectOffset.toInt() and 0xFF)
}

/**
 * Movement routine for small platforms ($2b, $2c).
 * Applies lift movement then checks for small platform collision.
 */
fun System.moveSmallPlatform() {
    //> MoveSmallPlatform:
    //> jsr MoveLiftPlatforms      ;execute common to all large and small lift platforms
    moveLiftPlatforms()
    //> jmp ChkSmallPlatCollision  ;branch to position player correctly
    chkSmallPlatCollision()
}

/**
 * Common movement routine for all lift platforms.
 * Adds vertical speed and movement force to position using simple fractional math.
 * Skipped when master timer control is set.
 */
private fun System.moveLiftPlatforms() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> MoveLiftPlatforms:
    //> lda TimerControl         ;if master timer control set, skip all of this
    //> bne ExLiftP              ;and branch to leave
    if (ram.timerControl != 0.toByte()) return

    //> lda Enemy_YMF_Dummy,x
    //> clc                      ;add contents of movement amount to whatever's here
    //> adc Enemy_Y_MoveForce,x
    //> sta Enemy_YMF_Dummy,x
    val dummyResult = (ram.sprObjYMFDummy[x + 1].toInt() and 0xFF) +
            (ram.sprObjYMoveForce[x + 1].toInt() and 0xFF)
    ram.sprObjYMFDummy[x + 1] = dummyResult.toByte()
    val carry = if (dummyResult > 0xFF) 1 else 0

    //> lda Enemy_Y_Position,x   ;add whatever vertical speed is set to current
    //> adc Enemy_Y_Speed,x      ;vertical position plus carry to move up or down
    //> sta Enemy_Y_Position,x   ;and then leave
    val posResult = (ram.sprObjYPos[x + 1].toInt() and 0xFF) +
            (ram.sprObjYSpeed[x + 1].toInt() and 0xFF) + carry
    ram.sprObjYPos[x + 1] = posResult.toByte()
    //> ExLiftP: rts                         ;then leave
}

//> ;$00 - page location of extended left boundary
//> ;$01 - extended left boundary position
//> ;$02 - page location of extended right boundary
//> ;$03 - extended right boundary position

/**
 * Checks collision for small platform after movement.
 * If collision occurred, positions the player on the platform.
 */
private fun System.chkSmallPlatCollision() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> ChkSmallPlatCollision:
    //> lda PlatformCollisionFlag,x ;get bounding box counter saved in collision flag
    //> beq ExLiftP                 ;if none found, leave player position alone
    val collFlag = ram.platformCollisionFlags[x]
    if (collFlag == 0.toByte()) return
    //> jsr PositionPlayerOnS_Plat  ;use to position player correctly
    positionPlayerOnS_Plat(collFlag.toInt() and 0xFF)
}

// =====================================================================
// MovePlatformUp / MovePlatformDown
// Assembly lines 7673-7697
// =====================================================================

/**
 * Applies upward gravity to a platform via ImposeGravity with bidirectional movement.
 * downForce=$05, upForce=$0a, maxSpeed=$03, direction=up (bidirectional=true, Y=1).
 */
fun System.movePlatformUp(enemyOfs: Int = ram.objectOffset.toInt() and 0xFF) {
    //> MovePlatformUp:
    //> lda #$01        ;save value to stack (direction = up)
    //> pha
    //> ldy Enemy_ID,x  ;get enemy object identifier
    //> inx             ;increment offset for enemy object
    //> lda #$05        ;load default value here (downForce)
    //> cpy #$29        ;residual comparison, object #29 never executes this
    //> bne SetDplSpd   ;thus unconditional branch here
    //> SetDplSpd: sta $00         ;save downward movement amount
    //> lda #$0a        ;save upward movement amount
    //> sta $01
    //> lda #$03        ;save maximum vertical speed
    //> sta $02
    //> pla; tay        ;Y = 1 (up = bidirectional)
    //> jsr ImposeGravity
    imposeGravity(
        sprObjOffset = enemyOfs + 1,
        downForce = 0x05,
        upForce = 0x0a,
        maxSpeed = 0x03,
        bidirectional = true
    )
}

/**
 * Applies downward gravity to a platform via ImposeGravity (downward only).
 * downForce=$05, maxSpeed=$03, direction=down (bidirectional=false, Y=0).
 */
fun System.movePlatformDown(enemyOfs: Int = ram.objectOffset.toInt() and 0xFF) {
    //> MovePlatformDown:
    //> .db $2c     ;part as BIT instruction)
    //> lda #$00    ;save value to stack (direction = down)
    //> .db $2c     ;BIT instruction - skip next lda
    //> (falls into MovePlatformUp path with Y=0)
    imposeGravity(
        sprObjOffset = enemyOfs + 1,
        downForce = 0x05,
        upForce = 0x0a,
        maxSpeed = 0x03,
        bidirectional = false
    )
}

// =====================================================================
// SetupFloateyNumber (platform-specific version)
// Assembly lines 11533-11541
// =====================================================================

/**
 * Sets up floatey number display for a specific enemy offset.
 * Used by balance platform to award points using a non-current object offset.
 */
private fun System.setupFloateyNumber(enemyOfs: Int, pointsControl: Int) {
    //> SetupFloateyNumber:
    //> sta FloateyNum_Control,x
    ram.floateyNumControl[enemyOfs] = pointsControl.toByte()
    //> lda #$30; sta FloateyNum_Timer,x
    ram.floateyNumTimer[enemyOfs] = 0x30
    //> lda Enemy_Y_Position,x; sta FloateyNum_Y_Pos,x
    ram.floateyNumYPos[enemyOfs] = ram.sprObjYPos[enemyOfs + 1].toUByte()
    //> lda Enemy_Rel_XPos; sta FloateyNum_X_Pos,x
    ram.floateyNumXPos[enemyOfs] = ram.enemyRelXPos.toUByte()
}

// =====================================================================
// DrawLargePlatform
// Assembly lines 13332-13407
// =====================================================================

/**
 * Draws a large platform using 6 sprites arranged in a row.
 * Platform appears as girders (tile $5b) normally, or clouds (tile $75) on cloud levels.
 * In castle levels or secondary hard mode, the last two sprites are shrunk (moved offscreen).
 * Individual sprites are moved offscreen based on horizontal offscreen bits.
 */
fun System.drawLargePlatform() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> DrawLargePlatform:
    //> ldy Enemy_SprDataOffset,x   ;get OAM data offset
    val sprOfs = (ram.enemySprDataOffset[x].toInt() and 0xFF) shr 2

    //> sty $02                     ;store here
    //> iny; iny; iny               ;add 3 to it for offset to X coordinate
    //> lda Enemy_Rel_XPos          ;get horizontal relative coordinate
    //> jsr SixSpriteStacker        ;store X coordinates using A as base, stack horizontally
    val relX = ram.enemyRelXPos.toInt() and 0xFF
    for (i in 0..5) {
        ram.sprites[sprOfs + i].x = ((relX + i * 8) and 0xFF).toUByte()
    }

    //> ldx ObjectOffset
    //> lda Enemy_Y_Position,x      ;get vertical coordinate
    //> jsr DumpFourSpr             ;dump into first four sprites as Y coordinate
    val enemyY = ram.sprObjYPos[x + 1].toUByte()
    for (i in 0..3) {
        ram.sprites[sprOfs + i].y = enemyY
    }

    //> ldy AreaType
    //> cpy #$03                    ;check for castle-type level
    //> beq ShrinkPlatform
    //> ldy SecondaryHardMode       ;check for secondary hard mode flag set
    //> beq SetLast2Platform        ;branch if not set elsewhere
    val isCastle = ram.areaType == AreaType.Castle
    val isHardMode = ram.secondaryHardMode != 0.toByte()
    val last2Y = if (isCastle || isHardMode) {
        //> ShrinkPlatform:
        //> lda #$f8                    ;load offscreen coordinate
        0xF8.toUByte()
    } else {
        enemyY
    }

    //> SetLast2Platform:
    //> sta Sprite_Y_Position+16,y  ;store vertical coordinate or offscreen
    //> sta Sprite_Y_Position+20,y  ;coordinate into last two sprites as Y coordinate
    ram.sprites[sprOfs + 4].y = last2Y
    ram.sprites[sprOfs + 5].y = last2Y

    //> lda #$5b                    ;load default tile for platform (girder)
    //> ldx CloudTypeOverride
    //> beq SetPlatformTilenum      ;if cloud level override flag not set, use
    //> lda #$75                    ;otherwise load other tile for platform (puff)
    val tile: Byte = if (ram.cloudTypeOverride) 0x75 else 0x5b

    //> SetPlatformTilenum:
    //> ldx ObjectOffset
    //> iny
    //> jsr DumpSixSpr              ;dump tile number into all six sprites
    for (i in 0..5) ram.sprites[sprOfs + i].tilenumber = tile

    //> lda #$02                    ;set palette controls
    //> iny
    //> jsr DumpSixSpr              ;dump attributes into all six sprites
    val attr = SpriteFlags(palette = 2)
    for (i in 0..5) ram.sprites[sprOfs + i].attributes = attr

    //> inx                         ;increment X for enemy objects
    //> jsr GetXOffscreenBits       ;get offscreen bits again
    //> dex
    val offBits = getXOffscreenBits(x + 1)

    //> ldy Enemy_SprDataOffset,x   ;get OAM data offset
    //> asl; pha; bcc SChk2         ;rotate d7 into carry, check sprites
    var bits = offBits
    if (bits and 0x80 != 0) {
        ram.sprites[sprOfs].y = 0xF8.toUByte()
    }
    //> SChk2: pla; asl; pha; bcc SChk3
    bits = (bits shl 1) and 0xFF
    if (bits and 0x80 != 0) {
        ram.sprites[sprOfs + 1].y = 0xF8.toUByte()
    }
    //> SChk3: pla; asl; pha; bcc SChk4
    bits = (bits shl 1) and 0xFF
    if (bits and 0x80 != 0) {
        ram.sprites[sprOfs + 2].y = 0xF8.toUByte()
    }
    //> SChk4: pla; asl; pha; bcc SChk5
    bits = (bits shl 1) and 0xFF
    if (bits and 0x80 != 0) {
        ram.sprites[sprOfs + 3].y = 0xF8.toUByte()
    }
    //> SChk5: pla; asl; pha; bcc SChk6
    bits = (bits shl 1) and 0xFF
    if (bits and 0x80 != 0) {
        ram.sprites[sprOfs + 4].y = 0xF8.toUByte()
    }
    //> SChk6: pla; asl; bcc SLChk
    bits = (bits shl 1) and 0xFF
    if (bits and 0x80 != 0) {
        ram.sprites[sprOfs + 5].y = 0xF8.toUByte()
    }

    //> SLChk:  lda Enemy_OffscreenBits     ;check d7 of offscreen bits
    //> bcc ExDLPl
    //> jsr MoveSixSpritesOffscreen ;otherwise branch to move all sprites offscreen
    //> ExDLPl: rts
}

// =====================================================================
// DrawSmallPlatform
// Assembly lines 14309-14368
// =====================================================================

/**
 * Draws a small platform using 6 sprites arranged in two rows of 3.
 * The top row is at the platform's Y position, the bottom row is 128 pixels below
 * (for the wrap-around effect of small platforms). Individual sprites are
 * moved offscreen based on vertical offscreen bits.
 */
fun System.drawSmallPlatform() {
    val x = ram.objectOffset.toInt() and 0xFF

    //> DrawSmallPlatform:
    //> ldy Enemy_SprDataOffset,x   ;get OAM data offset
    val sprOfs = (ram.enemySprDataOffset[x].toInt() and 0xFF) shr 2

    //> lda #$5b                    ;load tile number for small platforms
    //> iny; jsr DumpSixSpr         ;dump tile number into all six sprites
    for (i in 0..5) ram.sprites[sprOfs + i].tilenumber = 0x5b

    //> iny; lda #$02               ;load palette controls
    //> jsr DumpSixSpr              ;dump attributes into all six sprites
    val attr = SpriteFlags(palette = 2)
    for (i in 0..5) ram.sprites[sprOfs + i].attributes = attr

    //> dey; dey                    ;decrement for original offset
    //> lda Enemy_Rel_XPos          ;get relative horizontal coordinate
    val relX = ram.enemyRelXPos.toInt() and 0xFF
    //> sta Sprite_X_Position,y; sta Sprite_X_Position+12,y
    ram.sprites[sprOfs].x = relX.toUByte()
    ram.sprites[sprOfs + 3].x = relX.toUByte()
    //> clc; adc #$08
    //> sta Sprite_X_Position+4,y; sta Sprite_X_Position+16,y
    ram.sprites[sprOfs + 1].x = ((relX + 8) and 0xFF).toUByte()
    ram.sprites[sprOfs + 4].x = ((relX + 8) and 0xFF).toUByte()
    //> clc; adc #$08
    //> sta Sprite_X_Position+8,y; sta Sprite_X_Position+20,y
    ram.sprites[sprOfs + 2].x = ((relX + 16) and 0xFF).toUByte()
    ram.sprites[sprOfs + 5].x = ((relX + 16) and 0xFF).toUByte()

    //> lda Enemy_Y_Position,x      ;get vertical coordinate
    val enemyY = ram.sprObjYPos[x + 1].toInt() and 0xFF
    //> tax; pha
    //> cpx #$20                    ;if vertical coordinate below status bar,
    //> bcs TopSP                   ;do not mess with it
    val topY = if (enemyY < 0x20) {
        //> lda #$f8                    ;otherwise move first three sprites offscreen
        0xF8
    } else {
        enemyY
    }
    //> TopSP: jsr DumpThreeSpr     ;dump vertical coordinate into Y coordinates
    for (i in 0..2) ram.sprites[sprOfs + i].y = topY.toUByte()

    //> pla; clc; adc #$80          ;add 128 pixels
    val bottomY = (enemyY + 0x80) and 0xFF
    //> tax; cpx #$20               ;if below status bar (taking wrap into account)
    //> bcs BotSP                   ;then do not change altered coordinate
    val botY = if (bottomY < 0x20) {
        //> lda #$f8                    ;otherwise move last three sprites offscreen
        0xF8
    } else {
        bottomY
    }
    //> BotSP: sta Sprite_Y_Position+12,y ;dump vertical coordinate + 128 pixels
    //> sta Sprite_Y_Position+16,y        ;into Y coordinates
    //> sta Sprite_Y_Position+20,y
    for (i in 3..5) ram.sprites[sprOfs + i].y = botY.toUByte()

    //> lda Enemy_OffscreenBits     ;get offscreen bits
    //> sta Sprite_Data+8,y
    //> DumpThreeSpr:
    val offBits = ram.enemyOffscreenBits.toInt() and 0xFF

    //> and #%00001000              ;check d3
    //> beq SOfs
    //> SOfs:  pla                         ;move out and back into stack
    if (offBits and 0x08 != 0) {
        //> lda #$f8; sta Sprite_Y_Position,y; sta Sprite_Y_Position+12,y
        ram.sprites[sprOfs].y = 0xF8.toUByte()
        ram.sprites[sprOfs + 3].y = 0xF8.toUByte()
    }

    //> and #%00000100              ;check d2
    //> beq SOfs2
    //> SOfs2: pla                         ;get from stack
    if (offBits and 0x04 != 0) {
        //> lda #$f8; sta Sprite_Y_Position+4,y; sta Sprite_Y_Position+16,y
        ram.sprites[sprOfs + 1].y = 0xF8.toUByte()
        ram.sprites[sprOfs + 4].y = 0xF8.toUByte()
    }

    //> and #%00000010              ;check d1
    //> beq ExSPl
    if (offBits and 0x02 != 0) {
        //> lda #$f8; sta Sprite_Y_Position+8,y; sta Sprite_Y_Position+20,y
        ram.sprites[sprOfs + 2].y = 0xF8.toUByte()
        ram.sprites[sprOfs + 5].y = 0xF8.toUByte()
    }
    //> ExSPl: ldx ObjectOffset            ;get enemy object offset and leave
}

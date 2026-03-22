// by Claude - PowerUpObjHandler, VineObjectHandler: power-up mushroom/star/1up and vine processing
package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.*

// -------------------------------------------------------------------------------------
// ROM data tables
// -------------------------------------------------------------------------------------

//> VineHeightData:
//>       .db $30, $60
// by Claude
private val vineHeightData = intArrayOf(0x30, 0x60)

//> VineYPosAdder:
//>       .db $00, $30
//> ;$00 - offset to vine Y coordinate adder
//> ;$02 - offset to sprite data
// by Claude
private val vineYPosAdder = intArrayOf(0x00, 0x30)

//> PowerUpGfxTable:
//>       .db $76, $77, $78, $79 ;regular mushroom
//>       .db $d6, $d6, $d9, $d9 ;fire flower
//>       .db $8d, $8d, $e4, $e4 ;star
//>       .db $76, $77, $78, $79 ;1-up mushroom
// by Claude
private val powerUpGfxTable = byteArrayOf(
    0x76, 0x77, 0x78, 0x79,       // regular mushroom
    0xd6.toByte(), 0xd6.toByte(), 0xd9.toByte(), 0xd9.toByte(), // fire flower
    0x8d.toByte(), 0x8d.toByte(), 0xe4.toByte(), 0xe4.toByte(), // star
    0x76, 0x77, 0x78, 0x79        // 1-up mushroom
)

//> PowerUpAttributes:
//>       .db $02, $01, $02, $01
// by Claude
private val powerUpAttributes = byteArrayOf(0x02, 0x01, 0x02, 0x01)

// -------------------------------------------------------------------------------------
// PowerUpObjHandler
// -------------------------------------------------------------------------------------

/**
 * Handles power-up objects (mushroom, fire flower, star, 1-up mushroom).
 * Power-ups spawn in enemy slot 5 and progress through states:
 * rising from block (states $01-$10), then active with d7 set ($80+).
 */
// by Claude
fun System.powerUpObjHandler() {
    //> PowerUpObjHandler:
    //> ldx #$05                   ;set object offset for last slot in enemy object buffer
    //> stx ObjectOffset
    val x = 5
    ram.objectOffset = x.toByte()

    //> lda Enemy_State+5          ;check power-up object's state
    val state = ram.enemyState.getEnemyState(5)
    //> beq ExitPUp                ;if not set, branch to leave
    if (!state.isActive) return

    //> asl                        ;shift to check if d7 was set in object state
    //> bcc GrowThePowerUp         ;if not set, branch ahead to skip this part
    if (state.kickedOrEmerged) {
        //> lda TimerControl           ;if master timer control set,
        //> bne RunPUSubs              ;branch ahead to enemy object routines
        if (ram.timerControl != 0.toByte()) {
            runPowerUpSubs()
            return
        }

        //> lda PowerUpType            ;check power-up type
        val powerUpType = ram.powerUpType.toInt() and 0xFF
        when (powerUpType) {
            //> beq ShroomM                ;if normal mushroom, branch ahead to move it
            0x00 -> {
                //> ShroomM: jsr MoveNormalEnemy        ;do sub to make mushrooms move
                moveNormalEnemy()
                //> jsr EnemyToBGCollisionDet  ;deal with collisions
                enemyToBGCollisionDet()
            }
            //> cmp #$03
            //> FallE: jsr MoveD_EnemyVertically  ;do a sub here to move enemy downwards
            //> beq ShroomM                ;if 1-up mushroom, branch ahead to move it
            0x03 -> {
                moveNormalEnemy()
                enemyToBGCollisionDet()
            }
            //> cmp #$02
            //> MEHor: jmp MoveEnemyHorizontally  ;jump here to move enemy horizontally for <> $2e and d6 set
            //> bne SlowM                  ;if any other object where d6 set, jump to set Y
            //> cmp #PowerUpObject         ;check for power-up object
            //> beq MEHor                  ;if found, branch to move enemy horizontally
            //> bne RunPUSubs              ;if not star, branch elsewhere to skip movement
            0x02 -> {
                //> jsr MoveJumpingEnemy       ;otherwise impose gravity on star power-up and make it jump
                moveJumpingEnemy()
                //> jsr EnemyJump              ;note that green paratroopa shares the same code here
                enemyJump()
            }
            else -> {
                // fire flower (type 1) - no movement, just run subs
            }
        }
        //> jmp RunPUSubs              ;then jump to other power-up subroutines
        runPowerUpSubs()
        return
    }

    //> GrowThePowerUp:
    //> lda FrameCounter           ;get frame counter
    //> and #$03                   ;mask out all but 2 LSB
    //> bne ChkPUSte               ;if any bits set here, branch
    if ((ram.frameCounter.toInt() and 0x03) == 0) {
        //> dec Enemy_Y_Position+5     ;otherwise decrement vertical coordinate slowly
        ram.sprObjYPos[6] = (ram.sprObjYPos[6] - 1).toByte()

        //> lda Enemy_State+5          ;load power-up object state
        val currentState = ram.enemyState[5].toInt() and 0xFF  // used as numeric rise counter
        //> inc Enemy_State+5          ;increment state for next frame (to make power-up rise)
        ram.enemyState[5] = (currentState + 1).toByte()

        //> cmp #$11                   ;if power-up object state not yet past 16th pixel,
        //> bcc ChkPUSte               ;branch ahead to last part here
        if (currentState >= 0x11) {
            //> lda #$10
            //> sta Enemy_X_Speed,x        ;otherwise set horizontal speed
            ram.sprObjXSpeed[x + 1] = 0x10

            //> lda #%10000000
            //> sta Enemy_State+5          ;and then set d7 in power-up object's state
            ram.enemyState[5] = EnemyState.KICKED_SHELL.byte

            //> asl                        ;shift once to init A (0x80 << 1 = 0x00, carry set)
            //> sta Enemy_SprAttrib+5      ;initialize background priority bit set here
            ram.sprAttrib[6] = 0x00  // by Claude - Enemy_SprAttrib+5 = sprAttrib[6] (power-up slot)

            //> rol                        ;rotate A to set right moving direction (carry into d0 = 1)
            //> sta Enemy_MovingDir,x      ;set moving direction
            ram.enemyMovingDirs[x] = 0x01  // moving right
        }
    }

    //> ChkPUSte:  lda Enemy_State+5          ;check power-up object's state
    //> cmp #$06                   ;for if power-up has risen enough
    //> bcc ExitPUp                ;if not, don't even bother running these routines
    if ((ram.enemyState[5].toInt() and 0xFF) < 0x06) return  // numeric rise counter comparison

    //> RunPUSubs:
    runPowerUpSubs()
    //> ExitPUp:   rts
//> ;$06-$07 - used as block buffer address indirect
//> ;$05 - used to store metatile stored in A at beginning of PlayerHeadCollision
//> ;$02 - used to store vertical high nybble offset from block buffer routine
//> ;$00 - used to store metatile from block buffer routine
//> ;These apply to all routines in this section unless otherwise noted:
}

/**
 * Runs the common power-up subroutines: relative position, offscreen bits,
 * bounding box, draw, collision, and bounds check.
 */
// by Claude
private fun System.runPowerUpSubs() {
    //> RunPUSubs: jsr RelativeEnemyPosition  ;get coordinates relative to screen
    relativeEnemyPosition()
    //> jsr GetEnemyOffscreenBits  ;get offscreen bits
    getEnemyOffscreenBits()
    //> jsr GetEnemyBoundBox       ;get bounding box coordinates
    getEnemyBoundBox()
    //> jsr DrawPowerUp            ;draw the power-up object
    drawPowerUp()
    //> jsr PlayerEnemyCollision   ;check for collision with player
    playerEnemyCollision()
    //> jsr OffscreenBoundsCheck   ;check to see if it went offscreen
    offscreenBoundsCheck()
}

// -------------------------------------------------------------------------------------
// MoveJumpingEnemy, MoveNormalEnemy, EnemyJump
// These are part of the enemy movement system shared with other enemy types.
// -------------------------------------------------------------------------------------

/**
 * Imposes gravity on a jumping enemy (paratroopa, star power-up) and moves horizontally.
 * MoveJ_EnemyVertically uses downForce=$1c, maxSpeed=$03.
 */
// by Claude
fun System.moveJumpingEnemy() {
    //> MoveJumpingEnemy:
    //> jsr MoveJ_EnemyVertically  ;do a sub to impose gravity on green paratroopa
    val x = ram.objectOffset.toInt()
    //> MoveJ_EnemyVertically: ldy #$1c; SetHiMax: lda #$03
    //> inx; jsr ImposeGravitySprObj
    imposeGravitySprObj(sprObjOffset = x + 1, downForce = 0x1c, maxSpeed = 0x03)
    //> jmp MoveEnemyHorizontally  ;jump to move enemy horizontally
    moveEnemyHorizontally()
}

/**
 * Moves a normal enemy with state-dependent vertical and horizontal movement.
 * Handles stunned enemies, defeated enemies, falling enemies, and normal sliding.
 */
// by Claude
fun System.moveNormalEnemy() {
    //> MoveNormalEnemy:
    val x = ram.objectOffset.toInt()
    val state = ram.enemyState.getEnemyState(x)

    //> and #%01000000             ;check enemy state for d6 set
    //> bne FallE                  ;to move enemy vertically, then horizontally if necessary
    if (state.fallingOffEdge) {
        // FallE path
        moveD_EnemyVertically()
        val stateAfterFall = ram.enemyState.getEnemyState(x)
        if (!stateAfterFall.fallingOffEdge) {
            moveSteadyEnemy(x)
            return
        }
        if (ram.enemyID[x] == EnemyId.PowerUpObject.byte) {
            moveSteadyEnemy(x)
            return
        }
        moveSlowEnemy(x)
        return
    }

    //> lda Enemy_State,x
    //> asl                        ;check enemy state for d7 set
    //> bcs SteadM                 ;if set, branch to move enemy horizontally
    if (state.kickedOrEmerged) {
        moveSteadyEnemy(x)
        return
    }

    //> lda Enemy_State,x
    //> and #%00100000             ;check enemy state for d5 set
    //> bne MoveDefeatedEnemy      ;if set, branch to move defeated enemy object
    if (state.defeated) {
        moveD_EnemyVertically()
        moveEnemyHorizontally()
        return
    }

    //> lda Enemy_State,x
    //> and #%00000111             ;check d2-d0 of enemy state for any set bits
    val lowBits = state.lowBits
    //> beq SteadM                 ;if enemy in normal state, branch to move enemy horizontally
    if (lowBits == 0) {
        moveSteadyEnemy(x)
        return
    }

    //> cmp #$02; beq FallE        ;if state $02 (stomped goomba), branch to move vertically
    if (lowBits == 0x02) {
        // FallE: MoveD_EnemyVertically, then cmp #$02 beq MEHor → just move horizontally, no revive
        moveD_EnemyVertically()
        moveEnemyHorizontally()
        return
    }

    //> cmp #$05; beq FallE        ;if state $05 (spiny egg), branch to move vertically
    if (lowBits == 0x05) {
        moveD_EnemyVertically()
        // after FallE: and #%01000000; beq SteadM
        val stateAfterFall = ram.enemyState.getEnemyState(x)
        if (!stateAfterFall.fallingOffEdge) {
            moveSteadyEnemy(x)
        } else {
            // cmp #PowerUpObject; beq SteadM; bne SlowM
            if (ram.enemyID[x] == EnemyId.PowerUpObject.byte) {
                moveSteadyEnemy(x)
            } else {
                moveSlowEnemy(x)
            }
        }
        return
    }

    //> cmp #$03; bcs ReviveStunned ;if state $03 or $04 (stunned), branch ahead
    if (lowBits == 0x03 || lowBits == 0x04) {
        reviveStunned(x)
        return
    }

    // Default: for states like $01 (Koopa in shell)
    //> SteadM:
    moveD_EnemyVertically()
    moveSteadyEnemy(x)
}

//> XSpeedAdderData:
//>       .db $00, $e8, $00, $18
// by Claude - fixed: was incorrectly $d8/$28, assembly has $e8/$18
private val xSpeedAdderData = intArrayOf(0x00, 0xe8, 0x00, 0x18)

/**
 * Moves enemy horizontally at current speed with no speed adjustment.
 */
// by Claude
private fun System.moveSteadyEnemy(x: Int) {
    //> SteadM: lda Enemy_X_Speed,x; pha; bpl AddHS
    //> AddHS:  clc
    //> Y=0 for steady, so XSpeedAdderData[0]=$00 or XSpeedAdderData[2]=$00
    // No speed adjustment needed for steady movement
    moveEnemyHorizontally()
}

/**
 * Moves enemy horizontally with reduced speed (for d6-set enemies that aren't power-ups).
 */
// by Claude
private fun System.moveSlowEnemy(x: Int) {
    //> SlowM: ldy #$01
    val origSpeed = ram.sprObjXSpeed[x + 1]
    var adderIdx = 1  // slow offset
    if (origSpeed >= 0) {
        // moving right, don't adjust Y
    } else {
        adderIdx += 2  // adjust for negative speed
    }
    //> clc; adc XSpeedAdderData,y; sta Enemy_X_Speed,x
    ram.sprObjXSpeed[x + 1] = ((origSpeed.toInt() and 0xFF) + xSpeedAdderData[adderIdx]).toByte()
    //> jsr MoveEnemyHorizontally
    moveEnemyHorizontally()
    //> pla; sta Enemy_X_Speed,x  ;restore original speed
    ram.sprObjXSpeed[x + 1] = origSpeed
}

//> RevivedXSpeed:
//>       .db $08, $f8, $0c, $f4
// by Claude
private val revivedXSpeed = byteArrayOf(0x08, 0xf8.toByte(), 0x0c, 0xf4.toByte())

/**
 * Handles enemies in stunned state ($03 or $04) - revives them when timer expires.
 */
// by Claude
private fun System.reviveStunned(x: Int) {
    //> ReviveStunned:
    //> lda EnemyIntervalTimer,x  ;if enemy timer not expired yet,
    //> bne ChkKillGoomba         ;skip ahead to something else
    val timer = ram.timers[0x16 + x].toInt() and 0xFF
    if (timer != 0) {
        //> ChkKillGoomba:
        //> NKGmba: rts                   ;leave!
        //> cmp #$0e              ;check to see if enemy timer has reached a certain point
        //> bne NKGmba
        if (timer == 0x0e) {
            //> lda Enemy_ID,x; cmp #Goomba; bne NKGmba
            if (ram.enemyID[x] == EnemyId.Goomba.byte) {
                //> jsr EraseEnemyObject
                eraseEnemyObject()
            }
        }
        return
    }

    //> sta Enemy_State,x         ;initialize enemy state to normal
    ram.enemyState[x] = EnemyState.INACTIVE.byte
    //> lda FrameCounter; and #$01; tay; iny
    val dir = (ram.frameCounter.toInt() and 0x01) + 1
    //> sty Enemy_MovingDir,x     ;store as pseudorandom movement direction
    ram.enemyMovingDirs[x] = dir.toByte()
    //> dey                       ;decrement for use as pointer
    //> MoveDefeatedEnemy:
    //> SetRSpd: lda RevivedXSpeed,y       ;load and store new horizontal speed
    var speedIdx = dir - 1
    //> lda PrimaryHardMode; beq SetRSpd
    if (ram.primaryHardMode) {
        //> iny; iny               ;increment 2 bytes to next data
        speedIdx += 2
    }
    //> lda RevivedXSpeed,y; sta Enemy_X_Speed,x
    ram.sprObjXSpeed[x + 1] = revivedXSpeed[speedIdx]
}

/**
 * Handles star/paratroopa bounce physics: checks if enemy is on ground and makes it jump.
 * Delegates to the full ChkUnderEnemy -> ChkForNonSolids -> EnemyLanding chain
 * in enemyBGCollision.kt for proper block buffer collision detection.
 */
// by Claude - fixed to use real block buffer collision instead of stub
fun System.enemyJump() {
    //> EnemyJump:
    //> jsr SubtEnemyYPos     ;do a sub here
    //> bcc DoSide            ;if result < threshold, branch to side check
    val x = ram.objectOffset.toInt()
    val enemyY = ram.sprObjYPos[x + 1].toInt() and 0xFF
    //> SubtEnemyYPos: lda Enemy_Y_Position,x; clc; adc #$3e; cmp #$44
    val subtResult = (enemyY + 0x3e) and 0xFF
    if (subtResult < 0x44) {
        doEnemySideCheck(x)
        return
    }

    //> lda Enemy_Y_Speed,x; clc; adc #$02; cmp #$03
    val ySpeed = (ram.sprObjYSpeed[x + 1].toInt() and 0xFF) + 2
    //> bcc DoSide             ;if not falling fast enough, just check sides
    if ((ySpeed and 0xFF) < 0x03) {
        doEnemySideCheck(x)
        return
    }

    //> jsr ChkUnderEnemy     ;check if standing on anything
    //> beq DoSide            ;if not, just check sides
    val underResult = chkUnderEnemyFull(x)
    if (underResult.metatile == 0.toByte()) {
        doEnemySideCheck(x)
        return
    }

    //> jsr ChkForNonSolids   ;check for non-solid blocks
    //> beq DoSide
    if (chkForNonSolidsLocal(underResult.metatile.toInt() and 0xFF)) {
        doEnemySideCheck(x)
        return
    }

    //> jsr EnemyLanding      ;change vertical coordinate and speed
    enemyLanding(x)

    //> lda #$fd
    //> sta Enemy_Y_Speed,x   ;make the paratroopa/star jump again
    ram.sprObjYSpeed[x + 1] = 0xfd.toByte()

    //> DoSide: jmp DoEnemySideCheck
    doEnemySideCheck(x)
}

// -------------------------------------------------------------------------------------
// DrawPowerUp
// -------------------------------------------------------------------------------------

/**
 * Draws the power-up object (4 sprites in a 2x2 grid).
 * Regular mushroom and 1-up use static tiles; fire flower and star animate palette colors.
 * Fire flower and star also get horizontal flip on right-side sprites.
 */
// by Claude
private fun System.drawPowerUp() {
    //> DrawPowerUp:
    //> ldy Enemy_SprDataOffset+5  ;get power-up's sprite data offset
    val sprOfs = (ram.enemySprDataOffset[5].toInt() and 0xFF) shr 2

    //> lda Enemy_Rel_YPos         ;get relative vertical coordinate
    //> clc; adc #$08              ;add eight pixels
    //> sta $02                    ;store result here
    var yCoord = ((ram.enemyRelYPos.toInt() and 0xFF) + 8) and 0xFF

    //> lda Enemy_Rel_XPos         ;get relative horizontal coordinate
    //> sta $05                    ;store here
    val xCoord = ram.enemyRelXPos.toInt() and 0xFF

    //> ldx PowerUpType            ;get power-up type
    val puType = ram.powerUpType.toInt() and 0xFF

    //> lda PowerUpAttributes,x    ;get attribute data for power-up type
    //> ora Enemy_SprAttrib+5      ;add background priority bit if set
    //> sta $04                    ;store attributes here
    val baseAttribs = (powerUpAttributes[puType].toInt() and 0xFF) or
            (ram.sprAttrib[6].toInt() and 0xFF)  // by Claude - Enemy_SprAttrib+5 = sprAttrib[6]

    //> txa; pha                   ;save power-up type to the stack
    //> asl; asl                   ;multiply by four to get proper offset
    //> tax                        ;use as X
    var tblOfs = puType * 4

    //> lda #$01
    //> sta $07                    ;set counter here to draw two rows of sprite object
    //> sta $03                    ;init d1 of flip control (no flip)

    //> PUpDrawLoop:
    var sprIdx = sprOfs
    for (row in 0..1) {
        //> lda PowerUpGfxTable,x      ;load left tile of power-up object
        //> sta $00
        //> lda PowerUpGfxTable+1,x    ;load right tile
        //> jsr DrawOneSpriteRow       ;branch to draw one row
        val tile0 = powerUpGfxTable[tblOfs]
        val tile1 = powerUpGfxTable[tblOfs + 1]

        // Draw two side-by-side sprites (one row)
        ram.sprites[sprIdx].tilenumber = tile0
        ram.sprites[sprIdx + 1].tilenumber = tile1
        ram.sprites[sprIdx].y = yCoord.toUByte()
        ram.sprites[sprIdx + 1].y = yCoord.toUByte()
        ram.sprites[sprIdx].x = xCoord.toUByte()
        ram.sprites[sprIdx + 1].x = ((xCoord + 8) and 0xFF).toUByte()
        ram.sprites[sprIdx].attributes = SpriteFlags(baseAttribs.toByte())
        ram.sprites[sprIdx + 1].attributes = SpriteFlags(baseAttribs.toByte())

        yCoord = (yCoord + 8) and 0xFF
        sprIdx += 2
        tblOfs += 2
        //> dec $07; bpl PUpDrawLoop
    }

    //> ldy Enemy_SprDataOffset+5  ;get sprite data offset again
    //> pla                        ;pull saved power-up type from the stack
    //> beq PUpOfs                 ;if regular mushroom, do not change colors or flip
    //> cmp #$03; beq PUpOfs       ;if 1-up mushroom, do not change colors or flip
    if (puType != 0x00 && puType != 0x03) {
        //> sta $00                    ;store power-up type here now
        //> lda FrameCounter           ;get frame counter
        //> lsr                        ;divide by 2 to change colors every two frames
        //> and #%00000011             ;mask out all but d1 and d0
        val colorBits = ((ram.frameCounter.toInt() and 0xFF) ushr 1) and 0x03
        //> ora Enemy_SprAttrib+5      ;add background priority bit if any set
        val animAttribs = colorBits or (ram.sprAttrib[6].toInt() and 0xFF) // by Claude - Enemy_SprAttrib+5

        //> sta Sprite_Attributes,y    ;set as new palette bits for top left and
        //> sta Sprite_Attributes+4,y  ;top right sprites for fire flower and star
        ram.sprites[sprOfs].attributes = SpriteFlags(animAttribs.toByte())
        ram.sprites[sprOfs + 1].attributes = SpriteFlags(animAttribs.toByte())

        //> ldx $00; dex              ;check power-up type for fire flower
        //> beq FlipPUpRightSide      ;if found, skip bottom sprites
        if (puType != 0x01) {
            //> sta Sprite_Attributes+8,y  ;otherwise set new palette bits for bottom left
            //> sta Sprite_Attributes+12,y ;and bottom right sprites as well for star only
            ram.sprites[sprOfs + 2].attributes = SpriteFlags(animAttribs.toByte())
            ram.sprites[sprOfs + 3].attributes = SpriteFlags(animAttribs.toByte())
        }

        //> FlipPUpRightSide:
        //> lda Sprite_Attributes+4,y
        //> ora #%01000000             ;set horizontal flip bit for top right sprite
        //> sta Sprite_Attributes+4,y
        val topRightAttr = ram.sprites[sprOfs + 1].attributes.byte.toInt() and 0xFF
        ram.sprites[sprOfs + 1].attributes = SpriteFlags((topRightAttr or 0x40).toByte())

        //> lda Sprite_Attributes+12,y
        //> ora #%01000000             ;set horizontal flip bit for bottom right sprite
        //> sta Sprite_Attributes+12,y
        val botRightAttr = ram.sprites[sprOfs + 3].attributes.byte.toInt() and 0xFF
        ram.sprites[sprOfs + 3].attributes = SpriteFlags((botRightAttr or 0x40).toByte())
    }

    //> PUpOfs: jmp SprObjectOffscrChk
    //> ;$ef - used to hold enemy code used in gfx handler (may or may not resemble Enemy_ID values)
    //> ;$ed - used to hold enemy state from buffer
    //> ;$ec - used to hold either altered enemy state or special value used in gfx handler as condition
    //> ;$eb - used to hold sprite data offset
    //> ;$05 - used to store X position
    //> ;$04 - used to store enemy's sprite attributes
    //> ;$03 - used to store moving direction, used to flip enemies horizontally
    //> ;$02 - used to store Y position
    //> ;$00-$01 - used in DrawEnemyObjRow to hold sprite tile numbers
    sprObjectOffscrChkPowerUp(sprOfs)
}

/**
 * Checks offscreen bits for the power-up object and moves sprites offscreen as needed.
 * This is a simplified version of SprObjectOffscrChk for the power-up slot.
 */
// by Claude
private fun System.sprObjectOffscrChkPowerUp(sprOfs: Int) {
    //> SprObjectOffscrChk:
    val x = ram.objectOffset.toInt()
    val offscr = ram.enemyOffscreenBits.toInt() and 0xFF

    //> lsr; lsr; lsr              ;shift right 3 times, d2 ends up in carry
    //> bcc LcChk                  ;branch if d2 not set
    if ((offscr and 0x04) != 0) {
        //> lda #$04; jsr MoveESprColOffscreen  ;move right column offscreen
        val colOfs = sprOfs + 1  // right column
        ram.sprites[colOfs].y = 0xf8.toUByte()
        ram.sprites[colOfs + 2].y = 0xf8.toUByte()
    }

    //> pla; lsr; bcc Row3C        ;d3 = left column
    if ((offscr and 0x08) != 0) {
        //> lda #$00; jsr MoveESprColOffscreen  ;move left column offscreen
        ram.sprites[sprOfs].y = 0xf8.toUByte()
        ram.sprites[sprOfs + 2].y = 0xf8.toUByte()
    }

    //> lsr; lsr; bcc Row23C       ;d5 = third row
    if ((offscr and 0x20) != 0) {
        //> lda #$10; jsr MoveESprRowOffscreen  ;move third row offscreen
        // Power-ups only have 2 rows (4 sprites), so row 3 doesn't apply
    }

    //> pla; lsr; bcc AllRowC      ;d6 = second and third rows
    if ((offscr and 0x40) != 0) {
        //> lda #$08; jsr MoveESprRowOffscreen
        ram.sprites[sprOfs + 2].y = 0xf8.toUByte()
        ram.sprites[sprOfs + 3].y = 0xf8.toUByte()
    }

    //> pla; lsr; bcc ExEGHandler  ;d7 = all rows
    if ((offscr and 0x80) != 0) {
        //> jsr MoveESprRowOffscreen  ;move all sprites offscreen
        for (i in 0..3) {
            ram.sprites[sprOfs + i].y = 0xf8.toUByte()
        }
        //> lda Enemy_ID,x; cmp #Podoboo; beq ExEGHandler
        if (ram.enemyID[x] != EnemyId.Podoboo.byte) {
            //> lda Enemy_Y_HighPos,x; cmp #$02; bne ExEGHandler
            if ((ram.sprObjYHighPos[x + 1].toInt() and 0xFF) == 0x02) {
                //> jsr EraseEnemyObject
                eraseEnemyObject()
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// VineObjectHandler
// -------------------------------------------------------------------------------------

/**
 * Handles vine growth and rendering.
 * Vine grows upward from a block, one pixel every 4 frames, until it reaches
 * a height determined by VineHeightData. When tall enough, writes climbing
 * metatiles to the block buffer so the player can climb.
 */
// by Claude
fun System.vineObjectHandler() {
    //> VineObjectHandler:
    val x = ram.objectOffset.toInt()

    //> cpx #$05                  ;check enemy offset for special use slot
    //> bne ExitVH                ;if not in last slot, branch to leave
    if (x != 5) return

    //> ldy VineFlagOffset
    //> dey                       ;decrement vine flag in Y, use as offset
    val vineFlagOfs = ram.vineFlagOffset.toInt() and 0xFF
    if (vineFlagOfs == 0) return  // no vine spawned yet
    val y = vineFlagOfs - 1

    //> lda VineHeight
    val vineHeight = ram.vineHeight.toInt() and 0xFF
    //> cmp VineHeightData,y      ;if vine has reached certain height,
    //> beq RunVSubs              ;branch ahead to skip this part
    if (vineHeight != vineHeightData[y]) {
        //> lda FrameCounter          ;get frame counter
        //> lsr; lsr                  ;shift d1 into carry
        //> bcc RunVSubs              ;if d1 not set (2 frames every 4) skip this part
        if ((ram.frameCounter.toInt() and 0x02) != 0) {
            //> lda Enemy_Y_Position+5
            //> sbc #$01                  ;subtract vertical position of vine (carry is set from lsr)
            //> sta Enemy_Y_Position+5    ;one pixel every frame it's time
            ram.sprObjYPos[6] = (ram.sprObjYPos[6] - 1).toByte()

            //> inc VineHeight            ;increment vine height
            ram.vineHeight = ((vineHeight + 1) and 0xFF).toByte()
        }
    }

    //> RunVSubs:  lda VineHeight            ;if vine still very small,
    //> cmp #$08                  ;branch to leave
    //> bcc ExitVH
    val currentHeight = ram.vineHeight.toInt() and 0xFF
    if (currentHeight < 0x08) return

    //> jsr RelativeEnemyPosition ;get relative coordinates of vine,
    relativeEnemyPosition()
    //> jsr GetEnemyOffscreenBits ;and any offscreen bits
    getEnemyOffscreenBits()

    //> ldy #$00                  ;initialize offset used in draw vine sub
    //> VDrawLoop: jsr DrawVine   ;draw vine
    //> iny                       ;increment offset
    //> cpy VineFlagOffset        ;if offset in Y and offset here
    //> bne VDrawLoop             ;do not yet match, loop back to draw more vine
    var drawY = 0
    do {
        drawVine(drawY)
        drawY++
    } while (drawY != (ram.vineFlagOffset.toInt() and 0xFF))

    //> lda Enemy_OffscreenBits
    //> and #%00001100            ;mask offscreen bits
    //> beq WrCMTile              ;if none of the saved offscreen bits set, skip ahead
    val offscreenBits = ram.enemyOffscreenBits.toInt() and 0x0c
    if (offscreenBits != 0) {
        //> dey                       ;otherwise decrement Y to get proper offset again
        var killY = drawY - 1
        //> KillVine: ldx VineObjOffset,y       ;get enemy object offset for this vine object
        //> jsr EraseEnemyObject      ;kill this vine object
        //> dey                       ;decrement Y
        //> bpl KillVine              ;if any vine objects left, loop back to kill it
        while (killY >= 0) {
            val vineObjX = ram.vineObjOffsets[killY].toInt() and 0xFF
            eraseEnemyObject(vineObjX)
            killY--
        }
        //> sta VineFlagOffset        ;initialize vine flag/offset
        ram.vineFlagOffset = 0
        //> sta VineHeight            ;initialize vine height
        ram.vineHeight = 0
    }

    //> WrCMTile:  lda VineHeight            ;check vine height
    //> cmp #$20                  ;if vine small (less than 32 pixels tall)
    //> bcc ExitVH                ;then branch ahead to leave
    val heightNow = ram.vineHeight.toInt() and 0xFF
    if (heightNow >= 0x20) {
        //> ldx #$06                  ;set offset in X to last enemy slot
        //> lda #$01                  ;set A to obtain horizontal coordinate
        //> ldy #$1b                  ;set Y to offset to get block at ($04, $10) of coordinates
        //> jsr BlockBufferCollision  ;do a sub to get block buffer address set, return contents
        // TODO: BlockBufferCollision is private in collisionDetection.kt
        // This call checks the block buffer above the vine to write climbing metatiles.
        // For now we write the climbing metatile using a simplified approach.
        writeVineClimbingMetatile()
    }

    //> ExitVH:    ldx ObjectOffset          ;get enemy object offset and leave
    // ObjectOffset is already set
}

/**
 * Writes climbing metatile ($26) to the block buffer at the vine's position.
 * This allows the player to grab and climb the vine.
 *
 * Simplified from the assembly which uses BlockBufferCollision to compute the
 * block buffer address, then checks/writes the metatile.
 */
// by Claude
private fun System.writeVineClimbingMetatile() {
    //> ldx #$06                  ;set offset in X to last enemy slot
    //> lda #$01                  ;set A to obtain horizontal coordinate
    //> ldy #$1b                  ;set Y to offset to get block at ($04, $10) of coordinates
    //> jsr BlockBufferCollision
    val result = blockBufferCollision(sprObjOffset = 6, adderOffset = 0x1b, returnHorizontal = true)
    //> ldy $02
    //> cpy #$d0                  ;if vertical high nybble offset beyond extent of
    //> bcs ExitVH                ;current block buffer, branch to leave, do not write
    if (result.vertOffset >= 0xd0) return
    //> lda ($06),y               ;check contents of block buffer at current offset
    //> bne ExitVH                ;if not empty, branch to leave
    if (result.blockBuffer[result.blockBufferBase + result.vertOffset] != 0.toByte()) return
    //> lda #$26
    //> sta ($06),y               ;write climbing metatile to block buffer
    result.blockBuffer[result.blockBufferBase + result.vertOffset] = 0x26
}

// -------------------------------------------------------------------------------------
// DrawVine
// -------------------------------------------------------------------------------------

/**
 * Draws one vine segment (6 sprites stacked vertically in a staggered pattern).
 * Each vine segment consists of alternating left-right tiles to create the
 * characteristic winding vine appearance.
 *
 * @param vineIndex which vine segment to draw (0 = topmost, incrementing downward)
 */
// by Claude
private fun System.drawVine(vineIndex: Int) {
    //> DrawVine:
    //> sty $00                    ;save offset here

    //> lda Enemy_Rel_YPos         ;get relative vertical coordinate
    //> clc
    //> adc VineYPosAdder,y        ;add value using offset in Y to get value
    val relY = ram.enemyRelYPos.toInt() and 0xFF
    var yPos = (relY + vineYPosAdder[vineIndex]) and 0xFF

    //> ldx VineObjOffset,y        ;get offset to vine
    val vineObjX = ram.vineObjOffsets[vineIndex].toInt() and 0xFF
    //> ldy Enemy_SprDataOffset,x  ;get sprite data offset
    val sprOfs = (ram.enemySprDataOffset[vineObjX].toInt() and 0xFF) shr 2

    //> jsr SixSpriteStacker       ;stack six sprites on top of each other vertically
    // Stores Y position into 6 consecutive sprites, each 8 pixels apart
    for (i in 0..5) {
        ram.sprites[sprOfs + i].y = (yPos and 0xFF).toUByte()
        yPos = (yPos + 8) and 0xFF
    }

    //> lda Enemy_Rel_XPos         ;get relative horizontal coordinate
    val relX = ram.enemyRelXPos.toInt() and 0xFF
    //> sta Sprite_X_Position,y    ;store in first, third and fifth sprites
    //> sta Sprite_X_Position+8,y
    //> sta Sprite_X_Position+16,y
    ram.sprites[sprOfs].x = relX.toUByte()
    ram.sprites[sprOfs + 2].x = relX.toUByte()
    ram.sprites[sprOfs + 4].x = relX.toUByte()

    //> clc; adc #$06              ;add six pixels to second, fourth and sixth sprites
    //> sta Sprite_X_Position+4,y  ;to give characteristic staggered vine shape
    //> sta Sprite_X_Position+12,y
    //> sta Sprite_X_Position+20,y
    val rightX = ((relX + 6) and 0xFF).toUByte()
    ram.sprites[sprOfs + 1].x = rightX
    ram.sprites[sprOfs + 3].x = rightX
    ram.sprites[sprOfs + 5].x = rightX

    //> lda #%00100001             ;set bg priority and palette attribute bits
    //> sta Sprite_Attributes,y    ;set in first, third and fifth sprites
    //> sta Sprite_Attributes+8,y
    //> sta Sprite_Attributes+16,y
    val leftAttribs = SpriteFlags(0x21)
    ram.sprites[sprOfs].attributes = leftAttribs
    ram.sprites[sprOfs + 2].attributes = leftAttribs
    ram.sprites[sprOfs + 4].attributes = leftAttribs

    //> ora #%01000000             ;additionally, set horizontal flip bit
    //> sta Sprite_Attributes+4,y  ;for second, fourth and sixth sprites
    //> sta Sprite_Attributes+12,y
    //> sta Sprite_Attributes+20,y
    val rightAttribs = SpriteFlags(0x61)
    ram.sprites[sprOfs + 1].attributes = rightAttribs
    ram.sprites[sprOfs + 3].attributes = rightAttribs
    ram.sprites[sprOfs + 5].attributes = rightAttribs

    //> ldx #$05                   ;set tiles for six sprites
    //> VineTL: lda #$e1           ;set tile number for sprite
    //> sta Sprite_Tilenumber,y
    //> bpl VineTL                 ;loop until all sprites are done
    for (i in 0..5) {
        ram.sprites[sprOfs + i].tilenumber = 0xe1.toByte()
    }

    //> ldy $02                    ;get original offset
    //> lda $00                    ;get offset to vine adding data
    //> bne SkpVTop                ;if offset not zero, skip this part
    if (vineIndex == 0) {
        //> lda #$e0
        //> sta Sprite_Tilenumber,y    ;set other tile number for top of vine
        ram.sprites[sprOfs].tilenumber = 0xe0.toByte()
    }

    //> SkpVTop: ldx #$00           ;start with the first sprite again
    //> ChkFTop: lda VineStart_Y_Position
    //> bne ChkFTop
    //> NextVSp: iny                        ;move offset to next OAM data
    val startY = ram.vineStartYPosition.toInt() and 0xFF
    //> sec; sbc Sprite_Y_Position,y   ;subtract sprite's Y coordinate
    //> cmp #$64                   ;if two coordinates are less than 100 pixels apart
    //> bcc NextVSp                ;skip to leave sprite alone
    for (i in 0..5) {
        val sprY = ram.sprites[sprOfs + i].y.toInt() and 0xFF
        val diff = (startY - sprY) and 0xFF
        if (diff >= 0x64) {
            //> lda #$f8; sta Sprite_Y_Position,y   ;otherwise move sprite offscreen
            ram.sprites[sprOfs + i].y = 0xf8.toUByte()
        }
    }
}

// -------------------------------------------------------------------------------------
// Setup_Vine (called from block bump code when a vine block is hit)
// -------------------------------------------------------------------------------------

/**
 * Sets up a vine object in the given enemy slot using block object position data.
 *
 * @param enemySlot the enemy object slot to use (typically 5)
 * @param blockSlot the block object slot providing position data (from SprDataOffset_Ctrl)
 */
// by Claude
fun System.setupVine(enemySlot: Int, blockSlot: Int) {
    //> Setup_Vine:
    //> lda #VineObject          ;load identifier for vine object
    //> sta Enemy_ID,x           ;store in buffer
    ram.enemyID[enemySlot] = EnemyId.VineObject.byte

    //> lda #$01
    //> sta Enemy_Flag,x         ;set flag for enemy object buffer
    ram.enemyFlags[enemySlot] = 1

    //> lda Block_PageLoc,y
    //> sta Enemy_PageLoc,x      ;copy page location from block object
    ram.sprObjPageLoc[enemySlot + 1] = ram.sprObjPageLoc[9 + blockSlot]

    //> lda Block_X_Position,y
    //> sta Enemy_X_Position,x   ;copy horizontal coordinate from block object
    ram.sprObjXPos[enemySlot + 1] = ram.sprObjXPos[9 + blockSlot]

    //> lda Block_Y_Position,y
    //> sta Enemy_Y_Position,x   ;copy vertical coordinate from block object
    ram.sprObjYPos[enemySlot + 1] = ram.sprObjYPos[9 + blockSlot]

    //> ldy VineFlagOffset       ;load vine flag/offset to next available vine slot
    val vineFlagOfs = ram.vineFlagOffset.toInt() and 0xFF
    //> bne NextVO               ;if set at all, don't bother to store vertical
    if (vineFlagOfs == 0) {
        //> sta VineStart_Y_Position ;otherwise store vertical coordinate here
        ram.vineStartYPosition = ram.sprObjYPos[enemySlot + 1]
    }

    //> NextVO: txa                      ;store object offset to next available vine slot
    //> sta VineObjOffset,y      ;using vine flag as offset
    ram.vineObjOffsets[vineFlagOfs] = enemySlot.toByte()

    //> inc VineFlagOffset       ;increment vine flag offset
    ram.vineFlagOffset = ((vineFlagOfs + 1) and 0xFF).toByte()

    //> lda #Sfx_GrowVine
    //> sta Square2SoundQueue    ;load vine grow sound
    ram.square2SoundQueue = Constants.Sfx_GrowVine
}

// -------------------------------------------------------------------------------------
// SetupPowerUp (called from block bump code when a power-up block is hit)
// -------------------------------------------------------------------------------------

/**
 * Creates a power-up object in enemy slot 5. The power-up type is determined by:
 * - If PowerUpType is star ($02) or 1-up ($03), use it as-is
 * - Otherwise, use PlayerStatus to decide: small -> mushroom, big -> mushroom, fiery -> fire flower
 *
 * @param blockSlot the block object slot (from SprDataOffset_Ctrl) providing position
 */
// by Claude
fun System.setupPowerUp(blockSlot: Int) {
    //> SetupPowerUp:
    //> lda #PowerUpObject        ;load power-up identifier into
    //> sta Enemy_ID+5            ;special use slot of enemy object buffer
    ram.enemyID[5] = EnemyId.PowerUpObject.byte

    //> lda Block_PageLoc,x       ;store page location of block object
    //> sta Enemy_PageLoc+5
    ram.sprObjPageLoc[6] = ram.sprObjPageLoc[9 + blockSlot]

    //> lda Block_X_Position,x    ;store horizontal coordinate of block object
    //> sta Enemy_X_Position+5
    ram.sprObjXPos[6] = ram.sprObjXPos[9 + blockSlot]

    //> lda #$01
    //> sta Enemy_Y_HighPos+5     ;set vertical high byte of power-up object
    ram.sprObjYHighPos[6] = 1

    //> lda Block_Y_Position,x    ;get vertical coordinate of block object
    //> sec; sbc #$08             ;subtract 8 pixels
    //> sta Enemy_Y_Position+5    ;and use as vertical coordinate of power-up object
    val blockY = ram.sprObjYPos[9 + blockSlot].toInt() and 0xFF
    ram.sprObjYPos[6] = ((blockY - 8) and 0xFF).toByte()

    //> PwrUpJmp: lda #$01
    //> sta Enemy_State+5         ;set power-up object's state
    ram.enemyState[5] = EnemyState.NORMAL.byte
    //> sta Enemy_Flag+5          ;set buffer flag
    ram.enemyFlags[5] = 1

    //> lda #$03
    //> sta Enemy_BoundBoxCtrl+5  ;set bounding box size control for power-up object
    ram.enemyBoundBoxCtrls[5] = 3

    //> lda PowerUpType
    //> cmp #$02                  ;check currently loaded power-up type
    //> bcs PutBehind             ;if star or 1-up, branch ahead
    val puType = ram.powerUpType.toInt() and 0xFF
    if (puType < 0x02) {
        //> lda PlayerStatus          ;otherwise check player's current status
        val playerStatusOrd = ram.playerStatus.ordinal
        //> cmp #$02; bcc StrType     ;if player not fiery, use status as power-up type
        val newType = if (playerStatusOrd >= 0x02) {
            //> lsr                       ;otherwise shift right to force fire flower type
            playerStatusOrd ushr 1
        } else {
            playerStatusOrd
        }
        //> StrType: sta PowerUpType   ;store type here
        ram.powerUpType = newType.toByte()
    }

    //> PutBehind: lda #%00100000
    //> sta Enemy_SprAttrib+5     ;set background priority bit
    ram.sprAttrib[6] = 0x20  // by Claude - Enemy_SprAttrib+5 = sprAttrib[6] (power-up slot)

    //> lda #Sfx_GrowPowerUp
    //> sta Square2SoundQueue     ;load power-up reveal sound and leave
    ram.square2SoundQueue = Constants.Sfx_GrowPowerUp
}

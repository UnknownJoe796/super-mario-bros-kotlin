// by Claude - ProcessCannons, BulletBillHandler, OffscreenBoundsCheck subroutines
// Translates the bullet bill cannon spawning and handling chain.
package com.ivieleague.smbtranslation

//> CannonBitmasks:
//> .db $0f, $07
private val cannonBitmasks = intArrayOf(0x0f, 0x07)

//> BulletBillXSpdData:
//> .db $18, $e8
private val bulletBillXSpdData = intArrayOf(0x18, 0xe8)

/**
 * Processes bullet bill cannons. Checks the first three enemy slots (2 down to 0)
 * for cannon firing opportunities and handles existing bullet bills.
 */
fun System.processCannons() {
    //> ProcessCannons:
    //> lda AreaType                ;get area type
    //> beq ExCannon                ;if water type area, branch to leave
    if (ram.areaType == AreaType.Water) return

    //> ldx #$02
    //> ThreeSChk:
    for (x in 2 downTo 0) {
        //> stx ObjectOffset            ;start at third enemy slot
        ram.objectOffset = x.toByte()

        //> lda Enemy_Flag,x            ;check enemy buffer flag
        //> bne Chk_BB                  ;if set, branch to check enemy
        if (ram.enemyFlags[x] == 0.toByte()) {
            //> lda PseudoRandomBitReg+1,x  ;otherwise get part of LSFR
            //> ldy SecondaryHardMode       ;get secondary hard mode flag, use as offset
            //> and CannonBitmasks,y        ;mask out bits of LSFR
            val lsfr = ram.pseudoRandomBitReg[1 + x].toInt() and 0xFF
            val hardMode = ram.secondaryHardMode.toInt() and 0xFF
            val masked = lsfr and cannonBitmasks[if (hardMode != 0) 1 else 0]

            //> cmp #$06                    ;check to see if lower nybble is above certain value
            //> bcs Chk_BB                  ;if so, branch to check enemy
            if (masked < 6) {
                //> tay                         ;transfer masked contents to Y as pseudorandom offset
                val y = masked
                //> lda Cannon_PageLoc,y        ;get page location
                //> beq Chk_BB                  ;if not set or on page 0, branch to check enemy
                val cannonPage = ram.cannonPageLocs[y].toInt() and 0xFF
                if (cannonPage != 0) {
                    //> lda Cannon_Timer,y          ;get cannon timer
                    //> beq FireCannon              ;if expired, branch to fire cannon
                    val cannonTimer = ram.cannonTimers[y].toInt() and 0xFF
                    if (cannonTimer == 0) {
                        fireCannon(x, y)
                        continue
                    } else {
                        //> sbc #$00                    ;subtract borrow (carry clear, so subtracts 1)
                        //> sta Cannon_Timer,y          ;count timer down
                        ram.cannonTimers[y] = (cannonTimer - 1).toByte()
                        // fall through to Chk_BB
                    }
                }
            }
        }

        //> Chk_BB:
        chkBulletBill(x)
        //> Next3Slt: dex; bpl ThreeSChk
    }
    //> ExCannon: rts
}

private fun System.fireCannon(x: Int, y: Int) {
    //> FireCannon:
    //> lda TimerControl           ;if master timer control set,
    //> bne Chk_BB                 ;branch to check enemy
    if (ram.timerControl != 0.toByte()) {
        chkBulletBill(x)
        return
    }

    //> lda #$0e
    //> sta Cannon_Timer,y         ;first, reset cannon timer
    ram.cannonTimers[y] = 0x0e
    //> lda Cannon_PageLoc,y       ;get page location of cannon
    //> sta Enemy_PageLoc,x        ;save as page location of bullet bill
    ram.sprObjPageLoc[1 + x] = ram.cannonPageLocs[y]
    //> lda Cannon_X_Position,y    ;get horizontal coordinate of cannon
    //> sta Enemy_X_Position,x     ;save as horizontal coordinate of bullet bill
    ram.sprObjXPos[1 + x] = ram.cannonXPositions[y]
    //> lda Cannon_Y_Position,y    ;get vertical coordinate of cannon
    //> sec
    //> sbc #$08                   ;subtract eight pixels
    //> sta Enemy_Y_Position,x     ;save as vertical coordinate of bullet bill
    ram.sprObjYPos[1 + x] = ((ram.cannonYPositions[y].toInt() and 0xFF) - 8).toByte()
    //> lda #$01
    //> sta Enemy_Y_HighPos,x      ;set vertical high byte of bullet bill
    ram.sprObjYHighPos[1 + x] = 1
    //> sta Enemy_Flag,x           ;set buffer flag
    ram.enemyFlags[x] = 1
    //> lsr                        ;shift right once to init A to 0
    //> sta Enemy_State,x          ;then initialize enemy's state
    ram.enemyState[x] = 0
    //> lda #$09
    //> sta Enemy_BoundBoxCtrl,x   ;set bounding box size control for bullet bill
    ram.enemyBoundBoxCtrls[x] = 0x09
    //> lda #BulletBill_CannonVar
    //> sta Enemy_ID,x             ;load identifier for bullet bill (cannon variant)
    ram.enemyID[x] = EnemyId.BulletBillCannonVar.byte
    //> jmp Next3Slt               ;move onto next slot (done via loop continue)
}

private fun System.chkBulletBill(x: Int) {
    //> Chk_BB:
    //> lda Enemy_ID,x             ;check enemy identifier for bullet bill (cannon variant)
    //> cmp #BulletBill_CannonVar
    //> bne Next3Slt               ;if not found, branch to get next slot
    if (ram.enemyID[x] != EnemyId.BulletBillCannonVar.byte) return

    //> jsr OffscreenBoundsCheck   ;otherwise, check to see if it went offscreen
    offscreenBoundsCheck()
    //> lda Enemy_Flag,x           ;check enemy buffer flag
    //> beq Next3Slt               ;if not set, branch to get next slot
    if (ram.enemyFlags[x] == 0.toByte()) return

    //> jsr GetEnemyOffscreenBits  ;otherwise, get offscreen information
    getEnemyOffscreenBits()
    //> jsr BulletBillHandler      ;then do sub to handle bullet bill
    bulletBillHandler(x)
}

/**
 * Handles bullet bill movement, collision, and rendering.
 */
private fun System.bulletBillHandler(x: Int) {
    //> BulletBillHandler:
    //> lda TimerControl          ;if master timer control set,
    //> bne RunBBSubs             ;branch to run subroutines except movement sub
    if (ram.timerControl != 0.toByte()) {
        runBBSubs()
        return
    }

    //> lda Enemy_State,x
    //> bne ChkDSte               ;if bullet bill's state set, branch to check defeated state
    val enemyStateVal = ram.enemyState[x].toInt() and 0xFF
    if (enemyStateVal != 0) {
        //> ChkDSte: lda Enemy_State,x  ;check enemy state for d5 set
        //> and #%00100000
        //> beq BBFly                 ;if not set, skip to move horizontally
        if ((enemyStateVal and 0x20) != 0) {
            //> jsr MoveD_EnemyVertically ;otherwise do sub to move bullet bill vertically
            moveD_EnemyVertically()
        }
        //> BBFly: jsr MoveEnemyHorizontally ;do sub to move bullet bill horizontally
        moveEnemyHorizontally()
        runBBSubs()
        return
    }

    //> lda Enemy_OffscreenBits   ;otherwise load offscreen bits
    //> and #%00001100            ;mask out bits
    //> cmp #%00001100            ;check to see if all bits are set
    //> beq KillBB                ;if so, branch to kill this object
    val offBits = ram.enemyOffscreenBits.toInt() and 0xFF
    if ((offBits and 0x0C) == 0x0C) {
        eraseEnemyObject(x)
        return
    }

    //> ldy #$01                  ;set to move right by default
    //> jsr PlayerEnemyDiff       ;get horizontal difference between player and bullet bill
    val (lowDiff, highDiff) = playerEnemyDiff()
    //> bmi SetupBB               ;if enemy to the left of player, branch
    // bmi checks sign of high byte (page difference)
    var dir = if ((highDiff and 0x80) != 0) 1 else 2  // 1=right, 2=left
    //> SetupBB:
    //> sty Enemy_MovingDir,x     ;set bullet bill's moving direction
    ram.enemyMovingDirs[x] = dir.toByte()
    //> dey                       ;decrement to use as offset
    //> lda BulletBillXSpdData,y  ;get horizontal speed based on moving direction
    //> sta Enemy_X_Speed,x       ;and store it
    ram.sprObjXSpeed[1 + x] = bulletBillXSpdData[dir - 1].toByte()

    //> lda $00                   ;get horizontal difference (low byte)
    //> adc #$28                  ;add 40 pixels (carry from PlayerEnemyDiff's sbc)
    //> cmp #$50                  ;if less than certain amount, player too close
    //> bcc KillBB                ;to cannon, thus branch
    // carry from PlayerEnemyDiff's final sbc Player_PageLoc: set when enemy at/right of player
    val pedCarry = if ((highDiff and 0x80) == 0) 1 else 0
    val absDiff = lowDiff + 0x28 + pedCarry
    if ((absDiff and 0xFF) < 0x50) {
        eraseEnemyObject(x)
        return
    }

    //> lda #$01
    //> sta Enemy_State,x         ;otherwise set bullet bill's state
    ram.enemyState[x] = 1
    //> lda #$0a
    //> sta EnemyFrameTimer,x     ;set enemy frame timer
    ram.timers[0x0a + x] = 0x0a
    //> lda #Sfx_Blast
    //> sta Square2SoundQueue     ;play fireworks/gunfire sound
    ram.square2SoundQueue = Constants.Sfx_Blast

    //> ChkDSte → BBFly → RunBBSubs
    moveEnemyHorizontally()
    runBBSubs()
}

private fun System.runBBSubs() {
    //> RunBBSubs:
    //> jsr GetEnemyOffscreenBits ;get offscreen information
    getEnemyOffscreenBits()
    //> jsr RelativeEnemyPosition ;get relative coordinates
    relativeEnemyPosition()
    //> jsr GetEnemyBoundBox      ;get bounding box coordinates
    getEnemyBoundBox()
    //> jsr PlayerEnemyCollision  ;handle player to enemy collisions
    playerEnemyCollision()
    //> jmp EnemyGfxHandler       ;draw the bullet bill and leave
    enemyGfxHandler()
}

/**
 * Checks if an enemy object has gone too far offscreen and erases it if so.
 * Certain enemy types (flying cheep-cheep, piranha plant, flagpole flag,
 * star flag, jumpspring) are exempt from erasure.
 */
fun System.offscreenBoundsCheck() {
    val x = ram.objectOffset.toInt()

    //> OffscreenBoundsCheck:
    //> lda Enemy_ID,x          ;check for cheep-cheep object
    //> cmp #FlyingCheepCheep   ;branch to leave if found
    //> beq ExScrnBd
    val enemyId = ram.enemyID[x]
    if (enemyId == EnemyId.FlyingCheepCheep.byte) return

    //> lda ScreenLeft_X_Pos    ;get horizontal coordinate for left side of screen
    var leftBound = ram.screenLeftXPos.toInt() and 0xFF
    //> ldy Enemy_ID,x
    //> cpy #HammerBro          ;check for hammer bro object
    //> beq LimitB
    //> cpy #PiranhaPlant       ;check for piranha plant object
    //> bne ExtendLB
    var carry: Int
    if (enemyId == EnemyId.HammerBro.byte || enemyId == EnemyId.PiranhaPlant.byte) {
        //> LimitB: adc #$38    ;add 56 pixels if hammer bro or piranha plant
        // Carry is SET from cpy (enemyId == HammerBro or PiranhaPlant, both >= compared value)
        val adcResult = leftBound + 0x38 + 1  // +1 for carry set from cpy
        leftBound = adcResult and 0xFF
        carry = if (adcResult > 0xFF) 1 else 0
    } else {
        // For non-matching enemies: carry from last cpy #PiranhaPlant
        // enemyId < PiranhaPlant → carry CLEAR (bne branches, so we reach ExtendLB)
        carry = if ((enemyId.toInt() and 0xFF) >= (EnemyId.PiranhaPlant.byte.toInt() and 0xFF)) 1 else 0
    }
    //> ExtendLB: sbc #$48      ;subtract 72 pixels regardless
    // SBC with carry: result = A - operand - (1 - carry)
    val leftBoundResult = leftBound - 0x48 - (1 - carry)
    val leftX = leftBoundResult and 0xFF
    val leftBoundCarry = if (leftBoundResult >= 0) 1 else 0

    //> sta $01                 ;store result here
    //> lda ScreenLeft_PageLoc
    //> sbc #$00                ;subtract borrow from page location of left side
    //> sta $00
    val leftPageResult = (ram.screenLeftPageLoc.toInt() and 0xFF) - (1 - leftBoundCarry)
    val leftPage = leftPageResult and 0xFF
    // Carry from this sbc flows into the right-bound adc
    val leftPageCarry = if (leftPageResult >= 0) 1 else 0

    //> lda ScreenRight_X_Pos   ;add 72 pixels to the right side horizontal coordinate
    //> adc #$48                ;carry from left-page sbc chains through
    val rightBoundResult = (ram.screenRightXPos.toInt() and 0xFF) + 0x48 + leftPageCarry
    val rightX = rightBoundResult and 0xFF
    val rightCarry = if (rightBoundResult > 0xFF) 1 else 0
    //> sta $03
    //> lda ScreenRight_PageLoc
    //> adc #$00                ;then add the carry to the page location
    //> sta $02
    val rightPage = (ram.screenRightPageLoc.toInt() and 0xFF) + rightCarry

    //> lda Enemy_X_Position,x  ;compare enemy horizontal coordinate to left edge
    //> cmp $01
    val enemyX = ram.sprObjXPos[1 + x].toInt() and 0xFF
    val enemyPage = ram.sprObjPageLoc[1 + x].toInt() and 0xFF
    val leftCmp = enemyX - leftX
    val leftCmpBorrow = if (leftCmp < 0) 1 else 0
    //> lda Enemy_PageLoc,x
    //> sbc $00
    val leftPageDiff = (enemyPage - leftPage - leftCmpBorrow).toByte()
    //> bmi TooFar              ;if enemy object is too far left, branch to erase it
    if (leftPageDiff < 0) {
        eraseEnemyObject(x)
        return
    }

    //> lda Enemy_X_Position,x  ;compare enemy horizontal coordinate to right edge
    //> cmp $03
    val rightCmp = enemyX - rightX
    val rightCmpBorrow = if (rightCmp < 0) 1 else 0
    //> lda Enemy_PageLoc,x
    //> sbc $02
    val rightPageDiff = (enemyPage - rightPage - rightCmpBorrow).toByte()
    //> bmi ExScrnBd            ;if enemy is on the screen, leave
    if (rightPageDiff < 0) return

    // Enemy is offscreen to the right - check exemptions
    //> lda Enemy_State,x       ;if in state used by spiny's egg, do not erase
    //> cmp #HammerBro
    if (ram.enemyState[x] == EnemyId.HammerBro.byte) return
    //> cpy #PiranhaPlant       ;if piranha plant, do not erase
    if (enemyId == EnemyId.PiranhaPlant.byte) return
    //> cpy #FlagpoleFlagObject ;if flagpole flag, do not erase
    if (enemyId == EnemyId.FlagpoleFlagObject.byte) return
    //> cpy #StarFlagObject     ;if star flag, do not erase
    if (enemyId == EnemyId.StarFlagObject.byte) return
    //> cpy #JumpspringObject   ;if jumpspring, do not erase
    if (enemyId == EnemyId.JumpspringObject.byte) return

    //> TooFar: jsr EraseEnemyObject ;erase all others too far to the right
    eraseEnemyObject(x)
    //> ExScrnBd: rts
}

// playerEnemyDiff() moved to boundingBox.kt
// getEnemyBoundBox() moved to boundingBox.kt
// playerEnemyCollision() moved to collisionDetection.kt
// enemyGfxHandler() moved to enemyGfxHandler.kt

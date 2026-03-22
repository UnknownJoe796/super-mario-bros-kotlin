// by Claude - RunEnemyObjectsCore dispatcher and enemy movement handler implementations.
// Translates the runtime enemy behavior dispatching and per-enemy-type movement
// handlers from smbdism.asm. The top-level run pipelines (RunNormalEnemies,
// RunBowserFlame, etc.) are defined in enemyBehaviors.kt, platformRoutines.kt,
// and bowserRoutine.kt. This file provides:
//   - The RunEnemyObjectsCore JumpEngine dispatcher
//   - Movement handlers that replace TODO stubs in enemyBehaviors.kt:
//     ProcHammerBro, MoveBloober, MoveBulletBill, MoveSwimmingCheepCheep,
//     MovePodoboo, MovePiranhaPlant, ProcMoveRedPTroopa, MoveFlyGreenPTroopa,
//     MoveLakitu, MoveFlyingCheepCheep, ProcFirebar
package com.ivieleague.smbtranslation

// ---- Data tables ----

//> HammerThrowTmrData:
//> .db $30, $1c
private val hammerThrowTmrData = intArrayOf(0x30, 0x1c)

//> HammerBroJumpLData:
//> .db $20, $37
private val hammerBroJumpLData = intArrayOf(0x20, 0x37)

//> BlooberBitmasks:
//> .db %00111111, %00000011
private val blooberBitmasks = intArrayOf(0x3F, 0x03)

//> SwimCCXMoveData:
//> .db $40, $80
private val swimCCXMoveData = intArrayOf(0x40, 0x80)

//> PRandomSubtracter:
//> .db $f8, $a0, $70, $bd, $00
// by Claude - extended to 16 entries; indices 5-9 overflow into FlyCCBPriority,
// indices 10-15 overflow into MoveFlyingCheepCheep code bytes (matches ROM read behavior)
private val pRandomSubtracter = intArrayOf(
    0xf8, 0xa0, 0x70, 0xbd, 0x00,                   // original table data
    0x20, 0x20, 0x20, 0x00, 0x00,                   // overflow: FlyCCBPriority data
    0xb5, 0x1e, 0x29, 0x20, 0xf0, 0x08              // overflow: MoveFlyingCheepCheep code bytes
)

//> FlyCCBPriority:
//> .db $20, $20, $20, $00, $00
// by Claude - extended to 16 entries; indices 5-15 overflow into MoveFlyingCheepCheep code bytes (matches ROM read behavior)
private val flyCCBPriority = intArrayOf(
    0x20, 0x20, 0x20, 0x00, 0x00,                   // original table data
    0xb5, 0x1e, 0x29, 0x20, 0xf0, 0x08,             // overflow: MoveFlyingCheepCheep code bytes
    0xa9, 0x00, 0x9d, 0xc5, 0x03                     // overflow: more MoveFlyingCheepCheep code bytes
)

//> FlameYPosData:
//> .db $90, $80, $70, $90
private val flameYPosData = intArrayOf(0x90, 0x80, 0x70, 0x90)

//> LakituDiffAdj:
//> .db $15, $30, $40
private val lakituDiffAdj = intArrayOf(0x15, 0x30, 0x40)

// by Claude - runEnemyObjectsCore() dispatch is in enemiesAndLoopsCore.kt
// This file contains the movement handler implementations.

/**
 * Processes Hammer Brother behavior: jumping, hammer throwing, movement.
 * Hammer bros jump between platforms and throw hammers at the player.
 */
fun System.procHammerBro() {
    val x = ram.objectOffset.toInt()
    val state = ram.enemyState[x].toInt() and 0xFF

    //> ProcHammerBro:
    //> lda Enemy_State,x    ;check enemy state for d5 set
    //> and #%00100000
    //> beq ChkHBTime        ;if not set, branch to jump/throw code
    if ((state and 0x20) != 0) {
        //> jsr MoveDefeatedEnemy
        moveD_EnemyVertically()
        moveEnemyHorizontally()
        return
    }

    //> ChkHBTime:
    //> lda HammerBroJumpTimer  ;check jump timer
    //> beq HammerBroJumpCode   ;if expired, branch to jump code
    val jumpTimer = ram.hammerBroJumpTimers[x].toInt() and 0xFF // by Claude - indexed with x
    if (jumpTimer != 0) {
        //> dec HammerBroJumpTimer,x  ;decrement jump timer
        ram.hammerBroJumpTimers[x] = (jumpTimer - 1).toByte() // by Claude - indexed with x

        //> lda Enemy_OffscreenBits ;check offscreen bits
        //> and #%00001100
        //> bne MoveHammerBroXDir   ;skip hammer logic if partially offscreen
        val offBits = ram.enemyOffscreenBits.toInt() and 0x0C
        if (offBits != 0) {
            moveHammerBroXDir(x)
            return
        }

        //> lda HammerThrowingTimer ;check throw timer
        //> bne DecHammerTimer      ;if not expired, decrement
        val throwTimer = ram.hammerThrowingTimers[x].toInt() and 0xFF // by Claude - indexed with x
        if (throwTimer == 0) {
            //> ldy SecondaryHardMode
            //> lda HammerThrowTmrData,y  ;reset throw timer
            //> sta HammerThrowingTimer
            val hardIdx = if (ram.secondaryHardMode != 0.toByte()) 1 else 0
            ram.hammerThrowingTimers[x] = hammerThrowTmrData[hardIdx].toByte() // by Claude - indexed with x

            //> jsr SpawnHammerObj
            //> bcc SetHBThr
            val spawned = spawnHammerObj()
            if (spawned) {
                //> SetHBThr: lda Enemy_State,x; ora #%00001000; sta Enemy_State,x
                ram.enemyState[x] = (state or 0x08).toByte()
            }
            moveHammerBroXDir(x)
            return
        }

        //> DecHammerTimer: dec HammerThrowingTimer,x
        ram.hammerThrowingTimers[x] = (throwTimer - 1).toByte() // by Claude - indexed with x
        moveHammerBroXDir(x)
        return
    }

    //> HammerBroJumpCode:
    //> lda Enemy_State,x; and #%00000111; cmp #$01; beq MoveHammerBroXDir
    val lowState = state and 0x07
    if (lowState == 1) {
        moveHammerBroXDir(x)
        return
    }

    //> Calculate jump speed and preset value based on Y position
    var preset = 0
    var jumpSpd = 0xFA  // strong upward
    val yPos = ram.sprObjYPos[1 + x].toInt().toByte().toInt()  // signed
    if (yPos >= 0) {
        jumpSpd = 0xFD  // weak upward
        preset = 1
        if (yPos >= 0x70) {
            // below threshold - random choice
            preset = 0
            val lsfr = ram.pseudoRandomBitReg[(1 + x).coerceIn(0, ram.pseudoRandomBitReg.size - 1)].toInt() and 0x01
            if (lsfr == 0) {
                jumpSpd = 0xFA  // strong upward
            }
        }
    }

    //> sta Enemy_Y_Speed,x  ;set jump speed
    ram.sprObjYSpeed[1 + x] = jumpSpd.toByte()
    //> lda Enemy_State,x; ora #$01; sta Enemy_State,x
    ram.enemyState[x] = (state or 0x01).toByte()

    //> Set frame timer based on random data
    val masked = preset and (ram.pseudoRandomBitReg[(2 + x).coerceIn(0, ram.pseudoRandomBitReg.size - 1)].toInt() and 0xFF)
    val jmpOfs = if (ram.secondaryHardMode != 0.toByte()) masked else 0
    ram.timers[0x0a + x] = hammerBroJumpLData[jmpOfs.coerceIn(0, 1)].toByte()

    //> Random jump timer
    val lsfr1 = ram.pseudoRandomBitReg[(1 + x).coerceIn(0, ram.pseudoRandomBitReg.size - 1)].toInt() and 0xFF
    ram.hammerBroJumpTimers[x] = (lsfr1 or 0xC0).toByte() // by Claude - indexed with x

    moveHammerBroXDir(x)
}

/**
 * MoveHammerBroXDir: moves hammer bro horizontally with shimmying motion,
 * facing toward the player.
 */
private fun System.moveHammerBroXDir(x: Int) {
    //> MoveHammerBroXDir:
    //> ldy #$fc                  ;default shimmy left
    //> lda FrameCounter
    //> and #%01000000            ;if bit 6 set, branch (keep $FC = left)
    //> bne Shimmy
    //> ldy #$04                  ;if bit 6 clear, shimmy right
    // by Claude - fixed inverted condition: bne branches OVER the ldy #$04 when bit 6 is set
    var shimmy = 0xFC
    if ((ram.frameCounter.toInt() and 0x40) == 0) shimmy = 0x04
    //> HBWalk: sty Enemy_X_Speed,x
    ram.sprObjXSpeed[1 + x] = shimmy.toByte()

    //> ldy #$01; jsr PlayerEnemyDiff; bmi SetHBDir
    var dir = 1
    val (_, highDiff) = playerEnemyDiff()
    if ((highDiff and 0x80) == 0) {
        dir = 2
        //> lda EnemyIntervalTimer,x; beq HBSetSpd
        val intTimer = ram.timers[0x16 + x].toInt() and 0xFF
        if (intTimer == 0) {
            //> lda #$f8; sta Enemy_X_Speed,x
            ram.sprObjXSpeed[1 + x] = 0xf8.toByte()
        }
    }
    //> SetHBDir: sty Enemy_MovingDir,x
    ram.enemyMovingDirs[x] = dir.toByte()
    //> jmp MoveNormalEnemy
    moveNormalEnemy()
}

/**
 * Moves Podoboo (lava fireball) with timer-based re-initialization and gravity.
 * When timer expires, resets position to below screen and launches upward.
 */
fun System.movePodoboo() {
    val x = ram.objectOffset.toInt()

    //> MovePodoboo:
    //> lda EnemyIntervalTimer,x  ;check interval timer
    //> bne PdbM                  ;if not expired, just do movement
    val timer = ram.timers[0x16 + x].toInt() and 0xFF
    if (timer == 0) {
        //> Reinit podoboo at bottom of screen
        //> lda #$02; sta Enemy_Y_HighPos,x; sta Enemy_Y_Position,x
        ram.sprObjYHighPos[1 + x] = 2
        ram.sprObjYPos[1 + x] = 2
        //> lda #$00; sta Enemy_State,x
        ram.enemyState[x] = 0
        //> lda #$09; sta EnemyBoundBoxCtrl,x
        ram.enemyBoundBoxCtrls[x] = 0x09

        //> Set random Y move force and interval timer
        val lsfr = ram.pseudoRandomBitReg[(1 + x).coerceIn(0, ram.pseudoRandomBitReg.size - 1)].toInt() and 0xFF
        //> ora #%10000000; sta SprObject_Y_MoveForce,x
        ram.sprObjYMoveForce[1 + x] = (lsfr or 0x80).toByte()
        //> and #%00001111; ora #$06; sta EnemyIntervalTimer,x
        val intervals = (lsfr and 0x0F) or 0x06
        ram.timers[0x16 + x] = intervals.toByte()
        //> lda #$f9; sta SprObject_Y_Speed,x
        ram.sprObjYSpeed[1 + x] = 0xf9.toByte()
    }

    //> PdbM: jsr MoveJ_EnemyVertically
    moveJ_EnemyVertically()
}

/**
 * MoveJ_EnemyVertically: moves enemy downward with gravity.
 * Uses downForce=$1c, maxSpeed=$03.
 */
private fun System.moveJ_EnemyVertically() {
    //> MoveJ_EnemyVertically: ldy #$1c
    //> SetHiMax: lda #$03
    //> SetXMoveAmt: sty $00; inx; jsr ImposeGravitySprObj; ldx ObjectOffset
    val x = ram.objectOffset.toInt()
    imposeGravitySprObj(sprObjOffset = x + 1, downForce = 0x1C, maxSpeed = 0x03)
}

/**
 * MoveEnemySlowVert: moves enemy downward slowly (for defeated bloobers/cheep-cheeps).
 * Uses downForce=$0f, maxSpeed=$02.
 */
private fun System.moveEnemySlowVert() {
    //> MoveEnemySlowVert: ldy #$0f
    //> SetMdMax: lda #$02
    val x = ram.objectOffset.toInt()
    imposeGravitySprObj(sprObjOffset = x + 1, downForce = 0x0F, maxSpeed = 0x02)
}

/**
 * Moves Bloober (squid) enemy with swimming pattern.
 * Alternates between fast upward swim strokes and floating downward.
 */
fun System.moveBloober() {
    val x = ram.objectOffset.toInt()
    val state = ram.enemyState[x].toInt() and 0xFF

    //> MoveBloober:
    //> lda Enemy_State,x; and #%00100000; bne MoveDefeatedBloober
    if ((state and 0x20) != 0) {
        //> MoveDefeatedBloober: jmp MoveEnemySlowVert
        moveEnemySlowVert()
        return
    }

    //> ldy SecondaryHardMode; lda PseudoRandomBitReg+1,x; and BlooberBitmasks,y
    //> bne BlooberSwim
    val hardIdx = if (ram.secondaryHardMode != 0.toByte()) 1 else 0
    val lsfr = ram.pseudoRandomBitReg[(1 + x).coerceIn(0, ram.pseudoRandomBitReg.size - 1)].toInt() and 0xFF
    if ((lsfr and blooberBitmasks[hardIdx]) == 0) {
        //> Set moving direction toward player
        //> txa; lsr; bcs ChkRev (odd slot uses player dir)
        if ((x and 1) != 0) {
            ram.enemyMovingDirs[x] = ram.playerMovingDir
        } else {
            //> ChkRev: jsr PlayerEnemyDiff; bpl LMovBloworker
            var dir = 2
            val (_, highDiff) = playerEnemyDiff()
            if ((highDiff and 0x80) != 0) dir = 1
            ram.enemyMovingDirs[x] = dir.toByte()
        }
    }

    //> BlooberSwim:
    procSwimmingB(x)

    //> Vertical position update
    val yPos = ram.sprObjYPos[1 + x].toInt() and 0xFF
    val yForce = ram.sprObjYMoveForce[1 + x].toInt() and 0xFF
    val newY = yPos - yForce
    if ((newY and 0xFF) >= 0x20) {
        ram.sprObjYPos[1 + x] = (newY and 0xFF).toByte()
    }

    //> SwimX: horizontal movement based on direction
    val moveDir = ram.enemyMovingDirs[x].toInt() and 0xFF
    // by Claude - BlooperMoveSpeed,x is indexed (same RAM as Enemy_X_Speed at $58)
    val moveSpd = ram.sprObjXSpeed[1 + x].toInt() and 0xFF
    if (moveDir == 1) {
        // moving right
        val newX = (ram.sprObjXPos[1 + x].toInt() and 0xFF) + moveSpd
        ram.sprObjXPos[1 + x] = (newX and 0xFF).toByte()
        ram.sprObjPageLoc[1 + x] = ((ram.sprObjPageLoc[1 + x].toInt() and 0xFF) + (if (newX > 0xFF) 1 else 0)).toByte()
    } else {
        // moving left
        val newX = (ram.sprObjXPos[1 + x].toInt() and 0xFF) - moveSpd
        ram.sprObjXPos[1 + x] = (newX and 0xFF).toByte()
        ram.sprObjPageLoc[1 + x] = ((ram.sprObjPageLoc[1 + x].toInt() and 0xFF) - (if (newX < 0) 1 else 0)).toByte()
    }
}

/**
 * ProcSwimmingB: processes bloober swimming animation state machine.
 * Cycles through: fast swim up -> slow swim up -> float down.
 */
private fun System.procSwimmingB(x: Int) {
    //> ProcSwimmingB:
    // by Claude - BlooperMoveCounter,x is indexed (same RAM as Enemy_Y_Speed at $a0)
    val moveCounter = ram.sprObjYSpeed[1 + x].toInt() and 0xFF

    //> lda BlooperMoveCounter,x; and #%00000010; bne ChkForFloatdown
    if ((moveCounter and 0x02) != 0) {
        //> ChkForFloatdown: floating down phase
        val intTimer = ram.timers[0x16 + x].toInt() and 0xFF
        if (intTimer != 0) {
            //> Floatdown: slow descent while timer active
            if ((ram.frameCounter.toInt() and 0x01) == 0) {
                ram.sprObjYPos[1 + x] = ((ram.sprObjYPos[1 + x].toInt() and 0xFF) + 1).toByte()
            }
            return
        }
        //> ChkNearPlayer: check if above player
        val bloobY = (ram.sprObjYPos[1 + x].toInt() and 0xFF) + 0x10
        val playerY = ram.sprObjYPos[0].toInt() and 0xFF
        if ((bloobY and 0xFF) < playerY) {
            if ((ram.frameCounter.toInt() and 0x01) == 0) {
                ram.sprObjYPos[1 + x] = ((ram.sprObjYPos[1 + x].toInt() and 0xFF) + 1).toByte()
            }
        } else {
            //> Reset to fast swim phase
            ram.sprObjYSpeed[1 + x] = 0  // by Claude - BlooperMoveCounter,x = $a0+x
        }
        return
    }

    //> Swimming upward phases
    val fc3 = ram.frameCounter.toInt() and 0x07
    if ((moveCounter and 0x01) != 0) {
        //> SlowSwim: slow upward phase
        if (fc3 != 0) return
        val force = (ram.sprObjYMoveForce[1 + x].toInt() and 0xFF) - 1
        ram.sprObjYMoveForce[1 + x] = (force and 0xFF).toByte()
        ram.sprObjXSpeed[1 + x] = (force and 0xFF).toByte()  // by Claude - BlooperMoveSpeed,x = $58+x
        if ((force and 0xFF) == 0) {
            //> Transition to float-down phase
            ram.sprObjYSpeed[1 + x] = ((moveCounter + 1) and 0xFF).toByte()  // by Claude - BlooperMoveCounter,x
            ram.timers[0x16 + x] = 0x02
        }
    } else {
        //> Fast swim: fast upward phase
        if (fc3 != 0) return
        val force = (ram.sprObjYMoveForce[1 + x].toInt() and 0xFF) + 1
        ram.sprObjYMoveForce[1 + x] = (force and 0xFF).toByte()
        ram.sprObjXSpeed[1 + x] = (force and 0xFF).toByte()  // by Claude - BlooperMoveSpeed,x = $58+x
        if ((force and 0xFF) == 0x02) {
            //> Transition to slow swim phase
            ram.sprObjYSpeed[1 + x] = ((moveCounter + 1) and 0xFF).toByte()  // by Claude - BlooperMoveCounter,x
        }
    }
}

/**
 * Moves Bullet Bill enemy. If defeated (d5 set), falls with gravity.
 * Otherwise moves horizontally leftward at fixed speed.
 */
fun System.moveBulletBill() {
    val x = ram.objectOffset.toInt()

    //> MoveBulletBill:
    //> lda Enemy_State,x; and #%00100000; beq NotDefB
    if ((ram.enemyState[x].toInt() and 0x20) != 0) {
        //> jsr MoveJ_EnemyVertically; rts
        moveJ_EnemyVertically()
        return
    }

    //> NotDefB: lda #$e8; sta Enemy_X_Speed,x; jmp MoveEnemyHorizontally
    ram.sprObjXSpeed[1 + x] = 0xe8.toByte()
    moveEnemyHorizontally()
}

/**
 * Moves swimming Cheep Cheep (grey/red variants).
 * Grey cheep cheeps move slower horizontally than red ones.
 * Vertical movement oscillates based on original Y position.
 */
fun System.moveSwimmingCheepCheep() {
    val x = ram.objectOffset.toInt()
    val state = ram.enemyState[x].toInt() and 0xFF

    //> MoveSwimmingCheepCheep:
    //> lda Enemy_State,x; and #%00100000; bne MoveDefeatedCCheep
    if ((state and 0x20) != 0) {
        //> MoveDefeatedCCheep: jmp MoveEnemySlowVert
        moveEnemySlowVert()
        return
    }

    //> CCSwim: horizontal movement
    val enemyId = ram.enemyID[x].toInt() and 0xFF
    val ccIdx = (enemyId - 0x0A).coerceIn(0, swimCCXMoveData.size - 1)
    val subVal = swimCCXMoveData[ccIdx]

    //> Slow fractional horizontal movement (leftward)
    val force = (ram.sprObjXMoveForce[1 + x].toInt() and 0xFF) - subVal
    ram.sprObjXMoveForce[1 + x] = (force and 0xFF).toByte()
    val borrow1 = if (force < 0) 1 else 0
    val posX = (ram.sprObjXPos[1 + x].toInt() and 0xFF) - borrow1
    ram.sprObjXPos[1 + x] = (posX and 0xFF).toByte()
    val borrow2 = if (posX < 0) 1 else 0
    ram.sprObjPageLoc[1 + x] = ((ram.sprObjPageLoc[1 + x].toInt() and 0xFF) - borrow2).toByte()

    //> Vertical movement (only for slots >= 2)
    //> cpx #$02; bcc ExSwCC
    if (x < 2) return

    // by Claude - CheepCheepMoveMFlag aliases Enemy_X_Speed ($58+x) = sprObjXSpeed[1+x]
    val moveFlag = ram.sprObjXSpeed[1 + x].toInt() and 0xFF
    if (moveFlag < 0x10) {
        //> CCSwimUpwards: fractional upward movement
        val dummy = (ram.sprObjYMFDummy[1 + x].toInt() and 0xFF) - 0x20
        ram.sprObjYMFDummy[1 + x] = (dummy and 0xFF).toByte()
        val borrow = if (dummy < 0) 1 else 0
        val yPos = (ram.sprObjYPos[1 + x].toInt() and 0xFF) - borrow
        ram.sprObjYPos[1 + x] = (yPos and 0xFF).toByte()
        ram.sprObjYHighPos[1 + x] = ((ram.sprObjYHighPos[1 + x].toInt() and 0xFF) - (if (yPos < 0) 1 else 0)).toByte()
    } else {
        //> CCSwimDownwards: fractional downward movement
        val dummy = (ram.sprObjYMFDummy[1 + x].toInt() and 0xFF) + 0x20
        ram.sprObjYMFDummy[1 + x] = (dummy and 0xFF).toByte()
        val carry = if (dummy > 0xFF) 1 else 0
        val yPos = (ram.sprObjYPos[1 + x].toInt() and 0xFF) + carry
        ram.sprObjYPos[1 + x] = (yPos and 0xFF).toByte()
        ram.sprObjYHighPos[1 + x] = ((ram.sprObjYHighPos[1 + x].toInt() and 0xFF) + (if (yPos > 0xFF) 1 else 0)).toByte()
    }

    //> ChkSwimYPos: check if direction needs to reverse
    // by Claude - CheepCheepOrigYPos aliases Enemy_Y_MoveForce ($0434+x) = sprObjYMoveForce[1+x]
    val curY = ram.sprObjYPos[1 + x].toInt() and 0xFF
    val origY = ram.sprObjYMoveForce[1 + x].toInt() and 0xFF
    // by Claude - Direction flag is based on SIGN of (curY - origY), not on current moveFlag.
    // Assembly: BPL branches if result is positive (bit 7 clear).
    val rawDiff = (curY - origY) and 0xFF
    val isNegative = (rawDiff and 0x80) != 0
    val newFlag = if (isNegative) 0x10 else 0x00
    val absDiff = if (isNegative) ((rawDiff xor 0xFF) + 1) and 0xFF else rawDiff
    if (absDiff >= 0x0F) {
        ram.sprObjXSpeed[1 + x] = newFlag.toByte()
    }
}

/**
 * Moves Piranha Plant in and out of pipes.
 * Checks player proximity - won't emerge if player is too close.
 */
fun System.movePiranhaPlant() {
    val x = ram.objectOffset.toInt()

    //> MovePiranhaPlant:
    //> lda Enemy_State,x; bne PutinPipe
    if (ram.enemyState[x] != 0.toByte()) {
        //> PutinPipe: set background priority
        ram.sprAttrib[1 + x] = 0x20 // by Claude - indexed by x
        return
    }
    //> lda EnemyFrameTimer,x; bne PutinPipe
    if (ram.timers[0x0a + x] != 0.toByte()) {
        ram.sprAttrib[1 + x] = 0x20 // by Claude - indexed by x
        return
    }

    //> lda PiranhaPlant_MoveFlag,x; bne SetupToMovePPlant
    val moveFlag = ram.sprObjYSpeed[1 + x].toInt() and 0xFF  // PiranhaPlant_MoveFlag,x ($A0+x)
    if (moveFlag != 0) {
        setupToMovePPlant(x)
        return
    }

    //> lda PiranhaPlant_Y_Speed,x; bmi ReversePlantSpeed (if moving upward, reverse)
    val ySpeed = ram.sprObjXSpeed[1 + x].toInt().toByte().toInt()  // PiranhaPlant_Y_Speed,x ($58+x)
    if (ySpeed < 0) {
        reversePlantSpeed(x)
        return
    }

    //> Check player distance - don't emerge if too close
    //> jsr PlayerEnemyDiff
    val (lowDiff, highDiff) = playerEnemyDiff()
    var absDiff = lowDiff
    if ((highDiff and 0x80) != 0) {
        absDiff = ((lowDiff xor 0xFF) + 1) and 0xFF
    }
    //> cmp #$21; bcc PutinPipe
    if (absDiff < 0x21) {
        ram.sprAttrib[1 + x] = 0x20 // by Claude - indexed by x
        return
    }

    //> jmp ReversePlantSpeed
    reversePlantSpeed(x)
}

/**
 * ReversePlantSpeed: negates piranha plant Y speed and increments move flag.
 */
private fun System.reversePlantSpeed(x: Int) {
    //> ReversePlantSpeed:
    val speed = ram.sprObjXSpeed[1 + x].toInt() and 0xFF  // PiranhaPlant_Y_Speed,x ($58+x)
    //> Two's complement to reverse direction
    ram.sprObjXSpeed[1 + x] = (((speed xor 0xFF) + 1) and 0xFF).toByte()
    //> inc PiranhaPlant_MoveFlag,x
    ram.sprObjYSpeed[1 + x] = ((ram.sprObjYSpeed[1 + x].toInt() and 0xFF) + 1).toByte()  // ($A0+x)
    setupToMovePPlant(x)
}

/**
 * SetupToMovePPlant: executes piranha plant vertical movement every other frame.
 * When target position reached, resets move flag and sets frame timer.
 */
private fun System.setupToMovePPlant(x: Int) {
    //> SetupToMovePPlant:
    val ySpeed = ram.sprObjXSpeed[1 + x].toInt().toByte().toInt()  // PiranhaPlant_Y_Speed,x ($58+x)

    //> Determine target Y based on direction
    val targetY = if (ySpeed >= 0) {
        ram.sprObjYMoveForce[1 + x].toInt() and 0xFF  // PiranhaPlantDownYPos,x ($434+x)
    } else {
        ram.sprObjYMFDummy[1 + x].toInt() and 0xFF  // PiranhaPlantUpYPos,x ($417+x)
    }

    //> RiseFallPiranhaPlant:
    //> lda FrameCounter; and #%00000001; beq PutinPipe
    if ((ram.frameCounter.toInt() and 0x01) == 0) {
        ram.sprAttrib[1 + x] = 0x20 // by Claude - indexed by x
        return
    }
    //> lda TimerControl; bne PutinPipe
    if (ram.timerControl != 0.toByte()) {
        ram.sprAttrib[1 + x] = 0x20 // by Claude - indexed by x
        return
    }

    //> Move toward target
    val curY = ram.sprObjYPos[1 + x].toInt() and 0xFF
    val newY = (curY + ySpeed) and 0xFF
    ram.sprObjYPos[1 + x] = newY.toByte()

    //> Check if reached target
    if (newY == targetY) {
        ram.sprObjYSpeed[1 + x] = 0  // PiranhaPlant_MoveFlag,x ($A0+x)
        //> lda #$40; sta EnemyFrameTimer,x
        ram.timers[0x0a + x] = 0x40
    }

    //> PutinPipe: set background priority
    ram.sprAttrib[1 + x] = 0x20 // by Claude - indexed by x
}

/**
 * Processes Red Paratroopa movement (vertical back-and-forth on platform).
 * Moves between original Y position and center Y position.
 */
fun System.procMoveRedPTroopa() {
    val x = ram.objectOffset.toInt()

    //> ProcMoveRedPTroopa:
    //> lda Enemy_Y_Speed,x; ora Enemy_Y_MoveForce,x; bne MoveRedPTUpOrDown
    val ySpd = ram.sprObjYSpeed[1 + x].toInt() and 0xFF
    val yForce = ram.sprObjYMoveForce[1 + x].toInt() and 0xFF
    if (ySpd == 0 && yForce == 0) {
        //> sta Enemy_YMF_Dummy,x
        ram.sprObjYMFDummy[1 + x] = 0

        //> lda Enemy_Y_Position,x; cmp RedPTroopa_OrigXPos
        val curY = ram.sprObjYPos[1 + x].toInt() and 0xFF
        // by Claude - indexed by objectOffset (aliases Enemy_X_MoveForce,x at $0401+x)
        val origY = ram.sprObjXMoveForce[1 + x].toInt() and 0xFF
        if (curY < origY) {
            //> Slowly drift down to original position
            if ((ram.frameCounter.toInt() and 0x07) == 0) {
                ram.sprObjYPos[1 + x] = (curY + 1).toByte()
            }
            return
        }
    }

    //> MoveRedPTUpOrDown:
    val curY = ram.sprObjYPos[1 + x].toInt() and 0xFF
    // by Claude - indexed by objectOffset (aliases Enemy_X_Speed,x at $58+x)
    val centerY = ram.sprObjXSpeed[1 + x].toInt() and 0xFF
    if (curY < centerY) {
        //> MoveRedPTroopaDown: ldy #$00
        moveRedPTroopa(x, 0)
    } else {
        //> MoveRedPTroopaUp: ldy #$01
        moveRedPTroopa(x, 1)
    }
}

/**
 * MoveRedPTroopa: applies bidirectional gravity for red paratroopa vertical movement.
 * @param direction 0=down, 1=up (bidirectional flag for ImposeGravity)
 */
private fun System.moveRedPTroopa(x: Int, direction: Int) {
    //> MoveRedPTroopa: inx
    val sprOfs = x + 1
    //> lda #$03; sta $00 (downForce)
    //> lda #$06; sta $01 (upForce)
    //> lda #$02; sta $02 (maxSpeed)
    //> tya; jmp RedPTroopaGrav -> ImposeGravity
    imposeGravity(sprOfs, downForce = 0x03, upForce = 0x06, maxSpeed = 0x02, bidirectional = direction != 0)
}

/**
 * Moves flying Green Paratroopa in sine wave pattern.
 * Uses XMoveCntr system for horizontal oscillation and frame-based vertical bob.
 */
fun System.moveFlyGreenPTroopa() {
    val x = ram.objectOffset.toInt()

    //> MoveFlyGreenPTroopa:
    //> jsr XMoveCntr_GreenPTroopa
    xMoveCntr_Platform(x, 0x13)
    //> jsr MoveWithXMCntrs
    moveWithXMCntrs(x)

    //> Vertical bob: every 4th frame, move up or down based on frame counter bit 6
    //> lda FrameCounter; and #%00000011; bne NoMoveGPT
    if ((ram.frameCounter.toInt() and 0x03) != 0) return

    //> lda #$ff (move up by default)
    //> lda FrameCounter; and #%01000000; beq MoveGPTDn
    var adder = 0xFF  // move up
    if ((ram.frameCounter.toInt() and 0x40) != 0) adder = 0x01  // move down

    //> MoveGPTDn: adc Enemy_Y_Position,x; sta Enemy_Y_Position,x
    val yPos = (ram.sprObjYPos[1 + x].toInt() and 0xFF) + adder.toByte().toInt()
    ram.sprObjYPos[1 + x] = (yPos and 0xFF).toByte()
}

/**
 * XMoveCntr_Platform: oscillation counter for platform/flying enemy X movement.
 * Increments or decrements a secondary counter, toggling direction when limits are hit.
 * @param maxVal the maximum value for the secondary counter
 */
// by Claude - fixed: XMoveSecondaryCounter,x = sprObjXSpeed[1+x], XMovePrimaryCounter,x = sprObjYSpeed[1+x]
private fun System.xMoveCntr_Platform(x: Int, maxVal: Int) {
    //> XMoveCntr_Platform:
    //> lda FrameCounter; and #%00000011; bne NoXMoveP
    if ((ram.frameCounter.toInt() and 0x03) != 0) return

    // XMoveSecondaryCounter,x at $58+x = sprObjXSpeed[1+x]
    // XMovePrimaryCounter,x at $a0+x = sprObjYSpeed[1+x]
    val secondary = ram.sprObjXSpeed[1 + x].toInt() and 0xFF
    val primary = ram.sprObjYSpeed[1 + x].toInt() and 0xFF

    //> lda XMovePrimaryCounter,x; lsr; bcs DecSeXM
    if ((primary and 0x01) != 0) {
        //> DecSeXM: decrement secondary, or toggle primary if at zero
        if (secondary == 0) {
            ram.sprObjYSpeed[1 + x] = ((primary + 1) and 0xFF).toByte()
        } else {
            ram.sprObjXSpeed[1 + x] = ((secondary - 1) and 0xFF).toByte()
        }
    } else {
        //> Increment secondary, or toggle primary if at max
        if (secondary == maxVal) {
            ram.sprObjYSpeed[1 + x] = ((primary + 1) and 0xFF).toByte()
        } else {
            ram.sprObjXSpeed[1 + x] = ((secondary + 1) and 0xFF).toByte()
        }
    }
}

/**
 * MoveWithXMCntrs: converts oscillation counter to directional horizontal movement.
 * Negates speed for leftward movement based on primary counter bit 1.
 */
// by Claude - fixed: XMoveSecondaryCounter,x = sprObjXSpeed[1+x], XMovePrimaryCounter,x = sprObjYSpeed[1+x]
private fun System.moveWithXMCntrs(x: Int) {
    //> MoveWithXMCntrs:
    // XMoveSecondaryCounter,x at $58+x = sprObjXSpeed[1+x]
    // XMovePrimaryCounter,x at $a0+x = sprObjYSpeed[1+x]
    val savedSecondary = ram.sprObjXSpeed[1 + x]
    val primary = ram.sprObjYSpeed[1 + x].toInt() and 0xFF

    var dir = 1
    //> lda XMovePrimaryCounter,x; and #%00000010; bne XMRight (bit 1 set = full speed right)
    if ((primary and 0x02) == 0) {
        //> Negate for leftward movement
        val secondary = ram.sprObjXSpeed[1 + x].toInt() and 0xFF
        val neg = ((secondary xor 0xFF) + 1) and 0xFF
        ram.sprObjXSpeed[1 + x] = neg.toByte()
        dir = 2
    }
    //> XMRight: sty Enemy_MovingDir,x
    ram.enemyMovingDirs[x] = dir.toByte()
    //> jsr MoveEnemyHorizontally
    moveEnemyHorizontally()
    //> sta $00 (save return value, unused by green paratroopa caller)
    //> Restore secondary counter
    ram.sprObjXSpeed[1 + x] = savedSecondary
}

/**
 * Moves Lakitu enemy, tracking player position.
 * Sets spiny as frenzy buffer when active, adjusts speed based on player distance.
 * by Claude - LakituMoveSpeed ($58) = Enemy_X_Speed, LakituMoveDirection ($A0) = Enemy_Y_Speed;
 * both are indexed by x in assembly (,x addressing), so use sprObjXSpeed[1+x] / sprObjYSpeed[1+x]
 */
fun System.moveLakitu() {
    val x = ram.objectOffset.toInt()
    val state = ram.enemyState[x].toInt() and 0xFF

    //> MoveLakitu:
    //> lda Enemy_State,x; and #%00100000; bne KillLakitu
    if ((state and 0x20) != 0) {
        //> KillLakitu: defeated, fall with gravity
        moveD_EnemyVertically()
        return
    }

    //> lda Enemy_State,x; bne Fr12S (non-zero state = injured, no frenzy)
    if (state != 0) {
        //> Clear movement/frenzy state: sta LakituMoveDirection,x; sta EnemyFrenzyBuffer
        ram.sprObjYSpeed[1 + x] = 0  // LakituMoveDirection,x ($A0+x)
        ram.enemyFrenzyBuffer = 0
        //> lda #$10; bne SetLSpd (unconditional branch, A=$10 stored at SetLSpd)
        ram.sprObjXSpeed[1 + x] = 0x10  // LakituMoveSpeed,x ($58+x)
    } else {
        //> Fr12S: set spiny as frenzy and track player
        ram.enemyFrenzyBuffer = EnemyId.Spiny.byte
        playerLakituDiff(x)
    }

    //> SetLSpd: speed already stored to LakituMoveSpeed,x above
    //> lda LakituMoveDirection,x; and #$01
    val moveDir = ram.sprObjYSpeed[1 + x].toInt() and 0x01  // LakituMoveDirection,x
    var dir = 1
    if (moveDir == 0) {
        //> Negate speed for leftward movement: eor #$ff; clc; adc #$01; sta LakituMoveSpeed,x
        val speed = ram.sprObjXSpeed[1 + x].toInt() and 0xFF  // LakituMoveSpeed,x
        val negSpeed = ((speed xor 0xFF) + 1) and 0xFF
        ram.sprObjXSpeed[1 + x] = negSpeed.toByte()  // LakituMoveSpeed,x
        dir = 2
    }
    //> sty Enemy_MovingDir,x
    ram.enemyMovingDirs[x] = dir.toByte()
    //> jmp MoveEnemyHorizontally
    moveEnemyHorizontally()
}

/**
 * PlayerLakituDiff: calculates player-lakitu distance and adjusts movement speed.
 * Takes player scroll speed into account for smoother tracking.
 * by Claude - fixed: indexed LakituMoveSpeed/Direction, distance guard, ChkEmySpd logic, loop count
 */
internal fun System.playerLakituDiff(x: Int) {
    //> PlayerLakituDiff:
    var dirY = 0
    val (lowDiff, highDiff) = playerEnemyDiff()
    //> bpl ChkLakDif; iny (Y=1 if enemy to left of player)
    if ((highDiff and 0x80) != 0) dirY = 1

    //> Get absolute horizontal distance: eor #$ff; clc; adc #$01; sta $00
    var absDiff = lowDiff
    if ((highDiff and 0x80) != 0) {
        absDiff = ((lowDiff xor 0xFF) + 1) and 0xFF
    }

    //> ChkLakDif: cmp #$3c; bcc ChkPSpeed — direction change only when distance >= $3C
    if (absDiff >= 0x3C) {
        absDiff = 0x3C  //> lda #$3c; sta $00
        //> lda Enemy_ID,x; cmp #Lakitu; bne ChkPSpeed
        if (ram.enemyID[x] == EnemyId.Lakitu.byte) {
            //> tya; cmp LakituMoveDirection,x; beq ChkPSpeed
            if (dirY != (ram.sprObjYSpeed[1 + x].toInt() and 0xFF)) {  // LakituMoveDirection,x
                //> lda LakituMoveDirection,x; beq SetLMovD
                if (ram.sprObjYSpeed[1 + x] != 0.toByte()) {  // LakituMoveDirection,x
                    //> dec LakituMoveSpeed,x
                    val lSpd = (ram.sprObjXSpeed[1 + x].toInt() and 0xFF)  // LakituMoveSpeed,x
                    val newSpd = (lSpd - 1) and 0xFF
                    ram.sprObjXSpeed[1 + x] = newSpd.toByte()  // LakituMoveSpeed,x
                    //> lda LakituMoveSpeed,x; bne ExMoveLak
                    if (newSpd != 0) {
                        return  // ExMoveLak — early return, caller reads updated speed
                    }
                }
                //> SetLMovD: tya; sta LakituMoveDirection,x
                ram.sprObjYSpeed[1 + x] = dirY.toByte()  // LakituMoveDirection,x
            }
        }
    }

    //> ChkPSpeed: calculate speed adjustment based on player scroll speed
    val maskedDiff = (absDiff and 0x3C) ushr 2
    var adjIdx = 0
    val playerXSpd = ram.sprObjXSpeed[0].toInt() and 0xFF
    //> lda Player_X_Speed; beq SubDifAdj; lda ScrollAmount; beq SubDifAdj
    if (playerXSpd != 0 && ram.scrollAmount != 0.toByte()) {
        adjIdx = 1  //> iny
        //> cmp #$19; bcc ChkSpinyO
        if (playerXSpd >= 0x19 && (ram.scrollAmount.toInt() and 0xFF) >= 2) adjIdx = 2  //> iny
    } else {
        //> Player not moving or no scroll: adjIdx stays 0, skip to SubDifAdj
        // (Spiny and ChkEmySpd checks are bypassed in assembly via beq SubDifAdj)
        adjIdx = 0
    }

    //> ChkSpinyO / ChkEmySpd logic (only reached when playerXSpd != 0 && scrollAmount != 0)
    if (adjIdx > 0) {
        val isSpiny = ram.enemyID[x] == EnemyId.Spiny.byte
        if (isSpiny) {
            //> Spiny AND player moving: bne SubDifAdj (playerXSpd guaranteed nonzero here)
            //> Skip ChkEmySpd, use current adjIdx
        } else {
            //> ChkEmySpd: lda Enemy_Y_Speed,x; bne SubDifAdj; ldy #$00
            if (ram.sprObjYSpeed[1 + x] == 0.toByte()) {
                adjIdx = 0  // Y_Speed zero -> reset to 0
            }
            // Y_Speed nonzero -> keep current adjIdx
        }
    }

    //> SubDifAdj: lda $0001,y — load diff adj value
    var result = lakituDiffAdj[adjIdx.coerceIn(0, 2)]
    //> ldy $00; SPixelLak: sec; sbc #$01; dey; bpl SPixelLak
    //> Loop runs maskedDiff+1 times (Y counts down from maskedDiff to -1, bpl exits when Y<0)
    var countdown = maskedDiff
    do {
        result--
        countdown--
    } while (countdown >= 0)
    //> Result in A, returned to caller which stores at SetLSpd: sta LakituMoveSpeed,x
    ram.sprObjXSpeed[1 + x] = result.toByte()  // LakituMoveSpeed,x
}

/**
 * Moves flying Cheep Cheep in arc pattern.
 * Applies gravity and adjusts background priority based on Y position phase.
 */
fun System.moveFlyingCheepCheep() {
    val x = ram.objectOffset.toInt()

    //> MoveFlyingCheepCheep:
    //> lda Enemy_State,x; and #%00100000; beq FlyCC
    if ((ram.enemyState[x].toInt() and 0x20) != 0) {
        //> Defeated: clear sprite attrib and fall
        ram.sprAttrib[1 + x] = 0
        moveJ_EnemyVertically()
        return
    }

    //> FlyCC: horizontal movement + gravity
    moveEnemyHorizontally()
    //> ldy #$0d; lda #$05; jsr SetXMoveAmt
    val xOfs = ram.objectOffset.toInt()
    imposeGravitySprObj(sprObjOffset = xOfs + 1, downForce = 0x0D, maxSpeed = 0x05)

    //> BPGet: background priority based on Y position phase
    val force = ram.sprObjYMoveForce[1 + x].toInt() and 0xFF
    val forceIdx = (force ushr 4) and 0x0F // by Claude - removed coerceIn; table extended to 16 entries
    val yPos = ram.sprObjYPos[1 + x].toInt() and 0xFF
    var yDiff = yPos - pRandomSubtracter[forceIdx]
    if (yDiff < 0) yDiff = -yDiff
    if (yDiff < 8) {
        ram.sprObjYMoveForce[1 + x] = ((force + 0x10) and 0xFF).toByte()
    }
    val bpIdx = (ram.sprObjYMoveForce[1 + x].toInt() and 0xFF) ushr 4 // by Claude - removed coerceIn; table extended to 16 entries
    ram.sprAttrib[1 + x] = flyCCBPriority[bpIdx].toByte()
}

// by Claude - ProcFirebar with trig tables, FirebarSpin, GetFirebarPosition, FirebarCollision

//> FirebarPosLookupTbl:
//> horizontal adder is at first byte + high byte of spinstate,
//> vertical adder is same + 8 bytes, two's compliment if greater than $08 for proper oscillation
private val firebarPosLookupTbl = intArrayOf(
    0x00, 0x01, 0x03, 0x04, 0x05, 0x06, 0x07, 0x07, 0x08,
    0x00, 0x03, 0x06, 0x09, 0x0b, 0x0d, 0x0e, 0x0f, 0x10,
    0x00, 0x04, 0x09, 0x0d, 0x10, 0x13, 0x16, 0x17, 0x18,
    0x00, 0x06, 0x0c, 0x12, 0x16, 0x1a, 0x1d, 0x1f, 0x20,
    0x00, 0x07, 0x0f, 0x16, 0x1c, 0x21, 0x25, 0x27, 0x28,
    0x00, 0x09, 0x12, 0x1b, 0x21, 0x27, 0x2c, 0x2f, 0x30,
    0x00, 0x0b, 0x15, 0x1f, 0x27, 0x2e, 0x33, 0x37, 0x38,
    0x00, 0x0c, 0x18, 0x24, 0x2d, 0x35, 0x3b, 0x3e, 0x40,
    0x00, 0x0e, 0x1b, 0x28, 0x32, 0x3b, 0x42, 0x46, 0x48,
    0x00, 0x0f, 0x1f, 0x2d, 0x38, 0x42, 0x4a, 0x4e, 0x50,
    0x00, 0x11, 0x22, 0x31, 0x3e, 0x49, 0x51, 0x56, 0x58
)

//> FirebarMirrorData:
//> .db $01, $03, $02, $00
private val firebarMirrorData = intArrayOf(0x01, 0x03, 0x02, 0x00)

//> FirebarTblOffsets:
//> .db $00, $09, $12, $1b, $24, $2d, $36, $3f, $48, $51, $5a, $63
private val firebarTblOffsets = intArrayOf(
    0x00, 0x09, 0x12, 0x1b, 0x24, 0x2d,
    0x36, 0x3f, 0x48, 0x51, 0x5a, 0x63
)

//> FirebarYPos:
//> .db $0c, $18
private val firebarYPos = intArrayOf(0x0c, 0x18)

/**
 * Processes firebar rotation, position calculation, rendering, and collision detection.
 * Firebars rotate around a pivot point using a 16-bit spin state. The high byte of the
 * spin state indexes into trigonometric lookup tables to calculate each fireball's position.
 * Short firebars have 5 parts, long firebars have 11.
 */
fun System.procFirebar() {
    val x = ram.objectOffset.toInt()

    //> ProcFirebar:
    //> jsr GetEnemyOffscreenBits   ;get offscreen information
    getEnemyOffscreenBits()
    //> lda Enemy_OffscreenBits     ;check for d3 set
    //> and #%00001000              ;if so, branch to leave
    //> bne SkipFBar
    if ((ram.enemyOffscreenBits.toInt() and 0x08) != 0) return

    //> lda TimerControl            ;if master timer control set, branch
    //> bne SusFbar                 ;ahead of this part
    if (ram.timerControl == 0.toByte()) {
        //> lda FirebarSpinSpeed,x      ;load spinning speed of firebar
        val spinSpeed = ram.firebarSpinSpeed[x].toInt() and 0xFF
        //> jsr FirebarSpin             ;modify current spinstate
        val spinResult = firebarSpin(spinSpeed, x)
        //> and #%00011111              ;mask out all but 5 LSB
        //> sta FirebarSpinState_High,x ;and store as new high byte of spinstate
        ram.sprObjYSpeed[1 + x] = (spinResult and 0x1F).toByte()
    }

    //> SusFbar:  lda FirebarSpinState_High,x ;get high byte of spinstate
    var spinHigh = ram.sprObjYSpeed[1 + x].toInt() and 0xFF
    //> ldy Enemy_ID,x              ;check enemy identifier
    val enemyId = ram.enemyID[x].toInt() and 0xFF
    //> cpy #$1f
    //> bcc SetupGFB                ;if < $1f (long firebar), branch
    if (enemyId >= 0x1f) {
        //> cmp #$08                    ;check high byte of spinstate
        //> beq SkpFSte                 ;if eight, branch to change
        //> cmp #$18                    ;if not at twenty-four branch to not change
        //> bne SetupGFB
        if (spinHigh == 0x08 || spinHigh == 0x18) {
            //> SkpFSte:  clc
            //> adc #$01                    ;add one to spinning thing to avoid horizontal state
            spinHigh = (spinHigh + 1) and 0xFF
            //> sta FirebarSpinState_High,x
            ram.sprObjYSpeed[1 + x] = spinHigh.toByte()
        }
    }

    //> SetupGFB: sta $ef                     ;save high byte of spinning thing
    val savedSpinHigh = spinHigh

    //> jsr RelativeEnemyPosition   ;get relative coordinates to screen
    relativeEnemyPosition()

    //> jsr GetFirebarPosition      ;do a sub here (residual, too early to be used now)
    // The result is discarded because $00 is overwritten below, but we call it for side effects
    getFirebarPosition(savedSpinHigh, 0)

    //> ldy Enemy_SprDataOffset,x   ;get OAM data offset
    val sprDataOfs = (ram.enemySprDataOffset[x].toInt() and 0xFF) shr 2
    //> lda Enemy_Rel_YPos          ;get relative vertical coordinate
    //> sta Sprite_Y_Position,y     ;store as Y in OAM data
    //> sta $07                     ;also save here
    ram.sprites[sprDataOfs].y = ram.enemyRelYPos.toUByte()
    var screenY = ram.enemyRelYPos.toInt() and 0xFF

    //> lda Enemy_Rel_XPos          ;get relative horizontal coordinate
    //> sta Sprite_X_Position,y     ;store as X in OAM data
    //> sta $06                     ;also save here
    ram.sprites[sprDataOfs].x = ram.enemyRelXPos.toUByte()
    var sprOamOfs = sprDataOfs  // $06 holds current OAM data offset for iteration

    //> lda #$01; sta $00           ;set $01 value here (not necessary)
    // $00 is the firebar part counter, set to 1 initially (for the pivot collision check)

    //> jsr FirebarCollision        ;draw fireball part and do collision detection
    firebarCollision(sprDataOfs, sprOamOfs, screenY)

    //> ldy #$05                    ;load value for short firebars by default
    var maxParts = 0x05
    //> lda Enemy_ID,x; cmp #$1f   ;are we doing a long firebar?
    //> bcc SetMFbar
    if (enemyId >= 0x1f) {
        //> ldy #$0b                    ;otherwise load value for long firebars
        maxParts = 0x0b
    }

    //> SetMFbar: sty $ed                     ;store maximum value for length of firebars
    //> lda #$00; sta $00           ;initialize counter here
    var partCounter = 0

    //> DrawFbar:
    while (partCounter < maxParts) {
        //> lda $ef                     ;load high byte of spinstate
        //> jsr GetFirebarPosition      ;get fireball position data depending on firebar part
        val fbPos = getFirebarPosition(savedSpinHigh, partCounter)

        //> jsr DrawFirebar_Collision   ;position it properly, draw it and do collision detection
        sprOamOfs = drawFirebarCollision(fbPos, sprOamOfs, screenY)

        //> lda $00; cmp #$04; bne NextFbar
        if (partCounter == 0x04) {
            //> ldy DuplicateObj_Offset     ;if we arrive at fifth firebar part,
            val dupOffset = ram.duplicateObjOffset.toInt() and 0xFF
            //> lda Enemy_SprDataOffset,y   ;get offset from long firebar and load OAM data offset
            //> sta $06                     ;using long firebar offset, then store as new one here
            sprOamOfs = (ram.enemySprDataOffset[dupOffset].toInt() and 0xFF) shr 2
        }

        //> NextFbar: inc $00                     ;move onto the next firebar part
        partCounter++
    }
    //> SkipFBar: rts
}

/**
 * Modifies the firebar's spin state based on speed and direction.
 * Clockwise adds to the low byte (with carry to high), counter-clockwise subtracts.
 * @param spinSpeed the spinning speed value
 * @param x the enemy object offset
 * @return the new high byte of spinstate (A register value at return)
 */
private fun System.firebarSpin(spinSpeed: Int, x: Int): Int {
    //> FirebarSpin:
    //> sta $07                     ;save spinning speed here
    //> lda FirebarSpinDirection,x  ;check spinning direction
    val direction = ram.firebarSpinDirection[x].toInt() and 0xFF
    //> bne SpinCounterClockwise

    val spinLow = ram.sprObjXSpeed[1 + x].toInt() and 0xFF
    val spinHigh = ram.sprObjYSpeed[1 + x].toInt() and 0xFF

    if (direction == 0) {
        //> Clockwise (add)
        //> lda FirebarSpinState_Low,x
        //> clc; adc $07
        val newLow = spinLow + spinSpeed
        //> sta FirebarSpinState_Low,x
        ram.sprObjXSpeed[1 + x] = (newLow and 0xFF).toByte()
        //> lda FirebarSpinState_High,x; adc #$00
        val carry = if (newLow > 0xFF) 1 else 0
        return (spinHigh + carry) and 0xFF
    } else {
        //> SpinCounterClockwise:
        //> lda FirebarSpinState_Low,x
        //> sec; sbc $07
        val newLow = spinLow - spinSpeed
        //> sta FirebarSpinState_Low,x
        ram.sprObjXSpeed[1 + x] = (newLow and 0xFF).toByte()
        //> lda FirebarSpinState_High,x; sbc #$00
        val borrow = if (newLow < 0) 1 else 0
        return (spinHigh - borrow) and 0xFF
    }
}

/**
 * Calculates firebar position data (horizontal adder, vertical adder, mirror data)
 * from the spin state high byte and the current firebar part number.
 *
 * @param spinHigh the high byte of the spin state ($ef)
 * @param partNumber the current firebar part index ($00)
 * @return FirebarPosResult containing horizontal adder, vertical adder, and mirror data
 */
private fun getFirebarPosition(spinHigh: Int, partNumber: Int): FirebarPosResult {
    //> GetFirebarPosition:
    //> pha                        ;save high byte of spinstate

    //> and #%00001111             ;mask out low nybble
    var hIndex = spinHigh and 0x0F
    //> cmp #$09; bcc GetHAdder    ;if lower than $09, branch ahead
    if (hIndex >= 0x09) {
        //> eor #%00001111; clc; adc #$01  ;two's complement to oscillate
        hIndex = (hIndex xor 0x0F) + 1
    }
    //> GetHAdder: sta $01         ;store result here
    //> ldy $00                    ;load number of firebar ball
    //> lda FirebarTblOffsets,y    ;load offset to firebar position data
    val hTableIdx = firebarTblOffsets[partNumber.coerceIn(0, firebarTblOffsets.size - 1)]
    //> clc; adc $01; tay          ;add oscillated value as offset
    val hLookupIdx = (hTableIdx + hIndex).coerceIn(0, firebarPosLookupTbl.size - 1)
    //> lda FirebarPosLookupTbl,y  ;get data here and store as horizontal adder
    val hAdder = firebarPosLookupTbl[hLookupIdx]

    //> pla; pha                   ;get spinHigh back, save again
    //> clc; adc #$08              ;add eight for vertical phase offset
    //> and #%00001111             ;mask out high nybble
    var vIndex = (spinHigh + 0x08) and 0x0F
    //> cmp #$09; bcc GetVAdder
    if (vIndex >= 0x09) {
        //> eor #%00001111; clc; adc #$01
        vIndex = (vIndex xor 0x0F) + 1
    }
    //> GetVAdder: sta $02
    val vTableIdx = firebarTblOffsets[partNumber.coerceIn(0, firebarTblOffsets.size - 1)]
    val vLookupIdx = (vTableIdx + vIndex).coerceIn(0, firebarPosLookupTbl.size - 1)
    val vAdder = firebarPosLookupTbl[vLookupIdx]

    //> pla; lsr; lsr; lsr; tay    ;divide spinHigh by 8 for mirror index
    val mirrorIdx = (spinHigh ushr 3).coerceIn(0, firebarMirrorData.size - 1)
    //> lda FirebarMirrorData,y; sta $03
    val mirror = firebarMirrorData[mirrorIdx]

    return FirebarPosResult(hAdder, vAdder, mirror)
}

/**
 * Holds the computed firebar position data from GetFirebarPosition.
 */
private data class FirebarPosResult(
    val hAdder: Int,    // $01 - horizontal position adder
    val vAdder: Int,    // $02 - vertical position adder
    val mirror: Int     // $03 - mirroring data (controls adder sign)
)

/**
 * Positions a firebar sprite properly using position data and performs collision detection.
 * Handles mirroring (sign reversal) of the horizontal and vertical adders based on
 * the mirror data bits, then delegates to firebarCollision for rendering and hit testing.
 *
 * @param pos the firebar position result from GetFirebarPosition
 * @param currentOamOfs the current OAM data offset ($06)
 * @param screenY the current screen Y coordinate of the firebar center ($07)
 * @return the updated OAM data offset after collision processing
 */
private fun System.drawFirebarCollision(pos: FirebarPosResult, currentOamOfs: Int, screenY: Int): Int {
    //> DrawFirebar_Collision:
    //> lda $03; sta $05           ;store mirror data elsewhere
    var mirrorBits = pos.mirror

    //> ldy $06                    ;load OAM data offset for firebar
    val sprOfs = currentOamOfs

    //> lda $01                    ;load horizontal adder
    var hAdder = pos.hAdder
    //> lsr $05                    ;shift LSB of mirror data
    val mirrorH = mirrorBits and 0x01
    mirrorBits = mirrorBits ushr 1
    //> bcs AddHA                  ;if carry was set, skip negation
    if (mirrorH == 0) {
        //> eor #$ff; adc #$01     ;two's complement
        hAdder = (-(hAdder.toByte().toInt())) and 0xFF
    }

    //> AddHA: clc; adc Enemy_Rel_XPos  ;add horizontal coordinate relative to screen
    val sprX = (hAdder + (ram.enemyRelXPos.toInt() and 0xFF)) and 0xFF
    //> sta Sprite_X_Position,y
    ram.sprites[sprOfs].x = sprX.toUByte()

    //> cmp Enemy_Rel_XPos         ;compare X coordinate of sprite to original X of firebar
    val enemyRelX = ram.enemyRelXPos.toInt() and 0xFF
    val xDiff: Int
    if (sprX >= enemyRelX) {
        //> SubtR1: sec; sbc Enemy_Rel_XPos
        xDiff = sprX - enemyRelX
    } else {
        //> lda Enemy_Rel_XPos; sec; sbc $06
        xDiff = enemyRelX - sprX
    }

    //> ChkFOfs: cmp #$59         ;if difference of coordinates within a certain range
    //> bcc VAHandl                ;continue by handling vertical adder
    if (xDiff >= 0x59) {
        //> lda #$f8; bne SetVFbr  ;move sprite offscreen
        ram.sprites[sprOfs].y = 0xF8.toUByte()
        val newScreenY = 0xF8
        return firebarCollisionReturnOfs(sprOfs, newScreenY)
    }

    //> VAHandl: lda Enemy_Rel_YPos  ;if vertical relative coordinate offscreen
    //> cmp #$f8; beq SetVFbr
    val enemyRelY = ram.enemyRelYPos.toInt() and 0xFF
    if (enemyRelY == 0xF8) {
        //> SetVFbr: sta Sprite_Y_Position,y
        ram.sprites[sprOfs].y = 0xF8.toUByte()
        return firebarCollisionReturnOfs(sprOfs, 0xF8)
    }

    //> lda $02                    ;load vertical adder
    var vAdder = pos.vAdder
    //> lsr $05                    ;shift LSB of mirror data one more time
    val mirrorV = mirrorBits and 0x01
    //> bcs AddVA                  ;if carry was set, skip negation
    if (mirrorV == 0) {
        //> eor #$ff; adc #$01     ;two's complement
        vAdder = (-(vAdder.toByte().toInt())) and 0xFF
    }

    //> AddVA: clc; adc Enemy_Rel_YPos  ;add vertical coordinate
    val sprY = (vAdder + enemyRelY) and 0xFF
    //> SetVFbr: sta Sprite_Y_Position,y
    ram.sprites[sprOfs].y = sprY.toUByte()
    //> sta $07
    return firebarCollisionReturnOfs(sprOfs, sprY)
}

/**
 * Performs drawing and collision detection for a firebar sprite at the given OAM offset.
 * Draws the firebar tile, then checks if the firebar collides with the player by comparing
 * vertical and horizontal distances. If collision found, injuries the player.
 *
 * @param sprOfs the OAM sprite data offset for this firebar part
 * @param oamOfs the current $06 value (OAM offset for position tracking)
 * @param screenY the current $07 value (screen Y coordinate of this firebar part)
 * @return the updated OAM data offset ($06 + 4)
 */
private fun System.firebarCollision(sprOfs: Int, oamOfs: Int, screenY: Int): Int {
    val x = ram.objectOffset.toInt()

    //> FirebarCollision:
    //> jsr DrawFirebar          ;run sub here to draw current tile of firebar
    drawFirebar(sprOfs)

    //> lda StarInvincibleTimer  ;if star mario invincibility timer
    //> ora TimerControl         ;or master timer controls set
    //> bne NoColFB              ;then skip all of this
    if ((ram.starInvincibleTimer.toInt() and 0xFF) != 0 || ram.timerControl != 0.toByte()) {
        return oamOfs + 1  // advance OAM offset by 4 bytes (1 sprite)
    }

    //> sta $05                  ;initialize counter
    var collisionPhase = 0

    //> ldy Player_Y_HighPos; dey; bne NoColFB
    val playerYHigh = ram.playerYHighPos.toInt() and 0xFF
    if (playerYHigh != 1) {
        return oamOfs + 1
    }

    //> ldy Player_Y_Position    ;get player's vertical position
    var playerY = ram.playerYPosition.toInt() and 0xFF
    //> lda PlayerSize           ;get player's size
    //> bne AdjSm                ;if player small, branch
    if (ram.playerSize != 0.toByte()) {
        //> AdjSm: inc $05; inc $05  ;set counter to 2 (small/crouching flag)
        collisionPhase = 2
        //> tya; clc; adc #$18; tay  ;add 24 pixels to player's vertical coordinate
        playerY = (playerY + 0x18) and 0xFF
    } else {
        //> lda CrouchingFlag; beq BigJp
        if (ram.crouchingFlag != 0.toByte()) {
            //> AdjSm: inc $05; inc $05; tya; clc; adc #$18; tay
            collisionPhase = 2
            playerY = (playerY + 0x18) and 0xFF
        }
    }

    //> BigJp: tya               ;get vertical coordinate from Y
    var checkY = playerY

    //> FBCLoop:
    while (true) {
        //> sec; sbc $07          ;subtract vertical position of firebar
        var vDiff = checkY - screenY
        //> bpl ChkVFBD          ;if player lower on screen than firebar, skip negate
        if (vDiff < 0 || (vDiff and 0x80) != 0) {
            //> eor #$ff; clc; adc #$01  ;two's complement
            vDiff = (-(vDiff.toByte().toInt())) and 0xFF
        }
        vDiff = vDiff and 0xFF

        //> ChkVFBD: cmp #$08    ;if difference >= 8 pixels, skip ahead
        if (vDiff < 0x08) {
            //> lda $06           ;if firebar on far right, skip
            //> cmp #$f0
            if (oamOfs < 0xF0) {
                //> lda Sprite_X_Position+4  ;get OAM X coordinate for player sprite #1
                //> clc; adc #$04           ;add four pixels
                val playerSprX = ((ram.sprites[1].x.toInt() and 0xFF) + 4) and 0xFF

                //> sec; sbc $06            ;subtract horizontal coordinate of firebar
                var hDiff = playerSprX - oamOfs
                //> bpl ChkFBCl
                if (hDiff < 0 || (hDiff and 0x80) != 0) {
                    //> eor #$ff; clc; adc #$01
                    hDiff = (-(hDiff.toByte().toInt())) and 0xFF
                }
                hDiff = hDiff and 0xFF

                //> ChkFBCl: cmp #$08      ;if difference < 8 pixels, collision!
                if (hDiff < 0x08) {
                    //> ChgSDir:
                    //> ldx #$01            ;set movement direction by default
                    var moveDir = 1
                    //> lda $04; cmp $06    ;compare player sprite X to firebar X
                    //> bcs SetSDir
                    if (playerSprX < oamOfs) {
                        //> inx
                        moveDir = 2
                    }
                    //> SetSDir: stx Enemy_MovingDir
                    ram.enemyMovingDir = moveDir.toByte()
                    //> ldx #$00
                    //> lda $00; pha        ;save firebar part counter
                    //> jsr InjurePlayer
                    injurePlayer()
                    //> pla; sta $00        ;restore counter
                    return oamOfs + 1
                }
            }
        }

        //> Chk2Ofs: lda $05; cmp #$02; beq NoColFB
        if (collisionPhase >= 2) break

        //> ldy $05               ;use as offset
        //> lda Player_Y_Position; clc; adc FirebarYPos,y
        val fbYOfs = firebarYPos[collisionPhase.coerceIn(0, firebarYPos.size - 1)]
        checkY = ((ram.playerYPosition.toInt() and 0xFF) + fbYOfs) and 0xFF
        //> inc $05               ;increment phase and loop back
        collisionPhase++
    }

    //> NoColFB: pla; clc; adc #$04; sta $06
    return oamOfs + 1
}

/**
 * Helper that advances the OAM offset after firebar collision processing.
 * This encapsulates the NoColFB exit path: pop saved offset, add 4, store in $06.
 */
private fun System.firebarCollisionReturnOfs(sprOfs: Int, screenY: Int): Int {
    // The full firebarCollision does collision detection; this is the fast path
    // when the sprite was moved offscreen or skipped.
    drawFirebar(sprOfs)
    return sprOfs + 1
}

// Note: The following run pipeline functions are defined in other files:
// runNormalEnemies() - enemyBehaviors.kt
// runBowserFlame() - enemyBehaviors.kt
// runFireworks() - enemyBehaviors.kt
// runFirebarObj() - enemyBehaviors.kt
// runStarFlagObj() - enemyBehaviors.kt
// runRetainerObj() - enemyBehaviors.kt
// runSmallPlatform() - platformRoutines.kt
// runLargePlatform() - platformRoutines.kt
// runBowser() - bowserRoutine.kt
// powerUpObjHandler() - powerUpVine.kt
// vineObjectHandler() - powerUpVine.kt
// jumpspringHandler() - enemyBehaviors.kt
// warpZoneObject() - enemyBGCollision.kt
// moveNormalEnemy() - powerUpVine.kt
// moveJumpingEnemy() - powerUpVine.kt
// procBowserFlame() - bowserRoutine.kt
// enemiesCollision() - enemyBehaviors.kt (TODO stub)
// drawExplosionFireworks() - enemyBehaviors.kt (TODO stub)

// by Claude - RunNormalEnemies, EnemyMovementSubs, RunBowserFlame, RunFireworks, RunFirebarObj, RunRetainerObj, RunStarFlagObj, JumpspringHandler, WarpZoneObject, NoRunCode
// Translates enemy behavior dispatch and top-level enemy processing routines from smbdism.asm.
package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.EnemyState
import com.ivieleague.smbtranslation.utils.SpriteFlags
import com.ivieleague.smbtranslation.utils.getEnemyState

// ---- Data tables ----

//> Jumpspring_Y_PosData:
//> .db $08, $10, $08, $00
private val jumpspringYPosData = intArrayOf(0x08, 0x10, 0x08, 0x00)

//> StarFlagYPosAdder:
//> .db $00, $00, $08, $08
private val starFlagYPosAdder = intArrayOf(0x00, 0x00, 0x08, 0x08)

//> StarFlagXPosAdder:
//> .db $00, $08, $00, $08
private val starFlagXPosAdder = intArrayOf(0x00, 0x08, 0x00, 0x08)

//> StarFlagTileData:
//> .db $54, $55, $56, $57
private val starFlagTileData = intArrayOf(0x54, 0x55, 0x56, 0x57)

// ---- Top-level enemy dispatchers ----

/**
 * Empty handler for enemy objects that don't need processing.
 */
fun System.noRunCode() {
    //> NoRunCode:
    //> rts
}

/**
 * Processes the retainer (Toad/Princess) object at the end of castle levels.
 * Simply gets offscreen/relative position data and draws the sprite.
 */
fun System.runRetainerObj() {
    //> RunRetainerObj:
    //> jsr GetEnemyOffscreenBits
    getEnemyOffscreenBits()
    //> jsr RelativeEnemyPosition
    relativeEnemyPosition()
    //> jmp EnemyGfxHandler
    enemyGfxHandler()
}

/**
 * Main processing routine for normal enemies (IDs $00-$14).
 * Runs the full pipeline: offscreen check, collision detection, movement, and bounds check.
 */
fun System.runNormalEnemies() {
    val x = ram.objectOffset.toInt()
    fun traceState(step: String) {
        if (debugEnemyTrace) {
            val s = 1 + x
            println("  [RNE-$step] x=$x state=${ram.enemyState[x].toInt() and 0xFF} flag=${ram.enemyFlags[x].toInt() and 0xFF} ySpd=${ram.sprObjYSpeed[s].toInt() and 0xFF} yPos=${(ram.sprObjYPos[s].toInt() and 0xFF).toString(16)} yHi=${ram.sprObjYHighPos[s].toInt() and 0xFF} offscr=${ram.enemyOffscreenBits.toInt() and 0xFF}")
        }
    }
    //> RunNormalEnemies:
    //> lda #$00                  ;init sprite attributes
    //> sta Enemy_SprAttrib,x
    ram.sprAttrib[1 + x] = 0  // by Claude - indexed by x (Enemy_SprAttrib,x)
    //> jsr GetEnemyOffscreenBits
    shadow?.validated("getenemyoffscreenbits", this) { getEnemyOffscreenBits() } ?: getEnemyOffscreenBits()
    traceState("offscr")
    //> jsr RelativeEnemyPosition
    shadow?.validated("relativeenemyposition", this) { relativeEnemyPosition() } ?: relativeEnemyPosition()
    //> jsr EnemyGfxHandler
    enemyGfxHandler()
    traceState("gfx")
    //> jsr GetEnemyBoundBox
    shadow?.validated("getenemyboundbox", this) { getEnemyBoundBox() } ?: getEnemyBoundBox()
    //> jsr EnemyToBGCollisionDet
    shadow?.validated("enemytobgcollisiondet", this) { enemyToBGCollisionDet() } ?: enemyToBGCollisionDet()
    traceState("bgcol")
    //> jsr EnemiesCollision
    shadow?.validated("enemiescollision", this) { enemiesCollision() } ?: enemiesCollision()
    traceState("ecol")
    //> jsr PlayerEnemyCollision
    shadow?.validated("playerenemycollision", this) { playerEnemyCollision() } ?: playerEnemyCollision()
    traceState("pcol")
    //> ldy TimerControl          ;if master timer control set, skip to last routine
    //> bne SkipMove
    if (ram.timerControl == 0.toByte()) {
        //> jsr EnemyMovementSubs
        shadow?.validated("enemymovementsubs", this) { enemyMovementSubs() } ?: enemyMovementSubs()
    }
    traceState("move")
    //> SkipMove: jmp OffscreenBoundsCheck
    shadow?.validated("offscreenboundscheck", this) { offscreenBoundsCheck() } ?: offscreenBoundsCheck()
    traceState("bounds")
}

/**
 * Jump engine dispatching to per-enemy-type movement handlers based on Enemy_ID.
 * Only objects $00-$14 use this table.
 */
fun System.enemyMovementSubs() {
    val x = ram.objectOffset.toInt()
    //> EnemyMovementSubs:
    //> lda Enemy_ID,x
    //> jsr JumpEngine
    when (ram.enemyID[x].toInt() and 0xFF) {
        EnemyId.GreenKoopa.id -> moveNormalEnemy()   //> .dw MoveNormalEnemy      ;GreenKoopa
        EnemyId.RedKoopaShell.id -> moveNormalEnemy()   //> .dw MoveNormalEnemy
        EnemyId.BuzzyBeetle.id -> moveNormalEnemy()   //> .dw MoveNormalEnemy      ;BuzzyBeetle
        EnemyId.RedKoopa.id -> moveNormalEnemy()   //> .dw MoveNormalEnemy      ;RedKoopa
        //> .dw MoveNormalEnemy (SMB1) / .dw MovePiranhaPlant (SMB2J: UpsideDownPiranhaP)
        EnemyId.GreenKoopaVar.id -> if (variant == GameVariant.SMB2J) moveUpsideDownPiranhaP() else moveNormalEnemy()
        EnemyId.HammerBro.id -> procHammerBro()     //> .dw ProcHammerBro        ;HammerBro
        EnemyId.Goomba.id -> moveNormalEnemy()   //> .dw MoveNormalEnemy      ;Goomba
        EnemyId.Bloober.id -> moveBloober()       //> .dw MoveBloober          ;Bloober
        EnemyId.BulletBillFrenzyVar.id -> moveBulletBill()    //> .dw MoveBulletBill       ;BulletBill_FrenzyVar
        EnemyId.TallEnemy.id -> noMoveCode()        //> .dw NoMoveCode           ;TallEnemy (unused)
        EnemyId.GreyCheepCheep.id -> moveSwimmingCheepCheep()  //> .dw MoveSwimmingCheepCheep ;GreyCheepCheep
        EnemyId.RedCheepCheep.id -> moveSwimmingCheepCheep()  //> .dw MoveSwimmingCheepCheep ;RedCheepCheep
        EnemyId.Podoboo.id -> movePodoboo()       //> .dw MovePodoboo          ;Podoboo
        EnemyId.PiranhaPlant.id -> movePiranhaPlant()  //> .dw MovePiranhaPlant     ;PiranhaPlant
        EnemyId.GreenParatroopaJump.id -> moveJumpingEnemy()  //> .dw MoveJumpingEnemy     ;GreenParatroopaJump
        EnemyId.RedParatroopa.id -> procMoveRedPTroopa()  //> .dw ProcMoveRedPTroopa ;RedParatroopa
        EnemyId.GreenParatroopaFly.id -> moveFlyGreenPTroopa() //> .dw MoveFlyGreenPTroopa ;GreenParatroopaFly
        EnemyId.Lakitu.id -> moveLakitu()        //> .dw MoveLakitu           ;Lakitu
        EnemyId.Spiny.id -> moveNormalEnemy()   //> .dw MoveNormalEnemy      ;Spiny
        EnemyId.DummyEnemy.id -> noMoveCode()        //> .dw NoMoveCode           ;dummy
        EnemyId.FlyingCheepCheep.id -> moveFlyingCheepCheep()  //> .dw MoveFlyingCheepCheep ;FlyingCheepCheep
    }
}

/**
 * Empty movement handler for enemy types that don't move (TallEnemy, dummy).
 */
fun System.noMoveCode() {
    //> NoMoveCode:
    //> rts
}

/**
 * Processes Bowser's flame projectile.
 * Runs flame logic, then offscreen/collision/bounds checking.
 */
fun System.runBowserFlame() {
    //> RunBowserFlame:
    //> jsr ProcBowserFlame
    procBowserFlame()
    //> jsr GetEnemyOffscreenBits
    getEnemyOffscreenBits()
    //> jsr RelativeEnemyPosition
    relativeEnemyPosition()
    //> jsr GetEnemyBoundBox
    getEnemyBoundBox()
    //> jsr PlayerEnemyCollision
    playerEnemyCollision()
    //> jmp OffscreenBoundsCheck
    offscreenBoundsCheck()
}

/**
 * Processes firebar/spinning fire objects.
 * Runs firebar logic then checks offscreen bounds.
 */
fun System.runFirebarObj() {
    //> RunFirebarObj:
    //> jsr ProcFirebar
    procFirebar()
    //> jmp OffscreenBoundsCheck
    offscreenBoundsCheck()
}

// runSmallPlatform() moved to platformRoutines.kt

/**
 * Processes fireworks explosions at end of level.
 * Decrements explosion timer, advances graphics counter, and draws explosion sprites.
 * Awards 500 points and plays blast sound when explosion finishes.
 */
fun System.runFireworks() {
    val x = ram.objectOffset.toInt()
    //> RunFireworks:
    //> dec ExplosionTimerCounter,x ;decrement explosion timing counter here
    // ExplosionTimerCounter at $a0+x = sprObjYSpeed[1+x] (reused per-enemy)
    val timerVal = ((ram.sprObjYSpeed[1 + x].toInt() and 0xFF) - 1) and 0xFF
    ram.sprObjYSpeed[1 + x] = timerVal.toByte()
    //> bne SetupExpl               ;if not expired, skip this part
    if (timerVal == 0) {
        //> lda #$08
        //> sta ExplosionTimerCounter,x ;reset counter
        ram.sprObjYSpeed[1 + x] = 0x08
        //> inc ExplosionGfxCounter,x   ;increment explosion graphics counter
        // ExplosionGfxCounter at $58+x = sprObjXSpeed[1+x] (reused per-enemy)
        val gfxVal = ((ram.sprObjXSpeed[1 + x].toInt() and 0xFF) + 1) and 0xFF
        ram.sprObjXSpeed[1 + x] = gfxVal.toByte()
        //> lda ExplosionGfxCounter,x
        //> cmp #$03                    ;check explosion graphics counter
        //> bcs FireworksSoundScore     ;if at a certain point, branch to kill this object
        if (gfxVal >= 0x03) {
            fireworksSoundScore(x)
            return
        }
    }

    //> SetupExpl: jsr RelativeEnemyPosition   ;get relative coordinates of explosion
    relativeEnemyPosition()
    //> lda Enemy_Rel_YPos          ;copy relative coordinates
    //> sta Fireball_Rel_YPos       ;from the enemy object to the fireball object
    ram.fireballRelYPos = ram.enemyRelYPos
    //> lda Enemy_Rel_XPos          ;first vertical, then horizontal
    //> sta Fireball_Rel_XPos
    ram.fireballRelXPos = ram.enemyRelXPos
    //> ldy Enemy_SprDataOffset,x   ;get OAM data offset
    val sprDataOfs = (ram.enemySprDataOffset[x].toInt() and 0xFF) shr 2
    //> lda ExplosionGfxCounter,x   ;get explosion graphics counter
    val gfxCounter = ram.sprObjXSpeed[1 + x].toInt() and 0xFF
    //> jsr DrawExplosion_Fireworks ;do a sub to draw the explosion then leave
    drawExplosionFireworks(gfxCounter, sprDataOfs)
    //> rts
}

/**
 * Awards 500 points for a fireworks explosion and plays the blast sound.
 */
private fun System.fireworksSoundScore(x: Int) {
    //> FireworksSoundScore:
    //> lda #$00               ;disable enemy buffer flag
    //> sta Enemy_Flag,x
    ram.enemyFlags[x] = 0
    //> lda #Sfx_Blast         ;play fireworks/gunfire sound
    //> sta Square2SoundQueue
    ram.square2SoundQueue = Constants.Sfx_Blast
    //> lda #$05               ;set part of score modifier for 500 points
    //> sta DigitModifier+4
    ram.digitModifier[4] = 0x05
    //> jmp EndAreaPoints     ;jump to award points accordingly then leave
    endAreaPoints()
}

/**
 * Awards points at end of area: adds score based on DigitModifier for current player,
 * then updates the status bar display.
 */
fun System.endAreaPoints() {
    //> EndAreaPoints:
    // SMB2J: single-player, always uses player 1's score (ldy #$0b hardcoded)
    // then falls through to WriteDigits with A=$02 (updates score + game timer)
    // SMB1: indexes by CurrentPlayer to select player 1 or 2's score
    val scoreDisplay = if (variant == GameVariant.SMB2J) {
        ram.playerScoreDisplay
    } else if (ram.currentPlayer == 0.toByte()) {
        ram.playerScoreDisplay
    } else {
        ram.player2ScoreDisplay
    }
    //> ELPGive: jsr DigitsMathRoutine  ;award 50 points per game timer interval
    digitsMathRoutine(scoreDisplay)

    if (variant == GameVariant.SMB2J) {
        //> lda #$02; jmp WriteDigits
        updateNumber(0x02)
    } else {
        //> lda CurrentPlayer      ;get player on the screen
        //> asl; asl; asl; asl    ;shift to high nybble
        //> ora #%00000100         ;add four to set nybble for game timer
        val playerShifted = ((ram.currentPlayer.toInt() and 0xFF) shl 4) or 0x04
        //> jmp UpdateNumber       ;jump to print the new score and game timer
        updateNumber(playerShifted.toByte())
    }
}

// ---- Star Flag Object (end-of-level star) ----

/**
 * Processes the star flag object that appears at the end of each level.
 * Dispatches to subtasks: timer fireworks check, point award, flag raise, area end delay.
 */
fun System.runStarFlagObj() {
    val x = ram.objectOffset.toInt()
    //> RunStarFlagObj:
    //> lda #$00                 ;initialize enemy frenzy buffer
    //> sta EnemyFrenzyBuffer
    ram.enemyFrenzyBuffer = 0
    //> lda StarFlagTaskControl  ;check star flag object task number here
    //> cmp #$05                 ;if greater than 5, branch to exit
    //> bcs StarFlagExit
    val task = ram.starFlagTaskControl.toInt() and 0xFF
    if (task >= 0x05) return
    //> jsr JumpEngine           ;otherwise jump to appropriate sub
    when (task) {
        //> .dw StarFlagExit
        0 -> return
        //> .dw GameTimerFireworks
        1 -> gameTimerFireworks(x)
        //> .dw AwardGameTimerPoints
        2 -> awardGameTimerPoints()
        //> .dw RaiseFlagSetoffFWorks
        3 -> raiseFlagSetoffFWorks(x)
        //> .dw DelayToAreaEnd
        4 -> delayToAreaEnd(x)
    }
}

/**
 * Determines how many fireworks to display based on the last digit of the game timer.
 * Timer digit 1 -> 5 fireworks, 3 -> 3 fireworks, 6 -> 0 fireworks, else none.
 */
private fun System.gameTimerFireworks(x: Int) {
    //> GameTimerFireworks:
    //> lda GameTimerDisplay+2 ;get game timer's last digit
    val lastDigit = ram.gameTimerDisplay[2].toInt() and 0xFF
    // Assembly checks sequentially: if digit==1, Y=5; elif digit==3, Y=3; elif digit==6, Y=0; else A=$FF
    // A stays as the timer digit through the first three checks, only becomes $FF in the else case
    val (fireworks, enemyState) = when (lastDigit) {
        //> cmp #$01; beq SetFWC    ;if last digit set to 1, Y=$05 (5 fireworks)
        0x01 -> lastDigit to 0x05
        //> cmp #$03; beq SetFWC    ;if last digit set to 3, Y=$03 (3 fireworks)
        0x03 -> lastDigit to 0x03
        //> cmp #$06; beq SetFWC    ;if last digit set to 6, Y=$00 (0 fireworks)
        0x06 -> lastDigit to 0x00
        //> lda #$ff               ;otherwise set value for no fireworks
        else -> 0xFF to 0x00
    }

    //> SetFWC: sta FireworksCounter   ;set fireworks counter here
    ram.fireworksCounter = fireworks.toByte()
    //> sty Enemy_State,x      ;set whatever state we have in star flag object
    ram.enemyState[x] = enemyState.toByte()

    //> IncrementSFTask1:
    //> inc StarFlagTaskControl  ;increment star flag object task number
    ram.starFlagTaskControl = (ram.starFlagTaskControl + 1).toByte()
    //> StarFlagExit: rts
}

/**
 * Awards points for remaining game timer.
 * Subtracts 1 from game timer each frame and awards 50 points per tick.
 * Plays timer tick sound every 4 frames.
 */
private fun System.awardGameTimerPoints() {
    //> AwardGameTimerPoints:
    //> lda GameTimerDisplay   ;check all game timer digits for any intervals left
    //> ora GameTimerDisplay+1
    //> ora GameTimerDisplay+2
    val timeLeft = (ram.gameTimerDisplay[0].toInt() or ram.gameTimerDisplay[1].toInt() or ram.gameTimerDisplay[2].toInt()) and 0xFF
    //> beq IncrementSFTask1   ;if no time left on game timer at all, branch to next task
    if (timeLeft == 0) {
        //> IncrementSFTask1:
        ram.starFlagTaskControl = (ram.starFlagTaskControl + 1).toByte()
        return
    }
    //> lda FrameCounter
    //> and #%00000100         ;check frame counter for d2 set (skip ahead
    //> beq NoTTick            ;for four frames every four frames) branch if not set
    if ((ram.frameCounter.toInt() and 0x04) != 0) {
        //> lda #Sfx_TimerTick
        //> sta Square2SoundQueue  ;load timer tick sound
        ram.square2SoundQueue = Constants.Sfx_TimerTick
    }
    //> NoTTick: ldy #$23               ;set offset here to subtract from game timer's last digit
    //> lda #$ff               ;set adder here to $ff, or -1, to subtract one
    //> sta DigitModifier+5    ;from the last digit of the game timer
    ram.digitModifier[5] = 0xFF.toByte()
    //> jsr DigitsMathRoutine  ;subtract digit
    digitsMathRoutine(ram.gameTimerDisplay)
    //> lda #$05               ;set now to add 50 points
    //> sta DigitModifier+5    ;per game timer interval subtracted
    ram.digitModifier[5] = 0x05

    //> (fall through to EndAreaPoints)
    endAreaPoints()
}

/**
 * Raises the star flag to its final position, then triggers fireworks if applicable.
 */
private fun System.raiseFlagSetoffFWorks(x: Int) {
    //> RaiseFlagSetoffFWorks:
    //> lda Enemy_Y_Position,x  ;check star flag's vertical position
    val yPos = ram.sprObjYPos[1 + x].toInt() and 0xFF
    //> cmp #$72                ;against preset value
    //> bcc SetoffF             ;if star flag higher vertically, branch to other code
    if (yPos >= 0x72) {
        //> dec Enemy_Y_Position,x  ;otherwise, raise star flag by one pixel
        ram.sprObjYPos[1 + x] = ((yPos - 1) and 0xFF).toByte()
        //> jmp DrawStarFlag        ;and skip this part here
        drawStarFlag(x)
        return
    }

    //> SetoffF: lda FireworksCounter    ;check fireworks counter
    val fireworks = ram.fireworksCounter.toInt() and 0xFF
    //> beq DrawFlagSetTimer    ;if no fireworks left to go off, skip this part
    //> bmi DrawFlagSetTimer    ;if no fireworks set to go off, skip this part
    if (fireworks != 0 && (fireworks and 0x80) == 0) {
        //> lda #Fireworks
        //> sta EnemyFrenzyBuffer   ;otherwise set fireworks object in frenzy queue
        ram.enemyFrenzyBuffer = EnemyId.Fireworks.byte
        //> (fall through to DrawStarFlag)
        drawStarFlag(x)
        return
    }

    //> DrawFlagSetTimer:
    //> jsr DrawStarFlag          ;do sub to draw star flag
    drawStarFlag(x)
    //> lda #$06
    //> sta EnemyIntervalTimer,x  ;set interval timer here
    ram.timers[0x16 + x] = 0x06

    //> IncrementSFTask2:
    //> inc StarFlagTaskControl   ;move onto next task
    ram.starFlagTaskControl = (ram.starFlagTaskControl + 1).toByte()
    //> rts
}

/**
 * Draws the star flag as four sprites arranged in a 2x2 pattern.
 */
private fun System.drawStarFlag(x: Int) {
    //> DrawStarFlag:
    //> jsr RelativeEnemyPosition  ;get relative coordinates of star flag
    relativeEnemyPosition()
    //> ldy Enemy_SprDataOffset,x  ;get OAM data offset
    var sprOfs = (ram.enemySprDataOffset[x].toInt() and 0xFF) shr 2
    //> ldx #$03                   ;do four sprites
    //> DSFLoop:
    for (i in 3 downTo 0) {
        //> lda Enemy_Rel_YPos         ;get relative vertical coordinate
        //> clc
        //> adc StarFlagYPosAdder,x    ;add Y coordinate adder data
        //> sta Sprite_Y_Position,y    ;store as Y coordinate
        val sprY = ((ram.enemyRelYPos.toInt() and 0xFF) + starFlagYPosAdder[i]) and 0xFF
        ram.sprites[sprOfs].y = sprY.toUByte()
        //> lda StarFlagTileData,x     ;get tile number
        //> sta Sprite_Tilenumber,y    ;store as tile number
        ram.sprites[sprOfs].tilenumber = starFlagTileData[i].toByte()
        //> lda #$22                   ;set palette and background priority bits
        //> sta Sprite_Attributes,y    ;store as attributes
        ram.sprites[sprOfs].attributes = SpriteFlags(0x22)
        //> lda Enemy_Rel_XPos         ;get relative horizontal coordinate
        //> clc
        //> adc StarFlagXPosAdder,x    ;add X coordinate adder data
        //> sta Sprite_X_Position,y    ;store as X coordinate
        val sprX = ((ram.enemyRelXPos.toInt() and 0xFF) + starFlagXPosAdder[i]) and 0xFF
        ram.sprites[sprOfs].x = sprX.toUByte()
        //> iny; iny; iny; iny         ;increment OAM data offset four bytes for next sprite
        sprOfs++
        //> dex; bpl DSFLoop           ;do this until all sprites are done
    }
    //> ldx ObjectOffset           ;get enemy object offset and leave
    //> rts
}

/**
 * Waits for interval timer and event music to finish before advancing to next task.
 */
private fun System.delayToAreaEnd(x: Int) {
    //> DelayToAreaEnd:
    //> jsr DrawStarFlag          ;do sub to draw star flag
    drawStarFlag(x)
    //> lda EnemyIntervalTimer,x  ;if interval timer set in previous task
    //> bne StarFlagExit2         ;not yet expired, branch to leave
    if (ram.timers[0x16 + x] != 0.toByte()) return
    //> lda EventMusicBuffer      ;if event music buffer empty,
    //> beq IncrementSFTask2      ;branch to increment task
    if (ram.eventMusicBuffer == 0.toByte()) {
        //> IncrementSFTask2:
        //> inc StarFlagTaskControl   ;move onto next task
        ram.starFlagTaskControl = (ram.starFlagTaskControl + 1).toByte()
    }
    //> StarFlagExit2: rts
//> ;$00 - used to store horizontal difference between player and piranha plant
}

// ---- Jumpspring Handler ----

/**
 * Processes the jumpspring object: animates the spring, bounces the player upward,
 * and handles A-button boost for higher jumps.
 */
fun System.jumpspringHandler() {
    val x = ram.objectOffset.toInt()
    //> JumpspringHandler:
    //> jsr GetEnemyOffscreenBits   ;get offscreen information
    getEnemyOffscreenBits()
    //> lda TimerControl            ;check master timer control
    //> bne DrawJSpr                ;branch to last section if set
    if (ram.timerControl != 0.toByte()) {
        drawJumpspring(x)
        return
    }
    //> lda JumpspringAnimCtrl      ;check jumpspring frame control
    //> beq DrawJSpr                ;branch to last section if not set
    val animCtrl = ram.jumpspringAnimCtrl.toInt() and 0xFF
    if (animCtrl == 0) {
        drawJumpspring(x)
        return
    }

    //> tay
    //> dey                         ;subtract one from frame control,
    //> tya                         ;the only way a poor nmos 6502 can
    val y = (animCtrl - 1) and 0xFF
    //> and #%00000010              ;mask out all but d1, original value still in Y
    //> bne DownJSpr                ;if set, branch to move player up
    if ((y and 0x02) == 0) {
        //> inc Player_Y_Position
        //> inc Player_Y_Position       ;move player's vertical position down two pixels
        //> jmp PosJSpr                 ;skip to next part
        ram.playerYPosition = ((ram.playerYPosition.toInt() + 2) and 0xFF).toUByte()
    } else {
        //> DownJSpr:
        //> dec Player_Y_Position       ;move player's vertical position up two pixels
        //> dec Player_Y_Position
        ram.playerYPosition = ((ram.playerYPosition.toInt() - 2) and 0xFF).toUByte()
    }

    //> PosJSpr:
    //> lda Jumpspring_FixedYPos,x  ;get permanent vertical position
    // Jumpspring_FixedYPos at $58+x = sprObjXSpeed[1+x] (reused per-enemy)
    val fixedY = ram.sprObjXSpeed[1 + x].toInt() and 0xFF
    //> clc
    //> adc Jumpspring_Y_PosData,y  ;add value using frame control as offset
    val newY = (fixedY + jumpspringYPosData[y]) and 0xFF
    //> sta Enemy_Y_Position,x      ;store as new vertical position
    ram.sprObjYPos[1 + x] = newY.toByte()

    //> cpy #$01                    ;check frame control offset (second frame is $00)
    //> bcc BounceJS                ;if offset not yet at third frame ($01), skip to next part
    if (y >= 0x01) {
        //> lda A_B_Buttons
        //> and #A_Button               ;check saved controller bits for A button press
        val aPressed = (ram.aBButtons.toInt() and Constants.A_Button.toInt()) and 0xFF
        //> beq BounceJS                ;skip to next part if A not pressed
        if (aPressed != 0) {
            //> and PreviousA_B_Buttons     ;check for A button pressed in previous frame
            //> bne BounceJS                ;skip to next part if so
            if ((aPressed and ram.previousABButtons.toInt()) == 0) {
                //> lda #$f4
                //> sta JumpspringForce         ;otherwise write new jumpspring force here
                ram.jumpspringForce = 0xF4.toByte()
            }
        }
    }

    //> BounceJS:
    //> cpy #$03                    ;check frame control offset again
    //> bne DrawJSpr                ;skip to last part if not yet at fifth frame ($03)
    if (y == 0x03) {
        //> lda JumpspringForce
        //> sta Player_Y_Speed          ;store jumpspring force as player's new vertical speed
        ram.sprObjYSpeed[0] = ram.jumpspringForce
        //> lda #$00
        //> sta JumpspringAnimCtrl      ;initialize jumpspring frame control
        ram.jumpspringAnimCtrl = 0
    }

    //> DrawJSpr:
    drawJumpspring(x)
}

/**
 * Draws the jumpspring and checks if it should be animated or killed.
 */
private fun System.drawJumpspring(x: Int) {
    //> DrawJSpr:
    //> jsr RelativeEnemyPosition   ;get jumpspring's relative coordinates
    relativeEnemyPosition()
    //> jsr EnemyGfxHandler         ;draw jumpspring
    enemyGfxHandler()
    //> jsr OffscreenBoundsCheck    ;check to see if we need to kill it
    offscreenBoundsCheck()
    //> lda JumpspringAnimCtrl      ;if frame control at zero, don't bother
    //> beq ExJSpring               ;trying to animate it, just leave
    if (ram.jumpspringAnimCtrl == 0.toByte()) return
    //> lda JumpspringTimer
    //> bne ExJSpring               ;if jumpspring timer not expired yet, leave
    if (ram.jumpspringTimer != 0.toByte()) return
    //> lda #$04
    //> sta JumpspringTimer         ;otherwise initialize jumpspring timer
    ram.jumpspringTimer = 0x04
    //> inc JumpspringAnimCtrl      ;increment frame control to animate jumpspring
    ram.jumpspringAnimCtrl = (ram.jumpspringAnimCtrl + 1).toByte()
    //> ExJSpring: rts
}

// warpZoneObject() moved to enemyBGCollision.kt

// ---- Enemy Movement Handlers ----
// Per-enemy movement handlers are implemented in their respective files:

// moveNormalEnemy() - powerUpVine.kt
// moveJumpingEnemy() - powerUpVine.kt
// procHammerBro() - runEnemyObjectsCore.kt
// moveBloober() - runEnemyObjectsCore.kt
// moveBulletBill() - runEnemyObjectsCore.kt
// moveSwimmingCheepCheep() - runEnemyObjectsCore.kt
// movePodoboo() - runEnemyObjectsCore.kt
// movePiranhaPlant() - runEnemyObjectsCore.kt
// procMoveRedPTroopa() - runEnemyObjectsCore.kt
// moveFlyGreenPTroopa() - runEnemyObjectsCore.kt
// moveLakitu() - runEnemyObjectsCore.kt
// moveFlyingCheepCheep() - runEnemyObjectsCore.kt
// procFirebar() - runEnemyObjectsCore.kt (TODO stub)
// procBowserFlame() - bowserRoutine.kt

// by Claude - EnemiesCollision, ProcEnemyCollisions, DrawExplosion_Fireworks

//> ExplosionTiles:
//> .db $68, $67, $66
private val explosionTilesData = byteArrayOf(0x68, 0x67, 0x66)

//> SetBitsMask:
//> .db %10000000, %01000000, %00100000, %00010000, %00001000, %00000100, %00000010
private val setBitsMaskEC = byteArrayOf(
    0x80.toByte(), 0x40, 0x20, 0x10, 0x08, 0x04, 0x02
)

//> ClearBitsMask:
//> .db %01111111, %10111111, %11011111, %11101111, %11110111, %11111011, %11111101
private val clearBitsMaskEC = byteArrayOf(
    0x7f, 0xbf.toByte(), 0xdf.toByte(), 0xef.toByte(), 0xf7.toByte(), 0xfb.toByte(), 0xfd.toByte()
)

/**
 * Detects and handles collisions between enemy objects.
 * Only runs on odd frames and not in water areas. For each pair of enemies, checks
 * bounding box overlap. On collision, delegates to procEnemyCollisions which handles
 * shell-kills, turn-arounds, and point awards.
 */
fun System.enemiesCollision() {
    val x = ram.objectOffset.toInt()

    //> EnemiesCollision:
    //> lda FrameCounter            ;check counter for d0 set
    //> lsr
    //> bcc ExSFN                   ;if d0 not set, leave
    if ((ram.frameCounter.toInt() and 0x01) == 0) return

    //> lda AreaType
    //> beq ExSFN                   ;if water area type, leave
    if (ram.areaType == AreaType.Water) return

    //> lda Enemy_ID,x
    val enemyId = ram.enemyID[x].toInt() and 0xFF
    //> cmp #$15                    ;if enemy object => $15, branch to leave
    //> bcs ExitECRoutine
    if (enemyId >= EnemyId.BowserFlame.id) return
    //> cmp #Lakitu                 ;if lakitu, branch to leave
    //> beq ExitECRoutine
    if (enemyId == EnemyId.Lakitu.id) return
    //> cmp #PiranhaPlant           ;if piranha plant, branch to leave
    //> beq ExitECRoutine
    if (enemyId == EnemyId.PiranhaPlant.id) return
    // SMB2J: upside-down piranha (ID $04) also exempt from enemy-to-enemy collision
    if (variant == GameVariant.SMB2J && enemyId == EnemyId.GreenKoopaVar.id) return
    //> lda EnemyOffscrBitsMasked,x ;if masked offscreen bits nonzero, branch to leave
    //> bne ExitECRoutine
    if (ram.enemyOffscrBitsMaskeds[x] != 0.toByte()) return

    //> jsr GetEnemyBoundBoxOfs     ;get appropriate bounding box offset for first enemy
    val (firstBBOffset, _) = getEnemyBoundBoxOfs()
    val bbY = firstBBOffset  // Y register = bounding box offset for first enemy

    //> dex                         ;decrement for second enemy
    //> bmi ExitECRoutine           ;branch to leave if there are no other enemies
    var secondIdx = x - 1
    if (secondIdx < 0) return

    //> ECLoop:
    while (secondIdx >= 0) {
        //> stx $01                     ;save enemy object buffer offset for second enemy here
        val savedSecondIdx = secondIdx

        //> lda Enemy_Flag,x            ;check enemy object enable flag
        //> beq ReadyNextEnemy          ;branch if flag not set
        if (ram.enemyFlags[secondIdx] != 0.toByte()) {
            //> lda Enemy_ID,x
            val secondEnemyId = ram.enemyID[secondIdx].toInt() and 0xFF
            //> cmp #$15                    ;check for enemy object => $15
            //> bcs ReadyNextEnemy
            if (secondEnemyId < EnemyId.BowserFlame.id &&
                //> cmp #Lakitu; beq ReadyNextEnemy
                secondEnemyId != EnemyId.Lakitu.id &&
                //> cmp #PiranhaPlant; beq ReadyNextEnemy
                secondEnemyId != EnemyId.PiranhaPlant.id &&
                //> SMB2J: cmp #UpsideDownPiranhaP; beq ReadyNextEnemy
                !(variant == GameVariant.SMB2J && secondEnemyId == EnemyId.GreenKoopaVar.id) &&
                //> lda EnemyOffscrBitsMasked,x; bne ReadyNextEnemy
                ram.enemyOffscrBitsMaskeds[secondIdx] == 0.toByte()
            ) {
                //> txa                         ;get second enemy object's bounding box offset
                //> asl; asl                    ;multiply by four, then add four
                //> clc; adc #$04; tax          ;use as new contents of X
                val secondBBOffset = (secondIdx * 4) + 4

                //> jsr SprObjectCollisionCore  ;do collision detection using the two enemies here
                val collision = sprObjectCollisionCore(secondBBOffset, bbY)

                //> ldx ObjectOffset            ;use first enemy offset for X
                //> ldy $01                     ;use second enemy offset for Y
                //> bcc NoEnemyCollision        ;if carry clear, no collision, branch ahead of this
                if (collision) {
                    //> lda Enemy_State,x
                    //> ora Enemy_State,y           ;check both enemy states for d7 set
                    //> and #%10000000
                    val eitherKicked = ram.enemyState.getEnemyState(x).kickedOrEmerged ||
                            ram.enemyState.getEnemyState(savedSecondIdx).kickedOrEmerged
                    //> bne YesEC                   ;branch if at least one of them is set
                    if (!eitherKicked) {
                        //> lda Enemy_CollisionBits,y   ;load first enemy's collision-related bits
                        val collBits = ram.enemyCollisionBitsArr[savedSecondIdx].toInt() and 0xFF
                        //> and SetBitsMask,x           ;check to see if bit connected to second enemy is
                        //> bne ReadyNextEnemy          ;already set
                        if ((collBits and (setBitsMaskEC[x].toInt() and 0xFF)) != 0) {
                            // Already colliding, skip
                            secondIdx--
                            continue
                        }
                        //> lda Enemy_CollisionBits,y
                        //> ora SetBitsMask,x           ;set the bit now
                        //> sta Enemy_CollisionBits,y
                        ram.enemyCollisionBitsArr[savedSecondIdx] =
                            (collBits or (setBitsMaskEC[x].toInt() and 0xFF)).toByte()
                    }
                    //> YesEC:  jsr ProcEnemyCollisions     ;react according to collision
                    procEnemyCollisions(x, savedSecondIdx)
                    //> jmp ReadyNextEnemy
                } else {
                    //> NoEnemyCollision:
                    //> lda Enemy_CollisionBits,y     ;load first enemy's collision-related bits
                    //> and ClearBitsMask,x           ;clear bit connected to second enemy
                    //> sta Enemy_CollisionBits,y
                    val collBits = ram.enemyCollisionBitsArr[savedSecondIdx].toInt() and 0xFF
                    ram.enemyCollisionBitsArr[savedSecondIdx] =
                        (collBits and (clearBitsMaskEC[x].toInt() and 0xFF)).toByte()
                }
            }
        }

        //> ReadyNextEnemy:
        //> ldx $01; dex; bpl ECLoop
        secondIdx--
    }

    //> ExitECRoutine:
    //> ldx ObjectOffset
}

/**
 * Handles the result of a collision between two enemies.
 * If either is in shell state (>= $06), kills the other and awards points.
 * If neither is in shell state, both turn around.
 * @param firstIdx ObjectOffset - the first (current) enemy index
 * @param secondIdx the second enemy index from the loop
 */
private fun System.procEnemyCollisions(firstIdx: Int, secondIdx: Int) {
    //> ProcEnemyCollisions:
    //> lda Enemy_State,y        ;check both enemy states for d5 set
    //> ora Enemy_State,x
    //> and #%00100000           ;if d5 is set in either state, or both, branch to leave
    //> bne ExitProcessEColl
    val eitherDefeated = ram.enemyState.getEnemyState(secondIdx).defeated ||
            ram.enemyState.getEnemyState(firstIdx).defeated
    if (eitherDefeated) return

    //> lda Enemy_State,x
    val firstStateVal = ram.enemyState[firstIdx].toInt() and 0xFF
    //> cmp #$06                 ;if second enemy state < $06, branch elsewhere
    //> bcc ProcSecondEnemyColl
    if (firstStateVal >= 0x06) {
        //> lda Enemy_ID,x           ;check second enemy identifier for hammer bro
        val firstEnemyId = ram.enemyID[firstIdx].toInt() and 0xFF
        //> cmp #HammerBro           ;if hammer bro found in alt state, branch to leave
        //> beq ExitProcessEColl
        if (firstEnemyId == EnemyId.HammerBro.id) return

        //> lda Enemy_State,y        ;check first enemy state for d7 set
        //> asl; bcc ShellCollisions ;branch if d7 is clear
        if (ram.enemyState.getEnemyState(secondIdx).kickedOrEmerged) {
            //> lda #$06; jsr SetupFloateyNumber   ;award 1000 points for killing enemy
            val savedOfs = ram.objectOffset
            ram.objectOffset = firstIdx.toByte()
            setupFloateyNumber(0x06)
            ram.objectOffset = savedOfs
            //> jsr ShellOrBlockDefeat   ;then kill enemy
            shellOrBlockDefeat(firstIdx)
            //> ldy $01                  ;original offset of second enemy
        }

        //> ShellCollisions:
        //> tya; tax; jsr ShellOrBlockDefeat   ;kill second enemy
        shellOrBlockDefeat(secondIdx)
        //> ldx ObjectOffset
        //> lda ShellChainCounter,x  ;get chain counter for shell
        val chainCount = ram.shellChainCounters[firstIdx].toInt() and 0xFF
        //> clc; adc #$04                 ;add four to get appropriate point offset
        val pointsOfs = (chainCount + 0x04) and 0xFF
        //> ldx $01; jsr SetupFloateyNumber   ;award points for second enemy
        val savedOfs = ram.objectOffset
        ram.objectOffset = secondIdx.toByte()
        setupFloateyNumber(pointsOfs)
        ram.objectOffset = savedOfs
        //> ldx ObjectOffset
        //> inc ShellChainCounter,x  ;increment chain counter
        ram.shellChainCounters[firstIdx] = ((chainCount + 1) and 0xFF).toByte()
    } else {
        //> ProcSecondEnemyColl:
        val secondStateVal = ram.enemyState[secondIdx].toInt() and 0xFF
        //> lda Enemy_State,y        ;if first enemy state < $06, branch elsewhere
        //> cmp #$06; bcc MoveEOfs
        if (secondStateVal >= 0x06) {
            //> lda Enemy_ID,y           ;check first enemy identifier for hammer bro
            val secondEnemyId = ram.enemyID[secondIdx].toInt() and 0xFF
            //> cmp #HammerBro; beq ExitProcessEColl
            if (secondEnemyId == EnemyId.HammerBro.id) return

            //> jsr ShellOrBlockDefeat   ;kill first enemy (using X = ObjectOffset)
            shellOrBlockDefeat(firstIdx)
            //> ldy $01
            //> lda ShellChainCounter,y  ;get chain counter for shell
            val chainCount = ram.shellChainCounters[secondIdx].toInt() and 0xFF
            //> clc; adc #$04
            val pointsOfs = (chainCount + 0x04) and 0xFF
            //> ldx ObjectOffset; jsr SetupFloateyNumber
            val savedOfs = ram.objectOffset
            ram.objectOffset = firstIdx.toByte()
            setupFloateyNumber(pointsOfs)
            ram.objectOffset = savedOfs
            //> ldx $01; inc ShellChainCounter,x
            ram.shellChainCounters[secondIdx] = ((chainCount + 1) and 0xFF).toByte()
        } else {
            //> MoveEOfs:
            //> tya; tax; jsr EnemyTurnAround      ;do the sub using second enemy offset
            enemyTurnAround(secondIdx)
            //> ldx ObjectOffset                    ;then do it again using first enemy offset
            enemyTurnAround(firstIdx)
        }
    }
    //> ExitProcessEColl: rts
}

// smallPlatformCollision(), drawSmallPlatform(), moveSmallPlatform() moved to platformRoutines.kt

/**
 * Draws an explosion/fireworks using the fireball explosion sprite layout.
 * Draws 4 sprites in a 2x2 pattern with mirrored flipping.
 * @param gfxCounter explosion graphics frame (0-2)
 * @param sprDataOffset OAM sprite data offset to write to
 */
fun System.drawExplosionFireworks(gfxCounter: Int, sprDataOffset: Int) {
    //> DrawExplosion_Fireworks:
    val y = sprDataOffset

    //> tax                         ;use whatever's in A for offset
    //> lda ExplosionTiles,x        ;get tile number using offset
    val tile = explosionTilesData[gfxCounter.coerceIn(0, 2)]

    //> iny                         ;increment Y (contains sprite data offset)
    //> jsr DumpFourSpr             ;and dump into tile number part of sprite data
    ram.sprites[y].tilenumber = tile
    ram.sprites[y + 1].tilenumber = tile
    ram.sprites[y + 2].tilenumber = tile
    ram.sprites[y + 3].tilenumber = tile

    //> dey                         ;decrement Y so we have the proper offset again
    //> ldx ObjectOffset            ;return enemy object buffer offset to X

    //> lda Fireball_Rel_YPos       ;get relative vertical coordinate
    val relY = ram.fireballRelYPos.toInt() and 0xFF
    //> sec; sbc #$04               ;subtract four pixels vertically for first and third sprites
    val topY = ((relY - 4) and 0xFF).toUByte()
    //> sta Sprite_Y_Position,y
    //> sta Sprite_Y_Position+8,y
    ram.sprites[y].y = topY
    ram.sprites[y + 2].y = topY
    //> clc; adc #$08               ;add eight pixels vertically for second and fourth sprites
    val botY = ((topY.toInt() + 8) and 0xFF).toUByte()
    //> sta Sprite_Y_Position+4,y
    //> sta Sprite_Y_Position+12,y
    ram.sprites[y + 1].y = botY
    ram.sprites[y + 3].y = botY

    //> lda Fireball_Rel_XPos       ;get relative horizontal coordinate
    val relX = ram.fireballRelXPos.toInt() and 0xFF
    //> sec; sbc #$04               ;subtract four pixels horizontally for first and second sprites
    val leftX = ((relX - 4) and 0xFF).toUByte()
    //> sta Sprite_X_Position,y
    //> sta Sprite_X_Position+4,y
    ram.sprites[y].x = leftX
    ram.sprites[y + 1].x = leftX
    //> clc; adc #$08               ;add eight pixels horizontally for third and fourth sprites
    val rightX = ((leftX.toInt() + 8) and 0xFF).toUByte()
    //> sta Sprite_X_Position+8,y
    //> sta Sprite_X_Position+12,y
    ram.sprites[y + 2].x = rightX
    ram.sprites[y + 3].x = rightX

    //> lda #$02; sta Sprite_Attributes,y     ;no flip for first sprite
    ram.sprites[y].attributes = SpriteFlags(0x02)
    //> lda #$82; sta Sprite_Attributes+4,y   ;vertical flip for second sprite
    ram.sprites[y + 1].attributes = SpriteFlags(0x82.toByte())
    //> lda #$42; sta Sprite_Attributes+8,y   ;horizontal flip for third sprite
    ram.sprites[y + 2].attributes = SpriteFlags(0x42)
    //> lda #$c2; sta Sprite_Attributes+12,y  ;both flips for fourth sprite
    ram.sprites[y + 3].attributes = SpriteFlags(0xc2.toByte())
    //> rts
//> ;$01 - used to hold enemy offset for second enemy
//> ExSFN: rts
}

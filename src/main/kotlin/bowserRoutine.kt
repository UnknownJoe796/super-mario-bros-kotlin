// by Claude - RunBowser: Bowser AI, movement, flame attack, bridge collapse interaction
package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.SpriteFlags

//> PRandomRange:
//> .db $21, $41, $11, $31
private val PRandomRange = byteArrayOf(0x21, 0x41, 0x11, 0x31)

//> FlameTimerData:
//> .db $bf, $40, $bf, $bf, $bf, $40, $40, $bf
private val FlameTimerData = byteArrayOf(
    0xbf.toByte(), 0x40, 0xbf.toByte(), 0xbf.toByte(),
    0xbf.toByte(), 0x40, 0x40, 0xbf.toByte()
)

//> FlameYPosData:
//> .db $90, $80, $70, $90
private val FlameYPosData = byteArrayOf(
    0x90.toByte(), 0x80.toByte(), 0x70, 0x90.toByte()
)

// -------------------------------------------------------------------------------------
// RunBowser - Main Bowser processing
// -------------------------------------------------------------------------------------

/**
 * Main Bowser AI routine. Handles movement patterns, direction changes,
 * flame spawning, hammer throwing (world 6+), and defeat detection.
 * Bowser can be defeated by bridge collapse (handled in victorySubs.kt)
 * or by 5 fireballs (tracked in Enemy_State d5).
 */
fun System.runBowser() {
    val x = ram.objectOffset.toInt()
    val sprObjOfs = x + 1 // SprObject offset for this enemy

    //> RunBowser:
    //> lda Enemy_State,x       ;if d5 in enemy state is not set
    //> and #%00100000          ;then branch elsewhere to run bowser
    //> beq BowserControl
    val state = ram.enemyState[x].toInt() and 0xFF
    if (state and 0x20 != 0) {
        //> lda Enemy_Y_Position,x  ;otherwise check vertical position
        //> cmp #$e0                ;if above a certain point, branch to move defeated bowser
        //> bcc MoveD_Bowser        ;otherwise proceed to KillAllEnemies
        val yPos = ram.sprObjYPos[sprObjOfs].toInt() and 0xFF
        if (yPos < 0xE0) {
            //> MoveD_Bowser:
            moveDefeatedBowser()
            return
        }
        //> KillAllEnemies:
        killAllEnemies()
        return
    }

    //> BowserControl:
    //> lda #$00
    //> sta EnemyFrenzyBuffer      ;empty frenzy buffer
    ram.enemyFrenzyBuffer = 0

    //> lda TimerControl           ;if master timer control not set,
    //> beq ChkMouth               ;skip jump and execute code here
    //> jmp SkipToFB               ;otherwise, jump over a bunch of code
    if (ram.timerControl != 0.toByte()) {
        // SkipToFB: jump to flame check
        checkFireBreath(x)
        return
    }

    //> ChkMouth:  lda BowserBodyControls     ;check bowser's mouth
    //> bpl FeetTmr                ;if bit clear, go ahead with code here
    //> jmp HammerChk              ;otherwise skip a whole section starting here
    if (ram.bowserBodyControls.toInt() and 0x80 != 0) {
        // Mouth is open (d7 set): skip feet/movement, go to hammer check
        hammerCheck(x)
        return
    }

    //> FeetTmr:   dec BowserFeetCounter      ;decrement timer to control bowser's feet
    //> bne ResetMDr               ;if not expired, skip this part
    ram.bowserFeetCounter = (ram.bowserFeetCounter - 1).toByte()
    if (ram.bowserFeetCounter == 0.toByte()) {
        //> lda #$20                   ;otherwise, reset timer
        //> sta BowserFeetCounter
        ram.bowserFeetCounter = 0x20
        //> lda BowserBodyControls     ;and invert bit used
        //> eor #%00000001             ;to control bowser's feet
        //> sta BowserBodyControls
        ram.bowserBodyControls = (ram.bowserBodyControls.toInt() xor 0x01).toByte()
    }

    //> ResetMDr:  lda FrameCounter           ;check frame counter
    //> and #%00001111             ;if not on every sixteenth frame, skip
    //> bne B_FaceP                ;ahead to continue code
    if (ram.frameCounter.toInt() and 0x0F == 0) {
        //> lda #$02                   ;otherwise reset moving/facing direction every
        //> sta Enemy_MovingDir,x      ;sixteen frames
        ram.enemyMovingDirs[x] = 0x02
    }

    //> B_FaceP:   lda EnemyFrameTimer,x      ;if timer set here expired,
    //> beq GetPRCmp               ;branch to next section
    val enemyFrameTimer = ram.timers[0x0a + x].toInt() and 0xFF
    var skipToHammerCheck = false
    if (enemyFrameTimer != 0) {
        //> jsr PlayerEnemyDiff        ;get horizontal difference between player and bowser,
        //> bpl GetPRCmp               ;and branch if bowser to the right of the player
        val (_, highDiff) = playerEnemyDiff()
        if (highDiff.toByte() < 0) {
            //> lda #$01
            //> sta Enemy_MovingDir,x      ;set bowser to move and face to the right
            ram.enemyMovingDirs[x] = 0x01
            //> lda #$02
            //> sta BowserMovementSpeed    ;set movement speed
            ram.bowserMovementSpeed = 0x02
            //> lda #$20
            //> sta EnemyFrameTimer,x      ;set timer here
            ram.timers[0x0a + x] = 0x20
            //> sta BowserFireBreathTimer  ;set timer used for bowser's flame
            ram.bowserFireBreathTimer = 0x20
            //> lda Enemy_X_Position,x
            //> cmp #$c8                   ;if bowser to the right past a certain point,
            //> bcs HammerChk              ;skip ahead to some other section
            val bowserX = ram.sprObjXPos[sprObjOfs].toInt() and 0xFF
            if (bowserX >= 0xC8) {
                skipToHammerCheck = true
            }
        }
    }

    if (!skipToHammerCheck) {
        //> GetPRCmp:  lda FrameCounter           ;get frame counter
        //> and #%00000011
        //> bne HammerChk              ;execute this code every fourth frame, otherwise branch
        if (ram.frameCounter.toInt() and 0x03 == 0) {
            bowserMovementLogic(x, sprObjOfs)
        }
    }

    //> HammerChk:
    hammerCheck(x)
}

/**
 * Bowser's horizontal movement logic: manages position relative to origin point,
 * direction changes, and pseudorandom movement range.
 */
private fun System.bowserMovementLogic(x: Int, sprObjOfs: Int) {
    //> lda Enemy_X_Position,x
    //> cmp BowserOrigXPos         ;if bowser not at original horizontal position,
    //> bne GetDToO                ;branch to skip this part
    val bowserX = ram.sprObjXPos[sprObjOfs].toInt() and 0xFF
    val origX = ram.bowserOrigXPos.toInt() and 0xFF
    if (bowserX == origX) {
        //> lda PseudoRandomBitReg,x
        //> and #%00000011             ;get pseudorandom offset
        //> tay
        val randOffset = ram.pseudoRandomBitReg[x].toInt() and 0x03
        //> lda PRandomRange,y         ;load value using pseudorandom offset
        //> sta MaxRangeFromOrigin     ;and store here
        ram.maxRangeFromOrigin = PRandomRange[randOffset]
    }

    //> GetDToO:   lda Enemy_X_Position,x
    //> clc                        ;add movement speed to bowser's horizontal
    //> adc BowserMovementSpeed    ;coordinate and save as new horizontal position
    //> sta Enemy_X_Position,x
    val newX = (bowserX + (ram.bowserMovementSpeed.toInt() and 0xFF)) and 0xFF
    ram.sprObjXPos[sprObjOfs] = newX.toByte()

    //> ldy Enemy_MovingDir,x
    //> cpy #$01                   ;if bowser moving and facing to the right, skip ahead
    //> beq HammerChk
    val movingDir = ram.enemyMovingDirs[x].toInt() and 0xFF
    if (movingDir == 0x01) return // moving right, skip distance check

    //> ldy #$ff                   ;set default movement speed here (move left)
    var newSpeed = 0xFF
    //> sec                        ;get difference of current vs. original
    //> sbc BowserOrigXPos         ;horizontal position
    var diff = (newX - origX) and 0xFF
    //> bpl CompDToO               ;if current position to the right of original, skip ahead
    if (diff and 0x80 != 0) {
        //> eor #$ff
        //> clc                        ;get two's complement
        //> adc #$01
        diff = ((diff xor 0xFF) + 1) and 0xFF
        //> ldy #$01                   ;set alternate movement speed here (move right)
        newSpeed = 0x01
    }

    //> CompDToO:  cmp MaxRangeFromOrigin     ;compare difference with pseudorandom value
    //> bcc HammerChk              ;if difference < pseudorandom value, leave speed alone
    val maxRange = ram.maxRangeFromOrigin.toInt() and 0xFF
    if (diff >= maxRange) {
        //> sty BowserMovementSpeed    ;otherwise change bowser's movement speed
        ram.bowserMovementSpeed = newSpeed.toByte()
    }
}

/**
 * Handles hammer throwing (world 6+) and bowser's vertical movement/jump.
 * If the enemy frame timer is expired, bowser moves downward and may throw hammers.
 * If the timer is about to expire (==1), bowser initiates a jump.
 */
private fun System.hammerCheck(x: Int) {
    val sprObjOfs = x + 1

    //> HammerChk: lda EnemyFrameTimer,x      ;if timer set here not expired yet, skip ahead to
    //> bne MakeBJump              ;some other section of code
    val timer = ram.timers[0x0a + x].toInt() and 0xFF
    if (timer != 0) {
        //> MakeBJump: cmp #$01                   ;if timer not yet about to expire,
        //> bne ChkFireB               ;skip ahead to next part
        if (timer == 0x01) {
            //> dec Enemy_Y_Position,x     ;otherwise decrement vertical coordinate
            ram.sprObjYPos[sprObjOfs] = (ram.sprObjYPos[sprObjOfs] - 1).toByte()
            //> jsr InitVStf               ;initialize movement amount
            initVStf(sprObjOfs)
            //> lda #$fe
            //> sta Enemy_Y_Speed,x        ;set vertical speed to move bowser upwards
            ram.sprObjYSpeed[sprObjOfs] = 0xFE.toByte()
        }
        //> ChkFireB:
        checkFireBreath(x)
        return
    }

    //> jsr MoveEnemySlowVert      ;otherwise start by moving bowser downwards
    moveEnemySlowVert(sprObjOfs)

    //> lda WorldNumber            ;check world number
    //> cmp #World6
    //> bcc SetHmrTmr              ;if world 1-5, skip this part (not time to throw hammers yet)
    val world = ram.worldNumber.toInt() and 0xFF
    if (world >= Constants.World6) {
        //> lda FrameCounter
        //> and #%00000011             ;check to see if it's time to execute sub
        //> bne SetHmrTmr              ;if not, skip sub, otherwise
        if (ram.frameCounter.toInt() and 0x03 == 0) {
            //> jsr SpawnHammerObj         ;execute sub on every fourth frame to spawn misc object (hammer)
            spawnHammerObj()
        }
    }

    //> SetHmrTmr: lda Enemy_Y_Position,x     ;get current vertical position
    //> cmp #$80                   ;if still above a certain point
    //> bcc ChkFireB               ;then skip to world number check for flames
    val yPos = ram.sprObjYPos[sprObjOfs].toInt() and 0xFF
    if (yPos < 0x80) {
        checkFireBreath(x)
        return
    }

    //> lda PseudoRandomBitReg,x
    //> and #%00000011             ;get pseudorandom offset
    //> tay
    val randOffset = ram.pseudoRandomBitReg[x].toInt() and 0x03
    //> lda PRandomRange,y         ;get value using pseudorandom offset
    //> sta EnemyFrameTimer,x      ;set for timer here
    ram.timers[0x0a + x] = PRandomRange[randOffset]

    //> SkipToFB:  jmp ChkFireB               ;jump to execute flames code
    checkFireBreath(x)
}

/**
 * Checks whether Bowser should spawn a flame based on world number and timer.
 * Worlds 1-5 and world 7: no flames. Worlds 6, 8: flames enabled.
 * Toggles Bowser's mouth open/closed and spawns BowserFlame into the frenzy buffer.
 */
private fun System.checkFireBreath(x: Int) {
    //> ChkFireB:  lda WorldNumber            ;check world number here
    //> cmp #World8                ;world 8?
    //> beq SpawnFBr               ;if so, execute this part here
    val world = ram.worldNumber.toInt() and 0xFF
    if (world != Constants.World8.toInt()) {
        //> cmp #World6                ;world 6-7?
        //> bcs BowserGfxHandler       ;if so, skip this part here
        if (world >= Constants.World6) {
            // World 6 or 7: no flames, go straight to gfx
            bowserGfxHandler()
            return
        }
        // Worlds 1-5: fall through to SpawnFBr (flames enabled for world 8 path and below 6)
        // Actually, worlds 1-5 also fall through to SpawnFBr which checks the timer.
        // The original assembly: if world != 8 AND world >= 6 -> skip to gfx.
        // So worlds < 6 (1-5) fall through to SpawnFBr, and world 8 also falls through.
    }

    //> SpawnFBr:  lda BowserFireBreathTimer  ;check timer here
    //> bne BowserGfxHandler       ;if not expired yet, skip all of this
    if (ram.bowserFireBreathTimer != 0.toByte()) {
        bowserGfxHandler()
        return
    }

    // Timer expired: toggle mouth open/close and potentially spawn flame
    //> lda #$20
    //> sta BowserFireBreathTimer  ;set timer here
    ram.bowserFireBreathTimer = 0x20
    //> lda BowserBodyControls
    //> eor #%10000000             ;invert bowser's mouth bit to open
    //> sta BowserBodyControls     ;and close bowser's mouth
    ram.bowserBodyControls = (ram.bowserBodyControls.toInt() xor 0x80).toByte()

    //> bmi ChkFireB               ;if bowser's mouth open (d7 set), loop back
    if (ram.bowserBodyControls.toInt() and 0x80 != 0) {
        // Mouth just opened: loop back to check fire breath again
        // In the original assembly this creates a loop. In practice, the timer
        // was just set to $20 so the bne check above will exit on the next call.
        // We can just call bowserGfxHandler since the timer is now non-zero.
        bowserGfxHandler()
        return
    }

    // Mouth just closed: spawn flame
    //> jsr SetFlameTimer          ;get timing for bowser's flame
    var flameTime = setFlameTimer()

    //> ldy SecondaryHardMode
    //> beq SetFBTmr               ;if secondary hard mode flag not set, skip this
    if (ram.secondaryHardMode != 0.toByte()) {
        //> sec
        //> sbc #$10                   ;otherwise subtract from value in A
        flameTime = (flameTime - 0x10) and 0xFF
    }

    //> SetFBTmr:  sta BowserFireBreathTimer  ;set value as timer here
    ram.bowserFireBreathTimer = flameTime.toByte()
    //> lda #BowserFlame           ;put bowser's flame identifier
    //> sta EnemyFrenzyBuffer      ;in enemy frenzy buffer
    ram.enemyFrenzyBuffer = EnemyId.BowserFlame.byte

    bowserGfxHandler()
}

// -------------------------------------------------------------------------------------
// BowserGfxHandler - Draws Bowser's front and rear halves
// -------------------------------------------------------------------------------------

/**
 * Processes both halves of Bowser's sprite. Bowser is composed of two enemy objects:
 * a front half at the current ObjectOffset and a rear half at DuplicateObj_Offset.
 * Positions the rear relative to the front, copies state, and runs collision detection.
 */
fun System.bowserGfxHandler() {
    val x = ram.objectOffset.toInt()

    //> BowserGfxHandler:
    //> jsr ProcessBowserHalf    ;do a sub here to process bowser's front
    processBowserHalf()

    //> ldy #$10                 ;load default value here to position bowser's rear
    //> lda Enemy_MovingDir,x    ;check moving direction
    //> lsr
    //> bcc CopyFToR             ;if moving left, use default
    //> ldy #$f0                 ;otherwise load alternate positioning value here
    val movingDir = ram.enemyMovingDirs[x].toInt() and 0xFF
    val rearOffset = if (movingDir and 0x01 != 0) 0xF0 else 0x10

    //> CopyFToR: tya                      ;move bowser's rear object position value to A
    //> clc
    //> adc Enemy_X_Position,x   ;add to bowser's front object horizontal coordinate
    val frontSprOfs = x + 1
    val frontX = ram.sprObjXPos[frontSprOfs].toInt() and 0xFF
    val rearX = (frontX + rearOffset) and 0xFF

    //> ldy DuplicateObj_Offset  ;get bowser's rear object offset
    val dupOffset = ram.duplicateObjOffset.toInt() and 0xFF
    val dupSprOfs = dupOffset + 1

    //> sta Enemy_X_Position,y   ;store A as bowser's rear horizontal coordinate
    ram.sprObjXPos[dupSprOfs] = rearX.toByte()

    //> lda Enemy_Y_Position,x
    //> clc                      ;add eight pixels to bowser's front object
    //> adc #$08                 ;vertical coordinate and store as vertical coordinate
    //> sta Enemy_Y_Position,y   ;for bowser's rear
    val frontY = ram.sprObjYPos[frontSprOfs].toInt() and 0xFF
    ram.sprObjYPos[dupSprOfs] = ((frontY + 0x08) and 0xFF).toByte()

    //> lda Enemy_State,x
    //> sta Enemy_State,y        ;copy enemy state directly from front to rear
    ram.enemyState[dupOffset] = ram.enemyState[x]

    //> lda Enemy_MovingDir,x
    //> sta Enemy_MovingDir,y    ;copy moving direction also
    ram.enemyMovingDirs[dupOffset] = ram.enemyMovingDirs[x]

    //> lda ObjectOffset         ;save enemy object offset of front to stack
    //> pha
    val savedOffset = ram.objectOffset

    //> ldx DuplicateObj_Offset  ;put enemy object offset of rear as current
    //> stx ObjectOffset
    ram.objectOffset = dupOffset.toByte()

    //> lda #Bowser              ;set bowser's enemy identifier
    //> sta Enemy_ID,x           ;store in bowser's rear object
    ram.enemyID[dupOffset] = EnemyId.Bowser.byte

    //> jsr ProcessBowserHalf    ;do a sub here to process bowser's rear
    processBowserHalf()

    //> pla
    //> sta ObjectOffset         ;get original enemy object offset
    //> tax
    ram.objectOffset = savedOffset

    //> lda #$00                 ;nullify bowser's front/rear graphics flag
    //> sta BowserGfxFlag
    ram.bowserGfxFlag = 0

    //> ExBGfxH:  rts                      ;leave!
}

/**
 * Processes one half of Bowser (front or rear): gets offscreen bits,
 * relative position, draws via EnemyGfxHandler, and runs collision detection
 * if in normal state.
 * @return true if processing should continue (enemy in normal state), false to exit early
 */
private fun System.processBowserHalf(): Boolean {
    //> ProcessBowserHalf:
    //> inc BowserGfxFlag         ;increment bowser's graphics flag, then run subroutines
    ram.bowserGfxFlag = (ram.bowserGfxFlag + 1).toByte()

    //> jsr RunRetainerObj        ;to get offscreen bits, relative position and draw bowser
    // RunRetainerObj = GetEnemyOffscreenBits + RelativeEnemyPosition + EnemyGfxHandler
    getEnemyOffscreenBits()
    relativeEnemyPosition()
    enemyGfxHandler()

    val x = ram.objectOffset.toInt()
    //> lda Enemy_State,x
    //> bne ExBGfxH               ;if either enemy object not in normal state, branch to leave
    if (ram.enemyState[x] != 0.toByte()) {
        return false
    }

    //> lda #$0a
    //> sta Enemy_BoundBoxCtrl,x  ;set bounding box size control
    ram.enemyBoundBoxCtrls[x] = 0x0a
    //> jsr GetEnemyBoundBox      ;get bounding box coordinates
    getEnemyBoundBox()
    //> jmp PlayerEnemyCollision  ;do player-to-enemy collision detection
    playerEnemyCollision()
    return true
}

// -------------------------------------------------------------------------------------
// SetFlameTimer - Gets timing value for Bowser's flame from lookup table
// -------------------------------------------------------------------------------------

/**
 * Returns the next flame timer value from a cycling 8-entry table.
 * Increments the flame timer control index (wrapping 0-7).
 * @return the flame timer value (unsigned)
 */
internal fun System.setFlameTimer(): Int {
    //> SetFlameTimer:
    //> ldy BowserFlameTimerCtrl  ;load counter as offset
    val y = ram.bowserFlameTimerCtrl.toInt() and 0xFF
    //> inc BowserFlameTimerCtrl  ;increment
    //> lda BowserFlameTimerCtrl  ;mask out all but 3 LSB
    //> and #%00000111            ;to keep in range of 0-7
    //> sta BowserFlameTimerCtrl
    ram.bowserFlameTimerCtrl = (((ram.bowserFlameTimerCtrl + 1).toInt()) and 0x07).toByte()
    //> lda FlameTimerData,y      ;load value to be used then leave
    return FlameTimerData[y].toInt() and 0xFF
}

// -------------------------------------------------------------------------------------
// ProcBowserFlame - Processes Bowser's flame movement and rendering
// -------------------------------------------------------------------------------------

/**
 * Processes a Bowser flame object: moves it leftward with optional vertical tracking,
 * draws the 3-sprite flame graphic, and handles offscreen cleanup.
 */
fun System.procBowserFlame() {
    val x = ram.objectOffset.toInt()
    val sprObjOfs = x + 1

    //> ProcBowserFlame:
    //> lda TimerControl            ;if master timer control flag set,
    //> bne SetGfxF                 ;skip all of this
    if (ram.timerControl == 0.toByte()) {
        //> lda #$40                    ;load default movement force
        //> ldy SecondaryHardMode
        //> beq SFlmX                   ;if secondary hard mode flag not set, use default
        //> lda #$60                    ;otherwise load alternate movement force to go faster
        val moveForce = if (ram.secondaryHardMode != 0.toByte()) 0x60 else 0x40

        //> SFlmX:   sta $00                     ;store value here
        //> lda Enemy_X_MoveForce,x
        //> sec                         ;subtract value from movement force
        //> sbc $00
        //> sta Enemy_X_MoveForce,x     ;save new value
        val xMoveForce = ram.sprObjXMoveForce[sprObjOfs].toInt() and 0xFF
        val forceResult = xMoveForce - moveForce
        ram.sprObjXMoveForce[sprObjOfs] = (forceResult and 0xFF).toByte()
        val borrow1 = if (forceResult < 0) 1 else 0

        //> lda Enemy_X_Position,x
        //> sbc #$01                    ;subtract one from horizontal position to move
        //> sta Enemy_X_Position,x      ;to the left
        val xPos = ram.sprObjXPos[sprObjOfs].toInt() and 0xFF
        val xPosResult = xPos - 0x01 - borrow1
        ram.sprObjXPos[sprObjOfs] = (xPosResult and 0xFF).toByte()
        val borrow2 = if (xPosResult < 0) 1 else 0

        //> lda Enemy_PageLoc,x
        //> sbc #$00                    ;subtract borrow from page location
        //> sta Enemy_PageLoc,x
        val pageLoc = ram.sprObjPageLoc[sprObjOfs].toInt() and 0xFF
        ram.sprObjPageLoc[sprObjOfs] = ((pageLoc - borrow2) and 0xFF).toByte()

        //> ldy BowserFlamePRandomOfs,x ;get some value here and use as offset
        // BowserFlamePRandomOfs is at $0417 which is sprObjYMFDummy base $0416 + 1
        // Indexed by enemy slot: BowserFlamePRandomOfs,x = sprObjYMFDummy[x+1]
        val flamePRandomOfs = ram.sprObjYMFDummy[sprObjOfs].toInt() and 0xFF

        //> lda Enemy_Y_Position,x      ;load vertical coordinate
        //> cmp FlameYPosData,y         ;compare against coordinate data
        //> beq SetGfxF                 ;if equal, branch and do not modify coordinate
        val yPos = ram.sprObjYPos[sprObjOfs].toInt() and 0xFF
        val targetY = FlameYPosData.getOrElse(flamePRandomOfs) { 0x90.toByte() }.toInt() and 0xFF
        if (yPos != targetY) {
            //> clc
            //> adc Enemy_Y_MoveForce,x     ;otherwise add value here to coordinate and store
            //> sta Enemy_Y_Position,x      ;as new vertical coordinate
            val yMoveForce = ram.sprObjYMoveForce[sprObjOfs].toInt() and 0xFF
            ram.sprObjYPos[sprObjOfs] = ((yPos + yMoveForce) and 0xFF).toByte()
        }
    }

    //> SetGfxF: jsr RelativeEnemyPosition   ;get new relative coordinates
    relativeEnemyPosition()

    //> lda Enemy_State,x           ;if bowser's flame not in normal state,
    //> bne ExFl                    ;branch to leave
    if (ram.enemyState[x] != 0.toByte()) return

    //> lda #$51                    ;otherwise, continue
    //> sta $00                     ;write first tile number
    var tileNum = 0x51

    //> ldy #$02                    ;load attributes without vertical flip by default
    //> lda FrameCounter
    //> and #%00000010              ;invert vertical flip bit every 2 frames
    //> beq FlmeAt                  ;if d1 not set, write default value
    //> ldy #$82                    ;otherwise write value with vertical flip bit set
    val attribs = if (ram.frameCounter.toInt() and 0x02 != 0) 0x82 else 0x02

    //> FlmeAt:  sty $01                     ;set bowser's flame sprite attributes here
    //> ldy Enemy_SprDataOffset,x   ;get OAM data offset
    val sprDataOffset = (ram.enemySprDataOffset[x].toInt() and 0xFF) shr 2

    //> ldx #$00
    //> DrawFlameLoop:
    val relY = ram.enemyRelYPos.toInt() and 0xFF
    var relX = ram.enemyRelXPos.toInt() and 0xFF
    for (i in 0 until 3) {
        val sprIdx = sprDataOffset + i
        //> lda Enemy_Rel_YPos         ;get Y relative coordinate of current enemy object
        //> sta Sprite_Y_Position,y    ;write into Y coordinate of OAM data
        ram.sprites[sprIdx].y = relY.toUByte()
        //> lda $00
        //> sta Sprite_Tilenumber,y    ;write current tile number into OAM data
        ram.sprites[sprIdx].tilenumber = tileNum.toByte()
        //> inc $00                    ;increment tile number to draw more bowser's flame
        tileNum++
        //> lda $01
        //> sta Sprite_Attributes,y    ;write saved attributes into OAM data
        ram.sprites[sprIdx].attributes = SpriteFlags(attribs.toByte())
        //> lda Enemy_Rel_XPos
        //> sta Sprite_X_Position,y    ;write X relative coordinate
        ram.sprites[sprIdx].x = relX.toUByte()
        //> clc
        //> adc #$08
        //> sta Enemy_Rel_XPos         ;then add eight to it and store
        relX = (relX + 0x08) and 0xFF
        ram.enemyRelXPos = relX.toByte()
    }

    //> ldx ObjectOffset           ;reload original enemy offset
    //> jsr GetEnemyOffscreenBits  ;get offscreen information
    getEnemyOffscreenBits()

    //> ldy Enemy_SprDataOffset,x  ;get OAM data offset
    //> lda Enemy_OffscreenBits    ;get enemy object offscreen bits
    val offscreenBits = ram.enemyOffscreenBits.toInt() and 0xFF

    //> lsr                        ;move d0 to carry
    //> bcc M3FOfs                 ;branch if carry not set
    if (offscreenBits and 0x01 != 0) {
        //> lda #$f8                   ;otherwise move sprite offscreen
        //> sta Sprite_Y_Position+12,y ;residual since flame is only three sprites
        ram.sprites[sprDataOffset + 3].y = 0xF8u
    }

    //> lsr                        ;move d1 to carry
    //> bcc M2FOfs                 ;branch if carry not set
    if (offscreenBits and 0x02 != 0) {
        //> lda #$f8                   ;otherwise move third sprite offscreen
        //> sta Sprite_Y_Position+8,y
        ram.sprites[sprDataOffset + 2].y = 0xF8u
    }

    //> lsr                        ;move d2 to carry
    //> bcc M1FOfs                 ;branch if carry not set
    if (offscreenBits and 0x04 != 0) {
        //> lda #$f8                   ;otherwise move second sprite offscreen
        //> sta Sprite_Y_Position+4,y
        ram.sprites[sprDataOffset + 1].y = 0xF8u
    }

    //> lsr                        ;move d3 to carry
    //> bcc ExFlmeD                ;branch if carry not set
    if (offscreenBits and 0x08 != 0) {
        //> lda #$f8
        //> sta Sprite_Y_Position,y    ;otherwise move first sprite offscreen
        ram.sprites[sprDataOffset].y = 0xF8u
    }

    //> ExFlmeD: rts                        ;leave
}

// -------------------------------------------------------------------------------------
// Helper subroutines (private to this file)
// -------------------------------------------------------------------------------------

/**
 * Moves defeated Bowser downward slowly using gravity, then draws.
 * Called when Bowser's state has d5 set (hit by 5 fireballs) and
 * vertical position is above $E0.
 */
private fun System.moveDefeatedBowser() {
    //> MoveD_Bowser:
    //> jsr MoveEnemySlowVert     ;do a sub to move bowser downwards
    val sprObjOfs = ram.objectOffset.toInt() + 1
    moveEnemySlowVert(sprObjOfs)
    //> jmp BowserGfxHandler      ;jump to draw bowser's front and rear
    bowserGfxHandler()
}

/**
 * Moves an enemy object downward slowly with gravity.
 * Uses downForce=$0f and maxSpeed=$02.
 * Mirrors MoveEnemySlowVert from assembly.
 */
private fun System.moveEnemySlowVert(sprObjOfs: Int) {
    //> MoveEnemySlowVert:
    //> ldy #$0f         ;set movement amount
    //> SetMdMax: lda #$02         ;set maximum speed
    imposeGravitySprObj(sprObjOffset = sprObjOfs, downForce = 0x0f, maxSpeed = 0x02)
}

/**
 * Initializes vertical speed and movement force to zero for the given SprObject offset.
 * Mirrors InitVStf from assembly.
 */
private fun System.initVStf(sprObjOfs: Int) {
    //> InitVStf:
    //> lda #$00
    //> sta Enemy_Y_Speed,x
    ram.sprObjYSpeed[sprObjOfs] = 0
    //> sta Enemy_Y_MoveForce,x
    ram.sprObjYMoveForce[sprObjOfs] = 0
    //> rts
}

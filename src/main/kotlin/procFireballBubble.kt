// by Claude - ProcFireball_Bubble and FireballObjCore subroutines
// Translates fireball creation, movement, and air bubble processing.
package com.ivieleague.smbtranslation

//> FireballXSpdData:
//> .db $40, $c0
private val fireballXSpdData = intArrayOf(0x40, 0xC0)

//> Bubble_MForceData:
//> .db $ff, $50
private val bubbleMForceData = intArrayOf(0xFF, 0x50)

//> BubbleTimerData:
//> .db $40, $20
private val bubbleTimerData = intArrayOf(0x40, 0x20)

/**
 * Main entry point for fireball and air bubble processing.
 * Creates fireballs when B is pressed (if player is fiery),
 * then processes existing fireballs and air bubbles.
 */
fun System.procFireballBubble() {
    //> ProcFireball_Bubble:
    //> lda PlayerStatus           ;check player's status
    //> cmp #$02
    //> bcc ProcAirBubbles         ;if not fiery, branch
    if ((ram.playerStatus.toInt() and 0xFF) >= 2) {
        //> lda A_B_Buttons
        //> and #B_Button              ;check for b button pressed
        //> beq ProcFireballs          ;branch if not pressed
        val bPressed = (ram.aBButtons.toInt() and Constants.B_Button.toInt()) != 0
        if (bPressed) {
            //> and PreviousA_B_Buttons
            //> bne ProcFireballs          ;if button pressed in previous frame, branch
            val bPrev = (ram.previousABButtons.toInt() and Constants.B_Button.toInt()) != 0
            if (!bPrev) {
                tryCreateFireball()
            }
        }

        //> ProcFireballs:
        //> ldx #$00; jsr FireballObjCore  ;process first fireball object
        fireballObjCore(0)
        //> ldx #$01; jsr FireballObjCore  ;process second fireball object
        fireballObjCore(1)
    }

    //> ProcAirBubbles:
    //> lda AreaType                ;if not water type level, skip the rest
    //> bne BublExit
    if (ram.areaType != 0.toByte()) return

    //> ldx #$02
    //> BublLoop:
    for (x in 2 downTo 0) {
        //> stx ObjectOffset
        ram.objectOffset = x.toByte()
        //> jsr BubbleCheck             ;check timers and coordinates, create air bubble
        bubbleCheck(x)
        //> jsr RelativeBubblePosition  ;get relative coordinates
        relativeBubblePosition()
        //> jsr GetBubbleOffscreenBits  ;get offscreen information
        getBubbleOffscreenBits()
        //> jsr DrawBubble              ;draw the air bubble
        drawBubble()
        //> dex; bpl BublLoop
    }
    //> BublExit: rts
}

/**
 * Attempts to create a fireball when B is newly pressed.
 */
private fun System.tryCreateFireball() {
    //> lda FireballCounter        ;load fireball counter
    //> and #%00000001             ;get LSB and use as offset for buffer
    //> tax
    val fbIdx = ram.fireballCounter.toInt() and 0x01

    //> lda Fireball_State,x       ;load fireball state
    //> bne ProcFireballs          ;if not inactive, branch
    if (ram.fireballStates[fbIdx] != 0.toByte()) return

    //> ldy Player_Y_HighPos       ;if player too high or too low, branch
    //> dey
    //> bne ProcFireballs
    if ((ram.sprObjYHighPos[0].toInt() and 0xFF) != 1) return

    //> lda CrouchingFlag          ;if player crouching, branch
    //> bne ProcFireballs
    if (ram.crouchingFlag != 0.toByte()) return

    //> lda Player_State           ;if player's state = climbing, branch
    //> cmp #$03
    //> beq ProcFireballs
    if (ram.playerState == 3.toByte()) return

    //> lda #Sfx_Fireball          ;play fireball sound effect
    //> sta Square1SoundQueue
    ram.square1SoundQueue = Constants.Sfx_Fireball
    //> lda #$02                   ;load state
    //> sta Fireball_State,x
    ram.fireballStates[fbIdx] = 0x02
    //> ldy PlayerAnimTimerSet     ;copy animation frame timer setting
    //> sty FireballThrowingTimer  ;into fireball throwing timer
    ram.fireballThrowingTimer = ram.playerAnimTimerSet
    //> dey
    //> sty PlayerAnimTimer        ;decrement and store in player's animation timer
    ram.playerAnimTimer = (ram.playerAnimTimerSet.toInt() - 1).toByte()
    //> inc FireballCounter        ;increment fireball counter
    ram.fireballCounter = (ram.fireballCounter + 1).toByte()
}

/**
 * Core fireball processing for fireball at index [fbIdx] (0 or 1).
 */
private fun System.fireballObjCore(fbIdx: Int) {
    //> FireballObjCore:
    //> stx ObjectOffset             ;store offset as current object
    ram.objectOffset = fbIdx.toByte()

    //> lda Fireball_State,x         ;check for d7 = 1
    //> asl
    //> bcs FireballExplosion        ;if so, branch to get relative coordinates and draw explosion
    val fbState = ram.fireballStates[fbIdx].toInt() and 0xFF
    if ((fbState and 0x80) != 0) {
        //> FireballExplosion:
        //> jsr RelativeFireballPosition
        relativeFireballPosition()
        //> jmp DrawExplosion_Fireball
        drawExplosionFireball()
        return
    }

    //> ldy Fireball_State,x         ;if fireball inactive, branch to leave
    //> beq NoFBall
    if (fbState == 0) return

    //> dey                          ;if fireball state set to 1, skip this part and just run it
    //> beq RunFB
    if (fbState != 1) {
        // State 2: initialize fireball position from player
        //> lda Player_X_Position        ;get player's horizontal position
        //> adc #$04                     ;add four pixels
        //> sta Fireball_X_Position,x
        val playerX = ram.playerXPosition.toInt() and 0xFF
        val fbX = playerX + 4  // carry flag state from asl above is unpredictable, use +4
        ram.sprObjXPos[7 + fbIdx] = fbX.toByte()
        //> lda Player_PageLoc           ;get player's page location
        //> adc #$00                     ;add carry
        //> sta Fireball_PageLoc,x
        ram.sprObjPageLoc[7 + fbIdx] = ((ram.sprObjPageLoc[0].toInt() and 0xFF) + (if (fbX > 0xFF) 1 else 0)).toByte()
        //> lda Player_Y_Position        ;get player's vertical position
        //> sta Fireball_Y_Position,x
        ram.sprObjYPos[7 + fbIdx] = ram.sprObjYPos[0]
        //> lda #$01
        //> sta Fireball_Y_HighPos,x     ;set high byte of vertical position
        ram.sprObjYHighPos[7 + fbIdx] = 1
        //> ldy PlayerFacingDir          ;get player's facing direction
        //> dey                          ;decrement to use as offset
        val facingIdx = (ram.playerFacingDir.toInt() and 0xFF) - 1
        //> lda FireballXSpdData,y       ;set horizontal speed accordingly
        //> sta Fireball_X_Speed,x
        // by Claude - PlayerFacingDir is always 1 or 2 in normal gameplay, giving index 0 or 1
        ram.sprObjXSpeed[7 + fbIdx] = fireballXSpdData.getOrElse(facingIdx) { 0 }.toByte()
        //> lda #$04
        //> sta Fireball_Y_Speed,x       ;set vertical speed of fireball
        ram.sprObjYSpeed[7 + fbIdx] = 0x04
        //> lda #$07
        //> sta Fireball_BoundBoxCtrl,x  ;set bounding box size control
        ram.fireballBoundBoxCtrls[fbIdx] = 0x07
        //> dec Fireball_State,x         ;decrement state to 1
        ram.fireballStates[fbIdx] = (fbState - 1).toByte()
    }

    //> RunFB:
    //> txa; clc; adc #$07; tax     ;add 7 to offset for fireball SprObject offset
    val sprObjOfs = fbIdx + 7
    //> lda #$50; sta $00            ;set downward movement force
    //> lda #$03; sta $02            ;set maximum speed
    //> lda #$00                     ;A=0 for down-only direction
    //> jsr ImposeGravity
    imposeGravity(
        sprObjOffset = sprObjOfs,
        downForce = 0x50,
        upForce = 0,
        maxSpeed = 0x03,
        bidirectional = false
    )
    //> jsr MoveObjectHorizontally   ;move fireball horizontally
    moveObjectHorizontally(sprObjOfs)

    //> ldx ObjectOffset             ;return fireball offset to X
    //> jsr RelativeFireballPosition ;get relative coordinates
    relativeFireballPosition()
    //> jsr GetFireballOffscreenBits ;get offscreen information
    getFireballOffscreenBits()
    //> jsr GetFireballBoundBox      ;get bounding box coordinates
    getFireballBoundBox()
    //> jsr FireballBGCollision      ;do fireball to background collision detection
    fireballBGCollision()

    //> lda FBall_OffscreenBits      ;get fireball offscreen bits
    //> and #%11001100               ;mask out certain bits
    //> bne EraseFB                  ;if any bits still set, branch to kill fireball
    val fbOffBits = ram.offscrBits[2].toInt() and 0xFF  // fireball is condensed offset 2
    if ((fbOffBits and 0xCC) != 0) {
        //> EraseFB: lda #$00; sta Fireball_State,x
        ram.fireballStates[fbIdx] = 0
        return
    }

    //> jsr FireballEnemyCollision   ;do fireball to enemy collision detection
    fireballEnemyCollision()
    //> jmp DrawFireball             ;draw fireball and leave
    drawFireball()
    //> NoFBall: rts
}

/**
 * Checks air bubble timers and creates/moves air bubbles.
 */
private fun System.bubbleCheck(x: Int) {
    //> BubbleCheck:
    //> lda PseudoRandomBitReg+1,x  ;get part of LSFR
    //> and #$01
    //> sta $07                     ;store pseudorandom bit
    val randBit = ram.pseudoRandomBitReg[1 + x].toInt() and 0x01

    //> lda Bubble_Y_Position,x     ;get vertical coordinate for air bubble
    //> cmp #$f8                    ;if offscreen coordinate not set,
    //> bne MoveBubl                ;branch to move air bubble
    val bubbleY = ram.sprObjYPos[22 + x].toInt() and 0xFF
    if (bubbleY == 0xF8) {
        //> lda AirBubbleTimer          ;if air bubble timer not expired,
        //> bne ExitBubl                ;branch to leave
        if (ram.airBubbleTimer != 0.toByte()) return

        //> SetupBubble:
        setupBubble(x, randBit)
    }

    // MoveBubl:
    moveBubble(x, randBit)
}

private fun System.setupBubble(x: Int, randBit: Int) {
    //> SetupBubble:
    //> ldy #$00                 ;load default value here
    //> lda PlayerFacingDir      ;get player's facing direction
    //> lsr                      ;move d0 to carry
    //> bcc PosBubl              ;branch to use default value if facing left
    //> ldy #$08                 ;otherwise load alternate value here
    val adder = if ((ram.playerFacingDir.toInt() and 0x01) != 0) 0x08 else 0x00

    //> PosBubl: tya             ;use value loaded as adder
    //> adc Player_X_Position    ;add to player's horizontal position
    val playerX = ram.playerXPosition.toInt() and 0xFF
    val bubbleXResult = adder + playerX  // carry from lsr above
    //> sta Bubble_X_Position,x
    ram.sprObjXPos[22 + x] = bubbleXResult.toByte()
    //> lda Player_PageLoc
    //> adc #$00                 ;add carry to player's page location
    //> sta Bubble_PageLoc,x
    ram.sprObjPageLoc[22 + x] = ((ram.sprObjPageLoc[0].toInt() and 0xFF) + (if (bubbleXResult > 0xFF) 1 else 0)).toByte()

    //> lda Player_Y_Position
    //> clc
    //> adc #$08                 ;add eight pixels to player's vertical position
    //> sta Bubble_Y_Position,x
    ram.sprObjYPos[22 + x] = ((ram.sprObjYPos[0].toInt() and 0xFF) + 8).toByte()
    //> lda #$01
    //> sta Bubble_Y_HighPos,x   ;set vertical high byte for air bubble
    ram.sprObjYHighPos[22 + x] = 1

    //> ldy $07                  ;get pseudorandom bit, use as offset
    //> lda BubbleTimerData,y    ;get data for air bubble timer
    //> sta AirBubbleTimer
    ram.airBubbleTimer = bubbleTimerData[randBit].toByte()
}

private fun System.moveBubble(x: Int, randBit: Int) {
    //> MoveBubl:
    //> ldy $07                  ;get pseudorandom bit again, use as offset
    //> lda Bubble_YMF_Dummy,x
    //> sec                      ;subtract pseudorandom amount from dummy variable
    //> sbc Bubble_MForceData,y
    val dummyVal = ram.sprObjYMFDummy[22 + x].toInt() and 0xFF
    val forceVal = bubbleMForceData[randBit]
    val dummyResult = dummyVal - forceVal
    //> sta Bubble_YMF_Dummy,x
    ram.sprObjYMFDummy[22 + x] = dummyResult.toByte()

    val borrow = if (dummyResult < 0) 1 else 0
    //> lda Bubble_Y_Position,x
    //> sbc #$00                 ;subtract borrow from air bubble's vertical coordinate
    val bubbleY = ram.sprObjYPos[22 + x].toInt() and 0xFF
    val newBubbleY = bubbleY - borrow
    //> cmp #$20                 ;if below the status bar,
    //> bcs Y_Bubl               ;branch to go ahead and use
    val finalY = if ((newBubbleY and 0xFF) >= 0x20) {
        newBubbleY and 0xFF
    } else {
        //> lda #$f8             ;otherwise set offscreen coordinate
        0xF8
    }
    //> Y_Bubl: sta Bubble_Y_Position,x
    ram.sprObjYPos[22 + x] = finalY.toByte()
    //> ExitBubl: rts
}

// drawBubble() moved to drawRoutines.kt
// drawFireball() moved to drawRoutines.kt
// drawExplosionFireball() moved to drawRoutines.kt
// getFireballBoundBox() moved to boundingBox.kt
// fireballBGCollision() moved to collisionDetection.kt
// fireballEnemyCollision() moved to collisionDetection.kt

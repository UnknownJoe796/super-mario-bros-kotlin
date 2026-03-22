// by Claude - Gravity and vertical movement subroutines
// Translates ImposeGravity, ImposeGravitySprObj, ImposeGravityBlock,
// MoveD_EnemyVertically, MoveFallingPlatform, MovePlayerVertically.
package com.ivieleague.smbtranslation

/**
 * Core gravity routine. Applies vertical force accumulation, position update,
 * acceleration via downward force, and optional upward deceleration.
 *
 * @param sprObjOffset index into SprObject backing arrays
 * @param downForce amount to add to Y_MoveForce each frame (accelerates downward)
 * @param upForce amount to subtract from Y_MoveForce when bidirectional (decelerates upward)
 * @param maxSpeed maximum vertical speed magnitude (signed, positive = downward limit)
 * @param bidirectional if true, also apply upward deceleration; if false, only downward gravity
 */
fun System.imposeGravity(
    sprObjOffset: Int,
    downForce: Int,
    upForce: Int,
    maxSpeed: Int,
    bidirectional: Boolean
) {
    //> ImposeGravity:
    //> pha                          ;push direction flag

    //> --- Step 1: Fractional position accumulation ---
    //> lda SprObject_YMF_Dummy,x
    //> clc
    //> adc SprObject_Y_MoveForce,x
    //> sta SprObject_YMF_Dummy,x
    val fracResult = (ram.sprObjYMFDummy[sprObjOffset].toInt() and 0xFF) +
            (ram.sprObjYMoveForce[sprObjOffset].toInt() and 0xFF)
    ram.sprObjYMFDummy[sprObjOffset] = fracResult.toByte()
    val fracCarry = if (fracResult > 0xFF) 1 else 0

    //> --- Step 2: Position update with sign-extended speed ---
    //> ldy #$00
    //> lda SprObject_Y_Speed,x
    //> bpl AlterYP                  ;if moving downward (positive), Y=0
    //> dey                          ;if moving upward (negative), Y=$FF
    //> AlterYP: sty $07
    val ySpeed = ram.sprObjYSpeed[sprObjOffset]
    val signExt = if (ySpeed < 0) 0xFF else 0x00

    //> adc SprObject_Y_Position,x   ;speed + carry + position
    //> sta SprObject_Y_Position,x
    val posResult = (ram.sprObjYPos[sprObjOffset].toInt() and 0xFF) +
            (ySpeed.toInt() and 0xFF) + fracCarry
    ram.sprObjYPos[sprObjOffset] = posResult.toByte()
    val posCarry = if (posResult > 0xFF) 1 else 0

    //> lda SprObject_Y_HighPos,x
    //> adc $07                      ;carry + sign extension + high byte
    //> sta SprObject_Y_HighPos,x
    val highResult = (ram.sprObjYHighPos[sprObjOffset].toInt() and 0xFF) + signExt + posCarry
    ram.sprObjYHighPos[sprObjOffset] = highResult.toByte()

    //> --- Step 3: Acceleration - add downward force ---
    //> lda SprObject_Y_MoveForce,x
    //> clc
    //> adc $00                      ;add downward force to move force
    //> sta SprObject_Y_MoveForce,x
    val forceResult = (ram.sprObjYMoveForce[sprObjOffset].toInt() and 0xFF) + (downForce and 0xFF)
    ram.sprObjYMoveForce[sprObjOffset] = (forceResult and 0xFF).toByte()
    val forceCarry = if (forceResult > 0xFF) 1 else 0

    //> lda SprObject_Y_Speed,x
    //> adc #$00                     ;add carry to speed
    //> sta SprObject_Y_Speed,x
    val newSpeed = (ram.sprObjYSpeed[sprObjOffset].toInt() and 0xFF) + forceCarry
    ram.sprObjYSpeed[sprObjOffset] = (newSpeed and 0xFF).toByte()

    //> --- Step 4: Clamp to maximum downward speed ---
    //> cmp $02                      ;compare speed to max (signed)
    //> bmi ChkUpM                   ;if speed < max, skip clamping
    val currentSpeed = ram.sprObjYSpeed[sprObjOffset]  // re-read as signed
    val maxSpeedByte = maxSpeed.toByte()
    if (currentSpeed >= maxSpeedByte) {
        //> lda SprObject_Y_MoveForce,x
        //> cmp #$80                 ;if force < $80, skip clamping
        //> bcc ChkUpM
        if ((ram.sprObjYMoveForce[sprObjOffset].toInt() and 0xFF) >= 0x80) {
            //> lda $02; sta SprObject_Y_Speed,x
            ram.sprObjYSpeed[sprObjOffset] = maxSpeedByte
            //> lda #$00; sta SprObject_Y_MoveForce,x
            ram.sprObjYMoveForce[sprObjOffset] = 0
        }
    }

    //> --- Step 5: Optional upward deceleration ---
    //> ChkUpM: pla                  ;get direction flag
    //> eor #%11111111               ;otherwise get two's compliment of maximum speed
    //> beq ExVMove                  ;if 0 (down only), done
    if (!bidirectional) return

    //> lda $02; eor #$FF; tay; iny  ;negate max speed (two's complement)
    //> sty $07
    val negMaxSpeed = (maxSpeed.toByte().toInt().inv() + 1).toByte()  // -maxSpeed

    //> lda SprObject_Y_MoveForce,x
    //> sec
    //> sbc $01                      ;subtract upward force from move force
    //> sta SprObject_Y_MoveForce,x
    val upForceResult = (ram.sprObjYMoveForce[sprObjOffset].toInt() and 0xFF) - (upForce and 0xFF)
    ram.sprObjYMoveForce[sprObjOffset] = upForceResult.toByte()
    val upBorrow = if (upForceResult < 0) 1 else 0

    //> lda SprObject_Y_Speed,x
    //> sbc #$00                     ;subtract borrow from speed
    //> sta SprObject_Y_Speed,x
    val decelSpeed = (ram.sprObjYSpeed[sprObjOffset].toInt() and 0xFF) - upBorrow
    ram.sprObjYSpeed[sprObjOffset] = decelSpeed.toByte()

    //> --- Step 6: Clamp to maximum upward speed ---
    //> cmp $07                      ;compare speed to -max (signed)
    //> bpl ExVMove                  ;if speed >= -max, done
    val currentSpeedUp = ram.sprObjYSpeed[sprObjOffset]
    if (currentSpeedUp < negMaxSpeed) {
        //> lda SprObject_Y_MoveForce,x
        //> cmp #$80                 ;if force >= $80, done
        //> bcs ExVMove
        if ((ram.sprObjYMoveForce[sprObjOffset].toInt() and 0xFF) < 0x80) {
            //> lda $07; sta SprObject_Y_Speed,x
            ram.sprObjYSpeed[sprObjOffset] = negMaxSpeed
            //> lda #$ff; sta SprObject_Y_MoveForce,x
            ram.sprObjYMoveForce[sprObjOffset] = 0xFF.toByte()
        }
    }
    //> ExVMove: rts
}

/**
 * ImposeGravitySprObj: Downward-only gravity with caller-specified max speed and force.
 * Used via SetHiMax for enemy downward movement.
 */
fun System.imposeGravitySprObj(sprObjOffset: Int, downForce: Int, maxSpeed: Int) {
    //> ImposeGravitySprObj:
    //> sta $02            ;set maximum speed
    //> lda #$00           ;push 0 for down-only direction
    //> jmp ImposeGravity
    imposeGravity(sprObjOffset, downForce, upForce = 0, maxSpeed, bidirectional = false)
}

/**
 * ImposeGravityBlock: Gravity for block objects (bouncing bricks/coins).
 * Uses fixed downForce=$50 and maxSpeed=$08.
 */
fun System.imposeGravityBlock(sprObjOffset: Int) {
    //> ImposeGravityBlock:
    //> ldy #$01
    //> lda #$50; sta $00           ;downForce = $50
    //> lda MaxSpdBlockData,y       ;maxSpeed = MaxSpdBlockData[1] = $08
    //> (falls into ImposeGravitySprObj)
    imposeGravitySprObj(sprObjOffset, downForce = 0x50, maxSpeed = 0x08)
}

/**
 * MoveD_EnemyVertically: Moves an enemy downward with gravity.
 * Uses downForce=$3d normally, $20 for spiny's egg state.
 * MaxSpeed is always $03.
 */
fun System.moveD_EnemyVertically() {
    //> MoveD_EnemyVertically:
    //> ldy #$3d
    //> lda Enemy_State,x
    //> cmp #$05
    //> bne ContVMove
    val x = ram.objectOffset.toInt()
    val downForce = if (ram.enemyState[x] == 0x05.toByte()) {
        //> MoveFallingPlatform: ldy #$20
        0x20
    } else {
        0x3d
    }
    //> ContVMove: jmp SetHiMax
    //> SetHiMax: lda #$03; sty $00; inx; jsr ImposeGravitySprObj
    //> .db $06, $08
    //> MaxSpdBlockData:
    //> ResidualGravityCode:
    //> .db $2c        ;no code branches or jumps to it...
    imposeGravitySprObj(sprObjOffset = x + 1, downForce = downForce, maxSpeed = 0x03)
}

/**
 * MoveFallingPlatform: Variant of MoveD_EnemyVertically with downForce=$20.
 */
fun System.moveFallingPlatform() {
    //> MoveFallingPlatform: ldy #$20; ContVMove: jmp SetHiMax
    val x = ram.objectOffset.toInt()
    imposeGravitySprObj(sprObjOffset = x + 1, downForce = 0x20, maxSpeed = 0x03)
}

/**
 * MovePlayerVertically: Applies gravity to the player.
 * Skipped if jumpspring is animating (unless timer control is set).
 */
fun System.movePlayerVertically() {
    //> MovePlayerVertically:
    //> ldx #$00
    //> lda TimerControl
    //> bne NoJSChk             ;if master timer control set, skip jumpspring check
    if (ram.timerControl == 0.toByte()) {
        //> lda JumpspringAnimCtrl
        //> bne ExXMove             ;if jumpspring animating, leave
        if (ram.jumpspringAnimCtrl != 0.toByte()) return
    }
    //> NoJSChk: lda VerticalForce; sta $00
    //> lda #$04; jmp ImposeGravitySprObj
    imposeGravitySprObj(sprObjOffset = 0, downForce = ram.verticalForce.toInt() and 0xFF, maxSpeed = 0x04)
}

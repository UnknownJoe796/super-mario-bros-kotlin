// by Claude - ProcHammerObj, SpawnHammerObj, and DrawHammer subroutines
// Translates hammer throwing logic for Hammer Bros from smbdism.asm (lines ~6866-6957).
// IMPORTANT: The stubs in miscObjectsCore.kt (lines 111-112) for procHammerObj() and
// getMiscBoundBox() must be removed for these real implementations to be called.
package com.ivieleague.smbtranslation

// NOTE: GameRam.hammerEnemyOffset is declared as a single Byte but in the NES it's
// a 9-byte array at $06AE indexed by misc object slot (0..8).
// GameRam needs hammerEnemyOffsets: ByteArray(9) to fully support this.
// For now, we use a module-level backing array.
// TODO: Move this array into GameRam and remove this workaround.
private val hammerEnemyOffsets = ByteArray(9)

//> HammerEnemyOfsData:
//>       .db $04, $04, $04, $05, $05, $05
//>       .db $06, $06, $06
private val HammerEnemyOfsData = byteArrayOf(
    0x04, 0x04, 0x04, 0x05, 0x05, 0x05,
    0x06, 0x06, 0x06
)

//> HammerXSpdData:
//>       .db $10, $f0
private val HammerXSpdData = byteArrayOf(
    0x10, 0xf0.toByte()
)

/**
 * Attempts to spawn a hammer object from the current enemy (Hammer Bro).
 * Uses pseudorandom bits to select a misc object slot for the hammer.
 * @return true if a hammer was spawned (carry set), false otherwise (carry clear)
 */
fun System.spawnHammerObj(): Boolean {
    //> SpawnHammerObj:
    //> lda PseudoRandomBitReg+1 ;get pseudorandom bits from
    //> and #%00000111           ;second part of LSFR
    var offset = ram.pseudoRandomBitReg[1].toInt() and 0b00000111
    //> bne SetMOfs              ;if any bits are set, branch and use as offset
    if (offset == 0) {
        //> lda PseudoRandomBitReg+1
        //> and #%00001000           ;get d3 from same part of LSFR
        offset = ram.pseudoRandomBitReg[1].toInt() and 0b00001000
    }
    //> SetMOfs:  tay                      ;use either d3 or d2-d0 for offset here
    val y = offset
    //> lda Misc_State,y         ;if any values loaded in
    //> bne NoHammer             ;$2a-$32 where offset is then leave with carry clear
    if (ram.miscStates[y] != 0.toByte()) {
        //> NoHammer: ldx ObjectOffset         ;get original enemy object offset
        //> clc                      ;return with carry clear
        return false
    }
    //> ldx HammerEnemyOfsData,y ;get offset of enemy slot to check using Y as offset
    val enemySlot = HammerEnemyOfsData[y].toInt() and 0xFF
    //> lda Enemy_Flag,x         ;check enemy buffer flag at offset
    //> bne NoHammer             ;if buffer flag set, branch to leave with carry clear
    if (ram.enemyFlags.getOrElse(enemySlot) { 0 } != 0.toByte()) {
        return false
    }
    //> ldx ObjectOffset         ;get original enemy object offset
    val x = ram.objectOffset.toInt()
    //> txa
    //> sta HammerEnemyOffset,y  ;save here
    hammerEnemyOffsets[y] = x.toByte()
    //> lda #$90
    //> sta Misc_State,y         ;save hammer's state here
    ram.miscStates[y] = 0x90.toByte()
    //> lda #$07
    //> sta Misc_BoundBoxCtrl,y  ;set something else entirely, here
    // NOTE: Misc_BoundBoxCtrl is an indexed field at $04A2+y.
    // GameRam.miscBoundBoxCtrl is a single byte; we'd need an array for proper indexed access.
    // For now, only write to the single field (effective for the active misc object).
    // TODO: GameRam needs miscBoundBoxCtrls: ByteArray(9) for proper indexed access.
    ram.miscBoundBoxCtrl = 0x07
    //> sec                      ;return with carry set
    //> rts
    return true
}

/**
 * Processes a hammer misc object: handles state transitions, movement, and rendering.
 * Called from MiscObjectsCore when a misc object has d7 set in its state (hammer).
 */
fun System.procHammerObj() {
    //> ProcHammerObj:
    val x = ram.objectOffset.toInt()
    val miscOfs = 13 + x  // SprObject offset for misc objects

    //> lda TimerControl           ;if master timer control set
    //> bne RunHSubs               ;skip all of this code and go to last subs at the end
    if (ram.timerControl != 0.toByte()) {
        //> RunHSubs:
        runHammerSubs()
        return
    }

    //> lda Misc_State,x           ;otherwise get hammer's state
    //> and #%01111111             ;mask out d7
    val state = ram.miscStates[x].toInt() and 0x7F
    //> ldy HammerEnemyOffset,x    ;get enemy object offset that spawned this hammer
    val enemyOfs = hammerEnemyOffsets[x].toInt() and 0xFF

    //> cmp #$02                   ;check hammer's state
    //> beq SetHSpd                ;if currently at 2, branch
    //> bcs SetHPos                ;if greater than 2, branch elsewhere
    when {
        state < 2 -> {
            // State 0 or 1: hammer is in flight, apply gravity and move horizontally
            //> txa
            //> clc                        ;add 13 bytes to use
            //> adc #$0d                   ;proper misc object
            //> tax                        ;return offset to X
            //> lda #$10
            //> sta $00                    ;set downward movement force
            //> lda #$0f
            //> sta $01                    ;set upward movement force (not used)
            //> lda #$04
            //> sta $02                    ;set maximum vertical speed
            //> lda #$00                   ;set A to impose gravity on hammer
            //> jsr ImposeGravity          ;do sub to impose gravity on hammer and move vertically
            imposeGravity(
                sprObjOffset = miscOfs,
                downForce = 0x10,
                upForce = 0x0F,
                maxSpeed = 0x04,
                bidirectional = false  // A=0 means down-only
            )
            //> jsr MoveObjectHorizontally ;do sub to move it horizontally
            moveObjectHorizontally(miscOfs)
            //> ldx ObjectOffset           ;get original misc object offset
            //> jmp RunAllH                ;branch to essential subroutines
            //> RunAllH:  jsr PlayerHammerCollision  ;handle collisions
            playerHammerCollision()
            //> (falls through to RunHSubs)
            runHammerSubs()
        }
        state == 2 -> {
            //> SetHSpd:
            //> lda #$fe
            //> sta Misc_Y_Speed,x         ;set hammer's vertical speed
            ram.sprObjYSpeed[miscOfs] = 0xfe.toByte()
            //> lda Enemy_State,y          ;get enemy object state
            //> and #%11110111             ;mask out d3
            //> sta Enemy_State,y          ;store new state
            ram.enemyState[enemyOfs] = (ram.enemyState[enemyOfs].toInt() and 0b11110111).toByte()
            //> ldx Enemy_MovingDir,y      ;get enemy's moving direction
            //> dex                        ;decrement to use as offset
            val dirOfs = (ram.enemyMovingDirs[enemyOfs].toInt() and 0xFF) - 1
            //> lda HammerXSpdData,x       ;get proper speed to use based on moving direction
            val hammerSpeed = HammerXSpdData[dirOfs]
            //> ldx ObjectOffset           ;reobtain hammer's buffer offset
            //> sta Misc_X_Speed,x         ;set hammer's horizontal speed
            ram.sprObjXSpeed[miscOfs] = hammerSpeed

            //> (falls through to SetHPos)
            setHammerPos(x, miscOfs, enemyOfs)
            runHammerSubs()
        }
        else -> {
            // State > 2: position the hammer at the enemy and decrement state
            //> SetHPos:
            setHammerPos(x, miscOfs, enemyOfs)
            // Skip PlayerHammerCollision, go straight to RunHSubs
            //> bne RunHSubs               ;unconditional branch to skip first routine
            runHammerSubs()
        }
    }
}

/**
 * Positions the hammer relative to its parent enemy and decrements hammer state.
 */
private fun System.setHammerPos(x: Int, miscOfs: Int, enemyOfs: Int) {
    //> SetHPos:
    //> dec Misc_State,x           ;decrement hammer's state
    ram.miscStates[x] = (ram.miscStates[x] - 1).toByte()
    //> lda Enemy_X_Position,y     ;get enemy's horizontal position
    //> clc
    //> adc #$02                   ;set position 2 pixels to the right
    //> sta Misc_X_Position,x      ;store as hammer's horizontal position
    val enemySprOfs = 1 + enemyOfs  // Enemy SprObject offset
    val enemyXPos = ram.sprObjXPos[enemySprOfs].toInt() and 0xFF
    val newXPos = enemyXPos + 2
    ram.sprObjXPos[miscOfs] = newXPos.toByte()
    //> lda Enemy_PageLoc,y        ;get enemy's page location
    //> adc #$00                   ;add carry
    //> sta Misc_PageLoc,x         ;store as hammer's page location
    val carry = if (newXPos > 0xFF) 1 else 0
    ram.sprObjPageLoc[miscOfs] = ((ram.sprObjPageLoc[enemySprOfs].toInt() and 0xFF) + carry).toByte()
    //> lda Enemy_Y_Position,y     ;get enemy's vertical position
    //> sec
    //> sbc #$0a                   ;move position 10 pixels upward
    //> sta Misc_Y_Position,x      ;store as hammer's vertical position
    val enemyYPos = ram.sprObjYPos[enemySprOfs].toInt() and 0xFF
    ram.sprObjYPos[miscOfs] = (enemyYPos - 0x0a).toByte()
    //> lda #$01
    //> sta Misc_Y_HighPos,x       ;set hammer's vertical high byte
    ram.sprObjYHighPos[miscOfs] = 0x01
}

/**
 * Runs the standard subroutines for the hammer object: offscreen bits,
 * relative position, bounding box, and drawing.
 */
private fun System.runHammerSubs() {
    //> RunHSubs: jsr GetMiscOffscreenBits   ;get offscreen information
    getMiscOffscreenBits()
    //> jsr RelativeMiscPosition   ;get relative coordinates
    relativeMiscPosition()
    //> jsr GetMiscBoundBox        ;get bounding box coordinates
    getMiscBoundBox()
    //> jsr DrawHammer             ;draw the hammer
    drawHammer()
    //> rts
}

//> ;-------------------------------------------------------------------------------------
//> ;data for the hammer dynamic sprite rendering

//> FirstSprXPos:
//>       .db $04, $00, $04, $00
private val FirstSprXPos = byteArrayOf(0x04, 0x00, 0x04, 0x00)

//> FirstSprYPos:
//>       .db $00, $04, $00, $04
private val FirstSprYPos = byteArrayOf(0x00, 0x04, 0x00, 0x04)

//> SecondSprXPos:
//>       .db $00, $08, $00, $08
private val SecondSprXPos = byteArrayOf(0x00, 0x08, 0x00, 0x08)

//> SecondSprYPos:
//>       .db $08, $00, $08, $00
private val SecondSprYPos = byteArrayOf(0x08, 0x00, 0x08, 0x00)

//> FirstSprTilenum:
//>       .db $80, $82, $81, $83
private val FirstSprTilenum = byteArrayOf(0x80.toByte(), 0x82.toByte(), 0x81.toByte(), 0x83.toByte())

//> SecondSprTilenum:
//>       .db $81, $83, $80, $82
private val SecondSprTilenum = byteArrayOf(0x81.toByte(), 0x83.toByte(), 0x80.toByte(), 0x82.toByte())

//> HammerSprAttrib:
//>       .db $03, $03, $c3, $c3
private val HammerSprAttrib = byteArrayOf(0x03, 0x03, 0xc3.toByte(), 0xc3.toByte())

/**
 * Draws the hammer using two sprites with frame-based animation.
 * If the hammer is offscreen, its state is cleared.
 */
fun System.drawHammer() {
    //> DrawHammer:
    val x = ram.objectOffset.toInt()
    //> ldy Misc_SprDataOffset,x    ;get misc object OAM data offset
    val y = ram.miscSprDataOffsets[x].toInt() and 0xFF

    //> lda TimerControl
    //> bne ForceHPose              ;if master timer control set, skip this part
    val poseIndex: Int
    if (ram.timerControl != 0.toByte()) {
        //> ForceHPose: ldx #$00                    ;reset offset here
        //> beq RenderH                 ;do unconditional branch to rendering part
        poseIndex = 0
    } else {
        //> lda Misc_State,x            ;otherwise get hammer's state
        //> and #%01111111              ;mask out d7
        //> cmp #$01                    ;check to see if set to 1 yet
        //> beq GetHPose                ;if so, branch
        val state = ram.miscStates[x].toInt() and 0x7F
        if (state == 1) {
            //> GetHPose:   lda FrameCounter            ;get frame counter
            //> lsr                         ;move d3-d2 to d1-d0
            //> lsr
            //> and #%00000011              ;mask out all but d1-d0 (changes every four frames)
            //> tax                         ;use as timing offset
            poseIndex = (ram.frameCounter.toInt() ushr 2) and 0x03
        } else {
            //> ForceHPose: ldx #$00
            poseIndex = 0
        }
    }

    //> RenderH:
    //> lda Misc_Rel_YPos           ;get relative vertical coordinate
    val relY = ram.miscRelYPos.toInt() and 0xFF
    //> clc
    //> adc FirstSprYPos,x          ;add first sprite vertical adder based on offset
    //> sta Sprite_Y_Position,y     ;store as sprite Y coordinate for first sprite
    val spr1Y = (relY + (FirstSprYPos[poseIndex].toInt() and 0xFF)).toByte().toUByte()
    ram.sprites[y].y = spr1Y
    //> clc
    //> adc SecondSprYPos,x         ;add second sprite vertical adder based on offset
    //> sta Sprite_Y_Position+4,y   ;store as sprite Y coordinate for second sprite
    val spr2Y = ((spr1Y.toInt() and 0xFF) + (SecondSprYPos[poseIndex].toInt() and 0xFF)).toByte().toUByte()
    ram.sprites[y + 1].y = spr2Y

    //> lda Misc_Rel_XPos           ;get relative horizontal coordinate
    val relX = ram.miscRelXPos.toInt() and 0xFF
    //> clc
    //> adc FirstSprXPos,x          ;add first sprite horizontal adder based on offset
    //> sta Sprite_X_Position,y     ;store as sprite X coordinate for first sprite
    val spr1X = (relX + (FirstSprXPos[poseIndex].toInt() and 0xFF)).toByte().toUByte()
    ram.sprites[y].x = spr1X
    //> clc
    //> adc SecondSprXPos,x         ;add second sprite horizontal adder based on offset
    //> sta Sprite_X_Position+4,y   ;store as sprite X coordinate for second sprite
    val spr2X = ((spr1X.toInt() and 0xFF) + (SecondSprXPos[poseIndex].toInt() and 0xFF)).toByte().toUByte()
    ram.sprites[y + 1].x = spr2X

    //> lda FirstSprTilenum,x
    //> sta Sprite_Tilenumber,y     ;get and store tile number of first sprite
    ram.sprites[y].tilenumber = FirstSprTilenum[poseIndex]
    //> lda SecondSprTilenum,x
    //> sta Sprite_Tilenumber+4,y   ;get and store tile number of second sprite
    ram.sprites[y + 1].tilenumber = SecondSprTilenum[poseIndex]

    //> lda HammerSprAttrib,x
    //> sta Sprite_Attributes,y     ;get and store attribute bytes for both
    //> sta Sprite_Attributes+4,y   ;note in this case they use the same data
    val attrib = com.ivieleague.smbtranslation.utils.SpriteFlags(HammerSprAttrib[poseIndex])
    ram.sprites[y].attributes = attrib
    ram.sprites[y + 1].attributes = attrib

    //> ldx ObjectOffset            ;get misc object offset
    //> lda Misc_OffscreenBits
    //> and #%11111100              ;check offscreen bits
    //> beq NoHOffscr               ;if all bits clear, leave object alone
    if ((ram.miscOffscreenBits.toInt() and 0b11111100) != 0) {
        //> lda #$00
        //> sta Misc_State,x            ;otherwise nullify misc object state
        ram.miscStates[x] = 0
        //> lda #$f8
        //> jsr DumpTwoSpr              ;do sub to move hammer sprites offscreen
        ram.sprites[y].y = 0xf8u
        ram.sprites[y + 1].y = 0xf8u
    }
    //> NoHOffscr:  rts
}

// --- Stubs for subroutines not yet translated ---
// playerHammerCollision() moved to collisionDetection.kt

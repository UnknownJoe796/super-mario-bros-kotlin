// by Claude - EnemyGfxHandler subroutine: draws all enemy sprites
package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.*
import kotlin.experimental.and
import kotlin.experimental.or

// -------------------------------------------------------------------------------------
// Enemy graphics tables from assembly
// -------------------------------------------------------------------------------------

//> ;tiles arranged in top left, right, middle left, right, bottom left, right order
//> EnemyGraphicsTable:
//>       .db $fc, $fc, $aa, $ab, $ac, $ad  ;buzzy beetle frame 1
//>       .db $fc, $fc, $ae, $af, $b0, $b1  ;             frame 2
//>       .db $fc, $a5, $a6, $a7, $a8, $a9  ;koopa troopa frame 1
//>       .db $fc, $a0, $a1, $a2, $a3, $a4  ;             frame 2
//>       .db $69, $a5, $6a, $a7, $a8, $a9  ;koopa paratroopa frame 1
//>       .db $6b, $a0, $6c, $a2, $a3, $a4  ;                 frame 2
//>       .db $fc, $fc, $96, $97, $98, $99  ;spiny frame 1
//>       .db $fc, $fc, $9a, $9b, $9c, $9d  ;      frame 2
//>       .db $fc, $fc, $8f, $8e, $8e, $8f  ;spiny's egg frame 1
//>       .db $fc, $fc, $95, $94, $94, $95  ;            frame 2
//>       .db $fc, $fc, $dc, $dc, $df, $df  ;bloober frame 1
//>       .db $dc, $dc, $dd, $dd, $de, $de  ;        frame 2
//>       .db $fc, $fc, $b2, $b3, $b4, $b5  ;cheep-cheep frame 1
//>       .db $fc, $fc, $b6, $b3, $b7, $b5  ;            frame 2
//>       .db $fc, $fc, $70, $71, $72, $73  ;goomba
//>       .db $fc, $fc, $6e, $6e, $6f, $6f  ;koopa shell frame 1 (upside-down)
//>       .db $fc, $fc, $6d, $6d, $6f, $6f  ;            frame 2
//>       .db $fc, $fc, $6f, $6f, $6e, $6e  ;koopa shell frame 1 (rightsideup)
//>       .db $fc, $fc, $6f, $6f, $6d, $6d  ;            frame 2
//>       .db $fc, $fc, $f4, $f4, $f5, $f5  ;buzzy beetle shell frame 1 (rightsideup)
//>       .db $fc, $fc, $f4, $f4, $f5, $f5  ;                   frame 2
//>       .db $fc, $fc, $f5, $f5, $f4, $f4  ;buzzy beetle shell frame 1 (upside-down)
//>       .db $fc, $fc, $f5, $f5, $f4, $f4  ;                   frame 2
//>       .db $fc, $fc, $fc, $fc, $ef, $ef  ;defeated goomba
//>       .db $b9, $b8, $bb, $ba, $bc, $bc  ;lakitu frame 1
//>       .db $fc, $fc, $bd, $bd, $bc, $bc  ;       frame 2
//>       .db $7a, $7b, $da, $db, $d8, $d8  ;princess
//>       .db $cd, $cd, $ce, $ce, $cf, $cf  ;mushroom retainer
//>       .db $7d, $7c, $d1, $8c, $d3, $d2  ;hammer bro frame 1
//>       .db $7d, $7c, $89, $88, $8b, $8a  ;           frame 2
//>       .db $d5, $d4, $e3, $e2, $d3, $d2  ;           frame 3
//>       .db $d5, $d4, $e3, $e2, $8b, $8a  ;           frame 4
//>       .db $e5, $e5, $e6, $e6, $eb, $eb  ;piranha plant frame 1
//>       .db $ec, $ec, $ed, $ed, $ee, $ee  ;              frame 2
//>       .db $fc, $fc, $d0, $d0, $d7, $d7  ;podoboo
//>       .db $bf, $be, $c1, $c0, $c2, $fc  ;bowser front frame 1
//>       .db $c4, $c3, $c6, $c5, $c8, $c7  ;bowser rear frame 1
//>       .db $bf, $be, $ca, $c9, $c2, $fc  ;       front frame 2
//>       .db $c4, $c3, $c6, $c5, $cc, $cb  ;       rear frame 2
//>       .db $fc, $fc, $e8, $e7, $ea, $e9  ;bullet bill
//>       .db $f2, $f2, $f3, $f3, $f2, $f2  ;jumpspring frame 1
//>       .db $f1, $f1, $f1, $f1, $fc, $fc  ;           frame 2
//>       .db $f0, $f0, $fc, $fc, $fc, $fc  ;           frame 3
private val EnemyGraphicsTable = byteArrayOf(
    0xfc.toByte(), 0xfc.toByte(), 0xaa.toByte(), 0xab.toByte(), 0xac.toByte(), 0xad.toByte(),
    0xfc.toByte(), 0xfc.toByte(), 0xae.toByte(), 0xaf.toByte(), 0xb0.toByte(), 0xb1.toByte(),
    0xfc.toByte(), 0xa5.toByte(), 0xa6.toByte(), 0xa7.toByte(), 0xa8.toByte(), 0xa9.toByte(),
    0xfc.toByte(), 0xa0.toByte(), 0xa1.toByte(), 0xa2.toByte(), 0xa3.toByte(), 0xa4.toByte(),
    0x69, 0xa5.toByte(), 0x6a, 0xa7.toByte(), 0xa8.toByte(), 0xa9.toByte(),
    0x6b, 0xa0.toByte(), 0x6c, 0xa2.toByte(), 0xa3.toByte(), 0xa4.toByte(),
    0xfc.toByte(), 0xfc.toByte(), 0x96.toByte(), 0x97.toByte(), 0x98.toByte(), 0x99.toByte(),
    0xfc.toByte(), 0xfc.toByte(), 0x9a.toByte(), 0x9b.toByte(), 0x9c.toByte(), 0x9d.toByte(),
    0xfc.toByte(), 0xfc.toByte(), 0x8f.toByte(), 0x8e.toByte(), 0x8e.toByte(), 0x8f.toByte(),
    0xfc.toByte(), 0xfc.toByte(), 0x95.toByte(), 0x94.toByte(), 0x94.toByte(), 0x95.toByte(),
    0xfc.toByte(), 0xfc.toByte(), 0xdc.toByte(), 0xdc.toByte(), 0xdf.toByte(), 0xdf.toByte(),
    0xdc.toByte(), 0xdc.toByte(), 0xdd.toByte(), 0xdd.toByte(), 0xde.toByte(), 0xde.toByte(),
    0xfc.toByte(), 0xfc.toByte(), 0xb2.toByte(), 0xb3.toByte(), 0xb4.toByte(), 0xb5.toByte(),
    0xfc.toByte(), 0xfc.toByte(), 0xb6.toByte(), 0xb3.toByte(), 0xb7.toByte(), 0xb5.toByte(),
    0xfc.toByte(), 0xfc.toByte(), 0x70, 0x71, 0x72, 0x73,
    0xfc.toByte(), 0xfc.toByte(), 0x6e, 0x6e, 0x6f, 0x6f,
    0xfc.toByte(), 0xfc.toByte(), 0x6d, 0x6d, 0x6f, 0x6f,
    0xfc.toByte(), 0xfc.toByte(), 0x6f, 0x6f, 0x6e, 0x6e,
    0xfc.toByte(), 0xfc.toByte(), 0x6f, 0x6f, 0x6d, 0x6d,
    0xfc.toByte(), 0xfc.toByte(), 0xf4.toByte(), 0xf4.toByte(), 0xf5.toByte(), 0xf5.toByte(),
    0xfc.toByte(), 0xfc.toByte(), 0xf4.toByte(), 0xf4.toByte(), 0xf5.toByte(), 0xf5.toByte(),
    0xfc.toByte(), 0xfc.toByte(), 0xf5.toByte(), 0xf5.toByte(), 0xf4.toByte(), 0xf4.toByte(),
    0xfc.toByte(), 0xfc.toByte(), 0xf5.toByte(), 0xf5.toByte(), 0xf4.toByte(), 0xf4.toByte(),
    0xfc.toByte(), 0xfc.toByte(), 0xfc.toByte(), 0xfc.toByte(), 0xef.toByte(), 0xef.toByte(),
    0xb9.toByte(), 0xb8.toByte(), 0xbb.toByte(), 0xba.toByte(), 0xbc.toByte(), 0xbc.toByte(),
    0xfc.toByte(), 0xfc.toByte(), 0xbd.toByte(), 0xbd.toByte(), 0xbc.toByte(), 0xbc.toByte(),
    0x7a, 0x7b, 0xda.toByte(), 0xdb.toByte(), 0xd8.toByte(), 0xd8.toByte(),
    0xcd.toByte(), 0xcd.toByte(), 0xce.toByte(), 0xce.toByte(), 0xcf.toByte(), 0xcf.toByte(),
    0x7d, 0x7c, 0xd1.toByte(), 0x8c.toByte(), 0xd3.toByte(), 0xd2.toByte(),
    0x7d, 0x7c, 0x89.toByte(), 0x88.toByte(), 0x8b.toByte(), 0x8a.toByte(),
    0xd5.toByte(), 0xd4.toByte(), 0xe3.toByte(), 0xe2.toByte(), 0xd3.toByte(), 0xd2.toByte(),
    0xd5.toByte(), 0xd4.toByte(), 0xe3.toByte(), 0xe2.toByte(), 0x8b.toByte(), 0x8a.toByte(),
    0xe5.toByte(), 0xe5.toByte(), 0xe6.toByte(), 0xe6.toByte(), 0xeb.toByte(), 0xeb.toByte(),
    0xec.toByte(), 0xec.toByte(), 0xed.toByte(), 0xed.toByte(), 0xee.toByte(), 0xee.toByte(),
    0xfc.toByte(), 0xfc.toByte(), 0xd0.toByte(), 0xd0.toByte(), 0xd7.toByte(), 0xd7.toByte(),
    0xbf.toByte(), 0xbe.toByte(), 0xc1.toByte(), 0xc0.toByte(), 0xc2.toByte(), 0xfc.toByte(),
    0xc4.toByte(), 0xc3.toByte(), 0xc6.toByte(), 0xc5.toByte(), 0xc8.toByte(), 0xc7.toByte(),
    0xbf.toByte(), 0xbe.toByte(), 0xca.toByte(), 0xc9.toByte(), 0xc2.toByte(), 0xfc.toByte(),
    0xc4.toByte(), 0xc3.toByte(), 0xc6.toByte(), 0xc5.toByte(), 0xcc.toByte(), 0xcb.toByte(),
    0xfc.toByte(), 0xfc.toByte(), 0xe8.toByte(), 0xe7.toByte(), 0xea.toByte(), 0xe9.toByte(),
    0xf2.toByte(), 0xf2.toByte(), 0xf3.toByte(), 0xf3.toByte(), 0xf2.toByte(), 0xf2.toByte(),
    0xf1.toByte(), 0xf1.toByte(), 0xf1.toByte(), 0xf1.toByte(), 0xfc.toByte(), 0xfc.toByte(),
    0xf0.toByte(), 0xf0.toByte(), 0xfc.toByte(), 0xfc.toByte(), 0xfc.toByte(), 0xfc.toByte()
)

//> EnemyGfxTableOffsets:
//>       .db $0c, $0c, $00, $0c, $0c, $a8, $54, $3c
//>       .db $ea, $18, $48, $48, $cc, $c0, $18, $18
//>       .db $18, $90, $24, $ff, $48, $9c, $d2, $d8
//>       .db $f0, $f6, $fc
private val EnemyGfxTableOffsets = intArrayOf(
    0x0c, 0x0c, 0x00, 0x0c, 0x0c, 0xa8, 0x54, 0x3c,
    0xea, 0x18, 0x48, 0x48, 0xcc, 0xc0, 0x18, 0x18,
    0x18, 0x90, 0x24, 0xff, 0x48, 0x9c, 0xd2, 0xd8,
    0xf0, 0xf6, 0xfc
)

//> EnemyAttributeData:
//>       .db $01, $02, $03, $02, $01, $01, $03, $03
//>       .db $03, $01, $01, $02, $02, $21, $01, $02
//>       .db $01, $01, $02, $ff, $02, $02, $01, $01
//>       .db $02, $02, $02
private val EnemyAttributeData = intArrayOf(
    0x01, 0x02, 0x03, 0x02, 0x01, 0x01, 0x03, 0x03,
    0x03, 0x01, 0x01, 0x02, 0x02, 0x21, 0x01, 0x02,
    0x01, 0x01, 0x02, 0xff, 0x02, 0x02, 0x01, 0x01,
    0x02, 0x02, 0x02
)

//> EnemyAnimTimingBMask:
//>       .db $08, $18
private val EnemyAnimTimingBMask = intArrayOf(0x08, 0x18)

//> JumpspringFrameOffsets:
//>       .db $18, $19, $1a, $19, $18
private val JumpspringFrameOffsets = intArrayOf(0x18, 0x19, 0x1a, 0x19, 0x18)

// -------------------------------------------------------------------------------------
// EnemyGfxHandler - draws enemy sprites
// -------------------------------------------------------------------------------------

/**
 * Draws the current enemy object's sprites. Handles all enemy types including
 * Bowser, Goombas, Koopas, Piranha Plants, etc., with animation and defeat states.
 */
fun System.enemyGfxHandler() {
    //> EnemyGfxHandler:
    val objectX = ram.objectOffset.toInt()

    //> lda Enemy_Y_Position,x      ;get enemy object vertical position
    //> sta $02
    var yCoord = ram.sprObjYPos[1 + objectX]
    //> lda Enemy_Rel_XPos          ;get enemy object horizontal position
    //> sta $05                     ;relative to screen
    val xCoord = ram.enemyRelXPos
    //> ldy Enemy_SprDataOffset,x
    //> sty $eb                     ;get sprite data offset
    val sprDataOfs = (ram.enemySprDataOffset[objectX].toInt() and 0xFF) shr 2
    //> lda #$00
    //> sta VerticalFlipFlag        ;initialize vertical flip flag by default
    ram.verticalFlipFlag = false
    //> lda Enemy_MovingDir,x
    //> sta $03                     ;get enemy object moving direction
    var flipCtrl = ram.enemyMovingDirs[objectX]
    //> lda Enemy_SprAttrib,x
    //> sta $04                     ;get enemy object sprite attributes
    var attribs = ram.sprAttrib[1 + objectX]  // by Claude - indexed by x (Enemy_SprAttrib,x)
    //> lda Enemy_ID,x
    val enemyId = ram.enemyID[objectX].toInt() and 0xFF

    //> cmp #PiranhaPlant           ;is enemy object piranha plant?
    //> bne CheckForRetainerObj     ;if not, branch
    if (enemyId == EnemyId.PiranhaPlant.id) {
        //> ldy PiranhaPlant_Y_Speed,x
        //> bmi CheckForRetainerObj     ;if piranha plant moving upwards, branch
        val ppYSpeed = ram.sprObjYSpeed[1 + objectX]
        if (ppYSpeed.toInt() >= 0) {
            //> ldy EnemyFrameTimer,x
            //> beq CheckForRetainerObj     ;if timer for movement expired, branch
            val timer = ram.timers[0x0a + objectX]
            if (timer != 0.toByte()) {
                //> rts                         ;if all conditions fail, leave
                return
            }
        }
    }

    //> CheckForRetainerObj:
    //> lda Enemy_State,x           ;store enemy state
    //> sta $ed
    val savedEnemyState = ram.enemyState.getEnemyState(objectX)
    //> and #%00011111              ;nullify all but 5 LSB and use as Y
    //> tay
    var altState = savedEnemyState.byte.toInt() and 0x1F

    //> lda Enemy_ID,x              ;check for mushroom retainer/princess object
    //> cmp #RetainerObject
    //> bne CheckForBulletBillCV    ;if not found, branch
    var enemyCode = enemyId  // $ef: enemy code used throughout the handler

    if (enemyId == EnemyId.RetainerObject.id) {
        //> ldy #$00                    ;if found, nullify saved state in Y
        altState = 0
        //> lda #$01                    ;set value that will not be used
        //> sta $03
        flipCtrl = 0x01
        //> lda #$15                    ;set value $15 as code for mushroom retainer/princess object
        enemyCode = 0x15
    }

    //> CheckForBulletBillCV:
    //> bne CheckForJumpspring      ;if not found, branch again
    if (enemyCode == EnemyId.BulletBillCannonVar.id) {
        //> dec $02                     ;decrement saved vertical position
        yCoord = (yCoord - 1).toByte()
        //> lda #$03
        var bbAttrib = 0x03
        //> ldy EnemyFrameTimer,x       ;get timer for enemy object
        //> beq SBBAt                   ;if expired, do not set priority bit
        if (ram.timers[0x0a + objectX] != 0.toByte()) {
            //> ora #%00100000              ;otherwise do so
            bbAttrib = bbAttrib or 0x20
        }
        //> SBBAt: sta $04                     ;set new sprite attributes
        attribs = bbAttrib.toByte()
        //> ldy #$00                    ;nullify saved enemy state both in Y and in
        //> sty $ed                     ;memory location here
        altState = 0
        //> lda #$08                    ;set specific value to unconditionally branch once
        enemyCode = 0x08
    }

    //> CheckForJumpspring:
    //> bne CheckForPodoboo
    //> cmp #JumpspringObject        ;check for jumpspring object
    if (enemyCode == EnemyId.JumpspringObject.id) {
        //> ldy #$03                     ;set enemy state -2 MSB here for jumpspring object
        altState = 0x03
        //> ldx JumpspringAnimCtrl       ;get current frame number for jumpspring object
        val jsFrame = ram.jumpspringAnimCtrl.toInt() and 0xFF
        //> lda JumpspringFrameOffsets,x ;load data using frame number as offset
        enemyCode = JumpspringFrameOffsets[jsFrame]
    }

    //> CheckForPodoboo:
    //> sta $ef                 ;store saved enemy object value here
    //> sty $ec                 ;and Y here (enemy state -2 MSB if not changed)
    var savedAltState = altState  // $ec
    //> ldx ObjectOffset        ;get enemy object offset
    var x = ram.objectOffset.toInt()
    //> cmp #$0c                ;check for podoboo object
    //> bne CheckBowserGfxFlag  ;branch if not found
    if (enemyCode == 0x0c) {
        //> lda Enemy_Y_Speed,x     ;if moving upwards, branch
        //> bmi CheckBowserGfxFlag
        if (ram.sprObjYSpeed[1 + x].toInt() >= 0) {
            //> inc VerticalFlipFlag    ;otherwise, set flag for vertical flip
            ram.verticalFlipFlag = true
        }
    }

    //> CheckBowserGfxFlag:
    //> lda BowserGfxFlag   ;if not drawing bowser at all, skip to something else
    //> beq CheckForGoomba
    val bowserFlag = ram.bowserGfxFlag.toInt() and 0xFF
    if (bowserFlag != 0) {
        //> ldy #$16            ;if set to 1, draw bowser's front
        //> cmp #$01
        //> beq SBwsrGfxOfs
        //> iny                 ;otherwise draw bowser's rear
        //> SBwsrGfxOfs: sty $ef
        enemyCode = if (bowserFlag == 1) 0x16 else 0x17
    }

    //> CheckForGoomba:
    var gfxTableOfs: Int  // X in the assembly, offset into EnemyGraphicsTable

    if (enemyCode == EnemyId.Goomba.id) {
        //> lda Enemy_State,x
        //> cmp #$02              ;check for defeated state
        //> bcc GmbaAnim          ;if not defeated, go ahead and animate
        if (savedEnemyState.toInt() >= 0x02) {
            //> ldx #$04              ;if defeated, write new value here
            //> stx $ec
            savedAltState = 0x04
        }
        //> GmbaAnim: and #%00100000        ;check for d5 set in enemy object state
        //> ora TimerControl      ;or timer disable flag set
        //> bne CheckBowserFront  ;if either condition true, do not animate goomba
        val noAnimate = savedEnemyState.defeated || (ram.timerControl != 0.toByte())
        if (!noAnimate) {
            //> lda FrameCounter
            //> and #%00001000        ;check for every eighth frame
            //> bne CheckBowserFront
            if ((ram.frameCounter.toInt() and 0x08) == 0) {
                //> lda $03
                //> eor #%00000011        ;invert bits to flip horizontally every eight frames
                //> sta $03               ;leave alone otherwise
                flipCtrl = (flipCtrl.toInt() xor 0x03).toByte()
            }
        }
    }

    //> CheckBowserFront:
    //> lda EnemyAttributeData,y    ;load sprite attribute using enemy object
    //> ora $04                     ;as offset, and add to bits already loaded
    //> sta $04
    attribs = (attribs.toInt() or EnemyAttributeData[enemyCode]).toByte()
    //> lda EnemyGfxTableOffsets,y  ;load value based on enemy object as offset
    //> tax                         ;save as X
    gfxTableOfs = EnemyGfxTableOffsets[enemyCode]
    //> ldy $ec                     ;get previously saved value
    // altState = savedAltState (already set)

    //> lda BowserGfxFlag
    //> beq CheckForSpiny           ;if not drawing bowser object at all, skip all of this
    if (bowserFlag != 0) {
        //> cmp #$01
        if (bowserFlag == 1) {
            //> bne CheckBowserRear         ;if not drawing front part, branch to draw the rear part
            //> lda BowserBodyControls      ;check bowser's body control bits
            //> bpl ChkFrontSte             ;branch if d7 not set (control's bowser's mouth)
            if (ram.bowserBodyControls.toInt() < 0) {
                //> ldx #$de                    ;otherwise load offset for second frame
                gfxTableOfs = 0xde
            }
            //> ChkFrontSte: lda $ed                     ;check saved enemy state
            //> and #%00100000              ;if bowser not defeated, do not set flag
            //> beq DrawBowser
            if (savedEnemyState.defeated) {
                //> FlipBowserOver:
                //> stx VerticalFlipFlag  ;set vertical flip flag to nonzero
                //> jmp DrawEnemyObject   ;draw bowser's graphics now
                //> DrawBowser:
                ram.verticalFlipFlag = true  // nonzero
            }
        } else {
            //> CheckBowserRear:
            //> lda BowserBodyControls  ;check bowser's body control bits
            //> and #$01
            //> beq ChkRearSte          ;branch if d0 not set (control's bowser's feet)
            if ((ram.bowserBodyControls.toInt() and 0x01) != 0) {
                //> ldx #$e4                ;otherwise load offset for second frame
                gfxTableOfs = 0xe4
            }
            //> ChkRearSte: lda $ed                 ;check saved enemy state
            //> and #%00100000          ;if bowser not defeated, do not set flag
            if (savedEnemyState.defeated) {
                //> lda $02                 ;subtract 16 pixels from
                //> sec                     ;saved vertical coordinate
                //> sbc #$10
                //> sta $02
                yCoord = (yCoord.toInt() - 0x10).toByte()
                //> jmp FlipBowserOver      ;jump to set vertical flip flag
                ram.verticalFlipFlag = true  // nonzero
            }
        }
        //> DrawBowser: jmp DrawEnemyObject
        // (fall through to drawing code below)
    } else {
        // Not bowser - handle spiny, lakitu, shells, etc.

        //> CheckForSpiny:
        //> bne CheckForLakitu     ;if not found, branch
        //> cpx #$24               ;check if value loaded is for spiny
        if (gfxTableOfs == 0x24) {
            //> cpy #$05               ;if enemy state set to $05, do this,
            //> bne NotEgg             ;otherwise branch
            if (savedAltState == 0x05) {
                //> ldx #$30               ;set to spiny egg offset
                gfxTableOfs = 0x30
                //> lda #$02
                //> sta $03                ;set enemy direction to reverse sprites horizontally
                flipCtrl = 0x02
                //> lda #$05
                //> sta $ec                ;set enemy state
                savedAltState = 0x05
            }
            //> NotEgg: jmp CheckForHammerBro  ;skip a big chunk of this if we found spiny
        } else if (gfxTableOfs == 0x90) {
            //> CheckForLakitu:
            //> bne CheckUpsideDownShell  ;branch if not loaded
            //> cpx #$90                  ;check value for lakitu's offset loaded
            //> lda $ed
            //> and #%00100000            ;check for d5 set in enemy state
            //> bne NoLAFr                ;branch if set
            if (!savedEnemyState.defeated) {
                //> lda FrenzyEnemyTimer
                //> cmp #$10                  ;check timer to see if we've reached a certain range
                //> bcs NoLAFr                ;branch if not
                if ((ram.frenzyEnemyTimer.toInt() and 0xFF) < 0x10) {
                    //> ldx #$96                  ;if d6 not set and timer in range, load alt frame for lakitu
                    gfxTableOfs = 0x96
                }
            }
            //> NoLAFr: jmp CheckDefeatedState
        } else if (gfxTableOfs != 0x24 && gfxTableOfs != 0x90) {
            //> CheckUpsideDownShell:
            //> lda $ef                    ;check for enemy object => $04
            //> cmp #$04
            //> bcs CheckRightSideUpShell  ;branch if true
            if (enemyCode < 0x04 && savedAltState >= 0x02) {
                //> cpy #$02
                //> bcc CheckRightSideUpShell  ;branch if enemy state < $02
                //> ldx #$5a                   ;set for upside-down koopa shell by default
                gfxTableOfs = 0x5a
                //> ldy $ef
                //> cpy #BuzzyBeetle           ;check for buzzy beetle object
                //> bne CheckRightSideUpShell
                if (enemyCode == EnemyId.BuzzyBeetle.id) {
                    //> ldx #$7e                   ;set for upside-down buzzy beetle shell if found
                    gfxTableOfs = 0x7e
                    //> inc $02                    ;increment vertical position by one pixel
                    yCoord = (yCoord + 1).toByte()
                }
            }

            //> CheckRightSideUpShell:
            //> lda $ec                ;check for value set here
            //> cmp #$04               ;if enemy state < $02, do not change to shell, if
            //> bne CheckForHammerBro  ;enemy state => $02 but not = $04, leave shell upside-down
            if (savedAltState == 0x04) {
                //> ldx #$72               ;set right-side up buzzy beetle shell by default
                gfxTableOfs = 0x72
                //> inc $02                ;increment saved vertical position by one pixel
                yCoord = (yCoord + 1).toByte()
                if (enemyCode == EnemyId.BuzzyBeetle.id) {
                    //> beq CheckForDefdGoomba ;branch if found
                    // (use buzzy beetle shell, don't change further)
                } else {
                    //> ldx #$66               ;change to right-side up koopa shell if not found
                    gfxTableOfs = 0x66
                    //> inc $02                ;and increment saved vertical position again
                    yCoord = (yCoord + 1).toByte()
                }
                //> CheckForDefdGoomba:
                if (enemyCode == EnemyId.Goomba.id) {
                    //> ldx #$54               ;load for regular goomba
                    gfxTableOfs = 0x54
                    //> lda $ed                ;note that this only gets performed if enemy state => $02
                    //> and #%00100000         ;check saved enemy state for d5 set
                    if (!savedEnemyState.defeated) {
                        //> ldx #$8a               ;load offset for defeated goomba
                        gfxTableOfs = 0x8a
                        //> dec $02                ;set different value and decrement saved vertical position
                        yCoord = (yCoord - 1).toByte()
                    }
                }
            }
        }

        //> CheckForHammerBro:
        //> bne CheckForBloober      ;branch if not found
        x = ram.objectOffset.toInt()
        if (enemyCode == EnemyId.HammerBro.id) {
            //> lda $ed
            //> beq CheckToAnimateEnemy  ;branch if not in normal enemy state
            if (savedEnemyState.isActive) {
                //> and #%00001000
                //> beq CheckDefeatedState   ;if d3 not set, branch further away
                if (savedEnemyState.hammerThrown) {
                    //> ldx #$b4                 ;otherwise load offset for different frame
                    gfxTableOfs = 0xb4
                }
                //> bne CheckToAnimateEnemy  ;unconditional branch
            }
            // CheckToAnimateEnemy: skip animation checks for hammer bro
        } else {
            //> CheckForBloober:
            if (gfxTableOfs == 0x48) {
                //> cpx #$48                 ;check for cheep-cheep offset loaded
                //> beq CheckToAnimateEnemy  ;branch if found
                //> lda EnemyIntervalTimer,y
                // Cheep-cheep: skip to animation
            } else {
                val intervalTimer = ram.timers[0x16 + x].toInt() and 0xFF
                if (intervalTimer >= 0x05) {
                    //> cmp #$05
                    //> bcs CheckDefeatedState   ;branch if some timer is above a certain point
                    // Skip to defeated state check
                } else if (gfxTableOfs == 0x3c) {
                    //> cpx #$3c                 ;check for bloober offset loaded
                    if (intervalTimer == 0x01) {
                        //> cmp #$01
                        //> beq CheckDefeatedState   ;branch if timer is set to certain point
                        // Skip to defeated state check
                    } else {
                        //> inc $02                  ;increment saved vertical coordinate three pixels
                        //> inc $02
                        //> inc $02
                        yCoord = (yCoord.toInt() + 3).toByte()
                        //> jmp CheckAnimationStop   ;and do something else
                        // CheckAnimationStop: check for stopped animation
                        if ((!savedEnemyState.defeated && !savedEnemyState.kickedOrEmerged) && (ram.timerControl == 0.toByte())) {
                            gfxTableOfs += 6
                        }
                        // Skip the normal animation path, go to CheckDefeatedState
                    }
                } else {
                    // Not bloober or cheep-cheep
                    //> CheckToAnimateEnemy:
                    val shouldCheckAnim = when {
                        enemyCode == EnemyId.Goomba.id -> false
                        enemyCode == 0x08 -> false  // bullet bill
                        enemyCode == EnemyId.Podoboo.id -> false
                        enemyCode >= 0x18 -> false
                        else -> true
                    }

                    if (shouldCheckAnim) {
                        //> ldy #$00
                        //> cmp #$15                 ;check for mushroom retainer/princess object
                        //> bne CheckForSecondFrame  ;which uses different code here, branch if not found
                        if (enemyCode == 0x15) {
                            //> lda WorldNumber          ;are we on world 8?
                            //> cmp #World8
                            //> bcs CheckDefeatedState   ;if so, leave the offset alone (use princess)
                            if ((ram.worldNumber.toInt() and 0xFF) < Constants.World8.toInt() and 0xFF) {
                                //> ldx #$a2                 ;otherwise, set for mushroom retainer object instead
                                gfxTableOfs = 0xa2
                                //> lda #$03                 ;set alternate state here
                                //> sta $ec
                                savedAltState = 0x03
                            }
                        } else {
                            //> CheckForSecondFrame:
                            //> lda FrameCounter            ;load frame counter
                            //> and EnemyAnimTimingBMask,y  ;mask it
                            //> bne CheckDefeatedState      ;branch if timing is off
                            val timingMask = EnemyAnimTimingBMask[0]
                            if ((ram.frameCounter.toInt() and timingMask) == 0) {
                                //> CheckAnimationStop:
                                //> lda $ed                 ;check saved enemy state
                                //> and #%10100000          ;for d7 or d5, or check for timers stopped
                                //> ora TimerControl
                                //> bne CheckDefeatedState  ;if either condition true, branch
                                if ((!savedEnemyState.defeated && !savedEnemyState.kickedOrEmerged) && (ram.timerControl == 0.toByte())) {
                                    //> txa
                                    //> clc
                                    //> adc #$06                ;add $06 to current enemy offset
                                    //> tax                     ;to animate various enemy objects
                                    gfxTableOfs += 6
                                }
                            }
                        }
                    }
                }
            }
        }

        //> CheckDefeatedState:
        //> lda $ed               ;check saved enemy state
        //> and #%00100000        ;for d5 set
        if (savedEnemyState.defeated) {
            //> lda $ef
            //> cmp #$04              ;check for saved enemy object => $04
            //> bcc DrawEnemyObject   ;branch if less
            if (enemyCode >= 0x04) {
                //> ldy #$01
                //> sty VerticalFlipFlag  ;set vertical flip flag
                ram.verticalFlipFlag = true
                //> dey
                //> sty $ec               ;init saved value here
                savedAltState = 0
            }
        }
    }

    //> DrawEnemyObject:
    //> ldy $eb                    ;load sprite data offset
    var sprOfs = sprDataOfs
    //> jsr DrawEnemyObjRow        ;draw six tiles of data
    //> jsr DrawEnemyObjRow        ;into sprite data
    //> jsr DrawEnemyObjRow
    var tblIdx = gfxTableOfs
    for (row in 0 until 3) {
        val tile0 = EnemyGraphicsTable[tblIdx]
        val tile1 = EnemyGraphicsTable[tblIdx + 1]
        // DrawOneSpriteRow -> DrawSpriteObject
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
        tblIdx += 2
    }

    //> ldx ObjectOffset           ;get enemy object offset
    x = ram.objectOffset.toInt()
    //> ldy Enemy_SprDataOffset,x  ;get sprite data offset
    val y = (ram.enemySprDataOffset[x].toInt() and 0xFF) shr 2

    //> lda $ef
    //> cmp #$08                   ;get saved enemy object and check
    //> bne CheckForVerticalFlip   ;for bullet bill, branch if not found
    if (enemyCode == 0x08) {
        //> SkipToOffScrChk: jmp SprObjectOffscrChk
        sprObjectOffscrChk(x, y, enemyCode)
        return
    }

    //> CheckForVerticalFlip:
    //> lda VerticalFlipFlag       ;check if vertical flip flag is set here
    //> beq CheckForESymmetry      ;branch if not
    if (ram.verticalFlipFlag) {
        //> lda Sprite_Attributes,y    ;get attributes of first sprite we dealt with
        //> ora #%10000000             ;set bit for vertical flip
        val vFlipAttr = (ram.sprites[y].attributes.byte.toInt() or 0x80).toByte()
        //> iny
        //> iny                        ;increment two bytes so that we store the vertical flip
        //> jsr DumpSixSpr             ;in attribute bytes of enemy obj sprite data
        for (i in 0..5) {
            ram.sprites[y + i].attributes = SpriteFlags(vFlipAttr)
        }
        //> dey
        //> dey                        ;now go back to the Y coordinate offset
        //> tya
        //> tax                        ;give offset to X
        var flipRowOfs = y
        //> lda $ef
        //> cmp #HammerBro             ;check saved enemy object for hammer bro
        //> beq FlipEnemyVertically
        //> cmp #Lakitu                ;check saved enemy object for lakitu
        //> beq FlipEnemyVertically    ;branch for hammer bro or lakitu
        //> cmp #$15
        //> bcs FlipEnemyVertically    ;also branch if enemy object => $15
        val isSpecialFlip = (enemyCode == EnemyId.HammerBro.id) ||
                (enemyCode == EnemyId.Lakitu.id) ||
                (enemyCode >= 0x15)
        if (!isSpecialFlip) {
            //> txa
            //> clc
            //> adc #$08                   ;if not selected objects or => $15, set
            //> tax                        ;offset in X for next row
            flipRowOfs += 2  // +8 bytes = +2 sprites
        }

        //> FlipEnemyVertically:
        //> lda Sprite_Tilenumber,x     ;load first or second row tiles
        //> pha                         ;and save tiles to the stack
        //> lda Sprite_Tilenumber+4,x
        //> pha
        val savedTile0 = ram.sprites[flipRowOfs].tilenumber
        val savedTile1 = ram.sprites[flipRowOfs + 1].tilenumber
        //> lda Sprite_Tilenumber+16,y  ;exchange third row tiles
        //> sta Sprite_Tilenumber,x     ;with first or second row tiles
        //> lda Sprite_Tilenumber+20,y
        //> sta Sprite_Tilenumber+4,x
        ram.sprites[flipRowOfs].tilenumber = ram.sprites[y + 4].tilenumber
        ram.sprites[flipRowOfs + 1].tilenumber = ram.sprites[y + 5].tilenumber
        //> pla                         ;pull first or second row tiles from stack
        //> sta Sprite_Tilenumber+20,y  ;and save in third row
        //> pla
        //> sta Sprite_Tilenumber+16,y
        ram.sprites[y + 5].tilenumber = savedTile1
        ram.sprites[y + 4].tilenumber = savedTile0
    }

    //> CheckForESymmetry:
    //> lda BowserGfxFlag           ;are we drawing bowser at all?
    //> bne SkipToOffScrChk         ;branch if so
    if (bowserFlag != 0) {
        sprObjectOffscrChk(x, y, enemyCode)
        return
    }

    //> lda $ef
    //> ldx $ec                     ;get alternate enemy state
    //> cmp #$05                    ;check for hammer bro object
    //> bne ContES
    if (enemyCode == 0x05) {
        //> jmp SprObjectOffscrChk      ;jump if found
        sprObjectOffscrChk(x, y, enemyCode)
        return
    }

    //> ContES: cmp #Bloober                ;check for bloober object
    //> beq MirrorEnemyGfx
    //> cmp #PiranhaPlant           ;check for piranha plant object
    //> beq MirrorEnemyGfx
    //> cmp #Podoboo                ;check for podoboo object
    //> beq MirrorEnemyGfx          ;branch if either of three are found
    val shouldMirror = (enemyCode == EnemyId.Bloober.id) ||
            (enemyCode == EnemyId.PiranhaPlant.id) ||
            (enemyCode == EnemyId.Podoboo.id)

    val skipToMirrorLakitu: Boolean

    if (shouldMirror) {
        mirrorEnemyGfx(y, savedAltState, bowserFlag)
        skipToMirrorLakitu = true
    } else {
        //> cmp #Spiny                  ;check for spiny object
        //> bne ESRtnr                  ;branch closer if not found
        if (enemyCode == EnemyId.Spiny.id) {
            //> cpx #$05                    ;check spiny's state
            //> bne CheckToMirrorLakitu     ;branch if not an egg, otherwise
            if (savedAltState == 0x05) {
                mirrorEnemyGfx(y, savedAltState, bowserFlag)
                skipToMirrorLakitu = true
            } else {
                skipToMirrorLakitu = true
            }
        } else {
            //> ESRtnr: cmp #$15                    ;check for princess/mushroom retainer object
            //> bne SpnySC
            if (enemyCode == 0x15) {
                //> lda #$42                    ;set horizontal flip on bottom right sprite
                //> sta Sprite_Attributes+20,y  ;note that palette bits were already set earlier
                ram.sprites[y + 5].attributes = SpriteFlags(0x42)
            }
            //> SpnySC: cpx #$02                    ;if alternate enemy state set to 1 or 0, branch
            //> bcc CheckToMirrorLakitu
            if (savedAltState >= 0x02) {
                mirrorEnemyGfx(y, savedAltState, bowserFlag)
                skipToMirrorLakitu = true
            } else {
                skipToMirrorLakitu = true
            }
        }
    }

    //> CheckToMirrorLakitu:
    //> bne CheckToMirrorJSpring    ;branch if not found
    if (enemyCode == EnemyId.Lakitu.id) {
        //> lda VerticalFlipFlag
        //> bne NVFLak                  ;branch if vertical flip flag not set
        if (!ram.verticalFlipFlag) {
            //> lda Sprite_Attributes+16,y  ;save vertical flip and palette bits
            //> and #%10000001              ;in third row left sprite
            //> sta Sprite_Attributes+16,y
            ram.sprites[y + 4].attributes = SpriteFlags((ram.sprites[y + 4].attributes.byte.toInt() and 0x81).toByte())
            //> lda Sprite_Attributes+20,y  ;set horizontal flip and palette bits
            //> ora #%01000001              ;in third row right sprite
            //> sta Sprite_Attributes+20,y
            ram.sprites[y + 5].attributes = SpriteFlags((ram.sprites[y + 5].attributes.byte.toInt() or 0x41).toByte())
            //> ldx FrenzyEnemyTimer        ;check timer
            //> cpx #$10
            //> bcs SprObjectOffscrChk      ;branch if timer has not reached a certain range
            if ((ram.frenzyEnemyTimer.toInt() and 0xFF) < 0x10) {
                //> sta Sprite_Attributes+12,y  ;otherwise set same for second row right sprite
                ram.sprites[y + 3].attributes = ram.sprites[y + 5].attributes
                //> and #%10000001
                //> sta Sprite_Attributes+8,y   ;preserve vertical flip and palette bits for left sprite
                ram.sprites[y + 2].attributes = SpriteFlags((ram.sprites[y + 5].attributes.byte.toInt() and 0x81).toByte())
            }
        } else {
            //> NVFLak: lda Sprite_Attributes,y     ;get first row left sprite attributes
            //> and #%10000001
            //> sta Sprite_Attributes,y     ;save vertical flip and palette bits
            ram.sprites[y].attributes = SpriteFlags((ram.sprites[y].attributes.byte.toInt() and 0x81).toByte())
            //> lda Sprite_Attributes+4,y   ;get first row right sprite attributes
            //> ora #%01000001              ;set horizontal flip and palette bits
            //> sta Sprite_Attributes+4,y   ;note that vertical flip is left as-is
            ram.sprites[y + 1].attributes = SpriteFlags((ram.sprites[y + 1].attributes.byte.toInt() or 0x41).toByte())
        }
    }

    //> CheckToMirrorJSpring:
    //> lda $ef                     ;check for jumpspring object (any frame)
    //> cmp #$18
    //> bcc SprObjectOffscrChk      ;branch if not jumpspring object at all
    if (enemyCode >= 0x18) {
        //> lda #$82
        //> sta Sprite_Attributes+8,y   ;set vertical flip and palette bits of
        //> sta Sprite_Attributes+16,y  ;second and third row left sprites
        ram.sprites[y + 2].attributes = SpriteFlags(0x82.toByte())
        ram.sprites[y + 4].attributes = SpriteFlags(0x82.toByte())
        //> ora #%01000000
        //> sta Sprite_Attributes+12,y  ;set, in addition to those, horizontal flip
        //> sta Sprite_Attributes+20,y  ;for second and third row right sprites
        ram.sprites[y + 3].attributes = SpriteFlags(0xc2.toByte())
        ram.sprites[y + 5].attributes = SpriteFlags(0xc2.toByte())
    }

    //> SprObjectOffscrChk:
    sprObjectOffscrChk(ram.objectOffset.toInt(), y, enemyCode)
}

/**
 * Mirrors enemy graphics by copying left sprite attributes to right sprites with horizontal flip.
 */
private fun System.mirrorEnemyGfx(y: Int, altState: Int, bowserFlag: Int) {
    //> MirrorEnemyGfx:
    //> lda BowserGfxFlag           ;if enemy object is bowser, skip all of this
    //> bne CheckToMirrorLakitu
    if (bowserFlag != 0) return

    //> lda Sprite_Attributes,y     ;load attribute bits of first sprite
    //> and #%10100011
    val baseAttr = ram.sprites[y].attributes.byte.toInt() and 0xA3
    //> sta Sprite_Attributes,y     ;save vertical flip, priority, and palette bits
    //> sta Sprite_Attributes+8,y   ;in left sprite column of enemy object OAM data
    //> sta Sprite_Attributes+16,y
    ram.sprites[y].attributes = SpriteFlags(baseAttr.toByte())
    ram.sprites[y + 2].attributes = SpriteFlags(baseAttr.toByte())
    ram.sprites[y + 4].attributes = SpriteFlags(baseAttr.toByte())

    //> ora #%01000000              ;set horizontal flip
    var rightAttr = baseAttr or 0x40
    //> cpx #$05                    ;check for state used by spiny's egg
    //> bne EggExc                  ;if alternate state not set to $05, branch
    if (altState == 0x05) {
        //> ora #%10000000              ;otherwise set vertical flip
        rightAttr = rightAttr or 0x80
    }
    //> EggExc: sta Sprite_Attributes+4,y   ;set bits of right sprite column
    //> sta Sprite_Attributes+12,y  ;of enemy object sprite data
    //> sta Sprite_Attributes+20,y
    ram.sprites[y + 1].attributes = SpriteFlags(rightAttr.toByte())
    ram.sprites[y + 3].attributes = SpriteFlags(rightAttr.toByte())
    ram.sprites[y + 5].attributes = SpriteFlags(rightAttr.toByte())

    //> cpx #$04                    ;check alternate enemy state
    //> bne CheckToMirrorLakitu     ;branch if not $04
    if (altState == 0x04) {
        //> lda Sprite_Attributes+8,y   ;get second row left sprite attributes
        //> ora #%10000000
        //> sta Sprite_Attributes+8,y   ;store bits with vertical flip in
        //> sta Sprite_Attributes+16,y  ;second and third row left sprites
        val vFlipLeft = (baseAttr or 0x80).toByte()
        ram.sprites[y + 2].attributes = SpriteFlags(vFlipLeft)
        ram.sprites[y + 4].attributes = SpriteFlags(vFlipLeft)
        //> ora #%01000000
        //> sta Sprite_Attributes+12,y  ;store with horizontal and vertical flip in
        //> sta Sprite_Attributes+20,y  ;second and third row right sprites
        val hvFlipRight = (vFlipLeft.toInt() or 0x40).toByte()
        ram.sprites[y + 3].attributes = SpriteFlags(hvFlipRight)
        ram.sprites[y + 5].attributes = SpriteFlags(hvFlipRight)
    }
}

/**
 * Handles offscreen checking for enemy sprites: moves sprite rows/columns
 * offscreen based on the enemy's offscreen bits.
 */
private fun System.sprObjectOffscrChk(objectX: Int, sprOfs: Int, enemyCode: Int) {
    //> SprObjectOffscrChk:
    //> ldx ObjectOffset          ;get enemy buffer offset
    //> lda Enemy_OffscreenBits   ;check offscreen information
    val offscr = ram.enemyOffscreenBits.toInt() and 0xFF
    //> lsr
    //> lsr                       ;shift three times to the right
    //> lsr                       ;which puts d2 into carry
    var bits = offscr ushr 3

    //> pha                       ;save to stack
    //> bcc LcChk                 ;branch if not set (d2 was in carry after 3 shifts)
    // d2 of original offscr => after >>3, this is bit 0 of the result before the third shift
    // Actually: offscr >> 2 puts d2 in bit 0. Let me re-read:
    // lsr; lsr; lsr => shifts right 3 times. After 3 lsr, the carry contains what was bit 0 before
    // the third shift, which is bit 2+0 = bit 2... no.
    // After first lsr: A = offscr >> 1, carry = bit 0
    // After second lsr: A = offscr >> 2, carry = bit 1
    // After third lsr: A = offscr >> 3, carry = bit 2
    // So carry = (offscr >> 2) & 1 = d2

    if ((offscr and 0x04) != 0) {
        //> lda #$04                  ;set for right column sprites
        //> jsr MoveESprColOffscreen  ;and move them offscreen
        moveESprColOffscreen(objectX, 1)  // right column = +4 bytes = +1 sprite offset
    }
    //> LcChk: pla                       ;get from stack
    //> lsr                       ;move d3 to carry
    if ((offscr and 0x08) != 0) {
        //> lda #$00                  ;set for left column sprites,
        //> jsr MoveESprColOffscreen  ;move them offscreen
        moveESprColOffscreen(objectX, 0)  // left column = +0 offset
    }
    //> Row3C: pla
    //> lsr                       ;move d5 to carry this time
    //> lsr
    if ((offscr and 0x20) != 0) {
        //> lda #$10                  ;set for third row of sprites
        //> jsr MoveESprRowOffscreen  ;and move them offscreen
        moveESprRowOffscreen(objectX, 4)  // +$10 bytes = +4 sprites
    }
    //> Row23C: pla
    //> lsr                       ;move d6 into carry
    if ((offscr and 0x40) != 0) {
        //> lda #$08                  ;set for second and third rows
        //> jsr MoveESprRowOffscreen  ;move them offscreen
        moveESprRowOffscreen(objectX, 2)  // +$08 bytes = +2 sprites
    }
    //> AllRowC: pla
    //> lsr                       ;move d7 into carry
    if ((offscr and 0x80.toByte().toInt()) != 0) {
        //> jsr MoveESprRowOffscreen  ;move all sprites offscreen (A should be 0 by now)
        moveESprRowOffscreen(objectX, 0)  // A=0, all rows from start
        //> lda Enemy_ID,x
        //> cmp #Podoboo              ;check enemy identifier for podoboo
        //> beq ExEGHandler           ;skip this part if found, we do not want to erase podoboo!
        val eid = ram.enemyID[objectX].toInt() and 0xFF
        if (eid != EnemyId.Podoboo.id) {
            //> lda Enemy_Y_HighPos,x     ;check high byte of vertical position
            //> cmp #$02                  ;if not yet past the bottom of the screen, branch
            //> bne ExEGHandler
            if ((ram.sprObjYHighPos[1 + objectX].toInt() and 0xFF) == 0x02) {
                //> jsr EraseEnemyObject      ;what it says
                eraseEnemyObject()
            }
        }
    }
    //> ExEGHandler: rts
//> lda EnemyGraphicsTable+1,x
//> lda EnemyGraphicsTable,x    ;load two tiles of enemy graphics
//> DrawEnemyObjRow:
}

/**
 * Moves a row of two enemy sprites offscreen (sets Y to $f8).
 * [rowOffset] is the sprite offset from the enemy's base sprite data offset (in sprite units).
 */
private fun System.moveESprRowOffscreen(objectX: Int, rowOffset: Int) {
    //> MoveESprRowOffscreen:
    //> clc                         ;add A to enemy object OAM data offset
    //> adc Enemy_SprDataOffset,x
    //> tay                         ;use as offset
    val sprOfs = ((ram.enemySprDataOffset[objectX].toInt() and 0xFF) shr 2) + rowOffset
    //> lda #$f8
    //> jmp DumpTwoSpr              ;move first row of sprites offscreen
    ram.sprites[sprOfs].y = 0xf8.toUByte()
    ram.sprites[sprOfs + 1].y = 0xf8.toUByte()
}

/**
 * Moves a column of enemy sprites offscreen (first, second, and third row on one side).
 * [colOffset] is 0 for left column, 1 for right column.
 */
private fun System.moveESprColOffscreen(objectX: Int, colOffset: Int) {
    //> MoveESprColOffscreen:
    //> clc                         ;add A to enemy object OAM data offset
    //> adc Enemy_SprDataOffset,x
    //> tay                         ;use as offset
    val sprOfs = ((ram.enemySprDataOffset[objectX].toInt() and 0xFF) shr 2) + colOffset
    //> jsr MoveColOffscreen        ;move first and second row sprites in column offscreen
    ram.sprites[sprOfs].y = 0xf8.toUByte()
    ram.sprites[sprOfs + 2].y = 0xf8.toUByte()
    //> sta Sprite_Data+16,y        ;move third row sprite in column offscreen
    //> ;$05 - relative X position
    //> ;$04 - attributes
    //> ;$03 - horizontal flip flag (not used here)
    //> ;$02 - relative Y position
    //> ;$00-$01 - tile numbers
    ram.sprites[sprOfs + 4].y = 0xf8.toUByte()
}

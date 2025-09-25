package com.ivieleague.smbtranslation.old

import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.experimental.xor
import kotlin.repeat

/**
 * This has large skipped pieces because SCREW emulating the freaking PPU
 */
fun nonMaskableInterrupt() {

    //> NonMaskableInterrupt:
    //> lda Mirror_PPU_CTRL_REG1  ;disable NMIs in mirror reg
    //> and #%01111111            ;save all other bits
    //> sta Mirror_PPU_CTRL_REG1
    PictureProcessingUnit.Control.nmiEnabled = false

    //> and #%01111110            ;alter name table address to be $2800
    //> sta PPU_CTRL_REG1         ;(essentially $2000) but save other bits
    PictureProcessingUnit.Control.baseNametableAddress = 2

    //> lda Mirror_PPU_CTRL_REG2  ;disable OAM and background display by default
    //> and #%11100110
    PictureProcessingUnit.Mask.spriteEnabled = false
    PictureProcessingUnit.Mask.backgroundEnabled = false

    //> ldy DisableScreenFlag     ;get screen disable flag
    //> bne ScreenOff             ;if set, used bits as-is
    if(!system.disableScreenFlag) {
        //> lda Mirror_PPU_CTRL_REG2  ;otherwise reenable bits and save them
        //> ora #%00011110
        // (sta PPU_CTRL_REG2 will occur momentarily)
        PictureProcessingUnit.Mask.spriteEnabled = true
        PictureProcessingUnit.Mask.backgroundEnabled = true
        PictureProcessingUnit.Mask.showLeftSprites = true
        PictureProcessingUnit.Mask.showLeftBackground = true
    } else {
        //> ScreenOff:     sta Mirror_PPU_CTRL_REG2  ;save bits for later but not in register at the moment
        //> and #%11100111            ;disable screen for now
        //> sta PPU_CTRL_REG2
        PictureProcessingUnit.Mask.spriteEnabled = false
        PictureProcessingUnit.Mask.backgroundEnabled = false
    }

    //> ldx PPU_STATUS            ;reset flip-flop and reset scroll registers to zero
    // Redundant for us.

    //> lda #$00
    //> jsr InitScroll
    initScroll(0)

    //> sta PPU_SPR_ADDR          ;reset spr-ram address register
    PictureProcessingUnit.setOamAddress(0)

    //> lda #$02                  ;perform spr-ram DMA access on $0200-$02ff
    //> sta SPR_DMA
    ObjectAttributes.submitBuffer()

    //> ldx VRAM_Buffer_AddrCtrl  ;load control for pointer to buffer contents
    //> lda VRAM_AddrTable_Low,x  ;set indirect at $00 to pointer
    //> sta $00
    //> lda VRAM_AddrTable_High,x
    //> sta $01
    //> jsr UpdateScreen          ;update screen with buffer contents
    updateScreen(system.bufferToWrite)

    //> ldy #$00
    //> ldx VRAM_Buffer_AddrCtrl  ;check for usage of $0341
    //> cpx #$06
    //> bne InitBuffer
    //> iny                       ;get offset based on usage
    //> InitBuffer:    ldx VRAM_Buffer_Offset,y
    //> lda #$00                  ;clear buffer header at last location
    // This whole freaking block's purpose is literally just to choose which buffer to clear.
    val bufferToClear = if (system.bufferToWrite == system.vramBuffer2)
        system.vramBuffer2
    else
        system.vramBuffer1

    //> sta VRAM_Buffer1_Offset,x
    bufferToClear.offset = 0x0
    //> sta VRAM_Buffer1,x
    bufferToClear.bytes[0] = 0x0

    //> sta VRAM_Buffer_AddrCtrl  ;reinit address control to $0301
    system.bufferToWrite = system.vramBuffer1

    //> lda Mirror_PPU_CTRL_REG2  ;copy mirror of $2001 to register
    //> sta PPU_CTRL_REG2
    // Redundant, since we don't need the mirror.

    //> jsr SoundEngine           ;play sound
    soundEngine()
    //> jsr ReadJoypads           ;read joypads
    readJoypads()
    //> jsr PauseRoutine          ;handle pause
    pauseRoutine()
    //> jsr UpdateTopScore
    updateTopScore()

    //> lda GamePauseStatus       ;check for pause status
    //> lsr
    //> bcs PauseSkip
    // Timers only run if we're not paused.
    if (system.gamePauseStatus and 0x1 == 0x0.toByte()) {
        //> lda TimerControl          ;if master timer control not set, decrement
        //> beq DecTimers             ;all frame and interval timers
        if (system.game.area.timerControl != 0x0.toByte()) {
            //> dec TimerControl
            system.game.area.timerControl--
            //> bne NoDecTimers
            if (system.game.area.timerControl == 0x0.toByte()) decTimers()
        } else decTimers()
        //> NoDecTimers:   inc FrameCounter          ;increment frame counter
        system.game.area.frameCounter++
    }

    //> PauseSkip:     ldx #$00
    //> ldy #$07

    //> lda PseudoRandomBitReg    ;get first memory location of LSFR bytes
    //> and #%00000010            ;mask out all but d1
    //> sta $00                   ;save here
    val temp = system.game.psuedoRandomBitReg[0] and 0b00000010

    //> lda PseudoRandomBitReg+1  ;get second memory location
    //> and #%00000010            ;mask out all but d1
    //> eor $00                   ;perform exclusive-OR on d1 from first and second bytes
    val temp2 = system.game.psuedoRandomBitReg[1] and 0b00000010 xor temp

    //> clc                       ;if neither or both are set, carry will be clear
    //> beq RotPRandomBit
    //> sec                       ;if one or the other is set, carry will be set
    var carry = temp2 != 0x00.toByte()

    repeat(0x7) {
        //> RotPRandomBit: ror PseudoRandomBitReg,x  ;rotate carry into d7, and rotate last bit into carry
        val initial = system.game.psuedoRandomBitReg[it + 1]
        system.game.psuedoRandomBitReg[it + 1] = (initial.rotateRight(1) and 0b01111111) or
                (if(carry) 0b10000000.toByte() else 0.toByte())
        carry = (initial and 0b1) != 0.toByte()
        //> inx                       ;increment to next byte
        //> dey                       ;decrement for loop
        //> bne RotPRandomBit
    }

    // This cursed block's purpose is to only scroll AFTER the line with the world numbers scroll away.
    // We don't care.
    //> lda Sprite0HitDetectFlag  ;check for flag here
    //> beq SkipSprite0
//    if(!system.game.area.sprite0HitDetectFlag) {
        //> Sprite0Clr:    lda PPU_STATUS            ;wait for sprite 0 flag to clear, which will
        //> and #%01000000            ;not happen until vblank has ended
        //> bne Sprite0Clr
        // Wait till we've passed the top display
        // Meh, I'm not gonna finish this.

        //> lda GamePauseStatus       ;if in pause mode, do not bother with sprites at all
        //> lsr
        //> bcs Sprite0Hit
        //> jsr MoveSpritesOffscreen
        //> jsr SpriteShuffler
        //> Sprite0Hit:    lda PPU_STATUS            ;do sprite #0 hit detection
        //> and #%01000000
        //> beq Sprite0Hit
        //> ldy #$14                  ;small delay, to wait until we hit horizontal blank time
        //> HBlankDelay:   dey
        //> bne HBlankDelay
//    }
    //> SkipSprite0:   lda HorizontalScroll      ;set scroll registers from variables
    //> sta PPU_SCROLL_REG
    //> lda VerticalScroll
    //> sta PPU_SCROLL_REG
    PictureProcessingUnit.scroll(system.game.area.horizontalScroll, system.game.area.verticalScroll)

    //> lda Mirror_PPU_CTRL_REG1  ;load saved mirror of $2000
    //> pha
    //> sta PPU_CTRL_REG1

    //> lda GamePauseStatus       ;if in pause mode, do not perform operation mode stuff
    //> lsr
    //> bcs SkipMainOper
    //> jsr OperModeExecutionTree ;otherwise do one of many, many possible subroutines
    //> SkipMainOper:  lda PPU_STATUS            ;reset flip-flop
    //> pla
    //> ora #%10000000            ;reactivate NMIs
    //> sta PPU_CTRL_REG1
    //> rti                       ;we are done until the next frame!
}

private fun decTimers() {
    //> DecTimers:     ldx #$14                  ;load end offset for end of frame timers
    //> dec IntervalTimerControl  ;decrement interval timer control,
    Timers.current.intervalTimerControl--

    //> bpl DecTimersLoop         ;if not expired, only frame timers will decrement
    val highestTimerToDecrement = if (Timers.current.intervalTimerControl < 0) {
        //> lda #$14
        //> sta IntervalTimerControl  ;if control for interval timers expired,
        Timers.current.intervalTimerControl = 0x14
        //> ldx #$23                  ;interval timers will decrement along with frame timers
        0x23
    } else 0x14 // see above DecTimers: ldx #$14
    //> DecTimersLoop: lda Timers,x              ;check current timer
    for (index in highestTimerToDecrement downTo 0) {
        //> beq SkipExpTimer          ;if current timer expired, branch to skip,
        if(Timers.current.bytes[index] != 0x0.toByte()) {
            //> dec Timers,x              ;otherwise decrement the current timer
            Timers.current.bytes[index]--
        }
        //> SkipExpTimer:  dex                       ;move onto next timer
        // Handled by for loop
    }
    //> bpl DecTimersLoop         ;do this until all timers are dealt with
}

fun updateScreen(bufferToWrite: VramBuffer) {
    TODO()
}

fun soundEngine(): Unit = TODO()
fun readJoypads(): Unit = TODO()
fun pauseRoutine(): Unit = TODO()
fun updateTopScore(): Unit = TODO()
package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.old.InexactBitSetting
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.experimental.xor
import kotlin.repeat

/**
 * This has large skipped pieces because SCREW emulating the freaking PPU
 */
fun System.nonMaskableInterrupt() {

    //> NonMaskableInterrupt:
    //> lda Mirror_PPU_CTRL_REG1  ;disable NMIs in mirror reg
    //> and #%01111111            ;save all other bits
    //> sta Mirror_PPU_CTRL_REG1
    ram.mirrorPPUCTRLREG1.nmiEnabled = false

    //> and #%01111110            ;alter name table address to be $2800
    //> sta PPU_CTRL_REG1         ;(essentially $2000) but save other bits
    ppu.control.access.value = ram.mirrorPPUCTRLREG1.access.value
    ppu.control.baseNametableAddress = 2

    //> lda Mirror_PPU_CTRL_REG2  ;disable OAM and background display by default
    val tempReg2 = PictureProcessingUnit.Mask(object: ByteAccess {
        override var value: Byte = 0
    })
    tempReg2.access.value = ram.mirrorPPUCTRLREG2.access.value
    //> and #%11100110
    tempReg2.spriteEnabled = false
    tempReg2.backgroundEnabled = false

    //> ldy DisableScreenFlag     ;get screen disable flag
    //> bne ScreenOff             ;if set, used bits as-is
    if(!ram.disableScreenFlag) {
        //> lda Mirror_PPU_CTRL_REG2  ;otherwise reenable bits and save them
        tempReg2.access.value = ram.mirrorPPUCTRLREG2.access.value
        //> ora #%00011110
        tempReg2.spriteEnabled = true
        tempReg2.backgroundEnabled = true
        tempReg2.showLeftSprites = true
        tempReg2.showLeftBackground = true
    }
    //> ScreenOff:     sta Mirror_PPU_CTRL_REG2  ;save bits for later but not in register at the moment
    ram.mirrorPPUCTRLREG2.access.value = tempReg2.access.value
    //> and #%11100111            ;disable screen for now
    tempReg2.spriteEnabled = false
    tempReg2.backgroundEnabled = false
    //> sta PPU_CTRL_REG2
    ppu.mask.access.value = tempReg2.access.value


    //> ldx PPU_STATUS            ;reset flip-flop and reset scroll registers to zero
    // Redundant for us.

    //> lda #$00
    //> jsr InitScroll
    initScroll(0)

    //> sta PPU_SPR_ADDR          ;reset spr-ram address register
    ppu.setOamAddress(0)

    //> lda #$02                  ;perform spr-ram DMA access on $0200-$02ff
    //> sta SPR_DMA
    ppu.updateSpriteData(ram.sprites)

    //> ldx VRAM_Buffer_AddrCtrl  ;load control for pointer to buffer contents
    //> lda VRAM_AddrTable_Low,x  ;set indirect at $00 to pointer
    //> sta $00
    //> lda VRAM_AddrTable_High,x
    //> sta $01
    //> jsr UpdateScreen          ;update screen with buffer contents
    updateScreen(vramAddrTable[ram.vRAMBufferAddrCtrl.toInt()]!!)

    //> ldy #$00
    //> ldx VRAM_Buffer_AddrCtrl  ;check for usage of $0341
    //> cpx #$06
    //> bne InitBuffer
    //> iny                       ;get offset based on usage
    //> InitBuffer:    ldx VRAM_Buffer_Offset,y
    //> lda #$00                  ;clear buffer header at last location
    // This whole freaking block's purpose is literally just to choose which buffer to clear.
    val bufferToClear = if (vramAddrTable[ram.vRAMBufferAddrCtrl.toInt()] == ram.vRAMBuffer2)
        ram.vRAMBuffer2
    else
        ram.vRAMBuffer1

    //> sta VRAM_Buffer1_Offset,x
    bufferToClear.offset = 0x0
    //> sta VRAM_Buffer1,x
    bufferToClear.bytes[0] = 0x0

    //> sta VRAM_Buffer_AddrCtrl  ;reinit address control to $0301
    ram.vRAMBufferAddrCtrl = 0x0.toByte()

    //> lda Mirror_PPU_CTRL_REG2  ;copy mirror of $2001 to register
    //> sta PPU_CTRL_REG2
    ppu.mask.access.value = ram.mirrorPPUCTRLREG2.access.value

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
    if (ram.gamePauseStatus and 0x1 == 0x0.toByte()) {
        //> lda TimerControl          ;if master timer control not set, decrement
        //> beq DecTimers             ;all frame and interval timers
        if (ram.timerControl != 0x0.toByte()) {
            //> dec TimerControl
            ram.timerControl--
            //> bne NoDecTimers
            if (ram.timerControl == 0x0.toByte()) decTimers()
        } else decTimers()
        //> NoDecTimers:   inc FrameCounter          ;increment frame counter
        ram.frameCounter++
    }

    //> PauseSkip:     ldx #$00
    //> ldy #$07

    //> lda PseudoRandomBitReg    ;get first memory location of LSFR bytes
    //> and #%00000010            ;mask out all but d1
    //> sta $00                   ;save here
    val temp = ram.pseudoRandomBitReg[0] and 0b00000010

    //> lda PseudoRandomBitReg+1  ;get second memory location
    //> and #%00000010            ;mask out all but d1
    //> eor $00                   ;perform exclusive-OR on d1 from first and second bytes
    val temp2 = ram.pseudoRandomBitReg[1] and 0b00000010 xor temp

    //> clc                       ;if neither or both are set, carry will be clear
    //> beq RotPRandomBit
    //> sec                       ;if one or the other is set, carry will be set
    var carry = temp2 != 0x00.toByte()

    repeat(0x7) {
        //> RotPRandomBit: ror PseudoRandomBitReg,x  ;rotate carry into d7, and rotate last bit into carry
        val initial = ram.pseudoRandomBitReg[it + 1]
        ram.pseudoRandomBitReg[it + 1] = (initial.rotateRight(1) and 0b01111111) or
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
    if(!ram.sprite0HitDetectFlag) {
        //> Sprite0Clr:    lda PPU_STATUS            ;wait for sprite 0 flag to clear, which will
        //> and #%01000000            ;not happen until vblank has ended
        //> bne Sprite0Clr
        // Wait till we've passed the top display
        // TODO: Render the top bar at scroll zero

        //> lda GamePauseStatus       ;if in pause mode, do not bother with sprites at all
        //> lsr
        //> bcs Sprite0Hit
        @InexactBitSetting
        if(ram.gamePauseStatus != 0.toByte()) {
            //> jsr MoveSpritesOffscreen
            moveSpritesOffscreen()
            //> jsr SpriteShuffler
            spriteShuffler()
        }
        //> Sprite0Hit:    lda PPU_STATUS            ;do sprite #0 hit detection
        //> and #%01000000
        //> beq Sprite0Hit
        // Wait until we're hitting sprite 0

        //> ldy #$14                  ;small delay, to wait until we hit horizontal blank time
        //> HBlankDelay:   dey
        //> bne HBlankDelay
        // Not really relevant in gigahertz
    }
    //> SkipSprite0:   lda HorizontalScroll      ;set scroll registers from variables
    //> sta PPU_SCROLL_REG
    //> lda VerticalScroll
    //> sta PPU_SCROLL_REG
    ppu.scroll(ram.horizontalScroll, ram.verticalScroll)

    //> lda Mirror_PPU_CTRL_REG1  ;load saved mirror of $2000
    val reg1temp = ram.mirrorPPUCTRLREG1.access.value
    //> pha
    ram.stack.push(reg1temp)
    //> sta PPU_CTRL_REG1
    ppu.control.access.value = reg1temp

    //> lda GamePauseStatus       ;if in pause mode, do not perform operation mode stuff
    //> lsr
    //> bcs SkipMainOper
    @InexactBitSetting
    if(ram.gamePauseStatus != 0.toByte()) {
        //> jsr OperModeExecutionTree ;otherwise do one of many, many possible subroutines
        operModeExecutionTree()
    }
    //> SkipMainOper:  lda PPU_STATUS            ;reset flip-flop
    //> pla
    //> ora #%10000000            ;reactivate NMIs
    //> sta PPU_CTRL_REG1
    ppu.control.access.value = ram.stack.pop()
    ppu.control.nmiEnabled = true
    //> rti                       ;we are done until the next frame!
}

private fun System.decTimers() {
    //> DecTimers:     ldx #$14                  ;load end offset for end of frame timers
    //> dec IntervalTimerControl  ;decrement interval timer control,
    ram.intervalTimerControl--

    //> bpl DecTimersLoop         ;if not expired, only frame timers will decrement
    val highestTimerToDecrement = if (ram.intervalTimerControl < 0) {
        //> lda #$14
        //> sta IntervalTimerControl  ;if control for interval timers expired,
        ram.intervalTimerControl = 0x14
        //> ldx #$23                  ;interval timers will decrement along with frame timers
        0x23
    } else 0x14 // see above DecTimers: ldx #$14
    //> DecTimersLoop: lda Timers,x              ;check current timer
    for (index in highestTimerToDecrement downTo 0) {
        //> beq SkipExpTimer          ;if current timer expired, branch to skip,
        if(ram.timers[index] != 0x0.toByte()) {
            //> dec Timers,x              ;otherwise decrement the current timer
            ram.timers[index]--
        }
        //> SkipExpTimer:  dex                       ;move onto next timer
        // Handled by for loop
    }
    //> bpl DecTimersLoop         ;do this until all timers are dealt with
}

fun System.updateScreen(bufferToWrite: GameRam.VramBytes) {
    TODO()
}

fun System.soundEngine(): Unit = TODO()
fun System.readJoypads(): Unit = TODO()

fun System.pauseRoutine(): Unit {
    //> PauseRoutine:
    //> lda OperMode           ;are we in victory mode?
    //> cmp #VictoryModeValue  ;if so, go ahead
    //> beq ChkPauseTimer
    // wtf is this logic?
    if(ram.operMode != Constants.VictoryModeValue) {
        //> cmp #GameModeValue     ;are we in game mode?
        //> bne ExitPause          ;if not, leave
        if(ram.operMode != Constants.GameModeValue) return
        //> lda OperMode_Task      ;if we are in game mode, are we running game engine?
        //> cmp #$03
        //> bne ExitPause          ;if not, leave
        if(ram.operModeTask != 0x03.toByte()) return
    }

    //> ChkPauseTimer: lda GamePauseTimer     ;check if pause timer is still counting down
    //> beq ChkStart
    if(ram.gamePauseTimer != 0x0.toByte()) {
        //> dec GamePauseTimer     ;if so, decrement and leave
        ram.gamePauseTimer--
        //> rts
        return
    }

    // Pause logic
    //> ChkStart:      lda SavedJoypad1Bits   ;check to see if start is pressed
    //> and #Start_Button      ;on controller 1
    //> beq ClrPauseTimer
    if(inputs.joypadPort1.start) {
        //> lda GamePauseStatus    ;check to see if timer flag is set
        //> and #%10000000         ;and if so, do not reset timer (residual,
        //> bne ExitPause          ;joypad reading routine makes this unnecessary)
        if(ram.gamePauseStatus and 0b10000000.toByte() == 0x0.toByte()) return
        //> lda #$2b               ;set pause timer
        //> sta GamePauseTimer
        ram.gamePauseTimer = 0x2b.toByte()
        //> lda GamePauseStatus
        //> tay
        //> iny                    ;set pause sfx queue for next pause mode
        //> sty PauseSoundQueue
        ram.pauseSoundQueue = ram.gamePauseStatus.plus(1).toByte()
        //> eor #%00000001         ;invert d0 and set d7
        //> ora #%10000000
        //> bne SetPause           ;unconditional branch
        ram.gamePauseStatus = ram.gamePauseStatus.xor(0b1).or(0b10000000.toByte())
        return
    }
    //> ClrPauseTimer: lda GamePauseStatus    ;clear timer flag if timer is at zero and start button
    //> and #%01111111         ;is not pressed
    //> SetPause:      sta GamePauseStatus
    ram.gamePauseStatus = ram.gamePauseStatus.and(0b01111111.toByte())
    //> ExitPause:     rts
}
fun System.updateTopScore(): Unit = TODO()
fun System.moveSpritesOffscreen(): Unit = TODO()
fun System.spriteShuffler(): Unit {
    //> ;$00 - used for preset value
    //> SpriteShuffler:
    //> ldy AreaType                ;load level type, likely residual code
    //> lda #$28                    ;load preset value which will put it at
    //> sta $00                     ;sprite #10
    val temp = 0x28.toByte()
    //> ldx #$0e                    ;start at the end of OAM data offsets
    var x = 0x0e
    while(true) {
        //> ShuffleLoop:   lda SprDataOffset,x         ;check for offset value against
        val a = ram.sprites[x/4].x
        //> cmp $00                     ;the preset value
        //> bcc NextSprOffset           ;if less, skip this part
        if (a < temp.toUByte()) {
            //> ldy SprShuffleAmtOffset     ;get current offset to preset value we want to add
            //> clc
            // what the fuck is the point of clearing the carry before using add-with-carry?!?!
            //> adc SprShuffleAmt,y         ;get shuffle amount, add to current sprite offset
            //> bcc StrSprOffset            ;if not exceeded $ff, skip second add
            var updated = (a + ram.sprShuffleAmt[ram.sprShuffleAmtOffset.toInt()].toUInt()).toUByte()
            if(updated.overflows) {
                //> clc
                //> adc $00                     ;otherwise add preset value $28 to offset
                updated += temp.toUByte()
            }
            //> StrSprOffset:  sta SprDataOffset,x         ;store new offset here or old one if branched to here
            ram.sprites[x/4].x = updated
        }
        //> NextSprOffset: dex                         ;move backwards to next one
        x--
        //> bpl ShuffleLoop
    }

    //> ldx SprShuffleAmtOffset     ;load offset
    //> inx
    //> cpx #$03                    ;check if offset + 1 goes to 3
    //> bne SetAmtOffset            ;if offset + 1 not 3, store
    //> ldx #$00                    ;otherwise, init to 0
    //> SetAmtOffset:  stx SprShuffleAmtOffset
    //> ldx #$08                    ;load offsets for values and storage
    //> ldy #$02
    //> SetMiscOffset: lda SprDataOffset+5,y       ;load one of three OAM data offsets
    //> sta Misc_SprDataOffset-2,x  ;store first one unmodified, but
    //> clc                         ;add eight to the second and eight
    //> adc #$08                    ;more to the third one
    //> sta Misc_SprDataOffset-1,x  ;note that due to the way X is set up,
    //> clc                         ;this code loads into the misc sprite offsets
    //> adc #$08
    //> sta Misc_SprDataOffset,x
    //> dex
    //> dex
    //> dex
    //> dey
    //> bpl SetMiscOffset           ;do this until all misc spr offsets are loaded
    //> rts
}
fun System.operModeExecutionTree(): Unit = TODO()
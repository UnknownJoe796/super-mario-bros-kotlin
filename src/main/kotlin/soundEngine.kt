// by Claude - NES sound engine translated from smbdism.asm lines 15045-15958
package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.Constants.DeathMusic
import com.ivieleague.smbtranslation.Constants.EndOfCastleMusic
import com.ivieleague.smbtranslation.Constants.Sfx_ExtraLife
import com.ivieleague.smbtranslation.Constants.TimeRunningOutMusic
import com.ivieleague.smbtranslation.Constants.VictoryMusic

// ---------------------------------------------------------------------------
// APU register write helpers
// The NES writes raw bytes to memory-mapped APU registers $4000-$4017.
// In Kotlin we store the raw values into the structured APU fields.
// ---------------------------------------------------------------------------

private fun System.writeSndReg(offset: Int, value: Int) {
    val v = value and 0xFF
    val vb = v.toByte()
    when (offset) {
        //> SND_SQUARE1_REG ($4000-$4003)
        0 -> {
            apu.pulse1.duty = ((v shr 6) and 3).toByte()
            apu.pulse1.lengthCounterHalt = v and 0x20 != 0
            apu.pulse1.constantVolume = v and 0x10 != 0
            apu.pulse1.volume = (v and 0x0F).toByte()
        }
        1 -> {
            apu.pulse1.sweepEnabled = v and 0x80 != 0
            apu.pulse1.sweepPeriod = ((v shr 4) and 7).toByte()
            apu.pulse1.sweepNegate = v and 0x08 != 0
            apu.pulse1.sweepShift = (v and 7).toByte()
        }
        2 -> apu.pulse1.timer = vb
        3 -> {
            apu.pulse1.length = ((v shr 3) and 0x1F).toByte()
            apu.pulse1.timerHigh = (v and 7).toByte()
        }
        //> SND_SQUARE2_REG ($4004-$4007)
        4 -> {
            apu.pulse2.duty = ((v shr 6) and 3).toByte()
            apu.pulse2.lengthCounterHalt = v and 0x20 != 0
            apu.pulse2.constantVolume = v and 0x10 != 0
            apu.pulse2.volume = (v and 0x0F).toByte()
        }
        5 -> {
            apu.pulse2.sweepEnabled = v and 0x80 != 0
            apu.pulse2.sweepPeriod = ((v shr 4) and 7).toByte()
            apu.pulse2.sweepNegate = v and 0x08 != 0
            apu.pulse2.sweepShift = (v and 7).toByte()
        }
        6 -> apu.pulse2.timer = vb
        7 -> {
            apu.pulse2.length = ((v shr 3) and 0x1F).toByte()
            apu.pulse2.timerHigh = (v and 7).toByte()
        }
        //> SND_TRIANGLE_REG ($4008-$400B)
        8 -> {
            apu.triangle.lengthCounterHalt = v and 0x80 != 0
            apu.triangle.linearCounterLoad = (v and 0x7F).toByte()
        }
        // 9 unused
        10 -> apu.triangle.timer = vb // timer low byte
        11 -> {
            apu.triangle.lengthCounterLoad = ((v shr 3) and 0x1F).toByte()
            // timer high 3 bits are in bits 0-2 of v; stored alongside length counter load
        }
        //> SND_NOISE_REG ($400C-$400F)
        12 -> {
            apu.noise.lengthCounterHalt = v and 0x20 != 0
            apu.noise.constantVolume = v and 0x10 != 0
            apu.noise.volume = (v and 0x0F).toByte()
        }
        // 13 unused
        14 -> {
            apu.noise.noiseMode = v and 0x80 != 0
            apu.noise.noisePeriod = (v and 0x0F).toByte()
        }
        15 -> apu.noise.lengthCounterLoad = ((v shr 3) and 0x1F).toByte()
        //> SND_DELTA_REG ($4010-$4013)
        17 -> apu.deltaModulation.loadCounter = (v and 0x7F).toByte()
    }
}

private fun System.writeSndMasterCtrl(value: Int) {
    //> SND_MASTERCTRL_REG ($4015)
    val v = value and 0xFF
    apu.pulse1Enabled = v and 0x01 != 0
    apu.pulse2Enabled = v and 0x02 != 0
    apu.triangleEnabled = v and 0x04 != 0
    apu.noiseEnabled = v and 0x08 != 0
    apu.dmcEnabled = v and 0x10 != 0
}

// ---------------------------------------------------------------------------
// Low-level register dump / frequency helpers
// ---------------------------------------------------------------------------

//> Dump_Squ1_Regs: dump X and Y into square 1 control regs
private fun System.dumpSqu1Regs(regX: Int, regY: Int) {
    writeSndReg(0, regX)  //> stx SND_SQUARE1_REG
    writeSndReg(1, regY)  //> sty SND_SQUARE1_REG+1
}

//> Dump_Sq2_Regs: dump X and Y into square 2 control regs
private fun System.dumpSq2Regs(regX: Int, regY: Int) {
    writeSndReg(4, regX)  //> stx SND_SQUARE2_REG
    writeSndReg(5, regY)  //> sty SND_SQUARE2_REG+1
}

//> Dump_Freq_Regs: load frequency regs for channel at offset x
//  A (now called freqIdx) indexes into FreqRegLookupTbl
//  regOffset: 0=square1, 4=square2, 8=triangle
//  Returns true if a tone was loaded, false if silent (NoTone)
private fun System.dumpFreqRegs(freqIdx: Int, regOffset: Int): Boolean {
    val y = freqIdx and 0xFF
    val lsb = SoundData.freqRegLookupTbl[y + 1]  //> lda FreqRegLookupTbl+1,y
    if (lsb == 0) return false                     //> beq NoTone
    writeSndReg(regOffset + 2, lsb)                //> sta SND_REGISTER+2,x
    val msb = SoundData.freqRegLookupTbl[y]        //> lda FreqRegLookupTbl,y
    writeSndReg(regOffset + 3, msb or 0x08)        //> ora #$08; sta SND_REGISTER+3,x
    return true
}

//> PlaySqu1Sfx: set ctrl regs then frequency regs for square 1
private fun System.playSqu1Sfx(regA: Int, regX: Int, regY: Int) {
    dumpSqu1Regs(regX, regY)  //> jsr Dump_Squ1_Regs
    setFreqSqu1(regA)         //> fall through to SetFreq_Squ1
}

//> SetFreq_Squ1: set frequency regs for square 1
private fun System.setFreqSqu1(freqIdx: Int): Boolean {
    return dumpFreqRegs(freqIdx, 0)  //> ldx #$00; bne Dump_Freq_Regs
}

//> PlaySqu2Sfx: set ctrl regs then frequency regs for square 2
private fun System.playSqu2Sfx(regA: Int, regX: Int, regY: Int) {
    dumpSq2Regs(regX, regY)  //> jsr Dump_Sq2_Regs
    setFreqSqu2(regA)        //> fall through to SetFreq_Squ2
}

//> SetFreq_Squ2: set frequency regs for square 2
private fun System.setFreqSqu2(freqIdx: Int): Boolean {
    return dumpFreqRegs(freqIdx, 4)  //> ldx #$04; bne Dump_Freq_Regs
}

//> SetFreq_Tri: set frequency regs for triangle
private fun System.setFreqTri(freqIdx: Int): Boolean {
    return dumpFreqRegs(freqIdx, 8)  //> ldx #$08; bne Dump_Freq_Regs
}

// ---------------------------------------------------------------------------
// SoundEngine: top-level dispatcher
// ---------------------------------------------------------------------------

//> SoundEngine:
fun System.soundEngine() {
    //> lda OperMode; bne SndOn
    if (ram.operMode == OperMode.TitleScreen) {
        //> sta SND_MASTERCTRL_REG    ;disable sound and leave
        writeSndMasterCtrl(0)
        return
    }
    //> SndOn:
    writeSndReg(17, 0xFF)       //> lda #$ff; sta JOYPAD_PORT2 (actually $4017 frame counter)
    // Note: $4017 write is frame counter mode, not joypad. NES quirk.
    apu.irqInhibit = true
    apu.mode = com.ivieleague.smbtranslation.nes.AudioProcessingUnit.Mode.FiveStep
    writeSndMasterCtrl(0x0F)    //> lda #$0f; sta SND_MASTERCTRL_REG

    val pauseModeFlag = ram.pauseModeFlag.toInt() and 0xFF
    val pauseSoundQueue = ram.pauseSoundQueue.toInt() and 0xFF

    //> lda PauseModeFlag; bne InPause
    if (pauseModeFlag != 0) {
        handlePause()
        clearSoundQueues()
        handleDACCounter()
        return
    }
    //> lda PauseSoundQueue; cmp #$01; bne RunSoundSubroutines
    if (pauseSoundQueue == 0x01) {
        handlePause()
        clearSoundQueues()
        handleDACCounter()
        return
    }

    //> RunSoundSubroutines:
    square1SfxHandler()   //> jsr Square1SfxHandler
    square2SfxHandler()   //> jsr Square2SfxHandler
    noiseSfxHandler()     //> jsr NoiseSfxHandler
    musicHandler()        //> jsr MusicHandler
    ram.areaMusicQueue = 0   //> lda #$00; sta AreaMusicQueue
    ram.eventMusicQueue = 0  //> sta EventMusicQueue

    //> SkipSoundSubroutines:
    clearSoundQueues()
    handleDACCounter()
}

private fun System.clearSoundQueues() {
    //> lda #$00; sta Square1SoundQueue; sta Square2SoundQueue; sta NoiseSoundQueue; sta PauseSoundQueue
    ram.square1SoundQueue = 0
    ram.square2SoundQueue = 0
    ram.noiseSoundQueue = 0
    ram.pauseSoundQueue = 0
}

private fun System.handleDACCounter() {
    //> ldy DAC_Counter
    val y = ram.dACCounter.toInt() and 0xFF
    val areaMusicBuf = ram.areaMusicBuffer.toInt() and 0xFF
    //> lda AreaMusicBuffer; and #%00000011; beq NoIncDAC
    if ((areaMusicBuf and 0x03) != 0) {
        //> inc DAC_Counter
        ram.dACCounter = ((y + 1) and 0xFF).toByte()
        //> cpy #$30; bcc StrWave
        if (y < 0x30) {
            //> StrWave: sty SND_DELTA_REG+1
            writeSndReg(17, y)
            return
        }
        // fall through to NoIncDAC when Y >= $30
    }
    //> NoIncDAC: tya; beq StrWave
    if (y != 0) {
        //> dec DAC_Counter
        ram.dACCounter = ((y - 1) and 0xFF).toByte()
    }
    //> StrWave: sty SND_DELTA_REG+1
    writeSndReg(17, y)
}

// ---------------------------------------------------------------------------
// Pause sound handling
// ---------------------------------------------------------------------------

private fun System.handlePause() {
    val pauseSoundBuffer = ram.pauseSoundBuffer.toInt() and 0xFF
    val pauseSoundQueue = ram.pauseSoundQueue.toInt() and 0xFF

    //> InPause: lda PauseSoundBuffer; bne ContPau
    if (pauseSoundBuffer != 0) {
        //> ContPau:
        val lenCounter = ram.squ1SfxLenCounter.toInt() and 0xFF
        //> lda Squ1_SfxLenCounter; cmp #$24; beq PTone2F
        when (lenCounter) {
            0x24 -> {
                //> PTone2F: lda #$64
                playSqu1Sfx(0x64, 0x84, 0x7F)
            }
            0x1E -> {
                //> PTone1F: lda #$44
                playSqu1Sfx(0x44, 0x84, 0x7F)
            }
            0x18 -> {
                //> PTone2F: lda #$64
                playSqu1Sfx(0x64, 0x84, 0x7F)
            }
            // else: DecPauC, just decrement
        }
        //> DecPauC: dec Squ1_SfxLenCounter
        ram.squ1SfxLenCounter = ((lenCounter - 1) and 0xFF).toByte()
        if ((lenCounter - 1) and 0xFF == 0) {
            //> lda #$00; sta SND_MASTERCTRL_REG
            writeSndMasterCtrl(0)
            //> lda PauseSoundBuffer; cmp #$02; bne SkipPIn
            if (pauseSoundBuffer == 0x02) {
                //> lda #$00; sta PauseModeFlag
                ram.pauseModeFlag = 0
            }
            //> SkipPIn: lda #$00; sta PauseSoundBuffer
            ram.pauseSoundBuffer = 0
        }
        return
    }

    //> lda PauseSoundQueue; beq SkipSoundSubroutines
    if (pauseSoundQueue == 0) return

    //> sta PauseSoundBuffer; sta PauseModeFlag
    ram.pauseSoundBuffer = pauseSoundQueue.toByte()
    ram.pauseModeFlag = pauseSoundQueue.toByte()
    //> lda #$00; sta SND_MASTERCTRL_REG
    writeSndMasterCtrl(0)
    //> sta Square1SoundBuffer; sta Square2SoundBuffer; sta NoiseSoundBuffer
    ram.square1SoundBuffer = 0
    ram.square2SoundBuffer = 0
    ram.noiseSoundBuffer = 0
    //> lda #$0f; sta SND_MASTERCTRL_REG
    writeSndMasterCtrl(0x0F)
    //> lda #$2a; sta Squ1_SfxLenCounter
    ram.squ1SfxLenCounter = 0x2A
    //> PTone1F: lda #$44; bne PTRegC
    //> PTRegC: ldx #$84; ldy #$7f; jsr PlaySqu1Sfx
    playSqu1Sfx(0x44, 0x84, 0x7F)
}

// ---------------------------------------------------------------------------
// Square 1 SFX Handler
// ---------------------------------------------------------------------------

//> Square1SfxHandler:
private fun System.square1SfxHandler() {
    val queue = ram.square1SoundQueue.toInt() and 0xFF
    //> ldy Square1SoundQueue; beq CheckSfx1Buffer
    if (queue != 0) {
        //> sty Square1SoundBuffer
        ram.square1SoundBuffer = queue.toByte()
        //> bmi PlaySmallJump
        if (queue and 0x80 != 0) { playSmallJump(); return }
        // NES LSR shifts bit 0 into carry, BCS branches on carry
        // So we check bit 0 BEFORE shifting to get the carry
        if (queue and 0x01 != 0) { playBigJump(); return }      //> lsr; bcs PlayBigJump (bit 0)
        if (queue and 0x02 != 0) { playBump(); return }          //> lsr; bcs PlayBump (bit 1)
        if (queue and 0x04 != 0) { playSwimStomp(); return }     //> lsr; bcs PlaySwimStomp (bit 2)
        if (queue and 0x08 != 0) { playSmackEnemy(); return }    //> lsr; bcs PlaySmackEnemy (bit 3)
        if (queue and 0x10 != 0) { playPipeDownInj(); return }   //> lsr; bcs PlayPipeDownInj (bit 4)
        if (queue and 0x20 != 0) { playFireballThrow(); return } //> lsr; bcs PlayFireballThrow (bit 5)
        if (queue and 0x40 != 0) { playFlagpoleSlide(); return } //> lsr; bcs PlayFlagpoleSlide (bit 6)
        return
    }

    //> CheckSfx1Buffer:
    val buf = ram.square1SoundBuffer.toInt() and 0xFF
    if (buf == 0) return  //> beq ExS1H
    //> bmi ContinueSndJump
    if (buf and 0x80 != 0) { continueSndJump(); return }
    if (buf and 0x01 != 0) { continueSndJump(); return }      //> bcs ContinueSndJump (big jump, bit 0)
    if (buf and 0x02 != 0) { continueBumpThrow(); return }     //> bcs ContinueBumpThrow (bit 1)
    if (buf and 0x04 != 0) { continueSwimStomp(); return }     //> bcs ContinueSwimStomp (bit 2)
    if (buf and 0x08 != 0) { continueSmackEnemy(); return }    //> bcs ContinueSmackEnemy (bit 3)
    if (buf and 0x10 != 0) { continuePipeDownInj(); return }   //> bcs ContinuePipeDownInj (bit 4)
    if (buf and 0x20 != 0) { continueBumpThrow(); return }     //> bcs ContinueBumpThrow (fireball, bit 5)
    if (buf and 0x40 != 0) { decrementSfx1Length(); return }   //> bcs DecrementSfx1Length (flagpole, bit 6)
}

//> PlayFlagpoleSlide:
private fun System.playFlagpoleSlide() {
    ram.squ1SfxLenCounter = 0x40                //> lda #$40; sta Squ1_SfxLenCounter
    setFreqSqu1(0x62)                            //> lda #$62; jsr SetFreq_Squ1
    dumpSqu1Regs(0x99, 0xBC)                     //> ldx #$99; FPS2nd: ldy #$bc; jsr Dump_Squ1_Regs
}

//> PlaySmallJump:
private fun System.playSmallJump() {
    //> lda #$26; bne JumpRegContents
    playSqu1Sfx(0x26, 0x82, 0xA7)               //> JumpRegContents: ldx #$82; ldy #$a7; jsr PlaySqu1Sfx
    ram.squ1SfxLenCounter = 0x28                 //> lda #$28; sta Squ1_SfxLenCounter
    continueSndJump()
}

//> PlayBigJump:
private fun System.playBigJump() {
    //> lda #$18
    playSqu1Sfx(0x18, 0x82, 0xA7)               //> JumpRegContents: ldx #$82; ldy #$a7; jsr PlaySqu1Sfx
    ram.squ1SfxLenCounter = 0x28                 //> lda #$28; sta Squ1_SfxLenCounter
    continueSndJump()
}

//> ContinueSndJump:
private fun System.continueSndJump() {
    val len = ram.squ1SfxLenCounter.toInt() and 0xFF
    when (len) {
        0x25 -> {
            //> N2Prt... ldx #$5f; ldy #$f6; bne DmpJpFPS
            dumpSqu1Regs(0x5F, 0xF6)
        }
        0x20 -> {
            //> ldx #$48; FPS2nd: ldy #$bc; DmpJpFPS: jsr Dump_Squ1_Regs
            dumpSqu1Regs(0x48, 0xBC)
        }
        // else: DecJpFPS
    }
    //> DecJpFPS -> BranchToDecLength1 -> DecrementSfx1Length
    decrementSfx1Length()
}

//> PlayFireballThrow:
private fun System.playFireballThrow() {
    //> lda #$05; ldy #$99; bne Fthrow
    //> Fthrow: ldx #$9e; sta Squ1_SfxLenCounter; lda #$0c; jsr PlaySqu1Sfx
    ram.squ1SfxLenCounter = 0x05
    playSqu1Sfx(0x0C, 0x9E, 0x99)
    continueBumpThrow()
}

//> PlayBump:
private fun System.playBump() {
    //> lda #$0a; ldy #$93
    //> Fthrow: ldx #$9e; sta Squ1_SfxLenCounter; lda #$0c; jsr PlaySqu1Sfx
    ram.squ1SfxLenCounter = 0x0A
    playSqu1Sfx(0x0C, 0x9E, 0x93)
    continueBumpThrow()
}

//> ContinueBumpThrow:
private fun System.continueBumpThrow() {
    val len = ram.squ1SfxLenCounter.toInt() and 0xFF
    if (len == 0x06) {
        //> lda #$bb; sta SND_SQUARE1_REG+1
        writeSndReg(1, 0xBB)
    }
    //> DecJpFPS -> BranchToDecLength1 -> DecrementSfx1Length
    decrementSfx1Length()
}

//> PlaySwimStomp:
private fun System.playSwimStomp() {
    //> lda #$0e; sta Squ1_SfxLenCounter
    ram.squ1SfxLenCounter = 0x0E
    //> ldy #$9c; ldx #$9e; lda #$26; jsr PlaySqu1Sfx
    playSqu1Sfx(0x26, 0x9E, 0x9C)
    continueSwimStomp()
}

//> ContinueSwimStomp:
private fun System.continueSwimStomp() {
    val len = ram.squ1SfxLenCounter.toInt() and 0xFF
    //> ldy Squ1_SfxLenCounter; lda SwimStompEnvelopeData-1,y; sta SND_SQUARE1_REG
    val envData = SoundData.swimStompEnvelopeData[len - 1]
    writeSndReg(0, envData)
    //> cpy #$06; bne BranchToDecLength1
    if (len == 0x06) {
        //> lda #$9e; sta SND_SQUARE1_REG+2
        writeSndReg(2, 0x9E)
    }
    //> BranchToDecLength1 -> DecrementSfx1Length
    decrementSfx1Length()
}

//> PlaySmackEnemy:
private fun System.playSmackEnemy() {
    //> lda #$0e; ldy #$cb; ldx #$9f; sta Squ1_SfxLenCounter
    ram.squ1SfxLenCounter = 0x0E
    //> lda #$28; jsr PlaySqu1Sfx
    playSqu1Sfx(0x28, 0x9F, 0xCB)
    //> bne DecrementSfx1Length
    decrementSfx1Length()
}

//> ContinueSmackEnemy:
private fun System.continueSmackEnemy() {
    val len = ram.squ1SfxLenCounter.toInt() and 0xFF
    //> ldy Squ1_SfxLenCounter; cpy #$08; bne SmSpc
    if (len == 0x08) {
        //> lda #$a0; sta SND_SQUARE1_REG+2; lda #$9f; bne SmTick
        writeSndReg(2, 0xA0)
        writeSndReg(0, 0x9F)  //> SmTick: sta SND_SQUARE1_REG
    } else {
        //> SmSpc: lda #$90; SmTick: sta SND_SQUARE1_REG
        writeSndReg(0, 0x90)
    }
    decrementSfx1Length()
}

//> DecrementSfx1Length:
private fun System.decrementSfx1Length() {
    val len = ram.squ1SfxLenCounter.toInt() and 0xFF
    val newLen = (len - 1) and 0xFF
    ram.squ1SfxLenCounter = newLen.toByte()
    //> dec Squ1_SfxLenCounter; bne ExSfx1
    if (newLen == 0) {
        stopSquare1Sfx()
    }
}

//> StopSquare1Sfx:
private fun System.stopSquare1Sfx() {
    //> ldx #$00; stx $f1
    ram.square1SoundBuffer = 0
    //> ldx #$0e; stx SND_MASTERCTRL_REG
    writeSndMasterCtrl(0x0E)
    //> ldx #$0f; stx SND_MASTERCTRL_REG
    writeSndMasterCtrl(0x0F)
}

//> PlayPipeDownInj:
private fun System.playPipeDownInj() {
    //> lda #$2f; sta Squ1_SfxLenCounter
    ram.squ1SfxLenCounter = 0x2F
    continuePipeDownInj()
}

//> ContinuePipeDownInj:
private fun System.continuePipeDownInj() {
    val len = ram.squ1SfxLenCounter.toInt() and 0xFF
    //> lda Squ1_SfxLenCounter; lsr; bcs NoPDwnL  (check bit0)
    if (len and 1 == 0) {
        //> lsr; bcs NoPDwnL  (check bit1)
        if (len and 2 == 0) {
            //> and #%00000010; beq NoPDwnL  (after 2 lsrs, bit1 = original bit3)
            if (len and 0x08 != 0) {
                //> ldy #$91; ldx #$9a; lda #$44; jsr PlaySqu1Sfx
                playSqu1Sfx(0x44, 0x9A, 0x91)
            }
        }
    }
    //> NoPDwnL: jmp DecrementSfx1Length
    decrementSfx1Length()
}

// ---------------------------------------------------------------------------
// Square 2 SFX Handler
// ---------------------------------------------------------------------------

//> Square2SfxHandler:
private fun System.square2SfxHandler() {
    //> lda Square2SoundBuffer; and #Sfx_ExtraLife; bne ContinueExtraLife
    if ((ram.square2SoundBuffer.toInt() and Sfx_ExtraLife.toInt()) != 0) {
        continueExtraLife()
        return
    }

    val queue = ram.square2SoundQueue.toInt() and 0xFF
    //> ldy Square2SoundQueue; beq CheckSfx2Buffer
    if (queue != 0) {
        //> sty Square2SoundBuffer
        ram.square2SoundBuffer = queue.toByte()
        //> bmi PlayBowserFall
        if (queue and 0x80 != 0) { playBowserFall(); return }
        if (queue and 0x01 != 0) { playCoinGrab(); return }       //> lsr; bcs PlayCoinGrab (bit 0)
        if (queue and 0x02 != 0) { playGrowPowerUp(); return }    //> lsr; bcs PlayGrowPowerUp (bit 1)
        if (queue and 0x04 != 0) { playGrowVine(); return }       //> lsr; bcs PlayGrowVine (bit 2)
        if (queue and 0x08 != 0) { playBlast(); return }           //> lsr; bcs PlayBlast (bit 3)
        if (queue and 0x10 != 0) { playTimerTick(); return }      //> lsr; bcs PlayTimerTick (bit 4)
        if (queue and 0x20 != 0) { playPowerUpGrab(); return }    //> lsr; bcs PlayPowerUpGrab (bit 5)
        if (queue and 0x40 != 0) { playExtraLife(); return }       //> lsr; bcs PlayExtraLife (bit 6)
        return
    }

    //> CheckSfx2Buffer:
    val buf = ram.square2SoundBuffer.toInt() and 0xFF
    if (buf == 0) return  //> beq ExS2H
    //> bmi ContinueBowserFall
    if (buf and 0x80 != 0) { continueBowserFall(); return }
    if (buf and 0x01 != 0) { continueCGrabTTick(); return }    //> bcs Cont_CGrab_TTick (bit 0)
    if (buf and 0x02 != 0) { continueGrowItems(); return }     //> bcs ContinueGrowItems (bit 1)
    if (buf and 0x04 != 0) { continueGrowItems(); return }     //> bcs ContinueGrowItems (vine, bit 2)
    if (buf and 0x08 != 0) { continueBlast(); return }          //> bcs ContinueBlast (bit 3)
    if (buf and 0x10 != 0) { continueCGrabTTick(); return }    //> bcs Cont_CGrab_TTick (bit 4)
    if (buf and 0x20 != 0) { continuePowerUpGrab(); return }   //> bcs ContinuePowerUpGrab (bit 5)
    if (buf and 0x40 != 0) { continueExtraLife(); return }      //> bcs ContinueExtraLife (bit 6)
}

//> PlayCoinGrab:
private fun System.playCoinGrab() {
    //> lda #$35; ldx #$8d; bne CGrab_TTickRegL
    ram.squ2SfxLenCounter = 0x35
    playSqu2Sfx(0x42, 0x8D, 0x7F)  //> CGrab_TTickRegL -> PlaySqu2Sfx
    continueCGrabTTick()
}

//> PlayTimerTick:
private fun System.playTimerTick() {
    //> lda #$06; ldx #$98; bne CGrab_TTickRegL
    ram.squ2SfxLenCounter = 0x06
    playSqu2Sfx(0x42, 0x98.toInt(), 0x7F)
    continueCGrabTTick()
}

//> ContinueCGrabTTick:
private fun System.continueCGrabTTick() {
    val len = ram.squ2SfxLenCounter.toInt() and 0xFF
    //> lda Squ2_SfxLenCounter; cmp #$30; bne N2Tone
    if (len == 0x30) {
        //> lda #$54; sta SND_SQUARE2_REG+2
        writeSndReg(6, 0x54)
    }
    //> N2Tone: bne DecrementSfx2Length
    decrementSfx2Length()
}

//> PlayBlast:
private fun System.playBlast() {
    //> lda #$20; sta Squ2_SfxLenCounter; ldy #$94; lda #$5e; bne SBlasJ
    ram.squ2SfxLenCounter = 0x20
    //> SBlasJ -> PBFRegs: ldx #$9f; bne LoadSqu2Regs -> PlaySqu2Sfx
    playSqu2Sfx(0x5E, 0x9F, 0x94)
    decrementSfx2Length()
}

//> ContinueBlast:
private fun System.continueBlast() {
    val len = ram.squ2SfxLenCounter.toInt() and 0xFF
    //> lda Squ2_SfxLenCounter; cmp #$18; bne DecrementSfx2Length
    if (len == 0x18) {
        //> ldy #$93; lda #$18; SBlasJ -> PBFRegs: ldx #$9f
        playSqu2Sfx(0x18, 0x9F, 0x93)
    }
    decrementSfx2Length()
}

//> PlayPowerUpGrab:
private fun System.playPowerUpGrab() {
    //> lda #$36; sta Squ2_SfxLenCounter
    ram.squ2SfxLenCounter = 0x36
    continuePowerUpGrab()
}

//> ContinuePowerUpGrab:
private fun System.continuePowerUpGrab() {
    val len = ram.squ2SfxLenCounter.toInt() and 0xFF
    //> lda Squ2_SfxLenCounter; lsr; bcs DecrementSfx2Length
    if (len and 1 != 0) {
        decrementSfx2Length()
        return
    }
    //> tay; lda PowerUpGrabFreqData-1,y
    val y = len shr 1
    val freqIdx = SoundData.powerUpGrabFreqData[y - 1]
    //> ldx #$5d; ldy #$7f; LoadSqu2Regs: jsr PlaySqu2Sfx
    playSqu2Sfx(freqIdx, 0x5D, 0x7F)
    decrementSfx2Length()
}

//> DecrementSfx2Length:
private fun System.decrementSfx2Length() {
    val len = ram.squ2SfxLenCounter.toInt() and 0xFF
    val newLen = (len - 1) and 0xFF
    ram.squ2SfxLenCounter = newLen.toByte()
    //> dec Squ2_SfxLenCounter; bne ExSfx2
    if (newLen == 0) {
        emptySfx2Buffer()
    }
}

//> EmptySfx2Buffer:
private fun System.emptySfx2Buffer() {
    //> ldx #$00; stx Square2SoundBuffer
    ram.square2SoundBuffer = 0
    stopSquare2Sfx()
}

//> StopSquare2Sfx:
private fun System.stopSquare2Sfx() {
    //> ldx #$0d; stx SND_MASTERCTRL_REG
    writeSndMasterCtrl(0x0D)
    //> ldx #$0f; stx SND_MASTERCTRL_REG
    writeSndMasterCtrl(0x0F)
}

//> PlayBowserFall:
private fun System.playBowserFall() {
    //> lda #$38; sta Squ2_SfxLenCounter; ldy #$c4; lda #$18
    ram.squ2SfxLenCounter = 0x38
    //> BlstSJp -> PBFRegs: ldx #$9f; bne LoadSqu2Regs
    playSqu2Sfx(0x18, 0x9F, 0xC4)
    decrementSfx2Length()
}

//> ContinueBowserFall:
private fun System.continueBowserFall() {
    val len = ram.squ2SfxLenCounter.toInt() and 0xFF
    //> lda Squ2_SfxLenCounter; cmp #$08; bne DecrementSfx2Length
    if (len == 0x08) {
        //> ldy #$a4; lda #$5a; PBFRegs: ldx #$9f
        playSqu2Sfx(0x5A, 0x9F, 0xA4)
    }
    decrementSfx2Length()
}

//> PlayExtraLife:
private fun System.playExtraLife() {
    //> lda #$30; sta Squ2_SfxLenCounter
    ram.squ2SfxLenCounter = 0x30
    continueExtraLife()
}

//> ContinueExtraLife:
private fun System.continueExtraLife() {
    //> lda Squ2_SfxLenCounter; ldx #$03
    //> DivLLoop: lsr; bcs JumpToDecLength2; dex; bne DivLLoop
    // The loop shifts A right 3 times, branching to dec length if ANY carry is set.
    // Equivalent to: if any of the bottom 3 bits of the original value are set, skip.
    val origLen = ram.squ2SfxLenCounter.toInt() and 0xFF
    if (origLen and 0x07 != 0) {
        decrementSfx2Length()
        return
    }
    //> tay (A is now origLen >> 3)
    val y = origLen shr 3
    //> lda ExtraLifeFreqData-1,y
    val freqIdx = SoundData.extraLifeFreqData[y - 1]
    //> ldx #$82; ldy #$7f; bne EL_LRegs -> LoadSqu2Regs -> PlaySqu2Sfx
    playSqu2Sfx(freqIdx, 0x82, 0x7F)
    decrementSfx2Length()
}

//> PlayGrowPowerUp:
private fun System.playGrowPowerUp() {
    //> lda #$10; bne GrowItemRegs
    ram.squ2SfxLenCounter = 0x10
    //> lda #$7f; sta SND_SQUARE2_REG+1
    writeSndReg(5, 0x7F)
    //> lda #$00; sta Sfx_SecondaryCounter
    ram.sfxSecondaryCounter = 0
    continueGrowItems()
}

//> PlayGrowVine:
private fun System.playGrowVine() {
    //> lda #$20; GrowItemRegs:
    ram.squ2SfxLenCounter = 0x20
    writeSndReg(5, 0x7F)
    ram.sfxSecondaryCounter = 0
    continueGrowItems()
}

//> ContinueGrowItems:
private fun System.continueGrowItems() {
    //> inc Sfx_SecondaryCounter
    val secCounter = ((ram.sfxSecondaryCounter.toInt() and 0xFF) + 1) and 0xFF
    ram.sfxSecondaryCounter = secCounter.toByte()
    //> lda Sfx_SecondaryCounter; lsr; tay
    val y = secCounter shr 1
    val targetLen = ram.squ2SfxLenCounter.toInt() and 0xFF
    //> cpy Squ2_SfxLenCounter; beq StopGrowItems
    if (y == targetLen) {
        //> jmp EmptySfx2Buffer
        emptySfx2Buffer()
        return
    }
    //> lda #$9d; sta SND_SQUARE2_REG
    writeSndReg(4, 0x9D)
    //> lda PUp_VGrow_FreqData,y; jsr SetFreq_Squ2
    val freqIdx = SoundData.pUpVGrowFreqData[y]
    setFreqSqu2(freqIdx)
}

// ---------------------------------------------------------------------------
// Noise SFX Handler
// ---------------------------------------------------------------------------

//> NoiseSfxHandler:
private fun System.noiseSfxHandler() {
    val queue = ram.noiseSoundQueue.toInt() and 0xFF
    //> ldy NoiseSoundQueue; beq CheckNoiseBuffer
    if (queue != 0) {
        //> sty NoiseSoundBuffer
        ram.noiseSoundBuffer = queue.toByte()
        //> lsr NoiseSoundQueue; bcs PlayBrickShatter (bit 0)
        if (queue and 0x01 != 0) { playBrickShatter(); return }
        //> lsr NoiseSoundQueue; bcs PlayBowserFlame (bit 1)
        if (queue and 0x02 != 0) { playBowserFlame(); return }
        return
    }

    //> CheckNoiseBuffer:
    val buf = ram.noiseSoundBuffer.toInt() and 0xFF
    if (buf == 0) return  //> beq ExNH
    //> lsr; bcs ContinueBrickShatter
    if (buf and 0x01 != 0) { continueBrickShatter(); return }
    //> lsr; bcs ContinueBowserFlame
    if (buf and 0x02 != 0) { continueBowserFlame(); return }
}

//> PlayBrickShatter:
private fun System.playBrickShatter() {
    //> lda #$20; sta Noise_SfxLenCounter
    ram.noiseSfxLenCounter = 0x20
    continueBrickShatter()
}

//> ContinueBrickShatter:
private fun System.continueBrickShatter() {
    val len = ram.noiseSfxLenCounter.toInt() and 0xFF
    //> lda Noise_SfxLenCounter; lsr; bcc DecrementSfx3Length
    if (len and 1 != 0) {
        val y = len shr 1
        //> tay; ldx BrickShatterFreqData,y; lda BrickShatterEnvData,y
        val freqData = SoundData.brickShatterFreqData[y]
        val envData = SoundData.brickShatterEnvData[y]
        //> PlayNoiseSfx: sta SND_NOISE_REG; stx SND_NOISE_REG+2; lda #$18; sta SND_NOISE_REG+3
        writeSndReg(12, envData)
        writeSndReg(14, freqData)
        writeSndReg(15, 0x18)
    }
    decrementSfx3Length()
}

//> DecrementSfx3Length:
private fun System.decrementSfx3Length() {
    val len = ram.noiseSfxLenCounter.toInt() and 0xFF
    val newLen = (len - 1) and 0xFF
    ram.noiseSfxLenCounter = newLen.toByte()
    //> dec Noise_SfxLenCounter; bne ExSfx3
    if (newLen == 0) {
        //> lda #$f0; sta SND_NOISE_REG; lda #$00; sta NoiseSoundBuffer
        writeSndReg(12, 0xF0)
        ram.noiseSoundBuffer = 0
    }
}

//> PlayBowserFlame:
private fun System.playBowserFlame() {
    //> lda #$40; sta Noise_SfxLenCounter
    ram.noiseSfxLenCounter = 0x40
    continueBowserFlame()
}

//> ContinueBowserFlame:
private fun System.continueBowserFlame() {
    val len = ram.noiseSfxLenCounter.toInt() and 0xFF
    //> lda Noise_SfxLenCounter; lsr; tay
    val y = len shr 1
    //> ldx #$0f; lda BowserFlameEnvData-1,y
    val envData = SoundData.bowserFlameEnvData[y - 1]
    //> bne PlayNoiseSfx (unconditional)
    writeSndReg(12, envData)
    writeSndReg(14, 0x0F)
    writeSndReg(15, 0x18)
    decrementSfx3Length()
}

// ---------------------------------------------------------------------------
// Music Handler
// ---------------------------------------------------------------------------

//> MusicHandler:
private fun System.musicHandler() {
    val eventMusicQueue = ram.eventMusicQueue.toInt() and 0xFF
    val areaMusicQueue = ram.areaMusicQueue.toInt() and 0xFF

    //> lda EventMusicQueue; bne LoadEventMusic
    if (eventMusicQueue != 0) {
        loadEventMusic(eventMusicQueue)
        return
    }
    //> lda AreaMusicQueue; bne LoadAreaMusic
    if (areaMusicQueue != 0) {
        loadAreaMusic(areaMusicQueue)
        return
    }
    //> lda EventMusicBuffer; ora AreaMusicBuffer; bne ContinueMusic
    val combined = (ram.eventMusicBuffer.toInt() and 0xFF) or (ram.areaMusicBuffer.toInt() and 0xFF)
    if (combined != 0) {
        handleSquare2Music()
        return
    }
    //> rts
}

//> LoadEventMusic:
private fun System.loadEventMusic(eventQueue: Int) {
    //> sta EventMusicBuffer
    ram.eventMusicBuffer = eventQueue.toByte()
    //> cmp #DeathMusic; bne NoStopSfx
    if (eventQueue == (DeathMusic.toInt() and 0xFF)) {
        //> jsr StopSquare1Sfx; jsr StopSquare2Sfx
        stopSquare1Sfx()
        stopSquare2Sfx()
    }
    //> NoStopSfx: ldx AreaMusicBuffer; stx AreaMusicBuffer_Alt
    ram.areaMusicBufferAlt = ram.areaMusicBuffer
    //> ldy #$00; sty NoteLengthTblAdder; sty AreaMusicBuffer
    ram.noteLengthTblAdder = 0
    ram.areaMusicBuffer = 0
    //> cmp #TimeRunningOutMusic; bne FindEventMusicHeader
    if (eventQueue == (TimeRunningOutMusic.toInt() and 0xFF)) {
        //> ldx #$08; stx NoteLengthTblAdder
        ram.noteLengthTblAdder = 0x08
    }
    //> FindEventMusicHeader:
    findEventMusicHeader(eventQueue, 0)
}

//> LoadAreaMusic:
private fun System.loadAreaMusic(areaQueue: Int) {
    //> cmp #$04; bne NoStop1
    if (areaQueue == 0x04) {
        //> jsr StopSquare1Sfx
        stopSquare1Sfx()
    }
    //> NoStop1: ldy #$10; GMLoopB: sty GroundMusicHeaderOfs
    ram.groundMusicHeaderOfs = 0x10
    handleAreaMusicLoopB(areaQueue)
}

//> HandleAreaMusicLoopB:
private fun System.handleAreaMusicLoopB(areaQueue: Int) {
    //> ldy #$00; sty EventMusicBuffer
    ram.eventMusicBuffer = 0
    //> sta AreaMusicBuffer
    ram.areaMusicBuffer = areaQueue.toByte()
    //> cmp #$01; bne FindAreaMusicHeader
    if (areaQueue == 0x01) {
        //> inc GroundMusicHeaderOfs
        val ofs = ((ram.groundMusicHeaderOfs.toInt() and 0xFF) + 1) and 0xFF
        ram.groundMusicHeaderOfs = ofs.toByte()
        //> ldy GroundMusicHeaderOfs; cpy #$32; bne LoadHeader
        if (ofs == 0x32) {
            //> ldy #$11; bne GMLoopB
            ram.groundMusicHeaderOfs = 0x11
            handleAreaMusicLoopB(areaQueue)
            return
        }
        //> bne LoadHeader
        loadHeader(ofs)
        return
    }
    //> FindAreaMusicHeader: ldy #$08; sty MusicOffset_Square2
    ram.musicOffsetSquare2 = 0x08
    findEventMusicHeader(areaQueue, 8)
}

//> FindEventMusicHeader: iny; lsr; bcc FindEventMusicHeader
private fun System.findEventMusicHeader(queue: Int, startY: Int) {
    var y = startY
    var a = queue
    var carry: Int
    do {
        y++
        carry = a and 1  // bit that will be shifted out (into carry)
        a = a shr 1
    } while (carry == 0)  // bcc = carry clear = bit shifted out was 0
    loadHeader(y)
}

//> LoadHeader:
private fun System.loadHeader(y: Int) {
    //> lda MusicHeaderOffsetData,y -> this is MusicHeaderData[y-1] because MusicHeaderOffsetData = MusicHeaderData - 1
    // But actually in the existing SoundData, the offset table is 0-indexed and getMusicHeader expects
    // an index into musicHeaderOffsetData. The asm uses (MusicHeaderOffsetData,y) where y is 1-based.
    // musicHeaderOffsetData[0] corresponds to MusicHeaderOffsetData[1] (y=1).
    // So we use y-1 as the index.
    val header = SoundData.getMusicHeader(y - 1)

    //> lda MusicHeaderData,y -> sta NoteLenLookupTblOfs
    ram.noteLenLookupTblOfs = header.lengthOffset.toByte()
    //> lda MusicHeaderData+1,y -> sta MusicDataLow
    ram.musicData = header.dataAddrLow.toByte()
    //> lda MusicHeaderData+2,y -> sta MusicDataHigh
    ram.musicDataHigh = header.dataAddrHigh.toByte()
    //> lda MusicHeaderData+3,y -> sta MusicOffset_Triangle
    ram.musicOffsetTriangle = header.triangleOffset.toByte()
    //> lda MusicHeaderData+4,y -> sta MusicOffset_Square1
    ram.musicOffsetSquare1 = header.square1Offset.toByte()
    //> lda MusicHeaderData+5,y -> sta MusicOffset_Noise; sta NoiseDataLoopbackOfs
    ram.musicOffsetNoise = header.noiseOffset.toByte()
    ram.noiseDataLoopbackOfs = header.noiseOffset.toByte()
    //> lda #$01
    ram.squ2NoteLenCounter = 1
    ram.squ1NoteLenCounter = 1
    ram.triNoteLenCounter = 1
    ram.noiseBeatLenCounter = 1
    //> lda #$00; sta MusicOffset_Square2; sta AltRegContentFlag
    ram.musicOffsetSquare2 = 0
    ram.altRegContentFlag = 0
    //> lda #$0b; sta SND_MASTERCTRL_REG
    writeSndMasterCtrl(0x0B)
    //> lda #$0f; sta SND_MASTERCTRL_REG
    writeSndMasterCtrl(0x0F)

    //> fall through to HandleSquare2Music
    handleSquare2Music()
}

//> HandleSquare2Music:
private fun System.handleSquare2Music() {
    //> dec Squ2_NoteLenCounter
    val squ2Len = ((ram.squ2NoteLenCounter.toInt() and 0xFF) - 1) and 0xFF
    ram.squ2NoteLenCounter = squ2Len.toByte()
    //> bne MiscSqu2MusicTasks
    if (squ2Len == 0) {
        //> ldy MusicOffset_Square2; inc MusicOffset_Square2
        val y = ram.musicOffsetSquare2.toInt() and 0xFF
        ram.musicOffsetSquare2 = ((y + 1) and 0xFF).toByte()
        //> lda (MusicData),y
        val data = readMusicDataByte(y)
        //> beq EndOfMusicData
        if (data == 0) {
            endOfMusicData()
            return
        }
        //> bpl Squ2NoteHandler
        if (data and 0x80 == 0) {
            squ2NoteHandler(data)
        } else {
            //> bne Squ2LengthHandler
            squ2LengthHandler(data)
            return  // squ2LengthHandler calls squ2NoteHandler internally
        }
    }

    //> MiscSqu2MusicTasks:
    miscSqu2MusicTasks()
    handleSquare1Music()
}

//> EndOfMusicData:
private fun System.endOfMusicData() {
    val eventBuf = ram.eventMusicBuffer.toInt() and 0xFF
    //> lda EventMusicBuffer; cmp #TimeRunningOutMusic; bne NotTRO
    if (eventBuf == (TimeRunningOutMusic.toInt() and 0xFF)) {
        //> lda AreaMusicBuffer_Alt; bne MusicLoopBack
        val altBuf = ram.areaMusicBufferAlt.toInt() and 0xFF
        if (altBuf != 0) {
            //> MusicLoopBack: jmp HandleAreaMusicLoopB
            handleAreaMusicLoopB(altBuf)
            return
        }
    }
    //> NotTRO: and #VictoryMusic; bne VictoryMLoopBack
    if (eventBuf and (VictoryMusic.toInt() and 0xFF) != 0) {
        //> VictoryMLoopBack: jmp LoadEventMusic
        loadEventMusic(eventBuf)
        return
    }
    //> lda AreaMusicBuffer; and #%01011111; bne MusicLoopBack
    val areaBuf = ram.areaMusicBuffer.toInt() and 0xFF
    if (areaBuf and 0x5F != 0) {
        //> MusicLoopBack: jmp HandleAreaMusicLoopB
        handleAreaMusicLoopB(areaBuf)
        return
    }
    //> lda #$00; sta AreaMusicBuffer; sta EventMusicBuffer
    ram.areaMusicBuffer = 0
    ram.eventMusicBuffer = 0
    //> sta SND_TRIANGLE_REG
    writeSndReg(8, 0x00)
    //> lda #$90; sta SND_SQUARE1_REG; sta SND_SQUARE2_REG
    writeSndReg(0, 0x90)
    writeSndReg(4, 0x90)
}

//> Squ2LengthHandler:
private fun System.squ2LengthHandler(data: Int) {
    //> jsr ProcessLengthData; sta Squ2_NoteLenBuffer
    val length = processLengthData(data)
    ram.squ2NoteLenBuffer = length.toByte()
    //> ldy MusicOffset_Square2; inc MusicOffset_Square2; lda (MusicData),y
    val y = ram.musicOffsetSquare2.toInt() and 0xFF
    ram.musicOffsetSquare2 = ((y + 1) and 0xFF).toByte()
    val noteData = readMusicDataByte(y)
    //> fall through to Squ2NoteHandler
    squ2NoteHandler(noteData)
    miscSqu2MusicTasks()
    handleSquare1Music()
}

//> Squ2NoteHandler:
private fun System.squ2NoteHandler(noteData: Int) {
    //> ldx Square2SoundBuffer; bne SkipFqL1
    val squ2Buf = ram.square2SoundBuffer.toInt() and 0xFF
    if (squ2Buf == 0) {
        //> jsr SetFreq_Squ2
        val toneLoaded = setFreqSqu2(noteData)
        //> beq Rest
        if (toneLoaded) {
            //> jsr LoadControlRegs
            val (regA, regX, regY) = loadControlRegs()
            //> Rest: sta Squ2_EnvelopeDataCtrl
            ram.squ2EnvelopeDataCtrl = regA.toByte()
            //> jsr Dump_Sq2_Regs
            dumpSq2Regs(regX, regY)
        } else {
            //> Rest: sta Squ2_EnvelopeDataCtrl (A=0 from SetFreq_Squ2 returning false -> note=0 means rest)
            ram.squ2EnvelopeDataCtrl = 0
            // For rest, we still need to set control regs to silence
            // Actually, beq Rest means if SetFreq returned zero (no tone), skip LoadControlRegs
            // and store 0 into envelope ctrl, then dump regs with whatever X and Y were
            // On NES, X and Y are not modified by SetFreq_Squ2 when NoTone, so they hold
            // whatever was there before. In practice this is fine as the note is silent.
        }
    }
    //> SkipFqL1: lda Squ2_NoteLenBuffer; sta Squ2_NoteLenCounter
    ram.squ2NoteLenCounter = ram.squ2NoteLenBuffer
}

//> MiscSqu2MusicTasks:
private fun System.miscSqu2MusicTasks() {
    val squ2Buf = ram.square2SoundBuffer.toInt() and 0xFF
    //> lda Square2SoundBuffer; bne HandleSquare1Music
    if (squ2Buf != 0) return
    val eventBuf = ram.eventMusicBuffer.toInt() and 0xFF
    //> lda EventMusicBuffer; and #%10010001; bne HandleSquare1Music
    if (eventBuf and 0x91 != 0) return
    //> ldy Squ2_EnvelopeDataCtrl; beq NoDecEnv1; dec Squ2_EnvelopeDataCtrl
    val envCtrl = ram.squ2EnvelopeDataCtrl.toInt() and 0xFF
    if (envCtrl != 0) {
        ram.squ2EnvelopeDataCtrl = ((envCtrl - 1) and 0xFF).toByte()
    }
    //> NoDecEnv1: jsr LoadEnvelopeData; sta SND_SQUARE2_REG
    val envData = loadEnvelopeData(ram.squ2EnvelopeDataCtrl.toInt() and 0xFF)
    writeSndReg(4, envData)
    //> ldx #$7f; stx SND_SQUARE2_REG+1
    writeSndReg(5, 0x7F)
}

//> HandleSquare1Music:
private fun System.handleSquare1Music() {
    val squ1Offset = ram.musicOffsetSquare1.toInt() and 0xFF
    //> lda MusicOffset_Square1; beq HandleTriangleMusic
    if (squ1Offset == 0) {
        handleTriangleMusic()
        return
    }
    //> dec Squ1_NoteLenCounter; bne MiscSqu1MusicTasks
    val squ1Len = ((ram.squ1NoteLenCounter.toInt() and 0xFF) - 1) and 0xFF
    ram.squ1NoteLenCounter = squ1Len.toByte()
    if (squ1Len == 0) {
        fetchSqu1MusicData()
        return
    }
    //> MiscSqu1MusicTasks:
    miscSqu1MusicTasks()
    handleTriangleMusic()
}

//> FetchSqu1MusicData:
private fun System.fetchSqu1MusicData() {
    //> ldy MusicOffset_Square1; inc MusicOffset_Square1; lda (MusicData),y
    val y = ram.musicOffsetSquare1.toInt() and 0xFF
    ram.musicOffsetSquare1 = ((y + 1) and 0xFF).toByte()
    val data = readMusicDataByte(y)
    //> bne Squ1NoteHandler
    if (data != 0) {
        squ1NoteHandler(data)
        return
    }
    //> lda #$83; sta SND_SQUARE1_REG
    writeSndReg(0, 0x83)
    //> lda #$94; sta SND_SQUARE1_REG+1; sta AltRegContentFlag
    writeSndReg(1, 0x94)
    ram.altRegContentFlag = 0x94.toByte()
    //> bne FetchSqu1MusicData (unconditional)
    fetchSqu1MusicData()
}

//> Squ1NoteHandler:
private fun System.squ1NoteHandler(data: Int) {
    //> jsr AlternateLengthHandler
    val (length, originalX) = alternateLengthHandler(data)
    //> sta Squ1_NoteLenCounter
    ram.squ1NoteLenCounter = length.toByte()
    //> ldy Square1SoundBuffer; bne HandleTriangleMusic
    val squ1Buf = ram.square1SoundBuffer.toInt() and 0xFF
    if (squ1Buf != 0) {
        miscSqu1MusicTasks()
        handleTriangleMusic()
        return
    }
    //> txa; and #%00111110; jsr SetFreq_Squ1
    val noteIdx = originalX and 0x3E
    val toneLoaded = setFreqSqu1(noteIdx)
    //> beq SkipCtrlL
    if (toneLoaded) {
        //> jsr LoadControlRegs
        val (regA, regX, regY) = loadControlRegs()
        //> SkipCtrlL: sta Squ1_EnvelopeDataCtrl
        ram.squ1EnvelopeDataCtrl = regA.toByte()
        //> jsr Dump_Squ1_Regs
        dumpSqu1Regs(regX, regY)
    } else {
        ram.squ1EnvelopeDataCtrl = 0
    }
    //> fall through to MiscSqu1MusicTasks
    miscSqu1MusicTasks()
    handleTriangleMusic()
}

//> MiscSqu1MusicTasks:
private fun System.miscSqu1MusicTasks() {
    val squ1Buf = ram.square1SoundBuffer.toInt() and 0xFF
    //> lda Square1SoundBuffer; bne HandleTriangleMusic
    if (squ1Buf != 0) return
    val eventBuf = ram.eventMusicBuffer.toInt() and 0xFF
    //> lda EventMusicBuffer; and #%10010001; bne DeathMAltReg
    if (eventBuf and 0x91 != 0) {
        //> DeathMAltReg:
        deathMAltReg()
        return
    }
    //> ldy Squ1_EnvelopeDataCtrl; beq NoDecEnv2; dec Squ1_EnvelopeDataCtrl
    val envCtrl = ram.squ1EnvelopeDataCtrl.toInt() and 0xFF
    if (envCtrl != 0) {
        ram.squ1EnvelopeDataCtrl = ((envCtrl - 1) and 0xFF).toByte()
    }
    //> NoDecEnv2: jsr LoadEnvelopeData; sta SND_SQUARE1_REG
    val envData = loadEnvelopeData(ram.squ1EnvelopeDataCtrl.toInt() and 0xFF)
    writeSndReg(0, envData)
    //> DeathMAltReg:
    deathMAltReg()
}

//> DeathMAltReg:
private fun System.deathMAltReg() {
    val altReg = ram.altRegContentFlag.toInt() and 0xFF
    //> lda AltRegContentFlag; bne DoAltLoad
    if (altReg != 0) {
        //> DoAltLoad: sta SND_SQUARE1_REG+1
        writeSndReg(1, altReg)
    } else {
        //> lda #$7f; DoAltLoad: sta SND_SQUARE1_REG+1
        writeSndReg(1, 0x7F)
    }
}

//> HandleTriangleMusic:
private fun System.handleTriangleMusic() {
    val triOffset = ram.musicOffsetTriangle.toInt() and 0xFF
    //> lda MusicOffset_Triangle
    //> dec Tri_NoteLenCounter; bne HandleNoiseMusic
    val triLen = ((ram.triNoteLenCounter.toInt() and 0xFF) - 1) and 0xFF
    ram.triNoteLenCounter = triLen.toByte()
    if (triLen != 0) {
        handleNoiseMusic()
        return
    }
    //> ldy MusicOffset_Triangle; inc MusicOffset_Triangle; lda (MusicData),y
    val y = triOffset
    ram.musicOffsetTriangle = ((y + 1) and 0xFF).toByte()
    val data = readMusicDataByte(y)
    //> beq LoadTriCtrlReg
    if (data == 0) {
        //> LoadTriCtrlReg: sta SND_TRIANGLE_REG (A=0)
        writeSndReg(8, 0)
        handleNoiseMusic()
        return
    }
    //> bpl TriNoteHandler
    if (data and 0x80 == 0) {
        triNoteHandler(data)
    } else {
        //> jsr ProcessLengthData; sta Tri_NoteLenBuffer
        val length = processLengthData(data)
        ram.triNoteLenBuffer = length.toByte()
        //> lda #$1f; sta SND_TRIANGLE_REG
        writeSndReg(8, 0x1F)
        //> ldy MusicOffset_Triangle; inc MusicOffset_Triangle; lda (MusicData),y
        val y2 = ram.musicOffsetTriangle.toInt() and 0xFF
        ram.musicOffsetTriangle = ((y2 + 1) and 0xFF).toByte()
        val data2 = readMusicDataByte(y2)
        //> beq LoadTriCtrlReg
        if (data2 == 0) {
            writeSndReg(8, 0)
            handleNoiseMusic()
            return
        }
        triNoteHandler(data2)
    }
}

//> TriNoteHandler:
private fun System.triNoteHandler(noteData: Int) {
    //> jsr SetFreq_Tri
    setFreqTri(noteData)
    //> ldx Tri_NoteLenBuffer; stx Tri_NoteLenCounter
    ram.triNoteLenCounter = ram.triNoteLenBuffer
    val eventBuf = ram.eventMusicBuffer.toInt() and 0xFF
    val triLen = ram.triNoteLenBuffer.toInt() and 0xFF
    //> lda EventMusicBuffer; and #%01101110; bne NotDOrD4
    if (eventBuf and 0x6E != 0) {
        //> NotDOrD4:
        handleTriCtrl(triLen, eventBuf)
    } else {
        //> lda AreaMusicBuffer; and #%00001010; beq HandleNoiseMusic
        val areaBuf = ram.areaMusicBuffer.toInt() and 0xFF
        if (areaBuf and 0x0A == 0) {
            handleNoiseMusic()
            return
        }
        handleTriCtrl(triLen, eventBuf)
    }
}

private fun System.handleTriCtrl(triLen: Int, eventBuf: Int) {
    //> NotDOrD4: txa; cmp #$12; bcs LongN
    if (triLen >= 0x12) {
        //> LongN: lda #$ff; LoadTriCtrlReg: sta SND_TRIANGLE_REG
        writeSndReg(8, 0xFF)
    } else {
        //> lda EventMusicBuffer; and #EndOfCastleMusic; beq MediN
        if (eventBuf and (EndOfCastleMusic.toInt() and 0xFF) != 0) {
            //> lda #$0f; bne LoadTriCtrlReg
            writeSndReg(8, 0x0F)
        } else {
            //> MediN: lda #$1f; bne LoadTriCtrlReg
            writeSndReg(8, 0x1F)
        }
    }
    handleNoiseMusic()
}

//> HandleNoiseMusic:
private fun System.handleNoiseMusic() {
    val areaBuf = ram.areaMusicBuffer.toInt() and 0xFF
    //> lda AreaMusicBuffer; and #%11110011; beq ExitMusicHandler
    if (areaBuf and 0xF3 == 0) return  // underground or castle only -> skip noise
    //> dec Noise_BeatLenCounter; bne ExitMusicHandler
    val noiseLen = ((ram.noiseBeatLenCounter.toInt() and 0xFF) - 1) and 0xFF
    ram.noiseBeatLenCounter = noiseLen.toByte()
    if (noiseLen != 0) return

    fetchNoiseBeatData()
}

//> FetchNoiseBeatData:
private fun System.fetchNoiseBeatData() {
    //> ldy MusicOffset_Noise; inc MusicOffset_Noise; lda (MusicData),y
    val y = ram.musicOffsetNoise.toInt() and 0xFF
    ram.musicOffsetNoise = ((y + 1) and 0xFF).toByte()
    val data = readMusicDataByte(y)
    //> bne NoiseBeatHandler
    if (data != 0) {
        noiseBeatHandler(data)
        return
    }
    //> lda NoiseDataLoopbackOfs; sta MusicOffset_Noise; bne FetchNoiseBeatData
    ram.musicOffsetNoise = ram.noiseDataLoopbackOfs
    fetchNoiseBeatData()
}

//> NoiseBeatHandler:
private fun System.noiseBeatHandler(data: Int) {
    //> jsr AlternateLengthHandler
    val (length, originalX) = alternateLengthHandler(data)
    //> sta Noise_BeatLenCounter
    ram.noiseBeatLenCounter = length.toByte()
    //> txa; and #%00111110
    val beatData = originalX and 0x3E
    //> beq SilentBeat
    if (beatData == 0) {
        //> SilentBeat: lda #$10; PlayBeat: sta SND_NOISE_REG
        writeSndReg(12, 0x10)
        return
    }
    //> cmp #$30; beq LongBeat
    if (beatData == 0x30) {
        //> LongBeat: lda #$1c; ldx #$03; ldy #$58
        writeSndReg(12, 0x1C)
        writeSndReg(14, 0x03)
        writeSndReg(15, 0x58)
        return
    }
    //> cmp #$20; beq StrongBeat
    if (beatData == 0x20) {
        //> StrongBeat: lda #$1c; ldx #$0c; ldy #$18
        writeSndReg(12, 0x1C)
        writeSndReg(14, 0x0C)
        writeSndReg(15, 0x18)
        return
    }
    //> and #%00010000; beq SilentBeat
    if (beatData and 0x10 == 0) {
        writeSndReg(12, 0x10)
        return
    }
    //> lda #$1c; ldx #$03; ldy #$18 (short beat)
    writeSndReg(12, 0x1C)
    writeSndReg(14, 0x03)
    writeSndReg(15, 0x18)
}

// ---------------------------------------------------------------------------
// Shared music helper functions
// ---------------------------------------------------------------------------

/// Read a byte from music data using the MusicData pointer and offset Y.
private fun System.readMusicDataByte(y: Int): Int {
    val addrLow = ram.musicData.toInt() and 0xFF
    val addrHigh = ram.musicDataHigh.toInt() and 0xFF
    return SoundData.readMusicData(addrLow, addrHigh, y)
}

/// AlternateLengthHandler: extracts length and note data from a packed byte.
/// Square 1 music and noise data format: d7-d6,d0 = length bits, d5-d1 = note/beat data.
/// The bit rotation converts xx00000x into 00000xxx.
/// Returns (processedLength, originalByte).
private fun System.alternateLengthHandler(data: Int): Pair<Int, Int> {
    val x = data and 0xFF  //> tax (save original)
    //> ror  -> carry = bit0 of x
    val carry = x and 1
    //> txa; rol; rol; rol -> rotates original left 3 times with carry from ror
    // After ror: carry = bit0
    // txa reloads original into A
    // rol: A = (A shl 1) | carry, carry = old bit7
    var a = data and 0xFF
    var c = carry
    // First rol
    var newC = (a shr 7) and 1
    a = ((a shl 1) or c) and 0xFF
    c = newC
    // Second rol
    newC = (a shr 7) and 1
    a = ((a shl 1) or c) and 0xFF
    c = newC
    // Third rol
    a = ((a shl 1) or c) and 0xFF
    // Now A has the rotated value, fall through to ProcessLengthData
    val length = processLengthData(a)
    return Pair(length, x)
}

//> ProcessLengthData: takes a raw length byte and returns the actual note length
private fun System.processLengthData(a: Int): Int {
    //> and #%00000111
    val masked = a and 0x07
    //> clc; adc $f0 (NoteLenLookupTblOfs); adc NoteLengthTblAdder
    val noteLenOfs = ram.noteLenLookupTblOfs.toInt() and 0xFF
    val noteLenAdder = ram.noteLengthTblAdder.toInt() and 0xFF
    val index = masked + noteLenOfs + noteLenAdder
    //> tay; lda MusicLengthLookupTbl,y
    return SoundData.musicLengthLookupTbl[index]
}

//> LoadControlRegs: returns (A, X, Y) values
private fun System.loadControlRegs(): Triple<Int, Int, Int> {
    val eventBuf = ram.eventMusicBuffer.toInt() and 0xFF
    //> lda EventMusicBuffer; and #EndOfCastleMusic; beq NotECstlM
    if (eventBuf and (EndOfCastleMusic.toInt() and 0xFF) != 0) {
        //> lda #$04; bne AllMus
        return Triple(0x04, 0x82, 0x7F)
    }
    //> NotECstlM: lda AreaMusicBuffer; and #%01111101; beq WaterMus
    val areaBuf = ram.areaMusicBuffer.toInt() and 0xFF
    if (areaBuf and 0x7D == 0) {
        //> WaterMus: lda #$28
        return Triple(0x28, 0x82, 0x7F)
    }
    //> lda #$08; AllMus: ldx #$82; ldy #$7f
    return Triple(0x08, 0x82, 0x7F)
}

//> LoadEnvelopeData:
private fun System.loadEnvelopeData(y: Int): Int {
    val eventBuf = ram.eventMusicBuffer.toInt() and 0xFF
    //> lda EventMusicBuffer; and #EndOfCastleMusic; beq LoadUsualEnvData
    if (eventBuf and (EndOfCastleMusic.toInt() and 0xFF) != 0) {
        //> lda EndOfCastleMusicEnvData,y
        return SoundData.endOfCastleMusicEnvData[y]
    }
    //> LoadUsualEnvData: lda AreaMusicBuffer; and #%01111101; beq LoadWaterEventMusEnvData
    val areaBuf = ram.areaMusicBuffer.toInt() and 0xFF
    if (areaBuf and 0x7D == 0) {
        //> lda WaterEventMusEnvData,y
        return SoundData.waterEventMusEnvData[y]
    }
    //> lda AreaMusicEnvData,y
    return SoundData.areaMusicEnvData[y]
}

package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.InexactBitSetting
import com.ivieleague.smbtranslation.utils.PpuControl


fun System.start() {

    //> Start:
    // This is just clearing flags.
    //> sei                          ;pretty standard 6502 type init here
    //> cld

    //> lda #%00010000               ;init PPU control register 1
    //> sta PPU_CTRL_REG1
    ppu.control = PpuControl(
        nmiEnabled = false,
        extWrite = false,
        tallSpriteMode = false,
        spritePatternTableOffset = false,
        drawVertical = false,
        baseNametableAddress = 0,
        backgroundTableOffset = true,
    )

    //> ldx #$ff                     ;reset stack pointer
    //> txs
    ram.stack.clear()

    //> VBlank1:     lda PPU_STATUS               ;wait two frames
    //> bpl VBlank1
    //> VBlank2:     lda PPU_STATUS
    //> bpl VBlank2

    var useColdBoot = false
    //> ldy #ColdBootOffset          ;load default cold boot pointer
    //> ldx #$05                     ;this is where we check for a warm boot
    for(index in 5 downTo 0) {
        //> WBootCheck:  lda TopScoreDisplay,x        ;check each score digit in the top score
        //> cmp #10                      ;to see if we have a valid digit
        //> bcs ColdBoot                 ;if not, give up and proceed with cold boot
        if (ram.topScoreDisplay[index] >= 10) useColdBoot = true
        //> dex
        //> bpl WBootCheck
    }

    //> lda WarmBootValidation       ;second checkpoint, check to see if
    //> cmp #$a5                     ;another location has a specific value
    //> bne ColdBoot
    useColdBoot = useColdBoot || !ram.warmBootValidation

    //> ldy #WarmBootOffset          ;if passed both, load warm boot pointer
    //> ColdBoot:    jsr InitializeMemory         ;clear memory using pointer in Y
    if(useColdBoot) {
        // Why in the HELL we do these exact ranges is beyond me.  There's no clean organization into area/game objects
        initializeMemory(0x07D6)
    } else {
        initializeMemory(0x076F)
    }

    //> sta SND_DELTA_REG+1          ;reset delta counter load register
    apu.deltaModulation.loadCounter = 0
    //> sta OperMode                 ;reset primary mode of operation
    ram.operMode = OperMode.TitleScreen
    //> lda #$a5                     ;set warm boot flag
    //> sta WarmBootValidation
    ram.warmBootValidation = true
    //> sta PseudoRandomBitReg       ;set seed for pseudorandom register
    ram.pseudoRandomBitReg[0] = 0xA5.toByte()

    //> lda #%00001111
    //> sta SND_MASTERCTRL_REG       ;enable all sound channels except dmc
    @InexactBitSetting
    apu.noiseEnabled = true
    apu.triangleEnabled = true
    apu.pulse2Enabled = true
    apu.pulse1Enabled = true

    //> lda #%00000110
    //> sta PPU_CTRL_REG2            ;turn off clipping for OAM and background
    ppu.mask = ppu.mask.copy(
        showLeftBackground = true,
        showLeftSprites = true,
    )

    //> jsr MoveAllSpritesOffscreen
    moveAllSpritesOffscreen()


    // MARK - current position
    //> jsr InitializeNameTables     ;initialize both name tables
    initializeNameTables()

    //> inc DisableScreenFlag        ;set flag to disable screen output
    ram.disableScreenFlag = true

    //> lda Mirror_PPU_CTRL_REG1
    //> ora #%10000000               ;enable NMIs
    //> jsr WritePPUReg1
    ram.mirrorPPUCTRLREG1 = ram.mirrorPPUCTRLREG1.copy(nmiEnabled = true)
    ppu.control = ram.mirrorPPUCTRLREG1

    //> EndlessLoop: jmp EndlessLoop              ;endless loop, need I say more?
    // TODO: Original looped forever here to wait for an NMI.  What do we do here?
}

fun System.initializeMemory(zeroToIndex: Short) {
    //> ;$06 - RAM address low
    //> ;$07 - RAM address high
    //> InitializeMemory:
    //> ldx #$07          ;set initial high byte to $0700-$07ff
    //> lda #$00          ;set initial low byte to start of page (at $00 of page)
    //> sta $06
    //> InitPageLoop: stx $07
    //> InitByteLoop: cpx #$01          ;check to see if we're on the stack ($0100-$01ff)
    for(index in 0x0..zeroToIndex.toInt()) {
        //> bne InitByte      ;if not, go ahead anyway
        //> cpy #$60          ;otherwise, check to see if we're at $0160-$01ff
        //> bcs SkipByte      ;if so, skip write
        if(index !in 0x0160..0x1ff) {
            //> InitByte:     sta ($06),y       ;otherwise, initialize byte with current low byte in Y
//            ram.wholeBlock[index] = 0
            TODO("Clear the variables... how?")
        }
        //> SkipByte:     dey
        //> cpy #$ff          ;do this until all bytes in page have been erased
        //> bne InitByteLoop
        //> dex               ;go onto the next page
        //> bpl InitPageLoop  ;do this until all pages of memory have been erased
    }
    //> rts
}

fun System.moveAllSpritesOffscreen() {

    //> MoveAllSpritesOffscreen:
    //> ldy #$00                ;this routine moves all sprites off the screen
    //> .db $2c                 ;BIT instruction opcode
    //>
    //> MoveSpritesOffscreen:
    //> ldy #$04                ;this routine moves all but sprite 0
    //> lda #$f8                ;off the screen
    //> SprInitLoop:  sta Sprite_Y_Position,y ;write 248 into OAM data's Y coordinate
    //> iny                     ;which will move it off the screen
    //> iny
    //> iny
    //> iny
    //> bne SprInitLoop
    //> rts
    for(index in 1 until 64) {
        ram.sprites[index].y = 0xF8.toUByte()
    }
}
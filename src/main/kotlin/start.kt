package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.*
import com.ivieleague.smbtranslation.utils.InexactBitSetting
import com.ivieleague.smbtranslation.utils.PpuControl
import com.ivieleague.smbtranslation.utils.SpriteFlags
import kotlin.experimental.and
import kotlin.experimental.or

// Default OAM data offset table used to initialize SprDataOffset ($06e4-$06f2)
private val DefaultSprOffsets: List<Byte> = listOf(
    0x04, 0x30, 0x48, 0x60, 0x78, 0x90.toByte(), 0xA8.toByte(), 0xC0.toByte(),
    0xD8.toByte(), 0xE8.toByte(), 0x24, 0xF8.toByte(), 0xFC.toByte(), 0x28, 0x2C
)

// Sprite #0 setup data: Y, tile, attributes, X
private val Sprite0Data = GameRam.Sprite(
    y = 0x18.toUByte(),
    tilenumber = 0xFF.toByte(),
    attributes = SpriteFlags(0x23.toByte()),
    x = 0x58.toUByte(),
)

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
    apu.dmcEnabled = false
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
    //> bne InitByte      ;if not, go ahead anyway
    //> cpy #$60          ;otherwise, check to see if we're at $0160-$01ff
    //> bcs SkipByte      ;if so, skip write
    //> InitByte:     sta ($06),y       ;otherwise, initialize byte with current low byte in Y
    //> SkipByte:     dey
    //> cpy #$ff          ;do this until all bytes in page have been erased
    //> bne InitByteLoop
    //> dex               ;go onto the next page
    //> bpl InitPageLoop  ;do this until all pages of memory have been erased
    //> rts
    ram.reset(0x0..<0x160)
    ram.reset(0x200..<zeroToIndex)
}

fun System.secondaryGameSetup() {
    //> SecondaryGameSetup:
    //> lda #$00
    //> sta DisableScreenFlag     ;enable screen output
    ram.disableScreenFlag = false

    //> tay
    //> ClearVRLoop: sta VRAM_Buffer1-1,y      ;clear buffer at $0300-$03ff
    //> iny
    //> bne ClearVRLoop
    // In our high-level model, vRAMBuffer1 is a list of buffered PPU updates. Clearing the $0300-$03ff
    // region corresponds to clearing this buffer.
    ram.vRAMBuffer1.clear()

    //> sta GameTimerExpiredFlag  ;clear game timer exp flag
    ram.gameTimerExpiredFlag = false
    //> sta DisableIntermediate   ;clear skip lives display flag
    ram.disableIntermediate = false
    //> sta BackloadingFlag       ;clear value here
    ram.backloadingFlag = false

    //> lda #$ff
    //> sta BalPlatformAlignment  ;initialize balance platform assignment flag
    ram.balPlatformAlignment = 0xFF.toByte()

    //> lda ScreenLeft_PageLoc    ;get left side page location
    //> lsr Mirror_PPU_CTRL_REG1  ;shift LSB of ppu register #1 mirror out
    //> and #$01                  ;mask out all but LSB of page location
    //> ror                       ;rotate LSB of page location into carry then onto mirror
    //> rol Mirror_PPU_CTRL_REG1  ;this is to set the proper PPU name table
    // High-level: copy the LSB of ScreenLeft_PageLoc into bit 0 of baseNametableAddress,
    // preserving bit 1.
    ram.mirrorPPUCTRLREG1 = ram.mirrorPPUCTRLREG1.copy(
        baseNametableAddress = (ram.mirrorPPUCTRLREG1.baseNametableAddress and 0x2) or (ram.screenLeftPageLoc and 0x1)
    )

    //> jsr GetAreaMusic          ;load proper music into queue
    getAreaMusic()

    //> lda #$38                  ;load sprite shuffle amounts to be used later
    //> sta SprShuffleAmt+2
    ram.sprShuffleAmt[2] = 0x38
    //> lda #$48
    //> sta SprShuffleAmt+1
    ram.sprShuffleAmt[1] = 0x48
    //> lda #$58
    //> sta SprShuffleAmt
    ram.sprShuffleAmt[0] = 0x58

    //> ldx #$0e                  ;load default OAM offsets into $06e4-$06f2
    //> ShufAmtLoop: lda DefaultSprOffsets,x
    //>             sta SprDataOffset,x
    //>             dex                       ;do this until they're all set
    //>             bpl ShufAmtLoop
    for (i in DefaultSprOffsets.indices) {
        ram.sprDataOffsets[i] = DefaultSprOffsets[i]
    }

    //> ldy #$03                  ;set up sprite #0
    //> ISpr0Loop:   lda Sprite0Data,y
    //>             sta Sprite_Data,y
    //>             dey
    //>             bpl ISpr0Loop
    // High-level: write sprite #0's Y, tile, attributes, X
    ram.sprites[0].set(Sprite0Data)

    //> jsr DoNothing2            ;these jsrs doesn't do anything useful
    //> jsr DoNothing1

    //> inc Sprite0HitDetectFlag  ;set sprite #0 check flag
    ram.sprite0HitDetectFlag = true
    //> inc OperMode_Task         ;increment to next task
    ram.operModeTask++
    //> rts
}


fun System.moveAllSpritesOffscreen() {
    //> MoveAllSpritesOffscreen:
    //> ldy #$00                ;this routine moves all sprites off the screen
    //> .db $2c                 ;BIT instruction opcode
    // falls through to normal MoveSpritesOffscreen in the assembly
    for (index in 0 until 64) {
        ram.sprites[index].y = 0xF8.toUByte()
    }
}
fun System.moveSpritesOffscreen() {
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
    for (index in 1 until 64) {
        ram.sprites[index].y = 0xF8.toUByte()
    }
}

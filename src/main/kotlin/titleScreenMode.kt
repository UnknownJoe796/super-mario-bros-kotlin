package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.*
import com.ivieleague.smbtranslation.chr.OriginalRom
import com.ivieleague.smbtranslation.chr.rawChrData
import com.ivieleague.smbtranslation.utils.JoypadBits
import java.io.File
import kotlin.experimental.or
import kotlin.experimental.xor


fun System.titleScreenMode(): Unit {
    //> TitleScreenMode:
    //> lda OperMode_Task
    //> jsr JumpEngine

    //> .dw InitializeGame
    //> .dw ScreenRoutines
    //> .dw PrimaryGameSetup
    //> .dw GameMenuRoutine
    when(ram.operModeTask.toInt()) {
        0 -> initializeGame()
        1 -> screenRoutines()
        2 -> primaryGameSetup()
        3 -> gameMenuRoutine()
        else -> throw IllegalStateException()
    }
}

fun System.gameMenuRoutine() {
    //> GameMenuRoutine:
    //> ldy #$00
    //> lda SavedJoypad1Bits        ;check to see if either player pressed
    //> ora SavedJoypad2Bits        ;only the start button (either joypad)
    val eitherController = JoypadBits(ram.savedJoypad1Bits.byte or ram.savedJoypad2Bits.byte)
    when {
        //> cmp #Start_Button
        //> beq StartGame
        eitherController == JoypadBits(start = true) ||
                //> cmp #A_Button+Start_Button  ;check to see if A + start was pressed
                //> bne ChkSelect               ;if not, branch to check select button
                //> StartGame:    jmp ChkContinue             ;if either start or A + start, execute here
                eitherController == JoypadBits(start = true, a = true) -> chkContinue(eitherController)

        //> ChkSelect:    cmp #Select_Button          ;check to see if the select button was pressed
        //> beq SelectBLogic            ;if so, branch reset demo timer
        eitherController == JoypadBits(select = true) ||
                //> ldx DemoTimer               ;otherwise check demo timer
                //> bne ChkWorldSel             ;if demo timer not expired, branch to check world selection

                // This section will go down to the else
                //> sta SelectTimer             ;set controller bits here if running demo
                //> jsr DemoEngine              ;run through the demo actions
                //> bcs ResetTitle              ;if carry flag set, demo over, thus branch
                //> jmp RunDemo                 ;otherwise, run game engine for demo

                //> ChkWorldSel:  ldx WorldSelectEnableFlag   ;check to see if world selection has been enabled
                //> beq NullJoypad
                //> cmp #B_Button               ;if so, check to see if the B button was pressed
                //> bne NullJoypad
                // reset demo timer
                //> iny                         ;if so, increment Y and execute same code as select
                ram.demoTimer != 0.toByte() && ram.worldSelectEnableFlag && eitherController == JoypadBits(
            b = true
        ) -> {
            //> SelectBLogic: lda DemoTimer               ;if select or B pressed, check demo timer one last time
            //> beq ResetTitle              ;if demo timer expired, branch to reset title screen mode
            if (ram.demoTimer == 0.toByte()) return resetTitle()
            //> lda #$18                    ;otherwise reset demo timer
            //> sta DemoTimer
            ram.demoTimer = 0x18.toByte()
            //> lda SelectTimer             ;check select/B button timer
            //> bne NullJoypad              ;if not expired, branch
            if (ram.selectTimer != 0.toByte()) return nullJoypad()
            //> lda #$10                    ;otherwise reset select button timer
            //> sta SelectTimer
            ram.selectTimer = 0x10.toByte()
            //> cpy #$01                    ;was the B button pressed earlier?  if so, branch
            //> beq IncWorldSel             ;note this will not be run if world selection is disabled
            if (eitherController == JoypadBits(b = true)) return incWorldSel()
            //> lda NumberOfPlayers         ;if no, must have been the select button, therefore
            //> eor #%00000001              ;change number of players and draw icon accordingly
            //> sta NumberOfPlayers
            ram.numberOfPlayers = (ram.numberOfPlayers xor 0x01).toByte()
            //> jsr DrawMushroomIcon
            drawMushroomIcon()
            //> jmp NullJoypad
            return nullJoypad()
        }

        else -> {
            if (ram.demoTimer == 0.toByte()) {
                // set controller bits here if running demo
                ram.selectTimer = eitherController.byte
                // run through the demo actions; if carry flag set, demo over
                if (demoEngine()) resetTitle() else runDemo()
            } else {
                // DemoTimer != 0 and not B-only world select -> NullJoypad then RunDemo
                nullJoypad()
            }
        }
    }
}

//> WSelectBufferTemplate:
//> .db $04, $20, $73, $01, $00, $00
fun System.wSelectBufferTemplate(worldNumber: Byte) = listOf<BufferedPpuUpdate>(
    BufferedPpuUpdate.BackgroundPatternRepeat(
        nametable = 0,
        x = (0x73 % 0x20).toByte(),
        y = (0x73 / 0x20).toByte(),
        repetitions = 1,
        drawVertically = true,
        pattern = OriginalRom.backgrounds[worldNumber]
    )
)
private fun System.incWorldSel() {
    //> IncWorldSel:  ldx WorldSelectNumber       ;increment world select number
    //> inx
    var x = ram.worldSelectNumber + 1
    //> txa
    //> and #%00000111              ;mask out higher bits
    //> sta WorldSelectNumber       ;store as current world select number
    ram.worldSelectNumber = (x and 0x7).toByte()
    //> jsr GoContinue
    goContinue(ram.worldSelectNumber)
    //> UpdateShroom: lda WSelectBufferTemplate,x ;write template for world select in vram buffer
    //> sta VRAM_Buffer1-1,x        ;do this until all bytes are written
    //> inx
    //> cpx #$06
    //> bmi UpdateShroom
    // Copies in the template
    //> ldy WorldNumber             ;get world number from variable and increment for
    //> iny                         ;proper display, and put in blank byte before
    //> sty VRAM_Buffer1+3          ;null terminator
    // Updates the template with the world number
    // This code rewrites the world number.
    ram.vRAMBuffer1.clear()
    ram.vRAMBuffer1.addAll(wSelectBufferTemplate(ram.worldNumber))
    return nullJoypad()  // continue on
}
private fun System.nullJoypad() {
    //> NullJoypad:   lda #$00                    ;clear joypad bits for player 1
    //> sta SavedJoypad1Bits
    ram.savedJoypad1Bits = JoypadBits(0)
    return runDemo()
}
private fun System.runDemo() {
    //> RunDemo:      jsr GameCoreRoutine         ;run game engine
    gameCoreRoutine()
    //> lda GameEngineSubroutine    ;check to see if we're running lose life routine
    //> cmp #$06
    //> bne ExitMenu                ;if not, do not do all the resetting below
    if (ram.gameEngineSubroutine != 0x06.toByte()) return
    return resetTitle()
}
private fun System.resetTitle() {
    //> ResetTitle:   lda #$00                    ;reset game modes, disable
    //> sta OperMode                ;sprite 0 check and disable
    ram.operMode = OperMode.TitleScreen
    //> sta OperMode_Task           ;screen output
    ram.operModeTask = 0x00.toByte()
    //> sta Sprite0HitDetectFlag
    ram.sprite0HitDetectFlag = false
    //> inc DisableScreenFlag
    ram.disableScreenFlag = true
    //> rts
}
private fun System.chkContinue(joypadBits: JoypadBits) {
    //> ChkContinue:  ldy DemoTimer               ;if timer for demo has expired, reset modes
    //> beq ResetTitle
    if(ram.demoTimer == 0.toByte()) return resetTitle()
    //> asl                         ;check to see if A button was also pushed
    //> bcc StartWorld1             ;if not, don't load continue function's world number
    if(joypadBits.a) {
        //> lda ContinueWorld           ;load previously saved world number for secret
        //> jsr GoContinue              ;continue function when pressing A + start
        goContinue(ram.continueWorld)
    }
    //> StartWorld1:  jsr LoadAreaPointer
    loadAreaPointer()
    //> inc Hidden1UpFlag           ;set 1-up box flag for both players
    ram.hidden1UpFlag = true
    //> inc OffScr_Hidden1UpFlag
    ram.offScrHidden1UpFlag = true
    //> inc FetchNewGameTimerFlag   ;set fetch new game timer flag
    ram.fetchNewGameTimerFlag = true
    //> inc OperMode                ;set next game mode
    ram.operMode = OperMode.Game
    //> lda WorldSelectEnableFlag   ;if world select flag is on, then primary
    //> sta PrimaryHardMode         ;hard mode must be on as well
    ram.primaryHardMode = ram.worldSelectEnableFlag
    //> lda #$00
    //> sta OperMode_Task           ;set game mode here, and clear demo timer
    ram.operModeTask = 0x00.toByte()
    //> sta DemoTimer
    ram.demoTimer = 0x00.toByte()
    //> ldx #$17
    //> lda #$00
    //> InitScores:   sta ScoreAndCoinDisplay,x   ;clear player scores and coin displays
    //> dex
    //> bpl InitScores
    ram.playerScoreDisplay.zeros()
    ram.player2ScoreDisplay.zeros()
    ram.coinDisplay.zeros()
    ram.coin2Display.zeros()
    //> ExitMenu:     rts
}
private fun ByteArray.zeros() {
    for (i in indices) {
        this[i] = 0
    }
}
private fun System.goContinue(worldNumber: Byte) {
    //> GoContinue:   sta WorldNumber             ;start both players at the first area
    ram.worldNumber = worldNumber
    //> sta OffScr_WorldNumber      ;of the previously saved world number
    ram.offScrWorldNumber = worldNumber
    //> ldx #$00                    ;note that on power-up using this function
    //> stx AreaNumber              ;will make no difference
    ram.areaNumber = 0
    //> stx OffScr_AreaNumber
    ram.offScrAreaNumber = 0
    //> rts
}

fun System.initializeGame() {
    //> InitializeGame:
    //> ldy #$6f              ;clear all memory as in initialization procedure,
    //> jsr InitializeMemory  ;but this time, clear only as far as $076f
    initializeMemory(0x076f)

    //> ldy #$1f
    //> ClrSndLoop:  sta SoundMemory,y     ;clear out memory used
    //> dey                   ;by the sound engines
    //> bpl ClrSndLoop
    // Our sound "memory" is modeled by discrete queues/buffers; clear the common ones the engine touches.
    ram.reset(0x07b0..(0x07b0 + 0x1f))

    //> lda #$18              ;set demo timer
    //> sta DemoTimer
    ram.demoTimer = 0x18.toByte()
    //> jsr LoadAreaPointer
    loadAreaPointer()
    // by Claude - In the NES, the AreaData pointer ($E7/$E8) is set by GetAreaDataAddrs
    // (called from InitializeArea). The area parser at ScrTask=8 reads from this pointer.
    // In Kotlin, ram.areaData is a nullable ByteArray that must be explicitly set.
    // Call getAreaDataAddrs() here to load the demo level's area data.
    getAreaDataAddrs()
    println("DEBUG initializeGame: areaData=${ram.areaData?.size}, areaPointer=${ram.areaPointer}, areaType=${ram.areaType}, worldNumber=${ram.worldNumber}, areaNumber=${ram.areaNumber}")

    // After initialization, the original falls through to InitializeArea via the jump table sequencing.
    // Here we simply advance the mode task to mirror that control flow.
    ram.operModeTask++
}
// by Claude - PrimaryGameSetup falls through to SecondaryGameSetup in the assembly
// (no RTS between them). SecondaryGameSetup handles screen enable, sprite setup,
// music loading, and increments OperMode_Task.
fun System.primaryGameSetup() {
    //> PrimaryGameSetup:
    //> lda #$01
    //> sta FetchNewGameTimerFlag   ;set flag to load game timer from header
    //> sta PlayerSize              ;set player's size to small
    ram.fetchNewGameTimerFlag = true
    ram.playerSize = 0x01
    //> lda #$02
    //> sta NumberofLives           ;give each player three lives
    //> sta OffScr_NumberofLives
    ram.numberofLives = 0x02
    ram.offScrNumberofLives = 0x02
    //> (falls through to SecondaryGameSetup)
    secondaryGameSetup()
}

//> MushroomIconData:
//> .db $07, $22, $49, $83, $ce, $24, $24, $00
val mushroomIconData = listOf(
    BufferedPpuUpdate.BackgroundPatternString(
        nametable = 0,
        x = (0x249 % 0x20).toByte(),
        y = (0x249 / 0x20).toByte(),
        drawVertically = true,
        patterns = listOf(
            OriginalRom.backgrounds[0xce],
            OriginalRom.backgrounds[0x24],
            OriginalRom.backgrounds[0x24],
        )
    )
)

fun System.drawMushroomIcon() {
    //> DrawMushroomIcon:
    //> ldy #$07                ;read eight bytes to be read by transfer routine
    //> IconDataRead: lda MushroomIconData,y  ;note that the default position is set for a
    //> sta VRAM_Buffer1-1,y    ;1-player game
    //> dey
    //> bpl IconDataRead
    ram.vRAMBuffer1.addAll(mushroomIconData)

    //> lda NumberOfPlayers     ;check number of players
    //> beq ExitIcon            ;if set to 1-player game, we're done
    if (ram.numberOfPlayers != 0.toByte()) {
        ram.vRAMBuffer1[0] = (ram.vRAMBuffer1[0] as BufferedPpuUpdate.BackgroundPatternString).let {
            it.copy(patterns = listOf(
            //> lda #$24                ;otherwise, load blank tile in 1-player position
            //> sta VRAM_Buffer1+3
                OriginalRom.backgrounds[0x24],
                OriginalRom.backgrounds[0x24],
                //> lda #$ce                ;then load shroom icon tile in 2-player position
                //> sta VRAM_Buffer1+5
                OriginalRom.backgrounds[0xce],
            ))
        }
    }
    //> ExitIcon:     rts
}

private val originalRom = File("smb.nes").readBytes()
fun System.drawTitleScreen() {
    //> DrawTitleScreen:
    //> lda OperMode                 ;are we in title screen mode?
    if (ram.operMode != OperMode.TitleScreen) {
        //> bne IncModeTask_B            ;if not, exit
        //! Inlined
        ram.operModeTask++
        return
    }
    //> lda #>TitleScreenDataOffset  ;load address $1ec0 into
    //> sta PPU_ADDRESS              ;the vram address register
    //> lda #<TitleScreenDataOffset
    //> sta PPU_ADDRESS
    //> lda #$03                     ;put address $0300 into
    //> sta $01                      ;the indirect at $00
    //> ldy #$00
    //> sty $00
    //> lda PPU_DATA                 ;do one garbage read
    //> OutputTScr: lda PPU_DATA                 ;get title screen from chr-rom
    //> sta ($00),y                  ;store 256 bytes into buffer
    //> iny
    //> bne ChkHiByte                ;if not past 256 bytes, do not increment
    //> inc $01                      ;otherwise increment high byte of indirect
    //> ChkHiByte:  lda $01                      ;check high byte?
    //> cmp #$04                     ;at $0400?
    //> bne OutputTScr               ;if not, loop back and do another
    //> cpy #$3a                     ;check if offset points past end of data
    //> bcc OutputTScr               ;if not, loop back and do another
    //> lda #$05                     ;set buffer transfer control to $0300,
    //> jmp SetVRAMAddr_B            ;increment task and exit

    // Title screen pattern data lives in CHR at $1EC0
    val titleScreenDataOffset = 0x1EC0
    val totalBytes = 0x13A

    // Ensure CHR is loaded (rawChrData returns a zeroed array if ROM missing)
    val vramBufferBytes = rawChrData.copyOfRange(0x1EC0, 0x1EC0 + 0x13A)

    ram.vRAMBuffer1.clear()
    ram.vRAMBuffer1.addAll(BufferedPpuUpdate.parseVramBuffer(vramBufferBytes))
    ram.vRAMBufferAddrCtrl = 5
    ram.operModeTask++
}


//> DemoActionData:
//> .db $01, $80, $02, $81, $41, $80, $01
//> .db $42, $c2, $02, $80, $41, $c1, $41, $c1
//> .db $01, $c1, $01, $02, $80, $00
val demoActionData: List<JoypadBits> = listOf<Byte>(
    0x01,
    0x80.toByte(), 0x02, 0x81.toByte(), 0x41, 0x80.toByte(), 0x01,
    0x42, 0xC2.toByte(), 0x02, 0x80.toByte(), 0x41, 0xC1.toByte(), 0x41, 0xC1.toByte(),
    0x01, 0xC1.toByte(), 0x01, 0x02, 0x80.toByte(), 0x00
).map { JoypadBits(it) }

//> DemoTimingData:
//> .db $9b, $10, $18, $05, $2c, $20, $24
//> .db $15, $5a, $10, $20, $28, $30, $20, $10
//> .db $80, $20, $30, $30, $01, $ff, $00
val demoTimingData: List<Byte> = listOf(
    0x9B.toByte(), 0x10, 0x18, 0x05, 0x2C.toByte(), 0x20, 0x24,
    0x15, 0x5A.toByte(), 0x10, 0x20, 0x28, 0x30, 0x20, 0x10,
    0x80.toByte(), 0x20, 0x30, 0x30, 0x01, 0xFF.toByte(), 0x00
)

/**
 * Kotlin translation of DemoEngine.
 * @return True if the demo is done (carry set in original), false if still running (carry clear)
 */
fun System.demoEngine(): Boolean {
    //> DemoEngine:
    //> ldx DemoAction         ;load current demo action
    var x = ram.demoAction.toInt()
    //> lda DemoActionTimer    ;load current action timer
    //> bne DoAction           ;if timer still counting down, skip
    if (ram.demoActionTimer == 0.toByte()) {
        //> inx
        //> inc DemoAction         ;if expired, increment action, X, and
        x += 1
        ram.demoAction = x.toByte()
        //> sec                    ;set carry by default for demo over
        var carry = true
        //> lda DemoTimingData-1,x ;get next timer
        val nextTimer = demoTimingData.getOrNull(x - 1) ?: 0.toByte()
        //> sta DemoActionTimer    ;store as current timer
        ram.demoActionTimer = nextTimer
        //> beq DemoOver           ;if timer already at zero, skip
        if (nextTimer == 0.toByte()) {
            //> DemoOver: rts
            return true
        }
        // if timer was nonzero, we fall through to DoAction with carry currently set,
        // but the routine will clear it before returning.
    }

    //> DoAction: lda DemoActionData-1,x ;get and perform action (current or next)
    val actionByte = demoActionData.getOrNull(x - 1) ?: JoypadBits(0)
    //> sta SavedJoypad1Bits
    ram.savedJoypad1Bits = actionByte
    //> dec DemoActionTimer    ;decrement action timer
    ram.demoActionTimer = (ram.demoActionTimer - 1).toByte()
    //> clc                    ;clear carry if demo still going
    //> DemoOver: rts
    return false
}


fun System.initializeArea() {
    //> InitializeArea:
    //> ldy #$4b                 ;clear all memory again, only as far as $074b
    //> jsr InitializeMemory     ;this is only necessary if branching from
    initializeMemory(0x074b)

    //> ldx #$21
    //> lda #$00
    //> ClrTimersLoop: sta Timers,x             ;clear out memory between
    //> dex                      ;$0780 and $07a1
    //> bpl ClrTimersLoop
    for (i in ram.timers.indices) ram.timers[i] = 0

    //> lda HalfwayPage
    //> ldy AltEntranceControl   ;if AltEntranceControl not set, use halfway page, if any found
    //> beq StartPage
    //> lda EntrancePage         ;otherwise use saved entry page number here
    val startPage: Byte = if (ram.altEntranceControl == 0.toByte()) ram.halfwayPage else ram.entrancePage

    //> StartPage:     sta ScreenLeft_PageLoc   ;set as value here
    //> sta CurrentPageLoc       ;also set as current page
    //> sta BackloadingFlag      ;set flag here if halfway page or saved entry page number found
    ram.screenLeftPageLoc = startPage
    ram.currentPageLoc = startPage.toUByte()
    ram.backloadingFlag = startPage != 0.toByte()

    //> jsr GetScreenPosition    ;get pixel coordinates for screen borders
    getScreenPosition()
    // by Claude - getScreenPosition() sets ram.screenRightPageLoc; read it after the call
    val rightSideScreenPage = ram.screenRightPageLoc.toInt() and 0xFF

    //> ldy #$20                 ;if on odd numbered page, use $2480 as start of rendering
    //> and #%00000001           ;otherwise use $2080, this address used later as name table
    //> beq SetInitNTHigh        ;address for rendering of game area
    //> ldy #$24
    //> SetInitNTHigh: sty CurrentNTAddr_High   ;store name table address
    val rightSideScreenPageModTwo = rightSideScreenPage % 2
    ram.currentNTAddrHigh = if (rightSideScreenPageModTwo == 1) 0x24.toByte() else 0x20.toByte()
    //> ldy #$80
    //> sty CurrentNTAddr_Low
    ram.currentNTAddrLow = 0x80.toByte()

    //> asl                      ;store LSB of page number in high nybble
    //> asl                      ;of block buffer column position
    //> asl
    //> asl
    //> sta BlockBufferColumnPos
    ram.blockBufferColumnPos = (rightSideScreenPageModTwo shl 4).toByte()

    //> dec AreaObjectLength     ;set area object lengths for all empty
    ram.areaObjectLength[0]--
    //> dec AreaObjectLength+1
    ram.areaObjectLength[1]--
    //> dec AreaObjectLength+2
    ram.areaObjectLength[2]--

    //> lda #$0b                 ;set value for renderer to update 12 column sets
    //> sta ColumnSets           ;12 column sets = 24 metatile columns = 1 1/2 screens
    ram.columnSets = 0x0b

    //> jsr GetAreaDataAddrs     ;get enemy and level addresses and load header
    getAreaDataAddrs()

    //> lda PrimaryHardMode      ;check to see if primary hard mode has been activated
    //> bne SetSecHard           ;if so, activate the secondary no matter where we're at
    val secHard = if (ram.primaryHardMode) true else when {
        //> lda WorldNumber          ;otherwise check world number
        //> cmp #World5              ;if less than 5, do not activate secondary
        //> bcc CheckHalfway
        ram.worldNumber.toUByte() < Constants.World5.toUByte() -> false
        //> bne SetSecHard           ;if not equal to, then world > 5, thus activate
        ram.worldNumber != Constants.World5 -> true
        //> lda LevelNumber          ;otherwise, world 5, so check level number
        //> cmp #Level3              ;if 1 or 2, do not set secondary hard mode flag
        //> bcc CheckHalfway
        else -> ram.levelNumber >= Constants.Level3
    }
    //> SetSecHard:    inc SecondaryHardMode    ;set secondary hard mode flag for areas 5-3 and beyond
    if (secHard) ram.secondaryHardMode = (ram.secondaryHardMode + 1).toByte()

    //> CheckHalfway:  lda HalfwayPage
    //> beq DoneInitArea
    if (ram.halfwayPage != 0.toByte()) {
        //> lda #$02                 ;if halfway page set, overwrite start position from header
        //> sta PlayerEntranceCtrl
        ram.playerEntranceCtrl = 0x02
    }

    //> DoneInitArea:  lda #Silence             ;silence music
    //> sta AreaMusicQueue
    ram.areaMusicQueue = Constants.Silence
    //> lda #$01                 ;disable screen output
    //> sta DisableScreenFlag
    ram.disableScreenFlag = true
    //> inc OperMode_Task        ;increment one of the modes
    ram.operModeTask++
    //> rts
}

// by Claude - getScreenPosition() moved to scrollHandler.kt
// by Claude - Translated from assembly subroutines LoadAreaPointer, FindAreaPointer,
// GetAreaType, and GetAreaDataAddrs in smbdism.asm.

/**
 * Looks up the area pointer from WorldAddrOffsets + AreaAddrOffsets tables based on
 * the current world and area numbers, stores it, and derives the area type.
 */
fun System.loadAreaPointer() {
    //> LoadAreaPointer:
    //> jsr FindAreaPointer  ;find it and store it here

    // --- FindAreaPointer inlined ---
    //> ldy WorldNumber        ;load offset from world variable
    //> lda WorldAddrOffsets,y
    val worldOffset = RomData.worldAddrOffsets[ram.worldNumber.toInt() and 0xFF]
    //> clc                    ;add area number used to find data
    //> adc AreaNumber
    //> tay
    val areaIdx = (worldOffset + (ram.areaNumber.toInt() and 0xFF)) and 0xFF
    //> lda AreaAddrOffsets,y  ;from there we have our area pointer
    val pointer = RomData.areaAddrOffsets[areaIdx]

    //> sta AreaPointer
    ram.areaPointer = pointer.toByte()

    // --- GetAreaType inlined (also called standalone by getAreaDataAddrs) ---
    //> GetAreaType: and #%01100000       ;mask out all but d6 and d5
    //> asl / rol / rol / rol             ;make %0xx00000 into %000000xx
    //> sta AreaType
    ram.areaType = ((pointer and 0x60) shr 5).toByte()
}

/**
 * Extracts the area type (bits 6-5) from a raw area pointer value.
 * Equivalent to the GetAreaType subroutine: masks bits 6-5 and rotates them
 * to the two LSBs, yielding a value 0-3.
 */
private fun getAreaType(rawPointer: Int): Int = (rawPointer and 0x60) shr 5

/**
 * Uses the area pointer to look up both the enemy data and level (area) data arrays,
 * assigns them to RAM, then parses the 2-byte level header to set foreground scenery,
 * background color control, player entrance control, game timer setting, terrain control,
 * background scenery, cloud type override, and area style.
 */
private fun System.getAreaDataAddrs() {
    //> GetAreaDataAddrs:
    //> lda AreaPointer          ;use 2 MSB for Y
    val rawPointer = ram.areaPointer.toInt() and 0xFF
    //> jsr GetAreaType
    //> tay
    val areaType = getAreaType(rawPointer)
    //> sta AreaType
    ram.areaType = areaType.toByte()

    //> lda AreaPointer          ;mask out all but 5 LSB
    //> and #%00011111
    //> sta AreaAddrsLOffset     ;save as low offset
    val lowOffset = rawPointer and 0x1F
    ram.areaAddrsLOffset = lowOffset.toByte()

    // --- Look up enemy data array ---
    //> lda EnemyAddrHOffsets,y  ;load base value with 2 altered MSB
    //> clc
    //> adc AreaAddrsLOffset     ;becomes offset for level data
    //> tay
    val enemyIdx = (RomData.enemyAddrHOffsets[areaType] + lowOffset) and 0xFF
    //> lda EnemyDataAddrLow,y   ;use offset to load pointer
    //> sta EnemyDataLow
    //> lda EnemyDataAddrHigh,y
    //> sta EnemyDataHigh
    ram.enemyDataBytes = RomData.enemyDataArrays[enemyIdx]
    // The original stores low/high address bytes; we keep them for compatibility with
    // code that reads them, but the actual data access uses enemyDataBytes.
    ram.enemyDataLow = 0  // placeholder — not used for Kotlin data access
    ram.enemyDataHigh = 0

    // --- Look up area (level) data array ---
    //> ldy AreaType             ;use area type as offset
    //> lda AreaDataHOffsets,y   ;do the same thing but with different base value
    //> clc
    //> adc AreaAddrsLOffset
    //> tay
    val areaDataIdx = (RomData.areaDataHOffsets[areaType] + lowOffset) and 0xFF
    //> lda AreaDataAddrLow,y    ;use this offset to load another pointer
    //> sta AreaDataLow
    //> lda AreaDataAddrHigh,y
    //> sta AreaDataHigh
    val levelData = RomData.areaDataArrays[areaDataIdx]
    ram.areaData = levelData

    // --- Parse 2-byte level header ---
    //> ldy #$00                 ;load first byte of header
    //> lda (AreaData),y
    val headerByte0 = levelData[0].toInt() and 0xFF

    //> and #%00000111           ;save 3 LSB for foreground scenery or bg color control
    //> cmp #$04
    //> bcc StoreFore
    val low3 = headerByte0 and 0x07
    if (low3 >= 0x04) {
        //> sta BackgroundColorCtrl  ;if 4 or greater, save value here as bg color control
        ram.backgroundColorCtrl = low3.toByte()
        //> lda #$00
        //> StoreFore: sta ForegroundScenery
        ram.foregroundScenery = 0
    } else {
        //> StoreFore: sta ForegroundScenery    ;if less, save value here as foreground scenery
        ram.foregroundScenery = low3.toByte()
    }

    //> and #%00111000           ;save player entrance control bits
    //> lsr / lsr / lsr
    //> sta PlayerEntranceCtrl
    ram.playerEntranceCtrl = ((headerByte0 and 0x38) shr 3).toByte()

    //> and #%11000000           ;save 2 MSB for game timer setting
    //> clc / rol / rol / rol    ;rotate to LSBs
    //> sta GameTimerSetting
    ram.gameTimerSetting = ((headerByte0 and 0xC0) ushr 6).toByte()

    //> iny
    //> lda (AreaData),y         ;load second byte of header
    val headerByte1 = levelData[1].toInt() and 0xFF

    //> and #%00001111           ;mask out all but lower nybble
    //> sta TerrainControl
    ram.terrainControl = (headerByte1 and 0x0F).toByte()

    //> and #%00110000           ;save 2 bits for background scenery type
    //> lsr / lsr / lsr / lsr
    //> sta BackgroundScenery
    ram.backgroundScenery = ((headerByte1 and 0x30) shr 4).toByte()

    //> and #%11000000
    //> clc / rol / rol / rol    ;rotate to LSBs
    //> cmp #%00000011           ;if set to 3, store here and nullify other value
    //> bne StoreStyle
    val styleBits = (headerByte1 and 0xC0) ushr 6
    if (styleBits == 0x03) {
        //> sta CloudTypeOverride    ;store value in cloud type override
        ram.cloudTypeOverride = true
        //> lda #$00
        //> StoreStyle: sta AreaStyle
        ram.areaStyle = 0
    } else {
        //> StoreStyle: sta AreaStyle
        ram.areaStyle = styleBits.toByte()
    }

    //> lda AreaDataLow          ;increment area data address by 2 bytes
    //> clc / adc #$02 / sta AreaDataLow
    //> lda AreaDataHigh / adc #$00 / sta AreaDataHigh
    // In Kotlin, we advance the areaDataOffset past the 2-byte header so that
    // subsequent reads via areaData[areaDataOffset] start at the object data.
    ram.areaDataOffset = 0x02
}
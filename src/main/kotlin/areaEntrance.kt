@file:OptIn(ExperimentalUnsignedTypes::class)

package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.*
import com.ivieleague.smbtranslation.utils.SpriteFlags

/**
 * Translation of music selection and player entrance setup routines.
 * Each block is annotated with //> comments that mirror the original 6502 assembly.
 */

// ---- Data tables (originally .db) ----

//> MusicSelectData:
//> .db WaterMusic, GroundMusic, UndergroundMusic, CastleMusic
//> .db CloudMusic, PipeIntroMusic
private val MusicSelectData: ByteArray = byteArrayOf(
    Constants.WaterMusic,
    Constants.GroundMusic,
    Constants.UndergroundMusic,
    Constants.CastleMusic,
    Constants.CloudMusic,
    Constants.PipeIntroMusic,
)

//> PlayerStarting_X_Pos:
//> .db $28, $18
//> .db $38, $28
private val PlayerStarting_X_Pos: UByteArray = ubyteArrayOf(
    0x28u, 0x18u, 0x38u, 0x28u
)

//> AltYPosOffset:
//> .db $08, $00
private val AltYPosOffset: ByteArray = byteArrayOf(
    0x08, 0x00
)

//> PlayerStarting_Y_Pos:
//> .db $00, $20, $b0, $50, $00, $00, $b0, $b0
//> .db $f0
private val PlayerStarting_Y_Pos: UByteArray = ubyteArrayOf(
    0x00u, 0x20u, 0xb0u, 0x50u, 0x00u, 0x00u, 0xb0u, 0xb0u,
    0xf0u,
)

//> PlayerBGPriorityData:
//> .db $00, $20, $00, $00, $00, $00, $00, $00
private val PlayerBGPriorityData = arrayOf(
    SpriteFlags(0x00),
    SpriteFlags(0x20),
    SpriteFlags(0x00),
    SpriteFlags(0x00),
    SpriteFlags(0x00),
    SpriteFlags(0x00),
    SpriteFlags(0x00),
    SpriteFlags(0x00),
    SpriteFlags(0x20),
)

//> GameTimerData:
//> .db $20 ;dummy byte, used as part of bg priority data
//> .db $04, $03, $02
private val GameTimerData: ByteArray = byteArrayOf(
    0x20, 0x04, 0x03, 0x02
)

// ---- Routines ----

/**
 * Kotlin translation of GetAreaMusic.
 * Selects the appropriate background music for the current area and stores it in AreaMusicQueue.
 */
fun System.getAreaMusic() {
    //> GetAreaMusic:
    //> lda OperMode           ;if in title screen mode, leave
    //> beq ExitGetM
    if (ram.operMode == OperMode.TitleScreen) return

    //> lda AltEntranceControl ;check for specific alternate mode of entry
    //> cmp #$02               ;if found, branch without checking starting position
    //> beq ChkAreaType        ;from area object data header
    var indexY: Byte = 0
    if (ram.altEntranceControl != 0x02.toByte()) {
        //> ldy #$05               ;select music for pipe intro scene by default
        indexY = 0x05
        //> lda PlayerEntranceCtrl ;check value from level header for certain values
        //> cmp #$06
        //> beq StoreMusic         ;load music for pipe intro scene if header
        //> cmp #$07               ;start position either value $06 or $07
        //> beq StoreMusic
        val pec = ram.playerEntranceCtrl
        if (pec == 0x06.toByte() || pec == 0x07.toByte()) {
            // fall through to StoreMusic with indexY=5 (PipeIntro)
        } else {
            //> ChkAreaType: ldy AreaType           ;load area type as offset for music bit
            indexY = ram.areaType
            //> lda CloudTypeOverride
            //> beq StoreMusic         ;check for cloud type override
            //> ldy #$04               ;select music for cloud type level if found
            if ((ram.cloudTypeOverride) != 0.toByte()) indexY = 0x04
        }
    } else {
        // Using area type path directly
        //> ChkAreaType: ldy AreaType           ;load area type as offset for music bit
        indexY = ram.areaType
        //> lda CloudTypeOverride
        //> beq StoreMusic         ;check for cloud type override
        //> ldy #$04               ;select music for cloud type level if found
        if ((ram.cloudTypeOverride) != 0.toByte()) indexY = 0x04
    }

    //> StoreMusic:  lda MusicSelectData,y  ;otherwise select appropriate music for level type
    //> sta AreaMusicQueue     ;store in queue and leave
    val music: Byte = MusicSelectData[indexY]
    ram.areaMusicQueue = music
    //> ExitGetM:    rts
}

/**
 * Kotlin translation of Entrance_GameTimerSetup.
 * Initializes player state and timer based on current area and entrance mode.
 */
fun System.entranceGameTimerSetup() {
    //> Entrance_GameTimerSetup:
    //> lda ScreenLeft_PageLoc      ;set current page for area objects
    //> sta Player_PageLoc          ;as page location for player
    ram.playerPageLoc = ram.screenLeftPageLoc

    //> lda #$28                    ;store value here
    //> sta VerticalForceDown       ;for fractional movement downwards if necessary
    ram.verticalForceDown = 0x28

    //> lda #$01                    ;set high byte of player position and
    //> sta PlayerFacingDir         ;set facing direction so that player faces right
    //> sta Player_Y_HighPos
    ram.playerFacingDir = 0x01
    ram.playerYHighPos = 0x01

    //> lda #$00                    ;set player state to on the ground by default
    //> sta Player_State
    ram.playerState = 0x00

    //> dec Player_CollisionBits    ;initialize player's collision bits
    ram.playerCollisionBits--

    //> ldy #$00                    ;initialize halfway page
    //> sty HalfwayPage
    ram.halfwayPage = 0x00

    //> lda AreaType                ;check area type
    //> bne ChkStPos                ;if water type, set swimming flag, otherwise do not set
    //> iny
    //> ChkStPos: sty SwimmingFlag
    ram.swimmingFlag = ram.areaType == 0x0.toByte()

    //> ldx PlayerEntranceCtrl      ;get starting position loaded from header
    var playerEntranceCtrl = ram.playerEntranceCtrl

    //> ldy AltEntranceControl      ;check alternate mode of entry flag for 0 or 1
    //> beq SetStPos
    //> cpy #$01
    //> beq SetStPos
    if (ram.altEntranceControl != 0x00.toByte() && ram.altEntranceControl != 0x01.toByte()) {
        //> ldx AltYPosOffset-2,y       ;if not 0 or 1, override $0710 with new offset in X
        playerEntranceCtrl = AltYPosOffset[ram.altEntranceControl - 2]
    }

    //> SetStPos: lda PlayerStarting_X_Pos,y  ;load appropriate horizontal position
    //> sta Player_X_Position       ;and vertical positions for the player, using
    ram.playerXPosition = PlayerStarting_X_Pos[ram.altEntranceControl]

    //> lda PlayerStarting_Y_Pos,x  ;AltEntranceControl as offset for horizontal and either $0710
    //> sta Player_Y_Position       ;or value that overwrote $0710 as offset for vertical
    ram.playerYPosition = PlayerStarting_Y_Pos[playerEntranceCtrl]

    //> lda PlayerBGPriorityData,x
    //> sta Player_SprAttrib        ;set player sprite attributes using offset in X
    ram.playerSprAttrib = PlayerBGPriorityData[playerEntranceCtrl]

    //> jsr GetPlayerColors         ;get appropriate player palette
    getPlayerColors()

    //> ldy GameTimerSetting        ;get timer control value from header
    //> beq ChkOverR                ;if set to zero, branch (do not use dummy byte for this)
    val gts = ram.gameTimerSetting
    //> lda FetchNewGameTimerFlag   ;do we need to set the game timer? if not, use
    //> beq ChkOverR                ;old game timer setting
    if (gts != 0.toByte() && ram.fetchNewGameTimerFlag) {
        //> lda GameTimerData,y         ;if game timer is set and game timer flag is also set,
        //> sta GameTimerDisplay        ;use value of game timer control for first digit of game timer
        ram.gameTimerDisplay[0] = GameTimerData[gts]
        //> lda #$01
        //> sta GameTimerDisplay+2      ;set last digit of game timer to 1
        ram.gameTimerDisplay[2] = 0x01
        //> lsr
        //> sta GameTimerDisplay+1      ;set second digit of game timer
        ram.gameTimerDisplay[1] = 0x00
        //> sta FetchNewGameTimerFlag   ;clear flag for game timer reset
        //> sta StarInvincibleTimer     ;clear star mario timer
        ram.fetchNewGameTimerFlag = false
        ram.starInvincibleTimer = 0x00
    }

    //> ChkOverR: ldy JoypadOverride          ;if controller bits not set, branch to skip this part
    //> beq ChkSwimE
    if (ram.joypadOverride != 0x0.toByte()) {
        //> lda #$03                    ;set player state to climbing
        //> sta Player_State
        ram.playerState = 0x03
        //> ldx #$00                    ;set offset for first slot, for block object
        //> jsr InitBlock_XY_Pos
        initBlockXYPos(0x00)
        //> lda #$f0                    ;set vertical coordinate for block object
        //> sta Block_Y_Position
        ram.blockYPosition = 0xf0.toByte()
        //> ldx #$05                    ;set offset in X for last enemy object buffer slot
        //> ldy #$00                    ;set offset in Y for object coordinates used earlier
        //> jsr Setup_Vine              ;do a sub to grow vine
        setupVine(0x05, 0x00)
    }

    //> ChkSwimE: ldy AreaType                ;if level not water-type,
    //> bne SetPESub                ;skip this subroutine
    //> jsr SetupBubble             ;otherwise, execute sub to set up air bubbles
    if (ram.areaType == 0x00.toByte()) setupBubble()

    //> SetPESub: lda #$07                    ;set to run player entrance subroutine
    //> sta GameEngineSubroutine    ;on the next frame of game engine
    ram.gameEngineSubroutine = 0x07
    //> rts
}

// ---- Minimal helpers used by Entrance_GameTimerSetup ----

private fun System.initBlockXYPos(offsetX: Byte) {
    // Placeholder: the original computes and stores block object X/Y based on player position.
}

private fun System.setupVine(offsetX: Byte, offsetY: Byte) {
    // Placeholder: the original grows the vine object in the last enemy slot.
}

private fun System.setupBubble() {
    // Placeholder: the original initializes bubble timers when entering water areas.
}

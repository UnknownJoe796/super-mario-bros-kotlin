package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.*
object Constants {


    // sound effects constants
    const val Sfx_SmallJump: Byte         = 0b10000000.toByte()
    const val Sfx_Flagpole: Byte          = 0b01000000
    const val Sfx_Fireball: Byte          = 0b00100000
    const val Sfx_PipeDown_Injury: Byte   = 0b00010000
    const val Sfx_EnemySmack: Byte        = 0b00001000
    const val Sfx_EnemyStomp: Byte        = 0b00000100
    const val Sfx_Bump: Byte              = 0b00000010
    const val Sfx_BigJump: Byte           = 0b00000001

    const val Sfx_BowserFall: Byte        = 0b10000000.toByte()
    const val Sfx_ExtraLife: Byte         = 0b01000000
    const val Sfx_PowerUpGrab: Byte       = 0b00100000
    const val Sfx_TimerTick: Byte         = 0b00010000
    const val Sfx_Blast: Byte             = 0b00001000
    const val Sfx_GrowVine: Byte          = 0b00000100
    const val Sfx_GrowPowerUp: Byte       = 0b00000010
    const val Sfx_CoinGrab: Byte          = 0b00000001

    const val Sfx_BowserFlame: Byte       = 0b00000010
    const val Sfx_BrickShatter: Byte      = 0b00000001
    // SMB2J-only noise SFX
    const val Sfx_Wind: Byte             = 0b00000100
    const val Sfx_Skid: Byte             = 0b10000000.toByte()

    // music constants
    const val Silence: Byte               = 0b10000000.toByte()

    const val StarPowerMusic: Byte        = 0b01000000
    const val PipeIntroMusic: Byte        = 0b00100000
    const val CloudMusic: Byte            = 0b00010000
    const val CastleMusic: Byte           = 0b00001000
    const val UndergroundMusic: Byte      = 0b00000100
    const val WaterMusic: Byte            = 0b00000010
    const val GroundMusic: Byte           = 0b00000001

    const val TimeRunningOutMusic: Byte   = 0b01000000
    const val EndOfLevelMusic: Byte       = 0b00100000
    const val AltGameOverMusic: Byte      = 0b00010000
    const val EndOfCastleMusic: Byte      = 0b00001000
    const val VictoryMusic: Byte          = 0b00000100
    const val GameOverMusic: Byte         = 0b00000010
    const val DeathMusic: Byte            = 0b00000001

    // enemy object constants — see EnemyId enum

    // other constants
    const val World1: Byte = 0
    const val World2: Byte = 1
    const val World3: Byte = 2
    const val World4: Byte = 3
    const val World5: Byte = 4
    const val World6: Byte = 5
    const val World7: Byte = 6
    const val World8: Byte = 7
    const val World9: Byte = 8  // SMB2J only
    const val Level1: Byte = 0
    const val Level2: Byte = 1
    const val Level3: Byte = 2
    const val Level4: Byte = 3

    const val WarmBootOffset: Byte        = 0xd6.toByte()
    const val ColdBootOffset: Byte        = 0xfe.toByte()
    const val TitleScreenDataOffset: Short = 0x1ec0
    const val SoundMemory: Short           = 0x07b0
//    const val SwimTileRepOffset: Byte     = PlayerGraphicsTable + $9e
//    const val MusicHeaderOffsetData: Byte = MusicHeaderData - 1
//    const val MHD: Byte                   = MusicHeaderData

    const val A_Button: Byte              = 0b10000000.toByte()
    const val B_Button: Byte              = 0b01000000
    const val Select_Button: Byte         = 0b00100000
    const val Start_Button: Byte          = 0b00010000
    const val Up_Dir: Byte                = 0b00001000
    const val Down_Dir: Byte              = 0b00000100
    const val Left_Dir: Byte              = 0b00000010
    const val Right_Dir: Byte             = 0b00000001

    const val TitleScreenModeValue: Byte  = 0
    const val GameModeValue: Byte         = 1
    const val VictoryModeValue: Byte      = 2
    const val GameOverModeValue: Byte     = 3
}

enum class GameVariant { SMB1, SMB2J }
enum class Character { Mario, Luigi }

enum class OperMode {
    TitleScreen, Game, Victory, GameOver
}

enum class AreaType(val byte: Byte) {
    Water(0), Ground(1), Underground(2), Castle(3);
    companion object {
        fun fromByte(b: Byte) = entries.getOrElse(b.toInt() and 0xFF) { Water }
    }
}

enum class PlayerState(val byte: Byte) {
    OnGround(0), Falling(1), FallingAlt(2), Climbing(3);
    companion object {
        fun fromByte(b: Byte) = entries.getOrElse(b.toInt() and 0xFF) { OnGround }
    }
}

enum class PlayerSize(val byte: Byte) {
    Big(0), Small(1);
    companion object {
        fun fromByte(b: Byte) = if (b.toInt() and 0xFF == 0) Big else Small
    }
}

enum class PlayerStatus(val byte: Byte) {
    Small(0), Big(1), Fiery(2);
    companion object {
        fun fromByte(b: Byte) = entries.getOrElse(b.toInt() and 0xFF) { Small }
    }
}

enum class Direction(val byte: Byte) {
    None(0), Left(1), Right(2), Both(3);
    companion object {
        fun fromByte(b: Byte) = entries.getOrElse(b.toInt() and 0xFF) { None }
    }
}

enum class EnemyId(val byte: Byte) {
    GreenKoopa(0x00),
    RedKoopaShell(0x01),
    BuzzyBeetle(0x02),
    RedKoopa(0x03),
    GreenKoopaVar(0x04),
    HammerBro(0x05),
    Goomba(0x06),
    Bloober(0x07),
    BulletBillFrenzyVar(0x08),
    TallEnemy(0x09),
    GreyCheepCheep(0x0a),
    RedCheepCheep(0x0b),
    Podoboo(0x0c),
    PiranhaPlant(0x0d),
    GreenParatroopaJump(0x0e),
    RedParatroopa(0x0f),
    GreenParatroopaFly(0x10),
    Lakitu(0x11),
    Spiny(0x12),
    DummyEnemy(0x13),
    FlyingCheepCheep(0x14),
    BowserFlame(0x15),
    Fireworks(0x16),
    BBillCCheepFrenzy(0x17),
    StopFrenzy(0x18),
    Bowser(0x2d),
    PowerUpObject(0x2e),
    VineObject(0x2f),
    FlagpoleFlagObject(0x30),
    StarFlagObject(0x31),
    JumpspringObject(0x32),
    BulletBillCannonVar(0x33),
    RetainerObject(0x35);

    val id: Int get() = byte.toInt() and 0xFF

    companion object {
        /** Alias: FlyCheepCheepFrenzy and FlyingCheepCheep share the same byte value */
        val FlyCheepCheepFrenzy = FlyingCheepCheep
    }
}

/** Dispatch index into the GameRoutines jump table (RAM $0e). */
enum class GameEngineRoutine {
    EntranceGameTimerSetup,
    VineAutoClimb,
    SideExitPipeEntry,
    VerticalPipeEntry,
    FlagpoleSlide,
    PlayerEndLevel,
    PlayerLoseLife,
    PlayerEntrance,
    PlayerCtrlRoutine,
    PlayerChangeSize,
    PlayerInjuryBlink,
    PlayerDeath,
    PlayerFireFlower;

    fun next(): GameEngineRoutine = entries[ordinal + 1]

    companion object {
        fun fromByte(b: Byte): GameEngineRoutine =
            entries.getOrElse(b.toInt() and 0xFF) { entries.first() }
    }
}

/** Named constants for the altEntranceControl field values. */
object AltEntrance {
    /** Normal area entrance (no alternate). */
    const val NONE: Byte = 0
    /** Halfway point entrance (after reaching midpoint checkpoint). */
    const val HALFWAY: Byte = 1
    /** Pipe or door entrance (standard alternate entry). */
    const val PIPE_DOOR: Byte = 2
    /** Cloud/vine exit entrance (set via increment after PIPE_DOOR). */
    const val CLOUD_EXIT: Byte = 3
}

enum class ScreenRoutineTask {
    InitScreen,             // SMB1: 0x00, SMB2J: 0x00
    SetupIntermediate,      // SMB1: 0x01, SMB2J: 0x01
    WriteTopStatusLine,     // SMB1: 0x02, SMB2J: 0x02
    WriteBottomStatusLine,  // SMB1: 0x03, SMB2J: 0x03
    DisplayTimeUp,          // SMB1: 0x04, SMB2J: 0x04
    ResetSpritesAndScreenTimer1, // SMB1: 0x05, SMB2J: 0x05
    DisplayIntermediate,    // SMB1: 0x06, SMB2J: 0x06
    DemoReset,              // SMB2J only: 0x07 (reinitializes area for demo)
    ResetSpritesAndScreenTimer2, // SMB1: 0x07, SMB2J: 0x08
    AreaParserTaskControl,  // SMB1: 0x08, SMB2J: 0x09
    GetAreaPalette,         // SMB1: 0x09, SMB2J: 0x0A
    GetBackgroundColor,     // SMB1: 0x0A, SMB2J: 0x0B
    GetAlternatePalette1,   // SMB1: 0x0B, SMB2J: 0x0C
    DrawTitleScreen,        // SMB1: 0x0C, SMB2J: 0x0D
    ClearBuffersDrawIcon,   // SMB1: 0x0D, SMB2J: 0x0E
    WriteTopScore,          // SMB1: 0x0E, SMB2J: 0x0F
    ;

    /** Advance to the next task in the dispatch sequence for the given variant. */
    fun next(variant: GameVariant = GameVariant.SMB1): ScreenRoutineTask {
        if (variant == GameVariant.SMB1 && this == DisplayIntermediate) {
            // SMB1 skips DemoReset (doesn't exist)
            return ResetSpritesAndScreenTimer2
        }
        return entries[ordinal + 1]
    }

    companion object {
        // SMB2J ordinals match the enum directly (DemoReset at 7).
        // SMB1 has no DemoReset, so NES byte values >= 7 are offset by 1.
        fun fromByte(b: Byte, variant: GameVariant = GameVariant.SMB1): ScreenRoutineTask {
            val i = b.toInt() and 0xFF
            return if (variant == GameVariant.SMB2J) {
                entries.getOrElse(i) { entries.last() }
            } else {
                // SMB1: no DemoReset at position 7, so shift values >= 7 up by 1
                val adjusted = if (i >= 7) i + 1 else i
                entries.getOrElse(adjusted) { entries.last() }
            }
        }

        fun toByte(task: ScreenRoutineTask, variant: GameVariant = GameVariant.SMB1): Byte {
            val ord = task.ordinal
            return if (variant == GameVariant.SMB2J) {
                ord.toByte()
            } else {
                // SMB1: DemoReset doesn't exist, shift ordinals >= 7 down by 1
                (if (ord >= 7) ord - 1 else ord).toByte()
            }
        }
    }
}
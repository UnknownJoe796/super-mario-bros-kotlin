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

enum class OperMode {
    TitleScreen, Game, Victory, GameOver
}

<<<<<<< HEAD
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
    None(0), Left(1), Right(2);
    companion object {
        fun fromByte(b: Byte) = entries.getOrElse(b.toInt() and 0xFF) { None }
    }
}

enum class EnemyId(val byte: Byte) {
    GreenKoopa(0x00),
    BuzzyBeetle(0x02),
    RedKoopa(0x03),
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

    companion object {
        /** Alias: FlyCheepCheepFrenzy and FlyingCheepCheep share the same byte value */
        val FlyCheepCheepFrenzy = FlyingCheepCheep
    }
}
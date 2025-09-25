package com.ivieleague.smbtranslation

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

    // enemy object constants
    const val GreenKoopa: Byte            = 0x00
    const val BuzzyBeetle: Byte           = 0x02
    const val RedKoopa: Byte              = 0x03
    const val HammerBro: Byte             = 0x05
    const val Goomba: Byte                = 0x06
    const val Bloober: Byte               = 0x07
    const val BulletBill_FrenzyVar: Byte  = 0x08
    const val GreyCheepCheep: Byte        = 0x0a
    const val RedCheepCheep: Byte         = 0x0b
    const val Podoboo: Byte               = 0x0c
    const val PiranhaPlant: Byte          = 0x0d
    const val GreenParatroopaJump: Byte   = 0x0e
    const val RedParatroopa: Byte         = 0x0f
    const val GreenParatroopaFly: Byte    = 0x10
    const val Lakitu: Byte                = 0x11
    const val Spiny: Byte                 = 0x12
    const val FlyCheepCheepFrenzy: Byte   = 0x14
    const val FlyingCheepCheep: Byte      = 0x14
    const val BowserFlame: Byte           = 0x15
    const val Fireworks: Byte             = 0x16
    const val BBill_CCheep_Frenzy: Byte   = 0x17
    const val Stop_Frenzy: Byte           = 0x18
    const val Bowser: Byte                = 0x2d
    const val PowerUpObject: Byte         = 0x2e
    const val VineObject: Byte            = 0x2f
    const val FlagpoleFlagObject: Byte    = 0x30
    const val StarFlagObject: Byte        = 0x31
    const val JumpspringObject: Byte      = 0x32
    const val BulletBill_CannonVar: Byte  = 0x33
    const val RetainerObject: Byte        = 0x35
    const val TallEnemy: Byte             = 0x09

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
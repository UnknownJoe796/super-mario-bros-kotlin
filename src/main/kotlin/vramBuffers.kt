package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.*
import com.ivieleague.smbtranslation.chr.OriginalRom
import com.ivieleague.smbtranslation.nes.Color

val WaterPaletteData = listOf(
    BufferedPpuUpdate.BackgroundSetPalette(
        index = 0,
        colors = listOf(
            Color(0xff000000.toInt()),
            Color(0xffb01262.toInt()),
            Color(0xff3d2eff.toInt()),
            Color(0xfff863c6.toInt())
        )
    ),
    BufferedPpuUpdate.BackgroundSetPalette(
        index = 1,
        colors = listOf(
            Color(0xff000000.toInt()),
            Color(0xffc2efa8.toInt()),
            Color(0xff008900.toInt()),
            Color(0xff000000.toInt())
        )
    ),
    BufferedPpuUpdate.BackgroundSetPalette(
        index = 2,
        colors = listOf(
            Color(0xff000000.toInt()),
            Color(0xffffffff.toInt()),
            Color(0xff3d2eff.toInt()),
            Color(0xff000000.toInt())
        )
    ),
    BufferedPpuUpdate.BackgroundSetPalette(
        index = 3,
        colors = listOf(
            Color(0xff000000.toInt()),
            Color(0xffde9020.toInt()),
            Color(0xff3d2eff.toInt()),
            Color(0xff000000.toInt())
        )
    ),
    BufferedPpuUpdate.SpriteSetPalette(
        index = 0,
        colors = listOf(
            Color(0xff8287ff.toInt()),
            Color(0xffa92704.toInt()),
            Color(0xffde9020.toInt()),
            Color(0xff576600.toInt())
        )
    ),
    BufferedPpuUpdate.SpriteSetPalette(
        index = 1,
        colors = listOf(
            Color(0xff000000.toInt()),
            Color(0xffababab.toInt()),
            Color(0xffffffff.toInt()),
            Color(0xffde9020.toInt())
        )
    ),
    BufferedPpuUpdate.SpriteSetPalette(
        index = 2,
        colors = listOf(
            Color(0xff000000.toInt()),
            Color(0xffa92704.toInt()),
            Color(0xffffffff.toInt()),
            Color(0xffde9020.toInt())
        )
    ),
    BufferedPpuUpdate.SpriteSetPalette(
        index = 3,
        colors = listOf(
            Color(0xff000000.toInt()),
            Color(0xff000000.toInt()),
            Color(0xffffffff.toInt()),
            Color(0xffababab.toInt())
        )
    ),
)
val GroundPaletteData = listOf(
    BufferedPpuUpdate.BackgroundSetPalette(
        index = 0,
        colors = listOf(
            Color(0xff000000.toInt()),
            Color(0xff81c800.toInt()),
            Color(0xff008900.toInt()),
            Color(0xff000000.toInt())
        )
    ),
    BufferedPpuUpdate.BackgroundSetPalette(
        index = 1,
        colors = listOf(
            Color(0xff000000.toInt()),
            Color(0xfffdcace.toInt()),
            Color(0xff894600.toInt()),
            Color(0xff000000.toInt())
        )
    ),
    BufferedPpuUpdate.BackgroundSetPalette(
        index = 2,
        colors = listOf(
            Color(0xff000000.toInt()),
            Color(0xffffffff.toInt()),
            Color(0xff57a5ff.toInt()),
            Color(0xff000000.toInt())
        )
    ),
    BufferedPpuUpdate.BackgroundSetPalette(
        index = 3,
        colors = listOf(
            Color(0xff000000.toInt()),
            Color(0xffde9020.toInt()),
            Color(0xff894600.toInt()),
            Color(0xff000000.toInt())
        )
    ),
    BufferedPpuUpdate.SpriteSetPalette(
        index = 0,
        colors = listOf(
            Color(0xff000000.toInt()),
            Color(0xffa92704.toInt()),
            Color(0xffde9020.toInt()),
            Color(0xff576600.toInt())
        )
    ),
    BufferedPpuUpdate.SpriteSetPalette(
        index = 1,
        colors = listOf(
            Color(0xff000000.toInt()),
            Color(0xff008900.toInt()),
            Color(0xffffffff.toInt()),
            Color(0xffde9020.toInt())
        )
    ),
    BufferedPpuUpdate.SpriteSetPalette(
        index = 2,
        colors = listOf(
            Color(0xff000000.toInt()),
            Color(0xffa92704.toInt()),
            Color(0xffffffff.toInt()),
            Color(0xffde9020.toInt())
        )
    ),
    BufferedPpuUpdate.SpriteSetPalette(
        index = 3,
        colors = listOf(
            Color(0xff000000.toInt()),
            Color(0xff000000.toInt()),
            Color(0xfffdcace.toInt()),
            Color(0xff894600.toInt())
        )
    ),
)
val UndergroundPaletteData = listOf(
    BufferedPpuUpdate.BackgroundSetPalette(
        index = 0,
        colors = listOf(
            Color(0xff000000.toInt()),
            Color(0xff81c800.toInt()),
            Color(0xff008900.toInt()),
            Color(0xff004900.toInt())
        )
    ),
    BufferedPpuUpdate.BackgroundSetPalette(
        index = 1,
        colors = listOf(
            Color(0xff000000.toInt()),
            Color(0xffb6eae5.toInt()),
            Color(0xff006d90.toInt()),
            Color(0xff000000.toInt())
        )
    ),
    BufferedPpuUpdate.BackgroundSetPalette(
        index = 2,
        colors = listOf(
            Color(0xff000000.toInt()),
            Color(0xffffffff.toInt()),
            Color(0xff57a5ff.toInt()),
            Color(0xff006d90.toInt())
        )
    ),
    BufferedPpuUpdate.BackgroundSetPalette(
        index = 3,
        colors = listOf(
            Color(0xff000000.toInt()),
            Color(0xffde9020.toInt()),
            Color(0xff894600.toInt()),
            Color(0xff006d90.toInt())
        )
    ),
    BufferedPpuUpdate.SpriteSetPalette(
        index = 0,
        colors = listOf(
            Color(0xff000000.toInt()),
            Color(0xffa92704.toInt()),
            Color(0xffde9020.toInt()),
            Color(0xff576600.toInt())
        )
    ),
    BufferedPpuUpdate.SpriteSetPalette(
        index = 1,
        colors = listOf(
            Color(0xff000000.toInt()),
            Color(0xff006d90.toInt()),
            Color(0xfffdcace.toInt()),
            Color(0xff894600.toInt())
        )
    ),
    BufferedPpuUpdate.SpriteSetPalette(
        index = 2,
        colors = listOf(
            Color(0xff000000.toInt()),
            Color(0xffa92704.toInt()),
            Color(0xffffffff.toInt()),
            Color(0xffde9020.toInt())
        )
    ),
    BufferedPpuUpdate.SpriteSetPalette(
        index = 3,
        colors = listOf(
            Color(0xff000000.toInt()),
            Color(0xff00355e.toInt()),
            Color(0xffb6eae5.toInt()),
            Color(0xff006d90.toInt())
        )
    ),
)
val CastlePaletteData = listOf(
    BufferedPpuUpdate.BackgroundSetPalette(
        index = 0,
        colors = listOf(
            Color(0xff000000.toInt()),
            Color(0xffffffff.toInt()),
            Color(0xffababab.toInt()),
            Color(0xff626262.toInt())
        )
    ),
    BufferedPpuUpdate.BackgroundSetPalette(
        index = 1,
        colors = listOf(
            Color(0xff000000.toInt()),
            Color(0xffffffff.toInt()),
            Color(0xffababab.toInt()),
            Color(0xff626262.toInt())
        )
    ),
    BufferedPpuUpdate.BackgroundSetPalette(
        index = 2,
        colors = listOf(
            Color(0xff000000.toInt()),
            Color(0xffffffff.toInt()),
            Color(0xffa92704.toInt()),
            Color(0xff626262.toInt())
        )
    ),
    BufferedPpuUpdate.BackgroundSetPalette(
        index = 3,
        colors = listOf(
            Color(0xff000000.toInt()),
            Color(0xffde9020.toInt()),
            Color(0xff894600.toInt()),
            Color(0xff626262.toInt())
        )
    ),
    BufferedPpuUpdate.SpriteSetPalette(
        index = 0,
        colors = listOf(
            Color(0xff000000.toInt()),
            Color(0xffa92704.toInt()),
            Color(0xffde9020.toInt()),
            Color(0xff576600.toInt())
        )
    ),
    BufferedPpuUpdate.SpriteSetPalette(
        index = 1,
        colors = listOf(
            Color(0xff000000.toInt()),
            Color(0xff006d90.toInt()),
            Color(0xfffdcace.toInt()),
            Color(0xff894600.toInt())
        )
    ),
    BufferedPpuUpdate.SpriteSetPalette(
        index = 2,
        colors = listOf(
            Color(0xff000000.toInt()),
            Color(0xffa92704.toInt()),
            Color(0xffffffff.toInt()),
            Color(0xffde9020.toInt())
        )
    ),
    BufferedPpuUpdate.SpriteSetPalette(
        index = 3,
        colors = listOf(
            Color(0xff000000.toInt()),
            Color(0xff626262.toInt()),
            Color(0xffffffff.toInt()),
            Color(0xffababab.toInt())
        )
    ),
)
val DaySnowPaletteData = listOf(
    BufferedPpuUpdate.BackgroundSetPalette(
        index = 0,
        colors = listOf(
            Color(0xff8287ff.toInt()),
            Color(0xffffffff.toInt()),
            Color(0xff626262.toInt()),
            Color(0xffababab.toInt())
        )
    ),
)
val NightSnowPaletteData = listOf(
    BufferedPpuUpdate.BackgroundSetPalette(
        index = 0,
        colors = listOf(
            Color(0xff000000.toInt()),
            Color(0xffffffff.toInt()),
            Color(0xff626262.toInt()),
            Color(0xffababab.toInt())
        )
    ),
)
val MushroomPaletteData = listOf(
    BufferedPpuUpdate.BackgroundSetPalette(
        index = 0,
        colors = listOf(
            Color(0xff8287ff.toInt()),
            Color(0xffde9020.toInt()),
            Color(0xffa92704.toInt()),
            Color(0xff000000.toInt())
        )
    ),
)
val BowserPaletteData = listOf(
    BufferedPpuUpdate.SpriteSetPalette(
        index = 1,
        colors = listOf(
            Color(0xff000000.toInt()),
            Color(0xff008900.toInt()),
            Color(0xffffffff.toInt()),
            Color(0xffde9020.toInt())
        )
    ),
)
val MarioThanksMessage = listOf(
    BufferedPpuUpdate.BackgroundPatternString(
        nametable = 1,
        x = 8,
        y = 10,
        drawVertically = false,
        patterns = listOf(
            OriginalRom.backgrounds[0x1d],
            OriginalRom.backgrounds[0x11],
            OriginalRom.backgrounds[0xa],
            OriginalRom.backgrounds[0x17],
            OriginalRom.backgrounds[0x14],
            OriginalRom.backgrounds[0x24],
            OriginalRom.backgrounds[0x22],
            OriginalRom.backgrounds[0x18],
            OriginalRom.backgrounds[0x1e],
            OriginalRom.backgrounds[0x24],
            OriginalRom.backgrounds[0x16],
            OriginalRom.backgrounds[0xa],
            OriginalRom.backgrounds[0x1b],
            OriginalRom.backgrounds[0x12],
            OriginalRom.backgrounds[0x18],
            OriginalRom.backgrounds[0x2b]
        )
    ),
)
val LuigiThanksMessage = listOf(
    BufferedPpuUpdate.BackgroundPatternString(
        nametable = 1,
        x = 8,
        y = 10,
        drawVertically = false,
        patterns = listOf(
            OriginalRom.backgrounds[0x1d],
            OriginalRom.backgrounds[0x11],
            OriginalRom.backgrounds[0xa],
            OriginalRom.backgrounds[0x17],
            OriginalRom.backgrounds[0x14],
            OriginalRom.backgrounds[0x24],
            OriginalRom.backgrounds[0x22],
            OriginalRom.backgrounds[0x18],
            OriginalRom.backgrounds[0x1e],
            OriginalRom.backgrounds[0x24],
            OriginalRom.backgrounds[0x15],
            OriginalRom.backgrounds[0x1e],
            OriginalRom.backgrounds[0x12],
            OriginalRom.backgrounds[0x10],
            OriginalRom.backgrounds[0x12],
            OriginalRom.backgrounds[0x2b]
        )
    ),
)
val MushroomRetainerSaved = listOf(
    BufferedPpuUpdate.BackgroundPatternString(
        nametable = 1,
        x = 5,
        y = 14,
        drawVertically = false,
        patterns = listOf(
            OriginalRom.backgrounds[0xb],
            OriginalRom.backgrounds[0x1e],
            OriginalRom.backgrounds[0x1d],
            OriginalRom.backgrounds[0x24],
            OriginalRom.backgrounds[0x18],
            OriginalRom.backgrounds[0x1e],
            OriginalRom.backgrounds[0x1b],
            OriginalRom.backgrounds[0x24],
            OriginalRom.backgrounds[0x19],
            OriginalRom.backgrounds[0x1b],
            OriginalRom.backgrounds[0x12],
            OriginalRom.backgrounds[0x17],
            OriginalRom.backgrounds[0xc],
            OriginalRom.backgrounds[0xe],
            OriginalRom.backgrounds[0x1c],
            OriginalRom.backgrounds[0x1c],
            OriginalRom.backgrounds[0x24],
            OriginalRom.backgrounds[0x12],
            OriginalRom.backgrounds[0x1c],
            OriginalRom.backgrounds[0x24],
            OriginalRom.backgrounds[0x12],
            OriginalRom.backgrounds[0x17]
        )
    ),
    BufferedPpuUpdate.BackgroundPatternString(
        nametable = 1,
        x = 5,
        y = 16,
        drawVertically = false,
        patterns = listOf(
            OriginalRom.backgrounds[0xa],
            OriginalRom.backgrounds[0x17],
            OriginalRom.backgrounds[0x18],
            OriginalRom.backgrounds[0x1d],
            OriginalRom.backgrounds[0x11],
            OriginalRom.backgrounds[0xe],
            OriginalRom.backgrounds[0x1b],
            OriginalRom.backgrounds[0x24],
            OriginalRom.backgrounds[0xc],
            OriginalRom.backgrounds[0xa],
            OriginalRom.backgrounds[0x1c],
            OriginalRom.backgrounds[0x1d],
            OriginalRom.backgrounds[0x15],
            OriginalRom.backgrounds[0xe],
            OriginalRom.backgrounds[0x2b]
        )
    ),
)
val PrincessSaved1 = listOf(
    BufferedPpuUpdate.BackgroundPatternString(
        nametable = 1,
        x = 7,
        y = 13,
        drawVertically = false,
        patterns = listOf(
            OriginalRom.backgrounds[0x22],
            OriginalRom.backgrounds[0x18],
            OriginalRom.backgrounds[0x1e],
            OriginalRom.backgrounds[0x1b],
            OriginalRom.backgrounds[0x24],
            OriginalRom.backgrounds[0x1a],
            OriginalRom.backgrounds[0x1e],
            OriginalRom.backgrounds[0xe],
            OriginalRom.backgrounds[0x1c],
            OriginalRom.backgrounds[0x1d],
            OriginalRom.backgrounds[0x24],
            OriginalRom.backgrounds[0x12],
            OriginalRom.backgrounds[0x1c],
            OriginalRom.backgrounds[0x24],
            OriginalRom.backgrounds[0x18],
            OriginalRom.backgrounds[0x1f],
            OriginalRom.backgrounds[0xe],
            OriginalRom.backgrounds[0x1b],
            OriginalRom.backgrounds[0xaf]
        )
    ),
)
val PrincessSaved2 = listOf(
    BufferedPpuUpdate.BackgroundPatternString(
        nametable = 1,
        x = 3,
        y = 15,
        drawVertically = false,
        patterns = listOf(
            OriginalRom.backgrounds[0x20],
            OriginalRom.backgrounds[0xe],
            OriginalRom.backgrounds[0x24],
            OriginalRom.backgrounds[0x19],
            OriginalRom.backgrounds[0x1b],
            OriginalRom.backgrounds[0xe],
            OriginalRom.backgrounds[0x1c],
            OriginalRom.backgrounds[0xe],
            OriginalRom.backgrounds[0x17],
            OriginalRom.backgrounds[0x1d],
            OriginalRom.backgrounds[0x24],
            OriginalRom.backgrounds[0x22],
            OriginalRom.backgrounds[0x18],
            OriginalRom.backgrounds[0x1e],
            OriginalRom.backgrounds[0x24],
            OriginalRom.backgrounds[0xa],
            OriginalRom.backgrounds[0x24],
            OriginalRom.backgrounds[0x17],
            OriginalRom.backgrounds[0xe],
            OriginalRom.backgrounds[0x20],
            OriginalRom.backgrounds[0x24],
            OriginalRom.backgrounds[0x1a],
            OriginalRom.backgrounds[0x1e],
            OriginalRom.backgrounds[0xe],
            OriginalRom.backgrounds[0x1c],
            OriginalRom.backgrounds[0x1d],
            OriginalRom.backgrounds[0xaf]
        )
    ),
)
val WorldSelectMessage1 = listOf(
    BufferedPpuUpdate.BackgroundPatternString(
        nametable = 1,
        x = 10,
        y = 18,
        drawVertically = false,
        patterns = listOf(
            OriginalRom.backgrounds[0x19],
            OriginalRom.backgrounds[0x1e],
            OriginalRom.backgrounds[0x1c],
            OriginalRom.backgrounds[0x11],
            OriginalRom.backgrounds[0x24],
            OriginalRom.backgrounds[0xb],
            OriginalRom.backgrounds[0x1e],
            OriginalRom.backgrounds[0x1d],
            OriginalRom.backgrounds[0x1d],
            OriginalRom.backgrounds[0x18],
            OriginalRom.backgrounds[0x17],
            OriginalRom.backgrounds[0x24],
            OriginalRom.backgrounds[0xb]
        )
    ),
)
val WorldSelectMessage2 = listOf(
    BufferedPpuUpdate.BackgroundPatternString(
        nametable = 1,
        x = 8,
        y = 20,
        drawVertically = false,
        patterns = listOf(
            OriginalRom.backgrounds[0x1d],
            OriginalRom.backgrounds[0x18],
            OriginalRom.backgrounds[0x24],
            OriginalRom.backgrounds[0x1c],
            OriginalRom.backgrounds[0xe],
            OriginalRom.backgrounds[0x15],
            OriginalRom.backgrounds[0xe],
            OriginalRom.backgrounds[0xc],
            OriginalRom.backgrounds[0x1d],
            OriginalRom.backgrounds[0x24],
            OriginalRom.backgrounds[0xa],
            OriginalRom.backgrounds[0x24],
            OriginalRom.backgrounds[0x20],
            OriginalRom.backgrounds[0x18],
            OriginalRom.backgrounds[0x1b],
            OriginalRom.backgrounds[0x15],
            OriginalRom.backgrounds[0xd]
        )
    ),
)

val System.vramAddrTable: List<List<BufferedPpuUpdate>>
    get() = buildList {
        //> VRAM_AddrTable:
        add(ram.vRAMBuffer1)                // 0x00: VRAM_Buffer1
        add(WaterPaletteData)               // 0x01
        add(GroundPaletteData)              // 0x02
        add(UndergroundPaletteData)         // 0x03
        add(CastlePaletteData)             // 0x04
        add(ram.vRAMBuffer1)               // 0x05: TitleScreenGfxData (uses Buffer1)
        add(ram.vRAMBuffer2)               // 0x06: VRAM_Buffer2
        add(ram.vRAMBuffer2)               // 0x07: VRAM_Buffer2 (alt)
        add(BowserPaletteData)             // 0x08
        add(DaySnowPaletteData)            // 0x09
        add(NightSnowPaletteData)          // 0x0A
        add(MushroomPaletteData)           // 0x0B
        add(MarioThanksMessage)            // 0x0C: ThankYouMessage
        add(LuigiThanksMessage)            // 0x0D: MushroomRetainerMsg
        add(MushroomRetainerSaved)         // 0x0E: UnusedAttribData
        add(PrincessSaved1)                // 0x0F: FinalRoomPalette
        add(PrincessSaved2)                // 0x10: ThankYouMessageFinal
        add(WorldSelectMessage1)           // 0x11: PeaceIsPavedMsg
        add(WorldSelectMessage2)           // 0x12: WithKingdomSavedMsg
        // SMB2J additional entries (indices 0x13-0x1E)
        if (variant == GameVariant.SMB2J) {
            add(emptyList())               // 0x13: HurrahMsg (stub)
            add(emptyList())               // 0x14: OurOnlyHeroMsg (stub)
            add(emptyList())               // 0x15: ThisEndsYourTripMsg (stub)
            add(emptyList())               // 0x16: OfALongFriendshipMsg (stub)
            add(emptyList())               // 0x17: PointsAddedMsg (stub)
            add(emptyList())               // 0x18: ForEachPlayerLeftMsg (stub)
            add(emptyList())               // 0x19: DiskErrorMainMsg (stub)
            add(emptyList())               // 0x1A: DiskScreenPalette (stub)
            add(emptyList())               // 0x1B: PrincessPeachsRoom (stub)
            add(ram.vRAMBuffer1)           // 0x1C: MenuCursorTemplate (uses Buffer1)
            add(emptyList())               // 0x1D: FantasyWorld9Msg (stub)
            add(emptyList())               // 0x1E: SuperPlayerMsg (stub)
        }
    }
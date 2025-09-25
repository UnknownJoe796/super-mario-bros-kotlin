package com.ivieleague.smbtranslation

class System {
    val ram = GameRam()
    val ppu = PictureProcessingUnit()
    val apu = AudioProcessingUnit()
    val inputs = Inputs()

    //> VRAM_AddrTable_Low:
    //> VRAM_AddrTable_High:
    val vramAddrTable: List<GameRam.VramBytes?> = listOf(
        //> <VRAM_Buffer1
        //> >VRAM_Buffer1
        ram.vRAMBuffer1,
        //> <WaterPaletteData
        //> >WaterPaletteData
        null,
        //> <GroundPaletteData
        //> >GroundPaletteData
        null,
        //> <UndergroundPaletteData
        //> >UndergroundPaletteData
        null,
        //> <CastlePaletteData
        //> >CastlePaletteData
        null,
        //> <VRAM_Buffer1_Offset
        //> >VRAM_Buffer1_Offset
        null,
        //> <VRAM_Buffer2
        //> >VRAM_Buffer2
        ram.vRAMBuffer2,
        //> <VRAM_Buffer2
        //> >VRAM_Buffer2
        ram.vRAMBuffer2,
        //> <BowserPaletteData
        //> >BowserPaletteData
        null,
        //> <DaySnowPaletteData
        //> >DaySnowPaletteData
        null,
        //> <NightSnowPaletteData
        //> >NightSnowPaletteData
        null,
        //> <MushroomPaletteData
        //> >MushroomPaletteData
        null,
        //> <MarioThanksMessage
        //> >MarioThanksMessage
        null,
        //> <LuigiThanksMessage
        //> >LuigiThanksMessage
        null,
        //> <MushroomRetainerSaved
        //> >MushroomRetainerSaved
        null,
        //> <PrincessSaved1
        //> >PrincessSaved1
        null,
        //> <PrincessSaved2
        //> >PrincessSaved2
        null,
        //> <WorldSelectMessage1
        //> >WorldSelectMessage1
        null,
        //> <WorldSelectMessage2
        //> >WorldSelectMessage2
        null,
    )
}
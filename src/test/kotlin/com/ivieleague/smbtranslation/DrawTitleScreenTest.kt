package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.chr.OriginalRom
import com.ivieleague.smbtranslation.nes.Color
import com.ivieleague.smbtranslation.nes.DirectPalette
import com.ivieleague.smbtranslation.nes.NesNametable
import com.ivieleague.smbtranslation.nes.Palette
import com.ivieleague.smbtranslation.nes.PictureProcessingUnit
import com.ivieleague.smbtranslation.nes.PpuRenderer
import com.ivieleague.smbtranslation.nes.Tile
import com.ivieleague.smbtranslation.utils.SpriteFlags
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Surface
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test

class DrawTitleScreenTest {
    @Test
    fun test() {
        val scale = 1
        val tileSize = 8

        val system = System()

        // Simple 3-color palette for CI 1..3 (CI 0 comes from ppu.backgroundColor fill)
        val simplePalette = Palette.GRAYSCALE

        // Build a nametable: left half shows background patterns (0x0000), right half shows sprite patterns (0x1000)
        val widthTiles = 32
        val heightTiles = 30

        system.ppu.backgroundPalettes[0].palette = simplePalette
        system.ppu.backgroundPalettes[1].palette = simplePalette
        system.ppu.backgroundPalettes[2].palette = simplePalette
        system.ppu.backgroundPalettes[3].palette = simplePalette

        system.updateScreen(GroundPaletteData)
        system.ppu.backgroundPalettes[0].palette.colors[0] = Color(0x22.toByte()).also { println(it) }
        system.drawTitleScreen()
        system.updateScreen(system.vramAddrTable[5])
        system.updateScreen(mushroomIconData)

        // Let's also draw some sprites real quick.
        system.ppu.sprites[0].apply {
            x = 4u
            y = 4u
            attributes = SpriteFlags(palette = 0)
            pattern = OriginalRom.sprites[0x32]
        }
        system.ppu.sprites[1].apply {
            x = 12u
            y = 4u
            attributes = SpriteFlags(palette = 0)
            pattern = OriginalRom.sprites[0x33]
        }
        system.ppu.sprites[2].apply {
            x = 4u
            y = 12u
            attributes = SpriteFlags(palette = 0)
            pattern = OriginalRom.sprites[0x42]
        }
        system.ppu.sprites[3].apply {
            x = 12u
            y = 12u
            attributes = SpriteFlags(palette = 0)
            pattern = OriginalRom.sprites[0x43]
        }

        val pixelWidth = widthTiles * tileSize * scale
        val pixelHeight = heightTiles * tileSize * scale

        val surface = Surface.makeRasterN32Premul(pixelWidth, pixelHeight)
        val canvas = surface.canvas

        // Render the scene
        PpuRenderer.render(canvas, system.ppu, scale)

        // Encode to PNG
        val img = surface.makeImageSnapshot()
        val data = img.encodeToData(EncodedImageFormat.PNG)
            ?: error("Failed to encode PNG via Skia")

        val outPath: Path = Path.of("build", "title.png")
        Files.createDirectories(outPath.parent)
        Files.write(outPath, data.bytes)

        println("Wrote PPU patterns preview to: ${'$'}outPath (${'$'}pixelWidth x ${'$'}pixelHeight)")
    }
}
package com.ivieleague.smbtranslation.nes

import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Surface
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test

class PpuTests() {
    /**
     * Small utility entrypoint to visualize the PPU pattern tables using the simple Skia-based PpuRenderer.
     *
     * It attempts to read CHR data from the bundled smb.nes (via PictureProcessingUnit init).
     * The output image will be written to build/ppu_patterns.png
     */
    @Test
    fun renderAllPatterns() {
        val scale = 1
        val tileSize = 8

        val ppu = PictureProcessingUnit()

        // Simple 3-color palette for CI 1..3 (CI 0 comes from ppu.backgroundColor fill)
        val simplePalette = Palette.GRAYSCALE

        // Build a nametable: left half shows background patterns (0x0000), right half shows sprite patterns (0x1000)
        val widthTiles = 32
        val heightTiles = 30
        val nt = NesNametable(widthTiles, heightTiles)

        // Place background pattern table (256 tiles = 16x16) at (0,0)
        for (i in 0 until 256) {
            val x = i % 16
            val y = i / 16
            if (x < widthTiles && y < heightTiles) {
                nt[x, y] = Tile(ppu.originalRomBackgrounds[i], simplePalette)
            }
        }

        // Place sprite pattern table (256 tiles) starting at x=16
        for (i in 0 until 256) {
            val x = 16 + (i % 16)
            val y = i / 16
            if (x < widthTiles && y < heightTiles) {
                nt[x, y] = Tile(ppu.originalRomSprites[i], simplePalette)
            }
        }

        // Assign the constructed nametable into PPU slot 0
        ppu.backgroundTiles[0] = nt

        val pixelWidth = widthTiles * tileSize * scale
        val pixelHeight = heightTiles * tileSize * scale

        val surface = Surface.makeRasterN32Premul(pixelWidth, pixelHeight)
        val canvas = surface.canvas

        // Render the scene
        PpuRenderer.render(canvas, ppu, scale)

        // Encode to PNG
        val img = surface.makeImageSnapshot()
        val data = img.encodeToData(EncodedImageFormat.PNG)
            ?: error("Failed to encode PNG via Skia")

        val outPath: Path = Path.of("build", "ppu_patterns.png")
        Files.createDirectories(outPath.parent)
        Files.write(outPath, data.bytes)

        println("Wrote PPU patterns preview to: ${'$'}outPath (${'$'}pixelWidth x ${'$'}pixelHeight)")
    }
}
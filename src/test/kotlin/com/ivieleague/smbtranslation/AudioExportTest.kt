// by Claude - Exports game audio to WAV file for verification
package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.nes.ApuAudioOutput
import com.ivieleague.smbtranslation.utils.JoypadBits
import org.junit.jupiter.api.Test
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

// Runs the game for N frames and exports APU audio to a WAV file.
// Listen to build/audio/title-and-start.wav and gameplay-w1-1.wav
class AudioExportTest {

    @Test
    fun `export first 600 frames audio to WAV`() {
        exportAudio("title-and-start", frames = 600, pressStart = 180)
    }

    @Test
    fun `export 1800 frames with gameplay audio`() {
        exportAudio("gameplay-w1-1", frames = 1800, pressStart = 180)
    }

    private fun exportAudio(name: String, frames: Int, pressStart: Int = -1) {
        val system = System()
        // start() uses FrameDelay to spread initialization across frames
        var action: (() -> Unit)? = { system.start() }
        while (action != null) {
            try { action(); action = null }
            catch (d: com.ivieleague.smbtranslation.utils.FrameDelay) { action = d.nextAction }
        }

        val sampleRate = 44100
        val samplesPerFrame = sampleRate / 60
        val totalSamples = samplesPerFrame * frames
        val audioData = ShortArray(totalSamples)
        var sampleOffset = 0

        val audioOutput = ApuAudioOutput(sampleRate)
        system.audioOutput = audioOutput

        for (frame in 0 until frames) {
            if (frame == pressStart) {
                system.inputs.joypadPort1 = JoypadBits(0x10)
            } else if (frame == pressStart + 1) {
                system.inputs.joypadPort1 = JoypadBits(0x00)
            }

            system.nonMaskableInterrupt()

            val frameSamples = audioOutput.generateSamples(system.apu, samplesPerFrame)
            for (i in frameSamples.indices) {
                if (sampleOffset + i < totalSamples) {
                    audioData[sampleOffset + i] = frameSamples[i]
                }
            }
            sampleOffset += samplesPerFrame
        }

        val outDir = File("build/audio")
        outDir.mkdirs()
        val wavFile = File(outDir, "$name.wav")
        writeWav(wavFile, audioData, sampleRate)
        println("Wrote ${wavFile.absolutePath} ($frames frames, ${totalSamples / sampleRate}s)")
    }

    private fun writeWav(file: File, samples: ShortArray, sampleRate: Int) {
        val dataSize = samples.size * 2
        val byteRate = sampleRate * 2
        RandomAccessFile(file, "rw").use { raf ->
            val buf = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
            buf.put("RIFF".toByteArray())
            buf.putInt(36 + dataSize)
            buf.put("WAVE".toByteArray())
            buf.put("fmt ".toByteArray())
            buf.putInt(16)
            buf.putShort(1)
            buf.putShort(1)
            buf.putInt(sampleRate)
            buf.putInt(byteRate)
            buf.putShort(2)
            buf.putShort(16)
            buf.put("data".toByteArray())
            buf.putInt(dataSize)
            for (s in samples) buf.putShort(s)
            raf.write(buf.array())
        }
    }
}

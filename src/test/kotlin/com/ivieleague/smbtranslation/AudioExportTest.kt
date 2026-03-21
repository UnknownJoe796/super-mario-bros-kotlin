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

    @Test
    fun `trace all channel volumes and frequencies`() {
        val system = System()
        var action: (() -> Unit)? = { system.start() }
        while (action != null) {
            try { action(); action = null }
            catch (d: com.ivieleague.smbtranslation.utils.FrameDelay) { action = d.nextAction }
        }
        val audioOutput = ApuAudioOutput(44100)
        system.audioOutput = audioOutput

        for (frame in 0 until 500) {
            if (frame == 180) system.inputs.joypadPort1 = JoypadBits(0x10)
            else if (frame == 181) system.inputs.joypadPort1 = JoypadBits(0x00)
            system.nonMaskableInterrupt()

            val r = system.apu.rawRegs
            // Pulse 1: vol from reg0 bits 3-0, period from reg2+reg3
            val p1v = r[0].toInt() and 0x0F
            val p1cv = r[0].toInt() and 0x10 != 0  // constant volume flag
            val p1p = (r[2].toInt() and 0xFF) or ((r[3].toInt() and 7) shl 8)
            // Pulse 2
            val p2v = r[4].toInt() and 0x0F
            val p2cv = r[4].toInt() and 0x10 != 0
            val p2p = (r[6].toInt() and 0xFF) or ((r[7].toInt() and 7) shl 8)
            // Triangle
            val triLin = r[8].toInt() and 0x7F
            val triP = (r[10].toInt() and 0xFF) or ((r[11].toInt() and 7) shl 8)

            if (frame in 340..380) {
                println("F$frame: p1v=$p1v(cv=$p1cv,per=$p1p) p2v=$p2v(cv=$p2cv,per=$p2p) tri(lin=$triLin,per=$triP)")
            }
        }
    }

    @Test
    fun `trace noise channel state`() {
        val system = System()
        var action: (() -> Unit)? = { system.start() }
        while (action != null) {
            try { action(); action = null }
            catch (d: com.ivieleague.smbtranslation.utils.FrameDelay) { action = d.nextAction }
        }
        val audioOutput = ApuAudioOutput(44100)
        system.audioOutput = audioOutput

        // Press start at frame 180
        for (frame in 0 until 600) {
            if (frame == 180) system.inputs.joypadPort1 = JoypadBits(0x10)
            else if (frame == 181) system.inputs.joypadPort1 = JoypadBits(0x00)
            system.nonMaskableInterrupt()

            val regs = system.apu.rawRegs
            val noiseVol = regs[12].toInt() and 0x0F
            val noisePeriod = regs[14].toInt() and 0x0F
            val noiseMode = regs[14].toInt() and 0x80 != 0
            val noiseLen = regs[15].toInt() and 0xFF
            val enabled = regs[21].toInt() and 0x08 != 0
            val noiseBuf = system.ram.noiseSoundBuffer.toInt() and 0xFF
            val noiseSfxLen = system.ram.noiseSfxLenCounter.toInt() and 0xFF
            val areaMusicBuf = system.ram.areaMusicBuffer.toInt() and 0xFF

            if (frame >= 330 && frame <= 400) {
                println("F$frame: noiseVol=$noiseVol period=$noisePeriod mode=$noiseMode len=$noiseLen enabled=$enabled active=${audioOutput.noiseActiveForDebug()} noiseBuf=$noiseBuf sfxLen=$noiseSfxLen music=0x${areaMusicBuf.toString(16)}")
            }
        }
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

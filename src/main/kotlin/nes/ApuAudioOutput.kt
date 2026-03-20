// by Claude - NES APU audio synthesizer using javax.sound.sampled
package com.ivieleague.smbtranslation.nes

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

/**
 * Synthesizes NES APU audio and outputs via JVM audio.
 * Reads APU raw register state each frame and generates PCM samples.
 *
 * NES APU reference:
 * - CPU clock: 1,789,773 Hz (NTSC)
 * - Pulse channels: square wave, 4 duty cycles, 11-bit timer period
 *   Frequency = CPU_CLOCK / (16 * (period + 1))
 * - Triangle channel: triangle wave, 11-bit timer period
 *   Frequency = CPU_CLOCK / (32 * (period + 1))
 * - Noise channel: LFSR-based, 4-bit period index
 */
class ApuAudioOutput(private val sampleRate: Int = 44100) {

    companion object {
        private const val CPU_CLOCK = 1789773.0

        // NES noise period lookup table (NTSC)
        private val noisePeriodTable = intArrayOf(
            4, 8, 16, 32, 64, 96, 128, 160, 202, 254, 380, 508, 762, 1016, 2034, 4068
        )

        // Pulse duty cycle waveforms (8 steps each)
        // 0 = 12.5%, 1 = 25%, 2 = 50%, 3 = 75% (inverted 25%)
        private val dutyTable = arrayOf(
            intArrayOf(0, 1, 0, 0, 0, 0, 0, 0), // 12.5%
            intArrayOf(0, 1, 1, 0, 0, 0, 0, 0), // 25%
            intArrayOf(0, 1, 1, 1, 1, 0, 0, 0), // 50%
            intArrayOf(1, 0, 0, 1, 1, 1, 1, 1), // 75%
        )

        // Triangle waveform (32 steps)
        private val triangleTable = IntArray(32) { i ->
            if (i < 16) 15 - i else i - 16
        }
    }

    private var line: SourceDataLine? = null
    private val samplesPerFrame = sampleRate / 60
    private val buffer = ByteArray(samplesPerFrame * 2) // 16-bit mono

    // Phase accumulators for each channel (in CPU clock cycles)
    private var pulse1Phase = 0.0
    private var pulse2Phase = 0.0
    private var trianglePhase = 0.0
    private var noisePhase = 0.0

    // Pulse duty step positions
    private var pulse1Step = 0
    private var pulse2Step = 0
    private var triangleStep = 0

    // Noise LFSR (15-bit)
    private var noiseLfsr = 1

    fun start() {
        val format = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
        val line = AudioSystem.getSourceDataLine(format)
        line.open(format, samplesPerFrame * 4) // buffer for ~2 frames
        line.start()
        this.line = line
    }

    fun stop() {
        line?.stop()
        line?.close()
        line = null
    }

    /**
     * Generates one frame of audio from APU state and writes to the audio output.
     * Call once per NMI (60fps).
     */
    fun outputFrame(apu: AudioProcessingUnit) {
        val line = this.line ?: return
        val regs = apu.rawRegs

        // Read pulse 1 state
        val p1Vol = regs[0].toInt() and 0x0F
        val p1Duty = (regs[0].toInt() shr 6) and 3
        val p1Period = (regs[2].toInt() and 0xFF) or ((regs[3].toInt() and 7) shl 8)
        val p1Enabled = apu.pulse1Enabled && p1Period >= 8 && p1Vol > 0

        // Read pulse 2 state
        val p2Vol = regs[4].toInt() and 0x0F
        val p2Duty = (regs[4].toInt() shr 6) and 3
        val p2Period = (regs[6].toInt() and 0xFF) or ((regs[7].toInt() and 7) shl 8)
        val p2Enabled = apu.pulse2Enabled && p2Period >= 8 && p2Vol > 0

        // Read triangle state
        val triPeriod = (regs[10].toInt() and 0xFF) or ((regs[11].toInt() and 7) shl 8)
        val triLinear = regs[8].toInt() and 0x7F
        val triEnabled = apu.triangleEnabled && triPeriod >= 2 && triLinear > 0

        // Read noise state
        val noiseVol = regs[12].toInt() and 0x0F
        val noisePeriodIdx = regs[14].toInt() and 0x0F
        val noiseMode = regs[14].toInt() and 0x80 != 0
        val noisePeriod = noisePeriodTable[noisePeriodIdx]
        val noiseEnabled = apu.noiseEnabled && noiseVol > 0

        // CPU clocks per sample
        val clocksPerSample = CPU_CLOCK / sampleRate

        for (i in 0 until samplesPerFrame) {
            var sample = 0.0

            // Pulse 1
            if (p1Enabled) {
                pulse1Phase += clocksPerSample
                val stepSize = (p1Period + 1).toDouble() * 2.0
                while (pulse1Phase >= stepSize) {
                    pulse1Phase -= stepSize
                    pulse1Step = (pulse1Step + 1) and 7
                }
                sample += dutyTable[p1Duty][pulse1Step] * p1Vol * 0.00752
            }

            // Pulse 2
            if (p2Enabled) {
                pulse2Phase += clocksPerSample
                val stepSize = (p2Period + 1).toDouble() * 2.0
                while (pulse2Phase >= stepSize) {
                    pulse2Phase -= stepSize
                    pulse2Step = (pulse2Step + 1) and 7
                }
                sample += dutyTable[p2Duty][pulse2Step] * p2Vol * 0.00752
            }

            // Triangle
            if (triEnabled) {
                trianglePhase += clocksPerSample
                val stepSize = (triPeriod + 1).toDouble()
                while (trianglePhase >= stepSize) {
                    trianglePhase -= stepSize
                    triangleStep = (triangleStep + 1) and 31
                }
                sample += triangleTable[triangleStep] * 0.00851
            }

            // Noise
            if (noiseEnabled) {
                noisePhase += clocksPerSample
                while (noisePhase >= noisePeriod) {
                    noisePhase -= noisePeriod
                    // Clock the LFSR
                    val feedback = if (noiseMode) {
                        (noiseLfsr xor (noiseLfsr shr 6)) and 1
                    } else {
                        (noiseLfsr xor (noiseLfsr shr 1)) and 1
                    }
                    noiseLfsr = (noiseLfsr shr 1) or (feedback shl 14)
                }
                val noiseOut = if (noiseLfsr and 1 == 0) noiseVol else 0
                sample += noiseOut * 0.00494
            }

            // Mix and clamp to 16-bit signed
            val mixed = (sample * 20000).toInt().coerceIn(-32768, 32767)
            buffer[i * 2] = (mixed and 0xFF).toByte()
            buffer[i * 2 + 1] = (mixed shr 8).toByte()
        }

        // Write to audio output (non-blocking to avoid frame drops)
        val available = line.available()
        if (available >= buffer.size) {
            line.write(buffer, 0, buffer.size)
        }
    }
}

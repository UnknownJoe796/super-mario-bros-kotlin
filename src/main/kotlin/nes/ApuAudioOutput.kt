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
 * - Pulse: square wave, 4 duty cycles, freq = CPU / (16 * (period + 1))
 * - Triangle: triangle wave, freq = CPU / (32 * (period + 1))
 * - Noise: LFSR-based, 4-bit period index
 */
class ApuAudioOutput(private val sampleRate: Int = 44100) {

    companion object {
        private const val CPU_CLOCK = 1789773.0

        private val noisePeriodTable = intArrayOf(
            4, 8, 16, 32, 64, 96, 128, 160, 202, 254, 380, 508, 762, 1016, 2034, 4068
        )

        // Pulse duty waveforms: 0=12.5%, 1=25%, 2=50%, 3=75%
        private val dutyTable = arrayOf(
            intArrayOf(0, 1, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 1, 1, 0, 0, 0, 0, 0),
            intArrayOf(0, 1, 1, 1, 1, 0, 0, 0),
            intArrayOf(1, 0, 0, 1, 1, 1, 1, 1),
        )

        // Triangle 32-step waveform
        private val triangleTable = IntArray(32) { if (it < 16) 15 - it else it - 16 }
    }

    private var line: SourceDataLine? = null
    private val samplesPerFrame = sampleRate / 60
    private val buffer = ByteArray(samplesPerFrame * 2)

    // Per-channel state
    private var pulse1Phase = 0.0
    private var pulse2Phase = 0.0
    private var trianglePhase = 0.0
    private var noisePhase = 0.0
    private var pulse1Step = 0
    private var pulse2Step = 0
    private var triangleStep = 0
    private var noiseLfsr = 1

    // Track previous register values to detect changes
    private val prevRegs = ByteArray(24)
    private var prevEnabled = 0

    fun start() {
        val format = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
        val line = AudioSystem.getSourceDataLine(format)
        line.open(format, samplesPerFrame * 4)
        line.start()
        this.line = line
    }

    fun stop() {
        line?.stop()
        line?.close()
        line = null
    }

    /**
     * Generates samples for one frame into a ShortArray.
     */
    fun generateSamples(apu: AudioProcessingUnit, numSamples: Int = samplesPerFrame): ShortArray {
        val regs = apu.rawRegs
        val result = ShortArray(numSamples)

        val enabled = (regs[21].toInt() and 0xFF)

        // Detect channel enable/disable transitions — reset phase on re-enable
        val wasEnabled = prevEnabled
        if (enabled and 0x01 != 0 && wasEnabled and 0x01 == 0) { pulse1Phase = 0.0; pulse1Step = 0 }
        if (enabled and 0x02 != 0 && wasEnabled and 0x02 == 0) { pulse2Phase = 0.0; pulse2Step = 0 }
        if (enabled and 0x04 != 0 && wasEnabled and 0x04 == 0) { trianglePhase = 0.0; triangleStep = 0 }

        // Detect frequency changes — reset phase for cleaner transitions
        if (regs[2] != prevRegs[2] || regs[3] != prevRegs[3]) { pulse1Phase = 0.0; pulse1Step = 0 }
        if (regs[6] != prevRegs[6] || regs[7] != prevRegs[7]) { pulse2Phase = 0.0; pulse2Step = 0 }
        if (regs[10] != prevRegs[10] || regs[11] != prevRegs[11]) { trianglePhase = 0.0; triangleStep = 0 }

        // Read channel parameters
        val p1Vol = regs[0].toInt() and 0x0F
        val p1Duty = (regs[0].toInt() shr 6) and 3
        val p1Period = (regs[2].toInt() and 0xFF) or ((regs[3].toInt() and 7) shl 8)
        val p1Enabled = enabled and 0x01 != 0 && p1Period >= 8 && p1Vol > 0

        val p2Vol = regs[4].toInt() and 0x0F
        val p2Duty = (regs[4].toInt() shr 6) and 3
        val p2Period = (regs[6].toInt() and 0xFF) or ((regs[7].toInt() and 7) shl 8)
        val p2Enabled = enabled and 0x02 != 0 && p2Period >= 8 && p2Vol > 0

        val triPeriod = (regs[10].toInt() and 0xFF) or ((regs[11].toInt() and 7) shl 8)
        val triLinear = regs[8].toInt() and 0x7F
        val triEnabled = enabled and 0x04 != 0 && triPeriod >= 2 && triLinear > 0

        val noiseVol = regs[12].toInt() and 0x0F
        val noisePeriodIdx = regs[14].toInt() and 0x0F
        val noiseMode = regs[14].toInt() and 0x80 != 0
        val noisePeriod = noisePeriodTable[noisePeriodIdx]
        val noiseEnabled = enabled and 0x08 != 0 && noiseVol > 0

        val clocksPerSample = CPU_CLOCK / sampleRate

        for (i in 0 until numSamples) {
            var sample = 0.0

            if (p1Enabled) {
                pulse1Phase += clocksPerSample
                val stepSize = (p1Period + 1).toDouble() * 2.0
                while (pulse1Phase >= stepSize) { pulse1Phase -= stepSize; pulse1Step = (pulse1Step + 1) and 7 }
                sample += (dutyTable[p1Duty][pulse1Step] * 2 - 1) * p1Vol * 0.00752
            }

            if (p2Enabled) {
                pulse2Phase += clocksPerSample
                val stepSize = (p2Period + 1).toDouble() * 2.0
                while (pulse2Phase >= stepSize) { pulse2Phase -= stepSize; pulse2Step = (pulse2Step + 1) and 7 }
                sample += (dutyTable[p2Duty][pulse2Step] * 2 - 1) * p2Vol * 0.00752
            }

            if (triEnabled) {
                trianglePhase += clocksPerSample
                val stepSize = (triPeriod + 1).toDouble()
                while (trianglePhase >= stepSize) { trianglePhase -= stepSize; triangleStep = (triangleStep + 1) and 31 }
                sample += (triangleTable[triangleStep] - 8) * 0.00851
            }

            if (noiseEnabled) {
                noisePhase += clocksPerSample
                while (noisePhase >= noisePeriod) {
                    noisePhase -= noisePeriod
                    val feedback = if (noiseMode) {
                        (noiseLfsr xor (noiseLfsr shr 6)) and 1
                    } else {
                        (noiseLfsr xor (noiseLfsr shr 1)) and 1
                    }
                    noiseLfsr = (noiseLfsr shr 1) or (feedback shl 14)
                }
                sample += (if (noiseLfsr and 1 == 0) noiseVol else -noiseVol) * 0.00494
            }

            result[i] = (sample * 18000).toInt().coerceIn(-32768, 32767).toShort()
        }

        // Save state for next frame
        System.arraycopy(regs, 0, prevRegs, 0, regs.size.coerceAtMost(prevRegs.size))
        prevEnabled = enabled

        return result
    }

    /**
     * Generates one frame of audio and writes to the audio output.
     */
    fun outputFrame(apu: AudioProcessingUnit) {
        val line = this.line ?: return
        val samples = generateSamples(apu)
        for (i in samples.indices) {
            val s = samples[i].toInt()
            buffer[i * 2] = (s and 0xFF).toByte()
            buffer[i * 2 + 1] = (s shr 8).toByte()
        }
        val available = line.available()
        if (available >= buffer.size) {
            line.write(buffer, 0, buffer.size)
        }
    }
}

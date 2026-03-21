// by Claude - NES APU audio synthesizer using javax.sound.sampled
package com.ivieleague.smbtranslation.nes

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

/**
 * Synthesizes NES APU audio and outputs via JVM audio.
 *
 * Key NES APU behaviors modeled:
 * - Writing 0 to a channel's enable bit in $4015 sets its length counter to 0 (silences it)
 * - Writing to $4003/$4007/$400B/$400F reloads the length counter (re-activates sound)
 * - Pulse: square wave, 4 duty cycles, freq = CPU / (16 * (period + 1))
 * - Triangle: triangle wave, freq = CPU / (32 * (period + 1))
 * - Noise: 15-bit LFSR, period from lookup table
 */
class ApuAudioOutput(private val sampleRate: Int = 44100) {

    companion object {
        private const val CPU_CLOCK = 1789773.0

        // NES NTSC noise period table (in CPU cycles)
        private val noisePeriodTable = intArrayOf(
            4, 8, 16, 32, 64, 96, 128, 160, 202, 254, 380, 508, 762, 1016, 2034, 4068
        )

        // Pulse duty waveforms: 0=12.5%, 1=25%, 2=50%, 3=75%
        private val dutyTable = arrayOf(
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 1),
            intArrayOf(0, 0, 0, 0, 0, 0, 1, 1),
            intArrayOf(0, 0, 0, 0, 1, 1, 1, 1),
            intArrayOf(1, 1, 1, 1, 1, 1, 0, 0),
        )

        // Triangle 32-step waveform (centered on 0)
        private val triangleTable = IntArray(32) { if (it < 16) 15 - it else it - 16 }
    }

    private var line: SourceDataLine? = null
    private val samplesPerFrame = sampleRate / 60
    private val buffer = ByteArray(samplesPerFrame * 2)

    // Per-channel synthesis state
    private var pulse1Phase = 0.0
    private var pulse2Phase = 0.0
    private var trianglePhase = 0.0
    private var noisePhase = 0.0
    private var pulse1Step = 0
    private var pulse2Step = 0
    private var triangleStep = 0
    private var noiseLfsr = 1

    // Length counter active flags:
    // On real NES, writing 0 to a channel's $4015 bit sets length counter to 0 (silences it).
    // Writing to the length/timer-high register ($4003/$4007/$400B/$400F) reloads it.
    // We track this as a simple boolean: false = silenced, true = active.
    private var pulse1Active = false
    private var pulse2Active = false
    private var triangleActive = false
    private var noiseActive = false

    // Previous register state to detect changes
    private val prevRegs = ByteArray(24)

    // DC blocking filter state
    private var dcPrev = 0.0
    private var dcOut = 0.0

    fun start() {
        val format = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
        val line = AudioSystem.getSourceDataLine(format)
        line.open(format, samplesPerFrame * 6) // ~3 frames of buffer
        line.start()
        this.line = line
    }

    fun stop() {
        line?.stop()
        line?.close()
        line = null
    }

    /**
     * Notify that $4015 was written. Channels whose enable bit is 0 get silenced.
     * Called from soundEngine's writeSndMasterCtrl.
     */
    fun onMasterCtrlWrite(value: Int) {
        if (value and 0x01 == 0) pulse1Active = false
        if (value and 0x02 == 0) pulse2Active = false
        if (value and 0x04 == 0) triangleActive = false
        if (value and 0x08 == 0) noiseActive = false
    }

    /**
     * Notify that a channel's length/timer-high register was written.
     * This reloads the length counter, reactivating the channel.
     */
    fun onLengthLoad(channel: Int) {
        when (channel) {
            0 -> { pulse1Active = true; pulse1Phase = 0.0; pulse1Step = 0 }
            1 -> { pulse2Active = true; pulse2Phase = 0.0; pulse2Step = 0 }
            2 -> { triangleActive = true; trianglePhase = 0.0; triangleStep = 0 }
            3 -> { noiseActive = true }
        }
    }

    fun generateSamples(apu: AudioProcessingUnit, numSamples: Int = samplesPerFrame): ShortArray {
        val regs = apu.rawRegs
        val result = ShortArray(numSamples)

        // Read channel parameters
        val p1Vol = regs[0].toInt() and 0x0F
        val p1Duty = (regs[0].toInt() shr 6) and 3
        val p1Period = (regs[2].toInt() and 0xFF) or ((regs[3].toInt() and 7) shl 8)
        val p1On = pulse1Active && p1Period >= 8 && p1Vol > 0

        val p2Vol = regs[4].toInt() and 0x0F
        val p2Duty = (regs[4].toInt() shr 6) and 3
        val p2Period = (regs[6].toInt() and 0xFF) or ((regs[7].toInt() and 7) shl 8)
        val p2On = pulse2Active && p2Period >= 8 && p2Vol > 0

        val triPeriod = (regs[10].toInt() and 0xFF) or ((regs[11].toInt() and 7) shl 8)
        val triLinear = regs[8].toInt() and 0x7F
        val triOn = triangleActive && triPeriod >= 2 && triLinear > 0

        val noiseVol = regs[12].toInt() and 0x0F
        val noisePeriodIdx = regs[14].toInt() and 0x0F
        val noiseMode = regs[14].toInt() and 0x80 != 0
        val noisePeriod = noisePeriodTable[noisePeriodIdx]
        val noiseOn = noiseActive && noiseVol > 0

        val clocksPerSample = CPU_CLOCK / sampleRate

        for (i in 0 until numSamples) {
            // NES nonlinear mixer approximation
            var pulseSum = 0
            var tndSum = 0.0

            if (p1On) {
                pulse1Phase += clocksPerSample
                val stepSize = (p1Period + 1).toDouble() * 2.0
                while (pulse1Phase >= stepSize) { pulse1Phase -= stepSize; pulse1Step = (pulse1Step + 1) and 7 }
                if (dutyTable[p1Duty][pulse1Step] != 0) pulseSum += p1Vol
            }

            if (p2On) {
                pulse2Phase += clocksPerSample
                val stepSize = (p2Period + 1).toDouble() * 2.0
                while (pulse2Phase >= stepSize) { pulse2Phase -= stepSize; pulse2Step = (pulse2Step + 1) and 7 }
                if (dutyTable[p2Duty][pulse2Step] != 0) pulseSum += p2Vol
            }

            var triOut = 0
            if (triOn) {
                trianglePhase += clocksPerSample
                val stepSize = (triPeriod + 1).toDouble()
                while (trianglePhase >= stepSize) { trianglePhase -= stepSize; triangleStep = (triangleStep + 1) and 31 }
                triOut = triangleTable[triangleStep]
            }

            var noiseOut = 0
            if (noiseOn) {
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
                if (noiseLfsr and 1 == 0) noiseOut = noiseVol
            }

            // NES mixer formulas (from nesdev wiki)
            val pulseOut = if (pulseSum > 0) 95.88 / (8128.0 / pulseSum + 100.0) else 0.0
            tndSum = if (triOut > 0 || noiseOut > 0) {
                159.79 / (1.0 / (triOut / 8227.0 + noiseOut / 12241.0 + 0.0) + 100.0)
            } else 0.0

            val mixed = pulseOut + tndSum

            // Simple DC blocking filter
            dcOut = mixed - dcPrev + 0.995 * dcOut
            dcPrev = mixed

            result[i] = (dcOut * 32000).toInt().coerceIn(-32768, 32767).toShort()
        }

        System.arraycopy(regs, 0, prevRegs, 0, regs.size.coerceAtMost(prevRegs.size))
        return result
    }

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

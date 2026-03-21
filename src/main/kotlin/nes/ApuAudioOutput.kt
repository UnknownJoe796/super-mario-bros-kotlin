// by Claude - NES APU audio synthesizer using javax.sound.sampled
package com.ivieleague.smbtranslation.nes

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

/**
 * Synthesizes NES APU audio and outputs via JVM audio.
 *
 * Key NES APU hardware behaviors modeled:
 * - Hardware length counter: loaded from LENGTH_TABLE on reg 3/7/B/F write,
 *   decremented every other frame (240Hz). Channel silences when counter hits 0.
 * - $4015 disable: sets length counter to 0 (immediate silence)
 * - Nonlinear mixer from nesdev wiki
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

        // Triangle 32-step waveform
        private val triangleTable = IntArray(32) { if (it < 16) 15 - it else it - 16 }

        // NES APU length counter lookup table (indexed by bits 7-3 of $4003/$4007/$400B/$400F)
        private val lengthTable = intArrayOf(
            10, 254, 20,  2, 40,  4, 80,  6, 160,  8, 60, 10, 14, 12, 26, 14,
            12,  16, 24, 18, 48, 20, 96, 22, 192, 24, 72, 26, 16, 28, 32, 30
        )
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

    // Hardware length counters (in half-frames, decremented at 240Hz = 4x per frame)
    private var pulse1LenCtr = 0
    private var pulse2LenCtr = 0
    private var triangleLenCtr = 0
    private var noiseLenCtr = 0

    // Length counter halt flags (bit 5 of $4000/$4004, bit 7 of $4008, bit 5 of $400C)
    private var pulse1LenHalt = false
    private var pulse2LenHalt = false
    private var triangleLenHalt = false
    private var noiseLenHalt = false

    // Triangle linear counter (decremented at 240Hz = 4x per frame)
    // Loaded from $4008 bits 6-0 when $400B is written. Channel silences when 0.
    private var triLinearCounter = 0
    private var triLinearReload = 0  // value to reload from $4008

    // Channel enabled via $4015
    private var pulse1Enabled = false
    private var pulse2Enabled = false
    private var triangleEnabled = false
    private var noiseEnabled = false

    // DC blocking filter state
    private var dcPrev = 0.0
    private var dcOut = 0.0

    // Frame counter for length counter clocking (two half-frames per NMI frame)
    private var halfFrameToggle = false

    fun noiseActiveForDebug() = noiseLenCtr > 0

    fun start() {
        val format = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
        val line = AudioSystem.getSourceDataLine(format)
        line.open(format, samplesPerFrame * 6)
        line.start()
        this.line = line
    }

    fun stop() {
        line?.stop()
        line?.close()
        line = null
    }

    /**
     * Notify that $4015 was written. Channels whose enable bit is 0 get length counter = 0.
     */
    fun onMasterCtrlWrite(value: Int) {
        pulse1Enabled = value and 0x01 != 0
        pulse2Enabled = value and 0x02 != 0
        triangleEnabled = value and 0x04 != 0
        noiseEnabled = value and 0x08 != 0
        if (!pulse1Enabled) pulse1LenCtr = 0
        if (!pulse2Enabled) pulse2LenCtr = 0
        if (!triangleEnabled) triangleLenCtr = 0
        if (!noiseEnabled) noiseLenCtr = 0
    }

    /**
     * Notify that a channel's length/timer-high register was written.
     * Loads the hardware length counter from the LENGTH_TABLE.
     */
    fun onLengthLoad(channel: Int, regValue: Int) {
        val tableIdx = (regValue shr 3) and 0x1F
        val length = lengthTable[tableIdx]
        when (channel) {
            0 -> { if (pulse1Enabled) pulse1LenCtr = length; pulse1Phase = 0.0; pulse1Step = 0 }
            1 -> { if (pulse2Enabled) pulse2LenCtr = length; pulse2Phase = 0.0; pulse2Step = 0 }
            2 -> {
                if (triangleEnabled) triangleLenCtr = length
                // Reload the linear counter from the value stored by $4008
                triLinearCounter = triLinearReload
                trianglePhase = 0.0; triangleStep = 0
            }
            3 -> { if (noiseEnabled) noiseLenCtr = length }
        }
    }

    /**
     * Notify that a channel's control register ($4000/$4004/$4008/$400C) was written.
     * Updates the length counter halt flag.
     */
    fun onControlWrite(channel: Int, regValue: Int) {
        when (channel) {
            0 -> pulse1LenHalt = regValue and 0x20 != 0
            1 -> pulse2LenHalt = regValue and 0x20 != 0
            2 -> {
                triangleLenHalt = regValue and 0x80 != 0
                triLinearReload = regValue and 0x7F  // bits 6-0 = linear counter reload value
            }
            3 -> noiseLenHalt = regValue and 0x20 != 0
        }
    }

    fun generateSamples(apu: AudioProcessingUnit, numSamples: Int = samplesPerFrame): ShortArray {
        val regs = apu.rawRegs
        val result = ShortArray(numSamples)

        // Clock length counters at 120Hz (2 half-frames per NMI frame)
        for (hf in 0..1) {
            if (pulse1LenCtr > 0 && !pulse1LenHalt) pulse1LenCtr--
            if (pulse2LenCtr > 0 && !pulse2LenHalt) pulse2LenCtr--
            if (triangleLenCtr > 0 && !triangleLenHalt) triangleLenCtr--
            if (noiseLenCtr > 0 && !noiseLenHalt) noiseLenCtr--
        }

        // Clock triangle linear counter at 240Hz (4 quarter-frames per NMI frame)
        // Triangle only plays when BOTH length counter AND linear counter are > 0
        for (qf in 0..3) {
            if (triLinearCounter > 0) triLinearCounter--
        }

        // Read channel parameters
        val p1Vol = regs[0].toInt() and 0x0F
        val p1Duty = (regs[0].toInt() shr 6) and 3
        val p1Period = (regs[2].toInt() and 0xFF) or ((regs[3].toInt() and 7) shl 8)
        val p1On = pulse1LenCtr > 0 && p1Period >= 8 && p1Vol > 0

        val p2Vol = regs[4].toInt() and 0x0F
        val p2Duty = (regs[4].toInt() shr 6) and 3
        val p2Period = (regs[6].toInt() and 0xFF) or ((regs[7].toInt() and 7) shl 8)
        val p2On = pulse2LenCtr > 0 && p2Period >= 8 && p2Vol > 0

        val triPeriod = (regs[10].toInt() and 0xFF) or ((regs[11].toInt() and 7) shl 8)
        // Triangle plays only when length counter > 0 AND linear counter > 0
        val triOn = triangleLenCtr > 0 && triLinearCounter > 0 && triPeriod >= 2

        val noiseVol = regs[12].toInt() and 0x0F
        val noisePeriodIdx = regs[14].toInt() and 0x0F
        val noiseMode = regs[14].toInt() and 0x80 != 0
        val noisePeriod = noisePeriodTable[noisePeriodIdx]
        val noiseOn = noiseLenCtr > 0 && noiseVol > 0

        val clocksPerSample = CPU_CLOCK / sampleRate

        for (i in 0 until numSamples) {
            var pulseSum = 0

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

            // NES nonlinear mixer (nesdev wiki)
            val pulseOut = if (pulseSum > 0) 95.88 / (8128.0 / pulseSum + 100.0) else 0.0
            val tndOut = if (triOut > 0 || noiseOut > 0) {
                159.79 / (1.0 / (triOut / 8227.0 + noiseOut / 12241.0) + 100.0)
            } else 0.0

            val mixed = pulseOut + tndOut

            // DC blocking filter
            dcOut = mixed - dcPrev + 0.995 * dcOut
            dcPrev = mixed

            result[i] = (dcOut * 32000).toInt().coerceIn(-32768, 32767).toShort()
        }

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

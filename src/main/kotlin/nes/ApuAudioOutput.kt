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
    private var triLinearCounter = 0
    private var triLinearReload = 0

    // Pulse sweep units: automatically adjust timer period for pitch slides
    // The NES sweep unit shifts the timer period every (period+1) half-frames.
    // If negate: subtracts shifted amount (pitch rises). Else: adds (pitch drops).
    private var pulse1SweepEnabled = false
    private var pulse1SweepPeriod = 0     // divider period (0-7)
    private var pulse1SweepNegate = false
    private var pulse1SweepShift = 0      // shift amount (0-7)
    private var pulse1SweepDivider = 0    // current divider counter
    private var pulse1TimerPeriod = 0     // current 11-bit timer period (modified by sweep)

    private var pulse2SweepEnabled = false
    private var pulse2SweepPeriod = 0
    private var pulse2SweepNegate = false
    private var pulse2SweepShift = 0
    private var pulse2SweepDivider = 0
    private var pulse2TimerPeriod = 0

    // Hardware envelope generators (pulse 1, pulse 2, noise)
    // When constant volume flag is 0, the envelope decays volume from 15→0
    // at a rate controlled by the volume/period field, clocked at 240Hz
    private var pulse1EnvCounter = 0
    private var pulse1EnvDecay = 15
    private var pulse1EnvStart = false
    private var pulse1ConstVol = true
    private var pulse1VolPeriod = 0

    private var pulse2EnvCounter = 0
    private var pulse2EnvDecay = 15
    private var pulse2EnvStart = false
    private var pulse2ConstVol = true
    private var pulse2VolPeriod = 0

    private var noiseEnvCounter = 0
    private var noiseEnvDecay = 15
    private var noiseEnvStart = false
    private var noiseConstVol = true
    private var noiseVolPeriod = 0

    // Sweep reload flags (set on $4001/$4005 write, cleared after reload)
    private var pulse1SweepReload = false
    private var pulse2SweepReload = false

    // Sweep mute flags (set when target period > $7FF or current period < 8)
    private var pulse1SweepMute = false
    private var pulse2SweepMute = false

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

    /** Clock one envelope generator (called at quarter-frame rate, 240Hz). */
    private fun clockEnvelope(channel: Int) {
        val start = when (channel) { 0 -> pulse1EnvStart; 1 -> pulse2EnvStart; else -> noiseEnvStart }
        val loop = when (channel) { 0 -> pulse1LenHalt; 1 -> pulse2LenHalt; else -> noiseLenHalt }
        val period = when (channel) { 0 -> pulse1VolPeriod; 1 -> pulse2VolPeriod; else -> noiseVolPeriod }

        if (start) {
            when (channel) { 0 -> pulse1EnvStart = false; 1 -> pulse2EnvStart = false; else -> noiseEnvStart = false }
            when (channel) { 0 -> pulse1EnvDecay = 15; 1 -> pulse2EnvDecay = 15; else -> noiseEnvDecay = 15 }
            when (channel) { 0 -> pulse1EnvCounter = period; 1 -> pulse2EnvCounter = period; else -> noiseEnvCounter = period }
        } else {
            var counter = when (channel) { 0 -> pulse1EnvCounter; 1 -> pulse2EnvCounter; else -> noiseEnvCounter }
            if (counter == 0) {
                counter = period
                var decay = when (channel) { 0 -> pulse1EnvDecay; 1 -> pulse2EnvDecay; else -> noiseEnvDecay }
                if (decay > 0) {
                    decay--
                } else if (loop) {
                    decay = 15
                }
                when (channel) { 0 -> pulse1EnvDecay = decay; 1 -> pulse2EnvDecay = decay; else -> noiseEnvDecay = decay }
            } else {
                counter--
            }
            when (channel) { 0 -> pulse1EnvCounter = counter; 1 -> pulse2EnvCounter = counter; else -> noiseEnvCounter = counter }
        }
    }

    /** Get effective volume for a channel (constant volume or envelope decay). */
    private fun getVolume(channel: Int): Int {
        return when (channel) {
            0 -> if (pulse1ConstVol) pulse1VolPeriod else pulse1EnvDecay
            1 -> if (pulse2ConstVol) pulse2VolPeriod else pulse2EnvDecay
            else -> if (noiseConstVol) noiseVolPeriod else noiseEnvDecay
        }
    }

    /**
     * Clock one pulse channel's sweep unit (called at half-frame rate).
     * Matches NES hardware behavior per nesdev wiki.
     */
    private fun clockSweep(isPulse1: Boolean) {
        val period = if (isPulse1) pulse1TimerPeriod else pulse2TimerPeriod
        val shift = if (isPulse1) pulse1SweepShift else pulse2SweepShift
        val negate = if (isPulse1) pulse1SweepNegate else pulse2SweepNegate
        val enabled = if (isPulse1) pulse1SweepEnabled else pulse2SweepEnabled
        val dividerPeriod = if (isPulse1) pulse1SweepPeriod else pulse2SweepPeriod
        val divider = if (isPulse1) pulse1SweepDivider else pulse2SweepDivider
        val reload = if (isPulse1) pulse1SweepReload else pulse2SweepReload

        // Compute target period (always computed for mute check)
        val changeAmount = period shr shift
        val targetPeriod = if (negate) {
            if (isPulse1) period - changeAmount - 1 else period - changeAmount  // pulse 1 = ones' complement
        } else {
            period + changeAmount
        }

        // Mute if current period < 8 or target > $7FF
        val muted = period < 8 || targetPeriod > 0x7FF
        if (isPulse1) pulse1SweepMute = muted else pulse2SweepMute = muted

        // Clock the divider
        if (divider == 0 || reload) {
            // Reload divider
            if (isPulse1) {
                pulse1SweepDivider = dividerPeriod
                pulse1SweepReload = false
            } else {
                pulse2SweepDivider = dividerPeriod
                pulse2SweepReload = false
            }
            // If divider was 0 (not just reload): adjust period
            if (divider == 0 && enabled && shift > 0 && !muted) {
                val newPeriod = targetPeriod.coerceIn(0, 0x7FF)
                if (isPulse1) pulse1TimerPeriod = newPeriod else pulse2TimerPeriod = newPeriod
            }
        } else {
            if (isPulse1) pulse1SweepDivider-- else pulse2SweepDivider--
        }
    }

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
     * Unified register write handler. Tracks all hardware state needed for synthesis.
     */
    fun onRegWrite(offset: Int, value: Int) {
        when (offset) {
            // Pulse 1 control ($4000)
            0 -> {
                pulse1LenHalt = value and 0x20 != 0
                pulse1ConstVol = value and 0x10 != 0
                pulse1VolPeriod = value and 0x0F
            }
            // Pulse 1 sweep ($4001)
            1 -> {
                pulse1SweepEnabled = value and 0x80 != 0
                pulse1SweepPeriod = (value shr 4) and 7
                pulse1SweepNegate = value and 0x08 != 0
                pulse1SweepShift = value and 7
                pulse1SweepReload = true
            }
            // Pulse 1 timer low ($4002)
            2 -> pulse1TimerPeriod = (pulse1TimerPeriod and 0x700) or (value and 0xFF)
            // Pulse 1 timer high + length ($4003)
            3 -> {
                pulse1TimerPeriod = (pulse1TimerPeriod and 0x0FF) or ((value and 7) shl 8)
                pulse1EnvStart = true  // restart envelope
                onLengthLoad(0, value)
            }
            // Pulse 2 control ($4004)
            4 -> {
                pulse2LenHalt = value and 0x20 != 0
                pulse2ConstVol = value and 0x10 != 0
                pulse2VolPeriod = value and 0x0F
            }
            // Pulse 2 sweep ($4005)
            5 -> {
                pulse2SweepEnabled = value and 0x80 != 0
                pulse2SweepPeriod = (value shr 4) and 7
                pulse2SweepNegate = value and 0x08 != 0
                pulse2SweepShift = value and 7
                pulse2SweepReload = true
            }
            // Pulse 2 timer low ($4006)
            6 -> pulse2TimerPeriod = (pulse2TimerPeriod and 0x700) or (value and 0xFF)
            // Pulse 2 timer high + length ($4007)
            7 -> {
                pulse2TimerPeriod = (pulse2TimerPeriod and 0x0FF) or ((value and 7) shl 8)
                pulse2EnvStart = true
                onLengthLoad(1, value)
            }
            // Triangle control ($4008)
            8 -> {
                triangleLenHalt = value and 0x80 != 0
                triLinearReload = value and 0x7F
            }
            // Triangle timer high + length ($400B)
            11 -> onLengthLoad(2, value)
            // Noise control ($400C)
            12 -> {
                noiseLenHalt = value and 0x20 != 0
                noiseConstVol = value and 0x10 != 0
                noiseVolPeriod = value and 0x0F
            }
            // Noise length ($400F)
            15 -> {
                noiseEnvStart = true
                onLengthLoad(3, value)
            }
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

        // Clock triangle linear counter and envelopes at 240Hz (4 quarter-frames per NMI frame)
        for (qf in 0..3) {
            if (triLinearCounter > 0) triLinearCounter--
            // Envelope generators
            clockEnvelope(0)
            clockEnvelope(1)
            clockEnvelope(2) // noise
        }

        // Clock sweep units at 120Hz (2 half-frames per NMI frame)
        // NES sweep behavior per nesdev wiki:
        // 1. Compute target period = current + (current >> shift) or current - ...
        // 2. If target > $7FF, mute (regardless of enabled/shift)
        // 3. If divider == 0 AND enabled AND shift > 0: update timer period
        // 4. If divider == 0 OR reload flag: reload divider, clear reload flag
        // 5. Otherwise decrement divider
        for (hf in 0..1) {
            clockSweep(true)   // pulse 1
            clockSweep(false)  // pulse 2
        }

        // Read channel parameters — use hardware envelope and swept timer periods
        val p1Vol = getVolume(0)  // hardware envelope or constant volume
        val p1Duty = (regs[0].toInt() shr 6) and 3
        val p1Period = pulse1TimerPeriod  // swept period
        val p1On = pulse1LenCtr > 0 && p1Period in 8..0x7FF && p1Vol > 0 && !pulse1SweepMute

        val p2Vol = getVolume(1)
        val p2Duty = (regs[4].toInt() shr 6) and 3
        val p2Period = pulse2TimerPeriod  // swept period, not raw register
        val p2On = pulse2LenCtr > 0 && p2Period in 8..0x7FF && p2Vol > 0 && !pulse2SweepMute

        val triPeriod = (regs[10].toInt() and 0xFF) or ((regs[11].toInt() and 7) shl 8)
        // Triangle plays only when length counter > 0 AND linear counter > 0
        val triOn = triangleLenCtr > 0 && triLinearCounter > 0 && triPeriod >= 2

        val noiseVol = getVolume(2)  // noise envelope
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

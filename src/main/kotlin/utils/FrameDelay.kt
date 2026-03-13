package com.ivieleague.smbtranslation.utils

/**
 * Thrown when a routine wants to wait for the next frame to continue.
 * Caught in nonMaskableInterrupt to suspend current task execution.
 */
class FrameDelay(val nextAction: () -> Unit) : RuntimeException()

/**
 * Suspends execution until the next NMI frame, then resumes with the given block.
 */
fun waitForFrame(nextAction: () -> Unit): Unit {
    throw FrameDelay(nextAction)
}

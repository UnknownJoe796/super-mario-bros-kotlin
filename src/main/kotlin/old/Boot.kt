package com.ivieleague.smbtranslation.old

object Stack {
    val data = ByteArray(0xFF)
    var currentIndex: Int = 0
    fun push(byte: Byte) {
        data[currentIndex++] = byte
    }

    fun pop(): Byte = data[--currentIndex]
    fun clear() {
        currentIndex = 0
    }
}

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class InexactBitSetting

val topScore = ByteArray(6) { 0 }

object Boot {
    var warmBoot: Boolean = false

}



package com.ivieleague.smbtranslation

import kotlin.test.Test

class BufferTranslation {
    @Test fun test() {
        val data = $$"""
            
            TopStatusBarLine:
              .db $20, $43, $05, $16, $0a, $1b, $12, $18 ; "MARIO"
              .db $20, $52, $0b, $20, $18, $1b, $15, $0d ; "WORLD  TIME"
              .db $24, $24, $1d, $12, $16, $0e
              .db $20, $68, $05, $00, $24, $24, $2e, $29 ; score trailing digit and coin display
              .db $23, $c0, $7f, $aa ; attribute table data, clears name table 0 to palette 2
              .db $23, $c2, $01, $ea ; attribute table data, used for coin icon in status bar
              .db $ff ; end of data block

            WorldLivesDisplay:
              .db $21, $cd, $07, $24, $24 ; cross with spaces used on
              .db $29, $24, $24, $24, $24 ; lives display
              .db $21, $4b, $09, $20, $18 ; "WORLD  - " used on lives display
              .db $1b, $15, $0d, $24, $24, $28, $24
              .db $22, $0c, $47, $24 ; possibly used to clear time up
              .db $23, $dc, $01, $ba ; attribute table data for crown if more than 9 lives
              .db $ff

            TwoPlayerTimeUp:
              .db $21, $cd, $05, $16, $0a, $1b, $12, $18 ; "MARIO"
            OnePlayerTimeUp:
              .db $22, $0c, $07, $1d, $12, $16, $0e, $24, $1e, $19 ; "TIME UP"
              .db $ff

            TwoPlayerGameOver:
              .db $21, $cd, $05, $16, $0a, $1b, $12, $18 ; "MARIO"
            OnePlayerGameOver:
              .db $22, $0b, $09, $10, $0a, $16, $0e, $24 ; "GAME OVER"
              .db $18, $1f, $0e, $1b
              .db $ff

            WarpZoneWelcome:
              .db $25, $84, $15, $20, $0e, $15, $0c, $18, $16 ; "WELCOME TO WARP ZONE!"
              .db $0e, $24, $1d, $18, $24, $20, $0a, $1b, $19
              .db $24, $23, $18, $17, $0e, $2b
              .db $26, $25, $01, $24         ; placeholder for left pipe
              .db $26, $2d, $01, $24         ; placeholder for middle pipe
              .db $26, $35, $01, $24         ; placeholder for right pipe
              .db $27, $d9, $46, $aa         ; attribute data
              .db $27, $e1, $45, $aa
              .db $ff
        """.trimIndent()

        val system = System()
        data.lines()
            .filter { it.isNotBlank() }
            .split { it.contains(':') }
            .filter { it.size >= 2 }
            .forEach {
                val name = it[0].trim().trim(':')
                val bytes = it.drop(1).flatMap {
                    it.substringAfter(".db").substringBefore(";").trim().split(',').map { it.trim().trim('$').toInt(16).toByte() }
                }.toByteArray()
                val translated = BufferedPpuUpdate.parseVramBuffer(bytes)
                println("val System.$name get() = listOf(")
                translated.forEach { println("    $it,") }
                println(")")
            }
    }
}

fun <T> List<T>.split(matching: (T)->Boolean): List<List<T>> =
    fold(listOf(listOf<T>())) { acc, next ->
        if(matching(next)) {
            acc + listOf(listOf(next))
        } else {
            acc.dropLast(1) + listOf(acc.last() + next)
        }
    }
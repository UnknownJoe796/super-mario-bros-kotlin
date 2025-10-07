package com.ivieleague.smbtranslation

import kotlin.test.Test

class BufferTranslation {
    @Test
    fun test() {
        val data = $$"""
            
WaterPaletteData:
  .db $3f, $00, $20
  .db $0f, $15, $12, $25
  .db $0f, $3a, $1a, $0f
  .db $0f, $30, $12, $0f
  .db $0f, $27, $12, $0f
  .db $22, $16, $27, $18
  .db $0f, $10, $30, $27
  .db $0f, $16, $30, $27
  .db $0f, $0f, $30, $10
  .db $00

GroundPaletteData:
  .db $3f, $00, $20
  .db $0f, $29, $1a, $0f
  .db $0f, $36, $17, $0f
  .db $0f, $30, $21, $0f
  .db $0f, $27, $17, $0f
  .db $0f, $16, $27, $18
  .db $0f, $1a, $30, $27
  .db $0f, $16, $30, $27
  .db $0f, $0f, $36, $17
  .db $00

UndergroundPaletteData:
  .db $3f, $00, $20
  .db $0f, $29, $1a, $09
  .db $0f, $3c, $1c, $0f
  .db $0f, $30, $21, $1c
  .db $0f, $27, $17, $1c
  .db $0f, $16, $27, $18
  .db $0f, $1c, $36, $17
  .db $0f, $16, $30, $27
  .db $0f, $0c, $3c, $1c
  .db $00

CastlePaletteData:
  .db $3f, $00, $20
  .db $0f, $30, $10, $00
  .db $0f, $30, $10, $00
  .db $0f, $30, $16, $00
  .db $0f, $27, $17, $00
  .db $0f, $16, $27, $18
  .db $0f, $1c, $36, $17
  .db $0f, $16, $30, $27
  .db $0f, $00, $30, $10
  .db $00

DaySnowPaletteData:
  .db $3f, $00, $04
  .db $22, $30, $00, $10
  .db $00

NightSnowPaletteData:
  .db $3f, $00, $04
  .db $0f, $30, $00, $10
  .db $00

MushroomPaletteData:
  .db $3f, $00, $04
  .db $22, $27, $16, $0f
  .db $00

BowserPaletteData:
  .db $3f, $14, $04
  .db $0f, $1a, $30, $27
  .db $00

MarioThanksMessage:
;"THANK YOU MARIO!"
  .db $25, $48, $10
  .db $1d, $11, $0a, $17, $14, $24
  .db $22, $18, $1e, $24
  .db $16, $0a, $1b, $12, $18, $2b
  .db $00

LuigiThanksMessage:
;"THANK YOU LUIGI!"
  .db $25, $48, $10
  .db $1d, $11, $0a, $17, $14, $24
  .db $22, $18, $1e, $24
  .db $15, $1e, $12, $10, $12, $2b
  .db $00

MushroomRetainerSaved:
;"BUT OUR PRINCESS IS IN"
  .db $25, $c5, $16
  .db $0b, $1e, $1d, $24, $18, $1e, $1b, $24
  .db $19, $1b, $12, $17, $0c, $0e, $1c, $1c, $24
  .db $12, $1c, $24, $12, $17
;"ANOTHER CASTLE!"
  .db $26, $05, $0f
  .db $0a, $17, $18, $1d, $11, $0e, $1b, $24
  .db $0c, $0a, $1c, $1d, $15, $0e, $2b, $00

PrincessSaved1:
;"YOUR QUEST IS OVER."
  .db $25, $a7, $13
  .db $22, $18, $1e, $1b, $24
  .db $1a, $1e, $0e, $1c, $1d, $24
  .db $12, $1c, $24, $18, $1f, $0e, $1b, $af
  .db $00

PrincessSaved2:
;"WE PRESENT YOU A NEW QUEST."
  .db $25, $e3, $1b
  .db $20, $0e, $24
  .db $19, $1b, $0e, $1c, $0e, $17, $1d, $24
  .db $22, $18, $1e, $24, $0a, $24, $17, $0e, $20, $24
  .db $1a, $1e, $0e, $1c, $1d, $af
  .db $00

WorldSelectMessage1:
;"PUSH BUTTON B"
  .db $26, $4a, $0d
  .db $19, $1e, $1c, $11, $24
  .db $0b, $1e, $1d, $1d, $18, $17, $24, $0b
  .db $00

WorldSelectMessage2:
;"TO SELECT A WORLD"
  .db $26, $88, $11
  .db $1d, $18, $24, $1c, $0e, $15, $0e, $0c, $1d, $24
  .db $0a, $24, $20, $18, $1b, $15, $0d
  .db $00
        """.trimIndent()

        val system = System()
        data.lines().filter { it.isNotBlank() }.split { it.contains(':') }.filter { it.size >= 2 }.forEach {
                val name = it[0].trim().trim(':')
                val bytes = it.drop(1).flatMap {
                    it.substringAfter(".db").substringBefore(";").trim().split(',')
                        .filter { it.isNotBlank() }
                        .map { it.trim().trim('$').toInt(16).toByte() }
                }.toByteArray()
                val translated = BufferedPpuUpdate.parseVramBuffer(bytes)
                println("val $name = listOf(")
                translated.forEach { println("    $it,") }
                println(")")
            }
    }
}

fun <T> List<T>.split(matching: (T) -> Boolean): List<List<T>> = fold(listOf(listOf<T>())) { acc, next ->
    if (matching(next)) {
        acc + listOf(listOf(next))
    } else {
        acc.dropLast(1) + listOf(acc.last() + next)
    }
}
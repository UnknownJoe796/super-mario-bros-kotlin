---
apply: always
---

### Project overview
This repository is a line‑by‑line translation of the Super Mario Bros 6502 disassembly into readable, idiomatic Kotlin, while preserving RAM‑level behavior. Every assembly line is kept as an adjacent comment next to its Kotlin translation so future readers can audit accuracy against the original `smbdism.asm`.

Key points:
- Kotlin JVM 2.2.0, JDK 17 (via `kotlin.jvmToolchain` and Foojay resolver)
- Gradle build with wrapper; tests run on JUnit Platform using `kotlin("test")`
- Emulator surfaces (PPU/APU/system I/O) are mostly placeholders; focus today is translating pure logic and RAM mutations

### Build and run
- Full build: `./gradlew build`
- Compile only: `./gradlew compileKotlin`
- Run tests: `./gradlew test`

Ensure your IDE project SDK is set to JDK 17. The Gradle wrapper pins the toolchain; prefer `./gradlew` to local Gradle.

### Repository layout
- `src/main/kotlin`
    - Core state and simple behavior, e.g., `GameRam.kt`, `start.kt`, `nonMaskableInterrupt.kt`, PPU/APU surface definitions, utilities
    - In‑progress translated routines mirrored from `smbdism.asm`
- `src/test/kotlin`
    - Unit tests targeting pure logic and data manipulation (avoid TODO‑backed emulator surfaces)
- `smbdism.asm`
    - Canonical 6502 disassembly; serves as the truth source for comments and behavior

### Components

- System - the top level object that holds the entire state of the game, including RAM and I/O.
- GameRam - represents the RAM addresses from the original disassembly.
    - They were directly copied over as plain bytes, but that does NOT always fit how they're used.  For example, if you find a memory address that's used with an offset, you probably need to change its type to a `ByteArray`.  Other examples: if one of the variables is used purely as a flag, you can probably change its type to a `Boolean`.  Another example: if a value is treated as an enum, translating it to a Kotlin enum makes it much more readable.
- PpuRenderer - contains translations of the PPU addresses.
- ApuRenderer - contains translations of the APU addresses.

### A Good Example of a Translation

```kotlin
private fun System.areaParserTaskControl() {
    //> AreaParserTaskControl:
    //> inc DisableScreenFlag     ;turn off screen
    ram.disableScreenFlag = true
    do {
        //> TaskLoop:  jsr AreaParserTaskHandler ;render column set of current area
        areaParserTaskHandler()
        //> lda AreaParserTaskNum     ;check number of tasks
        //> bne TaskLoop              ;if tasks still not all done, do another one
    } while (ram.areaParserTaskNum != 0.toByte())
    //> dec ColumnSets            ;do we need to render more column sets?
    //> bpl OutputCol
    if (--ram.columnSets < 0) {
        //> inc ScreenRoutineTask     ;if not, move on to the next task
        ram.screenRoutineTask++
    }
    //> OutputCol: lda #$06                  ;set vram buffer to output rendered column set
    //> sta VRAM_Buffer_AddrCtrl  ;on next NMI
    ram.vRAMBufferAddrCtrl = 0x06.toByte()
    //> rts
}
```

Things that make the above code good:

- Every single line of the assembly related to the ChkContinue subroutine in the disassembly is preserved as a comment with `//> `.
- The equivalent line(s) of assembly are the comment above their Kotlin counterpart.
- Every subroutine in the disassembly is translated into a Kotlin function.
- Modern control flow constructs are used in a way very reminiscent of the original disassembly.


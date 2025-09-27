Project Guidelines (smb-translation)

This is an attempt to port Super Mario Bros to readable, modifiable, and proper Kotlin, while retaining RAM-level accuracy to the original.

The port includes *every single line of the disassembly* as comments, next to their equivalent Kotlin code.

One day, when this is runnable, you will be able to use a SMB rom to play the game on your computer.

Audience: Advanced Kotlin/Gradle developers working on this repository.

1) Build and Configuration

- Language/Toolchain
  - Kotlin JVM: 2.2.0 (Gradle Kotlin DSL).
  - JVM toolchain: 17 (configured via kotlin.jvmToolchain in build.gradle.kts). The project also uses Foojay resolver in settings.gradle.kts to provision JDKs automatically.
  - Build system: Gradle (wrapper included). Use the provided ./gradlew to ensure consistent versions.

- Build Commands
  - Full build: ./gradlew build
  - Compile only: ./gradlew compileKotlin
  - Run tests: ./gradlew test

- Dependencies
  - Testing: kotlin("test") with JUnit Platform (JUnit 5 engine).

- Notes about current code state (important for builds)
  - The emulator surface (PPU/APU/system routines) is incomplete by design. Many functions intentionally call TODO(). This does not prevent compilation, but will throw if executed at runtime. Avoid calling TODO-backed APIs in tests and sample code.
  - PictureProcessingUnit
    - To avoid JVM signature clashes with property setters, use explicit method names for register-like writes instead of “setXxx”. Example: writeOamAddress(address: Byte). Do not add a method named setOamAddress if there is a property oamAddress; the Kotlin compiler will produce a platform declaration clash on the JVM.
    - VRAM address property name is internalVramAddress (Short). The increment-on-read/write logic in readVram()/writeVram() references this property.

2) Testing

- Framework/Runner
  - kotlin.test API (assertEquals, assertTrue, etc.) running on the JUnit 5 platform. The Gradle test task is already configured with useJUnitPlatform().

- Test Layout
  - Place tests under src/test/kotlin using the same base package com.ivieleague.smbtranslation.
  - Prefer testing pure Kotlin logic that does not depend on unimplemented emulator subsystems.

- Writing Tests Against This Codebase
  - Safe units to test today:
    - Bit-level delegates in utils/bitops.kt (BitAccess, BitRangeAccess, BitAccess2, BitRangeAccess2).
    - Data containers and simple behaviors in GameRam (e.g., Stack push/pop, simple fields, RangeAccess bounds, etc.).
    - Constants in Constants.kt.
  - Avoid calling TODO-backed APIs (e.g., PictureProcessingUnit.scroll, readVram, writeVram; many APU/PPU behaviors; system routines that eventually touch these) inside tests.
  - When you must interact with ByteAccess, create a tiny stub: val backing = object : ByteAccess { override var value: Byte = 0 }.
  - Kotlin Byte arithmetic is signed; use toByte() for literals (0x00.toByte(), etc.) and mask with toInt() when bit-twiddling.

- Running Tests
  - All tests: ./gradlew test
  - Single class: ./gradlew test --tests "com.ivieleague.smbtranslation.ProjectSmokeTest"
  - Single test: ./gradlew test --tests "com.ivieleague.smbtranslation.ProjectSmokeTest.BitRangeAccess2*"
  - In IDE: use the gutter/run icons. Ensure the project SDK is set to JDK 17.

- Adding New Tests
  - Create a file under src/test/kotlin, declare package com.ivieleague.smbtranslation, and import kotlin.test.*.
  - Keep emulator-surface interactions mocked/stubbed until those parts are implemented.
  - Prefer small, deterministic unit tests over integration tests for now; emulator timing/PPU behavior is not implemented yet.

- Verified Example Test (as of 2025-09-25)
  - The following test file was created and executed successfully (2 passing tests) to validate the configuration. It has been removed after verification to keep the repo clean, but you can use this as a template:

  ```kotlin
  class ProjectSmokeTest {
      @Test
      fun `GameRam Stack push and pop maintain LIFO order`() {
          val ram = GameRam()
          ram.stack.clear()
          ram.stack.push(0x01)
          ram.stack.push(0x7F)
          ram.stack.push(0x00)
          assertEquals(0x00, ram.stack.pop())
          assertEquals(0x7F, ram.stack.pop())
          assertEquals(0x01, ram.stack.pop())
          assertEquals(0, ram.stack.currentIndex)
      }

      @Test
      fun `BitRangeAccess2 correctly sets and gets bit ranges`() {
          val backing = object : ByteAccess { override var value: Byte = 0 }
          class Holder(access: ByteAccess) { var field: Byte by BitRangeAccess2(access, 0, 1) }
          val h = Holder(backing)

          h.field = 0b10
          assertEquals(0b10, h.field.toInt())
          assertEquals(0b10, (backing.value.toInt() and 0b11))

          h.field = 0b01
          assertEquals(0b01, h.field.toInt())
          assertEquals(0b01, (backing.value.toInt() and 0b11))

          backing.value = (backing.value.toInt() or (1 shl 7)).toByte()
          assertTrue(backing.value.toInt() and (1 shl 7) != 0)

          h.field = 0b10
          assertTrue(backing.value.toInt() and (1 shl 7) != 0)
          assertEquals(0b10, (backing.value.toInt() and 0b11))
      }
  }
  ```

3) Additional Development Notes

- Code Style
  - Kotlin official code style is enforced (gradle.properties: kotlin.code.style=official). Use ktlint/Detekt if you want stricter checks; not currently wired into the build.

- Byte/Bit Manipulation Patterns
  - The code uses property delegates to model hardware register bitfields. This keeps call sites idiomatic (ppu.control.backgroundTableOffset = true) while mapping onto a backing ByteAccess.
  - When adding new bitfields, prefer the existing delegates (BitAccess2/BitRangeAccess2) over ad-hoc masking.

- API Conventions to Avoid JVM Clashes
  - Do not introduce “setXxx” methods alongside var properties of the same semantic name. On JVM they will both map to setXxx(T) and clash. Prefer verbs like writeXxx, loadXxx, updateXxx for side-effectful register-style operations.
  - Example fixed here: writeOamAddress(Byte) instead of setOamAddress(Byte).

- Emulator Surfaces
  - Many PPU/APU/System functions are placeholders. Keep business logic (e.g., RAM manipulation, math, bit ops) testable without invoking these until the emulator layers are implemented.
  - For scroll/VRAM operations, tests should target state transitions (e.g., backing properties) via stubs, not real I/O.

- Repository Layout
  - src/main/kotlin: production sources (System, GameRam, PPU/APU definitions, utilities, and the in-progress ported routines like start(), nonMaskableInterrupt(), initializeNameTables()).
  - smbdism.asm: reference disassembly for contextual comments.

- Known Sharp Edges
  - Kotlin Byte vs Int literals: use toByte() for byte-typed APIs (e.g., 0x00.toByte()).
  - Signedness: Kotlin Byte is signed; masking to Int is often necessary before shifting or comparing.
  - Some long-running original loops (e.g., NMI wait) have been left as TODOs or commented; do not replicate busy-waits in unit tests.

Housekeeping
- This document (.junie/guidelines.md) is the only artifact added by this task. Any temporary tests used to validate the instructions were removed after verification.

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

### NES package: what exists today
The `nes` package provides a thin, testable surface for PPU/inputs and a high‑level renderer. These are intentionally partial; most I/O routines are `TODO()` so translated logic should avoid depending on them at runtime.

- PictureProcessingUnit (`nes/PictureProcessingUnit.kt`)
  - Fields
    - `control: PpuControl` (PPU $2000) and `mask: PpuMask` (PPU $2001) from `utils` package
    - `status: PpuStatus` (PPU $2002) — currently a value object, not mutating side‑effects
    - `backgroundTiles: Array<NesNametable>(2)` — two nametables of 32x30 tiles
    - `backgroundPalettes: Array<IndirectPalette>(4)` and `spritePalettes: Array<IndirectPalette>(4)`
    - `sprites: Array<Sprite>(64)` — simple 8x8 sprite model
    - `oamAddress: Byte` — mirrors OAMADDR ($2003)
    - `internalVramAddress: VramAddress` — see below for auto‑increment semantics
  - Methods and side‑effects to preserve
    - `writeOamAddress(address: Byte)` — mirrors into `oamAddress`
    - `writeOamData(data: Byte)` — increments `oamAddress` (OAMDATA write side‑effect). The body is `TODO()`; tests should not depend on it.
    - `setVramAddress(value: VramAddress)` — sets `internalVramAddress`
    - `readVram()` / `writeVram(value: Byte)` — both increment `internalVramAddress` by 32 if `control.drawVertical` is true, otherwise by 1. Bodies are `TODO()`.
    - `scroll(x: Byte, y: Byte)` — `TODO()` placeholder for PPUSCROLL behavior.
    - `updateSpriteData(values: Array<GameRam.Sprite>)` — `TODO()` DMA helper.

- High‑level PPU model (`nes/HighLevelPictureProcessingUnit.kt`)
  - `NesNametable` stores `Tile(pattern: Pattern, palette: Palette)` with simple 2bpp pattern logic implemented in `Pattern.colorIndex(x, y)`.
  - `Palette` has `DirectPalette` and `IndirectPalette` implementations; `IndirectPalette` is used for PPU palette slots.
  - `Sprite` holds `y: UByte`, `pattern: Pattern`, `attributes: utils.SpriteFlags`, `x: UByte`.

- Inputs (`nes/Inputs.kt`)
  - Typealiases: `VramAddress = Short` (plus TwoBits/ThreeBits etc.)
  - `Inputs` exposes `joypadPort1`, `joypadPort2` as `utils.JoypadBits`.

- Renderer (`nes/PpuRenderer.kt`)
  - Software renderer for debugging: draws nametable 0 and 8x8 sprites to a Skia `Canvas`.
  - Honors sprite palette index, horizontal/vertical flip, and behind‑background priority. Does not implement 8x16 sprites, scrolling, emphasis, greyscale, sprite zero hit, or overflow.
  - Useful for visual tests; avoid coupling translated core logic to it.

- Audio (`nes/AudioProcessingUnit.kt`)
  - Structural model of APU registers: pulse1/pulse2, triangle, noise, DMC, and status/frame counter bits.
  - All synthesis/side‑effects are `TODO()`; use as a typed container only in translations that need to write sound registers.
  - Channel properties are split into register‑shaped fields (e.g., pulse duty/volume, sweep parameters). Avoid inventing new setters; set the documented fields directly or wrap with `writeXxx` helpers if you need side‑effects later.

Practical guidance for translators working near the PPU:
- Prefer calling the PPU surface methods already provided (e.g., `writeOamAddress`, `readVram`/`writeVram`) and keep their auto‑increment/side‑effects intact.
- Avoid adding JVM‑conflicting setters. Use `writeXxx`/`updateXxx` names as needed.
- When a routine requires PPUSCROLL/PPUSTATUS semantics not yet implemented, structure the code so tests can skip that path (commonly by checking `OperMode`/frame state first) and leave a clear `TODO()` in the PPU layer.

### Core state: GameRam
GameRam models NES RAM variables with a one‑to‑one mapping to the disassembly using the @RamLocation(address) annotation on Kotlin properties. Key points:
- Address mapping and aliases
  - The fields have been initially translated as plain Byte, but as we go, we need to update the types to be more accurate to the meaning.  Booleans for flags, ByteArrays for blocks of memory, and ByteArray windows for indexed memory.
  - The @RamLocation annotation is used to map the disassembly addresses to the Kotlin properties.
- Reset behavior
  - GameRam.clean captures a template instance. GameRam.reset(range) resets only properties whose @RamLocation falls within the given inclusive address range by copying values from clean. Use this to mirror routines that clear RAM pages.
- Controller/PPU mirrors and mode state
  - savedJoypadBits/1/2 hold latched controller states; joypadBitMask and joypadOverride are modeled as Bytes.
  - mirrorPPUCTRLREG1: PpuControl and mirrorPPUCTRLREG2: PpuMask mirror $2000/$2001 state in RAM.
  - operMode is an OperMode enum; operModeTask, screenRoutineTask, timer/status bytes follow the disassembly layout.
- Timers block ($780..)
  - timers is a ByteArray window plus named views (selectTimer, playerAnimTimer, etc.) at specific offsets to match the disassembly. Prefer the named properties in translations for clarity but respect the shared underlying bytes.
- OAM sprite RAM mirror ($0200..$02FF)
  - sprites is an Array(64) of GameRam.Sprite mapping the OAM layout: y, tile number, attributes (SpriteFlags), x. These are conveniences for tests/renderer; keep DMA behavior in PPU layer when translating routines that write OAM via $2003/$2004/$4014.
- Scrolling and column parsing
  - screenLeft/RightPageLoc/XPos, horizontalScroll, verticalScroll, scrollLock, scrollFractional, and parser state (currentPageLoc/currentColumnPos, areaObjectPageLoc/areaDataOffset, etc.) mirror the parser/scrolling state tightly.
  - attributeBuffer at $3F9 models the temporary 32‑byte attribute row buffer used by renderer/update routines.
- VRAM buffering
  - vRAMBuffer1 at $300 and vRAMBuffer2 at $340 are MutableVBuffer (typealias of ArrayList<BufferedPpuUpdate>), with vRAMBufferAddrCtrl ($773) modeling which buffer/addressing mode is active per frame, mirroring SMB’s double‑buffered VRAM updates.
- Area/level/session variables
  - areaType, areaStyle, background/foreground scenery, cloudTypeOverride, backgroundColorCtrl.
  - Header/entrance fields: playerEntranceCtrl, altEntranceControl, entrancePage, areaPointer/areaAddrsLOffset, gameTimerSetting.
  - Player/session bookkeeping: currentPlayer, playerSize/status, numberOfPlayers, lives/coin/world/area numbers, hidden1UpFlag, halfwayPage and their offscreen counterparts for 2‑player mode.
- Misc flags and counters
  - disableScreenFlag, sprite0HitDetectFlag, fetchNewGameTimerFlag, gameTimerExpiredFlag, brickCoinTimerFlag, hard mode flags, worldSelectEnableFlag, continueWorld, warmBootValidation, etc. Preserve exact byte semantics when matching branches in assembly.

Practical tips when translating against GameRam:
- Prefer the Kotlin property names as listed; if the disassembly uses a different alias at the same address, comment it next to the write for auditing.
- Never introduce new setters with JVM‑conflicting names; use write/update helpers in surfaces. For RAM, direct property assignment is correct.
- Mask when indexing into arrays derived from RAM bytes; many indices are 0..7 or 0..31 and need `and 0xFF` before modulo.

### VRAM buffering and updates (BufferedPpuUpdate)
BufferedPpuUpdate is a sealed hierarchy representing high‑level PPU mutations that can be applied to PictureProcessingUnit:
- BackgroundPatternString / BackgroundPatternRepeat
  - Place 8x8 tile patterns into a nametable across X or down Y depending on drawVertically.
  - Patterns come from OriginalRom.backgrounds; translations should write tile IDs into the VRAM buffer bytes; the parser resolves IDs to patterns.
- BackgroundSetPalette / SpriteSetPalette
  - Replace the 4‑entry palette at a given index (0..3) for background or sprites.
- BackgroundAttributeString / BackgroundAttributeRepeat
  - Write attribute bytes; each byte selects palettes per 2x2 tile quadrant within a 4x4 tile cell. Coordinates (ax, ay) are in attribute‑cell space (0..7 x 0..7).

Parser: BufferedPpuUpdate.parseVramBuffer(bytes)
- Input format mirrors SMB’s UpdateScreen buffer records: [addr_hi, addr_lo, control, data...], repeated until addr_hi==0.
- Control byte is utils.VramBufferControl with:
  - bit7 drawVertically → advance by 32 between writes; else by 1.
  - bit6 repeat → either replicate a single data byte ‘length’ times, or read literal ‘length’ bytes.
  - bits0..5 length → masked to 0x3F.
- Supported address ranges:
  - $2000‑$23BF/$2400‑$27BF/$2800‑$2BBF/$2C00‑$2FBF (nametable tile area): emits BackgroundPatternString/Repeat.
  - $23C0‑$23FF (+ mirrors) attribute tables: emits BackgroundAttributeString/Repeat.
  - $3F00‑$3F1F palette RAM: emits BackgroundSetPalette/SpriteSetPalette for aligned 4‑byte quads; unaligned writes currently throw for visibility during porting.
- Other ranges throw IllegalArgumentException to catch unexpected data.

Applying updates
- Each BufferedPpuUpdate implements operator fun invoke(ppu) to mutate ppu.backgroundTiles, backgroundPalettes/spritePalettes in a high‑level way suitable for tests and the software renderer. This bypasses PPUDATA auto‑increment intentionally in tests; when translating routines that write PPUDATA, you should still build the VRAM buffer bytes in RAM and let the parser apply them in tests.

Related tests and helpers
- See VramBufferParserTest and AttributeWrappingTest for examples of parsing/applying buffers. The PpuRenderer reads the mutated PPU state for visual verification.

### System container (System.kt)
System is a simple composition root bundling the emulator surfaces used by translations:
- Fields: ram: GameRam, ppu: PictureProcessingUnit, apu: AudioProcessingUnit, inputs: Inputs.
- There is no frame scheduler/stepper yet; translated routines should be written as pure functions that take (ram, ppu, apu, inputs) or System when convenient.
- Favor dependency injection in tests: construct a fresh System(), set necessary RAM fields, invoke your translated routine, then assert on RAM/PPU changes.

### What “translation” means here
The goal is to translate the intent and exact RAM effects of 6502 routines into Kotlin, not to write a 6502 interpreter. Each Kotlin routine:
- Keeps original assembly as comments inline (one or more asm lines per Kotlin statement where makes sense)
- Mutates the appropriate fields in `GameRam` (and other modeled components)
- Preserves data table contents (e.g., `.db`) and index logic
- Avoids invoking unimplemented PPU/APU/system routines unless explicitly required (those often `TODO()` at runtime)

### Where to implement translated code
- If the routine is part of startup or main flow, see `start.kt`, `nonMaskableInterrupt.kt`, `titleScreenMode.kt`
- Shared state lives in `GameRam.kt` (player fields, timers, collision, page locators, etc.)
- Bit‑and byte‑level helpers are in `utils/bitTypes.kt` (delegates `BitAccess2` and `BitRangeAccess2`)
- Data tables (e.g., music selection tables) can be constants near the routine or grouped with related logic; always mirror the `.db` layout

### Kotlin/6502 mapping guide
- 6502 registers `A`, `X`, `Y` are ephemeral. In translation, they usually become local variables or the values being written/read from `GameRam` fields. You do not emulate the registers unless necessary for clarity.
- Zero page variables (e.g., `OperMode`, `AreaType`) are modeled as properties in `GameRam`. Use and mutate them directly.
- Carry/Zero/Negative flags and branches (`bcs`, `bne`, etc.) become explicit Kotlin conditionals using masked integer logic.
- Addressing modes:
    - Direct `lda Variable` → read `ram.variable`
    - Indexed `lda Table,y` → read `table[index]` with masking
    - Indirect `sta ($06),y` → model the 16‑bit address built from two adjacent bytes at `$06/$07` plus `Y`. In Kotlin, compute an `Int` address and write to your chosen backing array (see “Modeling pointers/indirection”).

#### Byte and bit arithmetic in Kotlin
- Kotlin `Byte` is signed; always convert to `Int` for bitwise ops and shifts
    - Example: `(backing.value.toInt() and 0xFF)` to get the unsigned byte value
- Convert constants explicitly: `0x00.toByte()`, `0xFF.toByte()`
- Use the provided delegates in `utils/bitTypes.kt` (`BitAccess2`, `BitRangeAccess2`) to implement register bitfields against a `ByteAccess` backing

#### Modeling pointers/indirection
- When a routine uses zero‑page pointers like `($06),y`, decide the backing memory target:
    - If it points to a RAM page represented by an array in `GameRam`, compute: `val base = (ram.pointer06.toInt() and 0xFF) or ((ram.pointer07.toInt() and 0xFF) shl 8)`; then `val addr = base + (y.toInt() and 0xFF)` and index into the appropriate `ByteArray`
    - Keep address arithmetic in `Int`; mask indices to 0–65535 as needed
- If the target is PPU/OAM/VRAM, prefer calling stubbed methods following the API conventions (e.g., `writeOamAddress`) rather than creating property setters that will clash on JVM

### API naming to avoid JVM clashes
- Do not create `setXxx(T)` methods if there’s a `var xxx` property of that name. Prefer `writeXxx`, `updateXxx`, `loadXxx` for side‑effectful operations. Example: `writeOamAddress(address: Byte)` instead of `setOamAddress`.
- PPU’s VRAM address is `internalVramAddress: VramAddress` (typealias of `Short` declared in `nes/Inputs.kt`). The `readVram()/writeVram()` methods in `nes/PictureProcessingUnit.kt` already auto‑increment `internalVramAddress` by 32 if `control.drawVertical` is true, or by 1 otherwise; preserve this behavior if you touch these methods.
- `writeOamData(data: Byte)` in `PictureProcessingUnit` currently increments `oamAddress`; keep this side effect consistent with NES behavior.

### Data tables and constants
- `.db` sequences become Kotlin `byteArrayOf(...)` for raw bytes or typed arrays for domain values
- Keep ordering and values exact; mask when indexing with `Y`/`X`: `table[(y.toInt() and 0xFF)]`

Example (from the music selection area in `smbdism.asm`):
```kotlin
// MusicSelectData:
private val MusicSelectData = byteArrayOf(
    WaterMusic, GroundMusic, UndergroundMusic, CastleMusic,
    CloudMusic, PipeIntroMusic
)
```

### Example translation workflow (step‑by‑step)
Suppose you want to translate the `GetAreaMusic` routine around lines 2793–2811 in `smbdism.asm` (excerpt shown in your recent context):

1) Locate the routine in `smbdism.asm`
- Copy the label and immediate surrounding `.db` tables if they are only used here.

2) Identify RAM symbols used
- `OperMode`, `AltEntranceControl`, `PlayerEntranceCtrl`, `AreaType`, `CloudTypeOverride`, `AreaMusicQueue`
- Verify/introduce corresponding fields in `GameRam` if they exist; otherwise add appropriately (in this project, many already exist)

3) Create a Kotlin function in a relevant file (e.g., `start.kt` if it’s part of area init), and mirror logic with inline assembly comments
```kotlin
fun System.getAreaMusic() {
    //> GetAreaMusic:
    //> lda OperMode           ;if in title screen mode, leave
    //> beq ExitGetM
    if (ram.operMode == OperMode.TitleScreen) return

    //> lda AltEntranceControl ;check for specific alternate mode of entry
    //> cmp #$02               ;if found, branch without checking starting position
    //> beq ChkAreaType        ;from area object data header
    var indexY: Byte = 0
    if (ram.altEntranceControl != 0x02.toByte()) {
        //> ldy #$05               ;select music for pipe intro scene by default
        indexY = 0x05
        //> lda PlayerEntranceCtrl ;check value from level header for certain values
        //> cmp #$06
        //> beq StoreMusic         ;load music for pipe intro scene if header
        //> cmp #$07               ;start position either value $06 or $07
        //> beq StoreMusic
        val pec = ram.playerEntranceCtrl.toInt() and 0xFF
        if (pec == 0x06 || pec == 0x07) {
            // fall through to StoreMusic with indexY=5 (PipeIntro)
        } else {
            //> ChkAreaType: ldy AreaType           ;load area type as offset for music bit
            indexY = ram.areaType
            //> lda CloudTypeOverride
            //> beq StoreMusic         ;check for cloud type override
            //> ldy #$04               ;select music for cloud type level if found
            if ((ram.cloudTypeOverride.toInt() and 0xFF) != 0) indexY = 0x04
        }
    } else {
        // Using area type path directly
        //> ChkAreaType: ldy AreaType           ;load area type as offset for music bit
        indexY = ram.areaType
        //> lda CloudTypeOverride
        //> beq StoreMusic         ;check for cloud type override
        //> ldy #$04               ;select music for cloud type level if found
        if ((ram.cloudTypeOverride.toInt() and 0xFF) != 0) indexY = 0x04
    }

    //> StoreMusic:  lda MusicSelectData,y  ;otherwise select appropriate music for level type
    //> sta AreaMusicQueue     ;store in queue and leave
    val music: Byte = MusicSelectData[indexY.toInt()]
    ram.areaMusicQueue = music
    //> ExitGetM:    rts
}
```

4) Verify behavior with a focused unit test (see Testing section)

5) Keep comments one‑to‑one with assembly blocks so reviewers can compare easily

### Example: translating a state‑setup routine
Consider the `Entrance_GameTimerSetup` snippet in your context (lines ~2833–2848). A Kotlin translation might look like:
```kotlin
fun System.entranceGameTimerSetup() {
    //> Entrance_GameTimerSetup:
    //> lda ScreenLeft_PageLoc      ;set current page for area objects
    //> sta Player_PageLoc          ;as page location for player
    ram.playerPageLoc = ram.screenLeftPageLoc

    //> lda #$28                    ;store value here
    //> sta VerticalForceDown       ;for fractional movement downwards if necessary
    ram.verticalForceDown = 0x28

    //> lda #$01                    ;set high byte of player position and
    //> sta PlayerFacingDir         ;set facing direction so that player faces right
    //> sta Player_Y_HighPos
    ram.playerFacingDir = 0x01
    ram.playerYHighPos = 0x01

    //> lda #$00                    ;set player state to on the ground by default
    //> sta Player_State
    ram.playerState = 0x00

    //> dec Player_CollisionBits    ;initialize player's collision bits
    ram.playerCollisionBits--

    //> ldy #$00                    ;initialize halfway page
    //> sty HalfwayPage
    ram.halfwayPage = 0x00

    //> lda AreaType                ;check area type
    //> bne ChkStPos                ;if water type, set swimming flag, otherwise do not set
    //> iny
    //> ChkStPos: sty SwimmingFlag
    ram.swimmingFlag = ram.areaType == 0x0.toByte()
    
    //...
}
```
Fill in the remaining branches and side‑effects based on the subsequent assembly lines; keep all lines commented.

### Testing strategy for translated sections
- Test only pure Kotlin logic and RAM mutations that don’t require touching TODO‑backed emulator subsystems (PPU/APU/VRAM). If you must use a `ByteAccess`, create a tiny stub:
```kotlin
val backing = object : ByteAccess { override var value: Byte = 0 }
```
- Suggested test layout:
    - Package: `com.ivieleague.smbtranslation`
    - File under `src/test/kotlin`
    - Use `kotlin.test.*`

Example structure (adapt from the verified smoke tests):
```kotlin
class GetAreaMusicTest {
    @Test
    fun selectsPipeIntroForEntrance6or7() {
        val system = System()
        system.ram.OperMode = 1
        system.ram.AltEntranceControl = 0
        system.ram.PlayerEntranceCtrl = 0x06
        system.getAreaMusic()
        // Expect PipeIntroMusic at index 5
        assertEquals(MusicSelectData[5], ram.AreaMusicQueue)
    }
}
```
Run with: `./gradlew test` or in IDE via the gutter.

### Conventions and sharp edges
- Byte/int mixing: always mask bytes when comparing or indexing: `b.toInt() and 0xFF`
- Shifts: only shift `Int`s; convert back to `Byte` at writes
- Tables: when `.db` packs bitfields, consider `BitRangeAccess2`/`BitAccess2` to avoid manual masking at call sites
- Avoid busy‑wait loops or real‑time behavior in tests; emulator timing is not implemented yet
- Follow official Kotlin code style (enforced by `kotlin.code.style=official`)

### Working with PPU/APU/system placeholders
- Many functions are `TODO()` by design. If a routine interacts with them:
    - Keep the call in place but structure your code so tests can avoid executing that path (e.g., guard on state flags)
    - If needed for compile‑time references, define stubs that do not execute in tests

### Checklist for translating a routine
- [ ] Find the label in `smbdism.asm` and read surrounding comments/data tables
- [ ] List all referenced RAM variables, constants, and tables
- [ ] Verify/define corresponding fields in `GameRam` or appropriate component
- [ ] Decide where the routine fits (file/module) and create a Kotlin function
- [ ] Port logic line‑by‑line, preserving original comments next to Kotlin code
- [ ] Carefully handle byte math, flags, and branches using masked `Int`s
- [ ] Port `.db` tables as `byteArrayOf(...)` (or domain arrays) with exact values/order
- [ ] Add unit tests that exercise only pure logic and RAM mutations
- [ ] Confirm build/test pass with `./gradlew build` and `./gradlew test`

### Useful files during translation
- `smbdism.asm` — the authoritative reference for labels, tables, and semantics
- `GameRam.kt` — where most state lives; inspect before adding new fields
- `utils/bitTypes.kt` — delegates for bitfield access; use them to keep Kotlin idiomatic
- `start.kt`, `nonMaskableInterrupt.kt`, `titleScreenMode.kt` — exemplars of how routines are being ported and organized
- `build.gradle.kts` — build configuration and toolchain setup

### Final advice
- Keep diffs reviewable: small, self‑contained routine translations with tests
- Be consistent with naming and comment style; the 1:1 asm‑to‑comment mapping is your friend when auditing behavior
- When in doubt about 6502 behavior (carry/overflow on adds, page boundary effects), write a minimal test for the suspected edge and encode the expected RAM mutation

With this setup and workflow, you can confidently pick any well‑scoped section from `smbdism.asm`, translate it into Kotlin next to faithful comments, and validate it via unit tests without depending on unimplemented emulator surfaces.
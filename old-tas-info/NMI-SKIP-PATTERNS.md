# NMI Skip Patterns for SMB TAS Accuracy

## Summary

All NMI skips in SMB are associated with **Task 00→01 transitions**, which represent the `InitializeArea` routine. The number of skipped frames depends on the context.

## Complete Pattern Analysis

From analyzing 18,000 frames of FCEUX TAS data:

| Frame | Skips | State Changes | Operation |
|-------|-------|---------------|-----------|
| 0 | 2 | Area:ff→00, World:ff→00 | Reset/cold boot |
| 3 | 1 | (none) | Reset continued |
| 5 | 2 | Task:00→01, Area:00→01 | InitializeGame |
| 42 | 1 | Task:00→01 | InitializeArea (W1-1 start) |
| 612 | 1 | Task:00→01, Area:01→02 | Area transition (pipe) |
| 926 | 1 | Task:00→01, Area:02→01 | Area transition (exit pipe) |
| 1944 | 1 | Task:00→01 | InitializeArea (W1-2 via warp) |
| 2443 | 1 | Task:00→01 | InitializeArea (W1-3 via warp) |
| 3814 | 1 | Task:00→01, Area:02→01 | Area transition |
| 6042 | 1 | Task:00→01 | InitializeArea (W4-2) |
| 6541 | 1 | Task:00→01 | InitializeArea (W4-3) |
| 7220 | 1 | Task:00→01, Area:02→01 | Area transition |
| 7771 | 1 | Task:00→01 | InitializeArea |
| 10813 | 1 | Task:00→01 | InitializeArea |
| 12956 | 1 | Task:00→01 | InitializeArea |
| 15057 | 1 | Task:00→01 | InitializeArea |
| 15795 | 1 | Task:00→01 | InitializeArea |
| 16232 | 1 | Task:00→01 | InitializeArea |
| 16597 | 1 | Task:00→01, Area:03→00 | Area transition (castle→overworld) |
| 17467 | 1 | Task:00→01, Area:00→03 | Area transition (overworld→castle) |

## Key Observations

1. **All NMI skips occur during Task 00→01 transitions**
   - This corresponds to `InitializeArea` being called
   - Task 00 = waiting to initialize, Task 01 = area initialized

2. **Skip count varies by context:**
   - **Reset (frame 0-3):** 3 total NMIs skipped
   - **InitializeGame (frame 5):** 2 NMIs skipped
   - **InitializeArea (all others):** 1 NMI skipped each

3. **AreaType changes don't affect skip count**
   - Pipe transitions (01→02, 02→01) still skip 1 frame
   - Castle transitions (00→03, 03→00) still skip 1 frame

## Implementation for Decompiled Kotlin

### Option 1: State-Based Detection (Runtime)

The SMBRuntime can detect Task transitions and automatically skip NMIs:

```kotlin
class SMBRuntime {
    private var framesToSkip = 0
    private var prevTask: Int = 0
    private var prevMode: Int = 0
    private var isFirstFrame = true

    fun beforeNmi() {
        if (isFirstFrame) {
            // Reset: skip 3 frames
            framesToSkip = 3
            isFirstFrame = false
            return
        }

        val currentTask = operModeTask
        val currentMode = operMode

        // Detect Task 00→01 transition
        if (prevTask == 0 && currentTask == 1) {
            if (prevMode == 0 && currentMode == 0) {
                // InitializeGame: skip 2 frames
                framesToSkip = 2
            } else {
                // InitializeArea: skip 1 frame
                framesToSkip = 1
            }
        }

        prevTask = currentTask
        prevMode = currentMode
    }

    fun shouldFireNmi(): Boolean {
        if (framesToSkip > 0) {
            framesToSkip--
            return false
        }
        return true
    }
}
```

### Option 2: Explicit Markers in Code (Decompiler)

Insert `runtime.skipNmi(N)` calls in the decompiled code:

```kotlin
fun initializeGame() {
    // Clear memory $0000-$076F
    for (i in 0x0000..0x076F) memory[i] = 0
    // ... other initialization ...

    runtime.skipNmi(2)  // This operation takes 2 frames
}

fun initializeArea() {
    // Load area data
    // Clear screen buffers
    // ... area setup ...

    runtime.skipNmi(1)  // This operation takes 1 frame
}
```

### Option 3: Hybrid Approach

Use state detection for most cases, with explicit markers for edge cases:

```kotlin
class SMBRuntime {
    fun onTaskTransition(from: Int, to: Int) {
        if (from == 0 && to == 1) {
            // Auto-detect common case
            skipNmi(1)
        }
    }

    fun skipNmi(count: Int) {
        // Called explicitly for special cases
        framesToSkip += count
    }
}
```

## Correlation with SMB Code

The Task 00→01 transition happens in `OperModeExecutionTree`:

```kotlin
// From smbdism.asm lines 855-866
fun operModeExecutionTree() {
    when (operMode) {
        0 -> {  // Title screen mode
            when (operModeTask) {
                0 -> initializeGame()   // ← Takes 2+ frames
                1 -> screenRoutines()
                2 -> secondaryGameSetup()
                3 -> gameMenuRoutine()
            }
        }
        1 -> {  // Game mode
            when (operModeTask) {
                0 -> initializeArea()   // ← Takes 1+ frame
                1 -> screenRoutines()
                2 -> secondaryGameSetup()
                3 -> gameRoutines()
            }
        }
        // ...
    }
}
```

## Validation

After implementing frame skipping, verify:

1. **IntCtrl matches FCEUX** at key frames:
   - Frame 7: IntCtrl should be 0x14 (20 decimal)
   - Frame 43: IntCtrl should be 0x06

2. **RNG sequence matches** - the key test for correct NMI timing

3. **TAS inputs produce expected outcomes** at key milestones:
   - Frame 1944: W1-2 via warp
   - Frame 3814: W4-1 reached
   - Frame ~17000: Game completion

## Files

Analysis scripts in `local/tas/`:
- `analyze-nmi-skips.sh` - Detailed per-frame NMI analysis
- `summarize-nmi-skips.sh` - Pattern categorization
- `analyze-skip-patterns.sh` - Full TAS pattern extraction

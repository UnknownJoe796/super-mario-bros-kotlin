This project is a Kotlin translation of the original Super Mario Bros disassembly.  Its purpose is to teach how the original Super Mario Bros works by converting the ideas to a more modern language.

It is critical to this project that show the original assembly next to its equivalent Kotlin code.  The format we've decided for it is that comments in the format `//> <assembly>` are the assembly equivalent of the code below them.

You should also put effort into making the code as readable as possible, and make it clear how it matches up with the assembly.

A good example:

```kotlin
private fun System.chkContinue(joypadBits: JoypadBits) {
    //> ChkContinue:  ldy DemoTimer               ;if timer for demo has expired, reset modes
    //> beq ResetTitle
    if(ram.demoTimer == 0.toByte()) return resetTitle()
    //> asl                         ;check to see if A button was also pushed
    //> bcc StartWorld1             ;if not, don't load continue function's world number
    if(joypadBits.a) {
        //> lda ContinueWorld           ;load previously saved world number for secret
        //> jsr GoContinue              ;continue function when pressing A + start
        goContinue(ram.continueWorld)
    }
    //> StartWorld1:  jsr LoadAreaPointer
    loadAreaPointer()
    //> inc Hidden1UpFlag           ;set 1-up box flag for both players
    ram.hidden1UpFlag = true
    //> inc OffScr_Hidden1UpFlag
    ram.offScrHidden1UpFlag = true
    //> inc FetchNewGameTimerFlag   ;set fetch new game timer flag
    ram.fetchNewGameTimerFlag = true
    //> inc OperMode                ;set next game mode
    ram.operMode = OperMode.Game
    //> lda WorldSelectEnableFlag   ;if world select flag is on, then primary
    //> sta PrimaryHardMode         ;hard mode must be on as well
    ram.primaryHardMode = ram.worldSelectEnableFlag
    //> lda #$00
    //> sta OperMode_Task           ;set game mode here, and clear demo timer
    ram.operModeTask = 0x00.toByte()
    //> sta DemoTimer
    ram.demoTimer = 0x00.toByte()
    //> ldx #$17
    //> lda #$00
    //> InitScores:   sta ScoreAndCoinDisplay,x   ;clear player scores and coin displays
    //> dex
    //> bpl InitScores
    ram.playerScoreDisplay.zeros()
    ram.player2ScoreDisplay.zeros()
    ram.coinDisplay.zeros()
    ram.coin2Display.zeros()
    //> ExitMenu:     rts
}
```

Notes on the project at the moment:

- The PPU uses a very high-level abstraction not operating on bytes.  You need to use it if interacting with the PPU.

Dos and don'ts:

- DO use modern looping and control constructs to represent the logic.
- DON'T choose variable names based on the 6502 registers; instead, pick a name that accurately describes what the variable is used for.
- DO declare new variables even representing the same register if they are used in a different context.
- DON'T use raw byte arrays for inline data.  Instead...
- DO define blocks of data as data class instances and lists of data class instances, using that organization to better describe what they mean.  Define new data classes if needed to describe the data.

---

Translate the following assembly section:


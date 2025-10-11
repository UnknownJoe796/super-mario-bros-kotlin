package com.ivieleague.smbtranslation

import com.ivieleague.smbtranslation.utils.*

fun System.operModeExecutionTree(): Unit {
    //> OperModeExecutionTree:
    //> lda OperMode     ;this is the heart of the entire program,
    //> jsr JumpEngine   ;most of what goes on starts here
    //>
    //> .dw TitleScreenMode
    //> .dw GameMode
    //> .dw VictoryMode
    //> .dw GameOverMode
    when(ram.operMode) {
        OperMode.TitleScreen -> titleScreenMode()
        OperMode.Game -> gameMode()
        OperMode.Victory -> victoryMode()
        OperMode.GameOver -> gameOverMode()
    }
}

fun System.gameMode(): Unit = TODO()
fun System.gameOverMode(): Unit {
    //> GameOverMode:
    //> lda OperMode_Task
    //> jsr JumpEngine

    when(ram.operModeTask.toInt()) {
        //> .dw SetupGameOver
        0 -> gameOverSetup()
        //> .dw ScreenRoutines
        1 -> screenRoutines()
        //> .dw RunGameOver
        2 -> runGameOver()
    }
}

private fun System.gameOverSetup(): Unit {
    //> SetupGameOver:
    //> lda #$00                  ;reset screen routine task control for title screen, game,
    //> sta ScreenRoutineTask     ;and game over modes
    ram.screenRoutineTask = 0
    //> sta Sprite0HitDetectFlag  ;disable sprite 0 check
    ram.sprite0HitDetectFlag = false
    //> lda #GameOverMusic
    //> sta EventMusicQueue       ;put game over music in secondary queue
    ram.eventMusicQueue = Constants.GameOverMusic
    //> inc DisableScreenFlag     ;disable screen output
    ram.disableScreenFlag = true
    //> inc OperMode_Task         ;set secondary mode to 1
    ram.operModeTask++
    //> rts
}
private fun System.runGameOver(): Unit {
    //> RunGameOver:
    //> lda #$00              ;reenable screen
    //> sta DisableScreenFlag
    ram.disableScreenFlag = false
    //> lda SavedJoypad1Bits  ;check controller for start pressed
    //> and #Start_Button
    //> bne TerminateGame
    //> lda ScreenTimer       ;if not pressed, wait for
    //> bne GameIsOn          ;screen timer to expire
    if (!ram.savedJoypad1Bits.start && ram.screenTimer != 0.toByte()) return
    //> TerminateGame:
    //> lda #Silence          ;silence music
    //> sta EventMusicQueue
    ram.eventMusicQueue = Constants.Silence
    //> jsr TransposePlayers  ;check if other player can keep
    //> bcc ContinueGame      ;going, and do so if possible
    if (!transposePlayers()) return continueGame()
    //> lda WorldNumber       ;otherwise put world number of current
    //> sta ContinueWorld     ;player into secret continue function variable
    ram.continueWorld = ram.worldNumber
    //> lda #$00
    //> asl                   ;residual ASL instruction
    //> sta OperMode_Task     ;reset all modes to title screen and
    ram.operModeTask = 0.toByte()
    //> sta ScreenTimer       ;leave
    ram.screenTimer = 0.toByte()
    //> sta OperMode
    ram.operMode = OperMode.TitleScreen
    //> rts
}
fun System.continueGame() {
    //> ContinueGame:
    //> jsr LoadAreaPointer       ;update level pointer with
    loadAreaPointer()
    //> lda #$01                  ;actual world and area numbers, then
    //> sta PlayerSize            ;reset player's size, status, and
    ram.playerSize = 0x01.toByte()
    //> inc FetchNewGameTimerFlag ;set game timer flag to reload
    ram.fetchNewGameTimerFlag = true
    //> lda #$00                  ;game timer from header
    //> sta TimerControl          ;also set flag for timers to count again
    ram.timerControl = 0x00.toByte()
    //> sta PlayerStatus
    ram.playerStatus = 0x00.toByte()
    //> sta GameEngineSubroutine  ;reset task for game core
    ram.gameEngineSubroutine = 0x00.toByte()
    //> sta OperMode_Task         ;set modes and leave
    ram.operModeTask = 0x00.toByte()
    //> lda #$01                  ;if in game over mode, switch back to
    //> sta OperMode              ;game mode, because game is still on
    ram.operMode = OperMode.Game
    //> GameIsOn:  rts
}
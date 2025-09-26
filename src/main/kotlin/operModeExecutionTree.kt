package com.ivieleague.smbtranslation


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
fun System.victoryMode(): Unit = TODO()
fun System.gameOverMode(): Unit = TODO()
package com.example.gymstruck.domain

sealed interface TimerEvent {
    data class Start(val config: WorkoutConfig) : TimerEvent
    data object CompleteSet : TimerEvent
    data object RestFinished : TimerEvent
    data object Pause : TimerEvent
    data object Resume : TimerEvent
    data object Reset : TimerEvent
}

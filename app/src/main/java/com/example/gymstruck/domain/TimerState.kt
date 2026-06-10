package com.example.gymstruck.domain

sealed interface TimerState {
    data object Idle : TimerState
    data class Working(val setIndex: Int, val config: WorkoutConfig) : TimerState
    data class Resting(val setIndex: Int, val endAtElapsed: Long, val config: WorkoutConfig) : TimerState
    data class Paused(val resumeTo: TimerState, val remainingMillis: Long?) : TimerState
    data class Completed(val totalSets: Int) : TimerState
}

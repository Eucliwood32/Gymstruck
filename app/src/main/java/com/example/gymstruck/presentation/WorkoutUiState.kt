package com.example.gymstruck.presentation

import com.example.gymstruck.domain.RestMode
import com.example.gymstruck.domain.TimerState

enum class Phase { SETUP, WORK, REST, PAUSED_FROM_WORK, PAUSED_FROM_REST, DONE }

data class WorkoutUiState(
    val phase: Phase,
    val setIndex: Int,
    val totalSets: Int,
    val totalRestMillis: Long,
    val isActive: Boolean,
    val restMode: RestMode,
) {
    companion object {
        val Initial = WorkoutUiState(
            phase = Phase.SETUP,
            setIndex = 0,
            totalSets = 0,
            totalRestMillis = 0L,
            isActive = false,
            restMode = RestMode.DUCKING,
        )
    }
}

fun TimerState.toUiState(): WorkoutUiState = when (this) {
    is TimerState.Idle -> WorkoutUiState.Initial

    is TimerState.Working -> WorkoutUiState(
        phase = Phase.WORK,
        setIndex = setIndex,
        totalSets = config.totalSets,
        totalRestMillis = config.restMillis,
        isActive = true,
        restMode = config.restMode,
    )

    is TimerState.Resting -> WorkoutUiState(
        phase = Phase.REST,
        setIndex = setIndex,
        totalSets = config.totalSets,
        totalRestMillis = config.restMillis,
        isActive = true,
        restMode = config.restMode,
    )

    is TimerState.Paused -> {
        val phase = when (resumeTo) {
            is TimerState.Working -> Phase.PAUSED_FROM_WORK
            is TimerState.Resting -> Phase.PAUSED_FROM_REST
            else -> Phase.SETUP
        }
        val config = when (val rt = resumeTo) {
            is TimerState.Working -> rt.config
            is TimerState.Resting -> rt.config
            else -> null
        }
        val (idx, total, restMs) = when (val rt = resumeTo) {
            is TimerState.Working -> Triple(rt.setIndex, rt.config.totalSets, rt.config.restMillis)
            is TimerState.Resting -> Triple(rt.setIndex, rt.config.totalSets, rt.config.restMillis)
            else -> Triple(0, 0, 0L)
        }
        WorkoutUiState(
            phase = phase,
            setIndex = idx,
            totalSets = total,
            totalRestMillis = restMs,
            isActive = false,
            restMode = config?.restMode ?: RestMode.DUCKING,
        )
    }

    is TimerState.Completed -> WorkoutUiState(
        phase = Phase.DONE,
        setIndex = totalSets,
        totalSets = totalSets,
        totalRestMillis = 0L,
        isActive = false,
        restMode = RestMode.DUCKING,
    )
}

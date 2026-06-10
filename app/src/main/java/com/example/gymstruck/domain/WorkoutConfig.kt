package com.example.gymstruck.domain

data class WorkoutConfig(
    val totalSets: Int,
    val restMillis: Long,
    val restMode: RestMode = RestMode.DUCKING,
    val searchQuery: String = "",
)

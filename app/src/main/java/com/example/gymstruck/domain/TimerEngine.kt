package com.example.gymstruck.domain

class TimerEngine(private val now: () -> Long) {

    fun reduce(state: TimerState, event: TimerEvent): Pair<TimerState, List<SideEffect>> = when {
        event is TimerEvent.Start ->
            // 첫 세트도 Working 진입이므로 휴식 종료와 동일한 오디오 효과 적용
            // (DUCKING: 운동 중 음악 낮춤 시작 / SHORTS: 음악 정상)
            TimerState.Working(setIndex = 1, config = event.config) to
                listOf(SideEffect.KeepScreenOn, restExitAudio(event.config.restMode))

        state is TimerState.Working && event is TimerEvent.CompleteSet ->
            if (state.setIndex >= state.config.totalSets)
                // 마지막 세트: 더킹 종료 + 화면 유지 해제
                TimerState.Completed(state.config.totalSets) to listOf(SideEffect.StopDucking, SideEffect.ReleaseScreen)
            else
                // 세트 완료 → 휴식 진입
                // DUCKING: 더킹 종료(음악 복귀) / SHORTS: 더킹 시작(음악 낮춰 Short 소리 또렷이)
                TimerState.Resting(state.setIndex, now() + state.config.restMillis, state.config) to
                    listOf(restEnterAudio(state.config.restMode))

        state is TimerState.Resting && event is TimerEvent.RestFinished ->
            // 휴식 종료 → Working 진입
            // DUCKING: 더킹 시작(운동 중 음악 낮춤) / SHORTS: 더킹 종료(운동 세트 동안 음악 복귀)
            TimerState.Working(state.setIndex + 1, state.config) to
                listOf(restExitAudio(state.config.restMode))

        (state is TimerState.Working || state is TimerState.Resting) && event is TimerEvent.Pause -> {
            val remaining = if (state is TimerState.Resting) state.endAtElapsed - now() else null
            TimerState.Paused(resumeTo = state, remainingMillis = remaining) to emptyList()
        }

        state is TimerState.Paused && event is TimerEvent.Resume -> {
            val next = when (val rt = state.resumeTo) {
                is TimerState.Resting -> {
                    val newEnd = now() + (state.remainingMillis?.coerceAtLeast(0L) ?: 0L)
                    rt.copy(endAtElapsed = newEnd)
                }
                else -> rt
            }
            next to emptyList()
        }

        event is TimerEvent.Reset ->
            TimerState.Idle to listOf(SideEffect.StopDucking, SideEffect.ReleaseScreen)

        else -> state to emptyList()
    }

    // 휴식 진입 시 오디오 효과
    private fun restEnterAudio(mode: RestMode): SideEffect = when (mode) {
        RestMode.DUCKING -> SideEffect.StopDucking
        RestMode.SHORTS -> SideEffect.StartDucking
    }

    // 휴식 종료(운동 진입) 시 오디오 효과
    private fun restExitAudio(mode: RestMode): SideEffect = when (mode) {
        RestMode.DUCKING -> SideEffect.StartDucking
        RestMode.SHORTS -> SideEffect.StopDucking
    }
}

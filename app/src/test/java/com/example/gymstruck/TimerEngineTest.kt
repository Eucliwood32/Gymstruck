package com.example.gymstruck

import com.example.gymstruck.domain.RestMode
import com.example.gymstruck.domain.SideEffect
import com.example.gymstruck.domain.TimerEngine
import com.example.gymstruck.domain.TimerEvent
import com.example.gymstruck.domain.TimerState
import com.example.gymstruck.domain.WorkoutConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerEngineTest {

    private var fakeNow = 0L
    private val engine = TimerEngine(now = { fakeNow })
    private val config2Sets = WorkoutConfig(totalSets = 2, restMillis = 60_000L)

    // ── 기본 전이 ──────────────────────────────────────────────────────────────

    @Test
    fun `Start → Working 세트 1, KeepScreenOn 발행`() {
        val (next, effects) = engine.reduce(TimerState.Idle, TimerEvent.Start(config2Sets))
        assertTrue(next is TimerState.Working)
        assertEquals(1, (next as TimerState.Working).setIndex)
        assertTrue(effects.contains(SideEffect.KeepScreenOn))
    }

    @Test
    fun `DUCKING 모드 Start → 첫 세트부터 StartDucking 발행 (회귀)`() {
        // 버그: 첫 세트에서 더킹이 안 걸리던 문제 — Start도 Working 진입이므로 더킹 시작해야 함
        val (_, effects) = engine.reduce(TimerState.Idle, TimerEvent.Start(config2Sets))
        assertTrue(effects.contains(SideEffect.StartDucking))
    }

    @Test
    fun `SHORTS 모드 Start → StartDucking 발행하지 않음`() {
        val config = config2Sets.copy(restMode = RestMode.SHORTS)
        val (_, effects) = engine.reduce(TimerState.Idle, TimerEvent.Start(config))
        assertTrue(!effects.contains(SideEffect.StartDucking))
    }

    @Test
    fun `Working 세트 1 CompleteSet → Resting, endAt 올바르게 설정, StopDucking 발행`() {
        fakeNow = 1_000L
        val working = TimerState.Working(setIndex = 1, config = config2Sets)
        val (next, effects) = engine.reduce(working, TimerEvent.CompleteSet)
        assertTrue(next is TimerState.Resting)
        assertEquals(1_000L + 60_000L, (next as TimerState.Resting).endAtElapsed)
        assertTrue(effects.contains(SideEffect.StopDucking))
    }

    @Test
    fun `Resting RestFinished → Working 다음 세트, StartDucking 발행`() {
        val resting = TimerState.Resting(setIndex = 1, endAtElapsed = 61_000L, config = config2Sets)
        val (next, effects) = engine.reduce(resting, TimerEvent.RestFinished)
        assertTrue(next is TimerState.Working)
        assertEquals(2, (next as TimerState.Working).setIndex)
        assertTrue(effects.contains(SideEffect.StartDucking))
    }

    @Test
    fun `마지막 세트 CompleteSet → Completed, StopDucking + ReleaseScreen 발행`() {
        val working = TimerState.Working(setIndex = 2, config = config2Sets)
        val (next, effects) = engine.reduce(working, TimerEvent.CompleteSet)
        assertTrue(next is TimerState.Completed)
        assertEquals(2, (next as TimerState.Completed).totalSets)
        assertTrue(effects.contains(SideEffect.StopDucking))
        assertTrue(effects.contains(SideEffect.ReleaseScreen))
    }

    // ── SHORTS 모드 오디오 분기 ─────────────────────────────────────────────────

    private val configShorts = WorkoutConfig(
        totalSets = 2,
        restMillis = 60_000L,
        restMode = RestMode.SHORTS,
        searchQuery = "운동",
    )

    @Test
    fun `SHORTS 모드 CompleteSet → Resting 진입 시 StartDucking 발행`() {
        val working = TimerState.Working(setIndex = 1, config = configShorts)
        val (next, effects) = engine.reduce(working, TimerEvent.CompleteSet)
        assertTrue(next is TimerState.Resting)
        assertTrue(effects.contains(SideEffect.StartDucking))
    }

    @Test
    fun `SHORTS 모드 RestFinished → Working 진입 시 StopDucking 발행`() {
        val resting = TimerState.Resting(setIndex = 1, endAtElapsed = 61_000L, config = configShorts)
        val (next, effects) = engine.reduce(resting, TimerEvent.RestFinished)
        assertTrue(next is TimerState.Working)
        assertTrue(effects.contains(SideEffect.StopDucking))
    }

    // ── 일시정지 / 재개 ────────────────────────────────────────────────────────

    @Test
    fun `Working Pause → Paused, remainingMillis null`() {
        val working = TimerState.Working(setIndex = 1, config = config2Sets)
        val (next, effects) = engine.reduce(working, TimerEvent.Pause)
        assertTrue(next is TimerState.Paused)
        assertNull((next as TimerState.Paused).remainingMillis)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `Resting Pause → Paused, remainingMillis 정확히 계산`() {
        fakeNow = 10_000L
        val endAt = 70_000L
        val resting = TimerState.Resting(setIndex = 1, endAtElapsed = endAt, config = config2Sets)
        val (next, _) = engine.reduce(resting, TimerEvent.Pause)
        assertTrue(next is TimerState.Paused)
        assertEquals(endAt - fakeNow, (next as TimerState.Paused).remainingMillis)
    }

    @Test
    fun `Paused(from Working) Resume → 원래 Working 상태로 복원`() {
        val working = TimerState.Working(setIndex = 1, config = config2Sets)
        val paused = TimerState.Paused(resumeTo = working, remainingMillis = null)
        val (next, _) = engine.reduce(paused, TimerEvent.Resume)
        assertEquals(working, next)
    }

    @Test
    fun `Paused(from Resting) Resume → endAt이 남은시간 기준으로 재설정`() {
        fakeNow = 20_000L
        val remaining = 45_000L
        val resting = TimerState.Resting(setIndex = 1, endAtElapsed = 0L, config = config2Sets)
        val paused = TimerState.Paused(resumeTo = resting, remainingMillis = remaining)
        val (next, _) = engine.reduce(paused, TimerEvent.Resume)
        assertTrue(next is TimerState.Resting)
        assertEquals(fakeNow + remaining, (next as TimerState.Resting).endAtElapsed)
    }

    // ── 초기화 ────────────────────────────────────────────────────────────────

    @Test
    fun `어느 상태에서든 Reset → Idle, StopDucking + ReleaseScreen 발행`() {
        listOf(
            TimerState.Working(1, config2Sets),
            TimerState.Resting(1, 999L, config2Sets),
            TimerState.Completed(2),
        ).forEach { state ->
            val (next, effects) = engine.reduce(state, TimerEvent.Reset)
            assertTrue("$state 에서 Reset이 Idle로 가야 함", next is TimerState.Idle)
            assertTrue(effects.contains(SideEffect.StopDucking))
            assertTrue(effects.contains(SideEffect.ReleaseScreen))
        }
    }

    // ── 세트 루프 전체 ────────────────────────────────────────────────────────

    @Test
    fun `3세트 전체 루프 정상 완료`() {
        val config = WorkoutConfig(totalSets = 3, restMillis = 30_000L)
        var state: TimerState = TimerState.Idle
        fakeNow = 0L

        state = engine.reduce(state, TimerEvent.Start(config)).first
        repeat(3) { i ->
            assertTrue("세트 ${i + 1} Working 상태여야 함", state is TimerState.Working)
            state = engine.reduce(state, TimerEvent.CompleteSet).first
            if (i < 2) {
                assertTrue("세트 ${i + 1} 후 Resting이어야 함", state is TimerState.Resting)
                state = engine.reduce(state, TimerEvent.RestFinished).first
            }
        }
        assertTrue("3세트 완료 후 Completed이어야 함", state is TimerState.Completed)
    }
}

package com.example.gymstruck.presentation

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymstruck.data.PresetRepository
import com.example.gymstruck.data.YouTubeSearchService
import com.example.gymstruck.domain.RestMode
import com.example.gymstruck.domain.ShortVideo
import com.example.gymstruck.domain.SideEffect
import com.example.gymstruck.domain.TimerEngine
import com.example.gymstruck.domain.TimerEvent
import com.example.gymstruck.domain.TimerState
import com.example.gymstruck.domain.WorkoutConfig
import com.example.gymstruck.platform.AlarmScheduler
import com.example.gymstruck.platform.AudioCueController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class WorkoutViewModel(
    private val engine: TimerEngine,
    private val audioCue: AudioCueController,
    private val alarmScheduler: AlarmScheduler,
    private val presetRepository: PresetRepository,
    private val youTubeSearchService: YouTubeSearchService,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private val _timerState = MutableStateFlow<TimerState>(TimerState.Idle)

    val uiState: StateFlow<WorkoutUiState> =
        _timerState.map { it.toUiState() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutUiState.Initial)

    private val _remainingMillis = MutableStateFlow(0L)
    val remainingMillis: StateFlow<Long> = _remainingMillis.asStateFlow()

    val lastPreset: StateFlow<WorkoutConfig> = presetRepository.preset
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutConfig(3, 90_000L))

    // 휴식 중 재생할 Shorts 피드 (운동 시작 시 fetch)
    private val _shorts = MutableStateFlow<List<ShortVideo>>(emptyList())
    val shorts: StateFlow<List<ShortVideo>> = _shorts.asStateFlow()

    // Setup 화면 검색 미리보기 결과
    private val _searchPreview = MutableStateFlow<SearchPreview>(SearchPreview.Idle)
    val searchPreview: StateFlow<SearchPreview> = _searchPreview.asStateFlow()

    private var ticker: Job? = null
    private var searchJob: Job? = null

    init {
        restoreState()
    }

    fun onEvent(event: TimerEvent) {
        val (next, effects) = engine.reduce(_timerState.value, event)

        if (event is TimerEvent.Start) {
            viewModelScope.launch { presetRepository.save(event.config) }
            fetchShortsFeed(event.config)
        }

        _timerState.value = next
        effects.forEach { execute(it) }

        if (next is TimerState.Resting) {
            startRestTicker(next)
            alarmScheduler.scheduleRestEnd(next.endAtElapsed)
        } else {
            ticker?.cancel()
            ticker = null
            alarmScheduler.cancel()
        }

        persist(next)
    }

    private fun startRestTicker(resting: TimerState.Resting) {
        ticker?.cancel()
        _remainingMillis.value = resting.config.restMillis
        ticker = viewModelScope.launch {
            while (isActive) {
                val remaining = resting.endAtElapsed - SystemClock.elapsedRealtime()
                if (remaining <= 0L) {
                    _remainingMillis.value = 0L
                    onEvent(TimerEvent.RestFinished)
                    break
                }
                _remainingMillis.value = remaining
                delay(50)
            }
        }
    }

    // ── Shorts 검색 ──────────────────────────────────────────────────────────

    /** 운동 시작/복원 시 휴식용 Shorts 피드를 미리 가져온다. */
    private fun fetchShortsFeed(config: WorkoutConfig) {
        if (config.restMode != RestMode.SHORTS || config.searchQuery.isBlank()) {
            _shorts.value = emptyList()
            return
        }
        // 미리보기에서 같은 검색어로 이미 받아둔 결과가 있으면 재사용
        val preview = _searchPreview.value
        if (preview is SearchPreview.Success && preview.query == config.searchQuery && preview.videos.isNotEmpty()) {
            _shorts.value = preview.videos
            return
        }
        viewModelScope.launch {
            youTubeSearchService.searchShorts(config.searchQuery)
                .onSuccess { _shorts.value = it }
                .onFailure { _shorts.value = emptyList() }
        }
    }

    /** Setup 화면에서 검색 버튼을 눌렀을 때 호출. 미리보기 결과를 노출한다. */
    fun previewSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            _searchPreview.value = SearchPreview.Idle
            return
        }
        searchJob?.cancel()
        _searchPreview.value = SearchPreview.Loading
        searchJob = viewModelScope.launch {
            youTubeSearchService.searchShorts(trimmed)
                .onSuccess { _searchPreview.value = SearchPreview.Success(trimmed, it) }
                .onFailure { _searchPreview.value = SearchPreview.Error(it.message ?: "검색에 실패했습니다.") }
        }
    }

    private fun execute(effect: SideEffect) {
        when (effect) {
            SideEffect.StartDucking -> audioCue.startDucking()
            SideEffect.StopDucking -> audioCue.stopDucking()
            SideEffect.KeepScreenOn, SideEffect.ReleaseScreen -> {
                // uiState.isActive 변화로 Compose의 KeepScreenOn Composable이 처리
            }
        }
    }

    // ── 상태 복원 ─────────────────────────────────────────────────────────────

    private fun restoreState() {
        val stateType = savedState.get<String>(KEY_STATE_TYPE) ?: return
        val setIndex  = savedState.get<Int>(KEY_SET_INDEX)    ?: 0
        val totalSets = savedState.get<Int>(KEY_TOTAL_SETS)   ?: return
        val restMillis = savedState.get<Long>(KEY_REST_MILLIS) ?: return
        val restMode = savedState.get<String>(KEY_REST_MODE)
            ?.let { runCatching { RestMode.valueOf(it) }.getOrNull() }
            ?: RestMode.DUCKING
        val searchQuery = savedState.get<String>(KEY_SEARCH_QUERY) ?: ""
        val config = WorkoutConfig(totalSets, restMillis, restMode, searchQuery)

        val restored: TimerState = when (stateType) {
            STATE_WORKING -> TimerState.Working(setIndex, config)
            STATE_RESTING -> {
                val endAt = savedState.get<Long>(KEY_END_AT_ELAPSED) ?: return
                TimerState.Resting(setIndex, endAt, config)
            }
            STATE_PAUSED_WORK -> TimerState.Paused(
                resumeTo = TimerState.Working(setIndex, config),
                remainingMillis = null,
            )
            STATE_PAUSED_REST -> {
                val endAt     = savedState.get<Long>(KEY_END_AT_ELAPSED)  ?: return
                val remaining = savedState.get<Long>(KEY_REMAINING_MILLIS) ?: return
                TimerState.Paused(
                    resumeTo = TimerState.Resting(setIndex, endAt, config),
                    remainingMillis = remaining,
                )
            }
            STATE_COMPLETED -> TimerState.Completed(totalSets)
            else -> return
        }

        _timerState.value = restored

        // 휴식 중/일시정지 상태로 복원되면 Shorts 피드도 다시 가져온다.
        if (config.restMode == RestMode.SHORTS) {
            fetchShortsFeed(config)
        }

        // 프로세스 종료로 오디오 포커스가 해제됐으므로 복원 상태에 맞게 더킹을 다시 건다.
        // (DUCKING: 운동 중 음악 낮춤 / SHORTS: 휴식 중 음악 낮춤)
        val shouldDuck = (restored is TimerState.Working && config.restMode == RestMode.DUCKING) ||
            (restored is TimerState.Resting && config.restMode == RestMode.SHORTS)
        if (shouldDuck) audioCue.startDucking()

        if (restored is TimerState.Resting) {
            val remaining = restored.endAtElapsed - SystemClock.elapsedRealtime()
            if (remaining <= 0L) {
                val (next, effects) = engine.reduce(restored, TimerEvent.RestFinished)
                _timerState.value = next
                effects.forEach { execute(it) }
                persist(next)
            } else {
                startRestTicker(restored)
                alarmScheduler.scheduleRestEnd(restored.endAtElapsed)
            }
        }
    }

    private fun persist(state: TimerState) {
        when (state) {
            is TimerState.Idle -> {
                savedState[KEY_STATE_TYPE] = STATE_IDLE
            }
            is TimerState.Working -> {
                savedState[KEY_STATE_TYPE]  = STATE_WORKING
                savedState[KEY_SET_INDEX]   = state.setIndex
                savedState[KEY_TOTAL_SETS]  = state.config.totalSets
                savedState[KEY_REST_MILLIS] = state.config.restMillis
                persistConfigExtras(state.config)
            }
            is TimerState.Resting -> {
                savedState[KEY_STATE_TYPE]     = STATE_RESTING
                savedState[KEY_SET_INDEX]      = state.setIndex
                savedState[KEY_TOTAL_SETS]     = state.config.totalSets
                savedState[KEY_REST_MILLIS]    = state.config.restMillis
                savedState[KEY_END_AT_ELAPSED] = state.endAtElapsed
                persistConfigExtras(state.config)
            }
            is TimerState.Paused -> when (val rt = state.resumeTo) {
                is TimerState.Working -> {
                    savedState[KEY_STATE_TYPE]  = STATE_PAUSED_WORK
                    savedState[KEY_SET_INDEX]   = rt.setIndex
                    savedState[KEY_TOTAL_SETS]  = rt.config.totalSets
                    savedState[KEY_REST_MILLIS] = rt.config.restMillis
                    persistConfigExtras(rt.config)
                }
                is TimerState.Resting -> {
                    savedState[KEY_STATE_TYPE]      = STATE_PAUSED_REST
                    savedState[KEY_SET_INDEX]       = rt.setIndex
                    savedState[KEY_TOTAL_SETS]      = rt.config.totalSets
                    savedState[KEY_REST_MILLIS]     = rt.config.restMillis
                    savedState[KEY_END_AT_ELAPSED]  = rt.endAtElapsed
                    savedState[KEY_REMAINING_MILLIS] = state.remainingMillis ?: 0L
                    persistConfigExtras(rt.config)
                }
                else -> savedState[KEY_STATE_TYPE] = STATE_IDLE
            }
            is TimerState.Completed -> {
                savedState[KEY_STATE_TYPE] = STATE_COMPLETED
                savedState[KEY_TOTAL_SETS] = state.totalSets
                savedState[KEY_SET_INDEX]  = state.totalSets
            }
        }
    }

    private fun persistConfigExtras(config: WorkoutConfig) {
        savedState[KEY_REST_MODE]    = config.restMode.name
        savedState[KEY_SEARCH_QUERY] = config.searchQuery
    }

    override fun onCleared() {
        super.onCleared()
        audioCue.release()
        ticker?.cancel()
        searchJob?.cancel()
    }

    companion object {
        private const val KEY_STATE_TYPE       = "state_type"
        private const val KEY_SET_INDEX        = "set_index"
        private const val KEY_TOTAL_SETS       = "total_sets"
        private const val KEY_REST_MILLIS      = "rest_millis"
        private const val KEY_END_AT_ELAPSED   = "end_at_elapsed"
        private const val KEY_REMAINING_MILLIS = "remaining_millis"
        private const val KEY_REST_MODE        = "rest_mode"
        private const val KEY_SEARCH_QUERY     = "search_query"

        private const val STATE_IDLE        = "IDLE"
        private const val STATE_WORKING     = "WORKING"
        private const val STATE_RESTING     = "RESTING"
        private const val STATE_PAUSED_WORK = "PAUSED_WORK"
        private const val STATE_PAUSED_REST = "PAUSED_REST"
        private const val STATE_COMPLETED   = "COMPLETED"
    }
}

/** Setup 화면 검색 미리보기 상태. */
sealed interface SearchPreview {
    data object Idle : SearchPreview
    data object Loading : SearchPreview
    data class Success(val query: String, val videos: List<ShortVideo>) : SearchPreview
    data class Error(val message: String) : SearchPreview
}

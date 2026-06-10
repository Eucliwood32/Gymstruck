package com.example.gymstruck.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymstruck.di.AppContainer
import com.example.gymstruck.domain.TimerEvent
import com.example.gymstruck.presentation.Phase
import com.example.gymstruck.presentation.WorkoutViewModel
import com.example.gymstruck.ui.theme.GymstruckTheme

@Composable
fun WorkoutRoute(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val container = remember(context) { AppContainer.getInstance(context) }
    val vm: WorkoutViewModel = viewModel {
        WorkoutViewModel(
            engine = container.engine,
            audioCue = container.audioController,
            alarmScheduler = container.alarmScheduler,
            presetRepository = container.presetRepository,
            youTubeSearchService = container.youTubeSearchService,
            savedState = createSavedStateHandle(),
        )
    }

    val ui by vm.uiState.collectAsStateWithLifecycle()
    val remaining by vm.remainingMillis.collectAsStateWithLifecycle()
    val lastPreset by vm.lastPreset.collectAsStateWithLifecycle()
    val shorts by vm.shorts.collectAsStateWithLifecycle()
    val searchPreview by vm.searchPreview.collectAsStateWithLifecycle()

    val isRestPhase = ui.phase == Phase.REST || ui.phase == Phase.PAUSED_FROM_REST

    KeepScreenOn(enabled = ui.isActive)

    GymstruckTheme(darkTheme = isRestPhase) {
        Box(modifier = modifier.fillMaxSize()) {
            when (ui.phase) {
                Phase.SETUP -> SetupScreen(
                    lastPreset = lastPreset,
                    searchPreview = searchPreview,
                    onSearch = { vm.previewSearch(it) },
                    onStart = { vm.onEvent(TimerEvent.Start(it)) },
                )
                Phase.WORK -> WorkScreen(
                    ui = ui,
                    onCompleteSet = { vm.onEvent(TimerEvent.CompleteSet) },
                    onPause = { vm.onEvent(TimerEvent.Pause) },
                )
                Phase.REST -> RestScreen(
                    ui = ui,
                    remainingMillis = remaining,
                    shorts = shorts,
                    onPause = { vm.onEvent(TimerEvent.Pause) },
                )
                Phase.PAUSED_FROM_WORK, Phase.PAUSED_FROM_REST -> PausedScreen(
                    ui = ui,
                    remainingMillis = remaining,
                    onResume = { vm.onEvent(TimerEvent.Resume) },
                    onReset = { vm.onEvent(TimerEvent.Reset) },
                )
                Phase.DONE -> CompleteScreen(
                    totalSets = ui.totalSets,
                    onReset = { vm.onEvent(TimerEvent.Reset) },
                )
            }
        }
    }
}

@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val context = LocalContext.current
    DisposableEffect(enabled) {
        val window = context.findActivity()?.window
        if (enabled) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            if (enabled) window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

package com.example.gymstruck.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymstruck.domain.RestMode
import com.example.gymstruck.domain.ShortVideo
import com.example.gymstruck.presentation.WorkoutUiState

@Composable
fun RestScreen(
    ui: WorkoutUiState,
    remainingMillis: Long,
    shorts: List<ShortVideo>,
    onPause: () -> Unit,
) {
    // remainingMillis는 일반 Long(Compose State 아님)이라 derivedStateOf로는 변화 감지 불가.
    // 초(seconds) 단위로 키를 잡아 1초마다만 텍스트를 재계산한다.
    val remainingText = remember(remainingMillis / 1_000L) {
        val totalSecs = (remainingMillis / 1_000L).coerceAtLeast(0L)
        "%02d:%02d".format(totalSecs / 60, totalSecs % 60)
    }

    val progress = if (ui.totalRestMillis > 0L)
        (remainingMillis.toFloat() / ui.totalRestMillis.toFloat()).coerceIn(0f, 1f)
    else 0f

    if (ui.restMode == RestMode.SHORTS && shorts.isNotEmpty()) {
        ShortsRestScreen(ui, remainingText, progress, shorts, onPause)
    } else {
        TimerRestScreen(ui, remainingText, progress, onPause)
    }
}

/** SHORTS 모드: 전체화면 Shorts 피드 + 하단 타이머 오버레이. */
@Composable
private fun ShortsRestScreen(
    ui: WorkoutUiState,
    remainingText: String,
    progress: Float,
    shorts: List<ShortVideo>,
    onPause: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        ShortsFeed(shorts = shorts, modifier = Modifier.fillMaxSize())

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.93f),
            tonalElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(56.dp),
                        strokeWidth = 4.dp,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    )
                    Text(remainingText, style = MaterialTheme.typography.bodyMedium)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("휴식 중", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "세트 ${ui.setIndex} 완료 · 다음 ${ui.setIndex + 1}세트",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }

                IconButton(onClick = onPause) {
                    Icon(
                        Icons.Default.Pause,
                        contentDescription = "일시정지",
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}

/** 기존 동작(오디오 더킹): 중앙 원형 타이머. */
@Composable
private fun TimerRestScreen(
    ui: WorkoutUiState,
    remainingText: String,
    progress: Float,
    onPause: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        IconButton(
            onClick = onPause,
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Icon(Icons.Default.Pause, contentDescription = "일시정지", modifier = Modifier.size(32.dp))
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("휴식 중", style = MaterialTheme.typography.headlineMedium)

            Spacer(Modifier.height(8.dp))

            Text(
                "세트 ${ui.setIndex} 완료 · 다음은 ${ui.setIndex + 1}세트",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )

            Spacer(Modifier.height(48.dp))

            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(200.dp),
                    strokeWidth = 10.dp,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                )
                Text(remainingText, style = MaterialTheme.typography.displaySmall)
            }
        }
    }
}

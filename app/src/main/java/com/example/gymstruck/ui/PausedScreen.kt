package com.example.gymstruck.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymstruck.presentation.Phase
import com.example.gymstruck.presentation.WorkoutUiState

@Composable
fun PausedScreen(
    ui: WorkoutUiState,
    remainingMillis: Long,
    onResume: () -> Unit,
    onReset: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("⏸", style = MaterialTheme.typography.displayLarge)

        Spacer(Modifier.height(16.dp))

        Text("일시정지", style = MaterialTheme.typography.headlineLarge)

        Spacer(Modifier.height(24.dp))

        when (ui.phase) {
            Phase.PAUSED_FROM_WORK -> Text(
                "세트 ${ui.setIndex} / ${ui.totalSets} 진행 중",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Phase.PAUSED_FROM_REST -> {
                val secs = (remainingMillis / 1000L).coerceAtLeast(0L)
                Text(
                    "남은 휴식: %02d:%02d".format(secs / 60, secs % 60),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            else -> Unit
        }

        Spacer(Modifier.height(56.dp))

        Button(
            onClick = onResume,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text("재개", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Text("초기화", style = MaterialTheme.typography.titleMedium)
        }
    }
}

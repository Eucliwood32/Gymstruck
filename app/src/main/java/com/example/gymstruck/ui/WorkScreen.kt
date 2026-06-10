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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymstruck.presentation.WorkoutUiState

@Composable
fun WorkScreen(
    ui: WorkoutUiState,
    onCompleteSet: () -> Unit,
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
            Text(
                text = "세트 ${ui.setIndex} / ${ui.totalSets}",
                style = MaterialTheme.typography.displaySmall,
            )

            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                repeat(ui.totalSets) { i ->
                    val filled = i < ui.setIndex
                    Text(
                        text = if (filled) "●" else "○",
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (filled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(64.dp))

            Button(
                onClick = onCompleteSet,
                modifier = Modifier.fillMaxWidth(0.8f).height(72.dp),
            ) {
                Text("세트 완료", style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}

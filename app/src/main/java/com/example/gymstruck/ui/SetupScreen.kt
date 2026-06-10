package com.example.gymstruck.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.gymstruck.domain.RestMode
import com.example.gymstruck.domain.WorkoutConfig
import com.example.gymstruck.presentation.SearchPreview
import kotlin.math.roundToInt

@Composable
fun SetupScreen(
    lastPreset: WorkoutConfig,
    searchPreview: SearchPreview,
    onSearch: (String) -> Unit,
    onStart: (WorkoutConfig) -> Unit,
) {
    var setsValue by remember(lastPreset.totalSets) {
        mutableFloatStateOf(lastPreset.totalSets.toFloat())
    }
    var restValue by remember(lastPreset.restMillis) {
        mutableFloatStateOf((lastPreset.restMillis / 1000f).coerceIn(10f, 300f))
    }
    var restMode by remember(lastPreset.restMode) {
        mutableStateOf(lastPreset.restMode)
    }
    var query by remember(lastPreset.searchQuery) {
        mutableStateOf(lastPreset.searchQuery)
    }

    val totalSets = setsValue.roundToInt()
    val restSeconds = if (restValue <= 15f) 10
                      else ((restValue / 30f).roundToInt() * 30).coerceIn(30, 300)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))

        Text("GymStruck", style = MaterialTheme.typography.headlineLarge)

        Spacer(Modifier.height(48.dp))

        // 세트 수 슬라이더
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("세트 수", style = MaterialTheme.typography.bodyLarge)
            Text("$totalSets 세트", style = MaterialTheme.typography.bodyLarge)
        }
        Slider(
            value = setsValue,
            onValueChange = { setsValue = it },
            valueRange = 1f..10f,
            steps = 8,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))

        // 휴식 시간 슬라이더
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("휴식 시간", style = MaterialTheme.typography.bodyLarge)
            Text("${restSeconds}초", style = MaterialTheme.typography.bodyLarge)
        }
        Slider(
            value = restValue,
            onValueChange = { restValue = it },
            valueRange = 10f..300f,
            steps = 9,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(32.dp))

        // 휴식 모드 선택
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            Text("휴식 모드", style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = restMode == RestMode.DUCKING,
                onClick = { restMode = RestMode.DUCKING },
                label = { Text("오디오 더킹") },
            )
            FilterChip(
                selected = restMode == RestMode.SHORTS,
                onClick = { restMode = RestMode.SHORTS },
                label = { Text("YouTube Shorts") },
            )
        }

        // SHORTS 모드일 때만 검색 UI 노출
        if (restMode == RestMode.SHORTS) {
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("검색어 (예: 운동 자극, lo-fi)") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Search,
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { onSearch(query) },
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { onSearch(query) },
                enabled = query.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Shorts 검색")
            }

            Spacer(Modifier.height(12.dp))
            SearchPreviewSection(searchPreview)
        }

        Spacer(Modifier.height(40.dp))

        val canStart = restMode != RestMode.SHORTS ||
            (searchPreview is SearchPreview.Success && searchPreview.videos.isNotEmpty())

        Button(
            onClick = {
                onStart(
                    WorkoutConfig(
                        totalSets = totalSets,
                        restMillis = restSeconds * 1000L,
                        restMode = restMode,
                        searchQuery = query.trim(),
                    ),
                )
            },
            enabled = canStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text("운동 시작", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SearchPreviewSection(preview: SearchPreview) {
    when (preview) {
        is SearchPreview.Idle -> {
            Text(
                "검색 후 운동을 시작하면 휴식마다 Shorts가 재생됩니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        is SearchPreview.Loading -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
                Spacer(Modifier.fillMaxWidth(0.05f))
                Text("검색 중…", style = MaterialTheme.typography.bodySmall)
            }
        }
        is SearchPreview.Error -> {
            Text(
                preview.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        is SearchPreview.Success -> {
            if (preview.videos.isEmpty()) {
                Text(
                    "검색 결과가 없습니다. 다른 검색어를 입력해 보세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            } else {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        "${preview.videos.size}개 영상 준비됨",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.height(4.dp))
                    preview.videos.take(5).forEach { video ->
                        Text(
                            "• ${video.title}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

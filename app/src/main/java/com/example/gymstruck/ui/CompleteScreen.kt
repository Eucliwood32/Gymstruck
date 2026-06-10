package com.example.gymstruck.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CompleteScreen(
    totalSets: Int,
    onReset: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("운동 완료!", style = MaterialTheme.typography.displayMedium)

        Spacer(Modifier.height(16.dp))

        Text(
            "${totalSets}세트 달성",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(64.dp))

        Button(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth(0.7f).height(56.dp),
        ) {
            Text("처음으로", style = MaterialTheme.typography.titleMedium)
        }
    }
}

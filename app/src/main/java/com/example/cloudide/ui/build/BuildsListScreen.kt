package com.example.cloudide.ui.build

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.cloudide.domain.BuildRunSummary
import com.example.cloudide.ui.components.AppCard
import com.example.cloudide.ui.components.AppTopBar
import com.example.cloudide.ui.components.OutlinedPillButton
import com.example.cloudide.ui.components.StatusChip
import com.example.cloudide.ui.theme.Cream
import com.example.cloudide.ui.theme.Ink
import com.example.cloudide.ui.theme.InkMuted
import com.example.cloudide.ui.theme.Peach
import com.example.cloudide.util.relativeTime

@Composable
fun BuildsListScreen(
    onMenuClick: () -> Unit,
    onOpenRun: (Long) -> Unit,
    viewModel: BuildsListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = Cream,
        topBar = { AppTopBar(title = "Builds", onMenuClick = onMenuClick) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            if (state.repoFullName.isBlank()) {
                AppCard(containerColor = Peach) {
                    Text("Open a repo from Projects first.", color = Ink)
                }
                return@Column
            }
            Text(
                text = state.repoFullName,
                style = MaterialTheme.typography.titleMedium,
                color = Ink,
            )
            Spacer(Modifier.height(12.dp))

            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = Ink) }

                state.error != null -> {
                    Text("Error: ${state.error}", color = InkMuted)
                    Spacer(Modifier.height(8.dp))
                    OutlinedPillButton(
                        text = "Retry",
                        onClick = viewModel::refresh,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                state.runs.isEmpty() -> Text("No builds yet.", color = InkMuted)

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.runs, key = { it.id }) { run ->
                        RunCard(run = run, onClick = { onOpenRun(run.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun RunCard(run: BuildRunSummary, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Run #${run.id}", color = Ink, style = MaterialTheme.typography.titleMedium)
            StatusChip(status = run.status)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${run.branch} · ${relativeTime(run.timeMillis)}",
            color = InkMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (run.commitMessage.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = run.commitMessage,
                color = Ink,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
            )
        }
    }
}
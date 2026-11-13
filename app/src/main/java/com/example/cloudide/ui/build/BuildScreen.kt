package com.example.cloudide.ui.build

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.cloudide.domain.BuildState
import com.example.cloudide.ui.components.AppCard
import com.example.cloudide.ui.components.AppTopBar
import com.example.cloudide.ui.components.BuildStatus
import com.example.cloudide.ui.components.OutlinedPillButton
import com.example.cloudide.ui.components.PrimaryPillButton
import com.example.cloudide.ui.components.StatusChip
import com.example.cloudide.ui.theme.Cream
import com.example.cloudide.ui.theme.InkBlack
import com.example.cloudide.ui.theme.Lavender
import com.example.cloudide.ui.theme.MutedText
import com.example.cloudide.ui.theme.Peach

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildScreen(
    onOpenDrawer: () -> Unit,
    viewModel: BuildViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = Cream,
        topBar = {
            AppTopBar(title = "Build (debug)", onMenuClick = onOpenDrawer)
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.repoFullName.isBlank()) {
                AppCard(color = Peach) {
                    Text("Open Projects and tap a repo first.", color = InkBlack)
                }
            }

            AppCard {
                Text("Repository: ${state.repoFullName.ifBlank { "—" }}", color = InkBlack)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.branch,
                    onValueChange = viewModel::setBranch,
                    label = { Text("Branch") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = when (state.workflowStatus) {
                        WorkflowUiStatus.Unknown -> "Workflow: not checked"
                        WorkflowUiStatus.Exists -> "Workflow: present ✓"
                        WorkflowUiStatus.Missing -> "Workflow: missing ✗"
                    },
                    color = MutedText,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedPillButton(
                    text = if (state.isCheckingWorkflow) "Checking…" else "Check workflow",
                    onClick = viewModel::checkWorkflow,
                    enabled = state.ready && !state.isCheckingWorkflow,
                    modifier = Modifier.weight(1f),
                )
                if (state.workflowStatus == WorkflowUiStatus.Missing) {
                    PrimaryPillButton(
                        text = if (state.isCreatingWorkflow) "Creating…" else "Create workflow",
                        onClick = viewModel::createWorkflow,
                        enabled = !state.isCreatingWorkflow,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryPillButton(
                    text = if (state.isTriggering) "Triggering…" else "Trigger build",
                    onClick = viewModel::triggerBuild,
                    enabled = state.ready && !state.isTriggering,
                    modifier = Modifier.weight(1f),
                )
                if (state.isObserving) {
                    OutlinedPillButton(
                        text = "Cancel",
                        onClick = viewModel::cancelBuild,
                        modifier = Modifier.weight(1f),
                    )
                } else if (state.buildState?.isFinished == true) {
                    OutlinedPillButton(
                        text = "Re-run",
                        onClick = viewModel::rerunBuild,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            state.infoMessage?.let { msg ->
                AppCard(color = Lavender) { Text(msg, color = InkBlack) }
            }
            state.errorMessage?.let { msg ->
                AppCard(color = Peach) { Text(msg, color = InkBlack) }
            }

            state.buildState?.let { bs -> BuildStateCard(bs) }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BuildStateCard(state: BuildState) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Run #${state.run.id}", color = InkBlack, style = MaterialTheme.typography.titleMedium)
            StatusChip(status = state.uiStatus)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Branch: ${state.run.headBranch} · status: ${state.run.status}",
            color = MutedText,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(12.dp))
        state.jobs.forEach { job ->
            Text(
                text = "• ${job.name} — ${job.status}${job.conclusion?.let { " ($it)" } ?: ""}",
                color = InkBlack,
                style = MaterialTheme.typography.bodyMedium,
            )
            job.steps.forEach { step ->
                val icon = when {
                    step.isRunning -> "⏳"
                    step.uiStatus == BuildStatus.Passed -> "✓"
                    step.uiStatus == BuildStatus.Failed -> "✗"
                    step.uiStatus == BuildStatus.Cancelled -> "⨯"
                    else -> "·"
                }
                Text(
                    text = "    $icon [${step.number}] ${step.name} — ${step.status}${step.conclusion?.let { " ($it)" } ?: ""}",
                    color = MutedText,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )
            }
        }
    }
}
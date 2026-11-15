package com.example.cloudide.ui.build

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.cloudide.domain.BuildArtifact
import com.example.cloudide.domain.BuildJob
import com.example.cloudide.domain.BuildState
import com.example.cloudide.domain.BuildStatus
import com.example.cloudide.domain.BuildStep
import com.example.cloudide.domain.formatBytes
import com.example.cloudide.ui.components.AppCard
import com.example.cloudide.ui.components.AppTopBar
import com.example.cloudide.ui.components.OutlinedPillButton
import com.example.cloudide.ui.components.PrimaryPillButton
import com.example.cloudide.ui.components.StatusChip
import com.example.cloudide.ui.theme.ChipGreen
import com.example.cloudide.ui.theme.ChipRed
import com.example.cloudide.ui.theme.Cream
import com.example.cloudide.ui.theme.Ink
import com.example.cloudide.ui.theme.InkMuted
import com.example.cloudide.ui.theme.Lavender
import com.example.cloudide.ui.theme.OutlineSoft
import com.example.cloudide.ui.theme.Peach

@Composable
fun BuildScreen(
    onMenuClick: () -> Unit,
    viewModel: BuildViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showInstallDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Cream,
        topBar = { AppTopBar(title = "Build", onMenuClick = onMenuClick) },
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
                AppCard(containerColor = Peach) {
                    Text("Open Projects and tap a repo first.", color = Ink)
                }
                return@Column
            }

            // Debug controls (repo + branch + workflow check) when no run yet
            if (state.buildState == null) {
                AppCard {
                    Text("Repository: ${state.repoFullName}", color = Ink)
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
                            WorkflowUiStatus.Exists -> "Workflow: present"
                            WorkflowUiStatus.Missing -> "Workflow: missing"
                        },
                        color = InkMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedPillButton(
                        text = if (state.isCheckingWorkflow) "Checking..." else "Check workflow",
                        onClick = viewModel::checkWorkflow,
                        enabled = state.ready && !state.isCheckingWorkflow,
                        modifier = Modifier.weight(1f),
                    )
                    if (state.workflowStatus == WorkflowUiStatus.Missing) {
                        PrimaryPillButton(
                            text = if (state.isCreatingWorkflow) "Creating..." else "Create workflow",
                            onClick = viewModel::createWorkflow,
                            enabled = !state.isCreatingWorkflow,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                PrimaryPillButton(
                    text = if (state.isTriggering) "Triggering..." else "Trigger build",
                    onClick = viewModel::triggerBuild,
                    enabled = state.ready && !state.isTriggering,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            state.infoMessage?.let {
                AppCard(containerColor = Lavender) { Text(it, color = Ink) }
            }
            state.errorMessage?.let {
                AppCard(containerColor = Peach) { Text(it, color = Ink) }
            }

            state.buildState?.let { bs ->
                BuildHeaderCard(bs)
                BuildStepsCard(bs)
                BuildLogsCard(
                    state = state,
                    onRetry = { viewModel.loadLogsIfReady() },
                )
                if (state.isSuccess) {
                    SuccessCard(
                        artifact = state.artifacts.firstOrNull(),
                        isLoading = state.isLoadingArtifacts,
                        isDownloading = state.isDownloadingApk,
                        progress = state.downloadProgress,
                        apkReady = state.apkFile != null,
                        onDownload = viewModel::downloadApk,
                        onInstall = {
                            val ok = viewModel.tryInstall()
                            if (!ok) showInstallDialog = true
                        },
                        onShare = viewModel::shareApk,
                    )
                }
                if (state.isFailed || state.isCancelled) {
                    FailedCard(
                        isCancelled = state.isCancelled,
                        onRerun = viewModel::rerunBuild,
                    )
                }
                if (state.isLive) {
                    OutlinedPillButton(
                        text = "Cancel build",
                        onClick = viewModel::cancelBuild,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showInstallDialog) {
        AlertDialog(
            onDismissRequest = { showInstallDialog = false },
            title = { Text("Allow install?") },
            text = {
                Text(
                    "To install the APK, allow installs from this app in the system settings."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showInstallDialog = false
                    viewModel.openUnknownSourcesSettings()
                }) { Text("Open settings") }
            },
            dismissButton = {
                TextButton(onClick = { showInstallDialog = false }) { Text("Cancel") }
            },
        )
    }
}

// ---------------- Header ----------------

@Composable
private fun BuildHeaderCard(bs: BuildState) {
    val job = bs.jobs.firstOrNull()
    val totalSeconds = remember(bs.run.id, job?.completedAt) {
        computeDurationSeconds(job?.startedAt, job?.completedAt)
    }
    var liveSeconds by remember(bs.run.id) { mutableStateOf(totalSeconds ?: 0L) }
    LaunchedEffect(job?.status, job?.completedAt) {
        if (job?.status == "in_progress" && job.startedAt != null) {
            val start = parseIso(job.startedAt) ?: return@LaunchedEffect
            while (true) {
                liveSeconds = (System.currentTimeMillis() - start) / 1000
                kotlinx.coroutines.delay(1000L)
                if (job.completedAt != null) break
            }
        }
    }
    val shown = totalSeconds ?: liveSeconds

    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Run #${bs.run.id}", color = Ink, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${bs.run.headBranch} · ${formatElapsed(shown)}",
                    color = InkMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            StatusChip(status = bs.uiStatus)
        }
    }
}

// ---------------- Steps ----------------

@Composable
private fun BuildStepsCard(bs: BuildState) {
    AppCard {
        Text("Steps", color = Ink, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        bs.jobs.forEach { job ->
            JobRow(job)
        }
    }
}

@Composable
private fun JobRow(job: BuildJob) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(job.status, job.conclusion)
            Spacer(Modifier.size(8.dp))
            Text(job.name, color = Ink, style = MaterialTheme.typography.bodyMedium)
        }
        job.steps.forEach { step ->
            StepRow(step)
        }
    }
}

@Composable
private fun StepRow(step: BuildStep) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(step.status, step.conclusion)
        Spacer(Modifier.size(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.name,
                color = Ink,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (step.isRunning) {
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Ink,
                    trackColor = OutlineSoft,
                )
            }
        }
        val dur = computeDurationSeconds(step.startedAt, step.completedAt)
        if (dur != null && dur > 0) {
            Text(formatElapsed(dur), color = InkMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun StatusDot(status: String, conclusion: String?) {
    val color: Color = when {
        status == "in_progress" -> Ink
        status == "completed" && conclusion in setOf("success", "neutral") -> Color(0xFF1E7A3A)
        status == "completed" && conclusion in setOf("failure", "timed_out", "startup_failure") -> ChipRed
        status == "completed" && conclusion in setOf("cancelled", "skipped", "stale") -> Color(0xFF8B8580)
        else -> OutlineSoft
    }
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
}

// ---------------- Logs ----------------

@Composable
private fun BuildLogsCard(state: BuildUiState, onRetry: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    AppCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Logs", color = Ink, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = Ink,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                when {
                    state.isLoadingLogs -> Text("Loading logs…", color = InkMuted)
                    state.logsError != null -> {
                        Text("Logs unavailable: ${state.logsError}", color = ChipRed)
                        TextButton(onClick = onRetry) { Text("Retry") }
                    }
                    !state.buildState!!.isFinished -> Text(
                        "Logs available when the job finishes.",
                        color = InkMuted,
                    )
                    state.logs.isNullOrBlank() -> Text(
                        "No logs returned for this job.",
                        color = InkMuted,
                    )
                    else -> {
                        if (state.logsTruncated) {
                            Text(
                                "Showing the last ${200_000} characters.",
                                color = InkMuted,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                        Text(
                            text = state.logs!!,
                            color = Ink,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                            ),
                        )
                    }
                }
            }
        }
    }
}

// ---------------- Success / Failed ----------------

@Composable
private fun SuccessCard(
    artifact: BuildArtifact?,
    isLoading: Boolean,
    isDownloading: Boolean,
    progress: Float,
    apkReady: Boolean,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onShare: () -> Unit,
) {
    AppCard(containerColor = ChipGreen) {
        Text("Build succeeded", color = Ink, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        when {
            isLoading -> Text("Loading artifacts…", color = Ink)
            artifact == null -> Text("No APK artifact found for this run.", color = Ink)
            else -> {
                Text(
                    "${artifact.name} · ${formatBytes(artifact.sizeInBytes)}",
                    color = Ink,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                if (isDownloading) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Ink,
                        trackColor = OutlineSoft,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Downloading… ${(progress * 100).toInt()}%", color = Ink)
                } else if (apkReady) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrimaryPillButton(
                            text = "Install",
                            onClick = onInstall,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedPillButton(
                            text = "Share",
                            onClick = onShare,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    PrimaryPillButton(
                        text = "Download APK",
                        onClick = onDownload,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun FailedCard(isCancelled: Boolean, onRerun: () -> Unit) {
    AppCard(containerColor = if (isCancelled) Peach else ChipRed) {
        Text(
            text = if (isCancelled) "Build cancelled" else "Build failed",
            color = Ink,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedPillButton(
            text = "Re-run build",
            onClick = onRerun,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ---------------- Helpers ----------------

private fun parseIso(value: String?): Long? =
    if (value == null) null
    else runCatching { java.time.Instant.parse(value).toEpochMilli() }.getOrNull()

private fun computeDurationSeconds(startedAt: String?, completedAt: String?): Long? {
    if (startedAt == null || completedAt == null) return null
    val s = parseIso(startedAt) ?: return null
    val e = parseIso(completedAt) ?: return null
    return ((e - s) / 1000).coerceAtLeast(0)
}

private fun formatElapsed(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
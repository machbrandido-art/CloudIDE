package com.example.cloudide.ui.build

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.cloudide.domain.*
import com.example.cloudide.ui.components.*
import com.example.cloudide.ui.theme.*
import java.util.Locale

@Composable
fun BuildScreen(
    onMenuClick: () -> Unit,
    viewModel: BuildViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    var showInstallDialog by remember { mutableStateOf(false) }

    val title = when {
        state.isLive -> "Live Build"
        state.buildState != null -> "Build #${state.buildState!!.run.id}"
        else -> "Build"
    }

    Scaffold(
        containerColor = Cream,
        topBar = {
            AppTopBar(
                title = title,
                onMenuClick = onMenuClick,
                actions = {
                    if (state.isLive) {
                        PulsingDot()
                    }
                }
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Debug controls (repo + branch + workflow) - only when no build is active
            if (state.buildState == null && !state.isTriggering) {
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
                        color = InkMuted, style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    text = "Start Build",
                    onClick = viewModel::triggerBuild,
                    enabled = state.ready,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }

            state.errorMessage?.let {
                AppCard(containerColor = Peach) { Text(it, color = Ink) }
            }

            state.buildState?.let { bs ->
                if (state.isLive) {
                    LiveBuildView(
                        state = state,
                        bs = bs,
                        onCancel = viewModel::cancelBuild,
                        onToggleLogs = viewModel::toggleLogs,
                    )
                } else if (state.isSuccess) {
                    SuccessView(
                        state = state,
                        bs = bs,
                        onDownload = viewModel::downloadApk,
                        onInstall = {
                            val ok = viewModel.tryInstall()
                            if (!ok) showInstallDialog = true
                        },
                        onShare = viewModel::shareApk,
                        onToggleLogs = viewModel::toggleLogs,
                    )
                } else if (state.isFailed || state.isCancelled) {
                    FailedView(
                        state = state,
                        bs = bs,
                        onRerun = viewModel::rerunBuild,
                        onToggleLogs = viewModel::toggleLogs,
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
            text = { Text("To install the APK, allow installs from this app in the system settings.") },
            confirmButton = {
                TextButton(onClick = {
                    showInstallDialog = false
                    viewModel.openUnknownSourcesSettings()
                }) { Text("Open settings") }
            },
            dismissButton = { TextButton(onClick = { showInstallDialog = false }) { Text("Cancel") } },
        )
    }
}

// ---------------- Live Build View ----------------

@Composable
private fun LiveBuildView(
    state: BuildUiState,
    bs: BuildState,
    onCancel: () -> Unit,
    onToggleLogs: () -> Unit,
) {
    val job = bs.jobs.firstOrNull()
    val steps = job?.steps ?: emptyList()
    
    // Header Card (Lavender)
    AppCard(containerColor = Lavender) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.4f), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(14.dp), tint = Ink)
                    Spacer(Modifier.width(4.dp))
                    Text(bs.run.headBranch, color = Ink, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(state.repoFullName, color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        Text(
            text = formatTime(state.elapsedSeconds),
            color = Ink,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
        Text("Running...", color = Ink, style = MaterialTheme.typography.bodyMedium)
    }

    // Steps Cards
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        steps.forEach { step ->
            StepCard(step = step, totalSteps = steps.size, isLive = true)
        }
    }

    // Logs Card
    LogsCard(state = state, isFinished = bs.isFinished, onToggle = onToggleLogs)

    // Cancel Button
    OutlinedPillButton(
        text = "Cancel build",
        onClick = onCancel,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
}

// ---------------- Success View ----------------

@Composable
private fun SuccessView(
    state: BuildUiState,
    bs: BuildState,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onShare: () -> Unit,
    onToggleLogs: () -> Unit,
) {
    // Success Header (Green)
    AppCard(containerColor = ChipGreen) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OnChipGreen, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(8.dp))
            Text("Build succeeded", color = Ink, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Spacer(Modifier.height(4.dp))
            Text(formatTime(state.elapsedSeconds), color = Ink, style = MaterialTheme.typography.bodyMedium)
        }
    }

    // APK Card (Lavender)
    AppCard(containerColor = Lavender) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.background(Color.White.copy(alpha = 0.5f), CircleShape).padding(12.dp)
            ) {
                Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = Ink)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                val artifact = state.artifacts.firstOrNull()
                Text(artifact?.name ?: "app-debug.apk", color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(formatBytes(artifact?.sizeInBytes ?: 0), color = InkMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(16.dp))
        
        if (state.isDownloadingApk) {
            LinearProgressIndicator(progress = { state.downloadProgress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = Ink)
            Spacer(Modifier.height(8.dp))
            Text("Downloading... ${(state.downloadProgress * 100).toInt()}%", color = Ink)
        } else if (state.apkFile != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onInstall,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Ink)
                ) { Text("Install", color = Cream) }
                
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, Ink)
                ) { Text("Share APK", color = Ink) }
            }
        } else {
            Button(
                onClick = onDownload,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Ink)
            ) { Text("Download APK", color = Cream) }
        }
    }

    // Steps
    Text("Build steps", color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        bs.jobs.firstOrNull()?.steps?.forEach { step ->
            StepCard(step = step, totalSteps = bs.jobs.firstOrNull()?.steps?.size ?: 0, isLive = false)
        }
    }

    LogsCard(state = state, isFinished = bs.isFinished, onToggle = onToggleLogs)
}

// ---------------- Failed View ----------------

@Composable
private fun FailedView(
    state: BuildUiState,
    bs: BuildState,
    onRerun: () -> Unit,
    onToggleLogs: () -> Unit,
) {
    Text("Build failed", color = Ink, fontWeight = FontWeight.Bold, fontSize = 32.sp)
    
    // Summary Card (Red)
    AppCard(containerColor = ChipRed) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Build #${bs.run.id}", color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.background(OnChipRed.copy(alpha = 0.2f), CircleShape).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text("Failed", color = OnChipRed, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.weight(1f))
            Text(formatTime(state.elapsedSeconds), color = Ink, style = MaterialTheme.typography.bodyMedium)
        }
    }

    // Error Card (White) - Stub for now, Stage 7 will parse actual errors
    AppCard {
        Text("Build process failed", color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text("Check logs for more details.", color = InkMuted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { /* Stage 7 */ },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Ink)
            ) { Text("Go to line", color = Cream) }
            
            OutlinedButton(
                onClick = onRerun,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = CircleShape,
                border = BorderStroke(1.dp, Ink)
            ) { Text("Re-run build", color = Ink) }
        }
    }

    // Steps
    Text("Build steps", color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        bs.jobs.firstOrNull()?.steps?.forEachIndexed { index, step ->
            StepCard(step = step, totalSteps = bs.jobs.firstOrNull()?.steps?.size ?: 0, isLive = false, index = index)
        }
    }
    
    LogsCard(state = state, isFinished = bs.isFinished, onToggle = onToggleLogs)
}

// ---------------- Components ----------------

@Composable
private fun StepCard(step: BuildStep, totalSteps: Int, isLive: Boolean, index: Int = -1) {
    val isFailed = step.uiStatus == BuildStatus.Failed
    val isPassed = step.uiStatus == BuildStatus.Passed
    
    val containerColor = if (isFailed) ChipRed else CardWhite
    val borderModifier = if (isFailed) Modifier.border(1.dp, OnChipRed, RoundedCornerShape(24.dp)) else Modifier

    AppCard(containerColor = containerColor, modifier = borderModifier) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            if (index >= 0) {
                Text(String.format(Locale.US, "%02d", index + 1), color = Ink, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
            } else if (isPassed) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OnChipGreen, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            
            Text(step.name, color = Ink, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            
            // Status Chip
            val (chipBg, chipFg, chipText) = when {
                step.isRunning -> Triple(ChipAmber, OnChipAmber, "In Progress")
                isPassed -> Triple(ChipGreen, OnChipGreen, "Success")
                isFailed -> Triple(ChipRed, OnChipRed, "Failed")
                else -> Triple(Color.Transparent, InkMuted, "Pending")
            }
            
            if (step.isRunning) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(chipFg, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text(chipText, color = chipFg, style = MaterialTheme.typography.labelMedium)
                }
            } else if (isPassed || isFailed) {
                Box(modifier = Modifier.background(chipBg, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text(chipText, color = chipFg, style = MaterialTheme.typography.labelMedium)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).border(1.dp, chipFg, CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text(chipText, color = chipFg, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        
        // Progress bar for running step
        if (step.isRunning) {
            Spacer(Modifier.height(12.dp))
            val progress = 0.65f // Mock progress for now
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = Ink,
                    trackColor = OutlineSoft
                )
                Spacer(Modifier.width(12.dp))
                Text("${(progress * 100).toInt()}%", color = Ink, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun LogsCard(state: BuildUiState, isFinished: Boolean, onToggle: () -> Unit) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onToggle() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Logs", color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (state.showLogs) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null, tint = Ink
            )
        }
        AnimatedVisibility(visible = state.showLogs) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                when {
                    state.isLoadingLogs -> Text("Loading logs...", color = InkMuted)
                    state.logsError != null -> Text("Error: ${state.logsError}", color = ChipRed)
                    !isFinished -> Text("Logs available when the job finishes.", color = InkMuted)
                    state.logs.isNullOrBlank() -> Text("No logs returned.", color = InkMuted)
                    else -> {
                        if (state.logsTruncated) {
                            Text("Showing last 200k chars.", color = InkMuted, style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.height(4.dp))
                        }
                        Text(
                            text = state.logs!!,
                            color = Ink,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)).padding(8.dp).fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PulsingDot() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse)
    )
    Box(modifier = Modifier.padding(end = 16.dp).size(10.dp).background(ChipAmber.copy(alpha = alpha), CircleShape))
}

private fun formatTime(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
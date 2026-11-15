package com.example.cloudide.ui.build

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cloudide.data.github.BuildDetailsRepository
import com.example.cloudide.data.github.BuildRepository
import com.example.cloudide.data.github.WorkflowCheckResult
import com.example.cloudide.data.github.WorkflowCreateResult
import com.example.cloudide.data.github.WorkflowSetup
import com.example.cloudide.data.local.CurrentRepoStore
import com.example.cloudide.domain.BuildArtifact
import com.example.cloudide.domain.BuildState
import com.example.cloudide.domain.BuildStatus
import com.example.cloudide.domain.BuildTriggerResult
import com.example.cloudide.util.ApkInstaller
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class WorkflowUiStatus { Unknown, Exists, Missing }

data class BuildUiState(
    val owner: String = "",
    val repoName: String = "",
    val branch: String = "",
    val workflowStatus: WorkflowUiStatus = WorkflowUiStatus.Unknown,
    val isCheckingWorkflow: Boolean = false,
    val isCreatingWorkflow: Boolean = false,
    val isTriggering: Boolean = false,
    val buildState: BuildState? = null,
    val logs: String? = null,
    val logsTruncated: Boolean = false,
    val isLoadingLogs: Boolean = false,
    val logsError: String? = null,
    val artifacts: List<BuildArtifact> = emptyList(),
    val isLoadingArtifacts: Boolean = false,
    val apkFile: File? = null,
    val isDownloadingApk: Boolean = false,
    val downloadProgress: Float = 0f,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
) {
    val repoFullName: String
        get() = if (owner.isBlank() || repoName.isBlank()) "" else "$owner/$repoName"
    val ready: Boolean get() = repoFullName.isNotBlank() && branch.isNotBlank()
    val isLive: Boolean get() = buildState?.isFinished == false
    val isSuccess: Boolean get() = buildState?.uiStatus == BuildStatus.Passed
    val isFailed: Boolean get() = buildState?.uiStatus == BuildStatus.Failed
    val isCancelled: Boolean get() = buildState?.uiStatus == BuildStatus.Cancelled
}

@HiltViewModel
class BuildViewModel @Inject constructor(
    private val buildRepo: BuildRepository,
    private val detailsRepo: BuildDetailsRepository,
    private val workflowSetup: WorkflowSetup,
    private val currentRepoStore: CurrentRepoStore,
    private val apkInstaller: ApkInstaller,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(BuildUiState())
    val state: StateFlow<BuildUiState> = _state.asStateFlow()

    private var observeJob: Job? = null
    private var lastRunId: Long? = null
    private var logsLoadedForRun: Long? = null
    private var artifactsLoadedForRun: Long? = null

    init {
        viewModelScope.launch {
            val current = currentRepoStore.current.first()
            if (current != null) {
                _state.update {
                    it.copy(
                        owner = current.owner,
                        repoName = current.name,
                        branch = current.defaultBranch,
                    )
                }
            }
        }
    }

    fun setBranch(value: String) = _state.update { it.copy(branch = value) }

    fun checkWorkflow() {
        val s = _state.value
        if (!s.ready) {
            _state.update { it.copy(errorMessage = "Pick a repo and branch first.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isCheckingWorkflow = true, errorMessage = null) }
            when (val r = workflowSetup.check(s.owner, s.repoName, s.branch)) {
                WorkflowCheckResult.Exists -> _state.update {
                    it.copy(isCheckingWorkflow = false, workflowStatus = WorkflowUiStatus.Exists)
                }
                WorkflowCheckResult.Missing -> _state.update {
                    it.copy(isCheckingWorkflow = false, workflowStatus = WorkflowUiStatus.Missing)
                }
                is WorkflowCheckResult.Forbidden -> _state.update {
                    it.copy(isCheckingWorkflow = false, errorMessage = r.message)
                }
                is WorkflowCheckResult.Error -> _state.update {
                    it.copy(isCheckingWorkflow = false, errorMessage = r.message)
                }
            }
        }
    }

    fun createWorkflow() {
        val s = _state.value
        if (!s.ready) return
        viewModelScope.launch {
            _state.update { it.copy(isCreatingWorkflow = true, errorMessage = null) }
            when (val r = workflowSetup.create(s.owner, s.repoName, s.branch)) {
                WorkflowCreateResult.Created -> _state.update {
                    it.copy(
                        isCreatingWorkflow = false,
                        workflowStatus = WorkflowUiStatus.Exists,
                        infoMessage = "Workflow file created.",
                    )
                }
                is WorkflowCreateResult.Forbidden -> _state.update {
                    it.copy(isCreatingWorkflow = false, errorMessage = r.message)
                }
                is WorkflowCreateResult.Error -> _state.update {
                    it.copy(isCreatingWorkflow = false, errorMessage = r.message)
                }
            }
        }
    }

    fun triggerBuild() {
        val s = _state.value
        if (!s.ready) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isTriggering = true,
                    errorMessage = null,
                    infoMessage = null,
                    buildState = null,
                    logs = null,
                    artifacts = emptyList(),
                    apkFile = null,
                )
            }
            logsLoadedForRun = null
            artifactsLoadedForRun = null
            when (val r = buildRepo.trigger(s.owner, s.repoName, s.branch)) {
                is BuildTriggerResult.Triggered -> {
                    _state.update {
                        it.copy(isTriggering = false, infoMessage = "Run #${r.runId} started")
                    }
                    startObserving(r.runId)
                }
                BuildTriggerResult.NoNewRun -> _state.update {
                    it.copy(isTriggering = false, errorMessage = "No new run appeared in 30s.")
                }
                is BuildTriggerResult.WorkflowMissing -> _state.update {
                    it.copy(isTriggering = false, errorMessage = r.message)
                }
                is BuildTriggerResult.Forbidden -> _state.update {
                    it.copy(isTriggering = false, errorMessage = r.message)
                }
                is BuildTriggerResult.Error -> _state.update {
                    it.copy(isTriggering = false, errorMessage = r.message)
                }
            }
        }
    }

    private fun startObserving(runId: Long) {
        lastRunId = runId
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            val s = _state.value
            try {
                buildRepo.observe(s.owner, s.repoName, runId).collect { bs ->
                    _state.update { it.copy(buildState = bs) }
                    if (bs.isFinished) {
                        loadLogsIfReady()
                        if (bs.uiStatus == BuildStatus.Passed) loadArtifactsIfSuccess()
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun loadLogsIfReady() {
        val s = _state.value
        val bs = s.buildState ?: return
        if (!bs.isFinished) return
        if (logsLoadedForRun == bs.run.id) return
        logsLoadedForRun = bs.run.id
        viewModelScope.launch {
            _state.update { it.copy(isLoadingLogs = true, logsError = null) }
            val jobId = bs.jobs.firstOrNull()?.id
            if (jobId == null) {
                _state.update { it.copy(isLoadingLogs = false) }
                return@launch
            }
            detailsRepo.getLogs(s.owner, s.repoName, jobId)
                .onSuccess { text ->
                    val truncated = text.length > MAX_LOGS_CHARS
                    val display = if (truncated) text.takeLast(MAX_LOGS_CHARS) else text
                    _state.update {
                        it.copy(
                            logs = display,
                            logsTruncated = truncated,
                            isLoadingLogs = false,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(isLoadingLogs = false, logsError = e.message)
                    }
                }
        }
    }

    fun loadArtifactsIfSuccess() {
        val s = _state.value
        val bs = s.buildState ?: return
        if (!bs.isFinished || bs.uiStatus != BuildStatus.Passed) return
        if (artifactsLoadedForRun == bs.run.id) return
        artifactsLoadedForRun = bs.run.id
        viewModelScope.launch {
            _state.update { it.copy(isLoadingArtifacts = true) }
            detailsRepo.listArtifacts(s.owner, s.repoName, bs.run.id)
                .onSuccess { list ->
                    _state.update { it.copy(artifacts = list, isLoadingArtifacts = false) }
                }
                .onFailure {
                    _state.update { it.copy(isLoadingArtifacts = false) }
                }
        }
    }

    fun downloadApk() {
        val s = _state.value
        val artifact = s.artifacts.firstOrNull() ?: return
        if (s.isDownloadingApk) return
        viewModelScope.launch {
            _state.update { it.copy(isDownloadingApk = true, downloadProgress = 0f) }
            val dest = apkInstaller.apkCacheDir()
            detailsRepo.downloadApk(
                owner = s.owner,
                repo = s.repoName,
                artifactId = artifact.id,
                destDir = dest,
                onProgress = { p -> _state.update { it.copy(downloadProgress = p) } },
            ).onSuccess { file ->
                _state.update {
                    it.copy(isDownloadingApk = false, downloadProgress = 1f, apkFile = file)
                }
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        isDownloadingApk = false,
                        errorMessage = e.message ?: "Download failed",
                    )
                }
            }
        }
    }

    fun tryInstall(): Boolean {
        val file = _state.value.apkFile ?: return false
        if (!apkInstaller.canInstall()) return false
        apkInstaller.install(file)
        return true
    }

    fun openUnknownSourcesSettings() = apkInstaller.openUnknownSourcesSettings()

    fun shareApk() {
        val file = _state.value.apkFile ?: return
        apkInstaller.share(file)
    }

    fun cancelBuild() {
        val runId = lastRunId ?: return
        val s = _state.value
        viewModelScope.launch { buildRepo.cancel(s.owner, s.repoName, runId) }
    }

    fun rerunBuild() {
        val runId = lastRunId ?: return
        val s = _state.value
        viewModelScope.launch {
            buildRepo.rerun(s.owner, s.repoName, runId)
                .onSuccess {
                    _state.update { st -> st.copy(infoMessage = "Rerun requested.") }
                    logsLoadedForRun = null
                    artifactsLoadedForRun = null
                    startObserving(runId)
                }
                .onFailure { e ->
                    _state.update { st -> st.copy(errorMessage = e.message) }
                }
        }
    }

    fun consumeMessages() = _state.update { it.copy(errorMessage = null, infoMessage = null) }

    override fun onCleared() {
        observeJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val MAX_LOGS_CHARS = 200_000
    }
}
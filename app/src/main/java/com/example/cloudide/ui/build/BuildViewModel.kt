package com.example.cloudide.ui.build

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cloudide.data.github.BuildRepository
import com.example.cloudide.data.github.WorkflowCheckResult
import com.example.cloudide.data.github.WorkflowCreateResult
import com.example.cloudide.data.github.WorkflowSetup
import com.example.cloudide.data.local.CurrentRepoStore
import com.example.cloudide.domain.BuildState
import com.example.cloudide.domain.BuildTriggerResult
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val isObserving: Boolean = false,
    val buildState: BuildState? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
) {
    val repoFullName: String get() = if (owner.isBlank() || repoName.isBlank()) "" else "$owner/$repoName"
    val ready: Boolean get() = repoFullName.isNotBlank() && branch.isNotBlank()
}

@HiltViewModel
class BuildViewModel @Inject constructor(
    private val buildRepo: BuildRepository,
    private val workflowSetup: WorkflowSetup,
    private val currentRepoStore: CurrentRepoStore,
) : ViewModel() {

    private val _state = MutableStateFlow(BuildUiState())
    val state: StateFlow<BuildUiState> = _state.asStateFlow()

    private var observeJob: Job? = null
    private var lastRunId: Long? = null

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
            _state.update { it.copy(isTriggering = true, errorMessage = null, infoMessage = null) }
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
            _state.update { it.copy(isObserving = true, buildState = null) }
            val s = _state.value
            try {
                buildRepo.observe(s.owner, s.repoName, runId).collect { bs ->
                    _state.update { it.copy(buildState = bs, isObserving = !bs.isFinished) }
                }
            } catch (_: Exception) {
                _state.update { it.copy(isObserving = false) }
            }
        }
    }

    fun cancelBuild() {
        val runId = lastRunId ?: return
        val s = _state.value
        viewModelScope.launch {
            buildRepo.cancel(s.owner, s.repoName, runId)
        }
    }

    fun rerunBuild() {
        val runId = lastRunId ?: return
        val s = _state.value
        viewModelScope.launch {
            buildRepo.rerun(s.owner, s.repoName, runId)
                .onSuccess {
                    _state.update { st -> st.copy(infoMessage = "Rerun requested.") }
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
}
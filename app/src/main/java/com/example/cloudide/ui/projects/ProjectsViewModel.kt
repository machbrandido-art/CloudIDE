package com.example.cloudide.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cloudide.data.github.ProjectsRepository
import com.example.cloudide.data.github.RateLimitTracker
import com.example.cloudide.data.local.CurrentRepoStore
import com.example.cloudide.domain.BuildInfo
import com.example.cloudide.domain.CurrentRepo
import com.example.cloudide.domain.LoadFailure
import com.example.cloudide.domain.Repo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

sealed interface RunState {
    data object Loading : RunState
    data class Loaded(val info: BuildInfo) : RunState
    data object Unavailable : RunState
}

sealed interface ProjectsError {
    data object Offline : ProjectsError
    data class RateLimited(val resetAtMillis: Long) : ProjectsError
    data object Unknown : ProjectsError
}

data class ProjectsUiState(
    val repos: List<Repo> = emptyList(),
    val runs: Map<String, RunState> = emptyMap(),
    val query: String = "",
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val error: ProjectsError? = null,
    val rateLimitResetAt: Long? = null,
    val generation: Int = 0,
    val showAddDialog: Boolean = false
)

sealed interface ProjectsEvent {
    data object OpenEditor : ProjectsEvent
}

@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val repository: ProjectsRepository,
    private val currentRepoStore: CurrentRepoStore,
    private val rateLimitTracker: RateLimitTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectsUiState())
    val uiState: StateFlow<ProjectsUiState> = _uiState.asStateFlow()

    private val _events = Channel<ProjectsEvent>(Channel.BUFFERED)
    val events: Flow<ProjectsEvent> = _events.receiveAsFlow()

    // At most 3 "latest run" requests at the same time.
    private val runGate = Semaphore(3)
    private val fetchedGeneration = HashMap<String, Int>()
    private var listJob: Job? = null
    private var nextPage = 1

    init {
        loadFirstPage(refresh = false)
        observeRateLimit()
    }

    fun refresh() = loadFirstPage(refresh = true)

    fun onQueryChange(value: String) {
        _uiState.update { it.copy(query = value) }
    }

    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true) }
    }

    fun dismissAddDialog() {
        _uiState.update { it.copy(showAddDialog = false) }
    }

    fun openRepo(repo: Repo) {
        viewModelScope.launch {
            currentRepoStore.select(CurrentRepo(repo.owner, repo.name, repo.defaultBranch))
            _events.send(ProjectsEvent.OpenEditor)
        }
    }

    private fun loadFirstPage(refresh: Boolean) {
        listJob?.cancel()
        listJob = viewModelScope.launch {
            val hasData = _uiState.value.repos.isNotEmpty()
            _uiState.update {
                it.copy(
                    isInitialLoading = !hasData,
                    isRefreshing = hasData,
                    isLoadingMore = false,
                    error = null,
                    generation = if (refresh) it.generation + 1 else it.generation
                )
            }
            try {
                val page = repository.loadRepos(page = 1)
                val cached = repository.cachedBuilds().mapValues { RunState.Loaded(it.value) }
                nextPage = 2
                _uiState.update { s ->
                    s.copy(
                        repos = page.repos,
                        hasMore = page.hasMore,
                        runs = cached + s.runs.filterValues { it is RunState.Loaded },
                        isInitialLoading = false,
                        isRefreshing = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: LoadFailure) {
                _uiState.update {
                    it.copy(
                        isInitialLoading = false,
                        isRefreshing = false,
                        error = e.toUiError()
                    )
                }
            }
        }
    }

    fun loadMore() {
        val s = _uiState.value
        if (!s.hasMore || s.isLoadingMore || s.isInitialLoading || s.isRefreshing) return
        if (listJob?.isActive == true) return
        listJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            try {
                val page = repository.loadRepos(page = nextPage)
                nextPage++
                _uiState.update { st ->
                    st.copy(
                        repos = (st.repos + page.repos).distinctBy { it.id },
                        hasMore = page.hasMore,
                        isLoadingMore = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: LoadFailure) {
                _uiState.update { it.copy(isLoadingMore = false, error = e.toUiError()) }
            }
        }
    }

    /**
     * Called by a card while it is on screen. It runs inside the card's LaunchedEffect,
     * so scrolling the card away cancels the request.
     */
    suspend fun loadRun(repo: Repo) {
        val name = repo.fullName
        val generation = _uiState.value.generation
        if (fetchedGeneration[name] == generation) return
        if (_uiState.value.runs[name] == null) setRun(name, RunState.Loading)

        runGate.withPermit {
            try {
                val info = repository.getBuild(repo, force = generation > 0)
                fetchedGeneration[name] = generation
                setRun(name, RunState.Loaded(info))
            } catch (e: CancellationException) {
                throw e
            } catch (e: LoadFailure) {
                if (_uiState.value.runs[name] !is RunState.Loaded) {
                    setRun(name, RunState.Unavailable)
                }
            }
        }
    }

    private fun setRun(name: String, state: RunState) {
        _uiState.update { it.copy(runs = it.runs + (name to state)) }
    }

    private fun observeRateLimit() {
        viewModelScope.launch {
            rateLimitTracker.blockedUntil.collectLatest { until ->
                val now = System.currentTimeMillis()
                if (until > now) {
                    _uiState.update { it.copy(rateLimitResetAt = until) }
                    delay(until - now)
                }
                _uiState.update { it.copy(rateLimitResetAt = null) }
            }
        }
    }

    private fun LoadFailure.toUiError(): ProjectsError = when (this) {
        is LoadFailure.Offline -> ProjectsError.Offline
        is LoadFailure.RateLimited -> ProjectsError.RateLimited(resetAtMillis)
        else -> ProjectsError.Unknown
    }
}
package com.example.cloudide.ui.build

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cloudide.data.github.BuildDetailsRepository
import com.example.cloudide.data.local.CurrentRepoStore
import com.example.cloudide.domain.BuildRunSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BuildsListUiState(
    val owner: String = "",
    val repoName: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val runs: List<BuildRunSummary> = emptyList(),
) {
    val repoFullName: String
        get() = if (owner.isBlank() || repoName.isBlank()) "" else "$owner/$repoName"
}

@HiltViewModel
class BuildsListViewModel @Inject constructor(
    private val detailsRepo: BuildDetailsRepository,
    private val currentRepoStore: CurrentRepoStore,
) : ViewModel() {

    private val _state = MutableStateFlow(BuildsListUiState())
    val state: StateFlow<BuildsListUiState> = _state.asStateFlow()

    // أزلنا init واستبدلناها بـ refresh() لكي نستدعيها من الشاشة كلما دخلنا
    fun refresh() {
        viewModelScope.launch {
            val cur = currentRepoStore.current.first()
            if (cur == null) {
                _state.update { 
                    it.copy(isLoading = false, error = "No repo selected. Open one from Projects first.", runs = emptyList()) 
                }
                return@launch
            }
            
            _state.update { it.copy(owner = cur.owner, repoName = cur.name, isLoading = true, error = null) }
            
            detailsRepo.listRuns(cur.owner, cur.name)
                .onSuccess { runs ->
                    _state.update { it.copy(runs = runs, isLoading = false) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "Unknown error") }
                }
        }
    }
}
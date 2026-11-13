package com.example.cloudide.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cloudide.data.github.AuthRepository
import com.example.cloudide.data.github.SessionManager
import com.example.cloudide.data.local.CurrentRepoStore
import com.example.cloudide.data.local.SessionStore
import com.example.cloudide.data.local.TokenStore
import com.example.cloudide.ui.drawer.DrawerUiState
import com.example.cloudide.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val tokenStore: TokenStore,
    sessionStore: SessionStore,
    sessionManager: SessionManager,
    private val authRepository: AuthRepository,
    private val currentRepoStore: CurrentRepoStore
) : ViewModel() {

    private val _startRoute = MutableStateFlow<String?>(null)

    /** null while we are still reading the token. */
    val startRoute: StateFlow<String?> = _startRoute.asStateFlow()

    /** Emits when any API call got 401 for the stored token. */
    val sessionExpired: SharedFlow<Unit> = sessionManager.sessionExpired

    // The build badge becomes real in Stage 4.
    val drawerState: StateFlow<DrawerUiState> = combine(
        sessionStore.username,
        currentRepoStore.recents
    ) { name, recents ->
        DrawerUiState(
            username = name.orEmpty(),
            isConnected = name != null,
            activeBuilds = 0,
            recentRepos = recents.map { it.name }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DrawerUiState(
            username = "",
            isConnected = false,
            activeBuilds = 0,
            recentRepos = emptyList()
        )
    )

    init {
        viewModelScope.launch {
            val hasToken = tokenStore.token.first() != null
            _startRoute.value = if (hasToken) Routes.Projects else Routes.Onboarding
        }
    }

    /** Makes a repo from the drawer's "Recent repos" the current repo. */
    fun selectRecent(name: String) {
        viewModelScope.launch {
            val repo = currentRepoStore.recents.first().firstOrNull { it.name == name }
                ?: return@launch
            currentRepoStore.select(repo)
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
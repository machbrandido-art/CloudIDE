package com.example.cloudide.ui.connect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cloudide.data.github.AuthRepository
import com.example.cloudide.domain.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ConnectError { InvalidToken, Network, Unknown }

data class ConnectUiState(
    val token: String = "",
    val tokenVisible: Boolean = false,
    val loading: Boolean = false,
    val error: ConnectError? = null
)

sealed interface ConnectEvent {
    data object Connected : ConnectEvent
}

@HiltViewModel
class ConnectViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectUiState())
    val uiState: StateFlow<ConnectUiState> = _uiState.asStateFlow()

    private val _events = Channel<ConnectEvent>(Channel.BUFFERED)
    val events: Flow<ConnectEvent> = _events.receiveAsFlow()

    fun onTokenChange(value: String) {
        _uiState.update {
            it.copy(token = value.filterNot { c -> c.isWhitespace() }, error = null)
        }
    }

    fun toggleTokenVisibility() {
        _uiState.update { it.copy(tokenVisible = !it.tokenVisible) }
    }

    fun connect() {
        val current = _uiState.value
        if (current.loading || current.token.isBlank()) return
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (authRepository.connect(current.token)) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(loading = false, token = "") }
                    _events.send(ConnectEvent.Connected)
                }
                AuthResult.InvalidToken ->
                    _uiState.update { it.copy(loading = false, error = ConnectError.InvalidToken) }
                AuthResult.NetworkError ->
                    _uiState.update { it.copy(loading = false, error = ConnectError.Network) }
                AuthResult.Unknown ->
                    _uiState.update { it.copy(loading = false, error = ConnectError.Unknown) }
            }
        }
    }
}
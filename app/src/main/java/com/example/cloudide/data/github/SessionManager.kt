package com.example.cloudide.data.github

import com.example.cloudide.data.local.CurrentRepoStore
import com.example.cloudide.data.local.RepoBuildDao
import com.example.cloudide.data.local.SessionStore
import com.example.cloudide.data.local.TokenStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@Singleton
class SessionManager @Inject constructor(
    private val tokenStore: TokenStore,
    private val sessionStore: SessionStore,
    private val currentRepoStore: CurrentRepoStore,
    private val repoBuildDao: RepoBuildDao
) {
    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits when GitHub answered 401 for the stored token. */
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    suspend fun signOut() {
        tokenStore.clear()
        sessionStore.clear()
        currentRepoStore.clear()
        repoBuildDao.clearAll()
    }

    /** Called from the OkHttp thread (never the main thread). */
    fun onUnauthorized() {
        val hadToken = runBlocking { tokenStore.token.first() != null }
        if (!hadToken) return
        runBlocking { signOut() }
        _sessionExpired.tryEmit(Unit)
    }
}
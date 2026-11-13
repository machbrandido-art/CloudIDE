package com.example.cloudide.data.github

import com.example.cloudide.data.github.dto.toDomain
import com.example.cloudide.data.local.SessionStore
import com.example.cloudide.data.local.TokenStore
import com.example.cloudide.domain.AuthResult
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import retrofit2.HttpException

@Singleton
class AuthRepository @Inject constructor(
    private val api: GitHubApi,
    private val tokenStore: TokenStore,
    private val sessionStore: SessionStore,
    private val sessionManager: SessionManager
) {

    suspend fun connect(rawToken: String): AuthResult {
        val token = rawToken.trim()
        if (token.isEmpty()) return AuthResult.InvalidToken
        return try {
            val user = api.getUserWithToken("Bearer $token").toDomain()
            tokenStore.save(token)
            sessionStore.saveUsername(user.login)
            AuthResult.Success(user.login)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            if (e.code() == 401) AuthResult.InvalidToken else AuthResult.Unknown
        } catch (e: IOException) {
            AuthResult.NetworkError
        } catch (e: Exception) {
            AuthResult.Unknown
        }
    }

    suspend fun signOut() = sessionManager.signOut()
}
package com.example.cloudide.data.github

import com.example.cloudide.data.local.TokenStore
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")

        val hasExplicitAuth = original.header("Authorization") != null
        var tokenAttached = false
        if (!hasExplicitAuth) {
            val token = runBlocking { tokenStore.token.first() }
            if (token != null) {
                builder.header("Authorization", "Bearer $token")
                tokenAttached = true
            }
        }

        val response = chain.proceed(builder.build())
        if (response.code == 401 && tokenAttached) {
            sessionManager.onUnauthorized()
        }
        return response
    }
}
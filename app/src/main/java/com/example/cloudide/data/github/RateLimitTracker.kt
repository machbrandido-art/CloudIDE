package com.example.cloudide.data.github

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Headers

/** Remembers until when GitHub asked us to stop sending requests. */
@Singleton
class RateLimitTracker @Inject constructor() {

    private val _blockedUntil = MutableStateFlow(0L)
    val blockedUntil: StateFlow<Long> = _blockedUntil.asStateFlow()

    /** The reset time (epoch millis) while we are limited, otherwise null. */
    fun limitedUntil(): Long? =
        _blockedUntil.value.takeIf { it > System.currentTimeMillis() }

    fun onResponse(code: Int, headers: Headers) {
        val now = System.currentTimeMillis()
        val remaining = headers["x-ratelimit-remaining"]?.toIntOrNull()
        val resetMs = headers["x-ratelimit-reset"]?.toLongOrNull()?.times(1000)
        val retryMs = headers["retry-after"]?.toLongOrNull()?.times(1000)
        val limitedCode = code == 403 || code == 429

        val until: Long? = when {
            limitedCode && retryMs != null -> now + retryMs
            limitedCode && remaining == 0 && resetMs != null -> resetMs
            code == 429 -> now + DEFAULT_BLOCK_MS
            code != 401 && remaining == 0 && resetMs != null -> resetMs
            else -> null
        }
        if (until != null && until > now) {
            _blockedUntil.value = minOf(until, now + MAX_BLOCK_MS)
        }
    }

    private companion object {
        const val DEFAULT_BLOCK_MS = 60_000L
        const val MAX_BLOCK_MS = 60 * 60_000L
    }
}
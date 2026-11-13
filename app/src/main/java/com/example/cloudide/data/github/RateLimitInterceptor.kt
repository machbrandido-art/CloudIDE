package com.example.cloudide.data.github

import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

class RateLimitInterceptor @Inject constructor(
    private val tracker: RateLimitTracker
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        tracker.onResponse(response.code, response.headers)
        return response
    }
}
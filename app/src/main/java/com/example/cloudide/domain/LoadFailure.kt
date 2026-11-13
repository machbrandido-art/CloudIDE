package com.example.cloudide.domain

/** Typed failures of GitHub reads. The repository throws them, the ViewModel maps them to UI. */
sealed class LoadFailure : Exception() {
    class Offline : LoadFailure()
    class Unauthorized : LoadFailure()
    class RateLimited(val resetAtMillis: Long) : LoadFailure()
    class Http(val code: Int) : LoadFailure()
    class Unknown : LoadFailure()
}
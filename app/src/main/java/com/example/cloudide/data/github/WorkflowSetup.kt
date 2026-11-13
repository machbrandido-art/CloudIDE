package com.example.cloudide.data.github

import android.util.Base64
import com.example.cloudide.data.github.dto.PutContentRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

sealed class WorkflowCheckResult {
    data object Exists : WorkflowCheckResult()
    data object Missing : WorkflowCheckResult()
    data class Forbidden(val message: String) : WorkflowCheckResult()
    data class Error(val message: String) : WorkflowCheckResult()
}

sealed class WorkflowCreateResult {
    data object Created : WorkflowCreateResult()
    data class Forbidden(val message: String) : WorkflowCreateResult()
    data class Error(val message: String) : WorkflowCreateResult()
}

@Singleton
class WorkflowSetup @Inject constructor(
    private val api: GitHubApi,
    private val tracker: RateLimitTracker,
) {

    suspend fun check(owner: String, repo: String, branch: String): WorkflowCheckResult =
        withContext(Dispatchers.IO) {
            tracker.limitedUntil()?.let {
                return@withContext WorkflowCheckResult.Error("Rate limited. Try again later.")
            }
            try {
                val resp = api.getContents(owner, repo, WorkflowTemplate.FILE_PATH, branch)
                when {
                    resp.isSuccessful -> WorkflowCheckResult.Exists
                    resp.code() == 404 -> WorkflowCheckResult.Missing
                    resp.code() == 403 -> WorkflowCheckResult.Forbidden(forbiddenMessage())
                    else -> WorkflowCheckResult.Error("HTTP ${resp.code()}")
                }
            } catch (e: HttpException) {
                when (e.code()) {
                    404 -> WorkflowCheckResult.Missing
                    403 -> WorkflowCheckResult.Forbidden(forbiddenMessage())
                    else -> WorkflowCheckResult.Error("HTTP ${e.code()}")
                }
            } catch (e: Exception) {
                WorkflowCheckResult.Error(e.message ?: "Unknown error")
            }
        }

    suspend fun create(owner: String, repo: String, branch: String): WorkflowCreateResult =
        withContext(Dispatchers.IO) {
            tracker.limitedUntil()?.let {
                return@withContext WorkflowCreateResult.Error("Rate limited. Try again later.")
            }
            try {
                val body = PutContentRequest(
                    message = "Add CloudIDE build workflow",
                    content = Base64.encodeToString(
                        WorkflowTemplate.YAML.toByteArray(Charsets.UTF_8),
                        Base64.NO_WRAP,
                    ),
                    branch = branch,
                )
                val resp = api.putContents(owner, repo, WorkflowTemplate.FILE_PATH, body)
                when {
                    resp.isSuccessful -> WorkflowCreateResult.Created
                    resp.code() == 403 -> WorkflowCreateResult.Forbidden(forbiddenMessage())
                    else -> WorkflowCreateResult.Error("HTTP ${resp.code()}")
                }
            } catch (e: HttpException) {
                when (e.code()) {
                    403 -> WorkflowCreateResult.Forbidden(forbiddenMessage())
                    else -> WorkflowCreateResult.Error("HTTP ${e.code()}")
                }
            } catch (e: Exception) {
                WorkflowCreateResult.Error(e.message ?: "Unknown error")
            }
        }

    private fun forbiddenMessage(): String =
        "Your token is missing 'Workflows' permission. Add it at github.com/settings/tokens and reconnect."
}
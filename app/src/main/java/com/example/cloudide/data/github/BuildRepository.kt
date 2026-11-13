package com.example.cloudide.data.github

import com.example.cloudide.data.github.dto.DispatchRequestDto
import com.example.cloudide.data.github.dto.toBuildState
import com.example.cloudide.domain.BuildState
import com.example.cloudide.domain.BuildTriggerResult
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.coroutines.cancellation.CancellationException
import retrofit2.HttpException

@Singleton
class BuildRepository @Inject constructor(
    private val api: GitHubApi,
    private val tracker: RateLimitTracker,
) {

    suspend fun trigger(owner: String, repo: String, branch: String): BuildTriggerResult {
        tracker.limitedUntil()?.let {
            return BuildTriggerResult.Error("Rate limited. Try again later.")
        }

        val dispatchStart = Instant.now()
        try {
            val workflowId = WorkflowTemplate.FILE_PATH.substringAfterLast('/')
            val resp = api.dispatchWorkflow(owner, repo, workflowId, DispatchRequestDto(ref = branch))
            if (!resp.isSuccessful) {
                return when (resp.code()) {
                    403 -> BuildTriggerResult.Forbidden(
                        "Token needs 'Actions' & 'Workflows' write permission."
                    )
                    404 -> BuildTriggerResult.WorkflowMissing(
                        "Workflow file not found. Create it first."
                    )
                    else -> BuildTriggerResult.Error("HTTP ${resp.code()}")
                }
            }
        } catch (e: HttpException) {
            return when (e.code()) {
                403 -> BuildTriggerResult.Forbidden(
                    "Token needs 'Actions' & 'Workflows' write permission."
                )
                404 -> BuildTriggerResult.WorkflowMissing("Workflow file not found.")
                else -> BuildTriggerResult.Error("HTTP ${e.code()}")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return BuildTriggerResult.Error(e.message ?: "Unknown error")
        }

        val createdParam = ">=" + DateTimeFormatter.ISO_INSTANT.format(
            dispatchStart.truncatedTo(ChronoUnit.SECONDS).minusSeconds(2)
        )
        val deadline = System.currentTimeMillis() + 30_000L

        while (System.currentTimeMillis() < deadline) {
            tracker.limitedUntil()?.let {
                return BuildTriggerResult.Error("Rate limited during run discovery.")
            }
            try {
                val resp = api.getRunsByEvent(owner, repo, "workflow_dispatch", createdParam, perPage = 10)
                if (resp.isSuccessful) {
                    val run = resp.body()?.runs?.firstOrNull { it.headBranch == branch }
                    if (run != null) return BuildTriggerResult.Triggered(run.id)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
            delay(2_000L)
        }
        return BuildTriggerResult.NoNewRun
    }

    fun observe(owner: String, repo: String, runId: Long): Flow<BuildState> = flow {
        var errorStreak = 0
        while (true) {
            var emitted = false
            try {
                tracker.limitedUntil()?.let { throw IllegalStateException("rate limited") }
                val runResp = api.getRunById(owner, repo, runId)
                val jobsResp = api.getJobsForRun(owner, repo, runId)
                if (runResp.isSuccessful && jobsResp.isSuccessful) {
                    val run = runResp.body() ?: throw IllegalStateException("empty run body")
                    val jobs = jobsResp.body()?.jobs.orEmpty()
                    val state = run.toBuildState(jobs)
                    emit(state)
                    errorStreak = 0
                    emitted = true
                    if (state.isFinished) return@flow
                } else {
                    errorStreak++
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                errorStreak++
            }
            delay(if (emitted) 3_000L else minOf(errorStreak * 5_000L, 30_000L))
        }
    }

    suspend fun cancel(owner: String, repo: String, runId: Long): Result<Unit> = try {
        val r = api.cancelRun(owner, repo, runId)
        if (r.isSuccessful) Result.success(Unit) else Result.failure(Exception("HTTP ${r.code()}"))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun rerun(owner: String, repo: String, runId: Long): Result<Unit> = try {
        val r = api.rerunRun(owner, repo, runId)
        if (r.isSuccessful) Result.success(Unit) else Result.failure(Exception("HTTP ${r.code()}"))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
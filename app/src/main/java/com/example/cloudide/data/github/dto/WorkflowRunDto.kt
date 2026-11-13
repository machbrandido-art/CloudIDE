package com.example.cloudide.data.github.dto

import com.example.cloudide.domain.BuildStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkflowRunsDto(
    @SerialName("total_count") val totalCount: Int = 0,
    @SerialName("workflow_runs") val runs: List<WorkflowRunDto> = emptyList()
)

@Serializable
data class HeadCommitDto(val message: String? = null)

@Serializable
data class WorkflowRunDto(
    val id: Long,
    val status: String? = null,
    val conclusion: String? = null,
    @SerialName("head_branch") val headBranch: String? = null,
    @SerialName("display_title") val displayTitle: String? = null,
    @SerialName("head_commit") val headCommit: HeadCommitDto? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

fun WorkflowRunDto.toBuildStatus(): BuildStatus = when (status) {
    "completed" -> when (conclusion) {
        "success", "neutral" -> BuildStatus.Passed
        "failure", "timed_out", "startup_failure" -> BuildStatus.Failed
        "cancelled", "skipped", "stale" -> BuildStatus.Cancelled
        else -> BuildStatus.Pending
    }
    "in_progress" -> BuildStatus.Building
    else -> BuildStatus.Pending // queued, waiting, requested, pending
}

fun WorkflowRunDto.firstLineOfCommit(): String =
    (headCommit?.message ?: displayTitle).orEmpty().lines().firstOrNull().orEmpty().trim()

fun WorkflowRunDto.timeMillis(): Long = parseIsoMillis(updatedAt ?: createdAt)
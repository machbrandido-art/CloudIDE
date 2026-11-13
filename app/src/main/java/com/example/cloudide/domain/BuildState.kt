package com.example.cloudide.domain

data class BuildStep(
    val number: Int,
    val name: String,
    val status: String,
    val conclusion: String?,
    val startedAt: String?,
    val completedAt: String?,
) {
    val isDone: Boolean get() = status == "completed"
    val isRunning: Boolean get() = status == "in_progress"

    val uiStatus: BuildStatus get() = when {
        status == "in_progress" -> BuildStatus.Building
        status == "completed" && conclusion in setOf("success", "neutral") -> BuildStatus.Passed
        status == "completed" && conclusion in setOf("failure", "timed_out", "startup_failure") -> BuildStatus.Failed
        status == "completed" && conclusion in setOf("cancelled", "skipped", "stale") -> BuildStatus.Cancelled
        else -> BuildStatus.Pending
    }
}

data class BuildJob(
    val id: Long,
    val name: String,
    val status: String,
    val conclusion: String?,
    val startedAt: String?,
    val completedAt: String?,
    val steps: List<BuildStep>,
)

data class BuildRun(
    val id: Long,
    val status: String,
    val conclusion: String?,
    val headBranch: String,
    val createdAt: String?,
    val updatedAt: String?,
)

data class BuildState(
    val run: BuildRun,
    val jobs: List<BuildJob>,
) {
    val isFinished: Boolean get() = run.status == "completed"

    val uiStatus: BuildStatus get() = when {
        run.status == "in_progress" -> BuildStatus.Building
        run.status == "completed" -> when (run.conclusion) {
            "success", "neutral" -> BuildStatus.Passed
            "failure", "timed_out", "startup_failure" -> BuildStatus.Failed
            "cancelled", "skipped", "stale" -> BuildStatus.Cancelled
            else -> BuildStatus.Pending
        }
        else -> BuildStatus.Pending
    }
}

sealed class BuildTriggerResult {
    data class Triggered(val runId: Long) : BuildTriggerResult()
    data object NoNewRun : BuildTriggerResult()
    data class WorkflowMissing(val message: String) : BuildTriggerResult()
    data class Forbidden(val message: String) : BuildTriggerResult()
    data class Error(val message: String) : BuildTriggerResult()
}
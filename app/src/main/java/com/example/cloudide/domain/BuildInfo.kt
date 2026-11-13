package com.example.cloudide.domain

/** Latest workflow run of a repo, as shown on a project card. */
sealed interface BuildInfo {

    data object NoRuns : BuildInfo

    data class Run(
        val runId: Long,
        val status: BuildStatus,
        val commitMessage: String,
        val branch: String,
        val timeMillis: Long
    ) : BuildInfo
}
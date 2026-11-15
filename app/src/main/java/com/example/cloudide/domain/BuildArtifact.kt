package com.example.cloudide.domain

data class BuildArtifact(
    val id: Long,
    val name: String,
    val sizeInBytes: Long,
    val expired: Boolean,
)

data class BuildRunSummary(
    val id: Long,
    val status: BuildStatus,
    val branch: String,
    val commitMessage: String,
    val timeMillis: Long,
)

fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}
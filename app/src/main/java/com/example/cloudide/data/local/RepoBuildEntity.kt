package com.example.cloudide.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.cloudide.domain.BuildInfo
import com.example.cloudide.domain.BuildStatus

@Entity(tableName = "repo_builds")
data class RepoBuildEntity(
    @PrimaryKey val fullName: String,
    val hasRun: Boolean,
    val runId: Long,
    val status: String,
    val commitMessage: String,
    val branch: String,
    val runTimeMillis: Long,
    val etag: String?,
    val fetchedAt: Long
)

fun RepoBuildEntity.toDomain(): BuildInfo =
    if (!hasRun) {
        BuildInfo.NoRuns
    } else {
        BuildInfo.Run(
            runId = runId,
            status = runCatching { BuildStatus.valueOf(status) }.getOrDefault(BuildStatus.Pending),
            commitMessage = commitMessage,
            branch = branch,
            timeMillis = runTimeMillis
        )
    }
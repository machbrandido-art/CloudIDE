package com.example.cloudide.data.github.dto

import com.example.cloudide.domain.BuildJob
import com.example.cloudide.domain.BuildRun
import com.example.cloudide.domain.BuildState
import com.example.cloudide.domain.BuildStep
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DispatchRequestDto(val ref: String)

@Serializable
data class WorkflowStepDto(
    val name: String,
    val status: String,
    val conclusion: String? = null,
    val number: Int = 0,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
)

@Serializable
data class WorkflowJobDto(
    val id: Long,
    @SerialName("run_id") val runId: Long = 0,
    val name: String,
    val status: String,
    val conclusion: String? = null,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    val steps: List<WorkflowStepDto> = emptyList(),
)

@Serializable
data class WorkflowJobsResponseDto(
    @SerialName("total_count") val totalCount: Int = 0,
    val jobs: List<WorkflowJobDto> = emptyList(),
)

@Serializable
data class PutContentRequest(
    val message: String,
    val content: String,
    val branch: String? = null,
)

fun WorkflowRunDto.toBuildState(jobs: List<WorkflowJobDto>): BuildState = BuildState(
    run = BuildRun(
        id = id,
        status = status.orEmpty(),
        conclusion = conclusion,
        headBranch = headBranch.orEmpty(),
        createdAt = createdAt,
        updatedAt = updatedAt,
    ),
    jobs = jobs.map { j ->
        BuildJob(
            id = j.id,
            name = j.name,
            status = j.status,
            conclusion = j.conclusion,
            startedAt = j.startedAt,
            completedAt = j.completedAt,
            steps = j.steps.map { s ->
                BuildStep(
                    number = s.number,
                    name = s.name,
                    status = s.status,
                    conclusion = s.conclusion,
                    startedAt = s.startedAt,
                    completedAt = s.completedAt,
                )
            },
        )
    },
)
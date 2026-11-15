package com.example.cloudide.data.github.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArtifactsResponseDto(
    @SerialName("total_count") val totalCount: Int = 0,
    val artifacts: List<ArtifactDto> = emptyList(),
)

@Serializable
data class ArtifactDto(
    val id: Long,
    val name: String,
    @SerialName("size_in_bytes") val sizeInBytes: Long = 0,
    val expired: Boolean = false,
    @SerialName("archive_download_url") val archiveDownloadUrl: String = "",
)
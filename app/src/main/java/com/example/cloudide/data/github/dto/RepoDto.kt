package com.example.cloudide.data.github.dto

import com.example.cloudide.domain.Repo
import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OwnerDto(val login: String)

@Serializable
data class RepoDto(
    val id: Long,
    val name: String,
    val owner: OwnerDto,
    @SerialName("default_branch") val defaultBranch: String = "main",
    @SerialName("private") val isPrivate: Boolean = false,
    @SerialName("updated_at") val updatedAt: String? = null
)

fun RepoDto.toDomain(): Repo = Repo(
    id = id,
    owner = owner.login,
    name = name,
    defaultBranch = defaultBranch,
    isPrivate = isPrivate,
    updatedAtMillis = parseIsoMillis(updatedAt)
)

internal fun parseIsoMillis(value: String?): Long =
    if (value == null) 0L else runCatching { Instant.parse(value).toEpochMilli() }.getOrDefault(0L)
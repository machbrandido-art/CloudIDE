package com.example.cloudide.domain

data class Repo(
    val id: Long,
    val owner: String,
    val name: String,
    val defaultBranch: String,
    val isPrivate: Boolean,
    val updatedAtMillis: Long
) {
    val fullName: String get() = "$owner/$name"
}

data class RepoPage(
    val repos: List<Repo>,
    val hasMore: Boolean
)
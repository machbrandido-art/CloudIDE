package com.example.cloudide.domain

data class CurrentRepo(
    val owner: String,
    val name: String,
    val defaultBranch: String
) {
    val fullName: String get() = "$owner/$name"
}
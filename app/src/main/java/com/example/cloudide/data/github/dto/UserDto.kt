package com.example.cloudide.data.github.dto

import com.example.cloudide.domain.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val login: String,
    val id: Long = 0,
    val name: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

fun UserDto.toDomain(): User = User(login = login, avatarUrl = avatarUrl)
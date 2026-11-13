package com.example.cloudide.domain

sealed interface AuthResult {
    data class Success(val username: String) : AuthResult
    data object InvalidToken : AuthResult
    data object NetworkError : AuthResult
    data object Unknown : AuthResult
}
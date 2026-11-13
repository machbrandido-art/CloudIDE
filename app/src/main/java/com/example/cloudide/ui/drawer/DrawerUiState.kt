package com.example.cloudide.ui.drawer

data class DrawerUiState(
    val username: String,
    val isConnected: Boolean,
    val activeBuilds: Int,
    val recentRepos: List<String>
)
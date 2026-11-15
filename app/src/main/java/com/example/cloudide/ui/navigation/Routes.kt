package com.example.cloudide.ui.navigation

object Routes {
    const val Onboarding = "onboarding"
    const val Connect = "connect"
    const val Projects = "projects"
    const val Editor = "editor"
    const val Build = "build"
    const val BuildsList = "builds"
    const val Settings = "settings"
    const val Gallery = "gallery"

    val drawerRoutes = setOf(Projects, Editor, BuildsList, Settings, Gallery)
}
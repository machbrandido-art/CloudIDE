package com.example.cloudide.ui.navigation

object Routes {
    const val Onboarding = "onboarding"
    const val Connect = "connect"
    const val Projects = "projects"
    const val Editor = "editor"
    const val Build = "build"
    const val Settings = "settings"
    const val Gallery = "gallery"

    /** Routes where the navigation drawer is available. */
    val drawerRoutes = setOf(Projects, Editor, Build, Settings, Gallery)
}
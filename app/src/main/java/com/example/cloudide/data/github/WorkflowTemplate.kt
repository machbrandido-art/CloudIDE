package com.example.cloudide.data.github

object WorkflowTemplate {
    const val FILE_PATH = ".github/workflows/cloudide-build.yml"
    const val WORKFLOW_NAME = "CloudIDE Build"

    val YAML: String = """
name: CloudIDE Build
on: workflow_dispatch
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - uses: gradle/actions/setup-gradle@v4
      - name: Make gradlew executable
        run: chmod +x gradlew
      - name: Assemble Debug
        run: ./gradlew assembleDebug --stacktrace
      - uses: actions/upload-artifact@v4
        with:
          name: app-debug
          path: app/build/outputs/apk/debug/*.apk
          retention-days: 7
    """.trimIndent() + "\n"
}
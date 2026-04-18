package com.aicodequalityrisk.generator.model

data class GeneratedProject(
    val id: String,
    val promptName: String,
    val mode: GenerationMode,
    val variation: Int,
    val path: java.nio.file.Path
)
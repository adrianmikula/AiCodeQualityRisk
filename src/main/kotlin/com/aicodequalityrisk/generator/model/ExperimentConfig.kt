package com.aicodequalityrisk.generator.model

import kotlinx.serialization.Serializable

@Serializable
data class ExperimentConfig(
    val name: String,
    val promptTemplates: List<PromptTemplate>,
    val modes: List<GenerationMode>,
    val variationsPerPrompt: Int,
    val outputDir: String = "generated"
)
package com.aicodequalityrisk.generator.model

import kotlinx.serialization.Serializable

@Serializable
data class PromptTemplate(
    val name: String,
    val domain: String,
    val architecture: String,
    val features: List<String>,
    val iterationFeatures: List<String> = emptyList()
)
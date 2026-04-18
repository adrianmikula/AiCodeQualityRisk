package com.aicodequalityrisk.generator.model

import kotlinx.serialization.Serializable

@Serializable
enum class GenerationMode {
    SINGLE_SHOT,
    ITERATIVE
}
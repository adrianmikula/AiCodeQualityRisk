package com.aicodequalityrisk.generator

import com.aicodequalityrisk.generator.builder.PromptBuilder
import com.aicodequalityrisk.generator.llm.LlmCaller
import com.aicodequalityrisk.generator.model.ExperimentConfig
import com.aicodequalityrisk.generator.model.GenerationMode
import com.aicodequalityrisk.generator.model.PromptTemplate
import com.aicodequalityrisk.generator.parser.FileExtractor
import com.aicodequalityrisk.generator.runner.DetectionRunner
import com.aicodequalityrisk.generator.runner.ExperimentRunner
import com.aicodequalityrisk.generator.writer.ProjectWriter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.io.File

@Serializable
data class GeneratorConfigJson(
    val name: String,
    val outputDir: String = "generated",
    val model: String = "opencode/minimax",
    val maxRetries: Int = 3,
    val retryDelayMs: Long = 2000,
    val promptTemplates: List<PromptTemplateJson>,
    val modes: List<String>,
    val variationsPerPrompt: Int
)

@Serializable
data class PromptTemplateJson(
    val name: String,
    val domain: String,
    val architecture: String,
    val features: List<String>,
    val iterationFeatures: List<String> = emptyList()
)

fun main(args: Array<String>) {
    val configPath = args.firstOrNull() ?: "config/generator.json"
    val configFile = File(configPath)
    
    if (!configFile.exists()) {
        System.err.println("Config file not found: $configPath")
        System.exit(1)
    }
    
    val json = Json { ignoreUnknownKeys = true }
    val configJson = json.decodeFromString<GeneratorConfigJson>(configFile.readText())
    
    val config = ExperimentConfig(
        name = configJson.name,
        promptTemplates = configJson.promptTemplates.map { t ->
            PromptTemplate(
                name = t.name,
                domain = t.domain,
                architecture = t.architecture,
                features = t.features,
                iterationFeatures = t.iterationFeatures
            )
        },
        modes = configJson.modes.map { 
            GenerationMode.valueOf(it) 
        },
        variationsPerPrompt = configJson.variationsPerPrompt,
        outputDir = configJson.outputDir
    )

    val llm = LlmCaller(
        model = configJson.model,
        maxRetries = configJson.maxRetries,
        retryDelayMs = configJson.retryDelayMs
    )
    val promptBuilder = PromptBuilder()
    val fileExtractor = FileExtractor()
    val projectWriter = ProjectWriter()
    val detectionRunner = DetectionRunner()

    val runner = ExperimentRunner(
        llm = llm,
        promptBuilder = promptBuilder,
        fileExtractor = fileExtractor,
        projectWriter = projectWriter,
        detectionRunner = detectionRunner
    )

    println("Starting AI Code Generation Experiment...")
    println("Config: ${config.promptTemplates.size} templates × ${config.modes.size} modes × ${config.variationsPerPrompt} variations")
    println()

    runner.run(config)

    println("Experiment complete!")
}
package com.aicodequalityrisk.generator.runner

import com.aicodequalityrisk.generator.builder.PromptBuilder
import com.aicodequalityrisk.generator.llm.LlmCaller
import com.aicodequalityrisk.generator.model.ExperimentConfig
import com.aicodequalityrisk.generator.model.GenerationMode
import com.aicodequalityrisk.generator.model.PromptTemplate
import com.aicodequalityrisk.generator.parser.ExtractedFile
import com.aicodequalityrisk.generator.parser.FileExtractor
import com.aicodequalityrisk.generator.writer.ProjectWriter
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

class ExperimentRunner(
    private val llm: LlmCaller,
    private val promptBuilder: PromptBuilder,
    private val fileExtractor: FileExtractor,
    private val projectWriter: ProjectWriter,
    private val detectionRunner: DetectionRunner
) {
    fun run(config: ExperimentConfig) {
        val outputDir = File(config.outputDir)
        outputDir.mkdirs()
        
        val csvFile = File(outputDir, "results.csv")
        csvFile.writeText("project_id,mode,prompt_name,variation,duplicate_string_literals,duplicate_number_literals,duplicate_method_calls,duplicate_method_count,max_similarity_score,total_loc\n")

        config.promptTemplates.forEach { template ->
            config.modes.forEach { mode ->
                repeat(config.variationsPerPrompt) { variation ->
                    val projectId = UUID.randomUUID().toString()
                    val projectPath = outputDir.toPath().resolve(projectId)

                    println("Generating project $projectId: ${template.name}, $mode, variation ${variation + 1}")

                    try {
                        generateProject(template, mode, projectPath)
                        val result = detectionRunner.analyze(projectPath)
                        
                        val csvLine = listOf(
                            projectId,
                            mode.name,
                            template.name,
                            (variation + 1).toString(),
                            result.duplicateStringLiterals,
                            result.duplicateNumberLiterals,
                            result.duplicateMethodCalls,
                            result.duplicateMethodCount,
                            result.maxSimilarityScore,
                            result.totalLoc
                        ).joinToString(",")
                        
                        csvFile.appendText("$csvLine\n")
                        println("  Result: duplicate strings=${result.duplicateStringLiterals}, loc=${result.totalLoc}")
                    } catch (e: Exception) {
                        System.err.println("  Failed: ${e.message}")
                        csvFile.appendText("${projectId},${mode.name},${template.name},${variation + 1},-1,-1,-1,-1,-1,-1\n")
                    }
                }
            }
        }

        println("\nResults written to ${csvFile.absolutePath}")
    }

    private fun generateProject(template: PromptTemplate, mode: GenerationMode, projectPath: Path) {
        val basePrompt = promptBuilder.buildBasePrompt(template)
        val baseResponse = llm.generate(basePrompt)
        val baseFiles = fileExtractor.extractFiles(baseResponse)
        
        if (baseFiles.isEmpty()) {
            throw IllegalStateException("No files extracted from LLM response")
        }

        projectWriter.writeProject(projectPath, baseFiles)

        if (mode == GenerationMode.ITERATIVE && template.iterationFeatures.isNotEmpty()) {
            val existingFilesList = Files.walk(projectPath)
                .filter { it.toString().endsWith(".java") }
                .limit(3)
                .toList()
            val existingFiles = existingFilesList.joinToString("\n") { it.toFile().readText().take(2000) }

            template.iterationFeatures.forEach { feature ->
                val iterPrompt = promptBuilder.buildIterationPrompt(existingFiles, feature)
                val iterResponse = llm.generate(iterPrompt)
                val iterFiles = fileExtractor.extractFiles(iterResponse)
                
                if (iterFiles.isNotEmpty()) {
                    projectWriter.writeProject(projectPath, iterFiles)
                }
            }
        }
    }
}
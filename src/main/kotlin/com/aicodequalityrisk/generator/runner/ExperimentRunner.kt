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
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class ExperimentRunner(
    private val llm: LlmCaller,
    private val promptBuilder: PromptBuilder,
    private val fileExtractor: FileExtractor,
    private val projectWriter: ProjectWriter,
    private val detectionRunner: DetectionRunner
) {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    
    private fun logProgress(stage: String, message: String) {
        val timestamp = LocalTime.now().format(timeFormatter)
        println("[$timestamp] [$stage] $message")
    }
    fun run(config: ExperimentConfig) {
        logProgress("INIT", "Starting experiment: ${config.promptTemplates.size} templates × ${config.modes.size} modes × ${config.variationsPerPrompt} variations")
        
        val outputDir = File(config.outputDir)
        outputDir.mkdirs()
        
        val csvFile = File(outputDir, "results.csv")
        csvFile.writeText("project_id,mode,prompt_name,variation,duplicate_string_literals,duplicate_number_literals,duplicate_method_calls,duplicate_method_count,max_similarity_score,total_loc\n")
        logProgress("INIT", "Output directory: ${outputDir.absolutePath}")

        var totalProjects = 0
        var successfulProjects = 0
        
        config.promptTemplates.forEachIndexed { templateIdx, template ->
            logProgress("INIT", "Processing template ${templateIdx + 1}/${config.promptTemplates.size}: ${template.name}")
            
            config.modes.forEachIndexed { modeIdx, mode ->
                logProgress("INIT", "  Mode ${modeIdx + 1}/${config.modes.size}: $mode")
                
                repeat(config.variationsPerPrompt) { variation ->
                    totalProjects++
                    val projectId = UUID.randomUUID().toString().take(8)
                    val projectPath = outputDir.toPath().resolve(projectId)

                    logProgress("INIT", "    Project $totalProjects: $projectId (${template.name}, $mode, variation ${variation + 1})")

                    try {
                        generateProject(template, mode, projectPath)
                        
                        logProgress("WRITE", "    Verifying generated files...")
                        val existingFiles = Files.walk(projectPath)
                            .filter { it.toString().endsWith(".java") }
                            .toList()
                        
                        if (existingFiles.isEmpty()) {
                            throw IllegalStateException("No Java files written")
                        }
                        logProgress("WRITE", "    Found ${existingFiles.size} Java files")
                        
                        logProgress("ANALYZE", "    Running code quality analysis...")
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
                        successfulProjects++
                        logProgress("DONE", "    ✓ Result: dup_strings=${result.duplicateStringLiterals}, dup_nums=${result.duplicateNumberLiterals}, dup_calls=${result.duplicateMethodCalls}, similar_methods=${result.duplicateMethodCount}, max_sim=${String.format("%.2f", result.maxSimilarityScore)}, loc=${result.totalLoc}")
                    } catch (e: Exception) {
                        logProgress("ERROR", "    ✗ Failed: ${e.message}")
                        e.printStackTrace()
                        csvFile.appendText("${projectId},${mode.name},${template.name},${variation + 1},-1,-1,-1,-1,-1,-1\n")
                    }
                }
            }
        }

        logProgress("DONE", "Experiment complete: $successfulProjects/$totalProjects projects successful")
        logProgress("DONE", "Results written to ${csvFile.absolutePath}")
    }

    private fun generateProject(template: PromptTemplate, mode: GenerationMode, projectPath: Path) {
        logProgress("PROMPT", "    Building base prompt...")
        val basePrompt = promptBuilder.buildBasePrompt(template)
        logProgress("PROMPT", "    Base prompt length: ${basePrompt.length} chars")
        
        logProgress("LLM", "    Calling LLM for base generation...")
        val baseResponse = try {
            llm.generate(basePrompt)
        } catch (e: Exception) {
            logProgress("ERROR", "    LLM call failed: ${e.message}")
            throw e
        }
        logProgress("LLM", "    LLM response received: ${baseResponse.length} chars")
        
        logProgress("EXTRACT", "    Extracting files from response...")
        val baseFiles = fileExtractor.extractFiles(baseResponse)
        logProgress("EXTRACT", "    Extracted ${baseFiles.size} files")
        
        if (baseFiles.isEmpty()) {
            logProgress("ERROR", "    No files extracted. Response preview:")
            System.err.println(baseResponse.take(1000))
            throw IllegalStateException("No files extracted from LLM response")
        }

        logProgress("WRITE", "    Writing ${baseFiles.size} files to disk...")
        projectWriter.writeProject(projectPath, baseFiles)
        logProgress("WRITE", "    Files written to: $projectPath")

        if (mode == GenerationMode.ITERATIVE && template.iterationFeatures.isNotEmpty()) {
            logProgress("INIT", "    Starting iterative generation (${template.iterationFeatures.size} features)...")
            val existingFilesList = Files.walk(projectPath)
                .filter { it.toString().endsWith(".java") }
                .limit(3)
                .toList()
            val existingFiles = existingFilesList.joinToString("\n") { it.toFile().readText().take(2000) }

            template.iterationFeatures.forEachIndexed { idx, feature ->
                logProgress("PROMPT", "      Iteration ${idx + 1}/${template.iterationFeatures.size}: $feature")
                val iterPrompt = promptBuilder.buildIterationPrompt(existingFiles, feature)
                
                logProgress("LLM", "      Calling LLM for iteration...")
                val iterResponse = llm.generate(iterPrompt)
                logProgress("LLM", "      Response received: ${iterResponse.length} chars")
                
                logProgress("EXTRACT", "      Extracting files...")
                val iterFiles = fileExtractor.extractFiles(iterResponse)
                logProgress("EXTRACT", "      Extracted ${iterFiles.size} files")
                
                if (iterFiles.isNotEmpty()) {
                    logProgress("WRITE", "      Writing ${iterFiles.size} files...")
                    projectWriter.writeProject(projectPath, iterFiles)
                }
            }
            logProgress("INIT", "    Iterative generation complete")
        }
    }
}
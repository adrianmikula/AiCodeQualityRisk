package com.aicodequalityrisk.plugin.mcp

import com.aicodequalityrisk.plugin.model.RiskResult
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.BufferedReader
import java.io.BufferedWriter

@Service(Service.Level.PROJECT)
class McpServerService(
    private val project: Project
) : Disposable {

    private val logger = Logger.getInstance(McpServerService::class.java)
    private var serverJob: Job? = null
    private val json = Json { ignoreUnknownKeys = true }
    private val dataDir: File
        get() = File(project.basePath, DATA_DIR_NAME).also { it.mkdirs() }

    private val latestScanFile: File
        get() = File(dataDir, LATEST_SCAN_FILE)

    @Volatile
    private var analyzedFileCount = 0

    init {
        startMcpServer()
        CoroutineScope(Dispatchers.IO).launch {
            refreshFileCount()
        }
    }

    fun getAnalyzedFileCount(): Int = analyzedFileCount

    private fun refreshFileCount() {
        val scansDir = File(dataDir, SCANS_DIR_NAME)
        val count = scansDir.listFiles()?.count { it.extension == "json" } ?: 0
        analyzedFileCount = count
        logger.debug("Initialized file count: $analyzedFileCount")
    }

    private fun startMcpServer() {
        try {
            logger.info("Starting MCP Server...")
            
            serverJob = CoroutineScope(Dispatchers.IO).launch {
                val input = BufferedReader(InputStreamReader(System.`in`))
                val output = BufferedWriter(OutputStreamWriter(System.out))
                
                var initialized = false
                
                try {
                    while (true) {
                        val line = input.readLine() ?: break
                        if (line.isBlank()) continue
                        
                        try {
                            val request = json.parseToJsonElement(line).jsonObject
                            val id = request["id"]?.jsonPrimitive?.content
                            val method = request["method"]?.jsonPrimitive?.content
                            
                            when (method) {
                                "initialize" -> {
                                    val response = buildJsonObject {
                                        put("jsonrpc", "2.0")
                                        put("id", id ?: "0")
                                        put("result", buildJsonObject {
                                            put("protocolVersion", "2024-11-05")
                                            put("capabilities", buildJsonObject {
                                                put("tools", buildJsonObject {
                                                    put("listChanged", true)
                                                })
                                            })
                                            put("serverInfo", buildJsonObject {
                                                put("name", "ai-code-quality-risk")
                                                put("version", "1.0.0")
                                            })
                                        })
                                    }
                                    output.write(response.toString())
                                    output.newLine()
                                    output.flush()
                                    initialized = true
                                }
                                
                                "tools/list" -> {
                                    val response = buildJsonObject {
                                        put("jsonrpc", "2.0")
                                        put("id", id ?: "0")
                                        put("result", buildJsonObject {
                                            put("tools", buildJsonArray {
                                                add(buildJsonObject {
                                                    put("name", "get_latest_scan")
                                                    put("description", "Get the latest code quality scan results including risk scores and findings")
                                                    put("inputSchema", buildJsonObject {
                                                        put("type", "object")
                                                        put("properties", buildJsonObject { })
                                                    })
                                                })
                                                add(buildJsonObject {
                                                    put("name", "get_scan_for_file")
                                                    put("description", "Get code quality scan results for a specific file")
                                                    put("inputSchema", buildJsonObject {
                                                        put("type", "object")
                                                        put("properties", buildJsonObject {
                                                            put("filePath", buildJsonObject {
                                                                put("type", "string")
                                                                put("description", "The file path to get scan results for")
                                                            })
                                                        })
                                                        put("required", buildJsonArray { add(JsonPrimitive("filePath")) })
                                                    })
                                                })
                                                add(buildJsonObject {
                                                    put("name", "get_all_scans")
                                                    put("description", "Get all available code quality scan results")
                                                    put("inputSchema", buildJsonObject {
                                                        put("type", "object")
                                                        put("properties", buildJsonObject { })
                                                    })
                                                })
                                            })
                                        })
                                    }
                                    output.write(response.toString())
                                    output.newLine()
                                    output.flush()
                                }
                                
                                "tools/call" -> {
                                    val params = request["params"]?.jsonObject
                                    val toolName = params?.get("name")?.jsonPrimitive?.content
                                    val toolArgs = params?.get("arguments")?.jsonObject
                                    
                                    val result = when (toolName) {
                                        "get_latest_scan" -> handleGetLatestScan()
                                        "get_scan_for_file" -> handleGetScanForFile(toolArgs)
                                        "get_all_scans" -> handleGetAllScans()
                                        else -> "Unknown tool: $toolName"
                                    }
                                    
                                    val response = buildJsonObject {
                                        put("jsonrpc", "2.0")
                                        put("id", id ?: "0")
                                        put("result", buildJsonObject {
                                            put("content", buildJsonArray {
                                                add(buildJsonObject {
                                                    put("type", "text")
                                                    put("text", result)
                                                })
                                            })
                                        })
                                    }
                                    output.write(response.toString())
                                    output.newLine()
                                    output.flush()
                                }
                                
                                else -> {
                                    if (initialized) {
                                        logger.debug("Received method: $method")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            logger.warn("Error processing request", e)
                        }
                    }
                } catch (e: Exception) {
                    logger.error("MCP server error", e)
                } finally {
                    input.close()
                    output.close()
                }
            }
            
            logger.info("MCP Server started successfully")
            
        } catch (e: Exception) {
            logger.error("Failed to start MCP server", e)
        }
    }

    private fun handleGetLatestScan(): String {
        val result = readLatestScan()
        return if (result != null) {
            formatScanResult(result)
        } else {
            "No scan results available. Please run a scan first using the plugin."
        }
    }

    private fun handleGetScanForFile(args: JsonObject?): String {
        val filePath = args?.get("filePath")?.jsonPrimitive?.content
        return if (filePath == null) {
            "Error: filePath parameter is required"
        } else {
            val result = readScanForFile(filePath)
            if (result != null) {
                formatScanResult(result)
            } else {
                "No scan results found for file: $filePath"
            }
        }
    }

    private fun handleGetAllScans(): String {
        val results = readAllScans()
        return if (results.isEmpty()) {
            "No scan results available. Please run a scan first using the plugin."
        } else {
            buildString {
                appendLine("=== All Scan Results ===")
                results.forEach { (file, result) ->
                    appendLine()
                    appendLine("File: $file")
                    appendLine(formatScanResult(result))
                }
            }
        }
    }

    private fun readLatestScan(): RiskResult? {
        return try {
            if (latestScanFile.exists()) {
                val jsonStr = latestScanFile.readText()
                RiskResult.fromJson(jsonStr)
            } else null
        } catch (e: Exception) {
            logger.warn("Failed to read latest scan", e)
            null
        }
    }

    private fun readScanForFile(filePath: String): RiskResult? {
        val allScansDir = File(dataDir, SCANS_DIR_NAME)
        return try {
            allScansDir.listFiles()?.firstOrNull { it.nameWithoutExtension == filePath.replace("/", "_") }?.let { file ->
                RiskResult.fromJson(file.readText())
            } ?: readLatestScan()?.takeIf { it.sourceFilePath == filePath }
        } catch (e: Exception) {
            logger.warn("Failed to read scan for file: $filePath", e)
            null
        }
    }

    private fun readAllScans(): Map<String, RiskResult> {
        val result = mutableMapOf<String, RiskResult>()
        readLatestScan()?.let { result["latest"] = it }

        val allScansDir = File(dataDir, SCANS_DIR_NAME)
        allScansDir.listFiles()?.forEach { file ->
            try {
                val riskResult = RiskResult.fromJson(file.readText())
                riskResult.sourceFilePath?.let { path ->
                    result[path] = riskResult
                }
            } catch (e: Exception) {
                logger.warn("Failed to read scan file: ${file.name}", e)
            }
        }
        return result
    }

    private fun formatScanResult(result: RiskResult): String {
        return buildString {
            appendLine("=== SlopGuard [formerly EntropyGuard] Scan Results ===")
            appendLine()
            appendLine("Risk Score: ${result.score}/100")
            appendLine()
            appendLine("Category Scores:")
            appendLine("  - Complexity: ${result.complexityScore}")
            appendLine("  - Duplication: ${result.duplicationScore}")
            appendLine("  - Performance: ${result.performanceScore}")
            appendLine("  - Security: ${result.securityScore}")
            appendLine("  - Boilerplate Bloat: ${result.boilerplateBloatScore}")
            appendLine("  - Verbose Comment Spam: ${result.verboseCommentSpamScore}")
            appendLine("  - Over-Defensive Programming: ${result.overDefensiveProgrammingScore}")
            appendLine("  - Magic Numbers: ${result.magicNumbersScore}")
            appendLine("  - Complex Boolean Logic: ${result.complexBooleanLogicScore}")
            appendLine("  - Deep Nesting: ${result.deepNestingScore}")
            appendLine("  - Verbose Logging: ${result.verboseLoggingScore}")
            appendLine("  - Poor Naming: ${result.poorNamingScore}")
            appendLine("  - Framework Misuse: ${result.frameworkMisuseScore}")
            appendLine("  - Excessive Documentation: ${result.excessiveDocumentationScore}")
            appendLine("  - Null Returns: ${result.nullReturnScore}")
            appendLine()

            if (result.findings.isNotEmpty()) {
                appendLine("Findings (${result.findings.size}):")
                result.findings.forEachIndexed { index, finding ->
                    appendLine("  ${index + 1}. [${finding.severity}] ${finding.title}")
                    appendLine("     ${finding.detail}")
                    finding.filePath?.let { path -> appendLine("     File: $path") }
                    finding.lineNumber?.let { line -> appendLine("     Line: $line") }
                }
            } else {
                appendLine("No findings detected.")
            }

            result.sourceFilePath?.let { path ->
                appendLine()
                appendLine("Source File: $path")
            }

            if (result.explanations.isNotEmpty()) {
                appendLine()
                appendLine("Explanations:")
                result.explanations.forEach { explanation -> appendLine("  - $explanation") }
            }
        }
    }

    fun saveLatestScan(result: RiskResult) {
        try {
            latestScanFile.writeText(result.toJson())
            logger.debug("Saved latest scan to ${latestScanFile.absolutePath}")
        } catch (e: Exception) {
            logger.warn("Failed to save latest scan", e)
        }
    }

    fun saveScanForFile(result: RiskResult) {
        result.sourceFilePath?.let { filePath ->
            try {
                val allScansDir = File(dataDir, SCANS_DIR_NAME).also { it.mkdirs() }
                val sanitizedName = filePath.replace("/", "_").replace("\\", "_")
                val scanFile = File(allScansDir, "$sanitizedName.json")
                scanFile.writeText(result.toJson())
                analyzedFileCount++
                logger.debug("Saved scan for file ${scanFile.absolutePath}, count=$analyzedFileCount")
            } catch (e: Exception) {
                logger.warn("Failed to save scan for file: $filePath", e)
            }
        }
    }

    override fun dispose() {
        serverJob?.cancel()
        logger.info("McpServerService disposed")
    }

    companion object {
        const val DATA_DIR_NAME = ".aicodequalityrisk"
        const val LATEST_SCAN_FILE = "latest-scan.json"
        const val SCANS_DIR_NAME = "scans"

        fun getInstance(project: Project): McpServerService {
            return project.service()
        }
    }
}
package com.aicodequalityrisk.generator.llm

import java.io.BufferedReader
import java.io.InputStreamReader

class LlmCaller(
    private val model: String = "opencode/big-pickle",
    private val maxRetries: Int = 3,
    private val retryDelayMs: Long = 2000
) {
    fun generate(prompt: String): String {
        var lastError: Exception? = null
        
        for (attempt in 1..maxRetries) {
            try {
                return callOpenCode(prompt)
            } catch (e: Exception) {
                lastError = e
                System.err.println("Attempt $attempt failed: ${e.message}")
                if (attempt < maxRetries) {
                    Thread.sleep(retryDelayMs)
                }
            }
        }
        throw lastError ?: IllegalStateException("LLM call failed after $maxRetries attempts")
    }

    private fun callOpenCode(prompt: String): String {
        val escapedPrompt = prompt.replace("'", "'\\''")
        val fullCommand = "PATH=\$HOME/.npm-global/bin:\$PATH opencode run --yes --dangerously-skip-permissions --model $model '$escapedPrompt'"
        System.err.println("DEBUG: $fullCommand")
        
        val processBuilder = ProcessBuilder()
        processBuilder.command("bash", "-c", fullCommand)
        
        val process = processBuilder.start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val errorReader = BufferedReader(InputStreamReader(process.errorStream))
        val output = StringBuilder()
        
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            output.appendLine(line)
        }
        
        val errorOutput = StringBuilder()
        while (errorReader.readLine().also { line = it } != null) {
            errorOutput.appendLine(line)
        }
        
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            System.err.println("STDERR: $errorOutput")
            throw IllegalStateException("opencode failed with exit code $exitCode: $output")
        }
        
        return output.toString()
    }
}
package com.aicodequalityrisk.generator.llm

import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader

class LlmCaller(
    private val model: String = "opencode/big-pickle",
    private val maxRetries: Int = 3,
    private val retryDelayMs: Long = 3000
) {
    fun generate(prompt: String): String {
        repeat(maxRetries) { attempt ->
            try {
                System.err.println("Calling LLM...")
                val result = callOpenCode(prompt)
                if (result.isNotBlank() && result.length > 50) {
                    System.err.println("Success: ${result.length} chars")
                    return result
                }
                System.err.println("Empty response")
            } catch (e: Exception) {
                System.err.println("Failed: ${e.message}")
            }
            Thread.sleep(retryDelayMs)
        }
        throw IllegalStateException("All attempts failed")
    }

    private fun callOpenCode(prompt: String): String {
        val home = System.getenv("HOME") ?: "/home/adrian"
        val binPath = "$home/.npm-global/bin"
        
        val script = File.createTempFile("call_llm_", ".sh")
        script.writeText("""
            |#!/bin/bash
            |cd /tmp
            |exec $binPath/opencode run --attach http://127.0.0.1:36361 --password d242bac4-b1c9-49f0-a292-a84a4b685c2b --model $model "$prompt"
        """.trimMargin())
        script.setExecutable(true)
        
        val pb = ProcessBuilder()
        pb.command(script.absolutePath)
        pb.directory(File("/tmp"))
        pb.redirectErrorStream(true)
        
        val proc = pb.start()
        val output = proc.inputStream.bufferedReader().readText()
        proc.waitFor()
        
        script.delete()
        
        if (output.contains("Error:", ignoreCase = true) || !output.contains("file")) {
            throw IllegalStateException("LLM error")
        }
        
        return output
    }
}
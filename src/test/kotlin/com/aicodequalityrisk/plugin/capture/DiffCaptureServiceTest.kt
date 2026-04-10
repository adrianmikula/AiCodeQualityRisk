package com.aicodequalityrisk.plugin.capture

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiffCaptureServiceTest {
    private val service = DiffCaptureService()

    @Test
    fun `synthetic diff should include added file lines`() {
        val diff = service.syntheticDiff("src/Test.kt", "fun main() {\n println(\"hi\")\n}")

        assertTrue(diff.contains("diff --git"))
        assertTrue(diff.contains("+fun main() {"))
        assertTrue(diff.contains("+ println(\"hi\")"))
    }

    @Test
    fun `buildDiff should prefer staged diff when available`() {
        val captureService = DiffCaptureService { _, args ->
            when {
                args.contains("--staged") -> "STAGED_DIFF"
                else -> "UNSTAGED_DIFF"
            }
        }

        val result = captureService.buildDiff(
            projectPath = "/tmp/project",
            filePath = "/tmp/project/src/Test.kt",
            snapshot = "content"
        )

        assertEquals("STAGED_DIFF", result)
    }

    @Test
    fun `buildDiff should fallback to unstaged when staged is empty`() {
        val captureService = DiffCaptureService { _, args ->
            when {
                args.contains("--staged") -> ""
                else -> "UNSTAGED_DIFF"
            }
        }

        val result = captureService.buildDiff(
            projectPath = "/tmp/project",
            filePath = "/tmp/project/src/Test.kt",
            snapshot = "content"
        )

        assertEquals("UNSTAGED_DIFF", result)
    }

    @Test
    fun `buildDiff should fallback to synthetic when git diffs are empty`() {
        val captureService = DiffCaptureService { _, _ -> "" }

        val result = captureService.buildDiff(
            projectPath = "/tmp/project",
            filePath = "/tmp/project/src/Test.kt",
            snapshot = "line1"
        )

        assertTrue(result.startsWith("diff --git"))
        assertTrue(result.contains("+line1"))
    }
}

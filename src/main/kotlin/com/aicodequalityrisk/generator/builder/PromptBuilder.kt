package com.aicodequalityrisk.generator.builder

import com.aicodequalityrisk.generator.model.PromptTemplate

class PromptBuilder {
    fun buildBasePrompt(template: PromptTemplate): String {
        return buildString {
            appendLine("You are generating a Java Spring Boot project.")
            appendLine()
            appendLine("Domain: ${template.domain}")
            appendLine("Architecture: ${template.architecture}")
            appendLine("Features: ${template.features.joinToString(", ")}")
            appendLine()
            appendLine("Requirements:")
            appendLine("- Write complete, working code")
            appendLine("- Use realistic project structure")
            appendLine("- Do not explain, only output files")
            appendLine()
            appendLine("Output format:")
            appendLine("""<file path="src/main/java/com/example/App.java">""")
            appendLine("...code...")
            appendLine("</file>")
        }.trimEnd()
    }

    fun buildIterationPrompt(existingContext: String, feature: String): String {
        return buildString {
            appendLine("Modify the existing project:")
            appendLine()
            appendLine(existingContext)
            appendLine()
            appendLine("Add feature: $feature")
            appendLine()
            appendLine("Requirements:")
            appendLine("- Maintain consistency with existing code")
            appendLine("- Avoid rewriting unchanged files")
            appendLine("- Output only changed or new files")
            appendLine()
            appendLine("Output format:")
            appendLine("""<file path="src/main/java/com/example/Modified.java">""")
            appendLine("...code...")
            appendLine("</file>")
        }.trimEnd()
    }
}
package com.aicodequalityrisk.generator.builder

import com.aicodequalityrisk.generator.model.PromptTemplate

class PromptBuilder {
    fun buildBasePrompt(template: PromptTemplate): String {
        return buildString {
            appendLine("Generate a complete Java Spring Boot project.")
            appendLine()
            appendLine("Domain: ${template.domain}")
            appendLine("Architecture: ${template.architecture}")
            appendLine("Features: ${template.features.joinToString(", ")}")
            appendLine()
            appendLine("CRITICAL: Output ONLY the files wrapped in XML tags. No explanations.")
            appendLine()
            appendLine("Output format (MUST follow exactly):")
            appendLine("""<file path="src/main/java/com/example/App.java">""")
            appendLine("package com.example;")
            appendLine("// code here")
            appendLine("</file>")
            appendLine("""<file path="src/main/java/com/example/Task.java">""")
            appendLine("// code here")
            appendLine("</file>")
            appendLine("""<file path="src/main/resources/application.properties">""")
            appendLine("server.port=8080")
            appendLine("</file>")
        }.trimEnd()
    }

    fun buildIterationPrompt(existingContext: String, feature: String): String {
        return buildString {
            appendLine("Modify the existing project to add: $feature")
            appendLine()
            appendLine("Existing code:")
            appendLine(existingContext.take(1000))
            appendLine()
            appendLine("CRITICAL: Output ONLY the modified files in XML format.")
            appendLine("""<file path="src/main/java/com/example/TaskService.java">""")
            appendLine("// updated code")
            appendLine("</file>")
        }.trimEnd()
    }
}
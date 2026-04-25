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
            appendLine("Follow standard Java coding conventions and best practices.")
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
            appendLine("""<file path="src/main/java/com/example/TaskRepository.java">""")
            appendLine("// code here")
            appendLine("</file>")
            appendLine("""<file path="src/main/java/com/example/TaskService.java">""")
            appendLine("// code here")
            appendLine("</file>")
            appendLine("""<file path="src/main/java/com/example/TaskController.java">""")
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
            appendLine("Follow standard Java coding conventions and maintain consistency with existing code.")
            appendLine()
            appendLine("Existing code:")
            appendLine(existingContext.take(1500))
            appendLine()
            appendLine("CRITICAL: Output ONLY the modified files in XML format.")
            appendLine("Maintain and extend the existing codebase with the new feature.")
            appendLine()
            appendLine("""<file path="src/main/java/com/example/TaskService.java">""")
            appendLine("// updated code")
            appendLine("</file>")
            appendLine("""<file path="src/main/java/com/example/NewFeature.java">""")
            appendLine("// new feature code")
            appendLine("</file>")
        }.trimEnd()
    }
}
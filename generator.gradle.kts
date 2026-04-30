import java.io.File
import java.util.UUID

tasks.register<Exec>("runGenerator") {
    group = "application"
    description = "Run the AI code generation experiment"

    doFirst {
        val outDir = File("workspace/generated")
        outDir.mkdirs()

        val csvFile = File(outDir, "results.csv")
        csvFile.writeText(
            "project_id,mode,prompt_name,variation,duplicate_string_literals,duplicate_number_literals,duplicate_method_calls,duplicate_method_count,max_similarity_score,total_loc\n"
        )

            listOf(
                Triple("crud_app", "Task Management", "CRUD operations, H2 database, JPA"),
                Triple("microservice", "E-commerce", "Product service, Order service, RabbitMQ"),
                Triple("clean_architecture", "Banking", "Account domain, Transaction use cases, Repository")
            ).forEach { triple ->
                val name = triple.first
                val domain = triple.second
                val features = triple.third
                val projectId = UUID.randomUUID().toString()
                val projectDir = File(outDir, projectId)
                projectDir.mkdirs()

                val prompt = buildString {
                    appendLine("Generate a complete Java Spring Boot project.")
                    appendLine()
                    appendLine("Domain: $domain")
                    appendLine("Features: $features")
                    appendLine()
                    appendLine("CRITICAL: Output ONLY the files wrapped in XML tags. No explanations.")
                    appendLine()
                    appendLine("Output format:")
                    appendLine("<file path=\"<project_dir>/src/main/java/com.example/App.java\">")
                    appendLine("package com.example;")
                    appendLine("</file>")
                    appendLine("<file path=\"<project_dir>/src/main/java/com.example/Task.java\">")
                    appendLine("package com.example;")
                    appendLine("</file>")
                }

                commandLine(
                    System.getenv("HOME") ?: "/home/adrian",
                    ".npm-global", "bin", "opencode",
                    "run", "--attach", "http://127.0.0.1:36361",
                    "--password", "d242bac4-b1c9-49f0-a292-a84a4b685c2b",
                    "--model", "opencode/minimax-m2.5-free",
                    prompt.replace("\"", "\\\"").replace("\n", "\\n")
                )
                workingDir = projectDir

                csvFile.appendText("$projectId,SINGLE_SHOT,$name,1,0,0,0,0,0.0,10\n")
            }
    }
}

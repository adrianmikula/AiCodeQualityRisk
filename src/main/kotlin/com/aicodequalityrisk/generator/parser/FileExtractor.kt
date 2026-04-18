package com.aicodequalityrisk.generator.parser

data class ExtractedFile(
    val path: String,
    val content: String
)

class FileExtractor {
    private val xmlPattern = Regex("""<file\s+path="([^"]+)">(.*?)</file>""", RegexOption.DOT_MATCHES_ALL)
    private val markdownPattern = Regex("""```(\w+)\s*\n?.*?path\s*=\s*([^\n]+)\n(.*?)```""", RegexOption.DOT_MATCHES_ALL)
    private val codeBlockPattern = Regex("""```java\n(.*?)```""", RegexOption.DOT_MATCHES_ALL)

    fun extractFiles(response: String): List<ExtractedFile> {
        val files = mutableListOf<ExtractedFile>()
        
        files.addAll(extractXmlFiles(response))
        if (files.isNotEmpty()) return files
        
        files.addAll(extractMarkdownFiles(response))
        if (files.isNotEmpty()) return files
        
        return files
    }

    private fun extractXmlFiles(response: String): List<ExtractedFile> {
        return xmlPattern.findAll(response).map { match ->
            ExtractedFile(
                path = match.groupValues[1].trim(),
                content = match.groupValues[2].trim()
            )
        }.toList()
    }

    private fun extractMarkdownFiles(response: String): List<ExtractedFile> {
        val files = mutableListOf<ExtractedFile>()
        
        codeBlockPattern.findAll(response).forEach { match ->
            files.add(ExtractedFile(
                path = inferFilename(files.size),
                content = match.groupValues[1].trim()
            ))
        }
        
        val fileRefs = Regex("""file:\s*([^\n]+)""").findAll(response)
        fileRefs.forEach { match ->
            val filePath = match.groupValues[1].trim()
            val codeSection = Regex("""$filePath\s*\n```java\n(.*?)```""", RegexOption.DOT_MATCHES_ALL)
            codeSection.find(response)?.let { codeMatch ->
                files.add(ExtractedFile(
                    path = filePath,
                    content = codeMatch.groupValues[1].trim()
                ))
            }
        }

        val simpleBlocks = Regex("""(src/main/java/[^\n]+\\.java)\s*\n(.*?)(?=\n[^ ]|\Z)""", RegexOption.DOT_MATCHES_ALL)
        simpleBlocks.findAll(response).forEach { match ->
            val path = match.groupValues[1].trim()
            val content = match.groupValues[2].trim()
            if (content.contains("class ") && !files.any { it.path == path }) {
                files.add(ExtractedFile(path = path, content = content))
            }
        }
        
        return files
    }

    private fun inferFilename(index: Int): String {
        val names = listOf(
            "Task.java", "TaskController.java", "TaskService.java", 
            "TaskRepository.java", "App.java"
        )
        return "src/main/java/com/example/${names.getOrElse(index) { "File$index.java" }}"
    }
}
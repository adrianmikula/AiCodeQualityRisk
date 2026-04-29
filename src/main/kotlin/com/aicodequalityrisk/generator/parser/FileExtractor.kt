package com.aicodequalityrisk.generator.parser

data class ExtractedFile(
    val path: String,
    val content: String
)

class FileExtractor {
    
    fun extractFiles(response: String): List<ExtractedFile> {
        val files = mutableListOf<ExtractedFile>()
        
        // Try XML format first (fast, exact matching)
        files.addAll(extractXmlFormat(response))
        if (files.isNotEmpty()) return files
        
        // Try markdown code blocks with path hints (line-by-line parsing)
        files.addAll(extractMarkdownFormat(response))
        if (files.isNotEmpty()) return files
        
        // Try simple code block extraction
        files.addAll(extractSimpleCodeBlocks(response))
        
        return files
    }
    
    private fun extractXmlFormat(response: String): List<ExtractedFile> {
        val files = mutableListOf<ExtractedFile>()
        val lines = response.lines()
        var i = 0
        
        while (i < lines.size) {
            val line = lines[i]
            val xmlOpenMatch = Regex("""<file\s+path="([^"]+)">""").find(line)
            
            if (xmlOpenMatch != null) {
                val path = xmlOpenMatch.groupValues[1]
                val contentLines = mutableListOf<String>()
                i++
                
                while (i < lines.size && !lines[i].contains("</file>")) {
                    contentLines.add(lines[i])
                    i++
                }
                
                if (contentLines.isNotEmpty()) {
                    files.add(ExtractedFile(path = path, content = contentLines.joinToString("\n")))
                }
            }
            i++
        }
        
        return files
    }
    
    private fun extractMarkdownFormat(response: String): List<ExtractedFile> {
        val files = mutableListOf<ExtractedFile>()
        val lines = response.lines()
        var i = 0
        var currentPath: String? = null
        var currentContent = mutableListOf<String>()
        var inCodeBlock = false
        
        while (i < lines.size) {
            val line = lines[i]
            
            // Check for path hint: path="..." or path='...'
            val pathMatch = Regex("""path\s*=\s*["']([^"']+)["']""").find(line)
            if (pathMatch != null && !inCodeBlock) {
                currentPath = pathMatch.groupValues[1]
            }
            
            // Check for code block start
            if (line.trim().startsWith("```java") || line.trim().startsWith("```")) {
                if (!inCodeBlock) {
                    inCodeBlock = true
                    currentContent.clear()
                } else {
                    // End of code block
                    inCodeBlock = false
                    if (currentContent.isNotEmpty()) {
                        val path = currentPath ?: inferFilename(files.size)
                        files.add(ExtractedFile(
                            path = path,
                            content = currentContent.joinToString("\n")
                        ))
                        currentPath = null
                    }
                }
            } else if (inCodeBlock) {
                currentContent.add(line)
            }
            
            i++
        }
        
        return files
    }
    
    private fun extractSimpleCodeBlocks(response: String): List<ExtractedFile> {
        val files = mutableListOf<ExtractedFile>()
        val lines = response.lines()
        var i = 0
        var inCodeBlock = false
        var currentContent = mutableListOf<String>()
        
        while (i < lines.size) {
            val line = lines[i]
            
            if (line.trim() == "```java" || line.trim().startsWith("```") && !inCodeBlock) {
                inCodeBlock = true
                currentContent.clear()
            } else if (line.trim() == "```" && inCodeBlock) {
                inCodeBlock = false
                if (currentContent.isNotEmpty() && currentContent.any { it.contains("class ") }) {
                    files.add(ExtractedFile(
                        path = inferFilename(files.size),
                        content = currentContent.joinToString("\n")
                    ))
                }
            } else if (inCodeBlock) {
                currentContent.add(line)
            }
            
            i++
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
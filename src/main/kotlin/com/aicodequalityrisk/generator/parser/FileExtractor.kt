package com.aicodequalityrisk.generator.parser

data class ExtractedFile(
    val path: String,
    val content: String
)

class FileExtractor {
    private val filePattern = Regex("""<file\s+path="([^"]+)">(.*?)</file>""", RegexOption.DOT_MATCHES_ALL)

    fun extractFiles(response: String): List<ExtractedFile> {
        return filePattern.findAll(response).map { match ->
            ExtractedFile(
                path = match.groupValues[1].trim(),
                content = match.groupValues[2].trim()
            )
        }.toList()
    }
}
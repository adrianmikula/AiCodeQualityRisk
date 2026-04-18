package com.aicodequalityrisk.generator.writer

import com.aicodequalityrisk.generator.parser.ExtractedFile
import java.nio.file.Files
import java.nio.file.Path

class ProjectWriter {
    fun writeProject(basePath: Path, files: List<ExtractedFile>) {
        basePath.toFile().mkdirs()
        
        files.forEach { file ->
            val filePath = basePath.resolve(file.path)
            Files.createDirectories(filePath.parent)
            Files.writeString(filePath, file.content)
        }
    }

    fun deleteProject(basePath: Path) {
        basePath.toFile().deleteRecursively()
    }
}
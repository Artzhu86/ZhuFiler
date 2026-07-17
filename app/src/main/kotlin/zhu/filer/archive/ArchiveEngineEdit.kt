package zhu.filer.archive

import java.io.File

// 删除归档条目
fun ArchiveEngine.deleteEntry(archiveFile: File, entryPath: String, isDir: Boolean, password: String?): Boolean {
    val format = detectFormat(archiveFile) ?: return false
    val target = normalizeInternal(entryPath)
    val tempDir = File(archiveFile.parentFile, ".tmp_archive_${System.currentTimeMillis()}")
    tempDir.mkdirs()
    try {
        extractAll(archiveFile, tempDir, password, shouldSkip = { path ->
            path == target || (isDir && path.startsWith("$target/"))
        })
        val newFile = File(archiveFile.parentFile, ".tmp_new_${archiveFile.name}")
        val sources = tempDir.listFiles()?.toList() ?: emptyList()
        if (sources.isEmpty()) {
            newFile.delete()
            archiveFile.delete()
            return true
        }
        val created = createArchive(newFile, sources, tempDir, format, password)
        if (!created) return false
        archiveFile.delete()
        newFile.renameTo(archiveFile)
        return true
    } finally {
        tempDir.deleteRecursively()
    }
}

// 重命名归档条目
fun ArchiveEngine.renameEntry(archiveFile: File, entryPath: String, newName: String, isDir: Boolean, password: String?): Boolean {
    val format = detectFormat(archiveFile) ?: return false
    val target = normalizeInternal(entryPath)
    val tempDir = File(archiveFile.parentFile, ".tmp_archive_${System.currentTimeMillis()}")
    tempDir.mkdirs()
    try {
        extractAll(
            archiveFile, tempDir, password,
            rewritePath = { path ->
                if (path == target) newName
                else if (isDir && path.startsWith("$target/")) newName + path.substring(target.length)
                else path
            }
        )
        val newFile = File(archiveFile.parentFile, ".tmp_new_${archiveFile.name}")
        val sources = tempDir.listFiles()?.toList() ?: emptyList()
        if (sources.isEmpty()) return false
        val created = createArchive(newFile, sources, tempDir, format, password)
        if (!created) return false
        archiveFile.delete()
        newFile.renameTo(archiveFile)
        return true
    } finally {
        tempDir.deleteRecursively()
    }
}

// 添加文件到归档
fun ArchiveEngine.addFiles(archiveFile: File, files: List<File>, internalPath: String, password: String?): Boolean {
    val format = detectFormat(archiveFile) ?: return false
    val tempDir = File(archiveFile.parentFile, ".tmp_archive_${System.currentTimeMillis()}")
    tempDir.mkdirs()
    try {
        extractAll(archiveFile, tempDir, password)
        val prefix = normalizeInternal(internalPath)
        for (file in files) {
            val destName = file.name
            val destPath = if (prefix.isEmpty()) destName else "$prefix/$destName"
            val destFile = File(tempDir, destPath)
            if (destFile.exists()) destFile.deleteRecursively()
            if (file.isDirectory) file.copyRecursively(destFile) else file.copyTo(destFile)
        }
        val newFile = File(archiveFile.parentFile, ".tmp_new_${archiveFile.name}")
        val sources = tempDir.listFiles()?.toList() ?: emptyList()
        if (sources.isEmpty()) return false
        val created = createArchive(newFile, sources, tempDir, format, password)
        if (!created) return false
        archiveFile.delete()
        newFile.renameTo(archiveFile)
        return true
    } finally {
        tempDir.deleteRecursively()
    }
}

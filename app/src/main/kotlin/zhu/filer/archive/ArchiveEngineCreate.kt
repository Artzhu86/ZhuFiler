package zhu.filer.archive

import java.io.File
import java.io.FileInputStream
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.EncryptionMethod

// 创建归档
fun ArchiveEngine.createArchive(
    outputFile: File,
    sources: List<File>,
    baseDir: File,
    format: ArchiveFormat,
    password: String?,
    onProgress: ((currentFile: String, fileBytesRead: Long, fileBytesTotal: Long, fileIndex: Int, fileCount: Int) -> Unit)? = null
): Boolean {
    val items = collectSources(sources, baseDir)
    if (items.isEmpty()) return false
    outputFile.parentFile?.mkdirs()
    if (outputFile.exists()) outputFile.delete()

    return when (format) {
        ArchiveFormat.ZIP -> createZipArchive(outputFile, items, password, onProgress)
        ArchiveFormat.SEVEN_ZIP -> createSevenZipArchive(outputFile, items, password, onProgress)
        ArchiveFormat.TAR_GZ, ArchiveFormat.TAR_XZ -> createCompressedTar(outputFile, items, format, onProgress)
    }
}

// 创建Zip归档
internal fun ArchiveEngine.createZipArchive(
    outputFile: File,
    items: List<SourceItem>,
    password: String?,
    onProgress: ((currentFile: String, fileBytesRead: Long, fileBytesTotal: Long, fileIndex: Int, fileCount: Int) -> Unit)?
): Boolean {
    val zipFile = if (password != null) {
        ZipFile(outputFile, password.toCharArray())
    } else {
        ZipFile(outputFile)
    }
    val params = ZipParameters().apply {
        compressionMethod = CompressionMethod.DEFLATE
        compressionLevel = CompressionLevel.NORMAL
        if (password != null) {
            isEncryptFiles = true
            encryptionMethod = EncryptionMethod.AES
            aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
        }
    }
    for ((index, item) in items.withIndex()) {
        if (item.isDirectory) {
            val folderParams = ZipParameters(params).apply {
                isIncludeRootFolder = false
                fileNameInZip = item.pathInArchive + "/"
            }
            zipFile.addFolder(item.file, folderParams)
            onProgress?.invoke(item.pathInArchive, 0L, 0L, index, items.size)
        } else {
            val fileParams = ZipParameters(params).apply {
                fileNameInZip = item.pathInArchive
            }
            val total = item.file.length()
            onProgress?.invoke(item.pathInArchive, 0L, total, index, items.size)
            val counting = CountingInputStream(FileInputStream(item.file), total) { read ->
                onProgress?.invoke(item.pathInArchive, read, total, index, items.size)
            }
            zipFile.addStream(counting, fileParams)
        }
    }
    return true
}

// 收集归档源文件
internal fun ArchiveEngine.collectSources(sources: List<File>, baseDir: File): List<SourceItem> {
    val result = mutableListOf<SourceItem>()
    for (src in sources) {
        val rel = try {
            src.relativeTo(baseDir).path
        } catch (e: Exception) {
            src.name
        }
        val rootPath = rel.replace('\\', '/').trimEnd('/')
        if (rootPath.isEmpty()) continue
        if (src.isDirectory) addDirectory(result, src, rootPath)
        else result.add(SourceItem(src, rootPath, false))
    }
    return result
}

// 添加目录到归档源
internal fun ArchiveEngine.addDirectory(result: MutableList<SourceItem>, dir: File, pathInArchive: String) {
    result.add(SourceItem(dir, pathInArchive, true))
    val children = dir.listFiles() ?: return
    for (child in children.sortedBy { it.name }) {
        val childPath = "$pathInArchive/${child.name}"
        if (child.isDirectory) addDirectory(result, child, childPath)
        else result.add(SourceItem(child, childPath, false))
    }
}

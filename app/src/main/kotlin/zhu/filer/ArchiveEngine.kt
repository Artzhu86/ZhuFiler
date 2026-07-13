package zhu.filer

import net.sf.sevenzipjbinding.ArchiveFormat as SevenZipArchiveFormat
import net.sf.sevenzipjbinding.ExtractOperationResult
import net.sf.sevenzipjbinding.IArchiveOpenCallback
import net.sf.sevenzipjbinding.ICryptoGetTextPassword
import net.sf.sevenzipjbinding.IInArchive
import net.sf.sevenzipjbinding.IOutCreateArchive
import net.sf.sevenzipjbinding.IOutCreateCallback
import net.sf.sevenzipjbinding.IOutFeatureSetEncryptHeader
import net.sf.sevenzipjbinding.IOutFeatureSetLevel
import net.sf.sevenzipjbinding.IOutItemAllFormats
import net.sf.sevenzipjbinding.ISequentialInStream
import net.sf.sevenzipjbinding.ISequentialOutStream
import net.sf.sevenzipjbinding.PropID
import net.sf.sevenzipjbinding.SevenZip
import net.sf.sevenzipjbinding.SevenZipException
import net.sf.sevenzipjbinding.impl.InputStreamSequentialInStream
import net.sf.sevenzipjbinding.impl.OutItemFactory
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream
import net.sf.sevenzipjbinding.impl.RandomAccessFileOutStream
import org.tukaani.xz.LZMA2Options
import org.tukaani.xz.XZOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.Date
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.util.zip.GZIPOutputStream

// 归档需要密码异常
class ArchivePasswordRequiredException(message: String = "Password required") : Exception(message)

// 归档密码错误异常
class WrongArchivePasswordException(message: String = "Wrong password") : Exception(message)

// 归档格式枚举
enum class ArchiveFormat(val extension: String) {
    ZIP("zip"),
    SEVEN_ZIP("7z"),
    TAR_GZ("tar.gz"),
    TAR_XZ("tar.xz")
}

// 去除已知归档扩展名
internal fun stripKnownArchiveExt(name: String): String {
    for (fmt in ArchiveFormat.entries) {
        val suffix = ".${fmt.extension}"
        if (name.endsWith(suffix, ignoreCase = true)) return name.dropLast(suffix.length)
    }
    return name
}

// 归档引擎
object ArchiveEngine {

    // 归档条目信息
    data class EntryInfo(
        val path: String,
        val name: String,
        val isDirectory: Boolean,
        val encrypted: Boolean,
        val size: Long,
        val lastModified: Long = 0L
    )

    // 列出归档条目
    fun listEntries(archiveFile: File, internalPath: String, password: String?): List<EntryInfo> {
        val raf = RandomAccessFile(archiveFile, "r")
        var archive: IInArchive? = null
        try {
            val openCallback = OpenCallback(password)
            archive = try {
                SevenZip.openInArchive(null, RandomAccessFileInStream(raf), openCallback)
            } catch (e: SevenZipException) {
                if (openCallback.passwordRequested) throw ArchivePasswordRequiredException()
                throw e
            }
            return readChildren(archive, normalizeInternal(internalPath))
        } finally {
            try { archive?.close() } catch (ignored: Exception) {}
            try { raf.close() } catch (ignored: Exception) {}
        }
    }

    // 读取子条目列表
    private fun readChildren(archive: IInArchive, internalPath: String): List<EntryInfo> {
        val prefix = if (internalPath.isEmpty()) "" else "$internalPath/"
        val children = LinkedHashMap<String, EntryInfo>()
        val count = archive.numberOfItems
        for (i in 0 until count) {
            val rawPath = archive.getProperty(i, PropID.PATH) as? String ?: continue
            val path = rawPath.replace('\\', '/').trimEnd('/')
            if (path.isEmpty()) continue
            if (internalPath.isNotEmpty() && !path.startsWith(prefix)) continue
            val rel = if (internalPath.isEmpty()) path else path.substring(prefix.length)
            if (rel.isEmpty()) continue
            val firstSeg = rel.substringBefore('/')
            val isFolder =
                (archive.getProperty(i, PropID.IS_FOLDER) as? Boolean ?: false) || rel.contains('/')
            val encrypted =
                (archive.getProperty(i, PropID.ENCRYPTED) as? Boolean ?: false) && !isFolder
            val size = (archive.getProperty(i, PropID.SIZE) as? Long) ?: 0L
            val mtime = (archive.getProperty(i, PropID.LAST_MODIFICATION_TIME) as? Date)?.time ?: 0L
            val existing = children[firstSeg]
            if (existing == null) {
                children[firstSeg] = EntryInfo(
                    path = if (internalPath.isEmpty()) firstSeg else "$prefix$firstSeg",
                    name = firstSeg,
                    isDirectory = isFolder,
                    encrypted = encrypted,
                    size = if (isFolder) 0L else size,
                    lastModified = mtime
                )
            } else if (isFolder && !existing.isDirectory) {
                children[firstSeg] = existing.copy(isDirectory = true, encrypted = false, size = 0L)
            }
        }
        return children.values.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    // 解压单个条目
    fun extractEntry(archiveFile: File, entryPath: String, password: String?, destFile: File): Boolean {
        val raf = RandomAccessFile(archiveFile, "r")
        var archive: IInArchive? = null
        var out: FileOutputStream? = null
        try {
            val openCallback = OpenCallback(password)
            archive = try {
                SevenZip.openInArchive(null, RandomAccessFileInStream(raf), openCallback)
            } catch (e: SevenZipException) {
                if (openCallback.passwordRequested) throw ArchivePasswordRequiredException()
                throw e
            }
            val target = normalizeInternal(entryPath)
            val count = archive.numberOfItems
            var index = -1
            for (i in 0 until count) {
                val p = (archive.getProperty(i, PropID.PATH) as? String ?: "")
                    .replace('\\', '/').trimEnd('/')
                if (p == target) {
                    index = i
                    break
                }
            }
            if (index < 0) return false
            destFile.parentFile?.mkdirs()
            out = FileOutputStream(destFile)
            val outStream = object : ISequentialOutStream {
                // 写入解压数据
                override fun write(data: ByteArray?): Int {
                    if (data != null && data.isNotEmpty()) out.write(data)
                    return data?.size ?: 0
                }
            }
            val result = archive.extractSlow(index, outStream, password ?: "")
            return when (result) {
                ExtractOperationResult.OK -> true
                ExtractOperationResult.WRONG_PASSWORD -> throw WrongArchivePasswordException()
                else -> false
            }
        } finally {
            try { out?.close() } catch (ignored: Exception) {}
            try { archive?.close() } catch (ignored: Exception) {}
            try { raf.close() } catch (ignored: Exception) {}
        }
    }

    // 归档源条目信息
    private data class SourceItem(val file: File, val pathInArchive: String, val isDirectory: Boolean)

    // 创建归档
    fun createArchive(
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
    private fun createZipArchive(
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

    // 创建7z归档
    private fun createSevenZipArchive(
        outputFile: File,
        items: List<SourceItem>,
        password: String?,
        onProgress: ((currentFile: String, fileBytesRead: Long, fileBytesTotal: Long, fileIndex: Int, fileCount: Int) -> Unit)?
    ): Boolean {
        val raf = RandomAccessFile(outputFile, "rw")
        var outArchive: IOutCreateArchive<*>? = null
        try {
            outArchive = SevenZip.openOutArchive7z()
            (outArchive as? IOutFeatureSetLevel)?.setLevel(5)
            if (password != null) {
                (outArchive as? IOutFeatureSetEncryptHeader)?.setHeaderEncryption(true)
            }
            val outStream = RandomAccessFileOutStream(raf)
            val callback = if (password != null) {
                CreateCallbackWithPassword(items, password, onProgress)
            } else {
                CreateCallback(items, onProgress)
            }
            @Suppress("UNCHECKED_CAST")
            (outArchive as IOutCreateArchive<IOutItemAllFormats>).createArchive(outStream, items.size, callback as IOutCreateCallback<IOutItemAllFormats>)
            return !callback.failed
        } finally {
            try { outArchive?.close() } catch (ignored: Exception) {}
            try { raf.close() } catch (ignored: Exception) {}
        }
    }

    // 创建Tar归档
    private fun createCompressedTar(
        outputFile: File,
        items: List<SourceItem>,
        format: ArchiveFormat,
        onProgress: ((currentFile: String, fileBytesRead: Long, fileBytesTotal: Long, fileIndex: Int, fileCount: Int) -> Unit)?
    ): Boolean {
        val tempTar = File(outputFile.parentFile, ".${outputFile.name}.tmp.tar")
        var raf: RandomAccessFile? = null
        var outArchive: IOutCreateArchive<IOutItemAllFormats>? = null
        var callback: CreateCallback? = null
        try {
            if (tempTar.exists()) tempTar.delete()
            raf = RandomAccessFile(tempTar, "rw")
            outArchive = SevenZip.openOutArchive(SevenZipArchiveFormat.TAR)
            val outStream = RandomAccessFileOutStream(raf)
            callback = CreateCallback(items, onProgress)
            outArchive.createArchive(outStream, items.size, callback)
        } finally {
            try { outArchive?.close() } catch (ignored: Exception) {}
            try { raf?.close() } catch (ignored: Exception) {}
        }

        if (callback?.failed == true) {
            tempTar.delete()
            return false
        }

        onProgress?.invoke(outputFile.name, 0L, 0L, items.size, items.size)

        try {
            FileInputStream(tempTar).use { input ->
                when (format) {
                    ArchiveFormat.TAR_GZ -> {
                        GZIPOutputStream(FileOutputStream(outputFile)).use { output ->
                            input.copyTo(output)
                        }
                    }
                    ArchiveFormat.TAR_XZ -> {
                        XZOutputStream(FileOutputStream(outputFile), LZMA2Options()).use { output ->
                            input.copyTo(output)
                        }
                    }
                    else -> {}
                }
            }
        } finally {
            tempTar.delete()
        }
        return true
    }

    // 收集归档源文件
    private fun collectSources(sources: List<File>, baseDir: File): List<SourceItem> {
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
    private fun addDirectory(result: MutableList<SourceItem>, dir: File, pathInArchive: String) {
        result.add(SourceItem(dir, pathInArchive, true))
        val children = dir.listFiles() ?: return
        for (child in children.sortedBy { it.name }) {
            val childPath = "$pathInArchive/${child.name}"
            if (child.isDirectory) addDirectory(result, child, childPath)
            else result.add(SourceItem(child, childPath, false))
        }
    }

    // 规范化内部路径
    private fun normalizeInternal(path: String): String =
        path.replace('\\', '/').trim('/').trim()

    // 打开归档回调
    private class OpenCallback(val password: String?) : IArchiveOpenCallback, ICryptoGetTextPassword {
        var passwordRequested = false
        // 设置总进度
        override fun setTotal(files: Long?, bytes: Long?) {}
        // 设置已完成进度
        override fun setCompleted(files: Long?, bytes: Long?) {}
        // 获取解压密码
        override fun cryptoGetTextPassword(): String {
            if (password.isNullOrEmpty()) {
                passwordRequested = true
                throw SevenZipException("Password required to open archive")
            }
            return password
        }
    }

    // 创建归档回调
    private open class CreateCallback(
        val items: List<SourceItem>,
        private val onProgress: ((currentFile: String, fileBytesRead: Long, fileBytesTotal: Long, fileIndex: Int, fileCount: Int) -> Unit)? = null
    ) : IOutCreateCallback<IOutItemAllFormats> {
        var failed = false
        // 获取条目信息
        override fun getItemInformation(
            index: Int,
            outItemFactory: OutItemFactory<IOutItemAllFormats>
        ): IOutItemAllFormats {
            val it = items[index]
            val out = outItemFactory.createOutItem()
            out.setPropertyPath(it.pathInArchive)
            if (it.isDirectory) {
                out.setPropertyAttributes(PropID.AttributesBitMask.FILE_ATTRIBUTE_DIRECTORY)
                out.setDataSize(0L)
            } else {
                out.setDataSize(it.file.length())
            }
            return out
        }

        // 获取输入流
        override fun getStream(index: Int): ISequentialInStream? {
            val it = items[index]
            if (it.isDirectory) {
                onProgress?.invoke(it.pathInArchive, 0L, 0L, index, items.size)
                return null
            }
            val total = it.file.length()
            onProgress?.invoke(it.pathInArchive, 0L, total, index, items.size)
            val counting = CountingInputStream(FileInputStream(it.file), total) { read ->
                onProgress?.invoke(it.pathInArchive, read, total, index, items.size)
            }
            return InputStreamSequentialInStream(counting)
        }

        // 设置总进度
        override fun setTotal(total: Long) {}
        // 设置已完成进度
        override fun setCompleted(complete: Long) {}
        // 设置操作结果
        override fun setOperationResult(operationResultOk: Boolean) {
            if (!operationResultOk) failed = true
        }
    }

    // 带密码的创建回调
    private class CreateCallbackWithPassword(
        items: List<SourceItem>,
        private val password: String,
        onProgress: ((currentFile: String, fileBytesRead: Long, fileBytesTotal: Long, fileIndex: Int, fileCount: Int) -> Unit)? = null
    ) : CreateCallback(items, onProgress), ICryptoGetTextPassword {
        // 返回归档密码
        override fun cryptoGetTextPassword(): String = password
    }

    // 计数输入流
    private class CountingInputStream(
        private val wrapped: InputStream,
        val totalBytes: Long,
        private val onRead: (Long) -> Unit
    ) : InputStream() {
        private var bytesRead = 0L
        private var lastReportedPercent = -1
        // 读取单个字节
        override fun read(): Int {
            val b = wrapped.read()
            if (b != -1) {
                bytesRead++
                report()
            }
            return b
        }
        // 读取字节数组
        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val n = wrapped.read(b, off, len)
            if (n > 0) {
                bytesRead += n
                report()
            }
            return n
        }
        // 上报读取进度
        private fun report() {
            val percent = if (totalBytes > 0) ((bytesRead * 100 / totalBytes).toInt()) else 100
            if (percent == 100 || percent != lastReportedPercent) {
                lastReportedPercent = percent
                onRead(bytesRead)
            }
        }
        // 获取可用字节数
        override fun available(): Int = wrapped.available()
        // 关闭流
        override fun close() = wrapped.close()
    }
}

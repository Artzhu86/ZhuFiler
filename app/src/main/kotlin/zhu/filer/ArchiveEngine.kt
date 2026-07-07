package zhu.filer

import net.sf.sevenzipjbinding.ArchiveFormat
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
import java.io.RandomAccessFile
import java.util.Date
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.util.zip.GZIPOutputStream

class ArchivePasswordRequiredException(message: String = "Password required") : Exception(message)

class WrongArchivePasswordException(message: String = "Wrong password") : Exception(message)

enum class CompressFormat(val extension: String) {
    ZIP("zip"),
    SEVEN_ZIP("7z"),
    TAR_GZ("tar.gz"),
    TAR_XZ("tar.xz")
}

internal fun stripKnownArchiveExt(name: String): String {
    for (fmt in CompressFormat.entries) {
        val suffix = ".${fmt.extension}"
        if (name.endsWith(suffix, ignoreCase = true)) return name.dropLast(suffix.length)
    }
    return name
}

object ArchiveEngine {

    data class EntryInfo(
        val path: String,
        val name: String,
        val isDirectory: Boolean,
        val encrypted: Boolean,
        val size: Long,
        val lastModified: Long = 0L
    )

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

    private data class SourceItem(val file: File, val pathInArchive: String, val isDirectory: Boolean)

    fun createArchive(
        outputFile: File,
        sources: List<File>,
        baseDir: File,
        format: CompressFormat,
        password: String?,
        onProgress: ((current: Int, total: Int, currentFile: String) -> Unit)? = null
    ): Boolean {
        val allItems = collectSources(sources, baseDir)
        if (allItems.isEmpty()) return false
        val outputCanonical = outputFile.canonicalPath
        val items = allItems.filter { it.file.canonicalPath != outputCanonical }
        if (items.isEmpty()) return false
        outputFile.parentFile?.mkdirs()
        if (outputFile.exists()) outputFile.delete()

        return when (format) {
            CompressFormat.ZIP -> createZipArchive(outputFile, items, password, onProgress)
            CompressFormat.SEVEN_ZIP -> createSevenZipArchive(outputFile, items, password, onProgress)
            CompressFormat.TAR_GZ, CompressFormat.TAR_XZ -> createCompressedTar(outputFile, items, format, onProgress)
        }
    }

    private fun createZipArchive(
        outputFile: File,
        items: List<SourceItem>,
        password: String?,
        onProgress: ((current: Int, total: Int, currentFile: String) -> Unit)?
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
                val dirParams = ZipParameters(params).apply {
                    fileNameInZip = item.pathInArchive + "/"
                }
                zipFile.addFile(item.file, dirParams)
            } else {
                val fileParams = ZipParameters(params).apply {
                    fileNameInZip = item.pathInArchive
                }
                zipFile.addFile(item.file, fileParams)
            }
            onProgress?.invoke(index + 1, items.size, item.pathInArchive)
        }
        return true
    }

    private fun createSevenZipArchive(
        outputFile: File,
        items: List<SourceItem>,
        password: String?,
        onProgress: ((current: Int, total: Int, currentFile: String) -> Unit)?
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

    private fun createCompressedTar(
        outputFile: File,
        items: List<SourceItem>,
        format: CompressFormat,
        onProgress: ((current: Int, total: Int, currentFile: String) -> Unit)?
    ): Boolean {
        val tempTar = File(outputFile.parentFile, ".${outputFile.name}.tmp.tar")
        var raf: RandomAccessFile? = null
        var outArchive: IOutCreateArchive<IOutItemAllFormats>? = null
        var callback: CreateCallback? = null
        try {
            if (tempTar.exists()) tempTar.delete()
            raf = RandomAccessFile(tempTar, "rw")
            outArchive = SevenZip.openOutArchive(ArchiveFormat.TAR)
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

        onProgress?.invoke(items.size, items.size, outputFile.name)

        try {
            FileInputStream(tempTar).use { input ->
                when (format) {
                    CompressFormat.TAR_GZ -> {
                        GZIPOutputStream(FileOutputStream(outputFile)).use { output ->
                            input.copyTo(output)
                        }
                    }
                    CompressFormat.TAR_XZ -> {
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

    private fun addDirectory(result: MutableList<SourceItem>, dir: File, pathInArchive: String) {
        result.add(SourceItem(dir, pathInArchive, true))
        val children = dir.listFiles() ?: return
        for (child in children.sortedBy { it.name }) {
            val childPath = "$pathInArchive/${child.name}"
            if (child.isDirectory) addDirectory(result, child, childPath)
            else result.add(SourceItem(child, childPath, false))
        }
    }

    private fun normalizeInternal(path: String): String =
        path.replace('\\', '/').trim('/').trim()

    private class OpenCallback(val password: String?) : IArchiveOpenCallback, ICryptoGetTextPassword {
        var passwordRequested = false
        override fun setTotal(files: Long?, bytes: Long?) {}
        override fun setCompleted(files: Long?, bytes: Long?) {}
        override fun cryptoGetTextPassword(): String {
            if (password.isNullOrEmpty()) {
                passwordRequested = true
                throw SevenZipException("Password required to open archive")
            }
            return password
        }
    }

    private open class CreateCallback(
        val items: List<SourceItem>,
        private val onProgress: ((current: Int, total: Int, currentFile: String) -> Unit)? = null
    ) : IOutCreateCallback<IOutItemAllFormats> {
        var failed = false
        private var completedCount = 0
        override fun getItemInformation(
            index: Int,
            outItemFactory: OutItemFactory<IOutItemAllFormats>
        ): IOutItemAllFormats {
            val it = items[index]
            val out = outItemFactory.createOutItem()
            if (it.isDirectory) {
                out.setPropertyPath(it.pathInArchive + "/")
                out.setPropertyIsDir(true)
                out.setDataSize(0L)
            } else {
                out.setPropertyPath(it.pathInArchive)
                out.setDataSize(it.file.length())
            }
            return out
        }

        override fun getStream(index: Int): ISequentialInStream? {
            val it = items[index]
            if (it.isDirectory) return null
            return InputStreamSequentialInStream(FileInputStream(it.file))
        }

        override fun setTotal(total: Long) {}
        override fun setCompleted(complete: Long) {}
        override fun setOperationResult(operationResultOk: Boolean) {
            if (!operationResultOk) failed = true
            completedCount++
            if (completedCount <= items.size) {
                val item = items[completedCount - 1]
                onProgress?.invoke(completedCount, items.size, item.pathInArchive)
            }
        }
    }

    private class CreateCallbackWithPassword(
        items: List<SourceItem>,
        private val password: String,
        onProgress: ((current: Int, total: Int, currentFile: String) -> Unit)? = null
    ) : CreateCallback(items, onProgress), ICryptoGetTextPassword {
        override fun cryptoGetTextPassword(): String = password
    }
}

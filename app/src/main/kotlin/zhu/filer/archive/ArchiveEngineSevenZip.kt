package zhu.filer.archive

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import net.sf.sevenzipjbinding.ArchiveFormat as SevenZipArchiveFormat
import net.sf.sevenzipjbinding.IOutCreateArchive
import net.sf.sevenzipjbinding.IOutCreateCallback
import net.sf.sevenzipjbinding.IOutFeatureSetEncryptHeader
import net.sf.sevenzipjbinding.IOutFeatureSetLevel
import net.sf.sevenzipjbinding.IOutItemAllFormats
import net.sf.sevenzipjbinding.SevenZip
import net.sf.sevenzipjbinding.impl.RandomAccessFileOutStream
import org.tukaani.xz.LZMA2Options
import org.tukaani.xz.XZOutputStream
import java.util.zip.GZIPOutputStream

// 创建7z归档
internal fun ArchiveEngine.createSevenZipArchive(
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
internal fun ArchiveEngine.createCompressedTar(
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

package zhu.filer.archive

import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import net.sf.sevenzipjbinding.ExtractOperationResult
import net.sf.sevenzipjbinding.IInArchive
import net.sf.sevenzipjbinding.PropID
import net.sf.sevenzipjbinding.SevenZip
import net.sf.sevenzipjbinding.SevenZipException
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream

// 解压单个条目
fun ArchiveEngine.extractEntry(archiveFile: File, entryPath: String, password: String?, destFile: File): Boolean {
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
        val outStream = createOutStream(out)
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

// 解压条目到目录
fun ArchiveEngine.extractEntryToDir(archiveFile: File, entryPath: String, isDir: Boolean, password: String?, destDir: File): Boolean {
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
        val target = normalizeInternal(entryPath)
        val count = archive.numberOfItems
        for (i in 0 until count) {
            val p = (archive.getProperty(i, PropID.PATH) as? String ?: "")
                .replace('\\', '/').trimEnd('/')
            val isFolder = archive.getProperty(i, PropID.IS_FOLDER) as? Boolean ?: false
            if (isDir) {
                if (!p.startsWith("$target/") && p != target) continue
                val rel = if (p == target) "" else p.substringAfter("$target/")
                if (rel.isEmpty()) continue
                if (isFolder) continue
                val outFile = File(destDir, rel)
                outFile.parentFile?.mkdirs()
                val out = FileOutputStream(outFile)
                try {
                    val outStream = createOutStream(out)
                    val result = archive.extractSlow(i, outStream, password ?: "")
                    if (result == ExtractOperationResult.WRONG_PASSWORD) throw WrongArchivePasswordException()
                } finally {
                    out.close()
                }
            } else {
                if (p != target) continue
                val outFile = File(destDir, File(target).name)
                outFile.parentFile?.mkdirs()
                val out = FileOutputStream(outFile)
                try {
                    val outStream = createOutStream(out)
                    val result = archive.extractSlow(i, outStream, password ?: "")
                    if (result == ExtractOperationResult.WRONG_PASSWORD) throw WrongArchivePasswordException()
                    return true
                } finally {
                    out.close()
                }
            }
        }
        return isDir
    } finally {
        try { archive?.close() } catch (ignored: Exception) {}
        try { raf.close() } catch (ignored: Exception) {}
    }
}

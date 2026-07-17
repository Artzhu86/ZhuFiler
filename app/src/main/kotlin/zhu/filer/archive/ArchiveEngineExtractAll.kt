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

// 解压全部条目到目录
fun ArchiveEngine.extractAll(
    archiveFile: File,
    destDir: File,
    password: String?,
    shouldSkip: (String) -> Boolean = { false },
    rewritePath: (String) -> String = { it }
): Boolean {
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
        val count = archive.numberOfItems
        for (i in 0 until count) {
            val rawPath = (archive.getProperty(i, PropID.PATH) as? String ?: "")
                .replace('\\', '/').trimEnd('/')
            if (rawPath.isEmpty()) continue
            if (shouldSkip(rawPath)) continue
            val outPath = rewritePath(rawPath)
            if (outPath.isEmpty()) continue
            val isFolder = archive.getProperty(i, PropID.IS_FOLDER) as? Boolean ?: false
            val outFile = File(destDir, outPath)
            if (isFolder) {
                outFile.mkdirs()
                continue
            }
            outFile.parentFile?.mkdirs()
            val out = FileOutputStream(outFile)
            try {
                val outStream = createOutStream(out)
                val result = archive.extractSlow(i, outStream, password ?: "")
                if (result == ExtractOperationResult.WRONG_PASSWORD) throw WrongArchivePasswordException()
            } finally {
                out.close()
            }
        }
        return true
    } finally {
        try { archive?.close() } catch (ignored: Exception) {}
        try { raf.close() } catch (ignored: Exception) {}
    }
}

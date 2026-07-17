package zhu.filer.archive

import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.Date
import net.sf.sevenzipjbinding.IInArchive
import net.sf.sevenzipjbinding.ISequentialOutStream
import net.sf.sevenzipjbinding.PropID
import net.sf.sevenzipjbinding.SevenZip
import net.sf.sevenzipjbinding.SevenZipException
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream

// 归档源条目信息
internal data class SourceItem(val file: File, val pathInArchive: String, val isDirectory: Boolean)

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

    // 创建输出流
    internal fun createOutStream(out: FileOutputStream) = object : ISequentialOutStream {
        override fun write(data: ByteArray?): Int {
            if (data != null && data.isNotEmpty()) out.write(data)
            return data?.size ?: 0
        }
    }

    // 规范化内部路径
    internal fun normalizeInternal(path: String): String =
        path.replace('\\', '/').trim('/').trim()

    // 检测归档格式
    fun detectFormat(file: File): ArchiveFormat? {
        val name = file.name.lowercase()
        return when {
            name.endsWith(".zip") -> ArchiveFormat.ZIP
            name.endsWith(".7z") -> ArchiveFormat.SEVEN_ZIP
            name.endsWith(".tar.gz") || name.endsWith(".tgz") -> ArchiveFormat.TAR_GZ
            name.endsWith(".tar.xz") || name.endsWith(".txz") -> ArchiveFormat.TAR_XZ
            else -> null
        }
    }

    // 一次性解析归档全部条目
    fun parseAllEntries(archiveFile: File, password: String?): List<EntryInfo> {
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
            val entries = ArrayList<EntryInfo>(count)
            for (i in 0 until count) {
                val rawPath = archive.getProperty(i, PropID.PATH) as? String ?: continue
                val path = rawPath.replace('\\', '/').trimEnd('/')
                if (path.isEmpty()) continue
                val isFolder = archive.getProperty(i, PropID.IS_FOLDER) as? Boolean ?: false
                val encrypted = (archive.getProperty(i, PropID.ENCRYPTED) as? Boolean ?: false) && !isFolder
                val size = (archive.getProperty(i, PropID.SIZE) as? Long) ?: 0L
                val mtime = (archive.getProperty(i, PropID.LAST_MODIFICATION_TIME) as? Date)?.time ?: 0L
                val name = path.substringAfterLast('/')
                entries.add(EntryInfo(path, name, isFolder, encrypted, size, mtime))
            }
            return entries
        } finally {
            try { archive?.close() } catch (ignored: Exception) {}
            try { raf.close() } catch (ignored: Exception) {}
        }
    }

    // 从已缓存的全部条目中过滤出当前层级
    fun filterEntries(allEntries: List<EntryInfo>, internalPath: String): List<EntryInfo> {
        val dir = normalizeInternal(internalPath)
        val prefix = if (dir.isEmpty()) "" else "$dir/"
        val children = LinkedHashMap<String, EntryInfo>()
        for (entry in allEntries) {
            val path = entry.path
            if (dir.isNotEmpty() && !path.startsWith(prefix)) continue
            val rel = if (dir.isEmpty()) path else path.substring(prefix.length)
            if (rel.isEmpty()) continue
            val firstSeg = rel.substringBefore('/')
            val isFolder = entry.isDirectory || rel.contains('/')
            val existing = children[firstSeg]
            if (existing == null) {
                children[firstSeg] = EntryInfo(
                    path = if (dir.isEmpty()) firstSeg else "$prefix$firstSeg",
                    name = firstSeg,
                    isDirectory = isFolder,
                    encrypted = if (isFolder) false else entry.encrypted,
                    size = if (isFolder) 0L else entry.size,
                    lastModified = entry.lastModified
                )
            } else if (isFolder && !existing.isDirectory) {
                children[firstSeg] = existing.copy(isDirectory = true, encrypted = false, size = 0L)
            }
        }
        return children.values.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }
}

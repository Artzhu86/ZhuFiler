package zhu.filer.browser

import android.content.Context
import android.text.format.Formatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import zhu.filer.FileItem
import zhu.filer.FileType
import zhu.filer.R
import zhu.filer.util.ShizukuManager
import zhu.filer.util.SortMode
import zhu.filer.util.createFileItem
import zhu.filer.util.formatDate
import zhu.filer.util.getDirStats
import zhu.filer.util.getSortComparator
import zhu.filer.util.listFilesWithDetails

// 文件列表加载器
class FileListLoader(private val context: Context) {

    // 加载目录文件列表
    suspend fun loadItems(dir: File, showHidden: Boolean, sortMode: SortMode = SortMode.NAME): List<FileItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<FileItem>()

        dir.parentFile?.let {
            items.add(FileItem(it, "..", R.drawable.outline_folder_24, ""))
        }

        if (ShizukuManager.hasPermission()) {
            val shizukuItems = loadViaShizuku(dir, showHidden, sortMode)
            if (shizukuItems != null) {
                items.addAll(shizukuItems)
                return@withContext items
            }
        }

        val fileList = dir.listFiles()?.toList() ?: emptyList()
        val filtered = if (showHidden) fileList else fileList.filter { !it.name.startsWith(".") }
        val sorted = filtered.sortedWith(getSortComparator(sortMode))
        items.addAll(sorted.map { createFileItem(context, it) })
        items
    }

    // 通过Shizuku加载目录
    private fun loadViaShizuku(dir: File, showHidden: Boolean, sortMode: SortMode): List<FileItem>? {
        val infos = ShizukuManager.listFilesWithDetails(dir.absolutePath) ?: return null
        val filtered = if (showHidden) infos else infos.filter { !it.name.startsWith(".") }

        val sorted = filtered.sortedWith(compareByDescending<ShizukuManager.ShizukuFileInfo> { it.isDirectory }
            .let { comp ->
                when (sortMode) {
                    SortMode.NAME -> comp.thenBy { it.name.lowercase(Locale.ROOT) }
                    SortMode.SIZE -> comp.thenByDescending { it.size }
                    SortMode.DATE -> comp.thenByDescending { it.lastModified }
                }
            })

        return sorted.map { info ->
            val file = File(dir, info.name)
            val timeStr = formatDate(context, info.lastModified)
            val sizeStr = Formatter.formatFileSize(context, info.size)
            val iconRes = if (info.isDirectory) R.drawable.outline_folder_24 else FileType.getIconRes(info.name, false)
            FileItem(file, info.name, iconRes, "$timeStr  $sizeStr")
        }
    }

    // 获取目录统计信息
    fun getStats(dir: File): Pair<Int, Int> = getDirStats(dir)
}

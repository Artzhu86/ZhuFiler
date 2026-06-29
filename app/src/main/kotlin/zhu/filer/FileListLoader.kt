package zhu.filer

import android.content.Context
import android.text.format.Formatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class FileListLoader(private val context: Context) {

    suspend fun loadItems(dir: File, showHidden: Boolean): List<FileItem> = withContext(Dispatchers.IO) {
        val fileList = dir.listFiles()?.toList() ?: emptyList()
        val filtered = if (showHidden) fileList else fileList.filter { !it.name.startsWith(".") }
        val sorted = filtered.sortedWith(fileComparator)

        val items = mutableListOf<FileItem>()
        dir.parentFile?.let {
            items.add(FileItem(it, "..", R.drawable.outline_folder_24, ""))
        }
        items.addAll(sorted.map { file ->
            val timeStr = DATE_FORMAT.format(Date(file.lastModified()))
            val subtitle = if (file.isDirectory) timeStr else "$timeStr  ${Formatter.formatFileSize(context, file.length())}"
            val iconRes = if (file.isDirectory) R.drawable.outline_folder_24 else R.drawable.outline_insert_drive_file_24
            FileItem(file, file.name, iconRes, subtitle)
        })
        items
    }

    fun getStats(dir: File): Pair<Int, Int> = getDirStats(dir)
}
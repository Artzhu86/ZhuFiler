package zhu.filer

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileListLoader(private val context: Context) {

    suspend fun loadItems(dir: File, showHidden: Boolean, sortMode: SortMode = SortMode.NAME): List<FileItem> = withContext(Dispatchers.IO) {
        val fileList = dir.listFiles()?.toList() ?: emptyList()
        val filtered = if (showHidden) fileList else fileList.filter { !it.name.startsWith(".") }
        val sorted = filtered.sortedWith(getSortComparator(sortMode))

        val items = mutableListOf<FileItem>()
        dir.parentFile?.let {
            items.add(FileItem(it, "..", R.drawable.outline_folder_24, ""))
        }
        items.addAll(sorted.map { createFileItem(context, it) })
        items
    }

    fun getStats(dir: File): Pair<Int, Int> = getDirStats(dir)
}
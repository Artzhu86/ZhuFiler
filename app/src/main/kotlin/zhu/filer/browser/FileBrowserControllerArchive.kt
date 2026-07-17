package zhu.filer.browser

import android.text.format.Formatter
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import zhu.filer.FileItem
import zhu.filer.FileType
import zhu.filer.R
import zhu.filer.archive.ArchiveEngine
import zhu.filer.archive.ArchivePasswordRequiredException
import zhu.filer.ui.applyToolbarTitlePath
import zhu.filer.util.formatDate
import zhu.filer.util.toast

// 启动归档查看器
fun FileBrowserController.launchArchiveViewer(file: File, password: String? = null, onPasswordRequired: () -> Unit = {}) {
    saveScrollPosition()
    archiveState = null
    loadArchiveInternal(file, "", password, onPasswordRequired)
}

// 导航到归档内部路径
fun FileBrowserController.navigateArchiveTo(internalPath: String) {
    val state = archiveState ?: return
    saveScrollPosition()
    state.internalPath = internalPath
    displayArchiveEntries(state, 0, true)
}

// 加载归档内部
internal fun FileBrowserController.loadArchiveInternal(
    archiveFile: File,
    internalPath: String,
    password: String?,
    onPasswordRequired: () -> Unit = {},
    restorePosition: Int? = null,
    animate: Boolean = true
) {
    loadJob?.cancel()
    swipeRefreshLayout.isRefreshing = true
    loadJob = activity.lifecycleScope.launch {
        try {
            val allEntries = withContext(Dispatchers.IO) {
                ArchiveEngine.parseAllEntries(archiveFile, password)
            }
            if (!currentCoroutineContext().isActive) return@launch
            archiveState = FileBrowserController.ArchiveState(archiveFile, internalPath, password, allEntries)
            displayArchiveEntries(archiveState!!, restorePosition, animate)
        } catch (e: ArchivePasswordRequiredException) {
            archiveState = null
            onPasswordRequired()
        } catch (e: Exception) {
            archiveState = null
            toast(activity, activity.getString(R.string.archive_open_failed))
        } finally {
            swipeRefreshLayout.isRefreshing = false
        }
    }
}

// 显示归档条目
internal fun FileBrowserController.displayArchiveEntries(
    state: FileBrowserController.ArchiveState,
    restorePosition: Int? = null,
    animate: Boolean = true
) {
    val entries = ArchiveEngine.filterEntries(state.allEntries, state.internalPath)
    val items = mutableListOf<FileItem>()
    items.add(FileItem(File(".."), "..", R.drawable.outline_folder_24, ""))
    for (entry in entries) {
        val syntheticFile = File(state.archiveFile, entry.path)
        val iconRes = FileType.getIconRes(entry.name, entry.isDirectory)
        val timeStr = formatDate(activity, entry.lastModified)
        val sizeStr = Formatter.formatFileSize(activity, entry.size)
        val subtitle = "$timeStr  $sizeStr"
        items.add(FileItem(
            file = syntheticFile,
            displayName = entry.name,
            iconRes = iconRes,
            subtitle = subtitle,
            isDirectory = entry.isDirectory,
            encrypted = entry.encrypted,
            entryPath = entry.path,
            size = entry.size
        ))
    }
    applyToolbarTitlePath(toolbar, currentDisplayPath())
    val dirCount = entries.count { it.isDirectory }
    val fileCount = entries.count { !it.isDirectory }
    activity.supportActionBar?.subtitle = "${activity.getString(R.string.dir_count_label)}: $dirCount  ${activity.getString(R.string.file_count_label)}: $fileCount"
    val lm = recyclerView.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager
    val targetFirstVisible = when {
        restorePosition != null && restorePosition >= 0 -> restorePosition
        savedScrollPos >= 0 -> savedScrollPos
        else -> lm?.findFirstVisibleItemPosition() ?: 0
    }
    adapter.submitList(items, firstVisible = targetFirstVisible, animate = animate)
    if (restorePosition != null && restorePosition >= 0 && restorePosition < adapter.itemCount) {
        recyclerView.post {
            (recyclerView.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager)?.scrollToPositionWithOffset(restorePosition, 0)
        }
    } else if (savedScrollPos >= 0 && savedScrollPos < adapter.itemCount) {
        val pos = savedScrollPos
        val offset = savedScrollOffset
        recyclerView.post {
            (recyclerView.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager)?.scrollToPositionWithOffset(pos, offset)
        }
        savedScrollPos = -1
    } else {
        recyclerView.post { recyclerView.scrollToPosition(0) }
    }
    onDirLoaded()
}

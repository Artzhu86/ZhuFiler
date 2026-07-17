package zhu.filer.browser

import androidx.recyclerview.widget.LinearLayoutManager
import java.io.File
import zhu.filer.R
import zhu.filer.ui.applyToolbarTitlePath

// 应用缓存即时显示
internal fun FileBrowserController.applyCacheInstantly(
    dir: File,
    cache: FileBrowserController.DirCache,
    restorePosition: Int?,
    highlightPath: String?,
    animate: Boolean
) {
    currentDir = dir
    loadJob?.cancel()
    applyToolbarTitlePath(toolbar, currentDisplayPath())
    activity.supportActionBar?.subtitle = "${activity.getString(R.string.dir_count_label)}: ${cache.dirCount}  ${activity.getString(R.string.file_count_label)}: ${cache.fileCount}"
    var highlightPos = -1
    if (highlightPath != null) {
        val index = cache.items.indexOfFirst { it.file.absolutePath == highlightPath }
        if (index >= 0) highlightPos = index
    }
    val lm = recyclerView.layoutManager as? LinearLayoutManager
    val targetFirstVisible = when {
        highlightPos >= 0 && restorePosition != null && restorePosition >= 0 -> restorePosition
        highlightPos >= 0 -> highlightPos
        restorePosition != null && restorePosition >= 0 -> restorePosition
        savedScrollPos >= 0 -> savedScrollPos
        else -> lm?.findFirstVisibleItemPosition() ?: 0
    }
    adapter.submitList(cache.items, highlightPos = highlightPos, firstVisible = targetFirstVisible, animate = animate)
    if (highlightPos >= 0) {
        recyclerView.post {
            val lm2 = recyclerView.layoutManager as? LinearLayoutManager ?: return@post
            if (restorePosition != null && restorePosition >= 0 && restorePosition < adapter.itemCount) {
                lm2.scrollToPositionWithOffset(restorePosition, 0)
            } else {
                lm2.scrollToPositionWithOffset(highlightPos, 0)
                scrollPositions[dir.absolutePath] = highlightPos
            }
        }
    } else if (restorePosition != null && restorePosition >= 0 && restorePosition < adapter.itemCount) {
        recyclerView.post {
            (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(restorePosition, 0)
        }
    } else if (savedScrollPos >= 0 && savedScrollPos < adapter.itemCount) {
        val pos = savedScrollPos
        val offset = savedScrollOffset
        recyclerView.post {
            (recyclerView.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(pos, offset)
        }
        savedScrollPos = -1
    }
    onDirLoaded()
}

// 智能刷新
fun FileBrowserController.smartRefresh() {
    val state = archiveState
    if (state != null) {
        val savedPos = scrollPositions[currentScrollKey()] ?: calculateScrollPosition() ?: 0
        saveScrollPosition()
        loadArchiveInternal(state.archiveFile, state.internalPath, state.password, restorePosition = savedPos, animate = false)
        return
    }
    val key = currentDir.absolutePath
    val cache = if (dirtyDirs.contains(key)) null else dirCache[key]
    if (cache != null) {
        applyCacheInstantly(currentDir, cache, calculateScrollPosition(), null, false)
        verifyCacheInBackground(currentDir, cache, null)
    } else {
        saveScrollPosition()
        val savedPos = scrollPositions[key]
        loadDir(currentDir, showLoading = false, scrollToTop = false, restorePosition = savedPos, animate = false)
    }
}

// 刷新
fun FileBrowserController.refresh(animate: Boolean = true) {
    val state = archiveState
    if (state != null) {
        val savedPos = scrollPositions[currentScrollKey()] ?: calculateScrollPosition() ?: 0
        saveScrollPosition()
        loadArchiveInternal(state.archiveFile, state.internalPath, state.password, restorePosition = savedPos, animate = animate)
        return
    }
    val key = currentDir.absolutePath
    dirCache.remove(key)
    dirtyDirs.remove(key)
    saveScrollPosition()
    val savedPos = scrollPositions[key]
    loadDir(currentDir, showLoading = true, scrollToTop = false, restorePosition = savedPos, animate = animate)
}

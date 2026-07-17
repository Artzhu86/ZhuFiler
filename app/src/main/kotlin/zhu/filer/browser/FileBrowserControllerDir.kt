package zhu.filer.browser

import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import zhu.filer.R
import zhu.filer.ui.applyToolbarTitlePath
import zhu.filer.util.updateRecentDirs
// 加载目录
fun FileBrowserController.loadDir(dir: File, showLoading: Boolean = true, scrollToTop: Boolean = true,
    restorePosition: Int? = null,
    highlightPath: String? = null,
    animate: Boolean = true
) {
    archiveState = null
    loadJob?.cancel()
    val key = dir.absolutePath
    val cache = if (key in dirtyDirs) null else dirCache[key]
    if (cache != null && !scrollToTop) {
        applyCacheInstantly(dir, cache, restorePosition, highlightPath, animate)
        verifyCacheInBackground(dir, cache, highlightPath)
        return
    }
    if (showLoading) swipeRefreshLayout.isRefreshing = true
    loadJob = activity.lifecycleScope.launch {
        try {
            currentDir = dir
            applyToolbarTitlePath(toolbar, currentDisplayPath())
            val items = fileLoader.loadItems(dir, showHiddenProvider(), sortModeProvider())
            if (currentCoroutineContext().isActive) {
                val (dirs, files) = fileLoader.getStats(dir)
                activity.supportActionBar?.subtitle = "${activity.getString(R.string.dir_count_label)}: $dirs  ${activity.getString(R.string.file_count_label)}: $files"
                updateRecentDirs(prefs, dir.absolutePath)
                val (lastMod, childCount) = withContext(Dispatchers.IO) {
                    dir.lastModified() to (dir.listFiles()?.size ?: 0)
                }
                dirCache[key] = FileBrowserController.DirCache(items, dirs, files, lastMod, childCount)
                dirtyDirs.remove(key)
                var highlightPos = -1
                if (highlightPath != null) {
                    val index = items.indexOfFirst { it.file.absolutePath == highlightPath }
                    if (index >= 0) highlightPos = index
                }
                val lm = recyclerView.layoutManager as? LinearLayoutManager
                val targetFirstVisible = when {
                    scrollToTop -> 0
                    highlightPos >= 0 && restorePosition != null && restorePosition >= 0 -> restorePosition
                    highlightPos >= 0 -> highlightPos
                    restorePosition != null && restorePosition >= 0 -> restorePosition
                    savedScrollPos >= 0 -> savedScrollPos
                    else -> lm?.findFirstVisibleItemPosition() ?: 0
                }
                adapter.submitList(items, highlightPos = highlightPos, firstVisible = targetFirstVisible, animate = animate)
                if (scrollToTop) {
                    recyclerView.post { recyclerView.scrollToPosition(0) }
                } else if (highlightPos >= 0) {
                    recyclerView.post {
                        val lm2 = recyclerView.layoutManager as? LinearLayoutManager ?: return@post
                        if (restorePosition != null && restorePosition >= 0 && restorePosition < adapter.itemCount) {
                            lm2.scrollToPositionWithOffset(restorePosition, 0)
                            recyclerView.post {
                                val first = lm2.findFirstVisibleItemPosition()
                                val last = lm2.findLastVisibleItemPosition()
                                if (highlightPos < first || highlightPos > last) {
                                    lm2.scrollToPositionWithOffset(highlightPos, 0)
                                    scrollPositions[dir.absolutePath] = highlightPos
                                }
                            }
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
        } finally {
            swipeRefreshLayout.isRefreshing = false
        }
    }
}

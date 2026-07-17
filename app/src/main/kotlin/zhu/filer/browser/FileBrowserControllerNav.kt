package zhu.filer.browser

import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import zhu.filer.R
import zhu.filer.util.toast

// 向上导航
fun FileBrowserController.navigateUp() {
    val state = archiveState
    if (state != null) {
        saveScrollPosition()
        if (state.internalPath.isNotEmpty()) {
            val parentPath = state.internalPath.substringBeforeLast('/', "")
            val savedPos = scrollPositions["${state.archiveFile.absolutePath}::$parentPath"]
            state.internalPath = parentPath
            displayArchiveEntries(state, savedPos, true)
        } else {
            archiveState = null
            val parent = state.archiveFile.parentFile
            if (parent != null) {
                val savedPos = scrollPositions[parent.absolutePath]
                loadDir(parent, showLoading = true, scrollToTop = false, restorePosition = savedPos, highlightPath = state.archiveFile.absolutePath)
            }
        }
        return
    }
    val parent = currentDir.parentFile ?: return
    val childDir = currentDir
    saveScrollPosition()
    val savedPos = scrollPositions[parent.absolutePath]
    loadDir(parent, showLoading = true, scrollToTop = false, restorePosition = savedPos, highlightPath = childDir.absolutePath)
}

// 定位文件
fun FileBrowserController.locateFile(file: File) {
    val targetDir = if (file.isDirectory) file else file.parentFile
    if (targetDir == null || !targetDir.exists()) {
        toast(activity, activity.getString(R.string.cannot_read))
        return
    }
    saveScrollPosition()
    val highlightPath = if (!file.isDirectory) file.absolutePath else null
    loadDir(targetDir, scrollToTop = false, highlightPath = highlightPath)
}

// 保存滚动位置
fun FileBrowserController.saveScrollPosition() {
    val key = currentScrollKey()
    val pos = calculateScrollPosition() ?: return
    scrollPositions[key] = pos
}

// 计算滚动位置
internal fun FileBrowserController.calculateScrollPosition(): Int? {
    val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return null
    val firstPos = lm.findFirstVisibleItemPosition()
    if (firstPos == RecyclerView.NO_POSITION) return null
    val view = lm.findViewByPosition(firstPos)
    if (view == null) return firstPos
    val rect = android.graphics.Rect()
    view.getLocalVisibleRect(rect)
    val visibleHeight = rect.height()
    val totalHeight = view.height
    if (totalHeight == 0) return firstPos
    val visibleRatio = visibleHeight.toFloat() / totalHeight
    return if (visibleRatio >= 0.5f) firstPos else {
        val nextPos = firstPos + 1
        if (nextPos < adapter.itemCount) nextPos else firstPos
    }
}

// 后台验证缓存
internal fun FileBrowserController.verifyCacheInBackground(dir: File, cache: FileBrowserController.DirCache, highlightPath: String?) {
    val key = dir.absolutePath
    loadJob = activity.lifecycleScope.launch {
        val (currentLastMod, currentChildCount) = withContext(Dispatchers.IO) {
            Pair(dir.lastModified(), dir.listFiles()?.size ?: -1)
        }
        if (currentLastMod != cache.lastModified || currentChildCount != cache.childCount) {
            if (currentCoroutineContext().isActive) {
                dirCache.remove(key)
                loadDir(dir, showLoading = false, scrollToTop = false, restorePosition = calculateScrollPosition(), highlightPath = highlightPath, animate = false)
            }
        }
    }
}

// 获取当前滚动位置
fun FileBrowserController.getCurrentScrollPosition(): Int? = scrollPositions[currentScrollKey()]

// 获取滚动位置
fun FileBrowserController.getScrollPosition(path: String): Int? = scrollPositions[path]

// 保存滚动状态
fun FileBrowserController.saveScrollState(pos: Int, offset: Int) {
    savedScrollPos = pos
    savedScrollOffset = offset
}

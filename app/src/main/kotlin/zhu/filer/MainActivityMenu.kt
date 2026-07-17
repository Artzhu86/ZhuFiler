package zhu.filer

import java.io.File
import zhu.filer.browser.getScrollPosition
import zhu.filer.browser.loadDir
import zhu.filer.browser.locateFile
import zhu.filer.browser.saveScrollPosition
import zhu.filer.util.SortMode
import zhu.filer.util.getSortComparator

// 加载目录
internal fun MainActivity.loadDir(dir: File, showLoading: Boolean = true, scrollToTop: Boolean = true, restorePosition: Int? = null) {
    exitMultiSelect()
    browserController.loadDir(dir, showLoading, scrollToTop, restorePosition)
    bookmarkManager.updateMenu(dir)
    supportActionBar?.title = browserController.currentDisplayPath()
}

// 导航加载目录
internal suspend fun MainActivity.navigateToDir(dir: File) {
    browserController.saveScrollPosition()
    val savedPos = browserController.getScrollPosition(dir.absolutePath)
    loadDir(dir, scrollToTop = false, restorePosition = savedPos)
}

// 刷新目录
internal fun MainActivity.refreshDir(dir: File, highlightPath: String?) {
    exitMultiSelect()
    browserController.loadDir(dir, showLoading = true, scrollToTop = false, restorePosition = null, highlightPath = highlightPath)
    bookmarkManager.updateMenu(dir)
    supportActionBar?.title = browserController.currentDisplayPath()
}

// 定位文件
fun MainActivity.locateFile(file: File) {
    browserController.locateFile(file)
}

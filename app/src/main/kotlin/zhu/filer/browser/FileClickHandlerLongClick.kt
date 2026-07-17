package zhu.filer.browser

import zhu.filer.dialog.showCompressDialog
import zhu.filer.dialog.showFileOpsDialog
import zhu.filer.operation.performCompress
import zhu.filer.ui.updatePasteButtons

// 处理项长按
internal fun FileClickHandler.handleItemLongClick(pos: Int): Boolean {
    lastSwipeSelectPos = null
    val ms = multiSelectProvider()
    if (ms.isInMultiSelectMode()) {
        if (pos == 0 && browserController.canNavigateUp()) return true
        ms.showBatchOperationMenu()
        return true
    }
    if (pos == 0 && browserController.canNavigateUp()) return true
    val item = adapter.getFileItem(pos) ?: return true
    if (browserController.isInArchive()) {
        handleArchiveLongClick(item)
        return true
    }
    showFileOpsDialog(
        activity = activity,
        currentDir = browserController.currentDir,
        loadDir = { loadDir(it, false) },
        file = item.file,
        fileOpener = fileOpener,
        onCopyCut = { f, isCut ->
            clipboard.set(f, isCut)
            fabManager.updatePasteButtons(clipboard)
        },
        onBookmarkToggle = { path ->
            bookmarkManager.toggleBookmarkWithConfirm(path)
        },
        isBookmarked = if (item.isDirectory) bookmarkManager.isBookmarked(item.file.absolutePath) else false,
        onCompress = { f ->
            showCompressDialog(activity, listOf(f), browserController.currentDir) { outputFile, format, password ->
                fileOpsController.performCompress(listOf(f), outputFile, format, password)
            }
        },
        onDelete = { browserController.refresh(animate = false) },
        onRenamed = { browserController.refresh(animate = false) }
    )
    return true
}

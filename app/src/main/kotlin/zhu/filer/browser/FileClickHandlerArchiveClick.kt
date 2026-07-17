package zhu.filer.browser

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zhu.filer.FileItem
import zhu.filer.R
import zhu.filer.archive.ArchiveEngine
import zhu.filer.archive.deleteEntry
import zhu.filer.archive.renameEntry
import zhu.filer.dialog.extractArchiveItemToTemp
import zhu.filer.dialog.showArchiveItemOpsDialog
import zhu.filer.dialog.showCompressDialog
import zhu.filer.operation.performCompress
import zhu.filer.ui.updatePasteButtons
import zhu.filer.util.toast

// 处理归档项长按
internal fun FileClickHandler.handleArchiveLongClick(item: FileItem) {
    showArchiveItemOpsDialog(
        activity, item, fileOpener,
        onCopyCut = { archiveItem, isCut ->
            activity.lifecycleScope.launch {
                val tempFile = withContext(Dispatchers.IO) {
                    extractArchiveItemToTemp(activity, archiveItem, fileOpener)
                }
                if (tempFile != null && tempFile.exists()) {
                    clipboard.set(tempFile, isCut)
                    fabManager.updatePasteButtons(clipboard)
                    toast(activity, if (isCut) activity.getString(R.string.move) else activity.getString(R.string.copy))
                } else {
                    toast(activity, activity.getString(R.string.extract_failed))
                }
            }
        },
        onRename = { archiveItem, newName ->
            activity.lifecycleScope.launch {
                val archiveFile = browserController.getArchiveFile() ?: return@launch
                val password = browserController.getArchivePassword()
                val ok = withContext(Dispatchers.IO) {
                    ArchiveEngine.renameEntry(archiveFile, archiveItem.entryPath ?: return@withContext false, newName, archiveItem.isDirectory, password)
                }
                if (ok) {
                    toast(activity, activity.getString(R.string.rename_success))
                    browserController.refresh(animate = false)
                } else {
                    toast(activity, activity.getString(R.string.rename_failed))
                }
            }
        },
        onDelete = { archiveItem ->
            activity.lifecycleScope.launch {
                val archiveFile = browserController.getArchiveFile() ?: return@launch
                val password = browserController.getArchivePassword()
                val ok = withContext(Dispatchers.IO) {
                    ArchiveEngine.deleteEntry(archiveFile, archiveItem.entryPath ?: return@withContext false, archiveItem.isDirectory, password)
                }
                if (ok) {
                    browserController.refresh(animate = false)
                } else {
                    toast(activity, activity.getString(R.string.delete_failed))
                }
            }
        },
        onCompress = { archiveItem ->
            val archiveFile = browserController.getArchiveFile()
            if (archiveFile != null) {
                showCompressDialog(activity, listOf(archiveItem.file), archiveFile.parentFile ?: browserController.currentDir) { outputFile, format, password ->
                    activity.lifecycleScope.launch {
                        val tempFile = withContext(Dispatchers.IO) {
                            extractArchiveItemToTemp(activity, archiveItem, fileOpener)
                        }
                        if (tempFile != null && tempFile.exists()) {
                            fileOpsController.performCompress(listOf(tempFile), outputFile, format, password)
                        }
                    }
                }
            }
        }
    )
}

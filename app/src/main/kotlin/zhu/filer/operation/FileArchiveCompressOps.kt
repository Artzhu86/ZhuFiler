package zhu.filer.operation

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import zhu.filer.archive.ArchiveEngine
import zhu.filer.archive.ArchiveFormat
import zhu.filer.archive.addFiles
import zhu.filer.archive.createArchive
import zhu.filer.browser.getCurrentScrollPosition
import zhu.filer.browser.refresh
import zhu.filer.browser.saveScrollPosition
import zhu.filer.R
import zhu.filer.dialog.createCompressProgressDialog
import zhu.filer.util.toast
import zhu.filer.ui.updatePasteButtons

// 归档内粘贴
internal suspend fun FileOperationsController.performArchivePaste(files: List<File>, isMove: Boolean) {
    val archiveFile = browserController.getArchiveFile() ?: return
    val password = browserController.getArchivePassword()
    val internalPath = browserController.getArchiveInternalPath() ?: ""
    progressBar.show()
    var successCount = 0
    var failCount = 0
    for (file in files) {
        try {
            val ok = withContext(Dispatchers.IO) {
                ArchiveEngine.addFiles(archiveFile, listOf(file), internalPath, password)
            }
            if (ok) successCount++ else failCount++
            if (isMove) {
                withContext(Dispatchers.IO) {
                    if (file.isDirectory) file.deleteRecursively() else file.delete()
                }
            }
        } catch (e: Exception) {
            failCount++
        }
    }
    progressBar.hide()
    val msgRes = if (failCount == 0) {
        if (isMove) R.string.move_success else R.string.copy_success
    } else {
        R.string.paste_result_partial
    }
    val msg = when (msgRes) {
        R.string.move_success, R.string.copy_success -> activity.getString(msgRes, successCount)
        R.string.paste_result_partial -> activity.getString(msgRes, successCount, failCount)
        else -> activity.getString(msgRes)
    }
    toast(activity, msg)
    clipboard.clear()
    fabManager.updatePasteButtons(clipboard)
    browserController.refresh(animate = false)
}

// 执行压缩
internal fun FileOperationsController.performCompress(sources: List<File>, outputFile: File, format: ArchiveFormat, password: String?) {
    var compressJob: Job? = null

    val (progressDialog, updateProgress) = createCompressProgressDialog(
        activity,
        onHide = { },
        onCancel = { compressJob?.cancel() }
    )
    progressDialog.show()

    browserController.saveScrollPosition()
    val savedPos = browserController.getCurrentScrollPosition()

    compressJob = lifecycleScope.launch {
        try {
            val success = withContext(Dispatchers.IO) {
                ArchiveEngine.createArchive(outputFile, sources, browserController.currentDir, format, password) { currentFile, fileBytesRead, fileBytesTotal, fileIndex, fileCount ->
                    activity.runOnUiThread {
                        if (progressDialog.isShowing) {
                            updateProgress(currentFile, fileBytesRead, fileBytesTotal, fileIndex, fileCount)
                        }
                    }
                }
            }
            toast(activity, activity.getString(R.string.compress_success))
            browserController.markDirty(browserController.currentDir.absolutePath)
            loadDir(browserController.currentDir, true, false, savedPos)
        } catch (e: CancellationException) {
            withContext(NonCancellable + Dispatchers.IO) {
                if (outputFile.exists()) outputFile.delete()
            }
            browserController.markDirty(browserController.currentDir.absolutePath)
            loadDir(browserController.currentDir, true, false, savedPos)
        } catch (e: Exception) {
            toast(activity, activity.getString(R.string.compress_failed))
        } finally {
            if (progressDialog.isShowing) progressDialog.dismiss()
        }
    }
}

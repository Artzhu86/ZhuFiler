package zhu.filer.operation

import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zhu.filer.archive.ArchiveEngine
import zhu.filer.archive.deleteEntry
import zhu.filer.FileItem
import zhu.filer.R
import zhu.filer.ui.buildDialogTitle
import zhu.filer.dialog.extractArchiveItemToTemp
import zhu.filer.dialog.showCompressDialog
import zhu.filer.util.toast

// 归档模式批量复制或移动
internal fun MultiSelectController.batchArchiveCopyOrMove(items: List<FileItem>, isMove: Boolean) {
    val opener = fileOpener ?: return
    activity.lifecycleScope.launch {
        progressBar.show()
        val tempFiles = withContext(Dispatchers.IO) {
            items.mapNotNull { item -> extractArchiveItemToTemp(activity, item, opener) }
        }
        progressBar.hide()
        if (tempFiles.isNotEmpty()) {
            clipboardManager.set(tempFiles, isMove)
            exitMultiSelect()
            onClipboardChanged()
            toast(activity, if (isMove) activity.getString(R.string.cut_files, tempFiles.size) else activity.getString(R.string.copied, tempFiles.size))
        } else {
            toast(activity, activity.getString(R.string.extract_failed))
        }
    }
}

// 归档模式批量分享
internal fun MultiSelectController.batchArchiveShare(items: List<FileItem>) {
    val opener = fileOpener ?: return
    activity.lifecycleScope.launch {
        progressBar.show()
        val tempFiles = withContext(Dispatchers.IO) {
            items.mapNotNull { item -> extractArchiveItemToTemp(activity, item, opener) }
        }
        progressBar.hide()
        if (tempFiles.isNotEmpty()) {
            val uris = tempFiles.map { file ->
                FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file)
            }
            val intent = buildShareIntent(uris)
            activity.startActivity(android.content.Intent.createChooser(intent, activity.getString(R.string.share)))
            exitMultiSelect()
        } else {
            toast(activity, activity.getString(R.string.extract_failed))
        }
    }
}

// 归档模式批量删除
internal fun MultiSelectController.batchArchiveDelete(items: List<FileItem>) {
    val archiveFile = getArchiveFile() ?: return
    val password = getArchivePassword()
    MaterialAlertDialogBuilder(activity)
        .setCustomTitle(buildDialogTitle(activity, R.string.delete))
        .setMessage(activity.getString(R.string.batch_delete_confirm, items.size))
        .setPositiveButton(R.string.delete) { _, _ ->
            activity.lifecycleScope.launch {
                progressBar.show()
                withContext(Dispatchers.IO) {
                    items.forEach { item ->
                        val entryPath = item.entryPath ?: return@forEach
                        ArchiveEngine.deleteEntry(archiveFile, entryPath, item.isDirectory, password)
                    }
                }
                progressBar.hide()
                exitMultiSelect()
                if (onRefresh != null) onRefresh() else loadDir(getCurrentDir())
            }
        }
        .setNegativeButton(R.string.cancel, null)
        .show()
}

// 归档模式批量压缩
internal fun MultiSelectController.batchArchiveCompress(items: List<FileItem>) {
    val opener = fileOpener ?: return
    val archiveFile = getArchiveFile()
    if (archiveFile == null) return
    showCompressDialog(activity, items.map { it.file }, archiveFile.parentFile ?: getCurrentDir()) { outputFile, format, password ->
        activity.lifecycleScope.launch {
            progressBar.show()
            val tempFiles = withContext(Dispatchers.IO) {
                items.mapNotNull { item -> extractArchiveItemToTemp(activity, item, opener) }
            }
            progressBar.hide()
            if (tempFiles.isNotEmpty()) {
                onCompress?.invoke(tempFiles)
                exitMultiSelect()
            } else {
                toast(activity, activity.getString(R.string.extract_failed))
            }
        }
    }
}

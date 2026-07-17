package zhu.filer.operation

import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import zhu.filer.R
import zhu.filer.util.ShizukuManager
import zhu.filer.util.deleteFile
import zhu.filer.ui.buildDialogTitle
import zhu.filer.util.toast

// 批量删除
internal fun MultiSelectController.batchDelete(files: List<File>) {
    if (files.isEmpty()) return
    MaterialAlertDialogBuilder(activity)
        .setCustomTitle(buildDialogTitle(activity, R.string.delete))
        .setMessage(activity.getString(R.string.batch_delete_confirm, files.size))
        .setPositiveButton(R.string.delete) { _, _ ->
            activity.lifecycleScope.launch {
                progressBar.show()
                withContext(Dispatchers.IO) {
                    files.forEach { file ->
                        if (ShizukuManager.hasPermission()) {
                            ShizukuManager.deleteFile(file.absolutePath)
                        } else {
                            if (file.isDirectory) file.deleteRecursively() else file.delete()
                        }
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

// 批量复制或移动
internal fun MultiSelectController.batchCopyOrMove(files: List<File>, isMove: Boolean) {
    if (files.isEmpty()) return
    clipboardManager.set(files, isMove)
    exitMultiSelect()
    onClipboardChanged()
    toast(activity, if (isMove) activity.getString(R.string.cut_files, files.size) else activity.getString(R.string.copied, files.size))
}

// 构建分享Intent
internal fun MultiSelectController.buildShareIntent(uris: List<Uri>): Intent =
    Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "*/*"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

// 批量分享
internal fun MultiSelectController.batchShare(files: List<File>) {
    if (files.isEmpty()) return
    val uris = files.map { file ->
        FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file)
    }
    val intent = buildShareIntent(uris)
    activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.share)))
    exitMultiSelect()
}

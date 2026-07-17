package zhu.filer.dialog

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import zhu.filer.R
import zhu.filer.util.ShizukuManager
import zhu.filer.ui.buildDialogTitle
import zhu.filer.operation.FileOpener
import zhu.filer.util.shareFile
import zhu.filer.ui.showDetailsDialog
import zhu.filer.ui.showListDialog
import zhu.filer.util.toast

// 显示操作菜单
fun showFileOpsDialog(
    activity: AppCompatActivity,
    currentDir: File,
    loadDir: suspend (File) -> Unit,
    file: File,
    fileOpener: FileOpener,
    onCopyCut: (File, Boolean) -> Unit = { _, _ -> },
    onBookmarkToggle: ((String) -> Unit)? = null,
    isBookmarked: Boolean = false,
    onCompress: ((File) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onRenamed: (() -> Unit)? = null
) {
    val items = mutableListOf(
        activity.getString(R.string.copy),
        activity.getString(R.string.move),
        activity.getString(R.string.rename),
        activity.getString(R.string.delete)
    )
    if (!file.isDirectory) {
        items.add(activity.getString(R.string.open_with))
        items.add(activity.getString(R.string.share))
    }
    if (file.isDirectory && onBookmarkToggle != null) {
        val bookmarkActionRes = if (isBookmarked) R.string.remove_bookmark else R.string.add_bookmark
        items.add(activity.getString(bookmarkActionRes))
    }
    items.add(activity.getString(R.string.compress))
    items.add(activity.getString(R.string.properties))

    val dialog = MaterialAlertDialogBuilder(activity)
        .setCustomTitle(buildDialogTitle(activity, file.name))
        .setItems(items.toTypedArray()) { _, which ->
            val action = items[which]
            when (action) {
                activity.getString(R.string.rename) -> showRenameDialog(activity, currentDir, loadDir, file, onRenamed)
                activity.getString(R.string.copy) -> onCopyCut(file, false)
                activity.getString(R.string.move) -> onCopyCut(file, true)
                activity.getString(R.string.delete) -> {
                    MaterialAlertDialogBuilder(activity).setCustomTitle(buildDialogTitle(activity, R.string.delete))
                        .setMessage(activity.getString(R.string.delete_message, file.name))
                        .setPositiveButton(R.string.delete) { _, _ ->
                            activity.lifecycleScope.launch {
                                val ok = withContext(Dispatchers.IO) {
                                    if (ShizukuManager.hasPermission()) {
                                        tryShizukuDelete(file)
                                    } else {
                                        if (file.isDirectory) file.deleteRecursively() else file.delete()
                                    }
                                }
                                if (!ok) toast(activity, activity.getString(R.string.delete_failed))
                                if (ok) {
                                    if (onDelete != null) onDelete() else loadDir(currentDir)
                                }
                            }
                        }.setNegativeButton(R.string.cancel, null).show()
                }
                activity.getString(R.string.open_with) -> fileOpener.openFile(file, forceChoose = true)
                activity.getString(R.string.share) -> shareFile(activity, file)
                activity.getString(R.string.add_bookmark), activity.getString(R.string.remove_bookmark) -> onBookmarkToggle?.invoke(file.absolutePath)
                activity.getString(R.string.compress) -> onCompress?.invoke(file)
                activity.getString(R.string.properties) -> showDetailsDialog(activity, file)
            }
        }
        .setNegativeButton(R.string.cancel, null)
        .create()
    showListDialog(dialog)
}

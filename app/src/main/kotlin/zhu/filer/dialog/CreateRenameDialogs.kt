package zhu.filer.dialog

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import zhu.filer.FileItem
import zhu.filer.R
import zhu.filer.util.ShizukuManager
import zhu.filer.ui.buildDialogTitle
import zhu.filer.ui.createDialogContainer
import zhu.filer.ui.createInput
import zhu.filer.ui.focusAndShowKeyboard
import zhu.filer.util.isValid
import zhu.filer.util.toast

// 显示创建对话框
fun showCreateDialog(activity: AppCompatActivity, currentDir: File, loadDir: suspend (File, String?) -> Unit) {
    val rootLayout = createDialogContainer(activity)
    val (inputLayout, edit) = createInput(activity)
    rootLayout.addView(inputLayout)

    MaterialAlertDialogBuilder(activity)
        .setCustomTitle(buildDialogTitle(activity, R.string.create))
        .setView(rootLayout)
        .setPositiveButton(R.string.file) { _, _ ->
            val name = edit.text?.toString()?.trim() ?: ""
            if (isValid(name)) {
                val f = File(currentDir, name)
                activity.lifecycleScope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        if (ShizukuManager.hasPermission()) tryShizukuCreateFile(f) else f.createNewFile()
                    }
                    if (!ok) toast(activity, activity.getString(R.string.create_failed))
                    if (ok) loadDir(currentDir, f.absolutePath)
                }
            } else toast(activity, activity.getString(R.string.invalid_characters))
        }
        .setNegativeButton(R.string.directory) { _, _ ->
            val name = edit.text?.toString()?.trim() ?: ""
            if (isValid(name)) {
                val d = File(currentDir, name)
                activity.lifecycleScope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        if (ShizukuManager.hasPermission()) tryShizukuCreateDir(d) else d.mkdir()
                    }
                    if (!ok) toast(activity, activity.getString(R.string.create_failed))
                    if (ok) loadDir(currentDir, d.absolutePath)
                }
            } else toast(activity, activity.getString(R.string.invalid_characters))
        }
        .setNeutralButton(R.string.cancel, null)
        .show()
        .let { focusAndShowKeyboard(edit, it) }
}

// 显示归档重命名对话框
fun showArchiveRenameDialog(activity: AppCompatActivity, item: FileItem, onRename: (FileItem, String) -> Unit) {
    val oldName = item.displayName
    val rootLayout = createDialogContainer(activity)
    val (inputLayout, editText) = createInput(activity, oldName)
    rootLayout.addView(inputLayout)

    val dotIndex = oldName.lastIndexOf('.')
    if (dotIndex > 0 && !item.isDirectory) {
        editText.setSelection(0, dotIndex)
    } else {
        editText.selectAll()
    }

    MaterialAlertDialogBuilder(activity)
        .setCustomTitle(buildDialogTitle(activity, R.string.rename))
        .setView(rootLayout)
        .setPositiveButton(R.string.ok) { _, _ ->
            val newName = editText.text?.toString()?.trim() ?: ""
            when {
                newName.isEmpty() -> toast(activity, activity.getString(R.string.name_cannot_be_empty))
                newName == oldName -> toast(activity, activity.getString(R.string.name_unchanged))
                !isValid(newName) -> toast(activity, activity.getString(R.string.invalid_characters))
                else -> onRename(item, newName)
            }
        }
        .setNegativeButton(R.string.cancel, null)
        .show()
        .let { focusAndShowKeyboard(editText, it) }
}

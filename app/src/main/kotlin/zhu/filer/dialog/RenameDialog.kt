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
import zhu.filer.ui.createDialogContainer
import zhu.filer.ui.createInput
import zhu.filer.ui.focusAndShowKeyboard
import zhu.filer.util.isValid
import zhu.filer.util.toast

// 显示重命名对话框
fun showRenameDialog(activity: AppCompatActivity, currentDir: File, loadDir: suspend (File) -> Unit, file: File, onRenamed: (() -> Unit)? = null) {
    val oldName = file.name
    val rootLayout = createDialogContainer(activity)
    val (inputLayout, editText) = createInput(activity, oldName)
    rootLayout.addView(inputLayout)

    val dotIndex = oldName.lastIndexOf('.')
    if (dotIndex > 0 && !file.isDirectory) {
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
                else -> {
                    val parent = file.parent ?: run {
                        toast(activity, activity.getString(R.string.rename_failed))
                        return@setPositiveButton
                    }
                    val newFile = File(parent, newName)
                    if (newFile.exists()) {
                        toast(activity, activity.getString(R.string.file_exists))
                        return@setPositiveButton
                    }
                    activity.lifecycleScope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            if (ShizukuManager.hasPermission()) tryShizukuRename(file, newFile) else file.renameTo(newFile)
                        }
                        if (ok) {
                            toast(activity, activity.getString(R.string.rename_success))
                            if (onRenamed != null) onRenamed() else loadDir(currentDir)
                        } else {
                            toast(activity, activity.getString(R.string.rename_failed))
                        }
                    }
                }
            }
        }
        .setNegativeButton(R.string.cancel, null)
        .show()
        .let { focusAndShowKeyboard(editText, it) }
}

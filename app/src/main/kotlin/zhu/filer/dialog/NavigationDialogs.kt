package zhu.filer.dialog

import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.io.File
import zhu.filer.FileItem
import zhu.filer.browser.FileListAdapter
import zhu.filer.R
import zhu.filer.ui.buildDialogTitle
import zhu.filer.ui.createDialogContainer
import zhu.filer.ui.createInput
import zhu.filer.ui.dpToPx
import zhu.filer.ui.focusAndShowKeyboard
import zhu.filer.util.getRecentDirs
import zhu.filer.util.toast

// 显示导航对话框
fun showNavigateDialog(activity: AppCompatActivity, currentPath: String, loadDir: suspend (File) -> Unit, prefs: SharedPreferences) {
    val rootLayout = createDialogContainer(activity)
    val (inputLayout, editText) = createInput(activity, currentPath)
    rootLayout.addView(inputLayout)

    lateinit var dialog: AlertDialog
    val builder = MaterialAlertDialogBuilder(activity)
        .setCustomTitle(buildDialogTitle(activity, R.string.current_directory))
        .setView(rootLayout)
        .setPositiveButton(R.string.action_switch) { _, _ ->
            val path = editText.text?.toString()?.trim() ?: ""
            if (path.isNotEmpty()) {
                val targetDir = File(path)
                if (targetDir.exists() && targetDir.isDirectory) {
                    activity.lifecycleScope.launch { loadDir(targetDir) }
                } else {
                    toast(activity, activity.getString(R.string.directory_invalid))
                }
            }
        }
        .setNeutralButton(R.string.recent) { _, _ ->
            val recent = getRecentDirs(prefs)
            val files = recent.map { File(it) }
            val items = files.map { file ->
                FileItem(file, file.name, R.drawable.outline_folder_24, file.absolutePath)
            }

            lateinit var recentDialog: AlertDialog

            val rv = RecyclerView(activity).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(activity, 400))
                layoutManager = LinearLayoutManager(activity)
                adapter = FileListAdapter(
                    onItemClick = { file, _, _ ->
                        recentDialog.dismiss()
                        Handler(Looper.getMainLooper()).postDelayed({
                            activity.lifecycleScope.launch { loadDir(file) }
                        }, activity.resources.getInteger(R.integer.click_delay_ms).toLong())
                    },
                    onItemLongClick = { _, _ -> false }
                ).apply { submitList(items) }
            }

            recentDialog = MaterialAlertDialogBuilder(activity)
                .setCustomTitle(buildDialogTitle(activity, R.string.recent))
                .setView(rv)
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
        .setNegativeButton(R.string.cancel, null)
    dialog = builder.show()
    focusAndShowKeyboard(editText, dialog)
    editText.post { editText.selectAll() }
}

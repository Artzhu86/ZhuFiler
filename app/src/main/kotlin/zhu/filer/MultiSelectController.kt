package zhu.filer

import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// 多选控制器
class MultiSelectController(
    private val activity: AppCompatActivity,
    private val adapter: FileListAdapter,
    private val canNavigateUp: () -> Boolean,
    private val getCurrentDir: () -> File,
    private val loadDir: suspend (File) -> Unit,
    private val progressBar: CircularProgressIndicator,
    private val clipboardManager: ClipboardManager,
    private val onClipboardChanged: () -> Unit,
    private val onExitMultiSelect: () -> Unit = {},
    private val onCompress: ((List<File>) -> Unit)? = null,
    private val onRefresh: (() -> Unit)? = null
) {

    private var isMultiSelect = false

    // 是否在多选模式
    fun isInMultiSelectMode(): Boolean = isMultiSelect

    // 进入多选
    fun enterMultiSelect() {
        if (!isMultiSelect) {
            isMultiSelect = true
        }
    }

    // 退出多选
    fun exitMultiSelect() {
        if (isMultiSelect) {
            isMultiSelect = false
            adapter.clearSelection()
            onExitMultiSelect()
        }
    }

    // 切换选择状态
    fun toggleSelection(position: Int) {
        if (position == 0 && canNavigateUp()) {
            exitMultiSelect()
            return
        }
        adapter.toggleSelection(position)
        if (!adapter.hasSelection()) {
            exitMultiSelect()
        }
    }

    // 选择位置
    fun selectPosition(position: Int) {
        if (position == 0 && canNavigateUp()) return
        adapter.selectPosition(position)
        enterMultiSelect()
    }

    // 显示批量操作菜单
    fun showBatchOperationMenu() {
        val selected = adapter.getSelectedFiles()
        if (selected.isEmpty()) {
            toast(activity, activity.getString(R.string.no_selected))
            return
        }
        val items = mutableListOf(
            activity.getString(R.string.delete),
            activity.getString(R.string.copy),
            activity.getString(R.string.move),
            activity.getString(R.string.share),
            activity.getString(R.string.compress)
        )
        MaterialAlertDialogBuilder(activity)
            .setTitle(activity.getString(R.string.batch_operation, selected.size))
            .setItems(items.toTypedArray()) { _, which ->
                when (which) {
                    0 -> batchDelete(selected)
                    1 -> batchCopyOrMove(selected, isMove = false)
                    2 -> batchCopyOrMove(selected, isMove = true)
                    3 -> batchShare(selected)
                    4 -> {
                        onCompress?.invoke(selected)
                        exitMultiSelect()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // 批量删除
    private fun batchDelete(files: List<File>) {
        if (files.isEmpty()) return
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.confirm_delete)
            .setMessage(activity.getString(R.string.batch_delete_confirm, files.size))
            .setPositiveButton(R.string.delete) { _, _ ->
                activity.lifecycleScope.launch {
                    progressBar.show()
                    withContext(Dispatchers.IO) {
                        files.forEach { file ->
                            if (file.isDirectory) file.deleteRecursively() else file.delete()
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
    private fun batchCopyOrMove(files: List<File>, isMove: Boolean) {
        if (files.isEmpty()) return
        clipboardManager.set(files, isMove)
        exitMultiSelect()
        onClipboardChanged()
        toast(activity, if (isMove) activity.getString(R.string.cut_files, files.size) else activity.getString(R.string.copied, files.size))
    }

    // 批量分享
    private fun batchShare(files: List<File>) {
        if (files.isEmpty()) return
        val uris = files.map { file ->
            FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file)
        }
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND_MULTIPLE).apply {
            type = "application/octet-stream"
            putParcelableArrayListExtra(android.content.Intent.EXTRA_STREAM, java.util.ArrayList(uris))
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(android.content.Intent.createChooser(intent, activity.getString(R.string.share)))
        exitMultiSelect()
    }
}

package zhu.filer.operation

import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import java.io.File
import zhu.filer.browser.FileListAdapter
import zhu.filer.browser.clearSelection
import zhu.filer.browser.getSelectedFileItems
import zhu.filer.browser.getSelectedFiles
import zhu.filer.browser.hasSelection
import zhu.filer.browser.selectPosition
import zhu.filer.browser.toggleSelection
import zhu.filer.R
import zhu.filer.ui.buildDialogTitle
import zhu.filer.ui.showListDialog
import zhu.filer.util.toast

// 多选控制器
class MultiSelectController(
    internal val activity: AppCompatActivity,
    private val adapter: FileListAdapter,
    private val canNavigateUp: () -> Boolean,
    internal val getCurrentDir: () -> File,
    internal val loadDir: suspend (File) -> Unit,
    internal val progressBar: CircularProgressIndicator,
    internal val clipboardManager: ClipboardManager,
    internal val onClipboardChanged: () -> Unit,
    private val onExitMultiSelect: () -> Unit = {},
    internal val onCompress: ((List<File>) -> Unit)? = null,
    internal val onRefresh: (() -> Unit)? = null,
    private val isInArchive: () -> Boolean = { false },
    internal val fileOpener: FileOpener? = null,
    internal val getArchiveFile: () -> File? = { null },
    internal val getArchivePassword: () -> String? = { null }
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
        val inArchive = isInArchive()
        val selectedItems = adapter.getSelectedFileItems()
        if (selectedItems.isEmpty()) {
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
        val dialog = MaterialAlertDialogBuilder(activity)
            .setCustomTitle(buildDialogTitle(activity, activity.getString(R.string.batch_operation, selectedItems.size)))
            .setItems(items.toTypedArray()) { _, which ->
                if (inArchive) {
                    when (which) {
                        0 -> batchArchiveDelete(selectedItems)
                        1 -> batchArchiveCopyOrMove(selectedItems, isMove = false)
                        2 -> batchArchiveCopyOrMove(selectedItems, isMove = true)
                        3 -> batchArchiveShare(selectedItems)
                        4 -> batchArchiveCompress(selectedItems)
                    }
                } else {
                    val selected = adapter.getSelectedFiles()
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
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
        showListDialog(dialog)
    }
}

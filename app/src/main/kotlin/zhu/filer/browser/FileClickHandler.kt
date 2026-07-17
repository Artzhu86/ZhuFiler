package zhu.filer.browser

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import zhu.filer.ui.FabManager
import zhu.filer.FileItem
import zhu.filer.operation.ClipboardManager
import zhu.filer.operation.FileOpener
import zhu.filer.operation.FileOperationsController
import zhu.filer.operation.MultiSelectController
import zhu.filer.settings.BookmarkManager

// 文件点击处理器
class FileClickHandler(
    internal val activity: AppCompatActivity,
    internal val recyclerView: RecyclerView,
    internal val browserController: FileBrowserController,
    internal val fileOpener: FileOpener,
    internal val fileOpsController: FileOperationsController,
    internal val bookmarkManager: BookmarkManager,
    internal val multiSelectProvider: () -> MultiSelectController,
    internal val clipboard: ClipboardManager,
    internal val fabManager: FabManager,
    internal val toolbarScrollerController: ToolbarScrollerController,
    internal val loadDir: (File, Boolean) -> Unit,
    internal val exitMultiSelect: () -> Unit,
    internal val updateToolbarTitle: () -> Unit,
    internal val updateMultiSelectFabs: () -> Unit
) {

    internal var lastSwipeSelectPos: Int? = null

    lateinit var adapter: FileListAdapter
        internal set

    // 设置列表
    fun setup() {
        adapter = FileListAdapter(
            onItemClick = { _, pos, sharedView -> handleItemClick(pos, sharedView) },
            onItemLongClick = { _, pos -> handleItemLongClick(pos) }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.itemAnimator = null
        toolbarScrollerController.setupScrollListener()
        setupSwipeToSelect()
    }

    // 处理项点击
    internal fun handleItemClick(pos: Int, sharedView: View) {
        lastSwipeSelectPos = null
        val ms = multiSelectProvider()
        if (ms.isInMultiSelectMode()) {
            ms.toggleSelection(pos)
            updateToolbarTitle()
            updateMultiSelectFabs()
            if (!adapter.hasSelection()) exitMultiSelect()
            return
        }
        if (pos == 0 && browserController.canNavigateUp()) {
            browserController.navigateUp()
            return
        }
        val item: FileItem = adapter.getFileItem(pos) ?: return
        if (browserController.isInArchive()) {
            if (item.isDirectory) {
                browserController.navigateArchiveTo(item.entryPath!!)
            } else {
                fileOpener.openArchiveEntry(item, sharedView)
            }
            return
        }
        if (item.isDirectory) {
            browserController.saveScrollPosition()
            loadDir(item.file, true)
        } else {
            fileOpener.openFile(item.file, sharedView)
        }
    }

    // 全选
    fun selectAll() {
        val startPos = if (browserController.canNavigateUp()) 1 else 0
        for (i in startPos until adapter.itemCount) {
            adapter.selectPosition(i)
        }
        updateToolbarTitle()
        updateMultiSelectFabs()
    }

    // 取消全选
    fun deselectAll() {
        adapter.clearSelection()
        exitMultiSelect()
    }

    // 获取选中文件
    fun getSelectedFiles() = adapter.getSelectedFiles()

    // 重置滑动选择
    fun resetSwipeSelect() {
        lastSwipeSelectPos = null
    }
}

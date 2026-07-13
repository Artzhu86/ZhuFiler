package zhu.filer

import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

// 文件点击处理器
class FileClickHandler(
    private val activity: AppCompatActivity,
    private val recyclerView: RecyclerView,
    private val browserController: FileBrowserController,
    private val fileOpener: FileOpener,
    private val fileOpsController: FileOperationsController,
    private val bookmarkManager: BookmarkManager,
    private val multiSelectProvider: () -> MultiSelectController,
    private val clipboard: ClipboardManager,
    private val fabManager: FabManager,
    private val toolbarScrollerController: ToolbarScrollerController,
    private val loadDir: (File, Boolean) -> Unit,
    private val exitMultiSelect: () -> Unit,
    private val updateToolbarTitle: () -> Unit,
    private val updateMultiSelectFabs: () -> Unit
) {

    private var lastSwipeSelectPos: Int? = null

    // 适配器
    lateinit var adapter: FileListAdapter
        private set

    // 设置列表
    fun setup() {
        adapter = FileListAdapter(
            onItemClick = { _, pos, sharedView ->
                lastSwipeSelectPos = null
                val ms = multiSelectProvider()
                if (ms.isInMultiSelectMode()) {
                    ms.toggleSelection(pos)
                    updateToolbarTitle()
                    updateMultiSelectFabs()
                    if (!adapter.hasSelection()) exitMultiSelect()
                    return@FileListAdapter
                }
                if (pos == 0 && browserController.canNavigateUp()) {
                    browserController.navigateUp()
                    return@FileListAdapter
                }
                val item = adapter.getFileItem(pos) ?: return@FileListAdapter
                if (browserController.isInArchive()) {
                    if (item.isDirectory) {
                        browserController.navigateArchiveTo(item.entryPath!!)
                    } else {
                        fileOpener.openArchiveEntry(item)
                    }
                    return@FileListAdapter
                }
                if (item.isDirectory) {
                    browserController.saveScrollPosition()
                    loadDir(item.file, true)
                } else {
                    fileOpener.openFile(item.file, sharedView)
                }
            },
            onItemLongClick = { _, pos ->
                lastSwipeSelectPos = null
                val ms = multiSelectProvider()
                if (ms.isInMultiSelectMode()) {
                    if (pos == 0 && browserController.canNavigateUp()) return@FileListAdapter true
                    ms.showBatchOperationMenu()
                    return@FileListAdapter true
                }
                if (pos == 0 && browserController.canNavigateUp()) return@FileListAdapter true
                val item = adapter.getFileItem(pos) ?: return@FileListAdapter true
                if (browserController.isInArchive()) {
                    showArchiveItemOpsDialog(activity, item, fileOpener)
                    return@FileListAdapter true
                }
                showFileOpsDialog(
                    activity = activity,
                    currentDir = browserController.currentDir,
                    loadDir = { loadDir(it, false) },
                    file = item.file,
                    fileOpener = fileOpener,
                    onCopyCut = { f, isCut ->
                        clipboard.set(f, isCut)
                        fabManager.updatePasteButtons(clipboard)
                    },
                    onBookmarkToggle = { path ->
                        bookmarkManager.toggleBookmarkWithConfirm(path)
                    },
                    isBookmarked = if (item.isDirectory) bookmarkManager.isBookmarked(item.file.absolutePath) else false,
                    onCompress = { f ->
                        showCompressDialog(activity, listOf(f), browserController.currentDir) { outputFile, format, password ->
                            fileOpsController.performCompress(listOf(f), outputFile, format, password)
                        }
                    },
                    onDelete = { browserController.refresh(animate = false) },
                    onRenamed = { browserController.refresh(animate = false) }
                )
                true
            }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.itemAnimator = null
        toolbarScrollerController.setupScrollListener()
        setupSwipeToSelect()
    }

    // 设置滑动选择
    private fun setupSwipeToSelect() {
        val callback = SwipeToSelectCallback(adapter) { position, willBeSelected ->
            if (position == 0 && browserController.canNavigateUp()) return@SwipeToSelectCallback
            val ms = multiSelectProvider()
            if (!willBeSelected) {
                lastSwipeSelectPos = null
                if (ms.isInMultiSelectMode()) {
                    ms.toggleSelection(position)
                    updateToolbarTitle()
                    updateMultiSelectFabs()
                    if (!adapter.hasSelection()) exitMultiSelect()
                }
                return@SwipeToSelectCallback
            }
            val lastPos = lastSwipeSelectPos
            if (lastPos != null && lastPos != position && ms.isInMultiSelectMode()) {
                val range = if (lastPos < position) lastPos..position else position..lastPos
                range.forEach { adapter.selectPosition(it) }
            } else {
                if (!ms.isInMultiSelectMode()) {
                    ms.selectPosition(position)
                } else {
                    ms.toggleSelection(position)
                }
            }
            lastSwipeSelectPos = position
            updateToolbarTitle()
            updateMultiSelectFabs()
        }
        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)
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

    // 是否有选中
    fun hasSelection() = adapter.hasSelection()

    // 重置滑动选择
    fun resetSwipeSelect() {
        lastSwipeSelectPos = null
    }
}

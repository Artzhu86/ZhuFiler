package zhu.filer

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import zhu.filer.dialog.showCreateDialog
import zhu.filer.ui.hideAll
import zhu.filer.ui.showMode
import zhu.filer.ui.updateMultiSelectButtons
import zhu.filer.ui.updatePasteButtons

// 设置悬浮按钮
internal fun MainActivity.setupFabs() {
    fabAdd.setOnClickListener {
        showCreateDialog(this, browserController.currentDir) { dir, highlightPath ->
            lifecycleScope.launch { refreshDir(dir, highlightPath) }
        }
    }
    fabManager.setup(
        fabAction = fabAction,
        fabCancel = fabCancel,
        clipboard = clipboard,
        targetDirProvider = { browserController.currentDir },
        onPaste = { files, isMove, overwrite ->
            lifecycleScope.launch { fileOpsController.performPaste(files, browserController.currentDir, isMove, overwrite) }
        },
        onCancel = {
            clipboard.clear()
            fabManager.updatePasteButtons(clipboard)
        }
    )
    fabManager.setMultiSelectActions(
        onSelectAll = { fileClickHandler.selectAll() },
        onDeselect = { fileClickHandler.deselectAll() }
    )
}

// 更新多选悬浮按钮
internal fun MainActivity.updateMultiSelectFabs() {
    fabManager.updateMultiSelectButtons(multiSelectController.isInMultiSelectMode())
}

// 退出多选
internal fun MainActivity.exitMultiSelect() {
    fileClickHandler.resetSwipeSelect()
    multiSelectController.exitMultiSelect()
    updateToolbarTitle()
    updateMultiSelectFabs()
}

// 更新工具栏标题
internal fun MainActivity.updateToolbarTitle() {
    if (multiSelectController.isInMultiSelectMode()) {
        val count = fileClickHandler.getSelectedFiles().size
        val stats = statsSubtitle ?: ""
        supportActionBar?.subtitle = if (stats.isNotEmpty()) "$stats   " + getString(R.string.selected_count, count) else getString(R.string.selected_count, count)
    } else {
        supportActionBar?.subtitle = statsSubtitle
    }
}

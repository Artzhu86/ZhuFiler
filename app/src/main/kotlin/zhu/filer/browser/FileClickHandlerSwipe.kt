package zhu.filer.browser

import androidx.recyclerview.widget.ItemTouchHelper

// 设置滑动选择
internal fun FileClickHandler.setupSwipeToSelect() {
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

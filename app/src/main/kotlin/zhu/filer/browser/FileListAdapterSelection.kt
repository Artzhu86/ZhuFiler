package zhu.filer.browser

import java.io.File
import zhu.filer.FileItem

// 是否选中
fun FileListAdapter.isSelected(position: Int) = selectedPositions.contains(position)

// 切换选择状态
fun FileListAdapter.toggleSelection(position: Int) {
    if (position == 0 && isUpItem(position)) return
    if (selectedPositions.contains(position)) {
        selectedPositions.remove(position)
    } else {
        selectedPositions.add(position)
    }
    notifyItemChanged(position)
}

// 选择位置
fun FileListAdapter.selectPosition(position: Int) {
    if (position == 0 && isUpItem(position)) return
    if (selectedPositions.add(position)) {
        notifyItemChanged(position)
    }
}

// 清除选择
fun FileListAdapter.clearSelection() {
    val copy = selectedPositions.toSet()
    selectedPositions.clear()
    copy.forEach { notifyItemChanged(it) }
}

// 获取选中文件
fun FileListAdapter.getSelectedFiles(): List<File> {
    return selectedPositions.mapNotNull { items.getOrNull(it)?.file }
        .filter { it.name != ".." }
}

// 获取选中文件项
fun FileListAdapter.getSelectedFileItems(): List<FileItem> {
    return selectedPositions.mapNotNull { items.getOrNull(it) }
        .filter { it.file.name != ".." }
}

// 是否有选择
fun FileListAdapter.hasSelection() = selectedPositions.isNotEmpty()

// 是否上级项
fun FileListAdapter.isUpItem(position: Int): Boolean {
    return position == 0 && items.isNotEmpty() && items[0].file.name == ".."
}

// 获取文件项
fun FileListAdapter.getFileItem(position: Int): FileItem? = items.getOrNull(position)

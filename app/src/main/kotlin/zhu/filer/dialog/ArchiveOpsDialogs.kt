package zhu.filer.dialog

import android.text.format.Formatter
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import zhu.filer.FileItem
import zhu.filer.R
import zhu.filer.operation.FileOpener

// 显示归档条目操作
fun showArchiveItemOpsDialog(
    activity: AppCompatActivity,
    itemView: View,
    item: FileItem,
    fileOpener: FileOpener,
    onCopyCut: (FileItem, Boolean) -> Unit = { _, _ -> },
    onRename: (FileItem, String) -> Unit = { _, _ -> },
    onDelete: (FileItem) -> Unit = {},
    onCompress: (FileItem) -> Unit = {}
) {
    val items = mutableListOf(
        activity.getString(R.string.copy),
        activity.getString(R.string.move),
        activity.getString(R.string.rename),
        activity.getString(R.string.delete)
    )
    if (!item.isDirectory) {
        items.add(activity.getString(R.string.open_with))
        items.add(activity.getString(R.string.share))
    }
    items.add(activity.getString(R.string.compress))
    items.add(activity.getString(R.string.properties))

    val popup = PopupMenu(activity, itemView)
    items.forEachIndexed { index, label ->
        popup.menu.add(0, index, index, label)
    }
    popup.setOnMenuItemClickListener { menuItem ->
        val action = items[menuItem.itemId]
        when (action) {
            activity.getString(R.string.copy) -> onCopyCut(item, false)
            activity.getString(R.string.move) -> onCopyCut(item, true)
            activity.getString(R.string.rename) -> showArchiveRenameDialog(activity, item, onRename)
            activity.getString(R.string.delete) -> {
                MaterialAlertDialogBuilder(activity).setTitle(R.string.delete)
                    .setMessage(activity.getString(R.string.delete_message, item.displayName))
                    .setPositiveButton(R.string.delete) { _, _ -> onDelete(item) }
                    .setNegativeButton(R.string.cancel, null).show()
            }
            activity.getString(R.string.open_with) -> fileOpener.openArchiveEntry(item, forceChoose = true, itemView = itemView)
            activity.getString(R.string.share) -> shareArchiveEntry(activity, item, fileOpener)
            activity.getString(R.string.compress) -> onCompress(item)
            activity.getString(R.string.properties) -> showArchiveEntryDetailsDialog(activity, item)
        }
        true
    }
    popup.show()
}

// 显示归档条目详情对话框
fun showArchiveEntryDetailsDialog(activity: AppCompatActivity, item: FileItem) {
    val sizeStr = Formatter.formatFileSize(activity, item.size)
    val dialog = MaterialAlertDialogBuilder(activity)
        .setTitle(item.displayName)
        .setMessage(
            "${activity.getString(R.string.name_label)}: ${item.displayName}\n" +
            "${activity.getString(R.string.type_label)}: ${if (item.isDirectory) activity.getString(R.string.directory) else activity.getString(R.string.file)}\n" +
            "${activity.getString(R.string.size_label)}: $sizeStr"
        )
        .setPositiveButton(R.string.close, null)
        .create()
    dialog.show()
}

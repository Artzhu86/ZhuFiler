package zhu.filer.ui

import android.content.Context
import android.text.format.Formatter
import android.view.LayoutInflater
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import zhu.filer.R
import zhu.filer.util.getDirStats
import zhu.filer.util.toast

// 显示文件详情对话框
fun showDetailsDialog(activity: AppCompatActivity, file: File) {
    val rows = mutableListOf<Pair<String, String>>()
    rows.add(activity.getString(R.string.name_label) to file.name)
    rows.add(activity.getString(R.string.path_label) to (file.parentFile?.absolutePath ?: file.absolutePath))
    rows.add(activity.getString(R.string.type_label) to if (file.isDirectory) activity.getString(R.string.directory) else activity.getString(R.string.file))
    rows.add(activity.getString(R.string.size_label) to Formatter.formatFileSize(activity, file.length()))
    rows.add(activity.getString(R.string.modified_label) to SimpleDateFormat(activity.getString(R.string.date_format_details), Locale.getDefault()).format(Date(file.lastModified())))
    if (file.isDirectory) {
        val (dirs, files) = getDirStats(file)
        rows.add(activity.getString(R.string.dir_count_label) to dirs.toString())
        rows.add(activity.getString(R.string.file_count_label) to files.toString())
    }

    val table = LayoutInflater.from(activity)
        .inflate(R.layout.dialog_properties, null) as TableLayout
    for ((label, value) in rows) {
        val row = LayoutInflater.from(activity)
            .inflate(R.layout.item_property_row, table, false) as TableRow
        val labelView = row.findViewById<TextView>(R.id.property_label)
        labelView.text = label
        val rippleBg = android.util.TypedValue().let { tv ->
            if (activity.theme.resolveAttribute(android.R.attr.selectableItemBackground, tv, true))
                androidx.core.content.ContextCompat.getDrawable(activity, tv.resourceId)
            else null
        }
        val valueView = row.findViewById<TextView>(R.id.property_value)
        valueView.text = value
        valueView.background = rippleBg
        valueView.setOnLongClickListener {
            val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("value", value))
            toast(activity, activity.getString(R.string.copied_to_clipboard))
            true
        }
        table.addView(row)
    }

    MaterialAlertDialogBuilder(activity)
        .setCustomTitle(buildDialogTitle(activity, R.string.properties))
        .setView(table)
        .setPositiveButton(R.string.ok, null).show()
}

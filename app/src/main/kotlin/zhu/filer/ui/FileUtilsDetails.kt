package zhu.filer.ui

import android.text.format.Formatter
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import zhu.filer.R
import zhu.filer.util.getDirStats

// 显示文件详情对话框
fun showDetailsDialog(activity: AppCompatActivity, file: File) {
    val properties = mutableListOf<Pair<String, String>>()
    properties.add(activity.getString(R.string.name_label) to file.name)
    properties.add(activity.getString(R.string.path_label) to (file.parentFile?.absolutePath ?: file.absolutePath))
    properties.add(activity.getString(R.string.type_label) to if (file.isDirectory) activity.getString(R.string.directory) else activity.getString(R.string.file))
    properties.add(activity.getString(R.string.size_label) to Formatter.formatFileSize(activity, file.length()))
    properties.add(activity.getString(R.string.modified_label) to SimpleDateFormat(activity.getString(R.string.date_format_details), Locale.getDefault()).format(Date(file.lastModified())))
    if (file.isDirectory) {
        val (dirs, files) = getDirStats(file)
        properties.add(activity.getString(R.string.dir_count_label) to dirs.toString())
        properties.add(activity.getString(R.string.file_count_label) to files.toString())
    }

    val view = buildPropertiesView(activity, properties)
    MaterialAlertDialogBuilder(activity)
        .setTitle(R.string.properties)
        .setView(view)
        .setPositiveButton(R.string.ok, null).show()
}

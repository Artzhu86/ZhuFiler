package zhu.filer.dialog

import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import zhu.filer.R
import zhu.filer.ui.buildDialogTitle

// 创建压缩进度对话框
fun createCompressProgressDialog(
    activity: AppCompatActivity,
    onHide: () -> Unit,
    onCancel: () -> Unit
): Pair<AlertDialog, (String, Long, Long, Int, Int) -> Unit> {
    val container = activity.layoutInflater.inflate(R.layout.dialog_compress_progress, null) as LinearLayout
    val fileNameView = container.findViewById<TextView>(R.id.compress_progress_filename)
    val progressBar = container.findViewById<LinearProgressIndicator>(R.id.compress_progress_bar)

    fileNameView.setTextIsSelectable(false)
    fileNameView.setTextColor(MaterialColors.getColor(fileNameView, com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF888888.toInt()))
    progressBar.setProgressCompat(0, false)

    val dialog = MaterialAlertDialogBuilder(activity)
        .setCustomTitle(buildDialogTitle(activity, R.string.compress_progress_title))
        .setView(container)
        .setCancelable(false)
        .setPositiveButton(R.string.hide) { _, _ -> onHide() }
        .setNegativeButton(R.string.cancel) { _, _ -> onCancel() }
        .create()

    val updateProgress: (String, Long, Long, Int, Int) -> Unit = { currentFile, fileBytesRead, fileBytesTotal, _, _ ->
        fileNameView.text = currentFile
        val percent = if (fileBytesTotal > 0) ((fileBytesRead * 100 / fileBytesTotal).toInt()).coerceIn(0, 100) else 100
        progressBar.setProgressCompat(percent, true)
    }

    return dialog to updateProgress
}

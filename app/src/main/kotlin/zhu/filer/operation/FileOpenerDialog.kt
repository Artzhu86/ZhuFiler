package zhu.filer.operation

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.skydoves.transformationlayout.TransformationLayout
import java.io.File
import zhu.filer.util.EXTRA_FILE_PATH
import zhu.filer.R
import zhu.filer.util.TRANSITION_PARAMS_KEY
import zhu.filer.editor.TextEditorActivity
import zhu.filer.media.AudioPlayerActivity
import zhu.filer.media.ImageViewerActivity
import zhu.filer.media.VideoPlayerActivity
import zhu.filer.dialog.applySelectableEffectToListView
import zhu.filer.ui.buildDialogTitle
import zhu.filer.dialog.ApkViewerDialog
import zhu.filer.util.openFileWithSystem
import zhu.filer.ui.showListDialog

// 显示打开方式对话框
internal fun FileOpener.showOpenWithDialog(file: File, sharedView: View? = null) {
    val options = listOf(
        R.drawable.outline_insert_drive_file_24 to activity.getString(R.string.system_open),
        R.drawable.outline_description_24 to activity.getString(R.string.text_editor),
        R.drawable.outline_archive_24 to activity.getString(R.string.archive_viewer),
        R.drawable.outline_image_24 to activity.getString(R.string.image_viewer),
        R.drawable.outline_audio_file_24 to activity.getString(R.string.audio_player),
        R.drawable.outline_video_file_24 to activity.getString(R.string.video_player),
        R.drawable.outline_android_24 to activity.getString(R.string.apk_viewer)
    )
    val adapter = object : ArrayAdapter<Pair<Int, String>>(activity, R.layout.item_open_with, options) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(activity).inflate(R.layout.item_open_with, parent, false)
            val (iconRes, label) = options[position]
            view.findViewById<ImageView>(R.id.open_with_icon).setImageResource(iconRes)
            view.findViewById<TextView>(R.id.open_with_label).text = label
            return view
        }
    }
    val dialog = MaterialAlertDialogBuilder(activity)
        .setCustomTitle(buildDialogTitle(activity, R.string.open_with))
        .setAdapter(adapter) { _, which ->
            when (which) {
                0 -> openFileWithSystem(activity, file)
                1 -> launchActivity(TextEditorActivity::class.java, file, sharedView)
                2 -> launchArchiveViewer(file)
                3 -> launchActivity(ImageViewerActivity::class.java, file, sharedView)
                4 -> launchActivity(AudioPlayerActivity::class.java, file, sharedView)
                5 -> launchActivity(VideoPlayerActivity::class.java, file, sharedView)
                6 -> ApkViewerDialog.show(activity, file) { launchArchiveViewer(file) }
            }
        }
        .setNegativeButton(R.string.cancel, null)
        .create()
    showListDialog(dialog)
    dialog.listView?.let { applySelectableEffectToListView(it) }
}

// 启动Activity
internal fun FileOpener.launchActivity(cls: Class<*>, file: File, sharedView: View?) {
    val intent = Intent(activity, cls).apply {
        putExtra(EXTRA_FILE_PATH, file.absolutePath)
    }
    startActivityWithTransition(intent, sharedView)
}

// 带转场动画启动
internal fun FileOpener.startActivityWithTransition(intent: Intent, sharedView: View?) {
    if (sharedView is TransformationLayout) {
        val bundle = sharedView.withView(sharedView, "shared_content")
        intent.putExtra(TRANSITION_PARAMS_KEY, sharedView.getParcelableParams())
        activity.startActivity(intent, bundle)
    } else {
        activity.startActivity(intent)
    }
}

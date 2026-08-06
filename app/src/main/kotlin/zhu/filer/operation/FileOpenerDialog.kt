package zhu.filer.operation

import android.content.Intent
import android.view.View
import androidx.appcompat.widget.PopupMenu
import com.skydoves.transformationlayout.TransformationLayout
import java.io.File
import zhu.filer.util.EXTRA_FILE_PATH
import zhu.filer.R
import zhu.filer.util.TRANSITION_PARAMS_KEY
import zhu.filer.editor.TextEditorActivity
import zhu.filer.media.AudioPlayerActivity
import zhu.filer.media.ImageViewerActivity
import zhu.filer.media.VideoPlayerActivity
import zhu.filer.dialog.ApkViewerDialog
import zhu.filer.util.openFileWithSystem

// 强制显示PopupMenu图标
private fun PopupMenu.forceShowIcon() {
    try {
        val field = javaClass.getDeclaredField("mPopup")
        field.isAccessible = true
        val helper = field.get(this)
        helper.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.javaPrimitiveType).invoke(helper, true)
    } catch (_: Exception) {
    }
}

// 显示打开方式对话框
internal fun FileOpener.showOpenWithDialog(itemView: View, file: File, sharedView: View? = null) {
    val options = listOf(
        R.drawable.outline_insert_drive_file_24 to activity.getString(R.string.system_open),
        R.drawable.outline_description_24 to activity.getString(R.string.text_editor),
        R.drawable.outline_archive_24 to activity.getString(R.string.archive_viewer),
        R.drawable.outline_image_24 to activity.getString(R.string.image_viewer),
        R.drawable.outline_audio_file_24 to activity.getString(R.string.audio_player),
        R.drawable.outline_video_file_24 to activity.getString(R.string.video_player),
        R.drawable.outline_android_24 to activity.getString(R.string.apk_viewer)
    )
    val popup = PopupMenu(activity, itemView)
    popup.forceShowIcon()
    options.forEachIndexed { index, (iconRes, label) ->
        popup.menu.add(0, index, index, label).setIcon(iconRes)
    }
    popup.setOnMenuItemClickListener { menuItem ->
        when (menuItem.itemId) {
            0 -> openFileWithSystem(activity, file)
            1 -> launchActivity(TextEditorActivity::class.java, file, sharedView)
            2 -> launchArchiveViewer(file)
            3 -> launchActivity(ImageViewerActivity::class.java, file, sharedView)
            4 -> launchActivity(AudioPlayerActivity::class.java, file, sharedView)
            5 -> launchActivity(VideoPlayerActivity::class.java, file, sharedView)
            6 -> ApkViewerDialog.show(activity, file) { launchArchiveViewer(file) }
        }
        true
    }
    popup.show()
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

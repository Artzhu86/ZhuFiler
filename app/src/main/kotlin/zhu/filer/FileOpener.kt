package zhu.filer

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.skydoves.transformationlayout.TransformationLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// 文件打开器
class FileOpener(
    private val activity: AppCompatActivity,
    private val browserController: FileBrowserController,
    private val lifecycleScope: CoroutineScope,
    private val progressBar: CircularProgressIndicator
) {

    // 打开文件
    fun openFile(file: File, sharedView: View? = null, forceChoose: Boolean = false) {
        if (!file.canRead()) {
            toast(activity, activity.getString(R.string.cannot_read))
            return
        }
        if (forceChoose) {
            showOpenWithDialog(file, sharedView)
        } else {
            openByType(file, sharedView)
        }
    }

    // 打开归档条目
    fun openArchiveEntry(item: FileItem, forceChoose: Boolean = false) {
        if (item.encrypted) {
            val cached = browserController.getArchivePassword()
            if (cached != null) {
                extractAndOpen(item, cached, forceChoose)
            } else {
                showArchivePasswordDialog(activity) { password ->
                    browserController.cacheArchivePassword(password)
                    extractAndOpen(item, password, forceChoose)
                }
            }
        } else {
            extractAndOpen(item, null, forceChoose)
        }
    }

    // 解压并打开
    private fun extractAndOpen(item: FileItem, password: String?, forceChoose: Boolean) {
        val archiveFile = browserController.getArchiveFile() ?: return
        lifecycleScope.launch {
            progressBar.show()
            try {
                val tempDir = File(activity.cacheDir, "archive_extract").apply { mkdirs() }
                val tempFile = File(tempDir, item.displayName)
                if (tempFile.exists()) tempFile.delete()
                val success = withContext(Dispatchers.IO) {
                    ArchiveEngine.extractEntry(archiveFile, item.entryPath!!, password, tempFile)
                }
                if (success && tempFile.exists()) {
                    openFile(tempFile, forceChoose = forceChoose)
                } else {
                    toast(activity, activity.getString(R.string.extract_failed))
                }
            } catch (e: WrongArchivePasswordException) {
                toast(activity, activity.getString(R.string.wrong_password))
            } catch (e: Exception) {
                toast(activity, activity.getString(R.string.extract_failed))
            } finally {
                progressBar.hide()
            }
        }
    }

    // 显示打开方式对话框
    private fun showOpenWithDialog(file: File, sharedView: View?) {
        val options = listOf(
            R.drawable.outline_insert_drive_file_24 to activity.getString(R.string.system_open),
            R.drawable.outline_description_24 to activity.getString(R.string.text_editor),
            R.drawable.outline_archive_24 to activity.getString(R.string.archive_viewer),
            R.drawable.outline_image_24 to activity.getString(R.string.image_viewer),
            R.drawable.outline_audio_file_24 to activity.getString(R.string.audio_player),
            R.drawable.outline_video_file_24 to activity.getString(R.string.video_player)
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
            .setTitle(R.string.open_with)
            .setAdapter(adapter) { _, which ->
                when (which) {
                    0 -> openFileWithSystem(activity, file)
                    1 -> launchTextEditor(file, sharedView)
                    2 -> launchArchiveViewer(file)
                    3 -> launchImageViewer(file, sharedView)
                    4 -> launchAudioPlayer(file, sharedView)
                    5 -> launchVideoPlayer(file, sharedView)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
        dialog.show()
        dialog.listView?.let { applySelectableEffectToListView(it) }
    }

    // 按类型自动打开
    private fun openByType(file: File, sharedView: View?) {
        when {
            FileType.isArchive(file) -> launchArchiveViewer(file)
            FileType.isImage(file) -> launchImageViewer(file, sharedView)
            FileType.isAudio(file) -> launchAudioPlayer(file, sharedView)
            FileType.isVideo(file) -> launchVideoPlayer(file, sharedView)
            FileType.isText(file) -> launchTextEditor(file, sharedView)
            else -> openFileWithSystem(activity, file)
        }
    }

    // 启动归档查看器
    private fun launchArchiveViewer(file: File) {
        browserController.launchArchiveViewer(file, onPasswordRequired = {
            showArchivePasswordDialog(activity) { pwd ->
                browserController.launchArchiveViewer(file, pwd)
            }
        })
    }

    // 启动文本编辑器
    private fun launchTextEditor(file: File, sharedView: View?) {
        val intent = Intent(activity, TextEditorActivity::class.java).apply {
            putExtra(TextEditorActivity.EXTRA_FILE_PATH, file.absolutePath)
        }
        startActivityWithTransition(intent, sharedView)
    }

    // 启动图像查看器
    private fun launchImageViewer(file: File, sharedView: View?) {
        val intent = Intent(activity, ImageViewerActivity::class.java).apply {
            putExtra(ImageViewerActivity.EXTRA_FILE_PATH, file.absolutePath)
        }
        startActivityWithTransition(intent, sharedView)
    }

    // 启动音频播放器
    private fun launchAudioPlayer(file: File, sharedView: View?) {
        val intent = Intent(activity, AudioPlayerActivity::class.java).apply {
            putExtra(AudioPlayerActivity.EXTRA_FILE_PATH, file.absolutePath)
        }
        startActivityWithTransition(intent, sharedView)
    }

    // 启动视频播放器
    private fun launchVideoPlayer(file: File, sharedView: View?) {
        val intent = Intent(activity, VideoPlayerActivity::class.java).apply {
            putExtra(VideoPlayerActivity.EXTRA_FILE_PATH, file.absolutePath)
        }
        startActivityWithTransition(intent, sharedView)
    }

    // 带转场动画启动
    private fun startActivityWithTransition(intent: Intent, sharedView: View?) {
        if (sharedView is TransformationLayout) {
            val bundle = sharedView.withView(sharedView, "shared_content")
            intent.putExtra("TransformationParams", sharedView.getParcelableParams())
            activity.startActivity(intent, bundle)
        } else {
            activity.startActivity(intent)
        }
    }
}

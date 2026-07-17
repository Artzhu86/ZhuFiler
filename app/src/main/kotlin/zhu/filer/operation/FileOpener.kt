package zhu.filer.operation

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import zhu.filer.archive.ArchiveEngine
import zhu.filer.archive.WrongArchivePasswordException
import zhu.filer.archive.extractEntry
import zhu.filer.browser.FileBrowserController
import zhu.filer.browser.launchArchiveViewer
import zhu.filer.FileItem
import zhu.filer.FileType
import zhu.filer.R
import zhu.filer.dialog.ApkViewerDialog
import zhu.filer.dialog.showArchivePasswordDialog
import zhu.filer.editor.TextEditorActivity
import zhu.filer.media.AudioPlayerActivity
import zhu.filer.media.ImageViewerActivity
import zhu.filer.media.VideoPlayerActivity
import zhu.filer.util.openFileWithSystem
import zhu.filer.util.toast

// 文件打开器
class FileOpener(
    internal val activity: AppCompatActivity,
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

    // 获取归档文件
    fun getArchiveFile(): File? = browserController.getArchiveFile()

    // 获取归档密码
    fun getArchivePassword(): String? = browserController.getArchivePassword()

    // 打开归档条目
    fun openArchiveEntry(item: FileItem, sharedView: View? = null, forceChoose: Boolean = false) {
        if (item.encrypted) {
            val cached = browserController.getArchivePassword()
            if (cached != null) {
                extractAndOpen(item, cached, sharedView, forceChoose)
            } else {
                showArchivePasswordDialog(activity) { password ->
                    browserController.cacheArchivePassword(password)
                    extractAndOpen(item, password, sharedView, forceChoose)
                }
            }
        } else {
            extractAndOpen(item, null, sharedView, forceChoose)
        }
    }

    // 解压并打开
    private fun extractAndOpen(item: FileItem, password: String?, sharedView: View?, forceChoose: Boolean) {
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
                    openFile(tempFile, sharedView, forceChoose)
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

    // 按类型自动打开
    private fun openByType(file: File, sharedView: View?) {
        when {
            FileType.isArchive(file) -> launchArchiveViewer(file)
            FileType.isApk(file) -> ApkViewerDialog.show(activity, file) { launchArchiveViewer(file) }
            FileType.isImage(file) -> launchActivity(ImageViewerActivity::class.java, file, sharedView)
            FileType.isAudio(file) -> launchActivity(AudioPlayerActivity::class.java, file, sharedView)
            FileType.isVideo(file) -> launchActivity(VideoPlayerActivity::class.java, file, sharedView)
            FileType.isText(file) -> launchActivity(TextEditorActivity::class.java, file, sharedView)
            else -> openFileWithSystem(activity, file)
        }
    }

    // 启动归档查看器
    internal fun launchArchiveViewer(file: File) {
        browserController.launchArchiveViewer(file, onPasswordRequired = {
            showArchivePasswordDialog(activity) { pwd ->
                browserController.launchArchiveViewer(file, pwd)
            }
        })
    }
}

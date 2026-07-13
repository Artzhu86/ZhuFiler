package zhu.filer

import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// 文件操作控制器
class FileOperationsController(
    private val activity: AppCompatActivity,
    private val browserController: FileBrowserController,
    private val lifecycleScope: CoroutineScope,
    private val progressBar: CircularProgressIndicator,
    private val fabManager: FabManager,
    private val clipboard: ClipboardManager,
    private val loadDir: (File, Boolean, Boolean, Int?) -> Unit,
    private val refreshDir: (File, String?) -> Unit
) {

    // 执行粘贴
    suspend fun performPaste(files: List<File>, targetDir: File, isMove: Boolean, overwrite: Boolean) {
        browserController.saveScrollPosition()
        val savedPos = browserController.getCurrentScrollPosition()

        progressBar.show()
        var successCount = 0
        var skipCount = 0
        var failCount = 0
        for (file in files) {
            val targetFile = File(targetDir, file.name)
            try {
                if (targetFile.exists()) {
                    if (overwrite) {
                        if (targetFile.isDirectory) targetFile.deleteRecursively() else targetFile.delete()
                    } else {
                        skipCount++
                        continue
                    }
                }
                val opSuccess = if (isMove) {
                    if (file.renameTo(targetFile)) {
                        true
                    } else {
                        if (file.isDirectory) {
                            file.copyRecursively(targetFile, overwrite = true)
                            file.deleteRecursively()
                        } else {
                            file.copyTo(targetFile, overwrite = true)
                            file.delete()
                        }
                        true
                    }
                } else {
                    if (file.isDirectory) {
                        file.copyRecursively(targetFile, overwrite = overwrite)
                    } else {
                        file.copyTo(targetFile, overwrite = overwrite)
                    }
                    true
                }
                if (opSuccess) successCount++ else failCount++
            } catch (e: Exception) {
                failCount++
            }
        }
        progressBar.hide()
        val msgRes = when {
            failCount == 0 && skipCount == 0 -> if (isMove) R.string.move_success else R.string.copy_success
            failCount == 0 && skipCount > 0 -> if (isMove) R.string.move_success_skip else R.string.copy_success_skip
            failCount > 0 -> R.string.paste_result_partial
            else -> R.string.paste_failed
        }
        val msg = when (msgRes) {
            R.string.move_success, R.string.copy_success -> activity.getString(msgRes, successCount)
            R.string.move_success_skip, R.string.copy_success_skip -> activity.getString(msgRes, successCount, skipCount)
            R.string.paste_result_partial -> activity.getString(msgRes, successCount, failCount)
            else -> activity.getString(msgRes)
        }
        toast(activity, msg)
        clipboard.clear()
        fabManager.updatePasteButtons(clipboard)
        loadDir(targetDir, true, false, savedPos)
    }

    // 执行压缩
    fun performCompress(sources: List<File>, outputFile: File, format: ArchiveFormat, password: String?) {
        var compressJob: Job? = null

        val (progressDialog, updateProgress) = createCompressProgressDialog(
            activity,
            onHide = { },
            onCancel = { compressJob?.cancel() }
        )
        progressDialog.show()

        browserController.saveScrollPosition()
        val savedPos = browserController.getCurrentScrollPosition()

        compressJob = lifecycleScope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    ArchiveEngine.createArchive(outputFile, sources, browserController.currentDir, format, password) { currentFile, fileBytesRead, fileBytesTotal, fileIndex, fileCount ->
                        activity.runOnUiThread {
                            if (progressDialog.isShowing) {
                                updateProgress(currentFile, fileBytesRead, fileBytesTotal, fileIndex, fileCount)
                            }
                        }
                    }
                }
                toast(activity, activity.getString(R.string.compress_success))
                loadDir(browserController.currentDir, true, false, savedPos)
            } catch (e: CancellationException) {
                withContext(NonCancellable + Dispatchers.IO) {
                    if (outputFile.exists()) outputFile.delete()
                }
                loadDir(browserController.currentDir, true, false, savedPos)
            } catch (e: Exception) {
                toast(activity, activity.getString(R.string.compress_failed))
            } finally {
                if (progressDialog.isShowing) progressDialog.dismiss()
            }
        }
    }
}

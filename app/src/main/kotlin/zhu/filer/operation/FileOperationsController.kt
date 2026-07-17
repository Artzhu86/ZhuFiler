package zhu.filer.operation

import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.CoroutineScope
import java.io.File
import zhu.filer.ui.FabManager
import zhu.filer.browser.FileBrowserController
import zhu.filer.browser.getCurrentScrollPosition
import zhu.filer.browser.saveScrollPosition
import zhu.filer.R
import zhu.filer.util.ShizukuManager
import zhu.filer.util.toast
import zhu.filer.ui.updatePasteButtons

// 文件操作控制器
class FileOperationsController(
    internal val activity: AppCompatActivity,
    internal val browserController: FileBrowserController,
    internal val lifecycleScope: CoroutineScope,
    internal val progressBar: CircularProgressIndicator,
    internal val fabManager: FabManager,
    internal val clipboard: ClipboardManager,
    internal val loadDir: (File, Boolean, Boolean, Int?) -> Unit,
    private val refreshDir: (File, String?) -> Unit
) {

    // 执行粘贴
    suspend fun performPaste(files: List<File>, targetDir: File, isMove: Boolean, overwrite: Boolean) {
        if (browserController.isInArchive()) {
            performArchivePaste(files, isMove)
            return
        }
        browserController.saveScrollPosition()
        val savedPos = browserController.getCurrentScrollPosition()
        if (isMove) {
            files.firstOrNull()?.parentFile?.let { browserController.markDirty(it.absolutePath) }
        }
        browserController.markDirty(targetDir.absolutePath)

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
                    if (ShizukuManager.hasPermission()) {
                        tryShizukuMove(file, targetFile)
                    } else {
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
                    }
                } else {
                    if (ShizukuManager.hasPermission()) {
                        tryShizukuCopy(file, targetFile)
                    } else {
                        try {
                            if (file.isDirectory) {
                                file.copyRecursively(targetFile, overwrite = overwrite)
                            } else {
                                file.copyTo(targetFile, overwrite = overwrite)
                            }
                            true
                        } catch (e: Exception) {
                            false
                        }
                    }
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
}

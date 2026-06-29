package zhu.filer

import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FileOperationHelper(
    private val activity: AppCompatActivity,
    private val progressBar: ProgressBar,
    private val onComplete: () -> Unit
) {

    fun performPaste(source: File, target: File, isMove: Boolean, overwrite: Boolean) {
        activity.lifecycleScope.launch {
            progressBar.isVisible = true
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val targetParent = target.parentFile
                    if (targetParent != null && !targetParent.canWrite()) throw Exception("目标目录不可写")
                    if (overwrite && target.exists()) {
                        if (target.isDirectory) target.deleteRecursively() else target.delete()
                    }
                    when {
                        isMove -> {
                            if (!source.renameTo(target)) {
                                if (source.isDirectory) {
                                    source.copyRecursively(target, overwrite = true)
                                    source.deleteRecursively()
                                } else {
                                    source.copyTo(target, overwrite = true)
                                    source.delete()
                                }
                            }
                        }
                        else -> {
                            if (source.isDirectory) source.copyRecursively(target, overwrite = true)
                            else source.copyTo(target, overwrite = true)
                        }
                    }
                }
            }
            progressBar.isVisible = false
            if (result.isFailure) toast(activity, "操作失败: ${result.exceptionOrNull()?.message ?: "未知错误"}")
            onComplete()
        }
    }
}
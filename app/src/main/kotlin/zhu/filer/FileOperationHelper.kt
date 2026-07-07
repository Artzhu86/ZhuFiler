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
                    if (targetParent != null && !targetParent.canWrite()) {
                        throw Exception(activity.getString(R.string.target_not_writable))
                    }
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
            if (result.isFailure) {
                val errorMsg = result.exceptionOrNull()?.message ?: activity.getString(R.string.unknown_error)
                val msg = activity.getString(R.string.operation_failed, errorMsg)
                toast(activity, msg)
            }
            onComplete()
        }
    }
}
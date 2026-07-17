package zhu.filer.ui

import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import zhu.filer.R
import zhu.filer.operation.ClipboardManager

// 处理粘贴操作
internal fun FabManager.handlePaste(clipboard: ClipboardManager) {
    val files = clipboard.getFiles()
    if (files.isEmpty()) return
    val targetDir = targetDirProvider!!()
    val isMove = clipboard.isCut()
    val conflicts = files.filter { File(targetDir, it.name).exists() }
    if (conflicts.isNotEmpty()) {
        MaterialAlertDialogBuilder(activity)
            .setCustomTitle(buildDialogTitle(activity, R.string.target_exists))
            .setMessage(activity.getString(R.string.overwrite_conflict, conflicts.size))
            .setPositiveButton(R.string.overwrite) { _, _ ->
                onPaste?.invoke(files, isMove, true)
            }
            .setNegativeButton(R.string.skip) { _, _ ->
                onPaste?.invoke(files, isMove, false)
            }
            .show()
    } else {
        onPaste?.invoke(files, isMove, false)
    }
}

// 更新粘贴按钮状态
fun FabManager.updatePasteButtons(clipboard: ClipboardManager) {
    if (clipboard.hasContent()) {
        showMode(FabManager.FabMode.PASTE, R.drawable.outline_content_paste_24)
    } else {
        hideAll()
    }
}

// 更新多选按钮状态
fun FabManager.updateMultiSelectButtons(show: Boolean) {
    if (show) {
        showMode(FabManager.FabMode.MULTI_SELECT, R.drawable.outline_select_all_24)
    } else {
        hideAll()
    }
}

// 显示指定模式按钮
internal fun FabManager.showMode(mode: FabManager.FabMode, iconRes: Int) {
    if (currentMode == mode && fabAction.isVisible && fabAction.alpha == 1f) return
    currentMode = mode
    fabAction.setImageResource(iconRes)
    fabAction.animate().cancel()
    fabCancel.animate().cancel()
    if (!fabAction.isVisible) {
        fabAction.isVisible = true
        fabCancel.isVisible = true
        fabAction.alpha = 0f
        fabCancel.alpha = 0f
        fabAction.scaleX = 0.8f
        fabAction.scaleY = 0.8f
        fabCancel.scaleX = 0.8f
        fabCancel.scaleY = 0.8f
        fabAction.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(activity.resources.getInteger(R.integer.fab_show_duration_ms).toLong())
            .setInterpolator(DecelerateInterpolator())
            .start()
        fabCancel.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(activity.resources.getInteger(R.integer.fab_show_duration_ms).toLong())
            .setStartDelay(activity.resources.getInteger(R.integer.fab_show_cancel_delay_ms).toLong())
            .setInterpolator(DecelerateInterpolator())
            .start()
    } else {
        fabAction.alpha = 1f
        fabAction.scaleX = 1f
        fabAction.scaleY = 1f
        fabCancel.alpha = 1f
        fabCancel.scaleX = 1f
        fabCancel.scaleY = 1f
    }
}

// 隐藏所有按钮
internal fun FabManager.hideAll() {
    if (currentMode == FabManager.FabMode.NONE && !fabAction.isVisible) return
    currentMode = FabManager.FabMode.NONE
    if (fabAction.isVisible) {
        fabAction.animate().cancel()
        fabCancel.animate().cancel()
        fabAction.animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(activity.resources.getInteger(R.integer.fab_hide_duration_ms).toLong())
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                if (currentMode == FabManager.FabMode.NONE) {
                    fabAction.isVisible = false
                    fabAction.alpha = 1f
                    fabAction.scaleX = 1f
                    fabAction.scaleY = 1f
                }
            }
            .start()
        fabCancel.animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(activity.resources.getInteger(R.integer.fab_hide_duration_ms).toLong())
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                if (currentMode == FabManager.FabMode.NONE) {
                    fabCancel.isVisible = false
                    fabCancel.alpha = 1f
                    fabCancel.scaleX = 1f
                    fabCancel.scaleY = 1f
                }
            }
            .start()
    }
}

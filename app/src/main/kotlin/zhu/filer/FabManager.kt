package zhu.filer

import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File

// 悬浮按钮管理器
class FabManager(private val activity: AppCompatActivity) {

    private lateinit var fabAction: FloatingActionButton
    private lateinit var fabCancel: FloatingActionButton
    private var onPaste: ((List<File>, Boolean, Boolean) -> Unit)? = null
    private var onCancel: (() -> Unit)? = null
    private var targetDirProvider: (() -> File)? = null
    private var onSelectAll: (() -> Unit)? = null
    private var onDeselect: (() -> Unit)? = null
    private var currentMode: FabMode = FabMode.NONE

    // 悬浮按钮模式
    enum class FabMode { NONE, PASTE, MULTI_SELECT }

    // 初始化悬浮按钮
    fun setup(
        fabAction: FloatingActionButton,
        fabCancel: FloatingActionButton,
        clipboard: ClipboardManager,
        targetDirProvider: () -> File,
        onPaste: (List<File>, Boolean, Boolean) -> Unit,
        onCancel: () -> Unit
    ) {
        this.fabAction = fabAction
        this.fabCancel = fabCancel
        this.targetDirProvider = targetDirProvider
        this.onPaste = onPaste
        this.onCancel = onCancel

        fabAction.setOnClickListener {
            when (currentMode) {
                FabMode.PASTE -> handlePaste(clipboard)
                FabMode.MULTI_SELECT -> onSelectAll?.invoke()
                else -> {}
            }
        }

        fabCancel.setOnClickListener {
            when (currentMode) {
                FabMode.PASTE -> onCancel()
                FabMode.MULTI_SELECT -> onDeselect?.invoke()
                else -> {}
            }
        }
    }

    // 处理粘贴操作
    private fun handlePaste(clipboard: ClipboardManager) {
        val files = clipboard.getFiles()
        if (files.isEmpty()) return
        val targetDir = targetDirProvider!!()
        val isMove = clipboard.isCut()
        val conflicts = files.filter { File(targetDir, it.name).exists() }
        if (conflicts.isNotEmpty()) {
            MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.target_exists)
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

    // 设置多选操作回调
    fun setMultiSelectActions(onSelectAll: () -> Unit, onDeselect: () -> Unit) {
        this.onSelectAll = onSelectAll
        this.onDeselect = onDeselect
    }

    // 更新粘贴按钮状态
    fun updatePasteButtons(clipboard: ClipboardManager) {
        if (clipboard.hasContent()) {
            showMode(FabMode.PASTE, R.drawable.outline_content_paste_24)
        } else {
            hideAll()
        }
    }

    // 更新多选按钮状态
    fun updateMultiSelectButtons(show: Boolean) {
        if (show) {
            showMode(FabMode.MULTI_SELECT, R.drawable.outline_select_all_24)
        } else {
            hideAll()
        }
    }

    // 显示指定模式按钮
    private fun showMode(mode: FabMode, iconRes: Int) {
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
    private fun hideAll() {
        if (currentMode == FabMode.NONE && !fabAction.isVisible) return
        currentMode = FabMode.NONE
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
                    if (currentMode == FabMode.NONE) {
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
                    if (currentMode == FabMode.NONE) {
                        fabCancel.isVisible = false
                        fabCancel.alpha = 1f
                        fabCancel.scaleX = 1f
                        fabCancel.scaleY = 1f
                    }
                }
                .start()
        }
    }
}

package zhu.filer.ui

import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import zhu.filer.operation.ClipboardManager

// 悬浮按钮管理器
class FabManager(internal val activity: AppCompatActivity) {

    internal lateinit var fabAction: FloatingActionButton
    internal lateinit var fabCancel: FloatingActionButton
    internal var onPaste: ((List<File>, Boolean, Boolean) -> Unit)? = null
    internal var onCancel: (() -> Unit)? = null
    internal var targetDirProvider: (() -> File)? = null
    internal var onSelectAll: (() -> Unit)? = null
    internal var onDeselect: (() -> Unit)? = null
    internal var currentMode: FabMode = FabMode.NONE

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

    // 设置多选操作回调
    fun setMultiSelectActions(onSelectAll: () -> Unit, onDeselect: () -> Unit) {
        this.onSelectAll = onSelectAll
        this.onDeselect = onDeselect
    }
}

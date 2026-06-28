package zhu.filer

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

fun showNavigateDialog(activity: AppCompatActivity, currentDir: File, loadDir: suspend (File) -> Unit, prefs: android.content.SharedPreferences) {
    val rootLayout = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dpToPx(activity, 16), dpToPx(activity, 16), dpToPx(activity, 16), 0)
    }
    val (inputLayout, editText) = createInput(activity, currentDir.absolutePath)
    rootLayout.addView(inputLayout)

    lateinit var dialog: AlertDialog
    val builder = MaterialAlertDialogBuilder(activity)
        .setTitle("工作目录")
        .setView(rootLayout)
        .setPositiveButton("切换") { _, _ ->
            val path = editText.text?.toString()?.trim() ?: ""
            if (path.isNotEmpty()) {
                val targetDir = File(path)
                if (targetDir.exists() && targetDir.isDirectory) {
                    activity.lifecycleScope.launch { loadDir(targetDir) }
                } else {
                    toast(activity, "目录不存在或不是目录")
                }
            }
        }
        .setNeutralButton("最近") { _, _ ->
            val recent = getRecentDirs(prefs)
            val files = recent.map { File(it) }
            val items = files.map { file ->
                FileItem(file, file.name, "📁", file.absolutePath)
            }

            lateinit var recentDialog: AlertDialog

            val rv = RecyclerView(activity).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(activity, 400))
                layoutManager = LinearLayoutManager(activity)
                adapter = FileListAdapter(
                    onItemClick = { file, _ ->
                        recentDialog.dismiss()
                        Handler(Looper.getMainLooper()).postDelayed({
                            activity.lifecycleScope.launch { loadDir(file) }
                        }, CLICK_DELAY_MS)
                    },
                    onItemLongClick = { _, _ -> false }
                ).apply { submitList(items) }
            }

            recentDialog = MaterialAlertDialogBuilder(activity)
                .setTitle("最近")
                .setView(rv)
                .setNegativeButton("取消", null)
                .show()
        }
        .setNegativeButton("取消", null)
    dialog = builder.show()
    focusAndShowKeyboard(editText, dialog)
    editText.post { editText.selectAll() }
}

fun showCreate(activity: AppCompatActivity, currentDir: File, loadDir: suspend (File) -> Unit) {
    val rootLayout = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dpToPx(activity, 16), dpToPx(activity, 16), dpToPx(activity, 16), 0)
    }
    val (inputLayout, edit) = createInput(activity)
    rootLayout.addView(inputLayout)

    MaterialAlertDialogBuilder(activity)
        .setTitle("创建")
        .setView(rootLayout)
        .setPositiveButton("文件") { _, _ ->
            val name = edit.text?.toString()?.trim() ?: ""
            if (isValid(name)) {
                val f = File(currentDir, name)
                activity.lifecycleScope.launch {
                    val ok = withContext(Dispatchers.IO) { f.createNewFile() }
                    if (!ok) toast(activity, "创建失败")
                    if (ok) loadDir(currentDir)
                }
            } else toast(activity, "名称无效")
        }
        .setNegativeButton("目录") { _, _ ->
            val name = edit.text?.toString()?.trim() ?: ""
            if (isValid(name)) {
                val d = File(currentDir, name)
                activity.lifecycleScope.launch {
                    val ok = withContext(Dispatchers.IO) { d.mkdir() }
                    if (!ok) toast(activity, "创建失败")
                    if (ok) loadDir(currentDir)
                }
            } else toast(activity, "名称无效")
        }
        .setNeutralButton("取消", null)
        .show()
        .let { focusAndShowKeyboard(edit, it) }
}

fun showRenameDialog(activity: AppCompatActivity, currentDir: File, loadDir: suspend (File) -> Unit, file: File) {
    val oldName = file.name
    val rootLayout = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dpToPx(activity, 16), dpToPx(activity, 16), dpToPx(activity, 16), 0)
    }
    val (inputLayout, editText) = createInput(activity, oldName)
    rootLayout.addView(inputLayout)

    val dotIndex = oldName.lastIndexOf('.')
    if (dotIndex > 0 && !file.isDirectory) {
        editText.setSelection(0, dotIndex)
    } else {
        editText.selectAll()
    }

    MaterialAlertDialogBuilder(activity)
        .setTitle("重命名")
        .setView(rootLayout)
        .setPositiveButton("确定") { _, _ ->
            val newName = editText.text?.toString()?.trim() ?: ""
            when {
                newName.isEmpty() -> toast(activity, "名称不能为空")
                newName == oldName -> toast(activity, "名称未改变")
                !isValid(newName) -> toast(activity, "包含非法字符")
                else -> {
                    val parent = file.parent ?: run {
                        toast(activity, "无法重命名根目录")
                        return@setPositiveButton
                    }
                    val newFile = File(parent, newName)
                    if (newFile.exists()) {
                        toast(activity, "同名文件已存在")
                        return@setPositiveButton
                    }
                    activity.lifecycleScope.launch {
                        val ok = withContext(Dispatchers.IO) { file.renameTo(newFile) }
                        if (ok) {
                            toast(activity, "重命名成功")
                            loadDir(currentDir)
                        } else {
                            toast(activity, "重命名失败，请检查权限")
                        }
                    }
                }
            }
        }
        .setNegativeButton("取消", null)
        .show()
        .let { focusAndShowKeyboard(editText, it) }
}

fun showOps(activity: AppCompatActivity, currentDir: File, loadDir: suspend (File) -> Unit, file: File, progressBar: android.widget.ProgressBar) {
    val items = mutableListOf("复制", "移动", "重命名", "删除")
    if (!file.isDirectory) {
        items.add("打开方式")
        items.add("分享")
    }
    items.add("属性")

    val dialog = MaterialAlertDialogBuilder(activity)
        .setTitle(file.name)
        .setItems(items.toTypedArray()) { _, which ->
            val action = items[which]
            when (action) {
                "重命名" -> showRenameDialog(activity, currentDir, loadDir, file)
                "复制" -> showCopyMoveDialog(activity, currentDir, loadDir, file, isMove = false, progressBar)
                "移动" -> showCopyMoveDialog(activity, currentDir, loadDir, file, isMove = true, progressBar)
                "删除" -> {
                    MaterialAlertDialogBuilder(activity).setTitle("删除").setMessage("确定删除 ${file.name} 吗？")
                        .setPositiveButton("删除") { _, _ ->
                            activity.lifecycleScope.launch {
                                val ok = withContext(Dispatchers.IO) { if (file.isDirectory) file.deleteRecursively() else file.delete() }
                                if (!ok) toast(activity, "删除失败")
                                if (ok) loadDir(currentDir)
                            }
                        }.setNegativeButton("取消", null).show()
                }
                "打开方式" -> previewFile(activity, file, forceChoose = true)
                "分享" -> shareFile(activity, activity.packageName, file)
                "属性" -> showDetails(activity, file)
            }
        }
        .setNegativeButton("取消", null)
        .create()
    dialog.show()
}

fun showCopyMoveDialog(activity: AppCompatActivity, currentDir: File, loadDir: suspend (File) -> Unit, source: File, isMove: Boolean, progressBar: android.widget.ProgressBar) {
    val defaultPath = if (isMove) source.absolutePath else File(currentDir, source.name).absolutePath

    val rootLayout = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dpToPx(activity, 16), dpToPx(activity, 16), dpToPx(activity, 16), 0)
    }
    val (inputLayout, editText) = createInput(activity, defaultPath)
    rootLayout.addView(inputLayout)
    editText.selectAll()

    MaterialAlertDialogBuilder(activity)
        .setTitle(if (isMove) "移动" else "复制")
        .setView(rootLayout)
        .setPositiveButton("确定") { _, _ ->
            val targetPath = editText.text?.toString()?.trim() ?: ""
            if (targetPath.isEmpty()) { toast(activity, "请输入目标路径"); return@setPositiveButton }

            val targetFile = if (targetPath.startsWith("/")) File(targetPath) else File(currentDir, targetPath)

            val finalTarget = if (targetFile.exists() && targetFile.isDirectory) {
                File(targetFile, source.name)
            } else targetFile

            if (finalTarget.absolutePath == source.absolutePath) {
                toast(activity, "目标与源相同")
                return@setPositiveButton
            }
            if (source.isDirectory && finalTarget.absolutePath.startsWith(source.absolutePath + File.separator)) {
                toast(activity, "不能移动到自身内部")
                return@setPositiveButton
            }

            if (finalTarget.exists()) {
                MaterialAlertDialogBuilder(activity)
                    .setTitle("目标已存在")
                    .setMessage("${finalTarget.name} 已存在，是否覆盖？")
                    .setPositiveButton("覆盖") { _, _ ->
                        performFileOperation(activity, currentDir, loadDir, source, finalTarget, isMove, true, progressBar)
                    }
                    .setNegativeButton("跳过") { _, _ -> toast(activity, "已跳过") }
                    .show()
            } else {
                performFileOperation(activity, currentDir, loadDir, source, finalTarget, isMove, false, progressBar)
            }
        }
        .setNegativeButton("取消", null)
        .show()
        .let { focusAndShowKeyboard(editText, it) }
}

private fun performFileOperation(activity: AppCompatActivity, currentDir: File, loadDir: suspend (File) -> Unit, source: File, target: File, isMove: Boolean, overwrite: Boolean, progressBar: android.widget.ProgressBar) {
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
        loadDir(currentDir)
    }
}

fun applySelectableEffectToListView(listView: ListView) {
    for (i in 0 until listView.childCount) {
        val child = listView.getChildAt(i)
        child?.applySelectableEffect()
    }
    listView.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
        override fun onChildViewAdded(parent: View, child: View) = child.applySelectableEffect()
        override fun onChildViewRemoved(parent: View, child: View) {}
    })
}

fun View.applySelectableEffect() {
    val highlightColor = getThemeColor(context, android.R.attr.colorControlHighlight)
    val ripple = RippleDrawable(ColorStateList.valueOf(highlightColor), null, null)
    foreground = ripple
}
package zhu.filer

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.format.Formatter
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

val textExts = setOf("txt", "log", "md", "json", "xml", "kt", "java", "c", "cpp", "py", "html", "css", "js")
val imageExts = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
val fileComparator = compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase(Locale.ROOT) }
val CLICK_DELAY_MS = 100L
val DATE_FORMAT = SimpleDateFormat("yy-MM-dd HH:mm", Locale.getDefault())
const val TEXT_PREVIEW_MAX_BYTES = 1024 * 1024

fun toast(context: Context, msg: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(context, msg, duration).show()
}

fun dpToPx(context: Context, dp: Int): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), context.resources.displayMetrics).toInt()

fun getThemeColor(context: Context, attr: Int, fallback: Int = android.graphics.Color.TRANSPARENT): Int {
    val tv = TypedValue()
    return if (context.theme.resolveAttribute(attr, tv, true)) tv.data else fallback
}

fun createInput(context: Context, initial: String = ""): Pair<TextInputLayout, TextInputEditText> {
    val tl = TextInputLayout(context).apply {
        hint = null
        isHintEnabled = false
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
    val et = TextInputEditText(tl.context).apply {
        setSingleLine(true)
        setText(initial)
        setSelection(initial.length)
    }
    tl.addView(et)
    return tl to et
}

fun focusAndShowKeyboard(editText: TextInputEditText, dialog: AlertDialog) {
    editText.requestFocus()
    dialog.window?.apply {
        clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }
    editText.post {
        val imm = editText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }
}

fun isValid(name: String) = name.isNotBlank() && name.matches(Regex("^[^\\\\/:*?\"<>|]+\$"))

fun getDirStats(dir: File): Pair<Int, Int> {
    val files = runCatching { dir.listFiles() }.getOrDefault(emptyArray()) ?: emptyArray()
    val dirs = files.count { it.isDirectory }
    return dirs to (files.size - dirs)
}

fun getRecentDirs(prefs: SharedPreferences): List<String> {
    val str = prefs.getString("recent_dirs", "") ?: ""
    return str.split("|").filter { it.isNotEmpty() }.take(10)
}

fun updateRecentDirs(prefs: SharedPreferences, path: String) {
    val current = getRecentDirs(prefs).toMutableList()
    current.remove(path)
    current.add(0, path)
    while (current.size > 10) current.removeAt(current.size - 1)
    prefs.edit().putString("recent_dirs", current.joinToString("|")).apply()
}

fun shareFile(context: Context, packageName: String, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "$packageName.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享 ${file.name}"))
    } catch (e: Exception) {
        toast(context, "分享失败: ${e.message}")
    }
}

fun openFileWithSystem(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val mimeType = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(file.extension.lowercase(Locale.getDefault()))
            ?: "application/octet-stream"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "打开方式"))
    } catch (e: Exception) {
        toast(context, "无法打开: ${e.message}")
    }
}

fun previewFile(activity: AppCompatActivity, file: File, forceChoose: Boolean = false) {
    if (!file.canRead()) { toast(activity, "无法读取"); return }
    if (file.length() > 5 * 1024 * 1024) { toast(activity, "文件>5MB", Toast.LENGTH_LONG); return }
    val ext = file.extension.lowercase(Locale.ROOT)
    if (forceChoose || (ext !in textExts && ext !in imageExts)) {
        val options = listOf("系统", "文本", "图片")
        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle("打开方式")
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> openFileWithSystem(activity, file)
                    1 -> showTextPreview(activity, file)
                    2 -> showImagePreview(activity, file)
                }
            }
            .setNegativeButton("取消", null)
            .create()
        dialog.show()
        dialog.listView?.let { applySelectableEffectToListView(it) }
        return
    }
    when {
        ext in imageExts -> showImagePreview(activity, file)
        ext in textExts -> showTextPreview(activity, file)
    }
}

fun showTextPreview(activity: AppCompatActivity, file: File) {
    activity.lifecycleScope.launch {
        val content = withContext(Dispatchers.IO) {
            runCatching {
                file.bufferedReader().use { reader ->
                    val sb = StringBuilder()
                    var line: String?
                    var size = 0
                    while (reader.readLine().also { line = it } != null) {
                        sb.append(line).append('\n')
                        size += (line?.length ?: 0) + 1
                        if (size > TEXT_PREVIEW_MAX_BYTES) {
                            sb.append("\n... (文件太大，仅显示前 1MB)")
                            break
                        }
                    }
                    sb.toString()
                }
            }.getOrDefault("读取失败")
        }
        val scrollView = ScrollView(activity).apply {
            isFillViewport = true
            if (content.isEmpty()) {
                val tv = TextView(context).apply {
                    text = "文件为空"
                    textSize = 20f
                    setTextIsSelectable(false)
                    gravity = Gravity.CENTER
                    setPadding(dpToPx(context, 20), dpToPx(context, 20), dpToPx(context, 20), 0)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                addView(tv)
            } else {
                val tv = TextView(context).apply {
                    text = content
                    setTextIsSelectable(true)
                    setPadding(dpToPx(context, 16), dpToPx(context, 16), dpToPx(context, 16), dpToPx(context, 16))
                    textSize = 14f
                }
                addView(tv)
            }
        }
        MaterialAlertDialogBuilder(activity)
            .setTitle(file.name)
            .setView(scrollView)
            .setPositiveButton("关闭", null).show()
    }
}

fun showImagePreview(activity: AppCompatActivity, file: File) {
    activity.lifecycleScope.launch {
        val bmp = withContext(Dispatchers.IO) { runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull() }
        if (bmp == null) { toast(activity, "图片加载失败"); return@launch }
        val iv = ImageView(activity).apply {
            setImageBitmap(bmp)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(dpToPx(activity, 16), dpToPx(activity, 16), dpToPx(activity, 16), dpToPx(activity, 16))
        }
        MaterialAlertDialogBuilder(activity)
            .setTitle(file.name)
            .setView(iv)
            .setPositiveButton("关闭", null).show()
    }
}

fun showDetails(activity: AppCompatActivity, file: File) {
    val info = buildString {
        append("名称: ${file.name}\n")
        append("类型: ${if (file.isDirectory) "目录" else "文件"}\n")
        append("大小: ${Formatter.formatFileSize(activity, file.length())}\n")
        append("修改: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(file.lastModified()))}\n")
        if (file.isDirectory) {
            val (dirs, files) = getDirStats(file)
            append("目录: $dirs\n")
            append("文件: $files\n")
        }
    }
    val tv = TextView(activity).apply {
        text = info
        setTextIsSelectable(true)
        setPadding(dpToPx(activity, 16), dpToPx(activity, 16), dpToPx(activity, 16), dpToPx(activity, 16))
        textSize = 14f
    }
    MaterialAlertDialogBuilder(activity)
        .setTitle("属性")
        .setView(tv)
        .setPositiveButton("确定", null).show()
}

fun File.getDisplayPath(): String {
    val path = absolutePath
    return if (path.length <= 20) {
        path
    } else {
        "…" + path.takeLast(20)
    }
}
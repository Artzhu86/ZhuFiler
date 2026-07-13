package zhu.filer

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.text.format.Formatter
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.MimeTypeMap
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// 排序模式枚举
enum class SortMode(val labelRes: Int) {
    NAME(R.string.sort_by_name),
    SIZE(R.string.sort_by_size),
    DATE(R.string.sort_by_date)
}

// 获取排序比较器
fun getSortComparator(mode: SortMode): Comparator<File> {
    val byDir = compareByDescending<File> { it.isDirectory }
    return when (mode) {
        SortMode.NAME -> byDir.thenBy { it.name.lowercase(Locale.ROOT) }
        SortMode.SIZE -> byDir.thenByDescending { it.length() }
        SortMode.DATE -> byDir.thenByDescending { it.lastModified() }
    }
}

private const val RECENT_SEPARATOR = "|"
private const val RECENT_MAX_COUNT = 10

private const val ELLIPSIZE_START = 0
private const val ELLIPSIZE_MIDDLE = 1

// 应用工具栏标题
fun applyToolbarTitle(toolbar: Toolbar, text: String, mode: Int = ELLIPSIZE_MIDDLE) {
    toolbar.title = text
    toolbar.setTag(R.id.tag_toolbar_title, text to mode)
    refreshToolbarTitle(toolbar)
}

// 刷新工具栏标题显示
fun refreshToolbarTitle(toolbar: Toolbar) {
    toolbar.post {
        @Suppress("UNCHECKED_CAST")
        val tag = toolbar.getTag(R.id.tag_toolbar_title) as? Pair<String, Int> ?: return@post
        val mode = tag.second
        val titleView = getToolbarTitleTextView(toolbar)
        if (titleView == null) {
            toolbar.postDelayed({ refreshToolbarTitle(toolbar) }, 100)
            return@post
        }
        titleView.setSingleLine(true)
        titleView.ellipsize = when (mode) {
            ELLIPSIZE_START -> android.text.TextUtils.TruncateAt.START
            else -> android.text.TextUtils.TruncateAt.MIDDLE
        }
    }
}

// 按路径设置工具栏标题
fun applyToolbarTitlePath(toolbar: Toolbar, path: String) =
    applyToolbarTitle(toolbar, path, ELLIPSIZE_START)

// 按名称设置工具栏标题
fun applyToolbarTitleName(toolbar: Toolbar, name: String) =
    applyToolbarTitle(toolbar, name, ELLIPSIZE_MIDDLE)

// 获取工具栏标题文本视图
private fun getToolbarTitleTextView(toolbar: Toolbar): TextView? {
    return try {
        val field = Toolbar::class.java.getDeclaredField("mTitleTextView")
        field.isAccessible = true
        field.get(toolbar) as? TextView
    } catch (e: Exception) {
        null
    }
}

// 创建文件列表项
fun createFileItem(context: Context, file: File): FileItem {
    val timeStr = SimpleDateFormat(context.getString(R.string.date_format), Locale.getDefault()).format(Date(file.lastModified()))
    val sizeStr = Formatter.formatFileSize(context, file.length())
    return FileItem(file, file.name, FileType.getIconRes(file), "$timeStr  $sizeStr")
}

// 显示提示消息
fun toast(context: Context, msg: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(context, msg, duration).show()
}

// dp转px
fun dpToPx(context: Context, dp: Int): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), context.resources.displayMetrics).toInt()

// 获取主题颜色
fun getThemeColor(context: Context, attr: Int, fallback: Int = android.graphics.Color.TRANSPARENT): Int {
    val tv = TypedValue()
    return if (context.theme.resolveAttribute(attr, tv, true)) tv.data else fallback
}

// 获取状态栏高度
fun getStatusBarHeight(context: Context): Int {
    val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
    return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
}

// 创建对话框容器
fun createDialogContainer(context: Context): LinearLayout {
    return LayoutInflater.from(context)
        .inflate(R.layout.dialog_container, null) as LinearLayout
}

// 创建文本输入框
fun createInput(context: Context, initial: String = ""): Pair<TextInputLayout, TextInputEditText> {
    val tl = LayoutInflater.from(context)
        .inflate(R.layout.dialog_text_input, null) as TextInputLayout
    val et = tl.findViewById<TextInputEditText>(R.id.dialog_input_edit)
    et.setText(initial)
    et.setSelection(initial.length)
    return tl to et
}

// 创建带主题色的单选列表适配器
fun createSingleChoiceAdapter(context: Context, items: Array<String>): ArrayAdapter<String> {
    val primaryColor = getThemeColor(context, android.R.attr.colorPrimary)
    return object : ArrayAdapter<String>(context, android.R.layout.simple_list_item_single_choice, items) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent)
            val ctv = view.findViewById<android.widget.CheckedTextView>(android.R.id.text1)
            ctv?.checkMarkTintList = android.content.res.ColorStateList.valueOf(primaryColor)
            return view
        }
    }
}

// 创建密码输入框
fun createPasswordInput(context: Context, hint: String): Pair<TextInputLayout, TextInputEditText> {
    val tl = LayoutInflater.from(context)
        .inflate(R.layout.dialog_password_input, null) as TextInputLayout
    tl.hint = hint
    val et = tl.findViewById<TextInputEditText>(R.id.dialog_password_edit)
    return tl to et
}

// 聚焦并显示键盘
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

// 判断文件名是否合法
fun isValid(name: String) = name.isNotBlank() && name.matches(Regex("^[^\\\\/:*?\"<>|]+\$"))

// 获取目录统计信息
fun getDirStats(dir: File): Pair<Int, Int> {
    val files = runCatching { dir.listFiles() }.getOrDefault(emptyArray()) ?: emptyArray()
    val dirs = files.count { it.isDirectory }
    return dirs to (files.size - dirs)
}

// 获取最近访问目录
fun getRecentDirs(prefs: SharedPreferences): List<String> {
    val str = prefs.getString("recent_dirs", "") ?: ""
    return str.split(RECENT_SEPARATOR).filter { it.isNotEmpty() }.take(RECENT_MAX_COUNT)
}

// 更新最近访问目录
fun updateRecentDirs(prefs: SharedPreferences, path: String) {
    val current = getRecentDirs(prefs).toMutableList()
    current.remove(path)
    current.add(0, path)
    while (current.size > RECENT_MAX_COUNT) current.removeAt(current.size - 1)
    prefs.edit().putString("recent_dirs", current.joinToString(RECENT_SEPARATOR)).apply()
}

// 分享文件
fun shareFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share) + " " + file.name))
    } catch (e: Exception) {
        toast(context, context.getString(R.string.share_failed, e.message))
    }
}

// 用系统应用打开文件
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
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.open_with)))
    } catch (e: Exception) {
        toast(context, context.getString(R.string.open_failed, e.message))
    }
}

// 显示文件详情对话框
fun showDetailsDialog(activity: AppCompatActivity, file: File) {
    val rows = mutableListOf<Pair<String, String>>()
    rows.add(activity.getString(R.string.name_label) to file.name)
    rows.add(activity.getString(R.string.path_label) to (file.parentFile?.absolutePath ?: file.absolutePath))
    rows.add(activity.getString(R.string.type_label) to if (file.isDirectory) activity.getString(R.string.directory) else activity.getString(R.string.file))
    rows.add(activity.getString(R.string.size_label) to Formatter.formatFileSize(activity, file.length()))
    rows.add(activity.getString(R.string.modified_label) to SimpleDateFormat(activity.getString(R.string.date_format_details), Locale.getDefault()).format(Date(file.lastModified())))
    if (file.isDirectory) {
        val (dirs, files) = getDirStats(file)
        rows.add(activity.getString(R.string.dir_count_label) to dirs.toString())
        rows.add(activity.getString(R.string.file_count_label) to files.toString())
    }

    val table = LayoutInflater.from(activity)
        .inflate(R.layout.dialog_properties, null) as TableLayout
    for ((label, value) in rows) {
        val row = LayoutInflater.from(activity)
            .inflate(R.layout.item_property_row, table, false) as TableRow
        val labelView = row.findViewById<TextView>(R.id.property_label)
        labelView.text = label
        val rippleBg = android.util.TypedValue().let { tv ->
            if (activity.theme.resolveAttribute(android.R.attr.selectableItemBackground, tv, true))
                androidx.core.content.ContextCompat.getDrawable(activity, tv.resourceId)
            else null
        }
        val valueView = row.findViewById<TextView>(R.id.property_value)
        valueView.text = value
        valueView.background = rippleBg
        valueView.setOnLongClickListener {
            val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("value", value))
            toast(activity, activity.getString(R.string.copied_to_clipboard))
            true
        }
        table.addView(row)
    }

    MaterialAlertDialogBuilder(activity)
        .setTitle(R.string.properties)
        .setView(table)
        .setPositiveButton(R.string.ok, null).show()
}

package zhu.filer

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.text.format.Formatter
import android.util.TypedValue
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.MimeTypeMap
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

enum class SortMode(val labelRes: Int) {
    NAME(R.string.sort_by_name),
    SIZE(R.string.sort_by_size),
    DATE(R.string.sort_by_date)
}

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

fun applyToolbarTitle(toolbar: Toolbar, text: String, mode: Int = ELLIPSIZE_MIDDLE) {
    toolbar.title = text
    toolbar.setTag(R.id.tag_toolbar_title, text to mode)
    refreshToolbarTitle(toolbar)
}

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

fun applyToolbarTitlePath(toolbar: Toolbar, path: String) =
    applyToolbarTitle(toolbar, path, ELLIPSIZE_START)

fun applyToolbarTitleName(toolbar: Toolbar, name: String) =
    applyToolbarTitle(toolbar, name, ELLIPSIZE_MIDDLE)

private fun getToolbarTitleTextView(toolbar: Toolbar): TextView? {
    return try {
        val field = Toolbar::class.java.getDeclaredField("mTitleTextView")
        field.isAccessible = true
        field.get(toolbar) as? TextView
    } catch (e: Exception) {
        null
    }
}

fun createFileItem(context: Context, file: File): FileItem {
    val timeStr = SimpleDateFormat(context.getString(R.string.date_format), Locale.getDefault()).format(Date(file.lastModified()))
    val sizeStr = Formatter.formatFileSize(context, file.length())
    val subtitle = if (FileType.isApk(file)) {
        val appName = getApkAppName(context, file)
        if (appName != null) "$appName  $timeStr  $sizeStr" else "$timeStr  $sizeStr"
    } else {
        "$timeStr  $sizeStr"
    }
    return FileItem(file, file.name, FileType.getIconRes(file), subtitle)
}

fun getApkAppName(context: Context, file: File): String? {
    return try {
        val packageInfo = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
        packageInfo?.applicationInfo?.let { appInfo ->
            appInfo.nonLocalizedLabel?.toString() ?: packageInfo.packageName
        }
    } catch (e: Exception) {
        null
    }
}

fun getApkIcon(context: Context, file: File): android.graphics.drawable.Drawable? {
    return try {
        val packageInfo = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
        packageInfo?.applicationInfo?.let { appInfo ->
            appInfo.sourceDir = file.absolutePath
            appInfo.publicSourceDir = file.absolutePath
            context.packageManager.getApplicationIcon(appInfo)
        }
    } catch (e: Exception) {
        null
    }
}

fun toast(context: Context, msg: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(context, msg, duration).show()
}

fun dpToPx(context: Context, dp: Int): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), context.resources.displayMetrics).toInt()

fun getThemeColor(context: Context, attr: Int, fallback: Int = android.graphics.Color.TRANSPARENT): Int {
    val tv = TypedValue()
    return if (context.theme.resolveAttribute(attr, tv, true)) tv.data else fallback
}

fun createDialogContainer(context: Context): LinearLayout {
    return LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dpToPx(context, 16), dpToPx(context, 16), dpToPx(context, 16), 0)
    }
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

fun createPasswordInput(context: Context, hint: String): Pair<TextInputLayout, TextInputEditText> {
    val tl = TextInputLayout(context).apply {
        this.hint = hint
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
    val et = TextInputEditText(tl.context).apply {
        setSingleLine(true)
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
    return str.split(RECENT_SEPARATOR).filter { it.isNotEmpty() }.take(RECENT_MAX_COUNT)
}

fun updateRecentDirs(prefs: SharedPreferences, path: String) {
    val current = getRecentDirs(prefs).toMutableList()
    current.remove(path)
    current.add(0, path)
    while (current.size > RECENT_MAX_COUNT) current.removeAt(current.size - 1)
    prefs.edit().putString("recent_dirs", current.joinToString(RECENT_SEPARATOR)).apply()
}

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

fun previewFile(
    activity: AppCompatActivity,
    file: File,
    forceChoose: Boolean = false,
    onOpenArchive: ((File) -> Unit)? = null
) {
    if (!file.canRead()) { toast(activity, activity.getString(R.string.cannot_read)); return }
    val isArchive = FileType.isArchive(file)
    if (forceChoose) {
        val options = mutableListOf(
            activity.getString(R.string.open),
            activity.getString(R.string.text),
            activity.getString(R.string.image)
        )
        if (isArchive) options.add(activity.getString(R.string.archive))
        if (FileType.isApk(file)) options.add(activity.getString(R.string.install))
        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.open_with)
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> openFileWithSystem(activity, file)
                    1 -> launchTextEditor(activity, file)
                    2 -> launchImagePreview(activity, file)
                    3 -> {
                        if (isArchive) onOpenArchive?.invoke(file)
                        else if (FileType.isApk(file)) installApk(activity, file)
                    }
                    4 -> {
                        if (FileType.isApk(file)) installApk(activity, file)
                        else onOpenArchive?.invoke(file)
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
        dialog.show()
        dialog.listView?.let { applySelectableEffectToListView(it) }
        return
    }
    when {
        isArchive && onOpenArchive != null -> onOpenArchive.invoke(file)
        FileType.isApk(file) -> installApk(activity, file)
        FileType.isImage(file) -> launchImagePreview(activity, file)
        FileType.isText(file) -> launchTextEditor(activity, file)
        else -> openFileWithSystem(activity, file)
    }
}

private fun launchTextEditor(activity: AppCompatActivity, file: File) {
    val intent = android.content.Intent(activity, TextEditorActivity::class.java).apply {
        putExtra(TextEditorActivity.EXTRA_FILE_PATH, file.absolutePath)
    }
    activity.startActivity(intent)
}

private fun launchImagePreview(activity: AppCompatActivity, file: File) {
    val intent = android.content.Intent(activity, ImagePreviewActivity::class.java).apply {
        putExtra(ImagePreviewActivity.EXTRA_FILE_PATH, file.absolutePath)
    }
    activity.startActivity(intent)
}

private fun installApk(activity: AppCompatActivity, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
    } catch (e: Exception) {
        toast(activity, activity.getString(R.string.open_failed, e.message))
    }
}

fun showDetails(activity: AppCompatActivity, file: File) {
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

    val padding = dpToPx(activity, 16)
    val table = TableLayout(activity).apply {
        setPadding(padding, padding, padding, padding)
        setColumnStretchable(1, true)
    }
    for ((label, value) in rows) {
        val labelView = TextView(activity).apply {
            text = label
            setPadding(0, dpToPx(activity, 4), dpToPx(activity, 16), dpToPx(activity, 4))
            textSize = 14f
            layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT)
        }
        val rippleBg = android.util.TypedValue().let { tv ->
            if (activity.theme.resolveAttribute(android.R.attr.selectableItemBackground, tv, true))
                androidx.core.content.ContextCompat.getDrawable(activity, tv.resourceId)
            else null
        }
        val valueView = TextView(activity).apply {
            text = value
            setPadding(0, dpToPx(activity, 4), 0, dpToPx(activity, 4))
            textSize = 14f
            background = rippleBg
            setSingleLine(false)
            setOnLongClickListener {
                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("value", value))
                toast(activity, activity.getString(R.string.copied_to_clipboard))
                true
            }
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
        }
        val row = TableRow(activity).apply {
            addView(labelView)
            addView(valueView)
        }
        table.addView(row)
    }

    MaterialAlertDialogBuilder(activity)
        .setTitle(R.string.properties)
        .setView(table)
        .setPositiveButton(R.string.ok, null).show()
}

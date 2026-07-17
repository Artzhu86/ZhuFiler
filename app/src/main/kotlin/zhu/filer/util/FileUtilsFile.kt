package zhu.filer.util

import android.content.Context
import android.content.Intent
import android.text.format.Formatter
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import zhu.filer.FileItem
import zhu.filer.FileType
import zhu.filer.R

// 创建文件列表项
fun createFileItem(context: Context, file: File): FileItem {
    val timeStr = SimpleDateFormat(context.getString(R.string.date_format), Locale.getDefault()).format(Date(file.lastModified()))
    val sizeStr = Formatter.formatFileSize(context, file.length())
    return FileItem(file, file.name, FileType.getIconRes(file), "$timeStr  $sizeStr")
}

// 判断文件名是否合法
fun isValid(name: String) = name.isNotBlank() && name.matches(Regex("^[^\\\\/:*?\"<>|]+\$"))

// 获取目录统计信息
fun getDirStats(dir: File): Pair<Int, Int> {
    if (ShizukuManager.hasPermission()) {
        val infos = ShizukuManager.listFilesWithDetails(dir.absolutePath)
        if (infos != null) {
            val dirs = infos.count { it.isDirectory }
            return dirs to (infos.size - dirs)
        }
    }
    val files = runCatching { dir.listFiles() }.getOrDefault(emptyArray()) ?: emptyArray()
    val dirs = files.count { it.isDirectory }
    return dirs to (files.size - dirs)
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

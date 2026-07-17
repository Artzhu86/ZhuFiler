package zhu.filer

import java.io.File

// 文件项数据类
data class FileItem(
    val file: File,
    val displayName: String,
    val iconRes: Int,
    val subtitle: String,
    val isDirectory: Boolean = file.isDirectory,
    val encrypted: Boolean = false,
    val entryPath: String? = null,
    val size: Long = file.length()
)

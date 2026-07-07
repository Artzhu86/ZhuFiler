package zhu.filer

import java.io.File
import java.util.Locale

object FileType {

    val textExts = setOf("txt", "log", "md", "json", "xml", "kt", "java", "c", "cpp", "py", "html", "css", "js")
    val imageExts = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
    val archiveExts = setOf(
        "zip", "7z", "rar", "tar", "gz", "tgz", "bz2", "tbz2", "xz", "txz",
        "lzma", "cab", "iso", "arj", "lzh", "cpio", "z", "wim", "xar", "deb", "rpm"
    )

    private fun extOf(name: String): String =
        name.substringAfterLast('.', "").lowercase(Locale.ROOT)

    fun isText(file: File): Boolean = extOf(file.name) in textExts
    fun isText(name: String): Boolean = extOf(name) in textExts

    fun isImage(file: File): Boolean = extOf(file.name) in imageExts
    fun isImage(name: String): Boolean = extOf(name) in imageExts

    fun isArchive(file: File): Boolean = extOf(file.name) in archiveExts
    fun isArchive(name: String): Boolean = extOf(name) in archiveExts

    fun isApk(file: File): Boolean = extOf(file.name) == "apk"
    fun isApk(name: String): Boolean = extOf(name) == "apk"

    fun getIconRes(file: File): Int = getIconRes(file.name, file.isDirectory)

    fun getIconRes(name: String, isDirectory: Boolean): Int {
        if (isDirectory) return R.drawable.outline_folder_24
        val ext = extOf(name)
        return when {
            ext in textExts -> R.drawable.outline_description_24
            ext in imageExts -> R.drawable.outline_image_24
            ext in archiveExts -> R.drawable.outline_folder_zip_24
            else -> R.drawable.outline_insert_drive_file_24
        }
    }
}

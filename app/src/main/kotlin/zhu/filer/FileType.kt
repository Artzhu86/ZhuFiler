package zhu.filer

import java.io.File
import java.util.Locale

// 文件类型判断
object FileType {

    val textExts = setOf("txt", "log", "md", "json", "xml", "kt", "java", "c", "cpp", "py", "html", "css", "js")
    val imageExts = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
    val audioExts = setOf("mp3", "flac", "aac", "ogg", "wav", "m4a", "wma", "opus", "amr")
    val videoExts = setOf("mp4", "mkv", "avi", "mov", "flv", "webm", "3gp", "ts", "m4v", "wmv")
    val archiveExts = setOf("zip", "7z", "rar", "tar", "gz", "tgz", "bz2", "xz")
    val apkExts = setOf("apk")

    // 获取文件扩展名
    private fun extOf(name: String): String =
        name.substringAfterLast('.', "").lowercase(Locale.ROOT)

    // 判断是否文本文件
    fun isText(file: File): Boolean = extOf(file.name) in textExts

    // 判断是否图像文件
    fun isImage(file: File): Boolean = extOf(file.name) in imageExts

    // 判断是否音频文件
    fun isAudio(file: File): Boolean = extOf(file.name) in audioExts

    // 判断是否视频文件
    fun isVideo(file: File): Boolean = extOf(file.name) in videoExts

    // 判断是否归档文件
    fun isArchive(file: File): Boolean = extOf(file.name) in archiveExts

    // 判断是否APK文件
    fun isApk(file: File): Boolean = extOf(file.name) in apkExts

    // 获取文件图标
    fun getIconRes(file: File): Int = getIconRes(file.name, file.isDirectory)

    // 获取文件图标
    fun getIconRes(name: String, isDirectory: Boolean): Int {
        if (isDirectory) return R.drawable.outline_folder_24
        val ext = extOf(name)
        return when {
            ext in textExts -> R.drawable.outline_description_24
            ext in imageExts -> R.drawable.outline_image_24
            ext in audioExts -> R.drawable.outline_audio_file_24
            ext in videoExts -> R.drawable.outline_video_file_24
            ext in archiveExts -> R.drawable.outline_archive_24
            ext in apkExts -> R.drawable.outline_android_24
            else -> R.drawable.outline_insert_drive_file_24
        }
    }
}

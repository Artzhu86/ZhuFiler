package zhu.filer.util

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaMetadataRetriever
import android.widget.Toast
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import zhu.filer.R

// 排序模式枚举
enum class SortMode(val labelRes: Int) {
    NAME(R.string.sort_by_name),
    SIZE(R.string.sort_by_size),
    DATE(R.string.sort_by_date)
}

// 文件路径传递键
const val EXTRA_FILE_PATH = "extra_file_path"

// 转场动画参数键
const val TRANSITION_PARAMS_KEY = "TransformationParams"

// 获取排序比较器
fun getSortComparator(mode: SortMode): Comparator<File> {
    val byDir = compareByDescending<File> { it.isDirectory }
    return when (mode) {
        SortMode.NAME -> byDir.thenBy { it.name.lowercase(Locale.ROOT) }
        SortMode.SIZE -> byDir.thenByDescending { it.length() }
        SortMode.DATE -> byDir.thenByDescending { it.lastModified() }
    }
}

// 最近目录分隔符
private const val RECENT_SEPARATOR = "|"

// 最近目录最大数量
private const val RECENT_MAX_COUNT = 10

// 显示提示消息
fun toast(context: Context, msg: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(context, msg, duration).show()
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

// 获取音频嵌入封面
fun getAudioArtwork(file: File): ByteArray? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(file.absolutePath)
        retriever.embeddedPicture
    } catch (e: Exception) {
        null
    } finally {
        try { retriever.release() } catch (_: Exception) {}
    }
}

// 格式化日期
fun formatDate(context: Context, timestamp: Long): String =
    SimpleDateFormat(context.getString(R.string.date_format), Locale.getDefault()).format(Date(timestamp))

// 获取音频标题和艺术家
fun getAudioMetadata(file: File): Pair<String?, String?> {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(file.absolutePath)
        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        Pair(title, artist)
    } catch (e: Exception) {
        Pair(null, null)
    } finally {
        try { retriever.release() } catch (_: Exception) {}
    }
}

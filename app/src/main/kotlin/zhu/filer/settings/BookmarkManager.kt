package zhu.filer.settings

import android.content.SharedPreferences
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import java.io.File
import zhu.filer.R
import zhu.filer.ui.buildDialogTitle
import zhu.filer.util.toast

// 书签管理器
class BookmarkManager(
    internal val activity: AppCompatActivity,
    internal val drawerLayout: DrawerLayout,
    internal val navigationView: NavigationView,
    internal val prefs: SharedPreferences,
    internal val loadDir: suspend (File) -> Unit
) {

    // 伴生对象存储常量
    companion object {
        internal const val BOOKMARKS_KEY = "bookmarks"
        internal const val SEPARATOR = "|"
        internal const val GROUP_FIXED = 0x01
        internal const val GROUP_BOOKMARK = 0x02
        internal const val GROUP_SETTINGS = 0x03
        internal const val FIXED_ITEM_ROOT = 1
        internal const val FIXED_ITEM_STORAGE = 2
        internal const val FIXED_ITEM_PREFERENCES = 3
        internal const val BOOKMARK_START_ID = 100
        internal const val ORDER_ROOT = 0
        internal const val ORDER_STORAGE = 1
        internal const val ORDER_BOOKMARK_BASE = 2
        internal const val ORDER_PREFERENCES = 100
    }

    // 获取书签列表
    fun getBookmarks(): List<String> {
        return prefs.getString(BOOKMARKS_KEY, "")?.split(SEPARATOR)?.filter { it.isNotEmpty() } ?: emptyList()
    }

    // 保存书签列表
    private fun setBookmarks(bookmarks: List<String>) {
        prefs.edit().putString(BOOKMARKS_KEY, bookmarks.joinToString(SEPARATOR)).apply()
    }

    // 添加书签
    fun addBookmark(path: String) = modifyBookmarks { it.apply { if (!contains(path)) add(path) } }

    // 移除书签
    fun removeBookmark(path: String) = modifyBookmarks { it.remove(path) }

    // 切换书签状态并确认
    fun toggleBookmarkWithConfirm(path: String, onConfirmed: () -> Unit = {}) {
        if (!isBookmarked(path)) {
            addBookmark(path)
            toast(activity, activity.getString(R.string.bookmark_added))
            return
        }
        val displayName = File(path).name.ifEmpty { path }
        MaterialAlertDialogBuilder(activity)
            .setCustomTitle(buildDialogTitle(activity, R.string.remove_bookmark))
            .setMessage(activity.getString(R.string.confirm_remove_bookmark_msg, displayName))
            .setPositiveButton(R.string.remove) { _, _ ->
                removeBookmark(path)
                toast(activity, activity.getString(R.string.bookmark_removed))
                onConfirmed()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // 修改书签列表
    private inline fun modifyBookmarks(action: (MutableList<String>) -> Unit) {
        val list = getBookmarks().toMutableList()
        val sizeBefore = list.size
        action(list)
        if (list.size != sizeBefore) {
            setBookmarks(list)
            updateMenu()
        }
    }

    // 判断是否已收藏
    fun isBookmarked(path: String): Boolean = getBookmarks().contains(path)

    // 初始化默认书签
    fun initDefaultBookmarks() {
        if (getBookmarks().isNotEmpty()) return
        val defaults = listOf(
            Environment.DIRECTORY_DCIM,
            Environment.DIRECTORY_DOWNLOADS,
            Environment.DIRECTORY_PICTURES
        ).mapNotNull { dir ->
            Environment.getExternalStoragePublicDirectory(dir).absolutePath
                .takeIf { File(it).exists() }
        }
        if (defaults.isNotEmpty()) setBookmarks(defaults)
    }

    // 更新导航菜单
    fun updateMenu(currentDir: File? = null) {
        buildMenu(currentDir)
    }

    // 初始化书签功能
    fun setup() {
        setupNavigation()
    }
}

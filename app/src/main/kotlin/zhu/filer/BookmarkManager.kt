package zhu.filer

import android.content.SharedPreferences
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch
import java.io.File

// 书签管理器
class BookmarkManager(
    private val activity: AppCompatActivity,
    private val drawerLayout: DrawerLayout,
    private val navigationView: NavigationView,
    private val prefs: SharedPreferences,
    private val loadDir: suspend (File) -> Unit
) {

    // 伴生对象存储常量
    companion object {
        private const val BOOKMARKS_KEY = "bookmarks"
        private const val SEPARATOR = "|"
        private const val GROUP_FIXED = 0x01
        private const val GROUP_BOOKMARK = 0x02
        private const val GROUP_SETTINGS = 0x03
        private const val FIXED_ITEM_ROOT = 1
        private const val FIXED_ITEM_STORAGE = 2
        private const val FIXED_ITEM_PREFERENCES = 3
        private const val BOOKMARK_START_ID = 100
        private const val ORDER_ROOT = 0
        private const val ORDER_STORAGE = 1
        private const val ORDER_BOOKMARK_BASE = 2
        private const val ORDER_PREFERENCES = 100
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
            .setTitle(R.string.confirm_remove_bookmark)
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
        val menu = navigationView.menu
        menu.clear()

        val currentPath = currentDir?.absolutePath

        menu.add(GROUP_FIXED, FIXED_ITEM_ROOT, ORDER_ROOT, activity.getString(R.string.root_directory))
            .setIcon(R.drawable.outline_phone_android_24)
            .isChecked = currentPath == "/"

        menu.add(GROUP_FIXED, FIXED_ITEM_STORAGE, ORDER_STORAGE, activity.getString(R.string.internal_storage))
            .setIcon(R.drawable.outline_sd_card_24)
            .isChecked = currentPath == Environment.getExternalStorageDirectory().absolutePath

        menu.setGroupCheckable(GROUP_FIXED, true, true)

        val bookmarks = getBookmarks()
        bookmarks.forEachIndexed { index, path ->
            val file = File(path)
            val displayName = file.name.ifEmpty { path }
            menu.add(GROUP_BOOKMARK, BOOKMARK_START_ID + index, ORDER_BOOKMARK_BASE + index, displayName)
                .setIcon(R.drawable.outline_folder_24)
                .isChecked = currentPath == path
        }
        if (bookmarks.isNotEmpty()) menu.setGroupCheckable(GROUP_BOOKMARK, true, true)

        menu.add(GROUP_SETTINGS, FIXED_ITEM_PREFERENCES, ORDER_PREFERENCES, activity.getString(R.string.preferences))
            .setIcon(R.drawable.outline_settings_24)

        navigationView.post { attachBookmarkLongPress(bookmarks) }
    }

    // 绑定书签长按事件
    private fun attachBookmarkLongPress(bookmarks: List<String>) {
        val recyclerView = navigationView.getChildAt(0) as? android.view.ViewGroup ?: return
        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i)
            val itemId = child.id
            if (itemId in BOOKMARK_START_ID..Int.MAX_VALUE) {
                val index = itemId - BOOKMARK_START_ID
                if (index in bookmarks.indices) {
                    val path = bookmarks[index]
                    child.setOnLongClickListener {
                        toggleBookmarkWithConfirm(path)
                        true
                    }
                }
            }
        }
    }

    // 初始化书签功能
    fun setup() {
        initDefaultBookmarks()
        updateMenu()

        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                FIXED_ITEM_ROOT -> activity.lifecycleScope.launch { loadDir(File("/")) }
                FIXED_ITEM_STORAGE -> activity.lifecycleScope.launch { loadDir(Environment.getExternalStorageDirectory()) }
                FIXED_ITEM_PREFERENCES -> {
                    val intent = android.content.Intent(activity, PreferencesActivity::class.java)
                    activity.startActivity(intent)
                }

                in BOOKMARK_START_ID..Int.MAX_VALUE -> {
                    val bookmarks = getBookmarks()
                    val index = item.itemId - BOOKMARK_START_ID
                    if (index in bookmarks.indices) {
                        val path = bookmarks[index]
                        val dir = File(path)
                        if (dir.exists() && dir.isDirectory) {
                            activity.lifecycleScope.launch { loadDir(dir) }
                        } else {
                            removeBookmark(path)
                            toast(activity, activity.getString(R.string.directory_invalid))
                        }
                    }
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }
}

package zhu.filer.settings

import android.os.Environment
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File
import zhu.filer.R
import zhu.filer.util.toast

// 构建导航菜单
internal fun BookmarkManager.buildMenu(currentDir: File? = null) {
    val menu = navigationView.menu
    menu.clear()

    val currentPath = currentDir?.absolutePath

    menu.add(BookmarkManager.GROUP_FIXED, BookmarkManager.FIXED_ITEM_ROOT, BookmarkManager.ORDER_ROOT, activity.getString(R.string.root_directory))
        .setIcon(R.drawable.outline_phone_android_24)
        .isChecked = currentPath == "/"

    menu.add(BookmarkManager.GROUP_FIXED, BookmarkManager.FIXED_ITEM_STORAGE, BookmarkManager.ORDER_STORAGE, activity.getString(R.string.internal_storage))
        .setIcon(R.drawable.outline_sd_card_24)
        .isChecked = currentPath == Environment.getExternalStorageDirectory().absolutePath

    menu.setGroupCheckable(BookmarkManager.GROUP_FIXED, true, true)

    val bookmarks = getBookmarks()
    bookmarks.forEachIndexed { index, path ->
        val file = File(path)
        val displayName = file.name.ifEmpty { path }
        menu.add(BookmarkManager.GROUP_BOOKMARK, BookmarkManager.BOOKMARK_START_ID + index, BookmarkManager.ORDER_BOOKMARK_BASE + index, displayName)
            .setIcon(R.drawable.outline_folder_24)
            .isChecked = currentPath == path
    }
    if (bookmarks.isNotEmpty()) menu.setGroupCheckable(BookmarkManager.GROUP_BOOKMARK, true, true)

    menu.add(BookmarkManager.GROUP_SETTINGS, BookmarkManager.FIXED_ITEM_PREFERENCES, BookmarkManager.ORDER_PREFERENCES, activity.getString(R.string.preferences))
        .setIcon(R.drawable.outline_settings_24)

    navigationView.post { attachLongPress(bookmarks) }
}

// 绑定书签长按事件
internal fun BookmarkManager.attachLongPress(bookmarks: List<String>) {
    val recyclerView = navigationView.getChildAt(0) as? android.view.ViewGroup ?: return
    for (i in 0 until recyclerView.childCount) {
        val child = recyclerView.getChildAt(i)
        val itemId = child.id
        if (itemId in BookmarkManager.BOOKMARK_START_ID..Int.MAX_VALUE) {
            val index = itemId - BookmarkManager.BOOKMARK_START_ID
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

// 设置书签导航
internal fun BookmarkManager.setupNavigation() {
    initDefaultBookmarks()
    updateMenu()

    navigationView.setNavigationItemSelectedListener { item ->
        when (item.itemId) {
            BookmarkManager.FIXED_ITEM_ROOT -> activity.lifecycleScope.launch { loadDir(File("/")) }
            BookmarkManager.FIXED_ITEM_STORAGE -> activity.lifecycleScope.launch { loadDir(Environment.getExternalStorageDirectory()) }
            BookmarkManager.FIXED_ITEM_PREFERENCES -> {
                val intent = android.content.Intent(activity, PreferencesActivity::class.java)
                activity.startActivity(intent)
            }

            in BookmarkManager.BOOKMARK_START_ID..Int.MAX_VALUE -> {
                val bookmarks = getBookmarks()
                val index = item.itemId - BookmarkManager.BOOKMARK_START_ID
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

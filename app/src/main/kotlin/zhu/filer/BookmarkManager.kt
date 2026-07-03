package zhu.filer

import android.content.SharedPreferences
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import java.io.File

class BookmarkManager(
    private val activity: AppCompatActivity,
    private val drawerLayout: DrawerLayout,
    private val navigationView: NavigationView,
    private val prefs: SharedPreferences,
    private val loadDir: (File) -> Unit
) {

    companion object {
        private const val BOOKMARKS_KEY = "bookmarks"
        private const val SEPARATOR = "|"
        private const val GROUP_FIXED = 0x01
        private const val GROUP_BOOKMARK = 0x02
        private const val FIXED_ITEM_ROOT = 1
        private const val FIXED_ITEM_STORAGE = 2
        private const val BOOKMARK_START_ID = 100
        private const val ORDER_ROOT = 0
        private const val ORDER_STORAGE = 1
        private const val ORDER_BOOKMARK_BASE = 2
    }

    fun getBookmarks(): List<String> {
        return prefs.getString(BOOKMARKS_KEY, "")?.split(SEPARATOR)?.filter { it.isNotEmpty() } ?: emptyList()
    }

    private fun setBookmarks(bookmarks: List<String>) {
        prefs.edit().putString(BOOKMARKS_KEY, bookmarks.joinToString(SEPARATOR)).apply()
    }

    fun addBookmark(path: String) = modifyBookmarks { it.apply { if (!contains(path)) add(path) } }

    fun removeBookmark(path: String) = modifyBookmarks { it.remove(path) }

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

    private inline fun modifyBookmarks(action: (MutableList<String>) -> Unit) {
        val list = getBookmarks().toMutableList()
        val sizeBefore = list.size
        action(list)
        if (list.size != sizeBefore) {
            setBookmarks(list)
            updateMenu()
        }
    }

    fun isBookmarked(path: String): Boolean = getBookmarks().contains(path)

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

        navigationView.post { attachBookmarkLongPress(bookmarks) }
    }

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

    fun setup() {
        initDefaultBookmarks()
        updateMenu()

        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                FIXED_ITEM_ROOT -> loadDir(File("/"))
                FIXED_ITEM_STORAGE -> loadDir(Environment.getExternalStorageDirectory())

                in BOOKMARK_START_ID..Int.MAX_VALUE -> {
                    val bookmarks = getBookmarks()
                    val index = item.itemId - BOOKMARK_START_ID
                    if (index in bookmarks.indices) {
                        val path = bookmarks[index]
                        val dir = File(path)
                        if (dir.exists() && dir.isDirectory) {
                            loadDir(dir)
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
package zhu.filer

import android.content.SharedPreferences
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

// 菜单控制器
class MenuController(
    private val activity: AppCompatActivity,
    private val prefs: SharedPreferences,
    private val browserController: FileBrowserController,
    private val bookmarkManager: BookmarkManager,
    private val searchHelper: SearchHelper,
    private val onShowHiddenChanged: () -> Unit,
    private val onExitMultiSelect: () -> Unit,
    private val onExit: () -> Unit
) {

    private var showHidden: Boolean = false
    private var sortMode: SortMode = SortMode.NAME

    // 初始化偏好
    fun initPrefs() {
        showHidden = prefs.getBoolean("show_hidden", false)
        sortMode = runCatching {
            SortMode.valueOf(prefs.getString("sort_mode", SortMode.NAME.name) ?: SortMode.NAME.name)
        }.getOrDefault(SortMode.NAME)
    }

    // 获取是否显示隐藏文件
    fun isShowHidden() = showHidden

    // 获取排序模式
    fun getSortMode() = sortMode

    // 创建菜单
    fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, activity.getString(R.string.refresh)).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(Menu.NONE, Menu.FIRST + 1, Menu.NONE, activity.getString(R.string.search)).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(Menu.NONE, Menu.FIRST + 2, Menu.NONE, activity.getString(R.string.search_result)).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        val hiddenItem = menu.add(Menu.NONE, Menu.FIRST + 4, Menu.NONE, activity.getString(R.string.show_hidden))
        hiddenItem.setCheckable(true)
        hiddenItem.isChecked = showHidden
        hiddenItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        val bookmarkItem = menu.add(Menu.NONE, Menu.FIRST + 5, Menu.NONE, getBookmarkMenuTitle())
        bookmarkItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(Menu.NONE, Menu.FIRST + 6, Menu.NONE, activity.getString(R.string.sort_by)).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(Menu.NONE, Menu.FIRST + 3, Menu.NONE, activity.getString(R.string.exit)).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        return true
    }

    // 准备菜单
    fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(Menu.FIRST + 5)?.title = getBookmarkMenuTitle()
        return true
    }

    // 菜单项选择
    fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            Menu.FIRST -> {
                onExitMultiSelect()
                browserController.refresh()
            }
            Menu.FIRST + 1 -> searchHelper.showSearchDialog()
            Menu.FIRST + 2 -> searchHelper.showLastSearchResult()
            Menu.FIRST + 3 -> onExit()
            Menu.FIRST + 4 -> {
                showHidden = !showHidden
                item.isChecked = showHidden
                prefs.edit().putBoolean("show_hidden", showHidden).apply()
                onShowHiddenChanged()
            }
            Menu.FIRST + 5 -> {
                val path = browserController.currentDir.absolutePath
                bookmarkManager.toggleBookmarkWithConfirm(path) {
                    activity.invalidateOptionsMenu()
                }
            }
            Menu.FIRST + 6 -> showSortDialog()
        }
        return true
    }

    // 获取书签菜单标题
    private fun getBookmarkMenuTitle(): String {
        val isBookmarked = bookmarkManager.isBookmarked(browserController.currentDir.absolutePath)
        return activity.getString(if (isBookmarked) R.string.remove_current_bookmark else R.string.add_current_bookmark)
    }

    // 显示排序对话框
    private fun showSortDialog() {
        val modes = SortMode.entries
        val labels = modes.map { activity.getString(it.labelRes) }.toTypedArray()
        val current = modes.indexOf(sortMode)
        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.sort_by)
            .setSingleChoiceItems(createSingleChoiceAdapter(activity, labels), current) { dialog, which ->
                sortMode = modes[which]
                prefs.edit().putString("sort_mode", sortMode.name).apply()
                browserController.refresh()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
        dialog.show()
    }
}

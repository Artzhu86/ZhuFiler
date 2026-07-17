package zhu.filer

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.skydoves.transformationlayout.onTransformationStartContainer
import zhu.filer.browser.FileBrowserController
import zhu.filer.browser.FileClickHandler
import zhu.filer.browser.ToolbarScrollerController
import zhu.filer.browser.smartRefresh
import zhu.filer.databinding.ActivityMainBinding
import zhu.filer.operation.ClipboardManager
import zhu.filer.operation.FileOpener
import zhu.filer.operation.FileOperationsController
import zhu.filer.operation.MultiSelectController
import zhu.filer.settings.BookmarkManager
import zhu.filer.settings.SearchHelper
import zhu.filer.ui.FabManager
import zhu.filer.ui.ThemeHelper
import zhu.filer.ui.refreshToolbarTitle

// 主界面
class MainActivity : AppCompatActivity() {
    internal lateinit var binding: ActivityMainBinding
    internal lateinit var drawerLayout: DrawerLayout
    internal lateinit var navigationView: NavigationView
    internal lateinit var recyclerView: RecyclerView
    internal lateinit var toolbar: MaterialToolbar
    internal lateinit var progressBar: CircularProgressIndicator
    internal lateinit var fabAdd: FloatingActionButton
    internal lateinit var fabAction: FloatingActionButton
    internal lateinit var fabCancel: FloatingActionButton
    internal lateinit var swipeRefreshLayout: SwipeRefreshLayout
    internal lateinit var prefs: android.content.SharedPreferences
    internal lateinit var searchHelper: SearchHelper
    internal lateinit var browserController: FileBrowserController
    internal val clipboard = ClipboardManager()
    internal lateinit var bookmarkManager: BookmarkManager
    internal var statsSubtitle: String? = null
    internal val permissionHelper = PermissionHelper(this)
    internal val backPressHandler = BackPressHandler(this)
    internal val fabManager = FabManager(this)
    internal lateinit var toolbarScrollerController: ToolbarScrollerController
    internal lateinit var fileOpsController: FileOperationsController
    internal lateinit var fileOpener: FileOpener
    internal lateinit var menuController: MenuController
    internal lateinit var fileClickHandler: FileClickHandler
    internal lateinit var multiSelectController: MultiSelectController
    private var isFirstResume = true
    private var lastThemeColor: String = ""

    // 创建活动
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyThemeColor(this)
        onTransformationStartContainer()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        lastThemeColor = ThemeHelper.getColorName(this)
        setupWindow()
        initViews()
        setupSwipeRefresh()
        prefs = getSharedPreferences("filer_prefs", MODE_PRIVATE)
        setupToolbar()
        initControllers()
        initInitialContent(savedInstanceState)
        toolbar.post {
            toolbarScrollerController.onToolbarReady()
        }
    }

    // 保存状态
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        saveScrollState(outState)
    }

    // 准备菜单
    override fun onPrepareOptionsMenu(menu: Menu): Boolean = menuController.onPrepareOptionsMenu(menu)

    // 创建菜单
    override fun onCreateOptionsMenu(menu: Menu): Boolean = menuController.onCreateOptionsMenu(menu)

    // 菜单项选择
    override fun onOptionsItemSelected(item: MenuItem): Boolean = menuController.onOptionsItemSelected(item)

    // 销毁
    override fun onDestroy() {
        super.onDestroy()
        searchHelper.dismiss()
        permissionHelper.onDestroy()
    }

    // 恢复
    override fun onResume() {
        super.onResume()
        if (!isFirstResume && ::browserController.isInitialized) {
            val currentColor = ThemeHelper.getColorName(this)
            if (currentColor != lastThemeColor) {
                recreate()
                return
            }
            exitMultiSelect()
            browserController.smartRefresh()
        }
        isFirstResume = false
        if (::toolbar.isInitialized) {
            refreshToolbarTitle(toolbar)
        }
    }

    // 窗口焦点变化
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && ::toolbar.isInitialized) {
            refreshToolbarTitle(toolbar)
        }
    }
}

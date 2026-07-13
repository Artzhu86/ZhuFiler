package zhu.filer

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.skydoves.transformationlayout.onTransformationStartContainer
import kotlinx.coroutines.launch
import java.io.File
import com.google.android.material.R as materialR
import zhu.filer.databinding.ActivityMainBinding

// 主界面
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var recyclerView: RecyclerView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var progressBar: CircularProgressIndicator
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var fabAction: FloatingActionButton
    private lateinit var fabCancel: FloatingActionButton
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var searchHelper: SearchHelper
    private lateinit var browserController: FileBrowserController
    private val clipboard = ClipboardManager()
    private lateinit var bookmarkManager: BookmarkManager

    private var statsSubtitle: String? = null

    private val permissionHelper = PermissionHelper(this)
    private val backPressHandler = BackPressHandler(this)
    private val fabManager = FabManager(this)

    private lateinit var toolbarScrollerController: ToolbarScrollerController
    private lateinit var fileOpsController: FileOperationsController
    private lateinit var fileOpener: FileOpener
    private lateinit var menuController: MenuController
    private lateinit var fileClickHandler: FileClickHandler
    private lateinit var multiSelectController: MultiSelectController

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

        window.sharedElementsUseOverlay = true

        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }
        val isDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView)?.apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }

        initViews()
        setupSwipeRefresh()
        prefs = getSharedPreferences("filer_prefs", MODE_PRIVATE)

        setupToolbar()

        bookmarkManager = BookmarkManager(this, drawerLayout, navigationView, prefs, ::navigateToDir)
        bookmarkManager.setup()

        browserController = FileBrowserController(
            activity = this,
            toolbar = toolbar,
            recyclerView = recyclerView,
            swipeRefreshLayout = swipeRefreshLayout,
            prefs = prefs,
            showHiddenProvider = { menuController.isShowHidden() },
            sortModeProvider = { menuController.getSortMode() },
            onDirLoaded = {
                if (::bookmarkManager.isInitialized) {
                    bookmarkManager.updateMenu(browserController.currentDir)
                }
                statsSubtitle = supportActionBar?.subtitle?.toString()
                updateToolbarTitle()
                updateMultiSelectFabs()
                fabManager.updatePasteButtons(clipboard)
                toolbarScrollerController.animateToolbarColorOnDirSwitch()
            }
        )

        toolbarScrollerController = ToolbarScrollerController(toolbar, recyclerView, this)
        fileOpsController = FileOperationsController(
            activity = this,
            browserController = browserController,
            lifecycleScope = lifecycleScope,
            progressBar = progressBar,
            fabManager = fabManager,
            clipboard = clipboard,
            loadDir = ::loadDir,
            refreshDir = ::refreshDir
        )

        fileOpener = FileOpener(
            activity = this,
            browserController = browserController,
            lifecycleScope = lifecycleScope,
            progressBar = progressBar
        )

        fileClickHandler = FileClickHandler(
            activity = this,
            recyclerView = recyclerView,
            browserController = browserController,
            fileOpener = fileOpener,
            fileOpsController = fileOpsController,
            bookmarkManager = bookmarkManager,
            multiSelectProvider = { multiSelectController },
            clipboard = clipboard,
            fabManager = fabManager,
            toolbarScrollerController = toolbarScrollerController,
            loadDir = { dir, scrollToTop -> loadDir(dir, scrollToTop = scrollToTop) },
            exitMultiSelect = ::exitMultiSelect,
            updateToolbarTitle = ::updateToolbarTitle,
            updateMultiSelectFabs = ::updateMultiSelectFabs
        )
        fileClickHandler.setup()
        binding.fastScroller.attach(recyclerView)
        setupFabs()

        browserController.init(fileClickHandler.adapter)

        searchHelper = SearchHelper(this, { browserController.currentDir }, ::navigateToDir, ::locateFile)

        menuController = MenuController(
            activity = this,
            prefs = prefs,
            browserController = browserController,
            bookmarkManager = bookmarkManager,
            searchHelper = searchHelper,
            onShowHiddenChanged = { browserController.refresh() },
            onExitMultiSelect = ::exitMultiSelect,
            onExit = { finish() }
        )
        menuController.initPrefs()

        multiSelectController = MultiSelectController(
            activity = this,
            adapter = fileClickHandler.adapter,
            canNavigateUp = { browserController.canNavigateUp() },
            getCurrentDir = { browserController.currentDir },
            loadDir = { loadDir(it) },
            progressBar = progressBar,
            clipboardManager = clipboard,
            onClipboardChanged = { fabManager.updatePasteButtons(clipboard) },
            onExitMultiSelect = { updateMultiSelectFabs() },
            onCompress = { files ->
                showCompressDialog(this, files, browserController.currentDir) { outputFile, format, password ->
                    fileOpsController.performCompress(files, outputFile, format, password)
                }
            },
            onRefresh = { browserController.refresh(animate = false) }
        )

        backPressHandler.setup(
            multiSelectController = multiSelectController,
            drawerLayout = drawerLayout,
            browserController = browserController,
            onExit = { finish() },
            onExitMultiSelect = { exitMultiSelect() }
        )

        savedInstanceState?.let { bundle ->
            val path = bundle.getString("cached_path") ?: return@let
            val scrollPos = bundle.getInt("scroll_pos", -1)
            val scrollOffset = bundle.getInt("scroll_offset", 0)
            if (scrollPos >= 0) {
                browserController.saveScrollState(scrollPos, scrollOffset)
            }
            val dir = File(path)
            if (dir.exists() && dir.isDirectory && dir.canRead()) {
                loadDir(dir, scrollToTop = false)
            } else {
                initLoad()
            }
        } ?: initLoad()

        toolbar.post {
            toolbarScrollerController.onToolbarReady()
        }
    }

    // 初始化视图
    private fun initViews() {
        drawerLayout = binding.drawerLayout
        navigationView = binding.navigationView
        recyclerView = binding.recyclerView
        toolbar = binding.toolbar
        progressBar = binding.progressBar
        fabAdd = binding.fabAdd
        fabAction = binding.fabAction
        fabCancel = binding.fabCancel
    }

    // 设置工具栏
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, 0, 0)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        toolbar.post {
            for (i in 0 until toolbar.childCount) {
                val child = toolbar.getChildAt(i)
                if (child is TextView) {
                    child.setOnClickListener {
                        showNavigateDialog(this@MainActivity, browserController.currentDisplayPath(), ::navigateToDir, prefs)
                    }
                }
            }
        }
        val statusBarHeight = getStatusBarHeight(this)
        val actionBarHeight = toolbar.layoutParams.height
        toolbar.layoutParams.height = actionBarHeight + statusBarHeight
        toolbar.setPadding(0, statusBarHeight, 0, 0)
        toolbar.setBackgroundColor(Color.TRANSPARENT)
        toolbar.setSubtitleTextAppearance(this, R.style.ToolbarSubtitle)
        toolbar.post {
            supportActionBar?.title = Environment.getExternalStorageDirectory().absolutePath + "/"
        }
    }

    // 设置悬浮按钮
    private fun setupFabs() {
        fabAdd.setOnClickListener {
            showCreateDialog(this, browserController.currentDir) { dir, highlightPath ->
                lifecycleScope.launch { refreshDir(dir, highlightPath) }
            }
        }
        fabManager.setup(
            fabAction = fabAction,
            fabCancel = fabCancel,
            clipboard = clipboard,
            targetDirProvider = { browserController.currentDir },
            onPaste = { files, isMove, overwrite ->
                lifecycleScope.launch { fileOpsController.performPaste(files, browserController.currentDir, isMove, overwrite) }
            },
            onCancel = {
                clipboard.clear()
                fabManager.updatePasteButtons(clipboard)
            }
        )
        fabManager.setMultiSelectActions(
            onSelectAll = { fileClickHandler.selectAll() },
            onDeselect = { fileClickHandler.deselectAll() }
        )
    }

    // 更新多选悬浮按钮
    private fun updateMultiSelectFabs() {
        fabManager.updateMultiSelectButtons(multiSelectController.isInMultiSelectMode())
    }

    // 设置下拉刷新
    private fun setupSwipeRefresh() {
        val parent = recyclerView.parent as androidx.constraintlayout.widget.ConstraintLayout
        val params = recyclerView.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        parent.removeView(recyclerView)
        swipeRefreshLayout = SwipeRefreshLayout(this).apply {
            layoutParams = params
            setOnRefreshListener {
                exitMultiSelect()
                browserController.refresh()
            }
            setProgressBackgroundColorSchemeColor(
                getThemeColor(this@MainActivity, com.google.android.material.R.attr.colorSurface)
            )
            setColorSchemeColors(
                getThemeColor(this@MainActivity, android.R.attr.colorPrimary),
                getThemeColor(this@MainActivity, materialR.attr.colorSecondary),
                getThemeColor(this@MainActivity, materialR.attr.colorTertiary)
            )
            setProgressViewEndTarget(true, dpToPx(this@MainActivity, 64))
        }
        swipeRefreshLayout.addView(recyclerView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        parent.addView(swipeRefreshLayout)
    }

    // 退出多选
    private fun exitMultiSelect() {
        fileClickHandler.resetSwipeSelect()
        multiSelectController.exitMultiSelect()
        updateToolbarTitle()
        updateMultiSelectFabs()
    }

    // 更新工具栏标题
    private fun updateToolbarTitle() {
        if (multiSelectController.isInMultiSelectMode()) {
            val count = fileClickHandler.getSelectedFiles().size
            val stats = statsSubtitle ?: ""
            supportActionBar?.subtitle = if (stats.isNotEmpty()) "$stats   " + getString(R.string.selected_count, count) else getString(R.string.selected_count, count)
        } else {
            supportActionBar?.subtitle = statsSubtitle
        }
    }

    // 保存状态
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("cached_path", browserController.currentDir.absolutePath)
        val lm = recyclerView.layoutManager as? LinearLayoutManager
        if (lm != null) {
            val pos = lm.findFirstVisibleItemPosition()
            if (pos != RecyclerView.NO_POSITION) {
                val view = lm.findViewByPosition(pos)
                val offset = view?.top ?: 0
                outState.putInt("scroll_pos", pos)
                outState.putInt("scroll_offset", offset)
            }
        }
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
            browserController.refresh(animate = false)
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

    // 初始加载
    private fun initLoad() {
        permissionHelper.requestStoragePermission(
            onGranted = { loadDir(browserController.currentDir, scrollToTop = true) },
            onDenied = { toast(this, getString(R.string.need_storage_permission)); finish() }
        )
    }

    // 加载目录
    private fun loadDir(dir: File, showLoading: Boolean = true, scrollToTop: Boolean = true, restorePosition: Int? = null) {
        exitMultiSelect()
        browserController.loadDir(dir, showLoading, scrollToTop, restorePosition)
        bookmarkManager.updateMenu(dir)
        supportActionBar?.title = browserController.currentDisplayPath()
    }

    // 导航加载目录
    private suspend fun navigateToDir(dir: File) {
        browserController.saveScrollPosition()
        val savedPos = browserController.getScrollPosition(dir.absolutePath)
        loadDir(dir, scrollToTop = false, restorePosition = savedPos)
    }

    // 刷新目录
    private fun refreshDir(dir: File, highlightPath: String?) {
        exitMultiSelect()
        browserController.loadDir(dir, showLoading = true, scrollToTop = false, restorePosition = null, highlightPath = highlightPath)
        bookmarkManager.updateMenu(dir)
        supportActionBar?.title = browserController.currentDisplayPath()
    }

    // 定位文件
    fun locateFile(file: File) {
        browserController.locateFile(file)
    }
}

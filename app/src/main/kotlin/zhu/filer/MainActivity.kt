package zhu.filer

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.BundleCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch
import java.io.File

import com.google.android.material.R as materialR
import zhu.filer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var recyclerView: RecyclerView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var fabAction: FloatingActionButton
    private lateinit var fabCancel: FloatingActionButton
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var findHelper: FindHelper
    private lateinit var adapter: FileListAdapter
    private lateinit var browserController: FileBrowserController
    private val clipboard = ClipboardManager()
    private lateinit var multiSelectController: MultiSelectController
    private lateinit var bookmarkManager: BookmarkManager

    private var showHidden: Boolean = false
    private var sortMode: SortMode = SortMode.NAME
    private var statsSubtitle: String? = null

    private val permissionHelper = PermissionHelper(this)
    private val backPressHandler = BackPressHandler(this)
    private val fabManager = FabManager(this)

    private val containerColor: Int by lazy { getThemeColor(this, materialR.attr.colorPrimaryContainer) }
    private var toolbarAlphaThreshold: Int = 0

    private var lastSwipeSelectPos: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = Color.TRANSPARENT
        }

        initViews()
        setupSwipeRefresh()
        prefs = getSharedPreferences("filer_prefs", MODE_PRIVATE)
        showHidden = prefs.getBoolean("show_hidden", false)
        sortMode = runCatching {
            SortMode.valueOf(prefs.getString("sort_mode", SortMode.NAME.name) ?: SortMode.NAME.name)
        }.getOrDefault(SortMode.NAME)

        setupToolbar()

        bookmarkManager = BookmarkManager(this, drawerLayout, navigationView, prefs, ::loadDir)
        bookmarkManager.setup()

        browserController = FileBrowserController(
            activity = this,
            recyclerView = recyclerView,
            swipeRefreshLayout = swipeRefreshLayout,
            prefs = prefs,
            showHiddenProvider = { showHidden },
            sortModeProvider = { sortMode },
            onDirLoaded = {
                if (::bookmarkManager.isInitialized) {
                    bookmarkManager.updateMenu(browserController.currentDir)
                }
                statsSubtitle = supportActionBar?.subtitle?.toString()
                updateToolbarTitle()
                updateMultiSelectFabs()
                fabManager.updatePasteButtons(clipboard)
            }
        )

        setupRecyclerView()
        binding.fastScroller.attach(recyclerView)
        setupSwipeToSelect()
        setupFabs()

        browserController.init(adapter)

        findHelper = FindHelper(this, { browserController.currentDir }, ::loadDir, ::locateFile)

        multiSelectController = MultiSelectController(
            activity = this,
            adapter = adapter,
            canNavigateUp = { browserController.canNavigateUp() },
            getCurrentDir = { browserController.currentDir },
            loadDir = ::loadDir,
            progressBar = progressBar,
            clipboardManager = clipboard,
            onClipboardChanged = { fabManager.updatePasteButtons(clipboard) },
            onExitMultiSelect = { updateMultiSelectFabs() }
        )

        backPressHandler.setup(
            multiSelectController = multiSelectController,
            drawerLayout = drawerLayout,
            browserController = browserController,
            onExit = { finish() }
        )

        savedInstanceState?.let { bundle ->
            val path = bundle.getString("cached_path") ?: return@let
            val scroll = BundleCompat.getParcelable(bundle, "scroll", Parcelable::class.java)
            browserController.saveScrollState(scroll)
            val dir = File(path)
            if (dir.exists() && dir.isDirectory && dir.canRead()) {
                loadDir(dir, scrollToTop = false)
            } else {
                initLoad()
            }
        } ?: initLoad()

        toolbar.post {
            toolbarAlphaThreshold = toolbar.height
            toolbar.setBackgroundColor(Color.TRANSPARENT)
        }
    }

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
                        showNavigateDialog(this@MainActivity, browserController.currentDir, ::loadDir, prefs)
                    }
                }
            }
        }
        val statusBarHeight = getStatusBarHeight()
        val actionBarHeight = toolbar.layoutParams.height
        toolbar.layoutParams.height = actionBarHeight + statusBarHeight
        toolbar.setPadding(0, statusBarHeight, 0, 0)
        toolbar.setBackgroundColor(Color.TRANSPARENT)
        toolbar.setSubtitleTextAppearance(this, R.style.ToolbarSubtitle)
    }

    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    private fun setupRecyclerView() {
        adapter = FileListAdapter(
            onItemClick = { file, pos ->
                lastSwipeSelectPos = null
                if (multiSelectController.isInMultiSelectMode()) {
                    multiSelectController.toggleSelection(pos)
                    updateToolbarTitle()
                    updateMultiSelectFabs()
                    if (!adapter.hasSelection()) exitMultiSelect()
                    return@FileListAdapter
                }
                if (pos == 0 && browserController.canNavigateUp()) {
                    browserController.navigateUp()
                    return@FileListAdapter
                }
                val fileIndex = if (browserController.canNavigateUp()) pos - 1 else pos
                val target = browserController.getCurrentFiles().getOrNull(fileIndex) ?: return@FileListAdapter
                if (target.isDirectory) {
                    browserController.saveScrollPosition()
                    loadDir(target, scrollToTop = true)
                } else {
                    previewFile(this@MainActivity, target)
                }
            },
            onItemLongClick = { file, pos ->
                lastSwipeSelectPos = null
                if (multiSelectController.isInMultiSelectMode()) {
                    if (pos == 0 && browserController.canNavigateUp()) return@FileListAdapter true
                    multiSelectController.showBatchOperationMenu()
                    return@FileListAdapter true
                }
                if (pos == 0 && browserController.canNavigateUp()) return@FileListAdapter true
                val fileIndex = if (browserController.canNavigateUp()) pos - 1 else pos
                val target = browserController.getCurrentFiles().getOrNull(fileIndex) ?: return@FileListAdapter true
                showOps(
                    activity = this@MainActivity,
                    currentDir = browserController.currentDir,
                    loadDir = ::loadDir,
                    file = target,
                    progressBar = progressBar,
                    onCopyCut = { f, isCut ->
                        clipboard.set(f, isCut)
                        fabManager.updatePasteButtons(clipboard)
                    },
                    onBookmarkToggle = { path ->
                        bookmarkManager.toggleBookmarkWithConfirm(path)
                    },
                    isBookmarked = if (target.isDirectory) bookmarkManager.isBookmarked(target.absolutePath) else false
                )
                true
            }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.itemAnimator = null

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (toolbarAlphaThreshold == 0) return
                val offset = recyclerView.computeVerticalScrollOffset()
                val alpha = (offset.toFloat() / toolbarAlphaThreshold).coerceIn(0f, 1f)
                val color = Color.argb(
                    (alpha * 255).toInt(),
                    Color.red(containerColor),
                    Color.green(containerColor),
                    Color.blue(containerColor)
                )
                toolbar.setBackgroundColor(color)
            }
        })
    }

    private fun setupSwipeToSelect() {
        val callback = SwipeToSelectCallback(adapter) { position, willBeSelected ->
            if (position == 0 && browserController.canNavigateUp()) return@SwipeToSelectCallback
            if (!willBeSelected) {
                lastSwipeSelectPos = null
                if (multiSelectController.isInMultiSelectMode()) {
                    multiSelectController.toggleSelection(position)
                    updateToolbarTitle()
                    updateMultiSelectFabs()
                    if (!adapter.hasSelection()) exitMultiSelect()
                }
                return@SwipeToSelectCallback
            }
            val lastPos = lastSwipeSelectPos
            if (lastPos != null && lastPos != position && multiSelectController.isInMultiSelectMode()) {
                val range = if (lastPos < position) lastPos..position else position..lastPos
                range.forEach { adapter.selectPosition(it) }
            } else {
                if (!multiSelectController.isInMultiSelectMode()) {
                    multiSelectController.selectPosition(position)
                } else {
                    multiSelectController.toggleSelection(position)
                }
            }
            lastSwipeSelectPos = position
            updateToolbarTitle()
            updateMultiSelectFabs()
        }
        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)
    }

    private fun setupFabs() {
        fabAdd.setOnClickListener { 
            showCreate(this, browserController.currentDir) { dir, highlightPath ->
                lifecycleScope.launch { refreshDir(dir, highlightPath) }
            }
        }
        fabManager.setup(
            fabAction = fabAction,
            fabCancel = fabCancel,
            clipboard = clipboard,
            targetDirProvider = { browserController.currentDir },
            onPaste = { files, isMove, overwrite ->
                lifecycleScope.launch { performPaste(files, browserController.currentDir, isMove, overwrite) }
            },
            onCancel = {
                clipboard.clear()
                fabManager.updatePasteButtons(clipboard)
            }
        )
        fabManager.setMultiSelectActions(
            onSelectAll = {
                val startPos = if (browserController.canNavigateUp()) 1 else 0
                for (i in startPos until adapter.itemCount) {
                    adapter.selectPosition(i)
                }
                updateToolbarTitle()
                updateMultiSelectFabs()
            },
            onDeselect = {
                adapter.clearSelection()
                exitMultiSelect()
            }
        )
    }

    private fun updateMultiSelectFabs() {
        fabManager.updateMultiSelectButtons(multiSelectController.isInMultiSelectMode())
    }

    private suspend fun performPaste(files: List<File>, targetDir: File, isMove: Boolean, overwrite: Boolean) {
        browserController.saveScrollPosition()
        val savedPos = browserController.getCurrentScrollPosition()

        progressBar.isVisible = true
        var successCount = 0
        var skipCount = 0
        var failCount = 0
        for (file in files) {
            val targetFile = File(targetDir, file.name)
            try {
                if (targetFile.exists()) {
                    if (overwrite) {
                        if (targetFile.isDirectory) targetFile.deleteRecursively() else targetFile.delete()
                    } else {
                        skipCount++
                        continue
                    }
                }
                val opSuccess = if (isMove) {
                    if (file.renameTo(targetFile)) {
                        true
                    } else {
                        if (file.isDirectory) {
                            file.copyRecursively(targetFile, overwrite = true)
                            file.deleteRecursively()
                        } else {
                            file.copyTo(targetFile, overwrite = true)
                            file.delete()
                        }
                        true
                    }
                } else {
                    if (file.isDirectory) {
                        file.copyRecursively(targetFile, overwrite = overwrite)
                    } else {
                        file.copyTo(targetFile, overwrite = overwrite)
                    }
                    true
                }
                if (opSuccess) successCount++ else failCount++
            } catch (e: Exception) {
                failCount++
            }
        }
        progressBar.isVisible = false
        val msgRes = when {
            failCount == 0 && skipCount == 0 -> if (isMove) R.string.move_success else R.string.copy_success
            failCount == 0 && skipCount > 0 -> if (isMove) R.string.move_success_skip else R.string.copy_success_skip
            failCount > 0 -> R.string.paste_result_partial
            else -> R.string.paste_failed
        }
        val msg = when (msgRes) {
            R.string.move_success, R.string.copy_success -> getString(msgRes, successCount)
            R.string.move_success_skip, R.string.copy_success_skip -> getString(msgRes, successCount, skipCount)
            R.string.paste_result_partial -> getString(msgRes, successCount, failCount)
            else -> getString(msgRes)
        }
        toast(this, msg)
        clipboard.clear()
        fabManager.updatePasteButtons(clipboard)
        loadDir(targetDir, scrollToTop = false, restorePosition = savedPos)
    }

    private fun setupSwipeRefresh() {
        val parent = recyclerView.parent as androidx.constraintlayout.widget.ConstraintLayout
        val params = recyclerView.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        parent.removeView(recyclerView)
        swipeRefreshLayout = SwipeRefreshLayout(this).apply {
            layoutParams = params
            setOnRefreshListener { browserController.refresh() }
            setProgressBackgroundColorSchemeColor(
                getThemeColor(this@MainActivity, com.google.android.material.R.attr.colorSurface)
            )
            setColorSchemeColors(
                getThemeColor(this@MainActivity, materialR.attr.colorPrimary),
                getThemeColor(this@MainActivity, materialR.attr.colorSecondary),
                getThemeColor(this@MainActivity, materialR.attr.colorTertiary)
            )
            setProgressViewEndTarget(true, dpToPx(this@MainActivity, 64))
        }
        swipeRefreshLayout.addView(recyclerView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        parent.addView(swipeRefreshLayout)
    }

    private fun exitMultiSelect() {
        lastSwipeSelectPos = null
        multiSelectController.exitMultiSelect()
        updateToolbarTitle()
        updateMultiSelectFabs()
    }

    private fun updateToolbarTitle() {
        if (multiSelectController.isInMultiSelectMode()) {
            val count = adapter.getSelectedFiles().size
            val stats = statsSubtitle ?: ""
            supportActionBar?.subtitle = if (stats.isNotEmpty()) "$stats   " + getString(R.string.selected_count, count) else getString(R.string.selected_count, count)
        } else {
            supportActionBar?.subtitle = statsSubtitle
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("cached_path", browserController.currentDir.absolutePath)
        recyclerView.layoutManager?.let { outState.putParcelable("scroll", it.onSaveInstanceState()) }
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(Menu.FIRST + 5)?.title = getBookmarkMenuTitle()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, getString(R.string.refresh)).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(Menu.NONE, Menu.FIRST + 1, Menu.NONE, getString(R.string.search)).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(Menu.NONE, Menu.FIRST + 2, Menu.NONE, getString(R.string.search_result)).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        val hiddenItem = menu.add(Menu.NONE, Menu.FIRST + 4, Menu.NONE, getString(R.string.show_hidden))
        hiddenItem.setCheckable(true)
        hiddenItem.isChecked = showHidden
        hiddenItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        val bookmarkItem = menu.add(Menu.NONE, Menu.FIRST + 5, Menu.NONE, getBookmarkMenuTitle())
        bookmarkItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(Menu.NONE, Menu.FIRST + 6, Menu.NONE, getString(R.string.sort_by)).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(Menu.NONE, Menu.FIRST + 3, Menu.NONE, getString(R.string.exit)).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            Menu.FIRST -> browserController.refresh()
            Menu.FIRST + 1 -> findHelper.showSearchDialog()
            Menu.FIRST + 2 -> findHelper.showLastResult()
            Menu.FIRST + 3 -> finish()
            Menu.FIRST + 4 -> {
                showHidden = !showHidden
                item.isChecked = showHidden
                prefs.edit().putBoolean("show_hidden", showHidden).apply()
                browserController.refresh()
            }
            Menu.FIRST + 5 -> {
                val path = browserController.currentDir.absolutePath
                bookmarkManager.toggleBookmarkWithConfirm(path) {
                    invalidateOptionsMenu()
                }
            }
            Menu.FIRST + 6 -> showSortDialog()
        }
        return true
    }

    private fun getBookmarkMenuTitle(): String {
        val isBookmarked = bookmarkManager.isBookmarked(browserController.currentDir.absolutePath)
        return getString(if (isBookmarked) R.string.remove_current_bookmark else R.string.add_current_bookmark)
    }

    private fun showSortDialog() {
        val modes = SortMode.entries
        val labels = modes.map { getString(it.labelRes) }.toTypedArray()
        val current = modes.indexOf(sortMode)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.sort_by)
            .setSingleChoiceItems(labels, current) { dialog, which ->
                sortMode = modes[which]
                prefs.edit().putString("sort_mode", sortMode.name).apply()
                browserController.refresh()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        findHelper.dismiss()
    }

    private fun initLoad() {
        permissionHelper.requestStoragePermission(
            onGranted = { loadDir(browserController.currentDir, scrollToTop = true) },
            onDenied = { toast(this, getString(R.string.need_storage_permission)); finish() }
        )
    }

    private fun loadDir(dir: File, showLoading: Boolean = true, scrollToTop: Boolean = false, restorePosition: Int? = null) {
        exitMultiSelect()
        browserController.loadDir(dir, showLoading, scrollToTop, restorePosition)
        bookmarkManager.updateMenu(dir)
    }

    private fun refreshDir(dir: File, highlightPath: String?) {
        exitMultiSelect()
        browserController.loadDir(dir, showLoading = true, scrollToTop = false, restorePosition = null, highlightPath = highlightPath)
        bookmarkManager.updateMenu(dir)
    }

    fun locateFile(file: File) {
        browserController.locateFile(file)
    }
}

package zhu.filer

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var recyclerView: RecyclerView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var fabPaste: FloatingActionButton
    private lateinit var fabCancel: FloatingActionButton
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var findHelper: FindHelper
    private lateinit var adapter: FileListAdapter
    private lateinit var browserController: FileBrowserController
    private val clipboard = ClipboardManager()
    private lateinit var fileOperationHelper: FileOperationHelper

    private var backPressedOnce = false
    private var showHidden: Boolean = false

    private val requestStorage = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.all { it }) initLoad() else { toast(this, "需要存储权限"); finish() }
    }
    private val requestManage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) initLoad()
        else { toast(this, "需要所有文件访问权限"); finish() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        recyclerView = findViewById(R.id.recycler_view)
        toolbar = findViewById(R.id.toolbar)
        progressBar = findViewById(R.id.progress_bar)
        fabAdd = findViewById(R.id.fab_add)
        fabPaste = findViewById(R.id.fab_paste)
        fabCancel = findViewById(R.id.fab_cancel)

        prefs = getSharedPreferences("filer_prefs", MODE_PRIVATE)
        showHidden = prefs.getBoolean("show_hidden", false)

        setupToolbar()
        setupNavigationMenu()
        setupRecyclerView()
        setupFabs()
        setupSwipeRefresh()

        browserController = FileBrowserController(
            activity = this,
            recyclerView = recyclerView,
            swipeRefreshLayout = swipeRefreshLayout,
            prefs = prefs,
            showHiddenProvider = { showHidden },
            onDirLoaded = { updateNavigationMenu() }
        )
        browserController.init(adapter)

        findHelper = FindHelper(this, { browserController.currentDir }, ::loadDir, ::locateFile)

        fileOperationHelper = FileOperationHelper(
            activity = this,
            progressBar = progressBar,
            onComplete = {
                clipboard.clear()
                updatePasteButtons()
                loadDir(browserController.currentDir)
            }
        )

        setupBackPressed()

        savedInstanceState?.let { bundle ->
            val path = bundle.getString("cached_path") ?: return@let
            browserController.saveScrollState(bundle.getParcelable("scroll"))
            val dir = File(path)
            if (dir.exists() && dir.isDirectory && dir.canRead()) {
                loadDir(dir, scrollToTop = false)
            } else {
                initLoad()
            }
        } ?: initLoad()
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
    }

    private fun setupNavigationMenu() {
        val menu = navigationView.menu
        menu.add(0, 1, 0, "根目录").setIcon(R.drawable.outline_phone_android_24)
        menu.add(0, 2, 1, "内部存储").setIcon(R.drawable.outline_sd_card_24)
        menu.findItem(1).isCheckable = true
        menu.findItem(2).isCheckable = true
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                1 -> loadDir(File("/"), scrollToTop = true)
                2 -> loadDir(Environment.getExternalStorageDirectory(), scrollToTop = true)
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun updateNavigationMenu() {
        val dir = browserController.currentDir
        val menu = navigationView.menu
        menu.findItem(1).isChecked = dir.absolutePath == "/"
        menu.findItem(2).isChecked = dir.absolutePath == Environment.getExternalStorageDirectory().absolutePath
    }

    private fun setupRecyclerView() {
        adapter = FileListAdapter(
            onItemClick = { file, pos ->
                if (pos == 0 && browserController.canNavigateUp()) {
                    browserController.navigateUp()
                    return@FileListAdapter
                }
                val idx = if (browserController.canNavigateUp()) pos - 1 else pos
                val target = browserController.getCurrentFiles().getOrNull(idx) ?: return@FileListAdapter
                if (target.isDirectory) {
                    browserController.saveScrollPosition()
                    loadDir(target, scrollToTop = true)
                } else previewFile(this, target)
            },
            onItemLongClick = { file, pos ->
                if (pos == 0 && browserController.canNavigateUp()) return@FileListAdapter true
                val idx = if (browserController.canNavigateUp()) pos - 1 else pos
                val target = browserController.getCurrentFiles().getOrNull(idx) ?: return@FileListAdapter true
                showOps(
                    activity = this,
                    currentDir = browserController.currentDir,
                    loadDir = ::loadDir,
                    file = target,
                    progressBar = progressBar,
                    onCopyCut = { f, isCut ->
                        clipboard.set(f, isCut)
                        updatePasteButtons()
                    }
                )
                true
            }
        )
        recyclerView.layoutManager = null
        recyclerView.itemAnimator = null
    }

    private fun setupFabs() {
        fabAdd.setOnClickListener { showCreate(this, browserController.currentDir, ::loadDir) }

        fabPaste.setOnClickListener {
            val file = clipboard.getFile() ?: return@setOnClickListener
            val target = File(browserController.currentDir, file.name)
            if (target.exists()) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("目标已存在")
                    .setMessage("${target.name} 已存在，是否覆盖？")
                    .setPositiveButton("覆盖") { _, _ ->
                        fileOperationHelper.performPaste(file, target, clipboard.isCut(), true)
                    }
                    .setNegativeButton("跳过") { _, _ ->
                        clipboard.clear()
                        updatePasteButtons()
                    }
                    .show()
            } else {
                fileOperationHelper.performPaste(file, target, clipboard.isCut(), false)
            }
        }

        fabCancel.setOnClickListener {
            clipboard.clear()
            updatePasteButtons()
        }
    }

    private fun setupSwipeRefresh() {
        val parent = recyclerView.parent as androidx.constraintlayout.widget.ConstraintLayout
        val params = recyclerView.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        parent.removeView(recyclerView)

        swipeRefreshLayout = SwipeRefreshLayout(this).apply {
            layoutParams = params
            setOnRefreshListener { browserController.refresh() }
            setColorSchemeColors(
                ContextCompat.getColor(this@MainActivity, android.R.color.holo_blue_light),
                ContextCompat.getColor(this@MainActivity, android.R.color.holo_green_light),
                ContextCompat.getColor(this@MainActivity, android.R.color.holo_orange_light)
            )
        }
        swipeRefreshLayout.addView(recyclerView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        parent.addView(swipeRefreshLayout)
    }

    private fun setupBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else if (browserController.canNavigateUp()) {
                    browserController.navigateUp()
                } else if (backPressedOnce) {
                    finish()
                } else {
                    backPressedOnce = true
                    toast(this@MainActivity, "再次返回退出")
                    Handler(Looper.getMainLooper()).postDelayed({ backPressedOnce = false }, 2000)
                }
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("cached_path", browserController.currentDir.absolutePath)
        recyclerView.layoutManager?.let { outState.putParcelable("scroll", it.onSaveInstanceState()) }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, "刷新").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(Menu.NONE, Menu.FIRST + 1, Menu.NONE, "查找").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(Menu.NONE, Menu.FIRST + 2, Menu.NONE, "查找结果").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        val hiddenItem = menu.add(Menu.NONE, Menu.FIRST + 4, Menu.NONE, "显示隐藏文件")
        hiddenItem.setCheckable(true)
        hiddenItem.isChecked = showHidden
        hiddenItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(Menu.NONE, Menu.FIRST + 3, Menu.NONE, "退出").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
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
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        findHelper.dismiss()
    }

    private fun initLoad() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                loadDir(browserController.currentDir, scrollToTop = true)
            } else {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                requestManage.launch(intent)
            }
        } else {
            val perms = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (perms.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED })
                loadDir(browserController.currentDir, scrollToTop = true)
            else requestStorage.launch(perms)
        }
    }

    private fun loadDir(dir: File, showLoading: Boolean = true, scrollToTop: Boolean = true) {
        browserController.loadDir(dir, showLoading, scrollToTop)
    }

    fun locateFile(file: File) {
        browserController.locateFile(file)
    }

    private fun updatePasteButtons() {
        if (clipboard.hasContent()) {
            AnimationHelper.showButtons(fabPaste, fabCancel)
        } else {
            AnimationHelper.hideButtons(fabPaste, fabCancel)
        }
    }
}
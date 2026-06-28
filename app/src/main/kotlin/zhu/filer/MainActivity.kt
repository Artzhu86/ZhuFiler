package zhu.filer

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.text.format.Formatter
import android.util.TypedValue
import android.view.*
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var recyclerView: RecyclerView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var progressBar: ProgressBar
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private var currentDir: File = Environment.getExternalStorageDirectory()
    private var currentFiles: List<File> = emptyList()
    private lateinit var adapter: FileListAdapter
    private var backPressedOnce = false
    private lateinit var prefs: SharedPreferences

    private lateinit var findHelper: FindHelper
    private var loadJob: Job? = null
    private val scrollPositions = mutableMapOf<String, Int>()
    private var savedScrollState: Parcelable? = null

    private val requestStorage = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (it.values.all { v -> v }) initLoad() else { toast(this, "需要存储权限"); finish() }
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

        prefs = getSharedPreferences("filer_prefs", MODE_PRIVATE)
        findHelper = FindHelper(this, { currentDir }, ::loadDir, ::locateFile)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        val typedValue = TypedValue()
        if (theme.resolveAttribute(com.google.android.material.R.attr.colorPrimaryContainer, typedValue, true)) {
            window.statusBarColor = typedValue.data
        } else {
            theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
            window.statusBarColor = typedValue.data
        }

        val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, 0, 0)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        setupNavigationMenu()

        toolbar.post {
            for (i in 0 until toolbar.childCount) {
                val child = toolbar.getChildAt(i)
                if (child is TextView) {
                    child.setOnClickListener {
                        showNavigateDialog(this@MainActivity, currentDir, ::loadDir, prefs)
                    }
                }
            }
        }

        adapter = FileListAdapter(
            onItemClick = { file, pos ->
                if (pos == 0 && canUp()) { navigateUp(); return@FileListAdapter }
                val idx = if (canUp()) pos - 1 else pos
                val target = currentFiles.getOrNull(idx) ?: return@FileListAdapter
                if (target.isDirectory) {
                    lifecycleScope.launch {
                        saveScrollPosition()
                        loadDir(target, scrollToTop = true)
                    }
                } else previewFile(this, target)
            },
            onItemLongClick = { file, pos ->
                if (pos == 0 && canUp()) return@FileListAdapter true
                val idx = if (canUp()) pos - 1 else pos
                val target = currentFiles.getOrNull(idx) ?: return@FileListAdapter true
                showOps(this, currentDir, ::loadDir, target, progressBar)
                true
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.itemAnimator = null
        recyclerView.adapter = adapter
        fabAdd.setOnClickListener { showCreate(this, currentDir, ::loadDir) }

        val parent = recyclerView.parent as androidx.constraintlayout.widget.ConstraintLayout
        val params = recyclerView.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        parent.removeView(recyclerView)

        swipeRefreshLayout = SwipeRefreshLayout(this).apply {
            layoutParams = params
            setOnRefreshListener { refreshCurrentDir() }
            setColorSchemeColors(
                ContextCompat.getColor(this@MainActivity, android.R.color.holo_blue_light),
                ContextCompat.getColor(this@MainActivity, android.R.color.holo_green_light),
                ContextCompat.getColor(this@MainActivity, android.R.color.holo_orange_light)
            )
        }
        swipeRefreshLayout.addView(recyclerView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        parent.addView(swipeRefreshLayout)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else if (canUp()) {
                    navigateUp()
                } else if (backPressedOnce) {
                    finish()
                } else {
                    backPressedOnce = true
                    toast(this@MainActivity, "再次返回退出")
                    Handler(Looper.getMainLooper()).postDelayed({ backPressedOnce = false }, 2000)
                }
            }
        })

        savedInstanceState?.let { bundle ->
            val path = bundle.getString("cached_path") ?: return@let
            savedScrollState = bundle.getParcelable("scroll")
            val dir = File(path)
            if (dir.exists() && dir.isDirectory && dir.canRead()) {
                currentDir = dir
                lifecycleScope.launch { loadDir(currentDir, scrollToTop = false) }
            } else {
                initLoad()
            }
        } ?: initLoad()
    }

    private fun setupNavigationMenu() {
        val menu = navigationView.menu
        menu.add(0, 1, 0, "根目录").setIcon(R.drawable.outline_phone_android_24)
        menu.add(0, 2, 1, "内部存储").setIcon(R.drawable.outline_sd_card_24)

        menu.findItem(1).isCheckable = true
        menu.findItem(2).isCheckable = true

        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                1 -> lifecycleScope.launch { loadDir(File("/"), scrollToTop = true) }
                2 -> lifecycleScope.launch { loadDir(Environment.getExternalStorageDirectory(), scrollToTop = true) }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun updateNavigationMenuSelection(dir: File) {
        val rootPath = "/"
        val internalPath = Environment.getExternalStorageDirectory().absolutePath
        val menu = navigationView.menu
        menu.findItem(1).isChecked = dir.absolutePath == rootPath
        menu.findItem(2).isChecked = dir.absolutePath == internalPath
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("cached_path", currentDir.absolutePath)
        recyclerView.layoutManager?.let { outState.putParcelable("scroll", it.onSaveInstanceState()) }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, "刷新").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(Menu.NONE, Menu.FIRST + 1, Menu.NONE, "查找").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(Menu.NONE, Menu.FIRST + 2, Menu.NONE, "查找结果").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(Menu.NONE, Menu.FIRST + 3, Menu.NONE, "退出").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            Menu.FIRST -> refreshCurrentDir()
            Menu.FIRST + 1 -> findHelper.showSearchDialog()
            Menu.FIRST + 2 -> findHelper.showLastResult()
            Menu.FIRST + 3 -> finish()
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        findHelper.dismiss()
        loadJob?.cancel()
    }

    private fun refreshCurrentDir() {
        swipeRefreshLayout.isRefreshing = true
        lifecycleScope.launch {
            try {
                saveScrollPosition()
                loadDir(currentDir, showLoading = false, scrollToTop = false)
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun initLoad() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                lifecycleScope.launch { loadDir(currentDir, scrollToTop = true) }
            } else {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                requestManage.launch(intent)
            }
        } else {
            val perms = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (perms.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED })
                lifecycleScope.launch { loadDir(currentDir, scrollToTop = true) }
            else requestStorage.launch(perms)
        }
    }

    private fun saveScrollPosition() {
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        val firstPos = layoutManager.findFirstVisibleItemPosition()
        if (firstPos == RecyclerView.NO_POSITION) return

        val view = layoutManager.findViewByPosition(firstPos)
        if (view == null) {
            scrollPositions[currentDir.absolutePath] = firstPos
            return
        }

        val rect = android.graphics.Rect()
        view.getLocalVisibleRect(rect)
        val visibleHeight = rect.height()
        val totalHeight = view.height
        if (totalHeight == 0) {
            scrollPositions[currentDir.absolutePath] = firstPos
            return
        }

        val visibleRatio = visibleHeight.toFloat() / totalHeight
        val targetPos = if (visibleRatio >= 0.5f) firstPos else {
            val nextPos = firstPos + 1
            if (nextPos < adapter.itemCount) nextPos else firstPos
        }
        scrollPositions[currentDir.absolutePath] = targetPos
    }

    private suspend fun loadDir(dir: File, showLoading: Boolean = true, scrollToTop: Boolean = true) {
        loadJob?.cancel()
        loadJob = currentCoroutineContext()[Job]

        if (showLoading) swipeRefreshLayout.isRefreshing = true

        currentDir = dir
        supportActionBar?.title = dir.getDisplayPath()

        val items = withContext(Dispatchers.IO) {
            val fileList = dir.listFiles() ?: emptyArray()

            if (fileList.isEmpty() && !dir.canRead() && dir.exists()) {
                withContext(Dispatchers.Main) {
                    toast(this@MainActivity, "权限被拒绝")
                }
            }

            val sortedFiles = fileList.sortedWith(fileComparator)

            val tempItems = mutableListOf<FileItem>()
            dir.parentFile?.let {
                tempItems.add(FileItem(it, "..", "📁", ""))
            }
            tempItems.addAll(sortedFiles.map { file ->
                val timeStr = DATE_FORMAT.format(Date(file.lastModified()))
                val subtitle = if (file.isDirectory) timeStr else "$timeStr  ${Formatter.formatFileSize(this@MainActivity, file.length())}"
                FileItem(file, file.name, if (file.isDirectory) "📁" else "📄", subtitle)
            })
            tempItems
        }

        if (!currentCoroutineContext().isActive) {
            if (showLoading) swipeRefreshLayout.isRefreshing = false
            return
        }

        currentFiles = items.drop(if (dir.parentFile != null) 1 else 0).map { it.file }
        updateSubtitle()
        updateRecentDirs(prefs, dir.absolutePath)

        adapter.submitList(items)
        adapter.clearHighlight()

        if (scrollToTop) recyclerView.post { recyclerView.scrollToPosition(0) }
        else savedScrollState?.let { state ->
            recyclerView.post { recyclerView.layoutManager?.onRestoreInstanceState(state) }
            savedScrollState = null
        }

        if (showLoading) swipeRefreshLayout.isRefreshing = false

        updateNavigationMenuSelection(dir)
    }

    private fun canUp() = currentDir.parentFile != null

    private fun navigateUp() {
        val parent = currentDir.parentFile ?: return
        val childDir = currentDir
        lifecycleScope.launch {
            saveScrollPosition()
            loadDir(parent, scrollToTop = false)
            val savedPos = scrollPositions[parent.absolutePath]
            if (savedPos != null && savedPos >= 0 && savedPos < adapter.itemCount) {
                recyclerView.post {
                    (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(savedPos, 0)
                }
            }
            val index = currentFiles.indexOfFirst { it.absolutePath == childDir.absolutePath }
            if (index >= 0) {
                val pos = if (canUp()) index + 1 else index
                adapter.setHighlight(pos)
                recyclerView.post { adapter.startBlink(pos) }
            }
        }
    }

    private fun updateSubtitle() {
        val (dirs, files) = getDirStats(currentDir)
        supportActionBar?.subtitle = "目录: $dirs  文件: $files"
    }

    fun locateFile(file: File) {
        lifecycleScope.launch {
            val targetDir = if (file.isDirectory) file else file.parentFile
            if (targetDir == null || !targetDir.exists()) {
                toast(this@MainActivity, "无法定位")
                return@launch
            }
            saveScrollPosition()
            loadDir(targetDir, scrollToTop = false)
            if (!file.isDirectory) {
                val index = currentFiles.indexOfFirst { it.absolutePath == file.absolutePath }
                if (index >= 0) {
                    val pos = if (canUp()) index + 1 else index
                    adapter.setHighlight(pos)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    layoutManager.scrollToPositionWithOffset(pos, 0)
                    recyclerView.post { adapter.startBlink(pos) }
                }
            }
        }
    }
}
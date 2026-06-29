package zhu.filer

import android.os.Environment
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import java.io.File

class FileBrowserController(
    private val activity: AppCompatActivity,
    private val recyclerView: RecyclerView,
    private val swipeRefreshLayout: SwipeRefreshLayout,
    private val prefs: android.content.SharedPreferences,
    private val showHiddenProvider: () -> Boolean,
    private val onDirLoaded: () -> Unit
) {

    var currentDir: File = Environment.getExternalStorageDirectory()
        private set

    private var currentFiles: List<File> = emptyList()
    private lateinit var adapter: FileListAdapter
    private var loadJob: Job? = null
    private val scrollPositions = mutableMapOf<String, Int>()
    private var savedScrollState: Parcelable? = null
    private val fileLoader = FileListLoader(activity)

    private val canUp: Boolean
        get() = currentDir.parentFile != null

    fun init(adapter: FileListAdapter) {
        this.adapter = adapter
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.itemAnimator = null
    }

    fun loadDir(dir: File, showLoading: Boolean = true, scrollToTop: Boolean = true) {
        loadJob?.cancel()
        loadJob = activity.lifecycleScope.launch {
            try {
                if (showLoading) swipeRefreshLayout.isRefreshing = true

                currentDir = dir
                activity.supportActionBar?.title = dir.getDisplayPath()

                val items = fileLoader.loadItems(dir, showHiddenProvider())

                if (currentCoroutineContext().isActive) {
                    currentFiles = items.drop(if (dir.parentFile != null) 1 else 0).map { it.file }
                    val (dirs, files) = fileLoader.getStats(dir)
                    activity.supportActionBar?.subtitle = "目录: $dirs  文件: $files"
                    updateRecentDirs(prefs, dir.absolutePath)

                    adapter.submitList(items)
                    adapter.clearHighlight()

                    if (scrollToTop) {
                        recyclerView.post { recyclerView.scrollToPosition(0) }
                    } else {
                        savedScrollState?.let { state ->
                            recyclerView.post { recyclerView.layoutManager?.onRestoreInstanceState(state) }
                            savedScrollState = null
                        }
                    }

                    onDirLoaded()
                }
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    fun refresh() {
        loadDir(currentDir, showLoading = false, scrollToTop = false)
    }

    fun navigateUp(onComplete: (() -> Unit)? = null) {
        val parent = currentDir.parentFile ?: return
        val childDir = currentDir
        saveScrollPosition()
        loadDir(parent, scrollToTop = false)
        activity.lifecycleScope.launch {
            recyclerView.postDelayed({
                val savedPos = scrollPositions[parent.absolutePath]
                if (savedPos != null && savedPos >= 0 && savedPos < adapter.itemCount) {
                    recyclerView.post {
                        (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(savedPos, 0)
                    }
                }
                val index = currentFiles.indexOfFirst { it.absolutePath == childDir.absolutePath }
                if (index >= 0) {
                    val pos = if (canUp) index + 1 else index
                    adapter.setHighlight(pos)
                    recyclerView.post { adapter.startBlink(pos) }
                }
                onComplete?.invoke()
            }, 200)
        }
    }

    fun locateFile(file: File) {
        val targetDir = if (file.isDirectory) file else file.parentFile
        if (targetDir == null || !targetDir.exists()) {
            toast(activity, "无法定位")
            return
        }
        saveScrollPosition()
        loadDir(targetDir, scrollToTop = false)
        if (!file.isDirectory) {
            recyclerView.postDelayed({
                val index = currentFiles.indexOfFirst { it.absolutePath == file.absolutePath }
                if (index >= 0) {
                    val pos = if (canUp) index + 1 else index
                    adapter.setHighlight(pos)
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    layoutManager.scrollToPositionWithOffset(pos, 0)
                    recyclerView.post { adapter.startBlink(pos) }
                }
            }, 200)
        }
    }

    fun saveScrollPosition() {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
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

    fun getCurrentFiles(): List<File> = currentFiles
    fun canNavigateUp(): Boolean = canUp
    fun saveScrollState(state: Parcelable?) { savedScrollState = state }
}
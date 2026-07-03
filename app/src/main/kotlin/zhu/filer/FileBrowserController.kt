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
    private val sortModeProvider: () -> SortMode,
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

    fun loadDir(dir: File, showLoading: Boolean = true, scrollToTop: Boolean = true, restorePosition: Int? = null, highlightPath: String? = null) {
        loadJob?.cancel()
        loadJob = activity.lifecycleScope.launch {
            try {
                if (showLoading) swipeRefreshLayout.isRefreshing = true

                currentDir = dir
                activity.supportActionBar?.title = dir.getDisplayPath()

                val items = fileLoader.loadItems(dir, showHiddenProvider(), sortModeProvider())

                if (currentCoroutineContext().isActive) {
                    currentFiles = items.drop(if (dir.parentFile != null) 1 else 0).map { it.file }
                    val (dirs, files) = fileLoader.getStats(dir)
                    activity.supportActionBar?.subtitle = "${activity.getString(R.string.dir_count_label)}: $dirs  ${activity.getString(R.string.file_count_label)}: $files"
                    updateRecentDirs(prefs, dir.absolutePath)

                    var highlightPos = -1
                    if (highlightPath != null) {
                        val index = currentFiles.indexOfFirst { it.absolutePath == highlightPath }
                        if (index >= 0) {
                            highlightPos = if (dir.parentFile != null) index + 1 else index
                        }
                    }

                    adapter.submitList(items, highlightPos = highlightPos)

                    if (scrollToTop) {
                        recyclerView.post { recyclerView.scrollToPosition(0) }
                    } else if (highlightPos >= 0) {
                        recyclerView.post {
                            val lm = recyclerView.layoutManager as? LinearLayoutManager ?: return@post
                            if (restorePosition != null && restorePosition >= 0 && restorePosition < adapter.itemCount) {
                                lm.scrollToPositionWithOffset(restorePosition, 0)
                                recyclerView.post {
                                    val first = lm.findFirstVisibleItemPosition()
                                    val last = lm.findLastVisibleItemPosition()
                                    if (highlightPos < first || highlightPos > last) {
                                        lm.scrollToPositionWithOffset(highlightPos, 0)
                                        scrollPositions[dir.absolutePath] = highlightPos
                                    }
                                }
                            } else {
                                lm.scrollToPositionWithOffset(highlightPos, 0)
                                scrollPositions[dir.absolutePath] = highlightPos
                            }
                        }
                    } else if (restorePosition != null && restorePosition >= 0 && restorePosition < adapter.itemCount) {
                        recyclerView.post {
                            (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(restorePosition, 0)
                        }
                    } else if (savedScrollState != null) {
                        recyclerView.post { recyclerView.layoutManager?.onRestoreInstanceState(savedScrollState) }
                        savedScrollState = null
                    }

                    onDirLoaded()
                }
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    fun refresh() {
        saveScrollPosition()
        val savedPos = scrollPositions[currentDir.absolutePath]
        loadDir(currentDir, showLoading = false, scrollToTop = false, restorePosition = savedPos)
    }

    fun navigateUp() {
        val parent = currentDir.parentFile ?: return
        val childDir = currentDir
        saveScrollPosition()
        val savedPos = scrollPositions[parent.absolutePath]
        loadDir(parent, showLoading = true, scrollToTop = false, restorePosition = savedPos, highlightPath = childDir.absolutePath)
    }

    fun locateFile(file: File) {
        val targetDir = if (file.isDirectory) file else file.parentFile
        if (targetDir == null || !targetDir.exists()) {
            toast(activity, activity.getString(R.string.cannot_read))
            return
        }
        saveScrollPosition()
        val highlightPath = if (!file.isDirectory) file.absolutePath else null
        loadDir(targetDir, scrollToTop = false, highlightPath = highlightPath)
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

    fun getCurrentScrollPosition(): Int? = scrollPositions[currentDir.absolutePath]

    fun getCurrentFiles(): List<File> = currentFiles
    fun canNavigateUp(): Boolean = canUp
    fun saveScrollState(state: Parcelable?) { savedScrollState = state }
}
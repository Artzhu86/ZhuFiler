package zhu.filer

import android.os.Environment
import android.text.format.Formatter
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 文件浏览器控制器
class FileBrowserController(
    private val activity: AppCompatActivity,
    private val toolbar: Toolbar,
    private val recyclerView: RecyclerView,
    private val swipeRefreshLayout: SwipeRefreshLayout,
    private val prefs: android.content.SharedPreferences,
    private val showHiddenProvider: () -> Boolean,
    private val sortModeProvider: () -> SortMode,
    private val onDirLoaded: () -> Unit
) {

    var currentDir: File = Environment.getExternalStorageDirectory()
        private set

    private lateinit var adapter: FileListAdapter
    private var loadJob: Job? = null
    private val scrollPositions = mutableMapOf<String, Int>()
    private val archiveScrollPositions = mutableMapOf<String, Int>()
    private var savedScrollPos: Int = -1
    private var savedScrollOffset: Int = 0
    private val fileLoader = FileListLoader(activity)

    // 归档状态
    private data class ArchiveState(val archiveFile: File, var internalPath: String, var password: String?)
    private var archiveState: ArchiveState? = null

    // 是否在归档中
    fun isInArchive(): Boolean = archiveState != null

    // 获取归档文件
    fun getArchiveFile(): File? = archiveState?.archiveFile

    // 获取归档密码
    fun getArchivePassword(): String? = archiveState?.password

    // 缓存归档密码
    fun cacheArchivePassword(password: String) {
        archiveState?.password = password
    }

    // 当前显示路径
    fun currentDisplayPath(): String =
        formatDisplayPath(archiveState, currentDir)

    // 格式化显示路径
    private fun formatDisplayPath(
        archiveState: ArchiveState?,
        currentDirectory: File
    ): String = when {
        archiveState != null && archiveState.internalPath.isNotEmpty() ->
            File(archiveState.archiveFile.absolutePath, archiveState.internalPath).absolutePath + "/"
        archiveState != null ->
            archiveState.archiveFile.absolutePath.trimEnd('/') + "/"
        else ->
            currentDirectory.absolutePath.trimEnd('/') + "/"
    }

    private val canUp: Boolean
        get() = archiveState != null || currentDir.parentFile != null

    // 初始化
    fun init(adapter: FileListAdapter) {
        this.adapter = adapter
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.itemAnimator = null
    }

    // 加载目录
    fun loadDir(dir: File, showLoading: Boolean = true, scrollToTop: Boolean = true, restorePosition: Int? = null, highlightPath: String? = null, animate: Boolean = true) {
        archiveState = null
        loadJob?.cancel()
        if (showLoading) swipeRefreshLayout.isRefreshing = true
        loadJob = activity.lifecycleScope.launch {
            try {
                currentDir = dir
                applyToolbarTitlePath(toolbar, currentDisplayPath())

                val items = fileLoader.loadItems(dir, showHiddenProvider(), sortModeProvider())

                if (currentCoroutineContext().isActive) {
                    val (dirs, files) = fileLoader.getStats(dir)
                    activity.supportActionBar?.subtitle = "${activity.getString(R.string.dir_count_label)}: $dirs  ${activity.getString(R.string.file_count_label)}: $files"
                    updateRecentDirs(prefs, dir.absolutePath)

                    var highlightPos = -1
                    if (highlightPath != null) {
                        val index = items.indexOfFirst { it.file.absolutePath == highlightPath }
                        if (index >= 0) highlightPos = index
                    }

                    val lm = recyclerView.layoutManager as? LinearLayoutManager
                    val targetFirstVisible = when {
                        scrollToTop -> 0
                        highlightPos >= 0 && restorePosition != null && restorePosition >= 0 -> restorePosition
                        highlightPos >= 0 -> highlightPos
                        restorePosition != null && restorePosition >= 0 -> restorePosition
                        savedScrollPos >= 0 -> savedScrollPos
                        else -> lm?.findFirstVisibleItemPosition() ?: 0
                    }
                    adapter.submitList(items, highlightPos = highlightPos, firstVisible = targetFirstVisible, animate = animate)

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
                    } else if (savedScrollPos >= 0 && savedScrollPos < adapter.itemCount) {
                        val pos = savedScrollPos
                        val offset = savedScrollOffset
                        recyclerView.post {
                            (recyclerView.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(pos, offset)
                        }
                        savedScrollPos = -1
                    }

                    onDirLoaded()
                }
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    // 刷新
    fun refresh(animate: Boolean = true) {
        val state = archiveState
        if (state != null) {
            loadArchiveInternal(state.archiveFile, state.internalPath, state.password, animate = animate)
            return
        }
        saveScrollPosition()
        val savedPos = scrollPositions[currentDir.absolutePath]
        loadDir(currentDir, showLoading = true, scrollToTop = false, restorePosition = savedPos, animate = animate)
    }

    // 返回上一级
    fun navigateUp() {
        val state = archiveState
        if (state != null) {
            saveArchiveScrollPosition(state)
            if (state.internalPath.isNotEmpty()) {
                val parentPath = state.internalPath.substringBeforeLast('/', "")
                val savedPos = archiveScrollPositions[archiveScrollKey(state.archiveFile, parentPath)]
                loadArchiveInternal(state.archiveFile, parentPath, state.password, restorePosition = savedPos)
            } else {
                archiveState = null
                val parent = state.archiveFile.parentFile
                if (parent != null) {
                    val savedPos = scrollPositions[parent.absolutePath]
                    loadDir(parent, showLoading = true, scrollToTop = false, restorePosition = savedPos, highlightPath = state.archiveFile.absolutePath)
                }
            }
            return
        }
        val parent = currentDir.parentFile ?: return
        val childDir = currentDir
        saveScrollPosition()
        val savedPos = scrollPositions[parent.absolutePath]
        loadDir(parent, showLoading = true, scrollToTop = false, restorePosition = savedPos, highlightPath = childDir.absolutePath)
    }

    // 启动归档查看器
    fun launchArchiveViewer(file: File, password: String? = null, onPasswordRequired: () -> Unit = {}) {
        saveScrollPosition()
        archiveState = null
        loadArchiveInternal(file, "", password, onPasswordRequired)
    }

    // 导航到归档内路径
    fun navigateArchiveTo(internalPath: String) {
        val state = archiveState ?: return
        saveArchiveScrollPosition(state)
        loadArchiveInternal(state.archiveFile, internalPath, state.password)
    }

    // 加载归档内部
    private fun loadArchiveInternal(
        archiveFile: File,
        internalPath: String,
        password: String?,
        onPasswordRequired: () -> Unit = {},
        restorePosition: Int? = null,
        animate: Boolean = true
    ) {
        loadJob?.cancel()
        swipeRefreshLayout.isRefreshing = true
        loadJob = activity.lifecycleScope.launch {
            try {
                val entries = withContext(Dispatchers.IO) {
                    ArchiveEngine.listEntries(archiveFile, internalPath, password)
                }
                if (!currentCoroutineContext().isActive) return@launch
                archiveState = ArchiveState(archiveFile, internalPath, password)

                val items = mutableListOf<FileItem>()
                items.add(FileItem(File(".."), "..", R.drawable.outline_folder_24, ""))

                for (entry in entries) {
                    val syntheticFile = File(archiveFile, entry.path)
                    val iconRes = FileType.getIconRes(entry.name, entry.isDirectory)
                    val timeStr = SimpleDateFormat(activity.getString(R.string.date_format), Locale.getDefault()).format(Date(entry.lastModified))
                    val sizeStr = Formatter.formatFileSize(activity, entry.size)
                    val subtitle = "$timeStr  $sizeStr"
                    items.add(FileItem(
                        file = syntheticFile,
                        displayName = entry.name,
                        iconRes = iconRes,
                        subtitle = subtitle,
                        isDirectory = entry.isDirectory,
                        encrypted = entry.encrypted,
                        entryPath = entry.path,
                        size = entry.size
                    ))
                }

                applyToolbarTitlePath(toolbar, currentDisplayPath())
                val dirCount = entries.count { it.isDirectory }
                val fileCount = entries.count { !it.isDirectory }
                activity.supportActionBar?.subtitle = "${activity.getString(R.string.dir_count_label)}: $dirCount  ${activity.getString(R.string.file_count_label)}: $fileCount"

                val firstVisible = restorePosition ?: 0
                adapter.submitList(items, firstVisible = firstVisible, animate = animate)

                if (restorePosition != null && restorePosition >= 0 && restorePosition < adapter.itemCount) {
                    recyclerView.post {
                        (recyclerView.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(restorePosition, 0)
                    }
                } else {
                    recyclerView.post { recyclerView.scrollToPosition(0) }
                }

                onDirLoaded()
            } catch (e: ArchivePasswordRequiredException) {
                archiveState = null
                onPasswordRequired()
            } catch (e: Exception) {
                archiveState = null
                toast(activity, activity.getString(R.string.archive_open_failed))
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    // 定位文件
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

    // 保存滚动位置
    fun saveScrollPosition() {
        val key = currentDir.absolutePath
        val pos = calculateScrollPosition() ?: return
        scrollPositions[key] = pos
    }

    // 归档滚动键
    private fun archiveScrollKey(archiveFile: File, internalPath: String): String =
        "${archiveFile.absolutePath}::${internalPath}"

    // 保存归档滚动位置
    private fun saveArchiveScrollPosition(state: ArchiveState) {
        val key = archiveScrollKey(state.archiveFile, state.internalPath)
        val pos = calculateScrollPosition() ?: return
        archiveScrollPositions[key] = pos
    }

    // 计算滚动位置
    private fun calculateScrollPosition(): Int? {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return null
        val firstPos = layoutManager.findFirstVisibleItemPosition()
        if (firstPos == RecyclerView.NO_POSITION) return null
        val view = layoutManager.findViewByPosition(firstPos)
        if (view == null) return firstPos
        val rect = android.graphics.Rect()
        view.getLocalVisibleRect(rect)
        val visibleHeight = rect.height()
        val totalHeight = view.height
        if (totalHeight == 0) return firstPos
        val visibleRatio = visibleHeight.toFloat() / totalHeight
        return if (visibleRatio >= 0.5f) firstPos else {
            val nextPos = firstPos + 1
            if (nextPos < adapter.itemCount) nextPos else firstPos
        }
    }

    // 获取当前滚动位置
    fun getCurrentScrollPosition(): Int? = scrollPositions[currentDir.absolutePath]

    // 获取指定目录滚动位置
    fun getScrollPosition(path: String): Int? = scrollPositions[path]

    // 能否返回上一级
    fun canNavigateUp(): Boolean = canUp

    // 保存滚动状态
    fun saveScrollState(pos: Int, offset: Int) {
        savedScrollPos = pos
        savedScrollOffset = offset
    }
}

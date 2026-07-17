package zhu.filer.browser

import android.os.Environment
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.Job
import java.io.File
import zhu.filer.FileItem
import zhu.filer.util.SortMode
import zhu.filer.archive.ArchiveEngine
import zhu.filer.ui.dpToPx

// 文件浏览器控制器
class FileBrowserController(
    internal val activity: AppCompatActivity,
    internal val toolbar: Toolbar,
    internal val recyclerView: RecyclerView,
    internal val swipeRefreshLayout: SwipeRefreshLayout,
    internal val prefs: android.content.SharedPreferences,
    internal val showHiddenProvider: () -> Boolean,
    internal val sortModeProvider: () -> SortMode,
    internal val onDirLoaded: () -> Unit
) {

    internal var currentDir: File = Environment.getExternalStorageDirectory()
        internal set

    internal lateinit var adapter: FileListAdapter
        internal set

    internal var loadJob: Job? = null
    internal val scrollPositions = mutableMapOf<String, Int>()
    internal var savedScrollPos: Int = -1
    internal var savedScrollOffset: Int = 0
    internal val fileLoader = FileListLoader(activity)

    // 目录缓存
    internal data class DirCache(
        val items: List<FileItem>,
        val dirCount: Int,
        val fileCount: Int,
        val lastModified: Long,
        val childCount: Int
    )

    internal val dirCache = mutableMapOf<String, DirCache>()
    internal val dirtyDirs = mutableSetOf<String>()

    // 归档状态
    internal data class ArchiveState(
        val archiveFile: File,
        var internalPath: String,
        var password: String?,
        var allEntries: List<ArchiveEngine.EntryInfo> = emptyList()
    )

    internal var archiveState: ArchiveState? = null

    // 是否在归档中
    fun isInArchive(): Boolean = archiveState != null

    // 获取归档文件
    fun getArchiveFile(): File? = archiveState?.archiveFile

    // 获取归档密码
    fun getArchivePassword(): String? = archiveState?.password

    // 获取归档内部路径
    fun getArchiveInternalPath(): String? = archiveState?.internalPath

    // 缓存归档密码
    fun cacheArchivePassword(password: String) {
        archiveState?.password = password
    }

    // 当前显示路径
    fun currentDisplayPath(): String = formatDisplayPath(archiveState, currentDir)

    // 格式化显示路径
    private fun formatDisplayPath(state: ArchiveState?, dir: File): String = when {
        state != null && state.internalPath.isNotEmpty() ->
            state.archiveFile.name + "/" + state.internalPath + "/"
        state != null ->
            state.archiveFile.name + "/"
        else ->
            dir.absolutePath.trimEnd('/') + "/"
    }

    private val canUp: Boolean
        get() = archiveState != null || currentDir.parentFile != null

    // 初始化
    fun init(adapter: FileListAdapter) {
        this.adapter = adapter
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.itemAnimator = null
        val spacing = dpToPx(activity, 80)
        recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: android.graphics.Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                val pos = parent.getChildAdapterPosition(view)
                val total = parent.adapter?.itemCount ?: 0
                if (pos == total - 1) {
                    outRect.bottom = spacing
                }
            }
        })
    }

    // 当前滚动键
    internal fun currentScrollKey(): String {
        val state = archiveState
        return if (state != null) {
            "${state.archiveFile.absolutePath}::${state.internalPath}"
        } else {
            currentDir.absolutePath
        }
    }

    // 标记脏目录
    fun markDirty(path: String) {
        dirtyDirs.add(path)
        dirCache.remove(path)
    }

    // 是否可向上导航
    fun canNavigateUp(): Boolean = canUp
}

package zhu.filer

import android.animation.ValueAnimator
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import java.io.File
import com.google.android.material.R as materialR

// 文件项数据类
data class FileItem(
    val file: File,
    val displayName: String,
    val iconRes: Int,
    val subtitle: String,
    val isDirectory: Boolean = file.isDirectory,
    val encrypted: Boolean = false,
    val entryPath: String? = null,
    val size: Long = file.length()
)

// 文件列表适配器
class FileListAdapter(
    private val onItemClick: (File, Int, View) -> Unit,
    private val onItemLongClick: (File, Int) -> Boolean
) : RecyclerView.Adapter<FileListAdapter.ViewHolder>() {

    private val items = mutableListOf<FileItem>()
    private var highlightPosition: Int = -1
    private var blinkPosition: Int = -1
    private var dataVersion = 0
    private val itemVersionMap = mutableMapOf<String, Int>()
    private var firstVisiblePosition = 0

    private val selectedPositions = mutableSetOf<Int>()
    private var selectedColor: Int = 0
    private var defaultTextColor: Int = 0

    private val viewBuilder = FileItemViewBuilder(
        isSelected = { isSelected(it) },
        highlightPosition = { highlightPosition },
        blinkPosition = { blinkPosition },
        selectedColor = { selectedColor },
        ensureColorsInitialized = { context ->
            if (selectedColor == 0) {
                selectedColor = getThemeColor(context, materialR.attr.colorPrimaryContainer)
            }
            if (defaultTextColor == 0) {
                defaultTextColor = getThemeColor(context, materialR.attr.colorOnSurface)
            }
        }
    )

    private val itemBinder = FileItemBinder(
        isSelected = { isSelected(it) },
        highlightPosition = { highlightPosition },
        blinkPosition = { blinkPosition },
        clearBlinkIfMatches = { pos -> if (blinkPosition == pos) blinkPosition = -1 },
        clearHighlightIfMatches = { pos -> if (highlightPosition == pos) highlightPosition = -1 },
        selectedColor = { selectedColor },
        defaultTextColor = { defaultTextColor },
        shouldAnimateItem = { key ->
            val version = itemVersionMap[key] ?: 0
            val should = version != dataVersion
            if (should) itemVersionMap[key] = dataVersion
            should
        },
        firstVisiblePosition = { firstVisiblePosition },
        notifyItemChanged = { notifyItemChanged(it) },
        onItemClick = onItemClick,
        onItemLongClick = onItemLongClick
    )

    // 是否选中
    fun isSelected(position: Int) = selectedPositions.contains(position)

    // 切换选择状态
    fun toggleSelection(position: Int) {
        if (position == 0 && isUpItem(position)) return
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position)
        } else {
            selectedPositions.add(position)
        }
        notifyItemChanged(position)
    }

    // 选择位置
    fun selectPosition(position: Int) {
        if (position == 0 && isUpItem(position)) return
        if (selectedPositions.add(position)) {
            notifyItemChanged(position)
        }
    }

    // 清除选择
    fun clearSelection() {
        val copy = selectedPositions.toSet()
        selectedPositions.clear()
        copy.forEach { notifyItemChanged(it) }
    }

    // 获取选中文件
    fun getSelectedFiles(): List<File> {
        return selectedPositions.mapNotNull { items.getOrNull(it)?.file }
            .filter { it.name != ".." }
    }

    // 是否有选择
    fun hasSelection() = selectedPositions.isNotEmpty()

    // 是否上级项
    fun isUpItem(position: Int): Boolean {
        return position == 0 && items.isNotEmpty() && items[0].file.name == ".."
    }

    // 提交列表
    fun submitList(new: List<FileItem>, highlightPos: Int = -1, firstVisible: Int = 0, animate: Boolean = true) {
        items.clear()
        items.addAll(new)
        highlightPosition = highlightPos
        blinkPosition = highlightPos
        if (animate) {
            dataVersion++
            itemVersionMap.clear()
        }
        firstVisiblePosition = if (firstVisible >= 0) firstVisible else 0
        clearSelection()
        notifyDataSetChanged()
    }

    // 创建视图
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return viewBuilder.build(parent)
    }

    // 绑定视图
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        itemBinder.bind(holder, items[position], position)
    }

    // 视图回收
    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        Glide.with(holder.iconIv.context).clear(holder.iconIv)
        holder.itemView.setTag(FileItemBinder.VIEW_TAG_ID, false)
        holder.itemView.animate().cancel()
        holder.itemView.alpha = 1f
        (holder.container.getTag(R.id.file_item_container) as? ValueAnimator)?.cancel()
        (holder.nameTv.getTag(R.id.file_item_container) as? ValueAnimator)?.cancel()
        holder.container.setBackgroundColor(Color.TRANSPARENT)
        holder.iconIv.clearColorFilter()
        holder.nameTv.setTextColor(defaultTextColor)
        holder.thumbnailLoaded = false
    }

    // 获取项数
    override fun getItemCount(): Int = items.size

    // 视图持有者
    class ViewHolder(
        itemView: View,
        val container: LinearLayout,
        val cardView: MaterialCardView,
        val iconIv: ImageView,
        val nameTv: TextView,
        val subtitleTv: TextView,
        val encryptedTv: TextView
    ) : RecyclerView.ViewHolder(itemView) {
        var thumbnailLoaded: Boolean = false
    }

    // 获取文件项
    fun getFileItem(position: Int): FileItem? = items.getOrNull(position)
}

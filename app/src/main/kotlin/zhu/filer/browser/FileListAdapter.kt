package zhu.filer.browser

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
import zhu.filer.FileItem
import zhu.filer.R
import zhu.filer.ui.getThemeColor

// 文件列表适配器
class FileListAdapter(
    private val onItemClick: (File, Int, View) -> Unit,
    private val onItemLongClick: (File, Int) -> Boolean
) : RecyclerView.Adapter<FileListAdapter.ViewHolder>() {

    internal val items = mutableListOf<FileItem>()
    internal var highlightPosition: Int = -1
    internal var blinkPosition: Int = -1
    internal var dataVersion = 0
    internal val itemVersionMap = mutableMapOf<String, Int>()
    internal var firstVisiblePosition = 0

    internal val selectedPositions = mutableSetOf<Int>()
    internal var selectedColor: Int = 0
    internal var defaultTextColor: Int = 0

    internal val viewBuilder = FileItemViewBuilder(
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

    internal val itemBinder = FileItemBinder(
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
        holder.iconIv.setTag(R.id.tag_audio_art, null)
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

}

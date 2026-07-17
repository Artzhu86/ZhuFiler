package zhu.filer.browser

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.view.View
import android.widget.TextView
import java.io.File
import zhu.filer.FileItem
import zhu.filer.R
import zhu.filer.ui.getThemeColor

// 文件项绑定器
class FileItemBinder(
    internal val isSelected: (Int) -> Boolean,
    internal val highlightPosition: () -> Int,
    internal val blinkPosition: () -> Int,
    internal val clearBlinkIfMatches: (Int) -> Unit,
    internal val clearHighlightIfMatches: (Int) -> Unit,
    internal val selectedColor: () -> Int,
    internal val defaultTextColor: () -> Int,
    internal val shouldAnimateItem: (String) -> Boolean,
    internal val firstVisiblePosition: () -> Int,
    internal val notifyItemChanged: (Int) -> Unit,
    internal val onItemClick: (File, Int, View) -> Unit,
    internal val onItemLongClick: (File, Int) -> Boolean
) {

    // 伴生对象
    companion object {
        internal const val FADE_DURATION = 125L
        internal const val BLINK_FADE_DURATION = 125L
        internal const val ITEM_ANIMATION_DELAY_MS = 10L
        val VIEW_TAG_ID = R.id.tag_view
    }

    // 绑定数据
    fun bind(holder: FileListAdapter.ViewHolder, item: FileItem, position: Int) {
        holder.itemView.tag = position
        holder.cardView.tag = position
        setupThumbnail(holder, item, position)
        holder.nameTv.text = item.displayName
        holder.subtitleTv.text = item.subtitle
        holder.encryptedTv.visibility = if (item.encrypted) View.VISIBLE else View.GONE
        applyVisualState(holder, item, position)
        setupClickListeners(holder, item, position)
    }

    // 背景色动画
    internal fun animateBackgroundColor(view: View, target: Int) {
        (view.getTag(R.id.file_item_container) as? ValueAnimator)?.cancel()
        val from = if (view.background is android.graphics.drawable.ColorDrawable)
            (view.background as android.graphics.drawable.ColorDrawable).color else Color.TRANSPARENT
        if (from == target) {
            view.setBackgroundColor(target)
            return
        }
        ValueAnimator.ofObject(ArgbEvaluator(), from, target).apply {
            duration = 125
            addUpdateListener { view.setBackgroundColor(it.animatedValue as Int) }
            view.setTag(R.id.file_item_container, this)
            start()
        }
    }

    // 文字颜色动画
    internal fun animateTextColor(tv: TextView, target: Int) {
        (tv.getTag(R.id.file_item_container) as? ValueAnimator)?.cancel()
        val from = tv.currentTextColor
        if (from == target) {
            tv.setTextColor(target)
            return
        }
        ValueAnimator.ofObject(ArgbEvaluator(), from, target).apply {
            duration = 125
            addUpdateListener { tv.setTextColor(it.animatedValue as Int) }
            tv.setTag(R.id.file_item_container, this)
            start()
        }
    }

    // 设置点击监听
    internal fun setupClickListeners(holder: FileListAdapter.ViewHolder, item: FileItem, position: Int) {
        holder.cardView.setOnClickListener {
            onItemClick(item.file, position, holder.itemView)
        }
        holder.cardView.setOnLongClickListener {
            holder.container.getTag(R.id.file_item_container)?.let { anim ->
                (anim as? ValueAnimator)?.cancel()
            }
            val bg = when {
                position == highlightPosition() || position == blinkPosition() ->
                    getThemeColor(holder.itemView.context, android.R.attr.colorControlHighlight)
                isSelected(position) -> selectedColor()
                else -> Color.TRANSPARENT
            }
            holder.container.setBackgroundColor(bg)
            onItemLongClick(item.file, position)
        }
    }

    // 开始闪烁淡出
    internal fun startBlinkFade(view: View, onEnd: () -> Unit) {
        val highlightColor = getThemeColor(view.context, android.R.attr.colorControlHighlight)
        val transparent = Color.TRANSPARENT
        val anim = ValueAnimator.ofObject(ArgbEvaluator(), highlightColor, transparent)
        anim.duration = FileItemBinder.BLINK_FADE_DURATION
        anim.addUpdateListener {
            view.setBackgroundColor(it.animatedValue as Int)
        }
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onEnd()
            }
            override fun onAnimationCancel(animation: Animator) {
                onEnd()
            }
        })
        anim.start()
    }
}

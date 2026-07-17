package zhu.filer.browser

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import zhu.filer.FileItem
import zhu.filer.FileType
import zhu.filer.R
import zhu.filer.ui.getThemeColor

// 应用视觉状态
internal fun FileItemBinder.applyVisualState(holder: FileListAdapter.ViewHolder, item: FileItem, position: Int) {
    val isHighlight = position == highlightPosition() || position == blinkPosition()
    val isSel = isSelected(position)
    val bgColor = when {
        isHighlight -> getThemeColor(holder.container.context, android.R.attr.colorControlHighlight)
        isSel -> selectedColor()
        else -> Color.TRANSPARENT
    }
    animateBackgroundColor(holder.container, bgColor)
    val primaryColor = getThemeColor(holder.itemView.context, android.R.attr.colorPrimary)
    val targetTextColor = if (isSel) primaryColor else defaultTextColor()
    animateTextColor(holder.nameTv, targetTextColor)
    val hasThumbnail = item.entryPath == null && !item.isDirectory &&
        (FileType.isImage(item.file) || FileType.isVideo(item.file) ||
            FileType.isAudio(item.file) || FileType.isApk(item.file))
    if (!hasThumbnail || !holder.thumbnailLoaded) {
        if (isSel) {
            holder.iconIv.setColorFilter(primaryColor)
        } else {
            holder.iconIv.clearColorFilter()
        }
    }
    val key = item.file.absolutePath
    val shouldAnimate = shouldAnimateItem(key)
    if (shouldAnimate) {
        val relativePos = (position - firstVisiblePosition()).coerceAtLeast(0)
        val startDelay = (relativePos * FileItemBinder.ITEM_ANIMATION_DELAY_MS).toLong()
        holder.itemView.animate().cancel()
        holder.itemView.alpha = 0f
        holder.itemView.animate()
            .alpha(1f)
            .setDuration(FileItemBinder.FADE_DURATION)
            .setStartDelay(startDelay)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    holder.itemView.alpha = 1f
                    if (isHighlight) {
                        startBlinkOutAnimation(holder, position)
                    }
                }
                override fun onAnimationCancel(animation: Animator) {
                    holder.itemView.alpha = 1f
                }
            })
            .start()
    } else {
        handleNonAnimatedHighlight(holder, position, isHighlight)
    }
}

// 开始闪烁淡出动画
private fun FileItemBinder.startBlinkOutAnimation(holder: FileListAdapter.ViewHolder, position: Int) {
    val bgAnim = ValueAnimator.ofObject(
        ArgbEvaluator(),
        getThemeColor(holder.itemView.context, android.R.attr.colorControlHighlight),
        Color.TRANSPARENT
    )
    bgAnim.duration = FileItemBinder.BLINK_FADE_DURATION
    bgAnim.addUpdateListener {
        holder.container.setBackgroundColor(it.animatedValue as Int)
    }
    bgAnim.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            holder.container.setBackgroundColor(Color.TRANSPARENT)
            clearBlinkIfMatches(position)
            clearHighlightIfMatches(position)
        }
    })
    bgAnim.start()
}

// 处理非动画高亮
private fun FileItemBinder.handleNonAnimatedHighlight(holder: FileListAdapter.ViewHolder, position: Int, isHighlight: Boolean) {
    if (isHighlight) {
        val animStarted = holder.itemView.getTag(FileItemBinder.VIEW_TAG_ID) == true
        if (!animStarted) {
            holder.itemView.setTag(FileItemBinder.VIEW_TAG_ID, true)
            holder.container.setBackgroundColor(
                getThemeColor(holder.itemView.context, android.R.attr.colorControlHighlight)
            )
            startBlinkFade(holder.container) {
                holder.itemView.setTag(FileItemBinder.VIEW_TAG_ID, false)
                clearBlinkIfMatches(position)
                clearHighlightIfMatches(position)
                holder.container.setBackgroundColor(Color.TRANSPARENT)
                notifyItemChanged(position)
            }
        }
    } else {
        if (holder.itemView.getTag(FileItemBinder.VIEW_TAG_ID) == true) {
            holder.itemView.setTag(FileItemBinder.VIEW_TAG_ID, false)
        }
    }
}

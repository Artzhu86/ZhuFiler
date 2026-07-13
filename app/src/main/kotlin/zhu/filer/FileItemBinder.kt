package zhu.filer

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import java.io.File

// 文件项绑定器
class FileItemBinder(
    private val isSelected: (Int) -> Boolean,
    private val highlightPosition: () -> Int,
    private val blinkPosition: () -> Int,
    private val clearBlinkIfMatches: (Int) -> Unit,
    private val clearHighlightIfMatches: (Int) -> Unit,
    private val selectedColor: () -> Int,
    private val defaultTextColor: () -> Int,
    private val shouldAnimateItem: (String) -> Boolean,
    private val firstVisiblePosition: () -> Int,
    private val notifyItemChanged: (Int) -> Unit,
    private val onItemClick: (File, Int, View) -> Unit,
    private val onItemLongClick: (File, Int) -> Boolean
) {

    // 伴生对象
    companion object {
        private const val FADE_DURATION = 125L
        private const val BLINK_FADE_DURATION = 125L
        private const val ITEM_ANIMATION_DELAY_MS = 10L
        val VIEW_TAG_ID = R.id.tag_view
    }

    // 绑定数据
    fun bind(holder: FileListAdapter.ViewHolder, item: FileItem, position: Int) {
        holder.itemView.tag = position
        holder.cardView.tag = position

        val isImage = item.entryPath == null && !item.isDirectory && FileType.isImage(item.file)
        val isVideo = item.entryPath == null && !item.isDirectory && FileType.isVideo(item.file)
        val hasThumbnail = isImage || isVideo
        if (hasThumbnail) {
            val placeholderRes = if (isImage) R.drawable.outline_image_24 else R.drawable.outline_video_file_24
            val iconPx = dpToPx(holder.iconIv.context, FileItemViewBuilder.ICON_SIZE_DP)
            holder.iconIv.layoutParams = FrameLayout.LayoutParams(iconPx, iconPx).apply {
                gravity = Gravity.CENTER
            }
            holder.iconIv.scaleType = ImageView.ScaleType.FIT_CENTER
            holder.iconIv.setImageResource(placeholderRes)

            holder.thumbnailLoaded = false

            val request = Glide.with(holder.iconIv.context).load(item.file)
            if (isVideo) request.frame(1000L)
            request
                .centerCrop()
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        val iconPx = dpToPx(holder.iconIv.context, FileItemViewBuilder.ICON_SIZE_DP)
                        holder.iconIv.layoutParams = FrameLayout.LayoutParams(iconPx, iconPx).apply {
                            gravity = Gravity.CENTER
                        }
                        holder.iconIv.scaleType = ImageView.ScaleType.FIT_CENTER
                        holder.iconIv.setImageResource(placeholderRes)
                        holder.thumbnailLoaded = false
                        applyIconColor(holder, isSelected(position))
                        return true
                    }

                    // 资源就绪
                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        holder.iconIv.layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        holder.iconIv.scaleType = ImageView.ScaleType.CENTER_CROP
                        holder.thumbnailLoaded = true
                        holder.iconIv.clearColorFilter()
                        return false
                    }
                })
                .into(holder.iconIv)
        } else {
            val iconPx = dpToPx(holder.iconIv.context, FileItemViewBuilder.ICON_SIZE_DP)
            holder.iconIv.layoutParams = FrameLayout.LayoutParams(iconPx, iconPx).apply {
                gravity = Gravity.CENTER
            }
            holder.iconIv.scaleType = ImageView.ScaleType.FIT_CENTER
            Glide.with(holder.iconIv.context).clear(holder.iconIv)
            holder.iconIv.setImageResource(item.iconRes)
        }

        holder.nameTv.text = item.displayName
        holder.subtitleTv.text = item.subtitle
        holder.encryptedTv.visibility = if (item.encrypted) View.VISIBLE else View.GONE

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
            val startDelay = (relativePos * ITEM_ANIMATION_DELAY_MS).toLong()

            holder.itemView.animate().cancel()
            holder.itemView.alpha = 0f
            holder.itemView.animate()
                .alpha(1f)
                .setDuration(FADE_DURATION)
                .setStartDelay(startDelay)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        holder.itemView.alpha = 1f
                        if (isHighlight) {
                            val bgAnim = ValueAnimator.ofObject(
                                ArgbEvaluator(),
                                getThemeColor(holder.itemView.context, android.R.attr.colorControlHighlight),
                                Color.TRANSPARENT
                            )
                            bgAnim.duration = BLINK_FADE_DURATION
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
                    }
                    // 动画取消
                    override fun onAnimationCancel(animation: Animator) {
                        holder.itemView.alpha = 1f
                    }
                })
                .start()
        } else {
            if (isHighlight) {
                val animStarted = holder.itemView.getTag(VIEW_TAG_ID) == true
                if (!animStarted) {
                    holder.itemView.setTag(VIEW_TAG_ID, true)
                    holder.container.setBackgroundColor(
                        getThemeColor(holder.itemView.context, android.R.attr.colorControlHighlight)
                    )
                    startBlinkFade(holder.container) {
                        holder.itemView.setTag(VIEW_TAG_ID, false)
                        clearBlinkIfMatches(position)
                        clearHighlightIfMatches(position)
                        holder.container.setBackgroundColor(Color.TRANSPARENT)
                        notifyItemChanged(position)
                    }
                }
            } else {
                if (holder.itemView.getTag(VIEW_TAG_ID) == true) {
                    holder.itemView.setTag(VIEW_TAG_ID, false)
                }
            }
        }

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

    // 背景色动画
    private fun animateBackgroundColor(view: View, target: Int) {
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
    private fun animateTextColor(tv: TextView, target: Int) {
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

    // 开始闪烁淡出
    private fun startBlinkFade(view: View, onEnd: () -> Unit) {
        val highlightColor = getThemeColor(view.context, android.R.attr.colorControlHighlight)
        val transparent = Color.TRANSPARENT
        val anim = ValueAnimator.ofObject(ArgbEvaluator(), highlightColor, transparent)
        anim.duration = BLINK_FADE_DURATION
        anim.addUpdateListener {
            view.setBackgroundColor(it.animatedValue as Int)
        }
        anim.addListener(object : AnimatorListenerAdapter() {
            // 动画结束
            override fun onAnimationEnd(animation: Animator) {
                onEnd()
            }
            // 动画取消
            override fun onAnimationCancel(animation: Animator) {
                onEnd()
            }
        })
        anim.start()
    }

    // 应用图标颜色
    private fun applyIconColor(holder: FileListAdapter.ViewHolder, selected: Boolean) {
        val primaryColor = getThemeColor(holder.iconIv.context, android.R.attr.colorPrimary)
        if (selected) {
            holder.iconIv.setColorFilter(primaryColor)
        } else {
            holder.iconIv.clearColorFilter()
        }
    }
}

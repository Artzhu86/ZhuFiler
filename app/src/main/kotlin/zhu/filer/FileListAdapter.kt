package zhu.filer

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

data class FileItem(val file: File, val displayName: String, val iconRes: Int, val subtitle: String)

class FileListAdapter(
    private val onItemClick: (File, Int) -> Unit,
    private val onItemLongClick: (File, Int) -> Boolean
) : RecyclerView.Adapter<FileListAdapter.ViewHolder>() {

    private val items = mutableListOf<FileItem>()
    private var highlightPosition: Int = -1
    private var blinkPosition: Int = -1
    private var dataVersion = 0
    private val itemVersionMap = mutableMapOf<String, Int>()
    private var lastSubmitTime = 0L
    private var isAddItemAllowedToAnimate = false
    private var firstVisiblePosition = 0

    private companion object {
        private const val BLINK_TAG = 0x7f0a0001
        private const val ANIM_WINDOW_MS = 300L
        private const val FADE_DURATION = 125L
        private const val BLINK_FADE_DURATION = 125L
        private const val ITEM_ANIMATION_DELAY_MS = 15L
    }

    fun submitList(new: List<FileItem>, firstVisiblePos: Int = 0) {
        items.clear()
        items.addAll(new)
        highlightPosition = -1
        blinkPosition = -1
        dataVersion++
        itemVersionMap.clear()
        lastSubmitTime = System.currentTimeMillis()
        isAddItemAllowedToAnimate = false
        firstVisiblePosition = firstVisiblePos
        notifyDataSetChanged()
    }

    fun addItem(item: FileItem) {
        items.add(item)
        isAddItemAllowedToAnimate = true
        notifyItemInserted(items.size - 1)
    }

    fun setHighlight(position: Int) {
        val old = highlightPosition
        if (old != position) {
            highlightPosition = position
            if (position >= 0 && position < itemCount) {
                blinkPosition = position
                notifyItemChanged(position)
            }
            if (old >= 0 && old < itemCount) {
                if (old == blinkPosition) blinkPosition = -1
                notifyItemChanged(old)
            }
        }
    }

    fun clearHighlight() {
        if (highlightPosition >= 0) {
            val old = highlightPosition
            highlightPosition = -1
            if (old < itemCount) {
                if (old == blinkPosition) blinkPosition = -1
                notifyItemChanged(old)
            }
        }
    }

    fun startBlink(position: Int) {
        val old = blinkPosition
        if (old != position) {
            blinkPosition = position
            if (old >= 0 && old < itemCount) notifyItemChanged(old)
            if (position >= 0 && position < itemCount) notifyItemChanged(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context
        val highlightColor = getThemeColor(context, android.R.attr.colorControlHighlight)

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            minimumHeight = dpToPx(context, 56)
            setPadding(dpToPx(context, 16), dpToPx(context, 8), dpToPx(context, 16), dpToPx(context, 8))
            isClickable = true
            isFocusable = true
            background = null
            setBackgroundColor(Color.TRANSPARENT)
            val ripple = RippleDrawable(
                ColorStateList.valueOf(highlightColor),
                null,
                null
            )
            foreground = ripple
            setOnTouchListener { view, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        view.setBackgroundColor(highlightColor)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        view.setBackgroundColor(Color.TRANSPARENT)
                    }
                }
                false
            }
        }

        val iconIv = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(context, 32), dpToPx(context, 32)).apply {
                gravity = Gravity.CENTER_VERTICAL
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        container.addView(iconIv)

        val nameContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dpToPx(context, 12)
                gravity = Gravity.CENTER_VERTICAL
            }
        }

        val nameTv = TextView(context).apply {
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        nameContainer.addView(nameTv)

        val subtitleTv = TextView(context).apply {
            textSize = 11f
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        nameContainer.addView(subtitleTv)

        container.addView(nameContainer)

        return ViewHolder(container, iconIv, nameTv, subtitleTv)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.iconIv.setImageResource(item.iconRes)
        holder.nameTv.text = item.displayName
        holder.subtitleTv.text = item.subtitle

        holder.itemView.setBackgroundColor(Color.TRANSPARENT)

        val key = item.file.absolutePath
        val version = itemVersionMap[key] ?: 0
        val isWithinWindow = System.currentTimeMillis() - lastSubmitTime < ANIM_WINDOW_MS
        val shouldAnimate = version != dataVersion && (isWithinWindow || isAddItemAllowedToAnimate)

        val isHighlight = position == highlightPosition || position == blinkPosition
        val highlightColor = getThemeColor(holder.itemView.context, android.R.attr.colorControlHighlight)

        if (shouldAnimate) {
            itemVersionMap[key] = dataVersion
            val relativePos = if (position >= firstVisiblePosition) position - firstVisiblePosition else 0
            val startDelay = (relativePos * ITEM_ANIMATION_DELAY_MS).toLong()

            holder.itemView.animate().cancel()

            if (isHighlight) {
                holder.itemView.setBackgroundColor(highlightColor)
                holder.itemView.alpha = 0f
                holder.itemView.animate()
                    .alpha(1f)
                    .setDuration(FADE_DURATION)
                    .setStartDelay(startDelay)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            holder.itemView.alpha = 1f
                            val bgAnim = ValueAnimator.ofObject(
                                ArgbEvaluator(),
                                highlightColor,
                                Color.TRANSPARENT
                            )
                            bgAnim.duration = BLINK_FADE_DURATION
                            bgAnim.addUpdateListener {
                                holder.itemView.setBackgroundColor(it.animatedValue as Int)
                            }
                            bgAnim.addListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    holder.itemView.setBackgroundColor(Color.TRANSPARENT)
                                    if (blinkPosition == position) blinkPosition = -1
                                    if (highlightPosition == position) highlightPosition = -1
                                }
                            })
                            bgAnim.start()
                        }
                        override fun onAnimationCancel(animation: Animator) {
                            holder.itemView.alpha = 1f
                        }
                    })
                    .start()
            } else {
                holder.itemView.alpha = 0f
                holder.itemView.animate()
                    .alpha(1f)
                    .setDuration(FADE_DURATION)
                    .setStartDelay(startDelay)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            holder.itemView.alpha = 1f
                        }
                        override fun onAnimationCancel(animation: Animator) {
                            holder.itemView.alpha = 1f
                        }
                    })
                    .start()
            }
        } else {
            if (isHighlight) {
                val animStarted = holder.itemView.getTag(BLINK_TAG) == true
                if (!animStarted) {
                    holder.itemView.setTag(BLINK_TAG, true)
                    holder.itemView.setBackgroundColor(highlightColor)
                    startBlinkFade(holder.itemView) {
                        holder.itemView.setTag(BLINK_TAG, false)
                        if (blinkPosition == position) blinkPosition = -1
                        if (highlightPosition == position) highlightPosition = -1
                        holder.itemView.setBackgroundColor(Color.TRANSPARENT)
                        notifyItemChanged(position)
                    }
                }
            } else {
                if (holder.itemView.getTag(BLINK_TAG) == true) {
                    holder.itemView.setTag(BLINK_TAG, false)
                }
            }
        }

        holder.itemView.setOnClickListener {
            onItemClick(item.file, position)
        }

        holder.itemView.setOnLongClickListener { onItemLongClick(item.file, position) }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.itemView.setTag(BLINK_TAG, false)
        holder.itemView.animate().cancel()
        holder.itemView.alpha = 1f
        holder.itemView.setBackgroundColor(Color.TRANSPARENT)
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(
        itemView: View,
        val iconIv: ImageView,
        val nameTv: TextView,
        val subtitleTv: TextView
    ) : RecyclerView.ViewHolder(itemView)

    private fun startBlinkFade(view: View, onEnd: () -> Unit) {
        val highlightColor = getThemeColor(view.context, android.R.attr.colorControlHighlight)
        val transparent = Color.TRANSPARENT
        val anim = ValueAnimator.ofObject(ArgbEvaluator(), highlightColor, transparent)
        anim.duration = BLINK_FADE_DURATION
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
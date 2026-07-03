package zhu.filer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.recyclerview.widget.RecyclerView

class FastScroller @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var boundRecyclerView: RecyclerView? = null

    private val trackWidthPx = dpToPx(context, 8)
    private val thumbWidthPx = dpToPx(context, 8)
    private val thumbMinHeightPx = dpToPx(context, 52)

    private val autoHideDelayMs = 1500L
    private val showDurationMs = 150L
    private val hideDurationMs = 200L

    private var isShowing = false

    private var scrollRange = 0
    private var scrollExtent = 0
    private var scrollOffset = 0

    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumbRect = RectF()
    private val trackRect = RectF()
    private val path = Path()
    private val cornerRadius = dpToPx(context, 4).toFloat()

    private val handler = Handler(Looper.getMainLooper())
    private val autoHideRunnable = Runnable { hide() }

    init {
        thumbPaint.color = getThemeColor(
            context, com.google.android.material.R.attr.colorPrimary
        )
        trackPaint.color = getThemeColor(
            context, com.google.android.material.R.attr.colorPrimaryContainer
        )
        alpha = 0f
        translationX = trackWidthPx.toFloat()
    }

    fun attach(recyclerView: RecyclerView) {
        boundRecyclerView = recyclerView
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                updateThumb()
                if (dx != 0 || dy != 0) {
                    onScrollActive()
                }
            }
        })
        recyclerView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateThumb() }
        recyclerView.adapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() = updateThumb()
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = updateThumb()
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = updateThumb()
        })
        updateThumb()
    }

    private fun onScrollActive() {
        if (!isShowing && scrollRange - scrollExtent > 0) {
            show()
        }
        handler.removeCallbacks(autoHideRunnable)
        handler.postDelayed(autoHideRunnable, autoHideDelayMs)
    }

    private fun show() {
        isShowing = true
        handler.removeCallbacks(autoHideRunnable)
        animate().cancel()
        animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(showDurationMs)
            .setInterpolator(LinearOutSlowInInterpolator())
            .start()
    }

    private fun hide() {
        isShowing = false
        animate().cancel()
        animate()
            .alpha(0f)
            .translationX(trackWidthPx.toFloat())
            .setDuration(hideDurationMs)
            .setInterpolator(FastOutLinearInInterpolator())
            .start()
    }

    private fun updateThumb() {
        val rv = boundRecyclerView ?: return
        scrollRange = rv.computeVerticalScrollRange()
        scrollExtent = rv.computeVerticalScrollExtent()
        scrollOffset = rv.computeVerticalScrollOffset()
        visibility = if (scrollRange - scrollExtent > 0) VISIBLE else GONE
        invalidate()
    }

    private fun thumbHeight(): Float {
        val h = height.toFloat()
        if (scrollRange <= 0) return thumbMinHeightPx.toFloat()
        val ratio = scrollExtent.toFloat() / scrollRange.toFloat()
        return (h * ratio).coerceIn(thumbMinHeightPx.toFloat(), h)
    }

    private fun thumbTop(): Float {
        val h = height.toFloat()
        val range = (scrollRange - scrollExtent).coerceAtLeast(0)
        if (range <= 0) return 0f
        val maxTop = (h - thumbHeight()).coerceAtLeast(1f)
        return (scrollOffset.toFloat() / range.toFloat()) * maxTop
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (scrollRange - scrollExtent <= 0) return
        val w = width.toFloat()
        val h = height.toFloat()
        val thumbW = thumbWidthPx.toFloat()
        val cx = w - thumbW / 2f

        val trackLeft = cx - trackWidthPx / 2f
        trackRect.set(trackLeft, 0f, trackLeft + trackWidthPx, h)
        path.reset()
        path.addRoundRect(
            trackRect,
            floatArrayOf(cornerRadius, cornerRadius, 0f, 0f, 0f, 0f, cornerRadius, cornerRadius),
            Path.Direction.CW
        )
        canvas.drawPath(path, trackPaint)

        val top = thumbTop()
        val thumbH = thumbHeight()
        thumbRect.set(cx - thumbW / 2f, top, cx + thumbW / 2f, top + thumbH)
        path.reset()
        path.addRoundRect(
            thumbRect,
            floatArrayOf(cornerRadius, cornerRadius, 0f, 0f, 0f, 0f, cornerRadius, cornerRadius),
            Path.Direction.CW
        )
        canvas.drawPath(path, thumbPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val rv = boundRecyclerView ?: return false
        if (visibility != VISIBLE) return false
        val range = (scrollRange - scrollExtent).coerceAtLeast(0)
        if (range <= 0) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                show()
                parent?.requestDisallowInterceptTouchEvent(true)
                scrollTo(event.y, rv, range)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                scrollTo(event.y, rv, range)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                invalidate()
                handler.postDelayed(autoHideRunnable, autoHideDelayMs)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun scrollTo(touchY: Float, rv: RecyclerView, range: Int) {
        val h = height.toFloat()
        val thumbH = thumbHeight()
        val maxTop = (h - thumbH).coerceAtLeast(1f)
        val fraction = ((touchY - thumbH / 2f) / maxTop).coerceIn(0f, 1f)
        val targetOffset = (fraction * range).toInt()
        val current = rv.computeVerticalScrollOffset()
        val dy = targetOffset - current
        if (dy != 0) rv.scrollBy(0, dy)
    }
}

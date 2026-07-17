package zhu.filer.browser

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
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.R as materialR
import zhu.filer.ui.dpToPx
import zhu.filer.ui.getThemeColor

// 快速滚动条视图
class FastScroller @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    internal var boundRecyclerView: RecyclerView? = null
    internal val trackWidthPx = dpToPx(context, 10)
    internal val thumbWidthPx = dpToPx(context, 10)
    internal val thumbMinHeightPx = dpToPx(context, 52)
    internal val autoHideDelayMs = 1500L
    internal val showDurationMs = 150L
    internal val hideDurationMs = 200L
    internal var isShowing = false
    internal var isDragging = false
    internal var scrollRange = 0
    internal var scrollExtent = 0
    internal var scrollOffset = 0
    internal val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    internal val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    internal val thumbRect = RectF()
    internal val trackRect = RectF()
    internal val path = Path()
    internal val cornerRadius = dpToPx(context, 5).toFloat()
    internal val handler = Handler(Looper.getMainLooper())
    internal val autoHideRunnable = Runnable { hide() }

    // 初始化
    init {
        thumbPaint.color = getThemeColor(context, android.R.attr.colorPrimary)
        trackPaint.color = getThemeColor(context, materialR.attr.colorSurfaceVariant)
        alpha = 0f
        translationX = trackWidthPx.toFloat()
    }

    // 绑定RecyclerView
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

    // 绘制滚动条
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (scrollRange - scrollExtent <= 0) return
        val w = width.toFloat()
        val h = height.toFloat()
        val thumbW = thumbWidthPx.toFloat()
        val cx = w - thumbW / 2f
        val trackLeft = cx - trackWidthPx / 2f
        trackRect.set(trackLeft, 0f, trackLeft + trackWidthPx, h)
        if (isDragging) {
            path.reset()
            path.addRoundRect(trackRect, floatArrayOf(cornerRadius, cornerRadius, 0f, 0f, 0f, 0f, cornerRadius, cornerRadius), Path.Direction.CW)
            canvas.drawPath(path, trackPaint)
        }
        val top = thumbTop()
        val thumbH = thumbHeight()
        thumbRect.set(cx - thumbW / 2f, top, cx + thumbW / 2f, top + thumbH)
        path.reset()
        path.addRoundRect(thumbRect, floatArrayOf(cornerRadius, cornerRadius, 0f, 0f, 0f, 0f, cornerRadius, cornerRadius), Path.Direction.CW)
        canvas.drawPath(path, thumbPaint)
    }

    // 处理触摸事件
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val rv = boundRecyclerView ?: return false
        if (visibility != VISIBLE) return false
        val range = (scrollRange - scrollExtent).coerceAtLeast(0)
        if (range <= 0) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = true
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
                isDragging = false
                invalidate()
                handler.postDelayed(autoHideRunnable, autoHideDelayMs)
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}

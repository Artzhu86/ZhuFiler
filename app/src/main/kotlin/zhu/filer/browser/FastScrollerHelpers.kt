package zhu.filer.browser

import android.view.View
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.recyclerview.widget.RecyclerView

// 滚动激活处理
internal fun FastScroller.onScrollActive() {
    if (!isShowing && scrollRange - scrollExtent > 0) {
        show()
    }
    handler.removeCallbacks(autoHideRunnable)
    handler.postDelayed(autoHideRunnable, autoHideDelayMs)
}

// 显示滚动条
internal fun FastScroller.show() {
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

// 隐藏滚动条
internal fun FastScroller.hide() {
    isShowing = false
    animate().cancel()
    animate()
        .alpha(0f)
        .translationX(trackWidthPx.toFloat())
        .setDuration(hideDurationMs)
        .setInterpolator(FastOutLinearInInterpolator())
        .start()
}

// 更新滑块位置
internal fun FastScroller.updateThumb() {
    val rv = boundRecyclerView ?: return
    scrollRange = rv.computeVerticalScrollRange()
    scrollExtent = rv.computeVerticalScrollExtent()
    scrollOffset = rv.computeVerticalScrollOffset()
    visibility = if (scrollRange - scrollExtent > 0) View.VISIBLE else View.GONE
    invalidate()
}

// 计算滑块高度
internal fun FastScroller.thumbHeight(): Float {
    val h = height.toFloat()
    if (scrollRange <= 0) return thumbMinHeightPx.toFloat()
    val ratio = scrollExtent.toFloat() / scrollRange.toFloat()
    return (h * ratio).coerceIn(thumbMinHeightPx.toFloat(), h)
}

// 计算滑块顶部位置
internal fun FastScroller.thumbTop(): Float {
    val h = height.toFloat()
    val range = (scrollRange - scrollExtent).coerceAtLeast(0)
    if (range <= 0) return 0f
    val maxTop = (h - thumbHeight()).coerceAtLeast(1f)
    return (scrollOffset.toFloat() / range.toFloat()) * maxTop
}

// 滚动到指定位置
internal fun FastScroller.scrollTo(touchY: Float, rv: RecyclerView, range: Int) {
    val h = height.toFloat()
    val thumbH = thumbHeight()
    val maxTop = (h - thumbH).coerceAtLeast(1f)
    val fraction = ((touchY - thumbH / 2f) / maxTop).coerceIn(0f, 1f)
    val targetOffset = (fraction * range).toInt()
    val current = rv.computeVerticalScrollOffset()
    val dy = targetOffset - current
    if (dy != 0) rv.scrollBy(0, dy)
}

package zhu.filer.browser

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.PorterDuff
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.R as materialR
import zhu.filer.ui.getThemeColor

// 工具栏滚动控制器
class ToolbarScrollerController(
    private val toolbar: MaterialToolbar,
    private val recyclerView: RecyclerView,
    private val activity: AppCompatActivity
) {

    private var toolbarAlphaThreshold: Int = 0
    private var toolbarColorAnimator: ValueAnimator? = null
    private val containerColor: Int by lazy { getThemeColor(activity, materialR.attr.colorPrimaryContainer) }
    private val onSurfaceColor: Int by lazy { getThemeColor(activity, materialR.attr.colorOnSurface) }
    private val primaryColor: Int by lazy { getThemeColor(activity, android.R.attr.colorPrimary) }
    private val argbEvaluator = ArgbEvaluator()

    // 根据透明度计算文字图标颜色
    private fun computeContentColor(alpha: Float): Int {
        return argbEvaluator.evaluate(alpha, onSurfaceColor, primaryColor) as Int
    }

    // 应用文字和图标颜色
    private fun applyContentColor(color: Int) {
        toolbar.setTitleTextColor(color)
        toolbar.setSubtitleTextColor(color)
        toolbar.navigationIcon?.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        toolbar.overflowIcon?.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        for (i in 0 until toolbar.childCount) {
            val child = toolbar.getChildAt(i)
            if (child is TextView && child.id != android.R.id.text1 && child.id != android.R.id.text2) {
                child.setTextColor(color)
            }
        }
    }

    // 设置滚动监听
    fun setupScrollListener() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (toolbarAlphaThreshold == 0) return
                val offset = recyclerView.computeVerticalScrollOffset()
                val alpha = (offset.toFloat() / toolbarAlphaThreshold).coerceIn(0f, 1f)
                val bgColor = Color.argb(
                    (alpha * 255).toInt(),
                    Color.red(containerColor),
                    Color.green(containerColor),
                    Color.blue(containerColor)
                )
                toolbar.setBackgroundColor(bgColor)
                applyContentColor(computeContentColor(alpha))
            }
        })
    }

    // 切换目录时动画过渡颜色
    fun animateToolbarColorOnDirSwitch() {
        if (toolbarAlphaThreshold == 0) return
        recyclerView.post {
            val offset = recyclerView.computeVerticalScrollOffset()
            val alpha = (offset.toFloat() / toolbarAlphaThreshold).coerceIn(0f, 1f)
            val targetBg = Color.argb(
                (alpha * 255).toInt(),
                Color.red(containerColor),
                Color.green(containerColor),
                Color.blue(containerColor)
            )
            val targetContent = computeContentColor(alpha)
            val currentBg = (toolbar.background as? android.graphics.drawable.ColorDrawable)?.color ?: Color.TRANSPARENT
            val currentContent = if (toolbar.childCount > 0) {
                (0 until toolbar.childCount).mapNotNull { toolbar.getChildAt(it) as? TextView }
                    .firstOrNull()?.currentTextColor ?: onSurfaceColor
            } else {
                onSurfaceColor
            }

            if (currentBg != targetBg || currentContent != targetContent) {
                toolbarColorAnimator?.cancel()
                toolbarColorAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 200
                    addUpdateListener {
                        val fraction = it.animatedFraction
                        val bg = argbEvaluator.evaluate(fraction, currentBg, targetBg) as Int
                        val content = argbEvaluator.evaluate(fraction, currentContent, targetContent) as Int
                        toolbar.setBackgroundColor(bg)
                        applyContentColor(content)
                    }
                    start()
                }
            }
        }
    }

    // 工具栏就绪时初始化
    fun onToolbarReady() {
        toolbarAlphaThreshold = toolbar.height
        toolbar.setBackgroundColor(Color.TRANSPARENT)
        applyContentColor(onSurfaceColor)
    }
}

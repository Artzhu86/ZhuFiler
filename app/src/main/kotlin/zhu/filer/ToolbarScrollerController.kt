package zhu.filer

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.R as materialR

// 工具栏滚动控制器
class ToolbarScrollerController(
    private val toolbar: MaterialToolbar,
    private val recyclerView: RecyclerView,
    private val activity: AppCompatActivity
) {

    private var toolbarAlphaThreshold: Int = 0
    private var toolbarColorAnimator: ValueAnimator? = null
    private val containerColor: Int by lazy { getThemeColor(activity, materialR.attr.colorPrimaryContainer) }

    // 设置滚动监听
    fun setupScrollListener() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (toolbarAlphaThreshold == 0) return
                val offset = recyclerView.computeVerticalScrollOffset()
                val alpha = (offset.toFloat() / toolbarAlphaThreshold).coerceIn(0f, 1f)
                val color = Color.argb(
                    (alpha * 255).toInt(),
                    Color.red(containerColor),
                    Color.green(containerColor),
                    Color.blue(containerColor)
                )
                toolbar.setBackgroundColor(color)
            }
        })
    }

    // 切换目录时动画过渡颜色
    fun animateToolbarColorOnDirSwitch() {
        if (toolbarAlphaThreshold == 0) return
        recyclerView.post {
            val offset = recyclerView.computeVerticalScrollOffset()
            val alpha = (offset.toFloat() / toolbarAlphaThreshold).coerceIn(0f, 1f)
            val targetColor = Color.argb(
                (alpha * 255).toInt(),
                Color.red(containerColor),
                Color.green(containerColor),
                Color.blue(containerColor)
            )
            val currentColor = (toolbar.background as? ColorDrawable)?.color ?: Color.TRANSPARENT
            if (currentColor != targetColor) {
                toolbarColorAnimator?.cancel()
                toolbarColorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), currentColor, targetColor).apply {
                    duration = 200
                    addUpdateListener { toolbar.setBackgroundColor(it.animatedValue as Int) }
                    start()
                }
            }
        }
    }

    // 工具栏就绪时初始化
    fun onToolbarReady() {
        toolbarAlphaThreshold = toolbar.height
        toolbar.setBackgroundColor(Color.TRANSPARENT)
    }
}

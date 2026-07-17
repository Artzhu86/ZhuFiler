package zhu.filer.browser

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import com.skydoves.transformationlayout.TransformationLayout
import zhu.filer.R
import zhu.filer.ui.getThemeColor

// 文件项视图构建器
class FileItemViewBuilder(
    private val isSelected: (Int) -> Boolean,
    private val highlightPosition: () -> Int,
    private val blinkPosition: () -> Int,
    private val selectedColor: () -> Int,
    private val ensureColorsInitialized: (Context) -> Unit
) {

    // 伴生对象
    companion object {
        const val ICON_SIZE_DP = 24
        const val THUMB_SIZE_DP = 45
    }

    // 构建视图
    fun build(parent: ViewGroup): FileListAdapter.ViewHolder {
        val context = parent.context
        ensureColorsInitialized(context)
        val pressColor = getThemeColor(context, android.R.attr.colorControlHighlight)

        val transformationLayout = LayoutInflater.from(context)
            .inflate(R.layout.item_file, parent, false) as TransformationLayout

        val card = transformationLayout.findViewById<MaterialCardView>(R.id.file_item_card)
        val container = transformationLayout.findViewById<LinearLayout>(R.id.file_item_container)
        val iconIv = transformationLayout.findViewById<ImageView>(R.id.file_item_icon)
        val nameTv = transformationLayout.findViewById<TextView>(R.id.file_item_name)
        val subtitleTv = transformationLayout.findViewById<TextView>(R.id.file_item_subtitle)
        val encryptedTv = transformationLayout.findViewById<TextView>(R.id.file_item_encrypted)

        card.setOnTouchListener { v, event ->
            val innerContainer = (v as? MaterialCardView)?.getChildAt(0) as? LinearLayout
            val pos = v.tag as? Int ?: -1
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (!isSelected(pos)) {
                        innerContainer?.let { c ->
                            (c.getTag(R.id.file_item_container) as? ValueAnimator)?.cancel()
                            val target = if (pos == highlightPosition() || pos == blinkPosition())
                                getThemeColor(context, android.R.attr.colorControlHighlight) else pressColor
                            ValueAnimator.ofObject(ArgbEvaluator(), Color.TRANSPARENT, target).apply {
                                duration = 125
                                addUpdateListener { c.setBackgroundColor(it.animatedValue as Int) }
                                c.setTag(R.id.file_item_container, this)
                                start()
                            }
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val bg = when {
                        pos == highlightPosition() || pos == blinkPosition() ->
                            getThemeColor(context, android.R.attr.colorControlHighlight)
                        isSelected(pos) -> selectedColor()
                        else -> Color.TRANSPARENT
                    }
                    innerContainer?.let { c ->
                        (c.getTag(R.id.file_item_container) as? ValueAnimator)?.cancel()
                        val from = if (c.background is android.graphics.drawable.ColorDrawable)
                            (c.background as android.graphics.drawable.ColorDrawable).color else pressColor
                        ValueAnimator.ofObject(ArgbEvaluator(), from, bg).apply {
                            duration = 125
                            addUpdateListener { c.setBackgroundColor(it.animatedValue as Int) }
                            c.setTag(R.id.file_item_container, this)
                            start()
                        }
                    }
                }
            }
            false
        }

        return FileListAdapter.ViewHolder(
            transformationLayout, container, card, iconIv, nameTv, subtitleTv, encryptedTv
        )
    }
}

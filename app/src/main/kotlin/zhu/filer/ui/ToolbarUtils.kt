package zhu.filer.ui

import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import zhu.filer.R

// 椭圆化起始位置
private const val ELLIPSIZE_START = 0

// 椭圆化中间位置
private const val ELLIPSIZE_MIDDLE = 1

// 应用工具栏标题
fun applyToolbarTitle(toolbar: Toolbar, text: String, mode: Int = ELLIPSIZE_MIDDLE) {
    toolbar.title = text
    toolbar.setTag(R.id.tag_toolbar_title, text to mode)
    refreshToolbarTitle(toolbar)
}

// 刷新工具栏标题显示
fun refreshToolbarTitle(toolbar: Toolbar) {
    toolbar.post {
        @Suppress("UNCHECKED_CAST")
        val tag = toolbar.getTag(R.id.tag_toolbar_title) as? Pair<String, Int> ?: return@post
        val mode = tag.second
        val titleView = getToolbarTitleTextView(toolbar)
        if (titleView == null) {
            toolbar.postDelayed({ refreshToolbarTitle(toolbar) }, 100)
            return@post
        }
        titleView.setSingleLine(true)
        titleView.ellipsize = when (mode) {
            ELLIPSIZE_START -> android.text.TextUtils.TruncateAt.START
            else -> android.text.TextUtils.TruncateAt.MIDDLE
        }
    }
}

// 按路径设置工具栏标题
fun applyToolbarTitlePath(toolbar: Toolbar, path: String) =
    applyToolbarTitle(toolbar, path, ELLIPSIZE_START)

// 按名称设置工具栏标题
fun applyToolbarTitleName(toolbar: Toolbar, name: String) =
    applyToolbarTitle(toolbar, name, ELLIPSIZE_MIDDLE)

// 获取工具栏标题文本视图
private fun getToolbarTitleTextView(toolbar: Toolbar): TextView? {
    return try {
        val field = Toolbar::class.java.getDeclaredField("mTitleTextView")
        field.isAccessible = true
        field.get(toolbar) as? TextView
    } catch (e: Exception) {
        null
    }
}

package zhu.filer.ui

import android.content.Context
import android.util.TypedValue

// dp转px
fun dpToPx(context: Context, dp: Int): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), context.resources.displayMetrics).toInt()

// 获取主题颜色
fun getThemeColor(context: Context, attr: Int, fallback: Int = android.graphics.Color.TRANSPARENT): Int {
    val tv = TypedValue()
    return if (context.theme.resolveAttribute(attr, tv, true)) tv.data else fallback
}

// 获取状态栏高度
fun getStatusBarHeight(context: Context): Int {
    val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
    return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
}

// 获取导航栏高度
fun getNavigationBarHeight(context: Context): Int {
    val resourceId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
    return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
}

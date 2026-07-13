package zhu.filer

import android.content.Context
import com.google.android.material.color.DynamicColors

// 主题色工具
object ThemeHelper {

    // 获取当前主题色名称
    fun getColorName(context: Context): String {
        return context.getSharedPreferences("filer_prefs", Context.MODE_PRIVATE)
            .getString("theme_color", "dynamic") ?: "dynamic"
    }

    // 在当前主题上叠加主题色
    fun applyThemeColor(activity: android.app.Activity) {
        val colorName = getColorName(activity)
        if (colorName == "dynamic") {
            DynamicColors.applyToActivityIfAvailable(activity)
            return
        }
        val overlay = when (colorName) {
            "blue" -> R.style.ThemeOverlay_App_Blue
            "green" -> R.style.ThemeOverlay_App_Green
            "purple" -> R.style.ThemeOverlay_App_Purple
            "orange" -> R.style.ThemeOverlay_App_Orange
            else -> return
        }
        activity.theme.applyStyle(overlay, true)
    }
}

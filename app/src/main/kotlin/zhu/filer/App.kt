package zhu.filer

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

// 应用入口
class App : Application() {

    // 创建应用
    override fun onCreate() {
        super.onCreate()
        applyNightMode()
    }

    // 应用夜间模式
    fun applyNightMode() {
        val mode = getSharedPreferences("filer_prefs", MODE_PRIVATE)
            .getString("night_mode", "system") ?: "system"
        val nightMode = when (mode) {
            "on" -> AppCompatDelegate.MODE_NIGHT_YES
            "off" -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
}

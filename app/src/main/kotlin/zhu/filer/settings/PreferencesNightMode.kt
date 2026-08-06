package zhu.filer.settings

import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.PopupMenu
import zhu.filer.R

// 获取当前夜间模式摘要
internal fun PreferencesActivity.getCurrentNightModeSummary(): String {
    return when (prefs.getString("night_mode", "system")) {
        "on" -> getString(R.string.night_mode_on)
        "off" -> getString(R.string.night_mode_off)
        else -> getString(R.string.night_mode_system)
    }
}

// 显示夜间模式选择对话框
internal fun PreferencesActivity.showNightModeDialog(itemView: View, update: () -> Unit) {
    val labels = arrayOf(
        getString(R.string.night_mode_system),
        getString(R.string.night_mode_on),
        getString(R.string.night_mode_off)
    )
    val keys = arrayOf("system", "on", "off")
    val currentKey = prefs.getString("night_mode", "system") ?: "system"
    val current = keys.indexOf(currentKey)
    val popup = PopupMenu(this, itemView)
    labels.forEachIndexed { index, label ->
        popup.menu.add(0, index, index, label)
        if (index == current) popup.menu.getItem(index).isChecked = true
    }
    popup.menu.setGroupCheckable(0, true, true)
    popup.setOnMenuItemClickListener { menuItem ->
        if (menuItem.itemId == current) return@setOnMenuItemClickListener true
        val which = menuItem.itemId
        prefs.edit().putString("night_mode", keys[which]).apply()
        val nightMode = when (keys[which]) {
            "on" -> AppCompatDelegate.MODE_NIGHT_YES
            "off" -> AppCompatDelegate.MODE_NIGHT_NO
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
        update()
        true
    }
    popup.show()
}

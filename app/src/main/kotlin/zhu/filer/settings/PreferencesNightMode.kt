package zhu.filer.settings

import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import zhu.filer.R
import zhu.filer.ui.buildDialogTitle
import zhu.filer.ui.createSingleChoiceAdapter
import zhu.filer.ui.showListDialog

// 获取当前夜间模式摘要
internal fun PreferencesActivity.getCurrentNightModeSummary(): String {
    return when (prefs.getString("night_mode", "system")) {
        "on" -> getString(R.string.night_mode_on)
        "off" -> getString(R.string.night_mode_off)
        else -> getString(R.string.night_mode_system)
    }
}

// 显示夜间模式选择对话框
internal fun PreferencesActivity.showNightModeDialog(update: () -> Unit) {
    val current = prefs.getString("night_mode", "system") ?: "system"
    val labels = arrayOf(
        getString(R.string.night_mode_system),
        getString(R.string.night_mode_on),
        getString(R.string.night_mode_off)
    )
    val keys = arrayOf("system", "on", "off")
    val checked = keys.indexOf(current)
    val dialog = MaterialAlertDialogBuilder(this)
        .setCustomTitle(buildDialogTitle(this, R.string.night_mode))
        .setSingleChoiceItems(createSingleChoiceAdapter(this, labels), checked) { dialog, which ->
            prefs.edit().putString("night_mode", keys[which]).apply()
            val nightMode = when (keys[which]) {
                "on" -> AppCompatDelegate.MODE_NIGHT_YES
                "off" -> AppCompatDelegate.MODE_NIGHT_NO
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(nightMode)
            dialog.dismiss()
            update()
        }
        .setNegativeButton(R.string.cancel, null)
        .create()
    showListDialog(dialog)
}

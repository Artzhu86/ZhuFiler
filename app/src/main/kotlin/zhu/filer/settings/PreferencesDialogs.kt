package zhu.filer.settings

import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.LocaleListCompat
import zhu.filer.R
import zhu.filer.ui.ThemeHelper

// 获取当前语言摘要
internal fun PreferencesActivity.getCurrentLanguageSummary(): String {
    val locales = AppCompatDelegate.getApplicationLocales()
    return when {
        locales.isEmpty -> getString(R.string.language_system)
        locales.get(0)?.language == "zh" -> getString(R.string.language_chinese)
        locales.get(0)?.language == "en" -> getString(R.string.language_english)
        else -> getString(R.string.language_system)
    }
}

// 显示语言选择对话框
internal fun PreferencesActivity.showLanguageDialog(itemView: View, update: () -> Unit) {
    val labels = arrayOf(
        getString(R.string.language_system),
        getString(R.string.language_chinese),
        getString(R.string.language_english)
    )
    val locales = AppCompatDelegate.getApplicationLocales()
    val current = when {
        locales.isEmpty -> 0
        locales.get(0)?.language == "zh" -> 1
        locales.get(0)?.language == "en" -> 2
        else -> 0
    }
    val popup = PopupMenu(this, itemView)
    labels.forEachIndexed { index, label ->
        popup.menu.add(0, index, index, label)
        if (index == current) popup.menu.getItem(index).isChecked = true
    }
    popup.menu.setGroupCheckable(0, true, true)
    popup.setOnMenuItemClickListener { menuItem ->
        if (menuItem.itemId == current) return@setOnMenuItemClickListener true
        val locale = when (menuItem.itemId) {
            1 -> LocaleListCompat.forLanguageTags("zh")
            2 -> LocaleListCompat.forLanguageTags("en")
            else -> LocaleListCompat.getEmptyLocaleList()
        }
        AppCompatDelegate.setApplicationLocales(locale)
        update()
        true
    }
    popup.show()
}

// 获取当前主题色摘要
internal fun PreferencesActivity.getCurrentThemeColorSummary(): String {
    return when (ThemeHelper.getColorName(this)) {
        "blue" -> getString(R.string.theme_color_blue)
        "green" -> getString(R.string.theme_color_green)
        "purple" -> getString(R.string.theme_color_purple)
        "orange" -> getString(R.string.theme_color_orange)
        "red" -> getString(R.string.theme_color_red)
        "yellow" -> getString(R.string.theme_color_yellow)
        "cyan" -> getString(R.string.theme_color_cyan)
        else -> getString(R.string.theme_color_dynamic)
    }
}

// 显示主题色选择对话框
internal fun PreferencesActivity.showThemeColorDialog(itemView: View, update: () -> Unit) {
    val labels = arrayOf(
        getString(R.string.theme_color_dynamic),
        getString(R.string.theme_color_red),
        getString(R.string.theme_color_orange),
        getString(R.string.theme_color_yellow),
        getString(R.string.theme_color_green),
        getString(R.string.theme_color_cyan),
        getString(R.string.theme_color_blue),
        getString(R.string.theme_color_purple)
    )
    val keys = arrayOf("dynamic", "red", "orange", "yellow", "green", "cyan", "blue", "purple")
    val current = keys.indexOf(ThemeHelper.getColorName(this))
    val popup = PopupMenu(this, itemView)
    labels.forEachIndexed { index, label ->
        popup.menu.add(0, index, index, label)
        if (index == current) popup.menu.getItem(index).isChecked = true
    }
    popup.menu.setGroupCheckable(0, true, true)
    popup.setOnMenuItemClickListener { menuItem ->
        if (menuItem.itemId == current) return@setOnMenuItemClickListener true
        val which = menuItem.itemId
        prefs.edit().putString("theme_color", keys[which]).apply()
        update()
        recreate()
        true
    }
    popup.show()
}

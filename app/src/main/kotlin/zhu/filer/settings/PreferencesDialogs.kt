package zhu.filer.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import zhu.filer.R
import zhu.filer.ui.ThemeHelper
import zhu.filer.ui.buildDialogTitle
import zhu.filer.ui.createSingleChoiceAdapter
import zhu.filer.ui.showListDialog

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
internal fun PreferencesActivity.showLanguageDialog(update: () -> Unit) {
    val current = getCurrentLanguageSummary()
    val labels = arrayOf(
        getString(R.string.language_system),
        getString(R.string.language_chinese),
        getString(R.string.language_english)
    )
    val checked = labels.indexOf(current)
    val dialog = MaterialAlertDialogBuilder(this)
        .setCustomTitle(buildDialogTitle(this, R.string.language))
        .setSingleChoiceItems(createSingleChoiceAdapter(this, labels), checked) { dialog, which ->
            val locale = when (which) {
                1 -> LocaleListCompat.forLanguageTags("zh")
                2 -> LocaleListCompat.forLanguageTags("en")
                else -> LocaleListCompat.getEmptyLocaleList()
            }
            AppCompatDelegate.setApplicationLocales(locale)
            dialog.dismiss()
            update()
        }
        .setNegativeButton(R.string.cancel, null)
        .create()
    showListDialog(dialog)
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
internal fun PreferencesActivity.showThemeColorDialog(update: () -> Unit) {
    val current = ThemeHelper.getColorName(this)
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
    val checked = keys.indexOf(current)
    val dialog = MaterialAlertDialogBuilder(this)
        .setCustomTitle(buildDialogTitle(this, R.string.theme_color))
        .setSingleChoiceItems(createSingleChoiceAdapter(this, labels), checked) { dialog, which ->
            prefs.edit().putString("theme_color", keys[which]).apply()
            dialog.dismiss()
            update()
            recreate()
        }
        .setNegativeButton(R.string.cancel, null)
        .create()
    showListDialog(dialog)
}

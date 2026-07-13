package zhu.filer

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import zhu.filer.databinding.ActivityPreferencesBinding

// 设置界面
class PreferencesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPreferencesBinding
    private lateinit var prefs: SharedPreferences
    private lateinit var adapter: PreferencesAdapter

    // 创建界面
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyThemeColor(this)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityPreferencesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = Color.TRANSPARENT
        val statusBarHeight = getStatusBarHeight(this)
        binding.toolbar.layoutParams.height = binding.toolbar.layoutParams.height + statusBarHeight
        binding.toolbar.setPadding(0, statusBarHeight, 0, 0)
        binding.toolbar.setBackgroundColor(Color.TRANSPARENT)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        prefs = getSharedPreferences("filer_prefs", MODE_PRIVATE)

        val items = buildPreferenceItems()
        adapter = PreferencesAdapter(items)
        binding.preferencesList.layoutManager = LinearLayoutManager(this)
        binding.preferencesList.adapter = adapter
    }

    // 构建设置项
    private fun buildPreferenceItems(): List<PreferenceItem> {
        return listOf(
            PreferenceItem(
                title = getString(R.string.language),
                summaryProvider = { getCurrentLanguageSummary() },
                onClick = { update -> showLanguageDialog(update) }
            ),
            PreferenceItem(
                title = getString(R.string.theme_color),
                summaryProvider = { getCurrentThemeColorSummary() },
                onClick = { update -> showThemeColorDialog(update) }
            ),
            PreferenceItem(
                title = getString(R.string.night_mode),
                summaryProvider = { getCurrentNightModeSummary() },
                onClick = { update -> showNightModeDialog(update) }
            )
        )
    }

    // 获取当前语言摘要
    private fun getCurrentLanguageSummary(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return when {
            locales.isEmpty -> getString(R.string.language_system)
            locales.get(0)?.language == "zh" -> getString(R.string.language_chinese)
            locales.get(0)?.language == "en" -> getString(R.string.language_english)
            else -> getString(R.string.language_system)
        }
    }

    // 显示语言选择对话框
    private fun showLanguageDialog(update: () -> Unit) {
        val current = getCurrentLanguageSummary()
        val labels = arrayOf(
            getString(R.string.language_system),
            getString(R.string.language_chinese),
            getString(R.string.language_english)
        )
        val checked = labels.indexOf(current)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.language)
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
            .show()
    }

    // 获取当前主题色摘要
    private fun getCurrentThemeColorSummary(): String {
        return when (ThemeHelper.getColorName(this)) {
            "blue" -> getString(R.string.theme_color_blue)
            "green" -> getString(R.string.theme_color_green)
            "purple" -> getString(R.string.theme_color_purple)
            "orange" -> getString(R.string.theme_color_orange)
            else -> getString(R.string.theme_color_dynamic)
        }
    }

    // 显示主题色选择对话框
    private fun showThemeColorDialog(update: () -> Unit) {
        val current = ThemeHelper.getColorName(this)
        val labels = arrayOf(
            getString(R.string.theme_color_dynamic),
            getString(R.string.theme_color_blue),
            getString(R.string.theme_color_green),
            getString(R.string.theme_color_purple),
            getString(R.string.theme_color_orange)
        )
        val keys = arrayOf("dynamic", "blue", "green", "purple", "orange")
        val checked = keys.indexOf(current)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.theme_color)
            .setSingleChoiceItems(createSingleChoiceAdapter(this, labels), checked) { dialog, which ->
                prefs.edit().putString("theme_color", keys[which]).apply()
                dialog.dismiss()
                update()
                recreate()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // 获取当前夜间模式摘要
    private fun getCurrentNightModeSummary(): String {
        return when (prefs.getString("night_mode", "system")) {
            "on" -> getString(R.string.night_mode_on)
            "off" -> getString(R.string.night_mode_off)
            else -> getString(R.string.night_mode_system)
        }
    }

    // 显示夜间模式选择对话框
    private fun showNightModeDialog(update: () -> Unit) {
        val current = prefs.getString("night_mode", "system") ?: "system"
        val labels = arrayOf(
            getString(R.string.night_mode_system),
            getString(R.string.night_mode_on),
            getString(R.string.night_mode_off)
        )
        val keys = arrayOf("system", "on", "off")
        val checked = keys.indexOf(current)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.night_mode)
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
            .show()
    }
}

// 设置项数据
data class PreferenceItem(
    val title: String,
    val summaryProvider: (SharedPreferences) -> String,
    val onClick: (() -> Unit) -> Unit
)

// 设置项适配器
class PreferencesAdapter(
    private val items: List<PreferenceItem>
) : RecyclerView.Adapter<PreferencesAdapter.ViewHolder>() {

    // 视图持有者
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTv: TextView = view.findViewById(R.id.pref_title)
        val summaryTv: TextView = view.findViewById(R.id.pref_summary)
    }

    // 创建视图
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_preference, parent, false)
        return ViewHolder(view)
    }

    // 绑定数据
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val prefs = holder.itemView.context.getSharedPreferences("filer_prefs", AppCompatActivity.MODE_PRIVATE)
        holder.titleTv.text = item.title
        holder.summaryTv.text = item.summaryProvider(prefs)
        holder.itemView.setOnClickListener {
            item.onClick { notifyItemChanged(position) }
        }
    }

    // 数量
    override fun getItemCount() = items.size
}

package zhu.filer.settings

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import rikka.shizuku.Shizuku
import zhu.filer.R
import zhu.filer.ui.ThemeHelper
import zhu.filer.util.ShizukuManager
import zhu.filer.ui.getStatusBarHeight
import zhu.filer.databinding.ActivityPreferencesBinding

// 设置界面
class PreferencesActivity : AppCompatActivity() {

    internal lateinit var binding: ActivityPreferencesBinding
    internal lateinit var prefs: SharedPreferences
    internal lateinit var adapter: PreferencesAdapter

    internal val shizukuPermissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        if (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            adapter.notifyItemChanged(3)
        }
    }

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
    internal fun buildPreferenceItems(): List<PreferenceItem> {
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
            ),
            PreferenceItem(
                title = getString(R.string.shizuku),
                summaryProvider = { getShizukuStatusSummary() },
                onClick = { update -> showShizukuDialog(update) }
            )
        )
    }

    // 恢复
    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) {
            adapter.notifyItemChanged(3)
        }
    }

    // 销毁
    override fun onDestroy() {
        super.onDestroy()
        ShizukuManager.removePermissionResultListener(shizukuPermissionListener)
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

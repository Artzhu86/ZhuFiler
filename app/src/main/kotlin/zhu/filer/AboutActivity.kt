package zhu.filer

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import zhu.filer.ui.ThemeHelper
import zhu.filer.ui.getStatusBarHeight
import zhu.filer.databinding.ActivityAboutBinding

// 关于界面
class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    // 创建界面
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyThemeColor(this)
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = Color.TRANSPARENT
        val statusBarHeight = getStatusBarHeight(this)
        binding.toolbar.layoutParams.height = binding.toolbar.layoutParams.height + statusBarHeight
        binding.toolbar.setPadding(0, statusBarHeight, 0, 0)
        binding.toolbar.setBackgroundColor(Color.TRANSPARENT)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val versionName = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0)).versionName
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0).versionName
        }
        val items = listOf(
            AboutItem(R.string.official_website, "zhufiler.netlify.app", "https://zhufiler.netlify.app/"),
            AboutItem(R.string.view_github, "Artzhu86/ZhuFiler", "https://github.com/Artzhu86/ZhuFiler"),
            AboutItem(R.string.join_qq_group, "145559564", "mqqapi://card/show_pslcard?src_type=internal&version=1&card_type=group&uin=145559564"),
            AboutItem(R.string.official_bilibili, "3546694920702125", "bilibili://space/3546694920702125")
        )
        binding.aboutList.layoutManager = LinearLayoutManager(this)
        binding.aboutList.adapter = AboutAdapter(items, versionName?.let { "v$it" } ?: "")
    }
}

// 关于项数据
private data class AboutItem(
    val titleRes: Int,
    val summary: String,
    val url: String
)

// 关于适配器
private class AboutAdapter(
    private val items: List<AboutItem>,
    private val versionText: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    private class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconIv: ImageView = view.findViewById(R.id.ivIcon)
        val appNameTv: TextView = view.findViewById(R.id.tvAppName)
        val versionTv: TextView = view.findViewById(R.id.tvVersion)
    }

    private class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTv: TextView = view.findViewById(R.id.pref_title)
        val summaryTv: TextView = view.findViewById(R.id.pref_summary)
    }

    override fun getItemViewType(position: Int): Int =
        if (position == 0) TYPE_HEADER else TYPE_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(inflater.inflate(R.layout.item_about_header, parent, false))
            else -> ItemViewHolder(inflater.inflate(R.layout.item_preference, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                holder.versionTv.text = versionText
                holder.iconIv.setOnLongClickListener {
                    val rotate = RotateAnimation(0f, 360f,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f)
                    rotate.duration = 600
                    holder.iconIv.startAnimation(rotate)
                    true
                }
                val copyListener = View.OnLongClickListener { v ->
                    val clipboard = v.context
                        .getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("text", (v as TextView).text))
                    android.widget.Toast.makeText(v.context, v.context.getString(R.string.copied_to_clipboard), android.widget.Toast.LENGTH_SHORT).show()
                    true
                }
                holder.appNameTv.setOnLongClickListener(copyListener)
                holder.versionTv.setOnLongClickListener(copyListener)
            }
            is ItemViewHolder -> {
                val item = items[position - 1]
                holder.titleTv.text = holder.itemView.context.getString(item.titleRes)
                holder.summaryTv.text = item.summary
                holder.itemView.setOnClickListener {
                    val context = holder.itemView.context
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
                    }.onFailure {
                        android.widget.Toast.makeText(context, context.getString(R.string.open_failed, context.getString(item.titleRes)), android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                holder.itemView.setOnLongClickListener { v ->
                    val clipboard = v.context
                        .getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("text", item.summary))
                    android.widget.Toast.makeText(v.context, v.context.getString(R.string.copied_to_clipboard), android.widget.Toast.LENGTH_SHORT).show()
                    true
                }
            }
        }
    }

    override fun getItemCount() = items.size + 1
}

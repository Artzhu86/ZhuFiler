package zhu.filer

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.skydoves.transformationlayout.TransformationLayout
import com.skydoves.transformationlayout.onTransformationEndContainer
import io.getstream.photoview.PhotoView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import zhu.filer.databinding.ActivityImageViewerBinding
import zhu.filer.databinding.ItemImagePageBinding

import com.google.android.material.R as materialR

// 图像查看界面
class ImageViewerActivity : AppCompatActivity() {

    // 伴生对象
    companion object {
        const val EXTRA_FILE_PATH = "extra_file_path"
    }

    private lateinit var binding: ActivityImageViewerBinding
    private var images = mutableListOf<File>()
    private var currentIndex: Int = 0
    private var isFullscreen = false
    private var originalBgColor: Int = Color.TRANSPARENT

    // 创建活动
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyThemeColor(this)
        val params = intent.getParcelableExtra<TransformationLayout.Params>("TransformationParams")
        if (params != null) {
            onTransformationEndContainer(params)
        }
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (params != null) {
            val surfaceColor = getThemeColor(this, materialR.attr.colorSurface)
            val bgColor = getThemeColor(this, android.R.attr.colorBackground)
            (window.sharedElementEnterTransition as? MaterialContainerTransform)?.apply {
                setAllContainerColors(surfaceColor)
                scrimColor = bgColor
                fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
                addListener(object : android.transition.Transition.TransitionListener {
                    override fun onTransitionStart(transition: android.transition.Transition) {}
                    // 过渡结束
                    override fun onTransitionEnd(transition: android.transition.Transition) {
                        startGifIfNeeded()
                    }
                    // 过渡取消
                    override fun onTransitionCancel(transition: android.transition.Transition) {
                        startGifIfNeeded()
                    }
                    // 过渡暂停
                    override fun onTransitionPause(transition: android.transition.Transition) {}
                    // 过渡恢复
                    override fun onTransitionResume(transition: android.transition.Transition) {}
                })
            }
            (window.sharedElementReturnTransition as? MaterialContainerTransform)?.apply {
                setAllContainerColors(surfaceColor)
                scrimColor = bgColor
                fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
            }
        }

        val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
        val f = filePath?.let { File(it) }
        if (f == null || !f.canRead()) {
            finish()
            return
        }

        images = (loadImagesInDirectory(f) as List<File>).toMutableList()
        currentIndex = images.indexOf(f).coerceAtLeast(0)

        originalBgColor = getThemeColor(this, android.R.attr.colorBackground)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = currentFile().name
        }
        applyToolbarTitleName(binding.toolbar, currentFile().name)
        updateSubtitle()

        binding.toolbar.setBackgroundColor(getThemeColor(this, materialR.attr.colorPrimaryContainer))

        val statusBarHeight = getStatusBarHeight(this)
        binding.toolbar.layoutParams.height = binding.toolbar.layoutParams.height + statusBarHeight
        binding.toolbar.setPadding(0, statusBarHeight, 0, 0)

        binding.imagePager.adapter = ImagePagerAdapter(
            images = images,
            onTap = { toggleFullscreen() },
            onScaleChange = { scaleFactor ->
                if (scaleFactor > 1f && !isFullscreen) {
                    toggleFullscreen()
                }
            }
        )
        binding.imagePager.setCurrentItem(currentIndex, false)
        binding.imagePager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentIndex = position
                supportActionBar?.title = currentFile().name
                applyToolbarTitleName(binding.toolbar, currentFile().name)
                updateSubtitle()
                binding.imagePager.post { startGifIfNeeded() }
            }
        })
    }

    // 启动GIF动画
    private fun startGifIfNeeded() {
        val rv = binding.imagePager.getChildAt(0) as? RecyclerView ?: return
        val holder = rv.findViewHolderForAdapterPosition(binding.imagePager.currentItem) ?: return
        val photoView = (holder as? ImagePagerAdapter.PageHolder)?.binding?.root ?: return
        val drawable = photoView.drawable as? Animatable ?: return
        if (drawable.isRunning) return
        val d = photoView.drawable
        d.setVisible(true, false)
        d.callback = photoView
        drawable.start()
    }

    // 当前文件
    private fun currentFile(): File = images[currentIndex]

    // 加载目录中的图像
    private fun loadImagesInDirectory(target: File): List<File> {
        val dir = target.parentFile ?: return listOf(target)
        val prefs = getSharedPreferences("filer_prefs", MODE_PRIVATE)
        val showHidden = prefs.getBoolean("show_hidden", false)
        val sortMode = runCatching {
            SortMode.valueOf(prefs.getString("sort_mode", SortMode.NAME.name) ?: SortMode.NAME.name)
        }.getOrDefault(SortMode.NAME)
        val files = dir.listFiles { file ->
            file.isFile &&
                FileType.isImage(file) &&
                (showHidden || !file.name.startsWith("."))
        } ?: return listOf(target)
        return if (files.isEmpty()) listOf(target) else files.sortedWith(getSortComparator(sortMode))
    }

    // 更新副标题
    private fun updateSubtitle() {
        val file = currentFile()
        val dateStr = SimpleDateFormat(getString(R.string.date_format), Locale.getDefault())
            .format(Date(file.lastModified()))
        val sizeStr = Formatter.formatFileSize(this, file.length())

        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        val w = opts.outWidth
        val h = opts.outHeight
        val dimStr = if (w > 0 && h > 0) "  (${w}x${h})" else ""

        supportActionBar?.subtitle = "$dateStr  $sizeStr$dimStr"
    }

    // 创建菜单
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, getString(R.string.share))
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(Menu.NONE, Menu.FIRST + 1, Menu.NONE, getString(R.string.delete))
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        return true
    }

    // 菜单项选择
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            Menu.FIRST -> shareFile(this, currentFile())
            Menu.FIRST + 1 -> confirmDeleteCurrent()
        }
        return super.onOptionsItemSelected(item)
    }

    // 确认删除当前
    private fun confirmDeleteCurrent() {
        val file = currentFile()
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.confirm_delete)
            .setMessage(getString(R.string.delete_message, file.name))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                if (file.delete()) {
                    images.removeAt(currentIndex)
                    if (images.isEmpty()) {
                        finishAfterTransition()
                        return@setPositiveButton
                    }
                    if (currentIndex >= images.size) currentIndex = images.size - 1
                    binding.imagePager.adapter?.notifyItemRemoved(currentIndex)
                    if (binding.imagePager.currentItem != currentIndex) {
                        binding.imagePager.setCurrentItem(currentIndex, false)
                    }
                    supportActionBar?.title = currentFile().name
                    applyToolbarTitleName(binding.toolbar, currentFile().name)
                    updateSubtitle()
                } else {
                    toast(this, getString(R.string.delete_failed))
                }
            }
            .show()
    }

    // 切换全屏
    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        val controller = WindowCompat.getInsetsController(window, binding.root) ?: return
        if (isFullscreen) {
            binding.root.setBackgroundColor(Color.BLACK)
            binding.toolbar.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    binding.toolbar.visibility = View.GONE
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
                .start()
        } else {
            binding.root.setBackgroundColor(originalBgColor)
            controller.show(WindowInsetsCompat.Type.systemBars())
            binding.toolbar.visibility = View.VISIBLE
            binding.toolbar.alpha = 0f
            binding.toolbar.animate()
                .alpha(1f)
                .setDuration(200)
                .start()
        }
    }

    // 支持返回导航
    override fun onSupportNavigateUp(): Boolean {
        finishAfterTransition()
        return true
    }

    // 按下返回键
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        finishAfterTransition()
    }

    // 恢复
    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) {
            refreshToolbarTitle(binding.toolbar)
        }
    }

    // 窗口焦点变化
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && ::binding.isInitialized) {
            refreshToolbarTitle(binding.toolbar)
        }
    }

    // 图像分页适配器
    private class ImagePagerAdapter(
        private val images: List<File>,
        private val onTap: () -> Unit,
        private val onScaleChange: (Float) -> Unit
    ) : RecyclerView.Adapter<ImagePagerAdapter.PageHolder>() {

        // 页面持有者
        class PageHolder(val binding: ItemImagePageBinding) : RecyclerView.ViewHolder(binding.root)

        // 创建视图
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
            val binding = ItemImagePageBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return PageHolder(binding)
        }

        // 绑定视图
        override fun onBindViewHolder(holder: PageHolder, position: Int) {
            val photoView: PhotoView = holder.binding.root
            photoView.setScale(1f, false)
            photoView.setOnViewTapListener { _, _, _ -> onTap() }
            photoView.setOnScaleChangeListener { scaleFactor, _, _ -> onScaleChange(scaleFactor) }
            Glide.with(photoView.context)
                .load(images[position])
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .fitCenter()
                .error(R.drawable.outline_image_24)
                .into(photoView)
        }

        // 获取项数
        override fun getItemCount(): Int = images.size
    }
}

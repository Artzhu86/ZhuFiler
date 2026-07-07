package zhu.filer

import android.graphics.BitmapFactory
import android.graphics.Color
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
import io.getstream.photoview.PhotoView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import zhu.filer.databinding.ActivityImagePreviewBinding
import zhu.filer.databinding.ItemImagePageBinding

import com.google.android.material.R as materialR

class ImagePreviewActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FILE_PATH = "extra_file_path"
    }

    private lateinit var binding: ActivityImagePreviewBinding
    private lateinit var images: List<File>
    private var currentIndex: Int = 0
    private var isFullscreen = false
    private var originalBgColor: Int = Color.TRANSPARENT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityImagePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
        val f = filePath?.let { File(it) }
        if (f == null || !f.canRead()) {
            finish()
            return
        }

        images = loadImagesInDirectory(f)
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

        val statusBarHeight = getStatusBarHeight()
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
            }
        })
    }

    private fun currentFile(): File = images[currentIndex]

    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, getString(R.string.share))
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            Menu.FIRST -> shareFile(this, currentFile())
        }
        return super.onOptionsItemSelected(item)
    }

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

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) {
            refreshToolbarTitle(binding.toolbar)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && ::binding.isInitialized) {
            refreshToolbarTitle(binding.toolbar)
        }
    }

    private class ImagePagerAdapter(
        private val images: List<File>,
        private val onTap: () -> Unit,
        private val onScaleChange: (Float) -> Unit
    ) : RecyclerView.Adapter<ImagePagerAdapter.PageHolder>() {

        class PageHolder(val binding: ItemImagePageBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
            val binding = ItemImagePageBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return PageHolder(binding)
        }

        override fun onBindViewHolder(holder: PageHolder, position: Int) {
            val photoView: PhotoView = holder.binding.root
            photoView.setScale(1f, false)
            photoView.setOnViewTapListener { _, _, _ -> onTap() }
            photoView.setOnScaleChangeListener { scaleFactor, _, _ -> onScaleChange(scaleFactor) }
            Glide.with(photoView.context)
                .load(images[position])
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .fitCenter()
                .error(R.drawable.outline_image_24)
                .into(photoView)
        }

        override fun getItemCount(): Int = images.size
    }
}

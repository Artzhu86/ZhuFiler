package zhu.filer

import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import zhu.filer.databinding.ActivityImagePreviewBinding

import com.google.android.material.R as materialR

class ImagePreviewActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FILE_PATH = "extra_file_path"
    }

    private lateinit var binding: ActivityImagePreviewBinding
    private var isFullscreen = false
    private var originalBgColor: Int = Color.TRANSPARENT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityImagePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
        val file = filePath?.let { File(it) }
        if (file == null || !file.canRead()) {
            finish()
            return
        }

        originalBgColor = getThemeColor(this, android.R.attr.colorBackground)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = middleEllipsize(file.name)
            setDisplayHomeAsUpEnabled(true)
        }

        binding.toolbar.setBackgroundColor(getThemeColor(this, materialR.attr.colorPrimaryContainer))

        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = sb.top)
            insets
        }

        binding.imageView.setOnViewTapListener { _, _, _ ->
            toggleFullscreen()
        }

        binding.imageView.setOnScaleChangeListener { scaleFactor, _, _ ->
            if (scaleFactor > 1f && !isFullscreen) {
                toggleFullscreen()
            }
        }

        loadImage(file)
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

    private fun loadImage(file: File) {
        lifecycleScope.launch {
            val bmp = withContext(Dispatchers.IO) {
                runCatching {
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.absolutePath, opts)
                    val targetWidth = resources.displayMetrics.widthPixels
                    val targetHeight = resources.displayMetrics.heightPixels
                    var sampleSize = 1
                    while (opts.outWidth / sampleSize > targetWidth * 2 ||
                           opts.outHeight / sampleSize > targetHeight * 2) {
                        sampleSize *= 2
                    }
                    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                    BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
                }.getOrNull()
            }
            if (bmp == null) {
                Toast.makeText(
                    this@ImagePreviewActivity,
                    getString(R.string.image_load_failed),
                    Toast.LENGTH_LONG
                ).show()
                finish()
            } else {
                binding.imageView.setImageBitmap(bmp)
            }
        }
    }
}

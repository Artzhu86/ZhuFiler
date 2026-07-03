package zhu.filer

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import io.github.rosemoe.sora.event.ScrollEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import zhu.filer.databinding.ActivityTextPreviewBinding

import com.google.android.material.R as materialR

class TextPreviewActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FILE_PATH = "extra_file_path"
    }

    private lateinit var binding: ActivityTextPreviewBinding
    private val containerColor: Int by lazy { getThemeColor(this, materialR.attr.colorPrimaryContainer) }
    private var toolbarAlphaThreshold: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTextPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = Color.TRANSPARENT
        }

        val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
        val file = filePath?.let { File(it) }
        if (file == null || !file.canRead()) {
            finish()
            return
        }

        val statusBarHeight = getStatusBarHeight()
        val actionBarHeight = binding.toolbar.layoutParams.height
        binding.toolbar.layoutParams.height = actionBarHeight + statusBarHeight
        binding.toolbar.setPadding(0, statusBarHeight, 0, 0)
        binding.toolbar.setBackgroundColor(Color.TRANSPARENT)
        binding.toolbar.setSubtitleTextAppearance(this, R.style.ToolbarSubtitle)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = middleEllipsize(file.name)
            setDisplayHomeAsUpEnabled(true)
        }

        binding.editor.isEditable = false

        val ext = file.extension.lowercase()

        lifecycleScope.launch {
            val content = withContext(Dispatchers.IO) {
                CodeEditorTextMate.init(applicationContext)
                runCatching { file.bufferedReader().use { it.readText() } }
                    .getOrDefault(getString(R.string.read_failed))
            }

            binding.editor.colorScheme = CodeEditorTextMate.createColorScheme()
            binding.editor.setEditorLanguage(CodeEditorTextMate.languageForExtension(ext))
            binding.editor.setText(
                if (content.isEmpty()) getString(R.string.file_empty) else content
            )
        }

        binding.toolbar.post {
            toolbarAlphaThreshold = binding.toolbar.height
        }
        binding.editor.subscribeEvent(ScrollEvent::class.java) { _, _ ->
            if (toolbarAlphaThreshold == 0) return@subscribeEvent
            val offset = binding.editor.scrollY
            val alpha = (offset.toFloat() / toolbarAlphaThreshold).coerceIn(0f, 1f)
            val color = Color.argb(
                (alpha * 255).toInt(),
                Color.red(containerColor),
                Color.green(containerColor),
                Color.blue(containerColor)
            )
            binding.toolbar.setBackgroundColor(color)
        }
    }

    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::binding.isInitialized) {
            binding.editor.release()
        }
    }
}

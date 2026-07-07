package zhu.filer

import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import zhu.filer.databinding.ActivityTextEditorBinding

import com.google.android.material.R as materialR

class TextEditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FILE_PATH = "extra_file_path"
    }

    private lateinit var binding: ActivityTextEditorBinding
    private lateinit var file: File
    private var isModified = false
    private var initialContent: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityTextEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
        val f = filePath?.let { File(it) }
        if (f == null || !f.canRead()) {
            finish()
            return
        }
        file = f

        binding.toolbar.setBackgroundColor(getThemeColor(this, materialR.attr.colorPrimaryContainer))

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            binding.toolbar.updatePadding(top = sb.top)
            val bottomPad = maxOf(sb.bottom, ime.bottom)
            binding.symbolBarScroll.updatePadding(bottom = bottomPad)
            insets
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = file.name
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            @Suppress("DEPRECATION")
            onBackPressed()
        }
        applyToolbarTitleName(binding.toolbar, file.name)
        updateSubtitle()

        binding.editor.isEditable = true

        binding.editor.setDisplayLnPanel(false)

        setupSymbolBar()

        binding.editor.isVerticalScrollBarEnabled = true
        binding.editor.isHorizontalScrollBarEnabled = true
        binding.editor.verticalScrollbarThumbDrawable =
            androidx.core.content.ContextCompat.getDrawable(this, R.drawable.scrollbar_thumb)!!
        binding.editor.verticalScrollbarTrackDrawable =
            androidx.core.content.ContextCompat.getDrawable(this, R.drawable.scrollbar_track)!!
        binding.editor.horizontalScrollbarThumbDrawable =
            androidx.core.content.ContextCompat.getDrawable(this, R.drawable.scrollbar_thumb_horizontal)!!
        binding.editor.horizontalScrollbarTrackDrawable =
            androidx.core.content.ContextCompat.getDrawable(this, R.drawable.scrollbar_track_horizontal)!!

        val ext = file.extension.lowercase()

        CodeEditorTextMate.init(applicationContext)
        CodeEditorTextMate.applyTheme(this)

        binding.editor.colorScheme = CodeEditorTextMate.createColorScheme(this)
        CodeEditorTextMate.applyEditorColors(binding.editor, this)

        binding.editor.setEditorLanguage(CodeEditorTextMate.languageForExtension(ext))

        lifecycleScope.launch {
            val content = withContext(Dispatchers.IO) {
                runCatching { file.bufferedReader().use { it.readText() } }
                    .getOrDefault(getString(R.string.read_failed))
            }

            initialContent = content
            binding.editor.setText(content)

            binding.editor.subscribeAlways(ContentChangeEvent::class.java) {
                isModified = binding.editor.text.toString() != initialContent
                updateSubtitle()
            }
            binding.editor.subscribeAlways(SelectionChangeEvent::class.java) {
                updateSubtitle()
            }
        }
    }

    private fun updateSubtitle() {
        val cursor = binding.editor.cursor
        val line = cursor.leftLine + 1
        val col = cursor.leftColumn + 1
        val selected = if (cursor.isSelected()) cursor.right - cursor.left else 0
        val selectedTag = if (selected > 0) " ($selected)" else ""
        supportActionBar?.subtitle = "$line:$col$selectedTag"
        applyToolbarTitleName(binding.toolbar, if (isModified) "*${file.name}" else file.name)
    }

    private fun setupSymbolBar() {
        val symbols = listOf(
            "→" to "  ",
            ";", "=", ",", ".",
            "(", ")", "{", "}", "[", "]",
            "+", "-", "*", "/",
            ":", "\"", "'",
            "<", ">", "#", "@"
        )
        val container = binding.symbolBar
        val density = resources.displayMetrics.density
        val textColor = getThemeColor(this, materialR.attr.colorOnSurface)

        for (entry in symbols) {
            val display: String
            val insert: String
            when (entry) {
                is String -> { display = entry; insert = entry }
                is Pair<*, *> -> { display = entry.first as String; insert = entry.second as String }
                else -> continue
            }
            val tv = TextView(this).apply {
                text = display
                textSize = 18f
                gravity = Gravity.CENTER
                setTextColor(textColor)
                val padH = (density * 12).toInt()
                val padV = (density * 4).toInt()
                setPadding(padH, padV, padH, padV)
                isClickable = true
                isFocusable = true
                setBackgroundResource(R.drawable.symbol_key_background)
            }
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.gravity = Gravity.CENTER_VERTICAL
            tv.layoutParams = lp
            tv.setOnClickListener {
                binding.editor.insertText(insert, insert.length)
            }
            container.addView(tv)
        }
    }

    private fun saveFile(): Boolean {
        return try {
            val content = binding.editor.text.toString()
            file.writeText(content)
            initialContent = content
            isModified = false
            updateSubtitle()
            Toast.makeText(this, R.string.save_success, Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.save_failed, e.message), Toast.LENGTH_LONG).show()
            false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, getString(R.string.save))
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(Menu.NONE, Menu.FIRST + 1, Menu.NONE, getString(R.string.undo))
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(Menu.NONE, Menu.FIRST + 2, Menu.NONE, getString(R.string.redo))
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            Menu.FIRST -> saveFile()
            Menu.FIRST + 1 -> binding.editor.undo()
            Menu.FIRST + 2 -> binding.editor.redo()
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isModified) {
            MaterialAlertDialogBuilder(this)
                .setTitle(file.name)
                .setMessage(R.string.unsaved_changes)
                .setPositiveButton(R.string.save_and_exit) { _, _ ->
                    if (saveFile()) {
                        @Suppress("DEPRECATION")
                        super.onBackPressed()
                    }
                }
                .setNegativeButton(R.string.discard) { _, _ ->
                    @Suppress("DEPRECATION")
                    super.onBackPressed()
                }
                .setCancelable(false)
                .show()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
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

    override fun onDestroy() {
        super.onDestroy()
        if (::binding.isInitialized) {
            binding.editor.release()
        }
    }
}

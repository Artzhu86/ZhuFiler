package zhu.filer.editor

import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.transition.platform.MaterialContainerTransform
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zhu.filer.R
import zhu.filer.ui.getThemeColor
import com.google.android.material.R as materialR

// 设置过渡动画
internal fun TextEditorActivity.setupTransition(params: com.skydoves.transformationlayout.TransformationLayout.Params?) {
    if (params == null) return
    val surfaceColor = getThemeColor(this, materialR.attr.colorSurface)
    val bgColor = getThemeColor(this, android.R.attr.colorBackground)
    (window.sharedElementEnterTransition as? MaterialContainerTransform)?.apply {
        setAllContainerColors(surfaceColor)
        scrimColor = bgColor
        fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
    }
    (window.sharedElementReturnTransition as? MaterialContainerTransform)?.apply {
        setAllContainerColors(surfaceColor)
        scrimColor = bgColor
        fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
    }
}

// 设置内边距
internal fun TextEditorActivity.setupInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
        val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
        binding.toolbar.updatePadding(top = sb.top)
        val bottomPad = maxOf(sb.bottom, ime.bottom)
        binding.symbolBarScroll.updatePadding(bottom = bottomPad)
        insets
    }
}

// 配置编辑器
internal fun TextEditorActivity.setupEditor() {
    binding.toolbar.setBackgroundColor(getThemeColor(this, materialR.attr.colorPrimaryContainer))
    binding.editor.isEditable = true
    binding.editor.setDisplayLnPanel(false)
    binding.editor.isVerticalScrollBarEnabled = true
    binding.editor.isHorizontalScrollBarEnabled = true
    binding.editor.verticalScrollbarThumbDrawable =
        androidx.core.content.ContextCompat.getDrawable(this, R.drawable.widget_scrollbar_thumb)!!
    binding.editor.verticalScrollbarTrackDrawable =
        androidx.core.content.ContextCompat.getDrawable(this, R.drawable.widget_scrollbar_track)!!
    binding.editor.horizontalScrollbarThumbDrawable =
        androidx.core.content.ContextCompat.getDrawable(this, R.drawable.widget_scrollbar_thumb_horizontal)!!
    binding.editor.horizontalScrollbarTrackDrawable =
        androidx.core.content.ContextCompat.getDrawable(this, R.drawable.widget_scrollbar_track_horizontal)!!

    val ext = file.extension.lowercase()
    CodeEditorTextMate.init(applicationContext)
    CodeEditorTextMate.applyTheme(this)
    binding.editor.colorScheme = CodeEditorTextMate.createColorScheme(this)
    CodeEditorTextMate.applyEditorColors(binding.editor, this)
    binding.editor.setEditorLanguage(CodeEditorTextMate.languageForExtension(ext))
}

// 加载文件内容
internal fun TextEditorActivity.loadContent() {
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

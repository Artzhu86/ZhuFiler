package zhu.filer.editor

import android.content.Context
import com.google.android.material.R as materialR
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import zhu.filer.ui.getThemeColor

// 应用编辑器颜色
fun CodeEditorTextMate.applyEditorColors(editor: CodeEditor, context: Context) {
    val scheme = editor.colorScheme
    scheme.setColor(
        EditorColorScheme.LINE_NUMBER_BACKGROUND,
        getThemeColor(context, materialR.attr.colorSurfaceVariant)
    )
    scheme.setColor(
        EditorColorScheme.LINE_DIVIDER,
        getThemeColor(context, materialR.attr.colorOutlineVariant)
    )
    scheme.setColor(
        EditorColorScheme.LINE_NUMBER,
        getThemeColor(context, materialR.attr.colorOnSurfaceVariant)
    )
    scheme.setColor(
        EditorColorScheme.LINE_NUMBER_CURRENT,
        getThemeColor(context, android.R.attr.colorPrimary)
    )
    scheme.setColor(
        EditorColorScheme.WHOLE_BACKGROUND,
        getThemeColor(context, android.R.attr.colorBackground)
    )
    scheme.setColor(
        EditorColorScheme.CURRENT_LINE,
        getThemeColor(context, materialR.attr.colorPrimaryContainer)
    )
    scheme.setColor(
        EditorColorScheme.SELECTION_INSERT,
        getThemeColor(context, android.R.attr.colorPrimary)
    )
    scheme.setColor(
        EditorColorScheme.MATCHED_TEXT_BACKGROUND,
        getThemeColor(context, materialR.attr.colorPrimaryContainer)
    )
    val guideBase = getThemeColor(context, materialR.attr.colorOnSurfaceVariant) and 0x00FFFFFF
    scheme.setColor(EditorColorScheme.BLOCK_LINE, guideBase or 0x33000000)
    scheme.setColor(EditorColorScheme.BLOCK_LINE_CURRENT, guideBase or 0x99000000.toInt())
}

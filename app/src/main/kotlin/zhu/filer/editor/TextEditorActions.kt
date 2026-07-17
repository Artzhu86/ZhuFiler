package zhu.filer.editor

import android.widget.TextView
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import zhu.filer.R
import zhu.filer.ui.applyToolbarTitleName
import zhu.filer.ui.buildDialogTitle
import zhu.filer.ui.getThemeColor
import com.google.android.material.R as materialR

// 更新副标题信息
internal fun TextEditorActivity.updateSubtitle() {
    val cursor = binding.editor.cursor
    val line = cursor.leftLine + 1
    val col = cursor.leftColumn + 1
    val selected = if (cursor.isSelected()) cursor.right - cursor.left else 0
    val selectedTag = if (selected > 0) " ($selected)" else ""
    supportActionBar?.subtitle = "$line:$col$selectedTag"
    applyToolbarTitleName(binding.toolbar, if (isModified) "*${file.name}" else file.name)
}

// 设置符号栏
internal fun TextEditorActivity.setupSymbolBar() {
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
        val tv = layoutInflater.inflate(R.layout.item_symbol_key, container, false) as TextView
        tv.text = display
        tv.setTextColor(textColor)
        val padH = (density * 12).toInt()
        val padV = (density * 4).toInt()
        tv.setPadding(padH, padV, padH, padV)
        tv.setOnClickListener {
            binding.editor.insertText(insert, insert.length)
        }
        container.addView(tv)
    }
}

// 保存文件
internal fun TextEditorActivity.saveFile(): Boolean {
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

// 确认退出
internal fun TextEditorActivity.confirmExit() {
    MaterialAlertDialogBuilder(this)
        .setCustomTitle(buildDialogTitle(this, file.name))
        .setMessage(R.string.unsaved_changes)
        .setPositiveButton(R.string.save_and_exit) { _, _ ->
            if (saveFile()) {
                finishAfterTransition()
            }
        }
        .setNegativeButton(R.string.discard) { _, _ ->
            finishAfterTransition()
        }
        .setCancelable(false)
        .show()
}

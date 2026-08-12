package zhu.filer.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import zhu.filer.R

// 创建文本输入框
fun createInput(context: Context, initial: String = ""): Pair<View, TextInputEditText> {
    val root = LayoutInflater.from(context).inflate(R.layout.dialog_text_input, null)
    val tl = root.findViewById<TextInputLayout>(R.id.dialog_input_layout)
    val et = root.findViewById<TextInputEditText>(R.id.dialog_input_edit)
    et.setText(initial)
    et.setSelection(initial.length)
    return root to et
}

// 创建密码输入框
fun createPasswordInput(context: Context, hint: String): Pair<View, TextInputEditText> {
    val root = LayoutInflater.from(context).inflate(R.layout.dialog_password_input, null)
    val tl = root.findViewById<TextInputLayout>(R.id.dialog_password_layout)
    tl.hint = hint
    val et = root.findViewById<TextInputEditText>(R.id.dialog_password_edit)
    return root to et
}

// 聚焦并显示键盘
fun focusAndShowKeyboard(editText: TextInputEditText, dialog: AlertDialog) {
    editText.requestFocus()
    dialog.window?.apply {
        clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }
    editText.post {
        val imm = editText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }
}

// 获取 selectableItemBackground
private fun getSelectableBackground(context: Context): android.graphics.drawable.Drawable {
    val ta = context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
    val drawable = ta.getDrawable(0)
    ta.recycle()
    return drawable
    ?: android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
}

// 创建可长按复制的文本
fun createCopyableText(
    context: Context,
    text: CharSequence,
    textSizeSp: Float = 14f,
    textColor: Int = getThemeColor(context, com.google.android.material.R.attr.colorOnSurface),
    bold: Boolean = false
): TextView {
    return TextView(context).apply {
        this.text = text
        tag = text.toString()
        setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
        setTextColor(textColor)
        if (bold) {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        isClickable = true
        isLongClickable = true
        isFocusable = true
        background = getSelectableBackground(context)
        val hPad = dpToPx(context, 6)
        val vPad = dpToPx(context, 4)
        setPadding(hPad, vPad, hPad, vPad)
        setOnLongClickListener { v ->
            val value = v.tag as? String ?: ""
            if (value.isNotEmpty()) {
                val clipboard = v.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("text", value))
                Toast.makeText(v.context, v.context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
            }
            true
        }
    }
}

// 统一的水平内边距
private const val HORIZONTAL_PADDING_DP = 16
private const val ROW_VERTICAL_PADDING_DP = 8
private const val LABEL_WIDTH_DP = 88

// 构建属性列表视图（每行左标签右值，长按复制值）
fun buildPropertiesView(
    context: Context,
    properties: List<Pair<String, String>>,
    headerView: View? = null
): View {
    val scrollView = ScrollView(context)
    val container = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }

    // 插入头部视图（如有）
    headerView?.let { container.addView(it) }

    val hPad = dpToPx(context, HORIZONTAL_PADDING_DP)
    val vPad = dpToPx(context, ROW_VERTICAL_PADDING_DP)
    val labelColor = getThemeColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant)
    val valueColor = getThemeColor(context, com.google.android.material.R.attr.colorOnSurface)
    val labelWidth = dpToPx(context, LABEL_WIDTH_DP)

    properties.forEach { (label, value) ->
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(hPad, vPad, hPad, vPad)
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val labelTv = TextView(context).apply {
            text = label
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(labelColor)
            layoutParams = LinearLayout.LayoutParams(labelWidth, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val valueTv = createCopyableText(context, value, textSizeSp = 14f, textColor = valueColor).apply {
            gravity = android.view.Gravity.START
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        row.addView(labelTv)
        row.addView(valueTv)
        container.addView(row)
    }

    scrollView.addView(container)
    return scrollView
}

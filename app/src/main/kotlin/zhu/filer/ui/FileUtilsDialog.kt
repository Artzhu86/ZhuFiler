package zhu.filer.ui

import androidx.appcompat.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import zhu.filer.R

// 构建弹窗标题视图
fun buildDialogTitle(context: Context, title: String, icon: Drawable? = null): View {
    val padding = dpToPx(context, 16)
    val container = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(padding, padding, padding, padding)
    }
    if (icon != null) {
        val iconSize = dpToPx(context, 45)
        val iconMarginEnd = dpToPx(context, 16)
        val iconLp = LinearLayout.LayoutParams(iconSize, iconSize).apply {
            marginEnd = iconMarginEnd
        }
        val iconView = ImageView(context).apply {
            setImageDrawable(icon)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        container.addView(iconView, iconLp)
    }
    val titleLp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
    val titleView = TextView(context).apply {
        text = title
        textSize = 20f
        setTypeface(null, Typeface.BOLD)
        setSingleLine(true)
        ellipsize = android.text.TextUtils.TruncateAt.END
    }
    container.addView(titleView, titleLp)
    return container
}

// 构建弹窗标题视图
fun buildDialogTitle(context: Context, titleRes: Int, icon: Drawable? = null): View =
    buildDialogTitle(context, context.getString(titleRes), icon)

// 显示列表弹窗并修正顶部间距
fun showListDialog(dialog: AlertDialog): AlertDialog {
    dialog.show()
    dialog.listView?.setPadding(
        dialog.listView!!.paddingLeft, 0,
        dialog.listView!!.paddingRight, dialog.listView!!.paddingBottom
    )
    return dialog
}

// 创建对话框容器
fun createDialogContainer(context: Context): LinearLayout {
    return LayoutInflater.from(context)
        .inflate(R.layout.dialog_container, null) as LinearLayout
}

// 创建文本输入框
fun createInput(context: Context, initial: String = ""): Pair<TextInputLayout, TextInputEditText> {
    val tl = LayoutInflater.from(context)
        .inflate(R.layout.dialog_text_input, null) as TextInputLayout
    val et = tl.findViewById<TextInputEditText>(R.id.dialog_input_edit)
    et.setText(initial)
    et.setSelection(initial.length)
    return tl to et
}

// 创建带主题色的单选列表适配器
fun createSingleChoiceAdapter(context: Context, items: Array<String>): ArrayAdapter<String> {
    val primaryColor = getThemeColor(context, android.R.attr.colorPrimary)
    return object : ArrayAdapter<String>(context, android.R.layout.simple_list_item_single_choice, items) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getView(position, convertView, parent)
            val ctv = view.findViewById<android.widget.CheckedTextView>(android.R.id.text1)
            ctv?.checkMarkTintList = android.content.res.ColorStateList.valueOf(primaryColor)
            return view
        }
    }
}

// 创建密码输入框
fun createPasswordInput(context: Context, hint: String): Pair<TextInputLayout, TextInputEditText> {
    val tl = LayoutInflater.from(context)
        .inflate(R.layout.dialog_password_input, null) as TextInputLayout
    tl.hint = hint
    val et = tl.findViewById<TextInputEditText>(R.id.dialog_password_edit)
    return tl to et
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

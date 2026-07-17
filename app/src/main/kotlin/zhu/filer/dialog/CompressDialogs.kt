package zhu.filer.dialog

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.radiobutton.MaterialRadioButton
import zhu.filer.archive.ArchiveFormat
import zhu.filer.archive.stripKnownArchiveExt
import zhu.filer.R
import zhu.filer.ui.buildDialogTitle
import zhu.filer.ui.createDialogContainer
import zhu.filer.ui.createInput
import zhu.filer.ui.createPasswordInput
import zhu.filer.ui.dpToPx
import zhu.filer.ui.focusAndShowKeyboard
import java.io.File

// 显示归档密码对话框
fun showArchivePasswordDialog(activity: AppCompatActivity, onConfirm: (String) -> Unit) {
    val rootLayout = createDialogContainer(activity)
    val (inputLayout, editText) = createPasswordInput(activity, activity.getString(R.string.password))
    rootLayout.addView(inputLayout)

    MaterialAlertDialogBuilder(activity)
        .setCustomTitle(buildDialogTitle(activity, R.string.enter_password))
        .setView(rootLayout)
        .setPositiveButton(R.string.ok) { _, _ ->
            val password = editText.text?.toString() ?: ""
            onConfirm(password)
        }
        .setNegativeButton(R.string.cancel, null)
        .show()
        .let { focusAndShowKeyboard(editText, it) }
}

// 显示压缩对话框
fun showCompressDialog(
    activity: AppCompatActivity,
    sources: List<File>,
    currentDir: File,
    onCompress: (outputFile: File, format: ArchiveFormat, password: String?) -> Unit
) {
    val rootLayout = createDialogContainer(activity)

    val baseName = if (sources.size == 1) {
        sources[0].nameWithoutExtension
    } else {
        currentDir.name
    }
    val defaultName = "$baseName.zip"

    val (nameLayout, nameEdit) = createInput(activity, defaultName)
    rootLayout.addView(nameLayout)

    val dotIndex = defaultName.lastIndexOf('.')
    if (dotIndex > 0) {
        nameEdit.setSelection(0, dotIndex)
    }

    val radioGroup = RadioGroup(activity).apply {
        orientation = RadioGroup.HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dpToPx(activity, 8)
        }
    }
    data class RadioOpt(val format: ArchiveFormat, val radio: MaterialRadioButton)
    val opts = listOf("zip", "7z", "tar.gz", "tar.xz").map { label ->
        val radio = activity.layoutInflater.inflate(R.layout.item_compress_format, radioGroup, false) as MaterialRadioButton
        radio.text = label
        radio.id = View.generateViewId()
        if (label == "zip") radio.isChecked = true
        radioGroup.addView(radio)
        RadioOpt(ArchiveFormat.entries.first { it.extension == label }, radio)
    }
    rootLayout.addView(radioGroup)

    val (pwdLayout, pwdEdit) = createPasswordInput(activity, activity.getString(R.string.password))
    rootLayout.addView(pwdLayout)

    radioGroup.setOnCheckedChangeListener { _, checkedId ->
        val opt = opts.first { it.radio.id == checkedId }
        val currentName = nameEdit.text?.toString() ?: ""
        val stripped = stripKnownArchiveExt(currentName)
        val newName = "$stripped.${opt.format.extension}"
        nameEdit.setText(newName)
        nameEdit.setSelection(0, stripped.length)
        val supportsPassword = opt.format == ArchiveFormat.SEVEN_ZIP || opt.format == ArchiveFormat.ZIP
        pwdLayout.isEnabled = supportsPassword
        if (!supportsPassword) pwdEdit.setText("")
    }

    MaterialAlertDialogBuilder(activity)
        .setCustomTitle(buildDialogTitle(activity, R.string.create_archive))
        .setView(rootLayout)
        .setPositiveButton(R.string.ok) { _, _ ->
            val name = nameEdit.text?.toString()?.trim() ?: ""
            val password = pwdEdit.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            val format = opts.first { it.radio.isChecked }.format
            val outputFile = File(currentDir, name)
            onCompress(outputFile, format, password)
        }
        .setNegativeButton(R.string.cancel, null)
        .show()
        .let { focusAndShowKeyboard(nameEdit, it) }
}

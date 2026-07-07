package zhu.filer

import android.content.res.ColorStateList
import android.graphics.drawable.RippleDrawable
import android.os.Handler
import android.os.Looper
import android.text.format.Formatter
import android.view.View
import android.view.ViewGroup
import android.widget.CheckedTextView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

fun showNavigateDialog(activity: AppCompatActivity, currentPath: String, loadDir: suspend (File) -> Unit, prefs: android.content.SharedPreferences) {
    val rootLayout = createDialogContainer(activity)
    val (inputLayout, editText) = createInput(activity, currentPath)
    rootLayout.addView(inputLayout)

    lateinit var dialog: AlertDialog
    val builder = MaterialAlertDialogBuilder(activity)
        .setTitle(R.string.working_directory)
        .setView(rootLayout)
        .setPositiveButton(R.string.action_switch) { _, _ ->
            val path = editText.text?.toString()?.trim() ?: ""
            if (path.isNotEmpty()) {
                val targetDir = File(path)
                if (targetDir.exists() && targetDir.isDirectory) {
                    activity.lifecycleScope.launch { loadDir(targetDir) }
                } else {
                    toast(activity, activity.getString(R.string.directory_invalid))
                }
            }
        }
        .setNeutralButton(R.string.recent) { _, _ ->
            val recent = getRecentDirs(prefs)
            val files = recent.map { File(it) }
            val items = files.map { file ->
                FileItem(file, file.name, R.drawable.outline_folder_24, file.absolutePath)
            }

            lateinit var recentDialog: AlertDialog

            val rv = RecyclerView(activity).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(activity, 400))
                layoutManager = LinearLayoutManager(activity)
                adapter = FileListAdapter(
                    onItemClick = { file, _ ->
                        recentDialog.dismiss()
                        Handler(Looper.getMainLooper()).postDelayed({
                            activity.lifecycleScope.launch { loadDir(file) }
                        }, activity.resources.getInteger(R.integer.click_delay_ms).toLong())
                    },
                    onItemLongClick = { _, _ -> false }
                ).apply { submitList(items) }
            }

            recentDialog = MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.recent)
                .setView(rv)
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
        .setNegativeButton(R.string.cancel, null)
    dialog = builder.show()
    focusAndShowKeyboard(editText, dialog)
    editText.post { editText.selectAll() }
}

fun showCreate(activity: AppCompatActivity, currentDir: File, loadDir: suspend (File, String?) -> Unit) {
    val rootLayout = createDialogContainer(activity)
    val (inputLayout, edit) = createInput(activity)
    rootLayout.addView(inputLayout)

    MaterialAlertDialogBuilder(activity)
        .setTitle(R.string.create)
        .setView(rootLayout)
        .setPositiveButton(R.string.file) { _, _ ->
            val name = edit.text?.toString()?.trim() ?: ""
            if (isValid(name)) {
                val f = File(currentDir, name)
                activity.lifecycleScope.launch {
                    val ok = withContext(Dispatchers.IO) { f.createNewFile() }
                    if (!ok) toast(activity, activity.getString(R.string.create_failed))
                    if (ok) loadDir(currentDir, f.absolutePath)
                }
            } else toast(activity, activity.getString(R.string.invalid_characters))
        }
        .setNegativeButton(R.string.directory) { _, _ ->
            val name = edit.text?.toString()?.trim() ?: ""
            if (isValid(name)) {
                val d = File(currentDir, name)
                activity.lifecycleScope.launch {
                    val ok = withContext(Dispatchers.IO) { d.mkdir() }
                    if (!ok) toast(activity, activity.getString(R.string.create_failed))
                    if (ok) loadDir(currentDir, d.absolutePath)
                }
            } else toast(activity, activity.getString(R.string.invalid_characters))
        }
        .setNeutralButton(R.string.cancel, null)
        .show()
        .let { focusAndShowKeyboard(edit, it) }
}

fun showRenameDialog(activity: AppCompatActivity, currentDir: File, loadDir: suspend (File) -> Unit, file: File) {
    val oldName = file.name
    val rootLayout = createDialogContainer(activity)
    val (inputLayout, editText) = createInput(activity, oldName)
    rootLayout.addView(inputLayout)

    val dotIndex = oldName.lastIndexOf('.')
    if (dotIndex > 0 && !file.isDirectory) {
        editText.setSelection(0, dotIndex)
    } else {
        editText.selectAll()
    }

    MaterialAlertDialogBuilder(activity)
        .setTitle(R.string.rename)
        .setView(rootLayout)
        .setPositiveButton(R.string.ok) { _, _ ->
            val newName = editText.text?.toString()?.trim() ?: ""
            when {
                newName.isEmpty() -> toast(activity, activity.getString(R.string.name_cannot_be_empty))
                newName == oldName -> toast(activity, activity.getString(R.string.name_unchanged))
                !isValid(newName) -> toast(activity, activity.getString(R.string.invalid_characters))
                else -> {
                    val parent = file.parent ?: run {
                        toast(activity, activity.getString(R.string.rename_failed))
                        return@setPositiveButton
                    }
                    val newFile = File(parent, newName)
                    if (newFile.exists()) {
                        toast(activity, activity.getString(R.string.file_exists))
                        return@setPositiveButton
                    }
                    activity.lifecycleScope.launch {
                        val ok = withContext(Dispatchers.IO) { file.renameTo(newFile) }
                        if (ok) {
                            toast(activity, activity.getString(R.string.rename_success))
                            loadDir(currentDir)
                        } else {
                            toast(activity, activity.getString(R.string.rename_failed))
                        }
                    }
                }
            }
        }
        .setNegativeButton(R.string.cancel, null)
        .show()
        .let { focusAndShowKeyboard(editText, it) }
}

fun showOps(
    activity: AppCompatActivity,
    currentDir: File,
    loadDir: suspend (File) -> Unit,
    file: File,
    onCopyCut: (File, Boolean) -> Unit = { _, _ -> },
    onBookmarkToggle: ((String) -> Unit)? = null,
    isBookmarked: Boolean = false,
    onOpenArchive: ((File) -> Unit)? = null,
    onCompress: ((File) -> Unit)? = null
) {
    val items = mutableListOf(
        activity.getString(R.string.copy),
        activity.getString(R.string.move),
        activity.getString(R.string.rename),
        activity.getString(R.string.delete)
    )
    if (!file.isDirectory) {
        items.add(activity.getString(R.string.open_with))
        items.add(activity.getString(R.string.share))
    }
    if (file.isDirectory && onBookmarkToggle != null) {
        val bookmarkActionRes = if (isBookmarked) R.string.remove_bookmark else R.string.add_bookmark
        items.add(activity.getString(bookmarkActionRes))
    }
    items.add(activity.getString(R.string.compress))
    items.add(activity.getString(R.string.properties))

    val dialog = MaterialAlertDialogBuilder(activity)
        .setTitle(file.name)
        .setItems(items.toTypedArray()) { _, which ->
            val action = items[which]
            when (action) {
                activity.getString(R.string.rename) -> showRenameDialog(activity, currentDir, loadDir, file)
                activity.getString(R.string.copy) -> onCopyCut(file, false)
                activity.getString(R.string.move) -> onCopyCut(file, true)
                activity.getString(R.string.delete) -> {
                    MaterialAlertDialogBuilder(activity).setTitle(R.string.confirm_delete)
                        .setMessage(activity.getString(R.string.delete_message, file.name))
                        .setPositiveButton(R.string.delete) { _, _ ->
                            activity.lifecycleScope.launch {
                                val ok = withContext(Dispatchers.IO) { if (file.isDirectory) file.deleteRecursively() else file.delete() }
                                if (!ok) toast(activity, activity.getString(R.string.delete_failed))
                                if (ok) loadDir(currentDir)
                            }
                        }.setNegativeButton(R.string.cancel, null).show()
                }
                activity.getString(R.string.open_with) -> previewFile(activity, file, forceChoose = true, onOpenArchive = onOpenArchive)
                activity.getString(R.string.share) -> shareFile(activity, file)
                activity.getString(R.string.add_bookmark), activity.getString(R.string.remove_bookmark) -> onBookmarkToggle?.invoke(file.absolutePath)
                activity.getString(R.string.compress) -> onCompress?.invoke(file)
                activity.getString(R.string.properties) -> showDetails(activity, file)
            }
        }
        .setNegativeButton(R.string.cancel, null)
        .create()
    dialog.show()
}

fun showArchiveItemOps(
    activity: AppCompatActivity,
    item: FileItem,
    cachedPassword: String?,
    onExtract: (FileItem, String?) -> Unit,
    onCachePassword: (String) -> Unit
) {
    val items = mutableListOf(activity.getString(R.string.properties))
    if (!item.isDirectory) {
        items.add(0, activity.getString(R.string.open_with))
    }
    val dialog = MaterialAlertDialogBuilder(activity)
        .setTitle(item.displayName)
        .setItems(items.toTypedArray()) { _, which ->
            val action = items[which]
            when (action) {
                activity.getString(R.string.open_with) -> {
                    if (item.encrypted) {
                        if (cachedPassword != null) {
                            onExtract(item, cachedPassword)
                        } else {
                            showArchivePasswordDialog(activity) { password ->
                                onCachePassword(password)
                                onExtract(item, password)
                            }
                        }
                    } else {
                        onExtract(item, null)
                    }
                }
                activity.getString(R.string.properties) -> showArchiveEntryDetails(activity, item)
            }
        }
        .setNegativeButton(R.string.cancel, null)
        .create()
    dialog.show()
    dialog.listView?.let { applySelectableEffectToListView(it) }
}

fun showArchiveEntryDetails(activity: AppCompatActivity, item: FileItem) {
    val sizeStr = Formatter.formatFileSize(activity, item.size)
    val dialog = MaterialAlertDialogBuilder(activity)
        .setTitle(item.displayName)
        .setMessage(
            "${activity.getString(R.string.name_label)}: ${item.displayName}\n" +
            "${activity.getString(R.string.type_label)}: ${if (item.isDirectory) activity.getString(R.string.directory) else activity.getString(R.string.file)}\n" +
            "${activity.getString(R.string.size_label)}: $sizeStr"
        )
        .setPositiveButton(R.string.close, null)
        .create()
    dialog.show()
}

fun applySelectableEffectToListView(listView: ListView) {
    for (i in 0 until listView.childCount) {
        val child = listView.getChildAt(i)
        child?.applySelectableEffect()
    }
    listView.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
        override fun onChildViewAdded(parent: View, child: View) = child.applySelectableEffect()
        override fun onChildViewRemoved(parent: View, child: View) {}
    })
}

fun applySingleChoiceColors(listView: ListView) {
    val primaryColor = getThemeColor(listView.context, com.google.android.material.R.attr.colorPrimary)
    val colorStateList = ColorStateList.valueOf(primaryColor)
    val apply: (View) -> Unit = { child ->
        if (child is CheckedTextView) {
            child.checkMarkTintList = colorStateList
        }
    }
    for (i in 0 until listView.childCount) {
        apply(listView.getChildAt(i))
    }
    listView.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
        override fun onChildViewAdded(parent: View, child: View) {
            apply(child)
            child.applySelectableEffect()
        }
        override fun onChildViewRemoved(parent: View, child: View) {}
    })
}

fun View.applySelectableEffect() {
    val highlightColor = getThemeColor(context, android.R.attr.colorControlHighlight)
    val ripple = RippleDrawable(ColorStateList.valueOf(highlightColor), null, null)
    foreground = ripple
}

fun showArchivePasswordDialog(activity: AppCompatActivity, onConfirm: (String) -> Unit) {
    val rootLayout = createDialogContainer(activity)
    val (inputLayout, editText) = createPasswordInput(activity, activity.getString(R.string.password))
    rootLayout.addView(inputLayout)

    MaterialAlertDialogBuilder(activity)
        .setTitle(R.string.enter_password)
        .setView(rootLayout)
        .setPositiveButton(R.string.ok) { _, _ ->
            val password = editText.text?.toString() ?: ""
            onConfirm(password)
        }
        .setNegativeButton(R.string.cancel, null)
        .show()
        .let { focusAndShowKeyboard(editText, it) }
}

fun showCompressDialog(
    activity: AppCompatActivity,
    sources: List<File>,
    currentDir: File,
    onCompress: (outputFile: File, format: CompressFormat, password: String?) -> Unit
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
    data class RadioOpt(val format: CompressFormat, val radio: com.google.android.material.radiobutton.MaterialRadioButton)
    val opts = listOf("zip", "7z", "tar.gz", "tar.xz").map { label ->
        val radio = com.google.android.material.radiobutton.MaterialRadioButton(activity).apply {
            text = label
            id = View.generateViewId()
            setSingleLine(true)
            layoutParams = RadioGroup.LayoutParams(
                RadioGroup.LayoutParams.WRAP_CONTENT,
                RadioGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        if (label == "zip") radio.isChecked = true
        radioGroup.addView(radio)
        RadioOpt(CompressFormat.entries.first { it.extension == label }, radio)
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
        val supportsPassword = opt.format == CompressFormat.SEVEN_ZIP || opt.format == CompressFormat.ZIP
        pwdLayout.isEnabled = supportsPassword
        if (!supportsPassword) pwdEdit.setText("")
    }

    MaterialAlertDialogBuilder(activity)
        .setTitle(R.string.create_archive)
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

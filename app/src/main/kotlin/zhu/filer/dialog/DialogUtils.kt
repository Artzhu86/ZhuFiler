package zhu.filer.dialog

import android.content.res.ColorStateList
import android.graphics.drawable.RippleDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zhu.filer.archive.ArchiveEngine
import zhu.filer.archive.extractEntry
import zhu.filer.archive.extractEntryToDir
import zhu.filer.FileItem
import zhu.filer.R
import zhu.filer.ui.getThemeColor
import zhu.filer.operation.FileOpener
import zhu.filer.util.shareFile
import zhu.filer.util.toast
import java.io.File

// 应用列表可选效果
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

// 应用可选效果
fun View.applySelectableEffect() {
    val highlightColor = getThemeColor(context, android.R.attr.colorControlHighlight)
    val ripple = RippleDrawable(ColorStateList.valueOf(highlightColor), null, null)
    foreground = ripple
}

// 解压归档条目到临时目录
fun extractArchiveItemToTemp(
    activity: AppCompatActivity,
    item: FileItem,
    fileOpener: FileOpener
): File? {
    val archiveFile = fileOpener.getArchiveFile() ?: return null
    val password = fileOpener.getArchivePassword()
    val entryPath = item.entryPath ?: return null
    val tempDir = File(activity.cacheDir, "archive_clipboard")
    tempDir.mkdirs()
    return try {
        if (item.isDirectory) {
            val destDir = File(tempDir, item.displayName)
            if (destDir.exists()) destDir.deleteRecursively()
            destDir.mkdirs()
            ArchiveEngine.extractEntryToDir(archiveFile, entryPath, true, password, destDir)
            destDir
        } else {
            val destFile = File(tempDir, item.displayName)
            if (destFile.exists()) destFile.delete()
            ArchiveEngine.extractEntry(archiveFile, entryPath, password, destFile)
            destFile
        }
    } catch (e: Exception) {
        null
    }
}

// 分享归档条目
fun shareArchiveEntry(
    activity: AppCompatActivity,
    item: FileItem,
    fileOpener: FileOpener
) {
    activity.lifecycleScope.launch {
        val tempFile = withContext(Dispatchers.IO) {
            extractArchiveItemToTemp(activity, item, fileOpener)
        }
        if (tempFile != null && tempFile.exists()) {
            shareFile(activity, tempFile)
        } else {
            toast(activity, activity.getString(R.string.extract_failed))
        }
    }
}

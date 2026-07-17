package zhu.filer.settings

import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.LinkedList
import java.util.Queue
import zhu.filer.FileItem
import zhu.filer.browser.FileListAdapter
import zhu.filer.R
import zhu.filer.util.ShizukuManager
import zhu.filer.ui.buildDialogTitle
import zhu.filer.util.listFilesWithDetails
import zhu.filer.util.createFileItem
import com.google.android.material.R as materialR

// 执行搜索
internal fun SearchHelper.performSearch(query: String, recursive: Boolean = true) {
    searchJob?.cancel()
    resultItems.clear()

    val container = LayoutInflater.from(activity).inflate(R.layout.dialog_search_progress, null)
    val dirView = container.findViewById<TextView>(R.id.search_progress_dir)
    val progressBar = container.findViewById<LinearProgressIndicator>(R.id.search_progress_bar)
    dirView.setTextColor(MaterialColors.getColor(dirView, materialR.attr.colorOnSurfaceVariant, 0xFF888888.toInt()))
    dirView.setTextIsSelectable(false)
    progressBar.show()

    val progressDialog = MaterialAlertDialogBuilder(activity)
        .setCustomTitle(buildDialogTitle(activity, R.string.searching))
        .setView(container)
        .setCancelable(false)
        .setPositiveButton(R.string.stop) { _, _ ->
            searchJob?.cancel()
        }
        .create()

    progressDialog.show()

    searchJob = activity.lifecycleScope.launch(Dispatchers.IO) {
        val queue: Queue<File> = LinkedList()
        queue.add(currentDir())

        try {
            while (isActive && queue.isNotEmpty()) {
                val dir = queue.poll() ?: continue

                withContext(Dispatchers.Main) {
                    if (progressDialog.isShowing) {
                        dirView.text = dir.absolutePath
                    }
                }

                val files = if (ShizukuManager.hasPermission()) {
                    ShizukuManager.listFilesWithDetails(dir.absolutePath)
                        ?.map { File(dir, it.name) }
                } else {
                    dir.listFiles()?.toList()
                }

                if (files == null) continue

                for (file in files) {
                    if (!isActive) break

                    if (query.isEmpty() || file.name.contains(query, ignoreCase = true)) {
                        val item = createFileItem(activity, file)
                        resultItems.add(item)
                    }

                    if (file.isDirectory && recursive) queue.add(file)
                }
            }
        } finally {
            withContext(NonCancellable + Dispatchers.Main) {
                if (progressDialog.isShowing) progressDialog.dismiss()
                showSearchResult(resultItems.toList())
            }
        }
    }
}

// 显示搜索结果
internal fun SearchHelper.showSearchResult(items: List<FileItem>) {
    lastResultItems = items

    val root = LayoutInflater.from(activity).inflate(R.layout.dialog_search_result, null)
    val listContainer = root.findViewById<FrameLayout>(R.id.search_result_container)
    rv = listContainer.findViewById<RecyclerView>(R.id.search_result_list)
    emptyHint = listContainer.findViewById<TextView>(R.id.search_result_empty)
    emptyHint.text = activity.getString(R.string.no_search_result)
    rv.layoutManager = LinearLayoutManager(activity)

    adapter = FileListAdapter(
        onItemClick = { file, _, _ ->
            resultDialog?.dismiss()
            if (file.isDirectory) activity.lifecycleScope.launch { loadDir(file) }
            else locateFile(file)
        },
        onItemLongClick = { _, _ -> false }
    )
    rv.adapter = adapter

    if (items.isEmpty()) {
        emptyHint.isVisible = true
        rv.isVisible = false
    } else {
        emptyHint.isVisible = false
        rv.isVisible = true
        adapter.submitList(items)
    }

    resultDialog = MaterialAlertDialogBuilder(activity)
        .setCustomTitle(buildDialogTitle(activity, activity.getString(R.string.search_result_count, items.size)))
        .setView(root)
        .setNegativeButton(R.string.close, null)
        .show()
}

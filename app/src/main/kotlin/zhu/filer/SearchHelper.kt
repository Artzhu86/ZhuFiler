package zhu.filer

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.*
import java.io.File
import java.util.*
import com.google.android.material.R as materialR

// 文件搜索辅助类
class SearchHelper(
    private val activity: AppCompatActivity,
    private val currentDir: () -> File,
    private val loadDir: suspend (File) -> Unit,
    private val locateFile: (File) -> Unit
) {

    private var resultDialog: Dialog? = null
    private var searchJob: Job? = null
    private var lastResultItems: List<FileItem>? = null

    private lateinit var rv: RecyclerView
    private lateinit var adapter: FileListAdapter
    private lateinit var emptyHint: TextView
    private val resultItems = mutableListOf<FileItem>()

    // 显示搜索对话框
    fun showSearchDialog() {
        val rootLayout = LayoutInflater.from(activity).inflate(R.layout.dialog_search, null)

        val inputLayout = rootLayout.findViewById<TextInputLayout>(R.id.search_input_layout)
        val editText = inputLayout.findViewById<TextInputEditText>(R.id.search_input_edit)
        val checkBox = rootLayout.findViewById<AppCompatCheckBox>(R.id.search_subdir_checkbox)
        checkBox.text = activity.getString(R.string.subdirectory_search)

        val searchDialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.search)
            .setView(rootLayout)
            .setPositiveButton(R.string.ok) { _, _ ->
                val query = editText.text?.toString()?.trim() ?: ""
                val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(editText.windowToken, 0)
                performSearch(query, checkBox.isChecked)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()

        focusAndShowKeyboard(editText, searchDialog)
        editText.selectAll()
    }

    // 执行搜索
    private fun performSearch(query: String, recursive: Boolean = true) {
        searchJob?.cancel()
        resultItems.clear()

        val container = LayoutInflater.from(activity).inflate(R.layout.dialog_search_progress, null)
        val dirView = container.findViewById<TextView>(R.id.search_progress_dir)
        val progressBar = container.findViewById<LinearProgressIndicator>(R.id.search_progress_bar)
        dirView.setTextColor(MaterialColors.getColor(dirView, materialR.attr.colorOnSurfaceVariant, 0xFF888888.toInt()))
        dirView.setTextIsSelectable(false)
        progressBar.show()

        val progressDialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.searching)
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

                    val files = dir.listFiles() ?: continue

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
    private fun showSearchResult(items: List<FileItem>) {
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
            .setTitle(activity.getString(R.string.search_result_count, items.size))
            .setView(root)
            .setNegativeButton(R.string.close, null)
            .show()
    }

    // 显示上次搜索结果
    fun showLastSearchResult() {
        if (lastResultItems.isNullOrEmpty()) {
            toast(activity, activity.getString(R.string.no_search_result))
            return
        }
        showSearchResult(lastResultItems!!)
    }

    // 关闭搜索
    fun dismiss() {
        searchJob?.cancel()
        resultDialog?.dismiss()
    }
}

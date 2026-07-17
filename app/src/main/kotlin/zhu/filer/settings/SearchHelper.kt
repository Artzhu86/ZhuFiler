package zhu.filer.settings

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Job
import java.io.File
import zhu.filer.FileItem
import zhu.filer.browser.FileListAdapter
import zhu.filer.R
import zhu.filer.ui.buildDialogTitle
import zhu.filer.ui.focusAndShowKeyboard
import zhu.filer.util.toast

// 文件搜索辅助类
class SearchHelper(
    internal val activity: AppCompatActivity,
    internal val currentDir: () -> File,
    internal val loadDir: suspend (File) -> Unit,
    internal val locateFile: (File) -> Unit
) {

    internal var resultDialog: Dialog? = null
    internal var searchJob: Job? = null
    internal var lastResultItems: List<FileItem>? = null

    internal lateinit var rv: RecyclerView
    internal lateinit var adapter: FileListAdapter
    internal lateinit var emptyHint: TextView
    internal val resultItems = mutableListOf<FileItem>()

    // 显示搜索对话框
    fun showSearchDialog() {
        val rootLayout = LayoutInflater.from(activity).inflate(R.layout.dialog_search, null)

        val inputLayout = rootLayout.findViewById<TextInputLayout>(R.id.search_input_layout)
        val editText = inputLayout.findViewById<TextInputEditText>(R.id.search_input_edit)
        val checkBox = rootLayout.findViewById<AppCompatCheckBox>(R.id.search_subdir_checkbox)
        checkBox.text = activity.getString(R.string.subdirectory_search)

        val searchDialog = MaterialAlertDialogBuilder(activity)
            .setCustomTitle(buildDialogTitle(activity, R.string.search))
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

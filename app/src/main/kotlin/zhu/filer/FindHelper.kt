package zhu.filer

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.text.format.Formatter
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.*
import java.io.File
import java.util.*

class FindHelper(
    private val activity: AppCompatActivity,
    private val currentDir: () -> File,
    private val loadDir: suspend (File) -> Unit,
    private val locateFile: (File) -> Unit
) {

    private var dialog: Dialog? = null
    private var searchJob: Job? = null
    private var lastQuery: String? = null
    private var lastResultItems: List<FileItem>? = null
    private var lastFoundCount: Int = 0
    private var hasResult = false

    private lateinit var rv: RecyclerView
    private lateinit var adapter: FileListAdapter
    private lateinit var progress: ProgressBar
    private lateinit var doneIcon: TextView
    private lateinit var countText: TextView
    private lateinit var stopButton: MaterialButton
    private lateinit var emptyHint: TextView
    private lateinit var listContainer: FrameLayout
    private val resultItems = mutableListOf<FileItem>()

    fun showSearchDialog() {
        val rootLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(activity, 16), dpToPx(activity, 16), dpToPx(activity, 16), 0)
        }

        val (inputLayout, editText) = createInput(activity, lastQuery ?: "")
        rootLayout.addView(inputLayout)

        val checkBox = AppCompatCheckBox(activity).apply {
            text = "搜索子目录"
            isChecked = true
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        rootLayout.addView(checkBox)

        val searchDialog = MaterialAlertDialogBuilder(activity)
            .setTitle("查找")
            .setView(rootLayout)
            .setPositiveButton("确定") { _, _ ->
                val query = editText.text?.toString()?.trim() ?: ""
                val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(editText.windowToken, 0)
                Handler(Looper.getMainLooper()).postDelayed({
                    performSearch(query, checkBox.isChecked)
                }, 80)
            }
            .setNegativeButton("取消", null)
            .show()

        focusAndShowKeyboard(editText, searchDialog)
        editText.selectAll()
    }

    private fun performSearch(query: String, recursive: Boolean = true) {
        searchJob?.cancel()
        lastQuery = query
        hasResult = false
        lastResultItems = null
        lastFoundCount = 0
        resultItems.clear()

        if (dialog == null || dialog?.isShowing != true) {
            buildResultDialog()
        }

        dialog?.setTitle("查找结果")
        dialog?.setCancelable(false)

        progress.visibility = View.VISIBLE
        doneIcon.visibility = View.INVISIBLE
        countText.text = "已找到 0 个"
        countText.visibility = View.VISIBLE
        stopButton.visibility = View.VISIBLE
        stopButton.text = "结束"
        stopButton.setBackgroundColor(getThemeColor(activity, android.R.attr.colorPrimary))
        stopButton.setTextColor(Color.WHITE)
        stopButton.strokeWidth = 0
        stopButton.isEnabled = true
        stopButton.alpha = 1f
        rv.isVisible = false
        emptyHint.isVisible = false
        adapter.submitList(emptyList())

        searchJob = activity.lifecycleScope.launch(Dispatchers.IO) {
            var foundCount = 0
            val queue: Queue<File> = LinkedList()
            queue.add(currentDir())

            while (isActive && queue.isNotEmpty()) {
                val dir = queue.poll() ?: continue
                val files = dir.listFiles() ?: continue

                for (file in files) {
                    if (!isActive) break

                    if (query.isEmpty() || file.name.contains(query, ignoreCase = true)) {
                        val timeStr = DATE_FORMAT.format(Date(file.lastModified()))
                        val subtitle = if (file.isDirectory) timeStr else "$timeStr  ${Formatter.formatFileSize(activity, file.length())}"
                        val icon = if (file.isDirectory) "📁" else "📄"

                        foundCount++
                        val item = FileItem(file, file.name, icon, subtitle)
                        resultItems.add(item)

                        lastResultItems = resultItems.toList()
                        lastFoundCount = foundCount
                        hasResult = true

                        withContext(Dispatchers.Main) {
                            if (!isActive || dialog?.isShowing != true) return@withContext
                            countText.text = "已找到 $foundCount 个"
                            adapter.addItem(item)
                            rv.isVisible = true
                        }
                    }

                    if (file.isDirectory && recursive) queue.add(file)
                }
            }

            withContext(Dispatchers.Main) {
                if (!isActive || dialog?.isShowing != true) return@withContext
                progress.visibility = View.INVISIBLE
                doneIcon.visibility = View.VISIBLE
                stopButton.text = "已结束"
                stopButton.setBackgroundColor(Color.TRANSPARENT)
                val colorStateList = ColorStateList.valueOf(getThemeColor(activity, android.R.attr.colorPrimary))
                stopButton.setTextColor(colorStateList)
                stopButton.strokeWidth = dpToPx(activity, 1)
                stopButton.strokeColor = colorStateList
                stopButton.isEnabled = false
                stopButton.alpha = 0.6f

                if (foundCount == 0) {
                    emptyHint.isVisible = true
                    rv.isVisible = false
                    hasResult = false
                    lastResultItems = null
                    lastFoundCount = 0
                }
                dialog?.setCancelable(true)
            }
        }
    }

    private fun buildResultDialog() {
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val topRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(activity, 8), 0, dpToPx(activity, 8))
        }

        val iconContainer = FrameLayout(activity).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(activity, 32), dpToPx(activity, 32)).apply {
                leftMargin = dpToPx(activity, 16)
            }
        }

        progress = ProgressBar(activity).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            visibility = View.INVISIBLE
        }
        iconContainer.addView(progress)

        doneIcon = TextView(activity).apply {
            text = "✓"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(getThemeColor(activity, android.R.attr.colorPrimary))
            visibility = View.INVISIBLE
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        iconContainer.addView(doneIcon)

        topRow.addView(iconContainer)

        countText = TextView(activity).apply {
            textSize = 14f
            text = "已找到 0 个"
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                leftMargin = dpToPx(activity, 12)
                weight = 1f
            }
            layoutParams = lp
            visibility = View.GONE
        }
        topRow.addView(countText)

        val primaryColor = getThemeColor(activity, android.R.attr.colorPrimary)
        val colorStateList = ColorStateList.valueOf(primaryColor)

        stopButton = MaterialButton(activity).apply {
            text = "结束"
            textSize = 14f
            visibility = View.GONE
            setBackgroundColor(primaryColor)
            setTextColor(Color.WHITE)
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.END
                rightMargin = dpToPx(activity, 16)
            }
            layoutParams = lp

            setOnClickListener {
                searchJob?.cancel()
                progress.visibility = View.INVISIBLE
                doneIcon.visibility = View.VISIBLE
                text = "已结束"
                setBackgroundColor(Color.TRANSPARENT)
                setTextColor(colorStateList)
                strokeWidth = dpToPx(activity, 1)
                strokeColor = colorStateList
                isEnabled = false
                alpha = 0.6f
                dialog?.setCancelable(true)
            }
        }
        topRow.addView(stopButton)

        root.addView(topRow)

        listContainer = FrameLayout(activity).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(activity, 400))
        }

        rv = RecyclerView(activity).apply {
            layoutManager = LinearLayoutManager(activity)
            visibility = View.GONE
        }
        listContainer.addView(rv)

        emptyHint = TextView(activity).apply {
            text = "未找到匹配内容"
            textSize = 16f
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        listContainer.addView(emptyHint)

        root.addView(listContainer)

        adapter = FileListAdapter(
            onItemClick = { file, _ ->
                searchJob?.cancel()
                dialog?.dismiss()
                if (file.isDirectory) activity.lifecycleScope.launch { loadDir(file) }
                else locateFile(file)
            },
            onItemLongClick = { _, _ -> false }
        )
        rv.adapter = adapter

        dialog = MaterialAlertDialogBuilder(activity)
            .setTitle("查找结果")
            .setView(root)
            .setNegativeButton("关闭", null)
            .show()
    }

    fun showLastResult() {
        if (!hasResult || lastResultItems.isNullOrEmpty()) {
            toast(activity, "暂无查找结果")
            return
        }

        if (dialog == null || dialog?.isShowing != true) {
            buildResultDialog()
        }

        dialog?.setTitle("查找结果")
        dialog?.setCancelable(true)

        progress.visibility = View.INVISIBLE
        doneIcon.visibility = View.VISIBLE
        countText.text = "已找到 $lastFoundCount 个"
        countText.visibility = View.VISIBLE
        stopButton.visibility = View.GONE
        emptyHint.isVisible = false
        rv.isVisible = true
        adapter.submitList(lastResultItems!!)
    }

    fun dismiss() {
        searchJob?.cancel()
        dialog?.dismiss()
    }
}
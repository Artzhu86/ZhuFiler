package zhu.filer.editor

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.skydoves.transformationlayout.TransformationLayout
import com.skydoves.transformationlayout.onTransformationEndContainer
import java.io.File
import zhu.filer.util.EXTRA_FILE_PATH
import zhu.filer.R
import zhu.filer.util.TRANSITION_PARAMS_KEY
import zhu.filer.ui.ThemeHelper
import zhu.filer.ui.applyToolbarTitleName
import zhu.filer.ui.refreshToolbarTitle
import zhu.filer.databinding.ActivityTextEditorBinding

// 文本编辑器界面
class TextEditorActivity : AppCompatActivity() {

    internal lateinit var binding: ActivityTextEditorBinding
    internal lateinit var file: File
    internal var isModified = false
    internal var initialContent: String = ""

    // 初始化界面
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyThemeColor(this)
        val params = intent.getParcelableExtra<TransformationLayout.Params>(TRANSITION_PARAMS_KEY)
        if (params != null) {
            onTransformationEndContainer(params)
        }
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityTextEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTransition(params)

        val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
        val f = filePath?.let { File(it) }
        if (f == null || !f.canRead()) {
            finish()
            return
        }
        file = f

        setupInsets()
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = file.name
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finishAfterTransition()
        }
        applyToolbarTitleName(binding.toolbar, file.name)
        updateSubtitle()
        setupEditor()
        setupSymbolBar()
        loadContent()
    }

    // 创建菜单选项
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, getString(R.string.save))
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(Menu.NONE, Menu.FIRST + 1, Menu.NONE, getString(R.string.undo))
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(Menu.NONE, Menu.FIRST + 2, Menu.NONE, getString(R.string.redo))
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        return true
    }

    // 处理菜单点击
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            Menu.FIRST -> saveFile()
            Menu.FIRST + 1 -> binding.editor.undo()
            Menu.FIRST + 2 -> binding.editor.redo()
            android.R.id.home -> {
                finishAfterTransition()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    @Deprecated("Deprecated in Java")
    // 处理返回按键
    override fun onBackPressed() {
        if (isModified) confirmExit() else finishAfterTransition()
    }

    // 恢复时刷新标题
    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) {
            refreshToolbarTitle(binding.toolbar)
        }
    }

    // 窗口焦点变化时刷新
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && ::binding.isInitialized) {
            refreshToolbarTitle(binding.toolbar)
        }
    }

    // 销毁时释放资源
    override fun onDestroy() {
        super.onDestroy()
        if (::binding.isInitialized) {
            binding.editor.release()
        }
    }
}

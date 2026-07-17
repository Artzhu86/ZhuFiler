package zhu.filer.media

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
import zhu.filer.ui.getThemeColor
import zhu.filer.ui.refreshToolbarTitle
import zhu.filer.util.shareFile
import zhu.filer.databinding.ActivityImageViewerBinding

// 图像查看界面
class ImageViewerActivity : AppCompatActivity() {

    internal lateinit var binding: ActivityImageViewerBinding
    internal var images = mutableListOf<File>()
    internal var currentIndex: Int = 0
    internal var isFullscreen = false
    internal var originalBgColor: Int = android.graphics.Color.TRANSPARENT

    // 创建活动
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyThemeColor(this)
        val params = intent.getParcelableExtra<TransformationLayout.Params>(TRANSITION_PARAMS_KEY)
        if (params != null) {
            onTransformationEndContainer(params)
        }
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTransition(params)

        val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
        val f = filePath?.let { File(it) }
        if (f == null || !f.canRead()) {
            finish()
            return
        }
        images = (loadImagesInDirectory(f) as List<File>).toMutableList()
        currentIndex = images.indexOf(f).coerceAtLeast(0)
        originalBgColor = getThemeColor(this, android.R.attr.colorBackground)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = currentFile().name
        }
        applyToolbarTitleName(binding.toolbar, currentFile().name)
        updateSubtitle()

        setupToolbarChrome()
        setupPager()
    }

    // 创建菜单
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, getString(R.string.share))
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(Menu.NONE, Menu.FIRST + 1, Menu.NONE, getString(R.string.delete))
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        return true
    }

    // 菜单项选择
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            Menu.FIRST -> shareFile(this, currentFile())
            Menu.FIRST + 1 -> confirmDeleteCurrent()
        }
        return super.onOptionsItemSelected(item)
    }

    // 支持返回导航
    override fun onSupportNavigateUp(): Boolean {
        finishAfterTransition()
        return true
    }

    @Deprecated("Deprecated in Java")
    // 按下返回键
    override fun onBackPressed() {
        finishAfterTransition()
    }

    // 恢复
    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) {
            refreshToolbarTitle(binding.toolbar)
        }
    }

    // 窗口焦点变化
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && ::binding.isInitialized) {
            refreshToolbarTitle(binding.toolbar)
        }
    }
}

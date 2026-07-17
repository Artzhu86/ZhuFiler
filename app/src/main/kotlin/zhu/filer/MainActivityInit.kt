package zhu.filer

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.R as materialR
import zhu.filer.browser.refresh
import zhu.filer.dialog.showNavigateDialog
import zhu.filer.ui.ThemeHelper
import zhu.filer.ui.dpToPx
import zhu.filer.ui.getStatusBarHeight
import zhu.filer.ui.getThemeColor

// 设置窗口
internal fun MainActivity.setupWindow() {
    window.sharedElementsUseOverlay = true
    WindowCompat.setDecorFitsSystemWindows(window, false)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
    }
    val isDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
        android.content.res.Configuration.UI_MODE_NIGHT_YES
    WindowCompat.getInsetsController(window, window.decorView)?.apply {
        isAppearanceLightStatusBars = !isDark
        isAppearanceLightNavigationBars = !isDark
    }
}

// 初始化视图
internal fun MainActivity.initViews() {
    drawerLayout = binding.drawerLayout
    navigationView = binding.navigationView
    recyclerView = binding.recyclerView
    toolbar = binding.toolbar
    progressBar = binding.progressBar
    fabAdd = binding.fabAdd
    fabAction = binding.fabAction
    fabCancel = binding.fabCancel
}

// 设置工具栏
internal fun MainActivity.setupToolbar() {
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    val toggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, 0, 0)
    drawerLayout.addDrawerListener(toggle)
    toggle.syncState()
    toolbar.post {
        for (i in 0 until toolbar.childCount) {
            val child = toolbar.getChildAt(i)
            if (child is TextView) {
                child.setOnClickListener {
                    showNavigateDialog(this, browserController.currentDisplayPath(), { dir -> navigateToDir(dir) }, prefs)
                }
            }
        }
    }
    val statusBarHeight = getStatusBarHeight(this)
    val actionBarHeight = toolbar.layoutParams.height
    toolbar.layoutParams.height = actionBarHeight + statusBarHeight
    toolbar.setPadding(0, statusBarHeight, 0, 0)
    toolbar.setBackgroundColor(Color.TRANSPARENT)
    toolbar.setSubtitleTextAppearance(this, R.style.ToolbarSubtitle)
    toolbar.post {
        supportActionBar?.title = Environment.getExternalStorageDirectory().absolutePath + "/"
    }
}

// 设置下拉刷新
internal fun MainActivity.setupSwipeRefresh() {
    val activity = this
    val parent = recyclerView.parent as androidx.constraintlayout.widget.ConstraintLayout
    val params = recyclerView.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
    parent.removeView(recyclerView)
    swipeRefreshLayout = SwipeRefreshLayout(this).apply {
        layoutParams = params
        setOnRefreshListener {
            activity.exitMultiSelect()
            activity.browserController.refresh()
        }
        setProgressBackgroundColorSchemeColor(
            getThemeColor(activity, com.google.android.material.R.attr.colorSurface)
        )
        setColorSchemeColors(
            getThemeColor(activity, android.R.attr.colorPrimary),
            getThemeColor(activity, materialR.attr.colorSecondary),
            getThemeColor(activity, materialR.attr.colorTertiary)
        )
        setProgressViewEndTarget(true, dpToPx(activity, 64))
    }
    swipeRefreshLayout.addView(recyclerView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    parent.addView(swipeRefreshLayout)
}

// 保存滚动状态
internal fun MainActivity.saveScrollState(outState: Bundle) {
    outState.putString("cached_path", browserController.currentDir.absolutePath)
    val lm = recyclerView.layoutManager as? LinearLayoutManager
    if (lm != null) {
        val pos = lm.findFirstVisibleItemPosition()
        if (pos != RecyclerView.NO_POSITION) {
            val view = lm.findViewByPosition(pos)
            val offset = view?.top ?: 0
            outState.putInt("scroll_pos", pos)
            outState.putInt("scroll_offset", offset)
        }
    }
}

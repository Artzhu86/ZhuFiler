package zhu.filer.media

import android.content.Context
import android.graphics.drawable.Animatable
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.transition.platform.MaterialContainerTransform
import java.io.File
import zhu.filer.FileType
import zhu.filer.util.SortMode
import zhu.filer.ui.applyToolbarTitleName
import zhu.filer.ui.getStatusBarHeight
import zhu.filer.util.getSortComparator
import zhu.filer.ui.getThemeColor
import com.google.android.material.R as materialR

// 当前文件
internal fun ImageViewerActivity.currentFile(): File = images[currentIndex]

// 启动GIF动画
internal fun ImageViewerActivity.startGifIfNeeded() {
    val rv = binding.imagePager.getChildAt(0) as? RecyclerView ?: return
    val holder = rv.findViewHolderForAdapterPosition(binding.imagePager.currentItem) ?: return
    val photoView = (holder as? ImagePagerAdapter.PageHolder)?.binding?.root ?: return
    val drawable = photoView.drawable as? Animatable ?: return
    if (drawable.isRunning) return
    val d = photoView.drawable
    d.setVisible(true, false)
    d.callback = photoView
    drawable.start()
}

// 设置过渡动画
internal fun ImageViewerActivity.setupTransition(params: com.skydoves.transformationlayout.TransformationLayout.Params?) {
    if (params == null) return
    val surfaceColor = getThemeColor(this, materialR.attr.colorSurface)
    val bgColor = getThemeColor(this, android.R.attr.colorBackground)
    (window.sharedElementEnterTransition as? MaterialContainerTransform)?.apply {
        setAllContainerColors(surfaceColor)
        scrimColor = bgColor
        fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
        addListener(object : android.transition.Transition.TransitionListener {
            override fun onTransitionStart(transition: android.transition.Transition) {}
            override fun onTransitionEnd(transition: android.transition.Transition) {
                startGifIfNeeded()
            }
            override fun onTransitionCancel(transition: android.transition.Transition) {
                startGifIfNeeded()
            }
            override fun onTransitionPause(transition: android.transition.Transition) {}
            override fun onTransitionResume(transition: android.transition.Transition) {}
        })
    }
    (window.sharedElementReturnTransition as? MaterialContainerTransform)?.apply {
        setAllContainerColors(surfaceColor)
        scrimColor = bgColor
        fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
    }
}

// 设置工具栏外观
internal fun ImageViewerActivity.setupToolbarChrome() {
    binding.toolbar.setBackgroundColor(getThemeColor(this, materialR.attr.colorPrimaryContainer))
    val statusBarHeight = getStatusBarHeight(this)
    binding.toolbar.layoutParams.height = binding.toolbar.layoutParams.height + statusBarHeight
    binding.toolbar.setPadding(0, statusBarHeight, 0, 0)
}

// 设置分页器
internal fun ImageViewerActivity.setupPager() {
    binding.imagePager.adapter = ImagePagerAdapter(
        images = images,
        onTap = { toggleFullscreen() },
        onScaleChange = { scaleFactor ->
            if (scaleFactor > 1f && !isFullscreen) {
                toggleFullscreen()
            }
        }
    )
    binding.imagePager.setCurrentItem(currentIndex, false)
    binding.imagePager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            currentIndex = position
            supportActionBar?.title = currentFile().name
            applyToolbarTitleName(binding.toolbar, currentFile().name)
            updateSubtitle()
            binding.imagePager.post { startGifIfNeeded() }
        }
    })
}

// 加载目录中的图像
internal fun ImageViewerActivity.loadImagesInDirectory(target: File): List<File> {
    val dir = target.parentFile ?: return listOf(target)
    val prefs = getSharedPreferences("filer_prefs", Context.MODE_PRIVATE)
    val showHidden = prefs.getBoolean("show_hidden", false)
    val sortMode = runCatching {
        SortMode.valueOf(prefs.getString("sort_mode", SortMode.NAME.name) ?: SortMode.NAME.name)
    }.getOrDefault(SortMode.NAME)
    val files = dir.listFiles { file ->
        file.isFile &&
            FileType.isImage(file) &&
            (showHidden || !file.name.startsWith("."))
    } ?: return listOf(target)
    return if (files.isEmpty()) listOf(target) else files.sortedWith(getSortComparator(sortMode))
}

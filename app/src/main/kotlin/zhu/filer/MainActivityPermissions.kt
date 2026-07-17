package zhu.filer

import android.os.Bundle
import java.io.File
import zhu.filer.browser.saveScrollState
import zhu.filer.util.toast

// 初始加载
internal fun MainActivity.initLoad() {
    permissionHelper.requestStoragePermission(
        onGranted = { loadDir(browserController.currentDir, scrollToTop = true) },
        onDenied = { toast(this, getString(R.string.need_storage_permission)); finish() }
    )
}

// 初始化初始内容
internal fun MainActivity.initInitialContent(savedInstanceState: Bundle?) {
    savedInstanceState?.let { bundle ->
        val path = bundle.getString("cached_path") ?: return@let
        val scrollPos = bundle.getInt("scroll_pos", -1)
        val scrollOffset = bundle.getInt("scroll_offset", 0)
        if (scrollPos >= 0) {
            browserController.saveScrollState(scrollPos, scrollOffset)
        }
        val dir = File(path)
        if (dir.exists() && dir.isDirectory && dir.canRead()) {
            loadDir(dir, scrollToTop = false)
        } else {
            initLoad()
        }
    } ?: initLoad()
}

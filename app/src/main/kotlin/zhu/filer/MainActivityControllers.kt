package zhu.filer

import androidx.lifecycle.lifecycleScope
import zhu.filer.browser.FileBrowserController
import zhu.filer.browser.FileClickHandler
import zhu.filer.browser.ToolbarScrollerController
import zhu.filer.browser.refresh
import zhu.filer.dialog.showCompressDialog
import zhu.filer.operation.FileOpener
import zhu.filer.operation.FileOperationsController
import zhu.filer.operation.MultiSelectController
import zhu.filer.operation.performCompress
import zhu.filer.settings.BookmarkManager
import zhu.filer.settings.SearchHelper
import zhu.filer.ui.updatePasteButtons

// 初始化控制器
internal fun MainActivity.initControllers() {
    bookmarkManager = BookmarkManager(this, drawerLayout, navigationView, prefs) { dir -> navigateToDir(dir) }
    bookmarkManager.setup()

    browserController = FileBrowserController(
        activity = this,
        toolbar = toolbar,
        recyclerView = recyclerView,
        swipeRefreshLayout = swipeRefreshLayout,
        prefs = prefs,
        showHiddenProvider = { menuController.isShowHidden() },
        sortModeProvider = { menuController.getSortMode() },
        onDirLoaded = {
            try { bookmarkManager.updateMenu(browserController.currentDir) } catch (e: UninitializedPropertyAccessException) {}
            statsSubtitle = supportActionBar?.subtitle?.toString()
            updateToolbarTitle()
            updateMultiSelectFabs()
            fabManager.updatePasteButtons(clipboard)
            toolbarScrollerController.animateToolbarColorOnDirSwitch()
        }
    )

    toolbarScrollerController = ToolbarScrollerController(toolbar, recyclerView, this)
    fileOpsController = FileOperationsController(
        activity = this,
        browserController = browserController,
        lifecycleScope = lifecycleScope,
        progressBar = progressBar,
        fabManager = fabManager,
        clipboard = clipboard,
        loadDir = { dir, showLoading, scrollToTop, restorePosition -> loadDir(dir, showLoading, scrollToTop, restorePosition) },
        refreshDir = { dir, highlightPath -> refreshDir(dir, highlightPath) }
    )

    fileOpener = FileOpener(
        activity = this,
        browserController = browserController,
        lifecycleScope = lifecycleScope,
        progressBar = progressBar
    )

    fileClickHandler = FileClickHandler(
        activity = this,
        recyclerView = recyclerView,
        browserController = browserController,
        fileOpener = fileOpener,
        fileOpsController = fileOpsController,
        bookmarkManager = bookmarkManager,
        multiSelectProvider = { multiSelectController },
        clipboard = clipboard,
        fabManager = fabManager,
        toolbarScrollerController = toolbarScrollerController,
        loadDir = { dir, scrollToTop -> loadDir(dir, scrollToTop = scrollToTop) },
        exitMultiSelect = { exitMultiSelect() },
        updateToolbarTitle = { updateToolbarTitle() },
        updateMultiSelectFabs = { updateMultiSelectFabs() }
    )
    fileClickHandler.setup()
    binding.fastScroller.attach(recyclerView)
    setupFabs()

    browserController.init(fileClickHandler.adapter)

    searchHelper = SearchHelper(this, { browserController.currentDir }, { dir -> navigateToDir(dir) }, { file -> locateFile(file) })

    menuController = MenuController(
        activity = this,
        prefs = prefs,
        browserController = browserController,
        bookmarkManager = bookmarkManager,
        searchHelper = searchHelper,
        onShowHiddenChanged = { browserController.refresh() },
        onExitMultiSelect = { exitMultiSelect() },
        onExit = { finish() }
    )
    menuController.initPrefs()

    multiSelectController = MultiSelectController(
        activity = this,
        adapter = fileClickHandler.adapter,
        canNavigateUp = { browserController.canNavigateUp() },
        getCurrentDir = { browserController.currentDir },
        loadDir = { loadDir(it) },
        progressBar = progressBar,
        clipboardManager = clipboard,
        onClipboardChanged = { fabManager.updatePasteButtons(clipboard) },
        onExitMultiSelect = { updateMultiSelectFabs() },
        onCompress = { files ->
            showCompressDialog(this, files, browserController.currentDir) { outputFile, format, password ->
                fileOpsController.performCompress(files, outputFile, format, password)
            }
        },
        onRefresh = { browserController.refresh(animate = false) },
        isInArchive = { browserController.isInArchive() },
        fileOpener = fileOpener,
        getArchiveFile = { browserController.getArchiveFile() },
        getArchivePassword = { browserController.getArchivePassword() }
    )

    backPressHandler.setup(
        multiSelectController = multiSelectController,
        drawerLayout = drawerLayout,
        browserController = browserController,
        onExit = { finish() },
        onExitMultiSelect = { exitMultiSelect() }
    )
}

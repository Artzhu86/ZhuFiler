package zhu.filer

import android.os.Handler
import android.os.Looper
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout

// 返回键处理
class BackPressHandler(private val activity: AppCompatActivity) {

    private var backPressedOnce = false

    // 设置返回键回调
    fun setup(
        multiSelectController: MultiSelectController,
        drawerLayout: DrawerLayout,
        browserController: FileBrowserController,
        onExit: () -> Unit,
        onExitMultiSelect: () -> Unit
    ) {
        activity.onBackPressedDispatcher.addCallback(activity, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (multiSelectController.isInMultiSelectMode()) {
                    onExitMultiSelect()
                    return
                }
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else if (browserController.canNavigateUp()) {
                    if (multiSelectController.isInMultiSelectMode()) {
                        onExitMultiSelect()
                    }
                    browserController.navigateUp()
                } else if (backPressedOnce) {
                    onExit()
                } else {
                    backPressedOnce = true
                    toast(activity, activity.getString(R.string.back_press_exit))
                    Handler(Looper.getMainLooper()).postDelayed({ backPressedOnce = false }, activity.resources.getInteger(R.integer.back_press_exit_timeout_ms).toLong())
                }
            }
        })
    }
}

package zhu.filer

import android.os.Handler
import android.os.Looper
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout

class BackPressHandler(private val activity: AppCompatActivity) {

    private var backPressedOnce = false

    fun setup(
        multiSelectController: MultiSelectController,
        drawerLayout: DrawerLayout,
        browserController: FileBrowserController,
        onExit: () -> Unit
    ) {
        activity.onBackPressedDispatcher.addCallback(activity, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (multiSelectController.isInMultiSelectMode()) {
                    multiSelectController.exitMultiSelect()
                    return
                }
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else if (browserController.canNavigateUp()) {
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

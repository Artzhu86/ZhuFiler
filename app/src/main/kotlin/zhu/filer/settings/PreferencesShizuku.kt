package zhu.filer.settings

import zhu.filer.R
import zhu.filer.util.ShizukuManager
import zhu.filer.util.toast

// 获取Shizuku状态摘要
internal fun PreferencesActivity.getShizukuStatusSummary(): String {
    return when (ShizukuManager.getPermissionState()) {
        ShizukuManager.PermissionState.NotInstalled -> getString(R.string.shizuku_not_installed)
        ShizukuManager.PermissionState.NotRunning -> getString(R.string.shizuku_not_running)
        ShizukuManager.PermissionState.NoPermission -> getString(R.string.shizuku_no_permission)
        ShizukuManager.PermissionState.Granted -> {
            if (ShizukuManager.isRoot()) getString(R.string.shizuku_granted_root)
            else getString(R.string.shizuku_granted_adb)
        }
    }
}

// 显示Shizuku对话框
internal fun PreferencesActivity.showShizukuDialog(update: () -> Unit) {
    val state = ShizukuManager.getPermissionState()
    when (state) {
        ShizukuManager.PermissionState.NotInstalled -> {
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                intent.data = android.net.Uri.parse("https://github.com/RikkaApps/Shizuku/releases/")
                startActivity(intent)
            } catch (e: Exception) {
                toast(this, getString(R.string.shizuku_not_installed))
            }
        }
        ShizukuManager.PermissionState.NotRunning -> {
            try {
                val launchIntent = packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                if (launchIntent != null) {
                    startActivity(launchIntent)
                } else {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                    intent.data = android.net.Uri.parse("https://github.com/RikkaApps/Shizuku/releases/")
                    startActivity(intent)
                }
            } catch (e: Exception) {
                toast(this, getString(R.string.shizuku_not_running))
            }
        }
        ShizukuManager.PermissionState.NoPermission -> {
            ShizukuManager.addPermissionResultListener(shizukuPermissionListener)
            ShizukuManager.requestPermission(0)
        }
        ShizukuManager.PermissionState.Granted -> {
            update()
        }
    }
}

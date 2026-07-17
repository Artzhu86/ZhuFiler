package zhu.filer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import rikka.shizuku.Shizuku
import zhu.filer.util.ShizukuManager

// 权限请求助手
class PermissionHelper(private val activity: AppCompatActivity) {

    private var onGranted: (() -> Unit)? = null
    private var onDenied: (() -> Unit)? = null

    private val requestStorage = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            onGranted?.invoke()
        } else {
            tryShizukuFallback()
        }
    }

    private val requestManage = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            onGranted?.invoke()
        } else {
            tryShizukuFallback()
        }
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            onGranted?.invoke()
        } else {
            onDenied?.invoke()
        }
    }

    // 请求存储权限
    fun requestStoragePermission(onGranted: () -> Unit, onDenied: () -> Unit) {
        this.onGranted = onGranted
        this.onDenied = onDenied

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                onGranted()
            } else {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:${activity.packageName}")
                requestManage.launch(intent)
            }
        } else {
            val perms = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (perms.all { ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED }) {
                onGranted()
            } else {
                requestStorage.launch(perms)
            }
        }
    }

    // 尝试通过Shizuku获取权限
    private fun tryShizukuFallback() {
        val state = ShizukuManager.getPermissionState()
        when (state) {
            ShizukuManager.PermissionState.NotInstalled -> {
                onDenied?.invoke()
            }
            ShizukuManager.PermissionState.NotRunning -> {
                val launchIntent = activity.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                if (launchIntent != null) {
                    activity.startActivity(launchIntent)
                }
                onDenied?.invoke()
            }
            ShizukuManager.PermissionState.Granted -> {
                onGranted?.invoke()
            }
            ShizukuManager.PermissionState.NoPermission -> {
                ShizukuManager.addPermissionResultListener(permissionResultListener)
                ShizukuManager.requestPermission(0)
            }
        }
    }

    // 销毁时移除监听
    fun onDestroy() {
        ShizukuManager.removePermissionResultListener(permissionResultListener)
    }
}

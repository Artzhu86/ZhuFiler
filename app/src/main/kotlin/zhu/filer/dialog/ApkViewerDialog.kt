package zhu.filer.dialog

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zhu.filer.R
import zhu.filer.ui.buildDialogTitle
import java.io.File

// APK信息数据类
internal data class ApkInfo(
    val label: String?,
    val packageName: String?,
    val versionName: String?,
    val versionCode: Long?,
    val minSdkLabel: String?,
    val targetSdkLabel: String?,
    val icon: Drawable?
)

// APK查看器弹窗
object ApkViewerDialog {

    // 显示弹窗
    fun show(activity: AppCompatActivity, file: File, onView: () -> Unit) {
        activity.lifecycleScope.launch {
            val info = withContext(Dispatchers.IO) {
                parseApkInfo(activity, file)
            }

            val table = buildPropertiesTable(activity, file, info)
            val titleView = buildDialogTitle(activity, info.label ?: file.name, info.icon)

            val dialog = MaterialAlertDialogBuilder(activity)
                .setCustomTitle(titleView)
                .setView(table)
                .setNegativeButton(R.string.view) { _, _ ->
                    onView()
                }
                .setPositiveButton(R.string.install) { _, _ ->
                    installApk(activity, file)
                }
                .create()

            dialog.show()
        }
    }

    // 解析APK信息
    private fun parseApkInfo(activity: AppCompatActivity, file: File): ApkInfo {
        val pm = activity.packageManager
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageArchiveInfo(file.absolutePath, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageArchiveInfo(file.absolutePath, 0)
        }

        if (packageInfo == null) {
            return ApkInfo(null, null, null, null, null, null, null)
        }

        val appInfo = packageInfo.applicationInfo
        appInfo?.sourceDir = file.absolutePath
        appInfo?.publicSourceDir = file.absolutePath

        val label = appInfo?.loadLabel(pm)?.toString()
        val icon = appInfo?.loadIcon(pm)
        val packageName = packageInfo.packageName
        val versionName = packageInfo.versionName
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }

        val minSdk = appInfo?.minSdkVersion ?: 0
        val targetSdk = appInfo?.targetSdkVersion ?: 0
        val minSdkLabel = sdkVersionToLabel(minSdk)
        val targetSdkLabel = sdkVersionToLabel(targetSdk)

        return ApkInfo(label, packageName, versionName, versionCode, minSdkLabel, targetSdkLabel, icon)
    }

    // 安装APK
    private fun installApk(activity: AppCompatActivity, file: File) {
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
    }
}

package zhu.filer.dialog

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.divider.MaterialDivider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zhu.filer.R
import zhu.filer.ui.buildPropertiesView
import zhu.filer.ui.createCopyableText
import zhu.filer.ui.dpToPx
import zhu.filer.ui.getThemeColor
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

            val message = buildApkProperties(activity, file, info)
            val headerView = buildHeader(activity, file, info)
            val view = buildPropertiesView(activity, message, headerView)

            val dialog = MaterialAlertDialogBuilder(activity)
                .setTitle(file.name)
                .setView(view)
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

    // 构建头部视图：图标 + 应用名 + 版本名
    private fun buildHeader(activity: Context, file: File, info: ApkInfo): View {
        val hPad = dpToPx(activity, 16)
        val vPad = dpToPx(activity, 12)

        val header = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(hPad, vPad, hPad, vPad)
        }

        // APK图标
        val iconView = ImageView(activity).apply {
            info.icon?.let { setImageDrawable(it) }
            val iconSize = dpToPx(activity, 48)
            val iconMarginEnd = dpToPx(activity, 16)
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                marginEnd = iconMarginEnd
            }
        }

        // 应用名 + 版本名竖向排列，占满剩余空间
        val textLayout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        // 应用名：左对齐，加粗，16sp
        val nameView = createCopyableText(
            activity,
            info.label ?: file.name,
            textSizeSp = 16f,
            textColor = getThemeColor(activity, com.google.android.material.R.attr.colorOnSurface),
            bold = true
        ).apply {
            gravity = android.view.Gravity.START
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // 版本名：左对齐，13sp，次要色
        val versionView = createCopyableText(
            activity,
            info.versionName ?: "",
            textSizeSp = 13f,
            textColor = getThemeColor(activity, com.google.android.material.R.attr.colorOnSurfaceVariant)
        ).apply {
            gravity = android.view.Gravity.START
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(activity, 2)
            }
        }

        textLayout.addView(nameView)
        textLayout.addView(versionView)

        header.addView(iconView)
        header.addView(textLayout)

        // 分隔线，inset 与内容 padding 对齐
        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }
        container.addView(header)

        val divider = MaterialDivider(activity).apply {
            setDividerInsetStart(hPad)
            setDividerInsetEnd(hPad)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(divider)

        return container
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

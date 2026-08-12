package zhu.filer.dialog

import android.text.format.Formatter
import androidx.appcompat.app.AppCompatActivity
import zhu.filer.R
import java.io.File

// 构建属性列表
internal fun buildApkProperties(activity: AppCompatActivity, file: File, info: ApkInfo): List<Pair<String, String>> {
    return listOf(
        activity.getString(R.string.apk_package) to (info.packageName ?: ""),
        activity.getString(R.string.apk_version_code) to (info.versionCode?.toString() ?: ""),
        activity.getString(R.string.apk_size) to Formatter.formatFileSize(activity, file.length()),
        activity.getString(R.string.apk_signature) to (if (info.signed) activity.getString(R.string.apk_signed) else activity.getString(R.string.apk_unsigned)),
        activity.getString(R.string.apk_min_sdk) to (info.minSdkLabel ?: ""),
        activity.getString(R.string.apk_target_sdk) to (info.targetSdkLabel ?: ""),
        activity.getString(R.string.apk_installed) to (info.installedVersion ?: activity.getString(R.string.apk_not_installed))
    )
}

// SDK版本号转标签
internal fun sdkVersionToLabel(sdk: Int): String {
    val androidVersion = when (sdk) {
        0 -> ""
        1 -> "1.0"
        2 -> "1.1"
        3 -> "1.5"
        4 -> "1.6"
        5 -> "2.0"
        6 -> "2.0.1"
        7 -> "2.1"
        8 -> "2.2"
        9 -> "2.3"
        10 -> "2.3.3"
        11 -> "3.0"
        12 -> "3.1"
        13 -> "3.2"
        14 -> "4.0"
        15 -> "4.0.3"
        16 -> "4.1"
        17 -> "4.2"
        18 -> "4.3"
        19 -> "4.4"
        20 -> "4.4W"
        21 -> "5"
        22 -> "5.1"
        23 -> "6"
        24 -> "7"
        25 -> "7.1"
        26 -> "8"
        27 -> "8.1"
        28 -> "9"
        29 -> "10"
        30 -> "11"
        31 -> "12"
        32 -> "12L"
        33 -> "13"
        34 -> "14"
        35 -> "15"
        36 -> "16"
        else -> "API $sdk"
    }
    return if (androidVersion.isEmpty()) "" else "Android $androidVersion (API $sdk)"
}

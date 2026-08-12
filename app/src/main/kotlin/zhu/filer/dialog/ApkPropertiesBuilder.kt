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
        activity.getString(R.string.apk_min_sdk) to (info.minSdkLabel ?: ""),
        activity.getString(R.string.apk_target_sdk) to (info.targetSdkLabel ?: "")
    )
}

// SDK版本号转标签
internal fun sdkVersionToLabel(sdk: Int): String {
    return when (sdk) {
        0 -> ""
        1 -> "1.0"
        2 -> "1.1"
        3 -> "1.5 Cupcake"
        4 -> "1.6 Donut"
        5 -> "2.0 Eclair"
        6 -> "2.0.1 Eclair"
        7 -> "2.1 Eclair"
        8 -> "2.2 Froyo"
        9 -> "2.3 Gingerbread"
        10 -> "2.3.3 Gingerbread"
        11 -> "3.0 Honeycomb"
        12 -> "3.1 Honeycomb"
        13 -> "3.2 Honeycomb"
        14 -> "4.0 Ice Cream Sandwich"
        15 -> "4.0.3 Ice Cream Sandwich"
        16 -> "4.1 Jelly Bean"
        17 -> "4.2 Jelly Bean"
        18 -> "4.3 Jelly Bean"
        19 -> "4.4 KitKat"
        20 -> "4.4W KitKat Watch"
        21 -> "5.0 Lollipop"
        22 -> "5.1 Lollipop"
        23 -> "6.0 Marshmallow"
        24 -> "7.0 Nougat"
        25 -> "7.1 Nougat"
        26 -> "8.0 Oreo"
        27 -> "8.1 Oreo"
        28 -> "9 Pie"
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
}

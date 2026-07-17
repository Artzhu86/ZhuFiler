package zhu.filer.dialog

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.format.Formatter
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import zhu.filer.R
import zhu.filer.util.toast
import java.io.File

// 构建属性表格
internal fun buildPropertiesTable(activity: AppCompatActivity, file: File, info: ApkInfo): TableLayout {
    val rows = listOf(
        activity.getString(R.string.apk_package) to (info.packageName ?: ""),
        activity.getString(R.string.apk_version_name) to (info.versionName ?: ""),
        activity.getString(R.string.apk_version_code) to (info.versionCode?.toString() ?: ""),
        activity.getString(R.string.apk_size) to Formatter.formatFileSize(activity, file.length()),
        activity.getString(R.string.apk_min_sdk) to (info.minSdkLabel ?: ""),
        activity.getString(R.string.apk_target_sdk) to (info.targetSdkLabel ?: "")
    )

    val table = LayoutInflater.from(activity)
        .inflate(R.layout.dialog_properties, null) as TableLayout
    for ((label, value) in rows) {
        val row = LayoutInflater.from(activity)
            .inflate(R.layout.item_property_row, table, false) as TableRow
        row.findViewById<TextView>(R.id.property_label).text = label
        val valueView = row.findViewById<TextView>(R.id.property_value)
        valueView.text = value
        val rippleBg = TypedValue().let { tv ->
            if (activity.theme.resolveAttribute(android.R.attr.selectableItemBackground, tv, true))
                ContextCompat.getDrawable(activity, tv.resourceId)
            else null
        }
        valueView.background = rippleBg
        valueView.setOnLongClickListener {
            val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("value", value))
            toast(activity, activity.getString(R.string.copied_to_clipboard))
            true
        }
        table.addView(row)
    }
    return table
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

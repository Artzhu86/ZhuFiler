package zhu.filer.browser

import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import com.bumptech.glide.Glide
import zhu.filer.FileItem
import zhu.filer.FileType
import zhu.filer.R
import zhu.filer.ui.dpToPx
import zhu.filer.ui.getThemeColor

// 设置缩略图
internal fun FileItemBinder.setupThumbnail(holder: FileListAdapter.ViewHolder, item: FileItem, position: Int) {
    val isImage = item.entryPath == null && !item.isDirectory && FileType.isImage(item.file)
    val isVideo = item.entryPath == null && !item.isDirectory && FileType.isVideo(item.file)
    val isAudio = item.entryPath == null && !item.isDirectory && FileType.isAudio(item.file)
    val isApk = item.entryPath == null && !item.isDirectory && FileType.isApk(item.file)
    val hasThumbnail = isImage || isVideo || isAudio || isApk
    if (hasThumbnail) {
        val placeholderRes = when {
            isImage -> R.drawable.outline_image_24
            isVideo -> R.drawable.outline_video_file_24
            isAudio -> R.drawable.outline_audio_file_24
            isApk -> R.drawable.outline_android_24
            else -> R.drawable.outline_insert_drive_file_24
        }
        val iconPx = dpToPx(holder.iconIv.context, FileItemViewBuilder.ICON_SIZE_DP)
        val thumbPx = dpToPx(holder.iconIv.context, FileItemViewBuilder.THUMB_SIZE_DP)
        holder.iconIv.layoutParams = FrameLayout.LayoutParams(iconPx, iconPx).apply {
            gravity = Gravity.CENTER
        }
        holder.iconIv.scaleType = ImageView.ScaleType.FIT_CENTER
        holder.iconIv.setImageResource(placeholderRes)
        holder.thumbnailLoaded = false
        val listener = createThumbnailListener(holder, placeholderRes, position)
        if (isAudio) {
            loadAudioThumbnail(holder, item, thumbPx, listener)
        } else if (isApk) {
            loadApkThumbnail(holder, item, thumbPx, listener)
        } else {
            val request = Glide.with(holder.iconIv.context).load(item.file)
            if (isVideo) request.frame(1000L)
            request.centerCrop().override(thumbPx).listener(listener).into(holder.iconIv)
        }
    } else {
        val iconPx = dpToPx(holder.iconIv.context, FileItemViewBuilder.ICON_SIZE_DP)
        holder.iconIv.layoutParams = FrameLayout.LayoutParams(iconPx, iconPx).apply {
            gravity = Gravity.CENTER
        }
        holder.iconIv.scaleType = ImageView.ScaleType.FIT_CENTER
        Glide.with(holder.iconIv.context).clear(holder.iconIv)
        holder.iconIv.setImageResource(item.iconRes)
    }
}

// 应用图标颜色
internal fun FileItemBinder.applyIconColor(holder: FileListAdapter.ViewHolder, selected: Boolean) {
    val primaryColor = getThemeColor(holder.iconIv.context, android.R.attr.colorPrimary)
    if (selected) {
        holder.iconIv.setColorFilter(primaryColor)
    } else {
        holder.iconIv.clearColorFilter()
    }
}

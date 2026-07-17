package zhu.filer.browser

import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import zhu.filer.FileItem
import zhu.filer.R
import zhu.filer.ui.dpToPx
import zhu.filer.util.getAudioArtwork

// 创建缩略图监听器
internal fun FileItemBinder.createThumbnailListener(
    holder: FileListAdapter.ViewHolder,
    placeholderRes: Int,
    position: Int
): RequestListener<Drawable> {
    return object : RequestListener<Drawable> {
        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean): Boolean {
            val px = dpToPx(holder.iconIv.context, FileItemViewBuilder.ICON_SIZE_DP)
            holder.iconIv.layoutParams = FrameLayout.LayoutParams(px, px).apply {
                gravity = Gravity.CENTER
            }
            holder.iconIv.scaleType = ImageView.ScaleType.FIT_CENTER
            holder.iconIv.setImageResource(placeholderRes)
            holder.thumbnailLoaded = false
            applyIconColor(holder, isSelected(position))
            return true
        }
        override fun onResourceReady(resource: Drawable, model: Any, target: Target<Drawable>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
            holder.iconIv.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            holder.iconIv.scaleType = ImageView.ScaleType.CENTER_CROP
            holder.thumbnailLoaded = true
            holder.iconIv.clearColorFilter()
            return false
        }
    }
}

// 加载音频缩略图
internal fun FileItemBinder.loadAudioThumbnail(
    holder: FileListAdapter.ViewHolder,
    item: FileItem,
    thumbPx: Int,
    listener: RequestListener<Drawable>
) {
    val filePath = item.file.absolutePath
    holder.iconIv.setTag(R.id.tag_audio_art, filePath)
    Thread {
        val art = getAudioArtwork(item.file)
        holder.iconIv.post {
            if (holder.iconIv.getTag(R.id.tag_audio_art) != filePath) return@post
            if (art != null) {
                Glide.with(holder.iconIv.context).load(art).centerCrop().override(thumbPx).listener(listener).into(holder.iconIv)
            }
        }
    }.start()
}

// 加载APK缩略图
internal fun FileItemBinder.loadApkThumbnail(
    holder: FileListAdapter.ViewHolder,
    item: FileItem,
    thumbPx: Int,
    listener: RequestListener<Drawable>
) {
    val filePath = item.file.absolutePath
    holder.iconIv.setTag(R.id.tag_audio_art, filePath)
    Thread {
        val pm = holder.iconIv.context.packageManager
        val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageArchiveInfo(filePath, android.content.pm.PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageArchiveInfo(filePath, 0)
        }
        var icon: Drawable? = null
        if (packageInfo != null) {
            val appInfo = packageInfo.applicationInfo
            appInfo?.sourceDir = filePath
            appInfo?.publicSourceDir = filePath
            icon = appInfo?.loadIcon(pm)
        }
        holder.iconIv.post {
            if (holder.iconIv.getTag(R.id.tag_audio_art) != filePath) return@post
            if (icon != null) {
                Glide.with(holder.iconIv.context).load(icon).centerCrop().override(thumbPx).listener(listener).into(holder.iconIv)
            }
        }
    }.start()
}

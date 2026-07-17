package zhu.filer.media

import android.graphics.BitmapFactory
import android.graphics.Color
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import io.getstream.photoview.PhotoView
import java.io.File
import zhu.filer.R
import zhu.filer.ui.applyToolbarTitleName
import zhu.filer.ui.buildDialogTitle
import zhu.filer.util.formatDate
import zhu.filer.util.toast
import zhu.filer.databinding.ItemImagePageBinding

// 图像分页适配器
class ImagePagerAdapter(
    private val images: List<File>,
    private val onTap: () -> Unit,
    private val onScaleChange: (Float) -> Unit
) : RecyclerView.Adapter<ImagePagerAdapter.PageHolder>() {

    // 页面持有者
    class PageHolder(val binding: ItemImagePageBinding) : RecyclerView.ViewHolder(binding.root)

    // 创建视图
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageHolder {
        val binding = ItemImagePageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PageHolder(binding)
    }

    // 绑定视图
    override fun onBindViewHolder(holder: PageHolder, position: Int) {
        val photoView: PhotoView = holder.binding.root
        photoView.setScale(1f, false)
        photoView.setOnViewTapListener { _, _, _ -> onTap() }
        photoView.setOnScaleChangeListener { scaleFactor, _, _ -> onScaleChange(scaleFactor) }
        Glide.with(photoView.context)
            .load(images[position])
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .fitCenter()
            .error(R.drawable.outline_image_24)
            .into(photoView)
    }

    // 获取项数
    override fun getItemCount(): Int = images.size
}

// 更新副标题
internal fun ImageViewerActivity.updateSubtitle() {
    val file = currentFile()
    val dateStr = formatDate(this, file.lastModified())
    val sizeStr = Formatter.formatFileSize(this, file.length())
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, opts)
    val w = opts.outWidth
    val h = opts.outHeight
    val dimStr = if (w > 0 && h > 0) "  (${w}x${h})" else ""
    supportActionBar?.subtitle = "$dateStr  $sizeStr$dimStr"
}

// 确认删除当前
internal fun ImageViewerActivity.confirmDeleteCurrent() {
    val file = currentFile()
    com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
        .setCustomTitle(buildDialogTitle(this, R.string.delete))
        .setMessage(getString(R.string.delete_message, file.name))
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(R.string.delete) { _, _ ->
            if (file.delete()) {
                images.removeAt(currentIndex)
                if (images.isEmpty()) {
                    finishAfterTransition()
                    return@setPositiveButton
                }
                if (currentIndex >= images.size) currentIndex = images.size - 1
                binding.imagePager.adapter?.notifyItemRemoved(currentIndex)
                if (binding.imagePager.currentItem != currentIndex) {
                    binding.imagePager.setCurrentItem(currentIndex, false)
                }
                supportActionBar?.title = currentFile().name
                applyToolbarTitleName(binding.toolbar, currentFile().name)
                updateSubtitle()
            } else {
                toast(this, getString(R.string.delete_failed))
            }
        }
        .show()
}

// 切换全屏
internal fun ImageViewerActivity.toggleFullscreen() {
    isFullscreen = !isFullscreen
    val controller = WindowCompat.getInsetsController(window, binding.root) ?: return
    if (isFullscreen) {
        binding.root.setBackgroundColor(Color.BLACK)
        binding.toolbar.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                binding.toolbar.visibility = View.GONE
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            .start()
    } else {
        binding.root.setBackgroundColor(originalBgColor)
        controller.show(WindowInsetsCompat.Type.systemBars())
        binding.toolbar.visibility = View.VISIBLE
        binding.toolbar.alpha = 0f
        binding.toolbar.animate()
            .alpha(1f)
            .setDuration(200)
            .start()
    }
}

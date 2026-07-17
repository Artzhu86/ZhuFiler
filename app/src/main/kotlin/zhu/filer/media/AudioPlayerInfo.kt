package zhu.filer.media

import android.text.format.Formatter
import android.view.View
import androidx.media3.common.util.UnstableApi
import com.bumptech.glide.Glide
import zhu.filer.R
import zhu.filer.ui.buildDialogTitle
import zhu.filer.util.formatDate
import zhu.filer.util.getAudioArtwork
import zhu.filer.util.getAudioMetadata
import zhu.filer.util.toast

@UnstableApi
// 加载音频信息
internal fun AudioPlayerActivity.loadAudioInfo() {
    binding.audioTitle.text = file.name
    binding.audioArtist.text = getString(R.string.unknown_artist)
    Thread {
        val art = getAudioArtwork(file)
        val (title, artist) = getAudioMetadata(file)
        runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            if (title != null) {
                binding.audioTitle.text = title
            }
            binding.audioArtist.text = artist ?: getString(R.string.unknown_artist)
            if (art != null) {
                binding.audioArtwork.visibility = View.VISIBLE
                binding.audioIcon.visibility = View.GONE
                Glide.with(this)
                    .load(art)
                    .centerCrop()
                    .into(binding.audioArtwork)
            } else {
                binding.audioArtwork.visibility = View.GONE
                binding.audioIcon.visibility = View.VISIBLE
            }
        }
    }.start()
}

@UnstableApi
// 更新副标题
internal fun AudioPlayerActivity.updateSubtitle() {
    val dateStr = formatDate(this, file.lastModified())
    val sizeStr = Formatter.formatFileSize(this, file.length())
    supportActionBar?.subtitle = "$dateStr  $sizeStr"
}

@UnstableApi
// 确认删除当前
internal fun AudioPlayerActivity.confirmDeleteCurrent() {
    com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
        .setCustomTitle(buildDialogTitle(this, R.string.delete))
        .setMessage(getString(R.string.delete_message, file.name))
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(R.string.delete) { _, _ ->
            if (file.delete()) {
                finishAfterTransition()
            } else {
                toast(this, getString(R.string.delete_failed))
            }
        }
        .show()
}

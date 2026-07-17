package zhu.filer.media

import android.text.format.Formatter
import android.view.View
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import zhu.filer.R
import zhu.filer.ui.buildDialogTitle
import zhu.filer.util.formatDate
import zhu.filer.util.toast

@UnstableApi
// 更新副标题
internal fun VideoPlayerActivity.updateSubtitle() {
    val dateStr = formatDate(this, file.lastModified())
    val sizeStr = Formatter.formatFileSize(this, file.length())
    supportActionBar?.subtitle = "$dateStr  $sizeStr"
}

@UnstableApi
// 初始化播放器
internal fun VideoPlayerActivity.initializePlayer() {
    player = ExoPlayer.Builder(this).build()
    binding.playerView.player = player
    val uri = androidx.core.content.FileProvider.getUriForFile(
        this, "${packageName}.fileprovider", file
    )
    val mediaItem = MediaItem.fromUri(uri)
    player.setMediaItem(mediaItem)
    player.prepare()
    player.playWhenReady = true

    setupControllerVisibilityListener()
    binding.playerView.showController()

    player.addListener(object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_ENDED) {
                player.playWhenReady = false
            }
        }
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (playWhenReady && player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0)
            }
        }
    })
}

@UnstableApi
// 设置控制器可见性监听
internal fun VideoPlayerActivity.setupControllerVisibilityListener() {
    binding.playerView.setControllerVisibilityListener(
        androidx.media3.ui.PlayerView.ControllerVisibilityListener { visibility ->
            if (visibility == View.GONE && !isFullscreen) {
                enterFullscreen()
            } else if (visibility == View.VISIBLE && isFullscreen) {
                exitFullscreen()
            }
        }
    )
}

@UnstableApi
// 进入全屏
internal fun VideoPlayerActivity.enterFullscreen() {
    isFullscreen = true
    binding.toolbar.animate().cancel()
    binding.toolbar.animate()
        .alpha(0f)
        .setDuration(200)
        .withEndAction {
            binding.toolbar.visibility = View.GONE
            val wc = WindowCompat.getInsetsController(window, binding.root) ?: return@withEndAction
            wc.hide(WindowInsetsCompat.Type.systemBars())
            wc.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        .start()
}

@UnstableApi
// 退出全屏
internal fun VideoPlayerActivity.exitFullscreen() {
    isFullscreen = false
    binding.toolbar.animate().cancel()
    val wc = WindowCompat.getInsetsController(window, binding.root)
    wc?.show(WindowInsetsCompat.Type.systemBars())
    binding.toolbar.visibility = View.VISIBLE
    binding.toolbar.alpha = 0f
    binding.toolbar.animate()
        .alpha(1f)
        .setDuration(200)
        .start()
}

@UnstableApi
// 释放播放器
internal fun VideoPlayerActivity.releasePlayer() {
    if (isPlayerInitialized()) {
        player.release()
    }
}

@UnstableApi
// 确认删除当前
internal fun VideoPlayerActivity.confirmDeleteCurrent() {
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

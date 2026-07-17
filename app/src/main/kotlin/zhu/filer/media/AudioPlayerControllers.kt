package zhu.filer.media

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.TimeBar
import zhu.filer.R
import zhu.filer.ui.applyToolbarTitleName
import zhu.filer.ui.getNavigationBarHeight
import zhu.filer.ui.getStatusBarHeight
import zhu.filer.ui.getThemeColor
import com.google.android.material.R as materialR

// 格式化时间
fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

@UnstableApi
// 设置工具栏
internal fun AudioPlayerActivity.setupToolbar() {
    setSupportActionBar(binding.toolbar)
    supportActionBar?.apply {
        setDisplayHomeAsUpEnabled(true)
        title = file.name
    }
    applyToolbarTitleName(binding.toolbar, file.name)
    updateSubtitle()
    binding.toolbar.setBackgroundColor(getThemeColor(this, materialR.attr.colorPrimaryContainer))
    val statusBarHeight = getStatusBarHeight(this)
    binding.toolbar.layoutParams.height = binding.toolbar.layoutParams.height + statusBarHeight
    binding.toolbar.setPadding(0, statusBarHeight, 0, 0)
    val navBarHeight = getNavigationBarHeight(this)
    binding.controlBar.setPaddingRelative(
        binding.controlBar.paddingStart,
        binding.controlBar.paddingTop,
        binding.controlBar.paddingEnd,
        navBarHeight
    )
}

@UnstableApi
// 设置控制器
internal fun AudioPlayerActivity.setupControls() {
    binding.timeBar.addListener(object : TimeBar.OnScrubListener {
        override fun onScrubStart(timeBar: TimeBar, position: Long) {
            scrubbing = true
            binding.timeCurrent.text = formatTime(position)
        }
        override fun onScrubMove(timeBar: TimeBar, position: Long) {
            binding.timeCurrent.text = formatTime(position)
        }
        override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
            scrubbing = false
            if (isPlayerInitialized()) {
                player.seekTo(position)
            }
        }
    })
    binding.playPauseButton.setOnClickListener {
        if (isPlayerInitialized()) {
            if (player.isPlaying) {
                player.pause()
            } else {
                if (player.playbackState == Player.STATE_ENDED) {
                    player.seekTo(0)
                }
                player.play()
            }
        }
    }
}

@UnstableApi
// 更新播放暂停按钮
internal fun AudioPlayerActivity.updatePlayPauseButton(isPlaying: Boolean) {
    if (isPlaying) {
        binding.playPauseButton.setImageResource(R.drawable.outline_pause_24)
    } else {
        binding.playPauseButton.setImageResource(R.drawable.outline_play_arrow_24)
    }
}

@UnstableApi
// 初始化播放器
internal fun AudioPlayerActivity.initializePlayer() {
    player = ExoPlayer.Builder(this).build()
    val uri = androidx.core.content.FileProvider.getUriForFile(
        this, "${packageName}.fileprovider", file
    )
    val mediaItem = MediaItem.fromUri(uri)
    player.setMediaItem(mediaItem)
    player.prepare()
    player.playWhenReady = true
    player.addListener(object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlayPauseButton(isPlaying)
        }
        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_ENDED) {
                player.playWhenReady = false
                updatePlayPauseButton(false)
            }
            if (state == Player.STATE_READY) {
                val dur = player.duration
                if (dur != C.TIME_UNSET) {
                    binding.timeBar.setDuration(dur)
                    binding.timeDuration.text = formatTime(dur)
                }
            }
        }
    })
    handler.post(updateRunnable)
}

@UnstableApi
// 释放播放器
internal fun AudioPlayerActivity.releasePlayer() {
    handler.removeCallbacks(updateRunnable)
    if (isPlayerInitialized()) {
        player.release()
    }
}

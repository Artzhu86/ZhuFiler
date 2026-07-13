package zhu.filer

import android.os.Bundle
import android.text.format.Formatter
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.skydoves.transformationlayout.TransformationLayout
import com.skydoves.transformationlayout.onTransformationEndContainer
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import zhu.filer.databinding.ActivityVideoPlayerBinding

// 视频播放界面
@UnstableApi
class VideoPlayerActivity : AppCompatActivity() {

    // 伴生对象
    companion object {
        const val EXTRA_FILE_PATH = "extra_file_path"
    }

    private lateinit var binding: ActivityVideoPlayerBinding
    private lateinit var player: ExoPlayer
    private lateinit var file: File
    private var isFullscreen = false

    // 创建活动
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyThemeColor(this)
        val params = intent.getParcelableExtra<TransformationLayout.Params>("TransformationParams")
        if (params != null) {
            onTransformationEndContainer(params)
        }
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
        val f = filePath?.let { File(it) }
        if (f == null || !f.canRead()) {
            finish()
            return
        }
        file = f

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = file.name
        }
        applyToolbarTitleName(binding.toolbar, file.name)
        updateSubtitle()

        val statusBarHeight = getStatusBarHeight(this)
        binding.toolbar.layoutParams.height = binding.toolbar.layoutParams.height + statusBarHeight
        binding.toolbar.setPadding(0, statusBarHeight, 0, 0)

        binding.playerView.controllerHideOnTouch = true
        binding.playerView.controllerShowTimeoutMs = -1
    }

    // 更新副标题
    private fun updateSubtitle() {
        val dateStr = SimpleDateFormat(getString(R.string.date_format), Locale.getDefault())
            .format(Date(file.lastModified()))
        val sizeStr = Formatter.formatFileSize(this, file.length())
        supportActionBar?.subtitle = "$dateStr  $sizeStr"
    }

    // 开始播放
    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    // 初始化播放器
    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build()
        binding.playerView.player = player
        val uri = androidx.core.content.FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
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

    // 设置控制器可见性监听
    private fun setupControllerVisibilityListener() {
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

    // 进入全屏
    private fun enterFullscreen() {
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

    // 退出全屏
    private fun exitFullscreen() {
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

    // 释放播放器
    private fun releasePlayer() {
        if (::player.isInitialized) {
            player.release()
        }
    }

    // 停止播放
    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    // 创建菜单
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, getString(R.string.share))
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(Menu.NONE, Menu.FIRST + 1, Menu.NONE, getString(R.string.delete))
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        return true
    }

    // 菜单项选择
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            Menu.FIRST -> shareFile(this, file)
            Menu.FIRST + 1 -> confirmDeleteCurrent()
        }
        return super.onOptionsItemSelected(item)
    }

    // 确认删除当前
    private fun confirmDeleteCurrent() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(R.string.confirm_delete)
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

    // 支持返回导航
    override fun onSupportNavigateUp(): Boolean {
        finishAfterTransition()
        return true
    }

    // 按下返回键
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isFullscreen) {
            binding.playerView.showController()
        } else {
            finishAfterTransition()
        }
    }

    // 恢复
    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) {
            refreshToolbarTitle(binding.toolbar)
        }
    }

    // 窗口焦点变化
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && ::binding.isInitialized) {
            refreshToolbarTitle(binding.toolbar)
        }
    }
}

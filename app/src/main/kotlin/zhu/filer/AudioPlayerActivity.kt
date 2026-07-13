package zhu.filer

import android.os.Bundle
import android.text.format.Formatter
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.skydoves.transformationlayout.TransformationLayout
import com.skydoves.transformationlayout.onTransformationEndContainer
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import zhu.filer.databinding.ActivityAudioPlayerBinding

import com.google.android.material.R as materialR

// 音频播放界面
@UnstableApi
class AudioPlayerActivity : AppCompatActivity() {

    // 伴生对象
    companion object {
        const val EXTRA_FILE_PATH = "extra_file_path"
    }

    private lateinit var binding: ActivityAudioPlayerBinding
    private lateinit var player: ExoPlayer
    private lateinit var file: File

    // 创建活动
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyThemeColor(this)
        val params = intent.getParcelableExtra<TransformationLayout.Params>("TransformationParams")
        if (params != null) {
            onTransformationEndContainer(params)
        }
        super.onCreate(savedInstanceState)

        binding = ActivityAudioPlayerBinding.inflate(layoutInflater)
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

        binding.toolbar.setBackgroundColor(getThemeColor(this, materialR.attr.colorPrimaryContainer))

        val statusBarHeight = getStatusBarHeight(this)
        binding.toolbar.layoutParams.height = binding.toolbar.layoutParams.height + statusBarHeight
        binding.toolbar.setPadding(0, statusBarHeight, 0, 0)

        binding.audioTitle.text = file.name
        binding.audioInfo.text = buildString {
            append(Formatter.formatFileSize(this@AudioPlayerActivity, file.length()))
            append("  ")
            append(SimpleDateFormat(getString(R.string.date_format), Locale.getDefault())
                .format(Date(file.lastModified())))
        }
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
        binding.playerControlView.player = player
        val uri = androidx.core.content.FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
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
        finishAfterTransition()
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

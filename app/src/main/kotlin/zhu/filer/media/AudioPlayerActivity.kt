package zhu.filer.media

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.skydoves.transformationlayout.TransformationLayout
import com.skydoves.transformationlayout.onTransformationEndContainer
import java.io.File
import zhu.filer.util.EXTRA_FILE_PATH
import zhu.filer.R
import zhu.filer.util.TRANSITION_PARAMS_KEY
import zhu.filer.ui.ThemeHelper
import zhu.filer.ui.refreshToolbarTitle
import zhu.filer.util.shareFile
import zhu.filer.databinding.ActivityAudioPlayerBinding

@UnstableApi
// 音频播放界面
class AudioPlayerActivity : AppCompatActivity() {

    internal lateinit var binding: ActivityAudioPlayerBinding
    internal lateinit var player: ExoPlayer
    internal lateinit var file: File
    internal val handler = Handler(Looper.getMainLooper())
    internal var scrubbing = false

    // 检查播放器是否初始化
    fun isPlayerInitialized() = ::player.isInitialized

    internal val updateRunnable = object : Runnable {
        override fun run() {
            if (::player.isInitialized) {
                val pos = player.currentPosition
                val dur = player.duration
                if (!scrubbing) {
                    binding.timeBar.setPosition(pos)
                    binding.timeCurrent.text = formatTime(pos)
                }
                if (dur != C.TIME_UNSET) {
                    binding.timeBar.setDuration(dur)
                    binding.timeDuration.text = formatTime(dur)
                }
                binding.timeBar.setBufferedPosition(player.bufferedPosition)
            }
            handler.postDelayed(this, 200)
        }
    }

    // 创建活动
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applyThemeColor(this)
        val params = intent.getParcelableExtra<TransformationLayout.Params>(TRANSITION_PARAMS_KEY)
        if (params != null) {
            onTransformationEndContainer(params)
        }
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityAudioPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
        val f = filePath?.let { File(it) }
        if (f == null || !f.canRead()) {
            finish()
            return
        }
        file = f

        setupToolbar()
        setupControls()
        loadAudioInfo()
    }

    // 开始播放
    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    // 停止播放
    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    // 销毁
    override fun onDestroy() {
        handler.removeCallbacks(updateRunnable)
        super.onDestroy()
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

    // 支持返回导航
    override fun onSupportNavigateUp(): Boolean {
        finishAfterTransition()
        return true
    }

    @Deprecated("Deprecated in Java")
    // 按下返回键
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

package zhu.filer

import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.view.isVisible
import com.google.android.material.floatingactionbutton.FloatingActionButton

object AnimationHelper {

    private const val DURATION_IN = 200L
    private const val DURATION_OUT = 150L
    private const val START_DELAY_IN = 50L

    fun showButtons(paste: FloatingActionButton, cancel: FloatingActionButton) {
        paste.animate().cancel()
        cancel.animate().cancel()

        paste.alpha = 0f
        paste.scaleX = 0.8f
        paste.scaleY = 0.8f
        paste.isVisible = true
        paste.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(DURATION_IN)
            .setInterpolator(DecelerateInterpolator())
            .start()

        cancel.alpha = 0f
        cancel.scaleX = 0.8f
        cancel.scaleY = 0.8f
        cancel.isVisible = true
        cancel.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(DURATION_IN)
            .setStartDelay(START_DELAY_IN)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    fun hideButtons(paste: FloatingActionButton, cancel: FloatingActionButton) {
        paste.animate().cancel()
        cancel.animate().cancel()

        paste.animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(DURATION_OUT)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                paste.isVisible = false
                paste.alpha = 1f
                paste.scaleX = 1f
                paste.scaleY = 1f
            }
            .start()

        cancel.animate()
            .alpha(0f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(DURATION_OUT)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                cancel.isVisible = false
                cancel.alpha = 1f
                cancel.scaleX = 1f
                cancel.scaleY = 1f
            }
            .start()
    }
}
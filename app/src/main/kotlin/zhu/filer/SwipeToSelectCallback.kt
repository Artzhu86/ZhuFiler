package zhu.filer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class SwipeToSelectCallback(
    private val adapter: FileListAdapter,
    private val onSwipeToSelect: (Int, Boolean) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    private val vibratedHolders = mutableSetOf<RecyclerView.ViewHolder>()
    private val pendingSelectViews = mutableSetOf<android.view.View>()
    private val maxSwipeRatio = 0.15f
    private val thresholdRatio = 1.0f

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            val position = viewHolder.adapterPosition
            if (position == RecyclerView.NO_POSITION || adapter.isUpItem(position)) {
                viewHolder.itemView.translationX = 0f
                return
            }

            val itemView = viewHolder.itemView
            val maxSwipe = itemView.width * maxSwipeRatio
            val clamped = dX.coerceIn(-maxSwipe, maxSwipe)
            itemView.translationX = clamped

            if (Math.abs(clamped) >= maxSwipe * thresholdRatio) {
                if (viewHolder !in vibratedHolders) {
                    vibratedHolders.add(viewHolder)
                    val context = itemView.context
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.VIBRATE) == PackageManager.PERMISSION_GRANTED) {
                        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                        vibrator?.let {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                it.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                it.vibrate(40)
                            }
                        }
                    }
                }
                pendingSelectViews.add(itemView)
            }
        } else {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        val itemView = viewHolder.itemView
        val position = viewHolder.adapterPosition

        if (itemView in pendingSelectViews && position != RecyclerView.NO_POSITION) {
            pendingSelectViews.remove(itemView)
            vibratedHolders.remove(viewHolder)
            itemView.translationX = 0f
            val willBeSelected = !adapter.isSelected(position)
            onSwipeToSelect(position, willBeSelected)
            super.clearView(recyclerView, viewHolder)
            return
        }

        itemView.animate()
            .translationX(0f)
            .setDuration(150)
            .withEndAction {
                itemView.translationX = 0f
            }
            .start()

        pendingSelectViews.remove(itemView)
        vibratedHolders.remove(viewHolder)
        super.clearView(recyclerView, viewHolder)
    }

    override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val position = viewHolder.adapterPosition
        if (position == RecyclerView.NO_POSITION || adapter.isUpItem(position)) {
            return 0
        }
        return super.getSwipeDirs(recyclerView, viewHolder)
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float = 1.0f

    override fun getSwipeEscapeVelocity(defaultValue: Float): Float = Float.MAX_VALUE
}

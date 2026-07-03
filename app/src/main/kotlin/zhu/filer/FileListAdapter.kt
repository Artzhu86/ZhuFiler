package zhu.filer

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import java.io.File

import com.google.android.material.R as materialR

data class FileItem(val file: File, val displayName: String, val iconRes: Int, val subtitle: String)

class FileListAdapter(
   private val onItemClick: (File, Int) -> Unit,
   private val onItemLongClick: (File, Int) -> Boolean
) : RecyclerView.Adapter<FileListAdapter.ViewHolder>() {

   private val items = mutableListOf<FileItem>()
   private var highlightPosition: Int = -1
   private var blinkPosition: Int = -1
   private var dataVersion = 0
   private val itemVersionMap = mutableMapOf<String, Int>()
   private var lastSubmitTime = 0L
   private var isAddItemAllowedToAnimate = false

   private val selectedPositions = mutableSetOf<Int>()
   private var selectedColor: Int = 0

   fun isSelected(position: Int) = selectedPositions.contains(position)

   fun toggleSelection(position: Int) {
       if (position == 0 && isUpItem(position)) return
       if (selectedPositions.contains(position)) {
           selectedPositions.remove(position)
       } else {
           selectedPositions.add(position)
       }
       notifyItemChanged(position)
   }

   fun selectPosition(position: Int) {
       if (position == 0 && isUpItem(position)) return
       if (selectedPositions.add(position)) {
           notifyItemChanged(position)
       }
   }

   fun clearSelection() {
       val copy = selectedPositions.toSet()
       selectedPositions.clear()
       copy.forEach { notifyItemChanged(it) }
   }

   fun getSelectedFiles(): List<File> {
       return selectedPositions.mapNotNull { items.getOrNull(it)?.file }
           .filter { it.name != ".." }
   }

   fun hasSelection() = selectedPositions.isNotEmpty()

   fun isUpItem(position: Int): Boolean {
       return position == 0 && items.isNotEmpty() && items[0].file.name == ".."
   }

   private companion object {
       private const val ANIM_WINDOW_MS = 300L
       private const val FADE_DURATION = 125L
       private const val BLINK_FADE_DURATION = 125L
       private const val ITEM_ANIMATION_DELAY_MS = 15L
       private const val CORNER_RADIUS_DP = 12
       private const val ITEM_MARGIN_DP = 8
       private const val ITEM_VERTICAL_MARGIN_DP = 4
       private const val THUMB_SIZE_DP = 48
       private const val ICON_SIZE_DP = 24
       private const val THUMB_CORNER_RADIUS_DP = 8
       private const val ITEM_PADDING_DP = 8
       private const val ICON_TEXT_MARGIN_DP = 8
       private val VIEW_TAG_ID = R.id.tag_view
   }

   fun submitList(new: List<FileItem>, highlightPos: Int = -1) {
       items.clear()
       items.addAll(new)
       highlightPosition = highlightPos
       blinkPosition = highlightPos
       dataVersion++
       itemVersionMap.clear()
       lastSubmitTime = System.currentTimeMillis()
       isAddItemAllowedToAnimate = false
       clearSelection()
       notifyDataSetChanged()
   }

   fun addItem(item: FileItem) {
       items.add(item)
       isAddItemAllowedToAnimate = true
       notifyItemInserted(items.size - 1)
   }

   override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
       val context = parent.context
       val pressColor = getThemeColor(context, android.R.attr.colorControlHighlight)
       val cornerPx = dpToPx(context, CORNER_RADIUS_DP).toFloat()
       val marginPx = dpToPx(context, ITEM_MARGIN_DP)
       val verticalMarginPx = dpToPx(context, ITEM_VERTICAL_MARGIN_DP)
       val thumbSizePx = dpToPx(context, THUMB_SIZE_DP)
       val iconSizePx = dpToPx(context, ICON_SIZE_DP)
       val thumbCornerPx = dpToPx(context, THUMB_CORNER_RADIUS_DP).toFloat()
       val paddingPx = dpToPx(context, ITEM_PADDING_DP)
       val iconTextMarginPx = dpToPx(context, ICON_TEXT_MARGIN_DP)

       if (selectedColor == 0) {
           selectedColor = getThemeColor(context, materialR.attr.colorPrimaryContainer)
       }

       val card = MaterialCardView(context).apply {
           layoutParams = RecyclerView.LayoutParams(
               ViewGroup.LayoutParams.MATCH_PARENT,
               ViewGroup.LayoutParams.WRAP_CONTENT
           ).apply {
               leftMargin = marginPx
               rightMargin = marginPx
               topMargin = verticalMarginPx
               bottomMargin = verticalMarginPx
           }
           radius = cornerPx
           cardElevation = 0f
           strokeWidth = 0
           isClickable = true
           isFocusable = true
           setCardBackgroundColor(Color.TRANSPARENT)

           setOnTouchListener { v, event ->
               val container = (v as? MaterialCardView)?.getChildAt(0) as? LinearLayout
               when (event.action) {
                   MotionEvent.ACTION_DOWN -> {
                       container?.setBackgroundColor(pressColor)
                   }
                   MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                       val pos = v.tag as? Int ?: -1
                       val bg = when {
                           pos == highlightPosition || pos == blinkPosition ->
                               getThemeColor(context, android.R.attr.colorControlHighlight)
                           isSelected(pos) -> selectedColor
                           else -> Color.TRANSPARENT
                       }
                       container?.setBackgroundColor(bg)
                   }
               }
               false
           }
       }

       val container = LinearLayout(context).apply {
           orientation = LinearLayout.HORIZONTAL
           layoutParams = ViewGroup.LayoutParams(
               ViewGroup.LayoutParams.MATCH_PARENT,
               ViewGroup.LayoutParams.WRAP_CONTENT
           )
           setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
           setBackgroundColor(Color.TRANSPARENT)
       }

       card.addView(container)

       val thumbCard = MaterialCardView(context).apply {
           layoutParams = LinearLayout.LayoutParams(thumbSizePx, thumbSizePx).apply {
               gravity = Gravity.CENTER_VERTICAL
           }
           radius = thumbCornerPx
           cardElevation = 0f
           strokeWidth = 0
           isClickable = false
           isFocusable = false
           setCardBackgroundColor(Color.TRANSPARENT)
           clipChildren = true
           clipToOutline = true
       }

       val iconIv = ImageView(context).apply {
           layoutParams = FrameLayout.LayoutParams(iconSizePx, iconSizePx).apply {
               gravity = Gravity.CENTER
           }
           scaleType = ImageView.ScaleType.FIT_CENTER
       }
       thumbCard.addView(iconIv)
       container.addView(thumbCard)

       val nameContainer = LinearLayout(context).apply {
           orientation = LinearLayout.VERTICAL
           layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
               marginStart = iconTextMarginPx
               gravity = Gravity.CENTER_VERTICAL
           }
       }

       val nameTv = TextView(context).apply {
           textSize = 16f
           setSingleLine(true)
           maxLines = 1
           ellipsize = TextUtils.TruncateAt.MIDDLE
           layoutParams = LinearLayout.LayoutParams(
               ViewGroup.LayoutParams.MATCH_PARENT,
               ViewGroup.LayoutParams.WRAP_CONTENT
           )
       }
       nameContainer.addView(nameTv)

       val subtitleTv = TextView(context).apply {
           textSize = 11f
           setSingleLine(true)
           maxLines = 1
           ellipsize = TextUtils.TruncateAt.END
           layoutParams = LinearLayout.LayoutParams(
               ViewGroup.LayoutParams.MATCH_PARENT,
               ViewGroup.LayoutParams.WRAP_CONTENT
           )
       }
       nameContainer.addView(subtitleTv)

       container.addView(nameContainer)

       return ViewHolder(card, container, iconIv, nameTv, subtitleTv)
   }

   override fun onBindViewHolder(holder: ViewHolder, position: Int) {
       val item = items[position]
        holder.itemView.tag = position
        if (!item.file.isDirectory && item.file.extension.lowercase() in imageExts) {
            holder.iconIv.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            holder.iconIv.scaleType = ImageView.ScaleType.CENTER_CROP
            Glide.with(holder.iconIv.context)
                .load(item.file)
                .centerCrop()
                .into(holder.iconIv)
        } else {
            val iconPx = dpToPx(holder.iconIv.context, ICON_SIZE_DP)
            holder.iconIv.layoutParams = FrameLayout.LayoutParams(iconPx, iconPx).apply {
                gravity = Gravity.CENTER
            }
            holder.iconIv.scaleType = ImageView.ScaleType.FIT_CENTER
            Glide.with(holder.iconIv.context).clear(holder.iconIv)
            holder.iconIv.setImageResource(item.iconRes)
        }
        holder.nameTv.text = item.displayName
       holder.subtitleTv.text = item.subtitle

       val isHighlight = position == highlightPosition || position == blinkPosition
       val isSel = isSelected(position)

       val bgColor = when {
           isHighlight -> getThemeColor(holder.container.context, android.R.attr.colorControlHighlight)
           isSel -> selectedColor
           else -> Color.TRANSPARENT
       }
       holder.container.setBackgroundColor(bgColor)

       val key = item.file.absolutePath
       val version = itemVersionMap[key] ?: 0
       val isWithinWindow = System.currentTimeMillis() - lastSubmitTime < ANIM_WINDOW_MS
       val shouldAnimate = version != dataVersion && (isWithinWindow || isAddItemAllowedToAnimate)

       if (shouldAnimate) {
           itemVersionMap[key] = dataVersion
           val startDelay = (position * ITEM_ANIMATION_DELAY_MS).toLong()

           holder.itemView.animate().cancel()
           holder.itemView.alpha = 0f
           holder.itemView.animate()
               .alpha(1f)
               .setDuration(FADE_DURATION)
               .setStartDelay(startDelay)
               .setListener(object : AnimatorListenerAdapter() {
                   override fun onAnimationEnd(animation: Animator) {
                       holder.itemView.alpha = 1f
                       if (isHighlight) {
                           val bgAnim = ValueAnimator.ofObject(
                               ArgbEvaluator(),
                               getThemeColor(holder.itemView.context, android.R.attr.colorControlHighlight),
                               Color.TRANSPARENT
                           )
                           bgAnim.duration = BLINK_FADE_DURATION
                           bgAnim.addUpdateListener {
                               holder.container.setBackgroundColor(it.animatedValue as Int)
                           }
                           bgAnim.addListener(object : AnimatorListenerAdapter() {
                               override fun onAnimationEnd(animation: Animator) {
                                   holder.container.setBackgroundColor(Color.TRANSPARENT)
                                   if (blinkPosition == position) blinkPosition = -1
                                   if (highlightPosition == position) highlightPosition = -1
                               }
                           })
                           bgAnim.start()
                       }
                   }
                   override fun onAnimationCancel(animation: Animator) {
                       holder.itemView.alpha = 1f
                   }
               })
               .start()
       } else {
           if (isHighlight) {
               val animStarted = holder.itemView.getTag(VIEW_TAG_ID) == true
               if (!animStarted) {
                   holder.itemView.setTag(VIEW_TAG_ID, true)
                   holder.container.setBackgroundColor(getThemeColor(holder.itemView.context, android.R.attr.colorControlHighlight))
                   startBlinkFade(holder.container) {
                       holder.itemView.setTag(VIEW_TAG_ID, false)
                       if (blinkPosition == position) blinkPosition = -1
                       if (highlightPosition == position) highlightPosition = -1
                       holder.container.setBackgroundColor(Color.TRANSPARENT)
                       notifyItemChanged(position)
                   }
               }
           } else {
               if (holder.itemView.getTag(VIEW_TAG_ID) == true) {
                   holder.itemView.setTag(VIEW_TAG_ID, false)
               }
           }
       }

       holder.itemView.setOnClickListener {
           onItemClick(item.file, position)
       }

       holder.itemView.setOnLongClickListener { onItemLongClick(item.file, position) }
   }

   override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        Glide.with(holder.iconIv.context).clear(holder.iconIv)
        holder.itemView.setTag(VIEW_TAG_ID, false)
        holder.itemView.animate().cancel()
        holder.itemView.alpha = 1f
        holder.container.setBackgroundColor(Color.TRANSPARENT)
    }

   override fun getItemCount(): Int = items.size

   inner class ViewHolder(
       itemView: View,
       val container: LinearLayout,
       val iconIv: ImageView,
       val nameTv: TextView,
       val subtitleTv: TextView
   ) : RecyclerView.ViewHolder(itemView)

   private fun startBlinkFade(view: View, onEnd: () -> Unit) {
       val highlightColor = getThemeColor(view.context, android.R.attr.colorControlHighlight)
       val transparent = Color.TRANSPARENT
       val anim = ValueAnimator.ofObject(ArgbEvaluator(), highlightColor, transparent)
       anim.duration = BLINK_FADE_DURATION
       anim.addUpdateListener {
           view.setBackgroundColor(it.animatedValue as Int)
       }
       anim.addListener(object : AnimatorListenerAdapter() {
           override fun onAnimationEnd(animation: Animator) {
               onEnd()
           }
           override fun onAnimationCancel(animation: Animator) {
               onEnd()
           }
       })
       anim.start()
   }
}

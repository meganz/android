package mega.privacy.android.app.mediaplayer.playlist

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemPlaylistBinding
import mega.privacy.android.app.presentation.time.mapper.DurationInSecondsTextMapper

/**
 * RecyclerView adapter for playlist screen.
 * @param context Context
 * @param itemOperation PlaylistItemOperation
 * @param isAudio whether is audio
 * @param dragStartListener DragStartListener
 */
class PlaylistAdapter(
    private val context: Context,
    private val itemOperation: PlaylistItemOperation,
    val isAudio: Boolean,
    private val dragStartListener: DragStartListener,
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper,
) : ListAdapter<PlaylistItem, PlaylistViewHolder>(PlaylistItemDiffCallback()) {

    private var isPaused = false
    private var playingItemIndex: Int = 0
    private var currentPlayingPosition: Long = 0

    override fun getItemViewType(position: Int): Int {
        return getItem(position).type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder =
        PlaylistViewHolder(
            ItemPlaylistBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        //Added the touch listener for reorder icon to implement drag feature
        holder.itemView.findViewById<ImageView>(R.id.transfers_list_option_reorder)
            .setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    dragStartListener.onDragStarted(holder)
                }
                false
            }
        val playlistItem = getItem(holder.absoluteAdapterPosition)
        val currentItemIndex = holder.absoluteAdapterPosition

        with(holder.itemView.findViewById<TextView>(R.id.duration)) {
            isVisible = playlistItem.duration.inWholeSeconds > 0L

            if (playlistItem.type == TYPE_PLAYING) {
                playingItemIndex = holder.absoluteAdapterPosition
                text = playlistItem.formatCurrentPositionAndDuration(
                    currentPlayingPosition,
                    durationInSecondsTextMapper
                )
            } else {
                text = durationInSecondsTextMapper(playlistItem.duration)
            }
        }

        holder.itemView.findViewById<FrameLayout>(R.id.header_layout).isVisible =
            playlistItem.headerIsVisible
        holder.itemView.findViewById<FrameLayout>(R.id.next_layout).isVisible =
            currentItemIndex != itemCount - 1 && playlistItem.type == TYPE_PLAYING

        holder.bind(isPaused, playlistItem, itemOperation, holder, isAudio, currentItemIndex)
    }

    /**
     * Start the animation
     * @param holder the holder that adds the animation
     * @param position the position of item that adds the animation
     */
    fun startAnimation(holder: PlaylistViewHolder, position: Int) {
        val flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip)
        flipAnimation.duration = ANIMATION_DURATION
        flipAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                notifyItemChanged(position)
            }

            override fun onAnimationEnd(animation: Animation) {
                notifyItemChanged(position)
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
        holder.itemView.findViewById<ImageView>(R.id.image_selected)
            .startAnimation(flipAnimation)
    }

    /**
     * Get the position of playing item
     * @return the position of playing item
     */
    fun getPlayingPosition() = playingItemIndex

    /**
     * Set the current playing position
     * @param currentPosition current playing position
     */
    fun setCurrentPlayingPosition(currentPosition: Long?) {
        currentPlayingPosition = currentPosition ?: 0
        notifyItemChanged(playingItemIndex)
    }

    /**
     * Refresh UI when the paused state is changed
     *
     * @param paused true is paused, otherwise is false.
     */
    fun refreshPausedState(paused: Boolean) {
        isPaused = paused
        notifyItemChanged(playingItemIndex)
    }

    /**
     * Set paused state
     *
     * @param paused true is paused, otherwise is false.
     */
    fun setPaused(paused: Boolean) {
        isPaused = paused
    }

    companion object {
        /**
         * Animation duration
         */
        const val ANIMATION_DURATION = 250L

        /**
         * The previous type of media item
         */
        const val TYPE_PREVIOUS = 1

        /**
         * The playing type playing media item
         */
        const val TYPE_PLAYING = 2

        /**
         * The next type next media item
         */
        const val TYPE_NEXT = 3
    }
}

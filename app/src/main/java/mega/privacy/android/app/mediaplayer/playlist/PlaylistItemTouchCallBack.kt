package mega.privacy.android.app.mediaplayer.playlist

import android.graphics.Canvas
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.R
import mega.privacy.android.app.mediaplayer.service.MediaPlayerServiceViewModel

/**
 * Implement the reorder playlist by drag the item
 * @param adapter PlaylistAdapter
 * @param playerViewModel MediaPlayerServiceViewModel
 */
class PlaylistItemTouchCallBack(
    private val adapter: PlaylistAdapter,
    private val playerViewModel: MediaPlayerServiceViewModel
) : ItemTouchHelper.Callback() {
    companion object {
        // Set the item's opacity for 95% when dragging
        private const val ALPHA_MOVING = 0.95f
        // Recover the opacity after dragged
        private const val ALPHA_FINISHED = 1f
    }

    private var isDragging = true
    private var isDragFinished = false
    private var isUpdatePlaySource = false

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        val swipeFlags = 0
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val currentPosition = viewHolder.absoluteAdapterPosition
        val targetPosition = target.absoluteAdapterPosition
        playerViewModel.run {
            // Only allow to reorder items of next list
            if (currentPosition > getNextHeaderPosition()
                && targetPosition > getNextHeaderPosition()) {
                isUpdatePlaySource = true
                swapItems(currentPosition, targetPosition)
                adapter.notifyItemMoved(currentPosition, targetPosition)
            }
        }
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        recyclerView.post{
            when {
                isDragging -> {
                    startCustomAnimator(viewHolder, ALPHA_MOVING, View.GONE)
                    isDragging = false
                }
                isDragFinished -> {
                    startCustomAnimator(viewHolder, ALPHA_FINISHED, View.VISIBLE)
                    isDragging = true
                    isDragFinished = false
                }
            }
        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    override fun isLongPressDragEnabled(): Boolean {
        return false
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        if (isUpdatePlaySource) {
            // After playlist reordered, update the play source of exoplayer
            playerViewModel.updatePlaySource()
            isUpdatePlaySource = false
        }
        isDragFinished = true
    }

    /**
     * Start the custom animator
     * @param viewHolder RecyclerView.ViewHolder
     * @param alpha the alpha of item view
     * @param visibility the visibility of item decorator
     */
    private fun startCustomAnimator(
        viewHolder: RecyclerView.ViewHolder,
        alpha: Float,
        visibility: Int
    ) {
        // According the item whether is moving to display or hide the decorator of RecycleView
        viewHolder.itemView.findViewById<View>(R.id.view_decorators).visibility = visibility
        val animator = viewHolder.itemView.animate()
        viewHolder.itemView.alpha = alpha
        animator.start()
    }
}
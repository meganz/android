package mega.privacy.android.app.mediaplayer.playlist

import android.graphics.Canvas
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.mediaplayer.gateway.PlayerServiceViewModelGateway
import mega.privacy.android.app.utils.Util

/**
 * Implement the reorder playlist by drag the item
 * @param adapter PlaylistAdapter
 * @param playerServiceViewModelGateway ServiceViewModelGateway
 * @param playlistItemDecoration customized item decoration
 */
class PlaylistItemTouchCallBack(
    private val adapter: PlaylistAdapter,
    private val playerServiceViewModelGateway: PlayerServiceViewModelGateway,
    private val playlistItemDecoration: PlaylistItemDecoration,
) : ItemTouchHelper.Callback() {
    companion object {
        // Set the item's opacity for 95% when dragging
        private const val ALPHA_MOVING = 0.95f

        // Recover the opacity after dragged
        private const val ALPHA_FINISHED = 1f

        private const val ELEVATION = 2f
        private const val ELEVATION_ZERO = 0f
    }

    private var isDragging = true
    private var isDragFinished = false
    private var isUpdatePlaySource = false

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        val swipeFlags = 0
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder,
    ): Boolean {
        val currentPosition = viewHolder.absoluteAdapterPosition
        val targetPosition = target.absoluteAdapterPosition
        playerServiceViewModelGateway.run {
            // Only allow to reorder items of next list
            if (currentPosition > getPlayingPosition() && targetPosition > getPlayingPosition()) {
                isUpdatePlaySource = true
                swapItems(currentPosition, targetPosition)
                adapter.notifyItemMoved(currentPosition, targetPosition)
            }
        }
        return true
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
        isCurrentlyActive: Boolean,
    ) {
        recyclerView.post {
            when {
                isDragging -> {
                    recyclerView.removeItemDecoration(playlistItemDecoration)
                    startCustomAnimator(viewHolder, ALPHA_MOVING, ELEVATION)
                    isDragging = false
                }
                isDragFinished -> {
                    recyclerView.addItemDecoration(playlistItemDecoration)
                    startCustomAnimator(viewHolder, ALPHA_FINISHED)
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
            playerServiceViewModelGateway.updatePlaySource()
            isUpdatePlaySource = false
        }
        isDragFinished = true
    }

    /**
     * Start the custom animator
     * @param viewHolder RecyclerView.ViewHolder
     * @param alpha the alpha of item view
     * @param elevation elevation
     */
    private fun startCustomAnimator(
        viewHolder: RecyclerView.ViewHolder,
        alpha: Float,
        elevation: Float = ELEVATION_ZERO,
    ) {
        val animator = viewHolder.itemView.animate()
        viewHolder.itemView.alpha = alpha
        viewHolder.itemView.translationZ = Util.dp2px(elevation).toFloat()
        animator.start()
    }
}
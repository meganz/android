package mega.privacy.android.app.mediaplayer.playlist

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.utils.Util

/**
 * Customized item decoration for recycle view of playlist page
 * @param dividerDrawable the divider for preview and playing areas
 * @param dividerDrawableNext the divider for next area
 * @param playlistAdapter PlaylistAdapter
 */
class PlaylistItemDecoration(
    private val dividerDrawable: Drawable?,
    private val dividerDrawableNext: Drawable?,
    private val playlistAdapter: PlaylistAdapter
) :
    RecyclerView.ItemDecoration() {

    override fun getItemOffsets(rect: Rect, v: View, parent: RecyclerView, s: RecyclerView.State) {
        parent.adapter?.run {
            val childAdapterPosition = parent.getChildAdapterPosition(v)
                .let {
                    if (it == RecyclerView.NO_POSITION) return else it
                }
            when (childAdapterPosition) {
                // The playing item don't add divider
                playlistAdapter.getPlayingPosition() -> {
                    0
                }
                // The last item add the 16dp bottom space
                itemCount - 1 -> {
                    rect.bottom = Util.dp2px(16f)
                }
                else -> {
                    dividerDrawable?.run {
                        rect.bottom = this.intrinsicHeight
                    }
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        parent.adapter?.let {
            parent.children // Displayed children on screen
                .forEach { view ->
                    val childAdapterPosition = parent.getChildAdapterPosition(view)
                        .let { if (it == RecyclerView.NO_POSITION) return else it }
                    val left = parent.paddingLeft
                    val top = view.bottom
                    val right = view.right
                    var bottom = 0
                    dividerDrawable?.run {
                        bottom = top + this.intrinsicHeight
                    }
                    if (childAdapterPosition > playlistAdapter.getPlayingPosition()) {
                        dividerDrawableNext?.run {
                            bounds = Rect(left, top, right, bottom)
                            draw(canvas)
                        }
                    } else {
                        dividerDrawable?.run {
                            bounds = Rect(left, top, right, bottom)
                            draw(canvas)
                        }
                    }
                }
        }
    }
}
package mega.privacy.android.app.components

import android.content.res.Resources
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView

class SpaceBetweenAdaptersDecoration<T : RecyclerView.Adapter<*>>(
    private val addAtTheEndOfAdapter: Class<T>,
    private val spaceDp: Int,
) : RecyclerView.ItemDecoration() {

    private val spacePx: Int by lazy {
        (spaceDp * Resources.getSystem().displayMetrics.density).toInt()
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position != RecyclerView.NO_POSITION) {
            val adapter = parent.adapter
            if (adapter is ConcatAdapter) {
                val currentAdapter = adapter.getWrappedAdapterAndPosition(position).first
                val nextAdapter =
                    if ((currentAdapter::class.java == addAtTheEndOfAdapter) && position + 1 < adapter.itemCount) {
                        adapter.getWrappedAdapterAndPosition(position + 1).first
                    } else {
                        null
                    }

                if (currentAdapter != null && nextAdapter != null && currentAdapter != nextAdapter) {
                    outRect.bottom = spacePx
                }
            }
        }
    }
}
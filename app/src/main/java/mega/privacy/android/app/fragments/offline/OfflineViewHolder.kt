package mega.privacy.android.app.fragments.offline

import android.view.View
import androidx.recyclerview.widget.RecyclerView

abstract class OfflineViewHolder(
    itemView: View,
    protected val listener: OfflineAdapterListener,
    protected val itemGetter: (Int) -> OfflineNode
) : RecyclerView.ViewHolder(itemView) {

    init {
        itemView.setOnClickListener {
            val position = adapterPosition
            listener.onNodeClicked(position, itemGetter(position))
        }

        itemView.setOnLongClickListener {
            val position = adapterPosition
            listener.onNodeLongClicked(position, itemGetter(position))
            true
        }
    }

    open fun bind(position: Int, node: OfflineNode) {
        node.uiDirty = false
    }
}

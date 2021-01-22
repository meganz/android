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
            handleNodeClicked(adapterPosition)
        }

        itemView.setOnLongClickListener {
            handleNodeLongClicked(adapterPosition)
            true
        }
    }

    open fun bind(position: Int, node: OfflineNode) {
        node.uiDirty = false
    }

    open fun handleNodeClicked(position: Int) {
        listener.onNodeClicked(position, itemGetter(position))
    }

    open fun handleNodeLongClicked(position: Int) {
        listener.onNodeLongClicked(position, itemGetter(position))
    }
}

package mega.privacy.android.app.fragments.offline

import android.view.View
import androidx.recyclerview.widget.RecyclerView

abstract class OfflineViewHolder(
    itemView: View,
    private val onNodeClicked: (Int, OfflineNode) -> Unit,
    private val onNodeLongClicked: (Int, OfflineNode) -> Unit,
) : RecyclerView.ViewHolder(itemView) {


    open fun bind(position: Int, node: OfflineNode) {
        node.uiDirty = false
        itemView.setOnClickListener {
            handleNodeClicked(bindingAdapterPosition, node)
        }

        itemView.setOnLongClickListener {
            handleNodeLongClicked(bindingAdapterPosition, node)
            true
        }
    }

    open fun handleNodeClicked(position: Int, node: OfflineNode) {
        onNodeClicked(position, node)
    }

    open fun handleNodeLongClicked(position: Int, node: OfflineNode) {
        onNodeLongClicked(position, node)
    }
}

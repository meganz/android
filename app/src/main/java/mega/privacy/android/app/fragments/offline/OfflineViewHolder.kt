package mega.privacy.android.app.fragments.offline

import android.view.View
import androidx.recyclerview.widget.RecyclerView

abstract class OfflineViewHolder(
    itemView: View
) : RecyclerView.ViewHolder(itemView) {
    fun bind(position: Int, node: OfflineNode, listener: OfflineAdapterListener) {
        itemView.setOnClickListener { listener.onNodeClicked(position, node) }
        itemView.setOnLongClickListener {
            listener.onNodeLongClicked(position, node)
            true
        }

        bind(node)
    }

    protected abstract fun bind(node: OfflineNode)
}

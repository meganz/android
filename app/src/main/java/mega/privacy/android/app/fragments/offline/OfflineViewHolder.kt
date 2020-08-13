package mega.privacy.android.app.fragments.offline

import android.view.View
import androidx.recyclerview.widget.RecyclerView

abstract class OfflineViewHolder(
    private val itemView: View
) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(node: OfflineNode)
}

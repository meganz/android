package mega.privacy.android.app.fragments.offline

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.databinding.OfflineItemGridFileBinding
import mega.privacy.android.app.databinding.OfflineItemGridFolderBinding
import mega.privacy.android.app.databinding.OfflineItemListBinding

private const val TYPE_LIST = 1
private const val TYPE_GRID_FOLDER = 2
private const val TYPE_GRID_FILE = 3

class OfflineAdapter(
    private val isList: Boolean
) : RecyclerView.Adapter<OfflineViewHolder>() {
    private val nodes = ArrayList<OfflineNode>()

    fun setNodes(nodes: List<OfflineNode>) {
        this.nodes.clear()
        this.nodes.addAll(nodes)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfflineViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_GRID_FOLDER -> OfflineGridFolderViewHolder(
                OfflineItemGridFolderBinding.inflate(inflater, parent, false)
            )
            TYPE_GRID_FILE -> OfflineGridFileViewHolder(
                OfflineItemGridFileBinding.inflate(inflater, parent, false)
            )
            else -> OfflineListViewHolder(OfflineItemListBinding.inflate(inflater, parent, false))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            isList -> TYPE_LIST
            nodes[position] == OfflineNode.PLACE_HOLDER
                    || nodes[position].node.isFolder -> TYPE_GRID_FOLDER
            else -> TYPE_GRID_FILE
        }
    }

    override fun getItemCount(): Int {
        return nodes.size
    }

    override fun onBindViewHolder(holder: OfflineViewHolder, position: Int) {
        holder.bind(nodes[position])
    }
}

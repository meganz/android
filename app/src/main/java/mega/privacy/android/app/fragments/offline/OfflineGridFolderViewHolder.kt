package mega.privacy.android.app.fragments.offline

import android.view.View
import androidx.core.content.ContextCompat
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.OfflineItemGridFolderBinding

class OfflineGridFolderViewHolder(
    private val binding: OfflineItemGridFolderBinding,
    onNodeClicked: (Int, OfflineNode) -> Unit,
    onNodeLongClicked: (Int, OfflineNode) -> Unit,
    private val onNodeOptionsClicked: (Int, OfflineNode) -> Unit,
) : OfflineViewHolder(binding.root, onNodeClicked, onNodeLongClicked) {
    override fun bind(position: Int, node: OfflineNode) {
        super.bind(position, node)

        if (node == OfflineNode.PLACE_HOLDER) {
            binding.root.visibility = View.INVISIBLE
            return
        }

        binding.threeDots.setOnClickListener {
            onNodeOptionsClicked(bindingAdapterPosition, node)
        }

        binding.root.visibility = View.VISIBLE
        binding.filename.text = node.node.name
        binding.icon.setImageResource(
            if (node.selected) R.drawable.ic_select_folder else R.drawable.ic_folder_list
        )

        binding.root.background = ContextCompat.getDrawable(
            binding.root.context,
            if (node.selected) R.drawable.background_item_grid_selected
            else R.drawable.background_item_grid
        )
    }
}

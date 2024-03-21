package mega.privacy.android.app.presentation.offline.adapter.viewholder

import android.view.View
import androidx.core.content.ContextCompat
import mega.privacy.android.core.R as CoreUiR
import mega.privacy.android.icon.pack.R as IconPackR
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.OfflineItemGridFolderBinding
import mega.privacy.android.app.presentation.offline.model.OfflineNode

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
            if (node.selected) CoreUiR.drawable.ic_select_folder else IconPackR.drawable.ic_folder_medium_solid
        )

        binding.root.background = ContextCompat.getDrawable(
            binding.root.context,
            if (node.selected) R.drawable.background_item_grid_selected
            else R.drawable.background_item_grid
        )
    }
}

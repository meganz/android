package mega.privacy.android.app.presentation.offline.adapter.viewholder

import android.view.View
import androidx.core.content.ContextCompat
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
    override fun bind(position: Int, node: OfflineNode, selectionMode: Boolean) {
        super.bind(position, node, selectionMode)

        if (node == OfflineNode.PLACE_HOLDER) {
            binding.root.visibility = View.INVISIBLE
            return
        }

        binding.threeDots.setOnClickListener {
            onNodeOptionsClicked(bindingAdapterPosition, node)
        }

        binding.root.visibility = View.VISIBLE
        binding.filename.text = node.node.name
        if (selectionMode) {
            binding.selectRadioButton.visibility = View.VISIBLE
            binding.threeDots.visibility = View.GONE
        } else {
            binding.threeDots.visibility = View.VISIBLE
            binding.selectRadioButton.visibility = View.GONE
        }
        binding.icon.setImageResource(
            IconPackR.drawable.ic_folder_medium_solid
        )

        binding.selectRadioButton.isChecked = node.selected

        binding.root.background = ContextCompat.getDrawable(
            binding.root.context,
            if (node.selected) R.drawable.background_item_grid_selected
            else R.drawable.background_item_grid
        )
    }
}

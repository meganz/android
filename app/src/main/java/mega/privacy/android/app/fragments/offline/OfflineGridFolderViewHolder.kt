package mega.privacy.android.app.fragments.offline

import android.view.View
import androidx.core.content.ContextCompat
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.OfflineItemGridFolderBinding

class OfflineGridFolderViewHolder(
    private val binding: OfflineItemGridFolderBinding
) : OfflineViewHolder(binding.root) {
    override fun bind(position: Int, node: OfflineNode, listener: OfflineAdapterListener) {
        super.bind(position, node, listener)

        binding.threeDots.setOnClickListener { listener.onOptionsClicked(position, node) }

        if (node == OfflineNode.PLACE_HOLDER) {
            binding.root.visibility = View.INVISIBLE
            return
        }

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

    fun getThumbnailView(): View {
        return binding.icon
    }
}

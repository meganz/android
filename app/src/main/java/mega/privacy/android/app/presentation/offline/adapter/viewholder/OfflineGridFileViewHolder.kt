package mega.privacy.android.app.presentation.offline.adapter.viewholder

import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.facebook.drawee.generic.RoundingParams
import mega.privacy.android.app.MimeTypeThumbnail
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.OfflineItemGridFileBinding
import mega.privacy.android.app.presentation.offline.model.OfflineNode
import mega.privacy.android.data.model.MimeTypeList

class OfflineGridFileViewHolder(
    private val binding: OfflineItemGridFileBinding,
    onNodeClicked: (Int, OfflineNode) -> Unit,
    onNodeLongClicked: (Int, OfflineNode) -> Unit,
    private val onNodeOptionsClicked: (Int, OfflineNode) -> Unit,
) : OfflineViewHolder(binding.root, onNodeClicked, onNodeLongClicked) {
    override fun bind(position: Int, node: OfflineNode, selectionMode: Boolean) {
        super.bind(position, node, selectionMode)
        binding.filenameContainer.setOnClickListener {
            onNodeOptionsClicked(bindingAdapterPosition, node)
        }
        binding.threeDots.setOnClickListener { onNodeOptionsClicked(bindingAdapterPosition, node) }
        binding.thumbnail.isVisible = true

        val placeHolderRes = MimeTypeThumbnail.typeForName(node.node.name).iconResourceId

        if (node.thumbnail != null) {
            binding.thumbnail.setImageURI(Uri.fromFile(node.thumbnail))
        } else {
            binding.thumbnail.hierarchy.setPlaceholderImage(placeHolderRes)
        }

        val params = binding.thumbnail.layoutParams

        if (params is FrameLayout.LayoutParams) {
            val realThumbnailSize =
                binding.root.resources.getDimensionPixelSize(R.dimen.grid_node_item_width)
            val defaultThumbnailSize =
                binding.root.resources.getDimensionPixelSize(R.dimen.grid_node_default_thumbnail_size)
            val defaultThumbnailMarginTop = (realThumbnailSize - defaultThumbnailSize) / 2

            params.width =
                if (node.thumbnail == null) defaultThumbnailSize else ViewGroup.LayoutParams.MATCH_PARENT
            params.height = if (node.thumbnail == null) defaultThumbnailSize else realThumbnailSize
            params.topMargin = if (node.thumbnail == null) defaultThumbnailMarginTop else 0
            binding.thumbnail.layoutParams = params
        }

        if (selectionMode) {
            binding.threeDots.isVisible = false
            binding.selectRadioButton.isVisible = true
            binding.selectRadioButton.isChecked = node.selected
        } else {
            binding.threeDots.isVisible = true
            binding.selectRadioButton.isVisible = false
        }
        if (MimeTypeList.typeForName(node.node.name).isVideo) {
            binding.playButton.isVisible = true
            binding.playButtonGradient.isVisible = true
        } else if (MimeTypeList.typeForName(node.node.name).isAudio) {
            binding.playButton.isVisible = false
            binding.playButtonGradient.isVisible = false
        } else {
            binding.playButton.isVisible = false
            binding.playButtonGradient.isVisible = false
        }
        val radius =
            binding.root.resources.getDimensionPixelSize(R.dimen.homepage_node_grid_round_corner_radius)
                .toFloat()
        binding.thumbnail.hierarchy.roundingParams =
            RoundingParams.fromCornersRadii(radius, radius, 0F, 0F)

        binding.filename.text = node.node.name

        binding.root.background = ContextCompat.getDrawable(
            binding.root.context,
            if (node.selected) R.drawable.background_item_grid_selected
            else R.drawable.background_item_grid
        )
    }

    fun getThumbnailView(): View {
        return binding.thumbnail
    }
}

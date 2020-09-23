package mega.privacy.android.app.fragments.offline

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

class OfflineGridFileViewHolder(
    private val binding: OfflineItemGridFileBinding,
    listener: OfflineAdapterListener,
    itemGetter: (Int) -> OfflineNode
) : OfflineViewHolder(binding.root, listener, itemGetter) {
    init {
        binding.filenameContainer.setOnClickListener {
            val position = adapterPosition
            listener.onOptionsClicked(position, itemGetter(position))
        }
    }

    override fun bind(position: Int, node: OfflineNode) {
        super.bind(position, node)

        val placeHolderRes = MimeTypeThumbnail.typeForName(node.node.name).iconResourceId

        if (node.thumbnail != null) {
            binding.thumbnail.setImageURI(Uri.fromFile(node.thumbnail))
        } else {
            binding.thumbnail.setActualImageResource(placeHolderRes)
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

        val radius =
            binding.root.resources.getDimensionPixelSize(R.dimen.homepage_node_grid_round_corner_radius)
                .toFloat()
        binding.thumbnail.hierarchy.roundingParams =
            RoundingParams.fromCornersRadii(radius, radius, 0F, 0F)

        binding.filename.text = node.node.name

        binding.icSelected.isVisible = node.selected

        binding.root.background = ContextCompat.getDrawable(
            binding.root.context,
            if (node.selected) R.drawable.background_item_grid_selected
            else R.drawable.background_item_grid_new
        )
    }

    fun getThumbnailView(): View {
        return binding.thumbnail
    }
}

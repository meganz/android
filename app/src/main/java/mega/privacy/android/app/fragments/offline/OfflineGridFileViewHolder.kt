package mega.privacy.android.app.fragments.offline

import android.net.Uri
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.facebook.drawee.generic.RoundingParams
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.OfflineItemGridFileBinding

class OfflineGridFileViewHolder(
    private val binding: OfflineItemGridFileBinding
) : OfflineViewHolder(binding.root) {
    override fun bind(position: Int, node: OfflineNode, listener: OfflineAdapterListener) {
        super.bind(position, node, listener)

        binding.filenameContainer.setOnClickListener { listener.onOptionsClicked(position, node) }

        val placeHolderRes = MimeTypeList.typeForName(node.node.name).iconResourceId

        if (node.thumbnail != null) {
            binding.thumbnail.setImageURI(Uri.fromFile(node.thumbnail))
        } else {
            binding.thumbnail.setActualImageResource(placeHolderRes)
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

    fun getIcSelected(): View {
        return binding.icSelected
    }
}

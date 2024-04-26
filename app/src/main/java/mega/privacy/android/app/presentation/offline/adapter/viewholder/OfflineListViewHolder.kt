package mega.privacy.android.app.presentation.offline.adapter.viewholder

import mega.privacy.android.core.R as CoreUiR
import mega.privacy.android.icon.pack.R as IconPackR
import android.net.Uri
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.facebook.drawee.generic.RoundingParams
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.databinding.OfflineItemListBinding
import mega.privacy.android.app.presentation.offline.model.OfflineNode
import mega.privacy.android.app.utils.Util.dp2px

class OfflineListViewHolder(
    private val binding: OfflineItemListBinding,
    onNodeClicked: (Int, OfflineNode) -> Unit,
    onNodeLongClicked: (Int, OfflineNode) -> Unit,
    private val onNodeOptionsClicked: (Int, OfflineNode) -> Unit,
) : OfflineViewHolder(binding.root, onNodeClicked, onNodeLongClicked) {
    override fun bind(position: Int, node: OfflineNode, selectionMode: Boolean) {
        super.bind(position, node, selectionMode)
        binding.threeDots.setOnClickListener {
            onNodeOptionsClicked(bindingAdapterPosition, node)
        }

        binding.threeDots.isVisible = selectionMode.not()
        binding.thumbnail.apply {
            isVisible = true
            if (node.selected) {
                setActualImageResource(CoreUiR.drawable.ic_select_folder)
            } else {
                setActualImageResource(0)
                val placeHolderRes = MimeTypeList.typeForName(node.node.name).iconResourceId
                if (node.thumbnail != null) {
                    setImageURI(Uri.fromFile(node.thumbnail))
                } else {
                    hierarchy.setPlaceholderImage(if (node.node.isFolder) IconPackR.drawable.ic_folder_medium_solid else placeHolderRes)
                }
            }
        }
        binding.filename.text = node.node.name
        binding.nodeInfo.text = node.nodeInfo
    }

    fun getThumbnailView(): View {
        return binding.thumbnail
    }

    companion object {
        const val LARGE_IMAGE_WIDTH = 48F
        const val LARGE_IMAGE_MARGIN_LEFT = 12F
    }
}

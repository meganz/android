package mega.privacy.android.app.presentation.offline.adapter.viewholder

import android.net.Uri
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.facebook.drawee.generic.RoundingParams
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.core.R as CoreUiR
import mega.privacy.android.app.databinding.OfflineItemListBinding
import mega.privacy.android.app.presentation.offline.model.OfflineNode
import mega.privacy.android.app.utils.Util.dp2px

class OfflineListViewHolder(
    private val binding: OfflineItemListBinding,
    onNodeClicked: (Int, OfflineNode) -> Unit,
    onNodeLongClicked: (Int, OfflineNode) -> Unit,
    private val onNodeOptionsClicked: (Int, OfflineNode) -> Unit,
) : OfflineViewHolder(binding.root, onNodeClicked, onNodeLongClicked) {
    override fun bind(position: Int, node: OfflineNode) {
        super.bind(position, node)
        binding.threeDots.setOnClickListener {
            onNodeOptionsClicked(bindingAdapterPosition, node)
        }
        binding.thumbnail.apply {
            isVisible = true

            if (node.selected) {
                hierarchy.setOverlayImage(
                    ContextCompat.getDrawable(
                        context,
                        CoreUiR.drawable.ic_select_folder
                    )
                )
            } else {
                hierarchy.setOverlayImage(null)
                val placeHolderRes = MimeTypeList.typeForName(node.node.name).iconResourceId

                if (node.thumbnail != null) {
                    setImageURI(Uri.fromFile(node.thumbnail))
                } else {
                    setActualImageResource(if (node.node.isFolder) CoreUiR.drawable.ic_folder_list else placeHolderRes)
                }

                hierarchy.roundingParams = RoundingParams.fromCornersRadius(5F)
            }
        }

        val res = binding.root.resources.displayMetrics
        val param = binding.thumbnail.layoutParams as FrameLayout.LayoutParams

        if (node.thumbnail == null || node.selected) {
            param.width = dp2px(LARGE_IMAGE_WIDTH, res)
            param.height = param.width
            param.marginStart = dp2px(LARGE_IMAGE_MARGIN_LEFT, res)
        } else {
            param.width = dp2px(SMALL_IMAGE_WIDTH, res)
            param.height = param.width
            param.marginStart = dp2px(SMALL_IMAGE_MARGIN_LEFT, res)
        }

        binding.thumbnail.layoutParams = param

        binding.filename.text = node.node.name
        binding.nodeInfo.text = node.nodeInfo
    }

    fun getThumbnailView(): View {
        return binding.thumbnail
    }

    companion object {
        const val LARGE_IMAGE_WIDTH = 48F
        const val LARGE_IMAGE_MARGIN_LEFT = 12F
        private const val SMALL_IMAGE_WIDTH = 36F
        private const val SMALL_IMAGE_MARGIN_LEFT = 18F
    }
}

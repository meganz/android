package mega.privacy.android.app.fragments.offline

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.OfflineItemListBinding
import mega.privacy.android.app.utils.Util.px2dp

private const val LARGE_IMAGE_WIDTH = 48F
private const val LARGE_IMAGE_MARGIN_LEFT = 12F
private const val SMALL_IMAGE_WIDTH = 36F
private const val SMALL_IMAGE_MARGIN_LEFT = 18F

class OfflineListViewHolder(
    private val binding: OfflineItemListBinding
) : OfflineViewHolder(binding.root) {
    override fun bind(position: Int, node: OfflineNode, listener: OfflineAdapterListener) {
        super.bind(position, node, listener)

        binding.threeDots.setOnClickListener { listener.onOptionsClicked(position, node) }

        if (node.selected) {
            binding.root.setBackgroundColor(
                ContextCompat.getColor(binding.root.context, R.color.new_multiselect_color)
            )

            Glide.with(binding.thumbnail)
                .load(R.drawable.ic_select_folder)
                .fitCenter()
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.thumbnail)
        } else {
            binding.root.setBackgroundColor(Color.WHITE)

            val placeHolderRes = MimeTypeList.typeForName(node.node.name).iconResourceId

            val requestBuilder: RequestBuilder<Drawable> = if (node.thumbnail != null) {
                Glide.with(binding.thumbnail)
                    .load(node.thumbnail)
                    .placeholder(placeHolderRes)
            } else {
                Glide.with(binding.thumbnail)
                    .load(if (node.node.isFolder) R.drawable.ic_folder_list else placeHolderRes)
            }

            requestBuilder
                .transform(FitCenter(), RoundedCorners(5))
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.thumbnail)
        }

        val res = binding.root.resources.displayMetrics
        val param = binding.thumbnail.layoutParams as FrameLayout.LayoutParams
        if (node.thumbnail == null || node.selected) {
            param.width = px2dp(LARGE_IMAGE_WIDTH, res)
            param.height = param.width
            param.marginStart = px2dp(LARGE_IMAGE_MARGIN_LEFT, res)
        } else {
            param.width = px2dp(SMALL_IMAGE_WIDTH, res)
            param.height = param.width
            param.marginStart = px2dp(SMALL_IMAGE_MARGIN_LEFT, res)
        }
        binding.thumbnail.layoutParams = param

        binding.filename.text = node.node.name
        binding.nodeInfo.text = node.nodeInfo
    }

    fun getThumbnailView(): View {
        return binding.thumbnail
    }
}

package mega.privacy.android.app.fragments.offline

import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.OfflineItemGridFileBinding
import mega.privacy.android.app.utils.Util.px2dp

class OfflineGridFileViewHolder(
    private val binding: OfflineItemGridFileBinding
) : OfflineViewHolder(binding.root) {
    override fun bind(position: Int, node: OfflineNode, listener: OfflineAdapterListener) {
        super.bind(position, node, listener)

        binding.filenameContainer.setOnClickListener { listener.onOptionsClicked(position, node) }

        val placeHolderRes = MimeTypeList.typeForName(node.node.name).iconResourceId

        val requestBuilder: RequestBuilder<Drawable> = if (node.thumbnail != null) {
            Glide.with(binding.thumbnail)
                .load(node.thumbnail)
                .placeholder(placeHolderRes)
        } else {
            Glide.with(binding.thumbnail)
                .load(placeHolderRes)
        }

        val radius = px2dp(5F, binding.root.resources.displayMetrics).toFloat()
        requestBuilder
            .transform(FitCenter(), GranularRoundedCorners(radius, radius, 0F, 0F))
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.thumbnail)

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

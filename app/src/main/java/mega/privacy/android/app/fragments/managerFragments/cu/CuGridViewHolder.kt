package mega.privacy.android.app.fragments.managerFragments.cu

import android.net.Uri
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.generic.RoundingParams
import com.facebook.drawee.view.SimpleDraweeView
import mega.privacy.android.app.R
import mega.privacy.android.app.gallery.data.GalleryItemSizeConfig
import mega.privacy.android.app.utils.ZoomUtil

abstract class CuGridViewHolder(
    val itemView: View
) : RecyclerView.ViewHolder(itemView) {

    companion object {

        @JvmStatic
        fun updateThumbnailDisplay(
            thumbnail: SimpleDraweeView,
            node: CuNode,
            itemSizeConfig: GalleryItemSizeConfig
        ) {
            // force set the thumbnail visible, in case FullscreenImageViewer/AudioVideoPlayer
            // doesn't call setVisibility when dismissed
            thumbnail.visibility = View.VISIBLE
            if (node.thumbnail != null) {
                thumbnail.setImageURI(Uri.fromFile(node.thumbnail))
            } else {
                thumbnail.setActualImageResource(R.drawable.ic_image_thumbnail)
            }
            thumbnail.hierarchy.roundingParams = RoundingParams.fromCornersRadius(
                (if (node.isSelected) itemSizeConfig.roundCornerRadius else 0).toFloat()
            )
            val imagePadding = if (node.isSelected) itemSizeConfig.imageSelectedPadding else 0
            thumbnail.setPadding(imagePadding, imagePadding, imagePadding, imagePadding)
            if (node.isSelected) {
                thumbnail.background = ContextCompat.getDrawable(
                    thumbnail.context,
                    R.drawable.background_item_grid_selected
                )
            } else {
                thumbnail.background = null
            }
        }

        @JvmStatic
        fun setViewSize(grid: View, icSelected: View, itemSizeConfig: GalleryItemSizeConfig) {
            val params = grid.layoutParams as GridLayoutManager.LayoutParams
            params.width = itemSizeConfig.gridSize
            params.height = itemSizeConfig.gridSize

            if (itemSizeConfig.zoom == ZoomUtil.ZOOM_IN_1X) {
                params.marginStart = 0
                params.marginEnd = 0
            } else {
                params.marginStart = itemSizeConfig.imageMargin
                params.marginEnd = itemSizeConfig.imageMargin
            }

            params.bottomMargin = itemSizeConfig.imageMargin
            params.topMargin = itemSizeConfig.imageMargin
            grid.layoutParams = params

            val icSelectedParams = (icSelected.layoutParams as FrameLayout.LayoutParams).also {
                it.width = itemSizeConfig.icSelectedSize
                it.height = itemSizeConfig.icSelectedSize
                it.topMargin = itemSizeConfig.icSelectedMargin
                it.marginStart = itemSizeConfig.icSelectedMargin

            }
            icSelected.layoutParams = icSelectedParams
        }
    }

    fun bind(position: Int, node: CuNode, listener: CUGridViewAdapter.Listener) {
        if (handleClick() && node.node != null) {
            itemView.setOnClickListener { v: View? ->
                listener.onNodeClicked(
                    position,
                    node
                )
            }
            itemView.setOnLongClickListener { v: View? ->
                listener.onNodeLongClicked(position, node)
                true
            }
        }

        bind(node)
    }

    protected abstract fun bind(node: CuNode)

    protected open fun handleClick() = true
}
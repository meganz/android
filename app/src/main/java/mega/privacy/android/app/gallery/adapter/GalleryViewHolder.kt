package mega.privacy.android.app.gallery.adapter

import android.net.Uri
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.generic.RoundingParams
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.request.ImageRequestBuilder
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemGalleryImageBinding
import mega.privacy.android.app.databinding.ItemGalleryTitleBinding
import mega.privacy.android.app.databinding.ItemGalleryVideoBinding
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.fragments.homepage.ActionModeViewModel
import mega.privacy.android.app.fragments.homepage.ItemOperationViewModel
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.data.GalleryItemSizeConfig
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.ZoomUtil.ZOOM_DEFAULT
import mega.privacy.android.app.utils.ZoomUtil.ZOOM_IN_1X
import mega.privacy.android.app.utils.ZoomUtil.ZOOM_OUT_1X
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

class GalleryViewHolder(
        val binding: ViewDataBinding,
        private val mItemSizeConfig: GalleryItemSizeConfig
) : RecyclerView.ViewHolder(binding.root) {

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    companion object {

        @JvmStatic
        fun updateThumbnailDisplay(
                thumbnail: SimpleDraweeView,
                item: GalleryItem,
                itemSizeConfig: GalleryItemSizeConfig
        ) {
            // force set the thumbnail visible, in case FullscreenImageViewer/AudioVideoPlayer
            // doesn't call setVisibility when dismissed
            thumbnail.visibility = View.VISIBLE
            if (item.thumbnail != null) {
                val request = ImageRequestBuilder.newBuilderWithSource(Uri.fromFile(item.thumbnail)).build()
                val controller = Fresco.newDraweeControllerBuilder()
                        .setImageRequest(request)
                        .setOldController(thumbnail.controller)
                        .build()
                thumbnail.controller = controller
            } else {
                thumbnail.setActualImageResource(R.drawable.ic_image_thumbnail)
            }
            thumbnail.hierarchy.roundingParams = RoundingParams.fromCornersRadius(
                    (if (item.selected) itemSizeConfig.roundCornerRadius else 0).toFloat()
            )
            val imagePadding = if (item.selected) itemSizeConfig.imageSelectedPadding else 0
            thumbnail.setPadding(imagePadding, imagePadding, imagePadding, imagePadding)
            if (item.selected) {
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

            if (itemSizeConfig.zoom == ZOOM_IN_1X) {
                params.marginStart = 0
                params.marginEnd = 0
            } else {
                params.marginStart = itemSizeConfig.imageMargin
                params.marginEnd = itemSizeConfig.imageMargin
            }

            params.bottomMargin = itemSizeConfig.imageMargin
            params.topMargin = itemSizeConfig.imageMargin
            grid.layoutParams = params

            val icSelectedParams = (icSelected.layoutParams as ConstraintLayout.LayoutParams).also {
                it.width = itemSizeConfig.icSelectedSize
                it.height = itemSizeConfig.icSelectedSize
                it.topMargin = itemSizeConfig.icSelectedMargin
                it.marginStart = itemSizeConfig.icSelectedMargin

            }
            icSelected.layoutParams = icSelectedParams
        }
    }

    fun bind(
            actionModeViewModel: ActionModeViewModel,
            itemOperationViewModel: ItemOperationViewModel?,
            item: GalleryItem
    ) {
        binding.apply {
            when (this) {
                is ItemGalleryImageBinding -> {
                    this.actionModeViewModel = actionModeViewModel
                    this.itemOperationViewModel = itemOperationViewModel
                    this.item = item

                    setViewSize(
                            root,
                            iconSelected,
                            mItemSizeConfig
                    )

                    updateThumbnailDisplay(
                            thumbnail,
                            item,
                            mItemSizeConfig
                    )

                    handleFavouriteUI(
                        zoom = mItemSizeConfig.zoom,
                        isFavorite = item.node?.isFavourite,
                        favouriteView = favoriteOverlay
                    )

                }
                is ItemGalleryVideoBinding -> {
                    this.actionModeViewModel = actionModeViewModel
                    this.itemOperationViewModel = itemOperationViewModel
                    this.item = item

                    setViewSize(
                            root,
                            iconSelected,
                            mItemSizeConfig
                    )

                    when (mItemSizeConfig.zoom) {
                        ZOOM_IN_1X, ZOOM_DEFAULT, ZOOM_OUT_1X -> {
                            if (item.node != null) {
                                videoDuration.visibility = View.VISIBLE
                                videoDuration.text = TimeUtils.getVideoDuration(
                                        item.node!!.duration
                                )
                            } else {
                                videoDuration.visibility = View.GONE
                            }
                        }
                        else -> {
                            videoDuration.visibility = View.GONE
                        }
                    }

                    handleFavouriteUI(
                        zoom = mItemSizeConfig.zoom,
                        isFavorite = item.node?.isFavourite,
                        favouriteView = favouriteIcon)

                    videoInfo.setBackgroundResource(
                            if (item.selected) R.drawable.grid_cam_uploads_rounded else R.color.grey_alpha_032
                    )

                    updateThumbnailDisplay(
                            thumbnail,
                            item,
                            mItemSizeConfig
                    )
                }
                is ItemGalleryTitleBinding -> {
                    this.item = item
                }
            }
        }

        item.uiDirty = false
    }

    private fun handleFavouriteUI(zoom: Int, isFavorite: Boolean?, favouriteView: View) {
        when (zoom) {
            ZOOM_IN_1X, ZOOM_DEFAULT, ZOOM_OUT_1X -> {
                if (isFavorite == true) {
                    favouriteView.visibility = View.VISIBLE
                } else {
                    favouriteView.visibility = View.GONE
                }
            }
            else -> {
                favouriteView.visibility = View.GONE
            }
        }
    }
}
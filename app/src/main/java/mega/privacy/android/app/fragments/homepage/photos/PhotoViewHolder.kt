package mega.privacy.android.app.fragments.homepage.photos

import androidx.core.view.isVisible
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.databinding.ItemGalleryImageBinding
import mega.privacy.android.app.databinding.ItemGalleryTitleBinding
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.fragments.homepage.ActionModeViewModel
import mega.privacy.android.app.fragments.homepage.ItemOperationViewModel
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.utils.ZoomUtil
import mega.privacy.android.app.utils.ZoomUtil.getMargin
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

class PhotoViewHolder(val binding: ViewDataBinding, private val zoom: Int = ZoomUtil.ZOOM_DEFAULT) :
    RecyclerView.ViewHolder(binding.root) {

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

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

                    thumbnail.isVisible = true

                    val layoutParams = root.layoutParams as GridLayoutManager.LayoutParams
                    val imageMargin = getMargin(root.context, zoom)

                    if (zoom == ZoomUtil.ZOOM_IN_1X) {
                        layoutParams.setMargins(0, imageMargin, 0, imageMargin)
                    } else {
                        layoutParams.setMargins(imageMargin, imageMargin, imageMargin, imageMargin)
                    }

                    root.layoutParams = layoutParams
                }
                is ItemGalleryTitleBinding -> {
                    this.item = item
                }
            }
        }

        item.uiDirty = false
    }
}
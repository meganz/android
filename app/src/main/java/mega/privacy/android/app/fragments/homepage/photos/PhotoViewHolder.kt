package mega.privacy.android.app.fragments.homepage.photos

import androidx.core.view.isVisible
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemNodeListBinding
import mega.privacy.android.app.databinding.ItemPhotoBrowseBinding
import mega.privacy.android.app.databinding.ItemPhotosTitleBinding
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.fragments.homepage.ActionModeViewModel
import mega.privacy.android.app.fragments.homepage.ItemOperationViewModel
import mega.privacy.android.app.utils.ZoomUtil
import mega.privacy.android.app.utils.ZoomUtil.getMargin
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

class PhotoViewHolder(val binding: ViewDataBinding, private val zoom: Int = 0) :
    RecyclerView.ViewHolder(binding.root) {

    @MegaApi
    @Inject
    lateinit var megaApi: MegaApiAndroid

    fun bind(
        actionModeViewModel: ActionModeViewModel,
        itemOperationViewModel: ItemOperationViewModel?,
        item: PhotoNodeItem
    ) {
        binding.apply {
            when (this) {
                is ItemPhotoBrowseBinding -> {
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
                is ItemNodeListBinding -> {
                    this.itemOperationViewModel = itemOperationViewModel
                    this.actionModeViewModel = actionModeViewModel
                    this.item = item
                    this.megaApi = megaApi

                    thumbnail.isVisible = true

                    // This convoluted logic is a workaround for an UI issue of Fresco placeholder :
                    // The placeholder(If set) image shows up transiently before showing the "tick"
                    // image when the user long press an item for the very first time
                    if (actionModeViewModel.selectedNodes.value?.isEmpty() == true ||
                        actionModeViewModel.longClick.value?.peekContent()?.index != item.index ||
                        !item.selected
                    ) {
                        thumbnail.hierarchy.setPlaceholderImage(R.drawable.ic_image_list)
                    } else {
                        thumbnail.hierarchy.setPlaceholderImage(null)
                    }
                }
                is ItemPhotosTitleBinding -> {
                    this.item = item
                }
            }
        }

        item.uiDirty = false
    }
}
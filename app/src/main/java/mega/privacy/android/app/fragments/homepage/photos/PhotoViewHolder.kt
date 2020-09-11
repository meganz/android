package mega.privacy.android.app.fragments.homepage.photos

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemNodeListBinding
import mega.privacy.android.app.databinding.ItemPhotoBrowseBinding
import mega.privacy.android.app.databinding.ItemPhotosTitleBinding
import mega.privacy.android.app.fragments.homepage.ActionModeViewModel
import mega.privacy.android.app.fragments.homepage.ItemOperationViewModel
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

class PhotoViewHolder(private val binding: ViewDataBinding) :
    RecyclerView.ViewHolder(binding.root) {

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
                }
                is ItemNodeListBinding -> {
                    this.itemOperationViewModel = itemOperationViewModel
                    this.actionModeViewModel = actionModeViewModel
                    this.item = item
                    this.megaApi = megaApi

                    // This convoluted logic is a workaround for an UI issue of Fresco placeholder :
                    // The placeholder(If set) image shows up transiently before showing the "tick"
                    // image when the user long press an item for the very first time
                    if (actionModeViewModel.selectedNodes.value?.isEmpty() == true ||
                        actionModeViewModel.longClick.value?.peekContent()?.index != item.index ||
                            !item.selected) {
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
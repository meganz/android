package mega.privacy.android.app.fragments.photos

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.databinding.ItemPhotoBrowseBinding
import mega.privacy.android.app.databinding.ItemPhotoSearchBinding
import mega.privacy.android.app.databinding.ItemPhotosTitleBinding
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

class PhotoViewHolder(private val binding: ViewDataBinding) :
    RecyclerView.ViewHolder(binding.root) {

    @Inject
    lateinit var megaApi: MegaApiAndroid

    fun bind(
        viewModel: PhotosViewModel,
        actionModeViewModel: ActionModeViewModel,
        item: PhotoNode
    ) {
        binding.apply {
            when (this) {
                is ItemPhotoBrowseBinding -> {
                    this.viewModel = viewModel
                    this.actionModeViewModel = actionModeViewModel
                    this.item = item
                }
                is ItemPhotoSearchBinding -> {
                    this.viewModel = viewModel
                    this.actionModeViewModel = actionModeViewModel
                    this.item = item
                    this.megaApi = megaApi
                }
                is ItemPhotosTitleBinding -> {
                    this.item = item
                }
            }
        }

        item.uiDirty = false
    }
}
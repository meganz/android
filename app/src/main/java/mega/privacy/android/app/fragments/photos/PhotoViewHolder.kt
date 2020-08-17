package mega.privacy.android.app.fragments.photos

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.databinding.ItemPhotoBrowseBinding
import mega.privacy.android.app.databinding.ItemPhotoSearchBinding
import mega.privacy.android.app.databinding.ItemPhotosTitleBinding

class PhotoViewHolder(private val binding: ViewDataBinding) :
    RecyclerView.ViewHolder(binding.root) {

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
                }
                is ItemPhotosTitleBinding -> {
                    this.item = item
                }
            }
        }

        item.uiDirty = false
    }
}
package mega.privacy.android.app.fragments.photos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider
import mega.privacy.android.app.databinding.ItemPhotoBrowseBinding
import mega.privacy.android.app.databinding.ItemPhotoSearchBinding
import mega.privacy.android.app.databinding.ItemPhotosTitleBinding
import javax.inject.Inject

class PhotosSearchAdapter @Inject constructor(
    private val viewModel: PhotosViewModel,
    private val actionModeViewModel: ActionModeViewModel
) : ListAdapter<PhotoNode, PhotoViewHolder>(PhotoDiffCallback()),
    SectionTitleProvider {

    override fun getItemViewType(position: Int): Int {
        return getItem(position).type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        val binding = when (viewType) {
            PhotoNode.TYPE_TITLE ->
                ItemPhotosTitleBinding.inflate(
                    inflater,
                    parent,
                    false
                )
            else ->  // TYPE_PHOTO
                ItemPhotoSearchBinding.inflate(
                    inflater,
                    parent,
                    false
                )
        }

        // TYPE_TITLE views take layout positions for facilitating the logic calculation
        if (viewType == PhotoNode.TYPE_TITLE) {
            val layoutParams = binding.root.layoutParams
            layoutParams.width = 0
            layoutParams.height = 0
            binding.root.layoutParams = layoutParams
        }

        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(viewModel, actionModeViewModel, getItem(position))
    }

    private class PhotoDiffCallback : DiffUtil.ItemCallback<PhotoNode>() {
        override fun areItemsTheSame(oldItem: PhotoNode, newItem: PhotoNode): Boolean {
            return oldItem.node?.handle == newItem.node?.handle
        }

        override fun areContentsTheSame(oldItem: PhotoNode, newItem: PhotoNode): Boolean {
            if (newItem.uiDirty) {
                return false
            }

            return true
        }
    }

    override fun getSectionTitle(position: Int) = if (position < 0 || position >= itemCount) {
        ""
    } else getItem(position).modifiedDate
}

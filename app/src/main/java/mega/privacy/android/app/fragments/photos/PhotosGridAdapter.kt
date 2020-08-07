package mega.privacy.android.app.fragments.photos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import mega.privacy.android.app.databinding.ItemPhotoBinding
import mega.privacy.android.app.databinding.ItemPhotosTitleBinding
import javax.inject.Inject

class PhotosGridAdapter @Inject constructor(/*private val viewModel: PhotosViewModel*/) :
    ListAdapter<PhotoNode, PhotosGridAdapter.PhotoViewHolder>(PhotoDiffCallback()) {
    class PhotoViewHolder(private val binding: ViewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
//            if (binding is ItemPhotoBinding) {
//                val params =
//                    binding.root.layoutParams
//                params.width = 200
//                params.height = 200
//                binding.root.layoutParams = params
//            }
        }

        fun bind(item: PhotoNode) {
            if (binding is ItemPhotosTitleBinding) {
                binding.gridTitle.text = item.modifiedDate
            } else if (binding is ItemPhotoBinding) {
                binding.item = item
            }
        }
    }

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
                ItemPhotoBinding.inflate(
                    inflater,
                    parent,
                    false
                )
        }

        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class PhotoDiffCallback : DiffUtil.ItemCallback<PhotoNode>() {
        override fun areItemsTheSame(oldItem: PhotoNode, newItem: PhotoNode): Boolean {
            return oldItem.node?.handle == newItem.node?.handle
        }

        override fun areContentsTheSame(oldItem: PhotoNode, newItem: PhotoNode): Boolean {
            return oldItem.thumbnail == newItem.thumbnail
        }
    }


}
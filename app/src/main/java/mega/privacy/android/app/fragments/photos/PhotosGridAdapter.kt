package mega.privacy.android.app.fragments.photos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider
import mega.privacy.android.app.databinding.ItemPhotoBinding
import mega.privacy.android.app.databinding.ItemPhotosTitleBinding
import mega.privacy.android.app.fragments.managerFragments.cu.CuItemSizeConfig
import javax.inject.Inject

class PhotosGridAdapter @Inject constructor(/*private val viewModel: PhotosViewModel*/) :
    ListAdapter<PhotoNode, PhotosGridAdapter.PhotoViewHolder>(PhotoDiffCallback()),
    SectionTitleProvider {

    private var itemSizeConfig: CuItemSizeConfig? = null

    class PhotoViewHolder(private val binding: ViewBinding) :
        RecyclerView.ViewHolder(binding.root) {

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

        if (viewType == PhotoNode.TYPE_PHOTO && itemSizeConfig != null) {
            setItemLayoutParams(binding)
            // FastScroller would affect the normal process of RecyclerView that makes the "selected"
            // icon appear before binding the item. Therefore, hide the icon up front
            (binding as ItemPhotoBinding).iconSelected.visibility = View.GONE
        }

        return PhotoViewHolder(binding)
    }

    private fun setItemLayoutParams(binding: ViewBinding) {
        (binding.root.layoutParams as GridLayoutManager.LayoutParams).apply {
            with(itemSizeConfig!!) {
                gridSize.let {
                    width = it
                    height = it
                }
                gridMargin.let {
                    topMargin = it
                    bottomMargin = it
                    marginStart = it
                    marginEnd = it
                }
            }
        }
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

    fun setItemSizeConfig(config: CuItemSizeConfig) {
        itemSizeConfig = config
    }

    fun getSpanSizeLookup(spanCount: Int) = object : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int {
            return when (getItem(position).type) {
                PhotoNode.TYPE_TITLE -> spanCount
                else -> 1
            }
        }
    }

    override fun getSectionTitle(position: Int) = if (position < 0 || position >= itemCount) {
        ""
    } else getItem(position).modifiedDate
}
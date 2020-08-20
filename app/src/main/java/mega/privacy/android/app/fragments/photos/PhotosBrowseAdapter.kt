package mega.privacy.android.app.fragments.photos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.viewbinding.ViewBinding
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider
import mega.privacy.android.app.databinding.ItemPhotoBrowseBinding
import mega.privacy.android.app.databinding.ItemPhotosTitleBinding
import javax.inject.Inject

class PhotosBrowseAdapter @Inject constructor(
    private val viewModel: PhotosViewModel,
    private val actionModeViewModel: ActionModeViewModel
) : ListAdapter<PhotoNode, PhotoViewHolder>(PhotoDiffCallback()),
    SectionTitleProvider {

    private var itemDimen = 0

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
                ItemPhotoBrowseBinding.inflate(
                    inflater,
                    parent,
                    false
                )
        }

        if (viewType == PhotoNode.TYPE_PHOTO && itemDimen > 0) {
            setItemLayoutParams(binding)
            // FastScroller would affect the normal process of RecyclerView that makes the "selected"
            // icon appear before binding the item. Therefore, hide the icon up front
            (binding as ItemPhotoBrowseBinding).iconSelected.visibility = View.GONE
        }

        return PhotoViewHolder(binding)
    }

    private fun setItemLayoutParams(binding: ViewBinding) {
        (binding.root.layoutParams as GridLayoutManager.LayoutParams).apply {
            width = itemDimen
            height = itemDimen
        }
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

    fun setItemDimen(dimen: Int) {
        if (dimen > 0) itemDimen = dimen
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

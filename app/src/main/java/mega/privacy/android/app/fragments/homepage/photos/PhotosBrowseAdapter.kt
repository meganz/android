package mega.privacy.android.app.fragments.homepage.photos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import mega.privacy.android.app.components.dragger.DragThumbnailGetter
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider
import mega.privacy.android.app.databinding.ItemGalleryImageBinding
import mega.privacy.android.app.databinding.ItemGalleryTitleBinding
import mega.privacy.android.app.fragments.homepage.ActionModeViewModel
import mega.privacy.android.app.fragments.homepage.ItemOperationViewModel
import mega.privacy.android.app.gallery.data.GalleryItem

class PhotosBrowseAdapter constructor(
    private val actionModeViewModel: ActionModeViewModel,
    private val itemOperationViewModel: ItemOperationViewModel,
    private val zoom: Int
) : ListAdapter<GalleryItem, PhotoViewHolder>(GalleryItem.DiffCallback()),
    SectionTitleProvider, DragThumbnailGetter {

    private var itemDimen = 0

    override fun getNodePosition(handle: Long) =
        currentList.indexOfFirst { it.node?.handle == handle }

    override fun getThumbnail(viewHolder: RecyclerView.ViewHolder): View? {
        if (viewHolder is PhotoViewHolder && viewHolder.binding is ItemGalleryImageBinding) {
            return viewHolder.binding.thumbnail
        }

        return null
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        val binding = when (viewType) {
            GalleryItem.TYPE_HEADER ->
                ItemGalleryTitleBinding.inflate(
                    inflater,
                    parent,
                    false
                )
            else ->  // TYPE_PHOTO
                ItemGalleryImageBinding.inflate(
                    inflater,
                    parent,
                    false
                )
        }

        if (viewType == GalleryItem.TYPE_IMAGE && itemDimen > 0) {
            setItemLayoutParams(binding)
            // FastScroller would affect the normal process of RecyclerView that makes the "selected"
            // icon appear before binding the item. Therefore, hide the icon up front
            (binding as ItemGalleryImageBinding).iconSelected.visibility = View.GONE
        }

        return PhotoViewHolder(binding, zoom)
    }

    private fun setItemLayoutParams(binding: ViewBinding) {
        (binding.root.layoutParams as GridLayoutManager.LayoutParams).apply {
            width = itemDimen
            height = itemDimen
        }
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(actionModeViewModel, itemOperationViewModel, getItem(position))
    }

    fun setItemDimen(dimen: Int) {
        if (dimen > 0) itemDimen = dimen
    }

    fun getSpanSizeLookup(spanCount: Int) = object : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int {
            return when (getItem(position).type) {
                GalleryItem.TYPE_HEADER -> spanCount
                else -> 1
            }
        }
    }

    fun getNodeAtPosition(position: Int): GalleryItem? {
        return if (position >= 0 && position < currentList.size) currentList[position] else null
    }

    override fun getSectionTitle(position: Int) = if (position < 0 || position >= itemCount) {
        ""
    } else getItem(position).modifyDate
}

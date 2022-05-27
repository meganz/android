package mega.privacy.android.app.gallery.adapter

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
import mega.privacy.android.app.databinding.ItemGalleryVideoBinding
import mega.privacy.android.app.fragments.homepage.ActionModeViewModel
import mega.privacy.android.app.fragments.homepage.ItemOperationViewModel
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.data.GalleryItemSizeConfig
import mega.privacy.android.app.gallery.data.MediaType

class GalleryAdapter(
        private val actionModeViewModel: ActionModeViewModel,
        private val itemOperationViewModel: ItemOperationViewModel,
        private var itemSizeConfig: GalleryItemSizeConfig
) : ListAdapter<GalleryItem, GalleryViewHolder>(GalleryItem.DiffCallback()),
        SectionTitleProvider, DragThumbnailGetter {

    private var itemDimen = 0

    override fun getNodePosition(handle: Long) =
            currentList.indexOfFirst { it.node?.handle == handle }

    override fun getThumbnail(viewHolder: RecyclerView.ViewHolder): View? {
        if (viewHolder is GalleryViewHolder) {
            return when (viewHolder.binding) {
                is ItemGalleryImageBinding -> viewHolder.binding.thumbnail
                is ItemGalleryVideoBinding -> viewHolder.binding.thumbnail

                else -> null
            }
        }

        return null
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).type.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        val binding = when (MediaType.values()[viewType]) {
            MediaType.Image ->
                ItemGalleryImageBinding.inflate(
                        inflater,
                        parent,
                        false
                ).also {
                    if (itemDimen > 0) {
                        setItemLayoutParams(it)
                        it.iconSelected.visibility = View.GONE
                    }
                }
            MediaType.Video ->
                ItemGalleryVideoBinding.inflate(
                        inflater,
                        parent,
                        false
                ).also {
                    if (itemDimen > 0) {
                        setItemLayoutParams(it)
                        it.iconSelected.visibility = View.GONE
                    }
                }
            MediaType.Header ->
                ItemGalleryTitleBinding.inflate(
                        inflater,
                        parent,
                        false
                )
        }

        return GalleryViewHolder(binding, itemSizeConfig)
    }

    private fun setItemLayoutParams(binding: ViewBinding) {
        (binding.root.layoutParams as GridLayoutManager.LayoutParams).apply {
            width = itemDimen
            height = itemDimen
        }
    }

    override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
        holder.bind(actionModeViewModel, itemOperationViewModel, getItem(position))
    }

    fun setItemDimen(dimen: Int) {
        if (dimen > 0) itemDimen = dimen
    }

    fun getSpanSizeLookup(spanCount: Int) = object : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int {
            return when (getItem(position).type) {
                MediaType.Header -> spanCount
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
package mega.privacy.android.app.fragments.homepage.photos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.components.dragger.DragThumbnailGetter
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider
import mega.privacy.android.app.databinding.ItemNodeListBinding
import mega.privacy.android.app.fragments.homepage.ActionModeViewModel
import mega.privacy.android.app.fragments.homepage.ItemOperationViewModel
import mega.privacy.android.app.fragments.homepage.NodeDiffCallback

class PhotosSearchAdapter constructor(
    private val actionModeViewModel: ActionModeViewModel,
    private val itemOperationViewModel: ItemOperationViewModel
) : ListAdapter<PhotoNodeItem, PhotoViewHolder>(NodeDiffCallback()),
    SectionTitleProvider, DragThumbnailGetter {

    override fun getNodePosition(handle: Long) =
        currentList.indexOfFirst { it.node?.handle == handle }

    override fun getThumbnail(viewHolder: RecyclerView.ViewHolder): View? {
        if (viewHolder is PhotoViewHolder && viewHolder.binding is ItemNodeListBinding) {
            return viewHolder.binding.thumbnail
        }

        return null
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        val binding = ItemNodeListBinding.inflate(
            inflater,
            parent,
            false
        )

        binding.publicLink.visibility = View.GONE
        binding.savedOffline.visibility = View.GONE
        binding.takenDown.visibility = View.GONE
        binding.versionsIcon.visibility = View.GONE

        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(actionModeViewModel, itemOperationViewModel, getItem(position))
    }

    override fun getSectionTitle(position: Int) = if (position < 0 || position >= itemCount) {
        ""
    } else getItem(position).modifiedDate
}

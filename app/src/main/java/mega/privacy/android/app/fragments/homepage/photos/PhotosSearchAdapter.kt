package mega.privacy.android.app.fragments.homepage.photos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.R
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider
import mega.privacy.android.app.databinding.ItemNodeListBinding
import mega.privacy.android.app.fragments.homepage.ActionModeViewModel

class PhotosSearchAdapter constructor(
    private val viewModel: PhotosViewModel,
    private val actionModeViewModel: ActionModeViewModel
) : ListAdapter<PhotoNodeItem, PhotoViewHolder>(PhotoDiffCallback()),
    SectionTitleProvider {

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

        binding.thumbnail.hierarchy.setPlaceholderImage(R.drawable.ic_image_list)

        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(viewModel, actionModeViewModel, getItem(position))
    }

    private class PhotoDiffCallback : DiffUtil.ItemCallback<PhotoNodeItem>() {
        override fun areItemsTheSame(oldItem: PhotoNodeItem, newItem: PhotoNodeItem): Boolean {
            return oldItem.node?.handle == newItem.node?.handle
        }

        override fun areContentsTheSame(oldItem: PhotoNodeItem, newItem: PhotoNodeItem): Boolean {
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

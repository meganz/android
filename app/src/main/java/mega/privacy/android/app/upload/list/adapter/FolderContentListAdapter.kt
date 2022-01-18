package mega.privacy.android.app.upload.list.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider
import mega.privacy.android.app.databinding.ItemFolderContentBinding
import mega.privacy.android.app.databinding.SortByHeaderBinding
import mega.privacy.android.app.upload.list.data.FolderContent
import mega.privacy.android.app.utils.AdapterUtils.isValidPosition

class FolderContentListAdapter(
    private val folderClickCallback: (FolderContent.Data) -> Unit,
    private val onLongClickCallback: (FolderContent.Data) -> Unit
) : ListAdapter<FolderContent, RecyclerView.ViewHolder>(FolderContent.DiffCallback()),
    SectionTitleProvider {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_DATA = 1
    }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = SortByHeaderBinding.inflate(layoutInflater, parent, false)
                FolderContentHeaderHolder(binding)
            }
            else -> {
                val binding = ItemFolderContentBinding.inflate(layoutInflater, parent, false)
                FolderContentListHolder(binding).apply {
                    binding.root.apply {
                        setOnClickListener {
                            if (isValidPosition(bindingAdapterPosition)) {
                                folderClickCallback.invoke(getItem(bindingAdapterPosition) as FolderContent.Data)
                            }
                        }

                        setOnLongClickListener {
                            if (isValidPosition(bindingAdapterPosition)) {
                                onLongClickCallback.invoke(getItem(bindingAdapterPosition) as FolderContent.Data)
                            }

                            true
                        }
                    }
                }
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is FolderContentHeaderHolder -> holder.bind()
            is FolderContentListHolder -> holder.bind(getItem(position) as FolderContent.Data)
        }
    }

    override fun getItemId(position: Int): Long =
        getItem(position).id

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is FolderContent.Header -> VIEW_TYPE_HEADER
            is FolderContent.Data -> VIEW_TYPE_DATA
        }

    override fun getSectionTitle(position: Int): String =
        getItem(position).getSectionTitle()
}
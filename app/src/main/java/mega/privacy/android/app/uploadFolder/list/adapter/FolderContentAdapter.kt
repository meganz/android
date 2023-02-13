package mega.privacy.android.app.uploadFolder.list.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider
import mega.privacy.android.app.databinding.ItemFolderContentBinding
import mega.privacy.android.app.databinding.ItemFolderContentGridBinding
import mega.privacy.android.app.databinding.ItemGridSeparatorBinding
import mega.privacy.android.app.databinding.SortByHeaderBinding
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.uploadFolder.list.data.FolderContent
import mega.privacy.android.app.utils.AdapterUtils.isValidPosition

/**
 * RecyclerView's ListAdapter to show FolderContent.
 *
 * @property sortByViewModel     ViewModel to manage changes on "Sort by" header.
 * @property onClickCallback     Callback to be called when an item is clicked.
 * @property onLongClickCallback Callback to be called when an item is long clicked.
 */
class FolderContentAdapter(
    private val sortByViewModel: SortByHeaderViewModel,
    private val onClickCallback: (FolderContent.Data, Int) -> Unit,
    private val onLongClickCallback: (FolderContent.Data, Int) -> Unit
) : ListAdapter<FolderContent, RecyclerView.ViewHolder>(FolderContent.DiffCallback()),
    SectionTitleProvider {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_DATA_LIST = 1
        private const val VIEW_TYPE_DATA_GRID = 2
        private const val VIEW_TYPE_GRID_SEPARATOR = 3
    }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = SortByHeaderBinding.inflate(layoutInflater, parent, false)
                FolderContentHeaderHolder(sortByViewModel, binding)
            }
            VIEW_TYPE_DATA_LIST -> {
                val binding = ItemFolderContentBinding.inflate(layoutInflater, parent, false)
                FolderContentListHolder(binding).apply {
                    binding.root.apply {
                        setOnClickListener {
                            if (isValidPosition(bindingAdapterPosition)) {
                                onClickCallback.invoke(
                                    getItem(bindingAdapterPosition) as FolderContent.Data,
                                    bindingAdapterPosition
                                )
                            }
                        }

                        setOnLongClickListener {
                            if (isValidPosition(bindingAdapterPosition)) {
                                onLongClickCallback.invoke(
                                    getItem(bindingAdapterPosition) as FolderContent.Data,
                                    bindingAdapterPosition
                                )
                            }

                            true
                        }
                    }
                }
            }
            VIEW_TYPE_DATA_GRID -> {
                val binding = ItemFolderContentGridBinding.inflate(layoutInflater, parent, false)
                FolderContentGridHolder(binding).apply {
                    binding.root.apply {
                        setOnClickListener {
                            if (isValidPosition(bindingAdapterPosition)) {
                                onClickCallback.invoke(
                                    getItem(bindingAdapterPosition) as FolderContent.Data,
                                    bindingAdapterPosition
                                )
                            }
                        }

                        setOnLongClickListener {
                            if (isValidPosition(bindingAdapterPosition)) {
                                onLongClickCallback.invoke(
                                    getItem(bindingAdapterPosition) as FolderContent.Data,
                                    bindingAdapterPosition
                                )
                            }

                            true
                        }
                    }
                }
            }
            else -> {
                val binding = ItemGridSeparatorBinding.inflate(layoutInflater, parent, false)
                GridSeparatorHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is FolderContentHeaderHolder -> holder.bind()
            is FolderContentListHolder -> holder.bind(getItem(position) as FolderContent.Data)
            is FolderContentGridHolder -> holder.bind(getItem(position) as FolderContent.Data)
        }
    }

    override fun getItemId(position: Int): Long =
        getItem(position).id

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is FolderContent.Header -> VIEW_TYPE_HEADER
            is FolderContent.Data ->
                if (sortByViewModel.isListView()) VIEW_TYPE_DATA_LIST else VIEW_TYPE_DATA_GRID
            is FolderContent.Separator -> VIEW_TYPE_GRID_SEPARATOR
        }

    override fun getSectionTitle(position: Int): String =
        getItem(position).getSectionTitle()

    /**
     * Gets the numbers of spans a grid holder should occupy. It depends on the type of view holder:
     * VIEW_TYPE_HEADER and VIEW_TYPE_GRID_SEPARATOR should occupy all the spans of the row (spanCount),
     * otherwise only one span.
     *
     * @param spanCount The number of spans of each row.
     * @return The number of spans a grid holder should occupy.
     */
    fun getSpanSizeLookup(spanCount: Int): SpanSizeLookup =
        object : SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int =
                if (getItemViewType(position) == VIEW_TYPE_HEADER
                    || getItemViewType(position) == VIEW_TYPE_GRID_SEPARATOR
                ) spanCount else 1
        }
}
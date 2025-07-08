package mega.privacy.android.app.uploadFolder.list.adapter

import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.SortByHeaderBinding
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel

/**
 * RecyclerView's ViewHolder to show the "Sort by" header.
 *
 * @property sortByViewModel ViewModel to update the header with the latest state and manage actions over it.
 * @property binding         Item's view binding
 */
class FolderContentHeaderHolder(
    private val sortByViewModel: SortByHeaderViewModel,
    private val binding: SortByHeaderBinding,
) : RecyclerView.ViewHolder(binding.root) {
    init {
        binding.sortByLayout.setOnClickListener {
            sortByViewModel.showSortByDialog()
        }

        binding.listModeSwitch.setOnClickListener {
            sortByViewModel.switchViewType()
        }
    }

    fun bind() {
        binding.apply {
            sortedBy.text = root.context.getString(
                SortByHeaderViewModel.orderNameMap[sortByViewModel.order.offlineSortOrder]
                    ?: R.string.sortby_name
            )
        }

        binding.listModeSwitch.setImageResource(
            if (sortByViewModel.isListView())
                mega.privacy.android.icon.pack.R.drawable.ic_grid_4_small_thin_outline
            else
                mega.privacy.android.icon.pack.R.drawable.ic_list_small_small_thin_outline
        )
    }
}
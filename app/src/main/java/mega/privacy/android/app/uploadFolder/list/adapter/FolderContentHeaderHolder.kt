package mega.privacy.android.app.uploadFolder.list.adapter

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
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
    private val binding: SortByHeaderBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind() {
        binding.apply {
            orderNameStringId =
                SortByHeaderViewModel.orderNameMap[sortByViewModel.order.third]!!
            sortByHeaderViewModel = sortByViewModel
            enterMediaDiscovery.isVisible = false
        }
    }
}
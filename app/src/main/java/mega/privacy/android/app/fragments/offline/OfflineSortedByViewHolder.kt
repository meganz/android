package mega.privacy.android.app.fragments.offline

import mega.privacy.android.app.databinding.SortByHeaderBinding
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel

class OfflineSortedByViewHolder(
    private val binding: SortByHeaderBinding,
    private val sortByViewModel: SortByHeaderViewModel
) : OfflineViewHolder(binding.root) {
    override fun bind(position: Int, node: OfflineNode, listener: OfflineAdapterListener) {
        binding.apply {
            this.orderNameStringId =
                SortByHeaderViewModel.orderNameMap[sortByViewModel.order]!!
            this.sortByHeaderViewModel = sortByViewModel
        }
    }
}

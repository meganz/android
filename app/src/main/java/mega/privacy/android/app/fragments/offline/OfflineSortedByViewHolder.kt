package mega.privacy.android.app.fragments.offline

import mega.privacy.android.app.databinding.SortByHeaderBinding
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel

class OfflineSortedByViewHolder(
    private val binding: SortByHeaderBinding,
    private val sortByViewModel: SortByHeaderViewModel,
    listener: OfflineAdapterListener,
    itemGetter: (Int) -> OfflineNode
) : OfflineViewHolder(binding.root, listener, itemGetter) {

    override fun bind(position: Int, node: OfflineNode) {
        binding.apply {
            this.orderNameStringId =
                SortByHeaderViewModel.orderNameMap[sortByViewModel.order]!!
            this.sortByHeaderViewModel = sortByViewModel
        }
    }
}

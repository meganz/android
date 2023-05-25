package mega.privacy.android.app.fragments.offline

import androidx.core.view.isVisible
import mega.privacy.android.app.databinding.SortByHeaderBinding
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel

class OfflineSortedByViewHolder(
    private val binding: SortByHeaderBinding,
    private val sortByViewModel: SortByHeaderViewModel,
    onNodeClicked: (Int, OfflineNode) -> Unit,
    onNodeLongClicked: (Int, OfflineNode) -> Unit,
) : OfflineViewHolder(binding.root, onNodeClicked, onNodeLongClicked) {

    override fun bind(position: Int, node: OfflineNode) {
        binding.apply {
            this.orderNameStringId =
                SortByHeaderViewModel.orderNameMap[sortByViewModel.order.third]!!
            this.sortByHeaderViewModel = sortByViewModel
            this.enterMediaDiscovery.isVisible = false
        }
    }
}

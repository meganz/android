package mega.privacy.android.app.presentation.offline.adapter.viewholder

import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.SortByHeaderBinding
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.presentation.offline.model.OfflineNode

class OfflineSortedByViewHolder(
    private val binding: SortByHeaderBinding,
    private val sortByViewModel: SortByHeaderViewModel,
    onNodeClicked: (Int, OfflineNode) -> Unit,
    onNodeLongClicked: (Int, OfflineNode) -> Unit,
) : OfflineViewHolder(binding.root, onNodeClicked, onNodeLongClicked) {

    override fun bind(position: Int, node: OfflineNode, selectionMode: Boolean) {
        binding.apply {
            this.orderNameStringId =
                SortByHeaderViewModel.orderNameMap[sortByViewModel.order.offlineSortOrder]
                    ?: R.string.sortby_name
            this.sortByHeaderViewModel = sortByViewModel
        }
    }
}

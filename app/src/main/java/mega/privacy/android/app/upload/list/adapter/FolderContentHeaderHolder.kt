package mega.privacy.android.app.upload.list.adapter

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.databinding.SortByHeaderBinding
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel

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
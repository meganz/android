package mega.privacy.android.app.fragments.homepage

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.databinding.ItemNodeGridBinding
import mega.privacy.android.app.databinding.SortByHeaderBinding
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

class NodeGridViewHolder(private val binding: ViewDataBinding) :
    RecyclerView.ViewHolder(binding.root) {

    @Inject
    lateinit var megaApi: MegaApiAndroid

    fun bind(
        actionModeViewModel: ActionModeViewModel,
        itemOperationViewModel: ItemOperationViewModel,
        sortByHeaderViewModel: SortByHeaderViewModel,
        item: NodeItem
    ) {
        binding.apply {
            when (this) {
                is ItemNodeGridBinding -> {
                    this.itemOperationViewModel = itemOperationViewModel
                    this.actionModeViewModel = actionModeViewModel
                    this.item = item
                    this.megaApi = megaApi
                    this.context = binding.root.context
                }
                is SortByHeaderBinding -> {
                    this.orderNameStringId =
                        SortByHeaderViewModel.orderNameMap[sortByHeaderViewModel.order]!!
                    this.sortByHeaderViewModel = sortByHeaderViewModel
                }
            }
        }

        item.uiDirty = false
    }
}

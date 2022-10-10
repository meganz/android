package mega.privacy.android.app.fragments.homepage

import androidx.core.view.isVisible
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.databinding.ItemNodeGridBinding
import mega.privacy.android.app.databinding.ItemNodeListBinding
import mega.privacy.android.app.databinding.SortByHeaderBinding
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

class NodeViewHolder(private val binding: ViewDataBinding) :
    RecyclerView.ViewHolder(binding.root) {

    @MegaApi
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

                    thumbnail.isVisible = true
                }
                is ItemNodeListBinding -> {
                    this.itemOperationViewModel = itemOperationViewModel
                    this.actionModeViewModel = actionModeViewModel
                    this.item = item
                    this.megaApi = megaApi

                    thumbnail.isVisible = true
                }
                is SortByHeaderBinding -> {
                    this.orderNameStringId =
                        SortByHeaderViewModel.orderNameMap[sortByHeaderViewModel.order.first]!!
                    this.enterMediaDiscovery.isVisible = false
                    this.sortByHeaderViewModel = sortByHeaderViewModel
                }
            }
        }

        item.uiDirty = false
    }
}

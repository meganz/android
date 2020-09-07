package mega.privacy.android.app.fragments.homepage.documents

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.databinding.ItemNodeListBinding
import mega.privacy.android.app.databinding.SortByHeaderBinding
import mega.privacy.android.app.fragments.homepage.ActionModeViewModel
import mega.privacy.android.app.fragments.homepage.NodeItem
import nz.mega.sdk.MegaApiAndroid
import javax.inject.Inject

class DocumentViewHolder(private val binding: ViewDataBinding) :
    RecyclerView.ViewHolder(binding.root) {

    @Inject
    lateinit var megaApi: MegaApiAndroid

    fun bind(
        viewModel: DocumentsViewModel,
        actionModeViewModel: ActionModeViewModel,
        item: NodeItem
    ) {
        binding.apply {
            when (this) {
                is ItemNodeListBinding -> {
                    this.itemOperation = viewModel
                    this.actionModeViewModel = actionModeViewModel
                    this.item = item
                    this.megaApi = megaApi
                }
                is SortByHeaderBinding -> {
                }
            }
        }

        item.uiDirty = false
    }
}
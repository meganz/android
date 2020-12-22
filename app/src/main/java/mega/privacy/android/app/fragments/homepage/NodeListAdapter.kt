package mega.privacy.android.app.fragments.homepage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import mega.privacy.android.app.databinding.ItemNodeListBinding
import mega.privacy.android.app.databinding.SortByHeaderBinding

class NodeListAdapter(
    actionModeViewModel: ActionModeViewModel,
    itemOperationViewModel: ItemOperationViewModel,
    sortByHeaderViewModel: SortByHeaderViewModel
) : BaseNodeItemAdapter(actionModeViewModel, itemOperationViewModel, sortByHeaderViewModel) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NodeViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        val binding = when (viewType) {
            TYPE_ITEM ->
                ItemNodeListBinding.inflate(
                    inflater,
                    parent,
                    false
                )
            else ->  // TYPE_HEADER
                SortByHeaderBinding.inflate(
                    inflater,
                    parent,
                    false
                )
        }

        if (binding is ItemNodeListBinding) {
            binding.publicLink.visibility = View.GONE
            binding.savedOffline.visibility = View.GONE
            binding.takenDown.visibility = View.GONE
            binding.versionsIcon.visibility = View.GONE
        }

        return NodeViewHolder(binding)
    }
}

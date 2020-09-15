package mega.privacy.android.app.fragments.homepage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider
import mega.privacy.android.app.databinding.ItemNodeListBinding
import mega.privacy.android.app.databinding.SortByHeaderBinding

class NodeListAdapter constructor(
    private val actionModeViewModel: ActionModeViewModel,
    private val itemOperationViewModel: ItemOperationViewModel,
    private val sortByHeaderViewModel: SortByHeaderViewModel
) : ListAdapter<NodeItem, NodeViewHolder>(NodeDiffCallback()),
    SectionTitleProvider {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position).node) {
            null -> TYPE_HEADER
            else -> TYPE_ITEM
        }
    }

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

    override fun onBindViewHolder(holder: NodeViewHolder, position: Int) {
        holder.bind(
            actionModeViewModel,
            itemOperationViewModel,
            sortByHeaderViewModel,
            getItem(position)
        )
    }

    override fun getSectionTitle(position: Int): String? {
        if (position < 0 || position >= itemCount) {
            return null
        }

        val nodeName = getItem(position).node?.name ?: ""
        return if (nodeName == "") null else nodeName.substring(0, 1)
    }

    companion object {
        const val TYPE_ITEM = 0
        const val TYPE_HEADER = 1
    }
}

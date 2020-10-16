package mega.privacy.android.app.fragments.homepage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider
import mega.privacy.android.app.databinding.ItemNodeGridBinding
import mega.privacy.android.app.databinding.SortByHeaderBinding

class NodeGridAdapter(
    actionModeViewModel: ActionModeViewModel,
    itemOperationViewModel: ItemOperationViewModel,
    sortByHeaderViewModel: SortByHeaderViewModel
) : BaseNodeItemAdapter(actionModeViewModel, itemOperationViewModel, sortByHeaderViewModel) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NodeViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        val binding = when (viewType) {
            TYPE_ITEM ->
                ItemNodeGridBinding.inflate(
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

        if (binding is ItemNodeGridBinding) {
            // FastScroller would affect the normal process of RecyclerView that makes the "selected"
            // icon appear before binding the item. Therefore, hide the icon up front
            binding.icSelected.isVisible = false
            binding.takenDown.isVisible = false
            binding.videoInfo.isVisible = false
        }

        return NodeViewHolder(binding)
    }

    fun getSpanSizeLookup(spanCount: Int) = object : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int {
            return if (getItemViewType(position) == TYPE_HEADER) {
                spanCount
            } else {
                1
            }
        }
    }
}

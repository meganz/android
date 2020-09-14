package mega.privacy.android.app.fragments.homepage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider
import mega.privacy.android.app.databinding.ItemNodeGridBinding
import mega.privacy.android.app.databinding.SortByHeaderBinding

class NodeGridAdapter(
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

        if (viewType == NodeListAdapter.TYPE_ITEM) {
            // FastScroller would affect the normal process of RecyclerView that makes the "selected"
            // icon appear before binding the item. Therefore, hide the icon up front
            (binding as ItemNodeGridBinding).icSelected.visibility = View.GONE
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

    fun getSpanSizeLookup(spanCount: Int) = object : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int {
            return if (getItemViewType(position) == TYPE_HEADER) {
                spanCount
            } else {
                1
            }
        }
    }

    override fun getSectionTitle(position: Int): String {
        if (position < 0 || position >= itemCount) {
            return ""
        }

        val nodeName = getItem(position).node?.name ?: ""
        return if (nodeName == "") "" else nodeName.substring(0, 1)
    }

    companion object {
        private const val TYPE_ITEM = 0
        private const val TYPE_HEADER = 1
    }
}

package mega.privacy.android.app.fragments.homepage.documents

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.viewbinding.ViewBinding
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider
import mega.privacy.android.app.databinding.ItemNodeListBinding
import mega.privacy.android.app.databinding.ItemPhotoBrowseBinding
import mega.privacy.android.app.databinding.SortByHeaderBinding
import mega.privacy.android.app.fragments.homepage.ActionModeViewModel
import mega.privacy.android.app.fragments.homepage.NodeItem

class DocumentsAdapter constructor(
    private val viewModel: DocumentsViewModel,
    private val actionModeViewModel: ActionModeViewModel
) : ListAdapter<NodeItem, DocumentViewHolder>(PhotoDiffCallback()),
    SectionTitleProvider {

    private var itemDimen = 0

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> TYPE_HEADER
            else -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
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

        if (viewType == TYPE_ITEM && itemDimen > 0) {
            setItemLayoutParams(binding)
            // FastScroller would affect the normal process of RecyclerView that makes the "selected"
            // icon appear before binding the item. Therefore, hide the icon up front
            (binding as ItemPhotoBrowseBinding).iconSelected.visibility = View.GONE
        }

        return DocumentViewHolder(binding)
    }

    private fun setItemLayoutParams(binding: ViewBinding) {
        (binding.root.layoutParams as GridLayoutManager.LayoutParams).apply {
            width = itemDimen
            height = itemDimen
        }
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        holder.bind(viewModel, actionModeViewModel, getItem(position))
    }

    private class PhotoDiffCallback : DiffUtil.ItemCallback<NodeItem>() {
        override fun areItemsTheSame(oldItem: NodeItem, newItem: NodeItem): Boolean {
            return oldItem.node?.handle == newItem.node?.handle
        }

        override fun areContentsTheSame(oldItem: NodeItem, newItem: NodeItem): Boolean {
            if (newItem.uiDirty) {
                return false
            }

            return true
        }
    }

    fun setItemDimen(dimen: Int) {
        if (dimen > 0) itemDimen = dimen
    }

//    fun getSpanSizeLookup(spanCount: Int) = object : GridLayoutManager.SpanSizeLookup() {
//        override fun getSpanSize(position: Int): Int {
//            return when (getItem(position).type) {
//                NodeItem.TYPE_TITLE -> spanCount
//                else -> 1
//            }
//        }
//    }

    override fun getSectionTitle(position: Int) = if (position < 0 || position >= itemCount) {
        ""
    } else getItem(position).modifiedDate

    companion object {
        private const val TYPE_ITEM = 0
        private const val TYPE_HEADER = 1
    }
}

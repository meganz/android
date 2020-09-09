package mega.privacy.android.app.fragments.homepage.documents

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.viewbinding.ViewBinding
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider
import mega.privacy.android.app.databinding.ItemNodeListBinding
import mega.privacy.android.app.databinding.ItemPhotoBrowseBinding
import mega.privacy.android.app.databinding.SortByHeaderBinding
import mega.privacy.android.app.fragments.homepage.ActionModeViewModel
import mega.privacy.android.app.fragments.homepage.ItemOperationViewModel
import mega.privacy.android.app.fragments.homepage.NodeDiffCallback
import mega.privacy.android.app.fragments.homepage.NodeItem
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel

class DocumentsAdapter constructor(
    private val actionModeViewModel: ActionModeViewModel,
    private val itemOperationViewModel: ItemOperationViewModel,
    private val sortByHeaderViewModel: SortByHeaderViewModel
) : ListAdapter<NodeItem, DocumentViewHolder>(NodeDiffCallback()),
    SectionTitleProvider {

    private var itemDimen = 0

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position).node) {
            null -> TYPE_HEADER
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
        holder.bind(
            actionModeViewModel,
            itemOperationViewModel,
            sortByHeaderViewModel,
            getItem(position)
        )
    }

    fun setItemDimen(dimen: Int) {
        if (dimen > 0) itemDimen = dimen
    }

    override fun getSectionTitle(position: Int): String {
        if (position < 0 || position >= itemCount) {
            return ""
        }

        val nodeName = getItem(position).node?.name ?: ""
        return if (nodeName == "") "" else nodeName.substring(0, 1)
    }

    companion object {
        const val TYPE_ITEM = 0
        const val TYPE_HEADER = 1
    }
}

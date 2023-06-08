package mega.privacy.android.app.getLink.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.databinding.ItemHeaderLinksBinding
import mega.privacy.android.app.databinding.ItemLinkBinding
import mega.privacy.android.app.getLink.data.LinkItem

/**
 * ListAdapter to show [LinkItem]s.
 * Currently used in [mega.privacy.android.app.getLink.GetSeveralLinksFragment].
 */
class LinksAdapter : ListAdapter<LinkItem, RecyclerView.ViewHolder>(LinkItem.DiffCallback()) {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_DATA = 1
    }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            VIEW_TYPE_HEADER -> LinkHeaderViewHolder(
                ItemHeaderLinksBinding.inflate(
                    layoutInflater,
                    parent,
                    false
                )
            )
            else -> LinkViewHolder(ItemLinkBinding.inflate(layoutInflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is LinkViewHolder -> holder.bind(getItem(position) as LinkItem.Data)
            else -> {}
        }
    }

    override fun getItemId(position: Int): Long =
        getItem(position).id

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is LinkItem.Header -> VIEW_TYPE_HEADER
            is LinkItem.Data -> VIEW_TYPE_DATA
        }
}
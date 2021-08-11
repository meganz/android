package mega.privacy.android.app.getLink.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.databinding.ItemLinkBinding
import mega.privacy.android.app.getLink.data.LinkItem

class LinksAdapter : ListAdapter<LinkItem, LinkViewHolder>(LinkItem.DiffCallback()) {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LinkViewHolder =
        LinkViewHolder(ItemLinkBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: LinkViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).node.handle
    }
}
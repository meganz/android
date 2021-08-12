package mega.privacy.android.app.getLink.adapter

import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.databinding.ItemHeaderLinksBinding
import mega.privacy.android.app.getLink.data.LinkItem

class LinkHeaderViewHolder(
    private val binding: ItemHeaderLinksBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: LinkItem.Header) {
        binding.linksHeader.text = item.title
    }
}
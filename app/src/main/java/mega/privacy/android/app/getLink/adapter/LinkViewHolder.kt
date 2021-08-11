package mega.privacy.android.app.getLink.adapter

import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.databinding.ItemLinkBinding
import mega.privacy.android.app.getLink.data.LinkItem

class LinkViewHolder(
    private val binding: ItemLinkBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: LinkItem) {
        binding.nameText.text = item.name
        binding.linkText.text = item.link
        binding.infoText.text = item.info
    }
}
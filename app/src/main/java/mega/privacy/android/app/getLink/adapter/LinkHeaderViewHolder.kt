package mega.privacy.android.app.getLink.adapter

import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.databinding.ItemHeaderLinksBinding
import mega.privacy.android.app.getLink.data.LinkItem

/**
 * RecyclerView.ViewHolder to draw headers in [LinksAdapter].
 *
 * @property binding ItemHeaderLinksBinding necessary to draw the header item.
 */
class LinkHeaderViewHolder(
    private val binding: ItemHeaderLinksBinding
) : RecyclerView.ViewHolder(binding.root)
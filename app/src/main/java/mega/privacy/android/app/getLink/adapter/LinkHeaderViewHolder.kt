package mega.privacy.android.app.getLink.adapter

import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemHeaderLinksBinding

/**
 * RecyclerView.ViewHolder to draw headers in [LinksAdapter].
 *
 * @property binding ItemHeaderLinksBinding necessary to draw the header item.
 */
class LinkHeaderViewHolder(
    private val binding: ItemHeaderLinksBinding,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind() {
        binding.getLinkAccessSubtitle.text = binding.root.context.resources.getQuantityText(
            R.plurals.cloud_drive_subtitle_links_access_user,
            2
        )
    }
}
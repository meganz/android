package mega.privacy.android.app.fragments.offline

import mega.privacy.android.app.databinding.OfflineItemSortedByBinding

class OfflineSortedByViewHolder(
    private val binding: OfflineItemSortedByBinding,
    private val adapter: OfflineAdapter
) : OfflineViewHolder(binding.root) {
    override fun bind(position: Int, node: OfflineNode, listener: OfflineAdapterListener) {
        super.bind(position, node, listener)
        binding.sortedBy.text = adapter.sortedBy

        binding.root.setOnClickListener { listener.onSortedByClicked() }
    }
}

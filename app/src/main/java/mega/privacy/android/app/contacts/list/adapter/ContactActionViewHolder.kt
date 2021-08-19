package mega.privacy.android.app.contacts.list.adapter

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.contacts.list.data.ContactActionItem
import mega.privacy.android.app.databinding.ItemContactActionBinding

/**
 * RecyclerView's ViewHolder to show ContactActionItem.
 *
 * @property binding    Item's view binding
 */
class ContactActionViewHolder(
    private val binding: ItemContactActionBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: ContactActionItem) {
        binding.txtTitle.text = item.title
        binding.chipCounter.text = item.counter.toString()
        binding.chipCounter.isVisible = item.counter > 0
    }
}

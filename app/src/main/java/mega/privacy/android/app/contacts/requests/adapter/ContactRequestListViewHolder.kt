package mega.privacy.android.app.contacts.requests.adapter

import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.databinding.ItemContactRequestBinding
import mega.privacy.android.app.main.adapters.ShareContactRowState
import mega.privacy.android.app.main.adapters.bindShareContactRow
import mega.privacy.android.shared.contact.model.ContactItemUiState

/**
 * RecyclerView's ViewHolder to show ContactRequestItem.
 *
 * @property binding    Item's view binding
 */
class ContactRequestListViewHolder(
    private val binding: ItemContactRequestBinding,
) : RecyclerView.ViewHolder(binding.root) {

    private val rowState: ShareContactRowState = ShareContactRowState()
    private var contentInstalled: Boolean = false

    fun bind(uiState: ContactItemUiState) {
        rowState.uiState = uiState
        if (!contentInstalled) {
            bindShareContactRow(binding.contactComposeView, rowState)
            contentInstalled = true
        }
    }
}

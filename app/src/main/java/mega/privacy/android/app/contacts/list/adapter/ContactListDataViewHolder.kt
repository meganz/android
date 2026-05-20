package mega.privacy.android.app.contacts.list.adapter

import android.net.Uri
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.contacts.list.data.ContactItem
import mega.privacy.android.app.contacts.list.mapper.ContactItemDataToContactItemUiStateMapper
import mega.privacy.android.app.databinding.ItemContactDataBinding
import mega.privacy.android.app.utils.AvatarUtil
import java.io.File

/**
 * RecyclerView's ViewHolder to show ContactItem Data info.
 *
 * @property binding                     Item's view binding.
 * @property contactItemUiStateMapper    Maps the legacy [ContactItem.Data] row
 *                                       into the UI state consumed by the
 *                                       embedded `ContactItemView`.
 */
class ContactListDataViewHolder(
    private val binding: ItemContactDataBinding,
    private val contactItemUiStateMapper: ContactItemDataToContactItemUiStateMapper,
) : RecyclerView.ViewHolder(binding.root) {

    private val rowState: ContactListRowState = ContactListRowState()
    private var contentInstalled: Boolean = false

    fun bind(item: ContactItem.Data) {
        rowState.uiState = contactItemUiStateMapper(
            item = item,
            avatarFile = item.avatarUri?.toAvatarFile(),
            avatarColorArgb = AvatarUtil.getColorAvatar(item.handle),
        )
        if (!contentInstalled) {
            bindContactListRow(binding.contactComposeView, rowState)
            contentInstalled = true
        }
        binding.chipNew.isVisible = item.isNew
    }

    private fun Uri.toAvatarFile(): File? = path?.let(::File)?.takeIf { it.exists() }
}

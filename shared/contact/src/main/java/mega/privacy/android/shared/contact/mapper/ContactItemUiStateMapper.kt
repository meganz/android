package mega.privacy.android.shared.contact.mapper

import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.shared.contact.extension.displayName
import mega.privacy.android.shared.contact.model.ContactItemUiState
import javax.inject.Inject

/**
 * Maps a domain [ContactItem] to the presentational [ContactItemUiState] consumed
 * by `ContactItemView` in `:feature:contact:contact-snowflake-components`.
 *
 * The status text is taken from a caller-supplied parameter so the mapper stays
 * free of Android `Context` and string-resource resolution. Callers resolve the
 * subtitle (e.g. "Online", "Last seen today at HH:mm", a permission label) and
 * pass it in.
 *
 * @property contactItemStatusMapper
 * @property contactItemAvatarMapper
 */
class ContactItemUiStateMapper @Inject constructor(
    private val contactItemStatusMapper: ContactItemStatusMapper,
    private val contactItemAvatarMapper: ContactItemAvatarMapper,
) {

    /**
     * @param contactItem Domain contact.
     */
    operator fun invoke(
        contactItem: ContactItem,
    ): ContactItemUiState = ContactItemUiState(
        handle = contactItem.handle,
        displayName = contactItem.displayName(),
        status = contactItemStatusMapper(contactItem.status),
        lastSeen = contactItem.lastSeen,
        avatar = contactItemAvatarMapper(contactItem),
        isVerified = contactItem.areCredentialsVerified,
        email = contactItem.email,
    )
}

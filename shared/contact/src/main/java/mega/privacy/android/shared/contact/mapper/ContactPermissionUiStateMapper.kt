package mega.privacy.android.shared.contact.mapper

import mega.privacy.android.domain.entity.contacts.ContactPermission
import mega.privacy.android.shared.contact.model.ContactPermissionUiState
import javax.inject.Inject

/**
 * Maps a domain [ContactPermission] to the presentational [ContactPermissionUiState]
 *
 * @property contactMapper
 */
class ContactPermissionUiStateMapper @Inject constructor(
    private val contactMapper: ContactItemUiStateMapper,
) {

    /**
     * @param contactPermission Domain contact permission.
     */
    operator fun invoke(
        contactPermission: ContactPermission,
    ): ContactPermissionUiState =
        ContactPermissionUiState(
            contactItemUiState = contactMapper(contactPermission.contactItem),
            email = contactPermission.contactItem.email,
            permission = contactPermission.accessPermission,
        )
}

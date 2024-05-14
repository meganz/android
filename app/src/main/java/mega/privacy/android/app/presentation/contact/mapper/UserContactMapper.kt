package mega.privacy.android.app.presentation.contact.mapper

import mega.privacy.android.app.main.megachat.chat.explorer.ContactItemUiState
import mega.privacy.android.domain.entity.contacts.UserContact
import javax.inject.Inject

/**
 * A mapper to map [UserContact] domain entity into [ContactItemUiState].
 */
class UserContactMapper @Inject constructor() {

    /**
     * Invocation method.
     *
     * @param userContact The [UserContact] that will be mapped.
     *
     * @return [ContactItemUiState]
     */
    operator fun invoke(userContact: UserContact) = ContactItemUiState(
        contact = userContact.contact,
        user = userContact.user
    )
}

package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.user.UserUpdate

/**
 * Apply contact updates on a [ContactItem] list.
 */
fun interface ApplyContactUpdates {

    /**
     * Invoke.
     *
     * @param outdatedContactList [ContactItem] list to update.
     * @param contactUpdates      [UserUpdate].
     * @return The updated [ContactItem] list.
     */
    suspend operator fun invoke(
        outdatedContactList: List<ContactItem>,
        contactUpdates: UserUpdate,
    ): List<ContactItem>
}
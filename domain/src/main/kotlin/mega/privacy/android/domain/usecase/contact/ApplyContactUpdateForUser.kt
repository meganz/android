package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.user.UserUpdate

/**
 * Apply contact update for user
 */
interface ApplyContactUpdateForUser {
    /**
     * Invoke.
     *
     * @param contactItem   [ContactItem] list to update.
     * @return The updated  [ContactItem].
     */
    suspend operator fun invoke(
        contactItem: ContactItem,
        userUpdate: UserUpdate,
    ): ContactItem
}
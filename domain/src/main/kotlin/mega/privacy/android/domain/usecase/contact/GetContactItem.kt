package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.user.UserId

/**
 * Gets the ContactItem for a given [UserId]
 */
fun interface GetContactItem {
    suspend operator fun invoke(userId: UserId): ContactItem?
}
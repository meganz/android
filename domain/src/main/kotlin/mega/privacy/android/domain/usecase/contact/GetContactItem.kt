package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.user.UserId

/**
 * Gets the ContactItem for a given [UserId]
 */
fun interface GetContactItem {
    /**
     * Gets the ContactItem for a given [UserId]
     * @param userId of the user we want to fetch
     * @param skipCache if true a new fetch will be done, if false it may return a cached info
     */
    suspend operator fun invoke(userId: UserId, skipCache: Boolean): ContactItem?
}
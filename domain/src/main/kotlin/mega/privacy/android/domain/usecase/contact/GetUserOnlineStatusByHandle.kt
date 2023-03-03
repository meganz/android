package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.entity.contacts.UserStatus

/**
 * Get user online status from user handle
 */
fun interface GetUserOnlineStatusByHandle {
    /**
     * Invoke
     *
     * @param userHandle user handle is the reference id to the use
     */
    suspend operator fun invoke(userHandle: Long): UserStatus
}
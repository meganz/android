package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.entity.contacts.ContactItem

/**
 * Get contact from chat info
 */
interface GetContactFromChat {

    /**
     * invoke
     *
     * @param chatId chat id of selected chat
     * @param skipCache If true, force read from backend, refresh cache and return.
     *                  If false, use value in cache
     * @return [ContactItem]
     */
    suspend operator fun invoke(chatId: Long, skipCache: Boolean): ContactItem?
}
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
     * @return [ContactItem]
     */
    suspend operator fun invoke(chatId: Long): ContactItem?
}
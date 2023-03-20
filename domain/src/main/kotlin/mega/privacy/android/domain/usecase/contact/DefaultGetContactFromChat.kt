package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Get contact info from chat id
 */
class DefaultGetContactFromChat @Inject constructor(
    private val contactsRepository: ContactsRepository,
    private val getContactFromEmail: GetContactFromEmail,
) : GetContactFromChat {
    /**
     * invoke
     *
     * @param chatId chat id of selected chat
     * @param skipCache If true, force read from backend, refresh cache and return.
     *                  If false, use value in cache
     * @return [ContactItem] which contains contact information of selected user
     */
    override suspend fun invoke(chatId: Long, skipCache: Boolean): ContactItem? {
        val handle = contactsRepository.getUserEmailFromChat(chatId)
        return handle?.let { getContactFromEmail(it, skipCache) }
    }
}
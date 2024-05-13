package mega.privacy.android.domain.usecase.chat.explorer

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.contacts.UserChatStatus.Busy
import mega.privacy.android.domain.entity.contacts.UserChatStatus.Invalid
import mega.privacy.android.domain.entity.contacts.UserChatStatus.Online
import mega.privacy.android.domain.entity.contacts.UserContact
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.usecase.chat.GetChatRoomByUserUseCase
import mega.privacy.android.domain.usecase.contact.GetContactFromCacheByHandleUseCase
import mega.privacy.android.domain.usecase.contact.GetUserOnlineStatusByHandleUseCase
import mega.privacy.android.domain.usecase.contact.RequestUserLastGreenUseCase
import javax.inject.Inject

/**
 * Use case to retrieve the list of contacts that have no chat room
 *
 * @property contactsRepository [ContactsRepository]
 * @property getUserOnlineStatusByHandleUseCase [GetVisibleContactsWithoutChatRoomUseCase]
 */
class GetVisibleContactsWithoutChatRoomUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
    private val getChatRoomByUserUseCase: GetChatRoomByUserUseCase,
    private val getUserOnlineStatusByHandleUseCase: GetUserOnlineStatusByHandleUseCase,
    private val requestUserLastGreenUseCase: RequestUserLastGreenUseCase,
    private val getContactFromCacheByHandleUseCase: GetContactFromCacheByHandleUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {

    /**
     * Invocation method.
     *
     * @return List of user contacts
     */
    suspend operator fun invoke(): List<UserContact> = withContext(defaultDispatcher) {
        buildList {
            contactsRepository.getAvailableContacts()
                .forEach { user ->
                    if (user.visibility == UserVisibility.Visible) {
                        val chatRoom = getChatRoomByUserUseCase(user.handle)
                        if (chatRoom == null) {
                            if (user.handle != INVALID_HANDLE) {
                                val userStatus = getUserOnlineStatusByHandleUseCase(user.handle)
                                if (userStatus != Online && userStatus != Busy && userStatus != Invalid) {
                                    requestUserLastGreenUseCase(user.handle)
                                }
                            }
                            val cachedContact = getContactFromCacheByHandleUseCase(user.handle)
                            add(
                                UserContact(
                                    contact = cachedContact?.copy(
                                        email = cachedContact.email ?: user.email
                                    ),
                                    user = user
                                )
                            )
                        }
                    }
                }
        }
    }

    companion object {
        private const val INVALID_HANDLE = -1L
    }
}

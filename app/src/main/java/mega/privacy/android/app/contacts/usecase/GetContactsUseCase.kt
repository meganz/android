package mega.privacy.android.app.contacts.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.OnlineStatus
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserLastGreen
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.ContactsRepository
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * Use case to get contacts.
 *
 * @property accountsRepository Repository to provide information about the current account.
 * @property contactsRepository Repository to provide information about contacts.
 * @property chatRepository Repository to provide information about chats.
 */
internal class GetContactsUseCase @Inject constructor(
    private val accountsRepository: AccountRepository,
    private val contactsRepository: ContactsRepository,
    private val chatRepository: ChatRepository,
) {


    /**
     * Gets contacts.
     *
     * @param avatarFolder Avatar folder in cache.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<ContactItem>> =
        listChangedFlow().flatMapLatest { initialContacts ->
            merge(
                chatOnlineStatusChangeFlow(),
                lastGreenChangeFlow(),
                chatConnectionStateChangeFlow(),
                userUpdatesChangeFlow(),
            ).scan(initialContacts) { contacts, change: suspend (List<ContactItem>) -> List<ContactItem> ->
                change(contacts)
            }
        }.distinctUntilChanged()

    private fun lastGreenChangeFlow(): Flow<suspend (List<ContactItem>) -> List<ContactItem>> =
        contactsRepository.monitorChatPresenceLastGreenUpdates().map { event ->
            applyLastGreen(event)
        }

    private fun applyLastGreen(event: UserLastGreen): suspend (List<ContactItem>) -> List<ContactItem> =
        { contacts: List<ContactItem> ->
            contacts.map { contact ->
                if (contact.handle == event.handle) {
                    contact.copy(lastSeen = event.lastGreen)
                } else {
                    contact
                }
            }
        }

    private fun chatOnlineStatusChangeFlow(): Flow<suspend (List<ContactItem>) -> List<ContactItem>> =
        contactsRepository.monitorChatOnlineStatusUpdates().onEach {
            if (it.status != UserChatStatus.Online) contactsRepository.requestLastGreen(it.userHandle)
        }.map { event ->
            applyChatOnlineStatus(event)
        }

    private fun applyChatOnlineStatus(event: OnlineStatus): suspend (List<ContactItem>) -> List<ContactItem> =
        { contacts: List<ContactItem> ->
            contacts.map { contact ->
                if (contact.handle == event.userHandle) {
                    contact.copy(
                        status = event.status,
                    )
                } else {
                    contact
                }
            }
        }

    private fun chatConnectionStateChangeFlow(): Flow<suspend (List<ContactItem>) -> List<ContactItem>> =
        contactsRepository.monitorChatConnectionStateUpdates().map {
            applyChatConnectionState()
        }

    private fun applyChatConnectionState(): suspend (List<ContactItem>) -> List<ContactItem> =
        { contacts: List<ContactItem> ->
            contacts.map { contact ->
                if (contact.chatroomId == null && isWithinLastThreeDays(contact.timestamp)) {
                    contact.copy(
                        chatroomId = chatRepository.getChatRoomByUser(contact.handle)?.chatId
                    )
                } else {
                    contact
                }
            }
        }

    private fun isWithinLastThreeDays(timestamp: Long): Boolean {
        val now = LocalDateTime.now()
        val addedTime =
            Instant.ofEpochSecond(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()
        return Duration.between(addedTime, now).toDays() < 3
    }

    private fun listChangedFlow(): Flow<List<ContactItem>> = flow {
        val contacts = contactsRepository.getVisibleContacts()
        emit(contacts)
        emitAll(
            accountsRepository.monitorUserUpdates()
                .map { userUpdate ->
                    userUpdate.changes
                        .mapNotNull { (key, value) ->
                            key.id to value.filter { it is UserChanges.Visibility || it is UserChanges.AuthenticationInformation }
                        }.toMap()
                }.scan(contacts) { acc, userUpdate ->
                    if (hasVisibilityChange(acc, userUpdate)
                        || hasAuthChanges(userUpdate)
                    ) {
                        contactsRepository.getVisibleContacts()
                    } else acc
                }
        )
        awaitCancellation()
    }.distinctUntilChanged()

    private fun hasAuthChanges(userUpdate: Map<Long, List<UserChanges>>) =
        userUpdate.values.any { it.any { change -> change is UserChanges.AuthenticationInformation } }

    private fun hasVisibilityChange(
        acc: List<ContactItem>,
        userUpdate: Map<Long, List<UserChanges>>,
    ) = userUpdate.mapValues { (_, values) ->
        values.firstOrNull { it is UserChanges.Visibility }
            ?.let { it as UserChanges.Visibility }?.userVisibility
    }.let {
        hasCurrentContactVisibilityChanged(acc, it)
                || newContactHasBecomeVisible(it, acc)
    }

    private fun newContactHasBecomeVisible(
        visibilityChanges: Map<Long, UserVisibility?>,
        acc: List<ContactItem>,
    ) = acc.map { it.handle }
        .containsAll(visibilityChanges.filterValues { it == UserVisibility.Visible }.keys).not()

    private fun hasCurrentContactVisibilityChanged(
        acc: List<ContactItem>,
        visibilityChanges: Map<Long, UserVisibility?>,
    ) =
        acc.any { visibilityChanges[it.handle]?.let { visibility -> visibility != it.visibility } == true }

    private fun userUpdatesChangeFlow(): Flow<suspend (List<ContactItem>) -> List<ContactItem>> =
        accountsRepository.monitorUserUpdates().map {
            it.changes.mapKeys { (key, _) ->
                key.id
            }.mapValues { (_, list) ->
                list.filter { change -> change !is UserChanges.Visibility }
            }
        }.map { userUpdate ->
            applyUserUpdates(userUpdate)
        }

    private fun applyUserUpdates(userUpdate: Map<Long, List<UserChanges>>): suspend (List<ContactItem>) -> List<ContactItem> =
        { contacts: List<ContactItem> ->
            contacts.map { contact ->
                if (userUpdate.hasContactDataChangesForUser(contact) || userUpdate.hasAliasChange()) {
                    contact.copy(contactData = contactsRepository.getContactData(contact))
                } else {
                    contact
                }
            }
        }

    private fun Map<Long, List<UserChanges>>.hasContactDataChangesForUser(
        contact: ContactItem,
    ) = this[contact.handle]?.any {
        it is UserChanges.Avatar || it is UserChanges.Firstname || it is UserChanges.Lastname
    } == true

    private fun Map<Long, List<UserChanges>>.hasAliasChange() =
        this.values.any { it.any { change -> change is UserChanges.Alias } }

}
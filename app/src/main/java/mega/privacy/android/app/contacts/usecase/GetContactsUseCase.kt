package mega.privacy.android.app.contacts.usecase

import mega.privacy.android.domain.entity.contacts.ContactItem as DomainContact
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
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
import mega.privacy.android.app.R
import mega.privacy.android.app.contacts.list.data.ContactItem
import mega.privacy.android.app.contacts.mapper.ContactItemDataMapper
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.data.extensions.getDecodedAliases
import mega.privacy.android.domain.entity.contacts.OnlineStatus
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserLastGreen
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.ContactsRepository
import nz.mega.sdk.MegaRequest
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * Get contacts use case
 *
 *  @property onlineString             Get online string
 *  @property getUnformattedLastSeenDate Get unformatted last seen date
 *  @property getAliasMap              Get alias map
 *  @property getUserUpdates           Get user updates
 */
internal class GetContactsUseCase(
    private val onlineString: () -> String,
    private val getUnformattedLastSeenDate: (Int) -> String,
    private val getAliasMap: (MegaRequest) -> Map<Long, String>,
    private val getUserUpdates: () -> Flow<UserUpdate>,
    private val contactMapper: ContactItemDataMapper,
    private val contactsRepository: ContactsRepository,
    private val chatRepository: ChatRepository,
) {

    @Inject
    internal constructor(
        @ApplicationContext context: Context,
        accountsRepository: AccountRepository,
        contactsRepository: ContactsRepository,
        contactMapper: ContactItemDataMapper,
        chatRepository: ChatRepository,
    ) : this(
        onlineString = {
            context.getString(R.string.online_status)
        },
        getUnformattedLastSeenDate = { lastGreen ->
            TimeUtils.unformattedLastGreenDate(context, lastGreen)
        },
        getAliasMap = { request ->
            request.megaStringMap.getDecodedAliases()
        },
        getUserUpdates = {
            accountsRepository.monitorUserUpdates()
        },
        contactMapper = contactMapper,
        contactsRepository = contactsRepository,
        chatRepository = chatRepository,
    )


    /**
     * Gets contacts.
     *
     * @param avatarFolder Avatar folder in cache.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun get(): Flow<List<ContactItem.Data>> =
        listChangedFlow().flatMapLatest { initialContacts ->
            merge(
                chatOnlineStatusChangeFlow(),
                lastGreenChangeFlow(),
                chatConnectionStateChangeFlow(),
                userUpdatesChangeFlow(),
            ).scan(initialContacts) { contacts, change: suspend (List<DomainContact>) -> List<DomainContact> ->
                change(contacts)
            }
        }.map { domainList: List<DomainContact> ->
            domainList.map { contactMapper(it) }.sortedAlphabetically()
        }.distinctUntilChanged()

    private fun lastGreenChangeFlow(): Flow<suspend (List<DomainContact>) -> List<DomainContact>> =
        contactsRepository.monitorChatPresenceLastGreenUpdates().map { event ->
            applyLastGreen(event)
        }

    private fun applyLastGreen(event: UserLastGreen): suspend (List<DomainContact>) -> List<DomainContact> =
        { contacts: List<DomainContact> ->
            contacts.map { contact ->
                if (contact.handle == event.handle) {
                    contact.copy(lastSeen = event.lastGreen)
                } else {
                    contact
                }
            }
        }

    private fun chatOnlineStatusChangeFlow(): Flow<suspend (List<DomainContact>) -> List<DomainContact>> =
        contactsRepository.monitorChatOnlineStatusUpdates().onEach {
            if (it.status != UserChatStatus.Online) contactsRepository.requestLastGreen(it.userHandle)
        }.map { event ->
            applyChatOnlineStatus(event)
        }

    private fun applyChatOnlineStatus(event: OnlineStatus): suspend (List<DomainContact>) -> List<DomainContact> =
        { contacts: List<DomainContact> ->
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

    private fun chatConnectionStateChangeFlow(): Flow<suspend (List<DomainContact>) -> List<DomainContact>> =
        contactsRepository.monitorChatConnectionStateUpdates().map {
            applyChatConnectionState()
        }

    private fun applyChatConnectionState(): suspend (List<DomainContact>) -> List<DomainContact> =
        { contacts: List<DomainContact> ->
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

    private fun listChangedFlow(): Flow<List<DomainContact>> = flow {
        val contacts = contactsRepository.getVisibleContacts()
        emit(contacts)
        emitAll(getUserUpdates()
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
        acc: List<DomainContact>,
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
        acc: List<DomainContact>,
    ) = acc.map { it.handle }
        .containsAll(visibilityChanges.filterValues { it == UserVisibility.Visible }.keys).not()

    private fun hasCurrentContactVisibilityChanged(
        acc: List<DomainContact>,
        visibilityChanges: Map<Long, UserVisibility?>,
    ) =
        acc.any { visibilityChanges[it.handle]?.let { visibility -> visibility != it.visibility } == true }

    private fun userUpdatesChangeFlow(): Flow<suspend (List<DomainContact>) -> List<DomainContact>> =
        getUserUpdates().map {
            it.changes.mapKeys { (key, _) ->
                key.id
            }.mapValues { (_, list) ->
                list.filter { change -> change !is UserChanges.Visibility }
            }
        }.map { userUpdate ->
            applyUserUpdates(userUpdate)
        }

    private fun applyUserUpdates(userUpdate: Map<Long, List<UserChanges>>): suspend (List<DomainContact>) -> List<DomainContact> =
        { contacts: List<DomainContact> ->
            contacts.map { contact ->
                if (userUpdate.hasContactDataChangesForUser(contact) || userUpdate.hasAliasChange()) {
                    contact.copy(contactData = contactsRepository.getContactData(contact))
                } else {
                    contact
                }
            }
        }

    private fun Map<Long, List<UserChanges>>.hasContactDataChangesForUser(
        contact: DomainContact,
    ) = this[contact.handle]?.any {
        it is UserChanges.Avatar || it is UserChanges.Firstname || it is UserChanges.Lastname
    } == true

    private fun Map<Long, List<UserChanges>>.hasAliasChange() =
        this.values.any { it.any { change -> change is UserChanges.Alias } }


    private fun List<ContactItem.Data>.sortedAlphabetically(): List<ContactItem.Data> =
        sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, ContactItem.Data::getTitle))
}
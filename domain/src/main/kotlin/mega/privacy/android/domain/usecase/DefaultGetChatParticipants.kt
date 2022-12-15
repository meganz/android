package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.entity.chat.ChatListItemChanges
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.contacts.UserStatus
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.repository.ChatParticipantsRepository
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Default get chat participants use case implementation.
 */
class DefaultGetChatParticipants @Inject constructor(
    private val chatRepository: ChatRepository,
    private val chatParticipantsRepository: ChatParticipantsRepository,
    private val contactsRepository: ContactsRepository,
    private val requestLastGreen: RequestLastGreen,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : GetChatParticipants {

    override fun invoke(chatId: Long): Flow<List<ChatParticipant>> = flow {
        val participants = mutableListOf<ChatParticipant>().apply {
            addAll(getParticipants(chatId))
        }
        emit(participants)
        emitAll(
            merge(participants.requestParticipantInfo(),
                participants.monitorChatPresenceLastGreenUpdates(),
                participants.monitorChatOnlineStatusUpdates(),
                participants.monitorChatListItemUpdates(chatId),
                participants.monitorContactUpdates()
            )
        )
    }.flowOn(defaultDispatcher)

    private suspend fun getParticipants(chatId: Long): List<ChatParticipant> {
        return chatParticipantsRepository.getAllChatParticipants(chatId).toMutableList()
    }

    private fun MutableList<ChatParticipant>.requestParticipantInfo(): Flow<MutableList<ChatParticipant>> =
        flow {
            emitAll(
                merge(
                    requestStatus(),
                    requestAvatarUri(),
                    requestCredentials(),
                    requestAlias()))
        }.flowOn(defaultDispatcher)

    private fun MutableList<ChatParticipant>.requestStatus(): Flow<MutableList<ChatParticipant>> =
        flow<MutableList<ChatParticipant>> {
            map { participant ->
                val status = chatParticipantsRepository.getStatus(participant)
                if (status != UserStatus.Online) {
                    requestLastGreen(participant.handle)
                }

                apply {
                    val currentItemIndex = indexOfFirst { it.handle == participant.handle }
                    val currentItem = get(currentItemIndex)
                    set(currentItemIndex, currentItem.copy(status = status))
                }
            }
        }.flowOn(defaultDispatcher)

    private fun MutableList<ChatParticipant>.requestCredentials(): Flow<MutableList<ChatParticipant>> =
        flow<MutableList<ChatParticipant>> {
            filter { !it.isMe }
            map { participant ->
                val verified = chatParticipantsRepository.areCredentialsVerified(participant)
                apply {
                    val currentItemIndex = indexOfFirst { it.handle == participant.handle }
                    val currentItem = get(currentItemIndex)
                    set(currentItemIndex, currentItem.copy(areCredentialsVerified = verified))
                }
                this
            }
        }.flowOn(defaultDispatcher)

    private fun MutableList<ChatParticipant>.requestAlias(): Flow<MutableList<ChatParticipant>> =
        flow<MutableList<ChatParticipant>> {
            filter { !it.isMe }
            map { participant ->
                val alias = chatParticipantsRepository.getAlias(participant)
                apply {
                    val currentItemIndex = indexOfFirst { it.handle == participant.handle }
                    val currentItem = get(currentItemIndex)
                    set(currentItemIndex,
                        currentItem.copy(data = currentItem.data.copy(alias = alias)))
                }
                this
            }
        }.flowOn(defaultDispatcher)

    private fun MutableList<ChatParticipant>.requestAvatarUri(): Flow<MutableList<ChatParticipant>> =
        flow<MutableList<ChatParticipant>> {
            map { participant ->
                val avatarUri = chatParticipantsRepository.getAvatarUri(participant)
                apply {
                    val currentItemIndex = indexOfFirst { it.handle == participant.handle }
                    val currentItem = get(currentItemIndex)
                    set(currentItemIndex,
                        currentItem.copy(data = currentItem.data.copy(avatarUri = avatarUri)))
                }
                this
            }
        }.flowOn(defaultDispatcher)

    private fun MutableList<ChatParticipant>.monitorChatPresenceLastGreenUpdates(): Flow<MutableList<ChatParticipant>> =
        contactsRepository.monitorChatPresenceLastGreenUpdates()
            .filter { any { participant -> participant.handle == it.handle } }
            .map { update ->
                apply {
                    val currentItemIndex = indexOfFirst { it.handle == update.handle }
                    val currentItem = get(currentItemIndex)
                    set(currentItemIndex, currentItem.copy(lastSeen = update.lastGreen))
                }
                this
            }

    private fun MutableList<ChatParticipant>.monitorChatOnlineStatusUpdates(): Flow<MutableList<ChatParticipant>> =
        contactsRepository.monitorChatOnlineStatusUpdates()
            .filter { any { participant -> participant.handle == it.userHandle } }
            .map { update ->
                apply {
                    val currentItemIndex = indexOfFirst { it.handle == update.userHandle }
                    val currentItem = get(currentItemIndex)
                    if (update.status != UserStatus.Online) this@DefaultGetChatParticipants.requestLastGreen(
                        update.userHandle)
                    set(currentItemIndex, currentItem.copy(status = update.status))
                }
                this
            }

    private suspend fun MutableList<ChatParticipant>.monitorChatListItemUpdates(
        chatId: Long,
    ): Flow<MutableList<ChatParticipant>> =
        chatRepository.monitorChatListItemUpdates()
            .filter { item ->
                item.chatId == chatId &&
                        (item.changes == ChatListItemChanges.OwnPrivilege || item.changes == ChatListItemChanges.LastMessage)
            }
            .map { item ->
                apply {
                    if (item.changes == ChatListItemChanges.OwnPrivilege) {
                        val currentItemIndex =
                            indexOfFirst { it.isMe }
                        if (currentItemIndex != -1) {
                            val currentItem = get(currentItemIndex)
                            set(currentItemIndex,
                                currentItem.copy(privilege = item.ownPrivilege))
                        }
                    } else if (item.changes == ChatListItemChanges.LastMessage) {
                        if (item.lastMessageType == ChatRoomLastMessage.AlterParticipants) {
                            val newList = chatParticipantsRepository.updateList(chatId, this)
                            clear()
                            addAll(newList)
                        } else if (item.lastMessageType == ChatRoomLastMessage.PrivChange) {
                            map { participant ->
                                if (!participant.isMe) {
                                    apply {
                                        val currentItemIndex =
                                            indexOfFirst { it.handle == participant.handle }
                                        val currentItem = get(currentItemIndex)
                                        val permissions = chatParticipantsRepository.getPermissions(
                                            chatId,
                                            participant)
                                        set(currentItemIndex,
                                            currentItem.copy(privilege = permissions))
                                    }
                                }
                            }
                        }
                    }
                }
                this
            }

    private fun MutableList<ChatParticipant>.monitorContactUpdates(): Flow<MutableList<ChatParticipant>> =
        contactsRepository.monitorContactUpdates()
            .map { update ->
                update.changes.forEach { (userId, changes) ->
                    if (changes.contains(UserChanges.Alias)) {
                        map { participant ->
                            if (!participant.isMe) {
                                val alias = chatParticipantsRepository.getAlias(participant)
                                apply {
                                    val currentItemIndex =
                                        indexOfFirst { it.handle == participant.handle }
                                    val currentItem = get(currentItemIndex)
                                    set(currentItemIndex,
                                        currentItem.copy(data = currentItem.data.copy(alias = alias)))
                                }
                            }
                        }
                    }
                    if (changes.contains(UserChanges.AuthenticationInformation)) {
                        map { participant ->
                            if (!participant.isMe) {
                                val verified =
                                    chatParticipantsRepository.areCredentialsVerified(participant)
                                apply {
                                    val currentItemIndex =
                                        indexOfFirst { it.handle == participant.handle }
                                    val currentItem = get(currentItemIndex)
                                    set(currentItemIndex,
                                        currentItem.copy(areCredentialsVerified = verified))
                                }
                            }
                        }
                    }

                    filter { it.handle == userId.id }
                    map { participant ->
                        if (changes.contains(UserChanges.Firstname) || changes.contains(UserChanges.Lastname)) {
                            val newName = contactsRepository.getUserFullName(participant.email)
                            apply {
                                val currentItemIndex =
                                    indexOfFirst { it.handle == participant.handle }
                                val currentItem = get(currentItemIndex)
                                set(currentItemIndex,
                                    currentItem.copy(data = currentItem.data.copy(fullName = newName)))
                            }
                        }

                        if (changes.contains(UserChanges.Email)) {
                            val newEmail = contactsRepository.getUserEmail(participant.handle)
                            apply {
                                val currentItemIndex =
                                    indexOfFirst { it.handle == participant.handle }
                                val currentItem = get(currentItemIndex)
                                set(currentItemIndex,
                                    currentItem.copy(email = newEmail))
                            }
                        }

                        if (changes.contains(UserChanges.Avatar)) {
                            val newAvatarUri = chatParticipantsRepository.getAvatarUri(participant)
                            apply {
                                val currentItemIndex =
                                    indexOfFirst { it.handle == participant.handle }
                                val currentItem = get(currentItemIndex)
                                set(currentItemIndex,
                                    currentItem.copy(data = currentItem.data.copy(avatarUri = newAvatarUri)))
                            }
                        }
                    }

                }
                this
            }
}
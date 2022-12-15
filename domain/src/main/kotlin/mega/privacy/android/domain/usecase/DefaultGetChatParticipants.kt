package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.entity.chat.ChatListItemChanges
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.contacts.UserStatus
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.repository.AvatarRepository
import mega.privacy.android.domain.repository.ChatParticipantsRepository
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Default get chat participants use case implementation.
 *
 * @property chatRepository                 [ChatRepository]
 * @property chatParticipantsRepository     [ChatParticipantsRepository]
 * @property contactsRepository             [ContactsRepository]
 * @property avatarRepository               [AvatarRepository]
 * @property requestLastGreen               [RequestLastGreen]
 * @property defaultDispatcher              [CoroutineDispatcher]

 */
class DefaultGetChatParticipants @Inject constructor(
    private val chatRepository: ChatRepository,
    private val chatParticipantsRepository: ChatParticipantsRepository,
    private val contactsRepository: ContactsRepository,
    private val avatarRepository: AvatarRepository,
    private val requestLastGreen: RequestLastGreen,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : GetChatParticipants {

    override fun invoke(chatId: Long): Flow<List<ChatParticipant>> = flow {
        val participants = mutableListOf<ChatParticipant>().apply {
            addAll(getParticipants(chatId))
        }
        emit(participants)
        emit(participants.requestParticipantsInfo())
        emitAll(
            merge(
                participants.monitorChatPresenceLastGreenUpdates(),
                participants.monitorChatOnlineStatusUpdates(),
                participants.monitorChatListItemUpdates(chatId),
                participants.monitorContactUpdates(),
                participants.monitorMyAvatarUpdates(),
                participants.monitorMyNameUpdates(),
                participants.monitorMyEmailUpdates()
            )
        )
    }.flowOn(defaultDispatcher)

    private suspend fun getParticipants(chatId: Long): List<ChatParticipant> =
        chatParticipantsRepository.getAllChatParticipants(chatId).toMutableList()

    private suspend fun MutableList<ChatParticipant>.requestParticipantsInfo(): MutableList<ChatParticipant> {
        filter { !it.isMe }
        forEach { participant ->
            apply {
                val currentItemIndex = indexOfFirst { it.handle == participant.handle }
                val currentItem = get(currentItemIndex)
                withContext(defaultDispatcher) {
                    set(currentItemIndex,
                        currentItem.copy(status = chatParticipantsRepository.getStatus(
                            currentItem),
                            areCredentialsVerified = chatParticipantsRepository.areCredentialsVerified(
                                currentItem),
                            defaultAvatarColor = chatParticipantsRepository.getAvatarColor(
                                currentItem),
                            data = currentItem.data.copy(alias = chatParticipantsRepository.getAlias(
                                currentItem),
                                avatarUri = chatParticipantsRepository.getAvatarUri(currentItem)
                                    ?.toString())))
                }
            }
        }

        return this
    }

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
                    if (update.status != UserStatus.Online) {
                        this@DefaultGetChatParticipants.requestLastGreen(update.userHandle)
                        set(currentItemIndex, currentItem.copy(status = update.status))
                    } else {
                        set(currentItemIndex,
                            currentItem.copy(status = update.status, lastSeen = null))
                    }
                }
                this
            }

    private suspend fun updateItem(chatParticipant: ChatParticipant): ChatParticipant =
        withContext(defaultDispatcher) {
            return@withContext chatParticipant.copy(
                status = chatParticipantsRepository.getStatus(chatParticipant),
                areCredentialsVerified = chatParticipantsRepository.areCredentialsVerified(
                    chatParticipant),
                defaultAvatarColor = chatParticipantsRepository.getAvatarColor(chatParticipant),
                data = chatParticipant.data.copy(alias = chatParticipantsRepository.getAlias(
                    chatParticipant),
                    avatarUri = chatParticipantsRepository.getAvatarUri(chatParticipant)
                        ?.toString()))
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
                        return@map this
                    } else if (item.changes == ChatListItemChanges.LastMessage) {
                        if (item.lastMessageType == ChatRoomLastMessage.AlterParticipants) {
                            val newList = chatParticipantsRepository.getAllChatParticipants(chatId)
                                .toMutableList()

                            newList.forEach { newItem ->
                                apply {
                                    val newItemIndex = indexOfFirst { it.handle == newItem.handle }
                                    if (newItemIndex == -1) {
                                        apply {
                                            add(updateItem(newItem))
                                        }
                                    }
                                }
                            }

                            val iterator = this.iterator()
                            iterator.forEach { currentItem ->
                                val currentItemIndex =
                                    newList.indexOfFirst { it.handle == currentItem.handle }
                                if (currentItemIndex == -1) {
                                    apply {
                                        remove(currentItem)
                                    }
                                    return@forEach
                                }
                            }

                            return@map this
                        } else if (item.lastMessageType == ChatRoomLastMessage.PrivChange) {
                            map { participant ->
                                if (!participant.isMe) {
                                    apply {
                                        val currentItemIndex = indexOf(participant)
                                        val currentItem = this[currentItemIndex]
                                        val newPermissions =
                                            chatParticipantsRepository.getPermissions(
                                                chatId,
                                                currentItem)
                                        if (currentItem.privilege != newPermissions) {
                                            this[currentItemIndex] =
                                                currentItem.copy(
                                                    privilege = newPermissions
                                                )
                                        }
                                    }
                                }
                            }

                            return@map this
                        }
                    }
                }
            }


    private fun MutableList<ChatParticipant>.monitorMyAvatarUpdates(): Flow<MutableList<ChatParticipant>> =
        avatarRepository.monitorMyAvatarFile()
            .map { file ->
                apply {
                    map { participant ->
                        if (participant.isMe) {
                            val currentItemIndex = indexOfFirst { it.handle == participant.handle }
                            val currentItem = get(currentItemIndex)

                            var avatarUri: String? = null
                            file?.let {
                                if (it.exists() && it.length() > 0) {
                                    avatarUri = it.toString()
                                }
                            }
                            set(currentItemIndex, currentItem.copy(
                                data = currentItem.data.copy(
                                    avatarUri = avatarUri,
                                ), defaultAvatarColor = avatarRepository.getMyAvatarColor()
                            ))
                        }
                    }
                }
                this
            }

    private fun MutableList<ChatParticipant>.monitorMyNameUpdates(): Flow<MutableList<ChatParticipant>> =
        chatRepository.monitorMyName()
            .map {
                apply {
                    map { participant ->
                        if (participant.isMe) {
                            val currentItemIndex = indexOfFirst { it.handle == participant.handle }
                            val currentItem = get(currentItemIndex)
                            set(currentItemIndex,
                                currentItem.copy(data = currentItem.data.copy(fullName = contactsRepository.getUserFullName(
                                    currentItem.email))))
                        }
                    }
                }
                this
            }

    private fun MutableList<ChatParticipant>.monitorMyEmailUpdates(): Flow<MutableList<ChatParticipant>> =
        chatRepository.monitorMyEmail()
            .map { update ->
                apply {
                    map { participant ->
                        if (participant.isMe) {
                            update?.let { newEmail ->
                                val currentItemIndex =
                                    indexOfFirst { it.handle == participant.handle }
                                val currentItem = get(currentItemIndex)
                                set(currentItemIndex, currentItem.copy(email = newEmail))
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
                                apply {
                                    val currentItemIndex =
                                        indexOfFirst { it.handle == participant.handle }
                                    val currentItem = this[currentItemIndex]
                                    this[currentItemIndex] =
                                        currentItem.copy(
                                            data = currentItem.data.copy(
                                                alias = chatParticipantsRepository.getAlias(
                                                    currentItem))
                                        )
                                }
                            }
                        }
                    }
                    if (changes.contains(UserChanges.AuthenticationInformation)) {
                        map { participant ->
                            if (!participant.isMe) {
                                apply {
                                    val currentItemIndex =
                                        indexOfFirst { it.handle == participant.handle }
                                    val currentItem = this[currentItemIndex]
                                    this[currentItemIndex] =
                                        currentItem.copy(
                                            areCredentialsVerified = chatParticipantsRepository.areCredentialsVerified(
                                                currentItem)
                                        )
                                }
                            }
                        }
                    }

                    filter { it.handle == userId.id }
                    map { participant ->
                        if (changes.contains(UserChanges.Firstname) || changes.contains(UserChanges.Lastname)) {
                            if (participant.handle == userId.id) {
                                apply {
                                    val currentItemIndex = indexOfFirst { it.handle == userId.id }
                                    val currentItem = this[currentItemIndex]
                                    this[currentItemIndex] =
                                        currentItem.copy(
                                            data = currentItem.data.copy(
                                                fullName = contactsRepository.getUserFullName(
                                                    currentItem.email)
                                            )
                                        )
                                }
                            }
                        }

                        if (changes.contains(UserChanges.Email)) {
                            if (participant.handle == userId.id) {
                                apply {
                                    val currentItemIndex = indexOfFirst { it.handle == userId.id }
                                    val currentItem = this[currentItemIndex]
                                    this[currentItemIndex] =
                                        currentItem.copy(
                                            email = contactsRepository.getUserEmail(currentItem.handle)
                                        )
                                }
                            }
                        }

                        if (changes.contains(UserChanges.Avatar)) {
                            if (participant.handle == userId.id) {
                                apply {
                                    val currentItemIndex = indexOfFirst { it.handle == userId.id }
                                    val currentItem = this[currentItemIndex]
                                    this[currentItemIndex] =
                                        currentItem.copy(
                                            data = currentItem.data.copy(
                                                avatarUri = chatParticipantsRepository.getAvatarUri(
                                                    currentItem)?.toString())
                                        )
                                }
                            }
                        }
                    }
                }
                this
            }
}
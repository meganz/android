package mega.privacy.android.domain.usecase.meeting.waitingroom

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
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.meeting.ChatCallChanges
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.repository.AvatarRepository
import mega.privacy.android.domain.repository.CallRepository
import mega.privacy.android.domain.repository.ChatParticipantsRepository
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Get list of participants in the waiting room
 */
class MonitorWaitingRoomParticipantsUseCase @Inject constructor(
    private val callRepository: CallRepository,
    private val chatRepository: ChatRepository,
    private val chatParticipantsRepository: ChatParticipantsRepository,
    private val contactsRepository: ContactsRepository,
    private val avatarRepository: AvatarRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {

    /**
     * Invoke
     *
     * @param chatId Chat id
     * @return                  List of [ChatParticipant]
     */
    suspend operator fun invoke(chatId: Long): Flow<List<ChatParticipant>> = flow {
        callRepository.getChatCall(chatId)?.let { call ->
            call.waitingRoom?.let { waitingRoom ->
                waitingRoom.peers?.let { list ->
                    val participants = mutableListOf<ChatParticipant>().apply {
                        addAll(
                            chatParticipantsRepository.getSeveralChatParticipants(
                                chatId = chatId,
                                list,
                                preloadUserAttributes = true
                            ).toMutableList()
                        )
                    }
                    emit(participants)
                    emit(participants.requestParticipantsInfo())
                    emitAll(
                        merge(
                            participants.monitorChatCallUpdates(chatId),
                            participants.monitorChatListItemUpdates(chatId),
                            participants.monitorContactUpdates(),
                        )
                    )
                }

            }
        }
    }.flowOn(defaultDispatcher)

    private suspend fun MutableList<ChatParticipant>.requestParticipantsInfo(): MutableList<ChatParticipant> {
        forEach { participant ->
            apply {
                val currentItemIndex = indexOfFirst { it.handle == participant.handle }
                val currentItem = get(currentItemIndex)
                withContext(defaultDispatcher) {
                    set(
                        currentItemIndex,
                        currentItem.copy(
                            defaultAvatarColor = chatParticipantsRepository.getAvatarColor(
                                currentItem
                            ),
                            data = currentItem.data.copy(
                                alias = chatParticipantsRepository.getAlias(
                                    currentItem
                                ),
                                avatarUri = chatParticipantsRepository.getAvatarUri(currentItem)
                                    ?.toString()
                            )
                        )
                    )
                }
            }
        }

        return this
    }

    private suspend fun MutableList<ChatParticipant>.monitorChatListItemUpdates(
        chatId: Long,
    ): Flow<MutableList<ChatParticipant>> =
        chatRepository.monitorChatListItemUpdates()
            .filter { item ->
                item.chatId == chatId && item.changes == ChatListItemChanges.LastMessage
            }
            .map { item ->
                apply {
                    if (item.changes == ChatListItemChanges.LastMessage) {
                        if (item.lastMessageType == ChatRoomLastMessage.PrivChange) {
                            map { participant ->
                                apply {
                                    val currentItemIndex =
                                        indexOfFirst { it.handle == participant.handle }
                                    val currentItem = this[currentItemIndex]

                                    this[currentItemIndex] =
                                        currentItem.copy(
                                            privilege = chatParticipantsRepository.getPermissions(
                                                chatId,
                                                currentItem
                                            ),
                                            privilegesUpdated = !currentItem.privilegesUpdated
                                        )
                                }
                            }
                            return@map this
                        }
                    }

                }
            }

    private suspend fun MutableList<ChatParticipant>.monitorChatCallUpdates(
        chatId: Long,
    ): Flow<MutableList<ChatParticipant>> = callRepository.monitorChatCallUpdates()
        .filter { call ->
            call.chatId == chatId && call.waitingRoom != null &&
                    (call.changes?.contains(ChatCallChanges.WaitingRoomUsersEntered) ?: false || call.changes?.contains(
                        ChatCallChanges.WaitingRoomUsersLeave
                    ) ?: false)
        }
        .map { call ->
            apply {
                val currentList = call.waitingRoom?.peers
                if (currentList.isNullOrEmpty()) {
                    return@map mutableListOf<ChatParticipant>()
                }

                val listUpdated = chatParticipantsRepository.getSeveralChatParticipants(
                    chatId = chatId,
                    currentList,
                    preloadUserAttributes = true
                ).toMutableList()

                val toRemove = this.minus(listUpdated.toSet())
                toRemove.forEach {
                    this.remove(it)
                }

                val toAdd = listUpdated.minus(this.toSet())
                toAdd.forEach {
                    val newChatParticipant = ChatParticipant(
                        handle = it.handle,
                        data = ContactData(
                            fullName = it.data.fullName,
                            alias = chatParticipantsRepository.getAlias(it),
                            avatarUri = chatParticipantsRepository.getAvatarUri(it)?.toString(),
                        ),
                        email = it.email,
                        isMe = false,
                        privilege = it.privilege,
                        defaultAvatarColor = chatParticipantsRepository.getAvatarColor(
                            it
                        )
                    )
                    this.add(newChatParticipant)
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
                                                    currentItem
                                                )
                                            )
                                        )
                                }
                            }
                        }
                        return@map this
                    }

                    map { participant ->
                        if (!participant.isMe && participant.handle == userId.id) {
                            apply {
                                val currentItemIndex =
                                    indexOfFirst { it.handle == participant.handle }
                                val currentItem = this[currentItemIndex]
                                if (changes.contains(UserChanges.Avatar)) {
                                    this[currentItemIndex] =
                                        currentItem.copy(
                                            defaultAvatarColor = avatarRepository.getAvatarColor(
                                                currentItem.handle
                                            ),
                                            avatarUpdateTimestamp = System.currentTimeMillis(),
                                            data = currentItem.data.copy(
                                                avatarUri = chatParticipantsRepository.getAvatarUri(
                                                    currentItem,
                                                    true
                                                )?.toString()
                                            )
                                        )
                                }

                                if (changes.contains(UserChanges.Firstname) || changes.contains(
                                        UserChanges.Lastname
                                    )
                                ) {
                                    this[currentItemIndex] =
                                        currentItem.copy(
                                            data = currentItem.data.copy(
                                                fullName = contactsRepository.getUserFullName(
                                                    currentItem.handle
                                                )
                                            )
                                        )
                                }

                                if (changes.contains(UserChanges.Email)) {
                                    this[currentItemIndex] =
                                        currentItem.copy(
                                            email = contactsRepository.getUserEmail(currentItem.handle)
                                        )
                                }
                            }
                        }
                    }
                }
                this
            }
}
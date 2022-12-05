package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.MeetingRoomItem
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.ContactsRepository
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

/**
 * Default get meetings use case implementation.
 */
class DefaultGetMeetings @Inject constructor(
    private val chatRepository: ChatRepository,
    private val contactsRepository: ContactsRepository,
    private val accountRepository: AccountRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : GetMeetings {

    override fun invoke(): Flow<List<MeetingRoomItem>> =
        flow {
            val meetings = mutableListOf<MeetingRoomItem>()

            chatRepository.getMeetingChatRooms()?.forEach { chatRoom ->
                if (!chatRoom.isArchived) {
                    val scheduledMeeting = getScheduledMeeting(chatRoom.chatId)
                    val isMuted = chatRepository.isChatNotifiable(chatRoom.chatId)
                    val hasPermissions = chatRoom.ownPrivilege == ChatRoomPermission.Moderator
                    val highLight = chatRoom.unreadCount > 0 || chatRoom.isCallInProgress
                            || chatRoom.lastMessageType == ChatRoomLastMessage.CallStarted

                    var firstUserChar: Char? = null
                    var lastUserChar: Char? = null
                    when (chatRoom.peerCount) {
                        0L -> {
                            val myHandle = accountRepository.getUserAccount().userId?.id
                            firstUserChar = contactsRepository.getUserFirstName(myHandle.toString())?.firstOrNull()
                        }
                        1L -> {
                            val myHandle = accountRepository.getUserAccount().userId?.id
                            val lastChatRoomPeer = chatRepository.getGroupChatPeers(chatRoom.chatId)?.lastOrNull()?.userHandle
                            firstUserChar = contactsRepository.getUserFirstName(myHandle.toString())?.firstOrNull()
                            lastUserChar = contactsRepository.getUserFirstName(lastChatRoomPeer.toString())?.firstOrNull()
                        }
                        else -> {
                            val chatRoomPeers = chatRepository.getGroupChatPeers(chatRoom.chatId)
                            chatRoomPeers.firstOrNull()?.userHandle?.toString()?.let { handle ->
                                firstUserChar = contactsRepository.getUserFirstName(handle)?.firstOrNull()
                            }
                            chatRoomPeers.lastOrNull()?.userHandle?.toString()?.let { handle ->
                                lastUserChar = contactsRepository.getUserFirstName(handle)?.firstOrNull()
                            }
                        }
                    }

                    meetings.add(MeetingRoomItem(
                        chatId = chatRoom.chatId,
                        title = chatRoom.title,
                        lastMessage = chatRoom.lastMessage,
                        unreadCount = chatRoom.unreadCount,
                        isMuted = isMuted,
                        isActive = chatRoom.isActive,
                        isPublic = chatRoom.isPublic,
                        hasPermissions = hasPermissions,
                        highlight = highLight,
                        lastTimestamp = chatRoom.lastTimestamp,
                        lastTimestampFormatted = chatRoom.lastTimestamp.formatTimestamp(),
                        scheduledStartTimestamp = scheduledMeeting?.startDateTime?.toEpochSecond(),
                        firstUserChar = firstUserChar,
                        lastUserChar = lastUserChar,
                    ))
                }
            }

            emit(meetings)
        }.flowOn(defaultDispatcher)

    private suspend fun getScheduledMeeting(chatId: Long): ChatScheduledMeeting? =
        chatRepository.getScheduledMeetingsByChat(chatId)?.firstOrNull()

    private fun Long.formatTimestamp(): String =
        DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.LONG)
            .format(Instant.ofEpochSecond(this))
}

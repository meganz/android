package mega.privacy.android.feature.chat.navigation

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.RegexPatternType.CHAT_LINK
import mega.privacy.android.domain.entity.RegexPatternType.NEW_MESSAGE_CHAT_LINK
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.chat.ChatLinkContent
import mega.privacy.android.domain.exception.chat.IAmOnAnotherCallException
import mega.privacy.android.domain.exception.chat.MeetingEndedException
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.call.AnswerChatCallUseCase
import mega.privacy.android.domain.usecase.call.GetChatCallUseCase
import mega.privacy.android.domain.usecase.chat.IsEphemeralPlusPlusUseCase
import mega.privacy.android.domain.usecase.chat.link.GetChatLinkContentUseCase
import mega.privacy.android.domain.usecase.meeting.GetScheduledMeetingByChatUseCase
import mega.privacy.android.domain.usecase.meeting.StartMeetingInWaitingRoomChatUseCase
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.destination.ChatNavKey
import mega.privacy.android.navigation.destination.ChatsNavKey
import mega.privacy.android.navigation.destination.LegacyMeetingNavKey
import mega.privacy.android.navigation.destination.LegacyOpenLinkAfterFetchNodes
import mega.privacy.android.navigation.destination.LegacyWaitingRoomNavKey
import mega.privacy.android.navigation.destination.MeetingNavKeyInfo
import mega.privacy.android.navigation.destination.WaitingRoomNavKeyInfo
import mega.privacy.android.shared.resources.R
import javax.inject.Inject

/**
 * Deep link handler for chats deep links
 */
class ChatsDeepLinkHandler @Inject constructor(
    private val getChatLinkContentUseCase: GetChatLinkContentUseCase,
    private val getChatRoomUseCase: GetChatRoomUseCase,
    private val getChatCallUseCase: GetChatCallUseCase,
    private val getScheduledMeetingByChatUseCase: GetScheduledMeetingByChatUseCase,
    private val startMeetingInWaitingRoomChatUseCase: StartMeetingInWaitingRoomChatUseCase,
    private val answerChatCallUseCase: AnswerChatCallUseCase,
    private val isEphemeralPlusPlusUseCase: IsEphemeralPlusPlusUseCase,
    private val rootNodeExistsUseCase: RootNodeExistsUseCase,
    snackbarEventQueue: SnackbarEventQueue,
) : DeepLinkHandler(snackbarEventQueue) {
    override suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
    ): List<NavKey>? = when (regexPatternType) {
        NEW_MESSAGE_CHAT_LINK -> listOf(ChatsNavKey())
        else -> null
    }

    override suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
        isLoggedIn: Boolean,
    ): List<NavKey>? = when (regexPatternType) {
        CHAT_LINK -> if (isLoggedIn
            && runCatching { rootNodeExistsUseCase() }.getOrDefault(false).not()
        ) {
            listOf(LegacyOpenLinkAfterFetchNodes(uri.toString()))
        } else {
            runCatching {
                getChatLinkContentUseCase(uri.toString())
            }.fold(
                onSuccess = { chatLinkContent ->
                    when {
                        chatLinkContent.chatHandle == -1L || chatLinkContent.link.isEmpty() -> {
                            snackbarEventQueue.queueMessage(R.string.general_invalid_link)
                            emptyList()
                        }

                        chatLinkContent is ChatLinkContent.MeetingLink -> {
                            if (isLoggedIn) {
                                getChatRoomUseCase(chatLinkContent.chatHandle)?.let { chatRoom ->
                                    val call = getChatCallUseCase(chatRoom.chatId)
                                    if (chatRoom.isWaitingRoom
                                        && chatRoom.ownPrivilege == ChatRoomPermission.Moderator
                                    ) {
                                        startOrAnswerMeetingWithWaitingRoomAsAHost(
                                            call = call,
                                            chatId = chatRoom.chatId
                                        )
                                    } else {
                                        joinMeetingOrReturnToCall(
                                            call = call,
                                            chatId = chatLinkContent.chatHandle,
                                            isWaitingRoom = chatLinkContent.isWaitingRoom,
                                            link = chatLinkContent.link,
                                            rejoin = chatLinkContent.exist,
                                            publicChatHandle = chatLinkContent.userHandle,
                                            meetingName = chatLinkContent.text,
                                        )
                                    }
                                } ?: run {
                                    snackbarEventQueue.queueMessage(R.string.invalid_chat_link_error_message)
                                    emptyList()
                                }
                            } else {
                                openMeetingInGuestMode(
                                    chatId = chatLinkContent.chatHandle,
                                    isWaitingRoom = chatLinkContent.isWaitingRoom,
                                    link = chatLinkContent.link,
                                    meetingName = chatLinkContent.text,
                                )
                            }
                        }

                        else -> {
                            listOf(
                                ChatNavKey(
                                    chatId = chatLinkContent.chatHandle,
                                    action = ACTION_OPEN_CHAT_LINK,
                                    link = chatLinkContent.link
                                )
                            )
                        }
                    }
                },
                onFailure = { e ->
                    when (e) {
                        is IAmOnAnotherCallException -> {
                            snackbarEventQueue.queueMessage(R.string.can_only_join_one_call_error_message)
                            emptyList()
                        }

                        is MeetingEndedException -> {
                            listOf(MeetingHasEndedDialogNavKey(false, e.chatId))
                        }

                        else -> {
                            snackbarEventQueue.queueMessage(
                                if (isLoggedIn) {
                                    R.string.invalid_chat_link_error_message
                                } else {
                                    R.string.general_invalid_link
                                }
                            )
                            emptyList()
                        }
                    }
                })
        }

        else -> super.getNavKeys(uri, regexPatternType, isLoggedIn)
    }

    private suspend fun startOrAnswerMeetingWithWaitingRoomAsAHost(
        call: ChatCall?,
        chatId: Long,
    ) = listOfNotNull(
        call?.let {
            when (call.status) {
                ChatCallStatus.UserNoPresent,
                ChatCallStatus.Connecting,
                ChatCallStatus.Joining,
                ChatCallStatus.InProgress,
                    -> answerCall(chatId)

                else -> getScheduledMeetingByChatUseCase(chatId)?.first()?.schedId?.let { schedIdWr ->
                    startMeetingInWaitingRoomChatUseCase(
                        chatId = chatId,
                        schedIdWr = schedIdWr,
                        enabledVideo = false,
                        enabledAudio = true
                    )?.let { call ->
                        call.chatId.takeIf { it != -1L }?.let { openCall(call, false) }
                    }
                }
            }
        }
    )

    private suspend fun answerCall(chatId: Long) =
        answerChatCallUseCase(chatId = chatId, video = false, audio = true)?.let { call ->
            openCall(call, true)
        }

    private suspend fun openCall(call: ChatCall, answer: Boolean) =
        LegacyMeetingNavKey(
            call.chatId,
            MeetingNavKeyInfo.OpenCall(
                callId = call.callId,
                isGuest = isEphemeralPlusPlusUseCase(),
                hasLocalVideo = call.hasLocalVideo,
                isOutgoing = call.isOutgoing,
                answer = answer,
            )
        )

    private suspend fun joinMeetingOrReturnToCall(
        call: ChatCall?,
        chatId: Long,
        isWaitingRoom: Boolean,
        link: String,
        rejoin: Boolean,
        publicChatHandle: Long,
        meetingName: String,
    ) = listOf(
        if (call == null
            || call.status == ChatCallStatus.UserNoPresent
            || call.status == ChatCallStatus.WaitingRoom
        ) {
            if (isWaitingRoom) {
                LegacyWaitingRoomNavKey(chatId, WaitingRoomNavKeyInfo.JoinWaitingRoom(link))
            } else {
                LegacyMeetingNavKey(
                    chatId,
                    if (rejoin) {
                        MeetingNavKeyInfo.RejoinInProgressCall(meetingName, publicChatHandle, link)
                    } else {
                        MeetingNavKeyInfo.JoinInProgressCall(meetingName, link)
                    }
                )
            }
        } else {
            LegacyMeetingNavKey(
                chatId,
                MeetingNavKeyInfo.ReturnToInProgressCall(isEphemeralPlusPlusUseCase())
            )
        }
    )

    private fun openMeetingInGuestMode(
        chatId: Long,
        isWaitingRoom: Boolean,
        link: String,
        meetingName: String,
    ) = listOf(
        if (isWaitingRoom) {
            LegacyWaitingRoomNavKey(chatId, WaitingRoomNavKeyInfo.JoinAsGuest(link))
        } else {
            LegacyMeetingNavKey(chatId, MeetingNavKeyInfo.JoinAsGuest(meetingName, link))
        }
    )
}

internal const val ACTION_OPEN_CHAT_LINK = "OPEN_CHAT_LINK"
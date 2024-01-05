package mega.privacy.android.domain.usecase.chat.link

import mega.privacy.android.domain.entity.ChatRequestParamType
import mega.privacy.android.domain.entity.chat.ChatLinkContent
import mega.privacy.android.domain.exception.chat.IAmOnAnotherCallException
import mega.privacy.android.domain.exception.chat.MeetingEndedException
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.usecase.CheckChatLinkUseCase
import mega.privacy.android.domain.usecase.chat.CheckIfIAmInThisMeetingUseCase
import mega.privacy.android.domain.usecase.chat.GetAnotherCallParticipatingUseCase
import mega.privacy.android.domain.usecase.chat.IsMeetingEndedUseCase
import javax.inject.Inject

/**
 * Get Chat Link Content Use Case
 *
 */
class GetChatLinkContentUseCase @Inject constructor(
    private val checkChatLinkUseCase: CheckChatLinkUseCase,
    private val isMeetingEndedUseCase: IsMeetingEndedUseCase,
    private val getAnotherCallParticipatingUseCase: GetAnotherCallParticipatingUseCase,
    private val repository: ChatRepository,
    private val checkIfIAmInThisMeetingUseCase: CheckIfIAmInThisMeetingUseCase,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(link: String): ChatLinkContent {
        val request = checkChatLinkUseCase(link)
        if (request.paramType == ChatRequestParamType.MEETING_LINK) {
            if (isMeetingEndedUseCase(request.privilege, request.handleList)) {
                throw MeetingEndedException(request.link.orEmpty(), request.chatHandle,)
            }
            if (checkIfIAmInThisMeetingUseCase(request.chatHandle)) {
                return ChatLinkContent.MeetingLink(
                    link = request.link.orEmpty(),
                    chatHandle = request.chatHandle,
                    isInThisMeeting = true,
                    handles = request.handleList,
                    text = request.text.orEmpty(),
                    userHandle = request.userHandle,
                    exist = false,
                    isWaitingRoom = repository.hasWaitingRoomChatOptions(request.privilege),
                )
            }
            if (getAnotherCallParticipatingUseCase(request.chatHandle) != repository.getChatInvalidHandle()) {
                throw IAmOnAnotherCallException()
            }
            val chatPreview = repository.openChatPreview(request.link.orEmpty())
            return ChatLinkContent.MeetingLink(
                link = chatPreview.request.link.orEmpty(),
                chatHandle = chatPreview.request.chatHandle,
                isInThisMeeting = false,
                handles = chatPreview.request.handleList,
                text = chatPreview.request.text.orEmpty(),
                userHandle = chatPreview.request.userHandle,
                exist = chatPreview.exist,
                isWaitingRoom = repository.hasWaitingRoomChatOptions(chatPreview.request.privilege),
            )
        } else {
            return ChatLinkContent.ChatLink(
                link = request.link.orEmpty(),
                chatHandle = request.chatHandle,
            )
        }
    }
}
package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.usecase.chat.CreateEphemeralAccountUseCase
import mega.privacy.android.domain.usecase.chat.InitGuestChatSessionUseCase
import mega.privacy.android.domain.usecase.chat.link.OpenChatPreviewUseCase
import mega.privacy.android.domain.usecase.login.ChatLogoutUseCase
import javax.inject.Inject

/**
 * Join meeting as guest use case
 */
class JoinMeetingAsGuestUseCase @Inject constructor(
    private val openChatPreviewUseCase: OpenChatPreviewUseCase,
    private val chatLogoutUseCase: ChatLogoutUseCase,
    private val initGuestChatSessionUseCase: InitGuestChatSessionUseCase,
    private val createEphemeralAccountUseCase: CreateEphemeralAccountUseCase,
) {

    /**
     * Invoke
     *
     * @param meetingLink
     * @param firstName
     * @param lastName
     */
    suspend operator fun invoke(meetingLink: String, firstName: String, lastName: String) {
        openChatPreviewUseCase(meetingLink)
        chatLogoutUseCase()
        initGuestChatSessionUseCase(anonymousMode = false)
        createEphemeralAccountUseCase(firstName, lastName)
        openChatPreviewUseCase(meetingLink)
    }

}
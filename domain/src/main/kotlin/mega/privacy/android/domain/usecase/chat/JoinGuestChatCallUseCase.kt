package mega.privacy.android.domain.usecase.chat

import javax.inject.Inject

/**
 * Use case to join chat calls for guests
 *
 * @property createEphemeralAccountUseCase  [CreateEphemeralAccountUseCase]
 * @property initGuestChatSessionUseCase    [InitGuestChatSessionUseCase]
 * @property joinChatLinkUseCase            [JoinChatLinkUseCase]
 */
class JoinGuestChatCallUseCase @Inject constructor(
    private val createEphemeralAccountUseCase: CreateEphemeralAccountUseCase,
    private val initGuestChatSessionUseCase: InitGuestChatSessionUseCase,
    private val joinChatLinkUseCase: JoinChatLinkUseCase,
) {

    /**
     * Invoke
     *
     * @param chatLink
     * @param firstName
     * @param lastName
     */
    suspend operator fun invoke(
        chatLink: String,
        firstName: String,
        lastName: String,
    ) {
        initGuestChatSessionUseCase(anonymousMode = false)

        createEphemeralAccountUseCase(firstName, lastName)

        joinChatLinkUseCase(chatLink)
    }
}

package mega.privacy.android.domain.usecase.chat

import javax.inject.Inject

/**
 * Use case to join chat calls for guests
 *
 * @property createEphemeralAccountUseCase  [CreateEphemeralAccountUseCase]
 * @property initGuestChatSessionUseCase    [InitGuestChatSessionUseCase]
 * @property joinChatCallUseCase            [JoinChatCallUseCase]
 */
class JoinGuestChatCallUseCase @Inject constructor(
    private val createEphemeralAccountUseCase: CreateEphemeralAccountUseCase,
    private val initGuestChatSessionUseCase: InitGuestChatSessionUseCase,
    private val joinChatCallUseCase: JoinChatCallUseCase,
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

        joinChatCallUseCase(chatLink)
    }
}

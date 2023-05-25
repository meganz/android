package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Use case to get a list with the ids of active calls
 */
class GetChatCallIdsUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {

    /**
     * Invoke
     *
     * @return  List of call ids
     */
    suspend operator fun invoke(): List<Long> =
        callRepository.getChatCallIds()
}

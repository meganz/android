package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Use case to get if it's pending to hung up.
 */
class IsPendingToHangUpUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {
    /**
     * Invoke
     *
     * @param chatId    Chat id
     */
    suspend operator fun invoke(chatId: Long): Boolean = callRepository.isPendingToHangUp(chatId)
}
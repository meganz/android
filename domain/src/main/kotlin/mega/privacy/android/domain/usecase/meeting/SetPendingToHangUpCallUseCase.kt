package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * Use case to set a call to hang up when we retrieve the call information as we don't have it yet.
 */
class SetPendingToHangUpCallUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {
    /**
     * Invoke
     *
     * @param chatId    Chat id
     * @param add       True, add chatId, false, remove it.
     */
    suspend operator fun invoke(chatId: Long, add: Boolean) {
        when {
            add -> {
                callRepository.addCallPendingToHangUp(chatId)
                callRepository.removeFakeIncomingCall(chatId)
            }

            callRepository.isPendingToHangUp(chatId) -> callRepository.removeCallPendingToHangUp(
                chatId
            )

            else -> {}
        }
    }
}
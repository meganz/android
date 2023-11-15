package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * MonitorChatCallUpdatesUseCase
 *
 * Gets chat call updates which was part of MegaRequestListener
 */
class MonitorChatCallUpdatesUseCase @Inject constructor(private val callRepository: CallRepository) {

    /**
     * Invoke
     *
     * Calls monitor chat call flow from [CallRepository]
     * @return [Flow<ChatCall>]
     */
    operator fun invoke() = callRepository.monitorChatCallUpdates()
}
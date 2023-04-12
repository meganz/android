package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.repository.CallRepository
import javax.inject.Inject

/**
 * MonitorChatSessionUpdatesUseCase
 *
 * Gets chat session updates which was part of MegaRequestListener
 */
class MonitorChatSessionUpdatesUseCase @Inject constructor(private val callRepository: CallRepository) {

    /**
     * Invoke
     *
     * Calls monitor chat session flow from [CallRepository]
     * @return [Flow<ChatSessionUpdate>]
     */
    operator fun invoke() = callRepository.monitorChatSessionUpdates()
}
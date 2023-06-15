package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.repository.NetworkRepository
import javax.inject.Inject

/**
 * Use Case to Monitor if chat in presence of a Network Signal
 */
class MonitorChatSignalPresenceUseCase @Inject constructor(
    private val networkRepository: NetworkRepository,
) {
    /**
     * Invoke and Monitor Network Repository if chat signal is presence
     */
    operator fun invoke(): Flow<Unit> = networkRepository.monitorChatSignalPresence()
}
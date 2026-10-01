package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.NetworkRepository
import javax.inject.Inject

/**
 * Use Case to Monitor if chat in presence of a Network Signal
 */
class MonitorChatSignalPresenceUseCase @Inject constructor(
    private val networkRepository: NetworkRepository,
    private val chatRepository: ChatRepository,
) {
    /**
     * Invoke and Monitor Network Repository if chat signal is presence
     */
    operator fun invoke(): Flow<Unit> = networkRepository.monitorChatSignalPresence().filter {
        val preference = chatRepository.getChatPresenceConfig()
        preference != null && !preference.isPending
    }
}
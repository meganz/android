package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import mega.privacy.android.app.domain.repository.ChatRepository
import javax.inject.Inject

class DefaultMonitorChatNotificationCount @Inject constructor(private val chatRepository: ChatRepository) : MonitorChatNotificationCount {
    override fun invoke(): Flow<Int> {
        return flow {
            emit(chatRepository.getUnreadNotificationCount())
            emitAll(chatRepository.getUnreadNotificationCountChanges())
        }
    }
}
package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.app.domain.repository.ChatRepository
import javax.inject.Inject


class DefaultIsChatLoggedIn @Inject constructor(private val chatRepository: ChatRepository) : IsChatLoggedIn {
    override fun invoke(): Flow<Boolean> {
        return flow {
            emit(true)
            emitAll(chatRepository.notifyChatLogout().map { !it })
        }
    }
}

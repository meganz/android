package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.entity.*
import mega.privacy.android.app.domain.repository.ChatRepository
import javax.inject.Inject

class DefaultIsOnCall @Inject constructor(private val chatRepository: ChatRepository) : IsOnCall {
    private val validCalStates =
        listOf(
            CallStatus.Initial,
            CallStatus.Connecting,
            CallStatus.Joining,
            CallStatus.InProgress,
        )

    override suspend fun invoke(): Boolean {
        return validCalStates.any {
            chatRepository.getCallCountByState(it) > 0
        }
    }
}
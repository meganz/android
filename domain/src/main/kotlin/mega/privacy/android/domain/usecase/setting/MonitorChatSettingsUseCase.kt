package mega.privacy.android.domain.usecase.setting

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.settings.ChatSettings
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for monitoring the chat settings.
 */
class MonitorChatSettingsUseCase @Inject constructor(private val chatRepository: ChatRepository) {

    /**
     * Invoke.
     *
     * @return a [Flow] of [ChatSettings], emitting null if no chat settings are stored.
     */
    operator fun invoke(): Flow<ChatSettings?> = chatRepository.monitorChatSettings()
}

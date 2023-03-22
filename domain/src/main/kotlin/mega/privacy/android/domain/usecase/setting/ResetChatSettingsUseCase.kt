package mega.privacy.android.domain.usecase.setting

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for checking if chat settings is already initialized. If not, reset them by default.
 */
class ResetChatSettingsUseCase @Inject constructor(private val chatRepository: ChatRepository) {

    /**
     * Invoke.
     */
    suspend operator fun invoke() = chatRepository.resetChatSettings()
}
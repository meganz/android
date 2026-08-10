package mega.privacy.android.domain.usecase.setting

import mega.privacy.android.domain.entity.settings.ChatSettings
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for storing the chat settings.
 */
class SetChatSettingsUseCase @Inject constructor(private val chatRepository: ChatRepository) {

    /**
     * Invoke.
     *
     * @param chatSettings the [ChatSettings] to store.
     */
    suspend operator fun invoke(chatSettings: ChatSettings) =
        chatRepository.setChatSettings(chatSettings)
}

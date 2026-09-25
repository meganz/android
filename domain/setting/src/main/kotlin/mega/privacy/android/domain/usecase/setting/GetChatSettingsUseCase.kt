package mega.privacy.android.domain.usecase.setting

import mega.privacy.android.domain.entity.settings.ChatSettings
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for getting the stored chat settings.
 */
class GetChatSettingsUseCase @Inject constructor(private val chatRepository: ChatRepository) {

    /**
     * Invoke.
     *
     * @return the stored [ChatSettings], or null if none are stored.
     */
    suspend operator fun invoke(): ChatSettings? = chatRepository.getChatSettings()
}

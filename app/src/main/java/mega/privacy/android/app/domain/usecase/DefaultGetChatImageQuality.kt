package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.entity.ChatImageQuality
import mega.privacy.android.app.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Default get chat image quality.
 *
 *  @property settingsRepository Repository in which the value will be get.
 */
class DefaultGetChatImageQuality @Inject constructor(
    private val settingsRepository: SettingsRepository
) : GetChatImageQuality {

    override fun invoke(): Flow<ChatImageQuality> = settingsRepository.getChatImageQuality()
}
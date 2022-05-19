package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.entity.ChatImageQuality
import mega.privacy.android.app.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Default set chat image quality.
 *
 *  @property settingsRepository Repository in which the value will be updated.
 */
class DefaultSetChatImageQuality @Inject constructor(
    private val settingsRepository: SettingsRepository
) : SetChatImageQuality {
    override suspend fun invoke(quality: ChatImageQuality) =
        settingsRepository.setChatImageQuality(quality)
}
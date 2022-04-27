package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.exception.SettingNotFoundException
import mega.privacy.android.app.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Default toggle auto accept q r links
 *
 * @property fetchAutoAcceptQRLinks
 * @property settingsRepository
 */
class DefaultToggleAutoAcceptQRLinks @Inject constructor(
    private val fetchAutoAcceptQRLinks: FetchAutoAcceptQRLinks,
    private val settingsRepository: SettingsRepository,
) : ToggleAutoAcceptQRLinks {
    override suspend fun invoke(): Boolean {
        return kotlin.runCatching {
            fetchAutoAcceptQRLinks()
        }.onSuccess { current ->
            return settingsRepository.setAutoAcceptQR(!current)
        }.recover { error ->
            if (error is SettingNotFoundException) {
                return settingsRepository.setAutoAcceptQR(true)
            }
            throw error
        }.getOrThrow()
    }
}
package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.exception.SettingNotFoundException
import mega.privacy.android.app.domain.repository.SettingsRepository
import javax.inject.Inject

class DefaultToggleAutoAcceptQRLinks @Inject constructor(
    private val fetchAutoAcceptQRLinks: FetchAutoAcceptQRLinks,
    private val settingsRepository: SettingsRepository,
) : ToggleAutoAcceptQRLinks {
    override suspend fun invoke(): Boolean {
        return kotlin.runCatching {
            val currentValue = fetchAutoAcceptQRLinks()
            return settingsRepository.setAutoAcceptQR(!currentValue)
        }.onFailure { error ->
            if (error is SettingNotFoundException) {
                return settingsRepository.setAutoAcceptQR(false)
            }
            throw error
        }.getOrDefault(false)
    }
}
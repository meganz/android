package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.exception.SettingNotFoundException
import mega.privacy.android.app.domain.repository.SettingsRepository
import javax.inject.Inject

class DefaultToggleAutoAcceptQRLinks @Inject constructor(
    private val fetchAutoAcceptQRLinks: FetchAutoAcceptQRLinks,
    private val settingsRepository: SettingsRepository,
) : ToggleAutoAcceptQRLinks {
    override suspend fun invoke(): Boolean {
        kotlin.runCatching {
            fetchAutoAcceptQRLinks()
        }.onSuccess { currentValue ->
            settingsRepository.setAutoAcceptQR(!currentValue)
            return !currentValue
        }.onFailure { error ->
            if (error is SettingNotFoundException) {
                settingsRepository.setAutoAcceptQR(false)
                return false
            }

            throw error
        }

        return false
    }
}
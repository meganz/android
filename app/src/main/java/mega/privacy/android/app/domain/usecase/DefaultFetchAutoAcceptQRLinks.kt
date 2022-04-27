package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Default fetch auto accept q r links
 *
 * @property settingsRepository
 */
class DefaultFetchAutoAcceptQRLinks @Inject constructor(private val settingsRepository: SettingsRepository) : FetchAutoAcceptQRLinks {
    override suspend fun invoke(): Boolean {
        return settingsRepository.fetchContactLinksOption()
    }
}
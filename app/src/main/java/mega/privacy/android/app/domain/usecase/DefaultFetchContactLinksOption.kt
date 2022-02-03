package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.SettingsRepository
import nz.mega.sdk.MegaRequestListenerInterface
import javax.inject.Inject

class DefaultFetchContactLinksOption @Inject constructor(private val settingsRepository: SettingsRepository) : FetchContactLinksOption {
    override fun invoke(request: MegaRequestListenerInterface) {
        settingsRepository.fetchContactLinksOption(request)
    }
}
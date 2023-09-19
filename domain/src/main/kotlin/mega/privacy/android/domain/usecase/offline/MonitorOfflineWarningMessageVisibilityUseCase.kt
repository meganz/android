package mega.privacy.android.domain.usecase.offline

import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * MonitorOfflineWarningMessageVisibility
 */
class MonitorOfflineWarningMessageVisibilityUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {

    /**
     * Invoke
     */
    operator fun invoke() =
        settingsRepository.monitorOfflineWarningMessageVisibility().map { it ?: true }
}
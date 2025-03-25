package mega.privacy.android.domain.usecase.call

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Gets calls sound notifications preference.
 *
 */
class MonitorCallSoundEnabledUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {

    /**
     * Invoke.
     *
     * @return Calls sound notifications.
     */
    operator fun invoke() = settingsRepository.getCallsSoundNotifications()
}
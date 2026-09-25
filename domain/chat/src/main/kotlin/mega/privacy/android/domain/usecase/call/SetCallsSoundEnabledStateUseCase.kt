package mega.privacy.android.domain.usecase.call

import mega.privacy.android.domain.entity.CallsSoundEnabledState
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Sets calls sound notifications preference.
 */
class SetCallsSoundEnabledStateUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {

    /**
     * Invoke.
     *
     * @param soundNotificationsStatus sound notification.
     * @return Status of calls sound notifications.
     */
    suspend operator fun invoke(soundNotificationsStatus: CallsSoundEnabledState) =
        settingsRepository.setCallsSoundNotifications(soundNotificationsStatus)
}
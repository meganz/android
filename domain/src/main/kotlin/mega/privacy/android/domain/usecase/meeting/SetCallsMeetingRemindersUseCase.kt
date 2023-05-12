package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.CallsMeetingReminders
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Sets calls meeting reminders preference.
 * @property settingsRepository
 */
class SetCallsMeetingRemindersUseCase @Inject constructor(
    val settingsRepository: SettingsRepository,
) {

    /**
     * Invoke.
     *
     * @param callsMeetingReminders meeting reminders.
     */
    suspend operator fun invoke(callsMeetingReminders: CallsMeetingReminders) {
        settingsRepository.setCallsMeetingReminders(callsMeetingReminders)
    }
}

package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.meeting.UsersCallLimitReminders
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Sets users call limit reminders preference.
 * @property settingsRepository
 */
class SetUsersCallLimitRemindersUseCase @Inject constructor(
    val settingsRepository: SettingsRepository,
) {

    /**
     * Invoke.
     *
     * @param usersCallLimitReminder users call limit reminders.
     */
    suspend operator fun invoke(usersCallLimitReminder: UsersCallLimitReminders) {
        settingsRepository.setUsersCallLimitReminders(usersCallLimitReminder)
    }
}
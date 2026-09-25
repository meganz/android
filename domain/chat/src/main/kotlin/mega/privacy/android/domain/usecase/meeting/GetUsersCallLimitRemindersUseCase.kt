package mega.privacy.android.domain.usecase.meeting

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.meeting.UsersCallLimitReminders
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Gets users call limit reminders preference.
 * @property settingsRepository
 */
class GetUsersCallLimitRemindersUseCase @Inject constructor(
    val settingsRepository: SettingsRepository,
) {

    /**
     * Invoke.
     *
     * @return usersCallLimitReminders users call limit reminders.
     */
    operator fun invoke(): Flow<UsersCallLimitReminders> =
        settingsRepository.getUsersCallLimitReminders()
}
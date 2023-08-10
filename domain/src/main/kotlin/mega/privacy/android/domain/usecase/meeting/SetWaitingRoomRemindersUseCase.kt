package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.meeting.WaitingRoomReminders
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Sets waiting room reminders preference.
 * @property settingsRepository
 */
class SetWaitingRoomRemindersUseCase @Inject constructor(
    val settingsRepository: SettingsRepository,
) {

    /**
     * Invoke.
     *
     * @param waitingRoomReminders waiting room reminders.
     */
    suspend operator fun invoke(waitingRoomReminders: WaitingRoomReminders) {
        settingsRepository.setWaitingRoomReminders(waitingRoomReminders)
    }
}
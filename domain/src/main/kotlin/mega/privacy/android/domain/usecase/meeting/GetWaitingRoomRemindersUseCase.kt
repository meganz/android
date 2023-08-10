package mega.privacy.android.domain.usecase.meeting

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.meeting.WaitingRoomReminders
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Gets waiting room reminders preference.
 * @property settingsRepository
 */
class GetWaitingRoomRemindersUseCase @Inject constructor(
    val settingsRepository: SettingsRepository,
) {

    /**
     * Invoke.
     *
     * @return callsMeetingInvitations meeting invitations.
     */
    operator fun invoke(): Flow<WaitingRoomReminders> =
        settingsRepository.getWaitingRoomReminders()
}
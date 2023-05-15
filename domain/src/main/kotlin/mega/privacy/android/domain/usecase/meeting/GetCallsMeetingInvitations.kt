package mega.privacy.android.domain.usecase.meeting

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.CallsMeetingInvitations
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Gets calls meeting invitations preference.
 * @property settingsRepository
 */
class GetCallsMeetingInvitations @Inject constructor(
    val settingsRepository: SettingsRepository,
) {

    /**
     * Invoke.
     *
     * @return callsMeetingInvitations meeting invitations.
     */
    operator fun invoke(): Flow<CallsMeetingInvitations> =
        settingsRepository.getCallsMeetingInvitations()
}

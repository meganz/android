package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.entity.CallsMeetingInvitations
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Sets calls meeting invitations preference.
 * @property settingsRepository
 */
class SetCallsMeetingInvitationsUseCase @Inject constructor(
    val settingsRepository: SettingsRepository,
) {

    /**
     * Invoke.
     *
     * @param callsMeetingInvitations meeting invitations.
     */
    suspend operator fun invoke(callsMeetingInvitations: CallsMeetingInvitations) {
        settingsRepository.setCallsMeetingInvitations(callsMeetingInvitations)
    }
}

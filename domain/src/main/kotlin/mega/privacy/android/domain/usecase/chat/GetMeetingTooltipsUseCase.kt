package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.chat.MeetingTooltipItem
import mega.privacy.android.domain.repository.RemotePreferencesRepository
import javax.inject.Inject

/**
 * Get meeting tooltips use case
 *
 * @property remotePreferencesRepository [RemotePreferencesRepository]
 */
class GetMeetingTooltipsUseCase @Inject constructor(
    private val remotePreferencesRepository: RemotePreferencesRepository,
) {

    /**
     * Get scheduled meetings tooltip to be shown
     *
     * @return [MeetingTooltipItem]
     */
    suspend operator fun invoke(): MeetingTooltipItem =
        remotePreferencesRepository.getMeetingTooltipPreference()
}

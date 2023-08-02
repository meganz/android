package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.chat.MeetingTooltipItem
import mega.privacy.android.domain.repository.RemotePreferencesRepository
import javax.inject.Inject

/**
 * Set next meeting tooltip use case
 *
 * @property remotePreferencesRepository [RemotePreferencesRepository]
 */
class SetNextMeetingTooltipUseCase @Inject constructor(
    private val remotePreferencesRepository: RemotePreferencesRepository,
) {

    /**
     * Set scheduled meetings tooltip to be shown
     *
     * @param item   [MeetingTooltipItem]
     */
    suspend operator fun invoke(item: MeetingTooltipItem) =
        remotePreferencesRepository.setMeetingTooltipPreference(item)
}

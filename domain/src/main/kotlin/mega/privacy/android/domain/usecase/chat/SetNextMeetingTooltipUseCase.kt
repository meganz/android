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
     * Set next scheduled meetings tooltip based on current item
     *
     * @param currentItem   [MeetingTooltipItem]
     */
    suspend operator fun invoke(currentItem: MeetingTooltipItem) {
        val items = MeetingTooltipItem.values()
        val currentItemIndex = items.indexOf(currentItem)
        if (currentItemIndex < items.size - 1) {
            val nextItem = items[currentItemIndex + 1]
            remotePreferencesRepository.setMeetingTooltipPreference(nextItem)
        } else {
            remotePreferencesRepository.setMeetingTooltipPreference(currentItem)
        }
    }
}

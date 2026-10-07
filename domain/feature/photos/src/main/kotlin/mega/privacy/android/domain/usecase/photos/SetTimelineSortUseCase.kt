package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.entity.media.MediaTimelineSort
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Persist the timeline sort preference.
 */
class SetTimelineSortUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {

    /**
     * Invoke
     *
     * @param sort the sort to persist
     */
    suspend operator fun invoke(sort: MediaTimelineSort) =
        settingsRepository.setTimelineSort(sort)
}

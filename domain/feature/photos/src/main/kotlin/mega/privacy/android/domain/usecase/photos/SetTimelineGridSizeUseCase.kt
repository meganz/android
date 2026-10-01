package mega.privacy.android.domain.usecase.photos

import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Persist the timeline grid size preference.
 */
class SetTimelineGridSizeUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {

    /**
     * Invoke
     *
     * @param value the grid size ordinal to persist
     */
    suspend operator fun invoke(value: Int) = settingsRepository.setTimelineGridSize(value)
}

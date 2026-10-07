package mega.privacy.android.domain.usecase.photos

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.media.MediaTimelineSort
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Monitor the persisted timeline sort preference.
 */
class MonitorTimelineSortUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {

    /**
     * Invoke
     *
     * @return a [Flow] emitting the persisted sort, or [MediaTimelineSort.Default] when none has
     * been stored
     */
    operator fun invoke(): Flow<MediaTimelineSort> =
        settingsRepository.monitorTimelineSort().map { it ?: MediaTimelineSort.Default }
}

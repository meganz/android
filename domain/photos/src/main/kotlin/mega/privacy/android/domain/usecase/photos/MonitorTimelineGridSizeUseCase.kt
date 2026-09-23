package mega.privacy.android.domain.usecase.photos

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Monitor the persisted timeline grid size preference.
 */
class MonitorTimelineGridSizeUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {

    /**
     * Invoke
     *
     * @return a [Flow] emitting the persisted grid size ordinal, or `null` when none has been stored
     */
    operator fun invoke(): Flow<Int?> = settingsRepository.monitorTimelineGridSize()
}

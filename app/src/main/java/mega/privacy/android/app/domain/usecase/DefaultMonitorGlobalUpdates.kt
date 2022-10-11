package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.app.domain.repository.GlobalStatesRepository
import javax.inject.Inject

/**
 * Default monitor global updates implementation
 *
 * @property globalStatesRepository
 */
class DefaultMonitorGlobalUpdates @Inject constructor(
    private val globalStatesRepository: GlobalStatesRepository
) : MonitorGlobalUpdates {
    @Suppress("DEPRECATION")
    @Deprecated("See GlobalUpdatesRepository for individual replacements to use instead.")
    override fun invoke(): Flow<GlobalUpdate> = globalStatesRepository.monitorGlobalUpdates()
}
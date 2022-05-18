package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.data.model.GlobalUpdate
import mega.privacy.android.app.domain.repository.GlobalUpdatesRepository
import javax.inject.Inject

/**
 * Default monitor global updates implementation
 *
 * @property globalUpdatesRepository
 */
class DefaultMonitorGlobalUpdates @Inject constructor(
    private val globalUpdatesRepository: GlobalUpdatesRepository
) : MonitorGlobalUpdates {
    @Suppress("DEPRECATION")
    @Deprecated("See GlobalUpdatesRepository for individual replacements to use instead.")
    override fun invoke(): Flow<GlobalUpdate> = globalUpdatesRepository.monitorGlobalUpdates()
}
package mega.privacy.android.feature.sync.domain.usecase.progress

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.feature.sync.domain.entity.progress.SyncActivity
import mega.privacy.android.feature.sync.domain.repository.SyncProgressRepository
import javax.inject.Inject

/** Monitors the app-wide sync activity; the first value may be NotSeeded. */
internal class MonitorSyncActivityUseCase @Inject constructor(
    private val syncProgressRepository: SyncProgressRepository,
) {
    operator fun invoke(): Flow<SyncActivity> = syncProgressRepository.monitorSyncActivity()
}

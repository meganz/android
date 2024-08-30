package mega.privacy.android.feature.sync.domain.usecase.sync.worker

import mega.privacy.android.domain.usecase.appstart.AppStartTask
import javax.inject.Inject

/**
 * Task to stop the sync worker when the app starts.
 *
 * When the app is in the foreground, work manager is NOT required for syncing,
 * SDK does everything on its own.
 */
class StopSyncWorkerTask @Inject constructor(
    private val stopSyncWorkerUseCase: StopSyncWorkerUseCase,
) : AppStartTask {

    override suspend fun invoke() {
        stopSyncWorkerUseCase()
    }
}
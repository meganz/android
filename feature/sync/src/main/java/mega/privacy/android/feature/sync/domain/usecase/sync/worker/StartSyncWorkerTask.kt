package mega.privacy.android.feature.sync.domain.usecase.sync.worker

import mega.privacy.android.domain.usecase.appstart.AppStopTask
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.shared.sync.featuretoggles.SyncFeatures
import javax.inject.Inject

/**
 * Task to start the sync worker when the app stops to ensure that the folders are always synced
 */
class StartSyncWorkerTask @Inject constructor(
    private val startSyncWorkerUseCase: StartSyncWorkerUseCase,
) : AppStopTask {

    override suspend fun invoke() {
        startSyncWorkerUseCase()
    }
}
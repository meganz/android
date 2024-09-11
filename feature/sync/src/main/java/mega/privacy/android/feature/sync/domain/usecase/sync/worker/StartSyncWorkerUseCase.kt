package mega.privacy.android.feature.sync.domain.usecase.sync.worker

import kotlinx.coroutines.flow.first
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorSyncByWiFiUseCase
import javax.inject.Inject

/**
 * Use case to start the sync worker which will sync the folders when the app is closed
 */
class StartSyncWorkerUseCase @Inject constructor(
    private val syncRepository: SyncRepository,
    private val getSyncFrequencyUseCase: GetSyncFrequencyUseCase,
    private val getSyncByWiFiUseCase: MonitorSyncByWiFiUseCase
) {

    /**
     * Start the sync worker which will sync the folders when the app is closed
     */
    suspend operator fun invoke() {
        val syncFrequency = getSyncFrequencyUseCase()
        val syncByWifi = getSyncByWiFiUseCase().first()
        syncRepository.startSyncWorker(syncFrequency, syncByWifi)
    }
}
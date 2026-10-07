package mega.privacy.android.feature.sync.domain.usecase.progress

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.IsInTransferOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorStorageOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorTransferOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.paused.MonitorPausedTransfersUseCase
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressBlockers
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncStalledIssuesUseCase
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Monitors the conditions that can stop sync transfers from moving.
 */
internal class MonitorSyncProgressBlockersUseCase @Inject constructor(
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val monitorStorageOverQuotaUseCase: MonitorStorageOverQuotaUseCase,
    private val monitorTransferOverQuotaUseCase: MonitorTransferOverQuotaUseCase,
    private val isInTransferOverQuotaUseCase: IsInTransferOverQuotaUseCase,
    private val monitorPausedTransfersUseCase: MonitorPausedTransfersUseCase,
    private val monitorSyncStalledIssuesUseCase: MonitorSyncStalledIssuesUseCase,
) {

    operator fun invoke(): Flow<SyncProgressBlockers> = combine(
        monitorConnectivityUseCase(),
        monitorStorageOverQuotaUseCase(),
        // The flag's own start value is discarded upstream and sync transfers never raise it,
        // so the SDK is read on every emission.
        monitorTransferOverQuotaUseCase().map { isFlagSet ->
            isFlagSet || isSdkInTransferOverQuota()
        },
        monitorPausedTransfersUseCase(),
        monitorSyncStalledIssuesUseCase().map { it.size },
    ) { isConnected, isStorageOverQuota, isTransferOverQuota, areTransfersPaused, stalledIssueCount ->
        SyncProgressBlockers(
            isOffline = !isConnected,
            isStorageOverQuota = isStorageOverQuota,
            isTransferOverQuota = isTransferOverQuota,
            areTransfersPaused = areTransfersPaused,
            stalledIssueCount = stalledIssueCount,
        )
    }.distinctUntilChanged()

    private suspend fun isSdkInTransferOverQuota(): Boolean = try {
        isInTransferOverQuotaUseCase()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.w(e, "Failed to read the transfer over quota state")
        false
    }
}

package mega.privacy.android.feature.sync.domain.usecase.progress

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.core.coroutine.logAndSwallowExceptions
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Keeps the sync progress folds running while an account is logged in, from any login path.
 */
interface StartSyncProgressMonitoringUseCase {
    /** Never returns; suspends until the caller is cancelled. */
    suspend operator fun invoke()
}

internal class StartSyncProgressMonitoringUseCaseImpl @Inject constructor(
    private val monitorLoggedInRootNodeUseCase: MonitorLoggedInRootNodeUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val runSyncRunSnapshotUseCase: RunSyncRunSnapshotUseCase,
    private val resetSyncRunSnapshotUseCase: ResetSyncRunSnapshotUseCase,
    private val runSyncTransferStoreUseCase: RunSyncTransferStoreUseCase,
    private val resetSyncTransferStoreUseCase: ResetSyncTransferStoreUseCase,
    private val reconcileSyncTransfersUseCase: ReconcileSyncTransfersUseCase,
) : StartSyncProgressMonitoringUseCase {

    override suspend fun invoke() {
        monitorLoggedInRootNodeUseCase()
            .retryWhen { cause, attempt ->
                Timber.e(cause, "Sync progress session monitor failed")
                delay(retryDelay(attempt))
                true
            }
            // A retried monitor re-emits the same account; the distinct check below the retry
            // keeps the session.
            .distinctUntilChanged()
            .collectLatest { rootNode -> if (rootNode != null) runSession() }
    }

    private suspend fun runSession() {
        if (!isSyncProgressEnabled()) {
            Timber.d("Sync progress session: flag off")
            return
        }
        resetSyncProgress()
        Timber.d("Sync progress session: started")
        try {
            coroutineScope {
                launch { runCatching { runSyncRunSnapshotUseCase() }.logAndSwallowExceptions() }
                launch { runCatching { runSyncTransferStoreUseCase() }.logAndSwallowExceptions() }
                launch { runCatching { reconcileSyncTransfersUseCase() }.logAndSwallowExceptions() }
            }
        } finally {
            withContext(NonCancellable) {
                resetSyncProgress()
                Timber.d("Sync progress session: ended")
            }
        }
    }

    private suspend fun isSyncProgressEnabled(): Boolean = try {
        getFeatureFlagValueUseCase(ApiFeatures.SyncProgress)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.w(e, "Could not read the sync progress flag")
        false
    }

    private suspend fun resetSyncProgress() {
        runCatching { resetSyncRunSnapshotUseCase() }.logAndSwallowExceptions()
        runCatching { resetSyncTransferStoreUseCase() }.logAndSwallowExceptions()
    }

    private fun retryDelay(attempt: Long): Duration =
        (1L shl attempt.coerceAtMost(6).toInt()).seconds.coerceAtMost(1.minutes)
}

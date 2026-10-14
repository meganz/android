package mega.privacy.android.feature.sync.domain.usecase.progress

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreState
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Requests a reseed of the sync transfer store when the store keeps `active` or `overflow` rows and
 * no sync reports transfer work, both unchanged for [RECONCILE_AFTER]. Suspends until cancelled.
 */
internal class ReconcileSyncTransfersUseCase @Inject constructor(
    private val monitorSyncRunSnapshotUseCase: MonitorSyncRunSnapshotUseCase,
    private val monitorSyncTransferStoreUseCase: MonitorSyncTransferStoreUseCase,
    private val requestSyncTransferReseedUseCase: RequestSyncTransferReseedUseCase,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend operator fun invoke() {
        var reseededFor: SyncTransferStoreState? = null
        monitorSyncRunSnapshotUseCase()
            .map { !it.hasTransferWork }
            .distinctUntilChanged()
            .flatMapLatest { isIdle ->
                if (isIdle) {
                    monitorSyncTransferStoreUseCase().map { store ->
                        store.takeIf { it.active.isNotEmpty() || it.overflow.isNotEmpty() }
                    }
                } else {
                    flowOf(null)
                }
            }
            .distinctUntilChanged()
            .collectLatest { mismatch ->
                if (mismatch == null) {
                    reseededFor = null
                    return@collectLatest
                }
                if (mismatch == reseededFor) return@collectLatest
                delay(RECONCILE_AFTER)
                reseededFor = mismatch
                Timber.d("Sync transfer rows outlived the syncs' transfer work, requesting a reseed")
                runCatching { requestSyncTransferReseedUseCase() }.onFailure {
                    if (it is CancellationException) throw it
                    Timber.w(it, "Could not request a sync transfer reseed")
                }
            }
    }

    companion object {
        /** How long the store must keep rows with no sync transfer work before a reseed request. */
        val RECONCILE_AFTER = 10.seconds
    }
}

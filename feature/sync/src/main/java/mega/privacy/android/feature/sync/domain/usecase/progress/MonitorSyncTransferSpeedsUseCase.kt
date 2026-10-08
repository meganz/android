package mega.privacy.android.feature.sync.domain.usecase.progress

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import mega.privacy.android.core.coroutine.logAndSwallowExceptions
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.transfers.downloads.GetCurrentDownloadSpeedUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.GetCurrentUploadSpeedUseCase
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferSpeeds
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Monitors the upload and download speeds of sync transfers, as [GetSyncTransferSpeedsUseCase]
 * resolves them from the store and the SDK speeds. The SDK is read at once and then every
 * [SPEED_REFRESH], only while some sync transfer is pending.
 */
internal class MonitorSyncTransferSpeedsUseCase @Inject constructor(
    private val monitorSyncTransferStoreUseCase: MonitorSyncTransferStoreUseCase,
    private val getCurrentUploadSpeedUseCase: GetCurrentUploadSpeedUseCase,
    private val getCurrentDownloadSpeedUseCase: GetCurrentDownloadSpeedUseCase,
    private val getSyncTransferSpeedsUseCase: GetSyncTransferSpeedsUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<SyncTransferSpeeds> {
        val store = monitorSyncTransferStoreUseCase()
        return store
            .map { it.counts.pendingSyncTransfers > 0 }
            // A distinct gate, so store publishes never restart the poll.
            .distinctUntilChanged()
            .flatMapLatest { hasPending ->
                if (hasPending) {
                    combine(store, sdkSpeeds()) { state, sdk ->
                        getSyncTransferSpeedsUseCase(state, sdk.upload, sdk.download)
                    }
                } else {
                    flowOf(NotPending)
                }
            }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    private fun sdkSpeeds(): Flow<SdkSpeeds> = flow {
        while (true) {
            emit(
                SdkSpeeds(
                    upload = read { getCurrentUploadSpeedUseCase() },
                    download = read { getCurrentDownloadSpeedUseCase().toLong() },
                )
            )
            delay(SPEED_REFRESH)
        }
    }

    private suspend fun read(block: suspend () -> Long): Long? =
        runCatching { block() }.logAndSwallowExceptions().getOrNull()

    private data class SdkSpeeds(val upload: Long?, val download: Long?)

    companion object {
        /** How often the SDK speeds are read while a sync transfer is pending. */
        val SPEED_REFRESH = 700.milliseconds

        private val NotPending = SyncTransferSpeeds(uploadSpeed = null, downloadSpeed = null)
    }
}

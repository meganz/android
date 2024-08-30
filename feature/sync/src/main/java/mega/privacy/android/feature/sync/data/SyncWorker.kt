package mega.privacy.android.feature.sync.data

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import mega.privacy.android.domain.qualifier.LoginMutex
import mega.privacy.android.domain.usecase.login.BackgroundFastLoginUseCase
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncsUseCase
import timber.log.Timber

/**
 * Sync Worker designed to run the Sync process periodically in the background
 * when the app is closed.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val monitorSyncsUseCase: MonitorSyncsUseCase,
    @LoginMutex private val loginMutex: Mutex,
    private val backgroundFastLoginUseCase: BackgroundFastLoginUseCase,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (!loginMutex.isLocked) {
            runCatching { backgroundFastLoginUseCase() }.getOrElse(Timber::e)
        }
        while (true) {
            delay(SYNC_WORKER_RECHECK_DELAY)
            val syncs = monitorSyncsUseCase().first()
            Timber.d("SyncWorker syncs: $syncs")
            if (syncs.all { it.syncStatus == SyncStatus.SYNCED }) {
                Timber.d("SyncWorker finished")
                return Result.success()
            }
        }
    }

    companion object {
        /**
         * Tag identifying the worker when enqueued
         *
         */
        const val SYNC_WORKER_TAG = "SYNC_WORKER_TAG"

        /**
         * Delay for to check whether the syncs are finished
         */
        const val SYNC_WORKER_RECHECK_DELAY = 6000L
    }
}
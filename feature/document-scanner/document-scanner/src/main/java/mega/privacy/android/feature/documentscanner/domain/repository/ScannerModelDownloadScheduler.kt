package mega.privacy.android.feature.documentscanner.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.feature.documentscanner.domain.model.ScannerModelDownloadState

/**
 * Schedules and observes the background download of the scanner model.
 *
 * The job is enqueued as unique work, so repeatedly asking to download (e.g.
 * tapping "Scan" several times) keeps the in-flight download rather than
 * stacking duplicates.
 */
interface ScannerModelDownloadScheduler {

    /**
     * Enqueues the model-download job, keeping any already-running one.
     *
     * @param requireUnmeteredNetwork when true the download is deferred until an
     * un-metered (Wi-Fi) network is available — used when the user declined a
     * metered download, so it completes quietly in the background. When false it
     * runs as soon as any network is connected, which is what the prepare screen
     * waits on.
     */
    suspend fun enqueueModelDownload(requireUnmeteredNetwork: Boolean)

    /**
     * Observes the state of the unique model-download job so the prepare screen
     * can render progress and react to success or failure.
     *
     * Emits [ScannerModelDownloadState.Pending] when there is no job yet or it is
     * still waiting on its constraints.
     */
    fun monitorModelDownload(): Flow<ScannerModelDownloadState>
}

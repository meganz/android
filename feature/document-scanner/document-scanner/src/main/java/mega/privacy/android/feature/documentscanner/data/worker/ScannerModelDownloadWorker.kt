package mega.privacy.android.feature.documentscanner.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import mega.privacy.android.feature.documentscanner.data.model.ModelDownloadException
import mega.privacy.android.feature.documentscanner.domain.model.ScannerModelProvider
import timber.log.Timber

/**
 * Background worker that downloads (and integrity-verifies) the TFLite boundary
 * detection model on first scanner use.
 *
 * The work is delegated to [ScannerModelProvider.ensureModelReady], which is a
 * no-op when the model is already cached. The provider's progress callback is
 * forwarded to [setProgress] so the UI can observe it via WorkManager's
 * `WorkInfo.progress`.
 *
 * Network and battery constraints are not declared here; the enqueuer
 * (presentation layer) sets them based on user consent — cellular requires an
 * explicit opt-in, while [androidx.work.Constraints.Builder.setRequiresBatteryNotLow]
 * keeps WorkManager from running the download on a low battery and resumes it
 * once the device is charging or above the threshold.
 *
 * Retries: the underlying HTTP client has explicit timeouts, so a stuck network
 * connection surfaces as an exception rather than a hang. We retry with the
 * default exponential backoff up to [MAX_ATTEMPTS] before giving up so the
 * presentation layer can route the user to the legacy scanner.
 */
@HiltWorker
internal class ScannerModelDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val modelProvider: ScannerModelProvider,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        modelProvider.ensureModelReady { downloaded, total ->
            setProgress(
                workDataOf(
                    KEY_BYTES_DOWNLOADED to downloaded,
                    KEY_TOTAL_BYTES to total,
                )
            )
        }
        Result.success()
    } catch (cancellation: CancellationException) {
        // WorkManager cancels by cancelling the coroutine (e.g. constraint
        // broken mid-run); propagate so the framework records it as cancelled
        // rather than retried.
        throw cancellation
    } catch (permanent: ModelDownloadException.Permanent) {
        // HTTP 4xx (model removed or moved): retrying won't help. Fail fast so
        // the presentation layer can route the user to the legacy scanner.
        Timber.e(permanent, "[DocScanner][model] Worker failed permanently")
        Result.failure(workDataOf(KEY_FAILURE_REASON to FAILURE_PERMANENT))
    } catch (error: Throwable) {
        val attempt = runAttemptCount + 1
        val attemptsRemaining = MAX_ATTEMPTS - attempt
        Timber.w(error, "[DocScanner][model] Worker failed (attempt $attempt, $attemptsRemaining left)")
        if (attemptsRemaining > 0) {
            Result.retry()
        } else {
            Result.failure(workDataOf(KEY_FAILURE_REASON to FAILURE_TRANSIENT))
        }
    }

    companion object {
        const val UNIQUE_NAME = "scanner-model-download"
        const val KEY_BYTES_DOWNLOADED = "bytes_downloaded"
        const val KEY_TOTAL_BYTES = "total_bytes"

        // Failure reason written to `Result.failure(outputData)` so the prepare
        // screen can distinguish "model URL is gone, don't bother retrying" from
        // "we exhausted the retry budget but the network might recover".
        const val KEY_FAILURE_REASON = "failure_reason"
        const val FAILURE_PERMANENT = "permanent"
        const val FAILURE_TRANSIENT = "transient"

        // Caps the total attempts (initial + retries) per WorkManager enqueue.
        // A fresh enqueueUniqueWork resets `runAttemptCount` to 0, so this does
        // not bound the user's lifetime attempts — only attempts within one
        // enqueue cycle.
        internal const val MAX_ATTEMPTS = 5
    }
}

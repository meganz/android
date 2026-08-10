package mega.privacy.android.feature.documentscanner.domain.model

/**
 * Progress of the background scanner-model download, as observed by the prepare
 * screen. Derived from the WorkManager job keyed on
 * `ScannerModelDownloadWorker.UNIQUE_NAME`.
 */
sealed interface ScannerModelDownloadState {

    /** No download job exists yet — nothing has been enqueued. */
    data object NotStarted : ScannerModelDownloadState

    /**
     * The job is enqueued or waiting on its constraints (e.g. network) and has
     * not reported byte progress yet. The prepare screen shows an indeterminate
     * indicator.
     */
    data object Pending : ScannerModelDownloadState

    /**
     * A previous attempt failed transiently (e.g. a dropped connection) and
     * WorkManager is waiting to retry. Distinct from [Pending] so the prepare
     * screen can show a "reconnecting" hint instead of letting the progress look
     * like it silently reset.
     */
    data object Retrying : ScannerModelDownloadState

    /**
     * The job is running and reporting progress.
     *
     * @property bytesDownloaded bytes fetched so far.
     * @property totalBytes total bytes to fetch, or `0` when not yet known.
     */
    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytes: Long,
    ) : ScannerModelDownloadState {

        /** Completion fraction in `0f..1f`, or `null` while the total is unknown. */
        val progress: Float?
            get() = if (totalBytes > 0) {
                (bytesDownloaded.toFloat() / totalBytes).coerceIn(0f, 1f)
            } else {
                null
            }
    }

    /** The model is downloaded and verified; the camera can open. */
    data object Completed : ScannerModelDownloadState

    /**
     * The download failed.
     *
     * @property permanent `true` when retrying cannot help (e.g. the model URL is
     * gone), so the prepare screen only offers the legacy scanner; `false` when the
     * retry budget was exhausted but a fresh attempt might succeed.
     */
    data class Failed(val permanent: Boolean) : ScannerModelDownloadState
}

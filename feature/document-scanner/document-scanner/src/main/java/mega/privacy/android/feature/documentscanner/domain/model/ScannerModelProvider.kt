package mega.privacy.android.feature.documentscanner.domain.model

import java.io.File

/**
 * Supplies the on-device TFLite model file for the document boundary detector.
 *
 * The model (~93 MB) is not bundled in the APK; it is downloaded once on first
 * use and cached in the app's private storage. This keeps the install size
 * small at the cost of a one-time download before the scanner can run.
 *
 * Two access patterns:
 * - [cachedModelFile] is a cheap, non-blocking check used by the detector on
 *   the analysis thread to grab the cached file. Returns null only before the
 *   one-time download; once [ensureModelReady] has succeeded, the file is on
 *   disk for the rest of the process lifetime.
 * - [ensureModelReady] performs the (suspending) download + verification.
 *   Callers must invoke it before any detection path — the detector treats a
 *   present cached file as a precondition.
 */
interface ScannerModelProvider {

    /**
     * The cached, integrity-verified model file, or null if it has not been
     * downloaded yet. Non-blocking; safe to call from the analysis thread.
     */
    fun cachedModelFile(): File?

    /**
     * Ensures the model is downloaded and integrity-verified, returning the
     * local file. No-op when the verified file is already cached. Throws if the
     * download or verification fails so the caller can surface an error state.
     *
     * @param onProgress invoked periodically (≈ every 1% of the artifact size)
     *  during the download with `(bytesDownloaded, totalBytes)`. Not called
     *  when the file is already cached. Suspending so the worker can forward
     *  it directly to `setProgress` without an extra coroutine launch.
     */
    suspend fun ensureModelReady(
        onProgress: suspend (downloadedBytes: Long, totalBytes: Long) -> Unit,
    ): File
}

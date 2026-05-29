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
 *   the analysis thread: it returns the verified file if it's already on disk,
 *   or null if the model still needs downloading (the detector then yields no
 *   detections until it appears).
 * - [ensureModelReady] performs the (suspending) download + verification and is
 *   driven by the presentation layer, which can show progress while it runs.
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
     */
    suspend fun ensureModelReady(): File
}

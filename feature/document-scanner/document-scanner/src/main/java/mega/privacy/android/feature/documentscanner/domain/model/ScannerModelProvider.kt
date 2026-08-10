package mega.privacy.android.feature.documentscanner.domain.model

import java.io.File
import kotlinx.coroutines.flow.Flow

/**
 * Supplies the on-device TFLite model file for the document boundary detector.
 *
 * The model (~93 MB) is not bundled in the APK; it is downloaded once on first
 * use and cached in the app's private storage. This keeps the installation size
 * small at the cost of a one-time download before the scanner can run.
 *
 * Two access patterns:
 * - [cachedModelFile] is a cheap, non-blocking check used by the detector on
 *   the analysis thread to grab the cached file. Returns null only before the
 *   one-time download; once [ensureModelReady] has completed, the file is on
 *   disk for the rest of the process lifetime.
 * - [ensureModelReady] performs the download + verification, emitting progress
 *   along the way. Callers must collect it to completion before any detection
 *   path — the detector treats a present cached file as a precondition.
 */
interface ScannerModelProvider {

    /**
     * The cached, integrity-verified model file, or null if it has not been
     * downloaded yet. Non-blocking; safe to call from the analysis thread.
     */
    fun cachedModelFile(): File?

    /**
     * Ensures the model is downloaded and integrity-verified. No-op (empty
     * flow) when the file is already cached. Emits `(bytesDownloaded,
     * totalBytes)` approximately every 1% of the artifact during the download.
     * Throws if the download or verification fails so the collector can surface
     * an error state.
     */
    fun ensureModelReady(): Flow<Pair<Long, Long>>

    companion object {
        /**
         * Exact size of the model artifact, in bytes. Single source of truth for
         * both the download integrity check and the size shown to the user in the
         * download-confirmation dialog — keep it here so the displayed figure can
         * never drift from the size we verify against. Update it whenever the model
         * is re-trained / re-exported.
         */
        const val SIZE_BYTES = 97_867_228L
    }
}

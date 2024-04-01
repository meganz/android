package mega.privacy.android.app.camera.state

import android.net.Uri

/**
 * Capture result
 *
 */
sealed interface CaptureResult {
    /**
     * Success
     *
     * @property savedUri
     */
    data class Success(val savedUri: Uri?) : CaptureResult

    /**
     * Error
     *
     * @property throwable
     */
    data class Error(val throwable: Throwable?) : CaptureResult
}

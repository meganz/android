package mega.privacy.android.app.camera

import android.net.Uri
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed

/**
 * Camera ui state
 *
 * @property onCapturePhotoEvent
 * @property onCaptureVideoEvent
 */
data class CameraUiState(
    val onCapturePhotoEvent: StateEventWithContent<Uri?> = consumed(),
    val onCaptureVideoEvent: StateEventWithContent<Uri?> = consumed(),
)
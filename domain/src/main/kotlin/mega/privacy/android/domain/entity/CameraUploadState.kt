package mega.privacy.android.domain.entity

/**
 * CameraUpload State info
 *
 * @property shouldStopProcess
 * @property shouldSendEvent
 */
data class CameraUploadState(val shouldStopProcess: Boolean, val shouldSendEvent: Boolean)

package mega.privacy.android.domain.entity.camerauploads

/**
 * To hold Camera UploadState
 * @property primaryPendingUploads
 * @property secondaryPendingUploads
 * @property lastPrimaryTimeStamp
 * @property lastSecondaryTimeStamp
 * @property lastPrimaryHandle
 * @property lastSecondaryHandle
 * @property primaryTotalUploadBytes
 * @property secondaryTotalUploadBytes
 * @property primaryTotalUploadedBytes
 * @property secondaryTotalUploadedBytes
 * @property totalToUpload
 * @property totalUploaded
 * @property totalNumber
 */
data class CameraUploadsState(
    var primaryPendingUploads: Int = 0,
    var secondaryPendingUploads: Int = 0,
    var lastPrimaryTimeStamp: Long = -1,
    var lastSecondaryTimeStamp: Long = -1,
    var lastPrimaryHandle: Long = -1,
    var lastSecondaryHandle: Long = -1,
    var primaryTotalUploadBytes: Long = 0,
    var secondaryTotalUploadBytes: Long = 0,
    var primaryTotalUploadedBytes: Long = 0,
    var secondaryTotalUploadedBytes: Long = 0,
    var totalToUpload: Int = 0,
    var totalUploaded: Int = 0,
    var totalNumber: Int = 0,
)

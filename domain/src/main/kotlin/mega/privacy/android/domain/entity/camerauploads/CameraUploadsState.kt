package mega.privacy.android.domain.entity.camerauploads

import kotlin.math.roundToInt

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
) {
    /**
     *  Number of files pending to be uploaded
     */
    val pendingToUpload: Int
        get() = totalToUpload - totalUploaded

    /**
     *  Total number of bytes to be uploaded
     */
    val totalUploadBytes: Long
        get() = primaryTotalUploadBytes + secondaryTotalUploadBytes

    /**
     *  Total number of bytes uploaded
     */
    val totalUploadedBytes: Long
        get() = primaryTotalUploadedBytes + secondaryTotalUploadedBytes


    /**
     *  Current progress of the Camera uploads
     */
    val progress: Int
        get() =
            when (totalUploadBytes) {
                0L -> 0
                else -> ((totalUploadedBytes.toDouble() / totalUploadBytes) * 100).roundToInt()
            }

}

package mega.privacy.android.domain.entity.camerauploads

import mega.privacy.android.domain.entity.Progress

/**
 * Camera Uploads State
 * @property primaryCameraUploadsState state of primary folder
 * @property secondaryCameraUploadsState state of secondary folder
 * @property totalToUploadCount total number of files to upload
 * @property totalUploadedCount total number of files uploaded
 * @property totalPendingCount total number of files remaining to be uploaded
 * @property totalBytesToUploadCount Total number of bytes to upload
 * @property totalBytesUploadedCount Total number of bytes uploaded
 * @property totalProgress current progress of the camera uploads process. A [Float] between 0 and 1
 */
data class CameraUploadsState(
    val primaryCameraUploadsState: CameraUploadsFolderState = CameraUploadsFolderState(),
    val secondaryCameraUploadsState: CameraUploadsFolderState = CameraUploadsFolderState(),
) {
    val totalToUploadCount: Int
        get() = primaryCameraUploadsState.toUploadCount + secondaryCameraUploadsState.toUploadCount

    val totalUploadedCount: Int
        get() = primaryCameraUploadsState.uploadedCount + secondaryCameraUploadsState.uploadedCount

    val totalPendingCount: Int
        get() = totalToUploadCount - totalUploadedCount

    val totalBytesToUploadCount: Long
        get() = primaryCameraUploadsState.bytesToUploadCount + secondaryCameraUploadsState.bytesToUploadCount

    val totalBytesUploadedCount: Long
        get() = primaryCameraUploadsState.bytesUploadedCount + secondaryCameraUploadsState.bytesUploadedCount

    val totalProgress: Progress
        get() =
            when (totalBytesToUploadCount) {
                0L -> Progress(0f)
                else -> Progress(totalBytesUploadedCount, totalBytesToUploadCount)
            }
}

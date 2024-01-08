package mega.privacy.android.domain.entity.camerauploads

/**
 * Camera Uploads Status Info
 */
sealed interface CameraUploadsStatusInfo {

    /**
     *  object used to notify that the Camera Uploads has started
     */
    data object Started : CameraUploadsStatusInfo

    /**
     *  object used to notify that the Camera Uploads has finished
     */
    data object Finished : CameraUploadsStatusInfo

    /**
     *  class for displaying a notification for upload progress
     *  @property totalUploaded
     *  @property totalToUpload
     *  @property totalUploadedBytes
     *  @property totalUploadBytes
     *  @property progress
     *  @property areUploadsPaused
     */
    data class Progress(
        val totalUploaded: Int,
        val totalToUpload: Int,
        val totalUploadedBytes: Long,
        val totalUploadBytes: Long,
        val progress: Int,
        val areUploadsPaused: Boolean,
    ) : CameraUploadsStatusInfo

    /**
     *  class for displaying a notification for video compression progress
     *  @property progress
     *  @property currentFileIndex
     *  @property totalCount
     */
    data class VideoCompressionProgress(
        val progress: Int,
        val currentFileIndex: Int,
        val totalCount: Int,
    ) : CameraUploadsStatusInfo

    /**
     *  object used to notify video compression success
     */
    data object VideoCompressionSuccess : CameraUploadsStatusInfo

    /**
     *  object used to notify that local storage is out of space while compressing Video
     */
    data object VideoCompressionOutOfSpace : CameraUploadsStatusInfo

    /**
     *  object used to notify video compression error
     */
    data object VideoCompressionError : CameraUploadsStatusInfo

    /**
     *  object used to notify that camera uploads worker is checking Files For Upload
     */
    data object CheckFilesForUpload : CameraUploadsStatusInfo

    /**
     *  object used to notify that storage quota exceeded
     */
    data object StorageOverQuota : CameraUploadsStatusInfo

    /**
     *  object used to notify that Not Enough Storage on the Device
     */
    data object NotEnoughStorage : CameraUploadsStatusInfo

    /**
     *  object used to notify that Not Enough Storage on the Device
     *  @property cameraUploadsFolderType
     */
    data class FolderUnavailable(val cameraUploadsFolderType: CameraUploadFolderType) :
        CameraUploadsStatusInfo
}

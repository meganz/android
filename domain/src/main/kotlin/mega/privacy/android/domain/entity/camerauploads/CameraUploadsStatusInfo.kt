package mega.privacy.android.domain.entity.camerauploads

/**
 * Camera Uploads Status Info
 */
sealed interface CameraUploadsStatusInfo {

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
     *  object used to notify that local storage is out of space while compressing Video
     */
    object VideoCompressionOutOfSpace : CameraUploadsStatusInfo

    /**
     *  object used to notify video compression error
     */
    object VideoCompressionError : CameraUploadsStatusInfo


    /**
     *  object used to notify that camera uploads worker is checking Files For Upload
     */
    object CheckFilesForUpload : CameraUploadsStatusInfo

    /**
     *  object used to notify that storage quota exceeded
     */
    object StorageOverQuota : CameraUploadsStatusInfo

    /**
     *  object used to notify that Not Enough Storage on the Device
     */
    object NotEnoughStorage : CameraUploadsStatusInfo

    /**
     *  object used to notify that Not Enough Storage on the Device
     *  @property cameraUploadsFolderType
     */
    data class FolderUnavailable(val cameraUploadsFolderType: CameraUploadFolderType) :
        CameraUploadsStatusInfo
}

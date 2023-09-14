package mega.privacy.android.domain.entity.camerauploads

import mega.privacy.android.domain.entity.SyncRecordType

/**
 * Representation of the files,used by Camera Uploads Process, to identify which files to be uploaded
 *
 * @property mediaId _id retrieved from the mediaStore that uniquely identifies the media
 * @property fileName the name of the file retrieved from the mediaStore
 * @property filePath the absolute file path of the file retrieved from the mediaStore
 * @property timestamp the max between the creation date and the modified date
 * @property folderType Primary or Secondary
 * @property type Photo or Video
 * @property uploadStatus the upload status of the file when transferred through CU
 * @property originalFingerprint the fingerprint of the original file
 *                               Used for comparing files on the cloud with local files
 * @property generatedFingerprint the fingerprint of the generated file
 *                                Used for comparing files uploaded with local generated files (for video compression and gps tag removal)
 * @property tempFilePath the temporary path where the generated file will be located (used for video compression and gps tags removal)
 */
data class CameraUploadsRecord(
    val mediaId: Long,
    val fileName: String,
    val filePath: String,
    val timestamp: Long,
    val folderType: CameraUploadFolderType,
    val type: SyncRecordType,
    val uploadStatus: CameraUploadsRecordUploadStatus,
    val originalFingerprint: String,
    val generatedFingerprint: String?,
    val tempFilePath: String
)

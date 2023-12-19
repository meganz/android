package mega.privacy.android.domain.entity.camerauploads

import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.node.NodeId

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
 * @property latitude the latitude coordinates extracted from the file. This value is computed during upload process
 * @property longitude the longitude coordinates extracted from the file. This value is computed during upload process
 * @property existsInTargetNode true if the file already exists in the target Node. This value is computed during upload process
 * @property existingNodeId nodeId that corresponds to the parent folder This value is computed during upload process
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
    val tempFilePath: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val existsInTargetNode: Boolean? = null,
    val existingNodeId: NodeId? = null,
)

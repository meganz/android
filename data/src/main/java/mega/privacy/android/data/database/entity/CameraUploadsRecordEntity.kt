package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import mega.privacy.android.data.database.MegaDatabaseConstant
import mega.privacy.android.domain.entity.CameraUploadsRecordType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecordUploadStatus

/**
 * CameraUploadsRecord entity
 *
 * @property mediaId _id retrieved from the MediaStore,
 *                            also serve as the primary key in the table
 * @property timestamp timestamp representing the max between added date and modified date of the file,
 *                     also serve as the primary key in the table
 * @property folderType camera uploads folder type (Primary or Secondary),
 *                      also serve as the primary key in the table
 * @property fileName name of the file with the extension
 * @property filePath path of the file
 * @property fileType sync record type (Photo or Video)
 * @property uploadStatus upload status of the file. This value is not encrypted for queries performance
 * @property originalFingerprint original fingerprint, computed from the original file
 * @property generatedFingerprint generated fingerprint, null if unused, computed from the temp file
 * @property tempFilePath file path for the generated file, can be unused if the file does not need to be modified
 */
@Entity(
    tableName = MegaDatabaseConstant.TABLE_CAMERA_UPLOADS_RECORDS,
    primaryKeys = ["media_id", "timestamp", "folder_type"]
)
internal data class CameraUploadsRecordEntity(
    @ColumnInfo(name = "media_id") val mediaId: Long,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "folder_type") val folderType: CameraUploadFolderType,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "file_type") val fileType: CameraUploadsRecordType,
    @ColumnInfo(name = "upload_status") val uploadStatus: CameraUploadsRecordUploadStatus,
    @ColumnInfo(name = "original_fingerprint") val originalFingerprint: String,
    @ColumnInfo(name = "generated_fingerprint") val generatedFingerprint: String?,
    @ColumnInfo(name = "temp_file_path") val tempFilePath: String,
)

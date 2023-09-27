package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import mega.privacy.android.data.database.MegaDatabaseConstant
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecordUploadStatus

/**
 * CameraUploadsRecord entity
 *
 * @property encryptedMediaId encrypted _id retrieved from the MediaStore,
 *                            also serve as the primary key in the table
 * @property encryptedTimestamp encrypted timestamp representing the max between added date and modified date of the file,
 *                     also serve as the primary key in the table
 * @property folderType camera uploads folder type (Primary or Secondary),
 *                      also serve as the primary key in the table
 * @property encryptedFileName encrypted name of the file with the extension
 * @property encryptedFilePath encrypted path of the file
 * @property fileType sync record type (Photo or Video)
 * @property uploadStatus upload status of the file. This value is not encrypted for queries performance
 * @property encryptedOriginalFingerprint encrypted original fingerprint, computed from the original file
 * @property encryptedGeneratedFingerprint encrypted generated fingerprint, null if unused, computed from the temp file
 * @property encryptedTempFilePath encrypted file path for the generated file, can be unused if the file does not need to be modified
 */
@Entity(
    tableName = MegaDatabaseConstant.TABLE_CAMERA_UPLOADS_RECORDS,
    primaryKeys = ["media_id", "timestamp", "folder_type"]
)
internal data class CameraUploadsRecordEntity(
    @PrimaryKey
    @ColumnInfo(name = "media_id") val encryptedMediaId: String,
    @ColumnInfo(name = "timestamp") val encryptedTimestamp: String,
    @ColumnInfo(name = "folder_type") val folderType: CameraUploadFolderType,
    @ColumnInfo(name = "file_name") val encryptedFileName: String?,
    @ColumnInfo(name = "file_path") val encryptedFilePath: String?,
    @ColumnInfo(name = "file_type") val fileType: SyncRecordType,
    @ColumnInfo(name = "upload_status") val uploadStatus: CameraUploadsRecordUploadStatus,
    @ColumnInfo(name = "original_fingerprint") val encryptedOriginalFingerprint: String?,
    @ColumnInfo(name = "generated_fingerprint") val encryptedGeneratedFingerprint: String?,
    @ColumnInfo(name = "temp_file_path") val encryptedTempFilePath: String?,
)

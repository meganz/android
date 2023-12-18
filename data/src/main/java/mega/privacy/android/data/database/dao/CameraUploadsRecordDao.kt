package mega.privacy.android.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import mega.privacy.android.data.database.MegaDatabaseConstant
import mega.privacy.android.data.database.entity.CameraUploadsRecordEntity
import mega.privacy.android.domain.entity.CameraUploadsRecordType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecordUploadStatus

@Dao
internal interface CameraUploadsRecordDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrUpdateCameraUploadsRecords(entity: List<CameraUploadsRecordEntity>)

    @Query("SELECT * FROM ${MegaDatabaseConstant.TABLE_CAMERA_UPLOADS_RECORDS}")
    suspend fun getAllCameraUploadsRecords(): List<CameraUploadsRecordEntity>

    @Query("SELECT * FROM ${MegaDatabaseConstant.TABLE_CAMERA_UPLOADS_RECORDS} WHERE upload_status IN (:uploadStatus) AND file_type IN (:types) AND folder_type IN (:folderTypes)")
    suspend fun getCameraUploadsRecordsBy(
        uploadStatus: List<CameraUploadsRecordUploadStatus>,
        types: List<CameraUploadsRecordType>,
        folderTypes: List<CameraUploadFolderType>
    ): List<CameraUploadsRecordEntity>


    @Query("UPDATE ${MegaDatabaseConstant.TABLE_CAMERA_UPLOADS_RECORDS} SET upload_status = :uploadStatus WHERE media_id = :mediaId AND timestamp = :timestamp AND folder_type = :folderType")
    suspend fun updateCameraUploadsRecordUploadStatus(
        mediaId: String,
        timestamp: String,
        folderType: CameraUploadFolderType,
        uploadStatus: CameraUploadsRecordUploadStatus,
    )

    @Query("UPDATE ${MegaDatabaseConstant.TABLE_CAMERA_UPLOADS_RECORDS} SET generated_fingerprint = :generatedFingerprint WHERE media_id = :mediaId AND timestamp = :timestamp AND folder_type = :folderType")
    suspend fun updateCameraUploadsRecordGeneratedFingerprint(
        mediaId: String,
        timestamp: String,
        folderType: CameraUploadFolderType,
        generatedFingerprint: String,
    )

    @Query("DELETE FROM ${MegaDatabaseConstant.TABLE_CAMERA_UPLOADS_RECORDS} WHERE folder_type IN (:folderTypes)")
    suspend fun deleteCameraUploadsRecordsByFolderType(
        folderTypes: List<CameraUploadFolderType>,
    )
}

package mega.privacy.android.data.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.MegaDatabase
import mega.privacy.android.data.database.entity.CameraUploadsRecordEntity
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecordUploadStatus
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class CameraUploadsRecordDaoTest {
    private lateinit var cameraUploadsRecordDao: CameraUploadsRecordDao
    private lateinit var db: MegaDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, MegaDatabase::class.java
        ).build()
        cameraUploadsRecordDao = db.cameraUploadsRecordDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    private fun generateEntities() = (1..10).map {
        val entity = CameraUploadsRecordEntity(
            encryptedMediaId = "encryptedMediaId$it",
            encryptedTimestamp = "encryptedTimestamp$it",
            folderType = if (it % 2 == 0) CameraUploadFolderType.Primary else CameraUploadFolderType.Secondary,
            encryptedFileName = "encryptedFileName$it",
            encryptedFilePath = "encryptedFilePath$it",
            fileType = if (it < 6) SyncRecordType.TYPE_PHOTO else SyncRecordType.TYPE_VIDEO,
            uploadStatus = when {
                it < 3 -> CameraUploadsRecordUploadStatus.UPLOADED
                it < 7 -> CameraUploadsRecordUploadStatus.PENDING
                else -> CameraUploadsRecordUploadStatus.FAILED
            },
            encryptedOriginalFingerprint = "encryptedOriginalFingerprint$it",
            encryptedGeneratedFingerprint = "encryptedGeneratedFingerprint$it",
            encryptedTempFilePath = "encryptedTempFilePath$it",
        )
        entity
    }

    private suspend fun insertEntities(entities: List<CameraUploadsRecordEntity>) {
        cameraUploadsRecordDao.insertOrUpdateCameraUploadsRecords(entities)
    }


    @Test
    fun test_that_insertOrUpdateBackup_insert_the_corresponding_items() = runTest {
        val entities = generateEntities()
        val expected = entities.size

        insertEntities(entities)

        assertThat(cameraUploadsRecordDao.getAllCameraUploadsRecords().size)
            .isEqualTo(expected)
    }

    @Test
    fun test_that_insertOrUpdateBackup_update_the_corresponding_items() = runTest {
        val entities = generateEntities()
        val expected = entities.size

        insertEntities(entities)
        insertEntities(entities)

        assertThat(cameraUploadsRecordDao.getAllCameraUploadsRecords().size)
            .isEqualTo(expected)
    }

    @Test
    fun test_that_getCameraUploadsRecordByUploadStatusAndTypes_returns_the_corresponding_items() =
        runTest {
            val entities = generateEntities()
            val status = listOf(
                CameraUploadsRecordUploadStatus.PENDING,
                CameraUploadsRecordUploadStatus.FAILED,
            )
            val types = listOf(SyncRecordType.TYPE_PHOTO)
            val folderTypes = listOf(CameraUploadFolderType.Primary)
            val expected =
                entities.filter {
                    status.contains(it.uploadStatus)
                            && types.contains(it.fileType)
                            && folderTypes.contains(it.folderType)
                }.size

            insertEntities(entities)

            assertThat(
                cameraUploadsRecordDao
                    .getCameraUploadsRecordsBy(status, types, folderTypes).size,
            ).isEqualTo(expected)
        }

    @Test
    fun test_that_updateCameraUploadsRecordUploadStatus_update_the_status_of_the_corresponding_item() =
        runTest {
            val entities = generateEntities()
            val expected = CameraUploadsRecordUploadStatus.LOCAL_FILE_NOT_EXIST

            insertEntities(entities)

            val recordToUpdate = entities[0]
            cameraUploadsRecordDao.updateCameraUploadsRecordUploadStatus(
                recordToUpdate.encryptedMediaId,
                recordToUpdate.encryptedTimestamp,
                recordToUpdate.folderType,
                expected,
            )

            assertThat(
                cameraUploadsRecordDao.getAllCameraUploadsRecords().single {
                    it.encryptedMediaId == recordToUpdate.encryptedMediaId
                            && it.encryptedTimestamp == recordToUpdate.encryptedTimestamp
                            && it.folderType == recordToUpdate.folderType
                }.uploadStatus
            ).isEqualTo(expected)
        }

    @Test
    fun test_that_updateCameraUploadsRecordGeneratedFingerprint_update_the_generated_fingerprint_of_the_corresponding_item() =
        runTest {
            val entities = generateEntities()
            val expected = "generatedFingerprint"

            insertEntities(entities)

            val recordToUpdate = entities[0]
            cameraUploadsRecordDao.updateCameraUploadsRecordGeneratedFingerprint(
                recordToUpdate.encryptedMediaId,
                recordToUpdate.encryptedTimestamp,
                recordToUpdate.folderType,
                expected,
            )

            assertThat(
                cameraUploadsRecordDao.getAllCameraUploadsRecords().single {
                    it.encryptedMediaId == recordToUpdate.encryptedMediaId
                            && it.encryptedTimestamp == recordToUpdate.encryptedTimestamp
                            && it.folderType == recordToUpdate.folderType
                }.encryptedGeneratedFingerprint
            ).isEqualTo(expected)
        }

    @Test
    fun test_that_deleteCameraUploadsRecordsByFolderType_delete_all_the_corresponding_items() =
        runTest {
            val entities = generateEntities()
            val folderTypes = listOf(
                CameraUploadFolderType.Primary,
            )
            val expected = entities.filter { !folderTypes.contains(it.folderType) }.size

            insertEntities(entities)

            cameraUploadsRecordDao.deleteCameraUploadsRecordsByFolderType(folderTypes)

            assertThat(cameraUploadsRecordDao.getAllCameraUploadsRecords().size).isEqualTo(expected)
        }

    @Test
    fun test_that_getAllCameraUploadsRecords_returns_all_the_corresponding_items() =
        runTest {
            val expected = generateEntities()
            insertEntities(expected)
            assertThat(cameraUploadsRecordDao.getAllCameraUploadsRecords()).isEqualTo(expected)
        }
}

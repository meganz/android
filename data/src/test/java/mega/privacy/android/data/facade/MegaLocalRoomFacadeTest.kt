package mega.privacy.android.data.facade

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.cryptography.EncryptData
import mega.privacy.android.data.database.dao.ActiveTransferDao
import mega.privacy.android.data.database.dao.BackupDao
import mega.privacy.android.data.database.dao.CameraUploadsRecordDao
import mega.privacy.android.data.database.dao.ChatPendingChangesDao
import mega.privacy.android.data.database.dao.CompletedTransferDao
import mega.privacy.android.data.database.dao.ContactDao
import mega.privacy.android.data.database.dao.OfflineDao
import mega.privacy.android.data.database.dao.SdTransferDao
import mega.privacy.android.data.database.entity.ActiveTransferEntity
import mega.privacy.android.data.database.entity.BackupEntity
import mega.privacy.android.data.database.entity.CameraUploadsRecordEntity
import mega.privacy.android.data.database.entity.ChatPendingChangesEntity
import mega.privacy.android.data.database.entity.CompletedTransferEntity
import mega.privacy.android.data.database.entity.CompletedTransferEntityLegacy
import mega.privacy.android.data.database.entity.SdTransferEntity
import mega.privacy.android.data.facade.MegaLocalRoomFacade.Companion.MAX_INSERT_LIST_SIZE
import mega.privacy.android.data.mapper.backup.BackupEntityMapper
import mega.privacy.android.data.mapper.backup.BackupInfoTypeIntMapper
import mega.privacy.android.data.mapper.backup.BackupModelMapper
import mega.privacy.android.data.mapper.camerauploads.CameraUploadsRecordEntityMapper
import mega.privacy.android.data.mapper.camerauploads.CameraUploadsRecordModelMapper
import mega.privacy.android.data.mapper.chat.ChatRoomPendingChangesEntityMapper
import mega.privacy.android.data.mapper.chat.ChatRoomPendingChangesModelMapper
import mega.privacy.android.data.mapper.contact.ContactEntityMapper
import mega.privacy.android.data.mapper.contact.ContactModelMapper
import mega.privacy.android.data.mapper.offline.OfflineEntityMapper
import mega.privacy.android.data.mapper.offline.OfflineModelMapper
import mega.privacy.android.data.mapper.transfer.active.ActiveTransferEntityMapper
import mega.privacy.android.data.mapper.transfer.completed.CompletedTransferEntityMapper
import mega.privacy.android.data.mapper.transfer.completed.CompletedTransferLegacyModelMapper
import mega.privacy.android.data.mapper.transfer.completed.CompletedTransferModelMapper
import mega.privacy.android.data.mapper.transfer.sd.SdTransferEntityMapper
import mega.privacy.android.data.mapper.transfer.sd.SdTransferModelMapper
import mega.privacy.android.domain.entity.CameraUploadsRecordType
import mega.privacy.android.domain.entity.SdTransfer
import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecordUploadStatus
import mega.privacy.android.domain.entity.chat.ChatPendingChanges
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.Transfer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MegaLocalRoomFacadeTest {

    private lateinit var underTest: MegaLocalRoomFacade

    private val contactDao = mock<ContactDao>()
    private val contactEntityMapper = mock<ContactEntityMapper>()
    private val contactModelMapper = mock<ContactModelMapper>()
    private val completedTransferDao = mock<CompletedTransferDao>()
    private val completedTransferModelMapper = mock<CompletedTransferModelMapper>()
    private val encryptData = mock<EncryptData>()
    private val decryptData = mock<DecryptData>()
    private val activeTransferDao = mock<ActiveTransferDao>()
    private val activeTransferEntityMapper = mock<ActiveTransferEntityMapper>()
    private val completedTransferEntityMapper: CompletedTransferEntityMapper = mock()
    private val sdTransferDao: SdTransferDao = mock()
    private val sdTransferEntityMapper = mock<SdTransferEntityMapper>()
    private val sdTransferModelMapper = mock<SdTransferModelMapper>()
    private val backupDao = mock<BackupDao>()
    private val backupEntityMapper = mock<BackupEntityMapper>()
    private val backupModelMapper = mock<BackupModelMapper>()
    private val backupInfoTypeIntMapper = mock<BackupInfoTypeIntMapper>()
    private val offlineDao: OfflineDao = mock()
    private val offlineModelMapper: OfflineModelMapper = mock()
    private val offlineEntityMapper: OfflineEntityMapper = mock()
    private val cameraUploadsRecordDao: CameraUploadsRecordDao = mock()
    private val cameraUploadsRecordEntityMapper: CameraUploadsRecordEntityMapper = mock()
    private val cameraUploadsRecordModelMapper: CameraUploadsRecordModelMapper = mock()
    private val chatPendingChangesDao: ChatPendingChangesDao = mock()
    private val chatRoomPendingChangesEntityMapper: ChatRoomPendingChangesEntityMapper = mock()
    private val chatRoomPendingChangesModelMapper: ChatRoomPendingChangesModelMapper = mock()
    private val completedTransferLegacyModelMapper = mock<CompletedTransferLegacyModelMapper>()

    @BeforeAll
    fun setUp() {
        underTest = MegaLocalRoomFacade(
            contactDao = contactDao,
            contactEntityMapper = contactEntityMapper,
            contactModelMapper = contactModelMapper,
            completedTransferDao = completedTransferDao,
            activeTransferDao = activeTransferDao,
            completedTransferModelMapper = completedTransferModelMapper,
            activeTransferEntityMapper = activeTransferEntityMapper,
            encryptData = encryptData,
            decryptData = decryptData,
            completedTransferEntityMapper = completedTransferEntityMapper,
            completedTransferLegacyModelMapper = completedTransferLegacyModelMapper,
            sdTransferDao = sdTransferDao,
            sdTransferEntityMapper = sdTransferEntityMapper,
            sdTransferModelMapper = sdTransferModelMapper,
            backupDao = backupDao,
            backupEntityMapper = backupEntityMapper,
            backupModelMapper = backupModelMapper,
            backupInfoTypeIntMapper = backupInfoTypeIntMapper,
            offlineDao = offlineDao,
            offlineEntityMapper = offlineEntityMapper,
            offlineModelMapper = offlineModelMapper,
            cameraUploadsRecordDao = cameraUploadsRecordDao,
            cameraUploadsRecordEntityMapper = cameraUploadsRecordEntityMapper,
            cameraUploadsRecordModelMapper = cameraUploadsRecordModelMapper,
            chatPendingChangesDao = chatPendingChangesDao,
            chatRoomPendingChangesEntityMapper = chatRoomPendingChangesEntityMapper,
            chatRoomPendingChangesModelMapper = chatRoomPendingChangesModelMapper,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            contactDao,
            contactEntityMapper,
            contactModelMapper,
            completedTransferDao,
            completedTransferModelMapper,
            encryptData,
            activeTransferDao,
            activeTransferEntityMapper,
            backupDao,
            backupEntityMapper,
            backupModelMapper,
            backupInfoTypeIntMapper,
            cameraUploadsRecordDao,
            cameraUploadsRecordEntityMapper,
            cameraUploadsRecordModelMapper,
            chatPendingChangesDao,
            chatRoomPendingChangesEntityMapper,
            chatRoomPendingChangesModelMapper,
            completedTransferLegacyModelMapper,
        )
    }

    @Test
    fun `test that getAllCompletedTransfers returns a list of completed transfers ordered by timestamp descendant`() =
        runTest {
            val completedTransferEntities = listOf<CompletedTransferEntity>(
                mock(), mock(), mock(),
            )
            val completedTransfers = listOf<CompletedTransfer>(
                mock { on { timestamp }.thenReturn(1684228012974) },
                mock { on { timestamp }.thenReturn(1684228012975) },
                mock { on { timestamp }.thenReturn(1684228012973) },
            )

            whenever(completedTransferDao.getAllCompletedTransfers())
                .thenReturn(flowOf(completedTransferEntities))
            completedTransferEntities.forEachIndexed { index, completedTransferEntity ->
                whenever(completedTransferModelMapper(completedTransferEntity)).thenReturn(
                    completedTransfers[index]
                )
            }

            val expected =
                listOf(completedTransfers[1], completedTransfers[0], completedTransfers[2])

            assertThat(underTest.getCompletedTransfers().single()).isEqualTo(expected)
        }


    @Test
    fun `test that getAllCompletedTransfers returns a list of completed transfers with size max elements`() =
        runTest {
            val expectedSize = 2
            val completedTransferEntities = listOf<CompletedTransferEntity>(
                mock(), mock(), mock(), mock()
            )

            whenever(completedTransferDao.getAllCompletedTransfers()).thenReturn(
                flowOf(completedTransferEntities)
            )
            completedTransferEntities.forEach { entity ->
                val completedTransfer = mock<CompletedTransfer> {
                    on { timestamp }.thenReturn(1684228012974)
                }
                whenever(completedTransferModelMapper(entity)).thenReturn(completedTransfer)
            }

            assertThat(underTest.getCompletedTransfers(expectedSize).single().size)
                .isEqualTo(expectedSize)
        }

    @Test
    fun `test that getAllCompletedTransfers returns all completed transfers if the size parameter is null`() =
        runTest {
            val completedTransferEntities = listOf<CompletedTransferEntity>(
                mock(), mock(), mock(), mock()
            )

            whenever(completedTransferDao.getAllCompletedTransfers()).thenReturn(
                flowOf(completedTransferEntities)
            )
            completedTransferEntities.forEach { entity ->
                val completedTransfer = mock<CompletedTransfer> {
                    on { timestamp }.thenReturn(1684228012974)
                }
                whenever(completedTransferModelMapper(entity)).thenReturn(completedTransfer)
            }

            assertThat(underTest.getCompletedTransfers().single().size)
                .isEqualTo(completedTransferEntities.size)
        }

    @Test
    fun `test that insertSdTransfer invokes correctly when call insertSdTransfer`() = runTest {
        val sdTransferEntity = mock<SdTransferEntity>()
        val sdTransferModel = mock<SdTransfer>()
        whenever(sdTransferEntityMapper(sdTransferModel)).thenReturn(sdTransferEntity)
        underTest.insertSdTransfer(sdTransferModel)
        verify(sdTransferDao).insertSdTransfer(sdTransferEntity)
    }

    @Test
    fun `test that deleteSdTransferByTag invokes correctly when call deleteSdTransferByTag`() =
        runTest {
            val tag = 1
            underTest.deleteSdTransferByTag(tag)
            verify(sdTransferDao).deleteSdTransferByTag(tag)
        }

    @Test
    fun `test that getCompletedTransferById returns correctly when call getCompletedTransferById`() =
        runTest {
            val id = 1
            val completedTransferEntity = mock<CompletedTransferEntity>()
            val completedTransferModel = mock<CompletedTransfer>()
            whenever(completedTransferModelMapper(completedTransferEntity)).thenReturn(
                completedTransferModel
            )
            whenever(completedTransferDao.getCompletedTransferById(id)).thenReturn(
                completedTransferEntity
            )
            assertThat(underTest.getCompletedTransferById(id)).isEqualTo(completedTransferModel)
        }

    @Test
    fun `test that backupDao delete is invoked with the proper backup when deleteBackupById is invoked`() =
        runTest {
            val id = 1L
            whenever(encryptData(id.toString())).thenReturn(id.toString())
            underTest.deleteBackupById(id)
            verify(backupDao).deleteBackupByBackupId(id.toString())
        }

    @Test
    fun `test that backup is outdated when setBackupAsOutdated is invoked`() =
        runTest {
            val id = 1L
            val outdatedString = "true"
            whenever(encryptData(id.toString())).thenReturn(id.toString())
            whenever(encryptData(outdatedString)).thenReturn(outdatedString)
            underTest.setBackupAsOutdated(id)
            verify(backupDao).updateBackupAsOutdated(id.toString(), outdatedString)
        }

    @Test
    fun `test that backup is saved when saveBackup is invoked`() =
        runTest {
            val backup = mock<Backup>()
            val entity = mock<BackupEntity>()
            whenever(backupEntityMapper(backup)).thenReturn(entity)
            underTest.saveBackup(backup)
            verify(backupDao).insertOrUpdateBackup(entity)
        }

    @Test
    fun `test that camera upload backup is returned when getCuBackUp is invoked`() =
        runTest {
            val entities = listOf<BackupEntity>(mock(), mock())
            val backup = mock<Backup>()
            val falseString = "false"
            whenever(backupInfoTypeIntMapper(BackupInfoType.CAMERA_UPLOADS)).thenReturn(
                BackupInfoType.CAMERA_UPLOADS.ordinal
            )
            whenever(encryptData(falseString)).thenReturn(falseString)
            whenever(
                backupDao.getBackupByType(
                    backupType = BackupInfoType.CAMERA_UPLOADS.ordinal,
                    encryptedIsOutdated = falseString
                )
            ).thenReturn(entities)
            whenever(backupModelMapper(entities.last())).thenReturn(backup)
            val actual = underTest.getCuBackUp()
            assertThat(actual).isEqualTo(backup)
        }

    @Test
    fun `test that media upload backup is returned when getMuBackUp is invoked`() =
        runTest {
            val entities = listOf<BackupEntity>(mock(), mock())
            val backup = mock<Backup>()
            val falseString = "false"
            whenever(backupInfoTypeIntMapper(BackupInfoType.MEDIA_UPLOADS)).thenReturn(
                BackupInfoType.MEDIA_UPLOADS.ordinal
            )
            whenever(encryptData(falseString)).thenReturn(falseString)
            whenever(
                backupDao.getBackupByType(
                    backupType = BackupInfoType.MEDIA_UPLOADS.ordinal,
                    encryptedIsOutdated = falseString
                )
            ).thenReturn(entities)
            whenever(backupModelMapper(entities.last())).thenReturn(backup)
            val actual = underTest.getMuBackUp()
            assertThat(actual).isEqualTo(backup)
        }

    @Test
    fun `test that camera upload backup id is returned when getCuBackUpId is invoked`() =
        runTest {
            val ids = listOf("1", "2")
            val falseString = "false"
            whenever(backupInfoTypeIntMapper(BackupInfoType.CAMERA_UPLOADS)).thenReturn(
                BackupInfoType.CAMERA_UPLOADS.ordinal
            )
            whenever(encryptData(falseString)).thenReturn(falseString)
            whenever(
                backupDao.getBackupIdByType(
                    backupType = BackupInfoType.CAMERA_UPLOADS.ordinal,
                    encryptedIsOutdated = falseString
                )
            ).thenReturn(ids)
            whenever(decryptData(ids.last())).thenReturn(ids.last())
            val actual = underTest.getCuBackUpId()
            assertThat(actual).isEqualTo(ids.last().toLong())
        }

    @Test
    fun `test that media upload backup id is returned when getMuBackUpId is invoked`() =
        runTest {
            val ids = listOf("1", "2")
            val falseString = "false"
            whenever(backupInfoTypeIntMapper(BackupInfoType.MEDIA_UPLOADS)).thenReturn(
                BackupInfoType.MEDIA_UPLOADS.ordinal
            )
            whenever(encryptData(falseString)).thenReturn(falseString)
            whenever(
                backupDao.getBackupIdByType(
                    backupType = BackupInfoType.MEDIA_UPLOADS.ordinal,
                    encryptedIsOutdated = falseString
                )
            ).thenReturn(ids)
            whenever(decryptData(ids.last())).thenReturn(ids.last())
            val actual = underTest.getMuBackUpId()
            assertThat(actual).isEqualTo(ids.last().toLong())
        }

    @Test
    fun `test that backup is returned when getBackupById is invoked`() =
        runTest {
            val id = 1L
            val backup = mock<Backup>()
            val entity = mock<BackupEntity>()
            whenever(encryptData(id.toString())).thenReturn(id.toString())
            whenever(backupModelMapper(entity)).thenReturn(backup)
            whenever(backupDao.getBackupById(id.toString())).thenReturn(entity)
            val actual = underTest.getBackupById(id)
            assertThat(actual).isEqualTo(backup)
        }

    @Test
    fun `test that backup is updated when updateBackup is invoked`() =
        runTest {
            val id = 1L
            val backup = mock<Backup>()
            val entity = mock<BackupEntity>()
            whenever(encryptData(id.toString())).thenReturn(id.toString())
            whenever(backupEntityMapper(backup)).thenReturn(entity)
            underTest.updateBackup(backup)
            verify(backupDao).insertOrUpdateBackup(entity)
        }

    @Test
    fun `test that all backupDao deleteAllBackups is invoked when deleteAllBackups is invoked`() =
        runTest {
            underTest.deleteAllBackups()
            verify(backupDao).deleteAllBackups()
        }

    @Test
    fun `test that delete entities correctly when deleteOldestCompletedTransfers is called`() =
        runTest {
            val completedTransfers = (1..110).map { id ->
                CompletedTransferEntity(
                    id = id,
                    fileName = "2023-03-24 00.13.20_1.jpg",
                    type = 1,
                    state = 6,
                    size = "3.57 MB",
                    handle = 27169983390750L,
                    path = "Cloud drive/Camera uploads",
                    isOffline = false,
                    timestamp = System.nanoTime(),
                    error = "No error",
                    originalPath = "/data/user/0/mega.privacy.android.app/cache/cu/53132573053997.2023-03-24 00.13.20_1.jpg",
                    parentHandle = 11622336899311L,
                    appData = "appData",
                )
            }
            completedTransfers.forEach { entity ->
                whenever(completedTransferModelMapper(entity)).thenReturn(
                    CompletedTransfer(
                        id = entity.id,
                        fileName = entity.fileName,
                        type = entity.type,
                        state = entity.state,
                        size = entity.size,
                        handle = entity.handle,
                        path = entity.path,
                        isOffline = entity.isOffline,
                        timestamp = entity.timestamp,
                        error = entity.error,
                        originalPath = entity.originalPath,
                        parentHandle = entity.parentHandle,
                        appData = entity.appData,
                    )
                )
            }
            val deletedTransfers = completedTransfers.take(10)
            whenever(completedTransferDao.getCompletedTransfersCount()) doReturn completedTransfers.size
            whenever(completedTransferDao.getAllCompletedTransfers()) doReturn flowOf(
                completedTransfers
            )
            underTest.deleteOldestCompletedTransfers()
            verify(completedTransferDao).deleteCompletedTransferByIds(deletedTransfers.mapNotNull { it.id }
                .sortedDescending(), MAX_INSERT_LIST_SIZE)
        }

    @Test
    fun `test that insertOrUpdateCameraUploadsRecords insert or update in database`() =
        runTest {
            val records = listOf<CameraUploadsRecord>(mock())
            val entities = listOf<CameraUploadsRecordEntity>(mock())
            records.mapIndexed { index, record ->
                whenever(cameraUploadsRecordEntityMapper(record)).thenReturn(entities[index])
            }

            underTest.insertOrUpdateCameraUploadsRecords(records)

            verify(cameraUploadsRecordDao).insertOrUpdateCameraUploadsRecords(entities)
        }

    @Test
    fun `test that getCameraUploadsRecordByUploadStatusAndTypes returns the corresponding items`() =
        runTest {
            val entities = listOf<CameraUploadsRecordEntity>(mock())
            val expected = listOf<CameraUploadsRecord>(mock())
            entities.mapIndexed { index, entity ->
                whenever(cameraUploadsRecordModelMapper(entity)).thenReturn(expected[index])
            }
            val status = listOf<CameraUploadsRecordUploadStatus>(mock())
            val types = listOf<CameraUploadsRecordType>(mock())
            val folderTypes = listOf<CameraUploadFolderType>(mock())
            whenever(
                cameraUploadsRecordDao.getCameraUploadsRecordsBy(status, types, folderTypes)
            ).thenReturn(entities)

            assertThat(underTest.getCameraUploadsRecordsBy(status, types, folderTypes))
                .isEqualTo(expected)
        }

    @Test
    fun `test that updateCameraUploadsRecordUploadStatus update the upload status of the corresponding item`() =
        runTest {
            val mediaId = 1234L
            val timestamp = 5678L
            val folderType = CameraUploadFolderType.Primary
            val uploadStatus = CameraUploadsRecordUploadStatus.LOCAL_FILE_NOT_EXIST

            underTest.updateCameraUploadsRecordUploadStatus(
                mediaId,
                timestamp,
                folderType,
                uploadStatus,
            )

            verify(cameraUploadsRecordDao).updateCameraUploadsRecordUploadStatus(
                mediaId,
                timestamp,
                folderType,
                uploadStatus,
            )
        }

    @Test
    fun `test that setCameraUploadsRecordGeneratedFingerprint set the upload status of the corresponding item`() =
        runTest {
            val mediaId = 1234L
            val timestamp = 5678L
            val folderType = CameraUploadFolderType.Primary
            val generatedFingerprint = "generatedFingerprint"

            underTest.setCameraUploadsRecordGeneratedFingerprint(
                mediaId,
                timestamp,
                folderType,
                generatedFingerprint,
            )
            verify(cameraUploadsRecordDao).updateCameraUploadsRecordGeneratedFingerprint(
                mediaId,
                timestamp,
                folderType,
                generatedFingerprint,
            )
        }

    @Test
    fun `test that deleteCameraUploadsRecords deletes the corresponding items`() =
        runTest {
            val folderType = listOf(CameraUploadFolderType.Primary)

            underTest.deleteCameraUploadsRecords(folderType)

            verify(cameraUploadsRecordDao).deleteCameraUploadsRecordsByFolderType(folderType)
        }

    @Test
    fun `test that getAllCameraUploadsRecords returns the corresponding items`() =
        runTest {
            val entities = listOf<CameraUploadsRecordEntity>(mock())
            val expected = listOf<CameraUploadsRecord>(mock())
            entities.mapIndexed { index, entity ->
                whenever(cameraUploadsRecordModelMapper(entity)).thenReturn(expected[index])
            }
            whenever(
                cameraUploadsRecordDao.getAllCameraUploadsRecords()
            ).thenReturn(entities)

            assertThat(underTest.getAllCameraUploadsRecords()).isEqualTo(expected)
        }

    @Test
    fun `test that setChatRoomPreference invokes correctly when call setChatRoomPreference`() =
        runTest {
            val chatPendingChangesEntity = mock<ChatPendingChangesEntity>()
            val chatPendingChangesModel = mock<ChatPendingChanges>()
            whenever(chatRoomPendingChangesEntityMapper(chatPendingChangesModel)).thenReturn(
                chatPendingChangesEntity
            )
            underTest.setChatPendingChanges(chatPendingChangesModel)
            verify(chatPendingChangesDao).upsertChatPendingChanges(chatPendingChangesEntity)
        }

    @Test
    fun `test that getChatRoomPreference returns correctly when call getChatRoomPreference`() =
        runTest {
            val chatId = 1L
            val chatPendingChangesEntity = mock<ChatPendingChangesEntity>()
            val chatPendingChangesModel = mock<ChatPendingChanges>()
            whenever(chatPendingChangesDao.getChatPendingChanges(chatId)).thenReturn(
                flowOf(
                    chatPendingChangesEntity
                )
            )
            whenever(chatRoomPendingChangesModelMapper(chatPendingChangesEntity)).thenReturn(
                chatPendingChangesModel
            )
            underTest.monitorChatPendingChanges(chatId).test {
                assertThat(awaitItem()).isEqualTo(chatPendingChangesModel)
                awaitComplete()
            }
        }

    @Test
    fun `test that getSdTransferByTag returns mapped transfer from dao`() = runTest {
        val tag = 1
        val sdTransferEntity = mock<SdTransferEntity>()
        whenever(sdTransferDao.getSdTransferByTag(tag)) doReturn sdTransferEntity
        val expected = mock<SdTransfer>()
        whenever(sdTransferModelMapper(sdTransferEntity)) doReturn expected

        val actual = underTest.getSdTransferByTag(tag)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that addCompletedTransfers insert the mapped entities`() = runTest {
        val completedTransfers = (0..10).map {
            mock<CompletedTransfer>()
        }
        val roomEntitiesMap = completedTransfers.associateWith { mock<CompletedTransferEntity>() }
        completedTransfers.forEach {
            whenever(completedTransferEntityMapper(it)) doReturn roomEntitiesMap.getValue(it)
        }
        val expected = roomEntitiesMap.values.toList()

        underTest.addCompletedTransfers(completedTransfers)

        verify(completedTransferDao).insertOrUpdateCompletedTransfers(
            expected,
            MAX_INSERT_LIST_SIZE
        )
    }

    @Test
    fun `test that insertOrUpdateActiveTransfers insert the mapped entities`() = runTest {
        val activeTransfers = (0..10).map {
            mock<Transfer>()
        }
        val roomEntitiesMap = activeTransfers.associateWith { mock<ActiveTransferEntity>() }
        activeTransfers.forEach {
            whenever(activeTransferEntityMapper(it)) doReturn roomEntitiesMap.getValue(it)
        }
        val expected = roomEntitiesMap.values.toList()

        underTest.insertOrUpdateActiveTransfers(activeTransfers)

        verify(activeTransferDao).insertOrUpdateActiveTransfers(expected, MAX_INSERT_LIST_SIZE)
    }

    @Test
    fun `test that migrateLegacyCompletedTransfers get legacy transfers from legacy table and inserts the mapped entities to new table`() =
        runTest {
            val expected = mutableListOf<CompletedTransferEntity>()
            val legacyTransfers = (0..10).map {
                mock<CompletedTransferEntityLegacy>()
            }
            whenever(completedTransferDao.getAllLegacyCompletedTransfers()) doReturn legacyTransfers
            whenever(completedTransferLegacyModelMapper(any())) doReturn mock()
            whenever(completedTransferEntityMapper(any())) doAnswer {
                mock<CompletedTransferEntity>().also { expected.add(it) }
            }

            underTest.migrateLegacyCompletedTransfers()

            assertThat(expected).hasSize(legacyTransfers.size)
            verify(completedTransferDao).insertOrUpdateCompletedTransfers(eq(expected), any())
        }

    @Test
    fun `test that migrateLegacyCompletedTransfers migrates first 100 entities when there are more than 100 legacy entities`() =
        runTest {
            val expected = mutableListOf<CompletedTransferEntity>()
            val legacyTransfers = (0..200).map {
                mock<CompletedTransferEntityLegacy>()
            }
            whenever(completedTransferDao.getAllLegacyCompletedTransfers()) doReturn legacyTransfers
            whenever(completedTransferLegacyModelMapper(any())) doReturn mock()
            whenever(completedTransferEntityMapper(any())) doAnswer {
                mock<CompletedTransferEntity> {
                    on { it.timestamp } doReturn expected.size.toLong()
                }.also { expected.add(it) }
            }

            underTest.migrateLegacyCompletedTransfers()

            assertThat(expected).hasSize(100)
            verify(completedTransferDao).insertOrUpdateCompletedTransfers(
                eq(expected),
                any()
            )
        }

    @Test
    fun `test that deleteAllLegacyCompletedTransfers is invoked when migrateLegacyCompletedTransfers is invoked and there are legacy entities`() =
        runTest {
            val legacyTransfers = (0..10).map {
                mock<CompletedTransferEntityLegacy>()
            }
            whenever(completedTransferDao.getAllLegacyCompletedTransfers()) doReturn legacyTransfers
            whenever(completedTransferLegacyModelMapper(any())) doReturn mock()
            whenever(completedTransferEntityMapper(any())) doReturn mock()

            underTest.migrateLegacyCompletedTransfers()

            verify(completedTransferDao).deleteAllLegacyCompletedTransfers()
        }

    @Test
    fun `test that deleteAllLegacyCompletedTransfers is not invoked when migrateLegacyCompletedTransfers is invoked and there are no legacy entities`() =
        runTest {
            whenever(completedTransferDao.getAllLegacyCompletedTransfers()) doReturn emptyList()
            whenever(completedTransferLegacyModelMapper(any())) doReturn mock()
            whenever(completedTransferEntityMapper(any())) doReturn mock()

            underTest.migrateLegacyCompletedTransfers()

            verify(completedTransferDao, never()).deleteAllLegacyCompletedTransfers()
        }
}

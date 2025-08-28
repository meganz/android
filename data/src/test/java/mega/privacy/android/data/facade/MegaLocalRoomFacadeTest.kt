package mega.privacy.android.data.facade

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.cryptography.EncryptData
import mega.privacy.android.data.database.dao.ActiveTransferDao
import mega.privacy.android.data.database.dao.ActiveTransferGroupDao
import mega.privacy.android.data.database.dao.BackupDao
import mega.privacy.android.data.database.dao.CameraUploadsRecordDao
import mega.privacy.android.data.database.dao.ChatPendingChangesDao
import mega.privacy.android.data.database.dao.CompletedTransferDao
import mega.privacy.android.data.database.dao.ContactDao
import mega.privacy.android.data.database.dao.LastPageViewedInPdfDao
import mega.privacy.android.data.database.dao.MediaPlaybackInfoDao
import mega.privacy.android.data.database.dao.OfflineDao
import mega.privacy.android.data.database.dao.PendingTransferDao
import mega.privacy.android.data.database.dao.VideoRecentlyWatchedDao
import mega.privacy.android.data.database.entity.ActiveTransferActionGroupEntity
import mega.privacy.android.data.database.entity.ActiveTransferEntity
import mega.privacy.android.data.database.entity.BackupEntity
import mega.privacy.android.data.database.entity.CameraUploadsRecordEntity
import mega.privacy.android.data.database.entity.ChatPendingChangesEntity
import mega.privacy.android.data.database.entity.CompletedTransferEntity
import mega.privacy.android.data.database.entity.CompletedTransferEntityLegacy
import mega.privacy.android.data.database.entity.LastPageViewedInPdfEntity
import mega.privacy.android.data.database.entity.MediaPlaybackInfoEntity
import mega.privacy.android.data.database.entity.PendingTransferEntity
import mega.privacy.android.data.database.entity.VideoRecentlyWatchedEntity
import mega.privacy.android.data.facade.MegaLocalRoomFacade.Companion.MAX_COMPLETED_TRANSFER_ROWS
import mega.privacy.android.data.facade.MegaLocalRoomFacade.Companion.MAX_INSERT_LIST_SIZE
import mega.privacy.android.data.mapper.MediaPlaybackInfoEntityMapper
import mega.privacy.android.data.mapper.MediaPlaybackInfoMapper
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
import mega.privacy.android.data.mapper.pdf.LastPageViewedInPdfEntityMapper
import mega.privacy.android.data.mapper.pdf.LastPageViewedInPdfModelMapper
import mega.privacy.android.data.mapper.transfer.TransferStateIntMapper
import mega.privacy.android.data.mapper.transfer.active.ActiveTransferEntityMapper
import mega.privacy.android.data.mapper.transfer.active.ActiveTransferGroupEntityMapper
import mega.privacy.android.data.mapper.transfer.completed.CompletedTransferEntityMapper
import mega.privacy.android.data.mapper.transfer.completed.CompletedTransferLegacyModelMapper
import mega.privacy.android.data.mapper.transfer.completed.CompletedTransferModelMapper
import mega.privacy.android.data.mapper.transfer.pending.InsertPendingTransferRequestMapper
import mega.privacy.android.data.mapper.transfer.pending.PendingTransferModelMapper
import mega.privacy.android.data.mapper.videosection.VideoRecentlyWatchedEntityMapper
import mega.privacy.android.data.mapper.videosection.VideoRecentlyWatchedItemMapper
import mega.privacy.android.data.model.VideoRecentlyWatchedItem
import mega.privacy.android.domain.entity.CameraUploadsRecordType
import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecordUploadStatus
import mega.privacy.android.domain.entity.chat.ChatPendingChanges
import mega.privacy.android.domain.entity.mediaplayer.MediaPlaybackInfo
import mega.privacy.android.domain.entity.mediaplayer.MediaType
import mega.privacy.android.domain.entity.pdf.LastPageViewedInPdf
import mega.privacy.android.domain.entity.transfer.ActiveTransferActionGroup
import mega.privacy.android.domain.entity.transfer.ActiveTransferActionGroupImpl
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.InsertPendingTransferRequest
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferState
import mega.privacy.android.domain.entity.transfer.pending.UpdateAlreadyTransferredFilesCount
import mega.privacy.android.domain.entity.transfer.pending.UpdatePendingTransferState
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
    private val videoRecentlyWatchedDao: VideoRecentlyWatchedDao = mock()
    private val videoRecentlyWatchedEntityMapper: VideoRecentlyWatchedEntityMapper = mock()
    private val videoRecentlyWatchedItemMapper: VideoRecentlyWatchedItemMapper = mock()
    private val pendingTransferDao = mock<PendingTransferDao>()
    private val pendingTransferModelMapper = mock<PendingTransferModelMapper>()
    private val insertPendingTransferRequestMapper = mock<InsertPendingTransferRequestMapper>()
    private val activeTransferGroupDao = mock<ActiveTransferGroupDao>()
    private val activeTransferGroupEntityMapper = mock<ActiveTransferGroupEntityMapper>()
    private val lastPageViewedInPdfDao = mock<LastPageViewedInPdfDao>()
    private val lastPageViewedInPdfEntityMapper = mock<LastPageViewedInPdfEntityMapper>()
    private val lastPageViewedInPdfModelMapper = mock<LastPageViewedInPdfModelMapper>()
    private val mediaPlaybackInfoDao: MediaPlaybackInfoDao = mock()
    private val mediaPlaybackInfoEntityMapper: MediaPlaybackInfoEntityMapper = mock()
    private val mediaPlaybackInfoMapper: MediaPlaybackInfoMapper = mock()
    private val transferStateIntMapper = mock<TransferStateIntMapper>()

    @BeforeAll
    fun setUp() {
        underTest = MegaLocalRoomFacade(
            contactDao = { contactDao },
            contactEntityMapper = contactEntityMapper,
            contactModelMapper = contactModelMapper,
            completedTransferDao = { completedTransferDao },
            activeTransferDao = { activeTransferDao },
            completedTransferModelMapper = completedTransferModelMapper,
            activeTransferEntityMapper = activeTransferEntityMapper,
            encryptData = encryptData,
            decryptData = decryptData,
            completedTransferEntityMapper = completedTransferEntityMapper,
            completedTransferLegacyModelMapper = completedTransferLegacyModelMapper,
            backupDao = { backupDao },
            backupEntityMapper = backupEntityMapper,
            backupModelMapper = backupModelMapper,
            backupInfoTypeIntMapper = backupInfoTypeIntMapper,
            offlineDao = { offlineDao },
            offlineEntityMapper = offlineEntityMapper,
            offlineModelMapper = offlineModelMapper,
            cameraUploadsRecordDao = { cameraUploadsRecordDao },
            cameraUploadsRecordEntityMapper = cameraUploadsRecordEntityMapper,
            cameraUploadsRecordModelMapper = cameraUploadsRecordModelMapper,
            chatPendingChangesDao = { chatPendingChangesDao },
            chatRoomPendingChangesEntityMapper = chatRoomPendingChangesEntityMapper,
            chatRoomPendingChangesModelMapper = chatRoomPendingChangesModelMapper,
            videoRecentlyWatchedDao = { videoRecentlyWatchedDao },
            videoRecentlyWatchedItemMapper = videoRecentlyWatchedItemMapper,
            videoRecentlyWatchedEntityMapper = videoRecentlyWatchedEntityMapper,
            pendingTransferDao = { pendingTransferDao },
            pendingTransferModelMapper = pendingTransferModelMapper,
            insertPendingTransferRequestMapper = insertPendingTransferRequestMapper,
            activeTransferGroupDao = { activeTransferGroupDao },
            activeTransferGroupEntityMapper = activeTransferGroupEntityMapper,
            lastPageViewedInPdfDao = { lastPageViewedInPdfDao },
            lastPageViewedInPdfEntityMapper = lastPageViewedInPdfEntityMapper,
            lastPageViewedInPdfModelMapper = lastPageViewedInPdfModelMapper,
            mediaPlaybackInfoDao = { mediaPlaybackInfoDao },
            mediaPlaybackInfoEntityMapper = mediaPlaybackInfoEntityMapper,
            mediaPlaybackInfoMapper = mediaPlaybackInfoMapper,
            transferStateIntMapper = transferStateIntMapper,
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
            videoRecentlyWatchedDao,
            videoRecentlyWatchedItemMapper,
            videoRecentlyWatchedEntityMapper,
            pendingTransferDao,
            pendingTransferModelMapper,
            insertPendingTransferRequestMapper,
            activeTransferGroupDao,
            activeTransferGroupEntityMapper,
            mediaPlaybackInfoDao,
            mediaPlaybackInfoMapper,
            mediaPlaybackInfoEntityMapper,
            transferStateIntMapper,
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
    fun `test that deleteCompletedTransfersById invokes correctly`() =
        runTest {
            val ids = listOf(1, 2, 3)

            underTest.deleteCompletedTransfersById(ids)
            verify(completedTransferDao).deleteCompletedTransferByIds(ids, MAX_INSERT_LIST_SIZE)
        }

    @Test
    fun `test that delete entities correctly when deleteOldestCompletedTransfers is called`() =
        runTest {
            listOf(
                TransferState.STATE_COMPLETED,
                TransferState.STATE_CANCELLED,
                TransferState.STATE_FAILED
            ).forEach {
                whenever(transferStateIntMapper(it)) doReturn it.ordinal
            }
            whenever(completedTransferDao.getCompletedTransfersCount()) doReturn MAX_COMPLETED_TRANSFER_ROWS + 10

            underTest.deleteOldestCompletedTransfers()
            listOf(
                TransferState.STATE_COMPLETED,
                TransferState.STATE_CANCELLED,
                TransferState.STATE_FAILED
            ).forEach {
                verify(completedTransferDao).deleteOldCompletedTransfersByState(
                    it.ordinal,
                    MAX_COMPLETED_TRANSFER_ROWS
                )
            }
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
            MAX_INSERT_LIST_SIZE,
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

        verify(activeTransferDao).insertOrUpdateActiveTransfers(expected)
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
            verify(completedTransferDao).insertOrUpdateCompletedTransfers(
                eq(expected),
                any(),
            )
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
                any(),
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

    @Test
    fun `test that removeRecentlyWatchedVideo invokes as expected`() =
        runTest {
            val testVideoHandle = 123456L
            underTest.removeRecentlyWatchedVideo(testVideoHandle)
            verify(videoRecentlyWatchedDao).removeRecentlyWatchedVideo(testVideoHandle)
        }

    @Test
    fun `test that clearRecentlyWatchedVideos invokes as expected`() =
        runTest {
            underTest.clearRecentlyWatchedVideos()
            verify(videoRecentlyWatchedDao).clearRecentlyWatchedVideos()
        }

    @Test
    fun `test that saveRecentlyWatchedVideo insert the mapped entity`() = runTest {
        val testItem = mock<VideoRecentlyWatchedItem>()
        val testEntity = mock<VideoRecentlyWatchedEntity>()
        whenever(videoRecentlyWatchedEntityMapper(testItem)).thenReturn(testEntity)

        underTest.saveRecentlyWatchedVideo(testItem)
        verify(videoRecentlyWatchedDao).insertVideo(testEntity)
    }

    @Test
    fun `test that saveRecentlyWatchedVideos insert the mapped entities`() = runTest {
        val testItems = (1..100).map {
            mock<VideoRecentlyWatchedItem>()
        }
        val testEntities = (1..100).map {
            mock<VideoRecentlyWatchedEntity>()
        }
        testItems.forEachIndexed { index, item ->
            whenever(videoRecentlyWatchedEntityMapper(item)).thenReturn(testEntities[index])
        }

        underTest.saveRecentlyWatchedVideos(testItems)
        verify(videoRecentlyWatchedDao).insertOrUpdateRecentlyWatchedVideos(testEntities)
    }

    @Test
    fun `test that getAllRecentlyWatchedVideos returns as expected`() =
        runTest {
            val testItems = (1..100L).map { value ->
                mock<VideoRecentlyWatchedItem> {
                    on { videoHandle }.thenReturn(value)
                    on { watchedTimestamp }.thenReturn(value)
                }
            }
            val testEntities = (1..100L).map { value ->
                mock<VideoRecentlyWatchedEntity> {
                    on { videoHandle }.thenReturn(value)
                    on { watchedTimestamp }.thenReturn(value)
                }
            }
            whenever(videoRecentlyWatchedDao.getAllRecentlyWatchedVideos()).thenReturn(
                flowOf(
                    testEntities
                )
            )
            testItems.forEachIndexed { index, item ->
                whenever(
                    videoRecentlyWatchedItemMapper(
                        testEntities[index].videoHandle,
                        testEntities[index].watchedTimestamp
                    )
                ).thenReturn(item)
            }
            underTest.getAllRecentlyWatchedVideos().test {
                assertThat(awaitItem()).isEqualTo(testItems)
                awaitComplete()
            }
        }

    @Test
    fun `test that insertPendingTransfers invokes dao batch insert with mapped entities`() =
        runTest {
            val element = mock<InsertPendingTransferRequest>()
            val mapped = mock<PendingTransferEntity>()
            whenever(insertPendingTransferRequestMapper(element)) doReturn mapped
            underTest.insertPendingTransfers(listOf(element))
            verify(pendingTransferDao).insertOrUpdatePendingTransfers(
                listOf(mapped),
                MAX_INSERT_LIST_SIZE

            )
        }

    @Test
    fun `test that monitorPendingTransfersByType return mapped dao result`() = runTest {
        val type = TransferType.DOWNLOAD
        val pendingTransferEntity = mock<PendingTransferEntity>()
        val pendingTransfer = mock<PendingTransfer>()
        val result = flowOf(listOf(pendingTransferEntity))
        val expected = listOf(pendingTransfer)

        whenever(pendingTransferModelMapper(pendingTransferEntity)) doReturn pendingTransfer
        whenever(pendingTransferDao.monitorPendingTransfersByType(type)) doReturn result
        underTest.monitorPendingTransfersByType(type).test {
            val actual = awaitItem()
            assertThat(actual).isEqualTo(expected)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that getPendingTransfersByType return mapped dao result`() = runTest {
        val type = TransferType.DOWNLOAD
        val pendingTransferEntity = mock<PendingTransferEntity>()
        val pendingTransfer = mock<PendingTransfer>()
        val result = listOf(pendingTransferEntity)
        val expected = listOf(pendingTransfer)

        whenever(pendingTransferModelMapper(pendingTransferEntity)) doReturn pendingTransfer
        whenever(pendingTransferDao.getPendingTransfersByType(type)) doReturn result
        val actual = underTest.getPendingTransfersByType(type)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that getPendingTransfersByState return mapped dao result`() = runTest {
        val state = PendingTransferState.NotSentToSdk
        val pendingTransferEntity = mock<PendingTransferEntity>()
        val pendingTransfer = mock<PendingTransfer>()
        val result = listOf(pendingTransferEntity)
        val expected = listOf(pendingTransfer)

        whenever(pendingTransferModelMapper(pendingTransferEntity)) doReturn pendingTransfer
        whenever(pendingTransferDao.getPendingTransfersByState(state)) doReturn result
        val actual = underTest.getPendingTransfersByState(state)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that getPendingTransfersByTypeAndState return mapped dao result`() = runTest {
        val type = TransferType.DOWNLOAD
        val state = PendingTransferState.NotSentToSdk
        val pendingTransferEntity = mock<PendingTransferEntity>()
        val pendingTransfer = mock<PendingTransfer>()
        val result = listOf(pendingTransferEntity)
        val expected = listOf(pendingTransfer)

        whenever(pendingTransferModelMapper(pendingTransferEntity)) doReturn pendingTransfer
        whenever(pendingTransferDao.getPendingTransfersByTypeAndState(type, state)) doReturn result
        val actual = underTest.getPendingTransfersByTypeAndState(type, state)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that monitorPendingTransfersByTypeAndState return mapped dao result`() = runTest {
        val type = TransferType.DOWNLOAD
        val state = PendingTransferState.NotSentToSdk
        val pendingTransferEntity = mock<PendingTransferEntity>()
        val pendingTransfer = mock<PendingTransfer>()
        val result = flowOf(listOf(pendingTransferEntity))
        val expected = listOf(pendingTransfer)

        whenever(pendingTransferModelMapper(pendingTransferEntity)) doReturn pendingTransfer
        whenever(
            pendingTransferDao.monitorPendingTransfersByTypeAndState(
                type,
                state
            )
        ) doReturn result
        underTest.monitorPendingTransfersByTypeAndState(type, state).test {
            val actual = awaitItem()
            assertThat(actual).isEqualTo(expected)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that getPendingTransferByTag return mapped dao result`() = runTest {
        val uniqueId = 343L
        val pendingTransferEntity = mock<PendingTransferEntity>()
        val expected = mock<PendingTransfer>()

        whenever(pendingTransferModelMapper(pendingTransferEntity)) doReturn expected
        whenever(pendingTransferDao.getPendingTransferByUniqueId(uniqueId)) doReturn pendingTransferEntity
        val actual = underTest.getPendingTransfersByUniqueId(uniqueId)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that updatePendingTransfers invokes dao method correctly when it's a single update`() =
        runTest {
            val updatePendingTransferRequests = mock<UpdatePendingTransferState>()

            underTest.updatePendingTransfers(updatePendingTransferRequests)

            verify(pendingTransferDao).update(updatePendingTransferRequests)
        }

    @Test
    fun `test that updatePendingTransfers invokes dao method correctly when there are multiple updates`() =
        runTest {
            val updatePendingTransferRequests1 = mock<UpdatePendingTransferState>()
            val updatePendingTransferRequests2 = mock<UpdateAlreadyTransferredFilesCount>()

            underTest.updatePendingTransfers(
                updatePendingTransferRequests1,
                updatePendingTransferRequests2
            )

            verify(pendingTransferDao)
                .updateMultiple(
                    listOf(
                        updatePendingTransferRequests1,
                        updatePendingTransferRequests2
                    )
                )
        }

    @Test
    fun `test that deletePendingTransferByTag invokes dao method with correct parameter`() =
        runTest {
            val uniqueId = 6456L

            underTest.deletePendingTransferByUniqueId(uniqueId)

            verify(pendingTransferDao).deletePendingTransferByUniqueId(uniqueId)
        }

    @Test
    fun `test that deleteAllPendingTransfers invokes dao method correctly`() =
        runTest {
            underTest.deleteAllPendingTransfers()

            verify(pendingTransferDao).deleteAllPendingTransfers()
        }

    @Test
    fun `test that insertActiveTransferGroup insert the mapped entities`() = runTest {
        val groupId: Int? = null
        val transferType = TransferType.DOWNLOAD
        val destination = "destination"
        val startTime = 987465L
        val activeTransferGroup =
            ActiveTransferActionGroupImpl(groupId, transferType, destination, startTime)
        val roomEntity =
            ActiveTransferActionGroupEntity(groupId, transferType, destination, startTime)
        whenever(activeTransferGroupEntityMapper(activeTransferGroup)) doReturn roomEntity

        val expected = roomEntity

        underTest.insertActiveTransferGroup(activeTransferGroup)

        verify(activeTransferGroupDao).insertActiveTransferGroup(expected)
    }

    @Test
    fun `test that insertActiveTransferGroup returns the new id generated by Dao`() = runTest {
        val expected = 34387L
        val activeTransferActionGroup = mock<ActiveTransferActionGroup>()
        val roomEntity = mock<ActiveTransferActionGroupEntity>()
        whenever(activeTransferGroupEntityMapper(activeTransferActionGroup)) doReturn roomEntity
        whenever(activeTransferGroupDao.insertActiveTransferGroup(roomEntity)) doReturn expected

        val actual = underTest.insertActiveTransferGroup(activeTransferActionGroup)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that insertOrUpdateLastPageViewedInPdf invokes dao with mapped entity`() = runTest {
        val entity = mock<LastPageViewedInPdf>()
        val mappedEntity = mock<LastPageViewedInPdfEntity>()

        whenever(lastPageViewedInPdfEntityMapper(entity)).thenReturn(mappedEntity)

        underTest.insertOrUpdateLastPageViewedInPdf(entity)

        verify(lastPageViewedInPdfDao).insertOrUpdateLastPageViewedInPdf(mappedEntity)
    }

    @Test
    fun `test that getLastPageViewedInPdfByHandle invokes dao with correct handle and returns mapped entity`() =
        runTest {
            val handle = 123L
            val entity = mock<LastPageViewedInPdfEntity>()
            val expected = mock<LastPageViewedInPdf>()

            whenever(lastPageViewedInPdfDao.getLastPageViewedInPdfByHandle(handle))
                .thenReturn(entity)
            whenever(lastPageViewedInPdfModelMapper(entity)).thenReturn(expected)

            assertThat(underTest.getLastPageViewedInPdfByHandle(handle)).isEqualTo(expected)
        }

    @Test
    fun `test that deleteLastPageViewedInPdfByHandle invokes dao with correct handle`() = runTest {
        val handle = 123L

        underTest.deleteLastPageViewedInPdfByHandle(handle)

        verify(lastPageViewedInPdfDao).deleteLastPageViewedInPdfByHandle(handle)
    }

    @Test
    fun `test that deleteAllLastPageViewedInPdf invokes dao`() = runTest {
        underTest.deleteAllLastPageViewedInPdf()

        verify(lastPageViewedInPdfDao).deleteAllLastPageViewedInPdf()
    }

    @Test
    fun `test that deletePlaybackInfo invokes as expected`() =
        runTest {
            val testVideoHandle = 123456L
            underTest.deletePlaybackInfo(testVideoHandle)
            verify(mediaPlaybackInfoDao).removePlaybackInfo(testVideoHandle)
        }

    @Test
    fun `test that clearAllPlaybackInfos invokes as expected`() =
        runTest {
            underTest.clearAllPlaybackInfos()
            verify(mediaPlaybackInfoDao).clearAllPlaybackInfos()
        }

    @Test
    fun `test that clearAudioPlaybackInfos invokes as expected`() =
        runTest {
            underTest.clearAudioPlaybackInfos()
            verify(mediaPlaybackInfoDao).clearPlaybackInfosByType(MediaType.Audio)
        }

    @Test
    fun `test that insertOrUpdatePlaybackInfo insert the mapped entity`() = runTest {
        val testItem = mock<MediaPlaybackInfo>()
        val testEntity = mock<MediaPlaybackInfoEntity>()
        whenever(mediaPlaybackInfoEntityMapper(testItem)).thenReturn(testEntity)

        underTest.insertOrUpdatePlaybackInfo(testItem)
        verify(mediaPlaybackInfoDao).insertOrUpdatePlaybackInfo(testEntity)
    }

    @Test
    fun `test that insertOrUpdatePlaybackInfos insert the mapped entities`() = runTest {
        val testItems = (1..100).map {
            mock<MediaPlaybackInfo>()
        }
        val testEntities = (1..100).map {
            mock<MediaPlaybackInfoEntity>()
        }
        testItems.forEachIndexed { index, item ->
            whenever(mediaPlaybackInfoEntityMapper(item)).thenReturn(testEntities[index])
        }

        underTest.insertOrUpdatePlaybackInfos(testItems)
        verify(mediaPlaybackInfoDao).insertOrUpdatePlaybackInfos(testEntities)
    }

    @Test
    fun `test that monitorAllPlaybackInfos returns as expected`() =
        runTest {
            val testItems = (1..100L).map { value ->
                mock<MediaPlaybackInfo> {
                    on { mediaHandle }.thenReturn(value)
                    on { mediaType }.thenReturn(
                        if (value % 2 == 0L) {
                            MediaType.Video
                        } else {
                            MediaType.Audio
                        }
                    )
                }
            }
            val testEntities = (1..100L).map { value ->
                mock<MediaPlaybackInfoEntity> {
                    on { mediaHandle }.thenReturn(value)
                    on { mediaType }.thenReturn(
                        if (value % 2 == 0L) {
                            MediaType.Video
                        } else {
                            MediaType.Audio
                        }
                    )
                }
            }
            whenever(mediaPlaybackInfoDao.getAllPlaybackInfos()).thenReturn(
                flowOf(
                    testEntities
                )
            )
            testItems.forEachIndexed { index, item ->
                whenever(mediaPlaybackInfoMapper(testEntities[index])).thenReturn(item)
            }
            underTest.monitorAllPlaybackInfos().test {
                assertThat(awaitItem()).isEqualTo(testItems)
                awaitComplete()
            }
        }

    @Test
    fun `test that monitorAudioPlaybackInfos returns as expected`() =
        runTest {
            val testItems = (1..100L).map { value ->
                mock<MediaPlaybackInfo> {
                    on { mediaHandle }.thenReturn(value)
                    on { mediaType }.thenReturn(
                        if (value % 2 == 0L) {
                            MediaType.Video
                        } else {
                            MediaType.Audio
                        }
                    )
                }
            }
            val testEntities = (1..100L).map { value ->
                mock<MediaPlaybackInfoEntity> {
                    on { mediaHandle }.thenReturn(value)
                    on { mediaType }.thenReturn(
                        if (value % 2 == 0L) {
                            MediaType.Video
                        } else {
                            MediaType.Audio
                        }
                    )
                }
            }
            val audioEntities = testEntities.filter { it.mediaType == MediaType.Audio }
            whenever(
                mediaPlaybackInfoDao.getAllPlaybackInfosByType(MediaType.Audio)
            ).thenReturn(flowOf(audioEntities))

            testItems.forEachIndexed { index, item ->
                whenever(mediaPlaybackInfoMapper(testEntities[index])).thenReturn(item)
            }
            val audioItems = testItems.filter { it.mediaType == MediaType.Audio }
            underTest.monitorAudioPlaybackInfos().test {
                assertThat(awaitItem()).isEqualTo(audioItems)
                awaitComplete()
            }
        }

    @Test
    fun `test that getMediaPlaybackInfo returns as expected`() =
        runTest {
            val testHandle = 123456L
            val testInfo = mock<MediaPlaybackInfo> {
                on { mediaHandle }.thenReturn(testHandle)
                on { mediaType }.thenReturn(MediaType.Audio)
            }
            val testEntity = mock<MediaPlaybackInfoEntity> {
                on { mediaHandle }.thenReturn(testHandle)
                on { mediaType }.thenReturn(MediaType.Audio)
            }

            whenever(mediaPlaybackInfoMapper(testEntity)).thenReturn(testInfo)
            whenever(mediaPlaybackInfoDao.getMediaPlaybackInfo(testHandle)).thenReturn(testEntity)
            val actual = underTest.getMediaPlaybackInfo(testHandle)
            assertThat(actual).isEqualTo(testInfo)
        }

    @Test
    fun `test that getMediaPlaybackInfo returns null`() =
        runTest {
            val testHandle = 123456L
            whenever(mediaPlaybackInfoDao.getMediaPlaybackInfo(testHandle)).thenReturn(null)
            val actual = underTest.getMediaPlaybackInfo(testHandle)
            assertThat(actual).isNull()
        }
}

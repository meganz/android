package mega.privacy.android.data.facade

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cryptography.DecryptData
import mega.privacy.android.data.cryptography.EncryptData
import mega.privacy.android.data.database.dao.ActiveTransferDao
import mega.privacy.android.data.database.dao.BackupDao
import mega.privacy.android.data.database.dao.CompletedTransferDao
import mega.privacy.android.data.database.dao.ContactDao
import mega.privacy.android.data.database.dao.SdTransferDao
import mega.privacy.android.data.database.dao.SyncRecordDao
import mega.privacy.android.data.database.entity.BackupEntity
import mega.privacy.android.data.database.entity.CompletedTransferEntity
import mega.privacy.android.data.database.entity.SdTransferEntity
import mega.privacy.android.data.database.entity.SyncRecordEntity
import mega.privacy.android.data.mapper.SyncStatusIntMapper
import mega.privacy.android.data.mapper.backup.BackupEntityMapper
import mega.privacy.android.data.mapper.backup.BackupInfoTypeIntMapper
import mega.privacy.android.data.mapper.backup.BackupModelMapper
import mega.privacy.android.data.mapper.camerauploads.SyncRecordEntityMapper
import mega.privacy.android.data.mapper.camerauploads.SyncRecordModelMapper
import mega.privacy.android.data.mapper.camerauploads.SyncRecordTypeIntMapper
import mega.privacy.android.data.mapper.contact.ContactEntityMapper
import mega.privacy.android.data.mapper.contact.ContactModelMapper
import mega.privacy.android.data.mapper.transfer.active.ActiveTransferEntityMapper
import mega.privacy.android.data.mapper.transfer.completed.CompletedTransferEntityMapper
import mega.privacy.android.data.mapper.transfer.completed.CompletedTransferModelMapper
import mega.privacy.android.data.mapper.transfer.sd.SdTransferEntityMapper
import mega.privacy.android.data.mapper.transfer.sd.SdTransferModelMapper
import mega.privacy.android.domain.entity.SdTransfer
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.entity.backup.Backup
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
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
    private val syncRecordDao: SyncRecordDao = mock()
    private val syncRecordModelMapper: SyncRecordModelMapper = mock()
    private val syncRecordEntityMapper: SyncRecordEntityMapper = mock()
    private val syncStatusIntMapper: SyncStatusIntMapper = mock()
    private val syncRecordTypeIntMapper: SyncRecordTypeIntMapper = mock()
    private val completedTransferEntityMapper: CompletedTransferEntityMapper = mock()
    private val sdTransferDao: SdTransferDao = mock()
    private val sdTransferEntityMapper = mock<SdTransferEntityMapper>()
    private val sdTransferModelMapper = mock<SdTransferModelMapper>()
    private val backupDao = mock<BackupDao>()
    private val backupEntityMapper = mock<BackupEntityMapper>()
    private val backupModelMapper = mock<BackupModelMapper>()
    private val backupInfoTypeIntMapper = mock<BackupInfoTypeIntMapper>()

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
            syncRecordDao = syncRecordDao,
            syncRecordModelMapper = syncRecordModelMapper,
            syncRecordEntityMapper = syncRecordEntityMapper,
            syncStatusIntMapper = syncStatusIntMapper,
            syncRecordTypeIntMapper = syncRecordTypeIntMapper,
            encryptData = encryptData,
            decryptData = decryptData,
            completedTransferEntityMapper = completedTransferEntityMapper,
            sdTransferDao = sdTransferDao,
            sdTransferEntityMapper = sdTransferEntityMapper,
            sdTransferModelMapper = sdTransferModelMapper,
            backupDao = backupDao,
            backupEntityMapper = backupEntityMapper,
            backupModelMapper = backupModelMapper,
            backupInfoTypeIntMapper = backupInfoTypeIntMapper,
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
            syncRecordDao,
            syncRecordModelMapper,
            syncRecordEntityMapper,
            syncStatusIntMapper,
            syncRecordTypeIntMapper,
            backupDao,
            backupEntityMapper,
            backupModelMapper,
            backupInfoTypeIntMapper,
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

            assertThat(underTest.getAllCompletedTransfers().single()).isEqualTo(expected)
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

            assertThat(underTest.getAllCompletedTransfers(expectedSize).single().size)
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

            assertThat(underTest.getAllCompletedTransfers().single().size)
                .isEqualTo(completedTransferEntities.size)
        }

    @Test
    fun `test that saveSyncRecord saves the corresponding item`() =
        runTest {
            val entity = mock<SyncRecordEntity>()
            val record = mock<SyncRecord>()

            whenever(syncRecordEntityMapper(record)).thenReturn(entity)
            underTest.saveSyncRecord(record)
            verify(syncRecordDao).insertOrUpdateSyncRecord(entity)
        }

    @Test
    fun `test that setUploadVideoSyncStatus updates the corresponding item`() =
        runTest {
            val status = 1
            underTest.setUploadVideoSyncStatus(status)
            verify(syncRecordDao).updateVideoState(status)
        }

    @ParameterizedTest(name = "invoked with isSecondary {0} count {1}")
    @MethodSource("provideDoesFileNameExistParameters")
    fun `test that doesFileNameExist returns correctly`(
        isSecondary: Boolean,
        count: Int,
        expected: Boolean,
    ) =
        runTest {
            val fileName = "abcd.jpg"
            whenever(encryptData(fileName)).thenReturn(fileName)
            whenever(encryptData(isSecondary.toString())).thenReturn(isSecondary.toString())
            whenever(
                syncRecordDao.getSyncRecordCountByFileName(
                    fileName,
                    isSecondary.toString()
                )
            ).thenReturn(count)
            assertThat(underTest.doesFileNameExist(fileName, isSecondary)).isEqualTo(expected)
        }

    @ParameterizedTest(name = "invoked with isSecondary {0} count {1}")
    @MethodSource("provideDoesFileNameExistParameters")
    fun `test that doesLocalPathExist returns correctly`(
        isSecondary: Boolean,
        count: Int,
        expected: Boolean,
    ) =
        runTest {
            val fileName = "abcd.jpg"
            whenever(encryptData(fileName)).thenReturn(fileName)
            whenever(encryptData(isSecondary.toString())).thenReturn(isSecondary.toString())
            whenever(
                syncRecordDao.getSyncRecordCountByOriginalPath(
                    fileName,
                    isSecondary.toString()
                )
            ).thenReturn(count)
            assertThat(underTest.doesLocalPathExist(fileName, isSecondary)).isEqualTo(expected)
        }

    @Test
    fun `test that getSyncRecordByFingerprint returns correctly`() = runTest {
        val entity = mock<SyncRecordEntity>()
        val record = mock<SyncRecord>()
        val fingerprint = "abcd"
        val isSecondary = true
        val isCopyOnly = false
        whenever(syncRecordModelMapper(entity)).thenReturn(record)
        whenever(encryptData(fingerprint)).thenReturn(fingerprint)
        whenever(encryptData(isSecondary.toString())).thenReturn(isSecondary.toString())
        whenever(encryptData(isCopyOnly.toString())).thenReturn(isCopyOnly.toString())
        whenever(
            syncRecordDao.getSyncRecordByOriginalFingerprint(
                fingerprint,
                isSecondary.toString(),
                isCopyOnly.toString()
            )
        ).thenReturn(entity)
        assertThat(
            underTest.getSyncRecordByFingerprint(
                fingerprint,
                isSecondary,
                isCopyOnly
            )
        ).isEqualTo(record)
    }

    @Test
    fun `test that getPendingSyncRecords returns correctly`() = runTest {
        val entities = listOf<SyncRecordEntity>(mock(), mock())
        val records = listOf<SyncRecord>(mock(), mock())
        entities.forEachIndexed { index, entity ->
            whenever(syncRecordModelMapper(entity)).thenReturn(records[index])
        }
        whenever(syncStatusIntMapper(SyncStatus.STATUS_PENDING)).thenReturn(0)
        whenever(syncRecordDao.getSyncRecordsBySyncState(0)).thenReturn(entities)
        assertThat(underTest.getPendingSyncRecords()).isEqualTo(records)
    }

    @Test
    fun `test that getVideoSyncRecordsByStatus returns correctly`() = runTest {
        val state = 0
        val entities = listOf<SyncRecordEntity>(mock(), mock())
        val records = listOf<SyncRecord>(mock(), mock())
        entities.forEachIndexed { index, entity ->
            whenever(syncRecordModelMapper(entity)).thenReturn(records[index])
        }
        whenever(syncRecordTypeIntMapper(SyncRecordType.TYPE_VIDEO)).thenReturn(2)
        whenever(
            syncRecordDao.getSyncRecordsBySyncStateAndType(
                syncState = state,
                syncType = 2
            )
        ).thenReturn(entities)
        assertThat(underTest.getVideoSyncRecordsByStatus(state)).isEqualTo(records)
    }

    @Test
    fun `test that deleteAllSyncRecords deletes correctly`() = runTest {
        val syncType = 2
        underTest.deleteAllSyncRecords(syncType)
        verify(syncRecordDao).deleteSyncRecordsByType(syncType)
    }

    @Test
    fun `test that deleteAllSyncRecordsTypeAny deletes correctly`() = runTest {
        whenever(syncRecordTypeIntMapper(SyncRecordType.TYPE_ANY)).thenReturn(-1)
        underTest.deleteAllSyncRecordsTypeAny()
        verify(syncRecordDao).deleteSyncRecordsByType(-1)
    }

    @Test
    fun `test that deleteAllSecondarySyncRecords deletes correctly`() = runTest {
        val secondary = "true"
        whenever(encryptData(secondary)).thenReturn(secondary)
        underTest.deleteAllSecondarySyncRecords()
        verify(syncRecordDao).deleteSyncRecordsByIsSecondary(secondary)
    }

    @Test
    fun `test that deleteAllPrimarySyncRecords deletes correctly`() = runTest {
        val secondary = "false"
        whenever(encryptData(secondary)).thenReturn(secondary)
        underTest.deleteAllPrimarySyncRecords()
        verify(syncRecordDao).deleteSyncRecordsByIsSecondary(secondary)
    }

    @ParameterizedTest(name = "invoked with isSecondary = {0}")
    @ValueSource(booleans = [true, false])
    fun `test that getSyncRecordByLocalPath returns correctly`(isSecondary: Boolean) = runTest {
        val originalPath = "path/to/original/a.jpg"
        val entity = mock<SyncRecordEntity>()
        val record = mock<SyncRecord>()
        whenever(syncRecordModelMapper(entity)).thenReturn(record)
        whenever(encryptData(originalPath)).thenReturn(originalPath)
        whenever(encryptData(isSecondary.toString())).thenReturn(isSecondary.toString())
        whenever(
            syncRecordDao.getSyncRecordByOriginalPathAndIsSecondary(
                originalPath,
                isSecondary.toString()
            )
        ).thenReturn(entity)
        assertThat(underTest.getSyncRecordByLocalPath(originalPath, isSecondary)).isEqualTo(record)
    }

    @ParameterizedTest(name = "invoked with isSecondary = {0}")
    @ValueSource(booleans = [true, false])
    fun `test that deleteSyncRecordByPath returns correctly`(isSecondary: Boolean) = runTest {
        val originalPath = "path/to/original/a.jpg"
        whenever(encryptData(originalPath)).thenReturn(originalPath)
        whenever(encryptData(isSecondary.toString())).thenReturn(isSecondary.toString())
        underTest.deleteSyncRecordByPath(originalPath, isSecondary)
        verify(syncRecordDao).deleteSyncRecordByOriginalPathOrNewPathAndIsSecondary(
            originalPath,
            isSecondary.toString()
        )
    }

    @ParameterizedTest(name = "invoked with isSecondary = {0}")
    @ValueSource(booleans = [true, false])
    fun `test that deleteSyncRecordByLocalPath returns correctly`(isSecondary: Boolean) = runTest {
        val originalPath = "path/to/original/a.jpg"
        whenever(encryptData(originalPath)).thenReturn(originalPath)
        whenever(encryptData(isSecondary.toString())).thenReturn(isSecondary.toString())
        underTest.deleteSyncRecordByLocalPath(originalPath, isSecondary)
        verify(syncRecordDao).deleteSyncRecordByOriginalPathAndIsSecondary(
            originalPath,
            isSecondary.toString()
        )
    }

    @ParameterizedTest(name = "invoked with isSecondary = {0}")
    @ValueSource(booleans = [true, false])
    fun `test that deleteSyncRecordByFingerPrint returns correctly`(isSecondary: Boolean) =
        runTest {
            val originalFingerPrint = "abcde"
            val newFingerPrint = "efghijk"
            whenever(encryptData(originalFingerPrint)).thenReturn(originalFingerPrint)
            whenever(encryptData(newFingerPrint)).thenReturn(newFingerPrint)
            whenever(encryptData(isSecondary.toString())).thenReturn(isSecondary.toString())
            underTest.deleteSyncRecordByFingerPrint(
                originalPrint = originalFingerPrint,
                newPrint = newFingerPrint,
                isSecondary
            )
            verify(syncRecordDao).deleteSyncRecordByFingerprintsAndIsSecondary(
                originalFingerPrint,
                newFingerPrint,
                isSecondary.toString()
            )
        }

    @Test
    fun `test that getAllTimestampsOfSyncRecord returns correctly`() = runTest {
        val secondary = false
        val syncType = 2
        val timeStamps = (1L..10L).map {
            whenever(decryptData(it.toString())).thenReturn(it.toString())
            it.toString()
        }
        whenever(encryptData(secondary.toString())).thenReturn(secondary.toString())
        whenever(
            syncRecordDao.getAllTimestampsByIsSecondaryAndSyncType(
                secondary.toString(),
                syncType
            )
        ).thenReturn(timeStamps)
        val actual = underTest.getAllTimestampsOfSyncRecord(secondary, syncType)
        assertThat(actual).isEqualTo((1L..10L).map { it })
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
            whenever(backupModelMapper(entities.first())).thenReturn(backup)
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
            whenever(backupModelMapper(entities.first())).thenReturn(backup)
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
            whenever(decryptData(ids.first())).thenReturn(ids.first())
            val actual = underTest.getCuBackUpId()
            assertThat(actual).isEqualTo(ids.first().toLong())
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
            whenever(decryptData(ids.first())).thenReturn(ids.first())
            val actual = underTest.getMuBackUpId()
            assertThat(actual).isEqualTo(ids.first().toLong())
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

    private fun provideDoesFileNameExistParameters() = Stream.of(
        Arguments.of(true, 1, true),
        Arguments.of(false, 1, true),
        Arguments.of(true, 2, false),
        Arguments.of(false, 2, false),
    )
}

package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.DeleteSyncRecordByLocalPath
import mega.privacy.android.domain.usecase.FileNameExists
import mega.privacy.android.domain.usecase.GetDeviceCurrentNanoTimeUseCase
import mega.privacy.android.domain.usecase.GetSyncRecordByFingerprint
import mega.privacy.android.domain.usecase.node.GetChildNodeUseCase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.capture
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SaveSyncRecordsToDBUseCaseTest {

    private lateinit var underTest: SaveSyncRecordsToDBUseCase

    private val getSyncRecordByFingerprint = mock<GetSyncRecordByFingerprint>()
    private val deleteSyncRecordByLocalPath = mock<DeleteSyncRecordByLocalPath>()
    private val areUploadFileNamesKeptUseCase = mock<AreUploadFileNamesKeptUseCase>()
    private val getChildNodeUseCase = mock<GetChildNodeUseCase>()
    private val fileNameExists = mock<FileNameExists>()
    private val saveSyncRecordsUseCase = mock<SaveSyncRecordsUseCase>()
    private val getDeviceCurrentNanoTimeUseCase = mock<GetDeviceCurrentNanoTimeUseCase>()
    private val fileSystemRepository = mock<FileSystemRepository>()

    private val fakeUploadNodeId = NodeId(123L)
    private val fakeRecord = SyncRecord(
        localPath = "path",
        newPath = null,
        originFingerprint = "originFingerprint",
        newFingerprint = null,
        timestamp = 0L,
        fileName = "fileName.jpg",
        longitude = null,
        latitude = null,
        status = 0,
        type = SyncRecordType.TYPE_PHOTO,
        nodeHandle = null,
        isCopyOnly = false,
        isSecondary = false,
    )

    @BeforeAll
    fun setUp() {
        underTest = SaveSyncRecordsToDBUseCase(
            getSyncRecordByFingerprint,
            deleteSyncRecordByLocalPath,
            areUploadFileNamesKeptUseCase,
            getChildNodeUseCase,
            fileNameExists,
            saveSyncRecordsUseCase,
            getDeviceCurrentNanoTimeUseCase,
            fileSystemRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getSyncRecordByFingerprint,
            deleteSyncRecordByLocalPath,
            areUploadFileNamesKeptUseCase,
            getChildNodeUseCase,
            fileNameExists,
            saveSyncRecordsUseCase,
            getDeviceCurrentNanoTimeUseCase,
            fileSystemRepository,
        )
    }

    @Test
    fun `test that if record exists in DB with older timestamp, remove old record from DB`() =
        runTest {
            val record = fakeRecord.copy(timestamp = 12345L)
            val recordInDB = fakeRecord.copy(timestamp = 0L)
            val list = listOf(record)

            whenever(areUploadFileNamesKeptUseCase()).thenReturn(true)
            whenever(getChildNodeUseCase(fakeUploadNodeId, record.fileName)).thenReturn(null)
            whenever(fileNameExists(record.fileName, record.isSecondary)).thenReturn(false)
            whenever(getDeviceCurrentNanoTimeUseCase()).thenReturn(111111L)
            whenever(fileSystemRepository.doesFileExist(record.localPath)).thenReturn(true)
            whenever(
                getSyncRecordByFingerprint(
                    record.originFingerprint,
                    record.isSecondary,
                    record.isCopyOnly
                )
            ).thenReturn(recordInDB)

            underTest(list, fakeUploadNodeId, "")

            verify(deleteSyncRecordByLocalPath).invoke(
                recordInDB.localPath,
                recordInDB.isSecondary
            )
        }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `test that if record exists in DB with older timestamp, record is added to list to save in DB`() =
        runTest {
            val record = fakeRecord.copy(timestamp = 12345L)
            val recordInDB = fakeRecord.copy(timestamp = 0L)
            val list = listOf(record)

            whenever(areUploadFileNamesKeptUseCase()).thenReturn(true)
            whenever(getChildNodeUseCase(fakeUploadNodeId, record.fileName)).thenReturn(null)
            whenever(fileNameExists(record.fileName, record.isSecondary)).thenReturn(false)
            whenever(getDeviceCurrentNanoTimeUseCase()).thenReturn(111111L)
            whenever(fileSystemRepository.doesFileExist(record.localPath)).thenReturn(true)
            whenever(
                getSyncRecordByFingerprint(
                    record.originFingerprint,
                    record.isSecondary,
                    record.isCopyOnly
                )
            ).thenReturn(recordInDB)

            underTest(list, fakeUploadNodeId, "")

            val argumentCaptor: ArgumentCaptor<List<SyncRecord>> =
                ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<SyncRecord>>
            verify(saveSyncRecordsUseCase).invoke(capture(argumentCaptor))

            val capturedArgument: List<SyncRecord> = argumentCaptor.value
            assertThat(capturedArgument.any { it.fileName == record.fileName }).isTrue()
        }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `test that if record exists in DB with same or newer timestamp, record is not added to save in DB`() =
        runTest {
            val record = fakeRecord.copy(timestamp = 12345L)
            val recordInDB = fakeRecord.copy(timestamp = 12345L)
            val list = listOf(record)

            whenever(areUploadFileNamesKeptUseCase()).thenReturn(true)
            whenever(getChildNodeUseCase(fakeUploadNodeId, record.fileName)).thenReturn(null)
            whenever(fileNameExists(record.fileName, record.isSecondary)).thenReturn(false)
            whenever(getDeviceCurrentNanoTimeUseCase()).thenReturn(111111L)
            whenever(fileSystemRepository.doesFileExist(record.localPath)).thenReturn(true)
            whenever(
                getSyncRecordByFingerprint(
                    record.originFingerprint,
                    record.isSecondary,
                    record.isCopyOnly
                )
            ).thenReturn(recordInDB)

            underTest(list, fakeUploadNodeId, "")

            val argumentCaptor: ArgumentCaptor<List<SyncRecord>> =
                ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<SyncRecord>>
            verify(saveSyncRecordsUseCase).invoke(capture(argumentCaptor))

            val capturedArgument: List<SyncRecord> = argumentCaptor.value
            assertThat(capturedArgument.any { it.fileName == record.fileName }).isFalse()
        }

    @Test
    fun `test that if record should be uploaded and file does not exist, remove old record from DB`() =
        runTest {
            val record = fakeRecord.copy()
            val recordInDB = fakeRecord.copy()
            val list = listOf(record)

            whenever(areUploadFileNamesKeptUseCase()).thenReturn(true)
            whenever(getChildNodeUseCase(fakeUploadNodeId, record.fileName)).thenReturn(null)
            whenever(fileNameExists(record.fileName, record.isSecondary)).thenReturn(false)
            whenever(getDeviceCurrentNanoTimeUseCase()).thenReturn(111111L)
            whenever(fileSystemRepository.doesFileExist(record.localPath)).thenReturn(false)

            whenever(
                getSyncRecordByFingerprint(
                    record.originFingerprint,
                    record.isSecondary,
                    record.isCopyOnly
                )
            ).thenReturn(recordInDB)


            underTest(list, fakeUploadNodeId, "")

            verify(deleteSyncRecordByLocalPath).invoke(
                record.localPath,
                record.isSecondary
            )
        }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `test that if record should be uploaded and file does not exist, record is not added to save in DB`() =
        runTest {
            val primaryNodeId = NodeId(123)
            val record = fakeRecord.copy()
            val recordInDB = fakeRecord.copy()
            val list = listOf(record)

            whenever(areUploadFileNamesKeptUseCase()).thenReturn(true)
            whenever(getChildNodeUseCase(primaryNodeId, record.fileName)).thenReturn(null)
            whenever(fileNameExists(record.fileName, record.isSecondary)).thenReturn(false)
            whenever(getDeviceCurrentNanoTimeUseCase()).thenReturn(111111L)
            whenever(fileSystemRepository.doesFileExist(record.localPath)).thenReturn(false)
            whenever(
                getSyncRecordByFingerprint(
                    record.originFingerprint,
                    record.isSecondary,
                    record.isCopyOnly
                )
            ).thenReturn(recordInDB)

            underTest(list, primaryNodeId, "")

            val argumentCaptor: ArgumentCaptor<List<SyncRecord>> =
                ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<SyncRecord>>
            verify(saveSyncRecordsUseCase).invoke(capture(argumentCaptor))

            val capturedArgument: List<SyncRecord> = argumentCaptor.value
            assertThat(capturedArgument.any { it.fileName == record.fileName }).isFalse()
        }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `test that if keep name setting ON and name does not already exist, record is added to save in DB with original file name`() =
        runTest {
            val primaryNodeId = NodeId(123)
            val record = fakeRecord.copy()
            val list = listOf(record)

            whenever(areUploadFileNamesKeptUseCase()).thenReturn(true)
            whenever(getChildNodeUseCase(primaryNodeId, record.fileName)).thenReturn(null)
            whenever(fileNameExists(record.fileName, record.isSecondary)).thenReturn(false)
            whenever(getDeviceCurrentNanoTimeUseCase()).thenReturn(111111L)
            whenever(fileSystemRepository.doesFileExist(record.localPath)).thenReturn(true)
            whenever(
                getSyncRecordByFingerprint(
                    record.originFingerprint,
                    record.isSecondary,
                    record.isCopyOnly
                )
            ).thenReturn(null)

            underTest(list, primaryNodeId, "")

            val argumentCaptor: ArgumentCaptor<List<SyncRecord>> =
                ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<SyncRecord>>
            verify(saveSyncRecordsUseCase).invoke(capture(argumentCaptor))

            val capturedArgument: List<SyncRecord> = argumentCaptor.value
            assertThat(capturedArgument.any { it.fileName == record.fileName }).isTrue()
        }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `test that if keep name setting ON and name already exists in target folder, record is added to save in DB with suffix index`() =
        runTest {
            val primaryNodeId = NodeId(123)
            val record = fakeRecord.copy()
            val list = listOf(record)

            val unTypedNode = mock<FileNode> {
                on { id }.thenReturn(NodeId(9999L))
            }

            val expected =
                record.fileName.substringBeforeLast('.', "") +
                        "_1" + "." +
                        record.fileName.substringAfterLast('.', "")

            whenever(areUploadFileNamesKeptUseCase()).thenReturn(true)
            whenever(getChildNodeUseCase(primaryNodeId, record.fileName)).thenReturn(unTypedNode)
            whenever(getChildNodeUseCase(primaryNodeId, expected)).thenReturn(null)
            whenever(fileNameExists(record.fileName, record.isSecondary)).thenReturn(false)
            whenever(fileNameExists(expected, record.isSecondary)).thenReturn(false)
            whenever(getDeviceCurrentNanoTimeUseCase()).thenReturn(111111L)
            whenever(fileSystemRepository.doesFileExist(record.localPath)).thenReturn(true)
            whenever(
                getSyncRecordByFingerprint(
                    record.originFingerprint,
                    record.isSecondary,
                    record.isCopyOnly
                )
            ).thenReturn(null)

            underTest(list, primaryNodeId, "")

            val argumentCaptor: ArgumentCaptor<List<SyncRecord>> =
                ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<SyncRecord>>
            verify(saveSyncRecordsUseCase).invoke(capture(argumentCaptor))

            val capturedArgument: List<SyncRecord> = argumentCaptor.value
            assertThat(capturedArgument.any { it.fileName == expected }).isTrue()
        }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `test that if keep name setting ON and name already exists in DB, record is added to save in DB with suffix index`() =
        runTest {
            val primaryNodeId = NodeId(123)
            val record = fakeRecord.copy()
            val list = listOf(record)

            val expected =
                record.fileName.substringBeforeLast('.', "") +
                        "_1" + "." +
                        record.fileName.substringAfterLast('.', "")

            whenever(areUploadFileNamesKeptUseCase()).thenReturn(true)
            whenever(getChildNodeUseCase(primaryNodeId, record.fileName)).thenReturn(null)
            whenever(getChildNodeUseCase(primaryNodeId, expected)).thenReturn(null)
            whenever(fileNameExists(record.fileName, record.isSecondary)).thenReturn(true)
            whenever(fileNameExists(expected, record.isSecondary)).thenReturn(false)
            whenever(getDeviceCurrentNanoTimeUseCase()).thenReturn(111111L)
            whenever(fileSystemRepository.doesFileExist(record.localPath)).thenReturn(true)
            whenever(
                getSyncRecordByFingerprint(
                    record.originFingerprint,
                    record.isSecondary,
                    record.isCopyOnly
                )
            ).thenReturn(null)

            underTest(list, primaryNodeId, "")

            val argumentCaptor: ArgumentCaptor<List<SyncRecord>> =
                ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<SyncRecord>>
            verify(saveSyncRecordsUseCase).invoke(capture(argumentCaptor))

            val capturedArgument: List<SyncRecord> = argumentCaptor.value
            assertThat(capturedArgument.any { it.fileName == expected }).isTrue()
        }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `test that if keep name setting ON and name already exists in current list, record is added to save in DB with suffix index`() =
        runTest {
            val primaryNodeId = NodeId(123)
            val record0 = fakeRecord.copy()
            val record = fakeRecord.copy()
            val list = listOf(record0, record)

            val expected =
                record.fileName.substringBeforeLast('.', "") +
                        "_1" + "." +
                        record.fileName.substringAfterLast('.', "")

            whenever(areUploadFileNamesKeptUseCase()).thenReturn(true)
            whenever(getChildNodeUseCase(primaryNodeId, record.fileName)).thenReturn(null)
            whenever(getChildNodeUseCase(primaryNodeId, expected)).thenReturn(null)
            whenever(fileNameExists(record.fileName, record.isSecondary)).thenReturn(false)
            whenever(fileNameExists(expected, record.isSecondary)).thenReturn(false)
            whenever(getDeviceCurrentNanoTimeUseCase()).thenReturn(111111L)
            whenever(fileSystemRepository.doesFileExist(record.localPath)).thenReturn(true)
            whenever(
                getSyncRecordByFingerprint(
                    record.originFingerprint,
                    record.isSecondary,
                    record.isCopyOnly
                )
            ).thenReturn(null)

            underTest(list, primaryNodeId, "")

            val argumentCaptor: ArgumentCaptor<List<SyncRecord>> =
                ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<SyncRecord>>
            verify(saveSyncRecordsUseCase).invoke(capture(argumentCaptor))

            val capturedArgument: List<SyncRecord> = argumentCaptor.value
            assertThat(capturedArgument.any { it.fileName == expected }).isTrue()
        }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `test that if keep name setting OFF and name does not already exist, record is added to save in DB with formatted file name`() =
        runTest {
            val timestamp = System.currentTimeMillis()
            val primaryNodeId = NodeId(123)
            val record = fakeRecord.copy(timestamp = timestamp)
            val list = listOf(record)

            val sdf = SimpleDateFormat("yyyy-MM-dd HH.mm.ss", Locale.getDefault())
            val expected = sdf.format(Date(timestamp)) + "." +
                    record.fileName.substringAfterLast('.', "")

            whenever(areUploadFileNamesKeptUseCase()).thenReturn(false)
            whenever(getChildNodeUseCase(primaryNodeId, expected)).thenReturn(null)
            whenever(fileNameExists(expected, record.isSecondary)).thenReturn(false)
            whenever(getDeviceCurrentNanoTimeUseCase()).thenReturn(111111L)
            whenever(fileSystemRepository.doesFileExist(record.localPath)).thenReturn(true)
            whenever(
                getSyncRecordByFingerprint(
                    record.originFingerprint,
                    record.isSecondary,
                    record.isCopyOnly
                )
            ).thenReturn(null)

            underTest(list, primaryNodeId, "")

            val argumentCaptor: ArgumentCaptor<List<SyncRecord>> =
                ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<SyncRecord>>
            verify(saveSyncRecordsUseCase).invoke(capture(argumentCaptor))

            val capturedArgument: List<SyncRecord> = argumentCaptor.value
            assertThat(capturedArgument.any { it.fileName == expected }).isTrue()
        }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `test that if keep name setting OFF and name already exists in target folder, record is added to save in DB with formatted file name and suffix index`() =
        runTest {
            val timestamp = System.currentTimeMillis()
            val primaryNodeId = NodeId(123)
            val record = fakeRecord.copy(timestamp = timestamp)
            val list = listOf(record)

            val unTypedNode = mock<FileNode> {
                on { id }.thenReturn(NodeId(9999L))
            }

            val sdf = SimpleDateFormat("yyyy-MM-dd HH.mm.ss", Locale.getDefault())
            val initialName = sdf.format(Date(timestamp)) + "." +
                    record.fileName.substringAfterLast('.', "")
            val expected = initialName.substringBeforeLast('.', "") +
                    "_1" + "." +
                    initialName.substringAfterLast('.', "")

            whenever(areUploadFileNamesKeptUseCase()).thenReturn(false)
            whenever(getChildNodeUseCase(primaryNodeId, initialName)).thenReturn(unTypedNode)
            whenever(getChildNodeUseCase(primaryNodeId, expected)).thenReturn(null)
            whenever(fileNameExists(initialName, record.isSecondary)).thenReturn(false)
            whenever(fileNameExists(expected, record.isSecondary)).thenReturn(false)
            whenever(getDeviceCurrentNanoTimeUseCase()).thenReturn(111111L)
            whenever(fileSystemRepository.doesFileExist(record.localPath)).thenReturn(true)
            whenever(
                getSyncRecordByFingerprint(
                    record.originFingerprint,
                    record.isSecondary,
                    record.isCopyOnly
                )
            ).thenReturn(null)

            underTest(list, primaryNodeId, "")

            val argumentCaptor: ArgumentCaptor<List<SyncRecord>> =
                ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<SyncRecord>>
            verify(saveSyncRecordsUseCase).invoke(capture(argumentCaptor))

            val capturedArgument: List<SyncRecord> = argumentCaptor.value
            assertThat(capturedArgument.any { it.fileName == expected }).isTrue()
        }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `test that if keep name setting OFF and name already exists in DB, record is added to save in DB with suffix index`() =
        runTest {
            val timestamp = System.currentTimeMillis()
            val primaryNodeId = NodeId(123)
            val record = fakeRecord.copy(timestamp = timestamp)
            val list = listOf(record)

            val sdf = SimpleDateFormat("yyyy-MM-dd HH.mm.ss", Locale.getDefault())
            val initialName = sdf.format(Date(timestamp)) + "." +
                    record.fileName.substringAfterLast('.', "")
            val expected = initialName.substringBeforeLast('.', "") +
                    "_1" + "." +
                    initialName.substringAfterLast('.', "")

            whenever(areUploadFileNamesKeptUseCase()).thenReturn(false)
            whenever(getChildNodeUseCase(primaryNodeId, initialName)).thenReturn(null)
            whenever(getChildNodeUseCase(primaryNodeId, expected)).thenReturn(null)
            whenever(fileNameExists(initialName, record.isSecondary)).thenReturn(true)
            whenever(fileNameExists(expected, record.isSecondary)).thenReturn(false)
            whenever(getDeviceCurrentNanoTimeUseCase()).thenReturn(111111L)
            whenever(fileSystemRepository.doesFileExist(record.localPath)).thenReturn(true)
            whenever(
                getSyncRecordByFingerprint(
                    record.originFingerprint,
                    record.isSecondary,
                    record.isCopyOnly
                )
            ).thenReturn(null)

            underTest(list, primaryNodeId, "")

            val argumentCaptor: ArgumentCaptor<List<SyncRecord>> =
                ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<SyncRecord>>
            verify(saveSyncRecordsUseCase).invoke(capture(argumentCaptor))

            val capturedArgument: List<SyncRecord> = argumentCaptor.value
            assertThat(capturedArgument.any { it.fileName == expected }).isTrue()
        }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `test that if keep name setting OFF and name already exists in current list, record is added to save in DB with suffix index`() =
        runTest {
            val timestamp = System.currentTimeMillis()
            val primaryNodeId = NodeId(123)
            val record0 = fakeRecord.copy(timestamp = timestamp)
            val record = fakeRecord.copy(timestamp = timestamp)
            val list = listOf(record0, record)

            val sdf = SimpleDateFormat("yyyy-MM-dd HH.mm.ss", Locale.getDefault())
            val initialName = sdf.format(Date(timestamp)) + "." +
                    record.fileName.substringAfterLast('.', "")
            val expected = initialName.substringBeforeLast('.', "") +
                    "_1" + "." +
                    initialName.substringAfterLast('.', "")

            whenever(areUploadFileNamesKeptUseCase()).thenReturn(false)
            whenever(getChildNodeUseCase(primaryNodeId, initialName)).thenReturn(null)
            whenever(getChildNodeUseCase(primaryNodeId, expected)).thenReturn(null)
            whenever(fileNameExists(initialName, record.isSecondary)).thenReturn(false)
            whenever(fileNameExists(expected, record.isSecondary)).thenReturn(false)
            whenever(getDeviceCurrentNanoTimeUseCase()).thenReturn(111111L)
            whenever(fileSystemRepository.doesFileExist(record.localPath)).thenReturn(true)
            whenever(
                getSyncRecordByFingerprint(
                    record.originFingerprint,
                    record.isSecondary,
                    record.isCopyOnly
                )
            ).thenReturn(null)

            underTest(list, primaryNodeId, "")

            val argumentCaptor: ArgumentCaptor<List<SyncRecord>> =
                ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<SyncRecord>>
            verify(saveSyncRecordsUseCase).invoke(capture(argumentCaptor))

            val capturedArgument: List<SyncRecord> = argumentCaptor.value
            assertThat(capturedArgument.any { it.fileName == expected }).isTrue()
        }
}

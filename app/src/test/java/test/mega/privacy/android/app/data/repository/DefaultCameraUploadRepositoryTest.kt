package test.mega.privacy.android.app.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.data.gateway.api.MegaLocalStorageGateway
import mega.privacy.android.app.data.repository.DefaultCameraUploadRepository
import mega.privacy.android.data.gateway.FileAttributeGateway
import mega.privacy.android.data.mapper.SyncRecordTypeIntMapper
import mega.privacy.android.domain.entity.SyncRecord
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.entity.SyncStatus
import mega.privacy.android.domain.entity.SyncTimeStamp
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalCoroutinesApi::class)
@ExperimentalContracts
class DefaultCameraUploadRepositoryTest {
    private lateinit var underTest: CameraUploadRepository

    private val localStorageGateway = mock<MegaLocalStorageGateway>()
    private val fileAttributeGateway = mock<FileAttributeGateway>()
    private val syncRecordTypeIntMapper = mock<SyncRecordTypeIntMapper>()

    private val fakeRecord = SyncRecord(
        id = 0,
        localPath = null,
        newPath = null,
        originFingerprint = null,
        newFingerprint = null,
        timestamp = null,
        fileName = null,
        longitude = null,
        latitude = null,
        status = SyncStatus.STATUS_PENDING.value,
        type = -1,
        nodeHandle = null,
        isCopyOnly = false,
        isSecondary = false
    )

    @Before
    fun setUp() {
        underTest = DefaultCameraUploadRepository(
            localStorageGateway = localStorageGateway,
            megaApiGateway = mock(),
            cacheGateway = mock(),
            fileAttributeGateway = fileAttributeGateway,
            syncRecordTypeIntMapper = syncRecordTypeIntMapper,
            ioDispatcher = UnconfinedTestDispatcher(),
            cameraTimestampsPreferenceGateway = mock()
        )
    }

    @Test
    fun `test camera upload sync by wifi only setting`() = runTest {
        whenever(localStorageGateway.isSyncByWifi()).thenReturn(true)
        assertThat(underTest.isSyncByWifi()).isTrue()
    }

    @Test
    fun `test camera upload sync by wifi only default setting`() = runTest {
        whenever(localStorageGateway.isSyncByWifiDefault()).thenReturn(true)
        assertThat(underTest.isSyncByWifiDefault()).isTrue()
    }

    @Test
    fun `test camera upload retrieves sync records`() = runTest {
        whenever(localStorageGateway.getPendingSyncRecords()).thenReturn(listOf(fakeRecord))
        assertThat(underTest.getPendingSyncRecords()).isEqualTo(listOf(fakeRecord))
    }

    @Test
    fun `test camera upload gets sync file upload`() = runTest {
        whenever(localStorageGateway.getCameraSyncFileUpload()).thenReturn(null)
        assertThat(underTest.getSyncFileUpload()).isEqualTo(null)
    }

    @Test
    fun `test camera upload retrieves video quality for upload`() = runTest {
        whenever(localStorageGateway.getVideoQuality()).thenReturn("3")
        assertThat(underTest.getVideoQuality()).isEqualTo("3")
    }

    @Test
    fun `test camera upload gets sync record by fingerprint`() = runTest {
        whenever(
            localStorageGateway.getSyncRecordByFingerprint(
                fingerprint = null,
                isSecondary = false,
                isCopy = false
            )
        ).thenReturn(null)
        assertThat(
            underTest.getSyncRecordByFingerprint(
                fingerprint = null,
                isSecondary = false,
                isCopy = false
            )
        ).isEqualTo(null)
    }

    @Test
    fun `test camera upload gets sync record by new path`() = runTest {
        whenever(localStorageGateway.getSyncRecordByNewPath("")).thenReturn(null)
        assertThat(underTest.getSyncRecordByNewPath("")).isEqualTo(null)
    }

    @Test
    fun `test camera upload gets sync record by local path`() = runTest {
        whenever(localStorageGateway.getSyncRecordByLocalPath("", false)).thenReturn(null)
        assertThat(underTest.getSyncRecordByLocalPath("", false)).isEqualTo(null)
    }

    @Test
    fun `test camera upload retrieves file name exists`() = runTest {
        whenever(localStorageGateway.doesFileNameExist("", false, -1)).thenReturn(true)
        whenever(syncRecordTypeIntMapper(any())).thenReturn(-1)
        assertThat(underTest.doesFileNameExist("", false, SyncRecordType.TYPE_ANY)).isEqualTo(true)
    }

    @Test
    fun `test camera upload retrieves local path exists`() = runTest {
        whenever(localStorageGateway.doesLocalPathExist("", false, -1)).thenReturn(true)
        whenever(syncRecordTypeIntMapper(any())).thenReturn(-1)
        assertThat(underTest.doesLocalPathExist("", false, SyncRecordType.TYPE_ANY)).isEqualTo(true)
    }

    @Test
    fun `test camera upload retrieves the correct sync time stamp`() = runTest {
        whenever(localStorageGateway.getPhotoTimeStamp()).thenReturn("150")
        assertThat(underTest.getSyncTimeStamp(SyncTimeStamp.PRIMARY_PHOTO)).isEqualTo(
            "150"
        )
    }

    @Test
    fun `test camera upload retrieves if credentials exist`() = runTest {
        whenever(localStorageGateway.doCredentialsExist()).thenReturn(true)
        assertThat(underTest.doCredentialsExist()).isEqualTo(true)
    }

    @Test
    fun `test camera upload retrieves if preferences are set`() = runTest {
        whenever(localStorageGateway.doPreferencesExist()).thenReturn(true)
        assertThat(underTest.doPreferencesExist()).isEqualTo(true)
    }

    @Test
    fun `test camera upload retrieves if camera upload sync is enabled`() = runTest {
        whenever(localStorageGateway.isSyncEnabled()).thenReturn(true)
        assertThat(underTest.isSyncEnabled()).isEqualTo(true)
    }

    @Test
    fun `test camera upload retrieves sync local path`() = runTest {
        whenever(localStorageGateway.getSyncLocalPath()).thenReturn("")
        assertThat(underTest.getSyncLocalPath()).isEqualTo("")
    }

    @Test
    fun `test camera upload retrieves secondary folder path`() = runTest {
        whenever(localStorageGateway.getSecondaryFolderPath()).thenReturn("")
        assertThat(underTest.getSecondaryFolderPath()).isEqualTo("")
    }

    @Test
    fun `test camera upload retrieves remove GPS preference`() = runTest {
        whenever(localStorageGateway.getRemoveGpsDefault()).thenReturn(false)
        assertThat(underTest.getRemoveGpsDefault()).isEqualTo(false)
    }

    @Test
    fun `test camera upload retrieves upload video quality`() = runTest {
        whenever(localStorageGateway.getUploadVideoQuality()).thenReturn("")
        assertThat(underTest.getUploadVideoQuality()).isEqualTo("")
    }

    @Test
    fun `test camera upload retrieves keep file names preference`() = runTest {
        whenever(localStorageGateway.getKeepFileNames()).thenReturn(true)
        assertThat(underTest.getKeepFileNames()).isEqualTo(true)
    }

    @Test
    fun `test camera upload folder is on external SD card`() = runTest {
        whenever(localStorageGateway.isFolderExternalSd()).thenReturn(true)
        assertThat(underTest.isFolderExternalSd()).isEqualTo(true)
    }

    @Test
    fun `test camera upload retrieves external SD card URI`() = runTest {
        whenever(localStorageGateway.getUriExternalSd()).thenReturn("")
        assertThat(underTest.getUriExternalSd()).isEqualTo("")
    }

    @Test
    fun `test camera upload retrieves secondary folder enabled preference`() = runTest {
        whenever(localStorageGateway.isSecondaryMediaFolderEnabled()).thenReturn(false)
        assertThat(underTest.isSecondaryMediaFolderEnabled()).isEqualTo(false)
    }

    @Test
    fun `test camera upload if secondary media folder is on external SD card`() = runTest {
        whenever(localStorageGateway.isMediaFolderExternalSd()).thenReturn(false)
        assertThat(underTest.isMediaFolderExternalSd()).isEqualTo(false)
    }

    @Test
    fun `test camera upload retrieves media folder external SD card URI`() = runTest {
        whenever(localStorageGateway.getUriMediaFolderExternalSd()).thenReturn("")
        assertThat(underTest.getUriMediaFolderExternalSd()).isEqualTo("")
    }

    @Test
    fun `test camera upload should clear all sync records`() = runTest {
        whenever(localStorageGateway.shouldClearSyncRecords()).thenReturn(true)
        assertThat(underTest.shouldClearSyncRecords()).isEqualTo(true)
    }

    @Test
    fun `test camera upload retrieves maximal time stamp`() = runTest {
        whenever(localStorageGateway.getMaxTimestamp(false, -1)).thenReturn(1000L)
        whenever(syncRecordTypeIntMapper(any())).thenReturn(-1)
        assertThat(underTest.getMaxTimestamp(false, SyncRecordType.TYPE_ANY)).isEqualTo(1000L)
    }

    @Test
    fun `test camera upload retrieves video sync records by status`() = runTest {
        whenever(localStorageGateway.getVideoSyncRecordsByStatus(SyncStatus.STATUS_PENDING.value)).thenReturn(
            listOf(fakeRecord)
        )
        assertThat(underTest.getVideoSyncRecordsByStatus(SyncStatus.STATUS_PENDING.value)).isEqualTo(
            listOf(fakeRecord)
        )
    }

    @Test
    fun `test camera upload retrieves charging on size`() = runTest {
        whenever(localStorageGateway.getChargingOnSizeString()).thenReturn("1")
        assertThat(underTest.getChargingOnSize()).isEqualTo(1)
    }

    @Test
    fun `test camera upload retrieves convert on charging setting`() = runTest {
        whenever(localStorageGateway.convertOnCharging()).thenReturn(false)
        assertThat(underTest.convertOnCharging()).isEqualTo(false)
    }

    @Test
    fun `test that primary folder handle is returned successfully`() {
        runTest {
            val result = 1L
            whenever(localStorageGateway.getCamSyncHandle()).thenReturn(result)
            val actual = underTest.getPrimarySyncHandle()
            assertThat(actual).isEqualTo(result)
        }
    }

    @Test
    fun `test that secondary folder handle is returned successfully`() {
        runTest {
            val result = 2L
            whenever(localStorageGateway.getMegaHandleSecondaryFolder()).thenReturn(result)
            val actual = underTest.getSecondarySyncHandle()
            assertThat(actual).isEqualTo(result)
        }
    }

    @Test
    fun `test that correct GPS coordinates are retrieved by file type video`() {
        val result = Pair(6F, 9F)
        runTest {
            whenever(fileAttributeGateway.getVideoGPSCoordinates("")).thenReturn(result)
            val actual = underTest.getVideoGPSCoordinates("")
            assertThat(actual).isEqualTo(result)
        }
    }

    @Test
    fun `test that correct GPS coordinates are retrieved by file type photo`() {
        val result = Pair(6F, 9F)
        runTest {
            whenever(fileAttributeGateway.getPhotoGPSCoordinates("")).thenReturn(result)
            val actual = underTest.getPhotoGPSCoordinates("")
            assertThat(actual).isEqualTo(result)
        }
    }
}

package mega.privacy.android.feature.devicecenter.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.devicecenter.data.entity.Sync
import mega.privacy.android.feature.devicecenter.data.entity.SyncState
import mega.privacy.android.feature.devicecenter.data.entity.SyncStatus
import mega.privacy.android.feature.devicecenter.data.entity.SyncSubState
import mega.privacy.android.feature.devicecenter.data.entity.SyncType
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaBackupInfo
import nz.mega.sdk.MegaSync
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [SyncMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SyncMapperTest {
    private lateinit var underTest: SyncMapper

    private val syncStateMapper = mock<SyncStateMapper>()
    private val syncSubStateMapper = mock<SyncSubStateMapper>()
    private val syncStatusMapper = mock<SyncStatusMapper>()
    private val syncTypeMapper = mock<SyncTypeMapper>()

    // MegaBackupInfo values
    private val id = 123456L
    private val sdkType = MegaApiJava.BACKUP_TYPE_CAMERA_UPLOADS
    private val rootHandle = 789012L
    private val localFolderPath = "test/local/folder"
    private val deviceId = "12345-6789-device"
    private val sdkState = MegaBackupInfo.BACKUP_STATE_ACTIVE
    private val sdkSubState = MegaSync.Error.NO_SYNC_ERROR.swigValue()
    private val extraInfo = null
    private val name = "Test Sync Name"
    private val timestamp = 156732L
    private val sdkStatus = MegaBackupInfo.BACKUP_STATUS_UPTODATE
    private val progress = 0
    private val uploadCount = 50
    private val downloadCount = 0
    private val lastActivityTimestamp = 945601L
    private val lastSyncedNodeHandle = 432198L

    // Mapper converted values
    private val expectedType = SyncType.CAMERA_UPLOADS
    private val expectedState = SyncState.ACTIVE
    private val expectedSubState = SyncSubState.NO_SYNC_ERROR
    private val expectedStatus = SyncStatus.UPTODATE

    @BeforeAll
    fun setup() {
        underTest = SyncMapper(
            syncStateMapper = syncStateMapper,
            syncSubStateMapper = syncSubStateMapper,
            syncStatusMapper = syncStatusMapper,
            syncTypeMapper = syncTypeMapper,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(syncStateMapper, syncSubStateMapper, syncStatusMapper, syncTypeMapper)
    }

    @Test
    fun `test that the mapping is correct`() {
        val sdkBackupInfo = initMegaBackupInfo()

        whenever(syncStateMapper(sdkState)).thenReturn(expectedState)
        whenever(syncSubStateMapper(sdkSubState)).thenReturn(expectedSubState)
        whenever(syncStatusMapper(sdkStatus)).thenReturn(expectedStatus)
        whenever(syncTypeMapper(sdkType)).thenReturn(expectedType)

        assertMappedSyncObject(underTest(sdkBackupInfo))
    }

    /**
     * Initializes a [MegaBackupInfo] with data
     *
     * @return a [MegaBackupInfo] object
     */
    private fun initMegaBackupInfo() = mock<MegaBackupInfo> {
        on { id() }.thenReturn(id)
        on { type() }.thenReturn(sdkType)
        on { root() }.thenReturn(rootHandle)
        on { localFolder() }.thenReturn(localFolderPath)
        on { deviceId() }.thenReturn(deviceId)
        on { state() }.thenReturn(sdkState)
        on { substate() }.thenReturn(sdkSubState)
        on { extra() }.thenReturn(extraInfo)
        on { name() }.thenReturn(name)
        on { ts() }.thenReturn(timestamp)
        on { status() }.thenReturn(sdkStatus)
        on { progress() }.thenReturn(progress)
        on { uploads() }.thenReturn(uploadCount)
        on { downloads() }.thenReturn(downloadCount)
        on { activityTs() }.thenReturn(lastActivityTimestamp)
        on { lastSync() }.thenReturn(lastSyncedNodeHandle)
    }

    /**
     * Checks whether all values mapped from [MegaBackupInfo] to [Sync] are correct
     */
    private fun assertMappedSyncObject(sync: Sync) = assertAll(
        "Grouped Assertions of ${Sync::class.simpleName}",
        { assertThat(sync.id).isEqualTo(id) },
        { assertThat(sync.type).isEqualTo(expectedType) },
        { assertThat(sync.rootHandle).isEqualTo(rootHandle) },
        { assertThat(sync.localFolderPath).isEqualTo(localFolderPath) },
        { assertThat(sync.deviceId).isEqualTo(deviceId) },
        { assertThat(sync.state).isEqualTo(expectedState) },
        { assertThat(sync.subState).isEqualTo(expectedSubState) },
        { assertThat(sync.extraInfo).isEqualTo(extraInfo) },
        { assertThat(sync.name).isEqualTo(name) },
        { assertThat(sync.timestamp).isEqualTo(timestamp) },
        { assertThat(sync.status).isEqualTo(expectedStatus) },
        { assertThat(sync.progress).isEqualTo(progress) },
        { assertThat(sync.uploadCount).isEqualTo(uploadCount) },
        { assertThat(sync.downloadCount).isEqualTo(downloadCount) },
        { assertThat(sync.lastActivityTimestamp).isEqualTo(lastActivityTimestamp) },
        { assertThat(sync.lastSyncedNodeHandle).isEqualTo(lastSyncedNodeHandle) },
    )
}
package mega.privacy.android.data.mapper.backup

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.backup.BackupInfo
import mega.privacy.android.domain.entity.backup.BackupInfoHeartbeatStatus
import mega.privacy.android.domain.entity.backup.BackupInfoState
import mega.privacy.android.domain.entity.backup.BackupInfoSubState
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.backup.BackupInfoUserAgent
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
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

/**
 * Test class for [BackupInfoMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BackupInfoMapperTest {
    private lateinit var underTest: BackupInfoMapper

    private val backupInfoHeartbeatStatusMapper = mock<BackupInfoHeartbeatStatusMapper>()
    private val backupInfoStateMapper = mock<BackupInfoStateMapper>()
    private val backupInfoSubStateMapper = mock<BackupInfoSubStateMapper>()
    private val backupInfoTypeMapper = mock<BackupInfoTypeMapper>()
    private val backupInfoUserAgentMapper = mock<BackupInfoUserAgentMapper>()

    // MegaBackupInfo values
    private val id = 123456L
    private val sdkType = MegaApiJava.BACKUP_TYPE_CAMERA_UPLOADS
    private val rootHandle = 789012L
    private val localFolderPath = "test/local/folder"
    private val deviceId = "12345-6789-device"
    private val deviceUserAgent = "android"
    private val sdkState = MegaBackupInfo.BACKUP_STATE_ACTIVE
    private val sdkSubState = MegaSync.Error.NO_SYNC_ERROR.swigValue()
    private val extraInfo = null
    private val name = "Test Sync Name"
    private val timestamp = 156732L
    private val sdkHeartbeatStatus = MegaBackupInfo.BACKUP_STATUS_UPTODATE
    private val progress = 0
    private val uploadCount = 50
    private val downloadCount = 0
    private val lastActivityTimestamp = 945601L
    private val lastSyncedNodeHandle = 432198L

    // Mapper converted values
    private val expectedHeartbeatStatus = BackupInfoHeartbeatStatus.UPTODATE
    private val expectedState = BackupInfoState.ACTIVE
    private val expectedSubState = BackupInfoSubState.NO_SYNC_ERROR
    private val expectedType = BackupInfoType.CAMERA_UPLOADS
    private val expectedUserAgent = BackupInfoUserAgent.ANDROID

    @BeforeAll
    fun setup() {
        underTest = BackupInfoMapper(
            backupInfoHeartbeatStatusMapper = backupInfoHeartbeatStatusMapper,
            backupInfoStateMapper = backupInfoStateMapper,
            backupInfoSubStateMapper = backupInfoSubStateMapper,
            backupInfoTypeMapper = backupInfoTypeMapper,
            backupInfoUserAgentMapper = backupInfoUserAgentMapper,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            backupInfoHeartbeatStatusMapper,
            backupInfoStateMapper,
            backupInfoSubStateMapper,
            backupInfoTypeMapper,
            backupInfoUserAgentMapper,
        )
    }

    @Test
    fun `test that the mapping is correct`() {
        val sdkBackupInfo = initMegaBackupInfo()

        whenever(backupInfoHeartbeatStatusMapper(sdkHeartbeatStatus)).thenReturn(
            expectedHeartbeatStatus
        )
        whenever(backupInfoStateMapper(sdkState)).thenReturn(expectedState)
        whenever(backupInfoSubStateMapper(sdkSubState)).thenReturn(expectedSubState)
        whenever(backupInfoTypeMapper(sdkType)).thenReturn(expectedType)
        whenever(backupInfoUserAgentMapper(deviceUserAgent)).thenReturn(expectedUserAgent)

        assertMappedBackupInfoObject(underTest(sdkBackupInfo))
    }

    @Test
    fun `test that null is returned when mega backup info is null`() {
        assertThat(underTest(null)).isNull()
        verifyNoInteractions(
            backupInfoHeartbeatStatusMapper,
            backupInfoStateMapper,
            backupInfoSubStateMapper,
            backupInfoTypeMapper,
            backupInfoUserAgentMapper,
        )
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
        on { deviceUserAgent() }.thenReturn(deviceUserAgent)
        on { state() }.thenReturn(sdkState)
        on { substate() }.thenReturn(sdkSubState)
        on { extra() }.thenReturn(extraInfo)
        on { name() }.thenReturn(name)
        on { ts() }.thenReturn(timestamp)
        on { status() }.thenReturn(sdkHeartbeatStatus)
        on { progress() }.thenReturn(progress)
        on { uploads() }.thenReturn(uploadCount)
        on { downloads() }.thenReturn(downloadCount)
        on { activityTs() }.thenReturn(lastActivityTimestamp)
        on { lastSync() }.thenReturn(lastSyncedNodeHandle)
    }

    /**
     * Checks whether all values mapped from [MegaBackupInfo] to [BackupInfo] are correct
     *
     * @param backupInfo A potentially nullable [BackupInfo] object
     */
    private fun assertMappedBackupInfoObject(backupInfo: BackupInfo?) = backupInfo?.let {
        assertAll(
            "Grouped Assertions of ${BackupInfo::class.simpleName}",
            { assertThat(it.id).isEqualTo(id) },
            { assertThat(it.type).isEqualTo(expectedType) },
            { assertThat(it.rootHandle).isEqualTo(rootHandle) },
            { assertThat(it.localFolderPath).isEqualTo(localFolderPath) },
            { assertThat(it.deviceId).isEqualTo(deviceId) },
            { assertThat(it.userAgent).isEqualTo(expectedUserAgent) },
            { assertThat(it.state).isEqualTo(expectedState) },
            { assertThat(it.subState).isEqualTo(expectedSubState) },
            { assertThat(it.extraInfo).isEqualTo(extraInfo) },
            { assertThat(it.name).isEqualTo(name) },
            { assertThat(it.timestamp).isEqualTo(timestamp) },
            { assertThat(it.status).isEqualTo(expectedHeartbeatStatus) },
            { assertThat(it.progress).isEqualTo(progress) },
            { assertThat(it.uploadCount).isEqualTo(uploadCount) },
            { assertThat(it.downloadCount).isEqualTo(downloadCount) },
            { assertThat(it.lastActivityTimestamp).isEqualTo(lastActivityTimestamp) },
            { assertThat(it.lastSyncedNodeHandle).isEqualTo(lastSyncedNodeHandle) },
        )
    }
}
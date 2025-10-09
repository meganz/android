package mega.privacy.android.data.mapper.backup

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.backup.BackupInfo
import mega.privacy.android.domain.entity.backup.BackupInfoHeartbeatStatus
import mega.privacy.android.domain.entity.backup.BackupInfoState
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.backup.BackupInfoUserAgent
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncError
import nz.mega.sdk.MegaBackupInfo
import nz.mega.sdk.MegaBackupInfoList
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [BackupInfoListMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BackupInfoListMapperTest {

    private lateinit var underTest: BackupInfoListMapper

    private val backupInfoMapper = mock<BackupInfoMapper>()

    @BeforeAll
    fun setUp() {
        underTest = BackupInfoListMapper(backupInfoMapper)
    }

    @BeforeEach
    fun resetMocks() {
        reset(backupInfoMapper)
    }

    @Test
    fun `test that the mapping is correct`() = runTest {
        val backupSize = 2L
        val megaBackupInfoList = mock<MegaBackupInfoList> { on { size() }.thenReturn(backupSize) }
        val backupInfoList = mutableListOf<BackupInfo>()

        (0 until backupSize).forEach { index ->
            val megaBackupInfo = mock<MegaBackupInfo> { on { id() }.thenReturn(index + 1000L) }
            val backupInfo = mock<BackupInfo> { on { id }.thenReturn(index + 1000L) }
            whenever(megaBackupInfoList.get(index)).thenReturn(megaBackupInfo)
            whenever(backupInfoMapper(megaBackupInfo)).thenReturn(backupInfo)
            backupInfoList.add(backupInfo)
        }
        assertThat(underTest(megaBackupInfoList)).isEqualTo(backupInfoList)
    }

    @ParameterizedTest(name = "backup size: {0}")
    @ValueSource(longs = [0L, -1L])
    fun `test that an empty backup info list is returned`(backupSize: Long) = runTest {
        val megaBackupInfoList = mock<MegaBackupInfoList> { on { size() }.thenReturn(backupSize) }
        assertThat(underTest(megaBackupInfoList)).isEmpty()
    }

    @Test
    fun `test that an empty backup info list is returned if mega backup info list is null`() =
        runTest {
            assertThat(underTest(null)).isEmpty()
        }

    @Test
    fun `test that backup info list with single item is returned correctly`() = runTest {
        val backupSize = 1L
        val megaBackupInfoList = mock<MegaBackupInfoList> { on { size() }.thenReturn(backupSize) }

        val backupInfo = createBackupInfo(id = 1L, name = "Single Backup", timestamp = 1000L)
        setupMegaBackupInfoList(megaBackupInfoList, listOf(backupInfo))

        val result = underTest(megaBackupInfoList)

        assertThat(result).hasSize(1)
        assertThat(result[0]).isEqualTo(backupInfo)
    }

    /**
     * Creates a [BackupInfo] object for testing
     */
    private fun createBackupInfo(
        id: Long,
        name: String?,
        timestamp: Long,
        type: BackupInfoType = BackupInfoType.CAMERA_UPLOADS,
        state: BackupInfoState = BackupInfoState.ACTIVE,
        status: BackupInfoHeartbeatStatus = BackupInfoHeartbeatStatus.UPTODATE,
    ): BackupInfo {
        return BackupInfo(
            id = id,
            type = type,
            rootHandle = NodeId(123456L),
            localFolderPath = "/test/path",
            deviceId = "test-device-id",
            userAgent = BackupInfoUserAgent.ANDROID,
            state = state,
            subState = SyncError.NO_SYNC_ERROR,
            extraInfo = null,
            name = name,
            timestamp = timestamp,
            status = status,
            progress = 0,
            uploadCount = 0,
            downloadCount = 0,
            lastActivityTimestamp = timestamp,
            lastSyncedNodeHandle = 789012L
        )
    }

    /**
     * Sets up the mock [MegaBackupInfoList] with the provided backup infos
     */
    private suspend fun setupMegaBackupInfoList(
        megaBackupInfoList: MegaBackupInfoList,
        backupInfos: List<BackupInfo>,
    ) {
        backupInfos.forEachIndexed { index, backupInfo ->
            val megaBackupInfo = mock<MegaBackupInfo> { on { id() }.thenReturn(backupInfo.id) }
            whenever(megaBackupInfoList.get(index.toLong())).thenReturn(megaBackupInfo)
            whenever(backupInfoMapper(megaBackupInfo)).thenReturn(backupInfo)
        }
    }
}

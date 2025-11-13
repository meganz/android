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
        val backupInfoList = buildList {
            (0 until backupSize).forEach { index ->
                val megaBackupInfo = mock<MegaBackupInfo> { on { id() }.thenReturn(index + 1000L) }
                // Create BackupInfo objects with different device-folder combinations to avoid deduplication
                val backupInfo = mock<BackupInfo> {
                    on { id }.thenReturn(index + 1000L)
                    on { deviceId }.thenReturn("device-${index}")
                    on { rootHandle }.thenReturn(NodeId(index + 100L))
                    on { timestamp }.thenReturn((index + 1) * 1000L)
                }
                whenever(megaBackupInfoList.get(index)).thenReturn(megaBackupInfo)
                whenever(backupInfoMapper(megaBackupInfo)).thenReturn(backupInfo)
                add(backupInfo)
            }
        }.sortedByDescending { it.timestamp }

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

    @Test
    fun `test that backups are sorted by timestamp in descending order`() = runTest {
        val backupSize = 3L
        val megaBackupInfoList = mock<MegaBackupInfoList> { on { size() }.thenReturn(backupSize) }

        val backupInfo1 = createBackupInfo(
            id = 1L,
            name = "Backup 1",
            timestamp = 1000L,
            deviceId = "device1",
            rootHandle = NodeId(100L)
        )
        val backupInfo2 = createBackupInfo(
            id = 2L,
            name = "Backup 2",
            timestamp = 3000L,
            deviceId = "device2",
            rootHandle = NodeId(200L)
        )
        val backupInfo3 = createBackupInfo(
            id = 3L,
            name = "Backup 3",
            timestamp = 2000L,
            deviceId = "device3",
            rootHandle = NodeId(300L)
        )

        setupMegaBackupInfoList(megaBackupInfoList, listOf(backupInfo1, backupInfo2, backupInfo3))

        val result = underTest(megaBackupInfoList)

        assertThat(result).hasSize(3)
        assertThat(result[0].timestamp).isEqualTo(3000L) // Latest first
        assertThat(result[1].timestamp).isEqualTo(2000L)
        assertThat(result[2].timestamp).isEqualTo(1000L) // Oldest last
    }

    @Test
    fun `test that duplicates are removed keeping latest backup for same device-folder combination`() =
        runTest {
            val backupSize = 3L
            val megaBackupInfoList =
                mock<MegaBackupInfoList> { on { size() }.thenReturn(backupSize) }

            // Same device and root handle, different timestamps - should keep only the latest
            val olderBackup = createBackupInfo(
                id = 1L,
                name = "Older Backup",
                timestamp = 1000L,
                deviceId = "device1",
                rootHandle = NodeId(123456L)
            )
            val newerBackup = createBackupInfo(
                id = 2L,
                name = "Newer Backup",
                timestamp = 2000L,
                deviceId = "device1",
                rootHandle = NodeId(123456L)
            )
            val differentDevice = createBackupInfo(
                id = 3L,
                name = "Different Device",
                timestamp = 1500L,
                deviceId = "device2",
                rootHandle = NodeId(123456L)
            )

            setupMegaBackupInfoList(
                megaBackupInfoList,
                listOf(olderBackup, newerBackup, differentDevice)
            )

            val result = underTest(megaBackupInfoList)

            assertThat(result).hasSize(2)
            // Should contain the newer backup for device1 and the backup for device2
            assertThat(result).containsExactly(newerBackup, differentDevice)
            assertThat(result).doesNotContain(olderBackup)
        }

    @Test
    fun `test that different root handles on same device are kept separately`() = runTest {
        val backupSize = 2L
        val megaBackupInfoList = mock<MegaBackupInfoList> { on { size() }.thenReturn(backupSize) }

        // Same device, different root handles - both should be kept
        val backup1 = createBackupInfo(
            id = 1L,
            name = "Backup 1",
            timestamp = 1000L,
            deviceId = "device1",
            rootHandle = NodeId(100L)
        )
        val backup2 = createBackupInfo(
            id = 2L,
            name = "Backup 2",
            timestamp = 2000L,
            deviceId = "device1",
            rootHandle = NodeId(200L)
        )

        setupMegaBackupInfoList(megaBackupInfoList, listOf(backup1, backup2))

        val result = underTest(megaBackupInfoList)

        assertThat(result).hasSize(2)
        assertThat(result).containsExactly(backup2, backup1) // Sorted by timestamp descending
    }

    @Test
    fun `test that multiple duplicates keep only the latest one`() = runTest {
        val backupSize = 4L
        val megaBackupInfoList = mock<MegaBackupInfoList> { on { size() }.thenReturn(backupSize) }

        // Multiple backups for same device-folder combination
        val backup1 = createBackupInfo(
            id = 1L,
            name = "Backup 1",
            timestamp = 1000L,
            deviceId = "device1",
            rootHandle = NodeId(123456L)
        )
        val backup2 = createBackupInfo(
            id = 2L,
            name = "Backup 2",
            timestamp = 3000L,
            deviceId = "device1",
            rootHandle = NodeId(123456L)
        ) // Latest
        val backup3 = createBackupInfo(
            id = 3L,
            name = "Backup 3",
            timestamp = 2000L,
            deviceId = "device1",
            rootHandle = NodeId(123456L)
        )
        val backup4 = createBackupInfo(
            id = 4L,
            name = "Backup 4",
            timestamp = 1500L,
            deviceId = "device1",
            rootHandle = NodeId(123456L)
        )

        setupMegaBackupInfoList(megaBackupInfoList, listOf(backup1, backup2, backup3, backup4))

        val result = underTest(megaBackupInfoList)

        assertThat(result).hasSize(1)
        assertThat(result[0]).isEqualTo(backup2) // Should only keep the latest one
    }

    @Test
    fun `test that null backups from mapper are filtered out`() = runTest {
        val backupSize = 2L
        val megaBackupInfoList = mock<MegaBackupInfoList> { on { size() }.thenReturn(backupSize) }

        val megaBackupInfo1 = mock<MegaBackupInfo> { on { id() }.thenReturn(1L) }
        val megaBackupInfo2 = mock<MegaBackupInfo> { on { id() }.thenReturn(2L) }

        whenever(megaBackupInfoList.get(0)).thenReturn(megaBackupInfo1)
        whenever(megaBackupInfoList.get(1)).thenReturn(megaBackupInfo2)

        // First mapper call returns null (e.g., node not found), second returns valid backup
        whenever(backupInfoMapper(megaBackupInfo1)).thenReturn(null)
        val validBackup = createBackupInfo(id = 2L, name = "Valid Backup", timestamp = 2000L)
        whenever(backupInfoMapper(megaBackupInfo2)).thenReturn(validBackup)

        val result = underTest(megaBackupInfoList)

        assertThat(result).hasSize(1)
        assertThat(result[0]).isEqualTo(validBackup)
    }

    @Test
    fun `test that complex device-folder combinations are handled correctly`() = runTest {
        val backupSize = 6L
        val megaBackupInfoList = mock<MegaBackupInfoList> { on { size() }.thenReturn(backupSize) }

        // Complex scenario: Multiple devices, multiple folders, multiple timestamps
        val backupInfos = listOf(
            createBackupInfo(
                id = 1L,
                name = "Device1-Folder1-Old",
                timestamp = 1000L,
                deviceId = "device1",
                rootHandle = NodeId(100L)
            ),
            createBackupInfo(
                id = 2L,
                name = "Device1-Folder1-New",
                timestamp = 3000L,
                deviceId = "device1",
                rootHandle = NodeId(100L)
            ), // Latest for device1:100
            createBackupInfo(
                id = 3L,
                name = "Device1-Folder2",
                timestamp = 2000L,
                deviceId = "device1",
                rootHandle = NodeId(200L)
            ),
            createBackupInfo(
                id = 4L,
                name = "Device2-Folder1",
                timestamp = 2500L,
                deviceId = "device2",
                rootHandle = NodeId(100L)
            ),
            createBackupInfo(
                id = 5L,
                name = "Device2-Folder2-Old",
                timestamp = 1500L,
                deviceId = "device2",
                rootHandle = NodeId(200L)
            ),
            createBackupInfo(
                id = 6L,
                name = "Device2-Folder2-New",
                timestamp = 2200L,
                deviceId = "device2",
                rootHandle = NodeId(200L)
            ) // Latest for device2:200
        )

        setupMegaBackupInfoList(megaBackupInfoList, backupInfos)

        val result = underTest(megaBackupInfoList)

        assertThat(result).hasSize(4)
        // Should contain latest backup for each device-folder combination
        val resultNames = result.map { it.name }
        assertThat(resultNames).containsExactly(
            "Device1-Folder1-New",  // Latest for device1:100
            "Device2-Folder1",      // Only one for device2:100
            "Device2-Folder2-New",  // Latest for device2:200
            "Device1-Folder2"       // Only one for device1:200
        )
    }

    @Test
    fun `test that deduplication key format uses colon separator correctly`() = runTest {
        val backupSize = 2L
        val megaBackupInfoList = mock<MegaBackupInfoList> { on { size() }.thenReturn(backupSize) }

        // Test edge case where device ID could potentially conflict with root handle
        // Device "123" with root handle 456 should be different from device "12" with root handle 3456
        val backup1 = createBackupInfo(
            id = 1L,
            name = "Backup1",
            timestamp = 1000L,
            deviceId = "123",
            rootHandle = NodeId(456L)
        )
        val backup2 = createBackupInfo(
            id = 2L,
            name = "Backup2",
            timestamp = 2000L,
            deviceId = "12",
            rootHandle = NodeId(3456L)
        )

        setupMegaBackupInfoList(megaBackupInfoList, listOf(backup1, backup2))

        val result = underTest(megaBackupInfoList)

        // Both should be kept as they are different device-folder combinations
        assertThat(result).hasSize(2)
        assertThat(result).containsExactly(backup2, backup1) // Sorted by timestamp
    }

    @Test
    fun `test that empty device ID or zero root handle are handled correctly`() = runTest {
        val backupSize = 3L
        val megaBackupInfoList = mock<MegaBackupInfoList> { on { size() }.thenReturn(backupSize) }

        val backup1 = createBackupInfo(
            id = 1L,
            name = "Empty Device",
            timestamp = 1000L,
            deviceId = "",
            rootHandle = NodeId(100L)
        )
        val backup2 = createBackupInfo(
            id = 2L,
            name = "Zero Handle",
            timestamp = 2000L,
            deviceId = "device1",
            rootHandle = NodeId(0L)
        )
        val backup3 = createBackupInfo(
            id = 3L,
            name = "Normal Backup",
            timestamp = 3000L,
            deviceId = "device1",
            rootHandle = NodeId(100L)
        )

        setupMegaBackupInfoList(megaBackupInfoList, listOf(backup1, backup2, backup3))

        val result = underTest(megaBackupInfoList)

        // All should be kept as they have different device-folder combinations
        assertThat(result).hasSize(3)
        assertThat(result[0].timestamp).isEqualTo(3000L) // Latest first
        assertThat(result[1].timestamp).isEqualTo(2000L)
        assertThat(result[2].timestamp).isEqualTo(1000L)
    }

    @Test
    fun `test that same timestamp backups are handled deterministically`() = runTest {
        val backupSize = 2L
        val megaBackupInfoList = mock<MegaBackupInfoList> { on { size() }.thenReturn(backupSize) }

        // Same device, same folder, same timestamp - should keep first one after sorting (stable sort)
        val backup1 = createBackupInfo(
            id = 1L,
            name = "Backup1",
            timestamp = 2000L,
            deviceId = "device1",
            rootHandle = NodeId(100L)
        )
        val backup2 = createBackupInfo(
            id = 2L,
            name = "Backup2",
            timestamp = 2000L,
            deviceId = "device1",
            rootHandle = NodeId(100L)
        )

        setupMegaBackupInfoList(megaBackupInfoList, listOf(backup1, backup2))

        val result = underTest(megaBackupInfoList)

        assertThat(result).hasSize(1)
        // Should keep one of them (implementation dependent on sort stability)
        assertThat(result[0].timestamp).isEqualTo(2000L)
        assertThat(result[0].deviceId).isEqualTo("device1")
        assertThat(result[0].rootHandle).isEqualTo(NodeId(100L))
    }

    /**
     * Creates a [BackupInfo] object for testing
     */
    private fun createBackupInfo(
        id: Long,
        name: String?,
        timestamp: Long,
        deviceId: String = "test-device-id",
        rootHandle: NodeId = NodeId(123456L),
        type: BackupInfoType = BackupInfoType.CAMERA_UPLOADS,
        state: BackupInfoState = BackupInfoState.ACTIVE,
        status: BackupInfoHeartbeatStatus = BackupInfoHeartbeatStatus.UPTODATE,
    ): BackupInfo {
        return BackupInfo(
            id = id,
            type = type,
            rootHandle = rootHandle,
            localFolderPath = "/test/path",
            deviceId = deviceId,
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

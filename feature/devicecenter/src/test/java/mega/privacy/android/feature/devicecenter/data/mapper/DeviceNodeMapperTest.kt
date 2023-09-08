package mega.privacy.android.feature.devicecenter.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.backup.BackupInfo
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceCenterNodeStatus
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceFolderNode
import mega.privacy.android.feature.devicecenter.domain.entity.OtherDeviceNode
import mega.privacy.android.feature.devicecenter.domain.entity.OwnDeviceNode
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [DeviceNodeMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DeviceNodeMapperTest {
    private lateinit var underTest: DeviceNodeMapper

    private val deviceFolderNodeMapper = mock<DeviceFolderNodeMapper>()
    private val deviceNodeStatusMapper = mock<DeviceNodeStatusMapper>()

    @BeforeAll
    fun setUp() {
        underTest = DeviceNodeMapper(
            deviceFolderNodeMapper = deviceFolderNodeMapper,
            deviceNodeStatusMapper = deviceNodeStatusMapper,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(deviceFolderNodeMapper, deviceNodeStatusMapper)
    }

    @Test
    fun `test that the current device is mapped`() {
        val currentDeviceId = "1234-5678"
        val deviceIdAndNameMap = mapOf(
            currentDeviceId to "Current Device Name",
        )
        val backupInfoList = listOf<BackupInfo>(
            mock { on { deviceId }.thenReturn(currentDeviceId) }
        )
        val currentDeviceBackupInfo = backupInfoList.filterBackupInfoByDeviceId(currentDeviceId)
        val currentDeviceFolderNodes = listOf<DeviceFolderNode>(
            mock { on { name }.thenReturn("Current Device Folder One") }
        )
        val currentDeviceStatus = DeviceCenterNodeStatus.NoCameraUploads

        whenever(deviceFolderNodeMapper(currentDeviceBackupInfo)).thenReturn(
            currentDeviceFolderNodes
        )
        whenever(
            deviceNodeStatusMapper(
                folders = currentDeviceFolderNodes,
                isCameraUploadsEnabled = false,
                isCurrentDevice = true
            )
        ).thenReturn(currentDeviceStatus)

        assertThat(
            underTest(
                backupInfoList = backupInfoList,
                currentDeviceId = currentDeviceId,
                deviceIdAndNameMap = deviceIdAndNameMap,
                isCameraUploadsEnabled = false,
            )
        ).isEqualTo(
            listOf(
                OwnDeviceNode(
                    id = currentDeviceId,
                    name = deviceIdAndNameMap[currentDeviceId].orEmpty(),
                    status = currentDeviceStatus,
                    folders = currentDeviceFolderNodes,
                )
            )
        )
    }

    @Test
    fun `test that a non current device is mapped if it has backup folders`() {
        val currentDeviceId = "1234-5678"
        val otherDeviceId = "7391-9042"
        val deviceIdAndNameMap = mapOf(
            currentDeviceId to "Current Device Name",
            otherDeviceId to "Other Device name",
        )
        val backupInfoList = listOf<BackupInfo>(
            mock { on { deviceId }.thenReturn(currentDeviceId) },
            mock { on { deviceId }.thenReturn(otherDeviceId) },
        )
        val isCameraUploadsEnabled = false

        // Current Device
        val currentDeviceBackupInfo = backupInfoList.filterBackupInfoByDeviceId(currentDeviceId)
        val currentDeviceFolderNodes = listOf<DeviceFolderNode>(
            mock { on { name }.thenReturn("Current Device Folder One") }
        )
        val currentDeviceStatus = DeviceCenterNodeStatus.NoCameraUploads
        whenever(deviceFolderNodeMapper(currentDeviceBackupInfo)).thenReturn(
            currentDeviceFolderNodes
        )
        whenever(
            deviceNodeStatusMapper(
                folders = currentDeviceFolderNodes,
                isCameraUploadsEnabled = isCameraUploadsEnabled,
                isCurrentDevice = true
            )
        ).thenReturn(currentDeviceStatus)

        // Other Device/s
        val otherDeviceBackupInfo = backupInfoList.filterBackupInfoByDeviceId(otherDeviceId)
        val otherDeviceFolderNodes = listOf<DeviceFolderNode>(
            mock { on { name }.thenReturn("Other Device Folder One") }
        )
        val otherDeviceStatus = DeviceCenterNodeStatus.Overquota
        whenever(deviceFolderNodeMapper(otherDeviceBackupInfo)).thenReturn(otherDeviceFolderNodes)
        whenever(
            deviceNodeStatusMapper(
                folders = otherDeviceFolderNodes,
                isCameraUploadsEnabled = isCameraUploadsEnabled,
                isCurrentDevice = false,
            )
        ).thenReturn(otherDeviceStatus)

        val deviceNodes = listOf(
            OwnDeviceNode(
                id = currentDeviceId,
                name = deviceIdAndNameMap[currentDeviceId].orEmpty(),
                status = currentDeviceStatus,
                folders = currentDeviceFolderNodes,
            ),
            OtherDeviceNode(
                id = otherDeviceId,
                name = deviceIdAndNameMap[otherDeviceId].orEmpty(),
                status = otherDeviceStatus,
                folders = otherDeviceFolderNodes,
            ),
        )
        assertThat(
            underTest(
                backupInfoList = backupInfoList,
                currentDeviceId = currentDeviceId,
                deviceIdAndNameMap = deviceIdAndNameMap,
                isCameraUploadsEnabled = isCameraUploadsEnabled,
            )
        ).isEqualTo(deviceNodes)
    }

    @Test
    fun `test that a non current device is not mapped if it has no backup folders`() {
        val currentDeviceId = "1234-5678"
        val otherDeviceId = "7391-9042"
        val deviceIdAndNameMap = mapOf(
            currentDeviceId to "Current Device Name",
            otherDeviceId to "Other Device name",
        )
        val backupInfoList = listOf<BackupInfo>(
            mock { on { deviceId }.thenReturn(currentDeviceId) },
        )
        val isCameraUploadsEnabled = false

        // Current Device
        val currentDeviceBackupInfo = backupInfoList.filterBackupInfoByDeviceId(currentDeviceId)
        val currentDeviceFolderNodes = listOf<DeviceFolderNode>(
            mock { on { name }.thenReturn("Current Device Folder One") }
        )
        val currentDeviceStatus = DeviceCenterNodeStatus.NoCameraUploads
        whenever(deviceFolderNodeMapper(currentDeviceBackupInfo)).thenReturn(
            currentDeviceFolderNodes
        )
        whenever(
            deviceNodeStatusMapper(
                folders = currentDeviceFolderNodes,
                isCameraUploadsEnabled = isCameraUploadsEnabled,
                isCurrentDevice = true
            )
        ).thenReturn(currentDeviceStatus)

        // Other Device/s
        val otherDeviceStatus = DeviceCenterNodeStatus.Unknown
        whenever(deviceFolderNodeMapper(emptyList())).thenReturn(emptyList())
        whenever(
            deviceNodeStatusMapper(
                folders = emptyList(),
                isCameraUploadsEnabled = isCameraUploadsEnabled,
                isCurrentDevice = false,
            )
        ).thenReturn(otherDeviceStatus)

        val deviceNodes = listOf(
            OwnDeviceNode(
                id = currentDeviceId,
                name = deviceIdAndNameMap[currentDeviceId].orEmpty(),
                status = currentDeviceStatus,
                folders = currentDeviceFolderNodes,
            ),
        )
        assertThat(
            underTest(
                backupInfoList = backupInfoList,
                currentDeviceId = currentDeviceId,
                deviceIdAndNameMap = deviceIdAndNameMap,
                isCameraUploadsEnabled = isCameraUploadsEnabled,
            )
        ).isEqualTo(deviceNodes)
    }

    private fun List<BackupInfo>.filterBackupInfoByDeviceId(deviceId: String) =
        this.filter { backupInfo -> backupInfo.deviceId == deviceId }
}
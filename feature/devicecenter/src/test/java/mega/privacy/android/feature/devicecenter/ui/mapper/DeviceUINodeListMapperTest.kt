package mega.privacy.android.feature.devicecenter.ui.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.backup.BackupInfoUserAgent
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceCenterNodeStatus
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceFolderNode
import mega.privacy.android.feature.devicecenter.domain.entity.OtherDeviceNode
import mega.privacy.android.feature.devicecenter.domain.entity.OwnDeviceNode
import mega.privacy.android.feature.devicecenter.ui.model.BackupDeviceFolderUINode
import mega.privacy.android.feature.devicecenter.ui.model.OtherDeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.OwnDeviceUINode
import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceIconType
import mega.privacy.android.feature.devicecenter.ui.model.icon.FolderIconType
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [DeviceUINodeListMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DeviceUINodeListMapperTest {
    private lateinit var underTest: DeviceUINodeListMapper

    private val deviceCenterUINodeStatusMapper = mock<DeviceCenterUINodeStatusMapper>()
    private val deviceFolderUINodeListMapper = mock<DeviceFolderUINodeListMapper>()
    private val deviceUINodeIconMapper = mock<DeviceUINodeIconMapper>()

    @BeforeAll
    fun setUp() {
        underTest = DeviceUINodeListMapper(
            deviceCenterUINodeStatusMapper = deviceCenterUINodeStatusMapper,
            deviceFolderUINodeListMapper = deviceFolderUINodeListMapper,
            deviceUINodeIconMapper = deviceUINodeIconMapper,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(deviceCenterUINodeStatusMapper, deviceFolderUINodeListMapper, deviceUINodeIconMapper)
    }

    @ParameterizedTest(name = "is current device: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the mapping for the current device is correct`(isCurrentDevice: Boolean) {
        val deviceId = "12345-6789"
        val deviceName = "MacBook Pro M2"
        val deviceStatus = DeviceCenterNodeStatus.UpToDate

        val folderId = "0123-456"
        val folderName = "Backup Folder One"
        val folderStatus = DeviceCenterNodeStatus.UpToDate
        val folderType = BackupInfoType.BACKUP_UPLOAD
        val folderUserAgent = BackupInfoUserAgent.WINDOWS
        val folderRootHandle = 789012L

        val deviceFolders = listOf(
            DeviceFolderNode(
                id = folderId,
                name = folderName,
                status = folderStatus,
                rootHandle = folderRootHandle,
                type = folderType,
                userAgent = folderUserAgent,
            ),
        )
        val deviceList = if (isCurrentDevice) {
            listOf(
                OwnDeviceNode(
                    id = deviceId,
                    name = deviceName,
                    status = deviceStatus,
                    folders = deviceFolders,
                ),
            )
        } else {
            listOf(
                OtherDeviceNode(
                    id = deviceId,
                    name = deviceName,
                    status = deviceStatus,
                    folders = deviceFolders,
                ),
            )
        }

        val expectedUINodeStatus = DeviceCenterUINodeStatus.UpToDate
        val expectedDeviceUINodeIcon = DeviceIconType.Mobile
        val expectedFolderUINodeIcon = FolderIconType.Backup
        val expectedFolderUINodeList = listOf(
            BackupDeviceFolderUINode(
                id = folderId,
                name = folderName,
                icon = expectedFolderUINodeIcon,
                status = expectedUINodeStatus,
                rootHandle = folderRootHandle,
            )
        )
        val expectedDeviceUINodeList = if (isCurrentDevice) {
            listOf(
                OwnDeviceUINode(
                    id = deviceId,
                    name = deviceName,
                    icon = expectedDeviceUINodeIcon,
                    status = expectedUINodeStatus,
                    folders = expectedFolderUINodeList,
                ),
            )
        } else {
            listOf(
                OtherDeviceUINode(
                    id = deviceId,
                    name = deviceName,
                    icon = expectedDeviceUINodeIcon,
                    status = expectedUINodeStatus,
                    folders = expectedFolderUINodeList,
                ),
            )
        }

        whenever(
            deviceCenterUINodeStatusMapper(
                isDevice = true,
                status = DeviceCenterNodeStatus.UpToDate,
            )
        ).thenReturn(
            expectedUINodeStatus
        )
        whenever(deviceFolderUINodeListMapper(deviceFolders)).thenReturn(expectedFolderUINodeList)
        whenever(deviceUINodeIconMapper(deviceFolders)).thenReturn(expectedDeviceUINodeIcon)

        assertThat(underTest(deviceList)).isEqualTo(expectedDeviceUINodeList)
    }
}
package mega.privacy.android.feature.devicecenter.ui.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.devicecenter.R
import mega.privacy.android.feature.devicecenter.data.entity.BackupInfoType
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceCenterNodeStatus
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceFolderNode
import mega.privacy.android.feature.devicecenter.ui.model.DeviceFolderUINode
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [DeviceFolderUINodeListMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DeviceFolderUINodeListMapperTest {
    private lateinit var underTest: DeviceFolderUINodeListMapper

    private val deviceCenterUINodeStatusMapper = mock<DeviceCenterUINodeStatusMapper>()
    private val deviceFolderUINodeIconMapper = mock<DeviceFolderUINodeIconMapper>()

    @BeforeAll
    fun setUp() {
        underTest = DeviceFolderUINodeListMapper(
            deviceCenterUINodeStatusMapper = deviceCenterUINodeStatusMapper,
            deviceFolderUINodeIconMapper = deviceFolderUINodeIconMapper,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(deviceCenterUINodeStatusMapper, deviceFolderUINodeIconMapper)
    }

    @Test
    fun `test that the mapping is correct`() {
        val folderId = "12345-6789"
        val folderName = "Backup Folder One"
        val folderStatus = DeviceCenterNodeStatus.UpToDate
        val folderType = BackupInfoType.CAMERA_UPLOADS
        val folderList = listOf(
            DeviceFolderNode(
                id = folderId,
                name = folderName,
                status = folderStatus,
                type = folderType,
            )
        )
        val expectedUINodeStatus = DeviceCenterUINodeStatus.UpToDate
        val expectedFolderUINodeIcon = R.drawable.ic_device_folder_camera_uploads

        whenever(deviceCenterUINodeStatusMapper(folderStatus)).thenReturn(expectedUINodeStatus)
        whenever(deviceFolderUINodeIconMapper(folderType)).thenReturn(expectedFolderUINodeIcon)

        assertThat(underTest(folderList)).isEqualTo(
            listOf(
                DeviceFolderUINode(
                    id = folderId,
                    name = folderName,
                    icon = expectedFolderUINodeIcon,
                    status = expectedUINodeStatus,
                )
            )
        )
    }
}
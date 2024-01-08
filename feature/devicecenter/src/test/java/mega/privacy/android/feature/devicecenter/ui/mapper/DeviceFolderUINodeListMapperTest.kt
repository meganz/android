package mega.privacy.android.feature.devicecenter.ui.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.backup.BackupInfoUserAgent
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceCenterNodeStatus
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceFolderNode
import mega.privacy.android.feature.devicecenter.ui.model.BackupDeviceFolderUINode
import mega.privacy.android.feature.devicecenter.ui.model.NonBackupDeviceFolderUINode
import mega.privacy.android.feature.devicecenter.ui.model.icon.FolderIconType
import mega.privacy.android.feature.devicecenter.ui.model.status.DeviceCenterUINodeStatus
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
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
    fun `test that the mapping results in a backup folder`() {
        val folderId = "12345-6789"
        val folderName = "Backup Folder One"
        val folderStatus = DeviceCenterNodeStatus.UpToDate
        val folderType = BackupInfoType.BACKUP_UPLOAD
        val folderUserAgent = BackupInfoUserAgent.WINDOWS
        val folderRootHandle = 789012L
        val folderList = listOf(
            DeviceFolderNode(
                id = folderId,
                name = folderName,
                status = folderStatus,
                rootHandle = folderRootHandle,
                type = folderType,
                userAgent = folderUserAgent,
            )
        )
        val expectedUINodeStatus = DeviceCenterUINodeStatus.UpToDate
        val expectedFolderUINodeIcon = FolderIconType.Backup

        whenever(deviceCenterUINodeStatusMapper(folderStatus)).thenReturn(expectedUINodeStatus)
        whenever(deviceFolderUINodeIconMapper(folderType)).thenReturn(expectedFolderUINodeIcon)

        assertThat(underTest(folderList)).isEqualTo(
            listOf(
                BackupDeviceFolderUINode(
                    id = folderId,
                    name = folderName,
                    icon = expectedFolderUINodeIcon,
                    status = expectedUINodeStatus,
                    rootHandle = folderRootHandle,
                )
            )
        )
    }

    @ParameterizedTest(name = "when the backup type is {0}")
    @EnumSource(
        value = BackupInfoType::class, names = ["BACKUP_UPLOAD"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `test that the mapping results in a non backup folder`(
        folderType: BackupInfoType,
    ) {
        val folderId = "12345-6789"
        val folderName = "Backup Folder One"
        val folderStatus = DeviceCenterNodeStatus.UpToDate
        val folderUserAgent = BackupInfoUserAgent.WINDOWS
        val folderRootHandle = 789012L
        val folderList = listOf(
            DeviceFolderNode(
                id = folderId,
                name = folderName,
                status = folderStatus,
                rootHandle = folderRootHandle,
                type = folderType,
                userAgent = folderUserAgent,
            )
        )
        val expectedUINodeStatus = DeviceCenterUINodeStatus.UpToDate
        val expectedFolderUINodeIcon = FolderIconType.CameraUploads

        whenever(deviceCenterUINodeStatusMapper(folderStatus)).thenReturn(expectedUINodeStatus)
        whenever(deviceFolderUINodeIconMapper(folderType)).thenReturn(expectedFolderUINodeIcon)

        assertThat(underTest(folderList)).isEqualTo(
            listOf(
                NonBackupDeviceFolderUINode(
                    id = folderId,
                    name = folderName,
                    icon = expectedFolderUINodeIcon,
                    status = expectedUINodeStatus,
                    rootHandle = folderRootHandle,
                )
            )
        )
    }
}
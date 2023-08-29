package mega.privacy.android.feature.devicecenter.ui.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.devicecenter.domain.entity.BackupInfoType
import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceCenterUINodeIcon
import mega.privacy.android.feature.devicecenter.ui.model.icon.FolderIconType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * Test class for [DeviceFolderUINodeIconMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DeviceFolderUINodeIconMapperTest {
    private lateinit var underTest: DeviceFolderUINodeIconMapper

    @BeforeAll
    fun setUp() {
        underTest = DeviceFolderUINodeIconMapper()
    }

    @ParameterizedTest(name = "when backup info type is {0}, then the device folder icon is {1}")
    @MethodSource("provideParameters")
    fun `test that the correct device folder icon is returned`(
        backupInfoType: BackupInfoType,
        deviceFolderIcon: DeviceCenterUINodeIcon,
    ) {
        assertThat(underTest(backupInfoType)).isEqualTo(deviceFolderIcon)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(BackupInfoType.CAMERA_UPLOADS, FolderIconType.CameraUploads),
        Arguments.of(BackupInfoType.MEDIA_UPLOADS, FolderIconType.CameraUploads),
        Arguments.of(BackupInfoType.INVALID, FolderIconType.Sync),
        Arguments.of(BackupInfoType.UP_SYNC, FolderIconType.Sync),
        Arguments.of(BackupInfoType.DOWN_SYNC, FolderIconType.Sync),
        Arguments.of(BackupInfoType.TWO_WAY_SYNC, FolderIconType.Sync),
        Arguments.of(BackupInfoType.BACKUP_UPLOAD, FolderIconType.Backup),
    )
}
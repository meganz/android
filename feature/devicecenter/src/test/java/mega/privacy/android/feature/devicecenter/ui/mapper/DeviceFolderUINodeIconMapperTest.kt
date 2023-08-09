package mega.privacy.android.feature.devicecenter.ui.mapper

import androidx.annotation.DrawableRes
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.devicecenter.R
import mega.privacy.android.feature.devicecenter.data.entity.BackupInfoType
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
        @DrawableRes deviceFolderIconRes: Int,
    ) {
        assertThat(underTest(backupInfoType)).isEqualTo(deviceFolderIconRes)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(BackupInfoType.CAMERA_UPLOADS, R.drawable.ic_device_folder_camera_uploads),
        Arguments.of(BackupInfoType.MEDIA_UPLOADS, R.drawable.ic_device_folder_camera_uploads),
        Arguments.of(BackupInfoType.INVALID, R.drawable.ic_device_folder_sync),
        Arguments.of(BackupInfoType.UP_SYNC, R.drawable.ic_device_folder_sync),
        Arguments.of(BackupInfoType.DOWN_SYNC, R.drawable.ic_device_folder_sync),
        Arguments.of(BackupInfoType.TWO_WAY_SYNC, R.drawable.ic_device_folder_sync),
        Arguments.of(BackupInfoType.BACKUP_UPLOAD, R.drawable.ic_device_folder_backup),
    )
}
package mega.privacy.android.feature.devicecenter.ui.mapper

import androidx.annotation.DrawableRes
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.devicecenter.R
import mega.privacy.android.feature.devicecenter.data.entity.BackupInfoType
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceFolderNode
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import java.util.stream.Stream

/**
 * Test class for [DeviceUINodeIconMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DeviceUINodeIconMapperTest {
    private lateinit var underTest: DeviceUINodeIconMapper

    @BeforeAll
    fun setUp() {
        underTest = DeviceUINodeIconMapper()
    }

    @ParameterizedTest(name = "device folders: {0}, expected device icon: {1}")
    @MethodSource("provideParameters")
    fun `test that the correct device icon is returned`(
        deviceFolders: List<DeviceFolderNode>,
        @DrawableRes expectedDeviceIconRes: Int,
    ) {
        assertThat(underTest(deviceFolders)).isEqualTo(expectedDeviceIconRes)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(
            mockDeviceFolders(listOf(BackupInfoType.CAMERA_UPLOADS)),
            R.drawable.ic_device_mobile
        ),
        Arguments.of(
            mockDeviceFolders(listOf(BackupInfoType.MEDIA_UPLOADS)),
            R.drawable.ic_device_mobile
        ),
        Arguments.of(
            mockDeviceFolders(listOf(BackupInfoType.CAMERA_UPLOADS, BackupInfoType.MEDIA_UPLOADS)),
            R.drawable.ic_device_mobile,
        ),
        Arguments.of(
            mockDeviceFolders(
                listOf(
                    BackupInfoType.CAMERA_UPLOADS,
                    BackupInfoType.MEDIA_UPLOADS,
                    BackupInfoType.DOWN_SYNC,
                )
            ),
            R.drawable.ic_device_mobile,
        ),
        Arguments.of(mockDeviceFolders(emptyList()), R.drawable.ic_device_pc),
        Arguments.of(mockDeviceFolders(listOf(BackupInfoType.DOWN_SYNC)), R.drawable.ic_device_pc),
    )

    private fun mockDeviceFolders(backupFolderTypes: List<BackupInfoType>) =
        backupFolderTypes.map { backupInfoType ->
            mock<DeviceFolderNode> { on { type }.thenReturn(backupInfoType) }
        }
}
package mega.privacy.android.feature.devicecenter.ui.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.devicecenter.data.entity.BackupInfoType
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceFolderNode
import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceCenterUINodeIcon
import mega.privacy.android.feature.devicecenter.ui.model.icon.DeviceIconType
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
        expectedDeviceIcon: DeviceCenterUINodeIcon,
    ) {
        assertThat(underTest(deviceFolders)).isEqualTo(expectedDeviceIcon)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(
            mockDeviceFolders(listOf(BackupInfoType.CAMERA_UPLOADS)),
            DeviceIconType.Mobile
        ),
        Arguments.of(
            mockDeviceFolders(listOf(BackupInfoType.MEDIA_UPLOADS)),
            DeviceIconType.Mobile
        ),
        Arguments.of(
            mockDeviceFolders(listOf(BackupInfoType.CAMERA_UPLOADS, BackupInfoType.MEDIA_UPLOADS)),
            DeviceIconType.Mobile,
        ),
        Arguments.of(
            mockDeviceFolders(
                listOf(
                    BackupInfoType.CAMERA_UPLOADS,
                    BackupInfoType.MEDIA_UPLOADS,
                    BackupInfoType.DOWN_SYNC,
                )
            ),
            DeviceIconType.Mobile,
        ),
        Arguments.of(mockDeviceFolders(emptyList()), DeviceIconType.PC),
        Arguments.of(mockDeviceFolders(listOf(BackupInfoType.DOWN_SYNC)), DeviceIconType.PC),
    )

    private fun mockDeviceFolders(backupFolderTypes: List<BackupInfoType>) =
        backupFolderTypes.map { backupInfoType ->
            mock<DeviceFolderNode> { on { type }.thenReturn(backupInfoType) }
        }
}
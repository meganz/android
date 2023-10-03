package mega.privacy.android.feature.devicecenter.ui.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.backup.BackupInfoUserAgent
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

    @ParameterizedTest(name = "expected device icon: {1}")
    @MethodSource("provideParameters")
    fun `test that the correct device icon is returned`(
        deviceFolders: List<DeviceFolderNode>,
        expectedDeviceIcon: DeviceCenterUINodeIcon,
    ) {
        assertThat(underTest(deviceFolders)).isEqualTo(expectedDeviceIcon)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(
            mockDeviceFoldersThroughBackupUserAgents(
                listOf(
                    BackupInfoUserAgent.WINDOWS,
                    BackupInfoUserAgent.LINUX,
                    BackupInfoUserAgent.MAC,
                    BackupInfoUserAgent.ANDROID,
                    BackupInfoUserAgent.IPHONE,
                )
            ),
            DeviceIconType.Windows,
        ),
        Arguments.of(
            mockDeviceFoldersThroughBackupUserAgents(
                listOf(
                    BackupInfoUserAgent.LINUX,
                    BackupInfoUserAgent.MAC,
                    BackupInfoUserAgent.ANDROID,
                    BackupInfoUserAgent.IPHONE,
                )
            ),
            DeviceIconType.Linux,
        ),
        Arguments.of(
            mockDeviceFoldersThroughBackupUserAgents(
                listOf(
                    BackupInfoUserAgent.MAC,
                    BackupInfoUserAgent.ANDROID,
                    BackupInfoUserAgent.IPHONE,
                )
            ),
            DeviceIconType.Mac,
        ),
        Arguments.of(
            mockDeviceFoldersThroughBackupUserAgents(
                listOf(
                    BackupInfoUserAgent.ANDROID,
                    BackupInfoUserAgent.IPHONE,
                )
            ),
            DeviceIconType.Android,
        ),
        Arguments.of(
            mockDeviceFoldersThroughBackupUserAgents(listOf(BackupInfoUserAgent.IPHONE)),
            DeviceIconType.IOS,
        ),
        Arguments.of(
            mockDeviceFoldersThroughBackupInfoTypes(listOf(BackupInfoType.CAMERA_UPLOADS)),
            DeviceIconType.Mobile,
        ),
        Arguments.of(
            mockDeviceFoldersThroughBackupInfoTypes(listOf(BackupInfoType.MEDIA_UPLOADS)),
            DeviceIconType.Mobile,
        ),
        Arguments.of(
            mockDeviceFoldersThroughBackupInfoTypes(
                listOf(
                    BackupInfoType.CAMERA_UPLOADS,
                    BackupInfoType.MEDIA_UPLOADS,
                )
            ),
            DeviceIconType.Mobile,
        ),
        Arguments.of(emptyList<DeviceFolderNode>(), DeviceIconType.PC)
    )

    private fun mockDeviceFoldersThroughBackupUserAgents(backupUserAgents: List<BackupInfoUserAgent>) =
        backupUserAgents.map { backupUserAgent ->
            mock<DeviceFolderNode> { on { userAgent }.thenReturn(backupUserAgent) }
        }

    private fun mockDeviceFoldersThroughBackupInfoTypes(backupFolderTypes: List<BackupInfoType>) =
        backupFolderTypes.map { backupInfoType ->
            mock<DeviceFolderNode> { on { type }.thenReturn(backupInfoType) }
        }
}
package mega.privacy.android.feature.devicecenter.ui.mapper

import androidx.annotation.DrawableRes
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.devicecenter.R
import mega.privacy.android.feature.devicecenter.data.entity.BackupInfoType
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceFolderNode
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceNode
import mega.privacy.android.feature.devicecenter.domain.entity.OwnDeviceNode
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

    @ParameterizedTest(name = "device node: {0}, expected device icon: {1}")
    @MethodSource("provideParameters")
    fun `test that the correct device icon is returned`(
        deviceNode: DeviceNode,
        @DrawableRes expectedDeviceIconRes: Int,
    ) {
        assertThat(underTest(deviceNode)).isEqualTo(expectedDeviceIconRes)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(
            mockDeviceNode(listOf(BackupInfoType.CAMERA_UPLOADS)),
            R.drawable.ic_device_mobile
        ),
        Arguments.of(
            mockDeviceNode(listOf(BackupInfoType.MEDIA_UPLOADS)),
            R.drawable.ic_device_mobile
        ),
        Arguments.of(
            mockDeviceNode(listOf(BackupInfoType.CAMERA_UPLOADS, BackupInfoType.MEDIA_UPLOADS)),
            R.drawable.ic_device_mobile,
        ),
        Arguments.of(
            mockDeviceNode(
                listOf(
                    BackupInfoType.CAMERA_UPLOADS,
                    BackupInfoType.MEDIA_UPLOADS,
                    BackupInfoType.DOWN_SYNC,
                )
            ),
            R.drawable.ic_device_mobile,
        ),
        Arguments.of(mockDeviceNode(emptyList()), R.drawable.ic_device_pc),
        Arguments.of(mockDeviceNode(listOf(BackupInfoType.DOWN_SYNC)), R.drawable.ic_device_pc),
    )

    private fun mockDeviceNode(backupFolderTypes: List<BackupInfoType>): OwnDeviceNode {
        val expectedFolders = backupFolderTypes.map { backupInfoType ->
            mock<DeviceFolderNode> { on { type }.thenReturn(backupInfoType) }
        }
        return mock { on { folders }.thenReturn(expectedFolders) }
    }
}
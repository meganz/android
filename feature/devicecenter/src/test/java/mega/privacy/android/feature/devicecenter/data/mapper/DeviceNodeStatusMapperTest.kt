package mega.privacy.android.feature.devicecenter.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.devicecenter.data.entity.BackupInfoSubState
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceCenterNodeStatus
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceFolderNode
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import java.util.stream.Stream

/**
 * Test class for [DeviceNodeStatusMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DeviceNodeStatusMapperTest {
    private lateinit var underTest: DeviceNodeStatusMapper

    @BeforeAll
    fun setUp() {
        underTest = DeviceNodeStatusMapper()
    }

    @ParameterizedTest(name = "prioritized device status: {1}")
    @MethodSource("provideParameters")
    fun `test that the prioritized device status is returned`(
        folders: List<DeviceFolderNode>,
        expectedDeviceStatus: DeviceCenterNodeStatus,
    ) {
        assertThat(underTest(folders)).isEqualTo(expectedDeviceStatus)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(
            listOf<DeviceFolderNode>(
                mock { on { status }.thenReturn(DeviceCenterNodeStatus.Syncing(50)) },
                mock { on { status }.thenReturn(DeviceCenterNodeStatus.Scanning) }
            ), DeviceCenterNodeStatus.Syncing(0)
        ),
        Arguments.of(
            listOf<DeviceFolderNode>(
                mock { on { status }.thenReturn(DeviceCenterNodeStatus.Scanning) },
                mock { on { status }.thenReturn(DeviceCenterNodeStatus.Initializing) }
            ),
            DeviceCenterNodeStatus.Scanning,
        ),
        Arguments.of(
            listOf<DeviceFolderNode>(
                mock { on { status }.thenReturn(DeviceCenterNodeStatus.Initializing) },
                mock { on { status }.thenReturn(DeviceCenterNodeStatus.Paused) }
            ),
            DeviceCenterNodeStatus.Initializing,
        ),
        Arguments.of(
            listOf<DeviceFolderNode>(
                mock { on { status }.thenReturn(DeviceCenterNodeStatus.Paused) },
                mock { on { status }.thenReturn(DeviceCenterNodeStatus.Overquota) }
            ),
            DeviceCenterNodeStatus.Paused,
        ),
        Arguments.of(
            listOf<DeviceFolderNode>(
                mock { on { status }.thenReturn(DeviceCenterNodeStatus.Overquota) },
                mock { on { status }.thenReturn(DeviceCenterNodeStatus.Blocked(BackupInfoSubState.ACCOUNT_BLOCKED)) }
            ),
            DeviceCenterNodeStatus.Overquota,
        ),
        Arguments.of(
            listOf<DeviceFolderNode>(
                mock { on { status }.thenReturn(DeviceCenterNodeStatus.Blocked(BackupInfoSubState.ACCOUNT_BLOCKED)) },
                mock { on { status }.thenReturn(DeviceCenterNodeStatus.UpToDate) }
            ),
            DeviceCenterNodeStatus.Blocked(null),
        ),
        Arguments.of(
            listOf<DeviceFolderNode>(
                mock { on { status }.thenReturn(DeviceCenterNodeStatus.UpToDate) },
                mock { on { status }.thenReturn(DeviceCenterNodeStatus.Offline) }
            ),
            DeviceCenterNodeStatus.UpToDate,
        ),
        Arguments.of(
            listOf<DeviceFolderNode>(
                mock { on { status }.thenReturn(DeviceCenterNodeStatus.Offline) },
                mock { on { status }.thenReturn(DeviceCenterNodeStatus.Disabled) }
            ),
            DeviceCenterNodeStatus.Offline,
        ),
        Arguments.of(
            listOf<DeviceFolderNode>(
                mock { on { status }.thenReturn(DeviceCenterNodeStatus.Disabled) },
                mock { on { status }.thenReturn(DeviceCenterNodeStatus.Stopped) }
            ),
            DeviceCenterNodeStatus.Disabled,
        ),
        Arguments.of(
            listOf<DeviceFolderNode>(
                mock { on { status }.thenReturn(DeviceCenterNodeStatus.Stopped) },
                mock { on { status }.thenReturn(DeviceCenterNodeStatus.Unknown) }
            ),
            DeviceCenterNodeStatus.Stopped,
        ),
        Arguments.of(
            listOf<DeviceFolderNode>(mock { on { status }.thenReturn(DeviceCenterNodeStatus.Unknown) }),
            DeviceCenterNodeStatus.Unknown,
        ),
        Arguments.of(emptyList<DeviceFolderNode>(), DeviceCenterNodeStatus.Unknown)
    )
}
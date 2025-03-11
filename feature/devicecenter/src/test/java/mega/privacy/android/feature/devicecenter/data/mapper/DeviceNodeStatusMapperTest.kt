package mega.privacy.android.feature.devicecenter.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.sync.SyncError
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceStatus
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceFolderNode
import mega.privacy.android.feature.devicecenter.domain.entity.DeviceFolderStatus
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
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

    @Test
    fun `test that a nothing set up device status is returned if nothing has been set up yet`() {
        assertThat(
            underTest(
                folders = emptyList(),
                isCurrentDevice = true,
            )
        ).isEqualTo(DeviceStatus.NothingSetUp)
    }

    @ParameterizedTest(name = "expected current device status: {1}")
    @MethodSource("provideParameters")
    fun `test that the calculated device status is returned for current device`(
        folders: List<DeviceFolderNode>,
        expectedDeviceStatus: DeviceStatus,
    ) {
        assertThat(
            underTest(
                folders = folders,
                isCurrentDevice = true,
            )
        ).isEqualTo(expectedDeviceStatus)
    }

    @ParameterizedTest(name = "expected other device status: {1}")
    @MethodSource("provideParameters")
    fun `test that the calculated device status is returned for other devices`(
        folders: List<DeviceFolderNode>,
        expectedDeviceStatus: DeviceStatus,
    ) {
        assertThat(
            underTest(
                folders = folders,
                isCurrentDevice = false,
            )
        ).isEqualTo(expectedDeviceStatus)
    }

    @Test
    fun `test that unknown device status is returned for other devices when folders are empty`() {
        assertThat(
            underTest(
                folders = emptyList(),
                isCurrentDevice = false,
            )
        ).isEqualTo(DeviceStatus.Unknown)
    }


    private fun provideParameters() = Stream.of(
        Arguments.of(
            listOf<DeviceFolderNode>(
                mock { on { status }.thenReturn(DeviceFolderStatus.Updating(50)) },
                mock { on { status }.thenReturn(DeviceFolderStatus.Updating(0)) }
            ), DeviceStatus.Updating
        ),
        Arguments.of(
            listOf<DeviceFolderNode>(
                mock { on { status }.thenReturn(DeviceFolderStatus.Updating(0)) },
                mock { on { status }.thenReturn(DeviceFolderStatus.Updating(0)) }
            ),
            DeviceStatus.Updating,
        ),
        Arguments.of(
            listOf<DeviceFolderNode>(
                mock { on { status }.thenReturn(DeviceFolderStatus.Updating(0)) },
                mock { on { status }.thenReturn(DeviceFolderStatus.Paused) }
            ),
            DeviceStatus.AttentionNeeded,
        ),
        Arguments.of(
            listOf<DeviceFolderNode>(
                mock { on { status }.thenReturn(DeviceFolderStatus.Paused) },
                mock { on { status }.thenReturn(DeviceFolderStatus.Error(SyncError.STORAGE_OVERQUOTA)) }
            ),
            DeviceStatus.AttentionNeeded,
        ),
        Arguments.of(
            listOf<DeviceFolderNode>(
                mock { on { status }.thenReturn(DeviceFolderStatus.Error(SyncError.STORAGE_OVERQUOTA)) },
                mock { on { status }.thenReturn(DeviceFolderStatus.Error(SyncError.ACCOUNT_BLOCKED)) }
            ),
            DeviceStatus.AttentionNeeded,
        ),
        Arguments.of(
            listOf<DeviceFolderNode>(
                mock { on { status }.thenReturn(DeviceFolderStatus.Error(SyncError.ACCOUNT_BLOCKED)) },
                mock { on { status }.thenReturn(DeviceFolderStatus.Error(SyncError.INSUFFICIENT_DISK_SPACE)) },
            ),
            DeviceStatus.AttentionNeeded,
        ),
        Arguments.of(
            listOf<DeviceFolderNode>(
                mock { on { status }.thenReturn(DeviceFolderStatus.Error(SyncError.INSUFFICIENT_DISK_SPACE)) },
                mock { on { status }.thenReturn(DeviceFolderStatus.UpToDate) }
            ),
            DeviceStatus.AttentionNeeded,
        ),
        Arguments.of(
            listOf<DeviceFolderNode>(
                mock { on { status }.thenReturn(DeviceFolderStatus.UpToDate) },
                mock { on { status }.thenReturn(DeviceFolderStatus.Error(null)) }
            ),
            DeviceStatus.AttentionNeeded,
        ),
        Arguments.of(
            listOf<DeviceFolderNode>(
                mock { on { status }.thenReturn(DeviceFolderStatus.Error(null)) },
                mock { on { status }.thenReturn(DeviceFolderStatus.Disabled) }
            ),
            DeviceStatus.AttentionNeeded,
        ),
        Arguments.of(
            listOf<DeviceFolderNode>(
                mock { on { status }.thenReturn(DeviceFolderStatus.Disabled) },
                mock { on { status }.thenReturn(DeviceFolderStatus.Error(null)) }
            ),
            DeviceStatus.AttentionNeeded,
        ),
        Arguments.of(
            listOf<DeviceFolderNode>(
                mock { on { status }.thenReturn(DeviceFolderStatus.Error(null)) },
                mock { on { status }.thenReturn(DeviceFolderStatus.Unknown) }
            ),
            DeviceStatus.AttentionNeeded,
        ),
        Arguments.of(
            listOf<DeviceFolderNode>(mock { on { status }.thenReturn(DeviceFolderStatus.Unknown) }),
            DeviceStatus.Unknown,
        ),

        Arguments.of(
            listOf<DeviceFolderNode>(
                mock { on { status }.thenReturn(DeviceFolderStatus.UpToDate) },
                mock { on { status }.thenReturn(DeviceFolderStatus.Error(null)) },
                mock { on { status }.thenReturn(DeviceFolderStatus.Inactive) }
            ),
            DeviceStatus.Inactive,
        ),
        Arguments.of(
            listOf<DeviceFolderNode>(
                mock { on { status }.thenReturn(DeviceFolderStatus.UpToDate) },
                mock { on { status }.thenReturn(DeviceFolderStatus.Updating(0)) },
                mock { on { status }.thenReturn(DeviceFolderStatus.Error(null)) }
            ),
            DeviceStatus.AttentionNeeded,
        ),
        Arguments.of(
            listOf<DeviceFolderNode>(
                mock { on { status }.thenReturn(DeviceFolderStatus.UpToDate) },
                mock { on { status }.thenReturn(DeviceFolderStatus.Updating(0)) },
                mock { on { status }.thenReturn(DeviceFolderStatus.UpToDate) }
            ),
            DeviceStatus.Updating,
        ),
        Arguments.of(
            listOf<DeviceFolderNode>(
                mock { on { status }.thenReturn(DeviceFolderStatus.UpToDate) },
                mock { on { status }.thenReturn(DeviceFolderStatus.UpToDate) },
                mock { on { status }.thenReturn(DeviceFolderStatus.UpToDate) }
            ),
            DeviceStatus.UpToDate,
        ),
    )
}